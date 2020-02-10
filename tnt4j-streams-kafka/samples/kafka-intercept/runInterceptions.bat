@echo off
setlocal

set RUNDIR=%~dp0
set LIBPATH=%RUNDIR%..\..\*;%RUNDIR%..\..\lib\*
set TNT4JOPTS=-Dtnt4j.config="%RUNDIR%..\..\config\tnt4j.properties"
set LOG4JOPTS=-Dlog4j.configuration="file:%RUNDIR%..\..\config\log4j.properties"
set PRODUCER_CONFIG=-Dproducer.config="%RUNDIR%..\..\config\intercept\producer.properties"
set CONSUMER_CONFIG=-Dconsumer.config="%RUNDIR%..\..\config\intercept\consumer.properties"
set INTERCEPT_CONFIG=-Dinterceptors.config="%RUNDIR%..\..\config\intercept\interceptors.properties"
set STREAMSOPTS=%LOG4JOPTS% %TNT4JOPTS% %PRODUCER_CONFIG% %CONSUMER_CONFIG% %INTERCEPT_CONFIG% -Dfile.encoding=UTF-8

IF ["%JAVA_HOME%"] EQU [""] (
  echo "JAVA_HOME" env. variable is not defined!..
) else (
  echo Will use java from: "%JAVA_HOME%"
)

@echo on
"%JAVA_HOME%\bin\java" %STREAMSOPTS% -classpath "%LIBPATH%" com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.InterceptorsTest
pause
