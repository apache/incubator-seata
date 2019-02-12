<img src="https://github.com/fescar-group/fescar-samples/blob/master/doc/img/fescar.png"  height="100" width="426">

# Fescar: Fast & Easy Commit And Rollback

[![Build Status](https://travis-ci.org/alibaba/fescar.svg?branch=develop)](https://travis-ci.org/alibaba/fescar)
[![codecov](https://codecov.io/gh/alibaba/fescar/branch/develop/graph/badge.svg)](https://codecov.io/gh/alibaba/fescar)
![license](https://img.shields.io/github/license/alibaba/fescar.svg)
![maven](https://img.shields.io/maven-central/v/com.alibaba.fescar/fescar-all.svg)

## What is Fescar?

A **distributed transaction solution** with high performance and ease of use for **microservices** architecture.

### Distributed Transaction Problem in Microservices

Let's imagine a traditional monolithic application. Its business is built up with 3 modules. They use a single local data source.

Naturally, data consistency will be guaranteed by the local transaction.

![Monolithic App](https://cdn.nlark.com/lark/0/2018/png/18862/1545296770244-4cedf37e-9dc6-4fc0-a97f-f4240b9d8640.png) 

Things have changed in microservices architecture. The 3 modules mentioned above are designed to be 3 services on top of 3 different data sources ([Pattern: Database per service](http://microservices.io/patterns/data/database-per-service.html)). Data consistency within every single service is naturally guaranteed by the local transaction. 

**But how about the whole business logic scope?**

![Microservices Problem](https://cdn.nlark.com/lark/0/2018/png/18862/1545296781231-4029da9c-8803-43a4-ac2f-6c8b1e2ea448.png) 

### How Fescar do?

Fescar is just a solution to the problem mentioned above. 

![Fescar solution](https://cdn.nlark.com/lark/0/2018/png/18862/1545296791074-3bce7bce-025e-45c3-9386-7b95135dade8.png)

Firstly, how to define a **Distributed Transaction**?

We say, a **Distributed Transaction** is a **Global Transaction** which is made up with a batch of **Branch Transaction**, and normally **Branch Transaction** is just **Local Transaction**.

![Global & Branch](https://cdn.nlark.com/lark/0/2018/png/18862/1545015454979-a18e16f6-ed41-44f1-9c7a-bd82c4d5ff99.png) 

There are 3 basic components in Fescar: 

- **Transaction Coordinator(TC):** Maintain status of global and branch transactions, drive the global commit or rollback.
- **Transaction Manager(TM):** Define the scope of global transaction: begin a global transaction, commit or rollback a global transaction.
- **Resource Manager(RM):** Manage resources that branch transactions working on, talk to TC for registering branch transactions and reporting status of branch transactions, and drive the branch transaction commit or rollback.

![Model](https://cdn.nlark.com/lark/0/2018/png/18862/1545013915286-4a90f0df-5fda-41e1-91e0-2aa3d331c035.png) 

A typical lifecycle of Fescar managed distributed transaction:

1. TM asks TC to begin a new global transaction. TC generates an XID representing the global transaction.
2. XID is propagated through microservices' invoke chain.
3. RM register local transaction as a branch of the corresponding global transaction of XID to TC. 
4. TM asks TC for committing or rollbacking the corresponding global transaction of XID.
5. TC drives all branch transactions under the corresponding global transaction of XID to finish branch committing or rollbacking.

![Typical Process](https://cdn.nlark.com/lark/0/2018/png/18862/1545296917881-26fabeb9-71fa-4f3e-8a7a-fc317d3389f4.png) 

For more details about principle and design, please go to [Fescar wiki page](https://github.com/alibaba/fescar/wiki). 

### History

- **TXC**: Taobao Transaction Constructor. Alibaba middleware team start this project since 2014 to meet distributed transaction problem caused by application architecture change from monolithic to microservices.
- **GTS**: Global Transaction Service. TXC as an Aliyun middleware product with new name GTS was published since 2016.
- **Fescar**: we start the open source project Fescar based on TXC/GTS since 2019 to work closely with the community in the future.

## Maven dependency
```xml
<fescar.version>0.1.4</fescar.version>

<dependency>
    <groupId>com.alibaba.fescar</groupId>
    <artifactId>fescar-all</artifactId>
    <version>${fescar.version}</version>
</dependency>

```
## Quick Start

[Quick Start](https://github.com/alibaba/fescar/wiki/Quick-Start)

## Documentation

You can view the full documentation from the wiki: [Fescar wiki page](https://github.com/alibaba/fescar/wiki).

## Reporting bugs

Please follow the [template](https://github.com/alibaba/fescar/blob/develop/.github/ISSUE_TEMPLATE/BUG_REPORT.md) for reporting any issues.


## Contributing

Contributors are welcomed to join the FEATS project. Please check [CONTRIBUTING](./CONTRIBUTING.md) about how to contribute to this project.


## Contact

* [Twitter](): TBD. Follow along for latest Fescar news on Twitter.
* Email Group:
     * dev.fescar@gmail.com: Fescar developer discussion (APIs, feature design, etc).

**Dingtalk**

<img src="https://upload-images.jianshu.io/upload_images/4420767-4e95b186a1a1bfba.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240"  height="300" width="300">


## Fescar ecosystem

* [Fescar Ecosystem Entry](https://github.com/fescar-group) - A GitHub group `fescar-group` to gather all Fescar relevant projects not appropriate in [alibaba](https://github.com/alibaba) group yet
* [Fescar Samples](https://github.com/fescar-group/fescar-samples) - Samples for Fescar
* [Fescar Docker](https://github.com/fescar-group/fescar-docker) - Fescar integration with docker
* [Fescar K8s](https://github.com/fescar-group/fescar-k8s) - Fescar integration with k8s
* [Awesome Fescar](https://github.com/fescar-group/awesome-fescar) - Description of Fescar related projects 
* [Fescar Website](https://github.com/fescar-group/fescar.io) - Fescar official website （***In the process of design***）

## Contributors

This project exists thanks to all the people who contribute. [[Contribute](CONTRIBUTING.md)].
<a href="https://github.com/alibaba/fescar/graphs/contributors"><img src="https://opencollective.com/fescar/contributors.svg?width=890&button=false" /></a>

## License

Fescar is under the Apache 2.0 license. See the [LICENSE](https://github.com/alibaba/fescar/blob/master/LICENSE) file for details.
