#!/usr/bin/env bash
# ----------------------------------------------------------------------------
#  Copyright 2001-2006 The Apache Software Foundation.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
# ----------------------------------------------------------------------------
#
#   Copyright (c) 2001-2006 The Apache Software Foundation.  All rights
#   reserved.

if [[ $# != 1 ]]; then
	echo "./consul-config.sh consulAddr"
	exit -1
fi
consulAddr=$1
contentType="Content-type:application/json;charset=UTF-8"
echo "set consulAddr=$consulAddr"

error=0
for line in $(cat $(dirname "$PWD")/config.txt); do
    key=${line%%=*}
	value=${line#*=}
	echo "set" "${key}" "=" "${value}"
    result=$(curl -X PUT -H ${contentType} -d ${value} "http://$consulAddr/v1/kv/$key")
    echo "response:$result"

    if [[ -z ${result} ]]; then
        echo "Please check the cluster status."
        exit -1
    fi

    if [[ ! ${result} =~ "true" ]]; then
		(( error ++ ))
	fi
done

if [[ ${error} -eq 0 ]]; then
	echo "init consul config finished, please start seata-server."
else
	echo "init consul config fail."
fi
exit 0
