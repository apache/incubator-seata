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

# Setup JVM parameters for seata server

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

PRGDIR=`dirname "$PRG"`
BASEDIR=`cd "$PRGDIR/.." >/dev/null; pwd`
BASEDIR=${BASEDIR//"//"/"/"}

# Reset the REPO variable. If you need to influence this use the environment setup file.
REPO=


# OS specific support.  $var _must_ be set to either true or false.
cygwin=false;
darwin=false;
case "`uname`" in
  CYGWIN*) cygwin=true ;;
  Darwin*) darwin=true
    if [ -z "$JAVA_VERSION" ] ; then
      JAVA_VERSION="CurrentJDK"
    else
      echo "Using Java version: $JAVA_VERSION"
    fi
    if [ -z "$JAVA_HOME" ]; then
      if [ -x "/usr/libexec/java_home" ]; then
        JAVA_HOME=`/usr/libexec/java_home`
      else
        JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Versions/${JAVA_VERSION}/Home
      fi
    fi
  ;;
esac

if [ -z "$JAVA_HOME" ] ; then
  if [ -r /etc/gentoo-release ] ; then
    JAVA_HOME=`java-config --jre-home`
  fi
fi

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
  [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
  [ -n "$CLASSPATH" ] && CLASSPATH=`cygpath --path --unix "$CLASSPATH"`
fi

# If a specific java binary isn't specified search for the standard 'java' binary
if [ -z "$JAVACMD" ] ; then
  if [ -n "$JAVA_HOME"  ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
      # IBM's JDK on AIX uses strange locations for the executables
      JAVACMD="$JAVA_HOME/jre/sh/java"
    else
      JAVACMD="$JAVA_HOME/bin/java"
    fi
  else
    JAVACMD=`which java`
  fi
fi

if [ ! -x "$JAVACMD" ] ; then
  echo "Error: JAVA_HOME is not defined correctly." 1>&2
  echo "  We cannot execute $JAVACMD" 1>&2
  exit 1
fi

if [ -z "$REPO" ]
then
  REPO="$BASEDIR"/lib
fi

CLASSPATH="$BASEDIR"/conf:"$REPO"/*

ENDORSED_DIR=
if [ -n "$ENDORSED_DIR" ] ; then
  CLASSPATH=$BASEDIR/$ENDORSED_DIR/*:$CLASSPATH
fi

if [ -n "$CLASSPATH_PREFIX" ] ; then
  CLASSPATH=$CLASSPATH_PREFIX:$CLASSPATH
fi

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
  [ -n "$CLASSPATH" ] && CLASSPATH=`cygpath --path --windows "$CLASSPATH"`
  [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
  [ -n "$HOME" ] && HOME=`cygpath --path --windows "$HOME"`
  [ -n "$BASEDIR" ] && BASEDIR=`cygpath --path --windows "$BASEDIR"`
  [ -n "$REPO" ] && REPO=`cygpath --path --windows "$REPO"`
fi

if [ "$SKYWALKING_ENABLE" = "true" ]; then
  SKYWALKING_OPTS="-javaagent:${BASEDIR}/ext/apm-skywalking/skywalking-agent.jar -Dskywalking_config=${BASEDIR}/ext/apm-skywalking/config/agent.config -Dskywalking.logging.dir=${BASEDIR}/logs"
  JAVA_OPT="${JAVA_OPT} $SKYWALKING_OPTS"
  echo "apm-skywalking enabled opts: $SKYWALKING_OPTS"
else
  echo "apm-skywalking not enabled"
fi

# auto JVM Memory Calculation
AVAILABLE_MEM_BYTES=""

# cgroup v1
if [ -f /sys/fs/cgroup/memory/memory.limit_in_bytes ]; then
  CGROUP_LIMIT=$(cat /sys/fs/cgroup/memory/memory.limit_in_bytes 2>/dev/null)
  if [ "$CGROUP_LIMIT" != "9223372036854771712" ] && [ "$CGROUP_LIMIT" -gt 104857600 ]; then
    AVAILABLE_MEM_BYTES=$CGROUP_LIMIT
  fi
# cgroup v2
elif [ -f /sys/fs/cgroup/memory.max ]; then
  CGROUP_LIMIT=$(cat /sys/fs/cgroup/memory.max 2>/dev/null)
  if [ "$CGROUP_LIMIT" != "max" ] && [ "$CGROUP_LIMIT" -gt 104857600 ]; then
    AVAILABLE_MEM_BYTES=$CGROUP_LIMIT
  fi
fi

# for VMs / bare-metal
if [ -z "$AVAILABLE_MEM_BYTES" ]; then
  if command -v free >/dev/null 2>&1; then
    AVAILABLE_MEM_BYTES=$(free -b | awk '/Mem:/ {print $2}')
  elif [ -f /proc/meminfo ]; then
    AVAILABLE_MEM_BYTES=$(awk '/MemTotal/ {print $2 * 1024}' /proc/meminfo)
  fi
fi

# auto set JVM_XMX/JVM_XMS if not provided by user
if [ -n "$AVAILABLE_MEM_BYTES" ] && [ -z "$JVM_XMX" ]; then
  TOTAL_MEM_MB=$(( AVAILABLE_MEM_BYTES / 1024 / 1024 ))
  if [ "$TOTAL_MEM_MB" -le 4096 ]; then
    HEAP_PCT=70
  else
    HEAP_PCT=75
  fi
  HEAP_MB=$(( TOTAL_MEM_MB * HEAP_PCT / 100 ))
  # minimum heap
  [ "$HEAP_MB" -lt 256 ] && HEAP_MB=256

  JVM_XMX="${HEAP_MB}m"
  JVM_XMS="${HEAP_MB}m"
  echo "[INFO] Auto set JVM heap to ${HEAP_MB}m (total memory: ${TOTAL_MEM_MB}MB, heap%: ${HEAP_PCT}%)"
fi

# final fallback
JVM_XMX=${JVM_XMX:-"2048m"}
JVM_XMS=${JVM_XMS:-"2048m"}
JVM_XSS=${JVM_XSS:-"640k"}
JVM_MetaspaceSize=${JVM_MetaspaceSize:-"128m"}
JVM_MaxMetaspaceSize=${JVM_MaxMetaspaceSize:-"256m"}
JVM_MaxDirectMemorySize=${JVM_MaxDirectMemorySize:-"1024m"}

LOADER_PATH=$LOADER_PATH
LOG_HOME=$LOG_HOME
if [ -z "$LOG_HOME" ]; then
    LOG_HOME="$HOME/logs/seata"
    mkdir -p $LOG_HOME
fi
JAVA_OPT="${JAVA_OPT} -Dlog.home=${LOG_HOME} -server -Dloader.path=${LOADER_PATH:="$BASEDIR/lib"} -Xmx${JVM_XMX} -Xms${JVM_XMS} -Xss${JVM_XSS} -XX:SurvivorRatio=10 -XX:MetaspaceSize=${JVM_MetaspaceSize} -XX:MaxMetaspaceSize=${JVM_MaxMetaspaceSize} -XX:MaxDirectMemorySize=${JVM_MaxDirectMemorySize} -XX:-OmitStackTraceInFastThrow -XX:-UseAdaptiveSizePolicy"
JAVA_OPT="${JAVA_OPT} -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${LOG_HOME}/java_heapdump.hprof -XX:+DisableExplicitGC"

JAVA_MAJOR_VERSION=$($JAVACMD -version 2>&1 | sed '1!d' | sed -e 's/"//g' | awk '{print $3}' | awk -F '.' '{print $1}')
if [[ "$JAVA_MAJOR_VERSION" -eq "1" ]] ; then
  JAVA_MAJOR_VERSION=$($JAVACMD -version 2>&1 | sed '1!d' | sed -e 's/"//g' | awk '{print $3}' | awk -F '.' '{print $2}')
fi
if [[ "$JAVA_MAJOR_VERSION" -ge "9" ]] ; then
  JAVA_OPT="${JAVA_OPT} -Xlog:gc*:file=${LOG_HOME}/seata_gc.log:time,tags:filecount=10,filesize=10M"
else
  JAVA_OPT="${JAVA_OPT} -Xloggc:${LOG_HOME}/seata_gc.log -verbose:gc -XX:+PrintGCDetails  -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=10 -XX:GCLogFileSize=100M -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC"
fi

JAVA_OPT="${JAVA_OPT} -Dio.netty.leakDetectionLevel=advanced"
JAVA_OPT="${JAVA_OPT} -Dapp.name=seata-server -Dapp.home=${BASEDIR} -Dbasedir=${BASEDIR}"

if [ "$JMX_ENABLE" = "true" ]; then
  JMX_PORT=$JMX_PORT
  JMX_OPTS=$JMX_OPTS
  if [ -z "$JMX_OPTS" ]; then
    JMX_OPTS=" -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false  -Dcom.sun.management.jmxremote.ssl=false "
  fi
  if [ -z "$JMX_PORT" ]; then
    JMX_OPTS=" $JMX_OPTS -Dcom.sun.management.jmxremote.port=${JMX_PORT:="10055"} -Dcom.sun.management.jmxremote.rmi.port=${JMX_PORT:="10055"} "
  fi
  echo "JMX enabled"
else
  echo "JMX disabled"
fi

JAVA_OPT="${JAVA_OPT} ${JMX_OPTS}"

if [ ! -x "$BASEDIR"/logs ]; then
  mkdir "$BASEDIR"/logs
fi