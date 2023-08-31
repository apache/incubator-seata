所有提交到 develop 分支的 PR 请在此处登记。

<!-- 请根据PR的类型添加 `变更记录` 到以下对应位置(feature/bugfix/optimize/test) 下 -->

### feature:
- [[#5803](https://github.com/seata/seata/pull/5803)] docker镜像支持注入JVM参数到容器

### bugfix:
- [[#5749](https://github.com/seata/seata/pull/5749)] 修复在某些情况下，业务sql中主键字段名大小写与表元数据中的不一致，导致回滚失败
- [[#5762](https://github.com/seata/seata/pull/5762)] 修复TableMetaCache的一些字段类型，避免溢出
- [[#5769](https://github.com/seata/seata/pull/5769)] 修复不满足 sofa-rpc 中 setAttachment 方法的参数前缀要求问题
- [[#5814](https://github.com/seata/seata/pull/5814)] 修复druid依赖冲突导致的XA事务开始异常与回滚失败
- [[#5819](https://github.com/seata/seata/pull/5814)] 修复oracle alias 解析异常

### optimize:
- [[#5804](https://github.com/seata/seata/pull/5804)] 优化docker镜像的默认时区
- [[#5815](https://github.com/seata/seata/pull/5815)] 支持 Nacos applicationName 属性

### security:
- [[#5728](https://github.com/seata/seata/pull/5728)] 修复Java依赖漏洞
- [[#5766](https://github.com/seata/seata/pull/5766)] 修复序列化漏洞

### test:
- [[#XXX](https://github.com/seata/seata/pull/XXX)] XXX

非常感谢以下 contributors 的代码贡献。若有无意遗漏，请报告。

<!-- 请确保您的 GitHub ID 在以下列表中 -->
- [slievrly](https://github.com/slievrly)
- [capthua](https://github.com/capthua)
- [robynron](https://github.com/robynron)
- [dmego](https://github.com/dmego)
- [xingfudeshi](https://github.com/xingfudeshi)
- [hadoop835](https://github.com/hadoop835)
- [DroidEye2ONGU](https://github.com/DroidEye2ONGU)

同时，我们收到了社区反馈的很多有价值的issue和建议，非常感谢大家。
