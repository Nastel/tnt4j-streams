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

package com.jkoolcloud.tnt4j.streams.custom.parsers;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.parsers.AbstractActivityMapParser;
import com.jkoolcloud.tnt4j.streams.utils.CharBufferParser;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Implements an activity data parser that assumes each activity data item is an IBM MQ error log entry {@link String}.
 * Parser resolved log entry string fields are put into {@link Map} afterwards mapped into activity fields and
 * properties according to defined parser configuration.
 * <p>
 * This parser resolved data map may contain such entries:
 * <ul>
 * <li>Date - resolved log entry date string</li>
 * <li>Time - resolved log entry time string</li>
 * <li>Process - resolved log entry process identifier</li>
 * <li>User - resolved log entry user name</li>
 * <li>Program - resolved log entry program (application) name</li>
 * <li>Host - resolved log entry host name IBM MQ is running on</li>
 * <li>Installation - resolved log entry IBM MQ installation name</li>
 * <li>VRMF - resolved log entry running IBM MQ version descriptor</li>
 * <li>QMgr - resolved log entry Queue manager error occurred on</li>
 * <li>ErrCode - resolved log entry IBM MQ error code string</li>
 * <li>ErrText - resolved log entry IBM MQ error message text</li>
 * <li>Explanation - resolved log entry IBM MQ error explanation message text</li>
 * <li>Action - resolved log entry IBM MQ error fix action message text</li>
 * <li>Where - resolved log entry error descriptor location string containing source code file name and line number</li>
 * <li>pid - derived entry for user convenience made by taking part before {@code '.'} from resolved {@code 'Process'}
 * entry value</li>
 * <li>tid - derived entry for user convenience made by taking part after {@code '.'} from resolved {@code 'Process'}
 * entry value</li>
 * </ul>
 * <p>
 * IBM MQ versions starting {@code 9.1} adds these additional entries:
 * <ul>
 * <li>Severity - resolved log entry severity: {@code 'I' - INFO}, {@code 'W' - WARNING}, {@code 'E' - ERROR},
 * {@code 'S' - CRITICAL}, {@code 'T' - HALT}</li>
 * <li>TimeUTC - resolved log entry UTC time</li>
 * <li>CommentInsert(X) - resolved log entry text attribute values have been insert into log entry message, where (X) is
 * attribute index</li>
 * <li>ArithInsert(X) - resolved log entry numeric attribute values have been insert into log entry message, where (X)
 * is attribute index</li>
 * </ul>
 * <p>
 * This activity parser supports configuration properties from {@link AbstractActivityMapParser} (and higher hierarchy
 * parsers).
 *
 * @version $Revision: 2 $
 */
public class IBMMQLogParser extends AbstractActivityMapParser {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(IBMMQLogParser.class);

	/**
	 * Constant defining IBM MQ error log entries delimiter.
	 */
	public static final String ENTRIES_DELIM = "-----"; // NON-NLS

	private final IBMMQErrParser errEntryParser;

	/**
	 * Constructs a new IBMMQLogParser.
	 */
	public IBMMQLogParser() {
		errEntryParser = new IBMMQErrParser();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		// // no any additional properties are required yet.
		// if (false) {
		// logger().log(OpLevel.DEBUG,
		// StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.setting"),
		// name, value);
		// }
	}

	@Override
	public Object getProperty(String name) {
		return super.getProperty(name);
	}

	@Override
	public boolean canHaveDelimitedLocators() {
		return false;
	}

	/**
	 * Returns whether this parser supports the given format of the activity data. This is used by activity streams to
	 * determine if the parser can parse the data in the format that the stream has it.
	 * <p>
	 * This parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link java.lang.String}</li>
	 * <li>{@code byte[]}</li>
	 * <li>{@link java.nio.ByteBuffer}</li>
	 * <li>{@link java.io.Reader}</li>
	 * <li>{@link java.io.InputStream}</li>
	 * </ul>
	 *
	 * @param data
	 *            data object whose class is to be verified
	 * @return {@code true} if this parser can process data in the specified format, {@code false} - otherwise
	 */
	@Override
	protected boolean isDataClassSupportedByParser(Object data) {
		return isDataClassSupportedByDefault(data);
	}

