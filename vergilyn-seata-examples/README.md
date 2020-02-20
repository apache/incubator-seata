# vergilyn-seata-examples

+ [github, seata]
+ [vergilyn seata-fork]
+ [seata.io zh-cn docs]

以 [seata.io][seata.io zh-cn docs] 官方文档中提到的示例作为参考测试代码（不完全一样）。  
通过测试代码 debug 调试和阅读 seata源码的实现原理。

## 1. dependencies

|                      |                             version                              |
|:---------------------|:----------------------------------------------------------------:|
| nacos                |       [1.1.4](https://github.com/alibaba/nacos/releases/)        |
| mysql                |                       mysql-5.7.25-winx64                        |
| spring-boot          |                          2.1.1.RELEASE                           |
| spring-cloud         |                        Greenwich.RELEASE                         |
| spring-cloud-alibaba | [2.1.1.RELEASE](https://github.com/alibaba/spring-cloud-alibaba) |
| seata                |        [1.0.0](https://github.com/seata/seata/tree/1.0.0)        |

备注：
1. 因为 seata-v1.0.0 中依赖的是 nacos-v1.1.4。所以统一用的nacos-v1.1.4。
如果版本不一致，会出现一些乱七八糟的问题。例如`NacosNamingService.getAllInstances(...)`无法获取到nacos-v1.0.0中的实例。
（用v1.1.4的API可以注册到nacos-v1.0.0.）

| project | server.port |
|:--------|:-----------:|
| NACOS   |    8848     |
| SEATA   |    8091     |
| ACCOUNT |    902X     |
| ORDER   |    903X     |
| STORAGE |    904X     |

特别：
1. 运行SEATA源码需要用到[ProtoBuf](https://github.com/protocolbuffers/protobuf/releases)([protoc-3.11.3-windows-x86_64.exe](https://repo1.maven.org/maven2/com/google/protobuf/protoc/3.11.3/))

## 2. 项目说明

1. nacos只用作服务注册发现中心，其配置中心禁用`spring.cloud.nacos.config.enabled=false`


### 2.1 [vergilyn-account-examples](vergilyn-account-examples), [vergilyn-order-examples](vergilyn-order-examples), [vergilyn-storage-examples](vergilyn-storage-examples)
注意修改其中的配置。
- `bootstrap.yaml` 中的`spring.cloud.nacos.discovery.namespace`
- `application.yaml` 中的 datasource连接配置

### 2.2 [vergilyn-common-dependencies](vergilyn-common-dependencies)
除开一些公共的依赖外，**最主要是放置测试代码`src/test/java`。**

### 2.3 [server(seata-server)](../server)
1. `registry.conf`  
a) registry本示例采用`nacos`，注意其namespace与examples-application保持一致；  
b) config采用`file`。

2. `file.conf`  
本示例的seata config采用的是`file`模式。
其中，`store.mode=db`连接mysql数据库。


[seata.io zh-cn docs]: https://seata.io/zh-cn/docs/overview/what-is-seata.html
[github, seata]: https://github.com/seata/seata
[vergilyn seata-fork]: https://github.com/vergilyn/seata-fork