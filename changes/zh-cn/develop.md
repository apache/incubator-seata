所有提交到 develop 分支的 PR 请在此处登记。

<!-- 请根据PR的类型添加 `变更记录` 到以下对应位置(feature/bugfix/optimize/test) 下 -->

### feature：
- [[#4802](https://github.com/seata/seata/pull/4802)] dockerfile 支持 arm64
- [[#4863](https://github.com/seata/seata/pull/4863)] support oracle and postgresql multi primary key
- [[#4649](https://github.com/seata/seata/pull/4649)] seata-server支持多注册中心
- [[#4479](https://github.com/seata/seata/pull/4479)] TCC注解支持添加在实现类及其方法上也生效
- [[#4877](https://github.com/seata/seata/pull/4877)] seata client支持jdk17
- [[#4468](https://github.com/seata/seata/pull/4968)] 支持kryo 5.3.0
- [[#4914](https://github.com/seata/seata/pull/4914)] 支持mysql的update join联表更新语法



### bugfix：
- [[#4780](https://github.com/seata/seata/pull/4780)] 修复超时回滚成功后无法发送TimeoutRollbacked事件
- [[#4954](https://github.com/seata/seata/pull/4954)] 修复output表达式错误时，保存执行结果空指针异常
- [[#4817](https://github.com/seata/seata/pull/4817)] 修复高版本springboot配置不标准的问题
- [[#4838](https://github.com/seata/seata/pull/4838)] 修复使用 Statement.executeBatch() 时无法生成undo log 的问题
- [[#4533](https://github.com/seata/seata/pull/4533)] 修复handleRetryRollbacking的event重复导致的指标数据不准确
- [[#4779](https://github.com/seata/seata/pull/4779)] 修复支持 Apache Dubbo 3 版本
- [[#4912](https://github.com/seata/seata/pull/4912)] 修复mysql InsertOnDuplicateUpdate 列名大小写不一致无法正确匹配
- [[#4543](https://github.com/seata/seata/pull/4543)] 修复对 Oracle 数据类型nclob的支持
- [[#4915](https://github.com/seata/seata/pull/4915)] 修复获取不到ServerRecoveryProperties属性的问题
- [[#4919](https://github.com/seata/seata/pull/4919)] 修复XID的port和address出现null:0的情况
- [[#4928](https://github.com/seata/seata/pull/4928)] 修复 rpcContext.getClientRMHolderMap NPE 问题
- [[#4953](https://github.com/seata/seata/pull/4953)] 修复InsertOnDuplicateUpdate可绕过修改主键的问题
- [[#4978](https://github.com/seata/seata/pull/4978)] 修复 kryo 支持循环依赖
- [[#4985](https://github.com/seata/seata/pull/4985)] 修复 undo_log id重复的问题
- [[#5018](https://github.com/seata/seata/pull/5018)] 修复启动脚本中 loader path 使用相对路径导致 server 启动失败问题
- [[#5004](https://github.com/seata/seata/pull/5004)] 修复mysql update join行数据重复的问题
- [[#5033](https://github.com/seata/seata/pull/5033)] 修复InsertOnDuplicateUpdate的SQL语句中无插入列字段导致的空指针问题
- [[#5038](https://github.com/seata/seata/pull/5038)] 修复SagaAsyncThreadPoolProperties冲突问题


### optimize：
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

### test：
- [[#4794](https://github.com/seata/seata/pull/4794)] 重构代码，尝试修复单元测试 `DataSourceProxyTest.getResourceIdTest()`


非常感谢以下 contributors 的代码贡献。若有无意遗漏，请报告。

<!-- 请确保您的 GitHub ID 在以下列表中 -->
- [slievrly](https://github.com/slievrly)
- [tuwenlin](https://github.com/tuwenlin)
- [lcmvs](https://github.com/lcmvs)
- [wangliang181230](https://github.com/wangliang181230)
- [a364176773](https://github.com/a364176773)
- [AlexStocks](https://github.com/AlexStocks)
- [liujunlin5168](https://github.com/liujunlin5168)
- [pengten](https://github.com/pengten)
- [liuqiufeng](https://github.com/liuqiufeng)
- [yujianfei1986](https://github.com/yujianfei1986)
- [Bughue](https://github.com/Bughue)
- [AlbumenJ](https://github.com/AlbumenJ)
- [doubleDimple](https://github.com/doubleDimple)
- [jsbxyyx](https://github.com/jsbxyyx)
- [tuwenlin](https://github.com/tuwenlin)
- [CrazyLionLi](https://github.com/JavaLionLi)
- [whxxxxx](https://github.com/whxxxxx)
- [renliangyu857](https://github.com/renliangyu857)
- [neillee95](https://github.com/neillee95)
- [crazy-sheep](https://github.com/crazy-sheep)

同时，我们收到了社区反馈的很多有价值的issue和建议，非常感谢大家。
