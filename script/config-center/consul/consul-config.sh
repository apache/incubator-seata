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

error=0
for line in $(cat $(dirname "$PWD")/config.txt); do
    key=${line%%=*}
	value=${line#*=}
	echo "Set" "${key}" "=" "${value}"
    result=$(curl -X PUT -H ${contentType} -d ${value} "http://$consulAddr/v1/kv/$key")
    echo "Response:$result"

    if [[ -z ${result} ]]; then
        echo "Please check the cluster status."
        exit 1
    fi

    if [[ ! ${result} =~ "true" ]]; then
		(( error ++ ))
	fi
done

if [[ ${error} -eq 0 ]]; then
	echo "Init consul config finished, please start seata-server."
else
	echo "Init consul config fail."
fi
exit 0
