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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.commons.compress.compressors.snappy.SnappyCompressorInputStream;

import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * RAW activity data pre-parser capable to decompress provided Snappy compressor input stream
 * {@link SnappyCompressorInputStream} fed data to selected format: binary or text.
 * <p>
 * Default decompressed data type is {@link com.jkoolcloud.tnt4j.streams.preparsers.UncompressedType#BINARY}.
 * <p>
 * Default {@link java.nio.charset.Charset} used to convert between binary and string data is
 * {@link java.nio.charset.Charset#defaultCharset()}. Custom charset can be defined using constructor parameter
 * {@code charsetName}.
 *
 * @version $Revision: 1 $
 */
public class SnappyInputStreamPreParser extends AbstractSnappyPreParser<Object> {

	/**
	 * Constructs a new SnappyInputStreamPreParser.
	 */
	public SnappyInputStreamPreParser() {
		super(UncompressedType.BINARY);
	}

	/**
	 * Constructs a new SnappyInputStreamPreParser.
	 * 
	 * @param uncompressedType
	 *            decompressed data type
	 */
	public SnappyInputStreamPreParser(UncompressedType uncompressedType) {
		super(uncompressedType);
	}

	/**
	 * Constructs a new SnappyInputStreamPreParser.
	 *
	 * @param charsetName
	 *            charset name used to convert binary data to string
	 */
	public SnappyInputStreamPreParser(String charsetName) {
		super(charsetName);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This pre-parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link java.io.InputStream}</li>
	 * <li>{@code byte[]}</li>
	 * </ul>
	 */
	@Override
	public boolean isDataClassSupported(Object data) {
		return data instanceof InputStream || data instanceof byte[];
	}

	@Override
	public Object preParse(Object data) throws Exception {
		InputStream din;

		if (data instanceof InputStream) {
			din = (InputStream) data;
		} else {
			din = new ByteArrayInputStream((byte[]) data);
		}

		byte[] uncompressed;
		try (SnappyCompressorInputStream in = new SnappyCompressorInputStream(din)) {
			int size = in.getSize();
			uncompressed = new byte[size];
			in.read(uncompressed);
		}

		switch (uncompressedType) {
		case STRING:
			return Utils.getString(uncompressed, charset);
		case BINARY:
		default:
			return uncompressed;
		}
	}
}
