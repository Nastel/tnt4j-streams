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

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

/**
 * Base class for abstract activity Snappy compressed RAW data value pre-parser.
 *
 * @param <V>
 *            the type of Snappy compressed data provider: byte array, input stream, etc.
 * 
 * @version $Revision: 1 $
 */
public abstract class AbstractSnappyPreParser<V> extends AbstractPreParser<V, Object> {

	/**
	 * Decompressed data type.
	 */
	protected final UncompressedType uncompressedType;

	/**
	 * Charset used to convert binary data to string.
	 */
	protected Charset charset;

	/**
	 * Constructs a new AbstractSnappyPreParser.
	 * 
	 * @param uncompressedType
	 *            decompressed data type
	 */
	protected AbstractSnappyPreParser(UncompressedType uncompressedType) {
		this.uncompressedType = uncompressedType;
	}

	/**
	 * Constructs a new AbstractSnappyPreParser.
	 *
	 * @param charsetName
	 *            charset name used to convert binary data to string
	 */
	protected AbstractSnappyPreParser(String charsetName) {
		this(UncompressedType.STRING);

		try {
			this.charset = Charset.forName(charsetName);
		} catch (UnsupportedCharsetException | IllegalCharsetNameException e) {
			throw e;
		} catch (IllegalArgumentException e) {
			this.charset = Charset.defaultCharset();
		}
	}

	@Override
	public String dataTypeReturned() {
		switch (uncompressedType) {
		case STRING:
			return "TEXT"; // NON-NLS
		case BINARY:
		default:
			return "BINARY"; // NON-NLS
		}
	}
}
