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

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.ClusterResource;
import org.apache.kafka.common.TopicPartition;

/**
 * This class defines Kafka message trace events data package.
 * <p>
 * Message trace data shall be collected from Kafka producer/consumer events:
 * <ul>
 * <li>send</li>
 * <li>acknowledge</li>
 * <li>consume</li>
 * <li>commit</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class KafkaTraceEventData {
	/**
	 * Kafka "send" event name.
	 */
	public static final String SEND = "send"; // NON-NLS
	/**
	 * Kafka "acknowledge" event name.
	 */
	public static final String ACK = "acknowledge"; // NON-NLS
	/**
	 * Kafka "consume" event name.
	 */
	public static final String CONSUME = "consume"; // NON-NLS
	/**
	 * Kafka "commit" event name.
	 */
	public static final String COMMIT = "commit"; // NON-NLS

	/////// PRODUCER DATA ///////////////
	// send
	private ProducerRecord<?, ?> producerRecord;
	// ack
	private RecordMetadata recordMetadata;
	private Exception exception;
	private ClusterResource clusterResource;
	/////////////////////////////////////
	/////// CONSUMER DATA ///////////////
	// consume
	private ConsumerRecord<?, ?> consumerRecord;
	// commit
	private TopicPartition topicPartition;
	private OffsetAndMetadata offsetAndMetadata;
	/////////////////////////////////////

	/////// METADATA ////////////////////
	private String type;
	private String signature;
	private String appInfo;
	private String parentId;
	/////////////////////////////////////

	/**
	 * Constructs a new KafkaTraceEventData for Kafka producer {@code "send"} event.
	 *
	 * @param producerRecord
	 *            Kafka producer record instance
	 * @param clientID
	 *            client identifier
	 */
	public KafkaTraceEventData(ProducerRecord<?, ?> producerRecord, String clientID) {
		this.type = SEND;

		this.producerRecord = producerRecord;
		this.appInfo = clientID;
	}

	/**
	 * Constructs a new KafkaTraceEventData for Kafka producer {@code "acknowledge"} event.
	 *
	 * @param recordMetadata
	 *            record metadata
	 * @param exception
	 *            exception instance
	 * @param clusterResource
	 *            cluster resource descriptor
	 * @param clientID
	 *            client identifier
	 */
	public KafkaTraceEventData(RecordMetadata recordMetadata, Exception exception, ClusterResource clusterResource,
			String clientID) {
		this.type = ACK;

		this.recordMetadata = recordMetadata;
		this.exception = exception;
		this.clusterResource = clusterResource;
		this.appInfo = clientID;
	}

	/**
	 * Constructs a new KafkaTraceEventData for Kafka consumer {@code "consume"} event.
	 *
	 * @param consumerRecords
	 *            consumer records
	 * @param clientID
	 *            client identifier
	 */
	public KafkaTraceEventData(ConsumerRecord<?, ?> consumerRecords, String clientID) {
		this.type = CONSUME;

		this.consumerRecord = consumerRecords;
		this.appInfo = clientID;
	}

	/**
	 * Constructs a new KafkaTraceEventData for Kafka producer {@code "commit"} event.
	 *
	 * @param topicPartition
	 *            topic partition descriptor
	 * @param offsetAndMetadata
	 *            offset and metadata
	 * @param clientID
	 *            client identifier
	 */
	public KafkaTraceEventData(TopicPartition topicPartition, OffsetAndMetadata offsetAndMetadata, String clientID) {
		this.type = COMMIT;

		this.topicPartition = topicPartition;
		this.offsetAndMetadata = offsetAndMetadata;
		this.appInfo = clientID;
	}

	/**
	 * Returns event type.
	 *
	 * @return event type
	 */
	public String type() {
		return type;
	}

	/**
	 * Sets event type.
	 * 
	 * @param type
	 *            event type
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Returns event signature string.
	 *
	 * @return event signature string
	 */
	public String signature() {
		return signature;
	}

	/**
	 * Sets event signature string.
	 * 
	 * @param signature
	 *            event signature string
	 */
	public void setSignature(String signature) {
		this.signature = signature;
	}

	/**
	 * Returns application information.
	 * 
	 * @return application information
	 */
	public String appInfo() {
		return appInfo;
	}

	/**
	 * Sets application information.
	 *
	 * @param appInfo
	 *            application information
	 */
	public void setAppInfo(String appInfo) {
		this.appInfo = appInfo;
	}

	/**
	 * Returns parent object identifier.
	 * 
	 * @return parent object identifier
	 */
	public String parentId() {
		return parentId;
	}

	/**
	 * Sets parent object identifier.
	 *
	 * @param parentId
	 *            parent object identifier
	 */
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	/**
	 * Returns Kafka producer record instance bound to {@code "send"} event.
	 *
	 * @return producer record instance
	 */
	public ProducerRecord<?, ?> getProducerRecord() {
		return producerRecord;
	}

	/**
	 * Returns Kafka consumer record instance bound to {@code "consume"} event.
	 *
	 * @return consumer record instance
	 */
	public ConsumerRecord<?, ?> getConsumerRecord() {
		return consumerRecord;
	}

	/**
	 * Returns Kafka producer metadata bound to {@code "acknowledge"} event.
	 *
	 * @return record metadata
	 */
	public RecordMetadata getRecordMetadata() {
		return recordMetadata;
	}

	/**
	 * Returns Kafka producer exception bound to {@code "acknowledge"} event.
	 *
	 * @return exception instance
	 */
	public Exception getException() {
		return exception;
	}

	/**
	 * Returns Kafka producer cluster resource descriptor bound to {@code "acknowledge"} event.
	 *
	 * @return cluster resource descriptor
	 */
	public ClusterResource getClusterResource() {
		return clusterResource;
	}

	/**
	 * Returns Kafka client topic partition descriptor bound to {@code "commit"} event.
	 *
	 * @return topic partition descriptor
	 */
	public TopicPartition getTopicPartition() {
		return topicPartition;
	}

	/**
	 * Returns Kafka client offset and metadata bound to {@code "commit"} event.
	 *
	 * @return offset and metadata
	 */
	public OffsetAndMetadata getOffsetAndMetadata() {
		return offsetAndMetadata;
	}
}
