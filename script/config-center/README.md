# Script usage demo
![Since 1.1.0](https://img.shields.io/badge/Since%20-1.1.0-orange.svg?style=flat-square)

## Apollo
```bash
sh ${SEATAPATH}/script/config-center/apollo/apollo-config.sh -h localhost -p 8070 -e DEV -a seata-server -c default -n application -d apollo -r apollo -t 3aa026fc8435d0fc4505b345b8fa4578fb646a2c
```
参数说明：

-h: host，默认值 localhost

-p: port，默认值 8070

-e: 所管理的配置环境，默认值 DEV

-a: Namespace 所属的 AppId，默认值 seata-server

-c: 所管理的配置集群名， 一般情况下传入 default 即可。如果是特殊集群，传入相应集群的名称即可，默认值 default

-n: 所管理的 Namespace 的名称，如果是非 properties 格式，需要加上后缀名，如 sample.yml，默认值 application

-d: item 的创建人，格式为域账号，也就是 sso 系统的 User ID

-r: 发布人，域账号，注意：如果 ApolloConfigDB.ServerConfig 中的 namespace.lock.switch 设置为 true 的话（默认是 false），那么该环境不允许发布人和编辑人为同一人。所以如果编辑人是 zhangsan，发布人就不能再是 zhangsan。

-t: Apollo 管理员在 http://{portal_address}/open/manage.html 创建第三方应用，创建之前最好先查询此AppId是否已经创建。创建成功之后会生成一个 token

以上参数说明详情请看：

https://github.com/ctripcorp/apollo/wiki/Apollo%E5%BC%80%E6%94%BE%E5%B9%B3%E5%8F%B0

## Consul
```bash
sh ${SEATAPATH}/script/config-center/consul/consul-config.sh -h localhost -p 8500
```
参数说明：

-h: host，默认值 localhost

-p: port，默认值 8500

## Etcd3
```bash
sh ${SEATAPATH}/script/config-center/etcd3/etcd3-config.sh -h localhost -p 2379
```

参数说明：

-h: host，默认值 localhost

-p: port，默认值 2379

## Nacos
shell:
```bash
sh ${SEATAPATH}/script/config-center/nacos/nacos-config.sh -h localhost -p 8848
```

参数说明：

-h: host，默认值 localhost

-p: port，默认值 8848

python:
```bash
python ${SEATAPATH}/script/config-center/nacos/nacos-config.py localhost:8848
```

## ZK
```bash
sh ${SEATAPATH}/script/config-center/zk/zk-config.sh -h localhost -p 2181 -z "/Users/zhangchenghui/zookeeper-3.4.14"
```
参数说明：

-h: host，默认值 localhost

-p: port，默认值 2181

-z: zk所属路径

