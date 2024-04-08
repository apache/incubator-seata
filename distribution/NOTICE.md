Due to license compatibility issues, we cannot include jar dependencies such as mysql, mariadb, oracle, etc., in the distribution package.
Please copy database driver dependencies, such as `mysql-connector-java.jar`, to this directory. The following is an example of a directory structure:

```aidl
.
├── LICENSE
├── Dockerfile
├── ext
│   └── apm-skywalking
│       ├── skywalking-agent.jar
│       └── plugins
├── script
│   ├── logstash
│   │   └── config
│   ├── server
│   │   ├── helm
│   │   ├── db
│   │   ├── docker-compose
│   │   └── kubernetes
│   └── config-center
│       ├── README.md
│       ├── config.txt
│       ├── apollo
│       ├── consul
│       ├── etcd3
│       ├── nacos
│       └── zk
├── target
│   └── seata-server.jar
├── conf
│   ├── application.raft.example.yml
│   ├── application.example.yml
│   ├── logback-spring.xml
│   ├── application.yml
│   └── logback
│       ├── file-appender.xml
│       ├── kafka-appender.xml
│       ├── console-appender.xml
│       ├── logstash-appender.xml
│       └── metric-appender.xml
├── bin
│   ├── seata-server.bat
│   ├── seata-setup.sh
│   ├── seata-server.sh
│   └── nohup.out
├── logs
└── lib
    ├── DmJdbcDriver18-8.1.2.192.jar
    ├── HikariCP-4.0.3.jar
    ├── animal-sniffer-annotations-1.18.jar
    ├── annotations-4.1.1.4.jar
    ├── ant-launcher-1.10.12.jar
    ├── antlr-2.7.7.jar
    ├── antlr-runtime-3.4.jar
    ├── aopalliance-1.0.jar
    ├── apollo-client-2.0.1.jar
    ├── bolt-1.6.4.jar
    ├── byte-buddy-1.12.23.jar
    ├── checker-qual-3.5.0.jar
    ├── commons-codec-1.15.jar
    ├── commons-configuration-1.10.jar
    ├── commons-io-2.8.0.jar
    ├── commons-jxpath-1.3.jar
    ├── commons-lang-2.6.jar
    ├── commons-logging-1.2.jar
    ├── commons-math-2.2.jar
    ├── config-1.2.1.jar
    ├── consul-api-1.4.2.jar
    ├── dexx-collections-0.2.jar
    ├── eureka-client-1.10.18.jar
    ├── failureaccess-1.0.1.jar
    ├── fastjson-1.2.83.jar
    ├── grpc-context-1.27.1.jar
    ├── grpc-core-1.27.1.jar
    ├── grpc-grpclb-1.27.1.jar
    ├── grpc-protobuf-1.27.1.jar
    ├── grpc-protobuf-lite-1.27.1.jar
    ├── gson-2.9.1.jar
    ├── guava-32.1.3-jre.jar
    ├── guice-5.0.1.jar
    ├── h2-2.1.214.jar
    ├── hessian-4.0.63.jar
    ├── httpasyncclient-4.1.5.jar
    ├── httpclient-4.5.14.jar
    ├── httpcore-nio-4.4.16.jar
    ├── jackson-annotations-2.13.5.jar
    ├── jackson-databind-2.13.5.jar
    ├── jackson-datatype-jdk8-2.13.5.jar
    ├── jackson-module-parameter-names-2.13.5.jar
    ├── javax.inject-1.jar
    ├── jcommander-1.82.jar
    ├── jctools-core-2.1.1.jar
    ├── jedis-3.8.0.jar
    ├── jersey-apache-client4-1.19.1.jar
    ├── jersey-client-1.19.1.jar
    ├── jersey-core-1.19.1.jar
    ├── jetcd-common-0.5.0.jar
    ├── jetcd-resolver-0.5.0.jar
    ├── jettison-1.5.4.jar
    ├── jjwt-api-0.10.5.jar
    ├── jjwt-jackson-0.10.5.jar
    ├── jna-5.5.0.jar
    ├── joda-time-2.3.jar
    ├── jraft-core-1.3.13.jar
    ├── jsr305-3.0.2.jar
    ├── jsr311-api-1.1.1.jar
    ├── kafka-clients-3.1.2.jar
    ├── kryo-5.4.0.jar
    ├── kryo-serializers-0.45.jar
    ├── listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar
    ├── logback-classic-1.2.12.jar
    ├── logback-kafka-appender-0.2.0-RC2.jar
    ├── logstash-logback-encoder-6.5.jar
    ├── lz4-java-1.7.1.jar
    ├── metrics-core-4.2.21.jar
    ├── minlog-1.3.1.jar
    ├── mxparser-1.2.2.jar
    ├── nacos-api-1.4.6.jar
    ├── nacos-common-1.4.6.jar
    ├── netflix-eventbus-0.3.0.jar
    ├── netflix-infix-0.3.0.jar
    ├── netty-buffer-4.1.100.Final.jar
    ├── netty-codec-4.1.100.Final.jar
    ├── netty-codec-dns-4.1.100.Final.jar
    ├── netty-codec-haproxy-4.1.100.Final.jar
    ├── netty-codec-http2-4.1.100.Final.jar
    ├── netty-codec-memcache-4.1.100.Final.jar
    ├── netty-codec-redis-4.1.100.Final.jar
    ├── netty-codec-stomp-4.1.100.Final.jar
    ├── netty-codec-xml-4.1.100.Final.jar
    ├── netty-common-4.1.100.Final.jar
    ├── netty-handler-ssl-ocsp-4.1.100.Final.jar
    ├── netty-resolver-4.1.100.Final.jar
    ├── netty-resolver-dns-classes-macos-4.1.100.Final.jar
    ├── netty-resolver-dns-native-macos-4.1.100.Final-osx-aarch_64.jar
    ├── netty-resolver-dns-native-macos-4.1.100.Final-osx-x86_64.jar
    ├── netty-transport-4.1.100.Final.jar
    ├── netty-transport-classes-epoll-4.1.100.Final.jar
    ├── netty-transport-classes-kqueue-4.1.100.Final.jar
    ├── netty-transport-native-epoll-4.1.100.Final-linux-aarch_64.jar
    ├── netty-transport-native-epoll-4.1.100.Final-linux-x86_64.jar
    ├── netty-transport-native-epoll-4.1.100.Final.jar
    ├── netty-transport-native-kqueue-4.1.100.Final-osx-aarch_64.jar
    ├── netty-transport-native-kqueue-4.1.100.Final-osx-x86_64.jar
    ├── netty-transport-rxtx-4.1.100.Final.jar
    ├── netty-transport-sctp-4.1.100.Final.jar
    ├── netty-transport-udt-4.1.100.Final.jar
    ├── perfmark-api-0.19.0.jar
    ├── postgresql-42.3.8.jar
    ├── proto-google-common-protos-1.17.0.jar
    ├── reflectasm-1.11.9.jar
    ├── registry-client-all-6.3.0.jar
    ├── seata-common-2.1.0-SNAPSHOT.jar
    ├── seata-compressor-all-2.1.0-SNAPSHOT.jar
    ├── seata-compressor-bzip2-2.1.0-SNAPSHOT.jar
    ├── seata-compressor-deflater-2.1.0-SNAPSHOT.jar
    ├── seata-compressor-gzip-2.1.0-SNAPSHOT.jar
    ├── seata-compressor-lz4-2.1.0-SNAPSHOT.jar
    ├── seata-compressor-zip-2.1.0-SNAPSHOT.jar
    ├── seata-config-all-2.1.0-SNAPSHOT.jar
    ├── seata-config-apollo-2.1.0-SNAPSHOT.jar
    ├── seata-config-consul-2.1.0-SNAPSHOT.jar
    ├── seata-config-core-2.1.0-SNAPSHOT.jar
    ├── seata-config-etcd3-2.1.0-SNAPSHOT.jar
    ├── seata-config-nacos-2.1.0-SNAPSHOT.jar
    ├── seata-config-spring-cloud-2.1.0-SNAPSHOT.jar
    ├── seata-config-zk-2.1.0-SNAPSHOT.jar
    ├── seata-core-2.1.0-SNAPSHOT.jar
    ├── seata-discovery-consul-2.1.0-SNAPSHOT.jar
    ├── seata-discovery-custom-2.1.0-SNAPSHOT.jar
    ├── seata-discovery-etcd3-2.1.0-SNAPSHOT.jar
    ├── seata-discovery-nacos-2.1.0-SNAPSHOT.jar
    ├── seata-discovery-redis-2.1.0-SNAPSHOT.jar
    ├── seata-discovery-sofa-2.1.0-SNAPSHOT.jar
    ├── seata-metrics-api-2.1.0-SNAPSHOT.jar
    ├── seata-metrics-core-2.1.0-SNAPSHOT.jar
    ├── seata-metrics-registry-compact-2.1.0-SNAPSHOT.jar
    ├── seata-serializer-all-2.1.0-SNAPSHOT.jar
    ├── seata-serializer-hessian-2.1.0-SNAPSHOT.jar
    ├── seata-serializer-kryo-2.1.0-SNAPSHOT.jar
    ├── seata-spring-autoconfigure-server-2.1.0-SNAPSHOT.jar
    ├── servo-core-0.12.21.jar
    ├── simpleclient-0.15.0.jar
    ├── simpleclient_common-0.15.0.jar
    ├── simpleclient_tracer_common-0.15.0.jar
    ├── simpleclient_tracer_otel-0.15.0.jar
    ├── simpleclient_tracer_otel_agent-0.15.0.jar
    ├── snakeyaml-2.0.jar
    ├── snappy-java-1.1.8.4.jar
    ├── sofa-common-tools-1.0.12.jar
    ├── spring-aop-5.3.31.jar
    ├── spring-beans-5.3.31.jar
    ├── spring-boot-autoconfigure-2.7.18.jar
    ├── spring-boot-starter-logging-2.7.18.jar
    ├── spring-boot-starter-security-2.7.18.jar
    ├── spring-boot-starter-tomcat-2.7.18.jar
    ├── spring-boot-starter-web-2.7.18.jar
    ├── spring-context-5.3.31.jar
    ├── spring-core-5.3.31.jar
    ├── spring-expression-5.3.31.jar
    ├── spring-jcl-5.3.31.jar
    ├── spring-security-config-5.7.11.jar
    ├── spring-security-web-5.7.11.jar
    ├── stringtemplate-3.2.1.jar
    ├── tomcat-embed-core-9.0.82.jar
    ├── tomcat-embed-el-9.0.82.jar
    ├── tomcat-embed-websocket-9.0.82.jar
    ├── zkclient-0.11.jar
    ├── zookeeper-3.5.9.jar
    ├── zookeeper-jute-3.5.9.jar
    ├── zstd-jni-1.5.0-4.jar
    ├── ant-1.10.12.jar
    ├── apollo-core-2.0.1.jar
    ├── archaius-core-0.7.6.jar
    ├── asm-6.0.jar
    ├── audience-annotations-0.5.0.jar
    ├── commons-compiler-3.1.10.jar
    ├── commons-dbcp2-2.9.0.jar
    ├── commons-pool-1.6.jar
    ├── commons-pool2-2.11.1.jar
    ├── compactmap-2.0.jar
    ├── disruptor-3.3.7.jar
    ├── druid-1.2.7.jar
    ├── error_prone_annotations-2.21.1.jar
    ├── failsafe-2.3.3.jar
    ├── grpc-api-1.27.1.jar
    ├── grpc-netty-1.27.1.jar
    ├── grpc-stub-1.27.1.jar
    ├── hessian-4.0.3.jar
    ├── httpcore-4.4.16.jar
    ├── j2objc-annotations-2.8.jar
    ├── jackson-core-2.13.5.jar
    ├── jackson-datatype-jsr310-2.13.5.jar
    ├── jakarta.annotation-api-1.3.5.jar
    ├── janino-3.1.10.jar
    ├── javax.servlet-api-4.0.1.jar
    ├── jetcd-core-0.5.0.jar
    ├── jjwt-impl-0.10.5.jar
    ├── jul-to-slf4j-1.7.36.jar
    ├── logback-core-1.2.12.jar
    ├── nacos-client-1.4.6.jar
    ├── netty-all-4.1.100.Final.jar
    ├── netty-codec-http-4.1.100.Final.jar
    ├── netty-codec-mqtt-4.1.100.Final.jar
    ├── netty-codec-smtp-4.1.100.Final.jar
    ├── netty-codec-socks-4.1.100.Final.jar
    ├── netty-handler-4.1.100.Final.jar
    ├── netty-handler-proxy-4.1.100.Final.jar
    ├── netty-resolver-dns-4.1.100.Final.jar
    ├── netty-transport-native-unix-common-4.1.100.Final.jar
    ├── objenesis-3.2.jar
    ├── protobuf-java-3.16.3.jar
    ├── protobuf-java-util-3.11.0.jar
    ├── rocksdbjni-7.7.3.jar
    ├── seata-compressor-zstd-2.1.0-SNAPSHOT.jar
    ├── seata-console-2.1.0-SNAPSHOT.jar
    ├── seata-discovery-all-2.1.0-SNAPSHOT.jar
    ├── seata-discovery-core-2.1.0-SNAPSHOT.jar
    ├── seata-discovery-eureka-2.1.0-SNAPSHOT.jar
    ├── seata-discovery-zk-2.1.0-SNAPSHOT.jar
    ├── seata-metrics-all-2.1.0-SNAPSHOT.jar
    ├── seata-metrics-exporter-prometheus-2.1.0-SNAPSHOT.jar
    ├── seata-serializer-protobuf-2.1.0-SNAPSHOT.jar
    ├── seata-serializer-seata-2.1.0-SNAPSHOT.jar
    ├── seata-spring-autoconfigure-core-2.1.0-SNAPSHOT.jar
    ├── simpleclient_httpserver-0.15.0.jar
    ├── slf4j-api-1.7.36.jar
    ├── spring-boot-2.7.18.jar
    ├── spring-boot-starter-2.7.18.jar
    ├── spring-boot-starter-json-2.7.18.jar
    ├── spring-security-core-5.7.11.jar
    ├── spring-security-crypto-5.7.11.jar
    ├── spring-web-5.3.31.jar
    ├── spring-webmvc-5.3.31.jar
    ├── xstream-1.4.20.jar
    └── jdbc
        ├── mysql-connector-java-8.0.28.jar
        └── NOTICE.md

```

