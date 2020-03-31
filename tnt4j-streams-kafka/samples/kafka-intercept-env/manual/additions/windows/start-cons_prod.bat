start "Kafka-Console-Consumer" kafka-console-consumer --consumer.config ../../config/consumer.properties --bootstrap-server localhost:9092 --topic tx-topic
timeout /T 10
start "Kafka-Console-Producer" kafka-console-producer --producer.config ../../config/producer.properties --broker-list localhost:9092 --topic tx-topic
