# Script usage demo
![Since 1.2.0](https://img.shields.io/badge/Since%20-1.2.0-orange.svg?style=flat-square)

## important attributes 

you only need to follow the instructions below and keep the corresponding configuration in 'config.txt' to run. For more configuration information, please visit [seata.io](https://seata.io/)

| server                   | client                                                       |
| ------------------------ | ------------------------------------------------------------ |
| store.mode: file,db      | config.type: file、nacos 、apollo、zk、consul、etcd3、custom |
| #only db:                | #only file:                                                  |
| store.db.driverClassName | service.default.grouplist                                    |
| store.db.url             | #All:                                                        |
| store.db.user            | service.vgroupMapping.my_test_tx_group                       |
| store.db.password        | service.disableGlobalTransaction                             |

## Nacos
shell:
```bash
sh ${SEATAPATH}/script/config-center/nacos/nacos-config.sh -h localhost -p 8848 -g SEATA_GROUP -t 5a3c7d6c-f497-4d68-a71a-2e5e3340b3ca -u username -w password
```

Parameter Description:

-h: host, the default value is localhost.

-p: port, the default value is 8848.

-g: Configure grouping, the default value is 'SEATA_GROUP'.

-t: Tenant information, corresponding to the namespace ID field of Nacos, the default value is ''.

-u: username, nacos 1.2.0+ on permission control, the default value is ''.

-w: password, nacos 1.2.0+ on permission control, the default value is ''.

python:
```bash
python ${SEATAPATH}/script/config-center/nacos/nacos-config.py localhost:8848
```

## Apollo
```bash
sh ${SEATAPATH}/script/config-center/apollo/apollo-config.sh -h localhost -p 8070 -e DEV -a seata-server -c default -n application -d apollo -r apollo -t 3aa026fc8435d0fc4505b345b8fa4578fb646a2c
```
Parameter Description:

-h: host, the default value is localhost.

-p: port, the default value is 8070.

-e: Managed configuration environment, the default value is DEV.

-a: AppId to which the namespace belongs, the default value is seata-server.

-c: Managed configuration cluster name, Generally, you can pass in default. If it is a special cluster, just pass in the name of the corresponding cluster，the default value is default.

-n: Name of the managed namespace, If the format is not properties, you need to add a suffix name, such as sample.yml, the default value is application.

-d: The creator of the item, in the format of a domain account, which is the User ID of the sso system.

-r: Publisher, domain account, note: if namespace.lock.switch in ApolloConfigDB.ServerConfig is set to true (default is false), Then the environment does not allow the publisher and editor to be the same person. So if the editor is zhangsan, the publisher can no longer be zhangsan.

-t: Apollo admin creates third-party applications in http://{portal_address}/open/manage.html, It is best to check whether this AppId has been created before creation. After successful creation, a token will be generated.

For details of the above parameter descriptions, please see:

https://github.com/ctripcorp/apollo/wiki/Apollo%E5%BC%80%E6%94%BE%E5%B9%B3%E5%8F%B0

## Consul
```bash
sh ${SEATAPATH}/script/config-center/consul/consul-config.sh -h localhost -p 8500
```
Parameter Description:

-h: host, the default value is localhost.

-p: port, the default value is 8500.

## Etcd3
```bash
sh ${SEATAPATH}/script/config-center/etcd3/etcd3-config.sh -h localhost -p 2379
```

Parameter Description:

-h: host, the default value is localhost.

-p: port, the default value is 2379.

## ZK
```bash
sh ${SEATAPATH}/script/config-center/zk/zk-config.sh -h localhost -p 2181 -z "/Users/zhangchenghui/zookeeper-3.4.14"
```
Parameter Description:

-h: host, the default value is localhost.

-p: port, the default value is 2181.

-z: zk path.

