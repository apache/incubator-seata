所有提交到 develop 分支的 PR 请在此处登记。

<!-- 请根据PR的类型添加 `变更记录` 到以下对应位置(feature/bugfix/optimize/test) 下 -->

### feature:
- [[#PR_NO](https://github.com/seata/seata/pull/PR_NO)] 准确简要的PR描述

### bugfix:
- [[#5887](https://github.com/seata/seata/pull/5887)] 修复全局事务钩子重复执行
- [[#5991](https://github.com/seata/seata/pull/5991)] 修复redis sentinel master node 宕机时，lua脚本未同步的问题
- [[#6025](https://github.com/seata/seata/pull/6025)] 修复控制台点击事务信息页面中的"查看全局锁"按钮之后白屏的问题
- [[#6026](https://github.com/seata/seata/pull/6026)] 修复异常的打点
- [[#4410](https://github.com/seata/seata/pull/4410)] 修复jdk9+版本编译后，引入后ByteBuffer#flip NoSuchMethodError的问题
- [[#6104](https://github.com/seata/seata/pull/6104)] 修复在TCC模式下, dubbo 3.x版本消费者端不能生成TCC代理的问题
- [[#6409](https://github.com/seata/seata/pull/6409)] 修复 RemotingParser 失败导致 AT 模式无法执行的问题
- [[#6628](https://github.com/seata/seata/pull/6628)] 修复 Alibaba Dubbo 转换错误
- [[#6632](https://github.com/seata/seata/pull/6632)] 修复 hsf ConsumerModel 转换错误
- [[#6661](https://github.com/seata/seata/pull/6661)] 修复`tableMeta`缓存定时刷新失效问题
- [[#6715](https://github.com/apache/incubator-seata/pull/6715)] 修复达梦数据库的getRollbackInfo没有解压缩的问题
- [[#6716](https://github.com/apache/incubator-seata/pull/6716)] 修复达梦数据库的delete sql回滚失败的问题

### optimize:
- [[#6044](https://github.com/seata/seata/pull/6044)] 优化MySQL衍生数据库判断逻辑
- [[#6361](https://github.com/seata/seata/pull/6361)] 优化部分链接 401 的问题
- [[#6903](https://github.com/apache/incubator-seata/pull/6903)] 优化`tableMeta`缓存定时刷新问题
- [[#7001](https://github.com/apache/incubator-seata/pull/7001)] 优化 metrics 指标
- [[#7002](https://github.com/apache/incubator-seata/pull/7002)] 优化 AT 事务模式锁释放逻辑
- [[#7007](https://github.com/apache/incubator-seata/pull/7007)] 修复在ARM64平台下CI构建出错的问题

### security:
- [[#PR_NO](https://github.com/seata/seata/pull/PR_NO)] 准确简要的PR描述

### test:
- [[#6151](https://github.com/seata/seata/pull/6151)] 添加对 `MacOS` 和 `Windows` 的测试

非常感谢以下 contributors 的代码贡献。若有无意遗漏，请报告。

<!-- 请确保您的 GitHub ID 在以下列表中 -->
- [slievrly](https://github.com/slievrly)
- [jsbxyyx](https://github.com/jsbxyyx)
- [liuqiufeng](https://github.com/liuqiufeng)
- [ptyin](https://github.com/ptyin)
- [funky-eyes](https://github.com/funky-eyes)
- [laywin](https://github.com/laywin)
- [wuwen5](https://github.com/wuwen5)
- [caohdgege](https://github.com/caohdgege)
- [xingfudeshi](https://github.com/xingfudeshi)

同时，我们收到了社区反馈的很多有价值的issue和建议，非常感谢大家。
