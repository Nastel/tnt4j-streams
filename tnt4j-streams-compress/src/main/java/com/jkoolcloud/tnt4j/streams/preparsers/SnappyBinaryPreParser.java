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

import org.apache.commons.compress.compressors.CompressorStreamFactory;

/**
 * RAW activity data pre-parser capable to decompress provided binary or {@link java.io.InputStream} fed Snappy
 * compressed activity data to selected format: binary or text.
 * <p>
 * Default decompressed data type is {@link com.jkoolcloud.tnt4j.streams.preparsers.UncompressedType#BINARY}.
 * <p>
 * Default {@link java.nio.charset.Charset} used to convert between binary and string data is
 * {@link java.nio.charset.Charset#defaultCharset()}. Custom charset can be defined using constructor parameter
 * {@code charsetName}.
 * 
 * @deprecated use {@link com.jkoolcloud.tnt4j.streams.preparsers.CompressedBinaryPreParser} instead.
 *
 * @version $Revision: 2 $
 */
public class SnappyBinaryPreParser extends CompressedBinaryPreParser {
	/**
	 * Constructs a new SnappyBinaryPreParser.
	 */
	public SnappyBinaryPreParser() {
		this(UncompressedType.BINARY);
	}

	/**
	 * Constructs a new SnappyBinaryPreParser.
	 * 
	 * @param uncompressedType
	 *            decompressed data type
	 */
	public SnappyBinaryPreParser(UncompressedType uncompressedType) {
		super(CompressorStreamFactory.SNAPPY_RAW, uncompressedType);
	}

	/**
	 * Constructs a new SnappyBinaryPreParser.
	 *
	 * @param compressFormat
	 *            compression format to use
	 * @param charsetName
	 *            charset name used to convert binary data to string
	 */
	public SnappyBinaryPreParser(String compressFormat, String charsetName) {
		super(compressFormat, UncompressedType.STRING);
	}
}
