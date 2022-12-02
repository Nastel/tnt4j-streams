#! /bin/bash
if command -v readlink >/dev/null 2>&1; then
    SCRIPTPATH=$(dirname $(readlink -m $BASH_SOURCE))
else
    SCRIPTPATH=$(cd "$(dirname "$BASH_SOURCE")" ; pwd -P)
fi

LIBPATH="$LIBPATH:$SCRIPTPATH/../*:$SCRIPTPATH/../lib/*"
# tnt4j file override
if [[ -z "$TNT4J_PROPERTIES" ]]; then
  TNT4J_PROPERTIES="$SCRIPTPATH/../config/tnt4j.properties"
fi
TNT4JOPTS="-Dtnt4j.config=$TNT4J_PROPERTIES"
#TNT4JOPTS=${TNT4JOPTS:-"-Dtnt4j.config=$SCRIPTPATH/../config/tnt4j.properties"}

# log4j file override
if [[ -z "$LOG4J_PROPERTIES" ]]; then
  LOG4J_PROPERTIES="$SCRIPTPATH/../config/log4j2.xml"
fi
LOG4JOPTS="-Dlog4j2.configurationFile=file:$LOG4J_PROPERTIES"
#LOG4JOPTS=${LOG4JOPTS:-"-Dlog4j2.configurationFile=file:$SCRIPTPATH/../config/log4j2.xml"}

#LOGBACKOPTS="-Dlogback.configurationFile=file:$SCRIPTPATH/../config/logback.xml"
STREAMSOPTS="$STREAMSOPTS $LOG4JOPTS $TNT4JOPTS -Dfile.encoding=UTF-8"

if [[ "$MAINCLASS" == "" ]]; then
	MAINCLASS="com.jkoolcloud.tnt4j.streams.StreamsAgent"
fi

JAVA_EXEC="java"
if [[ "$JAVA_HOME" == "" ]]; then
  echo '"JAVA_HOME" env. variable is not defined!..'
else
  echo 'Will use java from:' "$JAVA_HOME"
  JAVA_EXEC="$JAVA_HOME/bin/java"
fi

$JAVA_EXEC $STREAMSOPTS -classpath "$LIBPATH" $MAINCLASS $*
