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

package com.jkoolcloud.tnt4j.streams.fields;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import com.jkoolcloud.tnt4j.core.*;
import com.jkoolcloud.tnt4j.format.JSONFormatter;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.source.SourceType;
import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.utils.*;
import com.jkoolcloud.tnt4j.tracker.TimeTracker;
import com.jkoolcloud.tnt4j.tracker.Tracker;
import com.jkoolcloud.tnt4j.tracker.TrackingActivity;
import com.jkoolcloud.tnt4j.tracker.TrackingEvent;
import com.jkoolcloud.tnt4j.uuid.DefaultUUIDFactory;

/**
 * This class represents an {@link com.jkoolcloud.tnt4j.core.Trackable} entity (e.g. activity/event/snapshot/dataset) to
 * record to jKoolCloud.
 *
 * @version $Revision: 3 $
 */
public class ActivityInfo {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(ActivityInfo.class);

	private static final TimeTracker ACTIVITY_TIME_TRACKER = TimeTracker.newTracker(1000, TimeUnit.HOURS.toMillis(8));

	private static final Pattern CHILD_FIELD_PATTERN = Pattern.compile("child\\[(?<child>\\S+)\\]\\.(?<field>\\S+)"); // NON-NLS
	private static final String KV_DELIM = "\\="; // NON-NLS
	private static final String PATH_DELIM = "\\."; // NON-NLS

	private static final Map<String, String> HOST_CACHE = new ConcurrentHashMap<>();
	private static final String LOCAL_SERVER_NAME_KEY = "LOCAL_SERVER_NAME_KEY"; // NON-NLS
	private static final String LOCAL_SERVER_IP_KEY = "LOCAL_SERVER_IP_KEY"; // NON-NLS

	private String serverName = null;
	private String serverIp = null;
	private String applName = null;
	private String userName = null;

	private String resourceName = null;

	private String eventName = null;
	private OpType eventType = null;
	private ActivityStatus eventStatus = null;
	private UsecTimestamp startTime = null;
	private UsecTimestamp endTime = null;
	private long elapsedTime = -1L;
	private OpCompCode compCode = null;
	private int reasonCode = 0;
	private String exception = null;
	private OpLevel severity = null;
	private String location = null;
	private Collection<String> correlator = null;

	private String trackingId = null;
	private String parentId = null;
	private String guid = null;
	private Collection<String> tag = null;
	private Object message = null;
	private String msgCharSet = null;
	private String msgEncoding = null;
	private Integer msgLength = null;
	private String msgMimeType = null;
	private long msgAge = -1L;
	private long ttl = TTL.TTL_DEFAULT;

	private Integer processId = null;
	private Integer threadId = null;

	private String category = null;

	private boolean filteredOut = false;
	private boolean complete = false;

	private Map<String, Property> activityProperties;
	private Map<String, List<ActivityInfo>> children;
	private ActivityInfo parent;
	private int ordinalIdx = 0;

	/**
	 * Constructs a new ActivityInfo object.
	 */
	public ActivityInfo() {
	}

	/**
	 * Constructs a new ActivityInfo object.
	 * 
	 * @param complete
	 *            {@code true} if activity is complete, {@code false} - otherwise
	 */
	public ActivityInfo(boolean complete) {
		this.complete = complete;
	}

	/**
	 * Applies the given value(s) for the specified field to the appropriate internal data field for reporting field to
	 * the jKoolCloud.
	 *
	 * @param field
	 *            field to apply
	 * @param value
	 *            value to apply for this field, which could be an array of objects if value for field consists of
	 *            multiple locations
	 *
	 * @throws java.text.ParseException
	 *             if an error parsing the specified value based on the field definition (e.g. does not match defined
	 *             format, etc.)
	 */
	public void applyFieldValue(ActivityField field, Object value) throws ParseException {
		LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"ActivityInfo.applying.field", field, Utils.toString(value));

		if (value == null) {
			LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityInfo.field.value.null", field);
			return;
		}

