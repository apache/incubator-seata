所有提交到 develop 分支的 PR 请在此处登记。

<!-- 请根据PR的类型添加 `变更记录` 到以下对应位置(feature/bugfix/optimize/test) 下 -->

### feature：
  - [[#4661](https://github.com/seata/seata/pull/4713)] 支持根据xid负载均衡算法
  - [[#4676](https://github.com/seata/seata/pull/4676)] 支持Nacos作为注册中心时，server通过挂载SLB暴露服务


### bugfix：
  - [[#4515](https://github.com/seata/seata/pull/4515)] 修复develop分支SeataTCCFenceAutoConfiguration在客户端未使用DB时，启动抛出ClassNotFoundException的问题。
  - [[#4661](https://github.com/seata/seata/pull/4661)] 修复控制台中使用PostgreSQL出现的SQL异常
  - [[#4667](https://github.com/seata/seata/pull/4682)] 修复develop分支RedisTransactionStoreManager迭代时更新map的异常
  - [[#4678](https://github.com/seata/seata/pull/4678)] 修复属性transport.enableRmClientBatchSendRequest没有配置的情况下缓存穿透的问题
  - [[#4701](https://github.com/seata/seata/pull/4701)] 修复命令行参数丢失问题
  - [[#4607](https://github.com/seata/seata/pull/4607)] 修复跳过全局锁校验的缺陷
  - [[#4696](https://github.com/seata/seata/pull/4696)] 修复 oracle 存储模式时的插入问题
  - [[#4726](https://github.com/seata/seata/pull/4726)] 修复批量发送消息时可能的NPE问题
  - [[#4729](https://github.com/seata/seata/pull/4729)] 修复AspectTransactional.rollbackForClassName设置错误

### optimize：
  - [[#4650](https://github.com/seata/seata/pull/4650)] 修复安全漏洞
  - [[#4670](https://github.com/seata/seata/pull/4670)] 优化branchResultMessageExecutor线程池的线程数
  - [[#4662](https://github.com/seata/seata/pull/4662)] 优化回滚事务监控指标
  - [[#4693](https://github.com/seata/seata/pull/4693)] 优化控制台导航栏
  - [[#4700](https://github.com/seata/seata/pull/4700)] 修复 maven-compiler-plugin 和 maven-resources-plugin 执行失败
  - [[#4711](https://github.com/seata/seata/pull/4711)] 分离部署时 lib 依赖
  - [[#4720](https://github.com/seata/seata/pull/4720)] 优化pom描述
  - [[#4728](https://github.com/seata/seata/pull/4728)] 将logback版本依赖升级至1.2.9

### test：
  - [[#4544](https://github.com/seata/seata/pull/4544)] 优化TransactionContextFilterTest中jackson包依赖问题
  - [[#4731](https://github.com/seata/seata/pull/4731)] 修复 AsyncWorkerTest 和 LockManagerTest 的单测问题。


非常感谢以下 contributors 的代码贡献。若有无意遗漏，请报告。

<!-- 请确保您的 GitHub ID 在以下列表中 -->
- [slievrly](https://github.com/slievrly)
- [pengten](https://github.com/pengten)
- [YSF-A](https://github.com/YSF-A)
- [tuwenlin](https://github.com/tuwenlin)
- [2129zxl](https://github.com/2129zxl)
- [Ifdevil](https://github.com/Ifdevil)
- [wingchi-leung](https://github.com/wingchi-leung)
- [liurong](https://github.com/robynron)
- [opelok-z](https://github.com/opelok-z)
- [a364176773](https://github.com/a364176773)
- [Smery-lxm](https://github.com/Smery-lxm)

同时，我们收到了社区反馈的很多有价值的issue和建议，非常感谢大家。