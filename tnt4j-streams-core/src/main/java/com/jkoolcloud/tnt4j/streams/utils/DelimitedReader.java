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

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Reads delimited string chunks from provided {@link BufferedReader} instance.
 * <p>
 * Applies delimiter handling rules defined by {@link DelimRule} enumeration.
 *
 * @version $Revision: 1 $
 */
public class DelimitedReader {

	private static final int BUFFER_SIZE = 1024;

	private final ReaderState rState;
	private int bufferSize = BUFFER_SIZE;

	/**
	 * Constructs a new DelimitedReader.
	 */
	public DelimitedReader() {
		rState = new ReaderState();
	}

	/**
	 * Constructs a new DelimitedReader.
	 *
	 * @param bufferSize
	 *            size of internal read buffer
	 */
	public DelimitedReader(int bufferSize) {
		this();

		this.bufferSize = bufferSize;
	}

	/**
	 * Reads delimited string chunk from provided reader instance {@code rdr}.
	 *
	 * @param rdr
	 *            reader to use for reading
	 * @param delimiter
	 *            string parts delimiter
	 * @param delimRule
	 *            delimiter handling rule
	 * @return string read from provided reader, or {@code null} if reader reached the end
	 *
	 * @throws IOException
	 *             if an I/O error occurs
	 *
	 * @see DelimRule
	 */
	public String readDelimited(BufferedReader rdr, String delimiter, DelimRule delimRule) throws IOException {
		StringBuilder result = new StringBuilder();
		char[] charBuffer = new char[bufferSize];
		int charsRead;

		while (true) {
			int delimiterIndex = rState.buffer.indexOf(delimiter);
			if (delimiterIndex >= 0) {
				switch (delimRule) {
				case BEGINNING:
					if (result.length() == 0) {
						result.append(delimiter);
						rState.buffer = rState.buffer.substring(delimiterIndex + delimiter.length());
						break;
					} else {
						result.append(rState.buffer, 0, delimiterIndex);
						rState.buffer = rState.buffer.substring(delimiterIndex);
						return result.toString();
					}
				case TERMINATING:
					result.append(rState.buffer, 0, delimiterIndex).append(delimiter);
					rState.buffer = rState.buffer.substring(delimiterIndex + delimiter.length());
					return result.toString();
				case DELIMITING:
				default:
					result.append(rState.buffer, 0, delimiterIndex);
					rState.buffer = rState.buffer.substring(delimiterIndex + delimiter.length());
					return result.toString();
				}
			}

			if (rState.buffer.length() > bufferSize * 2) {
				result.append(rState.buffer, 0, bufferSize);
				rState.buffer = rState.buffer.substring(bufferSize);
			}

			charsRead = rdr.read(charBuffer);
			if (charsRead == -1) {
				result.append(rState.buffer);
				rState.buffer = "";
				return result.length() > 0 ? result.toString() : null;
			}

			rState.buffer += new String(charBuffer, 0, charsRead);
		}
	}

	private static class ReaderState {
		private String buffer = "";
	}

	/**
	 * Defines activities delimiter handling rules.
	 */
	public enum DelimRule {
		/**
		 * Delimiter marks beginning of raw activity data. Delimiter itself gets included into raw activity data.
		 * Aliases - {@code "BEGIN", "STARTING", "START" "FROM"}
		 */
		BEGINNING,
		/**
		 * Delimiter marks end of RAW activity data. Delimiter itself gets included into raw activity data. Aliases -
		 * {@code "TERMINATE", "ENDING", "END", "STOP", "TO"}
		 */
		TERMINATING,
		/**
		 * Delimiter is ordinary data chunks separator. Delimiter itself gets ommited from raw activity data. Aliases -
		 * {@code "SPLIT", "DELIM", "DELIMIT", "SEPARATE", "DIVIDE", "DIV"}
		 */
		DELIMITING;

		/**
		 * Maps the specified {@code name} to a member of this enumeration.
		 * 
		 * @param name
		 *            enumeration member name to map
		 * @return enumeration member
		 */
		public static DelimRule valueOfAny(String name) {
			switch (name.toUpperCase()) {
			case "BEGINNING":
			case "BEGIN":
			case "STARTING":
			case "START":
			case "FROM":
				return BEGINNING;
			case "TERMINATING":
			case "TERMINATE":
			case "ENDING":
			case "END":
			case "STOP":
			case "TO":
			case "TRUE":
				return TERMINATING;
			case "FALSE":
			default:
				return DELIMITING;
			}
		}
	}
}
