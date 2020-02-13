# vergilyn-seata-examples

以 [seata.io][seata.io zh-cn docs] 官方文档中提到的示例作为参考测试代码（不完全一样）。  
通过测试代码 debug 调试和阅读 seata源码的实现原理。

## 1. dependencies

|                      |                             version                              |
|:---------------------|:----------------------------------------------------------------:|
| nacos                |       [1.0.0](https://github.com/alibaba/nacos/releases/)        |
| mysql                |                       mysql-5.7.25-winx64                        |
| spring-boot          |                          2.1.1.RELEASE                           |
| spring-cloud         |                        Greenwich.RELEASE                         |
| spring-cloud-alibaba | [2.1.1.RELEASE](https://github.com/alibaba/spring-cloud-alibaba) |
| seata                |        [1.0.0](https://github.com/seata/seata/tree/1.0.0)        |

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


## protobuf 安装
github: [https://github.com/protocolbuffers/protobuf](https://github.com/protocolbuffers/protobuf)  
release: [https://github.com/protocolbuffers/protobuf/releases](https://github.com/protocolbuffers/protobuf/releases)

1. protobuf
<font color="red">特别：通过maven-plugin来编译proto文件，**可能**不需要这么安装protobuf。(ps. 搞懵逼了，i'm five~~)</font>

备注：注意windows下载的是`protoc-3.11.3-win64.zip`，而不是`protobuf-java-3.11.3.zip`（这个需要自己编译）。

下载并解压后，将`bin`目录添加到`环境变量 - 系统变量 - path`。通过cmd验证是否安装成功：
```
PS C:\Users\Administrator> protoc --version
libprotoc 3.11.3
```

2. protobuf-maven-plugin
a) idea安装插件`Protobuf Support`（proto语法高亮，mvn编译命令）
b) maven-plugin 配置，例如seata源码中的相应 [pom.xml](../pom.xml)
    ```XML
    <plugin>
        <groupId>org.xolstice.maven.plugins</groupId>
        <artifactId>protobuf-maven-plugin</artifactId>
        <version>${protobuf-maven-plugin.version}</version>
        <configuration>
            <protoSourceRoot>${project.basedir}/src/main/resources/protobuf/io/seata/protocol/transcation/</protoSourceRoot>
            <protocArtifact>
                com.google.protobuf:protoc:3.3.0:exe:${os.detected.classifier}
            </protocArtifact>
        </configuration>
        <executions>
            <execution>
                <goals>
                    <goal>compile</goal>
                </goals>
            </execution>
        </executions>
    </plugin>
    ```
c) 编译，`Maven - {project} - plugins - protobuf - protobuf:compile-javanano`。

参考： [idea使用Protobuf插件](https://www.cnblogs.com/TechSnail/p/7793813.html)

## SEATA的启动流程

### 1. seata是如何加载conf文件的？
2020-02-13  
![register_conf_load](docs/plant-uml/register_conf_load.png)

### 2. seata是如何使用conf信息的？
通过`1.`已经知道了对象的配置文件`register.conf`和`file.conf`被加载到了内存，那么其中的配置何时使用，怎么使用？e.g.  
```
# register.conf
registry {
  # file 、nacos 、eureka、redis、zk、consul、etcd3、sofa
  type = "nacos"

  nacos {
    serverAddr = "127.0.0.1:8848"
    namespace = "19f8f2a7-1969-40df-b6b3-b17e48269520"
    cluster = "default"
  }

}

config {
  # file、nacos 、apollo、zk、consul、etcd3
  type = "nacos"

  nacos {
    serverAddr = "127.0.0.1:8848"
    namespace = "19f8f2a7-1969-40df-b6b3-b17e48269520"
  }
}

```

备注：
1. seata v1.0.0中，通过nacos获取conf并不支持指定GROUP，默认从`SEATA_GROUP`获取（在下一个版本开始支持配置GROUP）。  
e.g.  
```JAVA
package io.seata.config.nacos;

public class NacosConfiguration extends AbstractConfiguration {

    private static final String SEATA_GROUP = "SEATA_GROUP";

    @Override
    public String getConfig(String dataId, String defaultValue, long timeoutMills) {
        String value;
        if ((value = getConfigFromSysPro(dataId)) != null) {
            return value;
        }
        try {
            value = configService.getConfig(dataId, SEATA_GROUP, timeoutMills);
        } catch (NacosException exx) {
            LOGGER.error(exx.getErrMsg());
        }
        return value == null ? defaultValue : value;
    }
}
```

2. question: 现在seata支持的nacos的配置是一项一项的（nacos的dataId过多）  
- [SEATA issues#2011](https://github.com/seata/seata/issues/2011) 
  
e.g.  
```
store {
  ## store mode: file、db
  mode = "db"

  ## database store property
  db {
    datasource = "druid"
    db-type = "mysql"
    driver-class-name = "com.mysql.jdbc.Driver"
    url = "jdbc:mysql://127.0.0.1:3306/test_microservice"
    user = "root"
    password = "123456"
  }
}

对应的是7个data-id，而不是一个data-id中的key-value：
1. store.mode
2. store.db.datasource
3. store.db.db-type
4. ...
```



[seata.io zh-cn docs]: https://seata.io/zh-cn/docs/overview/what-is-seata.html
[protobuf]: https://github.com/protocolbuffers/protobuf

### 3. seata如何注册到nacos？
1. seata注册到nacos的服务名默认叫“serverAddr”
- [SEATA issues#1277](https://github.com/seata/seata/issues/1277)  

相关代码参考：[io.seata.discovery.registry.nacos.NacosRegistryServiceImpl#register(...)](../discovery/seata-discovery-nacos/src/main/java/io/seata/discovery/registry/nacos/NacosRegistryServiceImpl.java)

