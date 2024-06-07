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

	// Define the regex pattern to match HH:mm:ss.SSSSSSSSS, where SSSSSSSSS length is variable
	private static final Pattern HMS_DURATION_PATTERN = Pattern.compile("(\\d+):(\\d{2}):(\\d{2})\\.(\\d+)"); // NON-NLS

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
	 * Represents duration value in human readable form: {@code "hours:minutes:seconds.millis"}
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
	 * Represents duration value in human readable form: {@code "hours:minutes:seconds.millis"}
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

		return parseHMSDuration(durationStr, HMS_DURATION_PATTERN);
	}

	/**
	 * Parses provided duration string {@code durationStr} to {@link java.time.Duration} instance using defined duration
	 * string format RegEx pattern string {@code ptrRegex}.
	 *
	 * @param durationStr
	 *            duration string to parse
	 * @param ptrRegex
	 *            duration string format RegEx pattern string
	 * @return duration instance parsed from provided duration string, or {@code null} if provided duration string is
	 *         {@code null} or empty
	 */
	public static java.time.Duration parseHMSDuration(String durationStr, String ptrRegex) {
		if (StringUtils.isEmpty(durationStr)) {
			return null;
		}

		return parseHMSDuration(durationStr, Pattern.compile(ptrRegex));
	}

	private static java.time.Duration parseHMSDuration(String durationStr, Pattern pattern) {
		Matcher matcher = pattern.matcher(durationStr);

		if (!matcher.matches()) {
			throw new IllegalArgumentException(StreamsResources
					.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME, "Duration.invalid.format", durationStr)); // NON-NLS
		}

		// Extract and parse each component
		int hours = Integer.parseInt(matcher.group(1));
		int minutes = Integer.parseInt(matcher.group(2));
		int seconds = Integer.parseInt(matcher.group(3));
		String fractionalSecondsStr = matcher.group(4);

		// Convert fractional seconds to nanoseconds
		int nanoseconds = 0;
		if (!fractionalSecondsStr.isEmpty()) {
			// Ensure the fractional part has exactly 9 digits by padding with zeros if necessary
			fractionalSecondsStr = String.format("%-9s", fractionalSecondsStr).replace(' ', '0'); // NON-NLS
			nanoseconds = Integer.parseInt(fractionalSecondsStr);
		}

		// Convert hours, minutes, and seconds to total seconds
		long totalSeconds = hours * 3600L + minutes * 60L + seconds;

		// Create and return the Duration object
		return java.time.Duration.ofSeconds(totalSeconds).plusNanos(nanoseconds);
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
		return durationToTimestamp(parseHMSDuration(durationStr));
	}

	/**
	 * Parses provided duration string {@code durationStr} to {@link UsecTimestamp} instance using defined duration
	 * string format RegEx pattern string {@code ptrRegex}.
	 *
	 * @param durationStr
	 *            duration string to parse
	 * @param ptrRegex
	 *            duration string format RegEx pattern string
	 * @return timestamp instance parsed from provided duration string, or {@code null} if provided duration string is
	 *         {@code null} or empty
	 *
	 * @see #parseHMSDuration(String, String)
	 */
	public static UsecTimestamp parseHMSDurationToTimestamp(String durationStr, String ptrRegex) {
		return durationToTimestamp(parseHMSDuration(durationStr, ptrRegex));
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
