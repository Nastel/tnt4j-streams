set KAFKA_HOME=C:\kafka\kafka_2.12-2.4.1

rmdir /S /Q "%KAFKA_HOME%\logs"

set JMX_PORT=49998
start "Kafka-Used-Zookeeper" %KAFKA_HOME%\bin\windows\zookeeper-server-start.bat "%KAFKA_HOME%\config\zookeeper.properties"
timeout /T 10
set JMX_PORT=49999
start "Kafka-Server" %KAFKA_HOME%\bin\windows\kafka-server-start.bat "%KAFKA_HOME%\config\server.properties"