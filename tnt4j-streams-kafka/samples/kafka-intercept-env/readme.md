* Install Kafka (if not yet)
* Add `tnt4j-streams-kafka-[VERSION]-all.jar` to Kafka class-path:
    * If you can put files into `<KAFKA_INSTALL_DIR>/libs` dir, then just copy it there. 
    * If you wish to have streams lib isolated, then put jar to dedicated directory (e.g. `<KAFKA_INSTALL_DIR>/libs/tnt4j`). Then alter 
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
        for %%i in (%BASE_DIR%\libs\tnt4j\*) do (
            call :concat %%i
        )
        ```
        **NOTE:** there is sample `.bat` file provided next to this manual, use it as reference.
* Alter `<KAFKA_INSTALL_DIR>/config/consumer.properties` by adding:
```properties
interceptor.classes=com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.TNTKafkaCInterceptor
```
* Alter `<KAFKA_INSTALL_DIR>/config/procuder.properties` by adding:
```properties
interceptor.classes=com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.TNTKafkaPInterceptor
```
* Alter `<KAFKA_INSTALL_DIR>/config/tools-log4j.properties` by adding:
```properties
######################## TNT4J ########################

### direct log messages to file ###
log4j.appender.tnt4jAppender=org.apache.log4j.RollingFileAppender
log4j.appender.tnt4jAppender.File=${kafka.logs.dir}/tnt4j-streams.log
log4j.appender.tnt4jAppender.maxFileSize=10MB
log4j.appender.tnt4jAppender.maxBackupIndex=2
log4j.appender.tnt4jAppender.layout=org.apache.log4j.EnhancedPatternLayout
log4j.appender.tnt4jAppender.layout.ConversionPattern=%d{ISO8601} %-5p [%t!%c{1}] - %m%n
log4j.appender.tnt4jAppender.Threshold=TRACE
#log4j.appender.tnt4jAppender.bufferSize=512

### branch for sink written activity entities logger ###
log4j.appender.activities_log=org.apache.log4j.RollingFileAppender
log4j.appender.activities_log.File=${kafka.logs.dir}/tnt4j-streams-activities.log
log4j.appender.activities_log.maxFileSize=10MB
log4j.appender.activities_log.maxBackupIndex=3
log4j.appender.activities_log.layout=org.apache.log4j.EnhancedPatternLayout
log4j.appender.activities_log.layout.ConversionPattern=%m%n
#log4j.appender.activities_log.Threshold=INFO
#log4j.appender.activities_log.bufferSize=512

log4j.logger.com.jkoolcloud.tnt4j.streams=DEBUG, tnt4jAppender
#log4j.logger.com.jkoolcloud.tnt4j.streams=OFF
#### tnt4j API logger ####
#log4j.logger.com.jkoolcloud.tnt4j=DEBUG
#### tnt4j API logger ####
#log4j.logger.com.jkoolcloud.jesl=DEBUG
### if streams are not subject to log ###
log4j.additivity.com.jkoolcloud.tnt4j.streams=false
#### tnt4j API logger ####
#log4j.logger.com.jkoolcloud.tnt4j=DEBUG
#### tnt4j API logger ####
#log4j.logger.com.jkoolcloud.jesl=DEBUG
### Streamed activity entities logger ###
log4j.logger.com.jkoolcloud.tnt4j.streams.activities_log=INFO, activities_log
log4j.additivity.com.jkoolcloud.tnt4j.streams.activities_log=false
```
* Add producer (`kafka-console-producer`) and consumer (`kafka-console-consumer`) JVM system properties, referring TNT4J, interceptors and 
optionally LOG4J configuration files, e.g.:
    * *NIX:
    ```bash
    export KAFKA_OPTS="-Dtnt4j.config=../config/tnt4j_kafka.properties -Dinterceptors.config=../config/interceptors.properties"    
    ```
    * Windows:
    ```cmd
    set KAFKA_OPTS=-Dtnt4j.config=../../config/tnt4j_kafka.properties -Dinterceptors.config=../../config/interceptors.properties
    ```
  or with custom LOG4J configuration:
    * *NIX:
    ```bash
    export KAFKA_OPTS="-Dtnt4j.config=../config/tnt4j_kafka.properties -Dinterceptors.config=../config/interceptors.properties -Dlog4j.configuration=file:../config/my_log4j.properties"    
    ```
    * Windows:
    ```cmd
    set KAFKA_OPTS=-Dtnt4j.config=../../config/tnt4j_kafka.properties -Dinterceptors.config=../../config/interceptors.properties -Dlog4j.configuration="file:../../config/my_log4j.properties"
    ```

* Run Kafka provided console producer/consumer
    * *UIX:
    ```bash
    kafka-console-consumer.sh --consumer.config ../config/consumer.properties --bootstrap-server localhost:9092 --topic tnt4j_streams_kafka_intercept_test_page_visits --from-beginning
    ```    
    ```bash
    kafka-console-producer.sh --producer.config ../config/producer.properties --broker-list localhost:9092 --topic tnt4j_streams_kafka_intercept_test_page_visits
    ```
    * Windows:
    ```cmd
    kafka-console-consumer --consumer.config ../../config/consumer.properties --bootstrap-server localhost:9092 --topic tnt4j_streams_kafka_intercept_test_page_visits --from-beginning
    ```    
    ```cmd
    kafka-console-producer --producer.config ../../config/producer.properties --broker-list localhost:9092 --topic tnt4j_streams_kafka_intercept_test_page_visits
    ```
* If you need to change interceptor events parser configuration:
    * If using default parsers configuration from `tnt4j-streams-kafka-[VERSION]-all.jar` package:
        * Extract `tnt-data-source_kafka_msg_trace.xml` file from `tnt4j-streams-kafka-[VERSION]-all.jar`
        * Make required changes
        * Put changed `tnt-data-source_kafka_msg_trace.xml` file back to `tnt4j-streams-kafka-[VERSION]-all.jar` package (make sure file is 
        not used while repackaging).
    * If you want use custom interceptor events parser configuration:
        * define custom `interceptors.properties` property `messages.tracer.stream.parser` value, having path to your custom interceptor 
        events parser configuration file and your custom major parser name, e.g.: 
        `/usr/me/kafka-interceptors/tnt-data-source_my_kafka_trace.xml#MyKafkaMessageTraceParser`.