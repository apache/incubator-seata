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

if [[ $# != 1 ]]; then
  echo "USAGE: $0 consulAddr"
  exit 1
fi
consulAddr=$1
contentType="content-type:application/json;charset=UTF-8"
echo "Set consulAddr=$consulAddr"

failCount=0
tempLog=$(mktemp -t consul-config.log)
function addConfig() {
  curl -X PUT -H ${1} -d ${2} "http://$3/v1/kv/$4" >${tempLog} 2>/dev/null
  if [[ -z $(cat ${tempLog}) ]]; then
    echo "\033[31m Please check the cluster status. \033[0m"
    exit 1
  fi
  if [[ $(cat ${tempLog}) =~ "true" ]]; then
    echo "Set $4=$2\033[32m successfully \033[0m"
  else
    echo "Set $4=$2\033[31m failure \033[0m"
    (( failCount++ ))
 fi
}

count=0
for line in $(cat $(dirname "$PWD")/config.txt); do
  (( count++ ))
  key=${line%%=*}
  value=${line#*=}
  addConfig ${contentType} ${value} ${consulAddr} ${key}
done

echo "========================================================================="
echo " Complete initialization parameters, \033[32m total-count:$count \033[0m, \033[31m failure-count:$failCount \033[0m"
echo "========================================================================="

if [[ ${failCount} -eq 0 ]]; then
  echo "\033[32m Init consul config finished, please start seata-server. \033[0m"
else
  echo "\033[31m Init consul config fail. \033[0m"
fi
