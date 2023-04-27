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

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.FileSystem;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.HexDump;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.internal.LinkedTreeMap;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.core.OpType;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.NamedObject;

import groovy.util.CharsetToolkit;

/**
 * General utility methods used by TNT4J-Streams.
 *
 * @version $Revision: 1 $
 */
public final class Utils extends com.jkoolcloud.tnt4j.utils.Utils {

	/**
	 * Tags collection string token delimiter.
	 */
	public static final String TAG_DELIM = ","; // NON-NLS
	private static final String VALUE_DELIM = "\\|"; // NON-NLS
	private static final String HEX_PREFIX = "0x"; // NON-NLS
	private static final Pattern LINE_ENDINGS_PATTERN = Pattern.compile("(\\r\\n|\\r|\\n)"); // NON-NLS
	private static final Pattern UUID_PATTERN = Pattern
			.compile("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"); // NON-NLS

	/**
	 * Variable expression placeholder definition start token.
	 */
	public static final String VAR_EXP_START_TOKEN = "${"; // NON-NLS
	private static final String VAR_EXP_END_TOKEN = "}"; // NON-NLS
	private static final Pattern CFG_VAR_PATTERN = Pattern.compile("\\$\\{[\\w\\^\\[\\]=:.\\-+*/]+\\}"); // NON-NLS
	private static final Pattern EXPR_VAR_PATTERN = CFG_VAR_PATTERN;
	private static final int STACK_TRACE_ENTRIES_TO_LOG = Utils.getInt(
			"com.jkoolcloud.tnt4j.streams.log.stack.trace.depth", // NON-NLS
			System.getProperties(), -1);

	/**
	 * Default floating point numbers equality comparison difference tolerance {@value}.
	 */
	public static final double DEFAULT_EPSILON = 0.000001;

	/**
	 * Constant for system dependent line separator symbol.
	 */
	public static final String NEW_LINE = System.getProperty("line.separator");

	private Utils() {
	}

	/**
	 * Converts an array of bytes into a string representing the hexadecimal bytes values.
	 *
	 * @param src
	 *            byte sequence to encode
	 * @return encoded bytes hex string
	 */
	public static String encodeHex(byte[] src) {
		return Hex.encodeHexString(src);
	}

	/**
	 * Converts a string representing hexadecimal values into an array of bytes.
	 *
	 * @param str
	 *            String to convert
	 * @return decoded byte sequence
	 */
	public static byte[] decodeHex(String str) {
		byte[] ba = null;
		try {
			if (str != null && str.startsWith(HEX_PREFIX)) {
				str = str.substring(HEX_PREFIX.length());
			}
			ba = Hex.decodeHex((str == null ? "" : str).toCharArray());
		} catch (DecoderException e) {
		}
		return ba;
	}

	/**
	 * Maps OpType enumeration constant from string or numeric value.
	 *
	 * @param opType
	 *            object to be mapped to OpType enumeration constant
	 * @return OpType mapping, or {@code null} if mapping not found
	 */
	public static OpType mapOpType(Object opType) {
		if (opType == null) {
			return null;
		}
		if (opType instanceof Number) {
			return mapOpType(((Number) opType).intValue());
		}
		return mapOpType(String.valueOf(opType));
	}

	private static OpType mapOpType(String opType) {
		try {
			return OpType.valueOf(opType.toUpperCase());
		} catch (IllegalArgumentException exc) {
			if (StringUtils.equalsAnyIgnoreCase(opType, "END", "FINISH", "DISCONNECT", "COMMIT", "BACK")) { // NON-NLS
				return OpType.STOP;
			}
			if (StringUtils.equalsAnyIgnoreCase(opType, "CONNECT", "BEGIN")) { // NON-NLS
				return OpType.START;
			}
			if (StringUtils.equalsAnyIgnoreCase(opType, "CALLBACK", "GET")) { // NON-NLS
				return OpType.RECEIVE;
			}
			if (opType.equalsIgnoreCase("PUT")) { // NON-NLS
				return OpType.SEND;
			}
			if (StringUtils.endsWithIgnoreCase(opType, "OPEN")) { // NON-NLS
				return OpType.OPEN;
			}
			if (StringUtils.endsWithIgnoreCase(opType, "CLOSE")) { // NON-NLS
				return OpType.CLOSE;
			}
		}

		return OpType.OTHER;
	}

	private static OpType mapOpType(int opType) {
		try {
			return OpType.valueOf(opType);
		} catch (IllegalArgumentException exc) {
		}

		return OpType.OTHER;
	}

	/**
	 * Checks if provided string contains wildcard characters.
	 *
	 * @param str
	 *            string to check if it contains wildcard characters
	 * @return {@code true} if string contains wildcard characters
	 */
	public static boolean isWildcardString(String str) {
		if (StringUtils.isNotEmpty(str)) {
			return str.contains("*") || str.contains("?"); // NON-NLS
		}
		return false;
	}

	/**
	 * Transforms wildcard mask string to regex ready string.
	 *
	 * @param str
	 *            wildcard string
	 * @return regex ready string
	 */
	public static String wildcardToRegex(String str) {
		return StringUtils.isEmpty(str) ? str : str.replace("?", ".?").replace("*", ".*?"); // NON-NLS
	}

	/**
	 * Checks if string is wildcard mask string and if {@code true} then transforms it to regex ready string.
	 *
	 * @param str
	 *            string to check and transform
	 * @return regex ready string
	 *
	 * @see #wildcardToRegex(String)
	 */
	public static String wildcardToRegex2(String str) {
		return isWildcardString(str) ? wildcardToRegex(str) : str;
	}

	/**
	 * Counts text lines available in input.
	 *
	 * @param is
	 *            a {@link java.io.InputStream} object to provide the underlying file input stream
	 * @return number of lines currently available in input
	 * @throws java.io.IOException
	 *             If an I/O error occurs
	 */
	public static int countLines(InputStream is) throws IOException {
		try (InputStream bis = is instanceof BufferedInputStream ? (BufferedInputStream) is
				: new BufferedInputStream(is)) {
			byte[] cBuff = new byte[1024];
			int lCount = 0;
			int readChars = 0;
			boolean endsWithoutNewLine = false;
			boolean skipLF = false;
			while ((readChars = bis.read(cBuff)) != -1) {
				for (int i = 0; i < readChars; ++i) {
					int c = cBuff[i];
					if (skipLF) {
						skipLF = false;
						if (c == '\n') {
							continue;
						}
					}
					switch (c) {
					case '\r':
						skipLF = true;
					case '\n': /* Fall through */
						++lCount;
						break;
					}
				}
				endsWithoutNewLine = (cBuff[readChars - 1] != '\n');
			}
			if (endsWithoutNewLine) {
				++lCount;
			}
			return lCount;
		}
	}

	/**
	 * Returns first readable file path from {@code files} array which has last modification timestamp newer than
	 * {@code lastModif}. If {@code lastModif} is {@code null}, then newest readable file is returned.
	 *
	 * @param files
	 *            last modification timestamp ordered files array
	 * @param lastModif
	 *            last modification time to compare. If {@code null} - then newest file is returned
	 * @return first path from array which has modification time later than {@code lastModif}, or {@code null} if
	 *         {@code files} is empty or does not contain readable file with last modification time later than
	 *         {@code lastModif}
	 */
	public static Path getFirstNewer(Path[] files, Long lastModif) throws IOException {
		Path last = null;

		if (ArrayUtils.isNotEmpty(files)) {
			boolean changeDir = (Files.getLastModifiedTime(files[0], LinkOption.NOFOLLOW_LINKS)
					.compareTo(Files.getLastModifiedTime(lastOf(files), LinkOption.NOFOLLOW_LINKS)) < 0);

			for (int i = changeDir ? files.length - 1 : 0; changeDir ? i >= 0
					: i < files.length; i = changeDir ? i - 1 : i + 1) {
				Path f = files[i];
				if (Files.isReadable(f)) {
					if (lastModif == null) {
						last = f;
						break;
					} else {
						if (Files.getLastModifiedTime(f).toMillis() > lastModif) {
							last = f;
						} else {
							break;
						}
					}
				}
			}
		}

		return last;
	}

	/**
	 * De-serializes JSON data object ({@link String}, {@link java.io.Reader}, {@link java.io.InputStream}) into map
	 * structured data.
	 *
	 * @param jsonData
	 *            JSON format data object
	 * @param jsonAsLine
	 *            flag indicating that complete JSON data package is single line
	 * @return data map parsed from JSON data object
	 * @throws com.google.gson.JsonSyntaxException
	 *             if there was a problem reading from the Reader
	 * @throws com.google.gson.JsonIOException
	 *             if json is not a valid representation for an object of type
	 *
	 * @see com.google.gson.Gson#fromJson(String, Class)
	 * @see com.google.gson.Gson#fromJson(java.io.Reader, Class)
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, ?> fromJsonToMap(Object jsonData, boolean jsonAsLine) {
		Map<String, ?> map = new LinkedTreeMap<>();
		Gson gson = new Gson();

		if (jsonAsLine) {
			try {
				map = (Map<String, ?>) gson.fromJson(getStringLine(jsonData), map.getClass());
			} catch (IOException ioe) {
				throw new JsonIOException(ioe);
			}
		} else {
			if (jsonData instanceof String) {
				map = (Map<String, ?>) gson.fromJson((String) jsonData, map.getClass());
			} else if (jsonData instanceof byte[]) {
				map = (Map<String, ?>) gson.fromJson(getString((byte[]) jsonData), map.getClass());
			} else if (jsonData instanceof Reader) {
				map = (Map<String, ?>) gson.fromJson((Reader) jsonData, map.getClass());
			} else if (jsonData instanceof InputStream) {
				map = (Map<String, ?>) gson.fromJson(new BufferedReader(new InputStreamReader((InputStream) jsonData)),
						map.getClass());
			}
		}

		return map;
	}

	/**
	 * Returns string line read from data source. Data source object can be {@link String}, {@link java.io.Reader} or
	 * {@link java.io.InputStream}.
	 *
	 * @param data
	 *            data source object to read string line
	 * @return string line read from data source
	 * @throws java.io.IOException
	 *             If an I/O error occurs while reading line
	 *
	 * @see java.io.BufferedReader#readLine()
	 */
	public static String getStringLine(Object data) throws IOException {
		if (data == null) {
			return null;
		}

		BufferedReader rdr = null;
		boolean autoClose = false;
		if (data instanceof String) {
			rdr = new BufferedReader(new StringReader((String) data));
			autoClose = true;
		} else if (data instanceof byte[]) {
			rdr = new BufferedReader(new StringReader(getString((byte[]) data)));
			autoClose = true;
		} else if (data instanceof ByteBuffer) {
			ByteBuffer bb = (ByteBuffer) data;
			rdr = new BufferedReader(new StringReader(getString(bb.array())));
			autoClose = true;
		} else if (data instanceof BufferedReader) {
			rdr = (BufferedReader) data;
		} else if (data instanceof Reader) {
			rdr = new BufferedReader((Reader) data);
		} else if (data instanceof InputStream) {
			rdr = new BufferedReader(new InputStreamReader((InputStream) data));
		}

		try {
			return rdr == null ? null : rdr.readLine();
		} finally {
			if (autoClose) {
				close(rdr);
			}
		}
	}

	/**
	 * Returns tag strings array retrieved from provided data object. Data object can be string (tags delimiter ','),
	 * strings collection or strings array.
	 *
	 * @param tagsData
	 *            tags data object
	 * @return tag strings array, or {@code null} if arrays can't be made
	 */
	public static String[] getTags(Object tagsData) {
		if (tagsData instanceof byte[]) {
			return new String[] { encodeHex((byte[]) tagsData) };
		} else if (tagsData instanceof String) {
			return toArray((String) tagsData);
		} else if (tagsData instanceof String[]) {
			return (String[]) tagsData;
		} else if (isObjCollection(tagsData)) {
			Object[] tagsArray = makeArray(tagsData);

			if (ArrayUtils.isNotEmpty(tagsArray)) {
				List<String> tags = new ArrayList<>(tagsArray.length);
				for (Object aTagsArray : tagsArray) {
					String[] ta = getTags(aTagsArray);

					if (ArrayUtils.isNotEmpty(ta)) {
						Collections.addAll(tags, ta);
					}
				}

				return tags.toArray(new String[0]);
			}
		} else if (tagsData != null) {
			return new String[] { toString(tagsData) };
		}

		return null;
	}

