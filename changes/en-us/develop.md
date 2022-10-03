Add changes here for all PR submitted to the develop branch.

<!-- Please add the `changes` to the following location(feature/bugfix/optimize/test) based on the type of PR -->

### feature:
- [[#4802](https://github.com/seata/seata/pull/4802)] dockerfile support arm64
- [[#4863](https://github.com/seata/seata/pull/4863)] support oracle and postgresql multi primary key
- [[#4649](https://github.com/seata/seata/pull/4649)] seata-server support multiple registry
- [[#4479](https://github.com/seata/seata/pull/4479)] TCC mode supports tcc annotation marked on both interface and implementation class
- [[#4468](https://github.com/seata/seata/pull/4968)] support kryo 5.3.0


### bugfix:
- [[#4954](https://github.com/seata/seata/pull/4954)] fix output expression incorrectly throws npe
- [[#4817](https://github.com/seata/seata/pull/4817)] fix in high version springboot property not Standard
- [[#4838](https://github.com/seata/seata/pull/4838)] fix when use Statement.executeBatch() can not generate undo log
- [[#4779](https://github.com/seata/seata/pull/4779)] fix and support Apache Dubbo 3
- [[#4912](https://github.com/seata/seata/pull/4912)] fix mysql InsertOnDuplicateUpdate column case is different and cannot be matched
- [[#4543](https://github.com/seata/seata/pull/4543)] fix support Oracle nclob types
- [[#4915](https://github.com/seata/seata/pull/4915)] fix failed to get server recovery properties
- [[#4919](https://github.com/seata/seata/pull/4919)] fix XID port  and  address null:0 before coordinator.init
- [[#4928](https://github.com/seata/seata/pull/4928)] fix rpcContext.getClientRMHolderMap NPE 
- [[#4953](https://github.com/seata/seata/pull/4953)] fix InsertOnDuplicateUpdate bypass modify pk

### optimize:
- [[#4774](https://github.com/seata/seata/pull/4774)] optimize mysql8 dependencies for seataio/seata-server image
- [[#4790](https://github.com/seata/seata/pull/4790)] Add a github action to publish Seata to OSSRH
- [[#4765](https://github.com/seata/seata/pull/4765)] mysql 8.0.29 not should be hold for connection
- [[#4750](https://github.com/seata/seata/pull/4750)] optimize unBranchLock romove xid
- [[#4797](https://github.com/seata/seata/pull/4797)] optimize the github actions
- [[#4800](https://github.com/seata/seata/pull/4800)] Add NOTICE as Apache License V2
- [[#4761](https://github.com/seata/seata/pull/4761)] use hget replace hmget because only one field
- [[#4414](https://github.com/seata/seata/pull/4414)] exclude log4j dependencies
- [[#4836](https://github.com/seata/seata/pull/4836)] optimize BaseTransactionalExecutor#buildLockKey(TableRecords rowsIncludingPK) method more readable
- [[#4865](https://github.com/seata/seata/pull/4865)] fix some security vulnerabilities in GGEditor
- [[#4590](https://github.com/seata/seata/pull/4590)] auto degrade enable to dynamic configure
- [[#4490](https://github.com/seata/seata/pull/4490)] tccfence log table delete by index
- [[#4911](https://github.com/seata/seata/pull/4911)] add license checker workflow
- [[#4917](https://github.com/seata/seata/pull/4917)] upgrade package-lock.json fix vulnerabilities
- [[#4924](https://github.com/seata/seata/pull/4924)] optimize pom dependencies
- [[#4932](https://github.com/seata/seata/pull/4932)] extract the default values for some properties
- [[#4925](https://github.com/seata/seata/pull/4925)] optimize java doc warning
- [[#4921](https://github.com/seata/seata/pull/4921)] fix some vulnerabilities in console and upgrade skywalking-eyes
- [[#4936](https://github.com/seata/seata/pull/4936)] optimize read of storage configuration
- [[#4946](https://github.com/seata/seata/pull/4946)] pass the sqlexception to client when get lock
- [[#4962](https://github.com/seata/seata/pull/4962)] optimize build and fix the base image

### test:
- [[#4794](https://github.com/seata/seata/pull/4794)] try to fix the test `DataSourceProxyTest.getResourceIdTest()`


Thanks to these contributors for their code commits. Please report an unintended omission.

<!-- Please make sure your Github ID is in the list below -->
- [slievrly](https://github.com/slievrly)
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

Also, we receive many valuable issues, questions and advices from our community. Thanks for you all.
