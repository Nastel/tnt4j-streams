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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * Provides methods for parsing objects into numeric values and for formatting numeric values as strings.
 *
 * @version $Revision: 1 $
 *
 * @see DecimalFormat
 */
public class NumericFormatter {

	private final int radix;
	private final FormatterContext formatter;

	private static final Map<String, NumericFormatter> FORMATTERS_MAP = new HashMap<>();
	private static final Lock accessLock = new ReentrantLock();

	/**
	 * Creates a number formatter using the default numeric representation.
	 */
	protected NumericFormatter() {
		this(null, null);
	}

	/**
	 * Creates a number formatter using the default numeric representation in the specified radix.
	 *
	 * @param radix
	 *            the radix to use while parsing numeric strings
	 */
	protected NumericFormatter(int radix) {
		this.radix = radix;

		this.formatter = null;
	}

	/**
	 * Creates a number formatter/parser for numbers using the specified format pattern.
	 *
	 * @param pattern
	 *            format pattern - can be set to {@code null} to use default representation
	 * @param locale
	 *            locale for decimal format to use, or {@code null} if default locale shall be used
	 *
	 * @see #createFormatterContext(String, String)
	 */
	protected NumericFormatter(String pattern, String locale) {
		this.radix = 10;

		this.formatter = createFormatterContext(pattern, locale);
	}

	/**
	 * Gets cached or creates the number formatter/parser for numbers using the default numeric representation.
	 * 
	 * @return number formatter/parser instance
	 */
	public static NumericFormatter getInstance() {
		accessLock.lock();
		try {
			return FORMATTERS_MAP.computeIfAbsent(getFormatterKey(null, null, -1), k -> new NumericFormatter());
		} finally {
			accessLock.unlock();
		}
	}

	/**
	 * Gets cached or creates the number formatter/parser for numbers using the default numeric representation in the
	 * specified radix.
	 * 
	 * @param radix
	 *            the radix to use while parsing numeric strings
	 * @return number formatter/parser instance
	 */
	public static NumericFormatter getInstance(int radix) {
		accessLock.lock();
		try {
			return FORMATTERS_MAP.computeIfAbsent(getFormatterKey(null, null, radix), k -> new NumericFormatter(radix));
		} finally {
			accessLock.unlock();
		}
	}

	/**
	 * Gets cached or creates the number formatter/parser for numbers using the specified format pattern.
	 * 
	 * @param pattern
	 *            format pattern - can be set to {@code null} to use default representation
	 * @param locale
	 *            locale for decimal format to use, or {@code null} if default locale shall be used
	 * @return number formatter/parser instance
	 */
	public static NumericFormatter getInstance(String pattern, String locale) {
		accessLock.lock();
		try {
			return FORMATTERS_MAP.computeIfAbsent(getFormatterKey(pattern, locale, -1),
					k -> new NumericFormatter(pattern, locale));
		} finally {
			accessLock.unlock();
		}
	}

	private static String getFormatterKey(String pattern, String locale, int radix) {
		return pattern + "|&:&|" + locale + "|&:&|" + radix; // NON-NLS
	}

	/**
	 * Gets the radix used by this formatter.
	 *
	 * @return radix used for parsing numeric strings
	 */
	public int getRadix() {
		return radix;
	}

	/**
	 * Gets the format pattern string for this formatter.
	 *
	 * @return format pattern, or {@code null} if none specified
	 */
	public String getPattern() {
		return formatter == null ? null : formatter.pattern;
	}

	/**
	 * Gets the locale definition string for this formatter.
	 *
	 * @return formatter used locale, or {@code null} if none specified
	 */
	public String getLocale() {
		return formatter == null ? null : formatter.locale;
	}

