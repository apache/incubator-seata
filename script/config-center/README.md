# Script usage demo
![Since 1.1.0](https://img.shields.io/badge/Since%20-1.1.0-orange.svg?style=flat-square)

## Apollo
```bash
sh ${SEATAPATH}/script/config-center/apollo/apollo-config.sh -h localhost -p 8070 -e ENV -a seata-server -c default -n application -d apollo -r apollo -t 3aa026fc8435d0fc4505b345b8fa4578fb646a2c
```

## Consul
```bash
sh ${SEATAPATH}/script/config-center/consul/consul-config.sh -h localhost -p 8500
```

## Etcd3
```bash
sh ${SEATAPATH}/script/config-center/etcd3/etcd3-config.sh -h localhost -p 2379
```

## Nacos
shell:
```bash
sh ${SEATAPATH}/script/config-center/nacos/nacos-config.sh -h localhost -p 8848
```
python:
```bash
python ${SEATAPATH}/script/config-center/nacos/nacos-config.py localhost:8848
```

## ZK
```bash
sh ${SEATAPATH}/script/config-center/zk/zk-config.sh -h localhost -p 2181 -z "/Users/zhangchenghui/zookeeper-3.4.14"
```

