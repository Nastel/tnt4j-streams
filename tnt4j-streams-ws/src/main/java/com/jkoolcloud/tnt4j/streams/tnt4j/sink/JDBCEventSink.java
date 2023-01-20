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

package com.jkoolcloud.tnt4j.streams.tnt4j.sink;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import com.jkoolcloud.tnt4j.core.KeyValueStats;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.format.EventFormatter;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.sink.LoggedEventSink;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.SecurityUtils;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * This class implements {@link EventSink} with JDBC and HikariCP as the underlying sink implementation.
 *
 * @version $Revision: 3 $
 * 
 * @see com.jkoolcloud.tnt4j.streams.tnt4j.sink.JDBCEventSinkFactory
 * @see com.jkoolcloud.tnt4j.streams.tnt4j.format.SQLFormatter
 */
public class JDBCEventSink extends LoggedEventSink {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(JDBCEventSink.class);

	private String url = null;
	private String user = null;
	private String passwd = null;
	private int batchSize = 10;
	private long batchPeriod = TimeUnit.SECONDS.toMillis(30);
	private Properties cpProperties = new Properties();

	private HikariDataSource dbDataSource;
	private java.util.Timer batchTimer;
	private BatchTask batchTask;
	private final List<String> batch = new ArrayList<>();

	/**
	 * Constructs a new JDBC event sink with a given name, event formatter and logger sink instance.
	 *
	 * @param nm
	 *            event sink name
	 * @param props
	 *            additional configuration properties (e.g. {@link System#getProperties()})
	 * @param fmt
	 *            event formatter instance
	 * @param sink
	 *            logger sink instance
	 */
	public JDBCEventSink(String nm, Properties props, EventFormatter fmt, EventSink sink) {
		super(nm, fmt, sink);
	}

	/**
	 * Sets JDBC db connection URL.
	 *
	 * @param url
	 *            the JDBC db connection url string
	 * @return instance of this sink
	 */
	public JDBCEventSink setUrl(String url) {
		this.url = url;

		return this;
	}

	/**
	 * Sets db user name.
	 *
	 * @param user
	 *            the db user name
	 * @return instance of this sink
	 */
	public JDBCEventSink setUser(String user) {
		this.user = user;

		return this;
	}

	/**
	 * Sets db user password.
	 *
	 * @param passwd
	 *            the db user passwd
	 * @return instance of this sink
	 */
	public JDBCEventSink setPassword(String passwd) {
		this.passwd = passwd;

		return this;
	}

	/**
	 * Sets upsert queries batch size.
	 *
	 * @param batchSize
	 *            the upsert queries batch size
	 * @return instance of this sink
	 */
	public JDBCEventSink setBatchSize(int batchSize) {
		this.batchSize = batchSize;

		return this;
	}

	/**
	 * Sets batch upsert execution period in seconds. Value {@code <=0} turns off timed batch processing.
	 *
	 * @param batchPeriod
	 *            the batch upsert execution period
	 * @return instance of this sink
	 */
	public JDBCEventSink setBatchPeriod(int batchPeriod) {
		this.batchPeriod = batchPeriod > 0 ? TimeUnit.SECONDS.toMillis(batchPeriod) : batchPeriod;

		return this;
	}

	/**
	 * Sets HikariCP configuration properties.
	 *
	 * @param cpConfig
	 *            HikariCP configuration properties
	 * @return instance of this sink
	 */
	public JDBCEventSink setCPConfig(Properties cpConfig) {
		cpProperties.clear();

		if (cpConfig != null) {
			cpProperties.putAll(cpConfig);
		}

		return this;
	}

