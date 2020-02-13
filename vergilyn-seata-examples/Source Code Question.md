# Source Code Question

1. 
由于seata源码中使用了`protobuf`，导致无法启动。  
例如 [BranchCommitRequestConvertor.java](../codec/seata-codec-protobuf/src/main/java/io/seata/codec/protobuf/convertor/BranchCommitRequestConvertor.java)
提示`Cannot resolve symbol 'io.seata.codec.protobuf.generated.BranchCommitRequestProto'`。

由于`BranchCommitRequestProto`是通过protobuf自动生成，**暂时解决方法**： 本地执行下`mvn clean install -DskipTests=true`
（参考： [程序包io.seata.codec.protobuf.generated不存在](https://blog.csdn.net/zl570932980/article/details/103648812)）


2. 
SEATA启动时，会初始化一些定时任务`io.seata.server.coordinator.DefaultCoordinator#init()`包含：  
`handleRetryRollbacking()`、`handleRetryCommitting()`、`handleAsyncCommitting()`、`timeoutCheck()`、`undoLogDelete()`。

然后会创建一个“register ShutdownHook”，用于shutdown这些定时任务，其原理是调用`io.seata.core.rpc.Disposable#destroy()`。
`DefaultCoordinator`实现了该方法，但是其中并未处理`undoLogDelete`。

**个人觉得可能是忘记处理了。**
