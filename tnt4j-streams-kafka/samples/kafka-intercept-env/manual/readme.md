### How to manually integrate Kafka-X-Ray into your Kafka environment

1. Install Kafka (if not yet), but **NOTE** this guide written to perform actions on a clean instance of Kafka installation - if you already
   made any changes to your Kafka environment, it **may require some improvisation**.
1. Add `tnt4j-streams-kafka-[VERSION]-all.jar` to Kafka class-path:
    1. If you can put files into `<KAFKA_INSTALL_DIR>/libs` dir, then just copy it there.
    1. If you wish to have streams lib isolated, then put jar to dedicated directory (e.g. `<KAFKA_INSTALL_DIR>/libs/tnt4j`). Then alter
       `<KAFKA_INSTALL_DIR>/bin/kafka-run-class` script file:
        * *NIX: `<KAFKA_INSTALL_DIR>/bin/kafka-run-class.sh` by adding section after `# classpath addition for release` section:
        ```bash
        # classpath addition for tnt4j
        for file in "$base_dir"/libs/tnt4j/*;
        do
          if should_include_file "$file"; then
            CLASSPATH="$CLASSPATH":"$file"
          fi
        done
        ```
        * Windows: `<KAFKA_INSTALL_DIR>/bin/windows/kafka-run-class.bat` by adding section after `rem Classpath addition for release`
          section:
        ```cmd
        rem Classpath addition for tnt4j
        for %%i in ("%BASE_DIR%\libs\tnt4j\*") do (
            call :concat "%%i"
        )
        ```
1. Alter consumer runner script file (`<KAFKA_INSTALL_DIR>/bin/kafka-console-consumer`) by adding/amending `KAFKA_OPTS` environment variable
   defining JVM system properties, referring TNT4J, interceptors and optionally LOG4J configuration files, e.g. (**NOTE**: add `KAFKA_OPTS`
   right after line defining environment variable `KAFKA_HEAP_OPTS`):
    * *NIX:
    ```bash
    export KAFKA_OPTS="-Dtnt4j.config=../config/tnt4j.properties -Dinterceptors.config=../config/interceptorsC.properties"
    ```
    * Windows:
    ```cmd
    set KAFKA_OPTS=-Dtnt4j.config="../../config/tnt4j.properties" -Dinterceptors.config="../../config/interceptorsC.properties"
    ```
   or optionally with custom LOG4J configuration:
    * *NIX:
    ```bash
    export KAFKA_OPTS="-Dtnt4j.config=../config/tnt4j.properties -Dinterceptors.config=../config/interceptorsC.properties -Dlog4j2.configurationFile=file:../config/my_log4j2.xml"
    ```
    * Windows:
    ```cmd
    set KAFKA_OPTS=-Dtnt4j.config="../../config/tnt4j.properties" -Dinterceptors.config="../../config/interceptorsC.properties" -Dlog4j2.configurationFile="file:../../config/my_log4j2.xml"
    ```
1. Alter producer runner script file (`<KAFKA_INSTALL_DIR>/bin/kafka-console-producer`) by adding/amending `KAFKA_OPTS` environment variable
   defining JVM system properties, referring TNT4J, interceptors and optionally LOG4J configuration files, e.g. (**NOTE**: add `KAFKA_OPTS`
   right after line defining environment variable `KAFKA_HEAP_OPTS`):
    * *NIX:
    ```bash
    export KAFKA_OPTS="-Dtnt4j.config=../config/tnt4j.properties -Dinterceptors.config=../config/interceptorsP.properties"
    ```
    * Windows:
    ```cmd
    set KAFKA_OPTS=-Dtnt4j.config="../../config/tnt4j.properties" -Dinterceptors.config="../../config/interceptorsP.properties"
    ```
   or optionally with custom LOG4J configuration:
    * *NIX:
    ```bash
    export KAFKA_OPTS="-Dtnt4j.config=../config/tnt4j.properties -Dinterceptors.config=../config/interceptorsP.properties -Dlog4j2.configurationFile=file:../config/my_log4j2.xml"
    ```
    * Windows:
    ```cmd
    set KAFKA_OPTS=-Dtnt4j.config="../../config/tnt4j.properties" -Dinterceptors.config="../../config/interceptorsP.properties" -Dlog4j2.configurationFile="file:../../config/my_log4j2.xml"
    ```
