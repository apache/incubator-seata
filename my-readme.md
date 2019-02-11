# 1.Fescar项目结构
```
Fescar包括了以下子项目：

fescar-common： 包括了一些工具类和定义的异常
fescar-config： 配置项，主要用于配置NettyServer
fescar-core： Fescar的核心类，主要包括RPC服务相关和分布式事务中的一些模型对象，
              包括TM(Transaction Manager)和RM(Resource Manager)等
fescar-rm-distribution：目前为空，因为目前为单机项目，后续版本会加入分布式功能
fescar-dubbo：整合dubbo的示例
fescar-dataSource：数据库相关操作，主要作用为生成保存回滚事务相关的SQL
fescar-server：fescar的主服务，目前为单机版本
fescar-spring：fescar的spring注解，提供非侵入式服务
fescar-tm：fescar的TM模块，主要提供了事务管理功能
```


# 2.Fescar的基本原理（以回滚操作为例）
```
在Fescar的架构中，主要参与者有三个：

1、一个是单机模式运行的Fescar-Server，用于提供全局事务锁以及控制每一个分支事务是否进行回滚；
2、还有一个是提供远程RPC服务的Service，一般每一个分支事务对应一个远程RPC服务；
3、最后一个是分布式服务的调用者，通过调用多个RPC服务来完成一个完整的事务，同时也是回滚的最小单位，
一旦发生异常则需要通过fescar-server来对每一个分支事务进行回滚。

```


# 3.分布式服务调用者

##### 3.1 分布式服务的调用者通过使用fescar提供的@GlobalTransactional注解来表明一个方法需要进行分布式事务，fescar通过AOP或是CGLIB代理来对相应的方法进行增强操作，为其添加分布式事务特性，这部分的代码在GlobalTransactionalInterceptor.java中。
```
	public Object invoke(final MethodInvocation methodInvocation) throws Throwable {
        final GlobalTransactional anno = getAnnotation(methodInvocation.getMethod());
        if (anno != null) {
            try {
                return transactionalTemplate.execute(new TransactionalExecutor() {
                    @Override
                    public Object execute() throws Throwable {
                        // 在这里进行方法的调用
                        return methodInvocation.proceed();
                    }

                    @Override
                    public int timeout() {
                        return anno.timeoutMills();
                    }

                    @Override
                    public String name() {
                        if (anno.name() != null) {
                            return anno.name();
                        }
                        return formatMethod(methodInvocation.getMethod());
                    }
                });
            } catch (TransactionalExecutor.ExecutionException e) {
                ...
            }

        }
        return methodInvocation.proceed();
    }
```

##### 3.2 Fescar通过覆盖TransactionalTemplate对象的execute()来对被注解的方法进行代理调用

	public Object execute(TransactionalExecutor business) throws TransactionalExecutor.ExecutionException {

        // 1. get or create a transaction
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();

        // 2. begin transaction
        try {
            tx.begin(business.timeout(), business.name());

        } catch (TransactionException txe) {
            throw new TransactionalExecutor.ExecutionException(tx, txe,
                TransactionalExecutor.Code.BeginFailure);

        }

        Object rs = null;
        try {

            // Do Your Business
            // 在这里进行实际的方法的调用
            rs = business.execute();

        } catch (Throwable ex) {

            // 3. any business exception, rollback.
            try {
            	// 如果发生异常则进行回滚操作
                tx.rollback();

                // 3.1 Successfully rolled back
                throw new TransactionalExecutor.ExecutionException(tx, TransactionalExecutor.Code.RollbackDone, ex);

            } catch (TransactionException txe) {
                // 3.2 Failed to rollback
                throw new TransactionalExecutor.ExecutionException(tx, txe,
                    TransactionalExecutor.Code.RollbackFailure, ex);

            }

        }

        // 4. everything is fine, commit.
        try {
        	// 如果正常则进行提交
            tx.commit();

        } catch (TransactionException txe) {
            // 4.1 Failed to commit
            throw new TransactionalExecutor.ExecutionException(tx, txe,
                TransactionalExecutor.Code.CommitFailure);

        }
        return rs;
    }

##### 3.3 在TransactionalTemplate的execute()中如果发现抛出了任何的异常，则使用GlobalTransaction的rollback()进行回滚，这是一个接口，Fescar默认提供了一个实现，名叫DefaultGlobalTransaction，下面是DefaultGlobalTransaction的相关代码

	public void rollback() throws TransactionException {
        check();
        RootContext.unbind();
        if (role == GlobalTransactionRole.Participant) {
            // Participant has no responsibility of committing
            return;
        }
        status = transactionManager.rollback(xid);

    }

##### 3.4 在DefaultGlobalTransaction的rollback()方法中使用了TM的回滚方法，通过将事务ID传递给TM来进行指定事务的回滚，Fescar同样提供了一个默认的DefaultTransactionManager实现
    
    public GlobalStatus rollback(String xid) throws TransactionException {
        long txId = XID.getTransactionId(xid);
        GlobalRollbackRequest globalRollback = new GlobalRollbackRequest();
        globalRollback.setTransactionId(txId);
        GlobalRollbackResponse response = (GlobalRollbackResponse) syncCall(globalRollback);
        return response.getGlobalStatus();
    }
    
