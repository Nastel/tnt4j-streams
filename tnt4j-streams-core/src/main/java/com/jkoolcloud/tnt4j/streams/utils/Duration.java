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

import java.text.ParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import com.jkoolcloud.tnt4j.core.UsecTimestamp;

/**
 * Provides functionality to calculate duration between two events in milliseconds.
 *
 * @version $Revision: 1 $
 */
public class Duration {

	private long startTime;

	private Duration() {
	}

	/**
	 * Sets period {@code "start time"} to current system time in milliseconds for this duration instance.
	 *
	 * @see #now()
	 */
	public void set() {
		this.startTime = now();
	}

	/**
	 * Calculates duration between {@code "now"} and {@code "start time"} in milliseconds.
	 *
	 * @return duration value in milliseconds
	 *
	 * @see #duration(long)
	 */
	public long duration() {
		return duration(this.startTime);
	}

	/**
	 * Calculates duration between {@code "now"} and {@code "start time"} using {@code tUnit} defined units.
	 *
	 * @return duration value in {@code tUnit} defined units
	 *
	 * @see #duration(long, java.util.concurrent.TimeUnit)
	 */
	public long duration(TimeUnit tUnit) {
		return duration(this.startTime, tUnit);
	}

	/**
	 * Represents duration value in human-readable form: {@code "hours:minutes:seconds.millis"}
	 *
	 * @return human readable duration value string
	 *
	 * @see #durationHMS(long)
	 */
	public String durationHMS() {
		return durationHMS(this.startTime);
	}

	/**
	 * Calculates duration between {@code "now"} and {@code "start time"} in milliseconds, and resets period
	 * {@code "start time"} to {@code "now"} for this duration instance.
	 *
	 * @return duration value in milliseconds
	 */
	public long reset() {
		long now = now();
		long duration = now - startTime;
		startTime = now;

		return duration;
	}

	/**
	 * Returns system current time in milliseconds.
	 *
	 * @return system current time in milliseconds
	 *
	 * @see System#currentTimeMillis()
	 */
	public static long now() {
		return System.currentTimeMillis();
	}

	/**
	 * Constructs a new duration object instance and sets duration period {@code "start time"} value to current system
	 * time in milliseconds.
	 *
	 * @return constructed duration instance
	 *
	 * @see #set()
	 */
	public static Duration arm() {
		Duration d = new Duration();
		d.set();

		return d;
	}

	/**
	 * Calculates duration between {@code "now"} and provided {@code "startTime"} in milliseconds.
	 *
	 * @param startTime
	 *            duration period start time in milliseconds
	 * @return duration value in milliseconds
	 *
	 * @see #now()
	 */
	public static long duration(long startTime) {
		return now() - startTime;
	}

	/**
	 * Calculates duration between {@code "now"} and provided {@code "startTime"} using {@code tUnit} defined units.
	 *
	 * @param startTime
	 *            duration period start time in milliseconds
	 * @param tUnit
	 *            duration period time units
	 * @return duration value in {@code tUnit} defined units
	 *
	 * @see #duration(long)
	 * @see java.util.concurrent.TimeUnit#convert(long, java.util.concurrent.TimeUnit)
	 */
	public static long duration(long startTime, TimeUnit tUnit) {
		return tUnit.convert(duration(startTime), TimeUnit.MILLISECONDS);
	}

	/**
	 * Represents duration value in human-readable form: {@code "hours:minutes:seconds.millis"}
	 *
	 * @param startTime
	 *            duration period start time in milliseconds
	 * @return human readable duration value string
	 *
	 * @see #duration(long)
	 * @see org.apache.commons.lang3.time.DurationFormatUtils#formatDurationHMS(long)
	 */
	public static String durationHMS(long startTime) {
		return DurationFormatUtils.formatDurationHMS(duration(startTime));
	}

