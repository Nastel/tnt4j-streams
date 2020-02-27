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

import java.util.Collection;
import java.util.Map;

import com.jkoolcloud.tnt4j.streams.inputs.InputStreamListener;
import com.jkoolcloud.tnt4j.streams.inputs.StreamTasksListener;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;

/**
 * This interface defines basic builder functions of streams run environment context.
 * 
 * @version $Revision: 1 $
 */
public interface StreamsBuilder {
	/**
	 * Builds collection of streams to be run.
	 * 
	 * @return collection of streams
	 */
	Collection<TNTInputStream<?, ?>> getStreams();

	/**
	 * Returns stream listener instance.
	 * 
	 * @return stream listener instance
	 */
	InputStreamListener getStreamListener();

	/**
	 * Returns streams tasks listener instance.
	 * 
	 * @return streams tasks listener instance
	 */
	StreamTasksListener getTasksListener();

	/**
	 * Returns map of data source properties.
	 * 
	 * @return map of data source properties
	 */
	Map<String, String> getDataSourceProperties();
}
