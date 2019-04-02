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

import java.io.*;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.*;
import com.jkoolcloud.tnt4j.streams.filters.StreamFiltersGroup;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.preparsers.ActivityDataPreParser;
import com.jkoolcloud.tnt4j.streams.transform.ValueTransformation;
import com.jkoolcloud.tnt4j.streams.utils.*;

/**
 * Generic class for common activity parsers. It provides some generic functionality witch is common to most activity
 * parsers.
 * <p>
 * This parser supports the following configuration properties:
 * <ul>
 * <li>UseActivityDataAsMessageForUnset - flag indicating weather RAW activity data shall be put into field 'Message' if
 * there is no mapping defined for that field in stream parser configuration or value was not resolved by parser from
 * RAW activity data. NOTE: it is recommended to use it for DEBUGGING purposes only. For a production version of your
 * software, remove this property form stream parser configuration. Default value - '{@code false}'. (Optional)</li>
 * <li>ActivityDelim - defines activities delimiter symbol used by parsers. Value can be one of: {@code "EOL"} - end of
 * line, or {@code "EOF"} - end of file/stream. Default value - '{@code EOL}'. (Optional)</li>
 * <li>RequireDefault - indicates that all parser fields/locators by default requires to resolve non-null value. Default
 * value - {@code false}. (Optional)</li>
 * <li>AutoArrangeFields - flag indicating parser fields shall be automatically ordered by parser to ensure references
 * sequence. When {@code false}, fields shall maintain user parser configuration defined order. NOTE: it is alias for
 * parser configuration attribute {@code "manualFieldsOrder"}. Default value - {@code true}. (Optional)</li>
 * </ul>
 *
 * @param <T>
 *            the type of handled activity data
 * @version $Revision: 2 $
 */
public abstract class GenericActivityParser<T> extends ActivityParser {

	/**
	 * Constant for default values delimiter symbol used by parsers.
	 */
	protected static final String DEFAULT_DELIM = ","; // NON-NLS

	/**
	 * Constant defining locator placeholder {@value}, used to resolve complete activity data package. It is useful to
	 * redirect complete activity data to stacked parser.
	 */
	protected static final String LOC_FOR_COMPLETE_ACTIVITY_DATA = "$DATA$"; // NON-NLS

	/**
	 * List of supported activity fields used to extract values from RAW activity data defined by field location(s).
	 */
	protected final List<ActivityField> fieldList = new ArrayList<>();

	/**
	 * Flag indicating weather RAW activity data shall be put into field 'Message' if there is no mapping defined for
	 * that field in stream parser configuration or value was not resolved by parser from RAW activity data.
	 */
	protected boolean useActivityAsMessage = false;

	/**
	 * Parameter defining activities delimiter symbol used by parsers. Default value is {@code "EOL"} - end of line.
	 * Also can be {@code "EOF"} - end of file/stream.
	 */
	protected String activityDelim = ActivityDelim.EOL.name();

	/**
	 * Property defining activities composite data (like map) field path tokens delimiter. Default value is
	 * {@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#MAP_PROP_NAME_TOKENS_DELIM}. When streaming to
	 * AutoPilot it is recommended to use {@code "\"} delimiter.
	 */
	protected String compositeDelim = StreamsConstants.MAP_PROP_NAME_TOKENS_DELIM;

	/**
	 * Property indicating that all attributes are required by default.
	 */
	protected boolean requireAll = false;

	private StreamFiltersGroup<ActivityInfo> activityFilter;

	private List<ActivityDataPreParser<Object, Object>> preParsers;

	protected final Lock nextLock = new ReentrantLock();
	protected final Lock filterLock = new ReentrantLock();
	protected final Lock preParserLock = new ReentrantLock();

	private boolean autoArrangeFields = true;
	private ActivityField parentIdField;

	/**
	 * Constructs a new GenericActivityParser.
	 */
	protected GenericActivityParser() {
		super();
	}

	/**
	 * Constructs a new GenericActivityParser.
	 *
	 * @param defaultDataType
	 *            default data type to be used by parser bound fields/locators
	 */
	protected GenericActivityParser(ActivityFieldDataType defaultDataType) {
		setDefaultDataType(defaultDataType);
	}

	@Override
	public void setProperty(String name, String value) {
		if (ParserProperties.PROP_USE_ACTIVITY_DATA_AS_MESSAGE_FOR_UNSET.equalsIgnoreCase(name)) {
			useActivityAsMessage = Utils.toBoolean(value);

			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityParser.setting", name, value);
		} else if (ParserProperties.PROP_ACTIVITY_DELIM.equalsIgnoreCase(name)) {
			if (StringUtils.isNotEmpty(value)) {
				activityDelim = value;

				logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityParser.setting", name, value);
			}
		} else if (ParserProperties.PROP_REQUIRE_ALL.equalsIgnoreCase(name)) {
			if (StringUtils.isNotEmpty(value)) {
				requireAll = Utils.toBoolean(value);
				logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityParser.setting", name, value);
			}
		} else if (ParserProperties.PROP_COMPOSITE_DELIM.equalsIgnoreCase(name)) {
			if (StringUtils.isNotEmpty(value)) {
				compositeDelim = value;
				logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityParser.setting", name, value);
			}
		} else if (ParserProperties.PROP_AUTO_ARRANGE_FIELDS.equalsIgnoreCase(name)) {
			if (StringUtils.isNotEmpty(value)) {
				autoArrangeFields = Utils.toBoolean(value);
				logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityParser.setting", name, value);
			}
		}
	}

