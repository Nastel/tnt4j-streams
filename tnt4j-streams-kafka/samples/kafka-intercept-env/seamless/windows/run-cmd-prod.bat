set KAFKA_HOME=C:\kafka\kafka_2.12-2.4.1

start "Trace-Commands-Producer" %KAFKA_HOME%\bin\windows\kafka-console-producer --producer.config %KAFKA_HOME%\config\producer.properties --broker-list localhost:9092 --topic tnt4j-trace-config-topic
