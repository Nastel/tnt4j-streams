/*
 * Copyright 2014-2022 JKOOL, LLC.
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

import java.io.IOException;
import java.nio.CharBuffer;

/**
 * Base class for a {@link CharBuffer} based parser making possible to process characters in various ways: skip, peek,
 * expect, read, unread.
 * <p>
 * Input object types can be {@link String} or {@code char[]}.
 *
 * @param <I>
 *            type of parser input object
 * @param <O>
 *            type of parser produced object
 *
 * @version $Revision: 1 $
 */
public abstract class CharBufferParser<I, O> {
	/**
	 * Constant for SPACE character {@value}.
	 */
	protected static final char SPACE = ' ';
	/**
	 * Constant for NL character {@value}.
	 */
	protected static final char NL = '\n';
	/**
	 * Constant for RC character {@value}.
	 */
	protected static final char RC = '\r';
	/**
	 * Constant for ZERO character {@value}.
	 */
	protected static final char ZERO = '0';
	/**
	 * Constant for EOF character {@value}.
	 */
	protected static final int EOF = -1;

	/**
	 * Construct a new {@link CharBuffer} parser.
	 */
	public CharBufferParser() {
	}

	/**
	 * Wraps {@link String} into {@link CharBuffer}.
	 *
	 * @param str
	 *            string instance
	 * @return char buffer instance
	 */
	protected static CharBuffer stringToBuffer(String str) {
		CharBuffer cb = CharBuffer.wrap(str);
		cb.rewind();

		return cb;
	}

	/**
	 * Wraps {@code char[]} into {@link CharBuffer}.
	 *
	 * @param chars
	 *            character array instance
	 * @return char buffer instance
	 */
	protected static CharBuffer charsToBuffer(char[] chars) {
		CharBuffer cb = CharBuffer.wrap(chars);
		cb.rewind();

		return cb;
	}

	/**
	 * Parses provided characters data.
	 * 
	 * @param data
	 *            object to be wrapped into {@link CharBuffer} and parsed
	 * @return object produced from parsed characters data
	 * @throws Exception
	 *             if any char buffer parsing exception occurs
	 */
	public abstract O parse(I data) throws Exception;

