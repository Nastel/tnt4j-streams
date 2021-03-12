/*
 * Copyright 2014-2018 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.inputs;

import java.io.*;
import java.nio.charset.Charset;
import java.util.jar.JarInputStream;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Implements a zipped content activity stream, where each line of the zipped file entry is assumed to represent a
 * single activity or event which should be recorded. Zip file and entry names to stream are defined using "FileName"
 * property in stream configuration.
 * <p>
 * This activity stream requires parsers that can support {@link String} data.
 * <p>
 * This activity stream supports the following configuration properties (in addition to those supported by
 * {@link TNTParseableInputStream}):
 * <ul>
 * <li>FileName - defines zip file path and concrete zip file entry name or entry name pattern defined using characters
 * '*' and '?'. Definition pattern is "zipFilePath!entryNameWildcard". I.e.:
 * "./tnt4j-streams-core/samples/zip-stream/sample.zip!2/*.txt". (Required)</li>
 * <li>ArchType - defines archive type. Can be one of: ZIP, GZIP, JAR. Default value - ZIP. (Optional)</li>
 * <li>Charset - charset name used to decode file(s) contained data. Charset name must comply Java specification
 * (resolvable by {@link java.nio.charset.Charset#forName(String)} to be handled properly. Default value - one returned
 * by {@link java.nio.charset.Charset#defaultCharset()}. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 */
public class ZipLineStream extends TNTParseableInputStream<String> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(ZipLineStream.class);

	private static final String ZIP_PATH_SEPARATOR = "!"; // NON-NLS

	private String zipFileName;
	private String archType;

	private String zipPath;
	private String zipEntriesMask;

	private LineNumberReader lineReader;
	private InflaterInputStream zipStream;
	private Charset charset = Charset.defaultCharset();

	private int lineNumber = 0;
	private int totalBytesCount = 0;

	/**
	 * Constructs a new ZipLineStream.
	 */
	public ZipLineStream() {
		archType = ArchiveTypes.ZIP.name();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (StreamProperties.PROP_FILENAME.equalsIgnoreCase(name)) {
			zipFileName = value;

			if (StringUtils.isNotEmpty(zipFileName)) {
				String zdp[] = zipFileName.split(Pattern.quote(ZIP_PATH_SEPARATOR));

				if (zdp != null) {
					if (zdp.length > 0) {
						zipPath = zdp[0];
					}
					if (zdp.length > 1) {
						zipEntriesMask = StringUtils.isEmpty(zdp[1]) ? null
								: Utils.wildcardToRegex2(zdp[1].replace("\\", "/")); // NON-NLS
						if (zipEntriesMask != null) {
							zipEntriesMask = '^' + zipEntriesMask + '$'; // NON-NLS
						}
					}
				}
			}
		} else if (StreamProperties.PROP_ARCH_TYPE.equalsIgnoreCase(name)) {
			archType = value;
		} else if (StreamProperties.PROP_CHARSET.equalsIgnoreCase(name)) {
			charset = Charset.forName(value);
		}
	}

	@Override
	public Object getProperty(String name) {
		if (StreamProperties.PROP_FILENAME.equalsIgnoreCase(name)) {
			return zipFileName;
		}
		if (StreamProperties.PROP_ARCH_TYPE.equalsIgnoreCase(name)) {
			return archType;
		}
		if (StreamProperties.PROP_CHARSET.equalsIgnoreCase(name)) {
			return charset.name();
		}
		return super.getProperty(name);
	}

	@Override
	protected void applyProperties() throws Exception {
		super.applyProperties();

		if (StringUtils.isEmpty(zipFileName)) {
			throw new IllegalStateException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"TNTInputStream.property.undefined", StreamProperties.PROP_FILENAME));
		}
	}

	@Override
	protected void initialize() throws Exception {
		super.initialize();

		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"ZipLineStream.initializing.stream", zipFileName);

		InputStream fis = loadFile(zipPath);

		try {
			if (ArchiveTypes.JAR.name().equalsIgnoreCase(archType)) {
				zipStream = new JarInputStream(fis);
			} else if (ArchiveTypes.GZIP.name().equalsIgnoreCase(archType)) {
				zipStream = new GZIPInputStream(fis);
			} else {
				zipStream = new ZipInputStream(fis);
			}
		} catch (IOException exc) {
			Utils.close(fis);

			throw exc;
		}

		if (zipStream instanceof GZIPInputStream) {
			lineReader = new LineNumberReader(new BufferedReader(new InputStreamReader(zipStream, charset)));
		} else {
			hasNextEntry();
		}
	}

	/**
	 * Loads zip file as input stream to read.
	 *
	 * @param zipPath
	 *            system dependent zip file path
	 * @return file input stream to read
	 * @throws Exception
	 *             If path defined file is not found
	 */
	protected InputStream loadFile(String zipPath) throws Exception {
		return new FileInputStream(zipPath);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method returns a string containing the contents of the next line in the zip file entry.
	 */
	@Override
	public String getNextItem() throws Exception {
		if (lineReader == null) {
			throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ZipLineStream.zip.input.not.opened"));
		}

		String line = Utils.getNonEmptyLine(lineReader);
		lineNumber = lineReader.getLineNumber();

		if (line == null && hasNextEntry()) {
			line = getNextItem();
		}

		if (line != null) {
			addStreamedBytesCount(line.getBytes().length);
		}

		return line;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method returns line number of the zip file entry last read.
	 */
	@Override
	public int getActivityPosition() {
		return lineNumber;
	}

	@Override
	public long getTotalBytes() {
		return totalBytesCount;
	}

	@Override
	protected void cleanup() {
		Utils.close(lineReader);
		lineReader = null;

		if (zipStream instanceof ZipInputStream) {
			try {
				((ZipInputStream) zipStream).closeEntry();
			} catch (IOException exc) {
			}
		}

		Utils.close(zipStream);
		zipStream = null;

		super.cleanup();
	}

	private boolean hasNextEntry() throws IOException {
		if (zipStream instanceof ZipInputStream) {
			ZipInputStream zis = (ZipInputStream) zipStream;

			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				String entryName = entry.getName();

				if (entry.getSize() != 0 && (zipEntriesMask == null || entryName.matches(zipEntriesMask))) {
					totalBytesCount += entry.getSize();
					lineReader = new LineNumberReader(new BufferedReader(new InputStreamReader(zis, charset)));
					lineNumber = 0;

					logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"ZipLineStream.opening.entry", entryName);

					return true;
				}
			}
		}

		return false;
	}

	private enum ArchiveTypes {
		/**
		 * Zip archive type.
		 */
		ZIP,
		/**
		 * GZip archive type.
		 */
		GZIP,
		/**
		 * Jar archive type.
		 */
		JAR
	}
}
