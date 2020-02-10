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

import java.io.File;

import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.ChronicleQueueProperties;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.threads.Pauser;
import net.openhft.chronicle.wire.ReadMarshallable;

/**
 * Implements a Chronicle queue transported activity stream, where each Chronicle queue entry marshaled document payload
 * data is assumed to represent a single activity entity which should be recorded.
 * <p>
 * This activity stream requires parsers that can support Chronicle's
 * {@link net.openhft.chronicle.wire.ReadMarshallable} or {@link java.lang.Object} data.
 * <p>
 * This activity stream supports the following configuration properties (in addition to those supported by
 * {@link TNTParseableInputStream}):
 * <ul>
 * <li>FileName - path of Chronicle queue serialization folder. (Required)</li>
 * <li>MarshallClass - class name of marshaled Chronicle queue entries. (Required)</li>
 * </ul>
 *
 * @version $Revision: 1$
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 */
public class ChronicleQueueStream extends TNTParseableInputStream<ReadMarshallable> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(ChronicleQueueStream.class);

	private File queuePath;
	private String className;

	private ChronicleQueue queue;
	private ExcerptTailer tailer;
	private Pauser pauser;
	private Class<? extends ReadMarshallable> clazz;
	private boolean startFromLatest = true;

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
			className = value;
		} else if (ChronicleQueueProperties.PROP_START_FROM_LATEST.equalsIgnoreCase(name)) {
			startFromLatest = Utils.toBoolean(value);
		}
	}

	@Override
	public Object getProperty(String name) {
		if (StreamProperties.PROP_FILENAME.equals(name)) {
			return queuePath;
		}
		if (ChronicleQueueProperties.PROP_MARSHALL_CLASS.equalsIgnoreCase(name)) {
			return className;
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

		if (StringUtils.isEmpty(className)) {
			throw new IllegalStateException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"TNTInputStream.property.undefined", ChronicleQueueProperties.PROP_MARSHALL_CLASS));
		}
	}

	@Override
	protected void initialize() throws Exception {
		super.initialize();

		queue = ChronicleQueue.singleBuilder(queuePath).build();
		clazz = Class.forName(className).asSubclass(ReadMarshallable.class);
		tailer = queue.createTailer();

		if (startFromLatest) {
			tailer.toEnd();
		}

		pauser = Pauser.balanced();
	}

	@Override
	public ReadMarshallable getNextItem() throws Exception {
		while (true) {
			ReadMarshallable readMarshallable = clazz.newInstance();
			boolean s = tailer.readDocument(readMarshallable);
			if (s) {
				pauser.reset();
				return readMarshallable;
			} else {
				pauser.pause();
			}
		}
	}

	@Override
	protected void cleanup() {
		Utils.close(queue);

		super.cleanup();
	}
}
