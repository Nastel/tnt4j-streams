rmdir /S /Q "../../logs"

set JMX_PORT=49998
start "Kafka-Used-Zookeeper" zookeeper-server-start.bat ../../config/zookeeper.properties
timeout /T 10
set JMX_PORT=49999
start "Kafka-Server" kafka-server-start.bat ../../config/server.properties
