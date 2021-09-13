@echo on
setlocal
rem set JDBC_LIBPATH=.\lib\db2\*
set JDBC_LIBPATH=.\lib\postgres\*
rem set JDBC_LIBPATH=.\lib\mssql\*
rem set JDBC_LIBPATH=.\lib\oracle\*
rem set JDBC_LIBPATH=.\lib\mysql\*
set LIBPATH=%LIBPATH%;%JDBC_LIBPATH%
..\..\bin\tnt4j-streams.bat -f:tnt-data-source.xml