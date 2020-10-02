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

package com.jkoolcloud.tnt4j.streams.parsers;

import java.sql.*;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.WsParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldDataType;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType;
import com.jkoolcloud.tnt4j.streams.utils.*;

/**
 * Implements an activity data parser that assumes each activity data item is a {@link java.sql.ResultSet} row, where
 * each field is represented by row column and the column name/index is used to map each column into its corresponding
 * activity field.
 * <p>
 * This parser supports the following configuration properties (in addition to those supported by
 * {@link GenericActivityParser}):
 * <ul>
 * <li>SQLJavaMapping - defines mapping from SQL type name (as {@link String}) to class (as
 * {@link Class#forName(String)}) names in the Java programming language, e.g. {@code "NUMBER=java.lang.String"}. Parser
 * can have multiple definitions og this property. It is useful when default JDBC driver mapping produces inaccurate
 * result. IMPORTANT: if JDBC driver does not support {@link java.sql.ResultSet#getObject(int, java.util.Map)} or
 * {@link java.sql.ResultSet#getObject(String, java.util.Map)} implementation, leave SQL-Java types map empty!
 * (Optional)</li>
 * </ul>
 * <p>
 * This activity parser supports those activity field locator types:
 * <ul>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Index}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Label}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#StreamProp}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Cache}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Activity}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Expression}</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class ActivityJDBCResultSetParser extends GenericActivityParser<ResultSet> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(ActivityJDBCResultSetParser.class);

	private Map<String, Class<?>> typesMap = new HashMap<>();

	/**
	 * Constructs a new ActivityJDBCResultSetParser.
	 */
	public ActivityJDBCResultSetParser() {
		super(ActivityFieldDataType.AsInput);
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (WsParserProperties.PROP_SQL_JAVA_MAPPING.equalsIgnoreCase(name)) {
			if (StringUtils.isNotEmpty(value)) {
				String[] sqlJavaMappings = StreamsConstants.getMultiProperties(value);
				for (String sqlJavaMapping : sqlJavaMappings) {
					String[] nsFields = sqlJavaMapping.split("="); // NON-NLS
					try {
						typesMap.put(nsFields[0], Class.forName(nsFields[1]));
						logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
								"ActivityJDBCResultSetParser.adding.mapping", name, sqlJavaMapping);
					} catch (Throwable exc) {
						logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
								"ActivityJDBCResultSetParser.resolve.class.failed", nsFields[1],
								exc.getLocalizedMessage());
					}
				}
			}
		}
	}

	@Override
	public Object getProperty(String name) {
		if (WsParserProperties.PROP_SQL_JAVA_MAPPING.equalsIgnoreCase(name)) {
			return typesMap;
		}

		return super.getProperty(name);
	}

	/**
	 * Returns whether this parser supports the given format of the activity data. This is used by activity streams to
	 * determine if the parser can parse the data in the format that the stream has it.
	 * <p>
	 * This parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link java.sql.ResultSet}</li>
	 * </ul>
	 *
	 * @param data
	 *            data object whose class is to be verified
	 * @return {@code true} if this parser can process data in the specified format, {@code false} - otherwise
	 */
	@Override
	protected boolean isDataClassSupportedByParser(Object data) {
		return ResultSet.class.isInstance(data);
	}

	@Override
	protected Object resolveLocatorValue(ActivityFieldLocator locator, ActivityContext cData,
			AtomicBoolean formattingNeeded) throws ParseException {
		Object val = null;
		String locStr = locator.getLocator();
		ResultSet resultSet = cData.getData();
		Map<String, Class<?>> connTypes = null;
		try {
			Statement st = resultSet.getStatement();
			Connection dbConn = st.getConnection();
			connTypes = dbConn.getTypeMap();
		} catch (Throwable exc) {
			Utils.logThrowable(LOGGER, OpLevel.WARNING,
					StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"ActivityJDBCResultSetParser.driver.type.mappings.failed", exc);
		}

		if (StringUtils.isNotEmpty(locStr) && resultSet != null) {
			ActivityFieldDataType dataType = locator.getDataType();
			String tz = locator.getTimeZone();

			try {
				ActivityFieldLocatorType locType = locator.getBuiltInType();

				if (locType != null && locType.getDataType() == Integer.class) {
					val = getByIndex(locStr, resultSet, connTypes, dataType, tz);
				} else if (locType != null && locType.getDataType() == String.class) {
					val = getByName(locStr, resultSet, connTypes, dataType, tz);
				} else {
					try {
						val = getByIndex(locStr, resultSet, connTypes, dataType, tz);
					} catch (NumberFormatException exc) {
						val = getByName(locStr, resultSet, connTypes, dataType, tz);
					}
				}
			} catch (Throwable exc) {
				int row = getRsRow(resultSet);
				ParseException pe = new ParseException(
						StreamsResources.getStringFormatted(WsStreamConstants.RESOURCE_BUNDLE_NAME,
								"ActivityJDBCResultSetParser.sql.exception", row, locStr),
						row);
				pe.initCause(exc);
				throw pe;
			}
		}

		return val;
	}

	private Object getByIndex(String locStr, ResultSet resultSet, Map<String, Class<?>> driverTypes,
			ActivityFieldDataType dataType, String tz) throws SQLException {
		int index = Integer.parseInt(locStr);
		Object objVal = null;
		boolean valueResolved = false;
		if (MapUtils.isNotEmpty(typesMap)) {
			try {
				objVal = resultSet.getObject(index, typesMap);
				valueResolved = true;
			} catch (SQLFeatureNotSupportedException exc) {
				Utils.logThrowable(logger(), OpLevel.WARNING,
						StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
						"ActivityJDBCResultSetParser.value.resolution.using.types.map.failed", index, exc);
			}
		} else {
			if (dataType == ActivityFieldDataType.Timestamp || dataType == ActivityFieldDataType.DateTime) {
				if (StringUtils.isEmpty(tz)) {
					objVal = resultSet.getTimestamp(index);
					valueResolved = true;
				} else {
					objVal = resultSet.getTimestamp(index, Calendar.getInstance(TimeZone.getTimeZone(tz)));
					valueResolved = true;
				}
			}
		}

		if (objVal == null && !valueResolved) {
			objVal = resultSet.getObject(index);
		}

		return getRealValueFromSql(objVal);
	}

	private Object getByName(String locStr, ResultSet resultSet, Map<String, Class<?>> driverTypes,
			ActivityFieldDataType dataType, String tz) throws SQLException {
		Object objVal = null;
		boolean valueResolved = false;
		if (MapUtils.isNotEmpty(typesMap)) {
			try {
				objVal = resultSet.getObject(locStr, typesMap);
				valueResolved = true;
			} catch (SQLFeatureNotSupportedException exc) {
				Utils.logThrowable(logger(), OpLevel.WARNING,
						StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
						"ActivityJDBCResultSetParser.value.resolution.using.types.map.failed", locStr, exc);
			}
		} else {
			if (dataType == ActivityFieldDataType.Timestamp || dataType == ActivityFieldDataType.DateTime) {
				if (StringUtils.isEmpty(tz)) {
					objVal = resultSet.getTimestamp(locStr);
					valueResolved = true;
				} else {
					objVal = resultSet.getTimestamp(locStr, Calendar.getInstance(TimeZone.getTimeZone(tz)));
					valueResolved = true;
				}
			}
		}

		if (objVal == null && !valueResolved) {
			objVal = resultSet.getObject(locStr);
		}

		return getRealValueFromSql(objVal);
	}

	private Object getRealValueFromSql(Object sqlObjVal) {
		try {
			if (sqlObjVal instanceof Blob) {
				Blob blob = (Blob) sqlObjVal;
				return blob.getBytes(1, (int) blob.length());
			} else if (sqlObjVal instanceof Clob) {
				Clob clob = (Clob) sqlObjVal;
				return clob.getSubString(1, (int) clob.length());
			}
		} catch (SQLException exc) {
			Utils.logThrowable(LOGGER, OpLevel.WARNING,
					StreamsResources.getBundle(WsStreamConstants.RESOURCE_BUNDLE_NAME),
					"ActivityJDBCResultSetParser.sql.type.value.resolution.failed", sqlObjVal.getClass().getName(),
					exc);
		}
		return sqlObjVal;
	}

	private static int getRsRow(ResultSet resultSet) {
		try {
			return resultSet.getRow();
		} catch (SQLException exc) {
		}
		return 0;
	}

	private static final String[] ACTIVITY_DATA_TYPES = { "RESULT SET" }; // NON-NLS

	/**
	 * Returns type of RAW activity data entries.
	 *
	 * @return type of RAW activity data entries - {@code "RESULT SET"}
	 */
	@Override
	protected String[] getActivityDataType() {
		return ACTIVITY_DATA_TYPES;
	}

	@SuppressWarnings("deprecation")
	private static final EnumSet<ActivityFieldLocatorType> UNSUPPORTED_LOCATOR_TYPES = EnumSet
			.of(ActivityFieldLocatorType.REMatchId, ActivityFieldLocatorType.Range);

	/**
	 * {@inheritDoc}
	 * <p>
	 * Unsupported activity locator types are:
	 * <ul>
	 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#REMatchId}</li>
	 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Range}</li>
	 * </ul>
	 */
	@Override
	protected EnumSet<ActivityFieldLocatorType> getUnsupportedLocatorTypes() {
		return UNSUPPORTED_LOCATOR_TYPES;
	}
}
