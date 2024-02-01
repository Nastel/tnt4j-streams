/*
 * Copyright 2014-2023 JKOOL, LLC.
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
 * <li>QueryFetchRows - number of rows to be fetched from database per query returned {@link java.sql.ResultSet} cursor
 * access. Value {@code 0} implies to use default JDBC setting. See {@link java.sql.Statement#setFetchSize(int)} for
 * details. Default value - {@code 0}. (Optional)</li>
 * <li>QueryMaxRows - limit for the maximum number of rows that query returned {@link java.sql.ResultSet} can contain.
 * Value {@code 0} implies to use default JDBC setting. See {@link java.sql.Statement#setMaxRows(int)} for details.
 * Default value - {@code 0}. (Optional)</li>
 * <li>set of <a href="https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby">HikariCP supported
 * properties</a> used to configure JDBC data source. (Optional)</li>
 * <li>set of JDBC or driver vendor specified {@link javax.sql.DataSource} configuration properties prefixed by
 * {@value #DS_PROP_PREFIX}. (Optional)</li>
 * <li>when {@value com.jkoolcloud.tnt4j.streams.configure.StreamProperties#PROP_USE_EXECUTOR_SERVICE} is set to
 * {@code true} and {@value com.jkoolcloud.tnt4j.streams.configure.StreamProperties#PROP_EXECUTOR_THREADS_QTY} is
 * greater than {@code 1}, value for that property is reset to {@code 1} since {@link java.sql.ResultSet} can't be
 * accessed in multi-thread manner.</li>
 * </ul>
 *
 * @version $Revision: 5 $
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 * @see java.sql.DriverManager#getConnection(String, java.util.Properties)
 * @see java.sql.Connection#prepareStatement(String)
 * @see java.sql.PreparedStatement#executeQuery()
 */
