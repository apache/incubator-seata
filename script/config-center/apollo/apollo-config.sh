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

while getopts ":h:p:e:a:c:n:d:r:t:" opt
do
  case $opt in
  h)
    host=$OPTARG
    ;;
  p)
    port=$OPTARG
    ;;
  e)
    env=$OPTARG
    ;;
  a)
    appId=$OPTARG
    ;;
  c)
    clusterName=$OPTARG
    ;;
  n)
    namespaceName=$OPTARG
    ;;
  d)
    dataChangeCreatedBy=$OPTARG
    ;;
  r)
    releasedBy=$OPTARG
    ;;
  t)
    token=$OPTARG
    ;;
  ?)
    echo " USAGE OPTION: $0 [-h host] [-p port] [-e env] [a appId] [-c clusterName] [-n namespaceName] [-d dataChangeCreatedBy] [-r releasedBy] [-t token] "
    exit 1
    ;;
  esac
done

if [[ -z ${host} ]]; then
    host=localhost
fi
if [[ -z ${port} ]]; then
    port=8070
fi
if [[ -z ${env} ]]; then
    env=DEV
fi
if [[ -z ${appId} ]]; then
    appId=seata-server
fi
if [[ -z ${clusterName} ]]; then
    clusterName=default
fi
if [[ -z ${namespaceName} ]]; then
    namespaceName=application
fi
if [[ -z ${dataChangeCreatedBy} ]]; then
    echo " dataChangeCreatedBy is empty, please usage option: [-d dataChangeCreatedBy] "
    exit 1
fi
if [[ -z ${releasedBy} ]]; then
    echo " releasedBy is empty, please usage option: [-r releasedBy] "
    exit 1
fi
if [[ -z ${token} ]]; then
    echo " token is empty, please usage option: [-t token] "
    exit 1
fi

portalAddr=$host:$port
contentType="content-type:application/json;charset=UTF-8"
authorization="Authorization:$token"
publishBody="{\"releaseTitle\":\"$(date +%Y%m%d%H%M%S)\",\"releaseComment\":\"\",\"releasedBy\":\"${releasedBy}\"}"

echo "portalAddr is ${portalAddr}"
echo "env is ${env}"
echo "appId is ${appId}"
echo "clusterName is ${clusterName}"
echo "namespaceName is ${namespaceName}"
echo "dataChangeCreatedBy is ${dataChangeCreatedBy}"
echo "releasedBy is ${releasedBy}"
echo "token is ${token}"

failCount=0
tempLog=$(mktemp -u)
function addConfig() {
	curl -X POST -H "${1}" -H "${2}" -d "${3}" "http://${4}/openapi/v1/envs/${5}/apps/${6}/clusters/${7}/namespaces/${8}/items" >"${tempLog}" 2>/dev/null
	log=$(cat "${tempLog}")
	if [[ ${log} =~ ":401" || ${log} =~ ":403"
	    || ${log} =~ ":404" || ${log} =~ ":405"
	      || ${log} =~ ":500" || ! ${log} =~ "{" ]]; then
	  echo "set $9=${10} failure "
		(( failCount++ ))
	else
	  echo "set $9=${10} successfully "
	fi
}

function publishConfig() {
	curl -X POST -H "${1}" -H "${2}" -d "${3}" "http://${4}/openapi/v1/envs/${5}/apps/${6}/clusters/${7}/namespaces/${8}/releases" >"${tempLog}" 2>/dev/null
	log=$(cat "${tempLog}")
	if [[ ${log} =~ ":401" || ${log} =~ ":403"
	    || ${log} =~ ":404" || ${log} =~ ":405"
	      || ${log} =~ ":500" || ! ${log} =~ "{" ]]; then
	  echo " Publish fail "
	  exit 1
	else
	  echo " Publish successfully, please start seata-server. "
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
  body="{\"key\":\"${key}\",\"value\":\"${value}\",\"comment\":\"\",\"dataChangeCreatedBy\":\"${dataChangeCreatedBy}\"}"
  addConfig ${contentType} "${authorization}" "${body}" "${portalAddr}" "${env}" "${appId}" "${clusterName}" "${namespaceName}" "${key}" "${value}"
done

echo "========================================================================="
echo " Complete initialization parameters,  total-count:$count ,  failure-count:$failCount "
echo "========================================================================="

if [[ $failCount -eq 0 ]]; then
  read -p "Publish now, y/n: " result
  if [[ ${result} == "y" ]]; then
    publishConfig "${contentType}" "${authorization}" "${publishBody}" "${portalAddr}" "${env}" "${appId}" "${clusterName}" "${namespaceName}"
  else
    echo "Remember to publish later..."
  fi
else
  echo " init apollo config fail. "
fi
