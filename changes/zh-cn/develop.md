所有提交到 develop 分支的 PR 请在此处登记。

<!-- 请根据PR的类型添加 `变更记录` 到以下对应位置(feature/bugfix/optimize/test) 下 -->

### feature:
- [[#xxx](https://github.com/seata/seata/pull/xxx)] 支持 xxx

### bugfix:
- [[#5194](https://github.com/seata/seata/pull/5194)] 修复使用Oracle作为服务端DB存储时的建表失败问题
- [[#5021](https://github.com/seata/seata/pull/5201)] 修复 JDK17 下获取 Spring 原始代理对象失败的问题
- [[#5023](https://github.com/seata/seata/pull/5203)] 修复 `seata-core` 模块传递依赖冲突
- [[#5224](https://github.com/seata/seata/pull/5224)] 修复 oracle初始化脚本索引名重复的问题
- [[#5233](https://github.com/seata/seata/pull/5233)] 修复LoadBalance相关配置不一致的问题
- [[#5245](https://github.com/seata/seata/pull/5245)] 修复不完整的distribution模块依赖
- [[#5239](https://github.com/seata/seata/pull/5239)] 修复当使用JDK代理时，`getConfig` 方法获取部分配置时抛出 `ClassCastException` 异常的问题
- [[#5464](https://github.com/seata/seata/pull/5464)] 修复saga模式全局事务状态始终为Begin的问题

### optimize:
- [[#5208](https://github.com/seata/seata/pull/5208)] 优化多次重复获取Throwable#getCause问题
- [[#5212](https://github.com/seata/seata/pull/5212)] 优化不合理的日志信息级别
- [[#5237](https://github.com/seata/seata/pull/5237)] 优化异常日志打印(EnhancedServiceLoader.loadFile#cahtch)
- [[#5243](https://github.com/seata/seata/pull/5243)] 升级 kryo 5.4.0 优化对jdk17的兼容性
- [[#5153](https://github.com/seata/seata/pull/5153)] 只允许AT去尝试跨RM获取channel
- [[#5177](https://github.com/seata/seata/pull/5177)] 如果 `server.session.enable-branch-async-remove` 为真，异步删除分支，同步解锁。

### security:
- [[#5172](https://github.com/seata/seata/pull/5172)] 修复一些安全漏洞的版本

### test:
- [[#xxx](https://github.com/seata/seata/pull/xxx)] 增加 xxx 测试

非常感谢以下 contributors 的代码贡献。若有无意遗漏，请报告。

<!-- 请确保您的 GitHub ID 在以下列表中 -->
- [slievrly](https://github.com/slievrly)
- [xssdpgy](https://github.com/xssdpgy)
- [albumenj](https://github.com/albumenj)
- [PeppaO](https://github.com/PeppaO)

同时，我们收到了社区反馈的很多有价值的issue和建议，非常感谢大家。
