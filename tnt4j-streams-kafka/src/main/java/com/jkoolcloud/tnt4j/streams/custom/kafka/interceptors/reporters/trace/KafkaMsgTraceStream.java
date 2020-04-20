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

package com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.trace;

import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.inputs.AbstractBufferedStream;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;

/**
 * Implements Kafka intercepted messages trace reporter stream. Stream itself does nothing and used just to initiates
 * streaming process. Reporter uses only stream output to send intercepted messages data to jKoolCloud.
 *
 * @param <T>
 *            the type of handled RAW activity data
 *
 * @version $Revision: 1 $
 */
class KafkaMsgTraceStream<T> extends AbstractBufferedStream<T> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(KafkaMsgTraceStream.class);

	private boolean ended = false;

	/**
	 * Constructs a new KafkaMsgTraceStream.
	 */
	public KafkaMsgTraceStream() {
		setName("KafkaMsgTraceStream"); // NON-NLS
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public boolean addInputToBuffer(T inputData) throws IllegalStateException {
		return super.addInputToBuffer(inputData);
	}

	@Override
	protected ActivityInfo makeActivityInfo(T data) throws Exception {
		if (data instanceof ActivityInfo) {
			return (ActivityInfo) data;
		} else {
			return super.makeActivityInfo(data);
		}
	}

	@Override
	protected long getActivityItemByteSize(T activityItem) {
		return 0; // TODO
	}

	@Override
	protected boolean isInputEnded() {
		return ended;
	}

	/**
	 * Marks stream input end.
	 */
	public void markEnded() {
		ended = true;
		offerDieMarker();
	}

}