	/**
	 * Sets the format pattern string for this formatter.
	 * <p>
	 * Pattern also can be one of number types enumerators: {@code "integer"}, {@code "int"}, {@code "long"},
	 * {@code "double"}, {@code "float"}, {@code "short"}, {@code "byte"}, {@code "bigint"}, {@code "biginteger"},
	 * {@code "bint"}, {@code "bigdec"}, {@code "bigdecimal"}, {@code "bdec"} and {@value FormatterContext#ANY}.
	 * {@value FormatterContext#ANY} will resolve any possible numeric value out of provided string, e.g.
	 * {@code "30hj00"} will result {@code 30}.
	 * <p>
	 * {@code "~"} prefixed number type enumerator (except {@value FormatterContext#ANY}) means numeric casting shall be
	 * performed using plain Java API, in some cases resulting significant value loss.
	 * <p>
	 * {@code "^"} prefixed number type enumerator (except {@value FormatterContext#ANY}) means if value can't be cast
	 * to defined type, the closest upper bound type shall be used to maintain value without significant loss.
	 * <p>
	 * By default, exact casting (without significant value loss) is be performed. In case number can't be cast to
	 * target type, original value is kept.
	 *
	 * @param pattern
	 *            format pattern - can be set to {@code null} to use default representation
	 * @param locale
	 *            locale for decimal format to use, or {@code null} if default locale shall be used
	 */
	protected FormatterContext createFormatterContext(String pattern, String locale) {
		return new FormatterContext(pattern, locale);
	}

	/**
	 * Formats the specified object using the defined pattern, or using the default numeric formatting if no pattern was
	 * defined.
	 *
	 * @param value
	 *            value to convert
	 * @return formatted value of field in required internal data type
	 * @throws ParseException
	 *             if an error parsing the specified value based on the field definition (e.g. does not match defined
	 *             pattern, etc.)
	 *
	 * @see #parse(Object, Number)
	 */
	public Number parse(Object value) throws ParseException {
		return parse(value, (Number) null);
	}

	/**
	 * Formats the specified object using the defined pattern, or using the default numeric formatting if no pattern was
	 * defined.
	 *
	 * @param value
	 *            value to convert
	 * @param scale
	 *            value to multiply the formatted value by
	 * @return formatted value of field in required internal data type
	 * @throws ParseException
	 *             if an error parsing the specified value based on the field definition (e.g. does not match defined
	 *             pattern, etc.)
	 *
	 * @see #parse(Object, com.jkoolcloud.tnt4j.streams.utils.NumericFormatter.FormatterContext, int, Number)
	 */
	public Number parse(Object value, Number scale) throws ParseException {
		return parse(value, formatter, radix, scale);
	}

	/**
	 * Formats the specified object using the defined pattern, or using the default numeric formatting if no pattern was
	 * defined.
	 *
	 * @param value
	 *            value to convert
	 * @param pattern
	 *            number format pattern
	 * @return formatted value of field in required internal data type
	 * @throws ParseException
	 *             if an error parsing the specified value based on the field definition (e.g. does not match defined
	 *             pattern, etc.)
	 *
	 * @see #parse(Object, String, Number)
	 */
	public static Number parse(Object value, String pattern) throws ParseException {
		return parse(value, pattern, null);
	}

	/**
	 * Formats the specified object using the defined pattern, or using the default numeric formatting if no pattern was
	 * defined.
	 *
	 * @param value
	 *            value to convert
	 * @param pattern
	 *            number format pattern
	 * @param scale
	 *            value to multiply the formatted value by
	 * @return formatted value of field in required internal data type
	 * @throws ParseException
	 *             if an error parsing the specified value based on the field definition (e.g. does not match defined
	 *             pattern, etc.)
	 *
	 * @see #parse(Object, String, Number, String)
	 */
	public static Number parse(Object value, String pattern, Number scale) throws ParseException {
		return parse(value, pattern, scale, null);
	}

