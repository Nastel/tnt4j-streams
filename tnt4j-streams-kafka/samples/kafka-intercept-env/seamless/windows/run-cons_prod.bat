set KAFKA_HOME=C:\kafka\kafka_2.12-2.4.1
set CLASSPATH=../../../../tnt4j-streams-kafka-1.12.2-all.jar
set KAFKA_LOG4J_OPTS=-Dlog4j.configuration=file:../config/tools-log4j.properties

set KAFKA_OPTS=-Dtnt4j.config="../config/tnt4j.properties" -Dinterceptors.config="../config/interceptorsC.properties" -Dfile.encoding=UTF-8
start "Kafka-Console-Consumer" %KAFKA_HOME%\bin\windows\kafka-console-consumer --consumer.config ../config/consumer.properties --bootstrap-server localhost:9092 --topic tx-topic
timeout /T 10
set KAFKA_OPTS=-Dtnt4j.config="../config/tnt4j.properties" -Dinterceptors.config="../config/interceptorsP.properties" -Dfile.encoding=UTF-8
start "Kafka-Console-Producer" %KAFKA_HOME%\bin\windows\kafka-console-producer --producer.config ../config/producer.properties --broker-list localhost:9092 --topic tx-topic