	@Override
	protected void _open() throws IOException {
		try {
			if (isOpen()) {
				_close();
			}
			setErrorState(null);

			LOGGER.log(OpLevel.DEBUG, "Open sink ''{0}'' DB data source url={1}, user={2}, pass={3}", getName(), url,
					user, passwd == null ? null : "xxxxxx"); // NON-NLS

			HikariConfig dbConfig = new HikariConfig(cpProperties);
			dbConfig.setJdbcUrl(url);
			dbConfig.setUsername(user);
			dbConfig.setPassword(SecurityUtils.getPass2(passwd));

			dbDataSource = new HikariDataSource(dbConfig);

			if (batchPeriod > 0) {
				batchTimer = new java.util.Timer("JDBCSinkBatchProcessingTimer", true);
			}

			super._open();
		} catch (Throwable e) {
			LOGGER.log(OpLevel.ERROR, "Failed to open sink name={0}, url={1}, user={2}, pass={3}", getName(), url, user,
					passwd == null ? null : "xxxxxx", e);
			_close();

			if (e instanceof IOException) {
				throw (IOException) e;
			} else {
				throw new IOException(e.getMessage(), e);
			}
		}
	}

	@Override
	protected void _close() throws IOException {
		if (batchTimer != null) {
			batchTimer.cancel();
			batchTimer = null;
		}

		if (dbDataSource != null) {
			LOGGER.log(OpLevel.DEBUG, "Triggering upsert queries batch execution on sink close");
			processBatch();

			LOGGER.log(OpLevel.DEBUG, "Closing sink ''{0}'' DB data source url={1}, user={2}, pass={3}", getName(), url,
					user, passwd == null ? null : "xxxxxx"); // NON-NLS
			Utils.close(dbDataSource);
		}

		super._close();
	}

	@Override
	public String toString() {
		return super.toString() //
				+ "{url: " + url // NON-NLS
				+ ", user: " + user // NON-NLS
				+ ", pass: " + (passwd == null ? null : "xxxxxx") // NON-NLS
				+ ", batchSize: " + batchSize // NON-NLS
				+ ", batchPeriod: " + batchPeriod // NON-NLS
				+ ", handle: " + dbDataSource // NON-NLS
				+ "}"; // NON-NLS
	}

	@Override
	protected void writeLine(String sql) throws IOException {
		synchronized (batch) {
			batch.add(sql);
			armTimerTask();
		}
		checkBatchSize();
	}

	@Override
	public Object getSinkHandle() {
		return dbDataSource;
	}

	@Override
	public boolean isOpen() {
		return dbDataSource != null && !dbDataSource.isClosed();
	}

	@Override
	public KeyValueStats getStats(Map<String, Object> stats) { // TODO: improve
		super.getStats(stats);

		if (isOpen()) {
			MetricRegistry mRegistry = (MetricRegistry) dbDataSource.getMetricRegistry();
			if (mRegistry != null) {
				for (Map.Entry<String, Gauge> cpMetric : mRegistry.getGauges().entrySet()) {
					stats.put(Utils.qualify(this, cpMetric.getKey()), cpMetric.getValue().getValue());
				}
				for (Map.Entry<String, Counter> cpMetric : mRegistry.getCounters().entrySet()) {
					stats.put(Utils.qualify(this, cpMetric.getKey()), cpMetric.getValue().getCount());
				}
				for (Map.Entry<String, Histogram> cpMetric : mRegistry.getHistograms().entrySet()) {
					stats.put(Utils.qualify(this, cpMetric.getKey()), cpMetric.getValue().getSnapshot().getMean());
				}
				for (Map.Entry<String, Timer> cpMetric : mRegistry.getTimers().entrySet()) {
					stats.put(Utils.qualify(this, cpMetric.getKey()), cpMetric.getValue().getSnapshot().getMean());
				}
				for (Map.Entry<String, Meter> cpMetric : mRegistry.getMeters().entrySet()) {
					stats.put(Utils.qualify(this, cpMetric.getKey()), cpMetric.getValue().getMeanRate());
				}
			}
		}

		return this;
	}

	private void checkBatchSize() {
		synchronized (batch) {
			if (isBatchSizeHit()) {
				cancelTimerTask();
				LOGGER.log(OpLevel.DEBUG, "Triggering upsert queries batch execution on bath max. size hit");
				processBatch();
				if (!batch.isEmpty()) {
					armTimerTask();
				}
			}
		}
	}

