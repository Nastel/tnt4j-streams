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

package com.jkoolcloud.tnt4j.streams.preparsers;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;

/**
 * RAW activity data pre-parser capable to deserialize incoming activity data from binary data provider to protobuf
 * message {@link com.google.protobuf.Message}.
 * <p>
 * Binary data provider may be one of:
 * <ul>
 * <li>{@code byte[]} - byte array</li>
 * <li>{@link java.io.InputStream} - binary input stream</li>
 * <li>{@link com.google.protobuf.CodedInputStream} - coded message input stream</li>
 * <li>{@link java.nio.ByteBuffer} - byte buffer</li>
 * <li>{@link com.google.protobuf.ByteString} - binary string</li>
 * </ul>
 * <p>
 * Protobuf message class name for constructor is mandatory since that class defines binary data parser for protobuf
 * message deserialization.
 *
 * @version $Revision: 1 $
 */
public class ProtoBinaryToMessagePreParser extends AbstractPreParser<Object, Message> {

	private final Parser<Message> parser;
	private final boolean partial;

	/**
	 * Constructs a new ProtoBinaryToMessagePreParser.
	 * 
	 * @param className
	 *            class name for protobuf message to deserialize into
	 * 
	 * @throws Exception
	 *             if class name is invalid or parser for that class can't be accessed
	 */
	public ProtoBinaryToMessagePreParser(String className) throws Exception {
		this(className, false);
	}

	/**
	 * Constructs a new ProtoBinaryToMessagePreParser.
	 * 
	 * @param className
	 *            class name for protobuf message to deserialize into
	 * @param partial
	 *            flag indicating message may miss some required fields
	 * 
	 * @throws Exception
	 *             if class name is invalid or parser for that class can't be accessed
	 */
	@SuppressWarnings("unchecked")
	public ProtoBinaryToMessagePreParser(String className, boolean partial) throws Exception {
		Class<?> cls = Class.forName(className);
		Method parserMethod = cls.getDeclaredMethod("parser"); // NON-NLS
		parser = (Parser<Message>) parserMethod.invoke(null);

		this.partial = partial;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This pre-parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@code byte[]}</li>
	 * <li>{@link java.io.InputStream}</li>
	 * <li>{@link com.google.protobuf.CodedInputStream}</li>
	 * <li>{@link java.nio.ByteBuffer}</li>
	 * <li>{@link com.google.protobuf.ByteString}</li>
	 * </ul>
	 */
	@Override
	public boolean isDataClassSupported(Object data) {
		return data instanceof byte[] || data instanceof InputStream || data instanceof CodedInputStream
				|| data instanceof ByteBuffer || data instanceof ByteString;
	}

	@Override
	public Message preParse(Object data) throws Exception {
		Message msg = null;

		if (data instanceof byte[]) {
			byte[] msgBytes = (byte[]) data;
			msg = partial ? parser.parsePartialFrom(msgBytes) : parser.parseFrom(msgBytes);
		} else if (data instanceof InputStream) {
			InputStream msgIS = (InputStream) data;
			msg = partial ? parser.parsePartialFrom(msgIS) : parser.parseFrom(msgIS);
		} else if (data instanceof CodedInputStream) {
			CodedInputStream msgCIS = (CodedInputStream) data;
			msg = partial ? parser.parsePartialFrom(msgCIS) : parser.parseFrom(msgCIS);
		} else if (data instanceof ByteBuffer) {
			ByteBuffer msgBBuf = (ByteBuffer) data;
			msg = parser.parseFrom(msgBBuf);
		} else if (data instanceof ByteString) {
			ByteString msgBStr = (ByteString) data;
			msg = partial ? parser.parsePartialFrom(msgBStr) : parser.parseFrom(msgBStr);
		}

		return msg;
	}

	@Override
	public String dataTypeReturned() {
		return "PROTOBUF MESSAGE"; // NON-NLS
	}
}