	@Override
	public Object getProperty(String name) {
		if (ParserProperties.PROP_USE_ACTIVITY_DATA_AS_MESSAGE_FOR_UNSET.equalsIgnoreCase(name)) {
			return useActivityAsMessage;
		}
		if (ParserProperties.PROP_ACTIVITY_DELIM.equalsIgnoreCase(name)) {
			return activityDelim;
		}
		if (ParserProperties.PROP_REQUIRE_ALL.equalsIgnoreCase(name)) {
			return requireAll;
		}
		if (ParserProperties.PROP_COMPOSITE_DELIM.equalsIgnoreCase(name)) {
			return compositeDelim;
		}
		if (ParserProperties.PROP_AUTO_ARRANGE_FIELDS.equalsIgnoreCase(name)) {
			return autoArrangeFields;
		}

		return null;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see #isDataClassSupportedByParser(Object)
	 * @see #isDataClassSupportedByPreParser(Object)
	 */
	@Override
	public boolean isDataClassSupported(Object data) {
		return isDataClassSupportedByParser(data) || isDataClassSupportedByPreParser(data);
	}

	/**
	 * Returns whether this parser supports the given format of the activity data. This is used by activity streams to
	 * determine if the parser can parse the data in the format that the stream has it.
	 * <p>
	 * This parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link java.lang.String}</li>
	 * <li>{@code byte[]}</li>
	 * <li>{@link java.nio.ByteBuffer}</li>
	 * <li>{@link java.io.Reader}</li>
	 * <li>{@link java.io.InputStream}</li>
	 * </ul>
	 *
	 * @param data
	 *            data object whose class is to be verified
	 * @return {@code true} if this parser can process data in the specified format, {@code false} - otherwise
	 */
	protected boolean isDataClassSupportedByParser(Object data) {
		return String.class.isInstance(data) || byte[].class.isInstance(data) || ByteBuffer.class.isInstance(data)
				|| Reader.class.isInstance(data) || InputStream.class.isInstance(data);
	}

	/**
	 * Returns whether this parser pre-parsers supports the given format of the RAW activity data. This is used by
	 * activity streams to determine if the parser pre-parses can parse the data in the format that the stream has it.
	 *
	 * @param data
	 *            data object whose class is to be verified
	 * @return {@code true} if this parser pre-parsers can process data in the specified format, {@code false} -
	 *         otherwise
	 */
	protected boolean isDataClassSupportedByPreParser(Object data) {
		if (CollectionUtils.isNotEmpty(preParsers)) {
			for (ActivityDataPreParser<?, ?> dp : preParsers) {
				if (dp.isDataClassSupported(data)) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Returns the appropriate string representation for the specified object.
	 * <p>
	 * If {@code data} is byte array, HEX dump representation is returned.
	 * 
	 * @param data
	 *            object to convert to string representation
	 * @return string representation of object
	 */
	protected String toString(Object data) {
		return Utils.toStringDump(data);
	}

	@Override
	public void addField(ActivityField field) {
		if (field == null) {
			return;
		}

		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"ActivityParser.adding.field", field); // Utils.getDebugString(field));

		validateSupportedLocatorTypes(field);
		validateDuplicateFields(field);

		fieldList.add(field);
		field.referParser(this);
	}

	private void validateDuplicateFields(ActivityField field) {
		for (ActivityField aField : fieldList) {
			StreamFieldType fieldType = aField.getFieldType();
			if (aField.getFieldTypeName().equals(field.getFieldTypeName())
					&& !Utils.isCollectionType(fieldType == null ? null : fieldType.getDataType())) {
				throw new IllegalArgumentException(
						StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
								"ActivityParser.duplicate.field", getName(), aField.getFieldTypeName()));
			}
		}
	}

	/**
	 * Validates if activity field locators are supported by this parser.
	 *
	 * @param field
	 *            activity field to validate
	 */
	protected void validateSupportedLocatorTypes(ActivityField field) {
		EnumSet<ActivityFieldLocatorType> unsupportedLocators = getUnsupportedLocatorTypes();

		if (unsupportedLocators != null) {
			for (ActivityFieldLocatorType uLocatorType : unsupportedLocators) {
				if (field.hasLocatorsOfType(uLocatorType)) {
					throw new IllegalArgumentException(StreamsResources.getStringFormatted(
							StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.unsupported.locator",
							field.getFieldTypeName(), uLocatorType.name(), getClass().getName()));
				}
			}
		}
	}

	/**
	 * Returns set of locator types unsupported by this parser.
	 *
	 * @return set of unsupported locator types
	 */
	protected EnumSet<ActivityFieldLocatorType> getUnsupportedLocatorTypes() {
		return null;
	}

	@Override
	public void organizeFields() {
		List<ActivityField> sortedRefs = organizeFieldsReferences(fieldList);

		if (autoArrangeFields) {
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityParser.fields.user.order", fieldList.toString());
			if (sortedRefs != null) {
				fieldList.clear();
				fieldList.addAll(sortedRefs);
			}
			arrangeFieldsByType(fieldList);

			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityParser.fields.auto.order", fieldList.toString());
		}
	}

	/**
	 * Organizes fields by references for {@code fields} list provided parser fields. Organizing is performed in two
	 * steps:
	 * <ul>
	 * <li>validates fields references for unknown fields and cyclic references.</li>
	 * <li>arranges fields by soring them topologically (using references) to maintain correct value resolution
	 * sequence</li>
	 * </ul>
	 *
	 * @param fields
	 *            fields list to organize
	 * @return topologically sorted fields list
	 */
	protected List<ActivityField> organizeFieldsReferences(List<ActivityField> fields) {
		// make fields map to simplify fields access by name
		Map<String, ActivityField> parserFieldsMap = new LinkedHashMap<>(fields.size());
		Map<String, ActivityField> allFieldsMap = new LinkedHashMap<>(fields.size());
		for (ActivityField f : fields) {
			parserFieldsMap.put(f.getFieldTypeName(), f);
			allFieldsMap.put(f.getFieldTypeName(), f);
			collectStackedParsersFields(allFieldsMap, f);
		}

		// add auto-assignable fields
		Set<String> aaFields = new HashSet<>();
		aaFields.add(StreamFieldType.TrackingId.name());
		aaFields.add(StreamFieldType.StartTime.name());
		aaFields.add(StreamFieldType.EndTime.name());
		aaFields.add(StreamFieldType.ElapsedTime.name());
		aaFields.add(StreamFieldType.ServerName.name());
		aaFields.add(StreamFieldType.ServerIp.name());

		// make fields references matrix and verify missing references
		Map<ActivityField, Set<ActivityField>> refsMap = new LinkedHashMap<>(fields.size());
		for (ActivityField af : fields) {
			Set<String> refs = af.getReferredFields();
			Set<ActivityField> rFields = new LinkedHashSet<>(refs.size());

			if (CollectionUtils.isNotEmpty(refs)) {
				for (String ref : refs) {
					ActivityField rf = parserFieldsMap.get(ref);

					if (rf == null) {
						if (isExtRefField(ref, aaFields, allFieldsMap)) {
							continue;
						} else {
							throw new IllegalArgumentException(
									StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
											"ActivityParser.unknown.field.reference", af.getFieldTypeName(), ref));
						}
					}

					rFields.add(rf);
				}
			}
			refsMap.put(af, rFields);
		}

		// build fields references graph
		DirectedGraph<ActivityField> frg = new DirectedGraph<>();
		for (Map.Entry<ActivityField, Set<ActivityField>> re : refsMap.entrySet()) {
			frg.addNode(re.getKey());
		}

		for (Map.Entry<ActivityField, Set<ActivityField>> re : refsMap.entrySet()) {
			Set<ActivityField> refs = re.getValue();

			if (CollectionUtils.isNotEmpty(refs)) {
				for (ActivityField ref : refs) {
					frg.addEdge(ref, re.getKey());
				}
			}
		}
		// and detect cyclic dependencies
		try {
			return TopologicalSort.sort(frg);
		} catch (TopologicalSort.CyclicDependencyException exc) {
			throw new IllegalArgumentException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ActivityParser.cyclic.field.dependency", exc.getCycleNode()));
		}
	}

	private static boolean isExtRefField(String ref, Set<String> aaFields, Map<String, ActivityField> allFieldsMap) {
		// NOTE: ignoring parent parser fields references for now.
		return ref.startsWith(StreamsConstants.PARENT_REFERENCE_PREFIX) //
				|| aaFields.contains(ref) //
				|| allFieldsMap.containsKey(ref);
	}

	/**
	 * Arranges parser fields list by field locator type: CACHE fields has to be processed last
	 *
	 * @param fields
	 *            fields list to arrange
	 */
	protected static void arrangeFieldsByType(List<ActivityField> fields) {
		Collections.sort(fields, new Comparator<ActivityField>() {
			@Override
			public int compare(ActivityField f1, ActivityField f2) {
				int fi1 = fieldTypeIndex(f1);
				int fi2 = fieldTypeIndex(f2);

				return fi2 - fi1;
			}

			private int fieldTypeIndex(ActivityField field) {
				boolean cf = field.hasCacheLocators();
				boolean rf = !cf;

				int ftIdx = 0;
				if (cf) {
					ftIdx |= 1 << 0;
				}
				if (rf) {
					ftIdx |= 1 << 3;
				}

				return ftIdx;
			}
		});
	}

	/**
	 * Collects fields from field stacked parsers into provided fields map. When field has no stacked parsers, nothing
	 * new is added to map.
	 *
	 * @param fieldsMap
	 *            fields map to append stacked parsers fields
	 * @param f
	 *            field instance to collect stacked parsers fields
	 */
	protected void collectStackedParsersFields(Map<String, ActivityField> fieldsMap, ActivityField f) {
		Collection<ActivityField.FieldParserReference> sParsers = f.getStackedParsers();

		if (CollectionUtils.isEmpty(sParsers)) {
			return;
		}

		for (ActivityField.FieldParserReference spRef : sParsers) {
			GenericActivityParser<?> p = (GenericActivityParser<?>) spRef.getParser();

			for (ActivityField spf : p.fieldList) {
				ActivityField pmf = fieldsMap.put(spf.getFieldTypeName(), spf);

				if (pmf != null) {
					logger().log(OpLevel.WARNING, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"ActivityParser.stacked.field.conflict", f.getParser().getName(), f,
							spf.getParser().getName(), spf, pmf.getParser().getName(), pmf);
				}

				p.collectStackedParsersFields(fieldsMap, spf);
			}
		}
	}

	/**
	 * Removed an activity field definition from the set of fields supported by this parser.
	 *
	 * @param field
	 *            activity field to remove
	 */
	protected void removeField(ActivityField field) {
		if (field == null) {
			return;
		}

		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"ActivityParser.removing.field", field); // Utils.getDebugString(field));
		fieldList.remove(field);
		field.referParser(null);
	}

