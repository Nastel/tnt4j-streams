#! /bin/bash
if command -v readlink >/dev/null 2>&1; then
    SCRIPTPATH=$(dirname $(readlink -m $BASH_SOURCE))
else
    SCRIPTPATH=$(cd "$(dirname "$BASH_SOURCE")" ; pwd -P)
fi

### TrustStore options: path and password
TRUST_OPTS="-Djavax.net.ssl.trustStore=$SCRIPTPATH/qm_dev.jks -Djavax.net.ssl.trustStorePassword=nastel123"
### KeyStore options: path and password
KEY_OPTS="-Djavax.net.ssl.keyStore=$SCRIPTPATH/qm_dev.jks -Djavax.net.ssl.keyStorePassword=nastel123"
### Java SSL configuration options: enable SSL verbose messages for debugging and in case of running non IBM JVM for client - disable IBM cipher mappings
TLS_OPTS="-Djavax.net.debug=ssl -Dcom.ibm.mq.cfg.useIBMCipherMappings=false"

### Uncomment if WMQ SSL connection is required
#export STREAMSOPTS="$TRUST_OPTS $KEY_OPTS $TLS_OPTS"
# sourcing instead of executing to pass variables
. ../../bin/tnt4j-streams.sh -f:tnt-data-source.xml
