#! /bin/bash
# JDBC_LIBPATH=./lib/db2/*
JDBC_LIBPATH=./lib/postgres/*
# JDBC_LIBPATH=./lib/mssql/*
# JDBC_LIBPATH=./lib/oracle/*
# JDBC_LIBPATH=./lib/mysql/*
export LIBPATH="$LIBPATH:$JDBC_LIBPATH"
../../bin/tnt4j-streams.sh -f:tnt-data-source.xml