	/**
	 * Sets stream filters group used to filter activity data evaluating multiple activity information fields values.
	 * 
	 * @param afg
	 *            activity filters group instance
	 */
	public void setActivityFilter(StreamFiltersGroup<ActivityInfo> afg) {
		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"ActivityParser.adding.filter", afg);

		activityFilter = afg;
	}

	/**
	 * Reads the next RAW activity data string (line) from the specified data input source.
	 *
	 * @param data
	 *            input source for activity data
	 * @return string, or {@code null} if end of input source has been reached
	 * @throws IllegalArgumentException
	 *             if the class of input source supplied is not supported.
	 */
	protected String getNextActivityString(Object data) {
		if (data == null) {
			return null;
		}
		if (data instanceof String) {
			return (String) data;
		} else if (data instanceof byte[]) {
			return Utils.getString((byte[]) data);
		} else if (data instanceof ByteBuffer) {
			return Utils.getString(((ByteBuffer) data).array());// Utils.getStringLine(data);
		}
		BufferedReader rdr;
		if (data instanceof BufferedReader) {
			rdr = (BufferedReader) data;
		} else if (data instanceof Reader) {
			rdr = new BufferedReader((Reader) data);
		} else if (data instanceof InputStream) {
			rdr = new BufferedReader(new InputStreamReader((InputStream) data));
		} else {
			throw new IllegalArgumentException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ActivityParser.data.unsupported", data.getClass().getName()));
		}

		return readNextActivity(rdr);
	}

	/**
	 * Reads RAW activity data string (line) from {@link BufferedReader}.
	 *
	 * @param rdr
	 *            reader to use for reading
	 * @return non empty RAW activity data text string, or {@code null} if the end of the stream has been reached
	 */
	protected String readNextActivity(BufferedReader rdr) {
		String str = null;

		nextLock.lock();
		try {
			try {
				str = ActivityDelim.EOL.name().equals(activityDelim) ? Utils.getNonEmptyLine(rdr) : Utils.readAll(rdr);
			} catch (EOFException eof) {
				Utils.logThrowable(logger(), OpLevel.DEBUG,
						StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME), "ActivityParser.data.end",
						getActivityDataType(), eof);
			} catch (IOException ioe) {
				Utils.logThrowable(logger(), OpLevel.WARNING,
						StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityParser.error.reading", getActivityDataType(), ioe);
			}
		} finally {
			nextLock.unlock();
		}

		return str;
	}

	/**
	 * Returns "logical" type of RAW activity data entries.
	 *
	 * @return "logical" type of RAW activity data entries - TEXT
	 */
	@Override
	protected String getActivityDataType() {
		return "TEXT"; // NON-NLS
	}

	@Override
	protected ActivityInfo parse(TNTInputStream<?, ?> stream, Object data, ActivityInfo pai)
			throws IllegalStateException, ParseException {
		if (data == null) {
			return null;
		}

		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"ActivityParser.activity.raw.data", getLogString(data));

		data = preParse(stream, data);

		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"ActivityParser.preparsed.data", getLogString(data));

		ActivityContext cData = prepareItem(stream, data);
		if (pai != null) {
			cData.setParentActivity(pai);
		}

		if (cData == null || !cData.isValid()) {
			logger().log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityParser.nothing.to.parse");
			return null;
		}

		if (logger().isSet(OpLevel.DEBUG)) {
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityParser.parsing.data", getLogString(getDataAsMessage(cData)));
		}

		ActivityInfo ai = parsePreparedItem(cData);
		fillInMessageData(stream, ai, cData);
		postParse(cData);

		String parentId = (String) StreamsCache
				.getValue(stream.getName() + StreamsConstants.STREAM_GROUPING_ACTIVITY_ID_CACHE_KEY);
		if (StringUtils.isNotEmpty(parentId)) {
			if (parentIdField == null) {
				parentIdField = new ActivityField(StreamFieldType.ParentId.name(), ActivityFieldDataType.String);
			}

			ai.setFieldValue(parentIdField, parentId);
		}

		return ai;
	}

	private String getDataAsMessage(ActivityContext cData) {
		if (cData == null) {
			return null;
		}

		String msg = cData.getMessage();

		if (msg == null) {
			msg = getRawDataAsMessage(cData.getData());
			cData.setMessage(msg);
		}

		return msg;
	}

	/**
	 * Prepares RAW activity data to be parsed.
	 *
	 * @param stream
	 *            stream providing activity data
	 * @param data
	 *            raw activity data to prepare
	 * @return activity data context package
	 */
	@SuppressWarnings("unchecked")
	protected ActivityContext prepareItem(TNTInputStream<?, ?> stream, Object data) throws ParseException {
		T aData = (T) data;

		ActivityContext cData = new ActivityContext(stream, data, aData);
		// cData.setMessage(getRawDataAsMessage(aData));

		return cData;
	}

	/**
	 * Performs pre-parse actions on RAW activity data, e.g., conversion using pre-parsers.
	 * 
	 * @param stream
	 *            stream providing activity data
	 * @param data
	 *            raw activity data
	 * @return pre-parsed activity data
	 */
	protected Object preParse(TNTInputStream<?, ?> stream, Object data) {
		try {
			data = preParseActivityData(data);
		} catch (Exception exc) {
			Utils.logThrowable(logger(), OpLevel.ERROR,
					StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityParser.pre.parsing.failed", exc);
		}

		return data;
	}

	/**
	 * Performs post-parse actions on resolved activity fields data, e.g., filtering.
	 *
	 * @param cData
	 *            prepared activity data item context to parse
	 * @throws java.text.ParseException
	 *             if exception occurs applying field locator resolved cached value
	 */
	protected void postParse(ActivityContext cData) throws ParseException {
		if (cData == null || cData.getActivity() == null) {
			return;
		}

		ActivityInfo ai = cData.getActivity();

		try {
			filterActivity(ai);
		} catch (Exception exc) {
			Utils.logThrowable(logger(), OpLevel.WARNING,
					StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityParser.activity.filtering.failed", ai, exc);
		}

		if (!ai.isFilteredOut()) {
			ai.determineTrackingId();
			StreamsCache.cacheValues(ai, getName());
		}
	}

	/**
	 * Parse the specified prepared activity data, converting each field in prepared data to its corresponding value of
	 * activity info item.
	 *
	 * @param cData
	 *            prepared activity data item context to parse
	 * @return converted activity info, or {@code null} if activity data is {@code null}
	 * @throws ParseException
	 *             if exception occurs applying locator format properties to specified value
	 *
	 * @see #parseFields(com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser.ActivityContext)
	 */
	protected ActivityInfo parsePreparedItem(ActivityContext cData) throws ParseException {
		if (cData == null || cData.getData() == null) {
			return null;
		}

		ActivityInfo ai = new ActivityInfo();
		cData.setActivity(ai);
		try {
			parseFields(cData);
		} catch (MissingFieldValueException e) {
			logger().log(OpLevel.WARNING, Utils.getExceptionMessages(e));
			cData.setActivity(null);
			return null;
		} catch (Exception e) {
			ParseException pe = new ParseException(StreamsResources.getStringFormatted(
					StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.parsing.failed", cData.getField()), 0);
			pe.initCause(e);
			throw pe;
		}

		return ai;
	}

	/**
	 * Parses and applies values for all parser bound fields.
	 *
	 * @param cData
	 *            prepared activity data item context to parse
	 * @throws Exception
	 *             if exception occurs applying locator format properties to specified value, or required value has not
	 *             been resolved
	 *
	 * @see #parseLocatorValues(com.jkoolcloud.tnt4j.streams.fields.ActivityField,
	 *      com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser.ActivityContext)
	 * @see #applyFieldValue(com.jkoolcloud.tnt4j.streams.fields.ActivityField, Object,
	 *      com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser.ActivityContext)
	 */
	protected void parseFields(ActivityContext cData) throws Exception {
		// apply fields for parser
		Object value;
		for (ActivityField aField : fieldList) {
			cData.setField(aField);
			value = Utils.simplifyValue(parseLocatorValues(aField, cData));

			// if (value != null && aField.isEmptyAsNull() && Utils.isEmptyContent(value, true)) {
			// logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
			// "ActivityParser.field.empty.as.null", aField, toString(value));
			// value = null;
			// }

			applyFieldValue(aField, value, cData);
		}
	}

	/**
	 * Fills in activity entity 'Message' field with activity RAW data when parser has not resolved value for that
	 * field.
	 * <p>
	 * Parser has to be configured to act this way using parser configuration property
	 * 'UseActivityDataAsMessageForUnset'.
	 * 
	 * @param stream
	 *            stream providing activity data
	 * @param ai
	 *            converted activity info
	 * @param cData
	 *            parsing context data package
	 * @throws ParseException
	 *             if an error parsing the specified value
	 */
	protected void fillInMessageData(TNTInputStream<?, ?> stream, ActivityInfo ai, ActivityContext cData)
			throws ParseException {
		if (useActivityAsMessage && ai.getMessage() == null) {
			// save entire activity string as message data
			ActivityField field = new ActivityField(StreamFieldType.Message.name());
			applyFieldValue(stream, ai, field, getDataAsMessage(cData));
		}
	}

	/**
	 * Sets the value for the field in the specified activity entity. Depending on field definition if it is dynamic or
	 * resolved collection value has to be split, value is applied in dynamic manner using
	 * {@link #applyDynamicValue(com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser.ActivityContext, com.jkoolcloud.tnt4j.streams.fields.ActivityField, Object)}
	 * method. In all other cases {@link #applyFieldValue(TNTInputStream, ActivityInfo, ActivityField, Object)} is
	 * invoked.
	 *
	 * @param field
	 *            field to apply value to
	 * @param value
	 *            value to apply for this field
	 * @param cData
	 *            parsing context data package
	 * @throws IllegalStateException
	 *             if parser has not been properly initialized
	 * @throws ParseException
	 *             if an error parsing the specified value
	 *
	 * @see #applyDynamicValue(com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser.ActivityContext,
	 *      com.jkoolcloud.tnt4j.streams.fields.ActivityField, Object)
	 * @see #applyFieldValue(TNTInputStream, ActivityInfo, ActivityField, Object)
	 */
	protected void applyFieldValue(ActivityField field, Object value, ActivityContext cData) throws ParseException {
		logger().log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"ActivityParser.applying.field", getName(), field, Utils.toString(value));

		if (field.isDynamic() || (field.isSplitCollection() && Utils.isCollection(value))) {
			applyDynamicValue(cData, field, value);
		} else {
			applyFieldValue(cData.getStream(), cData.getActivity(), field, value);
		}
	}

	/**
	 * Transforms activity data to be put to activity entity field
	 * {@link com.jkoolcloud.tnt4j.streams.fields.StreamFieldType#Message}. This is used when no field
	 * {@link com.jkoolcloud.tnt4j.streams.fields.StreamFieldType#Message} mapping defined in parser configuration.
	 * 
	 * @param data
	 *            activity data
	 * @return data to be used for activity field {@link com.jkoolcloud.tnt4j.streams.fields.StreamFieldType#Message}
	 */
	protected String getRawDataAsMessage(T data) {
		return toString(data);
	}

	// protected void postParse(ActivityInfo ai, TNTInputStream<?, ?> stream, T data) throws ParseException {
	// Object msgData = getRawDataAsMessage(data);
	// if (useActivityAsMessage && ai.getMessage() == null && msgData != null) {
	// // save entire activity string as message data
	// ActivityField field = new ActivityField(StreamFieldType.Message.name());
	// applyFieldValue(stream, ai, field, msgData);
	// }
	// }

	/**
	 * Sets the value for the dynamic fields in the specified activity entity.
	 * <p>
	 * If field has stacked parser defined, then field value is parsed into separate activity using stacked parser. If
	 * field can be parsed by stacked parser, can be merged or added as a child into specified (parent) activity
	 * depending on stacked parser reference 'aggregation' attribute value.
	 *
	 * @param cData
	 *            parsing context data package
	 * @param field
	 *            field to apply value to
	 * @param value
	 *            value to apply for dynamic fields
	 * @throws IllegalStateException
	 *             if parser has not been properly initialized
	 * @throws ParseException
	 *             if an error parsing the specified value
	 *
	 * @see #applyFieldValue(TNTInputStream, ActivityInfo, ActivityField, Object)
	 */
	protected void applyDynamicValue(ActivityContext cData, ActivityField field, Object value) throws ParseException {
		Map<String, Object> dValMap = parseDynamicValues(cData, field.getDynamicLocators());

		Object[] fValues = value == null ? new Object[] { null } : Utils.makeArray(value);

		List<ActivityField> tFieldsList = new ArrayList<>(fValues.length);
		for (int vi = 0; vi < fValues.length; vi++) {
			ActivityField tField = field.createTempField(dValMap, vi);
			tFieldsList.add(tField);
		}

		reviewTempFieldsNames(tFieldsList);

		ActivityField tField = null;
		try {
			for (int tfi = 0; tfi < tFieldsList.size(); tfi++) {
				tField = tFieldsList.get(tfi);
				Object fValue = fValues[tfi];

				applyFieldValue(cData.getStream(), cData.getActivity(), tField, Utils.simplifyValue(fValue));
			}
		} catch (Exception e) {
			ParseException pe = new ParseException(StreamsResources.getStringFormatted(
					StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.parsing.failed", tField), 0);
			pe.initCause(e);
			throw pe;
		}
	}

	private static void reviewTempFieldsNames(List<ActivityField> tFieldsList) {
		if (tFieldsList != null) {
			int tid = 0;
			for (int tfi = 0; tfi < tFieldsList.size() - 1; tfi++) {
				ActivityField tField = tFieldsList.get(tfi);
				String tFieldName = tField.getFieldTypeName();
				String newName = null;

				for (int ntfi = tfi + 1; ntfi < tFieldsList.size(); ntfi++) {
					ActivityField ntField = tFieldsList.get(ntfi);
					String ntFieldName = ntField.getFieldTypeName();
					if (tFieldName.equals(ntFieldName)) {
						if (newName == null) {
							newName = tFieldName + (tid++);
						}

						ntField.setFieldTypeName(ntFieldName + (tid++));
					}
				}

				if (StringUtils.isNotEmpty(newName)) {
					tField.setFieldTypeName(newName);
				}
			}
		}
	}

	/**
	 * Parses values array from prepared activity data item using field bound locators.
	 *
	 * @param field
	 *            field instance to get locators
	 * @param cData
	 *            parsing context data package
	 * @return field locators parsed values array
	 * @throws ParseException
	 *             if exception occurs applying locator format properties to specified value
	 * @throws com.jkoolcloud.tnt4j.streams.parsers.MissingFieldValueException
	 *             if required locator value has not been resolved
	 * @see #parseLocatorValues(java.util.List,
	 *      com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser.ActivityContext)
	 */
	protected Object[] parseLocatorValues(ActivityField field, ActivityContext cData)
			throws ParseException, MissingFieldValueException {
		return parseLocatorValues(field.getLocators(), cData);
	}

	/**
	 * Parses values array from prepared activity data item using provided locators list.
	 *
	 * @param locators
	 *            value locators list
	 * @param cData
	 *            parsing context data package
	 * @return locators parsed values array
	 * @throws ParseException
	 *             if exception occurs applying locator format properties to specified value
	 * @throws com.jkoolcloud.tnt4j.streams.parsers.MissingFieldValueException
	 *             if required locator value has not been resolved
	 */
	protected Object[] parseLocatorValues(List<ActivityFieldLocator> locators, ActivityContext cData)
			throws ParseException, MissingFieldValueException {
		if (locators != null) {
			Object[] values = new Object[locators.size()];
			for (int li = 0; li < locators.size(); li++) {
				ActivityFieldLocator loc = locators.get(li);
				values[li] = getLocatorValue(loc, cData);

				if (values[li] == null && (loc.isRequired() || (requireAll && loc.isDefaultRequire()))) {
					throw new MissingFieldValueException(
							StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
									"ActivityParser.required.locator.not.found", loc, cData.getField()));
				}
			}
			return values;
		}

		return null;
	}

	private Map<String, Object> parseDynamicValues(ActivityContext cData,
			Map<String, ActivityFieldLocator> dynamicLocators) throws ParseException {
		Map<String, Object> dynamicValuesMap = new HashMap<>();
		if (dynamicLocators != null) {
			for (Map.Entry<String, ActivityFieldLocator> dLocator : dynamicLocators.entrySet()) {
				Object lValue = getLocatorValue(dLocator.getValue(), cData);
				Object dynamicLocatorValue = Utils.simplifyValue(lValue);
				if (dynamicLocatorValue != null) {
					dynamicValuesMap.put(dLocator.getKey(), dynamicLocatorValue);
				}
			}
		}

		return dynamicValuesMap;
	}

	/**
	 * Gets field value from raw data location and formats it according locator definition.
	 *
	 * @param stream
	 *            stream providing activity data
	 * @param locator
	 *            activity field locator
	 * @param data
	 *            activity object data
	 * @return value formatted based on locator definition or {@code null} if locator is not defined
	 * @throws ParseException
	 *             if exception occurs applying locator format properties to specified value
	 * @see #getLocatorValue(com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator,
	 *      com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser.ActivityContext)
	 * @deprecated used by tests only. Use
	 *             {@link #getLocatorValue(com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator, com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser.ActivityContext)}
	 *             instead.
	 */
	@Deprecated
	protected Object getLocatorValue(TNTInputStream<?, ?> stream, ActivityFieldLocator locator, T data)
			throws ParseException {
		return getLocatorValue(locator, new ActivityContext(stream, null, data));
	}

	/**
	 * Gets field value from raw data location and formats it according locator definition.
	 *
	 * @param locator
	 *            activity field locator
	 * @param cData
	 *            parsing context data package
	 * @return value formatted based on locator definition or {@code null} if locator is not defined
	 * @throws ParseException
	 *             if exception occurs applying locator format properties to specified value
	 * @see ActivityFieldLocator#formatValue(Object)
	 */
	protected Object getLocatorValue(ActivityFieldLocator locator, ActivityContext cData) throws ParseException {
		Object val = null;
		if (locator != null) {
			String locStr = locator.getLocator();
			AtomicBoolean formattingNeeded = new AtomicBoolean(true);
			if (StringUtils.isNotEmpty(locStr)) {
				if (locator.getBuiltInType() == ActivityFieldLocatorType.StreamProp) {
					val = cData.getStream().getProperty(locStr);
				} else if (locator.getBuiltInType() == ActivityFieldLocatorType.Cache) {
					val = Utils.simplifyValue(StreamsCache.getValue(cData.getActivity(), locStr, getName()));
				} else if (locator.getBuiltInType() == ActivityFieldLocatorType.Activity) {
					val = resolveActivityValue(locator, cData);
				} else if (LOC_FOR_COMPLETE_ACTIVITY_DATA.equals(locator.getLocator())) {
					val = cData.getData();
				} else {
					val = resolveLocatorValue(locator, cData, formattingNeeded);
				}

				// logger().log(val == null && !locator.isOptional() ? OpLevel.WARNING : OpLevel.TRACE,
				logger().log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityParser.locator.resolved", cData.getField(), locStr, toString(val));

				if (val != null && locator.isEmptyAsNull() && Utils.isEmptyContent(val, true)) {
					logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"ActivityParser.locator.empty.as.null", locStr, toString(val));
					val = null;
				}
			}

			val = transformValue(val, locator, cData, locStr, ValueTransformation.Phase.RAW);

			if (formattingNeeded.get()) {
				val = locator.formatValue(val);
			}

			val = transformValue(val, locator, cData, locStr, ValueTransformation.Phase.FORMATTED);

			try {
				boolean filteredOut = locator.filterValue(val, cData.getActivity());

				if (filteredOut) {
					val = null;
				}
			} catch (Exception exc) {
				Utils.logThrowable(logger(), OpLevel.WARNING,
						StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityParser.field.filtering.failed", locStr, toString(val), exc);
			}
		}
		return val;
	}

	/**
	 * Applies activity value transformations bound to defined activity value resolution {@code phase}.
	 *
	 * @param val
	 *            activity value to apply transformation
	 * @param locator
	 *            activity field locator defined transformations to use
	 * @param cData
	 *            parsing context data package
	 * @param locStr
	 *            activity data value locator string
	 * @param phase
	 *            activity data resolution phase defining transformations to apply
	 * @return transformed activity field value
	 */
	protected Object transformValue(Object val, ActivityFieldLocator locator, ActivityContext cData, String locStr,
			ValueTransformation.Phase phase) {
		try {
			return locator.transformValue(val, cData.getActivity(), phase);
		} catch (Exception exc) {
			Utils.logThrowable(logger(), OpLevel.WARNING,
					StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityParser.transformation.failed", locStr, toString(val), exc);
		}

		return val;
	}

	/**
	 * Gets activity entity data value resolved by locator.
	 *
	 * @param locator
	 *            activity field locator
	 * @param cData
	 *            activity object context data package
	 * @return raw value resolved by locator, or {@code null} if value is not resolved
	 */
	protected Object resolveActivityValue(ActivityFieldLocator locator, ActivityContext cData) {
		Object value = null;
		String locStr = locator.getLocator();
		if (StringUtils.isNotEmpty(locStr)) {
			if (locStr.startsWith(StreamsConstants.PARENT_REFERENCE_PREFIX)) {
				ActivityInfo pai = cData.getParentActivity();
				value = pai == null ? null : pai
						.getFieldValue(locStr.substring(StreamsConstants.PARENT_REFERENCE_PREFIX.length()), getName());
			} else {
				value = cData.getActivity().getFieldValue(locStr);
			}
		}

		return value;
	}

	/**
	 * Gets field raw data value resolved by locator.
	 *
	 * @param locator
	 *            activity field locator
	 * @param cData
	 *            activity object context data package
	 * @param formattingNeeded
	 *            flag to set if value formatting is not needed
	 * @return raw value resolved by locator, or {@code null} if value is not resolved
	 *
	 * @throws ParseException
	 *             if exception occurs while resolving raw data value
	 */
	protected abstract Object resolveLocatorValue(ActivityFieldLocator locator, ActivityContext cData,
			AtomicBoolean formattingNeeded) throws ParseException;

	/**
	 * Makes string representation of data package to put into log.
	 *
	 * @param data
	 *            data package to be logged
	 * @return string representation of data package to be logged
	 */
	protected String getLogString(Object data) {
		if (data == null || data instanceof String) {
			return String.valueOf(data);
		}
		return logger().isSet(OpLevel.TRACE) ? toString(data) : data.getClass().getName();
	}

	/**
	 * Applies stream filters group defined filters on activity information data. If activity data matches at least one
	 * excluding filter, activity is marked as "filtered out".
	 * 
	 * @param ai
	 *            activity information data
	 *
	 * @see com.jkoolcloud.tnt4j.streams.fields.ActivityInfo#setFiltered(boolean)
	 */
	protected void filterActivity(ActivityInfo ai) throws Exception {
		if (activityFilter == null || ai == null) {
			return;
		}

		filterLock.lock();
		try {
			boolean filteredOut = activityFilter.doFilter(null, ai);
			ai.setFiltered(filteredOut);
			logger().log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityParser.filtering.result", getName(), activityFilter.getName(), filteredOut);
		} finally {
			filterLock.unlock();
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void addReference(Object refObject) {
		if (refObject instanceof ActivityDataPreParser) {
			if (preParsers == null) {
				preParsers = new ArrayList<>();
			}
			preParsers.add((ActivityDataPreParser<Object, Object>) refObject);
		} else {
			logger().log(OpLevel.WARNING, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityParser.unsupported.reference", getName(),
					refObject == null ? null : refObject.getClass().getName());
		}
	}

	/**
	 * Converts RAW activity data using defined set of pre-parsers. Converted activity data then is parsed by parser
	 * itself.
	 * 
	 * @param data
	 *            RAW activity data to pre-parse
	 * @return pre-parsers converted activity data package
	 * @throws java.lang.Exception
	 *             if RAW activity data pre-parsing fails
	 */
	protected Object preParseActivityData(Object data) throws Exception {
		if (CollectionUtils.isNotEmpty(preParsers)) {
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityParser.data.before.pre.parsing", getLogString(data));

			preParserLock.lock();
			try {
				ActivityDataPreParser<?, ?> fpParser = preParsers.get(0);
				if (fpParser.isUsingParserForInput()) {
					data = getNextActivityString(data);
				}
				for (ActivityDataPreParser<Object, Object> preParser : preParsers) {
					boolean validData = preParser.isDataClassSupported(data);
					boolean logicalValid = isLogicalTypeSupported(preParser.dataTypeReturned());
					if (validData && logicalValid) {
						logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
								"ActivityParser.pre.parsing.data", Utils.getName(preParser));
						data = preParser.preParse(data);
						logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
								"ActivityParser.data.after.pre.parsing", getLogString(data));
					} else {
						logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
								"ActivityParser.pre.parsing.invalid", Utils.getName(preParser), validData, logicalValid,
								getLogString(data));
					}
				}
			} finally {
				preParserLock.unlock();
			}
		}

		return data;
	}

	private boolean isLogicalTypeSupported(String logicalType) {
		return logicalType == null || "OBJECT".equals(logicalType) || getActivityDataType().equals(logicalType); // NON-NLS
	}

	/**
	 * Activity data context containing all data used by parsers to resolve field values.
	 */
	protected class ActivityContext extends HashMap<String, Object> {
		private static final long serialVersionUID = 702832545435507437L;

		private static final String STREAM_KEY = "STREAM_DATA"; // NON-NLS
		private static final String RAW_DATA_KEY = "RAW_ACTIVITY_DATA"; // NON-NLS
		private static final String PARENT_ACTIVITY_KEY = "PARENT_ACTIVITY_DATA"; // NON-NLS

		private static final String PREPARED_DATA_KEY = "PREPARED_ACTIVITY_DATA"; // NON-NLS
		private static final String ACTIVITY_DATA_KEY = "ACTIVITY_DATA"; // NON-NLS
		private static final String MESSAGE_DATA_KEY = "ACT_MESSAGE_DATA"; // NON-NLS

		private static final String FIELD_KEY = "PARSED_FIELD"; // NON-NLS

		private boolean valid = true;

		/**
		 * Constructs new activity data context.
		 * 
		 * @param stream
		 *            stream providing activity data
		 * @param rawData
		 *            stream provided RAW activity data
		 */
		public ActivityContext(TNTInputStream<?, ?> stream, Object rawData) {
			put(STREAM_KEY, stream);
			put(RAW_DATA_KEY, rawData);
		}

		/**
		 * Constructs new activity data context.
		 *
		 * @param stream
		 *            stream providing activity data
		 * @param rawData
		 *            stream provided RAW activity data
		 * @param preparedData
		 *            parser prepared activity data compatible to locate values
		 */
		public ActivityContext(TNTInputStream<?, ?> stream, Object rawData, T preparedData) {
			put(STREAM_KEY, stream);
			put(RAW_DATA_KEY, rawData);
			put(PREPARED_DATA_KEY, preparedData);
		}

		/**
		 * Invalidates this activity data context.
		 */
		public void invalidate() {
			valid = true;
		}

		/**
		 * Reruns flag indicating whether this activity data context is valid.
		 *
		 * @return {@code true} is context is valid, {@code false} - otherwise
		 */
		public boolean isValid() {
			return valid;
		}

		/**
		 * Returns stream provided RAW activity data.
		 *
		 * @return stream provided RAW activity data
		 */
		public Object getRawData() {
			return get(RAW_DATA_KEY);
		}

		/**
		 * Sets parser prepared activity data.
		 * 
		 * @param preparedData
		 *            parser prepared activity data
		 */
		public void setData(T preparedData) {
			put(PREPARED_DATA_KEY, preparedData);
		}

		/**
		 * Returns parser prepared activity data.
		 * 
		 * @return parser prepared activity data
		 */
		@SuppressWarnings("unchecked")
		public T getData() {
			return (T) get(PREPARED_DATA_KEY);
		}

		/**
		 * Returns instance of stream providing activity data.
		 *
		 * @return stream providing activity data
		 */
		public TNTInputStream<?, ?> getStream() {
			return (TNTInputStream<?, ?>) get(STREAM_KEY);
		}

		/**
		 * Sets resolved activity entity data.
		 *
		 * @param ai
		 *            resolved activity entity data
		 */
		public void setActivity(ActivityInfo ai) {
			put(ACTIVITY_DATA_KEY, ai);
		}

		/**
		 * Returns resolved activity entity data.
		 *
		 * @return resolved activity entity data
		 */
		public ActivityInfo getActivity() {
			return (ActivityInfo) get(ACTIVITY_DATA_KEY);
		}

		/**
		 * Sets parent activity entity data.
		 *
		 * @param pai
		 *            parent activity entity data
		 */
		public void setParentActivity(ActivityInfo pai) {
			put(PARENT_ACTIVITY_KEY, pai);
		}

		/**
		 * Returns parent activity entity data.
		 *
		 * @return parent activity entity data
		 */
		public ActivityInfo getParentActivity() {
			return (ActivityInfo) get(PARENT_ACTIVITY_KEY);
		}

		/**
		 * Sets activity data string representation to be used as 'Message' field data.
		 *
		 * @param message
		 *            activity data string representation to be used as 'Message' field data
		 */
		public void setMessage(String message) {
			put(MESSAGE_DATA_KEY, message);
		}

		/**
		 * Returns activity data string representation to be used as 'Message' field data.
		 * 
		 * @return activity data string representation to be used as 'Message' field data
		 */
		public String getMessage() {
			return (String) get(MESSAGE_DATA_KEY);
		}

		/**
		 * Sets currently parsed field instance.
		 *
		 * @param field
		 *            currently parsed field instance
		 */
		public void setField(ActivityField field) {
			put(FIELD_KEY, field);
		}

		/**
		 * Gets currently parsed field instance.
		 * 
		 * @return currently parsed field instance
		 */
		public ActivityField getField() {
			return (ActivityField) get(FIELD_KEY);
		}
	}

	/**
	 * List built-in types of activity data delimiters within RAW data.
	 */
	protected enum ActivityDelim {
		/**
		 * Activity data delimiter is end-of-line.
		 */
		EOL,

		/**
		 * Activity data delimiter is end-of-file.
		 */
		EOF,
	}
}
