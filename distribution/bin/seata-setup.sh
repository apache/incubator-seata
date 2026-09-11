#!/bin/bash
# Copyright 1999-2019 Seata.io Group.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

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
JVM_XMX=$JVM_XMX
JVM_XMS=$JVM_XMS
JVM_XSS=$JVM_XSS
JVM_MetaspaceSize=$JVM_MetaspaceSize
JVM_MaxMetaspaceSize=$JVM_MaxMetaspaceSize
JVM_MaxDirectMemorySize=$JVM_MaxDirectMemorySize
LOADER_PATH=$LOADER_PATH
LOG_HOME=$LOG_HOME
if [ -z "$LOG_HOME" ]; then
    LOG_HOME="$HOME/logs/seata"
fi
JAVA_OPT="${JAVA_OPT} -Dlog.home=${LOG_HOME} -server -Dloader.path=${LOADER_PATH:="$BASEDIR/lib"} -Xmx${JVM_XMX:="2048m"} -Xms${JVM_XMS:="2048m"} -Xss${JVM_XSS:="640k"} -XX:SurvivorRatio=10 -XX:MetaspaceSize=${JVM_MetaspaceSize:="128m"} -XX:MaxMetaspaceSize=${JVM_MaxMetaspaceSize:="256m"} -XX:MaxDirectMemorySize=${JVM_MaxDirectMemorySize:=1024m} -XX:-OmitStackTraceInFastThrow -XX:-UseAdaptiveSizePolicy"
JAVA_OPT="${JAVA_OPT} -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${LOG_HOME}/java_heapdump.hprof -XX:+DisableExplicitGC"

JAVA_MAJOR_VERSION=$($JAVACMD -version 2>&1 | sed '1!d' | sed -e 's/"//g' | awk '{print $3}' | awk -F '.' '{print $1}')
if [[ "$JAVA_MAJOR_VERSION" -eq "1" ]] ; then
  JAVA_MAJOR_VERSION=$($JAVACMD -version 2>&1 | sed '1!d' | sed -e 's/"//g' | awk '{print $3}' | awk -F '.' '{print $2}')
fi
if [[ "$JAVA_MAJOR_VERSION" -ge "9" ]] ; then
  JAVA_OPT="${JAVA_OPT} -Xlog:gc*:file=${LOG_HOME}/seata_gc.log:time,tags:filecount=10,filesize=102400"
elif [[ "$JAVA_MAJOR_VERSION" -ge "17" ]] ; then
  JAVA_OPT="${JAVA_OPT} -Xlog:gc=trace:file=${LOG_HOME}/seata_gc.log:time,tags:filecount=10,filesize=10M"
else
  JAVA_OPT="${JAVA_OPT} -Xloggc:${LOG_HOME}/seata_gc.log -verbose:gc -XX:+PrintGCDetails  -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=10 -XX:GCLogFileSize=100M -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC"
fi

JAVA_OPT="${JAVA_OPT} -Dio.netty.leakDetectionLevel=advanced"
JAVA_OPT="${JAVA_OPT} -Dapp.name=seata-server -Dapp.pid=${$} -Dapp.home=${BASEDIR} -Dbasedir=${BASEDIR}"

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
