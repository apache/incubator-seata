# Seata E2E测试框架

目前此框架还在初次测试阶段，欢迎开发者提供相关反馈，后续会继续优化此框架。

## 1. 项目结构

common：E2E框架本身，以下是框架的组成简要说明。

- config：用于配置E2E框架的日志输出地址等配置。
- docker：负责在测试前后进行docker-compose文件中容器初始化和销毁，根据[Apache Skywalking](https://skywalking.apache.org/)中的java E2E框架的此功能改编而成。在结合skywalking的开发者们使用此功能的实践上进行修改，删除了一些开发者们不常用的冗余功能并将其精简。
- factory：封装了helper下的所有工具类。
- helper：一系列用于在测试中使用的有状态的工具类，包括简易的orm驱动的数据库查询更新工具类，map中的值与javaBean的属性值验证工具类，压力测试工具类（同步执行），固定次数执行任务类（同步执行），定时任务执行任务类（异步执行）。
- model：存放一些数据类。
- trigger：测试注解功能，根据Apache Skywalking中的E2E框架的测试注解进行修改。支持多错误（原为单个错误）下重试，按照一定次数重试，间隔一定时间重试。

e2e-scene:  存放各E2E场景测试下所依赖的服务代码，数据。例如各个服务的DockerFile文件，数据库初始化Sql文件等。社区的开发者可以在这里上传自己的测试场景。

e2e-test：存放各E2E场景下测试的代码，测试依赖的docker-compose文件，配置文件，数据卷挂载等。社区的开发者可以在这里书写自己的测试代码。

## 2. E2E框架功能

Seata e2e 框架是一个帮助开发者进行和 seata-server 有关的端到端测试框架。开发者只需要提供 docker-compose 文件即可在测试前进行容器启动，并通过代码的方式中借助框架中的各种功能来对测试进行自定义的操作。下面是框架中的主要功能：

### 2.1 测试注解功能

开发者可使用注解 `@TestTrigger` 像 `@Test ` 一样注解在待测试的方法上，并且可以指定此次测试方法重试次数，重试间隔时间，遇到哪些错误可以接着进行重试。具体的实现思路是通过 [junit5 文档](https://junit.org/junit5/docs/current/user-guide/#overview) 的 `Extension` 与 `@TestTemplate` 实现。

#### 2.2 docker-compose 文件中容器初始化与销毁功能

开发者只需提供 docker-compose 文件，在测试方法开始之前，框架会自动生成 docker-compose 文件中的所有容器，在测试方法结束后，框架会自动删除 docker-compose 文件中的所有容器。此功能具体依赖的第三方框架为 [Testcontainers](https://www.testcontainers.org/quickstart/junit_5_quickstart/)。 Testcontainers 将启动一个小的“ambassador”容器，它将在 Compose 托管容器和可供测试访问的端口之间进行代理。这是使用一个单独的最小容器来完成的，该容器将 socat（Netcat的加强版）作为 TCP 代理运行。

#### 2.3 DruidJdbcHelper 功能

`DruidJdbcHelper` 里面封装了 `Druid` 连接池和 `JdbcTemplate` 。允许开发者在测试时直接使用 orm 方式的进行数据的查询，直接传入sql 语句以及期望返回的数据类型即可。同时也允许进行简单的 update 语句执行和 sql 脚本执行。如果需要进行全面的数据库操作，推荐还是直接使用 mybaits-plus 等框架。

#### 2.4 PressureTask，TimesTask，CronTask 功能

`PressureTask ` 是一个压力测试任务（同步执行），`TimesTask` 是一个固定次数执行任务（同步执行），`CronTask` 是一个按照一定时间间隔执行任务（异步执行）。`PressureTask` 根据开发者设置的线程数和总的执行次数进行任务的执行，并且开发者可以传入相关函数进行每次测试的回调处理；`TimesTask` 根据开发者设置的执行次数和执行时间间隔执行；`CronTask`根据开发者设置的时间间隔一直执行，直至通过代码手动停止。三者待执行完毕后，都会将统计的执行结果输出。

#### 2.5 MapVerifyBeans 功能

在访问数据查询接口后，一般会得到一个对应一个数据库表中的 javaBean 对象。开发者将自己期望验证的数据（`value`）以及其在 java Bean 中对应的属性名（`key`）存在一个 `map` 中。即可将 `map` 与访问接口后得到的 javaBean 中的数据进行对比验证。

## 3. 测试场景

目前提供一个使用 E2E 框架的场景实例，业务场景中主要包括一个消费者，一个生产者，Nacos-server，Seata-server，MySQL。seata-server 以 file 作为配置中心，以 Nacos 作为注册中心。

将消费者和生产者所在的模块通过 `maven package` 命令打包后，即可获得它们的docker镜像。业务逻辑为生产者调用消费者进行库存的消费，并查看消费是否成功。