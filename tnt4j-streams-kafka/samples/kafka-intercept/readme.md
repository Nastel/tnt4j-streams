## Interceptors sample configuration

### Common

If you are willing to run this sample having Kafka server running locally (default host/port is `localhost:9092`), the only thing you need 
to configure is set your jKool repository token in TNT4J configuration (file `tnt4j.properties`):
```properties
event.sink.factory.Token: YOUR-TOKEN
```
All the rest configuration is for advanced use cases: different kafka server host/port, different metrics reporting topic name, etc.

**NOTE:** check if your `tnt4j.properties` file has stanza:
```properties
source: com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.metrics
```
if there is no such, it must be you have an old build of TNT4J-Streams or you have not built `tnt4j-streams-kafka` module.

### Advanced

#### Interceptors binding

To bind interceptors to any producer/consumer alter configuration properties:
* producer: 
```properties
interceptor.classes=com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.TNTKafkaPInterceptor
```

* consumer:
```properties
interceptor.classes=com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.TNTKafkaCInterceptor
```
#### Interceptors collected metrics streaming

To configure interceptors collected metrics streaming to dedicated Kafka topic use TNT4J configuration stanza from file 
`./config/intercept/tnt4j_kafka.properties`. This dedicated configuration stanza shall be merged into TNT4J configuration file 
(e.g. `./config/tnt4j.properties`), used by TNT4J-Streams over system property `tnt4j.config`. When building TNT4J-Streams deliverable 
package with `tnt4j-streams-kafka` module included - this merge is performed automatically.

* Define stanza for source `com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.metrics` (without this metrics will not be 
reported to dedicated Kafka topic):
    ```properties
      source: com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.metrics
    ```
* Set TNT4J to use `KafkaSink` to send interceptors collected statistics to dedicated Kafka topic (e.g. `tnt4j_kafka_interceptor_metrics`):
    * referring Kafka producer configuration file
    ```properties
      #### Kafka event sink factory configuration ####
      event.sink.factory: com.jkoolcloud.tnt4j.sink.impl.kafka.KafkaEventSinkFactory
      event.sink.factory.topic: tnt4j_kafka_interceptor_metrics
      event.sink.factory.propFile: ../config/tnt4j-kafka.properties
      #### Kafka event sink factory configuration end ####
    ```
    * **OR** defining producer properties under sink configuration
    ```properties
      #### Kafka event sink factory configuration ####
      event.sink.factory: com.jkoolcloud.tnt4j.sink.impl.kafka.KafkaEventSinkFactory
      event.sink.factory.topic: tnt4j_kafka_interceptor_metrics
      event.sink.factory.bootstrap.servers: localhost:9092
      event.sink.factory.acks: all
      event.sink.factory.retries: 0
      event.sink.factory.linger.ms: 1
      event.sink.factory.buffer.memory: 33554432
      event.sink.factory.key.serializer: org.apache.kafka.common.serialization.StringSerializer
      event.sink.factory.value.serializer: org.apache.kafka.common.serialization.StringSerializer
      #### Kafka event sink factory configuration end ####
    ```

#### Interceptors configuration

To configure interceptors use sample file `./config/intercept/interceptors.properties`. Configuration properties are:
* `metrics.report.period` - period (in seconds) of Kafka interceptors (and JMX) collected metrics reporting to dedicated Kafka topic. 
Default value - `30`.
* `metrics.report.delay` - delay (in seconds) before first metrics reporting is invoked. If not defined, it is equal to 
`metrics.report.period`.
* `messages.tracer.trace` - messages tracing options, allowing to enable/disable tracing of all, none or desired set of message 
producer/consumer intercepted operations:
    * `send` - producer send operation
    * `ack` - producer acknowledge operation
    * `consume` - consumer consume operation
    * `commit` - consumer commit operation
    * `all` or `true` - capture all producer/consumer operations
    * `none` or `false` - do not capture anything

  Default value - `none`.
* `messages.tracer.stream.parser` - parser reference, used to parse Kafka interceptors trace events (of class 
`com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.trace.KafkaTraceEventData`). Reference is defined using pattern 
`parsers_cfg_file_name_path#parserName`, where:
    * `parsers_cfg_file_path` - is parsers configuration file path (absolute or relative to interceptors configuration file). (Optional). 
    Default value - `tnt-data-source_kafka_msg_trace.xml`. 
    * `parserName` - primary parser name from referenced parsers configuration file. Default value - `KafkaTraceParser`.

  To define only parser name from file under default parsers configuration file path - use `#parserName` pattern. Default value - 
  `tnt-data-source_kafka_msg_trace.xml#KafkaTraceParser`.
