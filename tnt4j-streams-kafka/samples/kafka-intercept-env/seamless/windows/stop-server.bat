set KAFKA_HOME=C:\kafka\kafka_2.12-2.4.1

start "" %KAFKA_HOME%\bin\windows\kafka-server-stop.bat
timeout /T 10
start "" %KAFKA_HOME%\bin\windows\zookeeper-server-stop.bat