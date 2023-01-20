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

package com.jkoolcloud.tnt4j.streams.filters;

import java.util.*;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldFormatType;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Data value filtering based on direct filtered object value evaluation. Filtered object value may be formatted before
 * evaluating, but it does not affect filtered object value itself.
 *
 * @version $Revision: 1 $
 */
public class DefaultValueFilter extends AbstractEntityFilter<Object> {

	private String value;
	private HandleType handleType;
	private EvaluationType evalType;
	private ActivityFieldFormatType format;

	private Pattern matchPattern;
	private Set<String> valuesSet;

	/**
	 * Constructs a new DefaultValueFilter. Handle type is set to
	 * {@link com.jkoolcloud.tnt4j.streams.filters.HandleType#INCLUDE}.
	 *
	 * @param value
	 *            value to be used by filter for filtered data evaluation
	 */
	public DefaultValueFilter(String value) {
		this(null, null, null, value);
	}

	/**
	 * Constructs a new DefaultValueFilter.
	 *
	 * @param handleType
	 *            filter {@link com.jkoolcloud.tnt4j.streams.filters.HandleType} name
	 * @param evalType
	 *            filter {@link com.jkoolcloud.tnt4j.streams.filters.EvaluationType} name
	 * @param format
	 *            data format to be applied on filtered data before evaluating filter value
	 * @param value
	 *            value to be used by filter for filtered data evaluation
	 */
	public DefaultValueFilter(String handleType, String evalType, String format, String value) {
		this.handleType = StringUtils.isEmpty(handleType) ? HandleType.INCLUDE
				: HandleType.valueOf(handleType.toUpperCase());
		this.evalType = StringUtils.isEmpty(evalType) ? EvaluationType.IS
				: EvaluationType.valueOf(evalType.toUpperCase());
		this.format = StringUtils.isEmpty(format) ? null : ActivityFieldFormatType.valueOf(format);

		if (StringUtils.isEmpty(value)) {
			throw new IllegalArgumentException(StreamsResources
					.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME, "DefaultValueFilter.empty.value"));
		}

		this.value = value;

		initFilter();
	}

	/**
	 * Performs filter initialization: makes RegEx {@link java.util.regex.Pattern} when filter evaluation type is
	 * {@link com.jkoolcloud.tnt4j.streams.filters.EvaluationType#WILDCARD} or
	 * {@link com.jkoolcloud.tnt4j.streams.filters.EvaluationType#REGEX}.
	 */
	@Override
	protected void initFilter() {
		if (evalType == EvaluationType.WILDCARD) {
			matchPattern = Pattern.compile(Utils.wildcardToRegex2(value));
		} else if (evalType == EvaluationType.REGEX) {
			matchPattern = Pattern.compile(value);
		} else {
			String[] va = Utils.splitValue(value);
			valuesSet = new HashSet<>(va.length);

			Collections.addAll(valuesSet, va);
		}

		// TODO: values conversions: i.e. from MQ constant names to numeric.
	}

	@Override
	public HandleType getHandleType() {
		return handleType;
	}

	@Override
	public StreamFilterType getFilterType() {
		return StreamFilterType.VALUE;
	}

	@Override
	public boolean doFilter(Object value, ActivityInfo ai, String fName) {
		boolean match = false;

		if (matchPattern != null) {
			String fvStr = formatValue(value);
			match = matchPattern.matcher(fvStr).matches();
		} else if (evalType == EvaluationType.CONTAINS) {
			match = contains(valuesSet, value);
		} else {
			String fvStr = formatValue(value);
			match = equals(valuesSet, fvStr);
		}

		return isFilteredOut(handleType, match);
	}

	@Override
	public boolean doFilter(Map<String, ?> valBindings) throws FilterException {
		throw new FilterException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
				"DefaultValueFilter.unsupported.operation"));
	}

	/**
	 * Applies formatting of filtered object value before evaluation.
	 * 
	 * @param value
	 *            filtered object value
	 * @return formatted filtered object value
	 */
	protected String formatValue(Object value) {
		String fValue;

		if (value == null) {
			fValue = String.valueOf(value);
		} else if (format == ActivityFieldFormatType.hexBinary) {
			fValue = Utils.toHexString((byte[]) value);
		} else if (format == ActivityFieldFormatType.base64Binary) {
			fValue = Utils.base64EncodeStr((byte[]) value);
		} else {
			fValue = Utils.toString(value);
		}

		return fValue;
	}

	/**
	 * Evaluates whether filtered object contains any of filter defined values.
	 * 
	 * @param filterValues
	 *            filter defined values set
	 * @param fieldValue
	 *            filtered object value
	 * @return {@code true} if filtered object contains filter defined value, {@code false} - otherwise
	 */
	protected boolean contains(Set<String> filterValues, Object fieldValue) {
		if (fieldValue instanceof String) {
			return contains(filterValues, (String) fieldValue);
		} else if (fieldValue instanceof Collection<?>) {
			for (Object cv : (Collection<?>) fieldValue) {
				String cvs = Utils.toString(cv);
				if (equals(filterValues, cvs)) {
					return true;
				}
			}
		} else if (Utils.isObjArray(fieldValue)) {
			for (Object av : (Object[]) fieldValue) {
				String avs = Utils.toString(av);
				if (equals(filterValues, avs)) {
					return true;
				}
			}
		} else {
			String fvStr = formatValue(fieldValue);

			return contains(filterValues, fvStr);
		}

		return false;
	}

	/**
	 * Evaluates whether formatted filtered object string equals any of filter defined values.
	 * 
	 * @param filterValues
	 *            filter defined values set
	 * @param fvStr
	 *            formatted filtered object value string
	 * @return {@code true} if filtered object contains filter defined value, {@code false} - otherwise
	 */
	protected boolean equals(Set<String> filterValues, String fvStr) {
		if (valuesSet != null) {
			for (String v : filterValues) {
				if (Objects.equals(v, fvStr)) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Evaluates whether formatted filtered object string contains any of filter defined values.
	 *
	 * @param filterValues
	 *            filter defined values set
	 * @param fvStr
	 *            formatted filtered object value string
	 * @return {@code true} if filtered object contains filter defined value, {@code false} - otherwise
	 */
	protected boolean contains(Set<String> filterValues, String fvStr) {
		if (valuesSet != null) {
			for (String v : filterValues) {
				if (fvStr.contains(v)) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("DefaultValueFilter{"); // NON-NLS
		sb.append("handleType=").append(handleType); // NON-NLS
		sb.append(", evalType=").append(evalType); // NON-NLS
		if (format != null) {
			sb.append(", format=").append(format); // NON-NLS
		}
		sb.append(", value=").append(Utils.sQuote(value)); // NON-NLS
		sb.append('}');
		return sb.toString();
	}
}