	/**
	 * Makes strings array from string containing array value serialized as string, e.g. using
	 * {@link Arrays#toString(Object[])}.
	 *
	 * @param arrayString
	 *            array value serialized as string
	 * @return the array of strings resolved from {@code arrayString} value, or {@code null} if {@code arrayString} is
	 *         {@code null}
	 */
	public static String[] toArray(String arrayString) {
		if (arrayString == null) {
			return null;
		}

		arrayString = arrayString.trim();

		if (arrayString.isEmpty()) {
			return ArrayUtils.EMPTY_STRING_ARRAY;
		}

		// in case it is matrix array, flatten to single level array
		arrayString = arrayString.replace("][", TAG_DELIM).replace("][", TAG_DELIM); // NON-NLS
		arrayString = arrayString.replace("[", "").replace("]", ""); // NON-NLS

		String[] tags = arrayString.split(TAG_DELIM);

		for (int i = 0; i < tags.length; i++) {
			if (tags[i] != null) {
				tags[i] = tags[i].trim();
			}
		}

		return tags;
	}

	/**
	 * Makes Hex string representation (starting '0x') of byte array.
	 *
	 * @param bytes
	 *            byte array to represent as string
	 * @return hex string representation of byte array
	 *
	 * @see #encodeHex(byte[])
	 */
	public static String toHexString(byte[] bytes) {
		return HEX_PREFIX + encodeHex(bytes);
	}

	/**
	 * Cleans raw activity data. If activity data is string, removes 'new line' symbols from it. Returns same object
	 * otherwise.
	 *
	 * @param <T>
	 *            type of raw activity data
	 * @param activityData
	 *            raw activity data
	 * @return raw activity data without 'new line' symbols
	 */
	@SuppressWarnings("unchecked")
	public static <T> T cleanActivityData(T activityData) {
		if (activityData instanceof String) {
			String ads = (String) activityData;

			return (T) LINE_ENDINGS_PATTERN.matcher(ads).replaceAll(""); // NON-NLS
		}

		return activityData;
	}

	/**
	 * Returns UUID found in provided string.
	 *
	 * @param str
	 *            string to find UUID
	 * @return found uuid
	 */
	public static UUID findUUID(String str) {
		Matcher m = UUID_PATTERN.matcher(str);
		String fnUUID = null;
		if (m.find()) {
			fnUUID = m.group();
		}

		return StringUtils.isEmpty(fnUUID) ? null : UUID.fromString(fnUUID);
	}

	/**
	 * Transforms (serializes) XML DOM document to string.
	 *
	 * @param doc
	 *            document to transform to string
	 * @return XML string representation of document
	 * @throws javax.xml.transform.TransformerException
	 *             If an exception occurs while transforming XML DOM document to string
	 */
	public static String documentToString(Node doc) throws TransformerException {
		StringWriter sw = new StringWriter();
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no"); // NON-NLS
		transformer.setOutputProperty(OutputKeys.METHOD, "xml"); // NON-NLS
		transformer.setOutputProperty(OutputKeys.INDENT, "yes"); // NON-NLS
		transformer.setOutputProperty(OutputKeys.ENCODING, UTF8);
		transformer.setOutputProperty(OutputKeys.VERSION, "1.1"); // NON-NLS

		transformer.transform(new DOMSource(doc), new StreamResult(sw));

		return sw.toString();
		// NOTE: if return as single line
		// return sw.toString().replaceAll("\n|\r", ""); // NON-NLS
	}

	/**
	 * Checks equality of two double numbers with difference tolerance {@value #DEFAULT_EPSILON}.
	 *
	 * @param d1
	 *            first double to compare
	 * @param d2
	 *            second double to compare
	 * @return {@code true} if difference is less than epsilon, {@code false} - otherwise
	 */
	public static boolean equals(double d1, double d2) {
		return equals(d1, d2, DEFAULT_EPSILON);
	}

	/**
	 * Checks equality of two double numbers with given difference tolerance {@code epsilon}.
	 *
	 * @param d1
	 *            first double to compare
	 * @param d2
	 *            second double to compare
	 * @param epsilon
	 *            value of difference tolerance
	 * @return {@code true} if difference is less than epsilon, {@code false} - otherwise
	 */
	public static boolean equals(double d1, double d2, double epsilon) {
		return Math.abs(d1 - d2) < epsilon;
	}

