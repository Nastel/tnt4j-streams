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

import java.net.InetAddress;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.NamedObject;
import com.jkoolcloud.tnt4j.streams.filters.AbstractEntityFilter;
import com.jkoolcloud.tnt4j.streams.filters.AbstractExpressionFilter;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityParser;
import com.jkoolcloud.tnt4j.streams.reference.MatchingParserReference;
import com.jkoolcloud.tnt4j.streams.transform.AbstractScriptTransformation;
import com.jkoolcloud.tnt4j.streams.transform.ValueTransformation;
import com.jkoolcloud.tnt4j.streams.utils.*;

/**
 * Represents a specific activity field, containing the necessary information on how to extract its value from the raw
 * activity data.
 *
 * @version $Revision: 3 $
 */
public class ActivityField extends AbstractFieldEntity {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(ActivityField.class);

	/**
	 * Constant for default delimiter symbol used to delimit multiple field values.
	 */
	public static final String DEFAULT_FIELD_VALUES_DELIM = ","; // NON-NLS

	/**
	 * Constant defining entry key for dynamic locators resolved values map to store processed value index.
	 */
	public static final String VALUE_INDEX_ENTRY_KEY = "$ValueIndex$"; // NON-NLS

	/**
	 * Constant defining entity metadata built-in field name {@value}.
	 */
	public static final String META_FIELD_RESOLVE_SERVER = "@ResolveServerFromDNS@"; // NON-NLS
	/**
	 * Constant defining entity metadata built-in field name {@value}.
	 */
	public static final String META_FIELD_SPLIT_RELATIVES = "@SplitRelatives@"; // NON-NLS

	private static final Pattern METADATA_FIELD_NAME_PATTERN = Pattern.compile("@\\S+@");

	private String fieldTypeName;
	private List<ActivityFieldLocator> locators = null;
	private String separator = null;
	private String formattingPattern = null;
	private Set<FieldParserReference> stackedParsers;
	private boolean transparent = false;
	private boolean splitCollection = false;
	private String valueType = null;
	private Map<String, ActivityFieldLocator> dynamicLocators = null;

	private ActivityFieldLocator groupLocator;
	private ActivityParser parser;

	/**
	 * Constructs a new activity field entry.
	 *
	 * @param fieldTypeName
	 *            name of activity field type
	 *
	 * @throws IllegalArgumentException
	 *             if field type name is {@code null} or empty
	 */
	public ActivityField(String fieldTypeName) {
		if (StringUtils.isEmpty(fieldTypeName)) {
			throw new IllegalArgumentException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ActivityField.field.type.name.empty"));
		}
		setFieldTypeName(fieldTypeName);
	}

	/**
	 * Constructs a new activity field entry.
	 *
	 * @param fieldTypeName
	 *            name of activity field type
	 * @param dataType
	 *            type of field data type
	 *
	 * @throws NullPointerException
	 *             if field type is {@code null}
	 */
	public ActivityField(String fieldTypeName, ActivityFieldDataType dataType) {
		this(fieldTypeName);
		ActivityFieldLocator loc = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "0", dataType);
		locators = new ArrayList<>(1);
		locators.add(loc);
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	/**
	 * Returns activity parser instance enclosing this field.
	 *
	 * @param parser
	 *            parser instance enclosing this field
	 *
	 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#addField(ActivityField)
	 */
	public void referParser(ActivityParser parser) {
		this.parser = parser;
	}

	/**
	 * Returns activity parser instance enclosing this field.
	 *
	 * @return parser instance enclosing this field
	 */
	public ActivityParser getParser() {
		return parser;
	}

	/**
	 * Indicates if the raw data value for this activity field must be converted to a member or some enumeration type.
	 *
	 * @return {@code true} if value must be converted to an enumeration member, {@code false} otherwise
	 */
	public boolean isEnumeration() {
		StreamFieldType sft = getFieldType();

		return sft != null && sft.isEnumField();
	}

