<img src="https://github.com/seata/seata-samples/blob/master/doc/img/seata.png"  height="100" width="426">

# Seata: Simple Extensible Autonomous Transaction Architecture

[![Build Status](https://travis-ci.org/seata/seata.svg?branch=develop)](https://travis-ci.org/seata/seata)
[![codecov](https://codecov.io/gh/seata/seata/branch/develop/graph/badge.svg)](https://codecov.io/gh/seata/seata)
[![license](https://img.shields.io/github/license/seata/seata.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![maven](https://img.shields.io/maven-central/v/io.seata/seata-parent.svg)](https://search.maven.org/search?q=io.seata)
[![](https://img.shields.io/twitter/follow/seataio.svg?label=Follow&style=social&logoWidth=0)](https://twitter.com/intent/follow?screen_name=seataio)


## What is Seata?

A **distributed transaction solution** with high performance and ease of use for **microservices** architecture.
### Distributed Transaction Problem in Microservices

Let's imagine a traditional monolithic application. Its business is built up with 3 modules. They use a single local data source.

Naturally, data consistency will be guaranteed by the local transaction.

![Monolithic App](https://cdn.nlark.com/lark/0/2018/png/18862/1545296770244-4cedf37e-9dc6-4fc0-a97f-f4240b9d8640.png) 

Things have changed in microservices architecture. The 3 modules mentioned above are designed to be 3 services on top of 3 different data sources ([Pattern: Database per service](http://microservices.io/patterns/data/database-per-service.html)). Data consistency within every single service is naturally guaranteed by the local transaction. 

**But how about the whole business logic scope?**

![Microservices Problem](https://cdn.nlark.com/lark/0/2018/png/18862/1545296781231-4029da9c-8803-43a4-ac2f-6c8b1e2ea448.png) 

### How Seata do?

Seata is just a solution to the problem mentioned above. 

![Seata solution](https://cdn.nlark.com/lark/0/2018/png/18862/1545296791074-3bce7bce-025e-45c3-9386-7b95135dade8.png)

Firstly, how to define a **Distributed Transaction**?

We say, a **Distributed Transaction** is a **Global Transaction** which is made up with a batch of **Branch Transaction**, and normally **Branch Transaction** is just **Local Transaction**.

![Global & Branch](https://cdn.nlark.com/lark/0/2018/png/18862/1545015454979-a18e16f6-ed41-44f1-9c7a-bd82c4d5ff99.png) 

There are 3 basic components in Seata: 

- **Transaction Coordinator(TC):** Maintain status of global and branch transactions, drive the global commit or rollback.
- **Transaction Manager(TM):** Define the scope of global transaction: begin a global transaction, commit or rollback a global transaction.
- **Resource Manager(RM):** Manage resources that branch transactions working on, talk to TC for registering branch transactions and reporting status of branch transactions, and drive the branch transaction commit or rollback.

![Model](https://cdn.nlark.com/lark/0/2018/png/18862/1545013915286-4a90f0df-5fda-41e1-91e0-2aa3d331c035.png) 

A typical lifecycle of Seata managed distributed transaction:

1. TM asks TC to begin a new global transaction. TC generates an XID representing the global transaction.
2. XID is propagated through microservices' invoke chain.
3. RM register local transaction as a branch of the corresponding global transaction of XID to TC. 
4. TM asks TC for committing or rollbacking the corresponding global transaction of XID.
5. TC drives all branch transactions under the corresponding global transaction of XID to finish branch committing or rollbacking.

![Typical Process](https://cdn.nlark.com/lark/0/2018/png/18862/1545296917881-26fabeb9-71fa-4f3e-8a7a-fc317d3389f4.png) 

For more details about principle and design, please go to [Seata wiki page](https://github.com/seata/seata/wiki). 

### History

##### Ant Financial

- **XTS**: Extended Transaction Service. Ant Financial middleware team developed the distributed transaction middleware since 2007, which is widely used in Ant Financial and solves the problems of data consistency across databases and services.

- **DTX**: Distributed Transaction Extended. Since 2013, XTS has been published on the Ant Financial Cloud, with the name of DTX .

##### Alibaba

- **TXC**: Taobao Transaction Constructor. Alibaba middleware team start this project since 2014 to meet distributed transaction problem caused by application architecture change from monolithic to microservices.
- **GTS**: Global Transaction Service. TXC as an Aliyun middleware product with new name GTS was published since 2016.
- **Fescar**: we start the open source project Fescar based on TXC/GTS since 2019 to work closely with the community in the future.


##### Seata Community

- **Seata** :Simple Extensible Autonomous Transaction Architecture. Ant Financial joins Fescar, which make it to be a more neutral and open community for distributed transaction，and Fescar be renamed to Seata.



## Maven dependency
```xml
<seata.version>0.9.0</seata.version>

<dependency>
    <groupId>io.seata</groupId>
    <artifactId>seata-all</artifactId>
    <version>${seata.version}</version>
</dependency>

```
## Quick Start

[Quick Start](https://github.com/seata/seata/wiki/Quick-Start)

## Documentation

You can view the full documentation from the wiki: [Seata wiki page](https://github.com/seata/seata/wiki).

## Reporting bugs

Please follow the [template](https://github.com/seata/seata/blob/develop/.github/ISSUE_TEMPLATE/BUG_REPORT.md) for reporting any issues.


## Contributing

Contributors are welcomed to join the Seata project. Please check [CONTRIBUTING](./CONTRIBUTING.md) about how to contribute to this project.


## Contact

* [Twitter](https://twitter.com/seataio): Follow along for latest Seata news on Twitter.

* Mailing list: 
  * dev-seata@googlegroups.com , for dev/user discussion. [subscribe](mailto:dev-seata+subscribe@googlegroups.com), [unsubscribe](mailto:dev-seata+unsubscribe@googlegroups.com), [archive](https://groups.google.com/forum/#!forum/dev-seata)
  
**Dingtalk**

<img src="https://upload-images.jianshu.io/upload_images/4420767-4e95b186a1a1bfba.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240"  height="300" width="300">


## Seata ecosystem

* [Seata Ecosystem Entry](https://github.com/seata) - A GitHub group `seata` to gather all Seata relevant projects
* [Seata Samples](https://github.com/seata/seata-samples) - Samples for Seata
* [Seata Docker](https://github.com/seata/seata-docker) - Seata integration with docker
* [Seata K8s](https://github.com/seata/seata-k8s) - Seata integration with k8s
* [Awesome Seata](https://github.com/seata/awesome-seata) - Description of Seata related projects 
* [Seata Website](https://github.com/seata/seata.github.io) - Seata official website

## Contributors

This project exists thanks to all the people who contribute. [[Contributors](https://github.com/seata/seata/graphs/contributors)].

## License

Seata is under the Apache 2.0 license. See the [LICENSE](https://github.com/seata/seata/blob/master/LICENSE) file for details.

## Who is using

These are only part of the companies using Seata, for reference only. If you are using Seata, please [add your company 
here](https://github.com/seata/seata/issues/1246) to tell us your scenario to make Seata better.

![Alibaba Group](https://docs.alibabagroup.com/assets2/images/en/global/logo_header.png)
![蚂蚁金服](https://img.alicdn.com/tfs/TB1Du1couL2gK0jSZFmXXc7iXXa-250-120.jpg)
![阿里云](https://img.alicdn.com/tfs/TB1Ly5oS3HqK1RjSZFPXXcwapXa-238-54.png)
![中航信](http://www.travelsky.net/publish/main/images/logo.gif)
![滴滴](https://website.didiglobal.com/dist/media/logo-zh.a7abd90d.svg)
![浙江公安厅](https://img.alicdn.com/tfs/TB1ctCaoEY1gK0jSZFMXXaWcVXa-255-150.jpg)
![中国邮政](http://www.chinapost.com.cn/res/chinapostplan/structure/181041269.png)
![特步](https://www.xtep.com/images/logo.png)
![中通快递](https://www.zto.com/imgs/logo.png)
![浙江烟草](https://img.alicdn.com/tfs/TB1e7Wiovb2gK0jSZK9XXaEgFXa-1028-160.jpg)
![波司登](https://img.alicdn.com/tfs/TB1vVSjoAT2gK0jSZFkXXcIQFXa-314-170.jpg)
![凯京科技](https://img.alicdn.com/tfs/TB1j0dEop67gK0jSZPfXXahhFXa-400-208.jpg)
![点购集团](https://dgmall-1258058953.cos.ap-chengdu.myqcloud.com/img/logo_t.png)
![求是创新健康](http://www.truthai.cn/static/logo800.png)
![科蓝](https://img.alicdn.com/tfs/TB1DAedopY7gK0jSZKzXXaikpXa-312-142.jpg)
![康美药业](https://www.kanghehealth.com/images/logo.png)
![雁联](https://img.alicdn.com/tfs/TB1FKuioAY2gK0jSZFgXXc5OFXa-200-200.png)
![学两手](https://img.xue2shou.com/g-xue2shou/website/0.8.2/static/logo.png)
![衣二三](https://img.alicdn.com/tfs/TB1OCGioCf2gK0jSZFPXXXsopXa-500-179.jpg)
![悦途出行](http://yuetu365.com/uploads/allimg/20191016/d456dbbee0c54274a70d588af4ce6116.png)
![睿颐软件](http://ruiyicloud.com.cn/images/LOGO.png)
![赛维](http://www.savor.com.cn/common/img/logo.png)
![有利网](https://www.yooli.com/v2/local/img/common/logo.png?version=20191126190304)
![安心保险](https://query.95303.com/webins/images/logo.png)
![科达科技](https://img.alicdn.com/tfs/TB1JvOjouT2gK0jSZFvXXXnFXXa-386-146.jpg)
![会分期](https://img.alicdn.com/tfs/TB1yJyjopY7gK0jSZKzXXaikpXa-200-200.jpg)
![会找房](https://img.alicdn.com/tfs/TB1a81mouH2gK0jSZFEXXcqMpXa-200-200.jpg)
![全房通](https://img.alicdn.com/tfs/TB1Vz1loET1gK0jSZFhXXaAtVXa-200-200.jpg)
![会通教育](https://willclass.com/images/logo.png)
![享住智慧](http://image.xiangzhuzhihui.com/images/logo/logo_02.png)
![太极计算机](https://www.taiji.com.cn/TaijiCMS/r/cms/www/default/images/logo.png)
![兰亮网络](https://img.alicdn.com/tfs/TB1_miroq61gK0jSZFlXXXDKFXa-283-70.png)
![蓝天教育](https://img.alicdn.com/tfs/TB1CaSroAT2gK0jSZPcXXcKkpXa-492-176.jpg)
![政采云](https://img.alicdn.com/tfs/TB1is9ooBv0gK0jSZKbXXbK2FXa-440-158.jpg)
![烟台欣合](https://shinhoglobal.com/img/logo-shinho.svg)
![阿康健康](https://img.alicdn.com/tfs/TB1JNSqouH2gK0jSZFEXXcqMpXa-450-182.jpg)
![新脉远](https://img.alicdn.com/tfs/TB1NV1uouH2gK0jSZJnXXaT1FXa-462-172.jpg)
![乾动新能源](http://www.cangowin.com/images/logo.png)
![中国铁塔](https://www.china-tower.com/static/web/images/tower-logo.png)
![路客精品民宿](https://img.alicdn.com/tfs/TB1CCavoBr0gK0jSZFnXXbRRXXa-240-100.png)
![深圳好尔美](https://img.alicdn.com/tfs/TB1IIivoxD1gK0jSZFyXXciOVXa-200-130.png)



