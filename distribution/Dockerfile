# https://hub.docker.com/orgs/seataio
FROM openjdk:8u332-jre-slim

# set label
LABEL maintainer="Seata <seata.io>"

WORKDIR /$BASE_DIR

# ADD FORM distribution
ADD bin/ /seata-server/bin
ADD ext/ /seata-server/ext
ADD target/ /seata-server/target
ADD lib/ /seata-server/lib
ADD conf/ /seata-server/conf
ADD LICENSE /seata-server/LICENSE
ADD logs /seata-server/logs

# set extra environment
ENV EXTRA_JVM_ARGUMENTS="-Djava.security.egd=file:/dev/./urandom -server -Xss512k -XX:+UnlockExperimentalVMOptions -XX:+UseContainerSupport XX:SurvivorRatio=10 -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m -XX:MaxDirectMemorySize=1024m -XX:-OmitStackTraceInFastThrow -XX:-UseAdaptiveSizePolicy -XX:+HeapDumpOnOutOfMemoryError -XX:+DisableExplicitGC -Xloggc:/var/log/seata_gc.log -verbose:gc -Dio.netty.leakDetectionLevel=advanced"
ENV LOADER_PATH="/seata-server/lib"
CMD ["bash","/seata-server/bin/seata-server.sh"]