	/**
	 * Formats the specified object using the defined pattern, or using the default numeric formatting if no pattern was
	 * defined.
	 * <p>
	 * Pattern also can be one of number types enumerators: {@code "integer"}, {@code "int"}, {@code "long"},
	 * {@code "double"}, {@code "float"}, {@code "short"}, {@code "byte"}, {@code "bigint"}, {@code "biginteger"},
	 * {@code "bint"}, {@code "bigdec"}, {@code "bigdecimal"}, {@code "bdec"} and {@value FormatterContext#ANY}.
	 * {@value FormatterContext#ANY} will resolve any possible numeric value out of provided string, e.g.
	 * {@code "30hj00"} will result {@code 30}.
	 * <p>
	 * {@code "~"} prefixed number type enumerator (except {@value FormatterContext#ANY}) means numeric casting shall be
	 * performed using plain Java API, in some cases resulting significant value loss.
	 * <p>
	 * {@code "^"} prefixed number type enumerator (except {@value FormatterContext#ANY}) means if value can't be cast
	 * to defined type, the closest upper bound type shall be used to maintain value without significant loss.
	 * <p>
	 * By default, exact casting (without significant value loss) is be performed. In case number can't be cast to
	 * target type, original value is kept.
	 *
	 * @param value
	 *            value to convert
	 * @param pattern
	 *            number format pattern
	 * @param scale
	 *            value to multiply the formatted value by
	 * @param locale
	 *            locale for decimal format to use, or {@code null} if default locale shall be used
	 * @return formatted value of field in required internal data type
	 * @throws ParseException
	 *             if an error parsing the specified value based on the field definition (e.g. does not match defined
	 *             pattern, etc.)
	 *
	 * @see #parse(Object, com.jkoolcloud.tnt4j.streams.utils.NumericFormatter.FormatterContext, int, Number)
	 */
	public static Number parse(Object value, String pattern, Number scale, String locale) throws ParseException {
		return parse(value, new FormatterContext(pattern, locale), 10, scale);
	}

	/**
	 * Formats the specified object using the defined pattern, or using the default numeric formatting if no pattern was
	 * defined.
	 *
	 * @param value
	 *            value to convert
	 * @param formatter
	 *            formatter object to apply to value
	 * @param radix
	 *            the radix to use while parsing numeric strings
	 * @param scale
	 *            value to multiply the formatted value by
	 *
	 * @return formatted value of field in required internal data type
	 *
	 * @throws ParseException
	 *             if an error parsing the specified value based on the field definition (e.g. does not match defined
	 *             pattern, etc.)
	 */
	private static Number parse(Object value, FormatterContext formatter, int radix, Number scale)
			throws ParseException {
		if (value == null) {
			return null;
		}
		Number numValue = null;
		if (value instanceof Number) {
			numValue = (Number) value;
		} else {
			String strValue = Utils.toString(value).trim();
			if (isEmptyNumberStr(strValue)) {
				return null;
			}

			Exception nfe;
			if (formatter != null && formatter.isFormatDefined()) {
				try {
					numValue = formatter.format.parse(strValue);
					nfe = null;
				} catch (ParseException exc) {
					nfe = exc;
				}
			} else {
				try {
					numValue = strToNumber(strValue, radix, formatter.pattern);
					nfe = null;
				} catch (NumberFormatException exc) {
					nfe = exc;
				}
			}

			if (numValue == null && formatter != null && FormatterContext.ANY.equalsIgnoreCase(formatter.pattern)) {
				try {
					numValue = formatter.getGPFormat().parse(strValue);
					nfe = null;
				} catch (ParseException exc) {
					nfe = exc;
				}
			}

			if (nfe != null) {
				ParseException pe = new ParseException(nfe.getLocalizedMessage(), 0);
				pe.initCause(nfe);

				throw pe;
			}
		}

		if (formatter != null && !formatter.isFormatDefined()) {
			Number cNumValue = castNumber(numValue, formatter.pattern);
			if (cNumValue != null) {
				numValue = cNumValue;
			}
		}

		return scaleNumber(numValue, scale);
	}

