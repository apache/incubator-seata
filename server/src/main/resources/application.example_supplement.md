#  the default port number of the seata server, default 7091, can be modified
server:
  port: 7091

#  the instance name of seata after it is registered with the registry cannot be duplicated
spring:
  application:
    name: seata-server

#  log configuration
logging:
  #  a logback configuration file that defines which components are used to collect logs
  config: classpath:logback-spring.xml
  #  output the log to the path specified by file, where ${user.home}
  #  represents a path that has been defined in the system, for example,
  #  the user logged in to the Linux system is named seata_user,
  #  then the path represented by user.home is /home/seata_user,
  #  so this configuration means to output logs to /home/seata_user/logs/seata,
  #  See logback-spring.xml for details
  file:
    path: ${log.home:${user.home}/logs/seata}
  #  The components that process logs, these two are turned off by default,
  #  file-appender and console-appender are enabled by default,
  #  and the logback-spring .xml can be seen for details
  extend:
    #  turn on logstash, the configuration file is under logback in the current directory,
    #  the following is its working process
    #  App(Logback LogstashTcpSocketAppender) -> Logstash -> Elasticsearch pipeline
    logstash-appender:
      destination: 127.0.0.1:4560
    #  turn on logstash, the configuration file is under logback in the current directory,
    #  the following is its working process
    #  App(Logback KafkaAppender) -> Kafka -> Logstash -> Elasticsearch pipeline
    kafka-appender:
      bootstrap-servers: 127.0.0.1:9092
      topic: logback_to_logstash

#  the account and password of the seata console
console:
  user:
    username: seata
    password: seata

#  basic configuration information for seata
seata:
  #  seata's configuration center
  config:
    # the type of configuration center,support: file 、 nacos 、 consul 、 apollo 、 zk  、 etcd3
    type: file
    # type is the configuration when the file is made
    file:
      # write configuration information to the file.conf file
      name: file.conf
    # type is the configuration when the nacos is made
    nacos:
      # nacos service address
      server-addr: 127.0.0.1:8848
      # The namespace is generally a development environment, test environment, and production environment,
      # and if it is not set, it is in the public space by default
      namespace:
      # in nacos, services of the same type are generally grouped, and the role here is to register seata into SEATA_GROUP group
      group: SEATA_GROUP
      # the account and password of the nacos console
      username: nacos
      password: nacos
      # for the path of nacos health check, you need to configure an open web endpoint
      context-path: /${server.servlet.context-path}/actuator
      ##if use MSE Nacos with auth, mutex with username/password attribute
      #access-key:
      #secret-key:
      # create a configuration file named seataServer.properties in nacos, which can be
      # copied from the config.txt of /seata/script/config-center and modified as needed
      data-id: seataServer.properties
  #  seata's registration center
  registry:
    # the type of registration center,support: file 、 nacos 、 eureka 、 redis 、 zk  、 consul 、 etcd3 、 sofa
    type: file
    # when a seata server is deployed on a machine with multiple network interfaces,
    # specify which network interface the Seata server is bound to
    preferred-networks: 30.240.*
    # type is the configuration when the nacos is made
    nacos:
      # The service name registered by seata to NACOS cannot be duplicated,
      # so the name of the registry center and the configuration center cannot be the same
      application: seata-server
      # nacos service address
      server-addr: 127.0.0.1:8848
      # in nacos, services of the same type are generally grouped, and the role here is to register seata into SEATA_GROUP group
      group: SEATA_GROUP
      # the namespace is generally a development environment, test environment, and production environment,
      # and if it is not set, it is in the public space by default
      namespace:
      # the cluster mode is divided into two types: default and raft.
      # using default, seata's TC will create a temporary node in Nacos to indicate its existence,
      # and when the TC instance is shut down, this node will be deleted, and other seata TC
      # instances can listen to these nodes to obtain cluster information and coordinate accordingly.
      # using raft, seata's TC will be deployed in nacos as a raft cluster to achieve distributed consistency
      # and high availability, and seata's TC instances will participate in the raft algorithm to jointly
      # decide a cluster leader, and the leader is responsible for coordinating transaction-related operations.
      # Use default to meet most needs
      cluster: default
      # the account and password of the nacos console
      username: nacos
      password: nacos
      # for the path of nacos health check, you need to configure an open web endpoint
      context-path:
      ##if use MSE Nacos with auth, mutex with username/password attribute
      #access-key:
      #secret-key:
  # security controls
  security:
    # a security key for seata's encryption and decryption in transit of transactions
    secretKey: SeataSecretKey0c382ef121d778043159209298fd40bf3850a017
    # set the validity period of the token with the global transaction ID generated
    # by the Seata transaction initiator, and the default unit is milliseconds
    tokenValidityInMilliseconds: 1800000
    # The following path access is not checked
    ignore:
      urls: /,/**/*.css,/**/*.js,/**/*.html,/**/*.map,/**/*.svg,/**/*.png,/**/*.jpeg,/**/*.ico,/api/v1/auth/login