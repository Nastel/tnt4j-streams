/*
 * Copyright 2014-2022 JKOOL, LLC.
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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.filters.StreamFiltersGroup;
import com.jkoolcloud.tnt4j.streams.transform.ValueTransformation;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Base class for common activity field entities: field, field (value) locator, etc.
 *
 * @version $Revision: 1 $
 */
public abstract class AbstractFieldEntity {

	/**
	 * Field entity value resolution basis flag: {@code required="true"}, {@code optional="false"}, {@code default=""}.
	 */
	protected String requiredVal = ""; /* string to allow no value */

	protected boolean emptyAsNull = true;

	/**
	 * Field entity transformations list used to transform values.
	 */
	protected Collection<ValueTransformation<Object, Object>> transformations;
	/**
	 * Field entity filters group used to filter values.
	 */
	protected StreamFiltersGroup<Object> filter;

	/**
	 * Returns logger used by this activity field entity.
	 *
	 * @return activity field entity logger
	 */
	protected abstract EventSink logger();

	/**
	 * Sets the required option flag to indicator if field entity value is optional - can be {@code null}.
	 *
	 * @param requiredVal
	 *            {@code true}/{@code false} string
	 *
	 * @return instance of this field entity
	 */
	public AbstractFieldEntity setRequired(String requiredVal) {
		this.requiredVal = requiredVal;

		return this;
	}

	/**
	 * Determines whether value resolution by field entity is required depending on stream context. For example XML/JSON
	 * parser context may require all field entities to resolve non {@code null} values by default.
	 *
	 * @return flag indicating value resolution by field entity is required depending on stream context
	 */
	public boolean isDefaultRequire() {
		return StringUtils.isEmpty(requiredVal); // NON-NLS
	}

	/**
	 * Determines whether value resolution by field entity is optional.
	 *
	 * @return flag indicating value resolution by field entity is optional
	 */
	public boolean isOptional() {
		return "false".equalsIgnoreCase(requiredVal); // NON-NLS
	}

	/**
	 * Determines whether value resolution by field entity is required.
	 *
	 * @return flag indicating value resolution by field entity is required
	 */
	public boolean isRequired() {
		return "true".equalsIgnoreCase(requiredVal); // NON-NLS
	}

	/**
	 * Determines whether resolved empty value shall be treated as {@code null}.
	 * <p>
	 * Empty values are one of:
	 * <ul>
	 * <li>String equal to {@code ""}</li>
	 * <li>Array having all {@code null} elements</li>
	 * <li>Collection having all {@code null} elements</li>
	 * </ul>
	 *
	 * @return flag indicating resolved empty value shall be treated as {@code null}
	 */
	public boolean isEmptyAsNull() {
		return emptyAsNull;
	}

	/**
	 * Sets flag indicating resolved empty value shall be treated as {@code null}.
	 * 
	 * @param emptyAsNull
	 *            flag indicating resolved empty value shall be treated as {@code null}
	 *
	 * @return instance of this field entity
	 */
	public AbstractFieldEntity setEmptyAsNull(boolean emptyAsNull) {
		this.emptyAsNull = emptyAsNull;

		return this;
	}

	/**
	 * Adds data value transformation instance to field entity transformations list.
	 *
	 * @param transformation
	 *            transformation to add
	 */
	public void addTransformation(ValueTransformation<Object, Object> transformation) {
		if (transformations == null) {
			transformations = new ArrayList<>();
		}

		if (transformation.getPhase() == null) {
			transformation.setPhase(ValueTransformation.Phase.FORMATTED);
		}

		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"AbstractFieldEntity.adding.transformation", this, transformation);

		transformations.add(transformation);
	}

	/**
	 * Transforms provided object value using defined transformations. If more than one transformation defined,
	 * transformations are applied sequentially where transformation input data is output of previous transformation.
	 *
	 * @param fieldValue
	 *            value to transform
	 * @param ai
	 *            activity entity instance to get additional value for a transformation
	 * @param phase
	 *            activity data resolution phase defining transformations to apply
	 * @return transformed value
	 * @throws Exception
	 *             if transformation operation fails
	 */
	public Object transformValue(Object fieldValue, ActivityInfo ai, ValueTransformation.Phase phase) throws Exception {
		if (CollectionUtils.isEmpty(transformations)) {
			return fieldValue;
		}

		logger().log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"AbstractFieldEntity.value.before.transformations", this, phase, Utils.toString(fieldValue));
		Object tValue = fieldValue;
		for (ValueTransformation<Object, Object> vt : transformations) {
			if (vt.getPhase().equals(phase)) {
				tValue = vt.transform(tValue, ai, getName());
				logger().log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"AbstractFieldEntity.value.after.transformation", this, vt.getName(), Utils.toString(tValue));
			}
		}

		return tValue;
	}

	/**
	 * Sets field filters group instance to be used by this field entity to filter resolved values.
	 *
	 * @param ffg
	 *            field filters group
	 *
	 * @return instance of this field entity
	 */
	public AbstractFieldEntity setFilter(StreamFiltersGroup<Object> ffg) {
		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"AbstractFieldEntity.adding.filter", this, ffg.getName());

		this.filter = ffg;

		return this;
	}

	/**
	 * Applies field group filters on provided value to check if value should be filtered out from streaming.
	 *
	 * @param fieldValue
	 *            value to apply filters
	 * @param ai
	 *            activity entity instance to get additional value for a filtering
	 * @return {@code true} if value is filtered out, {@code false} - otherwise
	 * @throws Exception
	 *             if evaluation of filter fails
	 */
	public boolean filterValue(Object fieldValue, ActivityInfo ai) throws Exception {
		if (filter == null) {
			return false;
		}

		boolean filteredOut = filter.doFilter(fieldValue, ai, getName());

		logger().log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"AbstractFieldEntity.filtering.result", this, filter.getName(), filteredOut);

		return filteredOut;
	}

	/**
	 * Checks if activity field/locator transformations contains activity entity field name variables.
	 *
	 * @return {@code true} if activity field/locator transformations contains activity entity field name variables,
	 *         {@code false} - otherwise
	 * 
	 * @see com.jkoolcloud.tnt4j.streams.transform.ValueTransformation#hasActivityReferences()
	 */
	public boolean hasActivityTransformations() {
		if (transformations != null) {
			for (ValueTransformation<?, ?> vt : transformations) {
				if (vt.hasActivityReferences()) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Returns activity field/locator name.
	 * 
	 * @return entity name
	 */
	abstract String getName();
}
