#! /bin/bash
if command -v realpath >/dev/null 2>&1; then
    SCRIPTPATH=`dirname $(realpath $0)`
else
    SCRIPTPATH=$( cd "$(dirname "$0")" ; pwd -P )
fi

LIBPATH="$SCRIPTPATH/../../*:$SCRIPTPATH/../../lib/*"
TNT4JOPTS="-Dtnt4j.config=$SCRIPTPATH/../../config/tnt4j.properties"
LOG4JOPTS="-Dlog4j2.configurationFile=file:$SCRIPTPATH/../../config/log4j2.xml"
PRODUCER_CONFIG="-Dproducer.config=$SCRIPTPATH/../../config/intercept/producer.properties"
CONSUMER_CONFIG="-Dconsumer.config=$SCRIPTPATH/../../config/intercept/consumer.properties"
INTERCEPT_CONFIG="-Dinterceptors.config=$SCRIPTPATH/../../config/intercept/interceptorsC.properties"
STREAMSOPTS="$LOG4JOPTS $TNT4JOPTS $PRODUCER_CONFIG $CONSUMER_CONFIG $INTERCEPT_CONFIG -Dfile.encoding=UTF-8"

JAVA_EXEC="java"
if [[ "$JAVA_HOME" == "" ]]; then
  echo '"JAVA_HOME" env. variable is not defined!..'
else
  echo 'Will use java from:' "$JAVA_HOME"
  JAVA_EXEC="$JAVA_HOME/bin/java"
fi

$JAVA_EXEC $STREAMSOPTS -classpath "$LIBPATH" com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.InterceptorsTest

read -p "Press [Enter] key to exit..."