	/**
	 * Casts provided number value to desired number type.
	 * <p>
	 * Number type name can be one of:
	 * <ul>
	 * <li>to cast to {@link java.lang.Integer} - {@code "integer"}, {@code "int"}</li>
	 * <li>to cast to {@link java.lang.Long} - {@code "long"}</li>
	 * <li>to cast to {@link java.lang.Double} - {@code "double"}</li>
	 * <li>to cast to {@link java.lang.Float} - {@code "float"}</li>
	 * <li>to cast to {@link java.lang.Short} - {@code "short"}</li>
	 * <li>to cast to {@link java.lang.Byte} - {@code "byte"}</li>
	 * <li>to cast to {@link java.math.BigInteger} - {@code "bigint"}, {@code "biginteger"}, {@code "bint"}</li>
	 * <li>to cast to {@link java.math.BigDecimal} - {@code "bigdec"}, {@code "bigdecimal"}, {@code "bdec"}</li>
	 * </ul>
	 * <p>
	 * {@code "~"} prefixed number type enumerator (except {@value FormatterContext#ANY}) means numeric casting shall be
	 * performed using plain Java API, in some cases resulting significant value loss.
	 * <p>
	 * {@code "^"} prefixed number type enumerator (except {@value FormatterContext#ANY}) means if value can't be cast
	 * to defined type, the closest upper bound type shall be used to maintain value without significant loss.
	 * <p>
	 * By default, exact casting (without significant value loss) is be performed. In case number can't be cast to
	 * target type, original value is kept.
	 * 
	 * @param num
	 *            number value to cast
	 * @param typeExpr
	 *            number type expression containing type name and cast mode prefix
	 * @return number value cast to desired numeric type
	 */
	public static Number castNumber(Number num, String typeExpr) {
		if (StringUtils.isNotEmpty(typeExpr)) {
			CastMode cMode;
			String type;

			if (typeExpr.startsWith(CastMode.API.symbol)) {
				type = typeExpr.substring(1);
				cMode = CastMode.API;
			} else if (typeExpr.startsWith(CastMode.UP_BOUND.symbol)) {
				type = typeExpr.substring(1);
				cMode = CastMode.UP_BOUND;
			} else {
				type = typeExpr;
				cMode = CastMode.EXACT;
			}

			Class<? extends Number> nType = FormatterContext.getNumberClass(type);

			return castNumber(num, nType, cMode);
		}

		return num;
	}

	/**
	 * Casts provided number value to desired number type and casting mode.
	 *
	 * @param num
	 *            number value to cast
	 * @param clazz
	 *            number class to cast number to
	 * @param castMode
	 *            cast mode to be used
	 * @return number value cast to desired numeric type, or original number {@code num} if number can't be cast
	 * 
	 * @see #castNumber(Number, Class, com.jkoolcloud.tnt4j.streams.utils.NumericFormatter.CastMode, Number)
	 */
	public static Number castNumber(Number num, Class<? extends Number> clazz, CastMode castMode) {
		return castNumber(num, clazz, castMode, num);
	}

	/**
	 * Casts provided number value to desired number type and casting mode.
	 *
	 * @param num
	 *            number value to cast
	 * @param clazz
	 *            number class to cast number to
	 * @param castMode
	 *            cast mode to be used
	 * @param failValue
	 *            value to return if number can't be cast
	 * @return number value cast to desired numeric type, or {@code failValue} if number can't be cast
	 */
	public static Number castNumber(Number num, Class<? extends Number> clazz, CastMode castMode, Number failValue) {
		Number cNum;
		switch (castMode) {
		case API:
			cNum = castNumberAPI(num, clazz);
			break;
		case UP_BOUND:
			cNum = castNumberUpBound(num, clazz);
			break;
		case EXACT:
		default:
			cNum = castNumberExact(num, clazz);
			break;
		}

		return cNum == null ? failValue : cNum;
	}

	private static Number castNumberAPI(Number num, Class<? extends Number> clazz) {
		if (num == null || clazz == null) {
			return null;
		}

		Number cNum = null;

		if (!clazz.isAssignableFrom(num.getClass())) {
			if (clazz.isAssignableFrom(Long.class)) {
				cNum = num.longValue();
			} else if (clazz.isAssignableFrom(Integer.class)) {
				cNum = num.intValue();
			} else if (clazz.isAssignableFrom(Byte.class)) {
				cNum = num.byteValue();
			} else if (clazz.isAssignableFrom(Float.class)) {
				cNum = num.floatValue();
			} else if (clazz.isAssignableFrom(Double.class)) {
				cNum = num.doubleValue();
			} else if (clazz.isAssignableFrom(Short.class)) {
				cNum = num.shortValue();
			} else if (clazz.isAssignableFrom(BigInteger.class)) {
				cNum = toBigInteger(num);
			} else if (clazz.isAssignableFrom(BigDecimal.class)) {
				cNum = toBigDecimal(num);
			}
		}

		return cNum;
	}

