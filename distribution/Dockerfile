# https://hub.docker.com/_/openjdk
FROM centos:7.5.1804

# set label
LABEL maintainer="seata <niao.shuai123@163.com>"

# set environment
ENV SEATA_USER="seata" \
    JAVA_HOME="/usr/lib/jvm/java-1.8.0-openjdk" \
    JAVA="/usr/lib/jvm/java-1.8.0-openjdk/bin/java" \
    TIME_ZONE="Asia/Shanghai" 

ARG SEATA_VERSION=0.9.0

WORKDIR /$BASE_DIR

RUN set -x \
    && yum update -y \
    && yum install -y java-1.8.0-openjdk java-1.8.0-openjdk-devel wget iputils nc vim libcurl \
    && wget  https://github.com/seata/seata/releases/download/v${SEATA_VERSION}/seata-server-${SEATA_VERSION}.tar.gz -P /home \
    && tar -xzvf /home/seata-server-${SEATA_VERSION}.tar.gz -C /opt \
    && rm -rf /home/seata-server-${SEATA_VERSION}.tar.gz \
    && ln -snf /usr/share/zoneinfo/$TIME_ZONE /etc/localtime && echo '$TIME_ZONE' > /etc/timezone \
    && yum clean all

# set extra environment
ENV SEATA_IP ${SEATA_IP}
ENV SEATA_PORT ${SEATA_PORT:-8091}
ENV STORE_MODE ${STORE_MODE:-file}
ENV SERVER_NODE {SERVER_NODE:-1}
ENV EXTRA_JVM_ARGUMENTS="-XX:MaxDirectMemorySize=1024M"

CMD ["sh","/opt/seata/bin/seata-server.sh"]
EXPOSE ${SEATA_PORT}