	@Override
	protected Map<String, Object> getDataMap(Object data) {
		if (data == null) {
			return null;
		}

		String logEntry = getNextActivityString(data);
		if (StringUtils.isEmpty(logEntry)) {
			return null;
		}
		Map<String, Object> dataMap = new HashMap<>();
		dataMap.put(RAW_ACTIVITY_STRING_KEY, logEntry);

		try {
			synchronized (errEntryParser) {
				dataMap.putAll(errEntryParser.parse(logEntry));
			}
		} catch (Exception exc) {
			Utils.logThrowable(logger(), OpLevel.ERROR,
					StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"IBMMQLogParser.log.entry.parse.failed", exc);
		}

		return dataMap;
	}

	/**
	 * Reads RAW IBM MQ error log entry string from {@link BufferedReader}. If the data input source contains multiple
	 * error log entries, then each document must ends with {@value ENTRIES_DELIM}, and be separated by a new line.
	 * 
	 * @param rdr
	 *            reader to use for reading
	 * @return non empty RAW IBM MQ error log entry string, or {@code null} if the end of the stream has been reached
	 */
	@Override
	protected String readNextActivity(BufferedReader rdr) {
		String entryString = null;
		StringBuilder entryBuffer = new StringBuilder(1024);

		nextLock.lock();
		try {
			try {
				for (String line; entryString == null && (line = rdr.readLine()) != null;) {
					entryBuffer.append(line);
					if (line.endsWith(ENTRIES_DELIM)) {
						if (entryBuffer.length() > 0) {
							entryString = entryBuffer.toString();
							entryBuffer.setLength(0);
						}
					}
				}
			} catch (EOFException eof) {
				Utils.logThrowable(logger(), OpLevel.DEBUG,
						StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME), "ActivityParser.data.end",
						getActivityDataType()[0], eof);
			} catch (IOException ioe) {
				Utils.logThrowable(logger(), OpLevel.WARNING,
						StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityParser.error.reading", getActivityDataType()[0], ioe);
			}
		} finally {
			nextLock.unlock();
		}

		if (entryString == null && entryBuffer.length() > 0) {
			entryString = entryBuffer.toString();
		}

		return entryString;
	}

	private static final String[] ACTIVITY_DATA_TYPES = { "IBM MQ ERR LOG", "TEXT" }; // NON-NLS

	/**
	 * Returns types of RAW activity data entries.
	 *
	 * @return types of RAW activity data entries - {@code "IBM MQ ERR LOG"} and {@code "TEXT"}
	 */
	@Override
	protected String[] getActivityDataType() {
		return ACTIVITY_DATA_TYPES;
	}

	/**
	 * IBM MQ error log entries parser.
	 */
	private static class IBMMQErrParser extends CharBufferParser<String, Map<String, ?>> {
		private static final char CB = ')';
		private static final char COLON = ':';
		private static final char MINUS = '-';

		/**
		 * Construct a new IBMMQErrParser.
		 */
		public IBMMQErrParser() {
			super();
		}

		/**
		 * Parse IBM MQ error log entry string making map of parsed fields.
		 * 
		 * @param logEntry
		 *            IBM MQ error log entry
		 * @return a map, or {@code null} if line is empty.
		 * @throws Exception
		 *             if the underlying stream fails, or unexpected chars occurs.
		 */
		@Override
		public Map<String, Object> parse(String logEntry) throws Exception {
			CharBuffer cb = stringToBuffer(logEntry);

			String date = readWord(cb, 9);
			skipWhitespaces(cb);
			String time = readWord(cb, 8);
			readChars(cb, 3);
			String process = readBetween(cb, "Process(", CB); // NON-NLS
			String user = readBetween(cb, "User(", CB); // NON-NLS
			String program = readBetween(cb, "Program(", CB); // NON-NLS
			String host = readBetween(cb, "Host(", CB); // NON-NLS
			String install = readBetween(cb, "Installation(", CB); // NON-NLS
			String vrmf = readBetween(cb, "VRMF(", CB); // NON-NLS
			String qMgr = readBetween(cb, "QMgr(", CB, true); // NON-NLS

			Map<String, String> optMap = resolveOptionals(cb);

			String errCode = readUntil(cb, COLON);
			skipWhitespaces(cb);
			String errText = readUntil(cb, "EXPLANATION:"); // NON-NLS
			skipWhitespaces(cb);
			String explanation = readUntil(cb, "ACTION:"); // NON-NLS
			skipWhitespaces(cb);
			String action = readUntil(cb, ENTRIES_DELIM);

			String where = null;
			int c = peek(cb);
			if (c == SPACE) {
				where = readUntil(cb, MINUS);
			}

			return createFieldMap(date, time, process, user, program, host, install, vrmf, qMgr, errCode, errText,
					explanation, action, where, optMap);
		}

