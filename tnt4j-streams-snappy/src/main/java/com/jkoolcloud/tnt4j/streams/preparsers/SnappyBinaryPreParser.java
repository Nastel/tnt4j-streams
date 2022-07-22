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

import org.xerial.snappy.Snappy;

/**
 * RAW activity data pre-parser capable to decompress incoming Snappy compressed activity data from {@code byte[]} to
 * selected format: binary or text.
 * <p>
 * Default decompressed data type is {@link com.jkoolcloud.tnt4j.streams.preparsers.UncompressedType#BINARY}.
 * <p>
 * Default {@link java.nio.charset.Charset} used to convert between binary and string data is
 * {@link java.nio.charset.Charset#defaultCharset()}. Custom charset can be defined using constructor parameter
 * {@code charsetName}.
 *
 * @version $Revision: 1 $
 */
public class SnappyBinaryPreParser extends AbstractSnappyPreParser<byte[]> {

	/**
	 * Constructs a new SnappyBinaryPreParser.
	 */
	public SnappyBinaryPreParser() {
		super(UncompressedType.BINARY);
	}

	/**
	 * Constructs a new SnappyBinaryPreParser.
	 * 
	 * @param uncompressedType
	 *            decompressed data type
	 */
	public SnappyBinaryPreParser(UncompressedType uncompressedType) {
		super(uncompressedType);
	}

	/**
	 * Constructs a new SnappyBinaryPreParser.
	 *
	 * @param charsetName
	 *            charset name used to convert binary data to string
	 */
	public SnappyBinaryPreParser(String charsetName) {
		super(charsetName);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This pre-parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@code byte[]}</li>
	 * </ul>
	 */
	@Override
	public boolean isDataClassSupported(Object data) {
		return data instanceof byte[];
	}

	@Override
	public Object preParse(byte[] data) throws Exception {
		switch (uncompressedType) {
		case STRING:
			return Snappy.uncompressString(data, charset);
		case BINARY:
		default:
			return Snappy.uncompress(data);
		}
	}
}
