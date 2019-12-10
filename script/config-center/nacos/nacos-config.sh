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
	echo "./nacos-config.sh nacosAddr"
	exit -1
fi

nacosAddr=$1
echo "set nacosAddr=$nacosAddr"
error=0
contentType="content-type:application/json;charset=UTF-8"

for line in $(cat $(dirname "$PWD")/config.txt); do
	key=${line%%=*}
	value=${line#*=}
	echo "\r\n set "${key}" = "${value}

	result=$(curl -X POST -H ${contentType} "http://$nacosAddr/nacos/v1/cs/configs?dataId=$key&group=SEATA_GROUP&content=$value")

    if [[ -z ${result} ]]; then
        echo "Please check the cluster status."
        exit -1
    fi

	if [[ "$result"x == "true"x ]]; then
		echo "\033[42;37m $result \033[0m"
	else
		echo "\033[41;37 $result \033[0m"
		let error++
	fi
done

if [[ ${error} -eq 0 ]]; then
	echo "\r\n\033[42;37m init nacos config finished, please start seata-server. \033[0m"
else
	echo "\r\n\033[41;33m init nacos config fail. \033[0m"
fi
exit 0