所有提交到 develop 分支的 PR 请在此处登记。

<!-- 请根据PR的类型添加 `变更记录` 到以下对应位置(feature/bugfix/optimize/test) 下 -->

### feature:
- [[#xxx](https://github.com/seata/seata/pull/xxx)] 支持 xxx

### bugfix:
- [[#xxx](https://github.com/seata/seata/pull/xxx)] 修复 xxx

### optimize：
- [[#4681](https://github.com/seata/seata/pull/4681)] 优化竞争锁过程
- [[#4774](https://github.com/seata/seata/pull/4774)] 优化 seataio/seata-server 镜像中的 mysql8 依赖
- [[#4750](https://github.com/seata/seata/pull/4750)] 优化AT分支释放全局锁不使用xid
- [[#4790](https://github.com/seata/seata/pull/4790)] 添加一个 github action，用于自动发布Seata到OSSRH
- [[#4765](https://github.com/seata/seata/pull/4765)] mysql8.0.29版本及以上XA模式不持connection至二阶段
- [[#4797](https://github.com/seata/seata/pull/4797)] 优化所有github actions脚本
- [[#4800](https://github.com/seata/seata/pull/4800)] 按照 Apache 协议规范，添加 NOTICE 文件
- [[#4761](https://github.com/seata/seata/pull/4761)] 使用 hget 代替 RedisLocker 中的 hmget, 因为只有一个 field
- [[#4414](https://github.com/seata/seata/pull/4414)] 移除log4j依赖
- [[#4836](https://github.com/seata/seata/pull/4836)] 优化 BaseTransactionalExecutor#buildLockKey(TableRecords rowsIncludingPK) 方法可读性
- [[#4865](https://github.com/seata/seata/pull/4865)] 修复 Saga 可视化设计器 GGEditor 安全漏洞
- [[#4590](https://github.com/seata/seata/pull/4590)] 自动降级支持开关支持动态配置
- [[#4490](https://github.com/seata/seata/pull/4490)] tccfence 记录表优化成按索引删除
- [[#4911](https://github.com/seata/seata/pull/4911)] 添加 header 和license 检测
- [[#4917](https://github.com/seata/seata/pull/4917)] 升级 package-lock.json 修复漏洞
- [[#4924](https://github.com/seata/seata/pull/4924)] 优化 pom 依赖
- [[#4932](https://github.com/seata/seata/pull/4932)] 抽取部分配置的默认值
- [[#4925](https://github.com/seata/seata/pull/4925)] 优化 javadoc 注释
- [[#4921](https://github.com/seata/seata/pull/4921)] 修复控制台模块安全漏洞和升级 skywalking-eyes 版本
- [[#4936](https://github.com/seata/seata/pull/4936)] 优化存储配置的读取
- [[#4946](https://github.com/seata/seata/pull/4946)] 将获取锁时遇到的sql异常传递给客户端
- [[#4962](https://github.com/seata/seata/pull/4962)] 优化构建配置，并修正docker镜像的基础镜像
- [[#4974](https://github.com/seata/seata/pull/4974)] 取消redis模式下,查询globalStatus条数的限制
- [[#4981](https://github.com/seata/seata/pull/4981)] 优化当tcc栅栏记录查不到时的错误提示
- [[#4995](https://github.com/seata/seata/pull/4995)] 修复mysql InsertOnDuplicateUpdate后置镜像查询SQL中重复的主键查询条件
- [[#5047](https://github.com/seata/seata/pull/5047)] 移除无用代码
- [[#5051](https://github.com/seata/seata/pull/5051)] 回滚时undolog产生脏写需要抛出不再重试(BranchRollbackFailed_Unretriable)的异常
- [[#5075](https://github.com/seata/seata/pull/5075)] 拦截没有主键及唯一索引值的insert on duplicate update语句
- [[#5104](https://github.com/seata/seata/pull/5104)] ConnectionProxy脱离对druid的依赖
- [[#5120](https://github.com/seata/seata/pull/5120)] 统一yml文件中的配置项格式
- [[#5124](https://github.com/seata/seata/pull/5124)] 支持oracle删除tccfence记录表
- [[#5115](https://github.com/seata/seata/pull/5115)] 兼容 `spring-boot:3.x`

### test：
- [[#4411](https://github.com/seata/seata/pull/4411)] 测试Oracle数据库AT模式下类型支持
- [[#4794](https://github.com/seata/seata/pull/4794)] 重构代码，尝试修复单元测试 `DataSourceProxyTest.getResourceIdTest()`
- [[#5101](https://github.com/seata/seata/pull/5101)] 修复zk注册和配置中心报ClassNotFoundException的问题 `DataSourceProxyTest.getResourceIdTest()`

### test:
- [[#xxx](https://github.com/seata/seata/pull/xxx)] 增加 xxx 测试

非常感谢以下 contributors 的代码贡献。若有无意遗漏，请报告。

<!-- 请确保您的 GitHub ID 在以下列表中 -->
- [slievrly](https://github.com/slievrly)
- [wangliang181230](https://github.com/wangliang181230)

同时，我们收到了社区反馈的很多有价值的issue和建议，非常感谢大家。