	/**
	 * Finds time zone complying RFC 822 standard from provided reference and shift times provided in milliseconds. To
	 * comply all standard timezones, time rounding is set to {@code 15} minutes.
	 * 
	 * @param refTme
	 *            reference time
	 * @param shiftTime
	 *            shift time
	 * @return RFC 822 time zone
	 * 
	 * @throws java.lang.NullPointerException
	 *             if {@code refTime} or {@code shiftTime} is null
	 * 
	 * @see #getTimeZoneRFC822(Number, Number, int)
	 */
	public static String getTimeZoneRFC822(Number refTme, Number shiftTime) throws NullPointerException {
		return getTimeZoneRFC822(refTme, shiftTime, 15);
	}

	/**
	 * Finds time zone complying RFC 822 standard from provided reference and shift times provided in milliseconds.
	 * 
	 * @param refTme
	 *            reference time
	 * @param shiftTime
	 *            shift time
	 * @param roundMin
	 *            time round to nearest amount of minutes
	 * @return RFC 822 time zone
	 * 
	 * @throws java.lang.NullPointerException
	 *             if {@code refTime} or {@code shiftTime} is null
	 * 
	 * @see #getTimeZoneRFC822(long, long, java.util.concurrent.TimeUnit, int)
	 */
	public static String getTimeZoneRFC822(Number refTme, Number shiftTime, int roundMin) throws NullPointerException {
		return getTimeZoneRFC822(refTme, shiftTime, TimeUnit.MILLISECONDS, roundMin);
	}

	/**
	 * Finds time zone complying RFC 822 standard from provided reference and shift times provided in microseconds. To
	 * comply all standard timezones, time rounding is set to {@code 15} minutes.
	 * 
	 * @param refTme
	 *            reference time
	 * @param shiftTime
	 *            shift time
	 * @return RFC 822 time zone
	 * 
	 * @throws java.lang.NullPointerException
	 *             if {@code refTime} or {@code shiftTime} is null
	 * 
	 * @see #getTimeZoneRFC822Usec(Number, Number, int)
	 */
	public static String getTimeZoneRFC822Usec(Number refTme, Number shiftTime) throws NullPointerException {
		return getTimeZoneRFC822Usec(refTme, shiftTime, 15);
	}

	/**
	 * Finds time zone complying RFC 822 standard from provided reference and shift times provided in microseconds.
	 * 
	 * @param refTme
	 *            reference time
	 * @param shiftTime
	 *            shift time
	 * @param roundMin
	 *            time round to nearest amount of minutes
	 * @return RFC 822 time zone
	 * 
	 * @throws java.lang.NullPointerException
	 *             if {@code refTime} or {@code shiftTime} is null
	 * 
	 * @see #getTimeZoneRFC822(Number, Number, java.util.concurrent.TimeUnit, int)
	 */
	public static String getTimeZoneRFC822Usec(Number refTme, Number shiftTime, int roundMin)
			throws NullPointerException {
		return getTimeZoneRFC822(refTme, shiftTime, TimeUnit.MICROSECONDS, roundMin);
	}

	/**
	 * Finds time zone complying RFC 822 standard from provided reference and shift times.
	 * 
	 * @param refTme
	 *            reference time
	 * @param shiftTime
	 *            shift time
	 * @param tUnit
	 *            reference and shift time units
	 * @param roundMin
	 *            time round to nearest amount of minutes
	 * @return RFC 822 time zone
	 * 
	 * @throws java.lang.NullPointerException
	 *             if {@code refTime} or {@code shiftTime} is null
	 * 
	 * @see #getTimeZoneRFC822(long, long, java.util.concurrent.TimeUnit, int)
	 */
	public static String getTimeZoneRFC822(Number refTme, Number shiftTime, TimeUnit tUnit, int roundMin)
			throws NullPointerException {
		return getTimeZoneRFC822(refTme.longValue(), shiftTime.longValue(), tUnit, roundMin);
	}

