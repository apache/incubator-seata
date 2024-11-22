#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# the Dockerfile support x86 & arrch64
# build：
# 1. mvn -Prelease-seata -Dmaven.test.skip=true clean install -U
# 2. cd distribution/target/apache-seata-x.x.x-SNAPSHOT-incubating-bin/seata-namingserver/
# 3. docker build --no-cache --build-arg SEATA_VERSION=2.2.0-SNAPSHOT -t seata-namingserver:2.2.0-dev .
#
# run:
# 1. docker run --name=seata-namingserver -d seata-namingserver:2.2.0-dev
#
# https://hub.docker.com/r/apache/seata-namingserver
FROM eclipse-temurin:8u422-b05-jdk

# set label
LABEL maintainer="Seata <seata.apache.org>"

WORKDIR /$BASE_DIR

# ADD FORM distribution
ADD bin/ /seata-namingserver/bin
ADD lib/ /seata-namingserver/lib
ADD conf/ /seata-namingserver/conf
ADD target /seata-namingserver/target
ADD LICENSE /LICENSE
ADD NOTICE /NOTICE

# set extra environment
ENV LOADER_PATH="/seata-namingserver/lib"
ENV TZ="Asia/Shanghai"
CMD ["bash","-c","/seata-namingserver/bin/seata-namingserver.sh && tail -f /dev/null"]
