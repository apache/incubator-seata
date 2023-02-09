Add changes here for all PR submitted to the develop branch.

<!-- Please add the `changes` to the following location(feature/bugfix/optimize/test) based on the type of PR -->

### feature:
- [[#xxx](https://github.com/seata/seata/pull/xxx)] support xxx

### bugfix:
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
- [[#5316](https://github.com/seata/seata/pull/5316)] fix G1 jvm parameter in jdk8


### optimize:
- [[#5208](https://github.com/seata/seata/pull/5208)] optimize throwable getCause once more
- [[#5212](https://github.com/seata/seata/pull/5212)] optimize log message level
- [[#5237](https://github.com/seata/seata/pull/5237)] optimize exception log message print(EnhancedServiceLoader.loadFile#cahtch)
- [[#5243](https://github.com/seata/seata/pull/5243)] optimize kryo 5.4.0 optimize compatibility with jdk17
- [[#5153](https://github.com/seata/seata/pull/5153)] Only AT mode try to get channel with other app
- [[#5177](https://github.com/seata/seata/pull/5177)] If `server.session.enable-branch-async-remove` is true, delete the branch asynchronously and unlock it synchronously.
- [[#5273](https://github.com/seata/seata/pull/5273)] Optimize the compilation configuration of the `protobuf-maven-plugin` plug-in to solve the problem of too long command lines in higher versions.
- [[#5303](https://github.com/seata/seata/pull/5303)] remove startup script the -Xmn configuration

### security:
- [[#5172](https://github.com/seata/seata/pull/5172)] fix some security vulnerabilities

### test:
- [[#xxx](https://github.com/seata/seata/pull/xxx)] add test for xxx

Thanks to these contributors for their code commits. Please report an unintended omission.

<!-- Please make sure your Github ID is in the list below -->
- [slievrly](https://github.com/slievrly)
- [xssdpgy](https://github.com/xssdpgy)
- [albumenj](https://github.com/albumenj)
- [PeppaO](https://github.com/PeppaO)
- [yuruixin](https://github.com/yuruixin)
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


Also, we receive many valuable issues, questions and advices from our community. Thanks for you all.