	/**
	 * Finds time zone complying RFC 822 standard from provided reference and shift times.
	 * 
	 * @param refTme
	 *            reference time
	 * @param shiftTime
	 *            shift time
	 * @param tUnit
	 *            reference and shift time units
	 * @param roundMin
	 *            time round to nearest amount of minutes
	 * @return RFC 822 time zone
	 */
	public static String getTimeZoneRFC822(long refTme, long shiftTime, TimeUnit tUnit, int roundMin) {
		long roundDiff = roundDuration(refTme, shiftTime, tUnit, roundMin);

		String offset = DurationFormatUtils.formatDuration(Math.abs(roundDiff), "HHmm"); // NON-NLS
		return (roundDiff < 0 ? "-" : "+") + offset; // NON-NLS
	}

	private static long roundDuration(long refTme, long shiftTime, TimeUnit tUnit, int roundMin) {
		long roundMinMsec = TimeUnit.MINUTES.toMillis(roundMin);
		int upMin = (roundMin / 2) + 1;

		long timeDiff = shiftTime - refTme;
		long timeDiffMsec = (tUnit == null ? TimeUnit.MILLISECONDS : tUnit).toMillis(timeDiff);
		long timeDiffMsecAbs = Math.abs(timeDiffMsec);
		long round = timeDiffMsecAbs % roundMinMsec;
		timeDiffMsecAbs -= round;
		if (TimeUnit.MILLISECONDS.toMinutes(round) >= upMin) {
			timeDiffMsecAbs += roundMinMsec;
		}

		return timeDiffMsec < 0 ? -timeDiffMsecAbs : timeDiffMsecAbs;
	}

	/**
	 * Parses provided duration string {@code durationStr} to {@link java.time.Duration} instance.
	 * <p>
	 * Duration string shall be defined to match {@code HH:mm:ss.SSSSSSSSS} pattern, where {@code HH} and
	 * {@code SSSSSSSSS} parts can be variable length.
	 *
	 * @param durationStr
	 *            duration string to parse
	 * @return duration instance parsed from provided duration string, or {@code null} if provided duration string is
	 *         {@code null} or empty
	 */
	public static java.time.Duration parseHMSDuration(String durationStr) {
		if (StringUtils.isEmpty(durationStr)) {
			return null;
		}

		return parseHMSDuration(durationStr, DurationParser.DEFAULT_HMS_DURATION_PATTERN);
	}

	/**
	 * Parses provided duration string {@code durationStr} to {@link java.time.Duration} instance using defined duration
	 * string format RegEx pattern string {@code ptrRegex} or date-time pattern using symbols from
	 * {@link java.text.SimpleDateFormat}.
	 * <p>
	 * RegEx pattern string shall define these group names to properly resolve values:
	 * <ul>
	 * <li>years (date-time pattern token {@code yy}) - for years definition, assuming year has 365 days</li>
	 * <li>months (date-time pattern token {@code MM}) - for months definition, assuming month has 30 days</li>
	 * <li>weeks (date-time pattern token {@code ww}) - for weeks definition, assuming week has 7 days</li>
	 * <li>days (date-time pattern token {@code dd}) - for days definition</li>
	 * <li>hours (date-time pattern token {@code HH}) - for hours definition</li>
	 * <li>minutes (date-time pattern token {@code mm}) - for minutes definition</li>
	 * <li>seconds (date-time pattern token {@code ss}) - for seconds definition</li>
	 * <li>fraction (date-time pattern token from {@code SS} to {@code SSSSSSSSS}) - for fractional pars of the second:
	 * milliseconds, microsecond and nanoseconds</li>
	 * </ul>
	 *
	 * @param durationStr
	 *            duration string to parse
	 * @param ptrRegex
	 *            duration string format RegEx or date-time pattern string
	 * @return duration instance parsed from provided duration string, or {@code null} if provided duration string is
	 *         {@code null} or empty
	 */
	public static java.time.Duration parseHMSDuration(String durationStr, String ptrRegex) {
		if (StringUtils.isEmpty(durationStr)) {
			return null;
		}

		return DurationParser.parseHMSDuration(durationStr, ptrRegex);
	}

