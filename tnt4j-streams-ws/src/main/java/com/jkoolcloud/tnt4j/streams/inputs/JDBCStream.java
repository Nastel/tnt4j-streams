/*
 * Copyright 2014-2018 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jkoolcloud.tnt4j.streams.inputs;

import java.io.Closeable;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.Semaphore;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.configure.WsStreamProperties;
import com.jkoolcloud.tnt4j.streams.scenario.WsReqResponse;
import com.jkoolcloud.tnt4j.streams.scenario.WsRequest;
import com.jkoolcloud.tnt4j.streams.scenario.WsResponse;
import com.jkoolcloud.tnt4j.streams.scenario.WsScenarioStep;
import com.jkoolcloud.tnt4j.streams.utils.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Implements a scheduled JDBC query call activity stream, where each query call returned {@link java.sql.ResultSet} row
 * is assumed to represent a single activity or event which should be recorded.
 * <p>
 * JDBC query call is performed by invoking {@link java.sql.Connection#prepareStatement(String)} and
 * {@link java.sql.PreparedStatement#executeQuery()}.
 * <p>
 * This activity stream requires parsers that can support {@link java.sql.ResultSet} data to parse
 * {@link com.jkoolcloud.tnt4j.streams.scenario.WsResponse#getData()} provided result set.
 * <p>
 * This activity stream supports the following configuration properties (in addition to those supported by
 * {@link AbstractWsStream}):
 * <ul>
 * <li>DropRecurrentResultSets - flag indicating whether to drop streaming stream input buffer contained recurring
 * result sets, when stream input scheduler invokes JDBC queries faster than they can be processed (parsed and sent to
 * sink, e.g. because of sink/JKool limiter throttling). Default value - {@code false}. (Optional)</li>
 * <li>QueryFetchRows - number of rows to be fetched from database per query returned {@link java.sql.ResultSet} cursor
 * access. Value {@code 0} implies to use default JDBC setting. See {@link java.sql.Statement#setFetchSize(int)} for
 * details. Default value - {@code 0}. (Optional)</li>
 * <li>QueryMaxRows - limit for the maximum number of rows that query returned {@link java.sql.ResultSet} can contain.
 * Value {@code 0} implies to use default JDBC setting. See {@link java.sql.Statement#setMaxRows(int)} for details.
 * Default value - {@code 0}. (Optional)</li>
 * <li>set of <a href="https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby">HikariCP supported
 * properties</a> used to configure JDBC data source. (Optional)</li>
 * <li>when {@value com.jkoolcloud.tnt4j.streams.configure.StreamProperties#PROP_USE_EXECUTOR_SERVICE} is set to
 * {@code true} and {@value com.jkoolcloud.tnt4j.streams.configure.StreamProperties#PROP_EXECUTOR_THREADS_QTY} is
 * greater than {@code 1}, value for that property is reset to {@code 1} since {@link java.sql.ResultSet} can't be
 * accessed in multi-thread manner.</li>
 * </ul>
 *
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 * @see java.sql.DriverManager#getConnection(String, java.util.Properties)
 * @see java.sql.Connection#prepareStatement(String)
 * @see java.sql.PreparedStatement#executeQuery()
 */
