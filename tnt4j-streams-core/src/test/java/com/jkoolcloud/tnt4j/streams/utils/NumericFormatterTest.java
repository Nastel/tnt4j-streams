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

import static org.junit.Assert.*;

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
	public void testCastExact() {
		assertEquals(20L, NumericFormatter.castNumber(20L, Long.class, NumericFormatter.CastMode.EXACT));
		assertEquals(20L, NumericFormatter.castNumber(20.3, Long.class, NumericFormatter.CastMode.EXACT));
		assertEquals(20L, NumericFormatter.castNumber(20.9, Long.class, NumericFormatter.CastMode.EXACT));
		assertEquals(20L, NumericFormatter.castNumber(20L, Number.class, NumericFormatter.CastMode.EXACT));
		assertEquals(20L, NumericFormatter.castNumber(20.0, Long.class, NumericFormatter.CastMode.EXACT));
		assertEquals(20L, NumericFormatter.castNumber(20, Long.class, NumericFormatter.CastMode.EXACT));
		assertEquals(20.0,
				NumericFormatter.castNumber(20L, Double.class, NumericFormatter.CastMode.EXACT).doubleValue(), 0.0001);
		assertEquals(20.0,
				NumericFormatter.castNumber(20.0, Double.class, NumericFormatter.CastMode.EXACT).doubleValue(), 0.0001);
		assertEquals(123456.7890,
				NumericFormatter.castNumber(123456.7890, Float.class, NumericFormatter.CastMode.EXACT).doubleValue(),
				0.0001);
		assertEquals(new BigDecimal(20.0),
				NumericFormatter.castNumber(20L, BigDecimal.class, NumericFormatter.CastMode.EXACT));
		assertEquals(new BigDecimal("15445512248522412556325202"), NumericFormatter.castNumber(
				new BigInteger("15445512248522412556325202"), BigDecimal.class, NumericFormatter.CastMode.EXACT));
		assertEquals(new BigInteger("123456789012345678901234567890"), NumericFormatter.castNumber(
				new BigInteger("123456789012345678901234567890"), BigInteger.class, NumericFormatter.CastMode.EXACT));
		assertEquals(new BigDecimal("123456789012345678901234567890"), NumericFormatter.castNumber(
				new BigInteger("123456789012345678901234567890"), BigDecimal.class, NumericFormatter.CastMode.EXACT));
		assertEquals(new BigInteger("123456789012345678901234567890"),
				NumericFormatter.castNumber(new BigDecimal("123456789012345678901234567890.123"), BigInteger.class,
						NumericFormatter.CastMode.EXACT));
		assertEquals(123456L, NumericFormatter.castNumber(new BigDecimal("123456.789012345678901234567890"), Long.class,
				NumericFormatter.CastMode.EXACT));
	}

	@Test
	public void testCastExactFail() {
		assertEquals(null, NumericFormatter.castNumber(1234561245782125452253.123456567889225, Float.class,
				NumericFormatter.CastMode.EXACT, null));
		assertEquals(null, NumericFormatter.castNumber(123456, Byte.class, NumericFormatter.CastMode.EXACT, null));
		assertEquals(null, NumericFormatter.castNumber(new BigInteger("123456789012345678901234567890"), Long.class,
				NumericFormatter.CastMode.EXACT, null));
		assertEquals(null, NumericFormatter.castNumber(new BigInteger("12345678901234567890"), Integer.class,
				NumericFormatter.CastMode.EXACT, null));
		assertEquals(null, NumericFormatter.castNumber(new BigInteger("1234567890"), Short.class,
				NumericFormatter.CastMode.EXACT, null));
		assertEquals(null, NumericFormatter.castNumber(new BigInteger("12345"), Byte.class,
				NumericFormatter.CastMode.EXACT, null));

		assertEquals(null, NumericFormatter.castNumber(new BigDecimal("123456789012345678901234567890"), Long.class,
				NumericFormatter.CastMode.EXACT, null));
		assertEquals(null, NumericFormatter.castNumber(new BigDecimal("12345678901234567890"), Integer.class,
				NumericFormatter.CastMode.EXACT, null));
		assertEquals(null, NumericFormatter.castNumber(new BigDecimal("1234567890"), Short.class,
				NumericFormatter.CastMode.EXACT, null));
		assertEquals(null, NumericFormatter.castNumber(new BigDecimal("12345"), Byte.class,
				NumericFormatter.CastMode.EXACT, null));
		assertEquals(null, // 1.2345678901234568E+29,
				NumericFormatter.castNumber(new BigInteger("123456789012345678901234567890"), Double.class,
						NumericFormatter.CastMode.EXACT, null));
	}

	@Test
	public void testCastUpBound() {
		// byte
		Number cNum = NumericFormatter.castNumber(123, Byte.class, NumericFormatter.CastMode.UP_BOUND);
		assertEquals(Byte.class, cNum.getClass());
		assertEquals(123, cNum.byteValue());
		cNum = NumericFormatter.castNumber(12345, Byte.class, NumericFormatter.CastMode.UP_BOUND);
		assertEquals(Short.class, cNum.getClass());
		assertEquals(12345, cNum.shortValue());
		cNum = NumericFormatter.castNumber(12345.123, Byte.class, NumericFormatter.CastMode.UP_BOUND);
		assertEquals(Short.class, cNum.getClass());
		assertEquals(12345, cNum.shortValue());
		cNum = NumericFormatter.castNumber(123456, Byte.class, NumericFormatter.CastMode.UP_BOUND);
		assertEquals(Integer.class, cNum.getClass());
		assertEquals(123456, cNum.intValue());
		cNum = NumericFormatter.castNumber(123L, Byte.class, NumericFormatter.CastMode.UP_BOUND);
		assertEquals(Byte.class, cNum.getClass());
		assertEquals(123, cNum.byteValue());
		cNum = NumericFormatter.castNumber(1234567890123456789L, Byte.class, NumericFormatter.CastMode.UP_BOUND);
		assertEquals(Long.class, cNum.getClass());
		assertEquals(1234567890123456789L, cNum.longValue());
		cNum = NumericFormatter.castNumber(123f, Byte.class, NumericFormatter.CastMode.UP_BOUND);
		assertEquals(Byte.class, cNum.getClass());
		assertEquals(123, cNum.byteValue());
		cNum = NumericFormatter.castNumber(12345678901234567890.0f, Byte.class, NumericFormatter.CastMode.UP_BOUND);
		assertEquals(Float.class, cNum.getClass());
		assertEquals(12345678901234567890.0f, cNum.floatValue(), 0.00001);
		cNum = NumericFormatter.castNumber(123.0, Byte.class, NumericFormatter.CastMode.UP_BOUND);
		assertEquals(Byte.class, cNum.getClass());
		assertEquals(123, cNum.byteValue());
		cNum = NumericFormatter.castNumber(12345678901234567890.0, Byte.class, NumericFormatter.CastMode.UP_BOUND);
		assertEquals(Double.class, cNum.getClass());
		assertEquals(12345678901234567890.0, cNum.doubleValue(), 0.00001);
		cNum = NumericFormatter.castNumber(BigInteger.valueOf(123), Byte.class, NumericFormatter.CastMode.UP_BOUND);
		assertEquals(Byte.class, cNum.getClass());
		assertEquals(123, cNum.byteValue());
		cNum = NumericFormatter.castNumber(new BigInteger("1234567890123456789"), Byte.class,
				NumericFormatter.CastMode.UP_BOUND);
		assertEquals(Long.class, cNum.getClass());
		assertEquals(1234567890123456789L, cNum.longValue());
		cNum = NumericFormatter.castNumber(new BigInteger("12345678901234567890123456789012345678901234567890"),
				Byte.class, NumericFormatter.CastMode.UP_BOUND);
		assertEquals(BigInteger.class, cNum.getClass());
		assertEquals(new BigInteger("12345678901234567890123456789012345678901234567890"), cNum);
		cNum = NumericFormatter.castNumber(new BigDecimal(123.124), Byte.class, NumericFormatter.CastMode.UP_BOUND);
		assertEquals(Byte.class, cNum.getClass());
		assertEquals(123, cNum.byteValue());
		cNum = NumericFormatter.castNumber(new BigDecimal("12345678901234567890123456789012345678901234567890.123"),
				Byte.class, NumericFormatter.CastMode.UP_BOUND);
		assertEquals(BigInteger.class, cNum.getClass());
		assertEquals(new BigInteger("12345678901234567890123456789012345678901234567890"), cNum);
		cNum = NumericFormatter.castNumber(new BigDecimal(
				"1234567890123456789012345678901234567890123452343423523652352352352352352352352352352367890.123"),
				Byte.class, NumericFormatter.CastMode.UP_BOUND);
		assertEquals(BigInteger.class, cNum.getClass());
		assertEquals(
				new BigInteger(
						"1234567890123456789012345678901234567890123452343423523652352352352352352352352352352367890"),
				cNum);

		// float
		cNum = NumericFormatter.castNumber(123, Float.class, NumericFormatter.CastMode.UP_BOUND);
		assertEquals(Float.class, cNum.getClass());
		assertEquals(123, cNum.floatValue(), 0.0001);
		cNum = NumericFormatter.castNumber(123.125f, Float.class, NumericFormatter.CastMode.UP_BOUND);
		assertEquals(Float.class, cNum.getClass());
		assertEquals(123.125f, cNum.floatValue(), 0.0001);
		cNum = NumericFormatter.castNumber(123.125d, Float.class, NumericFormatter.CastMode.UP_BOUND);
		assertEquals(Float.class, cNum.getClass());
		assertEquals(123.125f, cNum.floatValue(), 0.0001);
		cNum = NumericFormatter.castNumber(1231452445454454578777878787788754545653.1454556525d, Float.class,
				NumericFormatter.CastMode.UP_BOUND);
		assertEquals(Double.class, cNum.getClass());
		assertEquals(1231452445454454578777878787788754545653.1454556525d, cNum.doubleValue(), 0.0001);
		cNum = NumericFormatter.castNumber(new BigInteger("1231452445454454578777878787788754545"), Float.class,
				NumericFormatter.CastMode.UP_BOUND);
		assertEquals(BigDecimal.class, cNum.getClass());
		assertEquals(new BigDecimal("1231452445454454578777878787788754545"), cNum);
		cNum = NumericFormatter.castNumber(new BigDecimal("1231452445454454578777878787788754545"), Float.class,
				NumericFormatter.CastMode.UP_BOUND);
		assertEquals(BigDecimal.class, cNum.getClass());
		assertEquals(new BigDecimal("1231452445454454578777878787788754545"), cNum);
		cNum = NumericFormatter.castNumber(new BigDecimal("1231452445454454578777878787788754545.45525"), Float.class,
				NumericFormatter.CastMode.UP_BOUND);
		assertEquals(BigDecimal.class, cNum.getClass());
		assertEquals(new BigDecimal("1231452445454454578777878787788754545.45525"), cNum);

		// edge numbers
		cNum = NumericFormatter.castNumber(Float.POSITIVE_INFINITY, Float.class, NumericFormatter.CastMode.UP_BOUND);
		assertEquals(Float.class, cNum.getClass());
		assertEquals(Float.POSITIVE_INFINITY, cNum.floatValue(), 0.0001);
		cNum = NumericFormatter.castNumber(Float.NEGATIVE_INFINITY, Float.class, NumericFormatter.CastMode.UP_BOUND);
		assertEquals(Float.class, cNum.getClass());
		assertEquals(Float.NEGATIVE_INFINITY, cNum.floatValue(), 0.0001);
		cNum = NumericFormatter.castNumber(Float.NaN, Float.class, NumericFormatter.CastMode.UP_BOUND);
		assertEquals(Float.class, cNum.getClass());
		assertEquals(Float.NaN, cNum.floatValue(), 0.0001);
		cNum = NumericFormatter.castNumber(Double.POSITIVE_INFINITY, Float.class, NumericFormatter.CastMode.UP_BOUND);
		assertEquals(Float.class, cNum.getClass());
		assertEquals(Double.POSITIVE_INFINITY, cNum.floatValue(), 0.0001);
		cNum = NumericFormatter.castNumber(Double.NEGATIVE_INFINITY, Float.class, NumericFormatter.CastMode.UP_BOUND);
		assertEquals(Float.class, cNum.getClass());
		assertEquals(Double.NEGATIVE_INFINITY, cNum.floatValue(), 0.0001);
		cNum = NumericFormatter.castNumber(Double.NaN, Float.class, NumericFormatter.CastMode.UP_BOUND);
		assertEquals(Float.class, cNum.getClass());
		assertEquals(Double.NaN, cNum.floatValue(), 0.0001);
		cNum = NumericFormatter.castNumber(Float.POSITIVE_INFINITY, Double.class, NumericFormatter.CastMode.UP_BOUND);
		assertEquals(Double.class, cNum.getClass());
		assertEquals(Float.POSITIVE_INFINITY, cNum.floatValue(), 0.0001);
		cNum = NumericFormatter.castNumber(Float.NEGATIVE_INFINITY, Double.class, NumericFormatter.CastMode.UP_BOUND);
		assertEquals(Double.class, cNum.getClass());
		assertEquals(Float.NEGATIVE_INFINITY, cNum.floatValue(), 0.0001);
		cNum = NumericFormatter.castNumber(Float.NaN, Double.class, NumericFormatter.CastMode.UP_BOUND);
		assertEquals(Double.class, cNum.getClass());
		assertEquals(Float.NaN, cNum.floatValue(), 0.0001);
	}

	@Test
	public void testCastAPI() {
		assertEquals((byte) 20, NumericFormatter.castNumber(20, Byte.class, NumericFormatter.CastMode.API));
		assertEquals((byte) -56, NumericFormatter.castNumber(200, Byte.class, NumericFormatter.CastMode.API));
		assertEquals((byte) -48, NumericFormatter.castNumber(2000, Byte.class, NumericFormatter.CastMode.API));
		assertEquals((short) 200, NumericFormatter.castNumber(200L, Short.class, NumericFormatter.CastMode.API));
		assertEquals((short) 3392, NumericFormatter.castNumber(200000L, Short.class, NumericFormatter.CastMode.API));
		assertEquals(2000000000,
				NumericFormatter.castNumber(2000000000L, Integer.class, NumericFormatter.CastMode.API));
		assertEquals(-1454759936,
				NumericFormatter.castNumber(2000000000000L, Integer.class, NumericFormatter.CastMode.API));
		assertEquals(2000000000000L,
				NumericFormatter.castNumber(2000000000000L, Long.class, NumericFormatter.CastMode.API));
		assertEquals(9223372036854775807L,
				NumericFormatter.castNumber(20000000000000000000f, Long.class, NumericFormatter.CastMode.API));
		assertEquals(20000000000000000000f,
				NumericFormatter.castNumber(20000000000000000000f, Float.class, NumericFormatter.CastMode.API));
		assertEquals(Float.POSITIVE_INFINITY, NumericFormatter.castNumber(
				2000000000000000000000000000000000000000.00001d, Float.class, NumericFormatter.CastMode.API));
		assertEquals(2000000000000000000000000000000000000000.00001d, NumericFormatter.castNumber(
				2000000000000000000000000000000000000000.00001d, Double.class, NumericFormatter.CastMode.API));
		assertEquals(1.7976931348623157E308,
				NumericFormatter.castNumber(Double.MAX_VALUE + 1, Double.class, NumericFormatter.CastMode.API));
		assertEquals(1.7976931348623157E308,
				NumericFormatter.castNumber(Double.MAX_VALUE + 123, Double.class, NumericFormatter.CastMode.API));
		assertEquals(new BigInteger("1545489754158478941211549841515484111551884411218411159181"),
				NumericFormatter.castNumber(
						new BigInteger("1545489754158478941211549841515484111551884411218411159181"), BigInteger.class,
						NumericFormatter.CastMode.API));
		assertEquals(new BigDecimal("1545489754158478941211549841515484111551884411218411159181"),
				NumericFormatter.castNumber(
						new BigInteger("1545489754158478941211549841515484111551884411218411159181"), BigDecimal.class,
						NumericFormatter.CastMode.API));
		assertEquals(new BigInteger("1545489754158478941211549841515484111551884411218411159181"),
				NumericFormatter.castNumber(
						new BigDecimal("1545489754158478941211549841515484111551884411218411159181.1245"),
						BigInteger.class, NumericFormatter.CastMode.API));
	}

	@Test
	public void testCastAPIFault() {
		assertNotEquals(new BigDecimal("1545489754158478941211549841515484111551884411218411159181"),
				NumericFormatter.castNumber(
						new BigInteger("1545489754158478941211549841515484111551884411218411159181"), BigInteger.class,
						NumericFormatter.CastMode.API));
		assertNotEquals(new BigInteger("1545489754158478941211549841515484111551884411218411159181"),
				NumericFormatter.castNumber(
						new BigInteger("1545489754158478941211549841515484111551884411218411159181"), BigDecimal.class,
						NumericFormatter.CastMode.API));
		assertNotEquals(new BigInteger("1545489754158478941211549841515484111551884411218411159181"),
				NumericFormatter.castNumber(
						new BigInteger("1545489754158478941211549841515484111551884411218411159181"), BigDecimal.class,
						NumericFormatter.CastMode.API));
		assertNotEquals(new BigDecimal("1545489754158478941211549841515484111551884411218411159181.145"),
				NumericFormatter.castNumber(
						new BigDecimal("1545489754158478941211549841515484111551884411218411159181.145"),
						BigInteger.class, NumericFormatter.CastMode.API));
	}
}
