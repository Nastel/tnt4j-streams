@echo off
setlocal

:: #### service variables initialization ####

set /p OS_ARC= Java architecture? ([win32]/win64)
IF ["%OS_ARC%"] NEQ ["win64"] (
	set OS_ARC=win32
) ELSE (
	set OS_ARC=win64
)

if exist %JAVA_HOME%bin\client\jvm.dll (
	set DLL_LOC=bin\client 
) else (
	set DLL_LOC=jre\bin\server
)

:: ### When x64 preferred to use ###
:: set OS_ARC=win64
set /p NAME=Enter your service name ([TNT4JSteams]): 
IF ["%NAME%"] EQU [""] set NAME=TNT4JSteams
set DESCRIPTION=TNT4J-Streams data stream service
set RUNDIR=%~dp0
set FILE_PATH=%RUNDIR%..\..
set LOG_PATH=%FILE_PATH%\logs
:: ### When using JRE from JDK ###
:: set JVM_DLL_PATH=%JAVA_HOME%\jre\bin\server
:: ### General use case standalone JRE###
set /p JVM_DLL_PATH= Enter the path of jvm.dll ([%JAVA_HOME%%DLL_LOC%]): 
IF ["%JVM_DLL_PATH%"] EQU [""] set JVM_DLL_PATH=%JAVA_HOME%%DLL_LOC%
:: ### Use stream configuration from default config dir or define custom one

set /p PARSER_CONFIG=Enter parser configuration file path ([%FILE_PATH%\config\tnt-data-source.xml]): 
IF ["%PARSER_CONFIG%"] EQU [""] set PARSER_CONFIG="%FILE_PATH%\config\tnt-data-source.xml"
set TNT4J_CONFIG="%FILE_PATH%\config\tnt4j.properties"
set LOG4J_CONFIG="%FILE_PATH%\config\log4j2.xml"
set SERVICE_PROVIDER=%RUNDIR%%OS_ARC%\prunsrv.exe

echo Description            %DESCRIPTION%  
echo DisplayName            %NAME%      
echo Install                %SERVICE_PROVIDER%      
echo LogPath                %LOG_PATH%      
echo Classpath              %FILE_PATH%\*;%FILE_PATH%\lib\*  
echo Jvm                    %JVM_DLL_PATH%\jvm.dll     
echo StartPath              %FILE_PATH% 
echo StartParams            start;-f:%PARSER_CONFIG%
echo JvmOptions             -Dlog4j2.configurationFile=%LOG4J_CONFIG%;-Dtnt4j.config=%TNT4J_CONFIG%

SET /P AREYOUSURE=Are you sure (Y/[N])?
IF /I "%AREYOUSURE%" NEQ "Y" GOTO END

:: #### installing service ####

%SERVICE_PROVIDER% //IS//%NAME%    ^
 --Description  "%DESCRIPTION%"  ^
 --DisplayName "%NAME%"      ^
 --Install "%SERVICE_PROVIDER%"      ^
 --LogPath "%LOG_PATH%"      ^
 --StdOutput  ""     ^
 --StdError auto      ^
 --Classpath %FILE_PATH%\*;%FILE_PATH%\lib\*  ^
 --Jvm "%JVM_DLL_PATH%\jvm.dll"      ^
 --StartMode jvm      ^
 --StopMode jvm      ^
 --StartPath %FILE_PATH%     ^
 --StartClass com.jkoolcloud.tnt4j.streams.StreamsDaemon      ^
 --StopClass com.jkoolcloud.tnt4j.streams.StreamsDaemon      ^
 --StopParams "stop"  ^
 --StartParams "start;-f:%PARSER_CONFIG%"      ^
 --JvmOptions "-Dlog4j2.configurationFile=%LOG4J_CONFIG%;-Dtnt4j.config=%TNT4J_CONFIG%"  ^
 --JvmMs 128      ^
 --JvmMx 512

:: #### making service uninstall script ####

echo "%SERVICE_PROVIDER%"  //DS//%NAME% > uninstallService.bat

:END
