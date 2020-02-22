# vgroup_mapping

- [事务分组专题](https://seata.io/zh-cn/docs/user/transaction-group.html)
- [seata参数配置 1.0.0版本](https://seata.io/zh-cn/docs/user/configurations.html)

TC - 事务协调者  
维护全局和分支事务的状态，驱动全局事务提交或回滚。

TM - 事务管理器  
定义全局事务的范围：开始全局事务、提交或回滚全局事务。

RM - 资源管理器  
管理分支事务处理的资源，与TC交谈以注册分支事务和报告分支事务的状态，并驱动分支事务提交或回滚。

TC服务：即seata-server

seata-server:
```HOCON
# seata-server
registry {
  # file 、nacos 、eureka、redis、zk、consul、etcd3、sofa
  type = "nacos"
  nacos {
    serverAddr = "localhost"
    namespace = ""
    cluster = "default"
  }
}
```

seata-client:
```HOCON
# register.conf
registry {
  # file 、nacos 、eureka、redis、zk、consul、etcd3、sofa
  type = "nacos"
  nacos {
    serverAddr = "localhost"
    namespace = ""
  }
}

# file.conf
service {
  # seata.tx-service-group = "my_test_tx_group"
  vgroup_mapping.my_test_tx_group = "default" # 需要保证seata-server在此TC集群下存在可用实例
  default.grouplist = "127.0.0.1:8091" # 仅注册中心是file时有用
}
```

## seata-client
- io.seata.discovery.registry.nacos.NacosRegistryServiceImpl#lookup(...)

1. 获取事务分组
springboot可配置在yml、properties中，服务启动时加载配置，对应的值"my_test_tx_group"即为一个事务分组名，
若不配置，默认为"{seata.application-id | spring.application.name}-seata-service-group"。

2. 查找TC集群名
拿到事务分组名"my_test_tx_group"拼接成"service.vgroup_mapping.my_test_tx_group"从配置中心查找到TC集群名clusterName为"default"

3. 查找TC服务
根据serverAddr和namespace以及clusterName在注册中心找到真实TC服务列表
(TC服务列表即 seata-server的 IP:PORT)

注：serverAddr和namespace与Server端一致，clusterName与Server端cluster一致


## FAQ

### Data truncation: Data too long for column 'transaction_service_group' at row 1
表`global_table`中，默认"`transaction_service_group` VARCHAR(32)"。  
而txServiceGroup如果采用 {spring.application.name}_{custom} 的形式，那么很容易超过设置。


global_table:
  add: client发起 begin-global-transaction时，server会去创建 DefaultCore#begin(...) -> GlobalSession#begin(...)
  update:
    1、async commit-global-transaction时
  remove: client发起 commit-global-transaction时，DefaultCore#commit(...) -> GlobalSession#closeAndClean(...) -> GlobalSession#clean(...)

branch_table:
  add/update: ？？？
  remove: client发起 commit-global-transaction时（branchTable != null），DefaultCore#commit(...) -> GlobalSession#closeAndClean(...) -> GlobalSession#clean(...)

lock_table:

undo_log:
  add: AbstractDataSourceProxy.class、DataSourceProxy.class
  `@Transaction` 提交事务时 `org.hibernate.resource.jdbc.internal.AbstractLogicalConnectionImplementor#commit(...)`  
  seata 通过代理 `io.seata.rm.datasource.ConnectionProxy`

