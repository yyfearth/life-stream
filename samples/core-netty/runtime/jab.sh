#!/bin/bash
#
# This script is used to start the server
#

export POKE_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo "** starting client jab from ${POKE_HOME} **"

cd ${POKE_HOME}

JAVA_MAIN='poke.demo.Jab'

# see http://java.sun.com/performance/reference/whitepapers/tuning.html
JAVA_TUNE='-Xms500m -Xmx1000m'


java ${JAVA_TUNE} -cp .:${POKE_HOME}/../lib/'*':${POKE_HOME}/../classes ${JAVA_MAIN}