	private static Number castNumberExact(Number num, Class<? extends Number> clazz) {
		Number cNum = castNumberAPI(num, clazz);

		if (isSignificantDifference(num, cNum, 2)) {
			return null;
		}

		return cNum;
	}

	private static Number castNumberUpBound(Number num, Class<? extends Number> clazz) {
		return castNumberUpBound(num, clazz, clazz);
	}

	private static Number castNumberUpBound(Number num, Class<? extends Number> clazz, Class<? extends Number> oClazz) {
		Number cNum = castNumberAPI(num, clazz);

		if (isSignificantDifference(num, cNum, 2.0d)) {
			Class<? extends Number> uClazz = null;
			try {
				if (isFloatPoint(oClazz)) {
					int cIndex = FormatterContext.UP_BOUNDS_FP.indexOf(clazz);
					uClazz = FormatterContext.UP_BOUNDS_FP.get(cIndex + 1);
				} else {
					int cIndex = FormatterContext.UP_BOUNDS.indexOf(clazz);
					uClazz = FormatterContext.UP_BOUNDS.get(cIndex + 1);
				}
			} catch (IndexOutOfBoundsException exc) {
			}

			if (uClazz == null) {
				return null;
			}

			return castNumberUpBound(num, uClazz, oClazz);
		}

		return cNum;
	}

	private static boolean isSignificantDifference(Number num, Number cNum, double delta) {
		if (cNum == null) {
			return false;
		}

		if (cNum.getClass().isAssignableFrom(Float.class) //
				|| cNum.getClass().isAssignableFrom(Double.class)) {
			if (!Double.isFinite(cNum.doubleValue())) {
				return Double.isFinite(num.doubleValue());
			}
		}

		if (isFloatPoint(num.getClass()) || isFloatPoint(cNum.getClass())) {
			BigDecimal obd = toBigDecimal(num);
			BigDecimal cbd = toBigDecimal(cNum);

			return obd.compareTo(cbd) != 0 && Math.abs(obd.subtract(cbd).doubleValue()) > delta;
		} else {
			BigInteger obi = toBigInteger(num);
			BigInteger cbi = toBigInteger(cNum);

			return obi.compareTo(cbi) != 0 && Math.abs(obi.subtract(cbi).doubleValue()) > delta;
		}
	}

	private static boolean isFloatPoint(Class<? extends Number> clazz) {
		return clazz.isAssignableFrom(Float.class) //
				|| clazz.isAssignableFrom(Double.class) //
				|| clazz.isAssignableFrom(BigDecimal.class);
	}

	private static BigInteger toBigInteger(Number num) {
		if (num == null) {
			return null;
		}

		if (num instanceof BigInteger) {
			return (BigInteger) num;
		} else if (num instanceof BigDecimal) {
			return ((BigDecimal) num).toBigInteger();
		}

		return BigInteger.valueOf(num.longValue());
	}

	private static BigDecimal toBigDecimal(Number num) {
		if (num == null) {
			return null;
		}

		if (num instanceof BigDecimal) {
			return (BigDecimal) num;
		} else if (num instanceof BigInteger) {
			return new BigDecimal((BigInteger) num);
		}

		// NOTE: simple call of .doubleValue produces rounding errors on large (not necessarily Big) numbers.
		return new BigDecimal(num.toString());
	}

	/**
	 * Resolves number value from provided string.
	 *
	 * @param str
	 *            string defining numeric value
	 * @return number value built from provided {@code str}, or {@code null} if {@code str} is {@code null}, empty or
	 *         equals {@code "null"} ignoring case
	 *
	 * @see #strToNumber(String, int, String)
	 */
	public static Number strToNumber(String str) {
		return strToNumber(str, 10, null);
	}

	/**
	 * Resolves number value from provided string.
	 *
	 * @param str
	 *            string defining numeric value
	 * @param radix
	 *            radix the radix to be used in interpreting {@code str}
	 * @param typeExpr
	 *            number type expression containing type name and cast mode prefix
	 * @return number value built from provided {@code str}, or {@code null} if {@code str} is {@code null}, empty or
	 *         equals {@code "null"} ignoring case
	 */
	public static Number strToNumber(String str, int radix, String typeExpr) {
		if (isEmptyNumberStr(str)) {
			return null;
		}

		if (radix != 10) {
			try {
				return Integer.valueOf(str, radix);
			} catch (NumberFormatException ie) {
				try {
					return Long.valueOf(str, radix);
				} catch (NumberFormatException le) {
					return new BigInteger(str, radix);
				}
			}
		} else {
			return createNumber(str, typeExpr);
		}
	}

