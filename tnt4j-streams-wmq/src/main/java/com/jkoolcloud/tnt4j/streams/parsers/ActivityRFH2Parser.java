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

package com.jkoolcloud.tnt4j.streams.parsers;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import com.ibm.mq.headers.MQRFH2;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.streams.utils.WmqStreamConstants;

/**
 * Implements an activity data parser that assumes each activity data item is an IBM MQ RFH2/JMS binary data package.
 * Parser resolved RFH2 folders/JMS payload data values put into {@link Map} afterwards mapped into activity fields and
 * properties according to defined parser configuration.
 * <p>
 * This parser resolved data map may contain such entries:
 * <ul>
 * <li>rfh2Folders - RFH2 folders data XML string. Root element for this XML is {@value #ROOT_ELEM}. Further XPath based
 * parsing can be processed by {@link com.jkoolcloud.tnt4j.streams.parsers.ActivityXmlParser}.</li>
 * <li>jmsMsgPayload - JMS JMS message payload data: de-serialized object or bytes if serialisation can't be done.</li>
 * </ul>
 * <p>
 * This activity parser supports configuration properties from {@link AbstractActivityMapParser} (and higher hierarchy
 * parsers).
 *
 *
 * @version $Revision: 1 $
 */
public class ActivityRFH2Parser extends AbstractActivityMapParser {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(ActivityRFH2Parser.class);

	/**
	 * Constant defining root element to construct valid XML string for RFH2 folders data.
	 */
	public static final String ROOT_ELEM = "rfh2Folders"; // NON-NLS
	/**
	 * Constant defining predefined fields name containing RFH2 folders data XML. Use is as locator value.
	 */
	public static final String FOLDERS = "rfh2Folders"; // NON-NLS
	/**
	 * Constant defining predefined field name containing JMS message payload data. Use it as locator value.
	 */
	public static final String JMS_DATA = "jmsMsgPayload";// NON-NLS

	/**
	 * Constructs a new ActivityRFH2Parser.
	 */
	public ActivityRFH2Parser() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	/**
	 * Returns whether this parser supports the given format of the activity data. This is used by activity streams to
	 * determine if the parser can parse the data in the format that the stream has it.
	 * <p>
	 * This parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@code byte[]}</li>
	 * <li>{@link java.nio.ByteBuffer}</li>
	 * <li>{@link java.io.InputStream}</li>
	 * </ul>
	 *
	 * @param data
	 *            data object whose class is to be verified
	 * @return {@code true} if this parser can process data in the specified format, {@code false} - otherwise
	 */
	@Override
	protected boolean isDataClassSupportedByParser(Object data) {
		return data instanceof byte[] || data instanceof ByteBuffer || data instanceof InputStream;
	}

	@Override
	protected Map<String, Object> getDataMap(Object data) {
		InputStream is;
		boolean closeWhenDone = false;
		if (data instanceof byte[]) {
			is = new ByteArrayInputStream((byte[]) data);
			closeWhenDone = true;
		} else if (data instanceof ByteBuffer) {
			is = new ByteArrayInputStream(((ByteBuffer) data).array());
			closeWhenDone = true;
		} else if (data instanceof InputStream) {
			is = (InputStream) data;
		} else {
			throw new IllegalArgumentException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ActivityParser.data.unsupported", data.getClass().getName()));
		}

		Map<String, Object> dataMap = new HashMap<>();

		DataInputStream dis = new DataInputStream(is);
		MQRFH2 rfh2 = new MQRFH2();

		try {
			rfh2.read(dis);
			dataMap.put(FOLDERS, createFoldersXML(rfh2.getFolderStrings()));

			Object jmsDataObject = readJMSDataObject(is);
			dataMap.put(JMS_DATA, jmsDataObject);

			dataMap.put(RAW_ACTIVITY_STRING_KEY, rfh2.toString());
		} catch (Exception exc) {
			Utils.logThrowable(logger(), OpLevel.ERROR,
					StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
					"ActivityRFH2Parser.rfh2.parse.failed", exc);
		} finally {
			if (closeWhenDone) {
				Utils.close(is);
			}
		}

		return dataMap;
	}

	private static final String[] ACTIVITY_DATA_TYPES = { "RFH2", "BINARY" }; // NON-NLS

	/**
	 * Returns types of RAW activity data entries.
	 *
	 * @return types of RAW activity data entries - {@code "RFH2"} and {@code "BINARY"}
	 */
	@Override
	protected String[] getActivityDataType() {
		return ACTIVITY_DATA_TYPES;
	}

	/**
	 * Reads JMS message payload data from provided {@link java.io.InputStream} {@code is}. It can be de-serialized
	 * object instance or byte array.
	 * <p>
	 * If stream does not provide serialized object data (stream bytes at current position should be
	 * {@link java.io.ObjectStreamConstants#STREAM_MAGIC} and {@link java.io.ObjectStreamConstants#STREAM_VERSION}),
	 * remaining bytes are read from input stream.
	 * 
	 * @param is
	 *            input stream to read
	 * @return object instance or byte array read from provided input stream, {@code null} - if stream read fails
	 *
	 * @see java.io.ObjectInputStream#readObject()
	 */
	private Object readJMSDataObject(InputStream is) {
		is.mark(0);
		Object msgObj = null;

		try (ObjectInputStream ois = new ObjectInputStream(is)) {
			msgObj = ois.readObject();
		} catch (Exception exc) {
			Utils.logThrowable(logger(), OpLevel.WARNING,
					StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
					"ActivityRFH2Parser.failed.read.object", exc);
			try {
				is.reset();

				byte[] msgDataBytes = new byte[is.available()];
				is.read(msgDataBytes);

				msgObj = msgDataBytes;
			} catch (IOException ioe) {
				Utils.logThrowable(logger(), OpLevel.WARNING,
						StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
						"ActivityRFH2Parser.failed.read.bytes", ioe);
			}
		}

		return msgObj;
	}

	/**
	 * Builds valid XML document string for provided array of RFH2 folder strings array {@code fStrings}.
	 *
	 * @param fStrings
	 *            RFH2 folder data strings array
	 * @return RFH2 folders XML string
	 */
	private static String createFoldersXML(String[] fStrings) {
		StringBuilder sb = new StringBuilder();
		sb.append("<" + ROOT_ELEM + ">").append("\n");

		if (fStrings != null) {
			for (String fStr : fStrings) {
				sb.append(fStr).append("\n");
			}
		}

		sb.append("</" + ROOT_ELEM + ">\n");

		return sb.toString();
	}
}