	/**
	 * Parses provided duration string {@code durationStr} to {@link UsecTimestamp} instance.
	 * <p>
	 * Duration string shall be defined to match {@code HH:mm:ss.SSSSSSSSS} pattern, where {@code HH} and
	 * {@code SSSSSSSSS} parts can be variable length.
	 *
	 * @param durationStr
	 *            duration string to parse
	 * @return timestamp instance parsed from provided duration string, or {@code null} if provided duration string is
	 *         {@code null} or empty
	 * 
	 * @see #parseHMSDuration(String)
	 */
	public static UsecTimestamp parseHMSDurationToTimestamp(String durationStr) {
		return DurationParser.durationToTimestamp(parseHMSDuration(durationStr));
	}

	/**
	 * Parses provided duration string {@code durationStr} to {@link UsecTimestamp} instance using defined duration
	 * string format RegEx pattern string {@code ptrRegex} or date-time pattern using symbols from
	 * {@link java.text.SimpleDateFormat}.
	 * <p>
	 * RegEx pattern string shall define these group names to properly resolve values:
	 * <ul>
	 * <li>years (date-time pattern token {@code yy}) - for years definition, assuming year has 365 days</li>
	 * <li>months (date-time pattern token {@code MM}) - for months definition, assuming month has 30 days</li>
	 * <li>weeks (date-time pattern token {@code ww}) - for weeks definition, assuming week has 7 days</li>
	 * <li>days (date-time pattern token {@code dd}) - for days definition</li>
	 * <li>hours (date-time pattern token {@code HH}) - for hours definition</li>
	 * <li>minutes (date-time pattern token {@code mm}) - for minutes definition</li>
	 * <li>seconds (date-time pattern token {@code ss}) - for seconds definition</li>
	 * <li>fraction (date-time pattern token from {@code SS} to {@code SSSSSSSSS}) - for fractional pars of the second:
	 * milliseconds, microsecond and nanoseconds</li>
	 * </ul>
	 *
	 * @param durationStr
	 *            duration string to parse
	 * @param ptrRegex
	 *            duration string format RegEx or date-time pattern string
	 * @return timestamp instance parsed from provided duration string, or {@code null} if provided duration string is
	 *         {@code null} or empty
	 *
	 * @see #parseHMSDuration(String, String)
	 */
	public static UsecTimestamp parseHMSDurationToTimestamp(String durationStr, String ptrRegex) {
		return DurationParser.durationToTimestamp(parseHMSDuration(durationStr, ptrRegex));
	}

	/**
	 * Converts provided {@code value} to a {@link java.time.Duration} without parsing.
	 * <p>
	 * If {@code value} is {@link Duration}, {@link java.time.Duration}, {@link Number} or
	 * {@link java.time.temporal.TemporalAmount} then cast or new instance of {@link java.time.Duration} is returned.
	 * {@code null} is returned otherwise.
	 *
	 * @param value
	 *            object value to get duration
	 * @param tUnit
	 *            time units of the value
	 * @return {@link java.time.Duration} built from provided {@code value}, or {@code null} if
	 *         {@link java.time.Duration} can't be built
	 */
	public static java.time.Duration getDuration(Object value, TimeUnit tUnit) {
		if (value instanceof Duration) {
			long dValue = ((Duration) value).duration(tUnit);

			return java.time.Duration.of(dValue, tUnit.toChronoUnit());
		}
		if (value instanceof java.time.Duration) {
			return (java.time.Duration) value;
		}
		if (value instanceof Number) {
			long dValue = ((Number) value).longValue();

			return java.time.Duration.of(dValue, tUnit.toChronoUnit());
		}
		if (value instanceof TemporalAmount) {
			return java.time.Duration.from((TemporalAmount) value);
		}

		return null;
	}

