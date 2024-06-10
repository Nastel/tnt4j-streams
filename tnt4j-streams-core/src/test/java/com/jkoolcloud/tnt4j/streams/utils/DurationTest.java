/*
 * Copyright 2014-2024 JKOOL, LLC.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.jkoolcloud.tnt4j.core.UsecTimestamp;

/**
 * @author akausinis
 * @version 1.0
 */
public class DurationTest {

	@Test
	public void testDurationRound() {
		Date now = new Date();
		Calendar c = Calendar.getInstance();
		c.add(Calendar.HOUR, 3);
		c.add(Calendar.MINUTE, 59);
		Date then = c.getTime();

		String offset = Duration.getTimeZoneRFC822(then.getTime(), now.getTime(), 15);
		assertEquals("Unexpected offset value for 'then-now' shift", "-0400", offset);

		offset = Duration.getTimeZoneRFC822(now.getTime(), then.getTime(), 15);
		assertEquals("Unexpected offset value for 'now-then' shift", "+0400", offset);

		offset = Duration.getTimeZoneRFC822(now.getTime(), now.getTime(), 15);
		assertEquals("Unexpected offset value for 'now-now' shift", "+0000", offset);

		offset = Duration.getTimeZoneRFC822(then.getTime(), then.getTime(), 15);
		assertEquals("Unexpected offset value for 'then-then' shift", "+0000", offset);

		offset = Duration.getTimeZoneRFC822Usec(then.getTime() * 1000, now.getTime() * 1000, 15);
		assertEquals("Unexpected offset value for 'then-now' * 1000 shift", "-0400", offset);
	}

	@Test
	public void testDurationNanosFormatting() {
		String fString = String.format("%-9s", "123456").replace(' ', '0'); // NON-NLS
		assertEquals("Unexpected formatting result", "123456000", fString);
	}

	@Test
	public void testDurationParsingHMS() {
		String periodString = "35:02:01.015576123"; // NON-NLS
		java.time.Duration duration = Duration.parseHMSDuration(periodString);
		long tMs = duration == null ? 0 : duration.toMillis();
		long tNs = duration == null ? 0 : duration.toNanos();

		assertTrue("Unexpected milliseconds value", tMs % 1_000 == 15);
		assertTrue("Unexpected nanoseconds value", tNs % 1_000 == 123);

		UsecTimestamp dTime = Duration.parseHMSDurationToTimestamp(periodString);
		long tUs = dTime.getTimeUsec();

		assertTrue("Unexpected nanoseconds value", tUs % 1_000 == 576);
		assertTrue("Unexpected microseconds value", tUs % 1_000_000 == 15576);
	}

	@Test
	public void testDurationParsingCustomPattern() {
		String ptr = "yyy MMm www ddd HHh mmm sss.SSSSSSSSS"; // NON-NLS
		String periodString = "6y 5m 1w 3d 15h 02m 01s.015576123"; // NON-NLS

		java.time.Duration duration = Duration.parseHMSDuration(periodString, ptr);
		long tMs = duration == null ? 0 : duration.toMillis();
		long tNs = duration == null ? 0 : duration.toNanos();

		long days = 6 * 365L + 5 * 30L + 1 * 7L + 3;
		long seconds = 15 * 3600L + 2 * 60L + 1;

		assertEquals("Unexpected milliseconds value",
				TimeUnit.DAYS.toMillis(days) + TimeUnit.SECONDS.toMillis(seconds) + 15, tMs);

		assertEquals("Unexpected microseconds value",
				TimeUnit.DAYS.toMicros(days) + TimeUnit.SECONDS.toMicros(seconds) + 15576,
				TimeUnit.NANOSECONDS.toMicros(tNs));

		assertEquals("Unexpected nanoseconds value",
				TimeUnit.DAYS.toNanos(days) + TimeUnit.SECONDS.toNanos(seconds) + 15576123, tNs);

		ptr = "(?<years>\\d+)y (?<months>\\d+)m (?<weeks>\\d+)w (?<days>\\d+)d (?<hours>\\d+)h (?<minutes>\\d{2})m (?<seconds>\\d{2})s.(?<fraction>\\d+)"; // NON-NLS
		periodString = "5y 4m 3w 2d 17h 06m 08s.054628634"; // NON-NLS

		duration = Duration.parseHMSDuration(periodString, ptr);
		tMs = duration == null ? 0 : duration.toMillis();
		tNs = duration == null ? 0 : duration.toNanos();

		days = 5 * 365L + 4 * 30L + 3 * 7L + 2;
		seconds = 17 * 3600L + 6 * 60L + 8;

		assertEquals("Unexpected milliseconds value",
				TimeUnit.DAYS.toMillis(days) + TimeUnit.SECONDS.toMillis(seconds) + 54, tMs);

		assertEquals("Unexpected microseconds value",
				TimeUnit.DAYS.toMicros(days) + TimeUnit.SECONDS.toMicros(seconds) + 54628,
				TimeUnit.NANOSECONDS.toMicros(tNs));

		assertEquals("Unexpected nanoseconds value",
				TimeUnit.DAYS.toNanos(days) + TimeUnit.SECONDS.toNanos(seconds) + 54628634, tNs);

	}
}
