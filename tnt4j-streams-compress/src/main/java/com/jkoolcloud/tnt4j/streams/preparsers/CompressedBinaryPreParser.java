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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * RAW activity data pre-parser capable to decompress provided binary or {@link java.io.InputStream} fed compressed
 * activity data to selected format: binary or text.
 * <p>
 * Default decompressed data type is {@link com.jkoolcloud.tnt4j.streams.preparsers.UncompressedType#BINARY}.
 * <p>
 * Default {@link java.nio.charset.Charset} used to convert between binary and string data is
 * {@link java.nio.charset.Charset#defaultCharset()}. Custom charset can be defined using constructor parameter
 * {@code charsetName}.
 *
 * @version $Revision: 1 $
 */
public class CompressedBinaryPreParser extends AbstractPreParser<Object, Object> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(CompressedBinaryPreParser.class);

	/**
	 * Decompressed data type.
	 */
	protected final UncompressedType uncompressedType;

	/**
	 * Charset used to convert binary data to string.
	 */
	protected Charset charset;

	protected String compressFormat;

	/**
	 * Constructs a new CompressedBinaryPreParser.
	 * 
	 * @param compressFormat
	 *            compression format to use
	 */
	public CompressedBinaryPreParser(String compressFormat) {
		this(compressFormat, UncompressedType.BINARY);
	}

	/**
	 * Constructs a new CompressedBinaryPreParser.
	 *
	 * @param compressFormat
	 *            compression format to use
	 * @param uncompressedType
	 *            decompressed data type
	 */
	public CompressedBinaryPreParser(String compressFormat, UncompressedType uncompressedType) {
		this.compressFormat = compressFormat;
		this.uncompressedType = uncompressedType;
	}

	/**
	 * Constructs a new CompressedBinaryPreParser.
	 *
	 * @param compressFormat
	 *            compression format to use
	 * @param charsetName
	 *            charset name used to convert binary data to string
	 */
	public CompressedBinaryPreParser(String compressFormat, String charsetName) {
		this(compressFormat, UncompressedType.STRING);

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
		try (InputStream in = getCompressorInputStream(compressFormat, din)) {
			uncompressed = IOUtils.toByteArray(in);
		} catch (IOException exc) {
			if ("Premature end of stream reading size".equals(exc.getMessage())) { // NON-NLS
				// stream closed
				return null;
			}

			throw exc;
		}

		switch (uncompressedType) {
		case STRING:
			return Utils.getString(uncompressed, charset);
		case BINARY:
		default:
			return uncompressed;
		}
	}

	/**
	 * Creates a compressor input stream from a compression format name and an input stream.
	 * 
	 * @param compressFormat
	 *            compression format name
	 * @param din
	 *            RAW compressed data input stream
	 * @return compressor input stream instance
	 * 
	 * @throws CompressorException
	 *             if the compress format name is not known
	 * 
	 * @see org.apache.commons.compress.compressors.CompressorStreamFactory
	 */
	protected static CompressorInputStream getCompressorInputStream(String compressFormat, InputStream din)
			throws CompressorException {
		if (din instanceof CompressorInputStream) {
			return (CompressorInputStream) din;
		} else if (StringUtils.equalsAnyIgnoreCase(compressFormat, "snappy", "snappy-raw")) { // NON-NLS
			return CompressorStreamFactory.getSingleton()
					.createCompressorInputStream(CompressorStreamFactory.SNAPPY_RAW, din, true);
		} else if (StringUtils.equalsAnyIgnoreCase(compressFormat, "gzip", "gz")) { // NON-NLS
			return CompressorStreamFactory.getSingleton().createCompressorInputStream(CompressorStreamFactory.GZIP, din,
					true);
		} else {
			return CompressorStreamFactory.getSingleton().createCompressorInputStream(compressFormat, din, true);
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