---

由于license兼容性问题，我们不能将mysql、mariadb、oracle等jar依赖包含在发布包中。
请将数据库driver相关依赖例如：`mysql-connector-java.jar`，拷贝到此目录下。目录结构示例如下：
```aidl
.
├── LICENSE
├── Dockerfile
├── ext
│   └── apm-skywalking
│       ├── skywalking-agent.jar
│       └── plugins
├── script
│   ├── logstash
│   │   └── config
│   ├── server
│   │   ├── helm
│   │   ├── db
│   │   ├── docker-compose
│   │   └── kubernetes
│   └── config-center
│       ├── README.md
│       ├── config.txt
│       ├── apollo
│       ├── consul
│       ├── etcd3
│       ├── nacos
│       └── zk
├── target
│   └── seata-server.jar
├── conf
│   ├── application.raft.example.yml
│   ├── application.example.yml
│   ├── logback-spring.xml
│   ├── application.yml
│   └── logback
│       ├── file-appender.xml
│       ├── kafka-appender.xml
│       ├── console-appender.xml
│       ├── logstash-appender.xml
│       └── metric-appender.xml
├── bin
│   ├── seata-server.bat
│   ├── seata-setup.sh
│   ├── seata-server.sh
│   └── nohup.out
├── logs
└── lib
    ├── DmJdbcDriver18-8.1.2.192.jar
    ├── HikariCP-4.0.3.jar
    ├── animal-sniffer-annotations-1.18.jar
    ├── annotations-4.1.1.4.jar
    ├── ant-launcher-1.10.12.jar
    ├── antlr-2.7.7.jar
    ├── antlr-runtime-3.4.jar
    ├── aopalliance-1.0.jar
    ├── apollo-client-2.0.1.jar
    ├── bolt-1.6.4.jar
    ├── byte-buddy-1.12.23.jar
    ├── checker-qual-3.5.0.jar
    ├── commons-codec-1.15.jar
    ├── commons-configuration-1.10.jar
    ├── commons-io-2.8.0.jar
    ├── commons-jxpath-1.3.jar
    ├── commons-lang-2.6.jar
    ├── commons-logging-1.2.jar
    ├── commons-math-2.2.jar
    ├── config-1.2.1.jar
    ├── consul-api-1.4.2.jar
    ├── dexx-collections-0.2.jar
    ├── eureka-client-1.10.18.jar
    ├── failureaccess-1.0.1.jar
    ├── fastjson-1.2.83.jar
    ├── grpc-context-1.27.1.jar
    ├── grpc-core-1.27.1.jar
    ├── grpc-grpclb-1.27.1.jar
    ├── grpc-protobuf-1.27.1.jar
    ├── grpc-protobuf-lite-1.27.1.jar
    ├── gson-2.9.1.jar
    ├── guava-32.1.3-jre.jar
    ├── guice-5.0.1.jar
    ├── h2-2.1.214.jar
    ├── hessian-4.0.63.jar
    ├── httpasyncclient-4.1.5.jar
    ├── httpclient-4.5.14.jar
    ├── httpcore-nio-4.4.16.jar
    ├── jackson-annotations-2.13.5.jar
    ├── jackson-databind-2.13.5.jar
    ├── jackson-datatype-jdk8-2.13.5.jar
    ├── jackson-module-parameter-names-2.13.5.jar
    ├── javax.inject-1.jar
    ├── jcommander-1.82.jar
    ├── jctools-core-2.1.1.jar
    ├── jedis-3.8.0.jar
    ├── jersey-apache-client4-1.19.1.jar
    ├── jersey-client-1.19.1.jar
    ├── jersey-core-1.19.1.jar
    ├── jetcd-common-0.5.0.jar
    ├── jetcd-resolver-0.5.0.jar
    ├── jettison-1.5.4.jar
    ├── jjwt-api-0.10.5.jar
    ├── jjwt-jackson-0.10.5.jar
    ├── jna-5.5.0.jar
    ├── joda-time-2.3.jar
    ├── jraft-core-1.3.13.jar
    ├── jsr305-3.0.2.jar
    ├── jsr311-api-1.1.1.jar
    ├── kafka-clients-3.1.2.jar
    ├── kryo-5.4.0.jar
    ├── kryo-serializers-0.45.jar
    ├── listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar
    ├── logback-classic-1.2.12.jar
    ├── logback-kafka-appender-0.2.0-RC2.jar
    ├── logstash-logback-encoder-6.5.jar
    ├── lz4-java-1.7.1.jar
    ├── metrics-core-4.2.21.jar
    ├── minlog-1.3.1.jar
    ├── mxparser-1.2.2.jar
    ├── nacos-api-1.4.6.jar
    ├── nacos-common-1.4.6.jar
    ├── netflix-eventbus-0.3.0.jar
    ├── netflix-infix-0.3.0.jar
    ├── netty-buffer-4.1.100.Final.jar
    ├── netty-codec-4.1.100.Final.jar
    ├── netty-codec-dns-4.1.100.Final.jar
    ├── netty-codec-haproxy-4.1.100.Final.jar
    ├── netty-codec-http2-4.1.100.Final.jar
    ├── netty-codec-memcache-4.1.100.Final.jar
    ├── netty-codec-redis-4.1.100.Final.jar
    ├── netty-codec-stomp-4.1.100.Final.jar
    ├── netty-codec-xml-4.1.100.Final.jar
    ├── netty-common-4.1.100.Final.jar
    ├── netty-handler-ssl-ocsp-4.1.100.Final.jar
    ├── netty-resolver-4.1.100.Final.jar
    ├── netty-resolver-dns-classes-macos-4.1.100.Final.jar
    ├── netty-resolver-dns-native-macos-4.1.100.Final-osx-aarch_64.jar
    ├── netty-resolver-dns-native-macos-4.1.100.Final-osx-x86_64.jar
    ├── netty-transport-4.1.100.Final.jar
    ├── netty-transport-classes-epoll-4.1.100.Final.jar
    ├── netty-transport-classes-kqueue-4.1.100.Final.jar
    ├── netty-transport-native-epoll-4.1.100.Final-linux-aarch_64.jar
    ├── netty-transport-native-epoll-4.1.100.Final-linux-x86_64.jar
    ├── netty-transport-native-epoll-4.1.100.Final.jar
    ├── netty-transport-native-kqueue-4.1.100.Final-osx-aarch_64.jar
    ├── netty-transport-native-kqueue-4.1.100.Final-osx-x86_64.jar
    ├── netty-transport-rxtx-4.1.100.Final.jar
    ├── netty-transport-sctp-4.1.100.Final.jar
    ├── netty-transport-udt-4.1.100.Final.jar
    ├── perfmark-api-0.19.0.jar
    ├── postgresql-42.3.8.jar
    ├── proto-google-common-protos-1.17.0.jar
    ├── reflectasm-1.11.9.jar
    ├── registry-client-all-6.3.0.jar
    ├── seata-common-2.1.0-SNAPSHOT.jar
    ├── seata-compressor-all-2.1.0-SNAPSHOT.jar
    ├── seata-compressor-bzip2-2.1.0-SNAPSHOT.jar
    ├── seata-compressor-deflater-2.1.0-SNAPSHOT.jar
    ├── seata-compressor-gzip-2.1.0-SNAPSHOT.jar
    ├── seata-compressor-lz4-2.1.0-SNAPSHOT.jar
    ├── seata-compressor-zip-2.1.0-SNAPSHOT.jar
    ├── seata-config-all-2.1.0-SNAPSHOT.jar
    ├── seata-config-apollo-2.1.0-SNAPSHOT.jar
    ├── seata-config-consul-2.1.0-SNAPSHOT.jar
    ├── seata-config-core-2.1.0-SNAPSHOT.jar
    ├── seata-config-etcd3-2.1.0-SNAPSHOT.jar
    ├── seata-config-nacos-2.1.0-SNAPSHOT.jar
    ├── seata-config-spring-cloud-2.1.0-SNAPSHOT.jar
    ├── seata-config-zk-2.1.0-SNAPSHOT.jar
    ├── seata-core-2.1.0-SNAPSHOT.jar
    ├── seata-discovery-consul-2.1.0-SNAPSHOT.jar
    ├── seata-discovery-custom-2.1.0-SNAPSHOT.jar
    ├── seata-discovery-etcd3-2.1.0-SNAPSHOT.jar
    ├── seata-discovery-nacos-2.1.0-SNAPSHOT.jar
    ├── seata-discovery-redis-2.1.0-SNAPSHOT.jar
    ├── seata-discovery-sofa-2.1.0-SNAPSHOT.jar
    ├── seata-metrics-api-2.1.0-SNAPSHOT.jar
    ├── seata-metrics-core-2.1.0-SNAPSHOT.jar
    ├── seata-metrics-registry-compact-2.1.0-SNAPSHOT.jar
    ├── seata-serializer-all-2.1.0-SNAPSHOT.jar
    ├── seata-serializer-hessian-2.1.0-SNAPSHOT.jar
    ├── seata-serializer-kryo-2.1.0-SNAPSHOT.jar
    ├── seata-spring-autoconfigure-server-2.1.0-SNAPSHOT.jar
    ├── servo-core-0.12.21.jar
    ├── simpleclient-0.15.0.jar
    ├── simpleclient_common-0.15.0.jar
    ├── simpleclient_tracer_common-0.15.0.jar
    ├── simpleclient_tracer_otel-0.15.0.jar
    ├── simpleclient_tracer_otel_agent-0.15.0.jar
    ├── snakeyaml-2.0.jar
    ├── snappy-java-1.1.8.4.jar
    ├── sofa-common-tools-1.0.12.jar
    ├── spring-aop-5.3.31.jar
    ├── spring-beans-5.3.31.jar
    ├── spring-boot-autoconfigure-2.7.18.jar
    ├── spring-boot-starter-logging-2.7.18.jar
    ├── spring-boot-starter-security-2.7.18.jar
    ├── spring-boot-starter-tomcat-2.7.18.jar
    ├── spring-boot-starter-web-2.7.18.jar
    ├── spring-context-5.3.31.jar
    ├── spring-core-5.3.31.jar
    ├── spring-expression-5.3.31.jar
    ├── spring-jcl-5.3.31.jar
    ├── spring-security-config-5.7.11.jar
    ├── spring-security-web-5.7.11.jar
    ├── stringtemplate-3.2.1.jar
    ├── tomcat-embed-core-9.0.82.jar
    ├── tomcat-embed-el-9.0.82.jar
    ├── tomcat-embed-websocket-9.0.82.jar
    ├── zkclient-0.11.jar
    ├── zookeeper-3.5.9.jar
    ├── zookeeper-jute-3.5.9.jar
    ├── zstd-jni-1.5.0-4.jar
    ├── ant-1.10.12.jar
    ├── apollo-core-2.0.1.jar
    ├── archaius-core-0.7.6.jar
    ├── asm-6.0.jar
    ├── audience-annotations-0.5.0.jar
    ├── commons-compiler-3.1.10.jar
    ├── commons-dbcp2-2.9.0.jar
    ├── commons-pool-1.6.jar
    ├── commons-pool2-2.11.1.jar
    ├── compactmap-2.0.jar
    ├── disruptor-3.3.7.jar
    ├── druid-1.2.7.jar
    ├── error_prone_annotations-2.21.1.jar
    ├── failsafe-2.3.3.jar
    ├── grpc-api-1.27.1.jar
    ├── grpc-netty-1.27.1.jar
    ├── grpc-stub-1.27.1.jar
    ├── hessian-4.0.3.jar
    ├── httpcore-4.4.16.jar
    ├── j2objc-annotations-2.8.jar
    ├── jackson-core-2.13.5.jar
    ├── jackson-datatype-jsr310-2.13.5.jar
    ├── jakarta.annotation-api-1.3.5.jar
    ├── janino-3.1.10.jar
    ├── javax.servlet-api-4.0.1.jar
    ├── jetcd-core-0.5.0.jar
    ├── jjwt-impl-0.10.5.jar
    ├── jul-to-slf4j-1.7.36.jar
    ├── logback-core-1.2.12.jar
    ├── nacos-client-1.4.6.jar
    ├── netty-all-4.1.100.Final.jar
    ├── netty-codec-http-4.1.100.Final.jar
    ├── netty-codec-mqtt-4.1.100.Final.jar
    ├── netty-codec-smtp-4.1.100.Final.jar
    ├── netty-codec-socks-4.1.100.Final.jar
    ├── netty-handler-4.1.100.Final.jar
    ├── netty-handler-proxy-4.1.100.Final.jar
    ├── netty-resolver-dns-4.1.100.Final.jar
    ├── netty-transport-native-unix-common-4.1.100.Final.jar
    ├── objenesis-3.2.jar
    ├── protobuf-java-3.16.3.jar
    ├── protobuf-java-util-3.11.0.jar
    ├── rocksdbjni-7.7.3.jar
    ├── seata-compressor-zstd-2.1.0-SNAPSHOT.jar
    ├── seata-console-2.1.0-SNAPSHOT.jar
    ├── seata-discovery-all-2.1.0-SNAPSHOT.jar
    ├── seata-discovery-core-2.1.0-SNAPSHOT.jar
    ├── seata-discovery-eureka-2.1.0-SNAPSHOT.jar
    ├── seata-discovery-zk-2.1.0-SNAPSHOT.jar
    ├── seata-metrics-all-2.1.0-SNAPSHOT.jar
    ├── seata-metrics-exporter-prometheus-2.1.0-SNAPSHOT.jar
    ├── seata-serializer-protobuf-2.1.0-SNAPSHOT.jar
    ├── seata-serializer-seata-2.1.0-SNAPSHOT.jar
    ├── seata-spring-autoconfigure-core-2.1.0-SNAPSHOT.jar
    ├── simpleclient_httpserver-0.15.0.jar
    ├── slf4j-api-1.7.36.jar
    ├── spring-boot-2.7.18.jar
    ├── spring-boot-starter-2.7.18.jar
    ├── spring-boot-starter-json-2.7.18.jar
    ├── spring-security-core-5.7.11.jar
    ├── spring-security-crypto-5.7.11.jar
    ├── spring-web-5.3.31.jar
    ├── spring-webmvc-5.3.31.jar
    ├── xstream-1.4.20.jar
    └── jdbc
        ├── mysql-connector-java-8.0.28.jar
        └── NOTICE.md

```