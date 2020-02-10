#! /bin/bash
if command -v realpath >/dev/null 2>&1; then
    SCRIPTPATH=`dirname $(realpath $0)`
else
    SCRIPTPATH=$( cd "$(dirname "$0")" ; pwd -P )
fi

LIBPATH="$LIBPATH:$SCRIPTPATH/../../*:$SCRIPTPATH/../../lib/*"
MAINCLASS="com.jkoolcloud.tnt4j.streams.utils.SecurityUtils"

if [ "$JAVA_HOME" == "" ]; then
  echo '"JAVA_HOME" env. variable is not defined!..'
else
  echo 'Will use java from: "$JAVA_HOME"'
fi

"$JAVA_HOME/bin/java" -classpath "$LIBPATH" $MAINCLASS $*
