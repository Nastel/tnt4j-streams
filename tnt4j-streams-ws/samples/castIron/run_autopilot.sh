#! /bin/bash
export STREAMSOPTS="-Dorg.quartz.threadPool.threadCount=1"
export TNT4J_PROPERTIES=".\tnt4j_autopilot.properties"
# sourcing instead of executing to pass variables
. ../../bin/tnt4j-streams.sh -f:tnt-data-source.xml