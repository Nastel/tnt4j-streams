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

package com.jkoolcloud.tnt4j.streams.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Base class defining numeric range.
 *
 * @version $Revision: 1 $
 *
 * @see DoubleRange
 * @see IntRange
 */
public abstract class Range<T extends Number> {
	/**
	 * Constant defining number minus sign RegEx.
	 */
	public static final String NEGATIVE_REGEX = "-?"; // NON-NLS
	/**
	 * Constant defining integer number RegEx.
	 */
	public static final String INT_REGEX = "(\\d)+"; // NON-NLS
	/**
	 * Constant defining double number RegEx.
	 */
	public static final String DOUBLE_REGEX = INT_REGEX + "(\\.)?(\\d)*"; // NON-NLS

	/**
	 * Constant for range separator.
	 */
	public static final String RANGE_SEPARATOR = ":"; // NON-NLS

	private T from;
	private T to;

	/**
	 * Constructs a new Range.
	 * 
	 * @param from
	 *            range lower bound
	 * @param to
	 *            range upper bound
	 */
	protected Range(T from, T to) {
		this.from = from;
		this.to = to;
	}

	/**
	 * Returns range lower bound.
	 *
	 * @return range lower bound
	 */
	public T getFrom() {
		return from;
	}

	/**
	 * Returns range upper bound.
	 *
	 * @return range upper bound
	 */
	public T getTo() {
		return to;
	}

	/**
	 * Checks if number value is in range bounds inclusive.
	 *
	 * @param num
	 *            number value to check
	 * @return flag indicating number is in range bounds inclusive
	 */
	public abstract boolean inRange(T num);

	@Override
	public String toString() {
		return String.valueOf(from) + RANGE_SEPARATOR + String.valueOf(to);
	}

	@Override
	public int hashCode() {
		int result = from.hashCode();
		result = 31 * result + to.hashCode();
		return result;
	}

	/**
	 * Parses string defining range values. Range definition separator is {@value #RANGE_SEPARATOR}.
	 * 
	 * @param rangeStr
	 *            range definition string to parse
	 * @param pattern
	 *            RegEx pattern to use
	 * @return string array where first element defines range lower bound and second element defines upper bound
	 * @throws Exception
	 *             if range string can't be parsed
	 */
	protected static String[] parseRange(String rangeStr, Pattern pattern) throws Exception {
		String cs = StringUtils.trimToNull(rangeStr);
		if (StringUtils.isEmpty(cs)) {
			throw new IllegalArgumentException(
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "Range.range.string.empty"));
		}
		int rCharIdx = cs.indexOf(RANGE_SEPARATOR);

		String[] numStrs = new String[2];
		int si = 0;
		Matcher m = pattern.matcher(cs);
		while (m.find()) {
			String g = m.group();
			if (StringUtils.isNotEmpty(g)) {
				numStrs[si++] = g;
			}
			m.end();
		}

		String fromStr = null;
		String toStr = null;

		if (rCharIdx == -1) { // no range separator symbol found - unary range
			fromStr = numStrs.length > 0 ? numStrs[0] : null;
			toStr = fromStr;
		} else {
			if (rCharIdx == 0) { // unbound low range
				toStr = numStrs.length > 0 ? numStrs[0] : null;
			} else if (rCharIdx == cs.length()) { // unbound high range
				fromStr = numStrs.length > 0 ? numStrs[0] : null;
			} else { // bounded range
				fromStr = numStrs.length > 0 ? numStrs[0] : null;
				toStr = numStrs.length > 1 ? numStrs[1] : null;
			}
		}

		numStrs[0] = fromStr;
		numStrs[1] = toStr;

		return numStrs;
	}
}