1. Alter `<KAFKA_INSTALL_DIR>/config/consumer.properties` by adding:
    ```properties
    client.id=kafka-x-ray-intercept-test-consumer
    interceptor.classes=com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.TNTKafkaCInterceptor
    ```
1. Alter `<KAFKA_INSTALL_DIR>/config/procuder.properties` by adding:
    ```properties
    # NOTE: when using with Kafka console-producer, value will be reset to 'console-producer' (turns out to be hardcoded)
    client.id=kafka-x-ray-intercept-test-producer
    interceptor.classes=com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.TNTKafkaPInterceptor
    ```
1. Alter `<KAFKA_INSTALL_DIR>/config/tools-log4j.properties` by adding:
    ```properties
    ######################## TNT4J ########################

    ### direct log messages to file ###
    log4j.appender.tnt4jAppender=org.apache.log4j.RollingFileAppender
    log4j.appender.tnt4jAppender.File=${kafka.logs.dir}/tnt4j-streams.log
    log4j.appender.tnt4jAppender.maxFileSize=10MB
    log4j.appender.tnt4jAppender.maxBackupIndex=2
    log4j.appender.tnt4jAppender.layout=org.apache.log4j.EnhancedPatternLayout
    log4j.appender.tnt4jAppender.layout.ConversionPattern=%d{ISO8601} %-5p [%t!%c{1}] - %m%n

    ### branch for sink written activity entities logger ###
    log4j.appender.activities_log=org.apache.log4j.RollingFileAppender
    log4j.appender.activities_log.File=${kafka.logs.dir}/tnt4j-streams-activities.log
    log4j.appender.activities_log.maxFileSize=10MB
    log4j.appender.activities_log.maxBackupIndex=3
    log4j.appender.activities_log.layout=org.apache.log4j.EnhancedPatternLayout
    log4j.appender.activities_log.layout.ConversionPattern=%m%n

    log4j.logger.com.jkoolcloud.tnt4j.streams=DEBUG, tnt4jAppender
    #log4j.logger.com.jkoolcloud.tnt4j.streams=OFF
    ### if streams are not subject to be added to main kafka log, streams will be logged to dedicated log only ###
    log4j.additivity.com.jkoolcloud.tnt4j.streams=false
    #### tnt4j API logger ####
    #log4j.logger.com.jkoolcloud.tnt4j=DEBUG
    #### jesl API logger ####
    #log4j.logger.com.jkoolcloud.jesl=DEBUG
    ### Streamed activity entities logger ###
    log4j.logger.com.jkoolcloud.tnt4j.streams.activities_log=INFO, activities_log
    log4j.additivity.com.jkoolcloud.tnt4j.streams.activities_log=false
    ```
   In case Kafka switched to `log4j2` see [tools-log4j2.xml](./../seamless/config/tools-log4j2.xml) as a sample configuration.
