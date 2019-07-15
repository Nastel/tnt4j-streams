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

package com.jkoolcloud.tnt4j.streams.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.Locale;

import org.junit.Test;

/**
 * @author akausinis
 * @version 1.0
 */
public class NumericFormatterTest {

	@Test
	public void testParseObject() throws ParseException {
		// Test Integers
		assertEquals(2, NumericFormatter.parse(1, null, 2));
		assertEquals(1, NumericFormatter.parse(1, (String) null));
		assertNull(NumericFormatter.parse(null, (String) null));

	}

	@Test
	public void testParseStaticHex() throws ParseException {
		assertEquals(342, NumericFormatter.parse("0xAB", null, 2)); // NON-NLS
		assertEquals(171, NumericFormatter.parse(0XAB, (String) null));
		assertNull(NumericFormatter.parse(null, (String) null));
	}

	@Test
	public void testParseObjectHex() throws ParseException {
		NumericFormatter formatter = new NumericFormatter();
		assertEquals(342, formatter.parse("0xAB", 2)); // NON-NLS
		assertEquals(171, formatter.parse(0XAB));
		assertEquals(new BigInteger("1000000000000000000000"),
				formatter.parse("0x00000000000000000000000000000000000000000000003635c9adc5dea00000"));
		assertEquals(new BigInteger("188100000000000000000000"),
				formatter.parse("0x0000000000000000000000000000000000000000000027d4ebe2fb7ce0900000"));
		assertNull(formatter.parse(null));
	}

	@Test
	public void testParseStringObjectNumber() throws ParseException {
		NumericFormatter.parse(10, null, 1);
	}

	@Test
	public void testRadix() {
		NumericFormatter formatter = new NumericFormatter(5);
		assertEquals(5, formatter.getRadix());
		formatter.setRadix(6);
		assertEquals(6, formatter.getRadix());
	}

	@Test
	public void testPattern() throws Exception {
		NumericFormatter formatter = new NumericFormatter();
		formatter.setPattern("#", null); // NON-NLS
		assertEquals("#", formatter.getPattern());
		formatter.parse("2");
	}

	@Test
	public void testGeneric() throws Exception {
		NumericFormatter formatter = new NumericFormatter();
		formatter.setPattern("any", Locale.US.toString());
		assertEquals(123456.789, formatter.parse("123,456.789")); // NON-NLS

		formatter = new NumericFormatter();
		formatter.setPattern("any", "lt-LT"); // NON-NLS
		assertEquals(-5896456.7898658, formatter.parse("-5896456,7898658")); // NON-NLS

		formatter = new NumericFormatter();
		formatter.setPattern("any", Locale.US.toString()); // NON-NLS
		assertEquals(30L, formatter.parse("30hj00")); // NON-NLS

		formatter = new NumericFormatter();
		assertEquals(25, formatter.parse("25")); // NON-NLS

		formatter = new NumericFormatter();
		assertEquals(171, formatter.parse("0xAB")); // NON-NLS

		formatter = new NumericFormatter();
		assertEquals(123.456789, formatter.parse("123.456789")); // NON-NLS

		formatter = new NumericFormatter();
		assertEquals(1.0f, formatter.parse("1.0")); // NON-NLS

		formatter = new NumericFormatter();
		formatter.setPattern("long", null);
		assertEquals(90000L, formatter.parse("0x15f90"));
	}

	@Test(expected = ParseException.class)
	public void testGenericFail() throws Exception {
		NumericFormatter formatter = new NumericFormatter();
		formatter.setPattern(null, "lt-LT"); // NON-NLS
		assertEquals(30, formatter.parse("30hj00")); // NON-NLS
	}

	@Test
	public void testCast() {
		assertEquals((Long) 20L, NumericFormatter.castNumber(20L, Long.class));
		assertEquals((Long) 20L, NumericFormatter.castNumber(20.3, Long.class));
		assertEquals((Long) 20L, NumericFormatter.castNumber(20.9, Long.class));
		assertEquals(20L, NumericFormatter.castNumber(20L, Number.class));
		assertEquals((Long) 20L, NumericFormatter.castNumber(20.0, Long.class));
		assertEquals((Long) 20L, NumericFormatter.castNumber(20, Long.class));
		assertEquals(20.0, NumericFormatter.castNumber(20L, Double.class), 0.0001);
		assertEquals(20.0, NumericFormatter.castNumber(20.0, Double.class), 0.0001);
		assertEquals(BigDecimal.valueOf(20.0), NumericFormatter.castNumber(20L, BigDecimal.class));
		assertEquals(0, new BigDecimal("15445512248522412556325202").compareTo(
				NumericFormatter.castNumber(new BigInteger("15445512248522412556325202"), BigDecimal.class)));
		assertEquals(1.2345678901234568E29,
				NumericFormatter.castNumber(new BigInteger("123456789012345678901234567890"), Double.class), 0.0001);
		assertEquals(new BigInteger("123456789012345678901234567890"),
				NumericFormatter.castNumber(new BigInteger("123456789012345678901234567890"), BigInteger.class));
		assertEquals(new BigDecimal("123456789012345678901234567890"),
				NumericFormatter.castNumber(new BigInteger("123456789012345678901234567890"), BigDecimal.class));
		assertEquals(new BigInteger("123456789012345678901234567890"),
				NumericFormatter.castNumber(new BigDecimal("123456789012345678901234567890.123"), BigInteger.class));
	}

	@Test
	public void testCastFail() {
		assertEquals(null, NumericFormatter.castNumber(new BigInteger("123456789012345678901234567890"), Long.class));
		assertEquals(null, NumericFormatter.castNumber(new BigInteger("12345678901234567890"), Integer.class));
		assertEquals(null, NumericFormatter.castNumber(new BigInteger("1234567890"), Short.class));
		assertEquals(null, NumericFormatter.castNumber(new BigInteger("12345"), Byte.class));

		assertEquals(null, NumericFormatter.castNumber(new BigDecimal("123456789012345678901234567890"), Long.class));
		assertEquals(null, NumericFormatter.castNumber(new BigDecimal("12345678901234567890"), Integer.class));
		assertEquals(null, NumericFormatter.castNumber(new BigDecimal("1234567890"), Short.class));
		assertEquals(null, NumericFormatter.castNumber(new BigDecimal("12345"), Byte.class));
	}
}
