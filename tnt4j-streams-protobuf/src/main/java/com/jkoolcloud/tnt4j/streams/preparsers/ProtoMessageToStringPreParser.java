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

package com.jkoolcloud.tnt4j.streams.preparsers;

import org.apache.commons.lang3.StringUtils;

import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import com.google.protobuf.util.JsonFormat;

/**
 * RAW activity data pre-parser capable to deserialize incoming activity data from protobuf message
 * {@link com.google.protobuf.Message} to selected format string: JSON or TEXT.
 * <p>
 * Default string print format is
 * {@link com.jkoolcloud.tnt4j.streams.preparsers.ProtoMessageToStringPreParser.PrintFormatType#JSON}.
 *
 * @version $Revision: 1 $
 */
public class ProtoMessageToStringPreParser extends AbstractPreParser<Message, String> {

	private static final JsonFormat.Printer jsonPrinter = JsonFormat.printer();
	private static final TextFormat.Printer textPrinter = TextFormat.printer();

	/**
	 * Protobuf message print format type.
	 */
	protected final PrintFormatType printFormatType;

	/**
	 * Constructs a new ProtoMessageToStringPreParser.
	 */
	protected ProtoMessageToStringPreParser() {
		this(PrintFormatType.JSON);
	}

	/**
	 * Constructs a new ProtoMessageToStringPreParser.
	 *
	 * @param printFormatType
	 *            protobuf message print format type
	 */
	protected ProtoMessageToStringPreParser(PrintFormatType printFormatType) {
		this.printFormatType = printFormatType;
	}

	/**
	 * Constructs a new ProtoMessageToStringPreParser.
	 *
	 * @param printFormatTypeName
	 *            protobuf message print format type name
	 */
	protected ProtoMessageToStringPreParser(String printFormatTypeName) {
		printFormatType = StringUtils.isEmpty(printFormatTypeName) ? PrintFormatType.JSON
				: PrintFormatType.valueOf(printFormatTypeName.toUpperCase());
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This pre-parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link com.google.protobuf.Message}</li>
	 * </ul>
	 */
	@Override
	public boolean isDataClassSupported(Object data) {
		return data instanceof Message;
	}

	@Override
	public String preParse(Message data) throws Exception {
		switch (printFormatType) {
		case TEXT:
			return textPrinter.printToString(data);
		case JSON:
		default:
			return jsonPrinter.print(data);
		}
	}

	@Override
	public String dataTypeReturned() {
		switch (printFormatType) {
		case TEXT:
			return "TEXT"; // NON-NLS
		case JSON:
		default:
			return "JSON"; // NON-NLS
		}
	}

	/**
	 * Defines supported protobuf message print format types.
	 */
	public static enum PrintFormatType {
		/**
		 * Prints protobuf message as a JSON string.
		 */
		JSON,

		/**
		 * Prints protobuf message as a text string.
		 */
		TEXT
	}
}
