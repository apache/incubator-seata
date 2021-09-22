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

## 3. Test scene

Currently, a example using the Seata E2E framework is provided. Mainly includes a consumer, a producer, Nacos-server, Seata-server, and MySQL. Seata-server uses file as the config center and Nacos as the registration center.

After packaging the modules where consumers and producers in through the `maven package` command, you can get their docker images. The business logic is producer calling the consumer to consume the inventory and checking whether the consumption is successful.