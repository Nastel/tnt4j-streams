/*
 * Copyright 2014-2020 JKOOL, LLC.
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

import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * RAW activity data pre-parser capable to convert incoming activity data from {@code byte[]} to
 * {@link java.lang.String} format.
 * <p>
 * Default {@link Charset} used to convert between binary and string data is
 * {@link java.nio.charset.Charset#defaultCharset()}. Custom charset can be defined using constructor parameter
 * {@code charsetName}.
 * 
 * @version $Revision: 1 $
 */
public class BinaryToStringPreParser extends AbstractPreParser<byte[], String> {

	private Charset charset;

	/**
	 * Constructs a new BinaryToStringPreParser.
	 */
	public BinaryToStringPreParser() {
		this.charset = Charset.defaultCharset();
	}

	/**
	 * Constructs a new BinaryToStringPreParser.
	 * 
	 * @param charsetName
	 *            RAW activity data charset
	 */
	public BinaryToStringPreParser(String charsetName) {
		try {
			this.charset = Charset.forName(charsetName);
		} catch (UnsupportedCharsetException | IllegalCharsetNameException e) {
			throw e;
		} catch (IllegalArgumentException e) {
			this.charset = Charset.defaultCharset();
		}
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
		return byte[].class.isInstance(data);
	}

	@Override
	public String preParse(byte[] data) throws Exception {
		return Utils.getString(data, charset);
	}

	@Override
	public String dataTypeReturned() {
		return "TEXT"; // NON-NLS
	}
}
