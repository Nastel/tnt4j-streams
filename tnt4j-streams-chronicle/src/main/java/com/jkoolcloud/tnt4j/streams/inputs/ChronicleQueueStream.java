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

package com.jkoolcloud.tnt4j.streams.inputs;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.ChronicleQueueProperties;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.*;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.threads.Pauser;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.ValueIn;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.Wires;

/**
 * Implements a Chronicle queue transported activity stream, where each Chronicle queue entry marshaled document payload
 * data is assumed to represent a single activity entity which should be recorded.
 * <p>
 * This activity stream requires parsers that can support {@link java.lang.Object} data.
 * <p>
 * This activity stream supports the following configuration properties (in addition to those supported by
 * {@link TNTParseableInputStream}):
 * <ul>
 * <li>FileName - path of Chronicle queue serialization folder. (Required)</li>
 * <li>MarshallClass - class name of marshaled Chronicle queue entries. (Required)</li>
 * </ul>
 *
 * @version $Revision: 2 $
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 */
public class ChronicleQueueStream extends TNTParseableInputStream<Object> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(ChronicleQueueStream.class);

	private File queuePath;
	private final Map<String, Class<?>> classNameMap = new HashMap<>();
	private String handlingClasses;

	private ChronicleQueue queue;
	private ExcerptTailer tailer;
	private Pauser pauser;
	private boolean startFromLatest = true;
	private Object lastRead;

	/**
	 * Constructs a new ChronicleQueueStream. Requires configuration settings to set input stream source.
	 */
	public ChronicleQueueStream() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (StreamProperties.PROP_FILENAME.equalsIgnoreCase(name)) {
			queuePath = new File(value);
			if (!queuePath.isDirectory()) {
				queuePath = queuePath.getParentFile();
			}
		} else if (ChronicleQueueProperties.PROP_MARSHALL_CLASS.equalsIgnoreCase(name)) {
			handlingClasses = value;

		} else if (ChronicleQueueProperties.PROP_START_FROM_LATEST.equalsIgnoreCase(name)) {
			startFromLatest = Utils.toBoolean(value);
		}
	}

	@Override
	public Object getProperty(String name) {
		if (StreamProperties.PROP_FILENAME.equalsIgnoreCase(name)) {
			return queuePath;
		}
		if (ChronicleQueueProperties.PROP_MARSHALL_CLASS.equalsIgnoreCase(name)) {
			return classNameMap;
		}

		return super.getProperty(name);
	}

	@Override
	protected void applyProperties() throws Exception {
		super.applyProperties();

		if (queuePath == null) {
			throw new IllegalStateException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"TNTInputStream.property.undefined", StreamProperties.PROP_FILENAME));
		}

		if (StringUtils.isEmpty(handlingClasses)) {
			throw new IllegalStateException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"TNTInputStream.property.undefined", ChronicleQueueProperties.PROP_MARSHALL_CLASS));
		}

		for (String className : StreamsConstants.getMultiProperties(handlingClasses)) {
			Class<?> aClass = Class.forName(className);
			classNameMap.put(aClass.getSimpleName().toUpperCase(), aClass);
		}
	}

	@Override
	protected void initialize() throws Exception {
		super.initialize();

		queue = ChronicleQueue.singleBuilder(queuePath).build();
		tailer = queue.createTailer();

		if (startFromLatest) {
			readOne0(tailer); // dummy read, seems that going to end without reading fails
			ExcerptTailer excerptTailer = tailer.toEnd();
		}

		pauser = Pauser.balanced();
	}

	@Override
	public String[] getDataTags(Object data) {
		return new String[] { data.getClass().getSimpleName().toUpperCase() };
	}

	@Override
	public Object getNextItem() throws Exception {
		while (true) {
			if (readOne0(tailer)) {
				return lastRead;
			} else {
				pauser.pause();
			}
		}
	}

	private boolean readOne0(ExcerptTailer in) throws InstantiationException, IllegalAccessException {
		try (DocumentContext context = in.readingDocument()) {
			if (!context.isPresent()) {
				return false;
			}

			if (context.isMetaData()) {
				readOneMetaData(context);

				return true;
			}

			if (context.isData()) {
				accept(context.wire());
			}
		}

		return lastRead != null;
	}

	private void accept(Wire wire) throws IllegalAccessException, InstantiationException {
		StringBuilder sb = new StringBuilder();
		ValueIn valueIn = wire.readEventName(sb);
		Class<?> aClass = classNameMap.get(sb.toString().toUpperCase());

		if (aClass == null) {
			logger().log(OpLevel.ERROR, StreamsResources.getString(ChronicleStreamConstants.RESOURCE_BUNDLE_NAME,
					"ChronicleQueueStream.unsupported.class"), sb.toString());
			lastRead = null;
		} else {
			Object entry = aClass.newInstance();
			lastRead = Wires.object0(valueIn, entry, aClass);
		}
	}

	private static boolean readOneMetaData(DocumentContext context) {
		StringBuilder sb = Wires.acquireStringBuilder();
		Wire wire = context.wire();
		Bytes<?> bytes = wire.bytes();
		long r = bytes.readPosition();
		wire.readEventName(sb);
		// roll back position to where it was before we read the SB
		bytes.readPosition(r);

		return true;
	}

	@Override
	protected void cleanup() {
		Utils.close(queue);

		super.cleanup();
	}
}