	private static Number createNumber(String str, String typeExpr) {
		String type = null;

		if (typeExpr != null) {
			if (typeExpr.startsWith(CastMode.API.symbol)) {
				type = typeExpr.substring(1);
			} else if (typeExpr.startsWith(CastMode.UP_BOUND.symbol)) {
				type = typeExpr.substring(1);
			} else {
				type = typeExpr;
			}
		}

		Class<? extends Number> nType = type == null ? null : FormatterContext.getNumberClass(type);
		if (nType == null) {
			return NumberUtils.createNumber(str);
		} else if (nType.isAssignableFrom(Long.class)) {
			return NumberUtils.createLong(str);
		} else if (nType.isAssignableFrom(Integer.class)) {
			return NumberUtils.createInteger(str);
		} else if (nType.isAssignableFrom(Byte.class)) {
			return NumberUtils.toByte(str);
		} else if (nType.isAssignableFrom(Float.class)) {
			return NumberUtils.createFloat(str);
		} else if (nType.isAssignableFrom(Double.class)) {
			return NumberUtils.createDouble(str);
		} else if (nType.isAssignableFrom(Short.class)) {
			return NumberUtils.toShort(str);
		} else if (nType.isAssignableFrom(BigInteger.class)) {
			return NumberUtils.createBigInteger(str);
		} else if (nType.isAssignableFrom(BigDecimal.class)) {
			return NumberUtils.createBigDecimal(str);
		} else {
			return NumberUtils.createNumber(str);
		}
	}

	private static boolean isEmptyNumberStr(String str) {
		return StringUtils.isEmpty(str) || "null".equalsIgnoreCase(str); // NON-NLS
	}

	/**
	 * Scales number value <tt>numValue</tt> by <tt>scale</tt> factor.
	 *
	 * @param numValue
	 *            number value to scale
	 * @param scale
	 *            scale factor
	 * @return scaled number value
	 */
	private static Number scaleNumber(Number numValue, Number scale) {
		if (numValue == null || scale == null) {
			return numValue;
		}

		Number scaledValue;
		if (numValue instanceof BigInteger) {
			BigDecimal bdv = new BigDecimal((BigInteger) numValue);
			scaledValue = bdv.multiply(new BigDecimal(scale.toString()));
		} else if (numValue instanceof BigDecimal) {
			BigDecimal bdv = (BigDecimal) numValue;
			scaledValue = bdv.multiply(new BigDecimal(scale.toString()));
		} else {
			// NOTE: simple call of .doubleValue produces rounding errors on large (not necessarily Big) numbers.
			// scaledValue = numValue.doubleValue() * scale.doubleValue();
			BigDecimal bdv = new BigDecimal(numValue.toString());
			scaledValue = bdv.multiply(new BigDecimal(scale.toString()));
		}
		return castNumber(scaledValue, numValue.getClass(), CastMode.UP_BOUND);
	}

	/**
	 * Formats the given object representing a number as a string using the specified pattern.
	 *
	 * @param pattern
	 *            format pattern
	 * @param value
	 *            number to format
	 * @param locale
	 *            locale for number format to use
	 * @return number formatted as a string
	 */
	public static String toString(String pattern, Object value, String locale) {
		FormatterContext formatter = new FormatterContext(pattern, locale);
		if (formatter.isFormatDefined()) {
			return formatter.format.format(value);
		} else {
			return String.valueOf(value);
		}
	}

	/**
	 * Number formatting context values.
	 */
	public static class FormatterContext {
		/**
		 * Constant defining generic number type name {@value}.
		 */
		public static final String ANY = "any"; // NON-NLS

