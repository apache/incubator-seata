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
  #echo "apm-skywalking enabled opts: $SKYWALKING_OPTS"
#else
  #echo "apm-skywalking not enabled"
fi
JVM_XMX=$JVM_XMX
JVM_XMS=$JVM_XMS
JVM_XSS=$JVM_XSS
JVM_MetaspaceSize=$JVM_MetaspaceSize
JVM_MaxMetaspaceSize=$JVM_MaxMetaspaceSize
JVM_MaxDirectMemorySize=$JVM_MaxDirectMemorySize
LOADER_PATH=$LOADER_PATH
JAVA_OPT="${JAVA_OPT} -server -Dloader.path=${LOADER_PATH:="$BASEDIR/lib"} -Xmx${JVM_XMX:="2048m"} -Xms${JVM_XMS:="2048m"} -Xss${JVM_XSS:="512k"} -XX:SurvivorRatio=10 -XX:MetaspaceSize=${JVM_MetaspaceSize:="128m"} -XX:MaxMetaspaceSize=${JVM_MaxMetaspaceSize:="256m"} -XX:MaxDirectMemorySize=${JVM_MaxDirectMemorySize:=1024m} -XX:-OmitStackTraceInFastThrow -XX:-UseAdaptiveSizePolicy"
JAVA_OPT="${JAVA_OPT} -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${BASEDIR}/logs/java_heapdump.hprof -XX:+DisableExplicitGC"

JAVA_MAJOR_VERSION=$($JAVACMD -version 2>&1 | sed '1!d' | sed -e 's/"//g' | awk '{print $3}' | awk -F '.' '{print $1}')
if [[ "$JAVA_MAJOR_VERSION" -eq "1" ]] ; then
  JAVA_MAJOR_VERSION=$($JAVACMD -version 2>&1 | sed '1!d' | sed -e 's/"//g' | awk '{print $3}' | awk -F '.' '{print $2}')
fi
if [[ "$JAVA_MAJOR_VERSION" -ge "9" ]] ; then
  JAVA_OPT="${JAVA_OPT} -Xlog:gc*:file=${BASEDIR}/logs/seata_gc.log:time,tags:filecount=10,filesize=102400"
elif [[ "$JAVA_MAJOR_VERSION" -ge "17" ]] ; then
  JAVA_OPT="${JAVA_OPT} -Xlog:gc=trace:file=${BASEDIR}/logs/seata_gc.log:time,tags:filecount=10,filesize=10M"
else
  JAVA_OPT="${JAVA_OPT} -Xloggc:${BASEDIR}/logs/seata_gc.log -verbose:gc -XX:+PrintGCDetails  -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=10 -XX:GCLogFileSize=100M -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC"
fi

JAVA_OPT="${JAVA_OPT} -Dio.netty.leakDetectionLevel=advanced"
JAVA_OPT="${JAVA_OPT} -Dapp.name=seata-server -Dapp.pid=${$} -Dapp.home=${BASEDIR} -Dbasedir=${BASEDIR}"
JAVA_OPT="${JAVA_OPT} -Dspring.config.additional-location=${BASEDIR}/conf/ -Dspring.config.location=${BASEDIR}/conf/application.yml -Dlogging.config=${BASEDIR}/conf/logback-spring.xml"
JAVA_OPT="${JAVA_OPT} -jar ${BASEDIR}/target/seata-server.jar"


if [ ! -x "$BASEDIR"/logs ]; then
  mkdir "$BASEDIR"/logs
fi

CMD_LINE_ARGS=$@
NEW_ARGS=$(echo "${CMD_LINE_ARGS}" | sed -e 's/^start//g' -e 's/^restart//g' -e 's/^ //g')



