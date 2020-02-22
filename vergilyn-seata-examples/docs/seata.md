# seata

1. seata的client-batch-request
参考： `AbstractRpcRemoting#sendAsyncRequest(...)`、`MergedSendRunnable.class`
逻辑大概是，seata-client启动时，创建1个线程在后台一直运行（获取mergeLock 再等待1ms）
然后取出 basketMap 中所有的请求，相同address组装后发送。

不知道怎么说，感觉存在一些性能问题。比如可能某次消息量很大，这个间隔时间不支持配置等问题...


client 发起 begin-global-transaction
    server `DefaultCore#begin(...)` **add, global_table**

`AbstractDMLBaseExecutor#executeAutoCommitFalse(...)`

seata中用于 TableMetaCache `com.github.ben-manes.caffeine:caffeine:2.7.0`
参考： `io.seata.rm.datasource.sql.struct.cache.AbstractTableMetaCache`

`io.seata.server.coordinator.DefaultCore#commit()` 允许async-commit时，哪里操作了branch_global？

如果`!shouldCommit = true`，且`status != Committed/Finish`时 client怎么处理？