	/**
	 * Read a char and assert the value.
	 *
	 * @param cb
	 *            char buffer containing text to read
	 * @param c
	 *            expected character
	 *
	 * @throws IOException
	 *             if expected and found characters differs
	 *
	 * @see #read(CharBuffer)
	 */
	protected static void expect(CharBuffer cb, int c) throws IOException {
		int pos = cb.position();
		int d = read(cb);

		if (d != c) {
			throw new IOException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"CharBufferParser.unexpected.char", pos, (char) c, (char) d));
		}
	}

	/**
	 * Read a char and assert the value. If read char is {@value #EOF} it is considered OK.
	 *
	 * @param cb
	 *            char buffer containing text to read
	 * @param c
	 *            expected character
	 *
	 * @throws IOException
	 *             if expected and found characters differs and found character is not {@value #EOF}
	 *
	 * @see #read(CharBuffer)
	 */
	protected static void expectOrEnd(CharBuffer cb, int c) throws IOException {
		int pos = cb.position();
		int d = read(cb);

		if (d != c && d != EOF) {
			throw new IOException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"CharBufferParser.unexpected.char", pos, (char) c, (char) d));
		}
	}

	/**
	 * Read a string and assert the value.
	 * 
	 * @param cb
	 *            char buffer containing text to read
	 * @param str
	 *            expected string
	 * @throws IOException
	 *             if expected and found strings differs
	 *
	 * @see #readChars(CharBuffer, int)
	 */
	protected static void expect(CharBuffer cb, String str) throws IOException {
		int pos = cb.position();
		String rs = readChars(cb, str.length());

		if (!str.equals(rs)) {
			throw new IOException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"CharBufferParser.unexpected.str", pos, str, rs));
		}
	}

	/**
	 * Read until a non space char ({@code ' '}) is found.
	 *
	 * @param cb
	 *            char buffer containing text to read
	 *
	 * @see #read(CharBuffer)
	 * @see #unread(CharBuffer)
	 */
	protected static void skipSpaces(CharBuffer cb) {
		while (read(cb) == SPACE) {
			continue;
		}
		unread(cb);
	}

	/**
	 * Read until a non-whitespace char is found.
	 *
	 * @param cb
	 *            char buffer containing text to read
	 *
	 * @see #read(CharBuffer)
	 * @see #unread(CharBuffer)
	 * @see java.lang.Character#isWhitespace(char)
	 */
	protected static void skipWhitespaces(CharBuffer cb) {
		while (Character.isWhitespace(read(cb))) {
			continue;
		}
		unread(cb);
	}

	/**
	 * Read the next char, but then unread it.
	 *
	 * @param cb
	 *            char buffer containing text to read
	 * @return an integer value
	 *
	 * @see #read(CharBuffer)
	 * @see #unread(CharBuffer)
	 */
	protected static int peek(CharBuffer cb) {
		int c = read(cb);
		unread(cb);
		return c;
	}

	/**
	 * Read the next char from buffer.
	 *
	 * @param cb
	 *            char buffer containing text to read
	 * @return next char, or {@link #EOF} if no more chars
	 *
	 * @see CharBuffer#mark()
	 * @see CharBuffer#get()
	 */
	protected static int read(CharBuffer cb) {
		cb.mark();
		try {
			return cb.get();
		} catch (RuntimeException exc) {
			return EOF;
		}
	}

	/**
	 * Push back character position to previous.
	 *
	 * @param cb
	 *            char buffer containing text to read
	 *
	 * @see CharBuffer#reset()
	 */
	protected static void unread(CharBuffer cb) {
		cb.reset();
	}

	/**
	 * Read a positive integer from buffer.
	 *
	 * @param cb
	 *            char buffer containing text to read
	 * @return an integer value
	 *
	 * @see #read(CharBuffer)
	 * @see #unread(CharBuffer, int)
	 * @see Character#isDigit(char)
	 */
	protected static int readInt(CharBuffer cb) {
		int c;
		int ret = 0;

		while (Character.isDigit(c = read(cb))) {
			ret = ret * 10 + (c - ZERO);
		}

		unread(cb, c);

		return ret;
	}

	/**
	 * Read fractional part of number - digits after a decimal point.
	 *
	 * @param cb
	 *            char buffer containing text to read
	 * @return a value in the range [0, 1)
	 *
	 * @see #read(CharBuffer)
	 * @see #unread(CharBuffer, int)
	 * @see Character#isDigit(char)
	 */
	protected static double readFractions(CharBuffer cb) {
		int c;
		int ret = 0;
		int order = 1;

		while (Character.isDigit(c = read(cb))) {
			ret = ret * 10 + (c - ZERO);
			order *= 10;
		}

		unread(cb, c);

		return (double) ret / order;
	}

	/**
	 * Read until a end of the word and discard read chars. Word is terminated by space char ({@code ' '}) or end of
	 * buffer.
	 *
	 * @param cb
	 *            char buffer containing text to read
	 *
	 * @see #read(CharBuffer)
	 * @see #unread(CharBuffer, int)
	 */
	protected static void skipWord(CharBuffer cb) {
		int c;

		do {
			c = read(cb);
		} while (c != SPACE && c != EOF);

		unread(cb, c);
	}

	/**
	 * Read a word into the given {@link StringBuilder}. Word is terminated by space char ({@code ' '}) or end of
	 * buffer.
	 *
	 * @param cb
	 *            char buffer containing text to read
	 * @param sb
	 *            string builder to fill
	 *
	 * @see #read(CharBuffer)
	 * @see #unread(CharBuffer, int)
	 * @see StringBuilder#append(char)
	 */
	protected static void readWord(CharBuffer cb, StringBuilder sb) {
		int c;

		while ((c = read(cb)) != SPACE && c != EOF) {
			sb.append((char) c);
		}

		unread(cb, c);
	}

	/**
	 * Read a word from buffer as a string. Word is terminated by pace char ({@code ' '}) or end of buffer.
	 *
	 * @param cb
	 *            char buffer containing text to read
	 * @param sizeHint
	 *            an guess on how large string will be, in chars
	 * @return a valid, but perhaps empty, word.
	 *
	 * @see #readWord(CharBuffer, StringBuilder)
	 */
	protected static String readWord(CharBuffer cb, int sizeHint) {
		StringBuilder sb = new StringBuilder(sizeHint);
		readWord(cb, sb);

		return sb.toString();
	}

	/**
	 * Read a defined number of chars or until end of buffer.
	 *
	 * @param cb
	 *            char buffer containing text to read
	 * @param count
	 *            number of chars to read
	 * @return string containing read characters
	 *
	 * @see #read(CharBuffer)
	 * @see #unread(CharBuffer)
	 * @see StringBuilder#append(char)
	 */
	protected static String readChars(CharBuffer cb, int count) {
		StringBuilder sb = new StringBuilder(count);
		int c;
		int i = 0;

		while ((c = read(cb)) != EOF && i < count) {
			sb.append((char) c);
			i++;
		}

		unread(cb);

		return sb.toString();
	}

	/**
	 * Read a line from buffer as a string. Line is terminated by new line char ('\n') or end of buffer.
	 *
	 * @param cb
	 *            char buffer containing text to read
	 * @param sizeHint
	 *            an guess on how large the line will be, in chars.
	 * @return read line string
	 *
	 * @see #read(CharBuffer)
	 * @see StringBuilder#append(char)
	 */
	protected static String readLine(CharBuffer cb, int sizeHint) {
		StringBuilder sb = new StringBuilder(sizeHint);
		int c;

		while ((c = read(cb)) != NL && c != EOF) {
			if (c != RC) {
				sb.append((char) c);
			}
		}

		return sb.toString();
	}

	/**
	 * Read char buffer until defined termination string is found or until end of buffer.
	 * 
	 * @param cb
	 *            char buffer containing text to read
	 * @param terminator
	 *            string indicating to stop reading
	 * @return string containing read characters
	 *
	 * @see #read(CharBuffer)
	 * @see #unread(CharBuffer, int)
	 * @see StringBuilder#append(char)
	 */
	protected static String readUntil(CharBuffer cb, String terminator) {
		boolean terminate = false;
		StringBuilder sb = new StringBuilder(64);
		int c;

		while ((c = read(cb)) != EOF && !terminate) {
			sb.append((char) c);

			terminate = sb.toString().endsWith(terminator);
		}

		unread(cb, c);

		String errText = sb.toString();
		return errText.substring(0, errText.length() - terminator.length());
	}

	/**
	 * Push back character position to previous if last read character is not {@link #EOF}.
	 *
	 * @param cb
	 *            char buffer containing text to read
	 * @param c
	 *            last read character
	 *
	 * @see #unread(CharBuffer)
	 */
	protected static void unread(CharBuffer cb, int c) {
		if (c != EOF) {
			unread(cb);
		}
	}
}