	/**
	 * Parses provided duration value {@code value} to {@link java.time.Duration} instance using defined duration format
	 * RegEx pattern string {@code ptrRegex} or date-time pattern using symbols from {@link java.text.SimpleDateFormat}.
	 * <p>
	 * RegEx pattern string shall define these group names to properly resolve values:
	 * <ul>
	 * <li>years (date-time pattern token {@code yy}) - for years definition, assuming year has 365 days</li>
	 * <li>months (date-time pattern token {@code MM}) - for months definition, assuming month has 30 days</li>
	 * <li>weeks (date-time pattern token {@code ww}) - for weeks definition, assuming week has 7 days</li>
	 * <li>days (date-time pattern token {@code dd}) - for days definition</li>
	 * <li>hours (date-time pattern token {@code HH}) - for hours definition</li>
	 * <li>minutes (date-time pattern token {@code mm}) - for minutes definition</li>
	 * <li>seconds (date-time pattern token {@code ss}) - for seconds definition</li>
	 * <li>fraction (date-time pattern token from {@code SS} to {@code SSSSSSSSS}) - for fractional pars of the second:
	 * milliseconds, microsecond and nanoseconds</li>
	 * </ul>
	 *
	 * @param value
	 *            duration value to parse
	 * @param format
	 *            duration string format RegEx or date-time pattern string, can be {@code null} to use default
	 *            {@code "HH:mm:ss.SSSSSSSSS"} formatting pattern
	 * @return {@link java.time.Duration} instance parsed from provided value
	 *
	 * @throws ParseException
	 *             if an error occurs while parsing the provided value based on provided pattern
	 * 
	 * @see #parseHMSDuration(String)
	 * @see #parseHMSDuration(String, String)
	 */
	public static java.time.Duration parseDuration(Object value, String format) throws ParseException {
		try {
			if (StringUtils.isEmpty(format)) {
				return Duration.parseHMSDuration(String.valueOf(value));
			} else {
				return Duration.parseHMSDuration(String.valueOf(value), format);
			}
		} catch (Exception exc) {
			throw new ParseException(exc.getMessage(), 0);
		}
	}

	private static class DurationParser {
		private static final Map<String, String> regexMap = new LinkedHashMap<>(15);
		static {
			regexMap.put("yy", "(?<years>\\d+)"); // NON-NLS
			regexMap.put("MM", "(?<months>\\d+)"); // NON-NLS
			regexMap.put("ww", "(?<weeks>\\d+)"); // NON-NLS
			regexMap.put("dd", "(?<days>\\d+)"); // NON-NLS
			regexMap.put("HH", "(?<hours>\\d+)"); // NON-NLS
			regexMap.put("mm", "(?<minutes>\\d{2})"); // NON-NLS
			regexMap.put("ss", "(?<seconds>\\d{2})"); // NON-NLS
			regexMap.put("SSSSSSSSS", "(?<fraction>\\d+)"); // NON-NLS
			regexMap.put("SSSSSSSS", "(?<fraction>\\d+)"); // NON-NLS
			regexMap.put("SSSSSSS", "(?<fraction>\\d+)"); // NON-NLS
			regexMap.put("SSSSSS", "(?<fraction>\\d+)"); // NON-NLS
			regexMap.put("SSSSS", "(?<fraction>\\d+)"); // NON-NLS
			regexMap.put("SSSS", "(?<fraction>\\d+)"); // NON-NLS
			regexMap.put("SSS", "(?<fraction>\\d+)"); // NON-NLS
			regexMap.put("SS", "(?<fraction>\\d+)"); // NON-NLS
		}

		private static final String DEFAULT_HMS_DURATION_PATTERN = "HH:mm:ss.SSSSSSSSS"; // NON-NLS
		private static final Map<String, Pattern> DURATION_PATTERNS_MAP = new HashMap<>(5);
		static {
			// Define the regex pattern to match HH:mm:ss.SSSSSSSSS, where SSSSSSSSS length is variable.
			DURATION_PATTERNS_MAP.put(DEFAULT_HMS_DURATION_PATTERN,
					translateDateTimePatternToRegEx(DEFAULT_HMS_DURATION_PATTERN));
		}