show_usage() {
    echo "  Usage: sh seata-server.sh(for linux and mac) or cmd seata-server.bat(for"
    echo "            windows) [options]"
    echo "  Options:"
    echo "  --host, -h"
    echo "    The ip to register to registry center."
    echo "  --port, -p"
    echo "    The port to listen."
    echo "    Default: 0"
    echo "  --storeMode, -m"
    echo "    log store mode : file, db, redis"
    echo "  --serverNode, -n"
    echo "    server node id, such as 1, 2, 3.it will be generated according to the"
    echo "    snowflake by default"
    echo "  --seataEnv, -e"
    echo "    The name used for multi-configuration isolation."
    echo "  --sessionStoreMode, -ssm"
    echo "    session log store mode : file, db, redis"
    echo "  --lockStoreMode, -lsm"
    echo "    lock log store mode : file, db, redis"
    echo "  --help"
}

# 校验host参数
function validate_host() {
    local host=$1
    local re_ip="^([0-9]{1,3}\.){3}[0-9]{1,3}$"
    if [[ ! $host =~ $re_ip ]]; then
        echo "Invalid host: $host"
        show_usage
        exit 1
    fi
}

# 校验port参数
function validate_port() {
    local port="$1"
    if ! [[ "$port" =~ ^[0-9]+$ ]]; then
        echo "Error: Invalid port: $port"
        show_usage
        exit 1
    fi
    return 0
}

function validate_mode() {
    local mode="$1"
    if [[ "$mode" != 'file' && "$mode" != 'db' && "$mode" != 'redis' ]]; then
        echo "Error: Invalid storeMode: $mode"
        show_usage
        exit 1
    fi
    return 0
}

function validate_serverNode() {
    local serverNode="$1"
        if ! [[ "$serverNode" =~ ^[0-9]+$ ]]; then
            echo "Error: Invalid serverNode: $serverNode"
            show_usage
            exit 1
        fi
    return 0
}

while [[ $# -gt 0 ]]; do
    key="$1"
    case "$key" in
      start|stop|restart)
          ;;
      -h|--host)
          if [[ -n "$2" ]]; then
            validate_host "$2"
            shift
          else
            echo "Error: Host value is required"
            show_usage
            exit 1
          fi
          ;;
      -p|--port)
          if [[ -n "$2" ]]; then
            validate_port "$2"
            shift
          else
            echo "Error: Port value is required"
            show_usage
            exit 1
          fi
          ;;
      -m|--storeMode)
          if [[ -n "$2" ]]; then
            validate_mode "$2"
            shift
          else
            echo "Error: storeMode value is required"
            show_usage
            exit 1
          fi
          ;;
      -ssm|--sessionStoreMode)
          if [[ -n "$2" ]]; then
            validate_mode "$2"
            shift
          else
            echo "Error: sessionStoreMode value is required"
            show_usage
            exit 1
          fi
          ;;
      -lsm|--lockStoreMode)
          if [[ -n "$2" ]]; then
            validate_mode "$2"
            shift
          else
            echo "Error: lockStoreMode value is required"
            show_usage
            exit 1
          fi
          ;;
      -e|--seataEnv)
          if [[ -n "$2" ]]; then
            shift
          else
            echo "Error: seataEnv value is required"
            show_usage
            exit 1
          fi
          ;;
      -n|--serverNode)
          if [[ -n "$2" ]]; then
            validate_serverNode "$2"
            shift
          else
            echo "Error: serverNode value is required"
            show_usage
            exit 1
          fi
          ;;
      --help)
          show_usage
          exit 1
          ;;
      *)
          echo "Error: Unknown argument: $key"
          show_usage
          exit 1
          ;;
  esac
  shift
done

function start_server() {
  echo "$JAVACMD ${JAVA_OPT} ${NEW_ARGS}" > ${BASEDIR}/logs/start.out 2>&1 &
  nohup $JAVACMD ${JAVA_OPT} ${NEW_ARGS} >> ${BASEDIR}/logs/start.out 2>&1 &
  echo "The seata-server is starting, you can check the ${BASEDIR}/logs/start.out"
}

