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

package com.jkoolcloud.tnt4j.streams.custom.interceptors.kafka;

import java.io.Closeable;
import java.util.Map;

import org.apache.kafka.common.ClusterResourceListener;

/**
 * This interface defines operations commonly used by TNT4J-Streams Kafka interceptors.
 *
 * @version $Revision: 1 $
 */
public interface TNTKafkaInterceptor extends Closeable, ClusterResourceListener {

	/**
	 * Returns configuration properties map.
	 *
	 * @return configuration properties map
	 */
	Map<String, ?> getConfig();
}
