Add changes here for all PR submitted to the develop branch.

<!-- Please add the `changes` to the following location(feature/bugfix/optimize/test) based on the type of PR -->

### feature:
- [[#5476](https://github.com/seata/seata/pull/5476)] First support `native-image` for `seata-client`
- [[#5495](https://github.com/seata/seata/pull/5495)] console integration saga-statemachine-designer
- [[#5668](https://github.com/seata/seata/pull/5668)] compatible with file.conf and registry.conf configurations in version 1.4.2 and below

### bugfix:
- [[#5682](https://github.com/seata/seata/pull/5682)]  fix saga mode replay context lost startParams
- [[#5671](https://github.com/seata/seata/pull/5671)] fix saga mode serviceTask inputParams json autoType convert exception
- [[#5194](https://github.com/seata/seata/pull/5194)] fix wrong keyword order for oracle when creating a table
- [[#5021](https://github.com/seata/seata/pull/5201)] Fix JDK Reflection for Spring origin proxy failed in JDK17
- [[#5023](https://github.com/seata/seata/pull/5203)] Fix `seata-core` dependency transitive conflict in `seata-dubbo`
- [[#5224](https://github.com/seata/seata/pull/5224)] fix oracle initialize script index_name is duplicate 
- [[#5233](https://github.com/seata/seata/pull/5233)] fix the inconsistent configuration item names related to LoadBalance
- [[#5266](https://github.com/seata/seata/pull/5265)] fix server console has queried the released lock
- [[#5245](https://github.com/seata/seata/pull/5245)] fix the incomplete dependency of distribution module
- [[#5239](https://github.com/seata/seata/pull/5239)] fix `getConfig` throw `ClassCastException` when use JDK proxy
- [[#5281](https://github.com/seata/seata/pull/5281)] parallel request handle throw IndexOutOfBoundsException
- [[#5288](https://github.com/seata/seata/pull/5288)] fix auto-increment of pk columns in Oracle in AT mode
- [[#5287](https://github.com/seata/seata/pull/5287)] fix auto-increment of pk columns in PostgreSQL in AT mode
- [[#5299](https://github.com/seata/seata/pull/5299)] fix GlobalSession deletion when retry rollback or retry commit timeout
- [[#5307](https://github.com/seata/seata/pull/5307)] fix that keywords don't add escaped characters
- [[#5311](https://github.com/seata/seata/pull/5311)] remove RollbackRetryTimeout sessions during in file storage recover
- [[#4734](https://github.com/seata/seata/pull/4734)] check if table meta cache should be refreshed in AT mode
- [[#5316](https://github.com/seata/seata/pull/5316)] fix G1 jvm parameter in jdk8
- [[#5321](https://github.com/seata/seata/pull/5321)] fix When the rollback logic on the TC side returns RollbackFailed, the custom FailureHandler is not executed
- [[#5332](https://github.com/seata/seata/pull/5332)] fix bugs found in unit tests
- [[#5145](https://github.com/seata/seata/pull/5145)] fix global session is always begin in saga mode
- [[#5413](https://github.com/seata/seata/pull/5413)] fix bad service configuration file and compilation failure
- [[#5415](https://github.com/seata/seata/pull/5415)] fix transaction timeout on client side not execute hook and failureHandler
- [[#5447](https://github.com/seata/seata/pull/5447)] fix oracle xa mode cannnot be used By same database
- [[#5472](https://github.com/seata/seata/pull/5472)] fix if using `@GlobalTransactional` in RM, `ShouldNeverHappenException` will be thrown
- [[#5535](https://github.com/seata/seata/pull/5535)] fix the log file path was loaded incorrectly
- [[#5538](https://github.com/seata/seata/pull/5538)] fix finished transaction swallows exception when committing
- [[#5539](https://github.com/seata/seata/pull/5539)] fix the full table scan issue with 'setDate' condition in Oracle 10g
- [[#5540](https://github.com/seata/seata/pull/5540)] fix GlobalStatus=9 can't be cleared in DB storage mode
- [[#5552](https://github.com/seata/seata/pull/5552)] fix mariadb rollback failed
- [[#5583](https://github.com/seata/seata/pull/5583)] fix grpc interceptor xid unbinding problem
- [[#5602](https://github.com/seata/seata/pull/5602)] fix log in participant transaction role
- [[#5645](https://github.com/seata/seata/pull/5645)] fix oracle insert undolog failed
- [[#5659](https://github.com/seata/seata/pull/5659)] fix the issue of case sensitivity enforcement on the database after adding escape characters to keywords
- [[#5663](https://github.com/seata/seata/pull/5663)] bugfix: fix the timeout is null when the connectionProxyXA connection is reused
- [[#5675](https://github.com/seata/seata/pull/5675)] bugfix: fix compatibility between xxx.grouplist and grouplist.xxx configuration items
- [[#5690](https://github.com/seata/seata/pull/5690)] fix console print `unauthorized error`

### optimize:
- [[#5208](https://github.com/seata/seata/pull/5208)] optimize throwable getCause once more
- [[#5212](https://github.com/seata/seata/pull/5212)] optimize log message level
- [[#5237](https://github.com/seata/seata/pull/5237)] optimize exception log message print(EnhancedServiceLoader.loadFile#cahtch)
- [[#5089](https://github.com/seata/seata/pull/5089)] optimize the check of the delay value of the TCC fence log clean task
- [[#5243](https://github.com/seata/seata/pull/5243)] optimize kryo 5.4.0 optimize compatibility with jdk17
- [[#5153](https://github.com/seata/seata/pull/5153)] Only AT mode try to get channel with other app
- [[#5177](https://github.com/seata/seata/pull/5177)] If `server.session.enable-branch-async-remove` is true, delete the branch asynchronously and unlock it synchronously.
- [[#5273](https://github.com/seata/seata/pull/5273)] Optimize the compilation configuration of the `protobuf-maven-plugin` plug-in to solve the problem of too long command lines in higher versions.
- [[#5303](https://github.com/seata/seata/pull/5303)] remove startup script the -Xmn configuration
- [[#5325](https://github.com/seata/seata/pull/5325)] add store mode,config type and registry type log info
- [[#5315](https://github.com/seata/seata/pull/5315)] optimize the log of SPI
- [[#5323](https://github.com/seata/seata/pull/5323)] add time info for global transaction timeout log
- [[#5414](https://github.com/seata/seata/pull/5414)] optimize transaction fail handler
- [[#5537](https://github.com/seata/seata/pull/5537)] optimize transaction log on client side
- [[#5541](https://github.com/seata/seata/pull/5541)] optimize server log output
- [[#5548](https://github.com/seata/seata/pull/5548)] update expire gpg key and publish workflow
- [[#5638](https://github.com/seata/seata/pull/5638)] optimize: set server's transaction level to READ_COMMITTED
- [[#5646](https://github.com/seata/seata/pull/5646)] refactor ColumnUtils and EscapeHandler
- [[#5648](https://github.com/seata/seata/pull/5648)] optimize server logs print
- [[#5647](https://github.com/seata/seata/pull/5647)] support case-sensitive attributes for table and column metadata
- [[#5678](https://github.com/seata/seata/pull/5678)] optimize escape character for case of columnNames
- [[#5684](https://github.com/seata/seata/pull/5684)] optimize github actions for CodeQL, skywalking-eyes and checkout
- [[#5700](https://github.com/seata/seata/pull/5700)] optimize distributed lock log


### security:
- [[#5172](https://github.com/seata/seata/pull/5172)] fix some security vulnerabilities
- [[#5683](https://github.com/seata/seata/pull/5683)] add Hessian Serializer WhiteDenyList
- [[#5696](https://github.com/seata/seata/pull/5696)] fix several node.js security vulnerabilities

### test:
- [[#5380](https://github.com/seata/seata/pull/5380)] fix UpdateExecutorTest failed
- [[#5382](https://github.com/seata/seata/pull/5382)] fix multi spring version test failed

Thanks to these contributors for their code commits. Please report an unintended omission.

<!-- Please make sure your Github ID is in the list below -->
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
- [yixia](https://github.com/wt-better)
- [jumtp](https://github.com/jumtp)


Also, we receive many valuable issues, questions and advices from our community. Thanks for you all.
