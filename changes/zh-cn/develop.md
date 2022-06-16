所有提交到 develop 分支的 PR 请在此处登记。

<!-- 请根据PR的类型添加 `变更记录` 到以下对应位置(feature/bugfix/optimize/test) 下 -->

### feature：

### bugfix：
  - [[#4515](https://github.com/seata/seata/pull/4515)] 修复develop分支SeataTCCFenceAutoConfiguration在客户端未使用DB时，启动抛出ClassNotFoundException的问题。
  - [[#4661](https://github.com/seata/seata/pull/4661)] 修复控制台中使用PostgreSQL出现的SQL异常
  - [[#4667](https://github.com/seata/seata/pull/4682)] 修复develop分支RedisTransactionStoreManager迭代时更新map的异常
  - [[#4678](https://github.com/seata/seata/pull/4678)] 修复属性transport.enableRmClientBatchSendRequest没有配置的情况下缓存穿透的问题
  - [[#4607](https://github.com/seata/seata/pull/4607)] 修复跳过全局锁校验的缺陷


### optimize：
  - [[#4650](https://github.com/seata/seata/pull/4650)] 修复安全漏洞
  - [[#4670](https://github.com/seata/seata/pull/4670)] 优化branchResultMessageExecutor线程池的线程数
  - [[#4662](https://github.com/seata/seata/pull/4662)] 优化回滚事务监控指标
  - [[#4693](https://github.com/seata/seata/pull/4693)] 优化控制台导航栏
  - [[#4544](https://github.com/seata/seata/pull/4544)] 优化测试用例TransactionContextFilterTest中jackson包依赖问题
  - [[#4708](https://github.com/seata/seata/pull/4708)] 优化@GlobalTransactional嵌套时TransactionHook的执行时机

### test：


非常感谢以下 contributors 的代码贡献。若有无意遗漏，请报告。

<!-- 请确保您的 GitHub ID 在以下列表中 -->
- [slievrly](https://github.com/slievrly)
- [pengten](https://github.com/pengten)
- [YSF-A](https://github.com/YSF-A)
- [tuwenlin](https://github.com/tuwenlin)
- [Ifdevil](https://github.com/Ifdevil)
- [wingchi-leung](https://github.com/wingchi-leung)
- [liurong](https://github.com/robynron)

同时，我们收到了社区反馈的很多有价值的issue和建议，非常感谢大家。