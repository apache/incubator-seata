Add changes here for all PR submitted to the develop branch.


<!-- Please add the `changes` to the following location(feature/bugfix/optimize/test) based on the type of PR -->

### feature：
  - [[#4661](https://github.com/seata/seata/pull/4713)] support xid load balance
  - [[#4676](https://github.com/seata/seata/pull/4676)] support server to expose Nacos services by mounting SLB
 

### bugfix：
  - [[#4515](https://github.com/seata/seata/pull/4515)] fix the error of SeataTCCFenceAutoConfiguration when database unused
  - [[#4661](https://github.com/seata/seata/pull/4661)] fix sql exception with PostgreSQL in module console
  - [[#4667](https://github.com/seata/seata/pull/4682)] fix the exception in RedisTransactionStoreManager for update map During iteration
  - [[#4678](https://github.com/seata/seata/pull/4678)] fix the error of key transport.enableRmClientBatchSendRequest cache penetration if not configure
  - [[#4701](https://github.com/seata/seata/pull/4701)] fix missing command line args
  - [[#4607](https://github.com/seata/seata/pull/4607)] fix bug on skipping lock check
  - [[#4696](https://github.com/seata/seata/pull/4696)] fix oracle database insert value
  - [[#4726](https://github.com/seata/seata/pull/4726)] fix batch message send may return NullPointException
  - [[#4729](https://github.com/seata/seata/pull/4729)] fix set AspectTransactional.rollbackForClassName with wrong value

### optimize：
  - [[#4650](https://github.com/seata/seata/pull/4650)] fix some security vulnerabilities
  - [[#4670](https://github.com/seata/seata/pull/4670)] optimize the thread pool size of branchResultMessageExecutor
  - [[#4662](https://github.com/seata/seata/pull/4662)] optimize rollback transaction metrics
  - [[#4693](https://github.com/seata/seata/pull/4693)] optimize the console navigation bar
  - [[#4700](https://github.com/seata/seata/pull/4700)] fix maven-compiler-plugin and maven-resources-plugin execute failed
  - [[#4711](https://github.com/seata/seata/pull/4711)] separate lib dependencies for deployments
  - [[#4720](https://github.com/seata/seata/pull/4720)] optimize pom description
  - [[#4728](https://github.com/seata/seata/pull/4728)] upgrade logback dependency to 1.2.9

### test:

  - [[#4544](https://github.com/seata/seata/pull/4544)] optimize jackson dependencies in TransactionContextFilterTest
  - [[#4731](https://github.com/seata/seata/pull/4731)] fix UT failed in AsyncWorkerTest and LockManagerTest

Thanks to these contributors for their code commits. Please report an unintended omission.

<!-- Please make sure your Github ID is in the list below -->
- [slievrly](https://github.com/slievrly)
- [pengten](https://github.com/pengten)
- [YSF-A](https://github.com/YSF-A)
- [tuwenlin](https://github.com/tuwenlin)
- [Ifdevil](https://github.com/Ifdevil)
- [wingchi-leung](https://github.com/wingchi-leung)
- [liurong](https://github.com/robynron)
- [opelok-z](https://github.com/opelok-z)
- [a364176773](https://github.com/a364176773)
- [2129zxl](https://github.com/2129zxl)
- [Smery-lxm](https://github.com/Smery-lxm)

Also, we receive many valuable issues, questions and advices from our community. Thanks for you all.
