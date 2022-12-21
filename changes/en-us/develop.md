Add changes here for all PR submitted to the develop branch.

<!-- Please add the `changes` to the following location(feature/bugfix/optimize/test) based on the type of PR -->

### feature:
- [[#xxx](https://github.com/seata/seata/pull/xxx)] support xxx

### bugfix:
- [[#xxx](https://github.com/seata/seata/pull/xxx)] fix xxx

### optimize:
- [[#4774](https://github.com/seata/seata/pull/4774)] optimize mysql8 dependencies for seataio/seata-server image
- [[#4790](https://github.com/seata/seata/pull/4790)] Add a github action to publish Seata to OSSRH
- [[#4765](https://github.com/seata/seata/pull/4765)] mysql 8.0.29 not should be hold for connection
- [[#4750](https://github.com/seata/seata/pull/4750)] optimize unBranchLock romove xid
- [[#4797](https://github.com/seata/seata/pull/4797)] optimize the github actions
- [[#4800](https://github.com/seata/seata/pull/4800)] Add NOTICE as Apache License V2
- [[#4681](https://github.com/seata/seata/pull/4681)] optimize the check lock during global transaction
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
- [[#4974](https://github.com/seata/seata/pull/4974)] optimize cancel the limit on the number of globalStatus queries in Redis mode
- [[#4981](https://github.com/seata/seata/pull/4981)] optimize tcc fence record not exists errMessage
- [[#4985](https://github.com/seata/seata/pull/4985)] fix undo_log id repeat
- [[#4995](https://github.com/seata/seata/pull/4995)] fix mysql InsertOnDuplicateUpdate duplicate pk condition in after image query sql
- [[#5047](https://github.com/seata/seata/pull/5047)] remove useless code
- [[#5051](https://github.com/seata/seata/pull/5051)] undo log dirty throw BranchRollbackFailed_Unretriable
- [[#5075](https://github.com/seata/seata/pull/5075)] intercept the InsertOnDuplicateUpdate statement which has no primary key and unique index value
- [[#5104](https://github.com/seata/seata/pull/5104)] remove the druid dependency in ConnectionProxy
- [[#5120](https://github.com/seata/seata/pull/5120)] unify the format of configuration items in yml files
- [[#5124](https://github.com/seata/seata/pull/5124)] support oracle on delete tccfence logs
- [[#5115](https://github.com/seata/seata/pull/5115)] compatible with the `spring-boot:3.x`

### test:
- [[#xxx](https://github.com/seata/seata/pull/xxx)] add test for xxx

Thanks to these contributors for their code commits. Please report an unintended omission.

<!-- Please make sure your Github ID is in the list below -->
- [slievrly](https://github.com/slievrly)
- [wangliang181230](https://github.com/wangliang181230)

Also, we receive many valuable issues, questions and advices from our community. Thanks for you all.
