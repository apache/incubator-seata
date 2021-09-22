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

e2e-scene:  存放各E2E场景测试下所依赖的服务代码，数据。例如各个服务的DockerFile文件，数据库初始化sql文件等。社区的开发者可以在这里上传自己的测试场景。

e2e-test：存放各E2E场景下测试的代码，测试依赖的docker-compose文件，配置文件，数据卷挂载等。社区的开发者可以在这里书写自己的测试代码。

## 2. E2E框架功能



## 3. 测试场景

目前提供一个使用 E2E 框架的场景实例，业务场景中主要包括一个消费者，一个生产者，Nacos-server，Seata-server，MySQL。seata-server 以 file 作为配置中心，以 Nacos 作为注册中心。

将消费者和生产者所在的模块通过 `maven package` 命令打包后，即可获得它们的docker镜像。业务逻辑为生产者调用消费者进行库存的消费，并查看消费是否成功。