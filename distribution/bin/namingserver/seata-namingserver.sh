#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Change to the correct character encoding
export LANG=en_US.UTF-8

ERROR_CODE=0

# Function to handle errors
handle_error() {
    local error_code=$1
    echo "Error code: $error_code"
    exit $error_code
}

# Initialize variables
CMD_LINE_ARGS="$@"
BASEDIR=$(dirname $(readlink -f $0))/../..
BASEDIR=$(realpath "$BASEDIR")

# Set repository and classpath
REPO=${BASEDIR}/lib
CLASSPATH="${BASEDIR}/conf:${REPO}/*"

# Check if log directory exists, if not create it
if [ ! -d "${BASEDIR}/logs" ]; then
    mkdir -p "${BASEDIR}/logs"
fi

# Set Java command if not already set
if [ -z "${JAVACMD}" ]; then
    JAVACMD=java
fi

# Execute Java command
${JAVACMD} ${JAVA_OPTS} ${SKYWALKING_OPTS} -server \
    -Dloader.path="${BASEDIR}/lib" \
    -Xmx2048m -Xms2048m -Xss512k -XX:SurvivorRatio=10 \
    -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m \
    -XX:MaxDirectMemorySize=1024m -XX:-OmitStackTraceInFastThrow \
    -XX:-UseAdaptiveSizePolicy -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath="${BASEDIR}/logs/java_heapdump.hprof" \
    -XX:+DisableExplicitGC -Xloggc:"${BASEDIR}/logs/seata_namingserver_gc.log" \
    -verbose:gc -Dio.netty.leakDetectionLevel=advanced \
    -classpath ${CLASSPATH} -Dapp.name="seata-namingserver" \
    -Dapp.repo="${REPO}" -Dapp.home="${BASEDIR}" \
    -Dbasedir="${BASEDIR}" \
    -Dspring.config.location="${BASEDIR}/conf/application.yml" \
    -jar "${BASEDIR}/target/namingserver.jar" ${CMD_LINE_ARGS}

ERROR_CODE=$?
if [ ${ERROR_CODE} -ne 0 ]; then
    handle_error ${ERROR_CODE}
fi

exit 0
