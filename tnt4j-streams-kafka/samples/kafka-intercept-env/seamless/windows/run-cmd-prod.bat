set KAFKA_HOME=D:\tmp\kafka_install\kafka_2.12-2.4.1_
rem set CLASSPATH=D:\JAVA\PROJECTS\Nastel\jKoolLLC\tnt4j-streams\tnt4j-streams-kafka\target\tnt4j-streams-kafka-1.12.0-SNAPSHOT-all.jar
rem set KAFKA_OPTS=-Dtnt4j.config="../config/tnt4j.properties" -Dinterceptors.config="../config/interceptorsP.properties" -Dfile.encoding=UTF-8
rem set KAFKA_LOG4J_OPTS=-Dlog4j.configuration=file:../config/tools-log4j.properties

rem start "Trace-Commands-Producer" %KAFKA_HOME%\bin\windows\kafka-console-producer --producer.config ../config/producer.properties --broker-list localhost:9092 --topic TNT_TRACE_CONFIG_TOPIC
start "Trace-Commands-Producer" %KAFKA_HOME%\bin\windows\kafka-console-producer --producer.config %KAFKA_HOME%\config\producer.properties --broker-list localhost:9092 --topic TNT_TRACE_CONFIG_TOPIC
