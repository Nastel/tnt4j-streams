@echo off
setlocal

set RUNDIR=%~dp0
set LIBPATH=%LIBPATH%;%RUNDIR%..\..\*;%RUNDIR%..\..\lib\*

set MAINCLASS=com.jkoolcloud.tnt4j.streams.utils.SecurityUtils

@echo on
"%JAVA_HOME%\bin\java" -classpath "%LIBPATH%" %MAINCLASS% %*