	/**
	 * Gets the type of this activity field.
	 *
	 * @return the activity field type
	 */
	public StreamFieldType getFieldType() {
		try {
			return StreamFieldType._valueOfIgnoreCase(fieldTypeName);
		} catch (IllegalArgumentException exc) {
			return null;
		}
	}

	/**
	 * Gets the type name of this activity field.
	 *
	 * @return the activity field type name
	 */
	public String getFieldTypeName() {
		return fieldTypeName;
	}

	/**
	 * Sets the type name of this activity field.
	 *
	 * @param fieldTypeName
	 *            the activity field type name
	 * 
	 * @return instance of this activity field
	 */
	public ActivityField setFieldTypeName(String fieldTypeName) {
		this.fieldTypeName = fieldTypeName;

		if (METADATA_FIELD_NAME_PATTERN.matcher(fieldTypeName).matches()) {
			setTransparent(true);
		}

		return this;
	}

	@Override
	public String getName() {
		return fieldTypeName;
	}

	/**
	 * Gets activity field locators list.
	 *
	 * @return the locators list
	 */
	public List<ActivityFieldLocator> getLocators() {
		return locators;
	}

	/**
	 * Adds activity field locator.
	 *
	 * @param locator
	 *            the locator to add
	 *
	 * @return instance of this activity field
	 */
	public ActivityField addLocator(ActivityFieldLocator locator) {
		if (locator != null) {
			boolean dynamic = false;
			if (StringUtils.isNotEmpty(locator.getId())) {
				String did = Utils.makeExpVariable(locator.getId());

				if (fieldTypeName.contains(did) || StringUtils.contains(valueType, did)
						|| StringUtils.contains(separator, did) || StringUtils.contains(formattingPattern, did)) {
					addDynamicLocator(did, locator);
					dynamic = true;
				}
			}

			if (!dynamic) {
				addStaticLocator(locator);
			}
		}

		return this;
	}

	private void addDynamicLocator(String id, ActivityFieldLocator locator) {
		if (dynamicLocators == null) {
			dynamicLocators = new HashMap<>();
		}

		dynamicLocators.put(id, locator);
	}

	private void addStaticLocator(ActivityFieldLocator locator) {
		if (locators == null) {
			locators = new ArrayList<>();
		}

		locators.add(locator);
	}

	/**
	 * Checks whether any of field static or dynamic locators has type 'Cache'.
	 *
	 * @return {@code true} if any of field static or dynamic locators has type 'Cache', {@code false} - otherwise.
	 */
	public boolean hasCacheLocators() {
		return hasLocatorsOfType(ActivityFieldLocatorType.Cache);
	}

	/**
	 * Checks if this field has no defined locator/value.
	 *
	 * @return {@code true} if field has no locators or has only one empty locator
	 *
	 * @see ActivityFieldLocator#isEmpty()
	 */
	public boolean hasNoValueLocator() {
		return locators == null || (locators.size() == 1 && locators.get(0).isEmpty());
	}

	/**
	 * Checks if this field has no defined locator/value and has activity transformations used to build field value.
	 *
	 * @return {@code true} if field has no defined locator/value and has activity transformations
	 *
	 * @see #hasNoValueLocator()
	 * @see #hasActivityTransformations()
	 */
	public boolean isResolvingValueOverTransformation() {
		return hasNoValueLocator() && hasActivityTransformations();
	}

