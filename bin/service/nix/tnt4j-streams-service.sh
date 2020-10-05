#! /bin/sh
#  /etc/init.d/mydaemon

### BEGIN INIT INFO
# Provides:          tnt4j-streams
# Required-Start:    $local-fs $remote_fs $network
# Required-Stop:     $local-fs $remote_fs $network
# Default-Start: 2 3 4 5
# Default-Stop: 0 1 6
# Description:       TNT4J Streams allows data streaming, parsing from various data sources into TNT4J event sinks.
# Short-Description: TNT4J Streams
### END INIT INFO

DESC="TNT4J Streams"
NAME=tnt4j-streams

# The path to Jsvc
EXEC="/usr/bin/jsvc"

# The path to the folder containing tnt4j-streams-core-*.jar
FILE_PATH="/home/tnt4j-streams-1.11.3/"

PARSER_CONFIG=$FILE_PATH/config/tnt-data-source.xml

# The path to the folder containing the java runtime
JAVA_HOME="/usr/lib/jvm/default-java"

# Our classpath including our jar file and the Apache Commons Daemon library
CLASS_PATH="$FILE_PATH/*:$FILE_PATH/lib/*"

# The fully qualified name of the class to execute
CLASS="com.jkoolcloud.tnt4j.streams.StreamsDaemon"

# Any command line arguments to be passed to the our Java Daemon implementations init() method 
ARGS="-f:$PARSER_CONFIG"

#The user to run the daemon as
USER="root"

# The file that will contain our process identification number (pid) for other scripts/programs that need to access it.
PID="/var/run/$NAME.pid"

# System.out writes to this file...
LOG_OUT="$FILE_PATH/log/$NAME-jsvc.out"

# System.err writes to this file...
LOG_ERR="$FILE_PATH/log/$NAME-jsvc.err"

# Path for the JAR's
LIBPATH="$LIBPATH:$FILE_PATH/*:$FILE_PATH/lib/*"

#TNT4J event sinks configuration file
TNT4J_PROPERTIES="$FILE_PATH/config/tnt4j.properties"

#Logger configuration
LOG4J_PROPERTIES="$FILE_PATH/config/log4j.properties"


TNT4JOPTS="-Dtnt4j.config=$TNT4J_PROPERTIES"
LOG4JOPTS="-Dlog4j.configuration=file:$LOG4J_PROPERTIES"
STREAMSOPTS="$STREAMSOPTS $LOG4JOPTS $TNT4JOPTS"

jsvc_exec()
{   
    cd $FILE_PATH
    echo "Executing"
    $EXEC -home $JAVA_HOME -cp $CLASS_PATH -user $USER $STREAMSOPTS -outfile $LOG_OUT -errfile $LOG_ERR -pidfile $PID $1 $CLASS $ARGS
}

case "$1" in
    start)  
        echo "Starting the $DESC..."        
        
        # Start the service
        jsvc_exec
        
        echo "The $DESC has started."
    ;;
    stop)
        echo "Stopping the $DESC..."
        
        # Stop the service
        jsvc_exec "-stop"       
        
        echo "The $DESC has stopped."
    ;;
    restart)
        if [ -f "$PID" ]; then
            
            echo "Restarting the $DESC..."
            
            # Stop the service
            jsvc_exec "-stop"
            
            # Start the service
            jsvc_exec
            
            echo "The $DESC has restarted."
        else
            echo "Daemon not running, no action taken"
            exit 1
        fi
            ;;
    *)
    echo "Usage: /etc/init.d/$NAME {start|stop|restart}" >&2
    exit 3
    ;;
esac
