#!/bin/sh
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

# author:wangyuewen

# shellcheck disable=SC2039,SC2162,SC2046,SC2013,SC2002,SC2086
echo -e "Please enter the host of nacos.\n请输入nacos的host [localhost]:"
read -p ">>> " host
echo -e "Please enter the port of nacos.\n请输入nacos的port [8848]:"
read -p ">>> " port
echo -e "Please enter the group of nacos.\n请输入nacos的group [SEATA_GROUP]:"
read -p ">>> " group
echo -e "Please enter the tenant of nacos.\n请输入nacos的tenant:"
read -p ">>> " tenant
echo -e "Please enter the username of nacos.\n请输入nacos的username:"
read -p ">>> " username
echo -e "Please enter the password of nacos.\n请输入nacos的password:"
read -p ">>> " password

read -p "Are you sure to continue? [y/n]" input
case $input in
    [yY]*)
        if [ -z ${host} ]; then
            host=localhost
        fi
        if [ -z ${port} ]; then
            port=8848
        fi
        if [ -z ${group} ]; then
            group="SEATA_GROUP"
        fi
        if [ -z ${tenant} ]; then
            tenant=""
        fi
        if [ -z ${username} ]; then
            username=""
        fi
        if [ -z ${password} ]; then
            password=""
        fi
        ;;
    [nN]*)
        exit
        ;;
    *)
        echo "Just enter y or n, please."
        exit
        ;;
esac

nacosAddr=$host:$port
contentType="content-type:application/json;charset=UTF-8"

echo "set nacosAddr=$nacosAddr"
echo "set group=$group"

urlencode() {
  length="${#1}"
  i=0
  while [ $length -gt $i ]; do
    char="${1:$i:1}"
    case $char in
    [a-zA-Z0-9.~_-]) printf $char ;;
    *) printf '%%%02X' "'$char" ;;
    esac
    i=`expr $i + 1`
  done
}

failCount=0
tempLog=$(mktemp -u)
addConfig() {
  dataId=`urlencode $1`
  content=`urlencode $2`
  curl -X POST -H "${contentType}" "http://$nacosAddr/nacos/v1/cs/configs?dataId=$dataId&group=$group&content=$content&tenant=$tenant&username=$username&password=$password" >"${tempLog}" 2>/dev/null
  if [ -z $(cat "${tempLog}") ]; then
    echo " Please check the cluster status. "
    exit 1
  fi
  if [ "$(cat "${tempLog}")" == "true" ]; then
    echo "Set $1=$2 successfully "
  else
    echo "Set $1=$2 failure "
    failCount=`expr $failCount + 1`
  fi
}

count=0
COMMENT_START="#"
for line in $(cat $(dirname "$PWD")/config.txt | sed s/[[:space:]]//g); do
  if [[ "$line" =~ ^"${COMMENT_START}".*  ]]; then
    continue
  fi
  count=`expr $count + 1`
	key=${line%%=*}
  value=${line#*=}
	addConfig "${key}" "${value}"
done

echo "========================================================================="
echo " Complete initialization parameters,  total-count:$count ,  failure-count:$failCount "
echo "========================================================================="

if [ ${failCount} -eq 0 ]; then
	echo " Init nacos config finished, please start seata-server. "
else
	echo " init nacos config fail. "
fi
