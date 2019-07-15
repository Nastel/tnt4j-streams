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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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

	private int radix = 10;
	private FormatterContext formatter = null;

	/**
	 * Creates a number formatter using the default numeric representation.
	 */
	public NumericFormatter() {
		this(null, null);
	}

	/**
	 * Creates a number formatter using the default numeric representation in the specified radix.
	 *
	 * @param radix
	 *            the radix to use while parsing numeric strings
	 */
	public NumericFormatter(int radix) {
		this.radix = radix;
	}

	/**
	 * Creates a number formatter/parser for numbers using the specified format pattern.
	 * <p>
	 * Pattern also can be one of number types enumerators: {@code "integer"}, {@code "int"}, {@code "long"},
	 * {@code "double"}, {@code "float"}, {@code "short"}, {@code "byte"}, {@code "bigint"}, {@code "biginteger"},
	 * {@code "bint"}, {@code "bigdec"}, {@code "bigdecimal"}, {@code "bdec"} and {@code "any"}. {@code "any"} will
	 * resolve any possible numeric value out of provided string, e.g. {@code "30hj00"} will result {@code 30}.
	 *
	 * @param pattern
	 *            format pattern - can be set to {@code null} to use default representation
	 * @param locale
	 *            locale for decimal format to use, or {@code null} if default locale shall be used
	 *
	 * @see #setPattern(String, String)
	 */
	public NumericFormatter(String pattern, String locale) {
		setPattern(pattern, locale);
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
	 * Sets the radix used by this formatter.
	 *
	 * @param radix
	 *            the radix to use while parsing numeric strings
	 */
	public void setRadix(int radix) {
		this.radix = radix;
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
	 * {@code "bint"}, {@code "bigdec"}, {@code "bigdecimal"}, {@code "bdec"} and {@code "any"}. {@code "any"} will
	 * resolve any possible numeric value out of provided string, e.g. {@code "30hj00"} will result {@code 30}.
	 *
	 * @param pattern
	 *            format pattern - can be set to {@code null} to use default representation
	 * @param locale
	 *            locale for decimal format to use, or {@code null} if default locale shall be used
	 */
	public void setPattern(String pattern, String locale) {
		formatter = new FormatterContext(pattern, locale);
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
	 * <p>
	 * Pattern also can be one of number types enumerators: {@code "integer"}, {@code "int"}, {@code "long"},
	 * {@code "double"}, {@code "float"}, {@code "short"}, {@code "byte"}, {@code "bigint"}, {@code "biginteger"},
	 * {@code "bint"}, {@code "bigdec"}, {@code "bigdecimal"}, {@code "bdec"} and {@code "any"}. {@code "any"} will
	 * resolve any possible numeric value out of provided string, e.g. {@code "30hj00"} will result {@code 30}.
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
	 * <p>
	 * Pattern also can be one of number types enumerators: {@code "integer"}, {@code "int"}, {@code "long"},
	 * {@code "double"}, {@code "float"}, {@code "short"}, {@code "byte"}, {@code "bigint"}, {@code "biginteger"},
	 * {@code "bint"}, {@code "bigdec"}, {@code "bigdecimal"}, {@code "bdec"} and {@code "any"}. {@code "any"} will
	 * resolve any possible numeric value out of provided string, e.g. {@code "30hj00"} will result {@code 30}.
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
	 * {@code "bint"}, {@code "bigdec"}, {@code "bigdecimal"}, {@code "bdec"} and {@code "any"}. {@code "any"} will
	 * resolve any possible numeric value out of provided string, e.g. {@code "30hj00"} will result {@code 30}.
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
			if (StringUtils.isEmpty(strValue)) {
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
					numValue = strToNumber(strValue, radix);
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
	 * 
	 * @param num
	 *            number value to cast
	 * @param type
	 *            number type name to cast number to
	 * @return number value cast to desired numeric type
	 */
	public static Number castNumber(Number num, String type) {
		if (StringUtils.isNotEmpty(type)) {
			return castNumber(num, FormatterContext.NUMBER_TYPES.get(type));
		}

		return num;
	}

	/**
	 * Casts provided number value to desired number type.
	 *
	 * @param num
	 *            number value to cast
	 * @param clazz
	 *            number class to cast number to
	 * @param <T>
	 *            desired number type
	 * @return number value cast to desired numeric type
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Number> T castNumber(Number num, Class<T> clazz) {
		if (num == null) {
			return null;
		}

		Number cNum = null;

		if (num instanceof BigInteger) {
			BigInteger bNum = (BigInteger) num;

			try {
				if (clazz.isAssignableFrom(Long.class)) {
					cNum = bNum.longValueExact();
				} else if (clazz.isAssignableFrom(Integer.class)) {
					cNum = bNum.intValueExact();
				} else if (clazz.isAssignableFrom(Byte.class)) {
					cNum = bNum.byteValueExact();
				} else if (clazz.isAssignableFrom(Short.class)) {
					cNum = bNum.shortValueExact();
				}
			} catch (ArithmeticException exc) {
				return null;
			}
		}

		if (num instanceof BigDecimal) {
			BigDecimal bNum = (BigDecimal) num;

			try {
				if (clazz.isAssignableFrom(Long.class)) {
					cNum = bNum.longValueExact();
				} else if (clazz.isAssignableFrom(Integer.class)) {
					cNum = bNum.intValueExact();
				} else if (clazz.isAssignableFrom(Byte.class)) {
					cNum = bNum.byteValueExact();
				} else if (clazz.isAssignableFrom(Short.class)) {
					cNum = bNum.shortValueExact();
				}
			} catch (ArithmeticException exc) {
				return null;
			}
		}

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

		return (T) (cNum == null ? num : cNum);
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

		return BigDecimal.valueOf(num.doubleValue());
	}

	/**
	 * Resolves number value from provided string.
	 *
	 * @param str
	 *            string defining numeric value
	 * @return number value built from provided {@code str}, or {@code null} if {@code str} is {@code null} or empty
	 *
	 * @see #strToNumber(String, int)
	 */
	public static Number strToNumber(String str) {
		return strToNumber(str, 10);
	}

	/**
	 * Resolves number value from provided string.
	 *
	 * @param str
	 *            string defining numeric value
	 * @param radix
	 *            radix the radix to be used in interpreting {@code str}
	 * @return number value built from provided {@code str}, or {@code null} if {@code str} is {@code null} or empty
	 */
	public static Number strToNumber(String str, int radix) {
		if (StringUtils.isEmpty(str)) {
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
			return NumberUtils.createNumber(str);
		}
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
			scaledValue = bdv.multiply(BigDecimal.valueOf(scale.doubleValue()));
		} else if (numValue instanceof BigDecimal) {
			BigDecimal bdv = (BigDecimal) numValue;
			scaledValue = bdv.multiply(BigDecimal.valueOf(scale.doubleValue()));
		} else {
			scaledValue = numValue.doubleValue() * scale.doubleValue();
		}
		return castNumber(scaledValue, numValue.getClass());
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
		public static final String ANY = "any"; // NON-NLS

		private static Map<String, Class<? extends Number>> NUMBER_TYPES = new HashMap<>(10);
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

		// private static final String GENERIC_NUMBER_PATTERN = "###,###.###"; // NON-NLS

		private String pattern;
		private String locale;
		private NumberFormat format;

		/**
		 * Creates a number formatter context using defined format <tt>pattern</tt></> and default locale.
		 * <p>
		 * Pattern also can be one of number types enumerators: {@code "integer"}, {@code "int"}, {@code "long"},
		 * {@code "double"}, {@code "float"}, {@code "short"}, {@code "byte"}, {@code "bigint"}, {@code "biginteger"},
		 * {@code "bint"}, {@code "bigdec"}, {@code "bigdecimal"}, {@code "bdec"} and {@code "any"}. {@code "any"} will
		 * resolve any possible numeric value out of provided string, e.g. {@code "30hj00"} will result {@code 30}.
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
		 * {@code "bint"}, {@code "bigdec"}, {@code "bigdecimal"}, {@code "bdec"} and {@code "any"}. {@code "any"} will
		 * resolve any possible numeric value out of provided string, e.g. {@code "30hj00"} will result {@code 30}.
		 *
		 * @param pattern
		 *            format pattern - can be set to {@code null} to use default representation.
		 * @param locale
		 *            locale for decimal format to use, or {@code null} if default locale shall be used
		 */
		private FormatterContext(String pattern, String locale) {
			this.pattern = pattern;
			this.locale = locale;

			Locale loc = Utils.getLocale(locale);

			if (pattern == null || NUMBER_TYPES.get(pattern.toLowerCase()) == null) {
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
	}
}
