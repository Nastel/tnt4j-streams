@echo off
setlocal
:: set JMS_IMPL_LIBPATH=.\lib\rabbitMQ\*
:: set JMS_IMPL_LIBPATH=.\lib\solaceLib\*
:: set JMS_IMPL_LIBPATH=.\lib\IBMMQ\*
:: set JMS_IMPL_LIBPATH=.\lib\ActiveMQ\*
set LIBPATH=%LIBPATH%;%JMS_IMPL_LIBPATH%
..\..\bin\tnt4j-streams.bat -f:tnt-data-source.xml