public class JDBCStream extends AbstractWsStream<String, ResultSet> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(JDBCStream.class);

	private static final String ROW_PROP = ".Rs.Row."; // NON-NLS
	/**
	 * Name prefix for JDBC or driver vendor specified {@link javax.sql.DataSource} configuration properties.
	 */
	protected static final String DS_PROP_PREFIX = "jdbc."; // NON-NLS

	private static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd"; // NON-NLS
	private static final String DEFAULT_TIME_PATTERN = "HH:mm:ss"; // NON-NLS
	private static final String DEFAULT_TIMESTAMP_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS"; // NON-NLS

	/**
	 * Contains HikariCP configuration properties used to configure JDBC data source.
	 */
	protected Map<String, String> cpProperties = new HashMap<>();
	/**
	 * Contains JDBC or driver vendor specified {@link javax.sql.DataSource} configuration properties.
	 */
	protected Map<String, String> dsProperties = new HashMap<>();

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

		if (WsStreamProperties.PROP_QUERY_FETCH_ROWS.equalsIgnoreCase(name)) {
			fetchSize = Integer.parseInt(value);
		} else if (WsStreamProperties.PROP_QUERY_MAX_ROWS.equalsIgnoreCase(name)) {
			maxRows = Integer.parseInt(value);
		} else if (!StreamsConstants.isStreamCfgProperty(name, WsStreamProperties.class)) {
			if (CustomProperties.isPrefixedPropertyName(name, DS_PROP_PREFIX)) {
				dsProperties.put(name, decPassword(value));
			} else {
				cpProperties.put(name, decPassword(value));
			}
		}
	}

	@Override
	public Object getProperty(String name) {
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

		pValue = cpProperties.get(name);
		if (pValue != null) {
			return pValue;
		}

		pValue = dsProperties.get(name);
		if (pValue != null) {
			return pValue;
		}

		return dsProperties.get(name.substring(DS_PROP_PREFIX.length()));
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
	protected long getActivityItemByteSize(WsResponse<String, ResultSet> item) {
		return 1; // TODO
	}

	@Override
	protected JobDetail buildJob(String group, String jobId, JobDataMap jobAttrs) {
		return JobBuilder.newJob(JdbcCallJob.class).withIdentity(jobId, group).usingJobData(jobAttrs).build();
	}

	@Override
	protected boolean isResponseConsumed(WsResponse<String, ResultSet> item) {
		ResultSet rs = item.getData();
		try {
			if (rs.isClosed() || !rs.next()) {
				logger().log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
						"JDBCStream.rs.consumption.done", item.getOriginalRequest().getId(),
						item.getParameterValue(ROW_PROP));
				item.getParameters().remove(ROW_PROP);

				return true;
			}

			WsRequest.Parameter param = item.getParameter(ROW_PROP);
			if (param == null) {
				param = new WsRequest.Parameter(ROW_PROP, 0, true);
				item.addParameter(param);
			}

			int rowNumber = rs.getRow();
			param.setValue(rowNumber);
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"JDBCStream.rs.consumption.marker.new", item.getOriginalRequest().getId(), rowNumber);

			return false;
		} catch (SQLException exc) {
			Utils.logThrowable(logger(), OpLevel.WARNING,
					StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"JDBCStream.rs.consumption.exception", item.getOriginalRequest().getId(), exc);

			return true;
		}
	}

	@Override
	protected void closeResponse(ResultSet rs) {
		try (Statement st = rs.getStatement(); Connection conn = st == null ? null : st.getConnection()) {
			if (conn != null && !conn.getAutoCommit()) {
				conn.commit();
			}
		} catch (SQLException exc) {
		} finally {
			Utils.close(rs);
		}
	}

	@Override
	protected boolean initItemForParsing(WsResponse<String, ResultSet> item) {
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
	 * @param dbRequest
	 *            DB request defining query, parameters and additional metadata
	 * @return JDBC call returned result set {@link java.sql.ResultSet}
	 * 
	 * @throws SQLException
	 *             if exception occurs while performing JDBC call
	 */
	protected ResultSet executeJdbcCall(String url, String user, String pass, WsRequest<String> dbRequest)
			throws SQLException {
		if (StringUtils.isEmpty(url)) {
			LOGGER.log(OpLevel.WARNING, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"JDBCStream.db.conn.not.defined", dbRequest.getId());
			return null;
		}

		String query = dbRequest.getData();

		if (StringUtils.isEmpty(query)) {
			LOGGER.log(OpLevel.WARNING, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"JDBCStream.query.not.defined", dbRequest.getId(), query);
			return null;
		}

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
				"JDBCStream.obtaining.db.connection", url);

		Connection dbConn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;

		try {
			Duration cod = Duration.arm();
			dbConn = getDbConnection(url, user, pass);
			LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"JDBCStream.db.connection.obtained", url, cod.durationHMS());

			LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"JDBCStream.preparing.query", dbRequest.getId(), query);

			statement = dbConn.prepareStatement(query); // ResultSet.TYPE_SCROLL_INSENSITIVE,
														// ResultSet.CONCUR_READ_ONLY);
			if (fetchSize > 0) {
				statement.setFetchSize(fetchSize);
			}
			if (maxRows > 0) {
				statement.setMaxRows(maxRows);
			}

			addStatementParameters(statement, dbRequest);

			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"JDBCStream.executing.query", dbRequest.getId(), url);
			Duration qed = Duration.arm();
			rs = statement.executeQuery();

			LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"JDBCStream.query.execution.completed", url, dbRequest.getId(), qed.durationHMS(),
					cod.durationHMS());

			return rs;
		} catch (SQLException exc) {
			Utils.close(rs);
			Utils.close(statement);
			Utils.close(dbConn);

			throw exc;
		}
	}

	/**
	 * Obtains database connection for provided connection URL and user credentials.
	 * <p>
	 * Set of HikariCP configuration properties are picked from {@link #cpProperties} map. Additional set of JDBC or
	 * driver vendor specified {@link javax.sql.DataSource} properties are from {@link #dsProperties} map.
	 * 
	 * @param url
	 *            DB connection URL
	 * @param user
	 *            DB user name
	 * @param pass
	 *            DB user password
	 * @return DB connection instance to be used to execute queries
	 * 
	 * @throws SQLException
	 *             if data source fails to obtain database connection
	 */
	protected Connection getDbConnection(String url, String user, String pass) throws SQLException {
		DataSource hds = dbDataSources.get(url);

		if (hds == null) {
			Properties cpProps = new Properties();
			cpProps.putAll(cpProperties);

			HikariConfig dbConfig = new HikariConfig(cpProps);
			dbConfig.setJdbcUrl(url);
			dbConfig.setUsername(user);
			dbConfig.setPassword(pass);

			for (Map.Entry<String, String> dsProp : dsProperties.entrySet()) {
				dbConfig.addDataSourceProperty(dsProp.getKey().substring(DS_PROP_PREFIX.length()), dsProp.getValue());
			}

			hds = new HikariDataSource(dbConfig);

			dbDataSources.put(url, hds);
		}

		return hds.getConnection();
	}

	/**
	 * Sets prepared SQL statement parameters provided by {@code params} map.
	 *
	 * @param statement
	 *            prepared SQL statement parameters to set
	 * @param dbRequest
	 *            DB request defining query parameters and additional metadata
	 * 
	 * @throws SQLException
	 *             if exception occurs while setting prepared statement parameter
	 */
	protected void addStatementParameters(PreparedStatement statement, WsRequest<String> dbRequest)
			throws SQLException {
		Map<String, WsRequest.Parameter> params = dbRequest.getParameters();
		if (params != null) {
			for (Map.Entry<String, WsRequest.Parameter> param : params.entrySet()) {
				try {
					int pIdx = Integer.parseInt(param.getValue().getId());
					String type = param.getValue().getAttribute(WsRequest.Parameter.ATTR_TYPE);
					String value = param.getValue().getStringValue();
					String format = param.getValue().getAttribute(WsRequest.Parameter.ATTR_FORMAT);
					String timeZone = param.getValue().getAttribute(WsRequest.Parameter.ATTR_TIMEZONE);

					if (type == null) {
						type = "";
					}

					switch (type.toUpperCase()) {
					case "INTEGER": // NON-NLS
						value = fillInRequestData(value, format);
						if (!param.getValue().isTransient()) {
							if (isNullValue(value)) {
								setNullParameter(statement, dbRequest.getId(), pIdx, Types.INTEGER, type.toUpperCase());
							} else {
								int iValue = Integer.parseInt(value);
								statement.setInt(pIdx, iValue);
								LOGGER.log(OpLevel.INFO,
										StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
										"JDBCStream.set.query.parameter", dbRequest.getId(), pIdx, value,
										type.toUpperCase());
							}
						}
						break;
					case "BIGINT": // NON-NLS
						value = fillInRequestData(value, format);
						if (!param.getValue().isTransient()) {
							if (isNullValue(value)) {
								setNullParameter(statement, dbRequest.getId(), pIdx, Types.BIGINT, type.toUpperCase());
							} else {
								long lValue = Long.parseLong(value);
								statement.setLong(pIdx, lValue);
								LOGGER.log(OpLevel.INFO,
										StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
										"JDBCStream.set.query.parameter", dbRequest.getId(), pIdx, value,
										type.toUpperCase());
							}
						}
						break;
					case "FLOAT": // NON-NLS
						value = fillInRequestData(value, format);
						if (!param.getValue().isTransient()) {
							if (isNullValue(value)) {
								setNullParameter(statement, dbRequest.getId(), pIdx, Types.FLOAT, type.toUpperCase());
							} else {
								float fValue = Float.parseFloat(value);
								statement.setFloat(pIdx, fValue);
								LOGGER.log(OpLevel.INFO,
										StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
										"JDBCStream.set.query.parameter", dbRequest.getId(), pIdx, value,
										type.toUpperCase());
							}
						}
						break;
					case "DOUBLE": // NON-NLS
					case "REAL": // NON-NLS
					case "DECIMAL": // NON-NLS
						value = fillInRequestData(value, format);
						if (!param.getValue().isTransient()) {
							if (isNullValue(value)) {
								setNullParameter(statement, dbRequest.getId(), pIdx, Types.DOUBLE, "DOUBLE"); // NON-NLS
							} else {
								double dValue = Double.parseDouble(value);
								statement.setDouble(pIdx, dValue);
								LOGGER.log(OpLevel.INFO,
										StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
										"JDBCStream.set.query.parameter", dbRequest.getId(), pIdx, value, "DOUBLE"); // NON-NLS
							}
						}
						break;
					case "DATE": // NON-NLS
						value = fillInRequestData(value, StringUtils.isEmpty(format) ? DEFAULT_DATE_PATTERN : format,
								timeZone);
						if (!param.getValue().isTransient()) {
							if (isNullValue(value)) {
								setNullParameter(statement, dbRequest.getId(), pIdx, Types.DATE, type.toUpperCase());
							} else {
								Date dtValue = Date.valueOf(value);
								statement.setDate(pIdx, dtValue);
								LOGGER.log(OpLevel.INFO,
										StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
										"JDBCStream.set.query.parameter", dbRequest.getId(), pIdx, value,
										type.toUpperCase());
							}
						}
						break;
					case "TIME": // NON-NLS
						value = fillInRequestData(value, StringUtils.isEmpty(format) ? DEFAULT_TIME_PATTERN : format,
								timeZone);
						if (!param.getValue().isTransient()) {
							if (isNullValue(value)) {
								setNullParameter(statement, dbRequest.getId(), pIdx, Types.TIME, type.toUpperCase());
							} else {
								Time tValue = Time.valueOf(value);
								statement.setTime(pIdx, tValue);
								LOGGER.log(OpLevel.INFO,
										StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
										"JDBCStream.set.query.parameter", dbRequest.getId(), pIdx, value,
										type.toUpperCase());
							}
						}
						break;
					case "TIMESTAMP": // NON-NLS
					case "DATETIME": // NON-NLS
						value = fillInRequestData(value,
								StringUtils.isEmpty(format) ? DEFAULT_TIMESTAMP_PATTERN : format, timeZone);
						if (!param.getValue().isTransient()) {
							if (isNullValue(value)) {
								setNullParameter(statement, dbRequest.getId(), pIdx, Types.TIMESTAMP,
										type.toUpperCase());
							} else {
								Timestamp tsValue = Timestamp.valueOf(value);
								statement.setTimestamp(pIdx, tsValue);
								LOGGER.log(OpLevel.INFO,
										StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
										"JDBCStream.set.query.parameter", dbRequest.getId(), pIdx, value,
										type.toUpperCase());
							}
						}
						break;
					case "BOOLEAN": // NON-NLS
						value = fillInRequestData(value, format);
						if (!param.getValue().isTransient()) {
							if (isNullValue(value)) {
								setNullParameter(statement, dbRequest.getId(), pIdx, Types.BOOLEAN, type.toUpperCase());
							} else {
								boolean bValue = Boolean.parseBoolean(value);
								statement.setBoolean(pIdx, bValue);
								LOGGER.log(OpLevel.INFO,
										StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
										"JDBCStream.set.query.parameter", dbRequest.getId(), pIdx, value,
										type.toUpperCase());
							}
						}
						break;
					case "BINARY": // NON-NLS
						value = fillInRequestData(value, format);
						if (!param.getValue().isTransient()) {
							if (isNullValue(value)) {
								setNullParameter(statement, dbRequest.getId(), pIdx, Types.BINARY, type.toUpperCase());
							} else {
								byte[] baValue = Utils.decodeHex(value);
								statement.setBytes(pIdx, baValue);
								LOGGER.log(OpLevel.INFO,
										StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
										"JDBCStream.set.query.parameter", dbRequest.getId(), pIdx, value,
										type.toUpperCase());
							}
						}
						break;
					case "VARCHAR": // NON-NLS
					default:
						value = fillInRequestData(value, format);
						if (!param.getValue().isTransient()) {
							if (isNullValue(value)) {
								setNullParameter(statement, dbRequest.getId(), pIdx, Types.VARCHAR, "VARCHAR"); // NON-NLS
							} else {
								statement.setString(pIdx, value);
								LOGGER.log(OpLevel.INFO,
										StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
										"JDBCStream.set.query.parameter", dbRequest.getId(), pIdx, value, "VARCHAR"); // NON-NLS
							}
						}
						break;
					}

					if (param.getValue().isTransient()) {
						param.getValue().setValue(value);
					}
				} catch (SQLException exc) {
					throw exc;
				} catch (Throwable exc) {
					throw new SQLException(
							StreamsResources.getStringFormatted(WsStreamConstants.RESOURCE_BUNDLE_NAME,
									"JDBCStream.failed.to.set.query.parameter", dbRequest.getId(), param.getValue()),
							exc);
				}
			}
		}
	}

	private static boolean isNullValue(String value) {
		return "null".equalsIgnoreCase(value); // NON-NLS
	}

	private static void setNullParameter(PreparedStatement statement, String reqId, int pIdx, int type, String typeName)
			throws SQLException {
		statement.setNull(pIdx, type);
		LOGGER.log(OpLevel.INFO, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
				"JDBCStream.set.query.parameter.null", reqId, pIdx, typeName);
	}

	@Override
	protected boolean isDirectRequestUse() {
		return false;
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
				ResultSet respRs;
				Semaphore acquiredSemaphore;
				WsRequest<String> processedRequest;
				for (WsRequest<String> request : scenarioStep.requestsArray()) {
					if (stream.isShotDown()) {
						return;
					}

					if (stream.isDropRecurring(request)) {
						continue;
					}

					respRs = null;
					acquiredSemaphore = null;
					processedRequest = null;
					try {
						acquiredSemaphore = stream.acquireSemaphore(request);
						processedRequest = stream.fillInRequest(request);
						respRs = stream.executeJdbcCall(scenarioStep.getUrlStr(), scenarioStep.getUsername(),
								decPassword(scenarioStep.getPassword()), processedRequest);
					} catch (VoidRequestException exc) {
						stream.logger().log(OpLevel.INFO,
								StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
								"AbstractWsStream.void.request", request.getId(), exc.getMessage());
					} catch (Throwable exc) {
						Utils.logThrowable(stream.logger(), OpLevel.ERROR,
								StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
								"JDBCStream.execute.exception", stream.getName(), processedRequest.getId(), exc);
					} finally {
						if (respRs != null) {
							stream.addInputToBuffer(new WsResponse<>(respRs, processedRequest));
						} else {
							stream.requestFailed(processedRequest);
							stream.releaseSemaphore(acquiredSemaphore, scenarioStep.getName(), request);
						}
					}
				}
			}
		}
	}
}
