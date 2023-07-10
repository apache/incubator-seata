所有提交到 develop 分支的 PR 请在此处登记。

<!-- 请根据PR的类型添加 `变更记录` 到以下对应位置(feature/bugfix/optimize/test) 下 -->

### feature:
- [[#5476](https://github.com/seata/seata/pull/5476)] seata客户端，首次支持 `native-image`
- [[#5495](https://github.com/seata/seata/pull/5495)] 控制台集成Saga状态机设计器
- [[#5668](https://github.com/seata/seata/pull/5668)] 兼容1.4.2及以下版本的file.conf/registry.conf配置

### bugfix:
- [[#5682](https://github.com/seata/seata/pull/5682)] 修复saga模式下replay context丢失startParams问题
- [[#5671](https://github.com/seata/seata/pull/5671)] 修复saga模式下serviceTask入参autoType转化失败问题
- [[#5194](https://github.com/seata/seata/pull/5194)] 修复使用Oracle作为服务端DB存储时的建表失败问题
- [[#5021](https://github.com/seata/seata/pull/5201)] 修复 JDK17 下获取 Spring 原始代理对象失败的问题
- [[#5023](https://github.com/seata/seata/pull/5203)] 修复 `seata-core` 模块传递依赖冲突
- [[#5224](https://github.com/seata/seata/pull/5224)] 修复 oracle初始化脚本索引名重复的问题
- [[#5233](https://github.com/seata/seata/pull/5233)] 修复LoadBalance相关配置不一致的问题
- [[#5266](https://github.com/seata/seata/pull/5265)] 修复控制台全局锁查询接口查到了已释放的锁
- [[#5245](https://github.com/seata/seata/pull/5245)] 修复不完整的distribution模块依赖
- [[#5239](https://github.com/seata/seata/pull/5239)] 修复当使用JDK代理时，`getConfig` 方法获取部分配置时抛出 `ClassCastException` 异常的问题
- [[#5281](https://github.com/seata/seata/pull/5281)] 修复并行rm请求处理时数组索引越界问题
- [[#5288](https://github.com/seata/seata/pull/5288)] 修复AT模式下oracle的主键列自增的问题
- [[#5287](https://github.com/seata/seata/pull/5287)] 修复AT模式下pgsql的主键列自增的问题
- [[#5299](https://github.com/seata/seata/pull/5299)] 修复TC端重试回滚或重试提交超时GlobalSession的删除问题
- [[#5307](https://github.com/seata/seata/pull/5307)] 修复生成update前后镜像sql不对关键字转义的bug
- [[#5311](https://github.com/seata/seata/pull/5311)] 移除基于文件存储恢复时的RollbackRetryTimeout事务
- [[#4734](https://github.com/seata/seata/pull/4734)] 修复AT模式下新增字段产生的字段找不到
- [[#5316](https://github.com/seata/seata/pull/5316)] 修复jdk8 中 G1 参数
- [[#5321](https://github.com/seata/seata/pull/5321)] 修复当TC端回滚返回RollbackFailed时，自定义FailureHandler的方法未执行
- [[#5332](https://github.com/seata/seata/pull/5332)] 修复单元测试中发现的bug
- [[#5145](https://github.com/seata/seata/pull/5145)] 修复saga模式全局事务状态始终为Begin的问题
- [[#5413](https://github.com/seata/seata/pull/5413)] 修复 arm64平台下的JDK和Spring兼容问题
- [[#5415](https://github.com/seata/seata/pull/5415)] 修复客户侧事务提交前超时未执行hook和failureHandler的问题
- [[#5447](https://github.com/seata/seata/pull/5447)] fix oracle xa mode cannnot be used By same database
- [[#5472](https://github.com/seata/seata/pull/5472)] 在RM中使用`@GlobalTransactional`时,如果RM执行失败会抛出`ShouldNeverHappenException`
- [[#5535](https://github.com/seata/seata/pull/5535)] 修复读取logback文件路径错误的问题
- [[#5538](https://github.com/seata/seata/pull/5538)] 修复提交事务时事务已完成不抛出异常问题
- [[#5539](https://github.com/seata/seata/pull/5539)] 修复Oracle 10g where条件包含setDate全表扫描问题
- [[#5540](https://github.com/seata/seata/pull/5540)] 修复 GlobalStatus=9 在DB存储模式无法清除的问题
- [[#5552](https://github.com/seata/seata/pull/5552)] 修复mariadb回滚失败的问题
- [[#5583](https://github.com/seata/seata/pull/5583)] 修复grpc xid 解绑问题
- [[#5602](https://github.com/seata/seata/pull/5602)] 修复participant情况下的重复日志
- [[#5645](https://github.com/seata/seata/pull/5645)] 修复 oracle 插入 undolog 失败问题
- [[#5659](https://github.com/seata/seata/pull/5659)] 修复后镜像查询时增加关键字转义符导致数据库强制开启大小写校验引起的sql异常
- [[#5663](https://github.com/seata/seata/pull/5663)] 修复connectionProxyXA连接复用时timeout为null
- [[#5675](https://github.com/seata/seata/pull/5675)] 修复 xxx.grouplist 和 grouplist.xxx 配置项兼容问题
- [[#5690](https://github.com/seata/seata/pull/5690)] 修复控制台打印 `unauthorized error` 问题

### optimize:
- [[#5208](https://github.com/seata/seata/pull/5208)] 优化多次重复获取Throwable#getCause问题
- [[#5212](https://github.com/seata/seata/pull/5212)] 优化不合理的日志信息级别
- [[#5237](https://github.com/seata/seata/pull/5237)] 优化异常日志打印(EnhancedServiceLoader.loadFile#cahtch)
- [[#5089](https://github.com/seata/seata/pull/5089)] 优化 TCC fence log 清理定时任务的 delay 参数值检查
- [[#5243](https://github.com/seata/seata/pull/5243)] 升级 kryo 5.4.0 优化对jdk17的兼容性
- [[#5153](https://github.com/seata/seata/pull/5153)] 只允许AT去尝试跨RM获取channel
- [[#5177](https://github.com/seata/seata/pull/5177)] 如果 `server.session.enable-branch-async-remove` 为真，异步删除分支，同步解锁。
- [[#5273](https://github.com/seata/seata/pull/5273)] 优化`protobuf-maven-plugin`插件的编译配置，解决高版本的命令行过长问题
- [[#5303](https://github.com/seata/seata/pull/5303)] 移除启动脚本的-Xmn参数
- [[#5325](https://github.com/seata/seata/pull/5325)] 添加配置中心、注册中心类型以及存储模式日志信息
- [[#5315](https://github.com/seata/seata/pull/5315)] 优化SPI加载日志
- [[#5323](https://github.com/seata/seata/pull/5323)] 为全局事务超时日志添加时间信息
- [[#5414](https://github.com/seata/seata/pull/5414)] 优化事务失败处理 handler
- [[#5537](https://github.com/seata/seata/pull/5537)] 优化客户侧事务日志
- [[#5541](https://github.com/seata/seata/pull/5541)] 优化Server日志输出
- [[#5548](https://github.com/seata/seata/pull/5548)] 优化 gpg key 和 发布流水线
- [[#5638](https://github.com/seata/seata/pull/5638)] 优化server端事务隔离级别为读已提交
- [[#5646](https://github.com/seata/seata/pull/5646)] 重构 ColumnUtils 和 EscapeHandler
- [[#5648](https://github.com/seata/seata/pull/5648)] 优化Server日志输出
- [[#5647](https://github.com/seata/seata/pull/5647)] 支持表和列元数据大小写敏感设置
- [[#5678](https://github.com/seata/seata/pull/5678)] 优化大小写转义符
- [[#5684](https://github.com/seata/seata/pull/5684)] 优化 CodeQL, skywalking-eyes 和 checkout 等 actions
- [[#5700](https://github.com/seata/seata/pull/5700)] 优化分布式锁竞争日志

### security:
- [[#5172](https://github.com/seata/seata/pull/5172)] 修复一些安全漏洞的版本
- [[#5683](https://github.com/seata/seata/pull/5683)] 增加Hessian 序列化黑白名单
- [[#5696](https://github.com/seata/seata/pull/5696)] 修复若干Node.js依赖安全漏洞

### test:
- [[#5380](https://github.com/seata/seata/pull/5380)] 修复 UpdateExecutorTest 单测失败问题
- [[#5382](https://github.com/seata/seata/pull/5382)] 修复多Spring版本测试失败

非常感谢以下 contributors 的代码贡献。若有无意遗漏，请报告。

<!-- 请确保您的 GitHub ID 在以下列表中 -->
- [slievrly](https://github.com/slievrly)
- [xssdpgy](https://github.com/xssdpgy)
- [albumenj](https://github.com/albumenj)
- [PeppaO](https://github.com/PeppaO)
- [yuruixin](https://github.com/yuruixin)
- [dmego](https://github.com/dmego)
- [CrazyLionLi](https://github.com/JavaLionLi)
- [xingfudeshi](https://github.com/xingfudeshi)
- [Bughue](https://github.com/Bughue)
- [pengten](https://github.com/pengten)
- [wangliang181230](https://github.com/wangliang181230)
- [GoodBoyCoder](https://github.com/GoodBoyCoder)
- [a364176773](https://github.com/a364176773)
- [isharpever](https://github.com/isharpever)
- [ZhangShiYeChina](https://github.com/ZhangShiYeChina)
- [mxsm](https://github.com/mxsm)
- [l81893521](https://github.com/l81893521)
- [liuqiufeng](https://github.com/liuqiufeng)
- [yixia](https://github.com/wt-better)
- [jumtp](https://github.com/jumtp)


同时，我们收到了社区反馈的很多有价值的issue和建议，非常感谢大家。
