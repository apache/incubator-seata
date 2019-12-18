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
		echo "\033[31m Invalid parameter，please refer to apollo-params.txt \033[0m"
		exit 1
		;;
	esac
done

if [[ -z ${portalAddr} || -z ${env} || -z ${appId} || -z ${clusterName} || -z ${namespaceName}
      || -z ${dataChangeCreatedBy} || -z ${releasedBy} || -z ${token} ]]; then
	echo "\033[31m Incomplete parameters, please fill in the complete parameters: portalAddr:$portalAddr,
            env:$env, appId:$appId, clusterName:$clusterName, namespaceName:$namespaceName,
            dataChangeCreatedBy:$dataChangeCreatedBy, releasedBy:$releasedBy, token:$token \033[0m"
	exit 1
fi

contentType="content-type:application/json;charset=UTF-8"
authorization="Authorization:$token"
publishBody="{\"releaseTitle\":\"$(date +%Y%m%d%H%M%S)\",\"releaseComment\":\"\",\"releasedBy\":\"${releasedBy}\"}"

echo "Portal address is ${portalAddr}"
echo "Env is ${env}"
echo "AppId is ${appId}"
echo "ClusterName is ${clusterName}"
echo "NamespaceName is ${namespaceName}"
echo "DataChangeCreatedBy is ${dataChangeCreatedBy}"
echo "ReleasedBy is ${releasedBy}"
echo "Token is ${token}"

failCount=0
tempLog=$(mktemp -t apollo-config.log)
function addConfig() {
	curl -X POST -H ${1} -H ${2} -d ${3} "http://${4}/openapi/v1/envs/${5}/apps/${6}/clusters/${7}/namespaces/${8}/items" >${tempLog} 2>/dev/null
	log=$(cat ${tempLog})
	if [[ ${log} =~ ":401" || ${log} =~ ":403"
	    || ${log} =~ ":404" || ${log} =~ ":405"
	      || ${log} =~ ":500" || ! ${log} =~ "{" ]]; then
	  echo "set $9=${10}\033[31m failure \033[0m"
		(( failCount++ ))
	else
	  echo "set $9=${10}\033[32m successfully \033[0m"
	fi
}

function publishConfig() {
	curl -X POST -H ${1} -H ${2} -d ${3} "http://${4}/openapi/v1/envs/${5}/apps/${6}/clusters/${7}/namespaces/${8}/releases" >${tempLog} 2>/dev/null
	log=$(cat ${tempLog})
	if [[ ${log} =~ ":401" || ${log} =~ ":403"
	    || ${log} =~ ":404" || ${log} =~ ":405"
	      || ${log} =~ ":500" || ! ${log} =~ "{" ]]; then
	  echo "\033[31m Publish fail \033[0m"
	  exit 1
	else
	  echo "\033[32m Publish successfully, please start seata-server. \033[0m"
	fi
}

count=0
for line in $(cat $(dirname "$PWD")/config.txt); do
	(( count++ ))
	key=${line%%=*}
	value=${line#*=}
	body="{\"key\":\"${key}\",\"value\":\"${value}\",\"comment\":\"\",\"dataChangeCreatedBy\":\"${dataChangeCreatedBy}\"}"
	addConfig ${contentType} ${authorization} ${body} ${portalAddr} ${env} ${appId} ${clusterName} ${namespaceName} ${key} ${value}
done

echo "========================================================================="
echo " Complete initialization parameters, \033[32m total-count:$count \033[0m, \033[31m failure-count:$failCount \033[0m"
echo "========================================================================="

if [[ $failCount -eq 0 ]]; then
  read -p "Publish now, y/n: " result
  if [[ ${result} == "y" ]]; then
    publishConfig ${contentType} ${authorization} ${publishBody} ${portalAddr} ${env} ${appId} ${clusterName} ${namespaceName}
  else
    echo "Remember to publish later..."
  fi
else
  echo "\033[31m init apollo config fail. \033[0m"
fi