		if (!field.isTransparent()) {
			setFieldValue(field, value);
		} else {
			addActivityProperty(field.getFieldTypeName(), getAggregatedFieldValue(value, field), true);
		}
	}

	/**
	 * Aggregates and applies the given value(s) for the specified field to the appropriate internal data field for
	 * reporting field to the jKoolCloud.
	 *
	 * @param field
	 *            field to apply
	 * @param value
	 *            value to apply for this field, which could be an array of objects if value for field consists of
	 *            multiple locations
	 *
	 * @throws java.text.ParseException
	 *             if an error parsing the specified value based on the field definition (e.g. does not match defined
	 *             format, etc.)
	 *
	 * @see #applyFieldValue(ActivityField, Object)
	 * @see com.jkoolcloud.tnt4j.streams.fields.ActivityField#aggregateFieldValue(Object, ActivityInfo)
	 */
	public void applyField(ActivityField field, Object value) throws ParseException {
		applyFieldValue(field, field.aggregateFieldValue(value, this));
	}

	/**
	 * Sets field to specified value, handling any necessary conversions based on internal data type for field.
	 *
	 * @param field
	 *            field whose value is to be set
	 * @param fieldValue
	 *            formatted value based on locator definition for field
	 *
	 * @throws java.text.ParseException
	 *             if there are any errors with conversion to internal format
	 */
	public void setFieldValue(ActivityField field, Object fieldValue) throws ParseException {
		if (Utils.isNullValue(fieldValue)) {
			return;
		}

		StreamFieldType fieldType = field.getFieldType();
		if (fieldType != null) {
			switch (fieldType) {
			case Message:
				message = substitute(message, fieldValue);
				break;
			case EventName:
				eventName = substitute(eventName, getStringValue(fieldValue, field));
				break;
			case EventType:
				eventType = substitute(eventType, Utils.mapOpType(fieldValue));
				break;
			case EventStatus:
				ActivityStatus as = fieldValue instanceof ActivityStatus ? (ActivityStatus) fieldValue
						: ActivityStatus.valueOf(fieldValue);
				eventStatus = substitute(eventStatus, as);
			case ApplName:
				applName = substitute(applName, getStringValue(fieldValue, field));
				break;
			case Correlator:
				addCorrelator(getStringsValue(fieldValue, field));
				break;
			case ElapsedTime:
				elapsedTime = substitute(elapsedTime, getNumberValue(fieldValue, Long.class, field));
				break;
			case EndTime:
				endTime = substitute(endTime, getTimestampValue(fieldValue, field));
				break;
			case Exception:
				exception = substitute(exception, getStringValue(fieldValue, field));
				break;
			case Location:
				location = substitute(location, getStringValue(fieldValue, field));
				break;
			case ReasonCode:
				reasonCode = substitute(reasonCode, getNumberValue(fieldValue, Integer.class, field));
				break;
			case ResourceName:
				resourceName = substitute(resourceName, getStringValue(fieldValue, field));
				break;
			case ServerIp:
				serverIp = substitute(serverIp, getStringValue(fieldValue, field));
				break;
			case ServerName:
				serverName = substitute(serverName, getStringValue(fieldValue, field));
				break;
			case Severity:
				OpLevel sev = fieldValue instanceof OpLevel ? (OpLevel) fieldValue : OpLevel.valueOf(fieldValue);
				severity = substitute(severity, sev);
				break;
			case TrackingId:
				trackingId = substitute(trackingId, getStringValue(fieldValue, field));
				break;
			case StartTime:
				startTime = substitute(startTime, getTimestampValue(fieldValue, field));
				break;
			case CompCode:
				OpCompCode cc = fieldValue instanceof OpCompCode ? (OpCompCode) fieldValue
						: OpCompCode.valueOf(fieldValue);
				compCode = substitute(compCode, cc);
				break;
			case Tag:
				addTag(getStringsValue(fieldValue, field));
				break;
			case UserName:
				userName = substitute(userName, getStringValue(fieldValue, field));
				break;
			case MsgCharSet:
				msgCharSet = substitute(msgCharSet, getStringValue(fieldValue, field));
				break;
			case MsgEncoding:
				msgEncoding = substitute(msgEncoding, getStringValue(fieldValue, field));
				break;
			case MsgLength:
				msgLength = substitute(msgLength, getNumberValue(fieldValue, Integer.class, field));
				break;
			case MsgMimeType:
				msgMimeType = substitute(msgMimeType, getStringValue(fieldValue, field));
				break;
			case MessageAge:
				msgAge = substitute(msgAge, getNumberValue(fieldValue, Long.class, field));
				break;
			case ProcessId:
				processId = substitute(processId, getNumberValue(fieldValue, Integer.class, field));
				break;
			case ThreadId:
				threadId = substitute(threadId, getNumberValue(fieldValue, Integer.class, field));
				break;
			case Category:
				category = substitute(category, getStringValue(fieldValue, field));
				break;
			case ParentId:
				parentId = substitute(parentId, getStringValue(fieldValue, field));
				break;
			case Guid:
				guid = substitute(guid, getStringValue(fieldValue, field));
				break;
			case TTL:
				ttl = substitute(ttl, getNumberValue(fieldValue, Long.class, field));
				break;
			default:
				throw new IllegalArgumentException(StreamsResources.getStringFormatted(
						StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityInfo.unrecognized.field", field));
			}

			LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityInfo.set.field", field, Utils.toString(fieldValue));
		} else {
			addCustomActivityProperty(field, fieldValue);
		}
	}

	private void addCustomActivityProperty(ActivityField field, Object fieldValue) throws ParseException {
		if (fieldValue instanceof Trackable) {
			addActivityProperty(field.getFieldTypeName(), fieldValue);
		} else if (fieldValue instanceof Map) {
			addPropertiesMap(field, (Map<?, ?>) fieldValue, "");
		} else {
			addActivityProperty(field.getFieldTypeName(), getAggregatedFieldValue(fieldValue, field),
					field.getValueType());
		}
	}

	private void addPropertiesMap(ActivityField field, Map<?, ?> pMap, String propPrefix) throws ParseException {
		for (Map.Entry<?, ?> pme : pMap.entrySet()) {
			String pKey = propPrefix + String.valueOf(pme.getKey());

			if (pme.getValue() instanceof Map) {
				addPropertiesMap(field, (Map<?, ?>) pme.getValue(),
						pKey + field.getParser().getProperty(ParserProperties.PROP_COMPOSITE_DELIM));
			} else {
				addActivityProperty(pKey, getAggregatedFieldValue(pme.getValue(), field));
			}
		}
	}

	/**
	 * Formats and aggregates raw field value using {@code field} defined data type and aggregation/formatting rules.
	 * 
	 * @param fieldValue
	 *            raw field value
	 * @param field
	 *            field whose value is to be aggregated
	 * @return aggregated and formatted field value
	 * @throws ParseException
	 *             if value aggregation fails
	 * 
	 * @see #getAggregatedFieldValue(Object, ActivityField, ActivityFieldLocator)
	 */
	public static Object getAggregatedFieldValue(Object fieldValue, ActivityField field) throws ParseException {
		ActivityFieldLocator fmLocator = field.getMasterLocator();

		return getAggregatedFieldValue(fieldValue, field, fmLocator);
	}

	/**
	 * Formats and aggregates raw field value using {@code locator} defined data type and {@code field} defined
	 * aggregation/formatting rules.
	 * 
	 * @param fieldValue
	 *            raw field value
	 * @param field
	 *            field whose value is to be aggregated
	 * @param locator
	 *            locator instance defining field value data type
	 * @return aggregated and formatted field value
	 * 
	 * @throws ParseException
	 *             if value aggregation fails
	 */
	public static Object getAggregatedFieldValue(Object fieldValue, ActivityField field, ActivityFieldLocator locator)
			throws ParseException {
		if (locator != null) {
			switch (locator.getDataType()) {
			case Number:
				return getNumberValue(fieldValue, field);
			case DateTime:
			case Timestamp:
				return getTimestampValue(fieldValue, field);
			case Generic:
				return getPredictedValue(fieldValue, field);
			case Binary:
			case String:
				return getStringValue(fieldValue, field);
			case AsInput:
			default:
				return fieldValue;
			}
		}

		return getStringValue(fieldValue, field);
	}

	private static Object getPredictedValue(Object fieldValue, ActivityField field) {
		// is it a number
		try {
			return getNumberValue(fieldValue, field);
		} catch (Exception e) {
		}

		// is it a boolean
		try {
			Boolean b = Utils.getBoolean(fieldValue);
			if (b != null) {
				return b;
			}
		} catch (Exception exc) {
		}

		// is it a timestamp
		try {
			return getTimestampValue(fieldValue, field);
		} catch (ParseException e1) {
		}

		// make a string eventually
		return getStringValue(fieldValue, field);
	}

	private static UsecTimestamp getTimestampValue(Object fieldValue, ActivityField field) throws ParseException {
		UsecTimestamp timestamp = TimestampFormatter.getTimestamp(fieldValue);
		if (timestamp != null) {
			return timestamp;
		}

		ActivityFieldLocator fmLocator = field.getMasterLocator();
		String tz = fmLocator == null ? null : fmLocator.getTimeZone();
		try {
			TimeUnit units = ActivityFieldLocator.getLocatorUnits(fmLocator, TimeUnit.MILLISECONDS);
			timestamp = TimestampFormatter.parse(units, fieldValue, tz);
			if (timestamp != null) {
				return timestamp;
			}
		} catch (ParseException exc) {
		}

		return TimestampFormatter.parse(fmLocator == null ? null : fmLocator.getFormat(),
				getStringValue(fieldValue, field), tz, fmLocator == null ? null : fmLocator.getLocale());
	}

	private static String substitute(String value, String newValue) {
		return StringUtils.isEmpty(newValue) ? value : newValue;
	}

	private static <T> T substitute(T value, T newValue) {
		return newValue == null ? value : newValue;
	}

	private static String getStringValue(Object value, ActivityField field) {
		if (StringUtils.isNotEmpty(field.getFormattingPattern())) {
			value = Utils.makeArray(value);
		}
		if (Utils.isObjArray(value)) {
			return formatValuesArray((Object[]) value, field);
		} else if (value instanceof byte[]) {
			return Utils.encodeHex((byte[]) value);
		} else if (Utils.isPrimitiveArray(value)) {
			return ArrayUtils.toString(value);
		}

		return Utils.toString(value);
	}

	private static String formatValuesArray(Object[] vArray, ActivityField field) {
		if (StringUtils.isNotEmpty(field.getFormattingPattern())) {
			return formatArrayPattern(field.getFormattingPattern(), vArray, field);
		} else {
			StringBuilder sb = new StringBuilder();
			for (int v = 0; v < vArray.length; v++) {
				if (v > 0) {
					sb.append(field.getSeparator());
				}

				if (vArray[v] instanceof UsecTimestamp) {
					ActivityFieldLocator locator = field.getLocators().size() == 1 ? field.getLocators().get(0)
							: v >= 0 && v < field.getLocators().size() ? field.getLocators().get(v) : null; // TODO

					String format = locator == null ? null : locator.getFormat();
					String tz = locator == null ? null : locator.getTimeZone();
					if (StringUtils.isNotEmpty(format)) {
						sb.append(((UsecTimestamp) vArray[v]).toString(format, tz));
					}
				} else {
					sb.append(vArray[v] == null ? "" : Utils.toString(vArray[v])); // NON-NLS
				}
			}

			return sb.toString();
		}
	}

	private static String formatArrayPattern(String pattern, Object[] vArray, ActivityField field) {
		MessageFormat mf = new MessageFormat(pattern);

		try {
			Field f = FieldUtils.getDeclaredField(mf.getClass(), "maxOffset", true);
			int maxOffset = f.getInt(mf);
			if (maxOffset >= 0) {
				int[] ana = (int[]) FieldUtils.readDeclaredField(mf, "argumentNumbers", true);
				int maxIndex = ana[maxOffset];
				int arrayLength = ArrayUtils.getLength(vArray);

				if (maxIndex >= arrayLength) {
					LOGGER.log(OpLevel.WARNING, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"ActivityInfo.formatting.arguments.mismatch", pattern, maxIndex, arrayLength, field);

					Object[] pvArray = new Object[maxIndex + 1];
					for (int i = 0; i < pvArray.length; i++) {
						pvArray[i] = i < arrayLength ? vArray[i] : "";
					}
					vArray = pvArray;
				}
			}
		} catch (Exception exc) {
		}

		return mf.format(vArray);
	}

	private static String[] getStringsValue(Object value, ActivityField field) {
		String[] strings = Utils.getTags(value);

		if (field.isArrayFormattable(strings)) {
			strings = new String[] { formatValuesArray(strings, field) };
		}

		return strings;
	}

	/**
	 * Adds activity item property to item properties map. Properties from map are transferred as tracking event
	 * properties when {@link #buildTrackable(com.jkoolcloud.tnt4j.tracker.Tracker, java.util.Map)} is invoked. Same as
	 * invoking {@link #addActivityProperty(String, Object, boolean)} setting transient flag value to {@code false}.
	 *
	 * @param propName
	 *            activity item property key
	 * @param propValue
	 *            activity item property value
	 * @return previous property value replaced by {@code propValue} or {@code null} if there was no such activity
	 *         property set
	 *
	 * @see java.util.Map#put(Object, Object)
	 * @see #buildTrackable(com.jkoolcloud.tnt4j.tracker.Tracker, java.util.Map)
	 * @see #addActivityProperty(String, Object, boolean)
	 */
	public Object addActivityProperty(String propName, Object propValue) {
		return addActivityProperty(propName, propValue, false);
	}

	/**
	 * Adds activity item property to item properties map. Properties from map are transferred as tracking event
	 * properties when {@link #buildTrackable(com.jkoolcloud.tnt4j.tracker.Tracker, java.util.Map)} is invoked.
	 *
	 * @param propName
	 *            activity item property key
	 * @param propValue
	 *            activity item property value
	 * @param transient_
	 *            flag indicating whether property is transient
	 * @return previous property value replaced by {@code propValue} or {@code null} if there was no such activity
	 *         property set
	 *
	 * @see java.util.Map#put(Object, Object)
	 * @see #buildTrackable(com.jkoolcloud.tnt4j.tracker.Tracker, java.util.Map)
	 * @see com.jkoolcloud.tnt4j.core.Property#Property(String, Object, boolean)
	 * @see #addActivityProperty(com.jkoolcloud.tnt4j.core.Property)
	 */
	public Object addActivityProperty(String propName, Object propValue, boolean transient_) {
		return addActivityProperty(new Property(propName, propValue, transient_));
	}

	/**
	 * Adds activity item property to item properties map. Properties from map are transferred as tracking event
	 * properties when {@link #buildTrackable(com.jkoolcloud.tnt4j.tracker.Tracker, java.util.Map)} is invoked.
	 *
	 * @param propName
	 *            activity item property key
	 * @param propValue
	 *            activity item property value
	 * @param valueType
	 *            activity item property value type from {@link com.jkoolcloud.tnt4j.core.ValueTypes} set
	 * @return previous property value replaced by {@code propValue} or {@code null} if there was no such activity
	 *         property set
	 *
	 * @see java.util.Map#put(Object, Object)
	 * @see #buildTrackable(com.jkoolcloud.tnt4j.tracker.Tracker, java.util.Map)
	 * @see com.jkoolcloud.tnt4j.core.Property#Property(String, Object, String)
	 * @see #addActivityProperty(com.jkoolcloud.tnt4j.core.Property)
	 * @see com.jkoolcloud.tnt4j.core.ValueTypes
	 */
	public Object addActivityProperty(String propName, Object propValue, String valueType) {
		return addActivityProperty(new Property(propName, propValue,
				StringUtils.isEmpty(valueType) ? getDefaultValueType(propValue) : valueType));
	}

	/**
	 * Adds activity item property to item properties map. Properties from map are transferred as tracking event
	 * properties when {@link #buildTrackable(com.jkoolcloud.tnt4j.tracker.Tracker, java.util.Map)} is invoked.
	 *
	 * @param property
	 *            property instance to add to activity entity properties map
	 * @return previous property value replaced by {@code propValue} or {@code null} if there was no such activity
	 *         property set
	 *
	 * @see java.util.Map#put(Object, Object)
	 * @see #buildTrackable(com.jkoolcloud.tnt4j.tracker.Tracker, java.util.Map)
	 * @see com.jkoolcloud.tnt4j.core.ValueTypes
	 */
	public Object addActivityProperty(Property property) {
		if (activityProperties == null) {
			activityProperties = new HashMap<>();
		}

		String propName = property.getKey();
		Property prevValue = activityProperties.put(propName, property);

		if (prevValue == null) {
			LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityInfo.set.property", propName, Utils.toString(property.getValue()),
					property.getValueType());
		} else {
			LOGGER.log(OpLevel.WARNING, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityInfo.replace.property", propName, Utils.toString(property.getValue()),
					property.getValueType(), Utils.toString(prevValue));
		}

		return prevValue;
	}

	private static String getDefaultValueType(Object propValue) {
		if (propValue instanceof UsecTimestamp) {
			return ValueTypes.VALUE_TYPE_TIMESTAMP;
		}

		return ValueTypes.VALUE_TYPE_NONE;
	}

	/**
	 * Appends activity item tags collection with provided tag strings array contents.
	 *
	 * @param tags
	 *            tag strings array
	 */
	public void addTag(String... tags) {
		this.tag = addStrings(this.tag, tags);
	}

	/**
	 * Appends activity item correlators collection with provided correlator strings array contents.
	 *
	 * @param correlators
	 *            correlator strings array
	 */
	public void addCorrelator(String... correlators) {
		this.correlator = addStrings(this.correlator, correlators);
	}

	private static Collection<String> addStrings(Collection<String> collection, String... strings) {
		if (ArrayUtils.isNotEmpty(strings)) {
			if (collection == null) {
				collection = new LinkedHashSet<>();
			}

			for (String str : strings) {
				if (StringUtils.isNotEmpty(str)) {
					collection.add(str.trim());
				}
			}
		}

		return collection;
	}

	/**
	 * Makes fully qualified name of activity source. Name is made from stream parsed data attributes.
	 *
	 * @param pattern
	 *            fqn pattern to fill
	 * @return fully qualified name of this activity source, or {@code null} if no source defining attributes were
	 *         parsed from stream.
	 */
	public String getSourceFQN(String pattern) {
		Collection<String> fqnTokens = getFQNTokens(pattern);

		StringBuilder fqnB = new StringBuilder();

		for (String fqnT : fqnTokens) {
			String[] pair = fqnT.split(KV_DELIM);
			SourceType type = SourceType.valueOf(pair[0]);
			addSourceValue(fqnB, type, getFQNValue(pair[1]));
		}

		String fqn = fqnB.toString();

		return StringUtils.isEmpty(fqn) ? null : fqn;
	}

	private static Collection<String> getFQNTokens(String fqnPattern) {
		Collection<String> fqnTokens = new ArrayList<>();
		StringTokenizer tk = new StringTokenizer(fqnPattern, "#");
		while (tk.hasMoreTokens()) {
			String sToken = tk.nextToken();
			fqnTokens.add(sToken);
		}

		return fqnTokens;
	}

	private String getFQNValue(String val) {
		if (val.startsWith(Utils.VAR_EXP_START_TOKEN)) {
			Object fieldValue = getFieldValue(val);

			return fieldValue == null ? null : Utils.toString(fieldValue);
		}

		return val;
	}

	private static void addSourceValue(StringBuilder sb, SourceType type, String value) {
		if (StringUtils.trimToNull(value) != null) {
			if (sb.length() > 0) {
				sb.append('#'); // NON-NLS
			}
			sb.append(type).append('=').append(value); // NON-NLS
		}
	}

	/**
	 * Creates the appropriate data package {@link com.jkoolcloud.tnt4j.tracker.TrackingActivity},
	 * {@link com.jkoolcloud.tnt4j.tracker.TrackingEvent}, {@link com.jkoolcloud.tnt4j.core.PropertySnapshot} or
	 * {@link com.jkoolcloud.tnt4j.core.Dataset} using the specified tracker for this activity data entity to be sent to
	 * jKoolCloud.
	 *
	 * @param tracker
	 *            {@link com.jkoolcloud.tnt4j.tracker.Tracker} instance to be used to build
	 *            {@link com.jkoolcloud.tnt4j.core.Trackable} activity data package
	 * @param childMap
	 *            map to add built child trackables, not included into parent trackable and transmitted separately,
	 *            e.g., activity child events
	 * @return trackable instance made from this activity entity data
	 * @throws IllegalArgumentException
	 *             if {@code tracker} is null
	 * @see com.jkoolcloud.tnt4j.streams.outputs.JKCloudActivityOutput#logItem(ActivityInfo)
	 */
	public Trackable buildTrackable(Tracker tracker, Map<Trackable, ActivityInfo> childMap) {
		if (tracker == null) {
			throw new IllegalArgumentException(
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityInfo.tracker.null"));
		}

		determineTimes();
		resolveServer(false);
		determineTrackingId();

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"ActivityInfo.building.trackable", tracker.getId(), eventType, trackingId);

		if (eventType == OpType.ACTIVITY) {
			return buildActivity(tracker, eventName, trackingId, childMap);
		} else if (eventType == OpType.SNAPSHOT) {
			return buildSnapshot(tracker, eventName, trackingId);
		} else if (eventType == OpType.DATASET) {
			return buildDataset(tracker, eventName, trackingId);
		} else {
			return buildEvent(tracker, eventName, trackingId, childMap);
		}
	}

	/**
	 * Assigns new activity entity tracking identifier value if not yet defined.
	 *
	 * @return the assigned tracking identifier
	 * 
	 * @see #determineTrackingId(String)
	 */
	public String determineTrackingId() {
		return determineTrackingId(null);
	}

	/**
	 * Assigns new activity entity tracking identifier value if not yet defined.
	 * <p>
	 * If context and provided {@code defaultTrackingId} tracking identifier values are {@code null} or empty - new UUID
	 * value is assigned.
	 *
	 * @param defaultTrackingId
	 *            tracking identifier to assign if there is no context applicable value
	 * @return the assigned tracking identifier
	 */
	public String determineTrackingId(String defaultTrackingId) {
		if (StringUtils.isEmpty(trackingId)) {
			if (StringUtils.isNotEmpty(guid)) {
				trackingId = guid;
			} else {
				trackingId = StringUtils.isEmpty(defaultTrackingId) ? DefaultUUIDFactory.getInstance().newUUID()
						: defaultTrackingId;
			}
		}
		return trackingId;
	}

	/**
	 * Creates the appropriate data package {@link com.jkoolcloud.tnt4j.tracker.TrackingActivity},
	 * {@link com.jkoolcloud.tnt4j.tracker.TrackingEvent}, {@link com.jkoolcloud.tnt4j.core.PropertySnapshot} or
	 * {@link com.jkoolcloud.tnt4j.core.Dataset} using the specified tracker for this activity data entity to be sent to
	 * jKoolCloud.
	 * <p>
	 * Does same as {@link #buildTrackable(com.jkoolcloud.tnt4j.tracker.Tracker, java.util.Map)} where {@code childMap}
	 * list is {@code null}.
	 *
	 * @param tracker
	 *            {@link com.jkoolcloud.tnt4j.tracker.Tracker} instance to be used to build
	 *            {@link com.jkoolcloud.tnt4j.core.Trackable} activity data package
	 * @return trackable instance made from this activity entity data
	 * @throws IllegalArgumentException
	 *             if {@code tracker} is null
	 * @see #buildTrackable(com.jkoolcloud.tnt4j.tracker.Tracker, java.util.Map)
	 */
	public Trackable buildTrackable(Tracker tracker) {
		return buildTrackable(tracker, null);
	}

	/**
	 * Builds {@link com.jkoolcloud.tnt4j.tracker.TrackingEvent} for activity data recording.
	 *
	 * @param tracker
	 *            communication gateway to use to record activity
	 * @param trackName
	 *            name of tracking event
	 * @param trackId
	 *            identifier (signature) of tracking event
	 * @param childMap
	 *            map to add built child trackables, not included into parent event and transmitted separately
	 * @return tracking event instance
	 */
	protected TrackingEvent buildEvent(Tracker tracker, String trackName, String trackId,
			Map<Trackable, ActivityInfo> childMap) {
		trackName = getVerifiedEntityName(trackName, trackId, TrackingEvent.class);

		TrackingEvent event = tracker.newEvent(severity == null ? OpLevel.INFO : severity, trackName, (String) null,
				(String) null, (Object[]) null);
		event.setTrackingId(trackId);
		event.setParentId(parentId);
		if (StringUtils.isNotEmpty(guid)) {
			event.setGUID(guid);
			event.setCorrelator(guid);
		}
		if (CollectionUtils.isNotEmpty(correlator)) {
			event.setCorrelator(correlator);
		}
		if (CollectionUtils.isNotEmpty(tag)) {
			event.setTag(tag);
		}
		if (message != null) {
			if (message instanceof byte[]) {
				byte[] binData = (byte[]) message;
				event.setMessage(binData, (Object[]) null);
			} else {
				String strData = Utils.toString(message);
				event.setMessage(strData, (Object[]) null);
			}

			if (msgLength != null) {
				event.setSize(msgLength);
			}
		}
		if (StringUtils.isNotEmpty(msgMimeType)) {
			event.setMimeType(msgMimeType);
		}
		if (StringUtils.isNotEmpty(msgEncoding)) {
			event.setEncoding(msgEncoding);
		}
		if (StringUtils.isNotEmpty(msgCharSet)) {
			event.setCharset(msgCharSet);
		}
		if (msgAge > 0L) {
			event.setMessageAge(msgAge);
		}

		event.getOperation().setCompCode(compCode == null ? OpCompCode.SUCCESS : compCode);
		event.getOperation().setReasonCode(reasonCode);
		event.getOperation().setType(eventType == null ? OpType.EVENT : eventType);
		event.getOperation().setException(exception);
		if (StringUtils.isNotEmpty(location)) {
			event.getOperation().setLocation(location);
		}
		event.getOperation().setResource(resourceName);
		event.getOperation().setUser(StringUtils.isEmpty(userName) ? tracker.getSource().getUser() : userName);
		event.getOperation().setTID(threadId == null ? Thread.currentThread().getId() : threadId);
		event.getOperation().setPID(processId == null ? Utils.getVMPID() : processId);
		// event.getOperation().setSeverity(severity == null ? OpLevel.INFO : severity);
		if (eventStatus != null) {
			addActivityProperty(JSONFormatter.JSON_STATUS_FIELD, eventStatus);
		}
		if (StringUtils.isNotEmpty(category)) {
			addActivityProperty(JSONFormatter.JSON_CATEGORY_FIELD, category);
		}
		event.start(startTime);
		event.stop(endTime, elapsedTime);
		event.setTTL(ttl);

		if (activityProperties != null) {
			for (Property ap : activityProperties.values()) {
				if (ap.isTransient()) {
					continue;
				}

				if (ap.getValue() instanceof Snapshot) {
					event.getOperation().addSnapshot((Snapshot) ap.getValue());
				} else {
					event.getOperation().addProperty(ap);
				}
			}
		}

		if (hasChildren()) {
			buildChildren(tracker, event, childMap);
		}

		return event;
	}

	/**
	 * Builds {@link com.jkoolcloud.tnt4j.tracker.TrackingActivity} for activity data recording.
	 *
	 * @param tracker
	 *            communication gateway to use to record activity
	 * @param trackName
	 *            name of tracking activity
	 * @param trackId
	 *            identifier (signature) of tracking activity
	 * @param childMap
	 *            map to add built child trackables, not included into parent activity and transmitted separately
	 * @return tracking activity instance
	 */
	protected TrackingActivity buildActivity(Tracker tracker, String trackName, String trackId,
			Map<Trackable, ActivityInfo> childMap) {
		trackName = getVerifiedEntityName(trackName, trackId, TrackingActivity.class);

		TrackingActivity activity = tracker.newActivity(severity == null ? OpLevel.INFO : severity, trackName);
		activity.setTrackingId(trackId);
		activity.setParentId(parentId);
		if (StringUtils.isNotEmpty(guid)) {
			activity.setGUID(guid);
			activity.setCorrelator(guid);
		}
		if (CollectionUtils.isNotEmpty(correlator)) {
			activity.setCorrelator(correlator);
		}
		if (CollectionUtils.isNotEmpty(tag)) {
			addActivityProperty(JSONFormatter.JSON_MSG_TAG_FIELD, tag);
		}
		if (message != null) {
			String strData;
			if (message instanceof byte[]) {
				byte[] binData = (byte[]) message;
				strData = Utils.base64EncodeStr(binData);
				msgEncoding = Message.ENCODING_BASE64;
				msgMimeType = Message.MIME_TYPE_BINARY;
			} else {
				strData = Utils.toString(message);
			}

			addActivityProperty(JSONFormatter.JSON_MSG_TEXT_FIELD, strData);
			addActivityProperty(JSONFormatter.JSON_MSG_SIZE_FIELD, msgLength == null ? strData.length() : msgLength);
		}
		if (StringUtils.isNotEmpty(msgMimeType)) {
			addActivityProperty(JSONFormatter.JSON_MSG_MIME_FIELD, msgMimeType);
		}
		if (StringUtils.isNotEmpty(msgEncoding)) {
			addActivityProperty(JSONFormatter.JSON_MSG_ENC_FIELD, msgEncoding);
		}
		if (StringUtils.isNotEmpty(msgCharSet)) {
			addActivityProperty(JSONFormatter.JSON_MSG_CHARSET_FIELD, msgCharSet);
		}
		if (msgAge > 0L) {
			addActivityProperty(JSONFormatter.JSON_MSG_AGE_USEC_FIELD, msgAge);
		}

		activity.setCompCode(compCode == null ? OpCompCode.SUCCESS : compCode);
		activity.setReasonCode(reasonCode);
		// activity.setType(eventType);
		activity.setStatus(StringUtils.isNotEmpty(exception) ? ActivityStatus.EXCEPTION
				: eventStatus == null ? ActivityStatus.END : eventStatus);
		activity.setException(exception);
		if (StringUtils.isNotEmpty(location)) {
			activity.setLocation(location);
		}
		activity.setResource(resourceName);
		activity.setUser(StringUtils.isEmpty(userName) ? tracker.getSource().getUser() : userName);
		activity.setTID(threadId == null ? Thread.currentThread().getId() : threadId);
		activity.setPID(processId == null ? Utils.getVMPID() : processId);
		// activity.setSeverity(severity == null ? OpLevel.INFO : severity);
		if (StringUtils.isNotEmpty(category)) {
			addActivityProperty(JSONFormatter.JSON_CATEGORY_FIELD, category);
		}
		activity.start(startTime);
		activity.stop(endTime, elapsedTime);
		activity.setTTL(ttl);

		if (activityProperties != null) {
			for (Property ap : activityProperties.values()) {
				if (ap.isTransient()) {
					continue;
				}

				if (ap.getValue() instanceof Trackable) {
					activity.add((Trackable) ap.getValue());
				} else {
					activity.addProperty(ap);
				}
			}
		}

		if (hasChildren()) {
			buildChildren(tracker, activity, childMap);
		}

		return activity;
	}

	private void buildChildren(Tracker tracker, Trackable parent, Map<Trackable, ActivityInfo> childMap) {
		List<ActivityInfo> childList = getChildren(true);
		for (ActivityInfo child : childList) {
			if (!child.isDeliverable()) {
				LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityInfo.filtered.child", child);
				continue;
			}

			if (childMap == null) {
				if (!isEmbeddableChild(parent, child)) {
					continue;
				}
			} else {
				if (isChildAlreadyBuilt(childMap.keySet(), child.determineTrackingId())) {
					continue;
				}
			}

			Trackable cTrackable = buildChild(tracker, child, parent);
			boolean consumed = addTrackableChild(parent, cTrackable);

			if (!consumed && childMap != null) {
				childMap.put(cTrackable, child);
			}
		}
	}

	private static boolean isEmbeddableChild(Trackable parent, ActivityInfo child) {
		if (parent != null && child != null) {
			return isSnapshotType(child.eventType) && isOperation(parent);
		}

		return false;
	}

	private static boolean isSnapshotType(OpType opType) {
		return opType == OpType.SNAPSHOT || opType == OpType.DATASET;
	}

	private static boolean isOperation(Trackable trackable) {
		return trackable instanceof Activity || trackable instanceof TrackingEvent;
	}

	private static boolean isNoop(OpType opType) {
		return opType == OpType.NOOP;
	}

	private static boolean isChildAlreadyBuilt(Set<Trackable> trackables, String chId) {
		for (Trackable trackable : trackables) {
			if (trackable.getTrackingId().equals(chId)) {
				return true;
			}

			Trackable snap = null;
			if (trackable instanceof Activity) {
				TrackingActivity ta = (TrackingActivity) trackable;
				snap = ta.getSnapshot(chId);
			} else if (trackable instanceof TrackingEvent) {
				TrackingEvent te = (TrackingEvent) trackable;
				snap = te.getOperation().getSnapshot(chId);
			}

			if (snap != null) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Builds child entity trackables as split relatives by merging this (parent) activity entity data into child
	 * entity.
	 * <p>
	 * If no child relatives are available, only this (parent) activity build trackable is added to {@code childMap}
	 * map.
	 *
	 * @param tracker
	 *            communication gateway to use to record activity
	 * @param childMap
	 *            map to add built child trackables
	 *
	 * @see #merge(ActivityInfo)
	 */
	public void buildSplitRelatives(Tracker tracker, Map<Trackable, ActivityInfo> childMap) {
		List<ActivityInfo> childList = getFinalChildren();
		for (ActivityInfo child : childList) {
			if (!child.isDeliverable()) {
				LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityInfo.filtered.child", child);
				continue;
			}

			child.determineTrackingId(childList.size() > 1 ? null : this.trackingId);
			child.mergeAllParents();
			verifyDuplicates(child, childMap);
			Trackable trackable = child.buildTrackable(tracker);
			if (childMap != null) {
				childMap.put(trackable, child);
			}
		}

		if (childMap != null && childMap.isEmpty()) {
			verifyDuplicates(this, childMap);
			Trackable trackable = buildTrackable(tracker);
			childMap.put(trackable, this);
		}
	}

	private void verifyDuplicates(ActivityInfo child, Map<Trackable, ActivityInfo> childMap) {
		Map.Entry<Trackable, ActivityInfo> dCh = getDuplicate(child, childMap);
		if (dCh != null) {
			LOGGER.log(OpLevel.WARNING, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityInfo.duplicate.child", child.trackingId, child, dCh.getValue());
		}
	}

	private Map.Entry<Trackable, ActivityInfo> getDuplicate(ActivityInfo child, Map<Trackable, ActivityInfo> childMap) {
		if (childMap != null) {
			for (Map.Entry<Trackable, ActivityInfo> chT : childMap.entrySet()) {
				if (chT.getKey().getTrackingId().equals(child.determineTrackingId())) {
					return chT;
				}
			}
		}

		return null;
	}

	private static boolean addTrackableChild(Trackable parent, Trackable child) {
		if (parent != null && child != null) {
			if (parent instanceof TrackingEvent) {
				TrackingEvent pEvent = (TrackingEvent) parent;
				if (child instanceof Snapshot) {
					Snapshot chSnapshot = (Snapshot) child;
					pEvent.getOperation().addSnapshot(chSnapshot);

					return true;
				}
			} else if (parent instanceof Activity) {
				Activity pActivity = (Activity) parent;
				pActivity.add(child);

				return child instanceof Snapshot;
			}

			if (StringUtils.equals(child.getParentId(), parent.getTrackingId())) {
				LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityInfo.invalid.child", resolveTrackableType(child), resolveTrackableType(parent),
						resolveChildTypesFor(parent), child.getTrackingId(), parent.getTrackingId());
			}

		}

		return false;
	}

	private static String resolveTrackableType(Trackable trackable) {
		if (trackable instanceof Activity) {
			return OpType.ACTIVITY.name();
		} else if (trackable instanceof TrackingEvent) {
			return OpType.EVENT.name();
		} else if (trackable instanceof Dataset) {
			return OpType.DATASET.name();
		} else if (trackable instanceof Snapshot) {
			return OpType.SNAPSHOT.name();
		} else {
			return trackable == null ? null : trackable.getClass().getName();
		}
	}

	private static String resolveChildTypesFor(Trackable trackable) {
		if (trackable instanceof Activity) {
			return "ACTIVITY, EVENT, SNAPSHOT, DATASET"; // NON-NLS
		} else if (trackable instanceof TrackingEvent) {
			return "SNAPSHOT, DATASET"; // NON-NLS
		} else if (trackable instanceof Snapshot) {
			return "NONE"; // NON-NLS
		} else {
			return "UNKNOWN"; // NON-NLS
		}
	}

	private static Trackable buildChild(Tracker tracker, ActivityInfo child, Trackable parent) {
		if (StringUtils.isEmpty(child.parentId)) {
			if (!isNoop(parent.getType())) {
				child.parentId = parent.getTrackingId();
			}
		}

		return child.buildTrackable(tracker);
	}

	/**
	 * Builds {@link com.jkoolcloud.tnt4j.core.Snapshot} for activity data recording.
	 *
	 * @param tracker
	 *            communication gateway to use to record snapshot
	 * @param trackName
	 *            name of snapshot
	 * @param trackId
	 *            identifier (signature) of snapshot
	 * @return snapshot instance
	 */
	protected Snapshot buildSnapshot(Tracker tracker, String trackName, String trackId) {
		trackName = getVerifiedEntityName(trackName, trackId, PropertySnapshot.class);

		PropertySnapshot snapshot = (PropertySnapshot) (category != null ? tracker.newSnapshot(category, trackName)
				: tracker.newSnapshot(trackName));
		fillSnapshot(snapshot, trackId);

		return snapshot;
	}

	/**
	 * Builds {@link com.jkoolcloud.tnt4j.core.Dataset} for activity data recording.
	 *
	 * @param tracker
	 *            communication gateway to use to record dataset
	 * @param trackName
	 *            name of dataset
	 * @param trackId
	 *            identifier (signature) of dataset
	 * @return dataset instance
	 */
	protected Dataset buildDataset(Tracker tracker, String trackName, String trackId) {
		trackName = getVerifiedEntityName(trackName, trackId, Dataset.class);

		Dataset dataset = category != null ? tracker.newDataset(category, trackName) : tracker.newDataset(trackName);
		fillSnapshot(dataset, trackId);

		return dataset;
	}

	private String getVerifiedEntityName(String trackName, String trackId, Class<?> eClass) {
		// NOTE: TNT4J API fails if operation name is null
		if (StringUtils.isEmpty(trackName) && !isNoop(eventType)) {
			if (StringUtils.isNotEmpty(guid)) {
				trackName = guid;
			} else {
				trackName = trackId;
			}
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityInfo.activity.has.no.name", eClass.getSimpleName(), trackId, trackName);
		}

		return trackName;
	}

	private void fillSnapshot(PropertySnapshot snapshot, String trackId) {
		snapshot.setTrackingId(trackId);
		snapshot.setParentId(parentId);
		if (StringUtils.isNotEmpty(guid)) {
			snapshot.setGUID(guid);
			snapshot.setCorrelator(guid);
		}
		snapshot.setSeverity(severity == null ? OpLevel.INFO : severity);
		if (CollectionUtils.isNotEmpty(correlator)) {
			snapshot.setCorrelator(correlator);
		}
		if (CollectionUtils.isNotEmpty(tag)) {
			snapshot.add(JSONFormatter.JSON_MSG_TAG_FIELD, tag);
		}
		if (message != null) {
			String strData;
			if (message instanceof byte[]) {
				byte[] binData = (byte[]) message;
				strData = Utils.base64EncodeStr(binData);
				msgEncoding = Message.ENCODING_BASE64;
				msgMimeType = Message.MIME_TYPE_BINARY;
			} else {
				strData = Utils.toString(message);
			}

			addActivityProperty(JSONFormatter.JSON_MSG_TEXT_FIELD, strData);
			addActivityProperty(JSONFormatter.JSON_MSG_SIZE_FIELD, msgLength == null ? strData.length() : msgLength);
		}
		if (StringUtils.isNotEmpty(msgMimeType)) {
			snapshot.add(JSONFormatter.JSON_MSG_MIME_FIELD, msgMimeType);
		}
		if (StringUtils.isNotEmpty(msgEncoding)) {
			snapshot.add(JSONFormatter.JSON_MSG_ENC_FIELD, msgEncoding);
		}
		if (StringUtils.isNotEmpty(msgCharSet)) {
			snapshot.add(JSONFormatter.JSON_MSG_CHARSET_FIELD, msgCharSet);
		}
		if (msgAge > 0L) {
			snapshot.add(JSONFormatter.JSON_MSG_AGE_USEC_FIELD, msgAge);
		}
		if (compCode != null) {
			snapshot.add(JSONFormatter.JSON_COMP_CODE_FIELD, compCode);
		}
		if (reasonCode > 0) {
			snapshot.add(JSONFormatter.JSON_REASON_CODE_FIELD, reasonCode);
		}
		if (StringUtils.isNotEmpty(exception)) {
			snapshot.add(JSONFormatter.JSON_EXCEPTION_FIELD, exception);
		}
		if (StringUtils.isNotEmpty(location)) {
			snapshot.add(JSONFormatter.JSON_LOCATION_FIELD, location);
		}
		if (StringUtils.isNotEmpty(resourceName)) {
			snapshot.add(JSONFormatter.JSON_RESOURCE_FIELD, resourceName);
		}
		if (StringUtils.isNotEmpty(userName)) {
			snapshot.add(JSONFormatter.JSON_USER_FIELD, userName);
		}
		if (threadId != null) {
			snapshot.add(JSONFormatter.JSON_TID_FIELD, threadId);
		}
		if (processId != null) {
			snapshot.add(JSONFormatter.JSON_PID_FIELD, processId);
		}
		snapshot.setTimeStamp(startTime == null ? (endTime == null ? UsecTimestamp.now() : endTime) : startTime);
		snapshot.setTTL(ttl);
		if (eventStatus != null) {
			addActivityProperty(JSONFormatter.JSON_STATUS_FIELD, eventStatus);
		}

		if (activityProperties != null) {
			for (Property ap : activityProperties.values()) {
				if (ap.isTransient()) {
					continue;
				}

				snapshot.add(ap);
			}
		}
	}

	/**
	 * Resolves server name and/or IP Address based on values specified.
	 *
	 * @param resolveOverDNS
	 *            flag indicating whether to use DNS to resolve server names and IP addresses
	 */
	public void resolveServer(boolean resolveOverDNS) {
		if (StringUtils.isEmpty(serverName) && StringUtils.isEmpty(serverIp)) {
			serverName = HOST_CACHE.get(LOCAL_SERVER_NAME_KEY);
			serverIp = HOST_CACHE.get(LOCAL_SERVER_IP_KEY);

			if (serverName == null) {
				serverName = Utils.getLocalHostName();
				HOST_CACHE.put(LOCAL_SERVER_NAME_KEY, serverName);
			}
			if (serverIp == null) {
				serverIp = Utils.getLocalHostAddress();
				HOST_CACHE.put(LOCAL_SERVER_IP_KEY, serverIp);
			}
		} else if (StringUtils.isEmpty(serverName)) {
			if (resolveOverDNS) {
				try {
					serverName = HOST_CACHE.get(serverIp);
					if (StringUtils.isEmpty(serverName)) {
						serverName = Utils.resolveAddressToHostName(serverIp);
						if (StringUtils.isEmpty(serverName)) {
							// Add entry so we don't repeatedly attempt to look up unresolvable IP Address
							HOST_CACHE.put(serverIp, "");
						} else {
							HOST_CACHE.put(serverIp, serverName);
							HOST_CACHE.put(serverName, serverIp);
						}
					}
				} catch (Exception e) {
					serverName = serverIp;
				}
			} else {
				serverName = serverIp;
			}
		} else if (StringUtils.isEmpty(serverIp)) {
			if (resolveOverDNS) {
				serverIp = HOST_CACHE.get(serverName);
				if (StringUtils.isEmpty(serverIp)) {
					serverIp = Utils.resolveHostNameToAddress(serverName);
					if (StringUtils.isEmpty(serverIp)) {
						// Add entry so we don't repeatedly attempt to look up unresolvable host name
						HOST_CACHE.put(serverName, "");
					} else {
						HOST_CACHE.put(serverIp, serverName);
						HOST_CACHE.put(serverName, serverIp);
					}
				}
			}
		}

		if (StringUtils.isEmpty(serverIp)) {
			serverIp = " "; // prevents streams API from resolving it to the local IP address
		}
	}

	/**
	 * Computes the unspecified operation times and/or elapsed time based on the specified ones.
	 */
	private void determineTimes() {
		long elapsedTimeNano = StringUtils.isEmpty(resourceName) ? TimeTracker.hitAndGet()
				: ACTIVITY_TIME_TRACKER.hitAndGet(resourceName);

		if (elapsedTime < 0L) {
			if (startTime != null && endTime != null) {
				elapsedTime = endTime.difference(startTime);
			} else {
				elapsedTime = TimestampFormatter.convert(elapsedTimeNano, TimeUnit.NANOSECONDS, TimeUnit.MICROSECONDS);
			}
		}
		if (endTime == null) {
			if (startTime != null) {
				endTime = new UsecTimestamp(startTime);
				endTime.add(elapsedTime);
			} else {
				endTime = UsecTimestamp.now();
			}
		}
		if (startTime == null) {
			startTime = new UsecTimestamp(endTime);
			startTime.add(-elapsedTime);
		}
	}

	private static Number getNumberValue(Object value, ActivityField field) throws ParseException {
		if (value instanceof Number) {
			// value = (Number) value;
		} else {
			value = StringUtils.trim(value == null ? null : Utils.toString(value));
		}

		ActivityFieldLocator fmLocator = field.getMasterLocator();

		if (fmLocator == null) {
			return value instanceof Number ? (Number) value : NumericFormatter.strToNumber((String) value);
		} else {
			return fmLocator.formatNumericValue(value);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T extends Number> T getNumberValue(Object value, Class<T> clazz, ActivityField field)
			throws ParseException {
		Number num = getNumberValue(value, field);
		num = NumericFormatter.castNumber(num, clazz, NumericFormatter.CastMode.EXACT);

		return (T) num;
	}

	/**
	 * Merges provided activity info data fields values into this activity info. Matching fields value is changed only
	 * if it currently holds default (initial) value.
	 *
	 * @param otherAi
	 *            activity info object to merge into this one
	 */
	public void merge(ActivityInfo otherAi) {
		if (StringUtils.isEmpty(serverName)) {
			serverName = otherAi.serverName;
		}
		if (StringUtils.isEmpty(serverIp)) {
			serverIp = otherAi.serverIp;
		}
		if (StringUtils.isEmpty(applName)) {
			applName = otherAi.applName;
		}
		if (StringUtils.isEmpty(userName)) {
			userName = otherAi.userName;
		}

		if (StringUtils.isEmpty(resourceName)) {
			resourceName = otherAi.resourceName;
		}

		if (StringUtils.isEmpty(eventName)) {
			eventName = otherAi.eventName;
		}
		if (eventType == null || isNoop(eventType)) {
			eventType = substitute(eventType, otherAi.eventType);
		}
		if (eventStatus == null) {
			eventStatus = otherAi.eventStatus;
		}
		if (startTime == null) {
			startTime = otherAi.startTime;
		}
		if (endTime == null) {
			endTime = otherAi.endTime;
		}
		if (elapsedTime == -1L) {
			elapsedTime = otherAi.elapsedTime;
		}
		if (compCode == null) {
			compCode = otherAi.compCode;
		}
		if (reasonCode == 0) {
			reasonCode = otherAi.reasonCode;
		}
		if (StringUtils.isEmpty(exception)) {
			exception = otherAi.exception;
		}
		if (severity == null) {
			severity = otherAi.severity;
		}
		if (StringUtils.isEmpty(location)) {
			location = otherAi.location;
		}
		if (otherAi.correlator != null) {
			if (correlator == null) {
				correlator = new LinkedHashSet<>();
			}

			correlator.addAll(otherAi.correlator);
		}

		if (StringUtils.isEmpty(trackingId)) {
			trackingId = otherAi.trackingId;
		}
		if (StringUtils.isEmpty(guid)) {
			guid = otherAi.guid;
		}
		if (otherAi.tag != null) {
			if (tag == null) {
				tag = new LinkedHashSet<>();
			}

			tag.addAll(otherAi.tag);
		}
		if (message == null) {
			message = otherAi.message;
		}
		if (StringUtils.isEmpty(msgCharSet)) {
			msgCharSet = otherAi.msgCharSet;
		}
		if (StringUtils.isEmpty(msgEncoding)) {
			msgEncoding = otherAi.msgEncoding;
		}
		if (msgLength == null) {
			msgLength = otherAi.msgLength;
		}
		if (StringUtils.isEmpty(msgMimeType)) {
			msgMimeType = otherAi.msgMimeType;
		}
		if (msgAge == -1L) {
			msgAge = otherAi.msgAge;
		}
		if (ttl == TTL.TTL_DEFAULT) {
			ttl = otherAi.ttl;
		}

		if (processId == null) {
			processId = otherAi.processId;
		}
		if (threadId == null) {
			threadId = otherAi.threadId;
		}

		if (StringUtils.isEmpty(category)) {
			category = otherAi.category;
		}

		if (StringUtils.isEmpty(parentId)) {
			parentId = otherAi.parentId;
		}

		filteredOut |= otherAi.filteredOut;
		// complete |= otherAi.complete;

		if (otherAi.activityProperties != null) {
			if (activityProperties == null) {
				activityProperties = new HashMap<>();
			}

			activityProperties.putAll(otherAi.activityProperties);
		}
	}

	/**
	 * Merges provided activity info data fields values and child entities into this activity info. Matching fields
	 * value is changed only if it currently holds default (initial) value.
	 *
	 * @param otherAi
	 *            activity info object to merge into this one
	 *
	 * @see #merge(ActivityInfo)
	 */
	public void mergeAll(ActivityInfo otherAi) {
		merge(otherAi);

		if (otherAi.hasChildren()) {
			if (children == null) {
				children = new LinkedHashMap<>();
			}

			for (Map.Entry<String, List<ActivityInfo>> chE : otherAi.children.entrySet()) {
				children.put(chE.getKey(), chE.getValue());

				for (ActivityInfo chAi : chE.getValue()) {
					chAi.setParent(this);
				}
			}
			if (parent == null) {
				parent = otherAi.parent;
			}
		}
	}

	/**
	 * Merges all parent activities info data fields values into this activity info. Matching fields value is changed
	 * only if it currently holds default (initial) value.
	 * 
	 * @see #isDeliverable()
	 * @see #merge(ActivityInfo)
	 */
	public void mergeAllParents() {
		if (!this.isDeliverable()) {
			return;
		}

		ActivityInfo pAi = parent;

		while (pAi != null) {
			if (pAi.isDeliverable()) {
				this.merge(pAi);
			}

			pAi = pAi.parent;
		}
	}

	/**
	 * Gets server name.
	 *
	 * @return the server name
	 */
	public String getServerName() {
		return serverName;
	}

	/**
	 * Gets server ip.
	 *
	 * @return the server ip
	 */
	public String getServerIp() {
		return serverIp;
	}

	/**
	 * Gets application name.
	 *
	 * @return the application name
	 */
	public String getApplName() {
		return applName;
	}

	/**
	 * Gets user name.
	 *
	 * @return the user name
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * Gets resource name.
	 *
	 * @return the resource name
	 */
	public String getResourceName() {
		return resourceName;
	}

	/**
	 * Gets event name.
	 *
	 * @return the event name
	 */
	public String getEventName() {
		return eventName;
	}

	/**
	 * Sets event name.
	 *
	 * @param eventName
	 *            the event name
	 */
	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

	/**
	 * Gets event type.
	 *
	 * @return the event type
	 */
	public OpType getEventType() {
		return eventType;
	}

	/**
	 * Gets event status.
	 *
	 * @return the event status
	 */
	public ActivityStatus getEventStatus() {
		return eventStatus;
	}

	/**
	 * Gets start time.
	 *
	 * @return the start time
	 */
	public UsecTimestamp getStartTime() {
		return startTime;
	}

	/**
	 * Gets end time.
	 *
	 * @return the end time
	 */
	public UsecTimestamp getEndTime() {
		return endTime;
	}

	/**
	 * Gets elapsed time.
	 *
	 * @return the elapsed time
	 */
	public long getElapsedTime() {
		return elapsedTime;
	}

	/**
	 * Gets activity completion code.
	 *
	 * @return the activity completion code
	 */
	public OpCompCode getCompCode() {
		return compCode;
	}

	/**
	 * Gets reason code.
	 *
	 * @return the reason code
	 */
	public int getReasonCode() {
		return reasonCode;
	}

	/**
	 * Gets exception/error message.
	 *
	 * @return the exception/error message
	 */
	public String getException() {
		return exception;
	}

	/**
	 * Gets severity.
	 *
	 * @return the severity
	 */
	public OpLevel getSeverity() {
		return severity;
	}

	/**
	 * Gets location.
	 *
	 * @return the location
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * Gets tracking identifier.
	 *
	 * @return the tracking identifier
	 */
	public String getTrackingId() {
		return trackingId;
	}

	/**
	 * Gets global identifier.
	 *
	 * @return the global identifier
	 */
	public String getGUID() {
		return guid;
	}

	/**
	 * Gets activity tag strings collection.
	 *
	 * @return the activity tag strings collection
	 */
	public Collection<String> getTag() {
		return tag;
	}

	/**
	 * Gets activity correlator strings collection.
	 *
	 * @return the activity correlator string collection
	 */
	public Collection<String> getCorrelator() {
		return correlator;
	}

	/**
	 * Gets activity message data.
	 *
	 * @return the activity message data
	 */
	public Object getMessage() {
		return message;
	}

	/**
	 * Gets message char set.
	 *
	 * @return the message char set
	 */
	public String getMsgCharSet() {
		return msgCharSet;
	}

	/**
	 * Gets message encoding.
	 *
	 * @return the message encoding
	 */
	public String getMsgEncoding() {
		return msgEncoding;
	}

	/**
	 * Gets message length.
	 *
	 * @return the message length
	 */
	public int getMsgLength() {
		return msgLength;
	}

	/**
	 * Gets message MIME type.
	 *
	 * @return the message MIME type
	 */
	public String getMsgMimeType() {
		return msgMimeType;
	}

	/**
	 * Gets message age.
	 *
	 * @return the message age
	 */
	public long getMsgAge() {
		return msgAge;
	}

	/**
	 * Gets activity entity time-to-live.
	 *
	 * @return the activity entity time-to-live
	 */
	public long getTTL() {
		return ttl;
	}

	/**
	 * Gets process identifier.
	 *
	 * @return the process identifier
	 */
	public Integer getProcessId() {
		return processId;
	}

	/**
	 * Gets thread identifier.
	 *
	 * @return the thread identifier
	 */
	public Integer getThreadId() {
		return threadId;
	}

	/**
	 * Gets activity category (e.g., snapshot category).
	 *
	 * @return the activity category
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * Gets parent activity identifier.
	 *
	 * @return the parent activity identifier
	 */
	public String getParentId() {
		return parentId;
	}

	/**
	 * Returns activity filtered out flag value.
	 *
	 * @return activity filtered out flag value
	 */
	public boolean isFilteredOut() {
		return filteredOut;
	}

	/**
	 * Sets activity filtered out flag value.
	 *
	 * @param filteredOut
	 *            {@code true} if activity is filtered out, {@code false} - otherwise
	 */
	public void setFiltered(boolean filteredOut) {
		this.filteredOut = filteredOut;
	}

	/**
	 * Returns activity complete flag value.
	 *
	 * @return activity complete flag value
	 */
	public boolean isComplete() {
		return complete;
	}

	/**
	 * Sets activity complete flag value.
	 *
	 * @param complete
	 *            {@code true} if activity is complete, {@code false} - otherwise
	 */
	public void setComplete(boolean complete) {
		this.complete = complete;
	}

	/**
	 * Checks if this activity item is deliverable and shall be sent to output sink.
	 * 
	 * @return {@code true} if this entity is complete and not filtered out, {@code false} - otherwise
	 */
	public boolean isDeliverable() {
		return complete && !filteredOut;
	}

	/**
	 * Adds child activity entity data package.
	 *
	 * @param groupName
	 *            children group name (e.g. parser name)
	 * @param ai
	 *            activity entity object containing child data
	 * 
	 * @see #addChild(String, ActivityInfo, boolean)
	 */
	public void addChild(String groupName, ActivityInfo ai) {
		addChild(groupName, ai, false);
	}

	/**
	 * Adds child activity entity data package.
	 * 
	 * @param groupName
	 *            children group name (e.g. parser name)
	 * @param ai
	 *            activity entity object containing child data
	 * @param flatten
	 *            indicates if child activity shall be added as root activity child
	 * 
	 * @see #addChild(ActivityInfo, String, ActivityInfo)
	 */
	public void addChild(String groupName, ActivityInfo ai, boolean flatten) {
		addChild(flatten ? getRootActivity() : this, groupName, ai);
	}

	/**
	 * Adds child activity entity data package to {@code parent} entity children entities collection.
	 * 
	 * @param parent
	 *            parent activity entity to add child
	 * @param groupName
	 *            children group name (e.g. parser name)
	 * @param ai
	 *            activity entity object containing child data
	 */
	protected static void addChild(ActivityInfo parent, String groupName, ActivityInfo ai) {
		if (parent.children == null) {
			parent.children = new LinkedHashMap<>();
		}

		List<ActivityInfo> chList = parent.children.computeIfAbsent(groupName, k -> new ArrayList<>());
		chList.add(ai);

		ai.setParent(parent).setOrdinal(chList.size());
	}

	/**
	 * Returns list of all child activity entities for this activity info.
	 *
	 * @return list of child activity entities
	 *
	 * @see #getChildren(boolean)
	 */
	public List<ActivityInfo> getChildren() {
		return getChildren(false);
	}

	/**
	 * Returns list of all child activity entities for this activity info.
	 *
	 * @param deepCollect
	 *            indicates if all children of children entities shall be added to returned list (flattened children
	 *            hierarchy)
	 *
	 * @return list of child activity entities
	 *
	 * @see #collectChildren(ActivityInfo, java.util.Collection, boolean)
	 */
	public List<ActivityInfo> getChildren(boolean deepCollect) {
		List<ActivityInfo> chList = new ArrayList<>();

		collectChildren(this, chList, deepCollect);

		return chList;
	}

	/**
	 * Collects provided entity {@code ai} child entities to {@code chList} collection.
	 * 
	 * @param ai
	 *            entity to collect children
	 * @param chList
	 *            list to add child activity entities
	 * @param deepCollect
	 *            indicates if all children of children entities shall be added to returned list (flattened children
	 *            hierarchy)
	 */
	protected static void collectChildren(ActivityInfo ai, Collection<ActivityInfo> chList, boolean deepCollect) {
		if (ai.children == null) {
			return;
		}

		for (List<ActivityInfo> children : ai.children.values()) {
			if (children != null) {
				for (ActivityInfo child : children) {
					chList.add(child);
					if (deepCollect) {
						collectChildren(child, chList, deepCollect);
					}
				}
			}
		}
	}

	/**
	 * Returns list of all child activity entities having no children for this activity info.
	 *
	 * @return list of child activity entities having no children
	 *
	 * @see #collectFinalChildren(ActivityInfo, java.util.Collection)
	 */
	public List<ActivityInfo> getFinalChildren() {
		List<ActivityInfo> chList = new ArrayList<>();

		collectFinalChildren(this, chList);

		return chList;
	}

	/**
	 * Collects provided entity {@code ai} child entities having no children to {@code chList} collection.
	 * 
	 * @param ai
	 *            entity to collect children
	 * @param chList
	 *            list to add child activity entities
	 */
	protected static void collectFinalChildren(ActivityInfo ai, Collection<ActivityInfo> chList) {
		if (ai.children == null) {
			return;
		}

		for (List<ActivityInfo> children : ai.children.values()) {
			if (children != null) {
				for (ActivityInfo child : children) {
					if (child.children == null) {
						chList.add(child);
					} else {
						collectFinalChildren(child, chList);
					}
				}
			}
		}
	}

	/**
	 * Returns root activity entity of this entity.
	 * <p>
	 * Root entity is the one having no parent defined.
	 *
	 * @return root entity of this entity
	 */
	protected ActivityInfo getRootActivity() {
		return parent == null ? this : parent.getRootActivity();
	}

	/**
	 * Returns list of child group activity entities.
	 *
	 * @param groupName
	 *            children group name
	 * @return list of child activity entities
	 */
	public List<ActivityInfo> getChildren(String groupName) {
		return children == null ? null : children.get(groupName);
	}

	/**
	 * Checks whether this activity entity has any child activity entities added.
	 *
	 * @return {@code false} if children list is {@code null} or empty, {@code true} - otherwise
	 */
	public boolean hasChildren() {
		return MapUtils.isNotEmpty(children);
	}

	/**
	 * Checks whether this activity entity has any child activity entities added into defined group.
	 * <p>
	 * When {@code groupName} is null, acts same as {@link #hasChildren()}.
	 *
	 * @param groupName
	 *            children group name
	 * @return {@code false} if group children list is {@code null} or empty, {@code true} - otherwise
	 */
	public boolean hasChildren(String groupName) {
		return hasChildren() && (groupName == null || CollectionUtils.isNotEmpty(children.get(groupName)));
	}

	/**
	 * Returns child activity entities count for defined group.
	 *
	 * @param groupName
	 *            children group name
	 * @return count of group child activity entities, or {@code 0} if there is no child activity entities
	 */
	public int getChildCount(String groupName) {
		if (hasChildren()) {
			List<ActivityInfo> cl = children.get(groupName);

			return cl == null ? 0 : cl.size();
		}

		return 0;
	}

	/**
	 * Sets parent activity entity instance.
	 *
	 * @param parent
	 *            parent activity entity instance
	 * @return instance of this activity data entity
	 */
	protected ActivityInfo setParent(ActivityInfo parent) {
		this.parent = parent;

		return this;
	}

	/**
	 * Sets ordinal index of this data entity within parent entity children entities collection.
	 *
	 * @param ordinalIdx
	 *            ordinal index of this data entity
	 * @return instance of this activity data entity
	 */
	public ActivityInfo setOrdinal(int ordinalIdx) {
		this.ordinalIdx = ordinalIdx;

		return this;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("ActivityInfo{"); // NON-NLS
		sb.append("serverName=").append(Utils.sQuote(serverName)); // NON-NLS
		sb.append(", serverIp=").append(Utils.sQuote(serverIp)); // NON-NLS
		sb.append(", applName=").append(Utils.sQuote(applName)); // NON-NLS
		sb.append(", userName=").append(Utils.sQuote(userName)); // NON-NLS
		sb.append(", resourceName=").append(Utils.sQuote(resourceName)); // NON-NLS
		sb.append(", eventName=").append(Utils.sQuote(eventName)); // NON-NLS
		sb.append(", eventType=").append(eventType); // NON-NLS
		sb.append(", eventStatus=").append(eventStatus); // NON-NLS
		sb.append(", startTime=").append(startTime); // NON-NLS
		sb.append(", endTime=").append(endTime); // NON-NLS
		sb.append(", elapsedTime=").append(elapsedTime); // NON-NLS
		sb.append(", compCode=").append(compCode); // NON-NLS
		sb.append(", reasonCode=").append(reasonCode); // NON-NLS
		sb.append(", exception=").append(Utils.sQuote(exception)); // NON-NLS
		sb.append(", severity=").append(severity); // NON-NLS
		sb.append(", location=").append(Utils.sQuote(location)); // NON-NLS
		sb.append(", correlator=").append(correlator); // NON-NLS
		sb.append(", trackingId=").append(Utils.sQuote(trackingId)); // NON-NLS
		sb.append(", parentId=").append(Utils.sQuote(parentId)); // NON-NLS
		sb.append(", guid=").append(Utils.sQuote(guid)); // NON-NLS
		sb.append(", tag=").append(tag); // NON-NLS
		sb.append(", message=").append(message); // NON-NLS
		sb.append(", msgCharSet=").append(Utils.sQuote(msgCharSet)); // NON-NLS
		sb.append(", msgEncoding=").append(Utils.sQuote(msgEncoding)); // NON-NLS
		sb.append(", msgLength=").append(msgLength); // NON-NLS
		sb.append(", msgMimeType=").append(Utils.sQuote(msgMimeType)); // NON-NLS
		sb.append(", msgAge=").append(msgAge); // NON-NLS
		sb.append(", ttl=").append(ttl); // NON-NLS
		sb.append(", processId=").append(processId); // NON-NLS
		sb.append(", threadId=").append(threadId); // NON-NLS
		sb.append(", category=").append(Utils.sQuote(category)); // NON-NLS
		sb.append(", filteredOut=").append(filteredOut); // NON-NLS
		sb.append(", complete=").append(complete); // NON-NLS
		sb.append(", activityProperties=").append(activityProperties == null ? "NONE" : activityProperties.size());// NON-NLS
		sb.append(", children=").append(children == null ? "NONE" : children.size()); // NON-NLS
		sb.append(", parent=").append(parent == null ? "NONE" : parent); // NON-NLS
		sb.append('}'); // NON-NLS
		return sb.toString();
	}

	/**
	 * Returns this activity field value.
	 * <p>
	 * {@code fieldName} can also be as some expression variable having {@code "${FIELD_NAME}"} format.
	 *
	 * @param fieldName
	 *            field name value to get
	 * @return field contained value
	 *
	 * @see #getFieldValue(String, ActivityInfo...)
	 */
	public Object getFieldValue(String fieldName) {
		return getFieldValue(fieldName, this);
	}

	/**
	 * Returns activity entity field value.
	 * <p>
	 * {@code fieldName} can also be as some expression variable having {@code "${FIELD_NAME}"} format.
	 *
	 * @param fieldName
	 *            field name value to get
	 * @param ais
	 *            set of referred (child-parent) activity entities
	 * @return field contained value
	 *
	 * @see #getFieldValue(String, String, ActivityInfo...)
	 */
	public static Object getFieldValue(String fieldName, ActivityInfo... ais) {
		return getFieldValue(fieldName, null, ais);
	}

	/**
	 * Returns activity field value.
	 * <p>
	 * {@code fieldName} can also be as some expression variable having {@code "${FIELD_NAME}"} format or wildcard mask.
	 *
	 * @param fieldName
	 *            field name value to get
	 * @param groupName
	 *            children group name, actual only then resolving child entity field value
	 * @param ais
	 *            set of referred (child-parent) activity entities
	 * @return field contained value, or {@code null} if field is not found
	 * @throws java.lang.IllegalArgumentException
	 *             if field name does not match expected pattern
	 * 
	 * @see #getParentFieldValue(String, String, ActivityInfo...)
	 * @see #getChildFieldValue(java.util.regex.Matcher, String, String, ActivityInfo...)
	 * @see #getWildcardFieldValue(String, ActivityInfo)
	 */
	public static Object getFieldValue(String fieldName, String groupName, ActivityInfo... ais)
			throws IllegalArgumentException {
		if (ArrayUtils.isEmpty(ais)) {
			return null;
		}

		if (StreamsConstants.isParentEntityRef(fieldName)) {
			return getParentFieldValue(fieldName, groupName, ais);
		}

		Matcher fnMatcher = CHILD_FIELD_PATTERN.matcher(fieldName);
		if (fnMatcher.matches()) {
			return getChildFieldValue(fnMatcher, fieldName, groupName, ais);
		}

		ActivityInfo ai = ais[0];

		if (StreamsConstants.CHILD_ORDINAL_INDEX.equals(fieldName)) {
			return ai.ordinalIdx;
		}

		if (Utils.isWildcardString(fieldName)) {
			return getWildcardFieldValue(fieldName, ai);
		}

		if (fieldName.startsWith(Utils.VAR_EXP_START_TOKEN)) {
			fieldName = Utils.getVarName(fieldName);
		}

		StreamFieldType sft = StreamFieldType._valueOfIgnoreCase(fieldName);
		if (sft != null) {
			switch (sft) {
			case ApplName:
				return ai.applName;
			case Category:
				return ai.category;
			case CompCode:
				return ai.compCode;
			case Correlator:
				return ai.correlator;
			case ElapsedTime:
				return ai.elapsedTime;
			case EndTime:
				return ai.endTime;
			case EventName:
				return ai.eventName;
			case EventStatus:
				return ai.eventStatus;
			case EventType:
				return ai.eventType;
			case Exception:
				return ai.exception;
			case Location:
				return ai.location;
			case Message:
				return ai.message;
			case MsgCharSet:
				return ai.msgCharSet;
			case MsgEncoding:
				return ai.msgEncoding;
			case MsgLength:
				return ai.msgLength;
			case MsgMimeType:
				return ai.msgMimeType;
			case MessageAge:
				return ai.msgAge;
			case TTL:
				return ai.ttl;
			case ParentId:
				return ai.parentId;
			case ProcessId:
				return ai.processId;
			case ReasonCode:
				return ai.reasonCode;
			case ResourceName:
				return ai.resourceName;
			case ServerIp:
				return ai.serverIp;
			case ServerName:
				return ai.serverName;
			case Severity:
				return ai.severity;
			case StartTime:
				return ai.startTime;
			case Tag:
				return ai.tag;
			case ThreadId:
				return ai.threadId;
			case TrackingId:
				return ai.determineTrackingId();
			case UserName:
				return ai.userName;
			case Guid:
				return ai.guid;
			default:
			}
		}

		Property p = ai.activityProperties == null ? null : ai.activityProperties.get(fieldName);

		return p == null ? null : p.getValue();
	}

	/**
	 * Returns parent activity entity field value.
	 * <p>
	 * Field name shall start with {@value StreamsConstants#PARENT_REFERENCE_PREFIX} prefix.
	 * 
	 * @param fieldName
	 *            field name value to get
	 * @param groupName
	 *            children group name, actual only then resolving child entity field value
	 * @param ais
	 *            set of referred (child-parent) activity entities
	 * @return field contained value, or {@code null} if field is not found
	 */
	public static Object getParentFieldValue(String fieldName, String groupName, ActivityInfo... ais) {
		// no activity entities to refer
		if (ArrayUtils.isEmpty(ais)) {
			return null;
		}

		if (ais.length == 1) {
			// no parent entity to refer
			ActivityInfo pActivity = ais[0].parent;
			if (pActivity == null) {
				return null;
			}

			ais = ArrayUtils.add(ais, pActivity);
		}

		ActivityInfo[] pais = Utils.endArray(ais, 1);
		String pFieldName = StreamsConstants.getParentFieldName(fieldName);

		Matcher cFnMatcher = CHILD_FIELD_PATTERN.matcher(pFieldName);
		Object value = getFieldValue(pFieldName, groupName, cFnMatcher.matches() ? ais : pais);

		if (value == null) {
			return getParentFieldValue(fieldName, groupName, pais);
		}

		return value;
	}

	/**
	 * Returns activity child entity field value.
	 * <p>
	 * Field name can be defined using two patterns:
	 * <ul>
	 * <li>{@code 'child[childIndex].fieldName'} - where {@code 'child'} is predefined value to resolve child entity,
	 * {@code 'childIndex'} is child index in group named by {@code defaultGroupName} parameter, {@code 'fieldName'} is
	 * child entity field name</li>
	 * <li>{@code 'child[groupName].fieldName'} - where {@code 'child'} is predefined value to resolve child entity,
	 * {@code 'groupName'} is activity children group name having child identified by {@code ai} defined activity entity
	 * ordinal index attribute, {@code 'fieldName'} is child entity field name</li>
	 * <li>{@code 'child[groupName.childIndex].fieldName'} - where {@code 'child'} is predefined value to resolve child
	 * entity, {@code 'groupName'} is activity children group name, {@code 'childIndex'} is child index in that group,
	 * {@code 'fieldName'} is child entity field name</li>
	 * <li>{@code 'child[groupName.matchExpression].fieldName'} - where {@code 'child'} is predefined value to resolve
	 * child entity, {@code 'groupName'} is activity children group name, {@code 'matchExpression'} is child match
	 * expression in that group (e.g.:fieldName=value) p, {@code 'fieldName'} is child entity field name</li>
	 * </ul>
	 *
	 * @param fnMatcher
	 *            field name RegEx matcher
	 * @param fieldName
	 *            field name value to get
	 * @param defaultGroupName
	 *            default group name, when {@code fieldName} does not define one
	 * @param ais
	 *            set of referred (child-parent) activity entities
	 * @return field contained value, or {@code null} if field is not found
	 * @throws IllegalArgumentException
	 *             if child field name does not match expected pattern or parent activity is not found
	 */
	protected static Object getChildFieldValue(Matcher fnMatcher, String fieldName, String defaultGroupName,
			ActivityInfo... ais) throws IllegalArgumentException {
		String fName = null;
		String groupName = null;
		int chIndex = -1;
		int chtLength = 0;
		String matchExpression = null;

		try {
			String chLocator = fnMatcher.group("child"); // NON-NLS
			fName = fnMatcher.group("field"); // NON-NLS
			String[] chTokens = chLocator == null ? null : chLocator.split(PATH_DELIM);
			chtLength = ArrayUtils.getLength(chTokens);
			if (chtLength == 1) {
				if (StringUtils.isNumeric(chTokens[0])) {
					groupName = defaultGroupName;
					chIndex = Integer.parseInt(chTokens[0]);
				} else {
					if (chTokens[0].contains("=")) { // NON-NLS
						matchExpression = chTokens[0];
					} else {
						groupName = chTokens[0];
						chIndex = ais[0].ordinalIdx > 0 ? ais[0].ordinalIdx - 1 : -1;
					}
				}
			} else if (chtLength > 1) {
				groupName = chTokens[0];
				try {
					chIndex = Integer.parseInt(chTokens[1]);
				} catch (NumberFormatException e) {
					matchExpression = chTokens[1];
				}
			}
		} catch (Exception exc) {
		}

		if (StringUtils.isEmpty(fName) || StringUtils.isEmpty(groupName)
				|| (chIndex < 0 && StringUtils.isEmpty(matchExpression))) {
			throw new IllegalArgumentException(StreamsResources.getStringFormatted(
					StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityInfo.invalid.child.field.locator",
					chtLength == 1 ? "child[childIndex].fieldName or child[groupName].fieldName" // NON-NLS
							: "child[groupName.childIndex].fieldName or child[groupName.matchExpression].fieldName", // NON-NLS
					fieldName));
		}

		ActivityInfo ai = ais.length > 1 ? ais[1] : null;

		if (ai == null) {
			LOGGER.log(OpLevel.TRACE, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ActivityInfo.child.field.locator.parent.not.found"), fieldName);
		}

		Map<String, List<ActivityInfo>> childMap = ai == null ? null : ai.children;
		List<ActivityInfo> children = childMap == null ? null : childMap.get(groupName);
		ActivityInfo child = null;

		if (chIndex >= 0) {
			if (children == null || chIndex >= children.size()) {
				LOGGER.log(OpLevel.TRACE,
						StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
								"ActivityInfo.child.field.locator.children.bounds"),
						groupName, chIndex, fieldName, children == null ? null : children.size());
			} else {
				child = children.get(chIndex);
			}
		} else {
			if (children != null && StringUtils.isNotEmpty(matchExpression)) {
				String[] varTokens = matchExpression.split(KV_DELIM);
				if (varTokens.length > 1) {
					for (ActivityInfo c : children) {
						Object cfv = c.getFieldValue(varTokens[0]);
						if (varTokens[1].equals(Utils.toString(cfv))) {
							child = c;
							break;
						}
					}
				}
			}
		}

		return child == null ? null : child.getFieldValue(fName);
	}

	/**
	 * Returns activity entity field(s) value(s) map. Field names are matched against provided {@code fieldName}
	 * wildcard mask.
	 * 
	 * @param fieldName
	 *            field name wildcard mask
	 * @param ai
	 *            activity entity to collect fields values from
	 * @return fields values map
	 */
	protected static Map<String, Object> getWildcardFieldValue(String fieldName, ActivityInfo ai) {
		Map<String, Object> valuesMap = new HashMap<>();
		Pattern mp = Pattern.compile(Utils.wildcardToRegex(fieldName));

		if (mp.matcher(StreamFieldType.ApplName.name()).matches()) {
			valuesMap.put(StreamFieldType.ApplName.name(), ai.applName);
		}
		if (mp.matcher(StreamFieldType.Category.name()).matches()) {
			valuesMap.put(StreamFieldType.Category.name(), ai.category);
		}
		if (mp.matcher(StreamFieldType.CompCode.name()).matches()) {
			valuesMap.put(StreamFieldType.CompCode.name(), ai.compCode);
		}
		if (mp.matcher(StreamFieldType.Correlator.name()).matches()) {
			valuesMap.put(StreamFieldType.Correlator.name(), ai.correlator);
		}
		if (mp.matcher(StreamFieldType.ElapsedTime.name()).matches()) {
			valuesMap.put(StreamFieldType.ElapsedTime.name(), ai.elapsedTime);
		}
		if (mp.matcher(StreamFieldType.EndTime.name()).matches()) {
			valuesMap.put(StreamFieldType.EndTime.name(), ai.endTime);
		}
		if (mp.matcher(StreamFieldType.EventName.name()).matches()) {
			valuesMap.put(StreamFieldType.EventName.name(), ai.eventName);
		}
		if (mp.matcher(StreamFieldType.EventStatus.name()).matches()) {
			valuesMap.put(StreamFieldType.EventStatus.name(), ai.eventStatus);
		}
		if (mp.matcher(StreamFieldType.EventType.name()).matches()) {
			valuesMap.put(StreamFieldType.EventType.name(), ai.eventType);
		}
		if (mp.matcher(StreamFieldType.Exception.name()).matches()) {
			valuesMap.put(StreamFieldType.Exception.name(), ai.exception);
		}
		if (mp.matcher(StreamFieldType.Location.name()).matches()) {
			valuesMap.put(StreamFieldType.Location.name(), ai.location);
		}
		if (mp.matcher(StreamFieldType.Message.name()).matches()) {
			valuesMap.put(StreamFieldType.Message.name(), ai.message);
		}
		if (mp.matcher(StreamFieldType.MsgCharSet.name()).matches()) {
			valuesMap.put(StreamFieldType.MsgCharSet.name(), ai.msgCharSet);
		}
		if (mp.matcher(StreamFieldType.MsgEncoding.name()).matches()) {
			valuesMap.put(StreamFieldType.MsgEncoding.name(), ai.msgEncoding);
		}
		if (mp.matcher(StreamFieldType.MsgLength.name()).matches()) {
			valuesMap.put(StreamFieldType.MsgLength.name(), ai.msgLength);
		}
		if (mp.matcher(StreamFieldType.MsgMimeType.name()).matches()) {
			valuesMap.put(StreamFieldType.MsgMimeType.name(), ai.msgMimeType);
		}
		if (mp.matcher(StreamFieldType.MessageAge.name()).matches()) {
			valuesMap.put(StreamFieldType.MessageAge.name(), ai.msgAge);
		}
		if (mp.matcher(StreamFieldType.TTL.name()).matches()) {
			valuesMap.put(StreamFieldType.TTL.name(), ai.ttl);
		}
		if (mp.matcher(StreamFieldType.ParentId.name()).matches()) {
			valuesMap.put(StreamFieldType.ParentId.name(), ai.parentId);
		}
		if (mp.matcher(StreamFieldType.ProcessId.name()).matches()) {
			valuesMap.put(StreamFieldType.ProcessId.name(), ai.processId);
		}
		if (mp.matcher(StreamFieldType.ReasonCode.name()).matches()) {
			valuesMap.put(StreamFieldType.ReasonCode.name(), ai.reasonCode);
		}
		if (mp.matcher(StreamFieldType.ResourceName.name()).matches()) {
			valuesMap.put(StreamFieldType.ResourceName.name(), ai.resourceName);
		}
		if (mp.matcher(StreamFieldType.ServerIp.name()).matches()) {
			valuesMap.put(StreamFieldType.ServerIp.name(), ai.serverIp);
		}
		if (mp.matcher(StreamFieldType.ServerName.name()).matches()) {
			valuesMap.put(StreamFieldType.ServerName.name(), ai.serverName);
		}
		if (mp.matcher(StreamFieldType.Severity.name()).matches()) {
			valuesMap.put(StreamFieldType.Severity.name(), ai.severity);
		}
		if (mp.matcher(StreamFieldType.StartTime.name()).matches()) {
			valuesMap.put(StreamFieldType.StartTime.name(), ai.startTime);
		}
		if (mp.matcher(StreamFieldType.Tag.name()).matches()) {
			valuesMap.put(StreamFieldType.Tag.name(), ai.tag);
		}
		if (mp.matcher(StreamFieldType.ThreadId.name()).matches()) {
			valuesMap.put(StreamFieldType.ThreadId.name(), ai.threadId);
		}
		if (mp.matcher(StreamFieldType.TrackingId.name()).matches()) {
			valuesMap.put(StreamFieldType.TrackingId.name(), ai.determineTrackingId());
		}
		if (mp.matcher(StreamFieldType.UserName.name()).matches()) {
			valuesMap.put(StreamFieldType.UserName.name(), ai.userName);
		}
		if (mp.matcher(StreamFieldType.Guid.name()).matches()) {
			valuesMap.put(StreamFieldType.Guid.name(), ai.guid);
		}

		if (ai.activityProperties != null) {
			for (Map.Entry<String, Property> pe : ai.activityProperties.entrySet()) {
				if (mp.matcher(pe.getKey()).matches()) {
					valuesMap.put(pe.getKey(), pe.getValue().getValue());
				}
			}
		}

		return valuesMap;
	}
}
