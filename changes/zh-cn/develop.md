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
- [[#5413](https://github.com/seata/seata/pull/5413)] 修复 arm64平台下的JDK和Spring兼容问题
- [[#5415](https://github.com/seata/seata/pull/5415)] 修复客户侧事务提交前超时未执行hook和failureHandler的问题
- [[#5447](https://github.com/seata/seata/pull/5447)] fix oracle xa mode cannnot be used By same database





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
- [[#5438](https://github.com/seata/seata/pull/5438)] 优化code style检测属性




### security:
- [[#5172](https://github.com/seata/seata/pull/5172)] 修复一些安全漏洞的版本

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


同时，我们收到了社区反馈的很多有价值的issue和建议，非常感谢大家。