function stop_server() {

  PID=`ps aux | grep -i 'seata-server' | grep java | grep -v grep | awk '{print $2}'`

  if [ -z "$PID" ]; then
    echo "No seata-server running."
    exit 1;
  fi
    echo "The seata-server(${PID}) is running..."
    kill ${PID}
    sleep 4
    echo "Send shutdown request to seata-server(${PID}) OK"

}

function replace_old_arg() {
  local old_arg="$1"
  local new_arg="$2"
  for i in "${!OLD_ARGS_ARRAY[@]}"
  do
    if [[ "${OLD_ARGS_ARRAY[$i]}" == "$old_arg" ]]; then
      OLD_ARGS_ARRAY[$i]="$new_arg"
      found=1
      for j in $(seq $((i+1)) "${#OLD_ARGS_ARRAY[@]}")
      do
        if [[ "${OLD_ARGS_ARRAY[$j]}" == "$old_arg" ]]; then
          unset OLD_ARGS_ARRAY[$j]
        else
          break
        fi
      done
      if [[ "$i+1" -lt "${#OLD_ARGS_ARRAY[@]}" && "${OLD_ARGS_ARRAY[$i+1]}" != -* ]]; then
        OLD_ARGS_ARRAY[$i+1]="${new_arg_value# }"
      fi
      break
    fi
  done
}

function restart_server() {

  PID=`ps aux | grep -i 'seata-server' | grep java | grep -v grep | awk '{print $2}'`


  #filtered
  OLD_ARGS=`ps -p $PID -o args= | grep -v "^$0" | sed -E 's/.*seata-server.jar(.*)/\1/g'`
  #Unfiltered
  #OLD_ARGS=`ps -p $PID -o args= | grep -v "^$0"`
  #echo "previous parameters  ${OLD_ARGS}"

  IFS=' ' read -r -a OLD_ARGS_ARRAY <<< "${OLD_ARGS}"
  IFS=' ' read -r -a NEW_ARGS_ARRAY <<< "${NEW_ARGS}"

  for new_arg in "${NEW_ARGS_ARRAY[@]}"
  do
    found=0
    if [[ "$new_arg" == "-p" || "$new_arg" == "--port" ]]; then
      replace_old_arg "-p" "$new_arg"
    elif [[ "$new_arg" == "-h" || "$new_arg" == "--host" ]]; then
      replace_old_arg "-h" "$new_arg"
    elif [[ "$new_arg" == "-m" || "$new_arg" == "--storeMode" ]]; then
      replace_old_arg "-m" "$new_arg"
    elif [[ "$new_arg" == "-n" || "$new_arg" == "--serverNode" ]]; then
      replace_old_arg "-n" "$new_arg"
    elif [[ "$new_arg" == "-e" || "$new_arg" == "--seataEnv" ]]; then
      replace_old_arg "-e" "$new_arg"
    elif [[ "$new_arg" == "-ssm" || "$new_arg" == "--sessionStoreMode" ]]; then
      replace_old_arg "-ssm" "$new_arg"
    elif [[ "$new_arg" == "-lsm" || "$new_arg" == "--lockStoreMode" ]]; then
      replace_old_arg "-lsm" "$new_arg"
    fi
    if [[ "$found" == 0 ]]; then
      OLD_ARGS_ARRAY+=("$new_arg")
    fi
  done

  NEW_ARGS=$(printf "%s " "${OLD_ARGS_ARRAY[@]}")

  #echo "Combined parameters ${NEW_ARGS}"

  stop_server

  echo "The seata-server restarting..."

  start_server
}


echo "开始执行函数,当前参数是: ${CMD_LINE_ARGS}"

if [ -z "${CMD_LINE_ARGS}" ]; then
  start_server
else
  case "${CMD_LINE_ARGS}" in
    start*)
        start_server
        ;;
    stop*)
        stop_server
        ;;
    restart*)
        restart_server
        ;;
    *)
        start_server
        ;;
  esac
fi