	@Override
	public boolean hasActivityTransformations() {
		if (super.hasActivityTransformations()) {
			return true;
		}

		if (locators != null) {
			for (ActivityFieldLocator loc : locators) {
				if (loc.hasActivityTransformations()) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Checks whether any of field static or dynamic locators has type 'Activity'.
	 *
	 * @return {@code true} if any of field static or dynamic locators has type 'Activity', {@code false} - otherwise.
	 */
	public boolean hasActivityLocators() {
		return hasLocatorsOfType(ActivityFieldLocatorType.Activity);
	}

	/**
	 * Checks whether any of field static or dynamic locators has type {@code lType}.
	 *
	 * @param lType
	 *            locator type
	 * @return {@code true} if any of field static or dynamic locators has type {@code lType}, {@code false} -
	 *         otherwise.
	 */
	public boolean hasLocatorsOfType(ActivityFieldLocatorType lType) {
		return hasLocatorsOfType(locators, lType)
				|| (dynamicLocators != null && hasLocatorsOfType(dynamicLocators.values(), lType));
	}

	/**
	 * Checks whether any of provided locators has type {@code lType}.
	 *
	 * @param locators
	 *            locators collection to check
	 * @param lType
	 *            locator type
	 * @return {@code true} if any of provided locators has type {@code lType}, {@code false} - otherwise.
	 */
	protected static boolean hasLocatorsOfType(Collection<ActivityFieldLocator> locators,
			ActivityFieldLocatorType lType) {
		if (locators != null) {
			for (ActivityFieldLocator afl : locators) {
				if (afl.getBuiltInType() == lType) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Sets grouping field (containing no direct value locator, but grouping several field value locators) locator used
	 * to format resolved activity RAW data avalue.
	 *
	 * @param radix
	 *            radix of field values
	 * @param reqVal
	 *            {@code true}/{@code false} string
	 * @param dataType
	 *            the data type for raw data field
	 * @param units
	 *            the units the raw data value represents
	 * @param format
	 *            the format string for interpreting raw data value
	 * @param locale
	 *            locale for formatter to use
	 * @param timeZone
	 *            the timeZone to set
	 * @param charset
	 *            the charset name for binary data
	 */
	public void setGroupLocator(int radix, String reqVal, ActivityFieldDataType dataType, String units, String format,
			String locale, String timeZone, String charset) {
		groupLocator = new ActivityFieldLocator();
		groupLocator.setRadix(radix);
		groupLocator.setRequired(reqVal);
		if (dataType != null) {
			groupLocator.setDataType(dataType);
		}
		if (StringUtils.isNotEmpty(units)) {
			groupLocator.setUnits(units);
		}
		if (StringUtils.isNotEmpty(format)) {
			groupLocator.setFormat(format, locale);
		}
		if (StringUtils.isNotEmpty(timeZone)) {
			groupLocator.setTimeZone(timeZone);
		}
		if (StringUtils.isNotEmpty(charset)) {
			groupLocator.setCharset(charset);
		}
	}

	/**
	 * Gets grouping field (containing no direct value locator, but grouping several field value locators) locator used
	 * to format resolved activity RAW data avalue.
	 *
	 * @return grouping field locator
	 */
	public ActivityFieldLocator getGroupLocator() {
		return groupLocator;
	}

	/**
	 * Gets master locator for this field. If field has grouping locator defined then this locator is returned. If no
	 * grouping locator defined, but there is at least one ordinary locator defined, then first ordinary locator is
	 * returned.
	 *
	 * @return field master locator, or {@code null} if none locators defined for this field
	 */
	public ActivityFieldLocator getMasterLocator() {
		return groupLocator != null ? groupLocator : CollectionUtils.isEmpty(locators) ? null : locators.get(0);
	}

	/**
	 * Gets the string to insert between values when concatenating multiple raw activity values into the converted value
	 * for this field.
	 *
	 * @return the string being used to separate raw values
	 */
	public String getSeparator() {
		return separator == null ? DEFAULT_FIELD_VALUES_DELIM : separator;
	}

	/**
	 * Checks whether provided {@code strings} array shall be formatted by this field using attributes {@code separator}
	 * or {@code formattingPattern} defined defined values.
	 * 
	 * @param strings
	 *            string array to be formatted
	 *
	 * @return {@code true} if field has {@code formattingPattern} defined or {@code separator} is not {@code null} and
	 *         {@code strings} array length is greater than 1, {@code false} - otherwise
	 */
	public boolean isArrayFormattable(String[] strings) {
		int sLength = ArrayUtils.getLength(strings);

		return (StringUtils.isNotEmpty(formattingPattern) && sLength > 0) || (separator != null && sLength > 1);
	}

	/**
	 * Sets the string to insert between values when concatenating multiple raw activity values into the converted value
	 * for this field.
	 *
	 * @param locatorSep
	 *            the string to use to separate raw values
	 *
	 * @return instance of this activity field
	 */
	public ActivityField setSeparator(String locatorSep) {
		this.separator = locatorSep;

		adjustMasterLocatorDataType(ActivityFieldDataType.String);

		return this;
	}

	/**
	 * Gets the string representation formatting pattern of multiple raw activity values concatenated into the converted
	 * value for this field.
	 *
	 * @return the string being used to format raw values
	 */
	public String getFormattingPattern() {
		return formattingPattern;
	}

	/**
	 * Sets the string representation formatting pattern of multiple raw activity values concatenated into the converted
	 * value for this field.
	 *
	 * @param pattern
	 *            the string to use to format raw values
	 *
	 * @return instance of this activity field
	 */
	public ActivityField setFormattingPattern(String pattern) {
		this.formattingPattern = pattern;

		adjustMasterLocatorDataType(ActivityFieldDataType.String);

		return this;
	}

	private void adjustMasterLocatorDataType(ActivityFieldDataType dType) {
		ActivityFieldLocator mLoc = getMasterLocator();
		if (mLoc != null) {
			if (mLoc.getDataType() == ActivityFieldDataType.AsInput
					|| mLoc.getDataType() == ActivityFieldDataType.Generic) {
				mLoc.setDataType(dType);
			}
		}
	}

	/**
	 * Gets field value type.
	 *
	 * @return string representing field value type
	 */
	public String getValueType() {
		return valueType;
	}

	/**
	 * Sets field value type.
	 *
	 * @param valueType
	 *            string representing field value type
	 *
	 * @return instance of this activity field
	 */
	public ActivityField setValueType(String valueType) {
		this.valueType = valueType;

		return this;
	}

	/**
	 * Returns string representing activity field by field type.
	 *
	 * @return a string representing field.
	 */
	@Override
	public String toString() {
		return fieldTypeName;
	}

	/**
	 * Adds activity field stacked parser.
	 *
	 * @param parserReference
	 *            the stacked parser reference to add
	 * @param aggregationType
	 *            resolved activity entities aggregation type
	 * @param applyOn
	 *            stacked parser application phase name
	 * 
	 * @return instance of this activity field
	 */
	public ActivityField addStackedParser(MatchingParserReference parserReference, String aggregationType,
			String applyOn) {
		if (parserReference != null) {
			if (stackedParsers == null) {
				stackedParsers = new LinkedHashSet<>(5);
			}

			LoggerUtils.log(LOGGER, OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityField.adding.stacked.parser", fieldTypeName, parserReference.getParser().getName());
			FieldParserReference pRef = new FieldParserReference(parserReference, aggregationType, applyOn);
			stackedParsers.add(pRef);
		}

		return this;
	}

	/**
	 * Gets activity field stacked parsers collection.
	 *
	 * @return stacked parsers collection
	 */
	public Collection<FieldParserReference> getStackedParsers() {
		return stackedParsers;
	}

	/**
	 * Gets the transparent flag indicating whether field value has to be added to activity info data package.
	 *
	 * @return flag indicating whether field value has to be added to activity info data package
	 */
	public boolean isTransparent() {
		return transparent;
	}

	/**
	 * Sets the transparent flag indicating whether field value has to be added to activity info data package.
	 *
	 * @param transparent
	 *            flag indicating whether field value has to be added to activity info data package
	 *
	 * @return instance of this activity field
	 */
	public ActivityField setTransparent(boolean transparent) {
		this.transparent = transparent;

		return this;
	}

	/**
	 * Gets the splitCollection flag indicating whether resolved field value collection (list/array) has to be split
	 * into separate fields of activity info data package.
	 *
	 * @return flag indicating whether resolved field value collection (list/array) has to be split into separate fields
	 *         of activity info data package
	 */
	public boolean isSplitCollection() {
		return splitCollection;
	}

	/**
	 * Sets the splitCollection flag indicating whether resolved field value collection (list/array) has to be split
	 * into separate fields of activity info data package.
	 *
	 * @param splitCollection
	 *            flag indicating whether resolved field value collection (list/array) has to be split into separate
	 *            fields of activity info data package
	 *
	 * @return instance of this activity field
	 */
	public ActivityField setSplitCollection(boolean splitCollection) {
		this.splitCollection = splitCollection;

		return this;
	}

	/**
	 * Gets activity field dynamic attribute values locators map.
	 *
	 * @return the dynamic attribute values locators map
	 */
	public Map<String, ActivityFieldLocator> getDynamicLocators() {
		return dynamicLocators;
	}

	/**
	 * Checks if field has any dynamic locators defined.
	 *
	 * @return {@code true} if field has any dynamic locators defined, {@code false} - otherwise
	 */
	public boolean isDynamic() {
		return MapUtils.isNotEmpty(dynamicLocators);
	}

	/**
	 * Checks if field attributes {@link #fieldTypeName}, {@link #valueType}, {@link #separator} and
	 * {@link #formattingPattern} values contains variable expressions.
	 *
	 * @return {@code true} if field attribute values contains variable expressions, {@code false} - otherwise
	 */
	public boolean hasDynamicAttrs() {
		return hasDynamicAttrs(fieldTypeName, valueType, separator, formattingPattern);
	}

	/**
	 * Checks if any of attribute values from set contains variable expression.
	 *
	 * @param attrValues
	 *            set of attribute values to check
	 *
	 * @return {@code true} if value of any attribute from set contains variable expression, {@code false} - otherwise
	 */
	public static boolean hasDynamicAttrs(String... attrValues) {
		if (attrValues != null) {
			for (String attrValue : attrValues) {
				if (isDynamicAttr(attrValue)) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Checks if attribute value string contains variable expression.
	 *
	 * @param attrValue
	 *            attribute value
	 *
	 * @return {@code true} if attribute value string contains variable expression, {@code false} - otherwise
	 */
	public static boolean isDynamicAttr(String attrValue) {
		return Utils.isVariableExpression(attrValue);
	}

	/**
	 * Checks if dynamic locators map contains entry keyed by identifier variable.
	 *
	 * @param dLocIdVar
	 *            dynamic locator identifier variable
	 *
	 * @return {@code true} if dynamic locators map contains entry keyed by identifier variable, {@code false} -
	 *         otherwise
	 */
	public boolean hasDynamicLocator(String dLocIdVar) {
		return dynamicLocators != null && dynamicLocators.containsKey(dLocIdVar);
	}

	/**
	 * Checks if this field uses provided {@code locator} string among defined locators, dynamic locators or group
	 * locator.
	 * 
	 * @param locator
	 *            locator string to check
	 * @return {@code true} if field uses defined locator string, {@code false} - otherwise
	 */
	public boolean hasLocator(String locator) {
		return hasLocator(locator, locators) //
				|| (isDynamic() && hasLocator(locator, dynamicLocators.values())) //
				|| (groupLocator != null && StringUtils.equals(groupLocator.getLocator(), locator));
	}

	private static boolean hasLocator(String locator, Collection<ActivityFieldLocator> locators) {
		if (locators != null) {
			for (ActivityFieldLocator loc : locators) {
				if (StringUtils.equals(loc.getLocator(), locator)) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Creates temporary field taking this field as template and filling dynamic attributes values with ones from
	 * dynamic locators resolved values map.
	 *
	 * @param dValues
	 *            dynamic locators resolved values map
	 *
	 * @return temporary field instance
	 */
	public ActivityField createTempField(Map<String, Object> dValues) {
		ActivityField tField = new ActivityField(fillDynamicAttr(fieldTypeName, dValues));
		tField.locators = getTempFieldLocators(locators, (int) dValues.get(VALUE_INDEX_ENTRY_KEY));
		tField.separator = fillDynamicAttr(separator, dValues);
		tField.formattingPattern = fillDynamicAttr(formattingPattern, dValues);
		tField.requiredVal = requiredVal;
		tField.stackedParsers = stackedParsers;
		tField.valueType = fillDynamicAttr(valueType, dValues);
		tField.transparent = transparent;
		tField.splitCollection = splitCollection;
		tField.parser = parser;
		tField.transformations = transformations;
		tField.filter = filter;
		tField.groupLocator = groupLocator;

		return tField;
	}

	private static String fillDynamicAttr(String dAttr, Map<String, Object> dValMap) {
		String tAttr = dAttr;

		if (isDynamicAttr(dAttr) && MapUtils.isNotEmpty(dValMap)) {
			int valueIndex = (int) dValMap.get(VALUE_INDEX_ENTRY_KEY);
			tAttr = dAttr = dAttr.replace(StreamsConstants.VALUE_ORDINAL_INDEX, String.valueOf(valueIndex));
			List<String> vars = new ArrayList<>();
			Utils.resolveCfgVariables(vars, dAttr);

			for (String var : vars) {
				Object dVal = dValMap.get(var);
				if (dVal instanceof Map) {
					tAttr = String.valueOf(Utils.getMapValueByPath(dAttr, dValMap));
				} else {
					tAttr = tAttr.replace(var, String.valueOf(Utils.getItem(dVal, valueIndex)));
				}
			}
		}

		return tAttr;
	}

	private static List<ActivityFieldLocator> getTempFieldLocators(List<ActivityFieldLocator> locators, int index) {
		if (CollectionUtils.size(locators) <= 1) {
			return locators;
		}

		List<ActivityFieldLocator> fLocators = new ArrayList<>(1);
		if (index >= 0 && index < locators.size()) {
			fLocators.add(locators.get(index));
		}

		return fLocators;
	}

	/**
	 * Collects all references for this field from bound transformation and filter expressions. If locator type is
	 * {@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Activity}, locator referenced field is added
	 * to references set too.
	 *
	 * @return set of field references
	 */
	public Set<String> getReferredFields() {
		Set<String> rFields = new HashSet<>();

		addTransformationAndFilterReferences(this, rFields);

		for (ActivityFieldLocator loc : locators) {
			if (loc.getBuiltInType() == ActivityFieldLocatorType.Activity) {
				rFields.add(loc.getLocator());
			}

			addTransformationAndFilterReferences(loc, rFields);
		}

		return rFields;
	}

	/**
	 * Collects field entity references form bound transformation and filter expressions.
	 *
	 * @param fe
	 *            field entity to collect references
	 * @param rFields
	 *            references set to append found references
	 */
	protected static void addTransformationAndFilterReferences(AbstractFieldEntity fe, Set<String> rFields) {
		if (CollectionUtils.isNotEmpty(fe.transformations)) {
			for (ValueTransformation<?, ?> vt : fe.transformations) {
				if (vt instanceof AbstractScriptTransformation) {
					AbstractScriptTransformation<?> st = (AbstractScriptTransformation<?>) vt;
					Set<String> exVars = st.getExpressionVariables();
					if (CollectionUtils.isNotEmpty(exVars)) {
						for (String expVar : exVars) {
							rFields.add(Utils.getVarName(expVar));
						}
					}
				}
			}
		}

		if (fe.filter != null && CollectionUtils.isNotEmpty(fe.filter.getFilters())) {
			for (AbstractEntityFilter<?> f : fe.filter.getFilters()) {
				if (f instanceof AbstractExpressionFilter) {
					AbstractExpressionFilter<?> ef = (AbstractExpressionFilter<?>) f;
					Set<String> exVars = ef.getExpressionVariables();
					if (CollectionUtils.isNotEmpty(exVars)) {
						for (String expVar : exVars) {
							rFields.add(Utils.getVarName(expVar));
						}
					}
				}
			}
		}
	}

	/**
	 * Aggregates field locators resolved values into single value to be set for activity entity field.
	 * <p>
	 * Aggregated value transformation and filtering is also performed by this method.
	 * <p>
	 * When field has flag {@code emptyAsNull} set to {@code true}, aggregation produced 'empty' values (e.g. string
	 * {@code ""}) are reset to {@code null} and suppressed by streaming process.
	 *
	 * @param value
	 *            field value to aggregate
	 * @param ai
	 *            activity entity instance to use for aggregation
	 * @return aggregated field value, or {@code null} if value aggregates to 'empty' (e.g. string {@code ""}) value and
	 *         field has flag {@code emptyAsNull} set to {@code true}
	 * @throws ParseException
	 *             if an error parsing the specified value based on the field definition (e.g. does not match defined
	 *             format, missing value when required, etc.)
	 *
	 * @see #transform(Object, ActivityInfo)
	 * @see #filterFieldValue(Object, ActivityInfo)
	 */
	public Object aggregateFieldValue(Object value, ActivityInfo ai) throws ParseException {
		LoggerUtils.log(LOGGER, OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"ActivityField.aggregating.field", this, value);
		Object[] values = Utils.makeArray(Utils.simplifyValue(value));

		if (values != null && CollectionUtils.isNotEmpty(locators)) {
			if (values.length == 1 && locators.size() > 1) {
				Object fValue = formatValue(groupLocator, values[0]);
				values = Utils.makeArray(fValue);
			} else {
				if (locators.size() > 1 && locators.size() != values.length) {
					throw new ParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ActivityField.failed.parsing", this), 0);
				}

				ActivityFieldLocator locator;
				Object fValue;
				List<Object> fvList = new ArrayList<>(locators.size());
				for (int v = 0; v < values.length; v++) {
					locator = locators.size() == 1 ? locators.get(0) : locators.get(v);
					fValue = formatValue(locator, values[v]);
					if (fValue == null && locator.isOptional()) {
						continue;
					}
					fvList.add(fValue);
				}

				values = Utils.makeArray(fvList);
			}

			if (isEnumeration() && values.length > 1) {
				throw new ParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ActivityField.multiple.enum.values", this), 0);
			}
		}

		Object fieldValue = Utils.simplifyValue(values);

		LoggerUtils.log(LOGGER, OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"ActivityField.aggregating.field.value", this, fieldValue);

		fieldValue = transform(fieldValue, ai);
		fieldValue = filterFieldValue(fieldValue, ai);

		if (fieldValue != null) {
			if (isEmptyAsNull() && Utils.isEmptyContent(fieldValue, true)) {
				LoggerUtils.log(LOGGER, OpLevel.TRACE,
						StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityField.field.empty.as.null", this, fieldValue);
				fieldValue = null;
			}
		}

		if (fieldValue == null && isRequired()) {
			throw new ParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ActivityField.required.but.null", this), 0);
		}

		return fieldValue;
	}

	/**
	 * Transforms the value for the field using defined field transformations.
	 * <p>
	 * Note that field value there is combination of all field locators resolved values. Transformations defined for
	 * particular locator is already performed by parser while resolving locator value.
	 *
	 * @param fieldValue
	 *            field data value to transform
	 * @param ai
	 *            activity entity instance to get additional value for a transformation
	 * @return transformed field value
	 *
	 * @see #transformValue(Object, java.util.Map, com.jkoolcloud.tnt4j.streams.transform.ValueTransformation.Phase)
	 */
	protected Object transform(Object fieldValue, ActivityInfo ai) {
		try {
			Map<String, Object> context = new HashMap<>(2);
			context.put(StreamsConstants.CTX_ACTIVITY_DATA_KEY, ai);
			context.put(StreamsConstants.CTX_FIELD_KEY, this);

			fieldValue = transformValue(fieldValue, context, ValueTransformation.Phase.AGGREGATED);
		} catch (Exception exc) {
			LoggerUtils.logThrowable(LOGGER, OpLevel.WARNING,
					StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityField.transformation.failed", fieldTypeName, fieldValue, exc);
		}

		return fieldValue;
	}

	/**
	 * Applies field defined filtering rules and marks this activity as filtered out or sets field value to {@code
	 * null}, if field is set as "optional" using attribute {@code required=false}.
	 *
	 * @param value
	 *            value to apply filters
	 * @param ai
	 *            activity info instance to alter "filtered out" flag
	 * @return value filters were applied on, or {@code null} if filters gets field value "filtered out"
	 *
	 * @see #filterValue(Object, ActivityInfo)
	 */
	protected Object filterFieldValue(Object value, ActivityInfo ai) {
		try {
			boolean filteredOut = filterValue(value, ai);

			if (filteredOut) {
				return null;
			}
		} catch (Exception exc) {
			LoggerUtils.logThrowable(LOGGER, OpLevel.WARNING,
					StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME), "ActivityField.filtering.failed",
					fieldTypeName, value, exc);
		}

		return value;
	}

	/**
	 * Formats the value for the field based on the required internal data type of the field and the definition of the
	 * field.
	 *
	 * @param locator
	 *            locator information for value
	 * @param value
	 *            raw value of field
	 * @return formatted value of field in required internal data type
	 */
	protected Object formatValue(ActivityFieldLocator locator, Object value) {
		if (value == null) {
			return null;
		}
		if (isEnumeration()) {
			if (value instanceof String) {
				String strValue = (String) value;
				value = StringUtils.isNumeric(strValue) ? Integer.valueOf(strValue) : strValue.toUpperCase().trim();
			}
		}
		StreamFieldType fieldType = getFieldType();
		if (fieldType != null) {
			switch (fieldType) {
			case ElapsedTime:
				try {
					// Elapsed time needs to be converted to usec
					if (!(value instanceof Number)) {
						value = Long.valueOf(Utils.toString(value));
					}
					TimeUnit units = ActivityFieldLocator.getLocatorUnits(locator, TimeUnit.MICROSECONDS);
					value = TimestampFormatter.convert((Number) value, units, TimeUnit.MICROSECONDS);
				} catch (Exception e) {
				}
				break;
			case ServerIp:
				if (value instanceof InetAddress) {
					value = ((InetAddress) value).getHostAddress();
				}
				break;
			case ServerName:
				if (value instanceof InetAddress) {
					value = ((InetAddress) value).getHostName();
				}
				break;
			default:
				break;
			}
		}
		return value;
	}

	/**
	 * Field referenced stacked parser reference definition.
	 */
	public static class FieldParserReference extends MatchingParserReference {
		private final MatchingParserReference parserRef;
		private final AggregationType aggregationType;
		private final ParserApplyType applyOn;

		/**
		 * Constructs a new ParserReference.
		 *
		 * @param parserReference
		 *            referenced parser instance
		 * @param aggregationType
		 *            activity entity resolved fields aggregation type name
		 * @param applyOn
		 *            stacked parser application phase type name
		 */
		FieldParserReference(MatchingParserReference parserReference, String aggregationType, String applyOn) {
			this(parserReference,
					StringUtils.isEmpty(aggregationType) ? AggregationType.Merge
							: Utils.valueOfIgnoreCase(AggregationType.class, aggregationType),
					StringUtils.isEmpty(applyOn) ? ParserApplyType.Field
							: Utils.valueOfIgnoreCase(ParserApplyType.class, applyOn));
		}

		/**
		 * Constructs a new FieldParserReference.
		 *
		 * @param parserReference
		 *            referenced parser instance
		 * @param aggregationType
		 *            activity entity resolved fields aggregation type
		 * @param applyOn
		 *            stacked parser application phase type
		 */
		FieldParserReference(MatchingParserReference parserReference, AggregationType aggregationType,
				ParserApplyType applyOn) {
			super(parserReference.getParser());

			this.parserRef = parserReference;

			this.aggregationType = aggregationType;
			this.applyOn = applyOn;
		}

		/**
		 * Returns activity entity resolved fields aggregation type.
		 *
		 * @return the aggregation type
		 */
		public AggregationType getAggregationType() {
			return aggregationType;
		}

		/**
		 * Returns stacked parser application phase type.
		 *
		 * @return the stacked parser application phase type
		 */
		public ParserApplyType getApplyOn() {
			return applyOn;
		}

		@Override
		public Boolean matchTags(String... dataTags) {
			return parserRef.matchTags(dataTags);
		}

		@Override
		public Boolean matchExp(NamedObject caller, Object value, Map<String, ?> context) {
			return parserRef.matchExp(caller, value, context);
		}

		@Override
		public String toString() {
			return getParser().getName() + ":" + aggregationType + ":" + applyOn; // NON-NLS
		}
	}
}
