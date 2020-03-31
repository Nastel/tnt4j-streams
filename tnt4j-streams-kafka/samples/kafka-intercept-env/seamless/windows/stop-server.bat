set KAFKA_HOME=D:\tmp\kafka_install\kafka_2.12-2.4.1_

start "" %KAFKA_HOME%\bin\windows\kafka-server-stop.bat
timeout /T 10
start "" %KAFKA_HOME%\bin\windows\zookeeper-server-stop.bat