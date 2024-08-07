@echo off
:: Licensed to the Apache Software Foundation (ASF) under one
:: or more contributor license agreements.  See the NOTICE file
:: distributed with this work for additional information
:: regarding copyright ownership.  The ASF licenses this file
:: to you under the Apache License, Version 2.0 (the
:: "License"); you may not use this file except in compliance
:: with the License.  You may obtain a copy of the License at
::
::   http://www.apache.org/licenses/LICENSE-2.0
::
:: Unless required by applicable law or agreed to in writing,
:: software distributed under the License is distributed on an
:: "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
:: KIND, either express or implied.  See the License for the
:: specific language governing permissions and limitations
:: under the License.

setlocal

if NOT "%ARTEMIS_INSTANCE%"=="" goto CHECK_ARTEMIS_INSTANCE
set ARTEMIS_INSTANCE="%~dp0.."

:CHECK_ARTEMIS_INSTANCE
if exist %ARTEMIS_INSTANCE%\bin\artemis.cmd goto CHECK_JAVA

:NO_HOME
echo ARTEMIS_INSTANCE environment variable is set incorrectly. Please set ARTEMIS_INSTANCE.
goto END

:CHECK_JAVA
set _JAVACMD=%JAVACMD%

if "%JAVA_HOME%" == "" goto NO_JAVA_HOME
if not exist "%JAVA_HOME%\bin\java.exe" goto NO_JAVA_HOME
if "%_JAVACMD%" == "" set _JAVACMD=%JAVA_HOME%\bin\java.exe
goto RUN_JAVA

:NO_JAVA_HOME
if "%_JAVACMD%" == "" set _JAVACMD=java.exe
echo.
echo Warning: JAVA_HOME environment variable is not set.
echo.

:RUN_JAVA

:: "Load Profile Config"
set ARTEMIS_INSTANCE_ETC="D:\tmp\apache-artemis-2.19.1\bin\mybroker0\etc"
call %ARTEMIS_INSTANCE_ETC%\artemis.profile.cmd %*

:: "Set Defaults."
:: set ARTEMIS_LOGGING_CONF=%ARTEMIS_INSTANCE_ETC_URI%/logging.properties
set ARTEMIS_LOGGING_CONF=%ARTEMIS_INSTANCE_ETC_URI%/tnt4j/logging.properties
set ARTEMIS_LOG_MANAGER=org.jboss.logmanager.LogManager

if not exist "%ARTEMIS_OOME_DUMP%" goto NO_ARTEMIS_OOME_DUMP
:: "Backup the last OOME heap dump"
move /Y "%ARTEMIS_OOME_DUMP%" "%ARTEMIS_OOME_DUMP%.bkp"

:NO_ARTEMIS_OOME_DUMP

:: "Create full JVM Args"
set JVM_ARGS=%JAVA_ARGS%
if not "%ARTEMIS_CLUSTER_PROPS%"=="" set JVM_ARGS=%JVM_ARGS% %ARTEMIS_CLUSTER_PROPS%
set JVM_ARGS=%JVM_ARGS% -classpath %ARTEMIS_HOME%\lib\artemis-boot.jar
set JVM_ARGS=%JVM_ARGS% -Dartemis.home=%ARTEMIS_HOME%
set JVM_ARGS=%JVM_ARGS% -Dartemis.instance=%ARTEMIS_INSTANCE%
set JVM_ARGS=%JVM_ARGS% -Ddata.dir=%ARTEMIS_DATA_DIR%
set JVM_ARGS=%JVM_ARGS% -Dartemis.instance.etc=%ARTEMIS_INSTANCE_ETC%
set JVM_ARGS=%JVM_ARGS% -Djava.util.logging.manager=%ARTEMIS_LOG_MANAGER%
set JVM_ARGS=%JVM_ARGS% -Dlogging.configuration=%ARTEMIS_LOGGING_CONF%
set JVM_ARGS=%JVM_ARGS% -Dartemis.default.sensitive.string.codec.key=%ARTEMIS_DEFAULT_SENSITIVE_STRING_CODEC_KEY%
set JVM_ARGS=%JVM_ARGS% -Dtnt4j.config=%ARTEMIS_INSTANCE_ETC%\tnt4j\tnt4j.properties

if not "%DEBUG_ARGS%"=="" set JVM_ARGS=%JVM_ARGS% %DEBUG_ARGS%

"%_JAVACMD%" %JVM_ARGS% org.apache.activemq.artemis.boot.Artemis %*

:END
endlocal
GOTO :EOF

:EOF