		private static Map<String, Object> createFieldMap(String date, String time, String process, String user,
				String program, String host, String install, String vrmf, String qMgr, String errCode, String errText,
				String exp, String action, String where, Map<String, String> optionals) {
			Map<String, Object> map = new HashMap<>(14);

			map.put("Date", date); // NON-NLS
			map.put("Time", time); // NON-NLS
			map.put("Process", process); // NON-NLS
			map.put("User", user); // NON-NLS
			map.put("Program", program); // NON-NLS
			map.put("Host", host); // NON-NLS
			map.put("Installation", install); // NON-NLS
			map.put("VRMF", vrmf); // NON-NLS
			map.put("QMgr", qMgr); // NON-NLS

			// retrieve 'Severity' value from 'ErrCode' and adjust 'ErrCode' value accordingly
			char severityChar = StringUtils.isEmpty(errCode) ? 0 : errCode.charAt(errCode.length() - 1);
			String severity;

			switch (severityChar) {
			case 'I':
				severity = OpLevel.INFO.name();
				errCode = errCode.substring(0, errCode.length() - 1);
				break;
			case 'W':
				severity = OpLevel.WARNING.name();
				errCode = errCode.substring(0, errCode.length() - 1);
				break;
			case 'E':
				severity = OpLevel.ERROR.name();
				errCode = errCode.substring(0, errCode.length() - 1);
				break;
			case 'S':
				severity = OpLevel.CRITICAL.name();
				errCode = errCode.substring(0, errCode.length() - 1);
				break;
			case 'T':
				severity = OpLevel.HALT.name();
				errCode = errCode.substring(0, errCode.length() - 1);
				break;
			case 0:
			default:
				severity = null;
				break;
			}

			map.put("ErrCode", errCode); // NON-NLS
			if (severity != null) {
				map.put("Severity", severity); // NON-NLS
			}
			map.put("ErrText", StringUtils.trim(errText)); // NON-NLS
			map.put("Explanation", StringUtils.trim(exp)); // NON-NLS
			map.put("Action", StringUtils.trim(action)); // NON-NLS
			map.put("Where", StringUtils.trim(where)); // NON-NLS

			// splitting 'Process' to pid and tid for user convenience
			String[] pt = process.split("\\."); // NON-NLS
			map.put("pid", pt.length > 0 ? pt[0] : null); // NON-NLS
			map.put("tid", pt.length > 1 ? pt[1] : null); // NON-NLS

			// map already has field 'Time' so rename optionals map field 'Time' to 'TimeUTC'
			String utcTime = optionals.remove("Time");
			if (utcTime != null) {
				optionals.put("TimeUTC", utcTime);
			}

			map.putAll(optionals);

			return map;
		}

		private static String readBetween(CharBuffer cb, String pre, char term) throws IOException {
			expect(cb, pre);
			String str = readUntil(cb, term);
			skipWhitespaces(cb);

			return str;
		}

		private static String readBetween(CharBuffer cb, String pre, char term, boolean optional) throws IOException {
			int pos = cb.position();
			try {
				expect(cb, pre);
			} catch (IOException exc) {
				if (optional) {
					cb.position(pos);
					return null;
				} else {
					throw exc;
				}
			}
			String str = readUntil(cb, term);
			skipWhitespaces(cb);

			return str;
		}

		private static String readUntil(CharBuffer cb, char tc) {
			StringBuilder sb = new StringBuilder(16);
			int c;

			while ((c = read(cb)) != tc && c != EOF) {
				sb.append((char) c);
			}

			return sb.toString();
		}

		private static Map<String, String> resolveOptionals(CharBuffer cb) {
			Map<String, String> optMap = new HashMap<>();
			Pattern optPattern = Pattern.compile("(?<key>\\w+)\\((?<value>.+)\\)");// NON-NLS
			Matcher optMatcher;

			String line;
			String key;
			String value;
			boolean matches;
			int mp;
			do {
				mp = cb.position();
				line = readLine(cb, 15);
				optMatcher = optPattern.matcher(line.trim());
				matches = optMatcher.matches();
				if (matches) {
					key = optMatcher.group("key");
					value = optMatcher.group("value");

					optMap.put(key, value);
				}
			} while (matches);

			cb.position(mp);

			return optMap;
		}
	}
}