public class JDBCStream extends AbstractWsStream<ResultSet> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(JDBCStream.class);

	private static final String QUERY_NAME_PROP = "QueryName"; // NON-NLS

	private static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd"; // NON-NLS
	private static final String DEFAULT_TIME_PATTERN = "HH:mm:ss"; // NON-NLS
	private static final String DEFAULT_TIMESTAMP_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS"; // NON-NLS

	/**
	 * Contains custom HikariCP configuration properties used to configure JDBC data source.
	 */
	protected Map<String, String> jdbcProperties = new HashMap<>();

	private boolean dropRecurrentResultSets = false;
	private int fetchSize = 0;
	private int maxRows = 0;

	private Map<String, DataSource> dbDataSources = new HashMap<>(3);

	/**
	 * Constructs an empty JDBCStream. Requires configuration settings to set input stream source.
	 */
	public JDBCStream() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (WsStreamProperties.PROP_DROP_RECURRENT_RESULT_SETS.equalsIgnoreCase(name)) {
			dropRecurrentResultSets = Utils.toBoolean(value);
		} else if (WsStreamProperties.PROP_QUERY_FETCH_ROWS.equalsIgnoreCase(name)) {
			fetchSize = Integer.parseInt(value);
		} else if (WsStreamProperties.PROP_QUERY_MAX_ROWS.equalsIgnoreCase(name)) {
			maxRows = Integer.parseInt(value);
		} else if (!StreamsConstants.isStreamCfgProperty(name, WsStreamProperties.class)) {
			jdbcProperties.put(name, decPassword(value));
		}
	}

	@Override
	public Object getProperty(String name) {
		if (WsStreamProperties.PROP_DROP_RECURRENT_RESULT_SETS.equalsIgnoreCase(name)) {
			return dropRecurrentResultSets;
		}
		if (WsStreamProperties.PROP_QUERY_FETCH_ROWS.equalsIgnoreCase(name)) {
			return fetchSize;
		}
		if (WsStreamProperties.PROP_QUERY_MAX_ROWS.equalsIgnoreCase(name)) {
			return maxRows;
		}

		Object pValue = super.getProperty(name);
		if (pValue != null) {
			return pValue;
		}

		return jdbcProperties.get(name);
	}

	@Override
	protected void initialize() throws Exception {
		boolean useExecService = (boolean) getProperty(StreamProperties.PROP_USE_EXECUTOR_SERVICE);
		int threadCount = (int) getProperty(StreamProperties.PROP_EXECUTOR_THREADS_QTY);
		if (useExecService && threadCount > 1) {
			logger().log(OpLevel.WARNING, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"JDBCStream.resetting.thread.count.property", threadCount);
			setProperty(StreamProperties.PROP_EXECUTOR_THREADS_QTY, "1");
		}

		super.initialize();
	}

	@Override
	protected void cleanup() {
		super.cleanup();

		for (DataSource dbDataSource : dbDataSources.values()) {
			if (dbDataSource instanceof Closeable) {
				Utils.close((Closeable) dbDataSource);
			}
		}

		dbDataSources.clear();
	}

	@Override
	protected void cleanupItem(WsResponse<ResultSet> item) {
		if (item != null && item.getData() != null) {
			try {
				closeRS(item.getData());
			} catch (SQLException exc) {
				Utils.logThrowable(logger(), OpLevel.WARNING,
						StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
						"JDBCStream.rs.consumption.exception", exc);
			}
		}
	}

	@Override
	protected long getActivityItemByteSize(WsResponse<ResultSet> item) {
		return 0; // TODO
	}

	@Override
	protected JobDetail buildJob(String jobId, JobDataMap jobAttrs) {
		return JobBuilder.newJob(JdbcCallJob.class).withIdentity(jobId).usingJobData(jobAttrs).build();
	}

	@Override
	protected boolean isItemConsumed(WsResponse<ResultSet> item) {
		if (item == null || item.getData() == null) {
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"JDBCStream.rs.consumption.null");
			return true;
		}

		if (dropRecurrentResultSets) {
			WsResponse<ResultSet> recurringItem = getRecurrentResultSet(item, inputBuffer);

			if (recurringItem != null) {
				logger().log(OpLevel.WARNING, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
						"JDBCStream.rs.consumption.drop", item.getParameters().get(QUERY_NAME_PROP).getValue());
				inputBuffer.remove(recurringItem);
				try {
					closeRS(recurringItem.getData());
				} catch (SQLException exc) {
					Utils.logThrowable(logger(), OpLevel.WARNING,
							StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
							"JDBCStream.rs.consumption.exception", exc);
				}
			}
		}

		ResultSet rs = item.getData();
		try {
			if (rs.isClosed() || !rs.next()) {
				closeRS(rs);

				logger().log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
						"JDBCStream.rs.consumption.done");
				return true;
			}

			logger().log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"JDBCStream.rs.consumption.marker.new", rs.getRow());
			return false;
		} catch (SQLException exc) {
			Utils.logThrowable(logger(), OpLevel.WARNING,
					StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"JDBCStream.rs.consumption.exception", exc);
			return true;
		}
	}

	private void closeRS(ResultSet rs) throws SQLException {
		Statement st = rs.getStatement();
		Connection conn = st == null ? null : st.getConnection();

		if (conn != null && !conn.getAutoCommit()) {
			try {
				conn.commit();
			} catch (SQLException exc) {
			}
		}

		Utils.close(rs);
		Utils.close(st);
		Utils.close(conn);
	}

	@SuppressWarnings("unchecked")
	protected WsResponse<ResultSet> getRecurrentResultSet(WsResponse<ResultSet> resp, Queue<?> buffer) {
		for (Object item : buffer) {
			if (item instanceof WsResponse) {
				WsResponse<ResultSet> respItem = (WsResponse<ResultSet>) item;

				if (respItem.getParameters().get(QUERY_NAME_PROP).getValue()
						.equals(resp.getParameters().get(QUERY_NAME_PROP).getValue())) {
					return respItem;
				}
			}
		}

		return null;
	}

	@Override
	protected boolean initItemForParsing(WsResponse<ResultSet> item) {
		return !isItemConsumed(item);
	}

	/**
	 * Performs JDBC query call.
	 *
	 * @param url
	 *            DB connection URL
	 * @param user
	 *            DB user name
	 * @param pass
	 *            DB user password
	 * @param query
	 *            DB query
	 * @param params
	 *            DB query parameters map
	 * @param stream
	 *            stream instance to use for JDBC query execution
	 * @return JDBC call returned result set {@link java.sql.ResultSet}
	 * @throws SQLException
	 *             if exception occurs while performing JDBC call
	 */
	protected static ResultSet executeJdbcCall(String url, String user, String pass, String query,
			Map<String, WsRequest.Parameter> params, JDBCStream stream) throws SQLException {
		if (StringUtils.isEmpty(url)) {
			LOGGER.log(OpLevel.WARNING, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"JDBCStream.db.conn.not.defined", url);
			return null;
		}

		if (StringUtils.isEmpty(query)) {
			LOGGER.log(OpLevel.WARNING, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"JDBCStream.query.not.defined", query);
			return null;
		}

		LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
				"JDBCStream.invoking.query", url, query);

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
				"JDBCStream.obtaining.db.connection", url);
		Duration cod = Duration.arm();
		Connection dbConn = getDbConnection(url, user, pass, stream);

		LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
				"JDBCStream.db.connection.obtained", url, cod.durationHMS());

		LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
				"JDBCStream.preparing.query", query);

		PreparedStatement statement = dbConn.prepareStatement(query);
		if (stream.fetchSize > 0) {
			statement.setFetchSize(stream.fetchSize);
		}
		if (stream.maxRows > 0) {
			statement.setMaxRows(stream.maxRows);
		}

		addStatementParameters(statement, params, stream);

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
				"JDBCStream.executing.query", url);
		Duration qed = Duration.arm();
		ResultSet rs = statement.executeQuery();

		LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
				"JDBCStream.query.execution.completed", url, qed.durationHMS(), cod.durationHMS());

		return rs;
	}

	/**
	 * Obtains database connection for provided connection URL and user credentials. Additional set of HikariCP
	 * configuration properties are taken from {@link #jdbcProperties} map.
	 * 
	 * @param url
	 *            DB connection URL
	 * @param user
	 *            DB user name
	 * @param pass
	 *            DB user password
	 * @param stream
	 *            stream instance to use for JDBC query execution
	 * @return DB connection instance to be used to execute queries
	 * @throws SQLException
	 *             if data source fails to obtain database connection
	 */
	protected static Connection getDbConnection(String url, String user, String pass, JDBCStream stream)
			throws SQLException {
		DataSource hds = stream.dbDataSources.get(url);

		if (hds == null) {
			Properties dbProps = new Properties();
			dbProps.putAll(stream.jdbcProperties);

			HikariConfig dbConfig = new HikariConfig(dbProps);
			dbConfig.setJdbcUrl(url);
			dbConfig.setUsername(user);
			dbConfig.setPassword(pass);

			hds = new HikariDataSource(dbConfig);

			stream.dbDataSources.put(url, hds);
		}

		return hds.getConnection();
	}

	/**
	 * Returns custom JDBC configuration properties stored in {@link #jdbcProperties} map.
	 *
	 * @return custom JDBC configuration properties
	 */
	@Override
	protected Map<String, String> getConfigProperties() {
		return jdbcProperties;
	}

	/**
	 * Sets prepared SQL statement parameters provided by {@code params} map.
	 *
	 * @param statement
	 *            prepared SQL statement parameters to set
	 * @param params
	 *            SQL query parameters map
	 * @param stream
	 *            stream instance to use for JDBC query execution
	 * @throws SQLException
	 *             if exception occurs while setting prepared statement parameter
	 */
	protected static void addStatementParameters(PreparedStatement statement, Map<String, WsRequest.Parameter> params,
			JDBCStream stream) throws SQLException {
		if (params != null) {
			for (Map.Entry<String, WsRequest.Parameter> param : params.entrySet()) {
				try {
					int pIdx = Integer.parseInt(param.getValue().getId());
					String type = param.getValue().getType();
					String value = param.getValue().getValue();
					String format = param.getValue().getFormat();

					if (type == null) {
						type = "";
					}

					switch (type.toUpperCase()) {
					case "INTEGER": // NON-NLS
						value = stream.fillInRequestData(value, format);
						if ("null".equalsIgnoreCase(value)) {
							setNullParameter(statement, pIdx, Types.INTEGER, type.toUpperCase());
						} else {
							int iValue = Integer.parseInt(value);
							statement.setInt(pIdx, iValue);
							LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
									"JDBCStream.set.query.parameter", pIdx, value, type.toUpperCase());
						}
						break;
					case "BIGINT": // NON-NLS
						value = stream.fillInRequestData(value, format);
						if ("null".equalsIgnoreCase(value)) {
							setNullParameter(statement, pIdx, Types.BIGINT, type.toUpperCase());
						} else {
							long lValue = Long.parseLong(value);
							statement.setLong(pIdx, lValue);
							LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
									"JDBCStream.set.query.parameter", pIdx, value, type.toUpperCase());
						}
						break;
					case "FLOAT": // NON-NLS
						value = stream.fillInRequestData(value, format);
						if ("null".equalsIgnoreCase(value)) {
							setNullParameter(statement, pIdx, Types.FLOAT, type.toUpperCase());
						} else {
							float fValue = Float.parseFloat(value);
							statement.setFloat(pIdx, fValue);
							LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
									"JDBCStream.set.query.parameter", pIdx, value, type.toUpperCase());
						}
						break;
					case "DOUBLE": // NON-NLS
					case "REAL": // NON-NLS
					case "DECIMAL": // NON-NLS
						value = stream.fillInRequestData(value, format);
						if ("null".equalsIgnoreCase(value)) {
							setNullParameter(statement, pIdx, Types.DOUBLE, "DOUBLE"); // NON-NLS
						} else {
							double dValue = Double.parseDouble(value);
							statement.setDouble(pIdx, dValue);
							LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
									"JDBCStream.set.query.parameter", pIdx, value, "DOUBLE"); // NON-NLS
						}
						break;
					case "DATE": // NON-NLS
						value = stream.fillInRequestData(value,
								StringUtils.isEmpty(format) ? DEFAULT_DATE_PATTERN : format);
						if ("null".equalsIgnoreCase(value)) {
							setNullParameter(statement, pIdx, Types.DATE, type.toUpperCase());
						} else {
							Date dtValue = Date.valueOf(value);
							statement.setDate(pIdx, dtValue);
							LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
									"JDBCStream.set.query.parameter", pIdx, value, type.toUpperCase());
						}
						break;
					case "TIME": // NON-NLS
						value = stream.fillInRequestData(value,
								StringUtils.isEmpty(format) ? DEFAULT_TIME_PATTERN : format);
						if ("null".equalsIgnoreCase(value)) {
							setNullParameter(statement, pIdx, Types.TIME, type.toUpperCase());
						} else {
							Time tValue = Time.valueOf(value);
							statement.setTime(pIdx, tValue);
							LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
									"JDBCStream.set.query.parameter", pIdx, value, type.toUpperCase());
						}
						break;
					case "TIMESTAMP": // NON-NLS
					case "DATETIME": // NON-NLS
						value = stream.fillInRequestData(value,
								StringUtils.isEmpty(format) ? DEFAULT_TIMESTAMP_PATTERN : format);
						if ("null".equalsIgnoreCase(value)) {
							setNullParameter(statement, pIdx, Types.TIMESTAMP, type.toUpperCase());
						} else {
							Timestamp tsValue = Timestamp.valueOf(value);
							statement.setTimestamp(pIdx, tsValue);
							LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
									"JDBCStream.set.query.parameter", pIdx, value, type.toUpperCase());
						}
						break;
					case "BOOLEAN": // NON-NLS
						value = stream.fillInRequestData(value, format);
						if ("null".equalsIgnoreCase(value)) {
							setNullParameter(statement, pIdx, Types.BOOLEAN, type.toUpperCase());
						} else {
							boolean bValue = Boolean.parseBoolean(value);
							statement.setBoolean(pIdx, bValue);
							LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
									"JDBCStream.set.query.parameter", pIdx, value, type.toUpperCase());
						}
						break;
					case "BINARY": // NON-NLS
						value = stream.fillInRequestData(value, format);
						if ("null".equalsIgnoreCase(value)) {
							setNullParameter(statement, pIdx, Types.BINARY, type.toUpperCase());
						} else {
							byte[] baValue = Utils.decodeHex(value);
							statement.setBytes(pIdx, baValue);
							LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
									"JDBCStream.set.query.parameter", pIdx, value, type.toUpperCase());
						}
						break;
					case "VARCHAR": // NON-NLS
					default:
						value = stream.fillInRequestData(value, format);
						if ("null".equalsIgnoreCase(value)) {
							setNullParameter(statement, pIdx, Types.VARCHAR, "VARCHAR"); // NON-NLS
						} else {
							statement.setString(pIdx, value);
							LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
									"JDBCStream.set.query.parameter", pIdx, value, "VARCHAR"); // NON-NLS
						}
						break;
					}
				} catch (SQLException exc) {
					throw exc;
				} catch (Throwable exc) {
					throw new SQLException(StreamsResources.getStringFormatted(WsStreamConstants.RESOURCE_BUNDLE_NAME,
							"JDBCStream.failed.to.set.query.parameter", param.getValue()), exc);
				}
			}
		}
	}

	private static void setNullParameter(PreparedStatement statement, int pIdx, int type, String typeName)
			throws SQLException {
		statement.setNull(pIdx, type);
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
				"JDBCStream.set.query.parameter.null", pIdx, typeName);
	}

	/**
	 * Scheduler job to execute JDBC call.
	 */
	public static class JdbcCallJob extends CallJob {

		/**
		 * Constructs a new JdbcCallJob.
		 */
		public JdbcCallJob() {
		}

		@Override
		public void executeCalls(JobDataMap dataMap) {
			JDBCStream stream = (JDBCStream) dataMap.get(JOB_PROP_STREAM_KEY);
			WsScenarioStep scenarioStep = (WsScenarioStep) dataMap.get(JOB_PROP_SCENARIO_STEP_KEY);

			if (!scenarioStep.isEmpty()) {
				String dbQuery;
				ResultSet respRs;
				int stepIdx = 0;
				Semaphore acquiredSemaphore = null;
				for (WsRequest<String> request : scenarioStep.getRequests()) {
					dbQuery = null;
					respRs = null;
					try {
						acquiredSemaphore = acquireSemaphore(stream, request);
						dbQuery = stream.fillInRequestData(request.getData());
						request.setSentData(dbQuery);
						respRs = executeJdbcCall(scenarioStep.getUrlStr(), scenarioStep.getUsername(),
								decPassword(scenarioStep.getPassword()), dbQuery, request.getParameters(), stream);
					} catch (Throwable exc) {
						Utils.logThrowable(stream.logger(), OpLevel.ERROR,
								StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
								"JDBCStream.execute.exception", exc);
					} finally {
						if (respRs != null) {
							WsResponse<ResultSet> resp = new WsReqResponse<>(respRs, request);
							resp.addParameter(new WsRequest.Parameter(QUERY_NAME_PROP,
									(stepIdx++) + ":" + scenarioStep.getName())); // NON-NLS
							stream.addInputToBuffer(resp);
						} else {
							releaseSemaphore(acquiredSemaphore, stream, scenarioStep.getName(), request);
						}
					}
				}
			}
		}
	}
}
