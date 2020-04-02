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

package com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors;

import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.ClusterResource;
import org.apache.kafka.common.TopicPartition;

/**
 * TNT4J-Streams Kafka consumer interceptor implementation.
 *
 * @version $Revision: 1 $
 */
public class TNTKafkaCInterceptor implements ConsumerInterceptor<Object, Object>, TNTKafkaInterceptor {

	private ClusterResource clusterResource;
	private Map<String, ?> configs;
	private String groupId = "";
	private String clientId = "";

	private InterceptionsManager iManager;

	/**
	 * Constructs a new TNTKafkaCInterceptor.
	 */
	public TNTKafkaCInterceptor() {
		iManager = InterceptionsManager.getInstance();
		iManager.bindReference(this);
	}

	@Override
	public ConsumerRecords<Object, Object> onConsume(ConsumerRecords<Object, Object> consumerRecords) {
		return iManager.consume(this, consumerRecords, clusterResource);
	}

	@Override
	public void onCommit(Map<TopicPartition, OffsetAndMetadata> map) {
		iManager.commit(this, map, clusterResource);
	}

	@Override
	public void close() {
		iManager.unbindReference(this);
	}

	@Override
	public void configure(Map<String, ?> configs) {
		this.configs = configs;
		Object cfgValue = configs.get(ConsumerConfig.GROUP_ID_CONFIG);
		if (cfgValue instanceof String) {
			groupId = (String) cfgValue;
		}
		cfgValue = configs.get(ConsumerConfig.CLIENT_ID_CONFIG);
		if (cfgValue instanceof String) {
			clientId = (String) cfgValue;
		}
	}

	@Override
	public void onUpdate(ClusterResource clusterResource) {
		this.clusterResource = clusterResource;
	}

	@Override
	public Map<String, ?> getConfig() {
		return configs;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("TNTKafkaCInterceptor{"); // NON-NLS
		sb.append("clusterResource=").append(clusterResource); // NON-NLS
		sb.append(", configs=").append(configs); // NON-NLS
		sb.append(", clientId='").append(clientId).append('\''); // NON-NLS
		sb.append(", groupId='").append(groupId).append('\''); // NON-NLS
		sb.append('}');

		return sb.toString();
	}
}
