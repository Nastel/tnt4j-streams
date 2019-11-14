@echo off
setlocal

set RUNDIR=%~dp0
rem TrustStore options: path and password
set TRUST_OPTS=-Djavax.net.ssl.trustStore="%RUNDIR%\qm_dev.jks" -Djavax.net.ssl.trustStorePassword=nastel123
rem KeyStore options: path and password
set KEY_OPTS=-Djavax.net.ssl.keyStore="%RUNDIR%\qm_dev.jks" -Djavax.net.ssl.keyStorePassword=nastel123
rem Java SSL configuration options: enable SSL verbose messages for debugging and in case of running non IBM JVM for client - disable IBM cipher mappings
set TLS_OPTS=-Djavax.net.debug=ssl -Dcom.ibm.mq.cfg.useIBMCipherMappings=false

rem Uncomment if WMQ SSL connection is required
rem set STREAMSOPTS=%TRUST_OPTS% %KEY_OPTS% %TLS_OPTS%

@echo on
..\..\bin\tnt4j-streams.bat -f:tnt-data-source.xml
