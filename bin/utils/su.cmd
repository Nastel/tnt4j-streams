@echo off
setlocal

set RUNDIR=%~dp0
set LIBPATH=%LIBPATH%;%RUNDIR%..\..\*;%RUNDIR%..\..\lib\*

set MAINCLASS=com.jkoolcloud.tnt4j.streams.utils.SecurityUtils

IF ["%JAVA_HOME%"] EQU [""] (
  echo "JAVA_HOME" env. variable is not defined!..
) else (
  echo Will use java from: "%JAVA_HOME%"
)

@echo on
"%JAVA_HOME%\bin\java" -classpath "%LIBPATH%" %MAINCLASS% %*