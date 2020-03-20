# Script Description

## [client](https://github.com/seata/seata/tree/develop/script/client) 

> Store configuration and SQL for client side

- at: Script of create table `undo_log` for AT mode.
- conf: Configuration which client need.
- saga: Script of create table in SAGA mode
- spring: Configuration for Spring Boot 

## [server](https://github.com/seata/seata/tree/develop/script/server)

> Store SQL and deploy script for server side

- db: Create table script for server when store mode is `db`
- docker-compose: Script for deploy server by docker-compose
- helm: Script for deploy server by Helm
- kubernetes: Script for deploy server by Kubernetes

## [config-center](https://github.com/seata/seata/tree/develop/script/config-center)

> Store initialize script for configuration center, will use `config.txt` as configuration when initial

- nacos: Initialize script for Nacos
- zk: Initialize script for ZooKeeper, the script need related script in Zookeeper, you need download yourself. You can modify `zk-params.txt` to change the ZooKeeper server configuration, or input when execute also
- apollo: Initialize script for Apollo. You can modify `apollo-params.txt` to change the Apollo server configuration, or input when execute also
- etcd3: Initialize script for Etcd3
- consul: Initialize script for consul

