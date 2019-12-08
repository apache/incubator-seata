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

# apollo open api, click on the link for details:
# https://github.com/ctripcorp/apollo/wiki/Apollo%E5%BC%80%E6%94%BE%E5%B9%B3%E5%8F%B0

# add config: http://{portal_address}/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items
# publish config: http://{portal_address}/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/releases


for line in $(cat apollo-params.txt); do
	key=${line%%=*}
	value=${line#*=}
	case ${key} in
	portalAddr)
		portalAddr=${value}
		;;
	env)
		env=${value}
		;;
	appId)
		appId=${value}
		;;
	clusterName)
		clusterName=${value}
		;;
	namespaceName)
		namespaceName=${value}
		;;
	dataChangeCreatedBy)
		dataChangeCreatedBy=${value}
		;;
	releasedBy)
		releasedBy=${value}
		;;
	token)
		token=${value}
		;;
	*)
		echo "invalid param"
		exit -1
		;;
	esac
done

if [[ -z ${portalAddr} || -z ${env} || -z ${appId} || -z ${clusterName} || -z ${namespaceName} || -z ${dataChangeCreatedBy} || -z ${releasedBy} || -z ${token} ]]; then
	echo "Incomplete parameters, please fill in the complete parameters: portalAddr:$portalAddr,
            env:$env, appId:$appId, clusterName:$clusterName, namespaceName:$namespaceName,
            dataChangeCreatedBy:$dataChangeCreatedBy, releasedBy:$releasedBy, token:$token"
	exit -1
fi

contentType="Content-type:application/json;charset=UTF-8"
authorization="Authorization:$token"
publishBody="{\"releaseTitle\":\"$(date +%Y%m%d%H%M%S)\",\"releaseComment\":\"\",\"releasedBy\":\"${releasedBy}\"}"

echo "portal address is ${portalAddr}"
echo "env is ${env}"
echo "appId is ${appId}"
echo "clusterName is ${clusterName}"
echo "namespaceName is ${namespaceName}"
echo "dataChangeCreatedBy is ${dataChangeCreatedBy}"
echo "releasedBy is ${releasedBy}"
echo "token is ${token}"

failCount=0
function addConfig() {
	result=$(curl -X POST -H ${1} -H ${2} -d ${3} "http://${4}/openapi/v1/envs/${5}/apps/${6}/clusters/${7}/namespaces/${8}/items")
	if [[ ${result} =~ "400" || ${result} =~ "401" || ${result} =~ "403" || ${result} =~ "404" || ${result} =~ "405" || ${result} =~ "500" || ! ${result} =~ "{" ]]; then
		((failCount++))
	fi
	echo ${result}
}

function publishConfig() {
	result=$(curl -X POST -H ${1} -H ${2} -d ${3} "http://${4}/openapi/v1/envs/${5}/apps/${6}/clusters/${7}/namespaces/${8}/releases")
	echo ${result}
}

count=0
for line in $(cat $(dirname "$PWD")/config.txt); do
	((count++))
	key=${line%%=*}
	value=${line#*=}
	echo "\r\n set" "${key}" "=" "${value}"
	body="{\"key\":\"${key}\",\"value\":\"${value}\",\"comment\":\"\",\"dataChangeCreatedBy\":\"${dataChangeCreatedBy}\"}"
	echo "$body"
	addConfig ${contentType} ${authorization} ${body} ${portalAddr} ${env} ${appId} ${clusterName} ${namespaceName}
done
echo "=================================================================="
echo " Init params success, total count:$count, fail count:$failCount "
echo "=================================================================="

read -p "Publish now, y/n: " result
if [[ ${result} == "y" ]]; then
	publishConfig ${contentType} ${authorization} ${publishBody} ${portalAddr} ${env} ${appId} ${clusterName} ${namespaceName}
else
	echo "Remember to publish later..."
fi
exit 0
