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
import java.util.ArrayList;
import java.util.Collection;

import com.google.protobuf.Message;
import com.google.protobuf.Parser;

/**
 * RAW activity data pre-parser capable to deserialize incoming activity data from {@link java.io.InputStream} provided
 * delimited binary data to list of protobuf messages {@link com.google.protobuf.Message}.
 * <p>
 * Protobuf message class name for constructor is mandatory since that class defines binary data parser for protobuf
 * message deserialization.
 *
 * @version $Revision: 1 $
 */
public class ProtoBinaryToDelimitedMessagesPreParser extends AbstractPreParser<InputStream, Collection<Message>> {

	private final Parser<Message> parser;
	private final boolean partial;

	/**
	 * Constructs a new ProtoBinaryToDelimitedMessagesPreParser.
	 *
	 * @param className
	 *            class name for protobuf message to deserialize into
	 *
	 * @throws Exception
	 *             if class name is invalid or parser for that class can't be accessed
	 */
	public ProtoBinaryToDelimitedMessagesPreParser(String className) throws Exception {
		this(className, false);
	}

	/**
	 * Constructs a new ProtoBinaryToDelimitedMessagesPreParser.
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
	public ProtoBinaryToDelimitedMessagesPreParser(String className, boolean partial) throws Exception {
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
	 * <li>{@link java.io.InputStream}</li>
	 * </ul>
	 */
	@Override
	public boolean isDataClassSupported(Object data) {
		return data instanceof InputStream;
	}

	@Override
	public Collection<Message> preParse(InputStream data) throws Exception {
		Collection<Message> msgs = new ArrayList<>();
		Message msg;

		while ((msg = partial ? parser.parsePartialDelimitedFrom(data) : parser.parseDelimitedFrom(data)) != null) {
			msgs.add(msg);
		}

		return msgs;
	}

	@Override
	public String dataTypeReturned() {
		return "PROTOBUF MESSAGE"; // NON-NLS
	}
}
