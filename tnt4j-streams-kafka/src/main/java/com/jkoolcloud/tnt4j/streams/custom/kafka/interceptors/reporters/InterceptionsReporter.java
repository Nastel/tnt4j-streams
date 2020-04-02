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

package com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters;

import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.ClusterResource;
import org.apache.kafka.common.TopicPartition;

import com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.TNTKafkaCInterceptor;
import com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.TNTKafkaPInterceptor;

/**
 * This interface defines operations commonly used by TNT4J-Streams Kafka interceptor reporters.
 *
 * @version $Revision: 1 $
 */
public interface InterceptionsReporter {
	/**
	 * Notifies reporter when Kafka producer interceptor has invoked
	 * {@link org.apache.kafka.clients.producer.ProducerInterceptor#onSend(org.apache.kafka.clients.producer.ProducerRecord)}
	 * event.
	 *
	 * @param interceptor
	 *            interceptor instance invoking send
	 * @param producerRecord
	 *            producer record to be sent
	 * @param clusterResource
	 *            cluster resource metadata records where sent to
	 */
	void send(TNTKafkaPInterceptor interceptor, ProducerRecord<Object, Object> producerRecord,
			ClusterResource clusterResource);

	/**
	 * Notifies reporter when Kafka producer interceptor has invoked
	 * {@link org.apache.kafka.clients.producer.ProducerInterceptor#onAcknowledgement(org.apache.kafka.clients.producer.RecordMetadata, Exception)}
	 * event.
	 *
	 * @param interceptor
	 *            interceptor instance invoking acknowledge
	 * @param recordMetadata
	 *            sent message record metadata
	 * @param e
	 *            occurred exception
	 * @param clusterResource
	 *            cluster resource metadata records where sent to
	 */
	void acknowledge(TNTKafkaPInterceptor interceptor, RecordMetadata recordMetadata, Exception e,
			ClusterResource clusterResource);

	/**
	 * Notifies reporter when Kafka consumer interceptor has invoked
	 * {@link org.apache.kafka.clients.consumer.ConsumerInterceptor#onConsume(org.apache.kafka.clients.consumer.ConsumerRecords)}
	 * event.
	 *
	 * @param interceptor
	 *            interceptor instance invoking consume
	 * @param consumerRecords
	 *            consumed records collection
	 * @param clusterResource
	 *            cluster resource metadata records where consumed from
	 */
	void consume(TNTKafkaCInterceptor interceptor, ConsumerRecords<Object, Object> consumerRecords,
			ClusterResource clusterResource);

	/**
	 * Notifies reporter when Kafka consumer interceptor has invoked
	 * {@link org.apache.kafka.clients.consumer.ConsumerInterceptor#onCommit(java.util.Map)} event.
	 *
	 * @param interceptor
	 *            interceptor instance invoking commit
	 * @param map
	 *            committed records topics and messages map
	 * @param clusterResource
	 *            cluster resource metadata records where consumed from
	 */
	void commit(TNTKafkaCInterceptor interceptor, Map<TopicPartition, OffsetAndMetadata> map,
			ClusterResource clusterResource);

	/**
	 * Notifies reporter that all interceptors has been closed and reporter should shutdown.
	 */
	void shutdown();
}
