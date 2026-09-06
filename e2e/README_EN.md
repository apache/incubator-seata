# Seata E2E test framework

At present, the framework is still in the initial test stage. Developers are welcome to provide relevant feedback, and the framework will continue to be optimized in the future.

## 1. Project structure

**common**: E2E framework itself. The following is a brief description of the composition of the framework. 

- **config**: Used to set the log output address of the E2E framework and other configurations.
- **docker**: Responsible for the initialization and destruction of the containers in the docker-compose file before and after the test, adapted from this function of the java E2E framework in [Apache Skywalking](https://skywalking.apache.org/). Some redundant functions that are not commonly used by developers have been removed and streamlined.
- **factory**: Encapsulates all the tool classes under the helper.
- **helper**: A series of stateful tool used in testing, including simple orm-driven database query and update tool , map values and javaBean field value verification tool , pressure test tool (synchronous execution), fixed number of task executor (synchronous execution) and asynchronous scheduled task executor (asynchronous execution).
- **model**: Some data models.
- **trigger**: Modified according to the test annotation of the E2E framework in Apache Skywalking. Supports retry under multiple errors (originally a single error), retry according to a certain number of times and retry at a certain interval.

**e2e-scene**: Code and data that each E2E test scene depends on. For example, Dockerfile files of various services, database initialization sql files, etc. Developers can upload their own test scene here.

**e2e-test**: Test code in each E2E test scene, the docker-compose file that the test depends on, configuration files, data volume mount, etc. Developers can write their own test code here.

## 2. E2E framework function

Seata e2e framework is a framework that helps developers to carry out end-to-end test related to Seata-server. Developers only need to provide the docker-compose file to start containers before the test, and use the various functions in the framework to customize the test by way of code. The following are the main functions in the framework:

### 2.1 Test annotation 

Developers can use the annotation `@TestTrigger` to annotate the method to be tested like `@Test`. The number of retries, the retry interval and which errors can be retried can be set for the test method. The realization idea is realized through the `Extension` and `@TestTemplate` of [junit5 document](https://junit.org/junit5/docs/current/user-guide/#overview).

#### 2.2 Container initialization and destruction in docker-compose file

Developers only need to provide the docker-compose file. Before the test method starts, the framework will automatically start all the containers in the docker-compose file. After the test method ends, the framework will automatically delete all the containers in the docker-compose file. The third-party framework that this function relies on is [Testcontainers](https://www.testcontainers.org/quickstart/junit_5_quickstart/). Testcontainers will spin up a small 'ambassador' container, which will proxy between the Compose-managed containers and ports that are accessible to your tests. This is done using a separate, minimal container that runs socat as a TCP proxy.

#### 2.3 DruidJdbcHelper

`DruidJdbcHelper` encapsulates `Druid` connection pool and `JdbcTemplate`. Allow developers to directly use the orm method to query data during testing. It also allows simple update statement execution and sql script execution. If you need to perform comprehensive database operations, it is recommended to directly use frameworks such as JPA.

#### 2.4 PressureTask, TimesTask, CronTask 

`PressureTask` is a pressure test task (synchronous execution), `TimesTask` is a fixed-time execution task (synchronous execution), and `CronTask` is a task executed at a certain time interval (asynchronous execution). `PressureTask` executes according to the number of threads and total execution times set by the developer, and the developer can pass in related functions for the callback processing of each test; `TimesTask` executes according to the execution times and execution time interval set by the developer ; `CronTask` executes according to the time interval set by the developer until it is manually stopped by code. After the three are executed, the statistical execution results will be output.

#### 2.5 MapVerifyBeans 

After accessing the data query interface, you will generally get a javaBean object corresponding to a database table. Developers store the data (`value`) they expect to verify and the corresponding field name (`key`) in the java Bean in a `map`. You can compare and verify the data in the javaBean obtained after accessing the interface with `map`.

## 3. Test scene

Currently, a example using the Seata E2E framework is provided. Mainly includes a consumer, a producer, Nacos-server, Seata-server, and MySQL. Seata-server uses file as the config center and Nacos as the registration center.

After packaging the modules where consumers and producers in through the `maven package` command, you can get their docker images. The business logic is producer calling the consumer to consume the inventory and checking whether the consumption is successful.