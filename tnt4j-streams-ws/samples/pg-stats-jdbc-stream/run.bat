@echo off
setlocal
:: set JDBC_LIBPATH=.\lib\db2\*
set JDBC_LIBPATH=.\lib\postgres\*
:: set JDBC_LIBPATH=.\lib\mssql\*
:: set JDBC_LIBPATH=.\lib\oracle\*
:: set JDBC_LIBPATH=.\lib\mysql\*
set LIBPATH=%LIBPATH%;%JDBC_LIBPATH%
..\..\bin\tnt4j-streams.bat -f:tnt-data-source.xml