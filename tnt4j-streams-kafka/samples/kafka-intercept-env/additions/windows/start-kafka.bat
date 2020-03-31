set JMX_PORT=49998
start zookeeper-server-start.bat ../../config/zookeeper.properties
timeout /T 10
set JMX_PORT=49999
start kafka-server-start.bat ../../config/server.properties
