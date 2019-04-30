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

import com.jkoolcloud.tnt4j.core.*;
import com.jkoolcloud.tnt4j.format.JSONFormatter;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
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
 * This class represents an {@link com.jkoolcloud.tnt4j.core.Trackable} entity (e.g. activity/event/snapshot) to record
 * to jKoolCloud.
 *
 * @version $Revision: 3 $
 */
public class ActivityInfo {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivityInfo.class);

	private static final Pattern CHILD_FIELD_PATTERN = Pattern.compile("child\\[(?<child>\\S+)\\]\\.(?<field>\\S+)"); // NON-NLS

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

	private static final TimeTracker ACTIVITY_TIME_TRACKER = TimeTracker.newTracker(1000, TimeUnit.HOURS.toMillis(8));

	private Map<String, Property> activityProperties;
	private Map<String, List<ActivityInfo>> children;
	private ActivityInfo parent;

	/**
	 * Constructs a new ActivityInfo object.
	 */
	public ActivityInfo() {
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
			addActivityProperty(field.getFieldTypeName(), getPropertyValue(value, field), true);
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
				addCorrelator(Utils.getTags(fieldValue));
				// addCorrelator(Utils.getTags(formatTagsObject(fieldValue, field)));
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
				addTag(Utils.getTags(fieldValue));
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
			addActivityProperty(field.getFieldTypeName(), getPropertyValue(fieldValue, field), field.getValueType());
		}
	}

	private void addPropertiesMap(ActivityField field, Map<?, ?> pMap, String propPrefix) throws ParseException {
		for (Map.Entry<?, ?> pme : pMap.entrySet()) {
			String pKey = propPrefix + String.valueOf(pme.getKey());

			if (pme.getValue() instanceof Map) {
				addPropertiesMap(field, (Map<?, ?>) pme.getValue(),
						pKey + field.getParser().getProperty(ParserProperties.PROP_COMPOSITE_DELIM));
			} else {
				addActivityProperty(pKey, getPropertyValue(pme.getValue(), field));
			}
		}
	}

	private static Object getPropertyValue(Object fieldValue, ActivityField field) throws ParseException {
		ActivityFieldLocator fmLocator = field.getMasterLocator();

		if (fmLocator != null) {
			switch (fmLocator.getDataType()) {
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
			Boolean b = getBooleanValue(fieldValue);
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
			TimeUnit units = fmLocator == null ? TimeUnit.MICROSECONDS : fmLocator.getBuiltInUnits();
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
		if (value instanceof Object[]) {
			return formatValuesArray((Object[]) value, field);
		} else if (value instanceof byte[]) {
			return Utils.encodeHex((byte[]) value);
		} else if (value != null && value.getClass().isArray()) {
			return ArrayUtils.toString(value);
		}

		return Utils.toString(value);
	}

	private static String formatValuesArray(Object[] vArray, ActivityField field) {
		if (StringUtils.isNotEmpty(field.getFormattingPattern())) {
			return formatArrayPattern(field.getFormattingPattern(), vArray);
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

	private static String formatArrayPattern(String pattern, Object[] vArray) {
		MessageFormat mf = new MessageFormat(pattern);

		try {
			Field f = mf.getClass().getDeclaredField("maxOffset");
			f.setAccessible(true);
			int maxOffset = f.getInt(mf);
			if (maxOffset >= 0) {
				f = mf.getClass().getDeclaredField("argumentNumbers");
				f.setAccessible(true);
				int[] ana = (int[]) f.get(mf);
				int maxIndex = ana[maxOffset];

				if (maxIndex >= vArray.length) {
					LOGGER.log(OpLevel.WARNING, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"ActivityInfo.formatting.arguments.mismatch", pattern, maxIndex,
							ArrayUtils.getLength(vArray));
				}
			}
		} catch (Exception exc) {
		}

		return mf.format(vArray);
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
				collection = new ArrayList<>();
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
	 *
	 * @return fully qualified name of this activity source, or {@code null} if no source defining attributes where
	 *         parsed from stream.
	 */
	public String getSourceFQN(String pattern) {
		Collection<String> fqnTokens = getFQNTokens(pattern);

		StringBuilder fqnB = new StringBuilder();

		for (String fqnT : fqnTokens) {
			String[] pair = fqnT.split("=");
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
	 * {@link com.jkoolcloud.tnt4j.tracker.TrackingEvent} or {@link com.jkoolcloud.tnt4j.core.PropertySnapshot} using
	 * the specified tracker for this activity data entity to be sent to jKoolCloud.
	 *
	 * @param tracker
	 *            {@link com.jkoolcloud.tnt4j.tracker.Tracker} instance to be used to build
	 *            {@link com.jkoolcloud.tnt4j.core.Trackable} activity data package
	 * @param chTrackables
	 *            collection to add built child trackables, not included into parent trackable and transmitted
	 *            separately, e.g., activity child events
	 *
	 * @return trackable instance made from this activity entity data
	 * @throws IllegalArgumentException
	 *             if {@code tracker} is null
	 * @see com.jkoolcloud.tnt4j.streams.outputs.JKCloudActivityOutput#logItem(ActivityInfo)
	 */
	public Trackable buildTrackable(Tracker tracker, Map<Trackable, ActivityInfo> chTrackables) {
		if (tracker == null) {
			throw new IllegalArgumentException(
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityInfo.tracker.null"));
		}

		determineTimes();
		resolveServer(false);
		determineTrackingId();

		if (eventType == OpType.ACTIVITY) {
			return buildActivity(tracker, eventName, trackingId, chTrackables);
		} else if (eventType == OpType.SNAPSHOT) {
			return buildSnapshot(tracker, eventName, trackingId);
		} else {
			return buildEvent(tracker, eventName, trackingId, chTrackables);
		}
	}

	/**
	 * Assigns new activity entity tracking identifier value if not yet defined.
	 *
	 * @return the tracking identifier
	 */
	public String determineTrackingId() {
		if (StringUtils.isEmpty(trackingId)) {
			trackingId = DefaultUUIDFactory.getInstance().newUUID();
		}
		return trackingId;
	}

	/**
	 * Creates the appropriate data package {@link com.jkoolcloud.tnt4j.tracker.TrackingActivity},
	 * {@link com.jkoolcloud.tnt4j.tracker.TrackingEvent} or {@link com.jkoolcloud.tnt4j.core.PropertySnapshot} using
	 * the specified tracker for this activity data entity to be sent to jKoolCloud.
	 * <p>
	 * Does same as {@link #buildTrackable(com.jkoolcloud.tnt4j.tracker.Tracker, java.util.Map)} where
	 * {@code chTrackables} list is {@code null}.
	 *
	 * @param tracker
	 *            {@link com.jkoolcloud.tnt4j.tracker.Tracker} instance to be used to build
	 *            {@link com.jkoolcloud.tnt4j.core.Trackable} activity data package
	 * 
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
	 * @param chTrackables
	 *            collection to add built child trackables, not included into parent event and transmitted separately
	 * @return tracking event instance
	 */
	protected TrackingEvent buildEvent(Tracker tracker, String trackName, String trackId,
			Map<Trackable, ActivityInfo> chTrackables) {
		if (StringUtils.isEmpty(trackName)) {
			trackName = "_UNNAMED_EVENT_"; // NON-NLS
			LOGGER.log(OpLevel.WARNING, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityInfo.activity.has.no.name", TrackingEvent.class.getSimpleName(), trackName);
		}

		TrackingEvent event = tracker.newEvent(severity == null ? OpLevel.INFO : severity, trackName, (String) null,
				(String) null, (Object[]) null);
		event.setTrackingId(trackId);
		event.setParentId(parentId);
		if (StringUtils.isNotEmpty(guid)) {
			event.setGUID(guid);
		}
		// event.setCorrelator(CollectionUtils.isEmpty(correlator) ? Collections.singletonList(trackId) : correlator);
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
			buildChildren(tracker, trackId, event, chTrackables);
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
	 * @param chTrackables
	 *            collection to add built child trackables, not included into parent activity and transmitted separately
	 * @return tracking activity instance
	 */
	protected TrackingActivity buildActivity(Tracker tracker, String trackName, String trackId,
			Map<Trackable, ActivityInfo> chTrackables) {
		if (StringUtils.isEmpty(trackName)) {
			trackName = "_UNNAMED_ACTIVITY_"; // NON-NLS
			LOGGER.log(OpLevel.WARNING, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityInfo.activity.has.no.name", TrackingActivity.class.getSimpleName(), trackName);
		}

		TrackingActivity activity = tracker.newActivity(severity == null ? OpLevel.INFO : severity, trackName);
		activity.setTrackingId(trackId);
		activity.setParentId(parentId);
		if (StringUtils.isNotEmpty(guid)) {
			activity.setGUID(guid);
		}
		// activity.setCorrelator(CollectionUtils.isEmpty(correlator) ? Collections.singletonList(trackId) :
		// correlator);
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
			buildChildren(tracker, trackId, activity, chTrackables);
		}

		return activity;
	}

	private void buildChildren(Tracker tracker, String trackId, Trackable pTrackable,
			Map<Trackable, ActivityInfo> chTrackables) {
		List<ActivityInfo> cais = getChildren();
		for (ActivityInfo child : cais) {
			if (child.filteredOut) {
				LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityInfo.filtered.child", child);
				continue;
			}

			Trackable cTrackable = buildChild(tracker, child, trackId);
			boolean consumed = addTrackableChild(pTrackable, cTrackable);

			if (!consumed && chTrackables != null) {
				chTrackables.put(cTrackable, child);
			}
		}
	}

	/**
	 * Builds child entity trackables as split relatives by merging this (parent) activity entity data into child
	 * entity.
	 * <p>
	 * If no child relatives are available, only this (parent) activity build trackable is added to {@code chTrackables}
	 * collation.
	 * 
	 * @param tracker
	 *            communication gateway to use to record activity
	 * @param chTrackables
	 *            collection to add built child trackables
	 * 
	 * @see #merge(ActivityInfo)
	 */
	public void buildSplitRelatives(Tracker tracker, Map<Trackable, ActivityInfo> chTrackables) {
		List<ActivityInfo> cais = getChildren();
		for (ActivityInfo child : cais) {
			if (child.filteredOut) {
				LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityInfo.filtered.child", child);
				continue;
			}

			child.merge(this);
			Trackable t = child.buildTrackable(tracker);
			if (chTrackables != null) {
				chTrackables.put(t, child);
			}
		}

		if (chTrackables != null && chTrackables.isEmpty()) {
			Trackable t = buildTrackable(tracker);
			chTrackables.put(t, this);
		}
	}

	private static boolean addTrackableChild(Trackable pTrackable, Trackable chTrackable) {
		if (pTrackable != null && chTrackable != null) {
			if (pTrackable instanceof TrackingEvent) {
				TrackingEvent pEvent = (TrackingEvent) pTrackable;
				if (chTrackable instanceof Snapshot) {
					Snapshot chSnapshot = (Snapshot) chTrackable;
					pEvent.getOperation().addSnapshot(chSnapshot);

					return true;
				}
			} else if (pTrackable instanceof Activity) {
				Activity pActivity = (Activity) pTrackable;
				pActivity.add(chTrackable);

				return chTrackable instanceof Snapshot;
			}

			LOGGER.log(OpLevel.WARNING, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityInfo.invalid.child", resolveTrackableType(chTrackable), resolveTrackableType(pTrackable),
					resolveChildTypesFor(pTrackable), chTrackable.getTrackingId(), pTrackable.getTrackingId());
		}

		return false;
	}

	private static String resolveTrackableType(Trackable trackable) {
		if (trackable instanceof Activity) {
			return OpType.ACTIVITY.name();
		} else if (trackable instanceof TrackingEvent) {
			return OpType.EVENT.name();
		} else if (trackable instanceof Snapshot) {
			return OpType.SNAPSHOT.name();
		} else {
			return trackable == null ? null : trackable.getClass().getName();
		}
	}

	private static String resolveChildTypesFor(Trackable trackable) {
		if (trackable instanceof Activity) {
			return "ACTIVITY, EVENT, SNAPSHOT"; // NON-NLS
		} else if (trackable instanceof TrackingEvent) {
			return "SNAPSHOT"; // NON-NLS
		} else if (trackable instanceof Snapshot) {
			return "NONE"; // NON-NLS
		} else {
			return "UNKNOWN"; // NON-NLS
		}
	}

	private static Trackable buildChild(Tracker tracker, ActivityInfo child, String parentId) {
		child.parentId = parentId;

		// child.resolveServer(false);
		// child.determineTimes();

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
		if (StringUtils.isEmpty(trackName)) {
			trackName = "_UNNAMED_SNAPSHOT_"; // NON-NLS
			LOGGER.log(OpLevel.WARNING, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityInfo.activity.has.no.name", PropertySnapshot.class.getSimpleName(), trackName);
		}

		PropertySnapshot snapshot = category != null ? (PropertySnapshot) tracker.newSnapshot(category, trackName)
				: (PropertySnapshot) tracker.newSnapshot(trackName);
		snapshot.setTrackingId(trackId);
		snapshot.setParentId(parentId);
		if (StringUtils.isNotEmpty(guid)) {
			snapshot.setGUID(guid);
		}
		snapshot.setSeverity(severity == null ? OpLevel.INFO : severity);
		// snapshot.setCorrelator(CollectionUtils.isEmpty(correlator) ? Collections.singletonList(trackId) :
		// correlator);
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

		return snapshot;
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
				endTime.add(0L, elapsedTime);
			} else {
				endTime = UsecTimestamp.now();
			}
		}
		if (startTime == null) {
			startTime = new UsecTimestamp(endTime);
			startTime.subtract(0L, elapsedTime);
		}
	}

	private static Number getNumberValue(Object value, ActivityField field) throws ParseException {
		if (value instanceof Number) {
			return (Number) value;
		}

		String valStr = Utils.toString(value);
		valStr = StringUtils.trim(valStr);

		ActivityFieldLocator fmLocator = field.getMasterLocator();

		return fmLocator == null ? NumericFormatter.strToNumber(valStr) : fmLocator.formatNumericValue(value);
	}

	private static <T extends Number> T getNumberValue(Object value, Class<T> clazz, ActivityField field)
			throws ParseException {
		Number num = getNumberValue(value, field);

		return NumericFormatter.castNumber(num, clazz);
	}

	private static Boolean getBooleanValue(Object value) {
		return value instanceof Boolean ? (Boolean) value : Utils.getBoolean(value);
	}

	/**
	 * Merges activity info data fields values. Values of fields are changed only if they currently hold default
	 * (initial) value.
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
		if (eventType == null) {
			eventType = otherAi.eventType;
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
				correlator = new ArrayList<>();
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
				tag = new ArrayList<>();
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

		if (otherAi.activityProperties != null) {
			if (activityProperties == null) {
				activityProperties = new HashMap<>();
			}

			activityProperties.putAll(otherAi.activityProperties);
		}
	}

	/**
	 * Merges activity info data fields values and child activity entities. Values of fields are changed only if they
	 * currently hold default (initial) value.
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

			children.putAll(otherAi.children);
			parent = otherAi.parent;
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
	 *            {@code true} if activity is filtered out, {@code false} otherwise
	 */
	public void setFiltered(boolean filteredOut) {
		this.filteredOut = filteredOut;
	}

	/**
	 * Adds child activity entity data package.
	 *
	 * @param ai
	 *            activity entity object containing child data
	 * @param groupName
	 *            children group name (e.g. parser name)
	 */
	public void addChild(String groupName, ActivityInfo ai) {
		if (children == null) {
			children = new LinkedHashMap<>();
		}

		ai.setParent(this);

		List<ActivityInfo> chList = children.computeIfAbsent(groupName, k -> new ArrayList<>());
		chList.add(ai);
	}

	/**
	 * Returns list of all child activity entities.
	 *
	 * @return list of child activity entities
	 */
	public List<ActivityInfo> getChildren() {
		if (children == null) {
			return null;
		}

		List<ActivityInfo> chList = new ArrayList<>();

		for (List<ActivityInfo> childs : children.values()) {
			if (childs != null) {
				chList.addAll(childs);
			}
		}

		return chList;
	}

	/**
	 * Returns list of child group activity entities.
	 *
	 * @param groupName
	 *            children group name
	 *
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
	 * Sets parent activity entity instance.
	 *
	 * @param parent
	 *            parent activity entity instance
	 */
	protected void setParent(ActivityInfo parent) {
		this.parent = parent;
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
		sb.append(", activityProperties=").append(activityProperties == null ? "NONE" : activityProperties.size());// NON-NLS
		sb.append(", children=").append(children == null ? "NONE" : children.size()); // NON-NLS
		sb.append(", parent=").append(parent == null ? "NONE" : parent); // NON-NLS
		sb.append('}'); // NON-NLS
		return sb.toString();
	}

	/**
	 * Returns activity field value.
	 * <p>
	 * {@code fieldName} can also be as some expression variable having {@code "${FIELD_NAME}"} format.
	 * 
	 * @param fieldName
	 *            field name value to get
	 * @return field contained value
	 *
	 * @see #getFieldValue(String, String)
	 */
	public Object getFieldValue(String fieldName) {
		return getFieldValue(fieldName, null);
	}

	/**
	 * Returns activity field value.
	 * <p>
	 * {@code fieldName} can also be as some expression variable having {@code "${FIELD_NAME}"} format.
	 *
	 * @param fieldName
	 *            field name value to get
	 * @param groupName
	 *            children group name, actual only then resolving child entity field value
	 * @return field contained value, or {@code null} if field is not found
	 * @throws java.lang.IllegalArgumentException
	 *             if field name does not match expected pattern
	 * 
	 * @see #getChildFieldValue(java.util.regex.Matcher, String, String)
	 */
	public Object getFieldValue(String fieldName, String groupName) throws IllegalArgumentException {
		try {
			if (StreamsConstants.isParentEntityRef(fieldName)) {
				return parent == null ? null : parent.getFieldValue(StreamsConstants.getParentFieldName(fieldName));
			}

			Matcher fnMatcher = CHILD_FIELD_PATTERN.matcher(fieldName);
			if (fnMatcher.matches()) {
				return getChildFieldValue(fnMatcher, fieldName, groupName);
			}

			if (fieldName.startsWith(Utils.VAR_EXP_START_TOKEN)) {
				fieldName = Utils.getVarName(fieldName);
			}

			StreamFieldType sft = Utils.valueOfIgnoreCase(StreamFieldType.class, fieldName);
			switch (sft) {
			case ApplName:
				return applName;
			case Category:
				return category;
			case CompCode:
				return compCode;
			case Correlator:
				return correlator;
			case ElapsedTime:
				return elapsedTime;
			case EndTime:
				return endTime;
			case EventName:
				return eventName;
			case EventStatus:
				return eventStatus;
			case EventType:
				return eventType;
			case Exception:
				return exception;
			case Location:
				return location;
			case Message:
				return message;
			case MsgCharSet:
				return msgCharSet;
			case MsgEncoding:
				return msgEncoding;
			case MsgLength:
				return msgLength;
			case MsgMimeType:
				return msgMimeType;
			case MessageAge:
				return msgAge;
			case TTL:
				return ttl;
			case ParentId:
				return parentId;
			case ProcessId:
				return processId;
			case ReasonCode:
				return reasonCode;
			case ResourceName:
				return resourceName;
			case ServerIp:
				return serverIp;
			case ServerName:
				return serverName;
			case Severity:
				return severity;
			case StartTime:
				return startTime;
			case Tag:
				return tag;
			case ThreadId:
				return threadId;
			case TrackingId:
				return determineTrackingId();
			case UserName:
				return userName;
			case Guid:
				return guid;
			default:
				throw new IllegalArgumentException(StreamsResources.getStringFormatted(
						StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityInfo.unrecognized.field", fieldName));
			}
		} catch (IllegalArgumentException exc) {
			Property p = activityProperties == null ? null : activityProperties.get(fieldName);

			return p == null ? null : p.getValue();
		}
	}

	/**
	 * Returns activity child entity field value.
	 * <p>
	 * Field name can be defined using two patterns:
	 * <ul>
	 * <li>{@code 'child[childIndex].fieldName'} - where {@code 'child'} is predefined value to resolve child entity,
	 * {@code 'childIndex'} is child index in group named by {@code defaultGroupName} parameter, {@code 'fieldName'} is
	 * child entity field name</li>
	 * <li>{@code 'child[groupName.childIndex].fieldName'} - where {@code 'child'} is predefined value to resolve child
	 * entity, {@code 'groupName'} is activity children group name, {@code 'childIndex'} is child index in that group,
	 * {@code 'fieldName'} is child entity field name</li>
	 * </ul>
	 *
	 * @param fnMatcher
	 *            field name RegEx matcher
	 * @param fieldName
	 *            field name value to get
	 * @param defaultGroupName
	 *            default group name, when {@code fieldName} does not define one
	 * @return field contained value, or {@code null} if field is not found
	 * @throws IllegalArgumentException
	 *             if child field name does not match expected pattern
	 */
	protected Object getChildFieldValue(Matcher fnMatcher, String fieldName, String defaultGroupName)
			throws IllegalArgumentException {
		String fName = null;
		String groupName = null;
		int chIndex = -1;
		int chtLength = 0;

		try {
			String chLocator = fnMatcher.group("child"); // NON-NLS
			fName = fnMatcher.group("field"); // NON-NLS
			String[] chTokens = chLocator == null ? null : chLocator.split("\\."); // NON-NLS
			chtLength = ArrayUtils.getLength(chTokens);
			if (chtLength == 1) {
				groupName = defaultGroupName;
				chIndex = Integer.parseInt(chTokens[0]);
			} else if (chtLength > 1) {
				groupName = chTokens[0];
				chIndex = Integer.parseInt(chTokens[1]);
			}
		} catch (Exception exc) {
		}

		if (StringUtils.isEmpty(fName) || StringUtils.isEmpty(groupName) || chIndex < 0) {
			throw new IllegalArgumentException(StreamsResources.getStringFormatted(
					StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityInfo.invalid.child.field.locator",
					chtLength == 1 ? "child[childIndex].fieldName" : "child[groupName.childIndex].fieldName", // NON-NLS
					fieldName));
		}

		List<ActivityInfo> childs = children == null ? null : children.get(groupName);
		return childs == null || chIndex >= childs.size() ? null : childs.get(chIndex).getFieldValue(fName);
	}
}