	/**
	 * Close the socket without exceptions.
	 * <p>
	 * For Java 6 backward comparability.
	 *
	 * @param socket
	 *            socket to close
	 */
	public static void close(Socket socket) {
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException exc) {

			}
		}
	}

	/**
	 * Close the server socket without exceptions.
	 * <p>
	 * For Java 6 backward comparability.
	 *
	 * @param socket
	 *            server socket to close
	 */
	public static void close(ServerSocket socket) {
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException exc) {

			}
		}
	}

	/**
	 * Returns first non-empty text string available to read from defined reader.
	 *
	 * @param reader
	 *            reader to use for reading
	 * @return non-empty text string, or {@code null} if the end of the stream has been reached
	 * @throws java.io.IOException
	 *             If an I/O error occurs
	 */
	public static String getNonEmptyLine(BufferedReader reader) throws IOException {
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.trim().isEmpty()) {
				// empty line
			} else {
				break;
			}
		}

		return line;
	}

	/**
	 * Reads all text available to read from defined {@code reader}.
	 *
	 * @param reader
	 *            reader to use for reading
	 * @return tests string read from {@code reader}
	 * @throws IOException
	 *             If an I/O error occurs
	 */
	public static String readAll(BufferedReader reader) throws IOException {
		StringBuilder sb = new StringBuilder();

		String line;
		while ((line = reader.readLine()) != null) {
			if (sb.length() != 0) {
				sb.append("\n");
			}
			sb.append(line);
		}

		return sb.toString();
	}

	/**
	 * Reads text lines into one string from provided {@link java.io.InputStream}.
	 *
	 * @param is
	 *            input stream to read
	 * @param separateLines
	 *            flag indicating whether to make string lines separated
	 * @return string read from input stream
	 *
	 * @see #readInput(java.io.BufferedReader, boolean)
	 */
	public static String readInput(InputStream is, boolean separateLines) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String text = null;
		try {
			text = readInput(reader, separateLines);
		} catch (IOException exc) {
		} finally {
			close(reader);
		}

		return text;
	}

	/**
	 * Reads text lines into one string from provided {@link java.io.BufferedReader}.
	 *
	 * @param reader
	 *            reader to read string
	 * @param separateLines
	 *            flag indicating whether to make string lines separated
	 * @return string read from reader
	 */
	public static String readInput(BufferedReader reader, boolean separateLines) throws IOException {
		StringBuilder builder = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			builder.append(line);
			builder.append(separateLines ? NEW_LINE : " ");
		}

		return builder.toString();
	}

	/**
	 * Gets a string representation of specified object for use in debugging, which includes the value of each object
	 * field.
	 *
	 * @param obj
	 *            object to get debugging info
	 * @return debugging string representation
	 */
	public static String getDebugString(Object obj) {
		if (obj == null) {
			return "null";
		}

		StringBuilder sb = new StringBuilder();
		Class<?> objClass = obj.getClass();
		sb.append(objClass.getSimpleName()).append('{');

		try {
			Field[] valueObjFields = objClass.getDeclaredFields();

			for (int i = 0; i < valueObjFields.length; i++) {
				String fieldName = valueObjFields[i].getName();
				valueObjFields[i].setAccessible(true);

				String valueStr = toString(valueObjFields[i].get(obj));

				sb.append(fieldName).append("=").append(sQuote(valueStr)) // NON-NLS
						.append(i < valueObjFields.length - 1 ? ", " : ""); // NON-NLS
			}
		} catch (Exception exc) {
			sb.append(getExceptionMessages(exc));
		}

		sb.append('}');

		return sb.toString();
	}

	/**
	 * Clears string builder content.
	 *
	 * @param sb
	 *            string builder to clear content
	 * @return empty string builder
	 */
	public static StringBuilder clear(StringBuilder sb) {
		return sb == null ? null : sb.delete(0, sb.length());
	}

	/**
	 * Clears string buffer content.
	 *
	 * @param sb
	 *            string buffer to clear content
	 * @return empty string buffer
	 */
	public static StringBuffer clear(StringBuffer sb) {
		return sb == null ? null : sb.delete(0, sb.length());
	}

	/**
	 * Checks if number object is {@code null} or has value equal to {@code 0}.
	 *
	 * @param number
	 *            number object to check
	 * @return {@code true} if number is {@code null} or number value is {@code 0}, {@code false} - otherwise
	 */
	public static boolean isZero(Number number) {
		return number == null || number.intValue() == 0;
	}

	/**
	 * Finds variable expressions like '${varName}' in provided array of strings and puts into collection.
	 *
	 * @param vars
	 *            collection to add resolved variable expression
	 * @param attrs
	 *            array of {@link String}s to find variable expressions
	 */
	public static void resolveCfgVariables(Collection<String> vars, String... attrs) {
		if (attrs != null) {
			for (String attr : attrs) {
				if (StringUtils.isNotEmpty(attr)) {
					Matcher m = CFG_VAR_PATTERN.matcher(attr);
					while (m.find()) {
						vars.add(m.group());
					}
				}
			}
		}
	}

	/**
	 * Finds variable expressions like '${VarName}' in provided string and puts into collection.
	 *
	 * @param vars
	 *            collection to add resolved variable expression
	 * @param exprStr
	 *            expression string
	 */
	public static void resolveExpressionVariables(Collection<String> vars, String exprStr) {
		if (StringUtils.isNotEmpty(exprStr)) {
			Matcher m = EXPR_VAR_PATTERN.matcher(exprStr);
			while (m.find()) {
				vars.add(m.group(0));
			}
		}
	}

	/**
	 * Checks provided expression string contains variable placeholders having {@code "${VAR_NAME}"} format.
	 *
	 * @param exp
	 *            expression string to check
	 * @return {@code true} if expression contains variable placeholders, {@code false} - otherwise
	 */
	public static boolean isVariableExpression(String exp) {
		return exp != null && EXPR_VAR_PATTERN.matcher(exp).find();
	}

	/**
	 * Makes expressions used variable placeholder representation.
	 *
	 * @param varName
	 *            variable name
	 * @return variable name surround by expression tokens
	 */
	public static String makeExpVariable(String varName) {
		return VAR_EXP_START_TOKEN + varName + VAR_EXP_END_TOKEN;
	}

	/**
	 * Makes {@link Object} type array from provided object instance.
	 * <p>
	 * Acts same as {@link #makeArray(Object, boolean)}, where {@code makeArray(obj, false)}.
	 *
	 * @param obj
	 *            object instance to make an array
	 * @return array made of provided object, or {@code null} if {@code obj} is {@code null}
	 *
	 * @see #makeArray(Object, boolean)
	 */
	public static Object[] makeArray(Object obj) {
		return makeArray(obj, false);
	}

	/**
	 * Makes {@link Object} type array from provided object instance.
	 * <p>
	 * Acts same as {@link #makeArray(Object, Class, boolean)}, where {@code makeArray(obj, null, splitRelatives)}.
	 * 
	 * @param obj
	 *            object instance to make an array
	 * @param splitPrimitives
	 *            flag indicating whether to split primitives array into object elements array
	 * @return array made of provided object, or {@code null} if {@code obj} is {@code null}
	 * 
	 * @see #makeArray(Object, Class, boolean)
	 */
	public static Object[] makeArray(Object obj, boolean splitPrimitives) {
		return makeArray(obj, null, splitPrimitives);
	}

	/**
	 * Makes {@link Object} type array from provided object instance.
	 * <p>
	 * Acts same as {@link #makeArray(Object, Class, boolean)}, where {@code makeArray(obj, cls, false)}.
	 * 
	 * @param obj
	 *            object instance to make an array
	 * @param cls
	 *            desired array type class, can be {@code null} - then array type will be the most common class of all
	 *            array/collection elements
	 * @return array made of provided object, or {@code null} if {@code obj} is {@code null}
	 * 
	 * @see #makeArray(Object, Class, boolean)
	 */
	public static Object[] makeArray(Object obj, Class<?> cls) {
		return makeArray(obj, cls, false);
	}

	/**
	 * Makes {@link Object} type array from provided object instance.
	 * <p>
	 * <ul>
	 * <li>If {@code obj} is {@code Object[]}, then method {@link #makeArray(Object[], Class)} is called.</li>
	 * <li>If {@code obj} is {@link java.util.Collection}, then method {@link #makeArray(java.util.Collection, Class)}
	 * is called.</li>
	 * <li>If {@code obj} is {@link java.lang.Iterable}, then all elements of iterable are recollected into array.</li>
	 * <li>In all other cases - new single item array is created.</li>
	 * </ul>
	 * <p>
	 * Particular type of produced array is the most common class for all parameter {@code obj} defined array/collection
	 * elements, but not lower level than defined {@code cls}
	 *
	 * @param obj
	 *            object instance to make an array
	 * @param cls
	 *            desired array type class, can be {@code null} - then array type will be the most common class of all
	 *            array/collection elements
	 * @param splitPrimitives
	 *            flag indicating whether to split primitives array into object elements array
	 * @return array made of provided object, or {@code null} if {@code obj} is {@code null}
	 *
	 * @see #makeArray(Object[], Class)
	 * @see #makeArray(java.util.Collection, Class)
	 * @see #primitiveArrayToObject(Object)
	 */
	public static Object[] makeArray(Object obj, Class<?> cls, boolean splitPrimitives) {
		if (obj == null) {
			return null;
		}

		if (isObjArray(obj)) {
			return makeArray((Object[]) obj, cls);
		}
		if (isPrimitiveArray(obj) && splitPrimitives) {
			return primitiveArrayToObject(obj);
		}
		if (obj instanceof Collection) {
			return makeArray((Collection<?>) obj, cls);
		}
		if (obj instanceof Iterable) {
			Collection<Object> iColl = new ArrayList<>();
			for (Object t : (Iterable<?>) obj) {
				iColl.add(t);
			}
			return iColl.toArray();
		}

		return makeArray(new Object[] { obj }, cls);
	}

	/**
	 * Makes {@link T} type array from provided {@link java.util.Collection} instance {@code coll}.
	 *
	 * @param coll
	 *            collection to make an array
	 * @return an array containing all the elements in provided collection
	 *
	 * @see #makeArray(java.util.Collection, Class)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] makeArray(Collection<T> coll) {
		return (T[]) makeArray(coll, null);
	}

	/**
	 * Makes {@link Object} type array from provided {@link java.util.Collection} instance {@code coll}. Particular type
	 * of produced array is the most common class for all parameter {@code coll} defined collection elements, but not
	 * lower level than defined {@code cls}.
	 *
	 * @param coll
	 *            collection to make an array
	 * @param cls
	 *            desired array type class, can be {@code null} - then array type will be the most common class of all
	 *            collection elements
	 * @return an array containing all elements in provided collection
	 *
	 * @see #determineCommonClass(Iterable, Class)
	 */
	public static Object[] makeArray(Collection<?> coll, Class<?> cls) {
		if (coll == null) {
			return null;
		}

		Object[] array = (Object[]) Array.newInstance(determineCommonClass(coll, cls), coll.size());

		return coll.toArray(array);
	}

	/**
	 * Makes {@link Object} type array from provided object {@code array}. Particular type of produced array is the most
	 * common class for all parameter {@code array} defined array elements.
	 *
	 * @param array
	 *            array to change type
	 * @return the changed type array
	 *
	 * @see #makeArray(Object[], Class)
	 */
	public static Object[] makeArray(Object[] array) {
		return makeArray(array, null);
	}

	/**
	 * Makes {@link Object} type array from provided object {@code array}. Particular type of produced array is the most
	 * common class for all parameter {@code array} defined array elements, but not lower level than defined
	 * {@code cls}.
	 *
	 * @param array
	 *            array to change type
	 * @param cls
	 *            desired array type class, can be {@code null} - then array type will be the most common class of all
	 *            {@code array} array elements
	 * @return the changed type array
	 *
	 * @see #determineCommonClass(Object[], Class)
	 */
	public static Object[] makeArray(Object[] array, Class<?> cls) {
		if (array == null) {
			return null;
		}

		Object[] cArray = (Object[]) Array.newInstance(determineCommonClass(array, cls), array.length);
		System.arraycopy(array, 0, cArray, 0, array.length);

		return cArray;
	}

	/**
	 * Converts primitives array to boxed counterpart objects array.
	 * 
	 * @param obj
	 *            an array of primitives
	 * @return an array of boxed objects
	 */
	public static Object[] primitiveArrayToObject(Object obj) {
		if (obj instanceof int[]) {
			return ArrayUtils.toObject((int[]) obj);
		}
		if (obj instanceof byte[]) {
			return ArrayUtils.toObject((byte[]) obj);
		}
		if (obj instanceof long[]) {
			return ArrayUtils.toObject((long[]) obj);
		}
		if (obj instanceof double[]) {
			return ArrayUtils.toObject((double[]) obj);
		}
		if (obj instanceof float[]) {
			return ArrayUtils.toObject((float[]) obj);
		}
		if (obj instanceof char[]) {
			return ArrayUtils.toObject((char[]) obj);
		}
		if (obj instanceof short[]) {
			return ArrayUtils.toObject((short[]) obj);
		}
		if (obj instanceof boolean[]) {
			return ArrayUtils.toObject((boolean[]) obj);
		}

		return null;
	}

	/**
	 * Makes {@link Iterable} type object from provided object instance.
	 * <p>
	 * Acts same as {@link #makeIterable(Object, boolean)}, where {@code makeIterable(obj, false)}.
	 * 
	 * @param obj
	 *            object instance to make an iterable
	 * @return iterable made of provided object, or {@code null} if {@code obj} is {@code null}
	 * 
	 * @see #makeIterable(Object, boolean)
	 */
	public static Iterable<?> makeIterable(Object obj) {
		return makeIterable(obj, false);
	}

	/**
	 * Makes {@link Iterable} type object from provided object instance.
	 * <p>
	 * <ul>
	 * <li>If {@code obj} is {@link java.lang.Iterable}, then simple casting is performed.</li>
	 * <li>If {@code obj} is {@code Object[]}, then method {@link java.util.Arrays#asList(Object[])} is called.</li>
	 * <li>If {@code obj} is primitives array and {@code splitPrimitives} is set to {@code true}, then method
	 * {@link #primitiveArrayToObject(Object)} is used to convert them into object elements array and method
	 * {@link java.util.Arrays#asList(Object[])} an iterable.</li>
	 * <li>In all other cases - new single item iterator is created using
	 * {@link java.util.Collections#singleton(Object)}</li>
	 * </ul>
	 * 
	 * @param obj
	 *            object instance to make an iterable
	 * @param splitPrimitives
	 *            flag indicating whether to split primitives array into object elements array
	 * @return iterable made of provided object, or {@code null} if {@code obj} is {@code null}
	 * 
	 * @see Arrays#asList(Object[])
	 * @see #primitiveArrayToObject(Object)
	 * @see Collections#singleton(Object)
	 */
	public static Iterable<?> makeIterable(Object obj, boolean splitPrimitives) {
		if (obj == null) {
			return null;
		}

		if (obj instanceof Iterable) {
			return (Iterable<?>) obj;
		}
		if (isObjArray(obj)) {
			return Arrays.asList((Object[]) obj);
		}
		if (isPrimitiveArray(obj) && splitPrimitives) {
			return Arrays.asList(primitiveArrayToObject(obj));
		}

		return Collections.singleton(obj);
	}

	/**
	 * Determines common collection elements class.
	 *
	 * @param c
	 *            collection instance to determine common elements type
	 * @param clazz
	 *            initial class level to start determination, can be {@code null} - then returned class will be the most
	 *            common class of all {@code c} collection elements
	 * @return class common for all collection elements, but not lower level than defined {@code clazz}
	 */
	public static Class<?> determineCommonClass(Iterable<?> c, Class<?> clazz) {
		Class<?> cls = clazz;

		if (c != null) {
			for (Object ci : c) {
				if (ci != null) {
					cls = getCommonClass(ci.getClass(), cls);
				}
			}
		}

		return cls == null ? Object.class : cls;
	}

	/**
	 * Determines common collection elements class.
	 *
	 * @param a
	 *            array instance to determine common elements type
	 * @param clazz
	 *            initial class level to start determination, can be {@code null} - then returned class will be the most
	 *            common class of all {@code a} array elements
	 * @return class common for all collection elements, but not lower level than defined {@code clazz}
	 */
	public static Class<?> determineCommonClass(Object[] a, Class<?> clazz) {
		Class<?> cls = clazz;

		if (ArrayUtils.isNotEmpty(a)) {
			for (Object ai : a) {
				if (ai != null) {
					cls = getCommonClass(ai.getClass(), cls);
				}
			}
		}

		return cls == null ? Object.class : cls;
	}

	/**
	 * Determines common class for provided classes.
	 *
	 * @param fc
	 *            first class
	 * @param sc
	 *            second class
	 * @return common class for provided classes
	 *
	 * @see Class#isAssignableFrom(Class)
	 * @see Class#getSuperclass()
	 */
	public static <T> Class<? super T> getCommonClass(Class<T> fc, Class<?> sc) {
		Class<? super T> cc = fc;

		if (sc != null) {
			while (!cc.isAssignableFrom(sc)) {
				cc = cc.getSuperclass();
			}
		}

		return cc;
	}

	/**
	 * Makes {@link Map.Entry} type array from provided {@link java.util.Map} instance {@code map}.
	 *
	 * @param map
	 *            map to make an array
	 * @return an array containing all the elements in provided map
	 *
	 * @see #makeArray(java.util.Collection)
	 */
	public static <K, V> Map.Entry<K, V>[] makeArray(Map<K, V> map) {
		return map == null ? null : makeArray(map.entrySet());
	}

	/**
	 * Checks if provided object is {@link java.util.Collection} or any type of array.
	 *
	 * @param obj
	 *            object to check
	 * @return {@code true} if {@code obj} is {@link java.util.Collection} or any type of array, {@code false} -
	 *         otherwise
	 *
	 * @see #isArray(Object)
	 */
	public static boolean isCollection(Object obj) {
		return isArray(obj) || obj instanceof Collection;
	}

	/**
	 * Checks if provided object is {@link java.util.Collection} or {@code Object[]}.
	 *
	 * @param obj
	 *            object to check
	 * @return {@code true} if {@code obj} is {@link java.util.Collection} or {@code Object[]}, {@code false} -
	 *         otherwise
	 *
	 * @see #isObjArray(Object)
	 */
	public static boolean isObjCollection(Object obj) {
		return isObjArray(obj) || obj instanceof Collection;
	}

	/**
	 * Checks if provided class represents an array class or is implementation of {@link java.util.Collection}.
	 *
	 * @param cls
	 *            class to check
	 * @return {@code true} if cls is implementation of {@link java.util.Collection} or represents an array class,
	 *         {@code false} - otherwise
	 */
	public static boolean isCollectionType(Class<?> cls) {
		return cls != null && (cls.isArray() || Collection.class.isAssignableFrom(cls));
	}

	/**
	 * Checks if provided object is {@link java.lang.Iterable} or any type of array.
	 * 
	 * @param obj
	 *            object to check
	 * @return {@code true} if {@code obj} is {@link java.lang.Iterable} or any type of array, {@code false} - otherwise
	 * 
	 * @see #isArray(Object)
	 */
	public static boolean isIterable(Object obj) {
		return isArray(obj) || obj instanceof Iterable;
	}

	/**
	 * Checks if provided object is {@link java.lang.Iterable} or {@code Object[]}..
	 *
	 * @param obj
	 *            object to check
	 * @return {@code true} if {@code obj} is {@link java.lang.Iterable} or {@code Object[]}., {@code false} - otherwise
	 *
	 * @see #isObjArray(Object)
	 */
	public static boolean isObjIterable(Object obj) {
		return isObjArray(obj) || obj instanceof Iterable;
	}

	/**
	 * Wraps provided object item seeking by index.
	 * <p>
	 * If {@code obj} is not {@link java.util.Collection} or {@code Object[]}, then same object is returned.
	 * <p>
	 * In case of {@link java.util.Collection} - it is transformed to {@code Object[]}.
	 * <p>
	 * When {@code obj} is {@code Object[]} - array item referenced by index is returned if
	 * {@code index < array.length}, first array item if {@code array.length == 1}, or {@code null} in all other cases.
	 *
	 * @param obj
	 *            object instance containing item
	 * @param index
	 *            item index
	 * @return item found, or {@code null} if {@code obj} is {@code null} or {@code index >= array.length}
	 */
	public static Object getItem(Object obj, int index) {
		if (obj instanceof Collection) {
			obj = makeArray((Collection<?>) obj);
		}

		if (isObjArray(obj)) {
			Object[] array = (Object[]) obj;

			return index < array.length ? array[index] : array.length == 1 ? array[0] : null;
		}

		return obj;
	}

	/**
	 * Returns the appropriate string representation for the specified object.
	 *
	 * @param value
	 *            object to convert to string representation
	 *
	 * @return string representation of object
	 */
	public static String toString(Object value) {
		if (value instanceof byte[]) {
			return getString((byte[]) value);
			// return Arrays.toString((byte[]) value);
		}
		if (value instanceof char[]) {
			return new String((char[]) value);
			// return Arrays.toString((char[]) value);
		}

		if (value instanceof Document) {
			try {
				return documentToString((Node) value);
			} catch (TransformerException exc) {
			}
		}
		if (value instanceof Node) {
			return ((Node) value).getTextContent();
		}

		return com.jkoolcloud.tnt4j.utils.Utils.toString(value);
	}

	/**
	 * Returns the appropriate string representation for the specified array.
	 *
	 * @param args
	 *            array to convert to string representation
	 * @return string representation of array
	 *
	 * @see #toStringDeep(Object[])
	 */
	public static String arrayToString(Object... args) {
		return toStringDeep(args);
	}

	/**
	 * Returns single object (first item) if list/array contains single item, makes an array from
	 * {@link java.util.Collection}, or returns same value as parameter in all other cases.
	 *
	 * @param value
	 *            object value to simplify
	 * @return same value as parameter if it is not array or collection; first item of list/array if it contains single
	 *         item; array of values if list/array contains more than one item; {@code null} if parameter object is
	 *         {@code null} or list/array is empty
	 */
	public static Object simplifyValue(Object value) {
		if (value instanceof Collection) {
			return simplifyValue((Collection<?>) value);
		}
		if (isObjArray(value)) {
			return simplifyValue((Object[]) value);
		}

		return value;
	}

	private static Object simplifyValue(Collection<?> valuesList) {
		if (CollectionUtils.isEmpty(valuesList)) {
			return null;
		}

		return valuesList.size() == 1 ? valuesList.iterator().next() : makeArray(valuesList);
	}

	private static Object simplifyValue(Object[] valuesArray) {
		if (ArrayUtils.isEmpty(valuesArray)) {
			return null;
		}

		return valuesArray.length == 1 ? valuesArray[0] : valuesArray;
	}

	/**
	 * Makes a HEX dump string representation of provided bytes array. Does all the same as
	 * {@link #toHexDump(byte[], int)} setting {@code offset} parameter to {@code 0}.
	 *
	 * @param b
	 *            bytes array make HEX dump
	 * @return returns HEX dump representation of provided bytes array
	 *
	 * @see #toHexDump(byte[], int, int)
	 */
	public static String toHexDump(byte[] b) {
		return toHexDump(b, 0);
	}

	/**
	 * Returns the appropriate string representation for the specified object.
	 * <p>
	 * If {@code data} is byte array, HEX dump representation is returned.
	 *
	 * @param data
	 *            object to convert to string representation
	 * @return string representation of object
	 */
	public static String toStringDump(Object data) {
		if (data instanceof byte[]) {
			return toHexDump((byte[]) data);
		}

		return toString(data);
	}

	/**
	 * Makes a HEX dump string representation of provided bytes array. Does all the same as
	 * {@link #toHexDump(byte[], int, int)} setting {@code len} parameter to {@code 0}.
	 *
	 * @param b
	 *            bytes array make HEX dump
	 * @param offset
	 *            offset at which to start dumping bytes
	 * @return returns HEX dump representation of provided bytes array
	 *
	 * @see #toHexDump(byte[], int, int)
	 */
	public static String toHexDump(byte[] b, int offset) {
		return toHexDump(b, offset, 0);
	}

	/**
	 * Makes a HEX dump string representation of provided bytes array.
	 *
	 * @param b
	 *            bytes array make HEX dump
	 * @param offset
	 *            offset at which to start dumping bytes
	 * @param len
	 *            maximum number of bytes to dump
	 * @return returns HEX dump representation of provided bytes array
	 */
	public static String toHexDump(byte[] b, int offset, int len) {
		if (ArrayUtils.isEmpty(b)) {
			return "<EMPTY>"; // NON-NLS
		}

		String hexStr;
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream(b.length * 2)) {
			if (len > 0 && len < b.length) {
				byte[] bc = Arrays.copyOfRange(b, offset, offset + len);
				HexDump.dump(bc, 0, bos, 0);
			} else {
				HexDump.dump(b, 0, bos, offset);
			}
			hexStr = NEW_LINE + bos.toString(UTF8);
		} catch (Exception exc) {
			hexStr = "HEX FAIL: " + getExceptionMessages(exc); // NON-NLS
		}

		return hexStr;
	}

	/**
	 * Splits object identification path expression into path nodes array.
	 * <p>
	 * If empty path node is found, it is removed from path.
	 *
	 * @param path
	 *            object identification path
	 * @param nps
	 *            path nodes separator
	 * @return array of path nodes, or {@code null} if path is empty
	 */
	public static String[] getNodePath(String path, String nps) {
		if (StringUtils.isNotEmpty(path)) {
			String[] pArray = StringUtils.isEmpty(nps) ? new String[] { path } : path.split(Pattern.quote(nps));
			List<String> pList = new ArrayList<>(pArray.length);
			for (String pe : pArray) {
				if (StringUtils.isNotEmpty(pe)) {
					pList.add(pe);
				}
			}

			return pList.toArray(new String[0]);
		}

		return null;
	}

	/**
	 * Checks if provided object is an objects array {@code Object[]}.
	 *
	 * @param obj
	 *            object to check
	 * @return {@code true} if {@code obj} is an array, {@code false} - otherwise
	 */
	public static boolean isObjArray(Object obj) {
		return obj instanceof Object[];
	}

	/**
	 * Checks if provided object is an array of primitives.
	 * 
	 * @param obj
	 *            object to check
	 * @return {@code true} if {@code obj} is an array of primitives, {@code false} - otherwise
	 */
	public static boolean isPrimitiveArray(Object obj) {
		return obj != null && obj.getClass().isArray();
	}

	/**
	 * Checks if provided object is an array - of primitives or objects.
	 *
	 * @param obj
	 *            object to check
	 * @return {@code true} if {@code obj} is an array, {@code false} - otherwise
	 *
	 * @see #isObjArray(Object)
	 * @see #isPrimitiveArray(Object)
	 */
	public static boolean isArray(Object obj) {
		return isObjArray(obj) || isPrimitiveArray(obj);
	}

	/**
	 * Sleeps current running thread and silently consumes thrown {@link InterruptedException}.
	 *
	 * @param millis
	 *            the length of time to sleep in milliseconds
	 *
	 * @see Thread#sleep(long)
	 */
	public static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException exc) {
		}
	}

	/**
	 * Sleeps current running thread and silently consumes thrown {@link InterruptedException}.
	 *
	 * @param millis
	 *            the length of time to sleep in milliseconds
	 * @param nanos
	 *            {@code 0-999999} additional nanoseconds to sleep
	 *
	 * @see Thread#sleep(long, int)
	 */
	public static void sleep(long millis, int nanos) {
		try {
			Thread.sleep(millis, nanos);
		} catch (InterruptedException exc) {
		}
	}

	/**
	 * Sleeps current running thread and silently consumes thrown {@link InterruptedException}.
	 *
	 * @param tUnit
	 *            time units to sleep
	 * @param timeout
	 *            sleep time value
	 *
	 * @see java.util.concurrent.TimeUnit#sleep(long)
	 */
	public static void sleep(TimeUnit tUnit, long timeout) {
		try {
			tUnit.sleep(timeout);
		} catch (InterruptedException exc) {
		}
	}

	/**
	 * Loads properties from file/resource referenced by provided system property.
	 *
	 * @param propKey
	 *            system property key referencing properties file path or resource name
	 * @return properties loaded from file
	 * @throws java.io.IOException
	 *             if an error occurred when reading properties file
	 * @throws java.lang.IllegalArgumentException
	 *             if system property value is {@code null} or empty
	 *
	 * @see #loadProperties(String)
	 */
	public static Properties loadPropertiesFor(String propKey) throws IOException {
		String propFile = System.getProperty(propKey);

		return loadProperties(propFile);
	}

	/**
	 * Loads properties from file.
	 *
	 * @param propFile
	 *            properties file path
	 * @return properties loaded from file
	 * @throws java.io.IOException
	 *             if an error occurred when reading properties file
	 * @throws java.lang.IllegalArgumentException
	 *             if properties file path is {@code null} or empty
	 *
	 * @see java.util.Properties#load(java.io.InputStream)
	 */
	public static Properties loadPropertiesFile(String propFile) throws IOException {
		if (StringUtils.isEmpty(propFile)) {
			throw new IllegalArgumentException(
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "Utils.empty.file.name"));
		}

		Properties fProps = new Properties();

		try (InputStream is = Files.newInputStream(Paths.get(propFile))) {
			fProps.load(is);
		}

		return fProps;
	}

	/**
	 * Loads properties from resource with the given name.
	 *
	 * @param name
	 *            the resource name
	 * @return properties loaded from resource
	 * @throws java.io.IOException
	 *             if an error occurred when reading properties file
	 *
	 * @see java.lang.ClassLoader#getResourceAsStream(String)
	 * @see java.util.Properties#load(java.io.InputStream)
	 */
	public static Properties loadPropertiesResource(String name) throws IOException {
		Properties rProps = new Properties();
		ClassLoader loader = getClassLoader();

		try (InputStream ins = loader.getResourceAsStream(name)) {
			rProps.load(ins);
		}

		return rProps;
	}

	/**
	 * Loads properties from all resource with the given name.
	 *
	 * @param name
	 *            the resource name
	 * @return properties loaded from all found resources
	 * @throws java.io.IOException
	 *             if an error occurred when reading properties file
	 *
	 * @see java.lang.ClassLoader#getResources(String)
	 * @see java.util.Properties#load(java.io.InputStream)
	 */
	public static Properties loadPropertiesResources(String name) throws IOException {
		Properties rProps = new Properties();

		ClassLoader loader = getClassLoader();
		Enumeration<URL> rEnum = loader.getResources(name);

		while (rEnum.hasMoreElements()) {
			try (InputStream ins = rEnum.nextElement().openStream()) {
				rProps.load(ins);
			}
		}

		return rProps;
	}

	/**
	 * Loads properties from file or resource with the given name.
	 * 
	 * @param propFile
	 *            properties file path or resource name
	 * @return properties loaded from file/resource
	 * @throws java.io.IOException
	 *             if an error occurred when reading properties file
	 * @throws java.lang.IllegalArgumentException
	 *             if properties file path is {@code null} or empty
	 *
	 * @see #loadPropertiesFile(String)
	 * @see #loadPropertiesResource(String)
	 */
	public static Properties loadProperties(String propFile) throws IOException {
		try {
			return loadPropertiesFile(propFile);
		} catch (IOException exc) {
			try {
				return loadPropertiesResource(propFile);
			} catch (IOException ioe) {
				throw exc;
			}
		}
	}

	/**
	 * Sets property {@code propName} value {@code propValue}, if it is absent in provided properties set {@code props}.
	 * 
	 * @param props
	 *            properties set to check and alter
	 * @param propName
	 *            property name
	 * @param propValue
	 *            property value
	 */
	public static void setPropertyIfAbsent(Properties props, String propName, String propValue) {
		if (props == null) {
			return;
		}

		if (StringUtils.isEmpty(props.getProperty(propName))) {
			props.setProperty(propName, propValue);
		}
	}

	/**
	 * Finds resource with the given name.
	 *
	 * @param name
	 *            the resource name
	 * @return {@link URL} object for reading the resource, or {@code null} if the resource could not be found or the
	 *         invoker doesn't have adequate privileges to get the resource.
	 *
	 * @see java.lang.ClassLoader#getResource(String)
	 */
	public static URL getResource(String name) {
		ClassLoader loader = getClassLoader();
		return loader.getResource(name);
	}

	/**
	 * Creates {@link java.io.BufferedReader} instance to read provided bytes data.
	 *
	 * @param data
	 *            bytes data to read
	 * @return bytes data reader
	 */
	public static BufferedReader bytesReader(byte[] data) {
		return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data)));
	}

	/**
	 * Creates file according to provided file descriptor - file path or file URI.
	 *
	 * @param fileDescriptor
	 *            string representing file path or URI
	 * @return file instance
	 * @throws IllegalArgumentException
	 *             if {@code fileDescriptor} is {@code null} or empty
	 * @throws java.net.URISyntaxException
	 *             if {@code fileDescriptor} defines malformed URI
	 */
	public static File createFile(String fileDescriptor) throws URISyntaxException {
		if (StringUtils.isEmpty(fileDescriptor)) {
			throw new IllegalArgumentException(
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "Utils.empty.file.descriptor"));
		}

		if (fileDescriptor.startsWith("file:/")) { // NON-NLS
			URI fUri = new URI(fileDescriptor);
			return Paths.get(fUri).toFile();
		} else {
			return Paths.get(fileDescriptor).toFile();
		}
	}

	/**
	 * Checks if provided numeric value match the mask.
	 *
	 * @param v
	 *            numeric value
	 * @param mask
	 *            mask to check
	 * @return {@code true} if value match the mask, {@code false} - otherwise
	 */
	public static boolean matchMask(int v, int mask) {
		return (v & mask) == mask;
	}

	/**
	 * Checks if provided numeric value match any bit of the mask.
	 *
	 * @param v
	 *            numeric value
	 * @param mask
	 *            mask to check
	 * @return {@code true} if value match any bit from the mask, {@code false} - otherwise
	 */
	public static boolean matchAny(int v, int mask) {
		return (v & mask) != 0;
	}

	/**
	 * Removes {@code null} elements from array.
	 *
	 * @param array
	 *            array to clean up
	 * @param <T>
	 *            type of array elements
	 * @return new array instance without {@code null} elements
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] tidy(T[] array) {
		if (array == null) {
			return null;
		}

		List<T> oList = new ArrayList<>(array.length);
		for (T obj : array) {
			if (obj != null) {
				oList.add(obj);
			}
		}

		return makeArray(oList);
	}

	/**
	 * Removes {@code null} elements from collection.
	 *
	 * @param coll
	 *            collection to clean
	 * @param <T>
	 *            type of collection elements
	 * @return new {@link java.util.List} instance without {@code null} elements
	 */
	public static <T> List<T> tidy(Collection<T> coll) {
		if (coll == null) {
			return null;
		}

		List<T> oList = new ArrayList<>(coll.size());
		for (T obj : coll) {
			if (obj != null) {
				oList.add(obj);
			}
		}

		return oList;
	}

	/**
	 * Sets system property value.
	 * <p>
	 * If property name is {@code null} - does nothing. If property value is {@code null} - removes property from system
	 * properties list.
	 *
	 * @param pName
	 *            the name of the system property
	 * @param pValue
	 *            the value of the system property
	 * @return the previous string value of the system property, or {@code null} if name is {@code null} or there was no
	 *         property with that name.
	 */
	public static String setSystemProperty(String pName, String pValue) {
		if (StringUtils.isEmpty(pName)) {
			return null;
		}

		if (pValue != null) {
			return System.setProperty(pName, pValue);
		} else {
			return System.clearProperty(pName);
		}
	}

	/**
	 * Returns enumeration entry based on entry name value ignoring case.
	 *
	 * @param enumClass
	 *            enumeration instance class
	 * @param name
	 *            name of enumeration entry
	 * @param <E>
	 *            type of enumeration
	 * @return enumeration object having provided name
	 * @throws IllegalArgumentException
	 *             if name is not a valid enumeration entry name or is {@code null}
	 */
	public static <E extends Enum<E>> E valueOfIgnoreCase(Class<E> enumClass, String name)
			throws IllegalArgumentException {
		if (StringUtils.isEmpty(name)) {
			throw new IllegalArgumentException(
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "Utils.name.empty"));
		}

		E[] enumConstants = enumClass.getEnumConstants();
		for (E ec : enumConstants) {
			if (ec.name().equalsIgnoreCase(name)) {
				return ec;
			}
		}

		throw new IllegalArgumentException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
				"Utils.no.enum.constant", name, enumClass.getSimpleName()));
	}

	/**
	 * Returns a quoted string, surrounded with single quote.
	 *
	 * @param str
	 *            string handle
	 * @return a quoted string, surrounded with single quote
	 *
	 * @see #surround(String, String)
	 */
	public static String sQuote(String str) {
		return surround(str, "'"); // NON-NLS
	}

	/**
	 * Returns a quoted object representation string, surrounded with single quote.
	 *
	 * @param obj
	 *            object handle
	 * @return a quoted object representation string, surrounded with single quote
	 *
	 * @see #sQuote(String)
	 * @see #toString(Object)
	 */
	public static String sQuote(Object obj) {
		return sQuote(toString(obj));
	}

	/**
	 * Splits string contained values delimited using '|' delimiter into array of separate values.
	 *
	 * @param value
	 *            string contained values to split
	 * @return array of split string values
	 */
	public static String[] splitValue(String value) {
		return StringUtils.isEmpty(value) ? new String[] { value } : value.split(VALUE_DELIM);
	}

	/**
	 * Searches for files matching name pattern. Name pattern also may contain path of directory, where file search
	 * should be performed, e.g., C:/Tomcat/logs/localhost_access_log.*.txt. If no path is defined (just file name
	 * pattern) then files are searched in {@code System.getProperty("user.dir")}. Files array is ordered by file
	 * modification timestamp in ascending order. Used file system is {@link java.nio.file.FileSystems#getDefault()}.
	 *
	 * @param namePattern
	 *            name pattern to find files
	 *
	 * @return array of found files
	 *
	 * @see #listFilesByName(String, java.nio.file.FileSystem)
	 */
	public static Path[] searchFiles(String namePattern) throws IOException {
		return searchFiles(namePattern, FileSystems.getDefault());
	}

	/**
	 * Searches for files matching name pattern. Name pattern also may contain path of directory, where file search
	 * should be performed, e.g., C:/Tomcat/logs/localhost_access_log.*.txt. If no path is defined (just file name
	 * pattern) then files are searched in {@code System.getProperty("user.dir")}. Files array is ordered by file
	 * modification timestamp in ascending order.
	 *
	 * @param namePattern
	 *            name pattern to find files
	 * @param fs
	 *            file system to use
	 * @return array of found files
	 * 
	 * @throws java.io.IOException
	 *             if an I/O error occurs
	 *
	 * @see WildcardFileFilter#WildcardFileFilter(String)
	 * @see File#listFiles(FilenameFilter)
	 */
	public static Path[] searchFiles(String namePattern, FileSystem fs) throws IOException {
		if (fs == null) {
			fs = FileSystems.getDefault();
		}
		Path f;
		Path dir;
		String glob;
		try {
			f = fs.getPath(namePattern);
			dir = f.toAbsolutePath().getParent();
			glob = "*";
		} catch (InvalidPathException e) {
			int lastSeparator = Math.max(namePattern.lastIndexOf(fs.getSeparator()), namePattern.lastIndexOf("/"));
			if (lastSeparator != -1) {
				dir = fs.getPath(namePattern.substring(0, lastSeparator));
				glob = namePattern.substring(lastSeparator + 1);
			} else {
				dir = fs.getPath(".");
				glob = namePattern;
			}
		}

		List<Path> files = new ArrayList<>();
		try (DirectoryStream<Path> activityFiles = Files.newDirectoryStream(dir, glob)) {
			for (Path p : activityFiles) {
				if (!Files.isDirectory(p)) {
					files.add(p);
				}
			}
		}

		files.sort(new Comparator<Path>() {
			@Override
			public int compare(Path o1, Path o2) {
				try {
					return Files.getLastModifiedTime(o1).compareTo(Files.getLastModifiedTime(o2));
				} catch (IOException e) {
					// handle exception
					return 0;
				}
			}
		});

		return files.toArray(new Path[0]);
	}

	/**
	 * Returns list of files matching provided file name. If file name contains wildcard symbols, then
	 * {@link #searchFiles(String)} is invoked. Used file system is {@link java.nio.file.FileSystems#getDefault()}.
	 *
	 * @param fileName
	 *            file name pattern to list matching files
	 * @return array of files matching file name
	 *
	 * @see #listFilesByName(String, java.nio.file.FileSystem)
	 */
	public static Path[] listFilesByName(String fileName) throws IOException {
		return listFilesByName(fileName, FileSystems.getDefault());
	}

	/**
	 * Returns list of files matching provided file name. If file name contains wildcard symbols, then
	 * {@link #searchFiles(String)} is invoked.
	 *
	 * @param fileName
	 *            file name pattern to list matching files
	 * @param fs
	 *            file system to use
	 * @return array of files matching file name
	 *
	 * @see #searchFiles(String)
	 */
	public static Path[] listFilesByName(String fileName, FileSystem fs) throws IOException {
		if (fs == null) {
			fs = FileSystems.getDefault();
		}
		if (isWildcardString(fileName)) {
			return searchFiles(fileName, fs);
		} else {
			return new Path[] { fs.getPath(fileName) };
		}
	}

	/**
	 * Resolves map contained value by provided map keys path.
	 * <p>
	 * Path delimiter value is {@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#DEFAULT_PATH_DELIM}, path
	 * level value is {@code 0} and accessed paths set is {@code null}.
	 * <p>
	 * If map keys path token is {@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#MAP_NODE_TOKEN}, then
	 * complete map instance is returned for that path level.
	 * <p>
	 * If map keys path token is {@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#MAP_UNMAPPED_TOKEN}, then
	 * all yet un-accessed (not contained in {@code accessedPaths} set) map entries are returned for that path level.
	 *
	 * @param path
	 *            map keys path string referencing wanted value
	 * @param dataMap
	 *            data map to get value from
	 * @return path resolved map contained value
	 *
	 * @see #getNodePath(String, String)
	 * @see #getMapValueByPath(String, Map, int, Set)
	 */
	public static Object getMapValueByPath(String path, Map<String, ?> dataMap) {
		return getMapValueByPath(path, dataMap, 0, null);
	}

	/**
	 * Resolves map contained value by provided map keys path.
	 * <p>
	 * Path delimiter value is {@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#DEFAULT_PATH_DELIM} and path
	 * level value is {@code 0}.
	 * <p>
	 * If map keys path token is {@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#MAP_NODE_TOKEN}, then
	 * complete map instance is returned for that path level.
	 * <p>
	 * If map keys path token is {@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#MAP_UNMAPPED_TOKEN}, then
	 * all yet un-accessed (not contained in {@code accessedPaths} set) map entries are returned for that path level.
	 *
	 * @param path
	 *            map keys path string referencing wanted value
	 * @param dataMap
	 *            data map to get value from
	 * @param accessedPaths
	 *            set of accessed map paths
	 * @return path resolved map contained value
	 *
	 * @see #getNodePath(String, String)
	 * @see #getMapValueByPath(String, Map, int, Set)
	 */
	public static Object getMapValueByPath(String path, Map<String, ?> dataMap, Set<String[]> accessedPaths) {
		return getMapValueByPath(path, dataMap, 0, accessedPaths);
	}

	/**
	 * Resolves map contained value by provided map keys path.
	 * <p>
	 * Path delimiter value is {@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#DEFAULT_PATH_DELIM}.
	 * <p>
	 * If map keys path token is {@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#MAP_NODE_TOKEN}, then
	 * complete map instance is returned for that path level.
	 * <p>
	 * If map keys path token is {@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#MAP_UNMAPPED_TOKEN}, then
	 * all yet un-accessed (not contained in {@code accessedPaths} set) map entries are returned for that path level.
	 *
	 * @param path
	 *            map keys path string referencing wanted value
	 * @param dataMap
	 *            data map to get value from
	 * @param level
	 *            path level
	 * @param accessedPaths
	 *            set of accessed map paths
	 * @return path resolved map contained value
	 *
	 * @see #getNodePath(String, String)
	 * @see #getMapValueByPath(String, String, Map, int, Set)
	 */
	public static Object getMapValueByPath(String path, Map<String, ?> dataMap, int level,
			Set<String[]> accessedPaths) {
		return getMapValueByPath(path, StreamsConstants.DEFAULT_PATH_DELIM, dataMap, level, accessedPaths);
	}

	/**
	 * Resolves map contained value by provided map keys path.
	 * <p>
	 * Path level value is {@code 0}.
	 * <p>
	 * If map keys path token is {@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#MAP_NODE_TOKEN}, then
	 * complete map instance is returned for that path level.
	 * <p>
	 * If map keys path token is {@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#MAP_UNMAPPED_TOKEN}, then
	 * all yet un-accessed (not contained in {@code accessedPaths} set) map entries are returned for that path level.
	 *
	 * @param path
	 *            map keys path string referencing wanted value
	 * @param pathDelim
	 *            path delimiter
	 * @param dataMap
	 *            data map to get value from
	 * @param accessedPaths
	 *            set of accessed map paths
	 * @return path resolved map contained value
	 *
	 * @see #getNodePath(String, String)
	 * @see #getMapValueByPath(String, String, Map, int, Set)
	 */
	public static Object getMapValueByPath(String path, String pathDelim, Map<String, ?> dataMap,
			Set<String[]> accessedPaths) {
		return getMapValueByPath(path, pathDelim, dataMap, 0, accessedPaths);
	}

	/**
	 * Resolves map contained value by provided map keys path.
	 * <p>
	 * If map keys path token is {@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#MAP_NODE_TOKEN}, then
	 * complete map instance is returned for that path level.
	 * <p>
	 * If map keys path token is {@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#MAP_UNMAPPED_TOKEN}, then
	 * all yet un-accessed (not contained in {@code accessedPaths} set) map entries are returned for that path level.
	 *
	 * @param path
	 *            map keys path string referencing wanted value
	 * @param pathDelim
	 *            path delimiter
	 * @param dataMap
	 *            data map to get value from
	 * @param level
	 *            path level
	 * @param accessedPaths
	 *            set of accessed map paths
	 * @return path resolved map contained value
	 *
	 * @see #getNodePath(String, String)
	 * @see #getMapValueByPath(String[], Object, int, java.util.Set)
	 */
	public static Object getMapValueByPath(String path, String pathDelim, Map<String, ?> dataMap, int level,
			Set<String[]> accessedPaths) {
		return getMapValueByPath(getNodePath(path, pathDelim), dataMap, level, accessedPaths);
	}

	/**
	 * Resolves map contained value by provided map keys path.
	 * <p>
	 * Path level value is {@code 0}.
	 * <p>
	 * If map keys path token is {@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#MAP_NODE_TOKEN}, then
	 * complete map instance is returned for that path level.
	 * <p>
	 * If map keys path token is {@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#MAP_UNMAPPED_TOKEN}, then
	 * all yet un-accessed (not contained in {@code accessedPaths} set) map entries are returned for that path level.
	 *
	 * @param path
	 *            map keys path tokens array referencing wanted value
	 * @param dataMap
	 *            data map to get value from
	 * @param accessedPaths
	 *            set of accessed map paths
	 * @return path resolved map contained value
	 *
	 * @see #getMapValueByPath(String[], Object, int, java.util.Set)
	 */
	public static Object getMapValueByPath(String[] path, Map<String, ?> dataMap, Set<String[]> accessedPaths) {
		return getMapValueByPath(path, dataMap, 0, accessedPaths);
	}

	/**
	 * Resolves map contained value by provided map keys path.
	 * <p>
	 * If map keys path token is {@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#MAP_NODE_TOKEN}, then
	 * complete map instance is returned for that path level.
	 * <p>
	 * If map keys path token is {@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#MAP_UNMAPPED_TOKEN}, then
	 * all yet un-accessed (not contained in {@code accessedPaths} set) map entries are returned for that path level.
	 *
	 * @param path
	 *            map keys path tokens array referencing wanted value
	 * @param dataSet
	 *            data map/collection to get value from
	 * @param level
	 *            path level
	 * @param accessedPaths
	 *            set of accessed map paths
	 * @return path resolved map contained value
	 */
	@SuppressWarnings("unchecked")
	public static Object getMapValueByPath(String[] path, Object dataSet, int level, Set<String[]> accessedPaths) {
		if (ArrayUtils.isEmpty(path) || dataSet == null) {
			return null;
		}

		Object val;
		if (StreamsConstants.MAP_NODE_TOKEN.equals(path[level])) {
			val = dataSet;
		} else {
			if (dataSet instanceof Map) {
				Map<String, ?> dataMap = (Map<String, ?>) dataSet;
				if (StreamsConstants.MAP_UNMAPPED_TOKEN.equals(path[level])) {
					return getUnaccessedMapEntries(frontArray(path, level), accessedPaths, dataMap);
				} else {
					val = dataMap.get(path[level]);

					if (val != null && level < path.length - 1) {
						val = getMapValueByPath(path, val, level + 1, accessedPaths);
					}
				}
			} else if (isIndexedCollection(dataSet)) {
				try {
					int lii = Integer.parseInt(getItemIndexStr(path[level]));
					val = getCollectionElement(dataSet, lii);
				} catch (IllegalArgumentException | IndexOutOfBoundsException exc) {
					val = null;
				}

				if (val != null && level < path.length - 1) {
					val = getMapValueByPath(path, val, level + 1, accessedPaths);
				}
			} else {
				val = dataSet;
			}
		}

		if (accessedPaths != null) {
			accessedPaths.add(path);
		}

		return val;
	}

	/**
	 * Checks if provided object is array or {@link java.util.List} and element of such collection can be accessed over
	 * numeric index.
	 * 
	 * @param obj
	 *            object to check
	 * @return {@code true} if {@code obj} is {@link java.util.List} or array, {@code false} - otherwise
	 */
	public static boolean isIndexedCollection(Object obj) {
		return isArray(obj) || obj instanceof List;
	}

	/**
	 * Returns the element at the specified position in provided array/collection/iterable object instance {@code obj}.
	 * 
	 * @param obj
	 *            array/list instance to get element
	 * @param index
	 *            index of the element to return
	 * @return the value of the indexed component in the specified array/collection/iterable
	 * 
	 * @throws IllegalArgumentException
	 *             if the specified object is not an array
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} argument is negative, or if it is greater than or equal to the
	 *             length/size of the specified array/list
	 */
	public static Object getCollectionElement(Object obj, int index)
			throws IllegalArgumentException, IndexOutOfBoundsException {
		// return CollectionUtils.get(obj, index);
		if (isArray(obj)) {
			return Array.get(obj, index);
		}
		if (obj instanceof List) {
			return ((List<?>) obj).get(index);
		}
		if (obj instanceof Iterable) {
			return IterableUtils.get((Iterable<?>) obj, index);
		}

		return CollectionUtils.get(obj, index);
	}

	private static String getItemIndexStr(String indexToken) {
		return indexToken.replaceAll("\\D+", ""); // NON-NLS
	}

	private static Map<String, ?> getUnaccessedMapEntries(String[] pathPrefix, Set<String[]> accessedPaths,
			Map<String, ?> dataMap) {
		if (CollectionUtils.isEmpty(accessedPaths)) {
			return dataMap;
		}

		Map<String, ?> unaccessedMap = copyMap(dataMap);

		for (String[] accessedPath : accessedPaths) {
			removeMapValueByPath(trimPathPrefix(pathPrefix, accessedPath), unaccessedMap);
		}

		return unaccessedMap;
	}

	private static String[] trimPathPrefix(String[] prefix, String[] fullPath) {
		if (ArrayUtils.isEmpty(prefix)) {
			return fullPath;
		}

		if (prefix.length >= fullPath.length) {
			return null;
		}

		for (int i = 0; i < prefix.length; i++) {
			if (!prefix[i].equals(fullPath[i])) {
				return null;
			}
		}

		return endArray(fullPath, prefix.length);
	}

	/**
	 * Removed map contained entry by provided map keys path.
	 * <p>
	 * If map keys path token is {@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#MAP_NODE_TOKEN}, then all
	 * map entries at that path level is removed, e.g. path {@code "*"} is equivalent to {@link java.util.Map#clear()}.
	 * 
	 * @param path
	 *            map keys path tokens array referencing wanted entry
	 * @param dataMap
	 *            data map to remove entry from
	 * @return removed entry value, or {@code null} if no entry has been removed
	 */
	@SuppressWarnings("unchecked")
	public static Object removeMapValueByPath(String[] path, Map<String, ?> dataMap) {
		if (ArrayUtils.isEmpty(path) || dataMap == null) {
			return null;
		}

		Object val = null;
		String lastLevel = lastOf(path);
		String[] pathMinusOne = Arrays.copyOf(path, path.length - 1);

		Object valueBeforeLast;
		if (pathMinusOne.length == 0) {
			valueBeforeLast = dataMap;
		} else {
			valueBeforeLast = getMapValueByPath(pathMinusOne, dataMap, 0, null);
		}

		if (valueBeforeLast instanceof Map) {
			if (lastLevel == StreamsConstants.MAP_NODE_TOKEN) {
				val = new HashMap<>((Map<String, ?>) valueBeforeLast); // return removed branch
				((Map<String, ?>) valueBeforeLast).clear();
			} else {
				val = ((Map<String, ?>) valueBeforeLast).remove(lastLevel);
			}

			// clear empty branch
			if (MapUtils.isEmpty((Map<String, ?>) valueBeforeLast)) {
				removeMapValueByPath(pathMinusOne, dataMap);
			}
		}

		return val;
	}

	/**
	 * Makes a deep copy of provided {@code oMap} instance. Map copy instance entries do not have references to original
	 * map, so altering copy map entries will not affect original map.
	 * 
	 * @param oMap
	 *            original map instance to copy
	 * @param <K>
	 *            map key type
	 * @param <V>
	 *            map value type
	 * @return copy of {@code oMap}, or {@code null} if original map is {@code null}
	 */
	@SuppressWarnings("unchecked")
	public static <K, V> Map<K, V> copyMap(Map<K, V> oMap) {
		if (oMap == null) {
			return null;
		}
		Map<K, V> cMap = (Map<K, V>) SerializationUtils.clone((Serializable) oMap);

		return cMap;
	}

	/**
	 * Extracts variable name from provided variable placeholder string {@code varPlh}.
	 *
	 * @param varPlh
	 *            variable placeholder string
	 * @return variable name found within placeholder string, or {@code varPlh} value if it is empty or does not start
	 *         with {@value #VAR_EXP_START_TOKEN}
	 */
	public static String getVarName(String varPlh) {
		if (StringUtils.isNotEmpty(varPlh)) {
			if (varPlh.startsWith(VAR_EXP_START_TOKEN)) {
				return varPlh.substring(VAR_EXP_START_TOKEN.length(), varPlh.length() - VAR_EXP_END_TOKEN.length());
			}
		}

		return varPlh;
	}

	/**
	 * Checks if {@code array} and all its elements are empty.
	 *
	 * @param array
	 *            array instance to check
	 * @return {@code true} if {@code array} is empty or all its elements are empty, {@code false} - otherwise
	 *
	 * @see #isEmpty(Object)
	 */
	public static boolean isEmptyContent(Object[] array) {
		if (ArrayUtils.isEmpty(array)) {
			return true;
		}

		for (Object ao : array) {
			if (!isEmpty(ao)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Checks if {@code collection} and all its elements are empty.
	 *
	 * @param collection
	 *            collection instance to check
	 * @return {@code true} if {@code collection} is empty or all its elements are empty, {@code false} - otherwise
	 *
	 * @see #isEmpty(Object)
	 */
	public static boolean isEmptyContent(Collection<?> collection) {
		if (CollectionUtils.isEmpty(collection)) {
			return true;
		}

		for (Object co : collection) {
			if (!isEmpty(co)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Checks if {@code obj} is empty and has empty content considering when it is:
	 * <ul>
	 *
	 * <li>{@link String} - is {@code null} or equal to {@code ""}</li>
	 * <li>{@code Array} - is {@code null}, length is {@code 0} or has all empty elements</li>
	 * <li>{@link Collection} - is {@code null}, length is {@code 0} or has all empty elements</li>
	 * <li>{@link Map} - is {@code null} or length is {@code 0}</li>
	 * <li>{@link Object} - is {@code null}</li>
	 * </ul>
	 *
	 * @param obj
	 *            object to check
	 * @param whitespaceStringEmpty
	 *            flag indicating string having all whitespace symbols shall be treated as empty
	 * @return {@code true} if {@code obj} is empty or all array/collection elements has empty content, {@code false} -
	 *         otherwise
	 *
	 * @see #isEmptyContent(Object[])
	 * @see #isEmptyContent(java.util.Collection)
	 */
	public static boolean isEmptyContent(Object obj, boolean whitespaceStringEmpty) {
		if (obj == null) {
			return true;
		}
		if (obj instanceof String) {
			String str = (String) obj;
			return whitespaceStringEmpty ? StringUtils.trimToNull(str) == null : StringUtils.isEmpty(str);
		}
		if (isArray(obj)) {
			boolean empty = ArrayUtils.getLength(obj) == 0;
			if (!empty && isObjArray(obj)) {
				empty = isEmptyContent((Object[]) obj);
			}
			return empty;
		}
		if (obj instanceof Collection) {
			Collection<?> cObj = (Collection<?>) obj;
			return CollectionUtils.isEmpty(cObj) && isEmptyContent(cObj);
		}
		if (obj instanceof Map) {
			return MapUtils.isEmpty((Map<?, ?>) obj);
		}

		return false;
	}

	/**
	 * Checks if {@code obj} is empty considering when it is:
	 * <ul>
	 * <li>{@code Array} - is {@code null}, length is {@code 0} or all elements are {@code null}</li>
	 * <li>{@link Collection} - is {@code null}, length is {@code 0} or all elements are {@code null}</li>
	 * <li>{@link Object} - is {@code null}</li>
	 * </ul>
	 *
	 * @param obj
	 *            object to check
	 * @return {@code true} if {@code obj} is {@code null} or all array/collection elements are {@code null},
	 *         {@code false} - otherwise
	 */
	public static boolean isNullValue(Object obj) {
		if (obj != null) {
			Object[] va = Utils.makeArray(obj);

			for (Object ve : va) {
				if (ve != null) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Checks if {@code obj} is empty considering when it is:
	 * <ul>
	 * <li>{@link String} - is {@code null} or equal to {@code ""}</li>
	 * <li>{@code Array} - is {@code null} or length is {@code 0}</li>
	 * <li>{@link Collection} - is {@code null} or length is {@code 0}</li>
	 * <li>{@link Map} - is {@code null} or length is {@code 0}</li>
	 * <li>{@link Iterable} - is {@code null} or has no any accessible element</li>
	 * <li>{@link Object} - is {@code null}</li>
	 * </ul>
	 *
	 * @param obj
	 *            object to check
	 * @return {@code true} if {@code obj} is empty, {@code false} - otherwise
	 */
	public static boolean isEmpty(Object obj) {
		if (obj == null) {
			return true;
		}
		if (obj instanceof String) {
			return StringUtils.isEmpty((String) obj);
		}
		if (isArray(obj)) {
			return ArrayUtils.getLength(obj) == 0;
		}
		if (obj instanceof Collection) {
			return CollectionUtils.isEmpty((Collection<?>) obj);
		}
		if (obj instanceof Map) {
			return MapUtils.isEmpty((Map<?, ?>) obj);
		}
		if (obj instanceof Iterable) {
			return IterableUtils.isEmpty((Iterable<?>) obj);
		}

		return false;
	}

	/**
	 * Resolves {@link java.io.InputStream} {@code is} referenced file name path.
	 * <p>
	 * Supported {@link java.io.InputStream} types to resolve file path:
	 * <ul>
	 * <li>{@link java.io.FileInputStream}</li>
	 * <li>{@link org.apache.commons.io.input.ReaderInputStream}</li>
	 * <li>{@link java.io.FilterInputStream}</li>
	 * </ul>
	 *
	 * @param is
	 *            input stream instance to resolve file path
	 * @return the path of the {@code is} referenced file
	 */
	public static String resolveInputFilePath(InputStream is) {
		try {
			if (is instanceof FileInputStream) {
				return (String) FieldUtils.readDeclaredField(is, "path", true); // NON-NLS
			} else if (is instanceof ReaderInputStream) {
				Reader reader = (Reader) FieldUtils.readDeclaredField(is, "reader", true); // NON-NLS

				return resolveReaderFilePath(reader);
			} else if (is instanceof FilterInputStream) {
				InputStream in = (InputStream) FieldUtils.readDeclaredField(is, "in", true); // NON-NLS

				if (is != in) {
					return resolveInputFilePath(in);
				}
			} else if (is.getClass().getPackage().getName().equals("sun.nio.ch")) { // NON-NLS
				// ChannelInputStream resolution
				ReadableByteChannel rbc = (ReadableByteChannel) FieldUtils.readDeclaredField(is, "ch", true); // NON-NLS

				return resolveInputFilePath(rbc);
			}
		} catch (Exception exc) {
		}

		return null;
	}

	private static String resolveInputFilePath(ReadableByteChannel rbc) {
		// FileChannelImpl resolution
		try {
			String path = (String) FieldUtils.readDeclaredField(rbc, "path", true); // NON-NLS
			if (StringUtils.isNotEmpty(path)) {
				return path;
			}
		} catch (Exception exc) {
		}

		// FileChannelImpl resolution
		try {
			Object parent = FieldUtils.readDeclaredField(rbc, "parent", true); // NON-NLS

			if (parent instanceof InputStream) {
				return resolveInputFilePath((InputStream) parent);
			} else if (parent instanceof Reader) {
				return resolveReaderFilePath((Reader) parent);
			}
		} catch (Exception exc) {
		}

		return null;
	}

	/**
	 * Resolves {@link java.io.Reader} {@code rdr} referenced file name path.
	 * <p>
	 * Reader referenced file path can be resolved only if reader lock object is instance of {@link java.io.InputStream}
	 * and falls under {@link #resolveInputFilePath(java.io.InputStream)} conditions.
	 *
	 * @param rdr
	 *            reader instance to resolve file path
	 * @return the path of the {@code rdr} referenced file
	 *
	 * @see #resolveInputFilePath(java.io.InputStream)
	 */
	public static String resolveReaderFilePath(Reader rdr) {
		try {
			Object lock = FieldUtils.readField(rdr, "lock", true);

			if (lock instanceof InputStream) {
				return resolveInputFilePath((InputStream) lock);
			} else if (lock instanceof Reader && lock != rdr) {
				return resolveReaderFilePath((Reader) lock);
			}
		} catch (Exception exc) {
		}

		return null;
	}

	/**
	 * Resolves Java object (POJO) instance {@code dataObj} field or non-arg method value, defined by field/method names
	 * {@code path} array.
	 * <p>
	 * If {@code path} level resolved value is primitive type ({@link Class#isPrimitive()}), then value resolution
	 * terminates at that level.
	 * <p>
	 * Value resolution also terminates if path element index {@code i} is {@code i >= path.length}.
	 *
	 * @param path
	 *            strings array as path of objects fields and non-arg methods names
	 * @param dataObj
	 *            POJO instance to resolve value
	 * @param i
	 *            processed locator path element index
	 * @return resolved POJO field/method value, or {@code null} if value is not resolved
	 * @throws java.lang.RuntimeException
	 *             if field/method can't be found or accessed
	 *
	 * @see #getNodePath(String, String)
	 * @see Class#isPrimitive()
	 * @see Field#get(Object)
	 * @see Method#invoke(Object, Object...)
	 */
	public static Object getFieldValue(String[] path, Object dataObj, int i) throws RuntimeException {
		if (ArrayUtils.isEmpty(path) || dataObj == null) {
			return null;
		}

		if (i >= path.length || dataObj.getClass().isPrimitive()) {
			return dataObj;
		}

		try {
			Object fVal = null;
			IllegalArgumentException fExc = null;
			try {
				fVal = FieldUtils.readDeclaredField(dataObj, path[i], true);
			} catch (IllegalArgumentException nfe) {
				fExc = nfe;
			}

			NoSuchMethodException mExc = null;
			if (fVal == null) {
				try {
					Method m = dataObj.getClass().getDeclaredMethod(path[i]);
					m.setAccessible(true);
					fVal = m.invoke(dataObj);
				} catch (NoSuchMethodException nme) {
					mExc = nme;
				}
			}

			if (fExc != null && mExc != null) {
				throw fExc;
			}

			return getFieldValue(path, fVal, i + 1);
		} catch (Exception exc) {
			throw new RuntimeException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"Utils.could.not.get.declared.field", path[i], dataObj.getClass().getSimpleName(),
					toString(dataObj)), exc);
		}
	}

	/**
	 * Converts provided object type {@code value} to a Boolean.
	 * <p>
	 * {@code value} will be converted to real boolean value only if:
	 * <ul>
	 * <li>value is {@link Boolean} object</li>
	 * <li>value is {@link String}, where {@code 'true'}, {@code 'on'}, {@code 'y'}, {@code 't'} or {@code 'yes'}
	 * (case-insensitive) will return {@code true}</li>
	 * </ul>
	 * In all other cases {@code null} is returned.
	 *
	 * @param value
	 *            object value to convert to a Boolean
	 * @return boolean value resolved from provided {@code value}, or {@code null} if {@code value} does not represent
	 *         boolean
	 *
	 * @see org.apache.commons.lang3.BooleanUtils#toBoolean(int)
	 * @see org.apache.commons.lang3.BooleanUtils#toBoolean(String)
	 */
	public static Boolean getBoolean(Object value) {
		if (value != null) {
			if (value instanceof Boolean) {
				return (Boolean) value;
			}
			String vStr = toString(value);

			return BooleanUtils.toBoolean(vStr);
		}

		return null;
	}

	/**
	 * Log a given resource bundle message with a specified severity and {@link java.lang.Throwable} details.
	 * <p>
	 * If {@code logger} log level is set to {@link com.jkoolcloud.tnt4j.core.OpLevel#TRACE}, then full exception stack
	 * trace is logged. In other cases only exception messages are logged.
	 *
	 * @param logger
	 *            event sink to be used for logging
	 * @param sev
	 *            message severity to log
	 * @param rb
	 *            resource bundle for messages
	 * @param key
	 *            into resource bundle
	 * @param args
	 *            arguments passed along the message
	 *
	 * @see #getThrowable(Object[])
	 * @see #getExceptionMessages(Throwable)
	 * @see com.jkoolcloud.tnt4j.sink.EventSink#isSet(com.jkoolcloud.tnt4j.core.OpLevel)
	 * @see com.jkoolcloud.tnt4j.sink.EventSink#log(com.jkoolcloud.tnt4j.core.OpLevel, java.util.ResourceBundle, String,
	 *      Object...)
	 */
	public static void logThrowable(EventSink logger, OpLevel sev, ResourceBundle rb, String key, Object... args) {
		Throwable ex = getThrowable(args);
		if (ex != null) {
			args[args.length - 1] = decorateThrowable(ex, logger);
		}
		logger.log(sev, rb, key, args);
	}

	/**
	 * Log a given string message with a specified severity and {@link java.lang.Throwable} details.
	 * <p>
	 * If {@code logger} log level is set to {@link com.jkoolcloud.tnt4j.core.OpLevel#TRACE}, then full exception stack
	 * trace is logged. In other cases only exception messages are logged.
	 *
	 * @param logger
	 *            event sink to be used for logging
	 * @param sev
	 *            message severity to log
	 * @param msg
	 *            string message to be logged
	 * @param args
	 *            arguments passed along the message
	 *
	 * @see #getThrowable(Object[])
	 * @see #getExceptionMessages(Throwable)
	 * @see com.jkoolcloud.tnt4j.sink.EventSink#isSet(com.jkoolcloud.tnt4j.core.OpLevel)
	 * @see com.jkoolcloud.tnt4j.sink.EventSink#log(com.jkoolcloud.tnt4j.core.OpLevel, String, Object...)
	 */
	public static void logThrowable(EventSink logger, OpLevel sev, String msg, Object... args) {
		Throwable ex = getThrowable(args);
		if (ex != null) {
			args[args.length - 1] = decorateThrowable(ex, logger);
		}
		logger.log(sev, msg, args);
	}

	private static Object decorateThrowable(Throwable t, EventSink logger) {
		if (STACK_TRACE_ENTRIES_TO_LOG >= 0) {
			return getTruncatedException(t, STACK_TRACE_ENTRIES_TO_LOG);
		}
		if (logger.isSet(OpLevel.DEBUG)) {
			return getTruncatedException(t, 16);
		}
		if (logger.isSet(OpLevel.TRACE)) {
			return t;
		}
		if (logger.isSet(OpLevel.NONE)) {
			return getTruncatedException(t, 0);
		}

		return getTruncatedException(t, 4);
	}

	private static Object getTruncatedException(Throwable t, int depth) {
		if (depth == 0) {
			return getExceptionMessages(t);
		}

		if (depth > 0) {
			List<Throwable> tList = ExceptionUtils.getThrowableList(t);
			StringBuilder sb = new StringBuilder();

			for (int ti = 0; ti < tList.size(); ti++) {
				Throwable it = tList.get(ti);

				if (ti == 0) {
					sb.append(it.toString()).append("\n"); // NON-NLS
				} else {
					sb.append("Caused by: ").append(it.toString()).append("\n"); // NON-NLS
				}

				StackTraceElement[] stackTraces = it.getStackTrace();
				int sLength = Math.min(depth, stackTraces.length);
				for (int si = 0; si < sLength; si++) {
					sb.append("\tat ").append(stackTraces[si]).append("\n"); // NON-NLS
				}
			}

			return sb.toString();
		}

		return t;
	}

	/**
	 * Converts a provided string value {@code str} to a boolean value.
	 * 
	 * <ul>
	 * <li>{@code 'true'}, {@code 'on'}, {@code 'yes'}, {@code 't'}, {@code 'y'} (case insensitive) will return
	 * {@code true}</li>
	 * <li>{@code 'false'}, {@code 'off'}, {@code 'no'}, {@code 'f'}, {@code 'n'} (case insensitive) will return
	 * {@code false}</li>
	 * <li>Otherwise, exception is thrown</li>
	 * </ul>
	 *
	 * @param str
	 *            string value to check
	 * @return boolean value resolved from provided string
	 * @throws IllegalArgumentException
	 *             if string value does not resolve to any known boolean value
	 *
	 * @see org.apache.commons.lang3.BooleanUtils#toBooleanObject(String)
	 */
	public static boolean toBoolean(String str) throws IllegalArgumentException {
		Boolean bValue = BooleanUtils.toBooleanObject(str);

		if (bValue == null) {
			throw new IllegalArgumentException(StreamsResources
					.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME, "Utils.illegal.boolean.value", str));
		}

		return bValue;
	}

	/**
	 * Checks if enumeration value {@code eValue} is one of {@code optValues}.
	 *
	 * @param eValue
	 *            enumeration value
	 * @param optValues
	 *            array of enumeration values to match
	 * @param <E>
	 *            enumeration class type
	 * @return {@code true} if {@code eValue} matches one of {@code optValues} element or both are {@code null},
	 *         {@code false} - otherwise
	 */
	@SafeVarargs
	public static <E extends Enum<E>> boolean isOneOf(Enum<E> eValue, Enum<E>... optValues) {
		if (optValues == null) {
			return eValue == null;
		}

		for (Enum<E> oValue : optValues) {
			if (oValue == eValue) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Returns name of named object instance or class name if object name is not defined.
	 *
	 * @param nObject
	 *            named object instance
	 * @return name of named object instance or class name if object name is not defined , {@code null} if
	 *         {@code nObject} is {@code null}
	 */
	public static String getName(NamedObject nObject) {
		if (nObject == null) {
			return null;
		}

		String name = nObject.getName();

		if (StringUtils.isEmpty(name)) {
			name = nObject.getClass().getName();
		}

		return name;
	}

	private static final char UNIX_PATH_SEPARATOR = '/';
	private static final char WIN_PATH_SEPARATOR = '\\';

	/**
	 * Resolves file name from provided file path string.
	 * 
	 * @param path
	 *            file path
	 * @return file name resolved from provided path, or {@code null} if path is {@code null}
	 */
	public static String getFileName(String path) {
		if (StringUtils.isEmpty(path)) {
			return path;
		}

		return path.substring(path.lastIndexOf(UNIX_PATH_SEPARATOR) + 1)
				.substring(path.lastIndexOf(WIN_PATH_SEPARATOR) + 1);
	}

	private static final String OBJ_NAME_TOKEN_DELIMITERS = "@#$"; // NON-NLS

	/**
	 * Resolves desired object name from provided fully qualified object name.
	 * <p>
	 * Function arguments sequence:
	 * <ul>
	 * <li>1 - resolution options: DEFAULT, BEFORE, AFTER, REPLACE, SECTION, FULL. Optional.</li>
	 * <li>2 - search symbols. Optional.</li>
	 * <li>3 - replacement symbols. Optional</li>
	 * </ul>
	 *
	 * @param objectName
	 *            fully qualified object name
	 * @param args
	 *            function arguments list
	 * @return object name resolved form provided fully qualified object name, or {@code null} if fully qualified object
	 *         name is {@code null}
	 */
	public static String resolveObjectName(String objectName, String... args) {
		if (StringUtils.isEmpty(objectName)) {
			return objectName;
		}

		String option = args == null || args.length < 1 ? null : args[0];
		ObjNameOptions opt;

		try {
			opt = StringUtils.isEmpty(option) ? ObjNameOptions.DEFAULT : ObjNameOptions.valueOf(option.toUpperCase());
		} catch (IllegalArgumentException exc) {
			opt = ObjNameOptions.DEFAULT;
		}

		switch (opt) {
		case FULL:
			break;
		case BEFORE:
			String sSymbol = args == null || args.length < 2 ? null : args[1];
			if (StringUtils.isNotEmpty(sSymbol)) {
				objectName = StringUtils.substringBefore(objectName, sSymbol);
			}
			break;
		case AFTER:
			sSymbol = args == null || args.length < 2 ? null : args[1];
			if (StringUtils.isNotEmpty(sSymbol)) {
				objectName = StringUtils.substringAfter(objectName, sSymbol);
			}
			break;
		case REPLACE:
			sSymbol = args == null || args.length < 2 ? null : args[1];
			if (StringUtils.isNotEmpty(sSymbol)) {
				String rSymbol = args == null || args.length < 3 ? args[2] : null;
				objectName = StringUtils.replaceChars(objectName, sSymbol, rSymbol == null ? "" : rSymbol);
			}
			break;
		case SECTION:
			String idxStr = args == null || args.length < 2 ? null : args[1];
			int idx;
			try {
				idx = Integer.parseInt(idxStr);
			} catch (Exception exc) {
				idx = -1;
			}

			if (idx >= 0) {
				sSymbol = args == null || args.length < 3 ? args[2] : null;
				String[] onTokens = StringUtils.split(objectName,
						StringUtils.isEmpty(sSymbol) ? OBJ_NAME_TOKEN_DELIMITERS : sSymbol);
				objectName = idx < ArrayUtils.getLength(onTokens) ? onTokens[idx] : objectName;
			}
			break;
		case DEFAULT:
		default:
			idx = StringUtils.indexOfAny(objectName, OBJ_NAME_TOKEN_DELIMITERS);
			if (idx > 0) {
				objectName = StringUtils.substring(objectName, 0, idx);
			}
			break;
		}

		return objectName;
	}

	private enum ObjNameOptions {
		DEFAULT, BEFORE, AFTER, REPLACE, SECTION, FULL
	}

	/**
	 * Tries to guess charset for file provided bytes.
	 * 
	 * @param file
	 *            file to guess charset
	 * @return charset found to match the file
	 * 
	 * @throws IOException
	 *             if fails to read the file
	 */
	public static Charset guessCharset(File file) throws IOException {
		CharsetToolkit toolkit = new CharsetToolkit(file);

		return toolkit.getCharset();
	}

	/**
	 * Returns a {@link java.io.LineNumberReader} for a provided file. File content charset is discovered using some set
	 * of first bytes from file.
	 * 
	 * @param file
	 *            file to get reader for
	 * @return instance of file reader using the discovered file charset
	 * 
	 * @throws IOException
	 *             if fails to read the file
	 */
	public static LineNumberReader getFileReader(File file) throws IOException {
		CharsetToolkit toolkit = new CharsetToolkit(file);

		return (LineNumberReader) toolkit.getReader();
	}

	/**
	 * Returns the last provided {@code array} element.
	 * 
	 * @param array
	 *            the array instance to get last element
	 * @param <T>
	 *            type of array elements
	 * @return the last array element
	 */
	public static <T> T lastOf(T[] array) {
		return array[array.length - 1];
	}

	/**
	 * Copies the specified ending range of the specified array into a new array.
	 * 
	 * @param array
	 *            the array from which an ending range is to be copied
	 * @param startIdx
	 *            the initial index of the range to be copied, inclusive
	 * @param <T>
	 *            the class of the objects in the array
	 * @return a new array containing the specified range from the end of original array, truncated to obtain the
	 *         required length
	 * 
	 * @see #subArray(Object[], int, int)
	 */
	public static <T> T[] endArray(T[] array, int startIdx) {
		return subArray(array, startIdx, array.length);
	}

	/**
	 * Copies the specified beginning range of the specified array into a new array.
	 * 
	 * @param array
	 *            the array from which a beginning range is to be copied
	 * @param endIdx
	 *            the final index of the range to be copied, exclusive (this index may lie outside the array)
	 * @param <T>
	 *            the class of the objects in the array
	 * @return a new array containing the specified range from the front of original array, truncated or padded with
	 *         nulls to obtain the required length
	 * 
	 * @see #subArray(Object[], int, int)
	 */
	public static <T> T[] frontArray(T[] array, int endIdx) {
		return subArray(array, 0, endIdx);
	}

	/**
	 * Copies the specified range of the specified array into a new array.
	 * 
	 * @param array
	 *            the array from which a range is to be copied
	 * @param startIdx
	 *            the initial index of the range to be copied, inclusive
	 * @param endIdx
	 *            the final index of the range to be copied, exclusive (this index may lie outside the array)
	 * @param <T>
	 *            the class of the objects in the array
	 * @return a new array containing the specified range from the original array, truncated or padded with nulls to
	 *         obtain the required length
	 *
	 * @throws ArrayIndexOutOfBoundsException
	 *             if {@code from < 0} or {@code from > original.length}
	 * @throws IllegalArgumentException
	 *             if {@code from > to}
	 * @throws NullPointerException
	 *             if {@code original} is null
	 */
	public static <T> T[] subArray(T[] array, int startIdx, int endIdx) {
		return Arrays.copyOfRange(array, startIdx, endIdx);
		// return ArrayUtils.subarray (array, startIdx, endIdx);
	}

	/**
	 * Fills in provided property value with JVM system properties and environment variables if provided property value
	 * has any variable expressions.
	 * <p>
	 * To fill JVM system property value - use expression {@code "${sys:propName}"}, e.g.:
	 * {@code "${sys:catalina.base}"}.
	 * <p>
	 * To fill JVM environment variable value - use expression {@code "${env:variableName}"}, e.g.:
	 * {@code "${env:CATALINA_HOME}"}.
	 * 
	 * @param propertyValue
	 *            property value to fill in
	 * @return system properties and environment variables filed in property value
	 * 
	 * @see #isVariableExpression(String)
	 * @see #resolveExpressionVariables(java.util.Collection, String)
	 */
	public static String getSystemVarsFilledInProperty(String propertyValue) {
		if (isVariableExpression(propertyValue)) {
			Collection<String> exprVars = new ArrayList<>();
			resolveExpressionVariables(exprVars, propertyValue);

			String fValue = propertyValue;
			if (CollectionUtils.isNotEmpty(exprVars)) {
				for (String eVar : exprVars) {
					String varName = Utils.getVarName(eVar);
					String val = null;
					if (varName.startsWith(StreamsConstants.VAR_SCOPE_SYSTEM)) {
						val = System.getProperty(varName.substring(StreamsConstants.VAR_SCOPE_SYSTEM.length()));
					} else if (varName.startsWith(StreamsConstants.VAR_SCOPE_ENVIRONMENT)) {
						val = System.getenv(varName.substring(StreamsConstants.VAR_SCOPE_ENVIRONMENT.length()));
					}
					if (val != null) {
						fValue = fValue.replace(eVar, val);
					}
				}
			}

			return fValue;
		}

		return propertyValue;
	}

	/**
	 * Converts var-arg array to conventional object array.
	 * 
	 * @param args
	 *            var-arg array to convert
	 * @return conventional arguments array
	 */
	public static Object[] args(Object... args) {
		return args;
	}
}