		private static Map<String, Class<? extends Number>> NUMBER_TYPES = new HashMap<>(13);
		static {
			NUMBER_TYPES.put("int", Integer.class); // NON-NLS
			NUMBER_TYPES.put("integer", Integer.class); // NON-NLS
			NUMBER_TYPES.put("long", Long.class); // NON-NLS
			NUMBER_TYPES.put("double", Double.class); // NON-NLS
			NUMBER_TYPES.put("float", Float.class); // NON-NLS
			NUMBER_TYPES.put("byte", Byte.class); // NON-NLS
			NUMBER_TYPES.put("bigint", BigInteger.class); // NON-NLS
			NUMBER_TYPES.put("biginteger", BigInteger.class); // NON-NLS
			NUMBER_TYPES.put("bint", BigInteger.class); // NON-NLS
			NUMBER_TYPES.put("bigdec", BigDecimal.class); // NON-NLS
			NUMBER_TYPES.put("bigdecimal", BigDecimal.class); // NON-NLS
			NUMBER_TYPES.put("bdec", BigDecimal.class); // NON-NLS

			NUMBER_TYPES.put(ANY, Number.class); // NON-NLS
		}
		private static List<Class<? extends Number>> UP_BOUNDS = new ArrayList<>();
		static {
			UP_BOUNDS.add(Byte.class);
			UP_BOUNDS.add(Short.class);
			UP_BOUNDS.add(Integer.class);
			UP_BOUNDS.add(Long.class);
			UP_BOUNDS.add(Float.class);
			UP_BOUNDS.add(Double.class);
			UP_BOUNDS.add(BigInteger.class);
			UP_BOUNDS.add(BigDecimal.class);
		}
		private static List<Class<? extends Number>> UP_BOUNDS_FP = new ArrayList<>();
		static {
			UP_BOUNDS_FP.add(Float.class);
			UP_BOUNDS_FP.add(Double.class);
			UP_BOUNDS_FP.add(BigDecimal.class);
		}

		// private static final String GENERIC_NUMBER_PATTERN = "###,###.###"; // NON-NLS

		private String pattern;
		private String locale;
		private NumberFormat format;

		/**
		 * Creates a number formatter context using defined format <tt>pattern</tt></> and default locale.
		 * <p>
		 * Pattern also can be one of number types enumerators: {@code "integer"}, {@code "int"}, {@code "long"},
		 * {@code "double"}, {@code "float"}, {@code "short"}, {@code "byte"}, {@code "bigint"}, {@code "biginteger"},
		 * {@code "bint"}, {@code "bigdec"}, {@code "bigdecimal"}, {@code "bdec"} and {@value ANY}. {@value ANY} will
		 * resolve any possible numeric value out of provided string, e.g. {@code "30hj00"} will result {@code 30}.
		 * <p>
		 * {@code "~"} prefixed number type enumerator (except {@value ANY}) means numeric casting shall be performed
		 * using plain Java API, in some cases resulting significant value loss.
		 * <p>
		 * {@code "^"} prefixed number type enumerator (except {@value ANY}) means if value can't be cast to defined
		 * type, the closest upper bound type shall be used to maintain value without significant loss.
		 * <p>
		 * By default, exact casting (without significant value loss) is be performed. In case number can't be cast to
		 * target type, original value is kept.
		 *
		 * @param pattern
		 *            format pattern - can be set to {@code null} to use default representation.
		 */
		private FormatterContext(String pattern) {
			this(pattern, null);
		}

