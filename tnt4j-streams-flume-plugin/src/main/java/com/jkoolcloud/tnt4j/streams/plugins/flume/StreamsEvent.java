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

package com.jkoolcloud.tnt4j.streams.plugins.flume;

import org.apache.flume.Event;
import org.apache.flume.event.JSONEvent;

/**
 * Extends Apache Flume {@link JSONEvent} to add additional attributes, required by TNT4J-Streams.
 *
 * @version $Revision: 1 $
 */
public class StreamsEvent extends JSONEvent {

	private String sinkName;
	private String chanelName;

	/**
	 * Constructs a new StreamsEvent.
	 */
	public StreamsEvent() {
		super();
	}

	/**
	 * Constructs a new StreamEvent based on Apache Flume output event data. Additionally sets event channel and sink
	 * names.
	 *
	 * @param event
	 *            event to wrap
	 * @param sinkName
	 *            event sink name
	 * @param chanelName
	 *            event channel name
	 */
	public StreamsEvent(Event event, String sinkName, String chanelName) {
		this(event);
		this.sinkName = sinkName;
		this.chanelName = chanelName;
	}

	/**
	 * Constructs a new StreamEvent based on Apache Flume output event data.
	 *
	 * @param event
	 *            event to wrap
	 */
	public StreamsEvent(Event event) {
		this();
		this.setBody(event.getBody());
		this.setHeaders(event.getHeaders());
	}

	/**
	 * Returns event sink name.
	 *
	 * @return sink name
	 */
	public String getSinkName() {
		return sinkName;
	}

	/**
	 * Sets event sink name.
	 *
	 * @param sinkName
	 *            sink name
	 */
	public void setSinkName(String sinkName) {
		this.sinkName = sinkName;
	}

	/**
	 * Returns event channel name.
	 *
	 * @return channel name
	 */
	public String getChanelName() {
		return chanelName;
	}

	/**
	 * Sets event channel name.
	 *
	 * @param chanelName
	 *            channel name
	 */
	public void setChanelName(String chanelName) {
		this.chanelName = chanelName;
	}

}
