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

package com.jkoolcloud.tnt4j.streams.utils;

import java.util.regex.Pattern;

import org.apache.commons.lang3.math.NumberUtils;

/**
 * Defines {@link Integer} type numeric range.
 *
 * @version $Revision: 1 $
 */
public class IntRange extends Range<Integer> {
	private static final Pattern I_PATTERN = Pattern.compile(NEGATIVE_REGEX + INT_REGEX);
	private static final Pattern I_PATTERN_POSITIVE = Pattern.compile(INT_REGEX);

	/**
	 * Constructs a new IntRange.
	 *
	 * @param from
	 *            range lower bound
	 * @param to
	 *            range upper bound
	 */
	public IntRange(Integer from, Integer to) {
		super(from, to);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Number) {
			return inRange(((Number) obj).intValue());
		}

		if (obj instanceof String) {
			try {
				return inRange(Integer.parseInt((String) obj));
			} catch (NumberFormatException exc) {
			}
		}

		return super.equals(obj);
	}

	@Override
	public boolean inRange(Integer num) {
		int compareMin = getFrom().compareTo(num);
		int compareMax = getTo().compareTo(num);

		return compareMin <= 0 && compareMax >= 0;
	}

	/**
	 * Same as {@link #getRange(String, boolean)} setting {@code positive} flag to {@code false}.
	 *
	 * @param rangeStr
	 *            range definition string to parse
	 * @return integer range parsed from range definition string
	 * @throws Exception
	 *             if range string can't be parsed
	 * @see #getRange(String, boolean)
	 */
	public static IntRange getRange(String rangeStr) throws Exception {
		return getRange(rangeStr, false);
	}

	/**
	 * Makes range object using values parsed from {@code rangeStr}.
	 * <p>
	 * If {@code rangeStr} has missing range bound values, default ones are set: lower
	 * {@code positive ? 0 : -Integer.MAX_VALUE}, upper {@code Integer.MAX_VALUE}.
	 * <p>
	 * Range separator symbol is {@value com.jkoolcloud.tnt4j.streams.utils.Range#RANGE_SEPARATOR}.
	 *
	 * @param rangeStr
	 *            range definition string to parse
	 * @param positive
	 *            {@code true} means range has only positive values, {@code} - range can have negative values.
	 * @return integer range parsed from range definition string
	 * @throws Exception
	 *             if range string can't be parsed
	 * @see Range#parseRange(String, Pattern)
	 */
	public static IntRange getRange(String rangeStr, boolean positive) throws Exception {
		String[] rangeTokens = parseRange(rangeStr, positive ? I_PATTERN_POSITIVE : I_PATTERN);

		int from = NumberUtils.toInt(rangeTokens[0], positive ? 0 : -Integer.MAX_VALUE);
		int to = NumberUtils.toInt(rangeTokens[1], Integer.MAX_VALUE);

		return new IntRange(from, to);
	}

	/**
	 * Checks if number value is in range bounds inclusive using provided range instance {@code range}.
	 *
	 * @param range
	 *            range instance to use for checking
	 * @param num
	 *            number value to check
	 * @return flag indicating number is in range bounds inclusive, or {@code range} is {@code null}
	 *
	 * @see #inRange(Integer)
	 */
	public static boolean inRange(IntRange range, Integer num) {
		return range == null || range.inRange(num);
	}
}
