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

import java.net.InetAddress;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.NamedObject;
import com.jkoolcloud.tnt4j.streams.filters.AbstractEntityFilter;
import com.jkoolcloud.tnt4j.streams.filters.AbstractExpressionFilter;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityParser;
import com.jkoolcloud.tnt4j.streams.reference.MatchingParserReference;
import com.jkoolcloud.tnt4j.streams.transform.AbstractScriptTransformation;
import com.jkoolcloud.tnt4j.streams.transform.ValueTransformation;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.TimestampFormatter;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Represents a specific activity field, containing the necessary information on how to extract its value from the raw
 * activity data.
 *
 * @version $Revision: 3 $
 */
public class ActivityField extends AbstractFieldEntity {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivityField.class);

	/**
	 * Constant for default delimiter symbol used to delimit multiple field values.
	 */
	public static final String DEFAULT_FIELD_VALUES_DELIM = ","; // NON-NLS

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
		this.fieldTypeName = fieldTypeName;
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
		}

		return null;
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
	 */
	public void setFieldTypeName(String fieldTypeName) {
		this.fieldTypeName = fieldTypeName;
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

				if (fieldTypeName.contains(did) || (valueType != null && valueType.contains(did))) {
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
	 * Checks whether field has array formatting attributes {@code separator} or {@code formattingPattern} defined.
	 *
	 * @return {@code true} if field has {@code separator} or {#code formattingPattern} defined, {@code false} -
	 *         otherwise
	 */
	public boolean isArrayFormattable() {
		return separator != null || StringUtils.isNotEmpty(formattingPattern);
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

		return this;
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
	 * Indicates whether some other object is "equal to" this field.
	 *
	 * @param obj
	 *            the reference object with which to compare.
	 *
	 * @return {@code true} if this field is the same as the obj argument, {@code false} otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}

		ActivityField that = (ActivityField) obj;

		return fieldTypeName.equals(that.fieldTypeName);
	}

	/**
	 * Returns hash code for this field object.
	 *
	 * @return a hash code value for this field.
	 */
	@Override
	public int hashCode() {
		return fieldTypeName.hashCode();
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

			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
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
	 * Checks if field attributes {@link #fieldTypeName} and {@link #valueType} values contains variable expressions.
	 *
	 * @return {@code true} if field attribute values contains variable expressions, {@code false} - otherwise
	 */
	public boolean hasDynamicAttrs() {
		return hasDynamicAttrs(fieldTypeName, valueType);
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
	 * Creates temporary field taking this field as template and filling dynamic attributes values with ones from
	 * dynamic locators resolved values map.
	 *
	 * @param dValues
	 *            dynamic locators resolved values map
	 * @param valueIndex
	 *            index of value in collection
	 *
	 * @return temporary field instance
	 */
	public ActivityField createTempField(Map<String, Object> dValues, int valueIndex) {
		ActivityField tField = new ActivityField(fillDynamicAttr(fieldTypeName, dValues, valueIndex));
		tField.locators = getTempFieldLocators(locators, valueIndex);
		tField.separator = separator;
		tField.requiredVal = requiredVal;
		tField.stackedParsers = stackedParsers;
		tField.valueType = fillDynamicAttr(valueType, dValues, valueIndex);
		tField.transparent = transparent;
		tField.parser = parser;

		return tField;
	}

	private static String fillDynamicAttr(String dAttr, Map<String, Object> dValMap, int valueIndex) {
		String tAttr = dAttr;

		if (isDynamicAttr(dAttr) && MapUtils.isNotEmpty(dValMap)) {
			List<String> vars = new ArrayList<>();
			Utils.resolveCfgVariables(vars, dAttr);

			for (String var : vars) {
				tAttr = tAttr.replace(var, String.valueOf(Utils.getItem(dValMap.get(var), valueIndex)));
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

	@Override
	protected ValueTransformation.Phase getDefaultTransformationPhase() {
		return ValueTransformation.Phase.AGGREGATED;
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
	 *             format, etc.)
	 *
	 * @see #transform(Object, ActivityInfo)
	 * @see #filterFieldValue(Object, ActivityInfo)
	 */
	public Object aggregateFieldValue(Object value, ActivityInfo ai) throws ParseException {
		LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"ActivityField.aggregating.field", this, Utils.toString(value));
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

		LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"ActivityField.aggregating.field.value", this, Utils.toString(fieldValue));

		if (fieldValue != null) {
			fieldValue = transform(fieldValue, ai);
			fieldValue = filterFieldValue(fieldValue, ai);

			if (fieldValue != null && isEmptyAsNull() && Utils.isEmptyContent(fieldValue, true)) {
				LOGGER.log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityField.field.empty.as.null", this, Utils.toStringDump(fieldValue));
				fieldValue = null;
			}
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
	 * @see #transformValue(Object, ActivityInfo, com.jkoolcloud.tnt4j.streams.transform.ValueTransformation.Phase)
	 */
	protected Object transform(Object fieldValue, ActivityInfo ai) {
		try {
			fieldValue = transformValue(fieldValue, ai, ValueTransformation.Phase.AGGREGATED);
		} catch (Exception exc) {
			Utils.logThrowable(LOGGER, OpLevel.WARNING,
					StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityField.transformation.failed", fieldTypeName, Utils.toString(fieldValue), exc);
		}

		return fieldValue;
	}

	/**
	 * Applies filed defined filtering rules and marks this activity as filtered out or sets field value to {@code
	 * null}, if field is set as "optional" using attribute {@code required=false}.
	 *
	 * @param value
	 *            value to apply filters @param ai activity info instance to alter "filtered out" flag @return value
	 *            after filtering applied: {@code null} if value gets filtered out and field is optional, or same as
	 *            passed over parameters - otherwise
	 *
	 * @see #filterValue(Object, ActivityInfo)
	 */
	protected Object filterFieldValue(Object value, ActivityInfo ai) {
		try {
			boolean filteredOut = filterValue(value, ai);

			if (filteredOut) {
				if (isOptional()) {
					return null;
				} else {
					ai.setFiltered(true);
				}
			}

			return value;
		} catch (Exception exc) {
			Utils.logThrowable(LOGGER, OpLevel.WARNING,
					StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME), "ActivityField.filtering.failed",
					fieldTypeName, Utils.toString(value), exc);
			return value;
		}
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
				value = StringUtils.containsOnly(strValue, "0123456789") ? Integer.valueOf(strValue) // NON-NLS
						: strValue.toUpperCase().trim();
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
					TimeUnit units = locator == null ? TimeUnit.MICROSECONDS : locator.getBuiltInUnits();
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
		private MatchingParserReference parserRef;
		private AggregationType aggregationType;
		private ParserApplyType applyOn;

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
		public Boolean matchTags(String[] dataTags) {
			return parserRef.matchTags(dataTags);
		}

		@Override
		public Boolean matchExp(NamedObject caller, Object value, ActivityInfo ai, ActivityField field) {
			return parserRef.matchExp(caller, value, ai, field);
		}

		@Override
		public String toString() {
			return getParser().getName() + ":" + aggregationType + ":" + applyOn; // NON-NLS
		}
	}
}
