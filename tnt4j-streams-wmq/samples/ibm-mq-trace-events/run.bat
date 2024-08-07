@echo off
setlocal

set RUNDIR=%~dp0
:: TrustStore options: path and password
set TRUST_OPTS=-Djavax.net.ssl.trustStore="%RUNDIR%\qm_dev.jks" -Djavax.net.ssl.trustStorePassword=nastel123
:: KeyStore options: path and password
set KEY_OPTS=-Djavax.net.ssl.keyStore="%RUNDIR%\qm_dev.jks" -Djavax.net.ssl.keyStorePassword=nastel123
:: Java SSL configuration options: enable SSL verbose messages for debugging and in case of running non IBM JVM for client - disable IBM cipher mappings
set TLS_OPTS=-Djavax.net.debug=ssl -Dcom.ibm.mq.cfg.useIBMCipherMappings=false

:: Uncomment if WMQ SSL connection is required
:: set STREAMSOPTS=%TRUST_OPTS% %KEY_OPTS% %TLS_OPTS%

@echo off
..\..\bin\tnt4j-streams.bat -f:tnt-data-source.xml
