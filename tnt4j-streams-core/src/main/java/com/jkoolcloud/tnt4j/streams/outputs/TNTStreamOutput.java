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

package com.jkoolcloud.tnt4j.streams.outputs;

import java.util.Collection;
import java.util.Map;

import com.jkoolcloud.tnt4j.streams.configure.NamedObject;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;

/**
 * This interface defines operations commonly used by TNT4J-Streams outputs.
 *
 * @param <T>
 *            the type of handled activity data
 *
 * @version $Revision: 1 $
 *
 * @see TNTInputStream#setOutput(TNTStreamOutput)
 * @see com.jkoolcloud.tnt4j.tracker.Tracker
 */
public interface TNTStreamOutput<T> extends NamedObject {

	/**
	 * Performs streamed activity item logging processing. To log activity item various implementations of
	 * {@link com.jkoolcloud.tnt4j.tracker.Tracker} may be used.
	 * 
	 * @param item
	 *            activity item to log
	 * @throws Exception
	 *             if any errors occurred while logging item
	 */
	void logItem(T item) throws Exception;

	/**
	 * Performs initialization of stream output handler.
	 * 
	 * @throws Exception
	 *             indicates that stream output handler is not configured properly
	 *
	 * @see #setProperties(Collection)
	 */
	void initialize() throws Exception;

	/**
	 * Performs stream output handler cleanup.
	 *
	 * @see TNTInputStream#cleanup()
	 */
	void cleanup();

	/**
	 * Handles consumer {@link Thread} initiation in streaming process. May require to create new
	 * {@link com.jkoolcloud.tnt4j.tracker.Tracker} for provided {@link Thread}.
	 * 
	 * @param t
	 *            thread to handle
	 * @throws IllegalStateException
	 *             indicates that tracker created for the thread is not opened and can not record activity data
	 */
	void handleConsumerThread(Thread t) throws IllegalStateException;

	/**
	 * Sets output configuration property.
	 * 
	 * @param name
	 *            property name
	 * @param value
	 *            property value
	 */
	void setProperty(String name, Object value);

	/**
	 * Sets related {@link TNTInputStream} instance.
	 * 
	 * @param inputStream
	 *            related input stream instance
	 */
	void setStream(TNTInputStream<?, ?> inputStream);

	/**
	 * Gets related {@link TNTInputStream} instance.
	 *
	 * @return related input stream instance
	 */
	TNTInputStream<?, ?> getStream();

	/**
	 * Sets output configuration properties collection.
	 *
	 * @param props
	 *            configuration properties to set
	 *
	 * @see #initialize()
	 */
	void setProperties(Collection<Map.Entry<String, String>> props);

	/**
	 * Returns flag indicating whether this output instance is closed.
	 *
	 * @return {@code true} is this output instance is closed, {@code false} - otherwise
	 */
	boolean isClosed();

	/**
	 * Adds defined {@code OutputStreamListener} to stream output listeners list.
	 *
	 * @param listener
	 *            the {@code OutputStreamListener} to be added
	 */
	void addOutputListener(OutputStreamListener listener);

	/**
	 * Checks if this stream output instance has sink reference by provided sink identifier.
	 * 
	 * @param sinkId
	 *            sink identifier
	 * @return {@code true} if this stream output instance has sink reference by provided sink identifier, {@code false}
	 *         - otherwise
	 */
	boolean hasSink(String sinkId);
}
