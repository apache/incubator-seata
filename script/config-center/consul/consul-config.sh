#!/usr/bin/env bash
# Copyright 1999-2019 Seata.io Group.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at、
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

while getopts ":h:p:" opt
do
  case $opt in
  h)
    host=$OPTARG
    ;;
  p)
    port=$OPTARG
    ;;
  ?)
    echo " USAGE OPTION: $0 [-h host] [-p port] "
    exit 1
    ;;
  esac
done

if [[ -z ${host} ]]; then
    host=localhost
fi
if [[ -z ${port} ]]; then
    port=8500
fi

consulAddr=$host:$port
contentType="content-type:application/json;charset=UTF-8"
echo "Set consulAddr=$consulAddr"

failCount=0
tempLog=$(mktemp -u)
function addConfig() {
  curl -X PUT -H "${1}" -d "${2}" "http://$3/v1/kv/$4" >"${tempLog}" 2>/dev/null
  if [[ -z $(cat "${tempLog}") ]]; then
    echo " Please check the cluster status. "
    exit 1
  fi
  if [[ $(cat "${tempLog}") =~ "true" ]]; then
    echo "Set $4=$2 successfully "
  else
    echo "Set $4=$2 failure "
    (( failCount++ ))
 fi
}

count=0
COMMENT_START="#"
for line in $(cat $(dirname "$PWD")/config.txt | sed s/[[:space:]]//g); do
  if [[ "$line" =~ ^"${COMMENT_START}".*  ]]; then
        continue
  fi
  (( count++ ))
  key=${line%%=*}
  value=${line#*=}
  addConfig "${contentType}" "${value}" "${consulAddr}" "${key}"
done

echo "========================================================================="
echo " Complete initialization parameters,  total-count:$count ,  failure-count:$failCount "
echo "========================================================================="

if [[ ${failCount} -eq 0 ]]; then
  echo " Init consul config finished, please start seata-server. "
else
  echo " Init consul config fail. "
fi