##### 从代码中可以看到在这里发起了一个同步调用，使用事务的XID组装了一个GlobalRollbackRequest，同时向Fescar-Server发起远程调用表示需要对XID这个事务进行全局回滚，在这边阻塞直到收到Fescar-Server执行完毕的回复，至此调用者的逻辑结束。


# 4.Fescar-Server

##### Fescar-Server在启动的时候会启动RpcServer

    public static void main(String[] args) throws IOException {
        RpcServer rpcServer = new RpcServer(WORKING_THREADS);
        ...
        rpcServer.init();
        System.exit(0);
    }

##### RpcServer的init()方法中会完成服务的启动及监听
    
    public void init() {
        super.init();
        setChannelHandlers(RpcServer.this);
        DefaultServerMessageListenerImpl defaultServerMessageListenerImpl = new DefaultServerMessageListenerImpl(
            transactionMessageHandler);
        defaultServerMessageListenerImpl.setServerMessageSender(this);
        this.setServerMessageListener(defaultServerMessageListenerImpl);
        super.start();

    }
    
    RpcServer启动之后就会监听来自TM的消息，如上代码所示，在开启RpcServer之前会注册一个DefaultServerMessageListenerImpl用于对TM发过来的消息进行监听。

##### 如上一节所说，当开始一个分布式事务的时候TM首先会向Fescar-Server注册分支事务，然后进行分支事务的调用，当发现调用抛出异常后就会向Fescar-Server发起GlobalRollbackRequest，当Fescar-Server收到这个消息之后就会根据传过来的XID寻找相应的事务进行回滚操作

    public GlobalRollbackResponse handle(GlobalRollbackRequest request, final RpcContext rpcContext) {
        GlobalRollbackResponse response = new GlobalRollbackResponse();
        exceptionHandleTemplate(new Callback<GlobalRollbackRequest, GlobalRollbackResponse>() {
            @Override
            public void execute(GlobalRollbackRequest request, GlobalRollbackResponse response) throws TransactionException {
                doGlobalRollback(request, response, rpcContext);
            }
        }, request, response);
        return response;
    }
    
    protected void doGlobalRollback(GlobalRollbackRequest request, GlobalRollbackResponse response, RpcContext rpcContext) throws TransactionException {
        response.setGlobalStatus(core.rollback(XID.generateXID(request.getTransactionId())));

    }
    
##### listener的handler()方法会调用doGlobalRollback()方法，doGlobalRollback()方法会调用DefaultCore的rollback()
    
    public GlobalStatus rollback(String xid) throws TransactionException {
        GlobalSession globalSession = SessionHolder.findGlobalSession(XID.getTransactionId(xid));
        if (globalSession == null) {
            return GlobalStatus.Finished;
        }
        GlobalStatus status = globalSession.getStatus();

        globalSession.close(); // Highlight: Firstly, close the session, then no more branch can be registered.

        if (status == GlobalStatus.Begin) {
            globalSession.changeStatus(GlobalStatus.Rollbacking);
            // 调用内部的doGlobalRollback()
            doGlobalRollback(globalSession, false);

        }
        return globalSession.getStatus();
    }

    @Override
    public void doGlobalRollback(GlobalSession globalSession, boolean retrying) throws TransactionException {
        for (BranchSession branchSession : globalSession.getReverseSortedBranches()) {
            BranchStatus currentBranchStatus = branchSession.getStatus();
            if (currentBranchStatus == BranchStatus.PhaseOne_Failed) {
                continue;
            }
            try {
                BranchStatus branchStatus = resourceManagerInbound.branchRollback(XID.generateXID(branchSession.getTransactionId()), branchSession.getBranchId(),
                        branchSession.getResourceId(), branchSession.getApplicationData());

                ...
            }
    }

##### doGlobalRollback()方法中最重要的是try的第一句，调用了ResourceManagerInbound的branchRollback()方法来进行分支回滚，这是一个抽象方法，同样Fescar-Server提供了一个实现，在DataSourceManager.java中

       public BranchStatus branchRollback(String xid, long branchId, String resourceId, String applicationData) throws TransactionException {
           DataSourceProxy dataSourceProxy = get(resourceId);
           if (dataSourceProxy == null) {
               throw new ShouldNeverHappenException();
           }
           try {
               UndoLogManager.undo(dataSourceProxy, xid, branchId);
           } catch (TransactionException te) {
               ...
           }
           return BranchStatus.PhaseTwo_Rollbacked;
    
       }

    
##### 整个回滚操作中最重要的就是UndoLogManager，在这里通过undolog记录的用于回滚的信息进行数据库回滚，Fescar-Server的回滚实现思路是根据INSERT,UPDATE和DELETE三种语句进行解析，反向生成用于回滚的SQL，具体实现可以参见fescar-rm-distribution项目中undo包中的MySQLUndoDeleteExecutor，MySQLUndoInsertExecutor和MySQLUndoUpdateExecutor，最终Fescar-Server会将回滚操作的结果组装成GlobalRollbackResponse返回给TM调用方，至此Fescar-Server的回滚逻辑完成。