		/**
		 * Creates a number formatter context using defined format <tt>pattern</tt></> and <tt>locale</tt>.
		 * <p>
		 * Pattern also can be one of number types enumerators: {@code "integer"}, {@code "int"}, {@code "long"},
		 * {@code "double"}, {@code "float"}, {@code "short"}, {@code "byte"}, {@code "bigint"}, {@code "biginteger"},
		 * {@code "bint"}, {@code "bigdec"}, {@code "bigdecimal"}, {@code "bdec"} and {@value ANY}. {@value ANY} will
		 * resolve any possible numeric value out of provided string, e.g. {@code "30hj00"} will result {@code 30}.
		 * <p>
		 * {@code "~"} prefixed number type enumerator (except {@value ANY}) means numeric casting shall be performed
		 * using plain Java API, in some cases resulting significant value loss.
		 * <p>
		 * {@code "^"} prefixed number type enumerator (except {@value ANY}) means if value can't be cast to defined
		 * type, the closest upper bound type shall be used to maintain value without significant loss.
		 * <p>
		 * By default, exact casting (without significant value loss) is be performed. In case number can't be cast to
		 * target type, original value is kept.
		 * 
		 * @param pattern
		 *            format pattern - can be set to {@code null} to use default representation.
		 * @param locale
		 *            locale for decimal format to use, or {@code null} if default locale shall be used
		 */
		private FormatterContext(String pattern, String locale) {
			this.pattern = pattern;
			this.locale = locale;

			boolean numNamePattern = StringUtils.length(pattern) > 1;

			if (numNamePattern) {
				String numName = pattern.startsWith(CastMode.API.symbol) || pattern.startsWith(CastMode.UP_BOUND.symbol)
						? pattern.substring(1) : pattern;
				numNamePattern = getNumberClass(numName) != null;
			}

			if (!numNamePattern) {
				Locale loc = Utils.getLocale(locale);

				format = StringUtils.isEmpty(pattern) ? null : loc == null ? new DecimalFormat(pattern)
						: new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(loc));
			}
		}

		/**
		 * Checks if number format is defined for this formatter.
		 *
		 * @return {@code true} if number format is defined, {@code false} - otherwise
		 */
		private boolean isFormatDefined() {
			return format != null;
		}

		/**
		 * Builds number format instance to parse number from string using general-purpose number format for default or
		 * specified {@code locale}.
		 *
		 * @return general-purpose number format
		 *
		 * @see #getGPFormat(String)
		 */
		private NumberFormat getGPFormat() {
			return getGPFormat(locale);
		}

		/**
		 * Builds number format instance to parse number from string using general-purpose number format for default or
		 * specified <tt>locale</tt>.
		 *
		 * @param locale
		 *            locale for decimal format to use, or {@code null} if default locale shall be used
		 *
		 * @return general-purpose number format
		 */
		private static NumberFormat getGPFormat(String locale) {
			Locale loc = Utils.getLocale(locale);

			// return loc == null ? new DecimalFormat(GENERIC_NUMBER_PATTERN)
			// : new DecimalFormat(GENERIC_NUMBER_PATTERN, new DecimalFormatSymbols(loc));

			return loc == null ? NumberFormat.getNumberInstance() : NumberFormat.getNumberInstance(loc);
		}

		/**
		 * Resolves number class for provided number type name. Supported number type names are:
		 * <ul>
		 * <li>{@code "int"} or {@code "integer"} for {@link Integer}</li>
		 * <li>{@code "long"} for {@link Long}</li>
		 * <li>{@code "double"} for {@link Double}</li>
		 * <li>{@code "float"} for {@link Float}</li>
		 * <li>{@code "byte"} for {@link Byte}</li>
		 * <li>{@code "bigint"}, {@code "biginteger"}, {@code "bint"} for {@link BigInteger}</li>
		 * <li>{@code "bigdec"}, {@code "bigdecimal"}, {@code "bdec"} for {@link BigDecimal}</li>
		 * <li>{@value ANY} for {@link Number}</li>
		 * </ul>
		 * 
		 * @param type
		 *            number class type name
		 * @return number class for provided type name
		 */
		private static Class<? extends Number> getNumberClass(String type) {
			return NUMBER_TYPES.get(type.toLowerCase());
		}
	}

	/**
	 * Supported casting modes enumeration.
	 */
	public static enum CastMode {
		/**
		 * Numeric casting shall be performed using plain Java API, in some cases resulting significant value loss.
		 */
		API("~"), // NON-NLS
		/**
		 * Numeric casting shall be performed without significant value loss. In case number can't be cast to target
		 * type, original value is kept.
		 */
		EXACT(""), // NON-NLS
		/**
		 * Numeric casting shall be performed in such a way that when value can't be cast to defined type, closest upper
		 * bound type shall be used to maintain value without significant loss.
		 */
		UP_BOUND("^"); // NON-NLS

		private String symbol;

		private CastMode(String symbol) {
			this.symbol = symbol;
		}

		/**
		 * Returns cast mode prefix symbol.
		 * 
		 * @return cast mode prefix symbol.
		 */
		public String getSymbol() {
			return symbol;
		}
	}
}
