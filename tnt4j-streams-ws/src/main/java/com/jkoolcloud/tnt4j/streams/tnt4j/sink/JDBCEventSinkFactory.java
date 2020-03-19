/*
 * Copyright 2014-2020 JKOOL, LLC.
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

import java.util.Map;
import java.util.Properties;

import com.jkoolcloud.tnt4j.config.ConfigException;
import com.jkoolcloud.tnt4j.format.EventFormatter;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.sink.LoggedEventSinkFactory;
import com.jkoolcloud.tnt4j.streams.tnt4j.format.SQLFormatter;
import com.jkoolcloud.tnt4j.utils.Utils;

/**
 * Concrete implementation of {@link com.jkoolcloud.tnt4j.sink.EventSinkFactory} interface, which creates instances of
 * {@link EventSink}. This factory uses {@link com.jkoolcloud.tnt4j.streams.tnt4j.sink.JDBCEventSink} as the underlying
 * sink provider and by default uses {@link com.jkoolcloud.tnt4j.streams.tnt4j.format.SQLFormatter} to format messages.
 *
 * @version $Revision: 2 $
 * 
 * @see com.jkoolcloud.tnt4j.streams.tnt4j.sink.JDBCEventSink
 * @see com.jkoolcloud.tnt4j.streams.tnt4j.format.SQLFormatter
 */
public class JDBCEventSinkFactory extends LoggedEventSinkFactory {

	private static final String JDBC_CP_CONFIG_PREFIX = "CP."; // NON-NLS

	private String url = null;
	private String user = null;
	private String passwd = null;
	private int batchSize = 10;
	private boolean synchronizedWrites = false;
	private final Properties cpProperties = new Properties();

	private static EventSink sinkInstance;

	/**
	 * Constructs a new JDBC Event Sink factory.
	 */
	public JDBCEventSinkFactory() {
	}

	@Override
	public EventSink getEventSink(String name) {
		return getEventSink(name, System.getProperties());
	}

	@Override
	public EventSink getEventSink(String name, Properties props) {
		return getEventSink(name, props, new SQLFormatter());
	}

	@Override
	public EventSink getEventSink(String name, Properties props, EventFormatter frmt) {
		if (synchronizedWrites) {
			synchronized (cpProperties) {
				if (sinkInstance == null) {
					EventSink outSink = getLogSink(name, props, frmt);
					sinkInstance = configureSink(new JDBCEventSink(name, props, frmt, outSink));
				}

				return sinkInstance;
			}
		} else {
			EventSink outSink = getLogSink(name, props, frmt);
			return configureSink(new JDBCEventSink(name, props, frmt, outSink));
		}
	}

	@Override
	protected EventSink configureSink(EventSink sink) {
		JDBCEventSink jdbcSink = (JDBCEventSink) super.configureSink(sink);
		jdbcSink.setUrl(url).setUser(user).setPassword(passwd).setBatchSize(batchSize).setCPConfig(cpProperties);

		return jdbcSink;
	}

	@Override
	public void setConfiguration(Map<String, ?> settings) throws ConfigException {
		super.setConfiguration(settings);

		url = Utils.getString("Url", settings, url); // NON-NLS
		user = Utils.getString("User", settings, user);// NON-NLS
		passwd = Utils.getString("Passwd", settings, passwd); // NON-NLS
		batchSize = Utils.getInt("BatchSize", settings, batchSize); // NON-NLS
		synchronizedWrites = Utils.getBoolean("SynchronizedWrites", settings, synchronizedWrites); // NON-NLS

		_applyConfig(settings);
	}

	private void _applyConfig(Map<String, ?> settings) throws ConfigException {
		cpProperties.clear();

		for (Map.Entry<String, ?> cme : settings.entrySet()) {
			if (cme.getKey().startsWith(JDBC_CP_CONFIG_PREFIX)) {
				cpProperties.setProperty(cme.getKey().substring(JDBC_CP_CONFIG_PREFIX.length()),
						Utils.toString(cme.getValue()));
			}
		}
	}
}
