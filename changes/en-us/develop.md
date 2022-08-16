Add changes here for all PR submitted to the develop branch.

<!-- Please add the `changes` to the following location(feature/bugfix/optimize/test) based on the type of PR -->

### feature:
- [[#4802](https://github.com/seata/seata/pull/4802)] dockerfile support arm64
- [[#4649](https://github.com/seata/seata/pull/4649)] seata-server support multiple registry


### bugfix:
- [[#4817](https://github.com/seata/seata/pull/4817)] fix in high version springboot property not Standard
- [[#4838](https://github.com/seata/seata/pull/4838)] fix when use Statement.executeBatch() can not generate undo log

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

Also, we receive many valuable issues, questions and advices from our community. Thanks for you all.
