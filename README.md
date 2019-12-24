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
<seata.version>1.0.0</seata.version>

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

<div style='vertical-align: middle'>
    <img alt='Alibaba Group' height='40'  src='https://docs.alibabagroup.com/assets2/images/en/global/logo_header.png'  /img>
    <img alt='蚂蚁金服' height='40'  src='https://img.alicdn.com/tfs/TB1wuuCoET1gK0jSZFhXXaAtVXa-496-202.jpg'  /img>
    <img alt='阿里云' height='40'  src='https://img.alicdn.com/tfs/TB1Ly5oS3HqK1RjSZFPXXcwapXa-238-54.png'  /img>
    <img alt='中航信' height='40'  src='http://www.travelsky.net/publish/main/images/logo.gif'  /img>
    <img alt='中国铁塔' height='40'  src='https://www.china-tower.com/static/web/images/tower-logo.png'  /img>
    <img alt='滴滴' height='40'  src='https://website.didiglobal.com/dist/media/logo-zh.a7abd90d.svg'  /img>
    <img alt='中国邮政' height='40'  src='http://www.chinapost.com.cn/res/chinapostplan/structure/181041269.png'  /img>
    <img alt='太极计算机' height='40'  src='https://img.alicdn.com/tfs/TB1.zqEoAL0gK0jSZFAXXcA9pXa-245-38.png'  /img>
    <img alt='政采云' height='40'  src='https://img.alicdn.com/tfs/TB1DDiCorY1gK0jSZTEXXXDQVXa-440-114.jpg'  /img>
    <img alt='浙江公安厅' height='40'  src='https://img.alicdn.com/tfs/TB1SXGzoxn1gK0jSZKPXXXvUXXa-426-180.jpg'  /img>
    <img alt='特步' height='40'  src='https://www.xtep.com/images/logo.png'  /img>
    <img alt='中通快递' height='40'  src='https://www.zto.com/imgs/logo.png'  /img>
    <img alt='浙江烟草' height='40'  src='https://img.alicdn.com/tfs/TB1e7Wiovb2gK0jSZK9XXaEgFXa-1028-160.jpg'  /img>
    <img alt='波司登' height='40'  src='https://img.alicdn.com/tfs/TB12cmCouL2gK0jSZFmXXc7iXXa-310-110.jpg'  /img>
    <img alt='凯京科技' height='40'  src='https://img.alicdn.com/tfs/TB1j0dEop67gK0jSZPfXXahhFXa-400-208.jpg'  /img>
    <img alt='点购集团' height='40'  src='https://dgmall-1258058953.cos.ap-chengdu.myqcloud.com/img/logo_t.png'  /img>
    <img alt='求是创新健康' height='40'  src='http://www.truthai.cn/static/logo800.png'  /img>
    <img alt='科蓝' height='40'  src='https://img.alicdn.com/tfs/TB1tuSyouT2gK0jSZFvXXXnFXXa-304-94.jpg'  /img>
    <img alt='康美药业' height='40'  src='https://www.kanghehealth.com/images/logo.png'  /img>
    <img alt='雁联' height='40'  src='https://img.alicdn.com/tfs/TB1c8iCouL2gK0jSZFmXXc7iXXa-428-102.jpg'  /img>
    <img alt='学两手' height='40'  src='https://img.xue2shou.com/g-xue2shou/website/0.8.2/static/logo.png'  /img>
    <img alt='衣二三' height='40'  src='https://img.alicdn.com/tfs/TB1OCGioCf2gK0jSZFPXXXsopXa-500-179.jpg'  /img>
    <img alt='悦途出行' height='40'  src='http://yuetu365.com/uploads/allimg/20191016/d456dbbee0c54274a70d588af4ce6116.png'  /img>
    <img alt='睿颐软件' height='40'  src='https://img.alicdn.com/tfs/TB143R4op67gK0jSZPfXXahhFXa-148-42.png'  /img>
    <img alt='赛维' height='40'  src='http://www.savor.com.cn/common/img/logo.png'  /img>
    <img alt='有利网' height='40'  src='https://www.yooli.com/v2/local/img/common/logo.png?version=20191126190304'  /img>
    <img alt='安心保险' height='40'  src='https://query.95303.com/webins/images/logo.png'  /img>
    <img alt='科达科技' height='40'  src='https://img.alicdn.com/tfs/TB1JvOjouT2gK0jSZFvXXXnFXXa-386-146.jpg'  /img>
    <img alt='会分期' height='40'  src='https://img.alicdn.com/tfs/TB1ChKFoBr0gK0jSZFnXXbRRXXa-402-166.jpg'  /img>
    <img alt='会找房' height='40'  src='https://img.alicdn.com/tfs/TB1bNWFoBr0gK0jSZFnXXbRRXXa-398-336.jpg'  /img>
    <img alt='全房通' height='40'  src='https://img.alicdn.com/tfs/TB1iMSAopP7gK0jSZFjXXc5aXXa-398-182.jpg'  /img>
    <img alt='会通教育' height='40'  src='https://img.alicdn.com/tfs/TB1_D9Boxn1gK0jSZKPXXXvUXXa-580-218.jpg'  /img>
    <img alt='享住智慧' height='40'  src='http://image.xiangzhuzhihui.com/images/logo/logo_02.png'  /img>
    <img alt='兰亮网络' height='40'  src='https://img.alicdn.com/tfs/TB1_miroq61gK0jSZFlXXXDKFXa-283-70.png'  /img>
    <img alt='蓝天教育' height='40'  src='https://img.alicdn.com/tfs/TB1CaSroAT2gK0jSZPcXXcKkpXa-492-176.jpg'  /img>
    <img alt='烟台欣合' height='40'  src='https://shinhoglobal.com/img/logo-shinho.svg'  /img>
    <img alt='阿康健康' height='40'  src='https://img.alicdn.com/tfs/TB1JNSqouH2gK0jSZFEXXcqMpXa-450-182.jpg'  /img>
    <img alt='新脉远' height='40'  src='https://img.alicdn.com/tfs/TB1NV1uouH2gK0jSZJnXXaT1FXa-462-172.jpg'  /img>
    <img alt='乾动新能源' height='40'  src='http://www.cangowin.com/images/logo.png'  /img>
    <img alt='路客精品民宿' height='40'  src='https://img.alicdn.com/tfs/TB1CCavoBr0gK0jSZFnXXbRRXXa-240-100.png'  /img>
    <img alt='深圳好尔美' height='40'  src='https://img.alicdn.com/tfs/TB1IIivoxD1gK0jSZFyXXciOVXa-200-130.png'  /img>
</div>


