#! /bin/bash
if command -v readlink >/dev/null 2>&1; then
    SCRIPTPATH=$(dirname $(readlink -m $BASH_SOURCE))
else
    SCRIPTPATH=$(cd "$(dirname "$BASH_SOURCE")" ; pwd -P)
fi

export MAINCLASS="com.jkoolcloud.tnt4j.streams.configure.zookeeper.ZKConfigInit"
# sourcing instead of executing to pass variables
. ./tnt4j-streams.sh -c -f:$SCRIPTPATH/../config/zk-init-cfg.properties