	private void triggerBatchPeriod(String taskId) {
		synchronized (batch) {
			LOGGER.log(OpLevel.DEBUG, "Triggering upsert queries batch execution from timer task ''{0}''", taskId);
			processBatch();
			batchTask = null;
			if (!batch.isEmpty()) {
				armTimerTask();
			}
		}
	}

	private boolean isBatchSizeHit() {
		return batch.size() >= batchSize;
	}

	/**
	 * Performs JDBC update/insert (upsert) queries batch processing.
	 */
	protected void processBatch() {
		synchronized (batch) {
			if (batch.isEmpty()) {
				LOGGER.log(OpLevel.INFO, "Noting to upsert, batch is empty.");
				return;
			}

			LOGGER.log(OpLevel.DEBUG, "Will send {0} upsert queries to database", batch.size());
			Connection dbConn = null;
			try {
				dbConn = dbDataSource.getConnection();
				boolean useBatch = dbConn.getMetaData().supportsBatchUpdates();
				int qCount;
				if (useBatch) {
					qCount = batchQueries(dbConn, batch);
				} else {
					qCount = executeQueries(dbConn, batch);
				}
				LOGGER.log(OpLevel.INFO, "Sent {0} upsert queries to database", qCount);
			} catch (SQLException exc) {
				Utils.logThrowable(LOGGER, OpLevel.ERROR, "Failed to obtain DB connection or metadata: {0}", exc);
			} finally {
				Utils.close(dbConn);
			}
		}
	}

	private int batchQueries(Connection dbConn, List<String> batch) {
		Statement dbSt = null;
		int qCount = 0;
		try {
			dbSt = dbConn.createStatement(); // TODO: prepared statements batching
			for (String sql : batch) {
				dbSt.addBatch(sql);
				incrementBytesSent(sql.length());
			}
			dbSt.executeBatch();
			if (!dbConn.getAutoCommit()) {
				dbConn.commit();
			}
			qCount = batch.size();
			batch.clear();
		} catch (SQLException exc) {
			Utils.logThrowable(LOGGER, OpLevel.ERROR, "Failed to process whole upsert batch: {0}", exc);
		} finally {
			Utils.close(dbSt);
		}

		return qCount;
	}

	private int executeQueries(Connection dbConn, List<String> batch) {
		Statement dbSt = null;
		int qCount = 0;
		for (int i = 0; i < batch.size(); i++) {
			try {
				String sql = batch.get(i);
				dbSt = dbConn.createStatement();
				dbSt.executeUpdate(sql);
				incrementBytesSent(sql.length());
				if (!dbConn.getAutoCommit()) {
					dbConn.commit();
				}
				batch.remove(i--);
				qCount++;
			} catch (SQLException exc) {
				Utils.logThrowable(LOGGER, OpLevel.ERROR, "Failed to process batch upsert query: {0}", exc);
			} finally {
				Utils.close(dbSt);
			}
		}

		return qCount;
	}

	private void armTimerTask() {
		if (batchTimer != null) {
			if (batchTask == null) {
				batchTimer.purge();
				batchTimer.schedule(batchTask = new BatchTask(Thread.currentThread().getName(), this), batchPeriod);
				LOGGER.log(OpLevel.DEBUG, "Armed upsert queries batch timer task ''{0}'' on batch sized {1}",
						batchTask.getId(), batch.size());
			}
		}
	}

	private void cancelTimerTask() {
		if (batchTask != null) {
			batchTask.cancel();
			LOGGER.log(OpLevel.DEBUG, "Canceled upsert queries batch timer task ''{0}'' on batch sized {1}",
					batchTask.getId(), batch.size());
			batchTask = null;
		}
	}

	private static class BatchTask extends TimerTask {
		private final String id;
		private final JDBCEventSink sink;

		BatchTask(String id, JDBCEventSink sink) {
			this.id = id;
			this.sink = sink;
		}

		String getId() {
			return id;
		}

		@Override
		public void run() {
			sink.triggerBatchPeriod(id);
		}
	}
}