1. Copy your `tnt4j.properties` file (one having your JKool token defined) into `<KAFKA_INSTALL_DIR>/config` directory. Also, check if your
   `tnt4j.properties` file contains stanza for source `com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.metrics`. If not -
   append it with this one:
    ```properties
    # Stanza used for TNT4J-Streams-Kafka interceptors reporters sources
    {
    	source: com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.metrics
    	source.factory: com.jkoolcloud.tnt4j.source.SourceFactoryImpl
    	source.factory.APPL: Kafka-X-Ray-Interceptors-DEV
    	source.factory.DATACENTER: SLABS
    	source.factory.RootFQN: APPL=?#RUNTIME=?#SERVER=?#NETADDR=?#DATACENTER=?
    	source.factory.RootSSN: kafka-x-ray-metrics

    	tracker.factory: com.jkoolcloud.tnt4j.tracker.DefaultTrackerFactory
    	dump.sink.factory: com.jkoolcloud.tnt4j.dump.DefaultDumpSinkFactory
    	tracker.default.snapshot.category: kafka-x-ray-metrics-snapshot

    	# event sink configuration: destination and data format
    	# NOTE: IT IS NOT RECOMMENDED TO USE BufferedEventSinkFactory (asynchronous sinks) WITH STREAMS. Streams and sinks are meant to
    	# act in sync, especially when sink (e.g., 'JKCloud', 'Mqtt', 'Kafka') consumer uses network communication.
    	###event.sink.factory: com.jkoolcloud.tnt4j.sink.impl.BufferedEventSinkFactory
    	###event.sink.factory.PooledLoggerFactory: com.jkoolcloud.tnt4j.sink.impl.PooledLoggerFactoryImpl

    	#### Kafka event sink factory configuration ####
    	event.sink.factory: com.jkoolcloud.tnt4j.sink.impl.kafka.KafkaEventSinkFactory
    	event.sink.factory.topic: tnt4j-kafka-interceptor-metrics
    	## You can configure sink used Kafka Producer by providing external configuration file path
    	# event.sink.factory.propFile: ../config/tnt4j-kafka.properties
    	## Or right there within sink factory configuration
    	event.sink.factory.bootstrap.servers: localhost:9092
    	event.sink.factory.acks: all
    	event.sink.factory.retries: 0
    	event.sink.factory.linger.ms: 1
    	event.sink.factory.buffer.memory: 33554432
    	event.sink.factory.key.serializer: org.apache.kafka.common.serialization.StringSerializer
    	event.sink.factory.value.serializer: org.apache.kafka.common.serialization.StringSerializer
    	event.sink.factory.client.id: kafka-x-ray-metrics-reporter
    	#### Kafka event sink factory configuration end ####

    	event.formatter: com.jkoolcloud.tnt4j.format.LevelingJSONFormatter
    	#event.formatter.Newline: true
    	; Configures special numeric values handling. Can be one of: SUPPRESS, ENQUOTE, MAINTAIN. Default value: SUPPRESS
    	event.formatter.SpecNumbersHandling: SUPPRESS
    	event.formatter.Level: 2
    	event.formatter.KeyReplacements: ","->";" "."->"_" "\\"->"_"

    	tracking.selector: com.jkoolcloud.tnt4j.selector.DefaultTrackingSelector
    	tracking.selector.Repository: com.jkoolcloud.tnt4j.repository.FileTokenRepository
    }
    ```