* `messages.tracer.stream.name` - Kafka interceptors trace events stream name.

##### Kafka messages trace configuration over file

When messaging tracing is enabled, it generates quite noticeable overhead and you're able to control tracing process using topic: 
`TNT_TRACE_CONFIG_TOPIC`. To read (poll) messages tracing handling commands from topic `TNT_TRACE_CONFIG_TOPIC`, set consumer configuration 
properties in `interceptor.properties` file.

Use standard Kafka consumer keys with prefix `messages.tracer.kafka.`, e.g:
```properties
messages.tracer.kafka.group.id=kafka-x-ray-interceptors
messages.tracer.kafka.bootstrap.servers=localhost:9092
messages.tracer.kafka.client.id=kafka-x-ray-intercept-test-producer
```

Provided configuration should be OK to start with. In the distributed systems adjustments should be made.

`messages.tracer.kafka.group.id` must be unique, in order to all interceptors get the commands, otherwise results will be unpredictable.
`messages.tracer.kafka.client.id` should not duplicate with other consumers in the same JVM, as you'll get many warnings about MBeans not 
being registered.

All other `messages.tracer.kafka.<KAFKA_CONUMER_PROP_NAME>` options are subject depending on your environment configuration.

To enable trace of all messages from any topic, set (one of below): 
```properties
# to trace all intercepted message producer/consumer operations
messages.tracer.trace=all
# to disable messages tracing
messages.tracer.trace=none
# to trace only message producer operations
messages.tracer.trace=send, ack
# to trace only message consumer operations
messages.tracer.trace=commit, consume
# to trace only message producer 'send' and consumer 'consume' operations
messages.tracer.trace=send, consume
# to trace all listed message producer/consumer operations
messages.tracer.trace=send, ack, consume, commit
```

##### Kafka trace control topic commands

To control tracing you should send English-like command to control topic `TNT_TRACE_CONFIG_TOPIC`. 
E.g. if you wish to use `kafka-console-producer` provided with Apache Kafka, run command:
```cmd
kafka-console-producer --broker-list localhost:9092,localhost:9093 --topic TNT_TRACE_CONFIG_TOPIC
```

* `trace on` - will enable trace
* `trace off` - will disable trace
* `trace until 2017-11-10 12:54` - will enable trace until 10/11/2017 12:54 am
* `trace between 2017-11-10 12:54 2018-11-10 12:54` - will enable trace after 10/11/2017 12:54 am and disable at 10/11/2018 12:54am
* `trace 100 messages` - will enable trace and count for 100 messages before disabling it
* `trace 100 messages topic TNT4JStreams` - do the same for specified topic `TNT4JStreams`
* `trace on topic TNT4JStreams` - will enable trace for specified topic `TNT4JStreams`

#### Kafka broker configuration

Default Kafka broker is configured to be on local machine (default host/port is `localhost:9092`). If this does not meet your environment, 
set it to match yours in those files:
* `consumer.properties` file property `bootstrap.servers` 
* `producer.properties` file property `bootstrap.servers`
* `tnt4j.properties` file property `event.sink.factory.bootstrap.servers`
* `tnt-data-source.xml` file Kafka stream property `bootstrap.servers`

## Running interceptors sample

**NOTE:** before running this sample, ensure your Kafka server instance is running.

### Using run scripts

1. run `runMetricsStreaming.bat(.sh)` - starts stream reading Kafka topic containing interceptors collected metrics and sending to jKool. 
2. run `runInterceptionsTest.bat(.sh)` - starts producer and consumer test applications, having bound interceptors to collect Kafka metrics 
and post it to Kafka stream listened topic.

### Using JUnit (for advanced users, like developers)

1. To run interceptions sample run JUnit `InterceptionsManagerTest.interceptionsTest`. It runs sample producer/consumer app with bound 
interceptors.

    **NOTE:** JUnit test now is configured to use working dir `./tnt4j-streams/tnt4j-streams-kafka`

    InterceptionsManager has bound two InterceptionReporters: 
    * tnt - to collect and post metrics to Kafka topic
    * stream - to stream intercepted data to jKool

2. To stream Kafka topic contained metrics to jKool, run `runMetricsStreaming.bat(.sh)` script.
