/*
 * Copyright 2014-2019 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.configure.build;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.collections4.CollectionUtils;

import com.jkoolcloud.tnt4j.streams.inputs.InputStreamListener;
import com.jkoolcloud.tnt4j.streams.inputs.StreamTasksListener;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;

/**
 * Build streams and streaming context using plain Java objects (POJO).
 * 
 * @version $Revision: 1 $
 */
public class POJOStreamsBuilder implements StreamsBuilder {
	private InputStreamListener sListener;
	private StreamTasksListener tListener;
	private Collection<TNTInputStream<?, ?>> streams = new ArrayList<>();

	/**
	 * Sets set of stream instances to be run.
	 * 
	 * @param streams
	 *            set of stream instances
	 * @return instance of this streams builder
	 */
	public POJOStreamsBuilder addStreams(TNTInputStream<?, ?>... streams) {
		if (streams != null) {
			CollectionUtils.addAll(this.streams, streams);
		}

		return this;
	}

	/**
	 * Sets set of stream instances to be run.
	 * 
	 * @param streams
	 *            collection of stream instances
	 * @return instance of this streams builder
	 */
	public POJOStreamsBuilder addStreams(Collection<TNTInputStream<?, ?>> streams) {
		if (streams != null) {
			CollectionUtils.addAll(this.streams, streams);
		}

		return this;
	}

	/**
	 * Adds stream instance to collection of streams to be run.
	 * 
	 * @param stream
	 *            stream instance to add
	 * @return instance of this streams builder
	 */
	public POJOStreamsBuilder addStream(TNTInputStream<?, ?> stream) {
		streams.add(stream);

		return this;
	}

	/**
	 * Sets input stream listener to be added to every run stream.
	 * 
	 * @param l
	 *            input stream listener
	 * @return instance of this streams builder
	 */
	public POJOStreamsBuilder setStreamListener(InputStreamListener l) {
		this.sListener = l;

		return this;
	}

	/**
	 * Sets stream tasks listener to be added to every run stream.
	 * 
	 * @param l
	 *            stream tasks listener
	 * @return instance of this streams builder
	 */
	public POJOStreamsBuilder setTaskListener(StreamTasksListener l) {
		this.tListener = l;

		return this;
	}

	@Override
	public Collection<TNTInputStream<?, ?>> getStreams() {
		return streams;
	}

	@Override
	public InputStreamListener getStreamListener() {
		return sListener;
	}

	@Override
	public StreamTasksListener getTasksListener() {
		return tListener;
	}
}