1. Create file `<KAFKA_INSTALL_DIR>/config/interceptorsC.properties` and fill it with these properties:
    ```properties
    ### Kafka metrics reporting configuration
    # Use value 0 to disable metrics reporting
    metrics.report.period=30
    metrics.report.delay=10

    ### Kafka messages tracing configuration
    # Possible values: all, none, consume, commit
    messages.tracer.trace=all
    # Default parser reference is from -all.jar resource
    messages.tracer.stream.parser=tnt-data-source_kafka_msg_trace_custom.xml#KafkaTraceParser
    messages.tracer.stream.name=KafkaConsumerXXXStream
    ## Additional stream configuration properties
    # Add any stream property prefixed by 'messages.tracer.stream.'  
    #messages.tracer.stream.BuildSourceFQNFromStreamedData=true
    messages.tracer.stream.SourceFQN=APPL=${ApplName}#SERVER=${ServerName}#GEOADDR=${Location}
    messages.tracer.stream.RetryStateCheck=3
    messages.tracer.stream.RetryInterval=5
    ## Trace configuration topic consumer properties
    messages.tracer.cfg.topic=tnt4j-trace-config-topic
    messages.tracer.kafka.bootstrap.servers=localhost:9092
    messages.tracer.kafka.group.id=kafka-x-ray-trace-config-consumers
    messages.tracer.kafka.client.id=kafka-x-ray-trace-config-listener-c
    ```
   **NOTE:** see [Interceptors configuration section](../../kafka-intercept/readme.md#interceptors-configuration) for more details about
   interceptors configuration.
1. Create file `<KAFKA_INSTALL_DIR>/config/interceptorsP.properties` and fill it with these properties:
    ```properties
    ### Kafka metrics reporting configuration
    # Use value 0 to disable metrics reporting
    metrics.report.period=30
    metrics.report.delay=10

    ### Kafka messages tracing configuration
    # Possible values: all, none, send, ack
    messages.tracer.trace=all
    # Default parser reference is from -all.jar resource
    messages.tracer.stream.parser=tnt-data-source_kafka_msg_trace_custom.xml#KafkaTraceParser
    messages.tracer.stream.name=KafkaProducerXXXStream
    ## Additional stream configuration properties
    # Add any stream property prefixed by 'messages.tracer.stream.'   
    #messages.tracer.stream.BuildSourceFQNFromStreamedData=true
    messages.tracer.stream.SourceFQN=APPL=${ApplName}#SERVER=${ServerName}#GEOADDR=${Location}
    messages.tracer.stream.RetryStateCheck=3
    messages.tracer.stream.RetryInterval=5
    ## Trace configuration topic consumer properties
    messages.tracer.cfg.topic=tnt4j-trace-config-topic
    messages.tracer.kafka.bootstrap.servers=localhost:9092
    messages.tracer.kafka.group.id=kafka-x-ray-trace-config-consumers
    messages.tracer.kafka.client.id=kafka-x-ray-trace-config-listener-p
    ```
   **NOTE:** see [Interceptors configuration section](../../kafka-intercept/readme.md#interceptors-configuration) for more details about
   interceptors configuration.
1. (Optional) In case you may want to change Kafka messages trace parser configuration (fields naming/mapping), we would recommend you to
   copy [Kafka trace messages parser](../../kafka-intercept/tnt-data-source_kafka_msg_trace.xml) as
   `<KAFKA_INSTALL_DIR>/config/tnt-data-source_kafka_msg_trace_custom.xml` to have it easily accessible and editable in your Kafka
   environment.

   By default this file resides inside `tnt4j-streams-kafka-[VERSION]-all.jar` and is used for traces messages parsing if interceptors
   configuration does not define any other parser reference.
1. Run Kafka provided console producer/consumer:
    * *UIX:
    ```bash
    exec kafka-console-consumer.sh --consumer.config ../config/consumer.properties --bootstrap-server localhost:9092 --topic tx-topic
    ```
    ```bash
    exec kafka-console-producer.sh --producer.config ../config/producer.properties --broker-list localhost:9092 --topic tx-topic
    ```
    * Windows:
    ```cmd
    start kafka-console-consumer --consumer.config ../../config/consumer.properties --bootstrap-server localhost:9092 --topic tx-topic
    ```
    ```cmd
    start kafka-console-producer --producer.config ../../config/producer.properties --broker-list localhost:9092 --topic 
    ```
   **NOTE:** as an example you may refer script [file](./additions/windows/start-cons_prod.bat).
1. (Optional) To configure Kafka messages trace interceptions at interceptors runtime, run configuration topic `tnt4j-trace-config-topic`
   producer:
    * *NIX:
    ```bash
    exec kafka-console-producer.sh --producer.config ../config/producer.properties --broker-list localhost:9092 --topic tnt4j-trace-config-topic
    ```
    * Windows:
    ```cmd
    start kafka-console-producer --producer.config ../../config/producer.properties --broker-list localhost:9092 --topic tnt4j-trace-config-topic
    ```
   **NOTE:** as an example you may refer script [file](./additions/windows/start-cmd-prod.bat).

   **NOTE:** see [Kafka trace control topic commands section](../../kafka-intercept/readme.md#kafka-trace-control-topic-commands) for more
   details on trace configuration commands.
1. Run `Kafka-X-Ray` produced metrics consumer stream by executing:
    * *NIX: [../../kafka-intercept/runMetricsStreaming.sh](../../kafka-intercept/runMetricsStreaming.sh)
    * Windows: [../../kafka-intercept/runMetricsStreaming.bat](../../kafka-intercept/runMetricsStreaming.bat)