		private static Pattern translateDateTimePatternToRegEx(String dtPattern) {
			String regexPattern = dtPattern;
			for (Map.Entry<String, String> entry : regexMap.entrySet()) {
				regexPattern = regexPattern.replace(entry.getKey(), entry.getValue());
			}
			return Pattern.compile(regexPattern);
		}

		private static java.time.Duration parseHMSDuration(String durationStr, String durationPtr) {
			Pattern regEx = DURATION_PATTERNS_MAP.computeIfAbsent(durationPtr, s -> durationPtr.contains("(?<") // NON-NLS
					? Pattern.compile(durationPtr) : translateDateTimePatternToRegEx(durationPtr));

			return parseHMSDuration(durationStr, regEx);
		}

		private static java.time.Duration parseHMSDuration(String durationStr, Pattern pattern) {
			Matcher matcher = pattern.matcher(durationStr);

			if (!matcher.matches()) {
				throw new IllegalArgumentException(StreamsResources.getStringFormatted(
						StreamsResources.RESOURCE_BUNDLE_NAME, "Duration.invalid.format", durationStr)); // NON-NLS
			}

			// Extract and parse each component
			int years = getFieldValue(matcher, ChronoUnit.YEARS.name().toLowerCase());
			int months = getFieldValue(matcher, ChronoUnit.MONTHS.name().toLowerCase());
			int weeks = getFieldValue(matcher, ChronoUnit.WEEKS.name().toLowerCase());
			int days = getFieldValue(matcher, ChronoUnit.DAYS.name().toLowerCase());
			int hours = getFieldValue(matcher, ChronoUnit.HOURS.name().toLowerCase());
			int minutes = getFieldValue(matcher, ChronoUnit.MINUTES.name().toLowerCase());
			int seconds = getFieldValue(matcher, ChronoUnit.SECONDS.name().toLowerCase());
			String fractionalSecondsStr = matcher.group("fraction");

			// Convert fractional seconds to nanoseconds
			int nanoseconds = 0;
			if (StringUtils.isNotEmpty(fractionalSecondsStr)) {
				// Ensure the fractional part has exactly 9 digits by padding with zeros if necessary
				fractionalSecondsStr = String.format("%-9s", fractionalSecondsStr).replace(' ', '0'); // NON-NLS
				nanoseconds = Integer.parseInt(fractionalSecondsStr);
			}

			// Convert resolved temporal units to total seconds
			long totalSeconds = TimeUnit.DAYS.toSeconds(years * 365L) //
					+ TimeUnit.DAYS.toSeconds(months * 30L) //
					+ TimeUnit.DAYS.toSeconds(weeks * 7L) //
					+ TimeUnit.DAYS.toSeconds(days) //
					+ TimeUnit.HOURS.toSeconds(hours) //
					+ TimeUnit.MINUTES.toSeconds(minutes) //
					+ seconds;

			// Create and return the Duration object
			return java.time.Duration.ofSeconds(totalSeconds).plusNanos(nanoseconds);
		}

		private static int getFieldValue(Matcher matcher, String field) {
			try {
				String fValue = matcher.group(field);
				if (StringUtils.isNotEmpty(fValue)) {
					return Integer.parseInt(fValue);
				}
			} catch (IllegalArgumentException exc) {
				return 0;
			}

			return 0;
		}

		private static UsecTimestamp durationToTimestamp(java.time.Duration duration) {
			if (duration == null) {
				return null;
			}

			long nSec = duration.toNanos();

			long mSec = TimeUnit.NANOSECONDS.toMillis(nSec);// nSec / 1_000_000L;
			long uSec = TimeUnit.NANOSECONDS.toMicros(nSec % 1_000_000L);// / 1_000L;

			UsecTimestamp dTime = new UsecTimestamp(mSec, uSec);

			return dTime;
		}
	}
}
