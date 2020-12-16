<img src="https://github.com/seata/seata-samples/blob/master/doc/img/seata.png"  height="100" width="426">

# Seata: Simple Extensible Autonomous Transaction Architecture

[![Build Status](https://github.com/seata/seata/workflows/build/badge.svg?branch=develop)](https://github.com/seata/seata/actions)
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

Things have changed in a microservices architecture. The 3 modules mentioned above are designed to be 3 services on top of 3 different data sources ([Pattern: Database per service](http://microservices.io/patterns/data/database-per-service.html)). Data consistency within every single service is naturally guaranteed by the local transaction. 

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
3. RM registers local transaction as a branch of the corresponding global transaction of XID to TC. 
4. TM asks TC for committing or rollbacking the corresponding global transaction of XID.
5. TC drives all branch transactions under the corresponding global transaction of XID to finish branch committing or rollbacking.

![Typical Process](https://cdn.nlark.com/lark/0/2018/png/18862/1545296917881-26fabeb9-71fa-4f3e-8a7a-fc317d3389f4.png) 

For more details about principle and design, please go to [Seata wiki page](https://github.com/seata/seata/wiki). 

### History

##### Ant Financial

- **XTS**: Extended Transaction Service. Ant Financial middleware team developed the distributed transaction middleware since 2007, which is widely used in Ant Financial and solves the problems of data consistency across databases and services.

- **DTX**: Distributed Transaction Extended. Since 2013, XTS has been published on the Ant Financial Cloud, with the name of DTX .

##### Alibaba

- **TXC**: Taobao Transaction Constructor. Alibaba middleware team started this project since 2014 to meet the distributed transaction problems caused by application architecture change from monolithic to microservices.
- **GTS**: Global Transaction Service. TXC as an Aliyun middleware product with new name GTS was published since 2016.
- **Fescar**: we started the open source project Fescar based on TXC/GTS since 2019 to work closely with the community in the future.


##### Seata Community

- **Seata** :Simple Extensible Autonomous Transaction Architecture. Ant Financial joins Fescar, which make it to be a more neutral and open community for distributed transaction, and Fescar be renamed to Seata.



## Maven dependency
```xml
<seata.version>1.4.0</seata.version>

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
  

<img src="https://img.alicdn.com/tfs/TB1NvtaFrj1gK0jSZFOXXc7GpXa-1218-404.jpg"  height="200" width="630">


## Seata ecosystem

* [Seata Ecosystem Entry](https://github.com/seata) - A GitHub group `seata` to gather all Seata relevant projects
* [Seata Samples](https://github.com/seata/seata-samples) - Samples for Seata
* [Seata Docker](https://github.com/seata/seata-docker) - Seata integration with docker
* [Seata K8s](https://github.com/seata/seata-k8s) - Seata integration with k8s
* [Awesome Seata](https://github.com/seata/awesome-seata) - Seata's slides and video address in meetup
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
    <img alt='中航信' height='40'  src='https://www.travelskyir.com/img/logo.gif'  /img>
    <img alt='联通(浙江)' height='40'  src='https://img.alicdn.com/tfs/TB1hvabw9f2gK0jSZFPXXXsopXa-174-100.png'  /img>
    <img alt='中国铁塔' height='40'  src='https://www.china-tower.com/static/web/images/tower-logo.png'  /img>
    <img alt='滴滴' height='40'  src='https://website.didiglobal.com/dist/media/logo-zh.a7abd90d.svg'  /img>
    <img alt='中国邮政' height='40'  src='http://www.chinapost.com.cn/res/chinapostplan/structure/181041269.png'  /img>
    <img alt='58集团' height='40'  src='http://img.58cdn.com.cn/logo/58/252_84/logo-o.png?v=2'  /img>
    <img alt='南航' height='40'  src='https://img.alicdn.com/tfs/TB1GMQpZHY1gK0jSZTEXXXDQVXa-203-63.png'  /img>
    <img alt='TCL' height='40'  src='https://img.alicdn.com/tfs/TB1oHThw.Y1gK0jSZFCXXcwqXXa-214-200.png'  /img>
    <img alt='韵达快递' height='40'  src='http://www.yunda56.com/cn/images/ky_images/logo.png'  /img>
    <img alt='科大讯飞' height='40'  src='https://img.alicdn.com/tfs/TB1x0p5jxvbeK8jSZPfXXariXXa-272-83.png'  /img>
    <img alt='太极计算机' height='40'  src='https://img.alicdn.com/tfs/TB1.zqEoAL0gK0jSZFAXXcA9pXa-245-38.png'  /img>
    <img alt='政采云' height='40'  src='https://img.alicdn.com/tfs/TB1DDiCorY1gK0jSZTEXXXDQVXa-440-114.jpg'  /img>
    <img alt='浙江公安厅' height='40'  src='https://img.alicdn.com/tfs/TB1SXGzoxn1gK0jSZKPXXXvUXXa-426-180.jpg'  /img>
    <img alt='特步' height='40'  src='https://www.xtep.com/public/images/logo.png'  /img>
    <img alt='欧莱雅百库' height='40'  src='https://img.alicdn.com/tfs/TB1Xa3bZQL0gK0jSZFtXXXQCXXa-936-93.png'  /img> 
    <img alt='中通快递' height='40'  src='https://img.alicdn.com/tfs/TB1rCNSFxn1gK0jSZKPXXXvUXXa-172-31.png'  /img>
    <img alt='美的集团' height='40' src='https://img.alicdn.com/tfs/TB1cgvjwYj1gK0jSZFOXXc7GpXa-1040-282.png'  /img>    
    <img alt='浙江烟草' height='40'  src='https://img.alicdn.com/tfs/TB1e7Wiovb2gK0jSZK9XXaEgFXa-1028-160.jpg'  /img>
    <img alt='波司登' height='40'  src='https://img.alicdn.com/tfs/TB12cmCouL2gK0jSZFmXXc7iXXa-310-110.jpg'  /img> 
    <img alt='凯京科技' height='40'  src='https://img.alicdn.com/tfs/TB1j0dEop67gK0jSZPfXXahhFXa-400-208.jpg'  /img>
    <img alt='点购集团' height='40'  src='https://dgmall-1258058953.cos.ap-chengdu.myqcloud.com/img/logo_t.png'  /img>
    <img alt='求是创新健康' height='40'  src='http://www.truthai.cn/static/logo800.png'  /img>
    <img alt='科蓝' height='40'  src='https://img.alicdn.com/tfs/TB1tuSyouT2gK0jSZFvXXXnFXXa-304-94.jpg'  /img>
    <img alt='康美药业' height='40'  src='http://www.kangmei.com.cn/statics/images/kangmei/logo.png'  /img>
    <img alt='雁联' height='40'  src='https://img.alicdn.com/tfs/TB1c8iCouL2gK0jSZFmXXc7iXXa-428-102.jpg'  /img>
    <img alt='学两手' height='40'  src='https://img.xue2shou.com/g-xue2shou/website/0.8.2/static/logo.png'  /img>
    <img alt='衣二三' height='40'  src='https://img.alicdn.com/tfs/TB1OCGioCf2gK0jSZFPXXXsopXa-500-179.jpg'  /img>
    <img alt='北京薪福社' height='40'  src='https://img.alicdn.com/tfs/TB1Atu9ovzO3e4jSZFxXXaP_FXa-310-60.png'  /img> 
    <img alt='叩丁狼教育' height='40'  src='https://img.alicdn.com/tfs/TB1pfYTpRBh1e4jSZFhXXcC9VXa-151-72.png'  /img> 
    <img alt='悦途出行' height='40'  src='http://yuetu365.com/uploads/allimg/20191016/d456dbbee0c54274a70d588af4ce6116.png'  /img>
    <img alt='睿颐软件' height='40'  src='https://img.alicdn.com/tfs/TB143R4op67gK0jSZPfXXahhFXa-148-42.png'  /img>
    <img alt='有利网' height='40'  src='https://www.yooli.com/v2/local/img/common/logo.png?version=20191126190304'  /img>
    <img alt='赛维' height='40'  src='http://www.savor.com.cn/common/img/logo.png'  /img>
    <img alt='安心保险' height='40'  src='https://www.95303.com/img/header/logo_header.png'  /img>
    <img alt='科达科技' height='40'  src='https://img.alicdn.com/tfs/TB1JvOjouT2gK0jSZFvXXXnFXXa-386-146.jpg'  /img>
    <img alt='会分期' height='40'  src='https://img.alicdn.com/tfs/TB1ChKFoBr0gK0jSZFnXXbRRXXa-402-166.jpg'  /img>
    <img alt='会找房' height='40'  src='https://img.alicdn.com/tfs/TB1bNWFoBr0gK0jSZFnXXbRRXXa-398-336.jpg'  /img>
    <img alt='全房通' height='40'  src='https://img.alicdn.com/tfs/TB1iMSAopP7gK0jSZFjXXc5aXXa-398-182.jpg'  /img>
    <img alt='会通教育' height='40'  src='https://img.alicdn.com/tfs/TB1_D9Boxn1gK0jSZKPXXXvUXXa-580-218.jpg'  /img>
    <img alt='享住智慧' height='40'  src='http://image.xiangzhuzhihui.com/images/logo/logo_02.png'  /img>
    <img alt='兰亮网络' height='40'  src='https://img.alicdn.com/tfs/TB1_miroq61gK0jSZFlXXXDKFXa-283-70.png'  /img>
    <img alt='桔子数科' height='40'  src='https://img.alicdn.com/tfs/TB1HD.oZUY1gK0jSZFMXXaWcVXa-300-300.png'  /img> 
    <img alt='蓝天教育' height='40'  src='https://img.alicdn.com/tfs/TB1CaSroAT2gK0jSZPcXXcKkpXa-492-176.jpg'  /img>
    <img alt='烟台欣合' height='40'  src='https://shinhoglobal.com/img/logo-shinho.svg'  /img>
    <img alt='阿康健康' height='40'  src='https://img.alicdn.com/tfs/TB1JNSqouH2gK0jSZFEXXcqMpXa-450-182.jpg'  /img>
    <img alt='新脉远' height='40'  src='https://img.alicdn.com/tfs/TB1NV1uouH2gK0jSZJnXXaT1FXa-462-172.jpg'  /img>
    <img alt='乾动新能源' height='40'  src='http://www.cangowin.com/images/logo.png'  /img>
    <img alt='路客精品民宿' height='40'  src='https://img.alicdn.com/tfs/TB1CCavoBr0gK0jSZFnXXbRRXXa-240-100.png'  /img>
    <img alt='深圳好尔美' height='40'  src='https://img.alicdn.com/tfs/TB1IIivoxD1gK0jSZFyXXciOVXa-200-130.png'  /img>
    <img alt='浙大睿医' height='40'  src='https://img.alicdn.com/tfs/TB1kQThrFY7gK0jSZKzXXaikpXa-220-110.jpg'  /img>
    <img alt='深圳市云羿贸易科技有限公司' height='40'  src='https://img.alicdn.com/tfs/TB15r7dZHY1gK0jSZTEXXXDQVXa-234-233.png'  /img> 
    <img alt='居然之家' height='40'  src='https://img.alicdn.com/tfs/TB1LK6jrUT1gK0jSZFrXXcNCXXa-180-54.png'  /img>
    <img alt='深圳来电科技有限公司' height='40'  src='https://img.alicdn.com/tfs/TB1SEzM0eL2gK0jSZFmXXc7iXXa-154-45.png'  /img> 
    <img alt='臻善科技' height='40'  src='http://www.gisquest.com/static/web/img/img-1.png?v=v3'  /img>
    <img alt='中国支付通' height='40'  src='https://img.alicdn.com/tfs/TB1VGpTFET1gK0jSZFrXXcNCXXa-193-55.png'  /img>
    <img alt='众网小贷' height='40'  src='https://img.alicdn.com/tfs/TB19Y8XFEY1gK0jSZFMXXaWcVXa-160-60.png'  /img>
    <img alt='谐云科技' height='40'  src='https://img.alicdn.com/tfs/TB1V1YlrRv0gK0jSZKbXXbK2FXa-514-160.png'  /img>
    <img alt='浙江甄品' height='40'  src='https://img.alicdn.com/tfs/TB1oC2prND1gK0jSZFyXXciOVXa-246-124.jpg'  /img>
    <img alt='深圳海豚网' height='40'  src='https://img.alicdn.com/tfs/TB1defkrLb2gK0jSZK9XXaEgFXa-434-146.jpg'  /img>
    <img alt='汇通天下' height='40'  src='https://img.alicdn.com/tfs/TB1uIHmrHr1gK0jSZR0XXbP8XXa-1024-568.png'  /img>
    <img alt='九机网' height='40'  src='https://img.alicdn.com/tfs/TB1ERHlrUY1gK0jSZFMXXaWcVXa-120-60.png'  /img>
    <img alt='有好东西' height='40'  src='https://img.alicdn.com/tfs/TB1LT2lrNn1gK0jSZKPXXXvUXXa-300-300.jpg'  /img>
    <img alt='南京智慧盾' height='40'  src='https://img.alicdn.com/tfs/TB1s2LprUY1gK0jSZFCXXcwqXXa-618-148.jpg'  /img>
    <img alt='数跑科技' height='40'  src='https://img.alicdn.com/tfs/TB1qtGew7T2gK0jSZPcXXcKkpXa-294-104.png'  /img>
    <img alt='拉粉粉' height='40'  src='https://www.lafenfen.cn/img/icon03.png'  /img> 
    <img alt='汇通达' height='40'  src='https://img.alicdn.com/tfs/TB1KVJ9wWL7gK0jSZFBXXXZZpXa-145-59.png'  /img>
    <img alt='财新传媒' height='40'  src='http://file.caixin.com/file/content/images/new/logo_bottom.png'  /img>
    <img alt='易宝支付' height='40'  src='https://img.alicdn.com/tfs/TB1vWafw7T2gK0jSZFkXXcIQFXa-301-100.png'  /img>
    <img alt='维恩贝特' height='40'  src='http://www.vivebest.com/templates/crs/images/vnb_logo.png'  /img>
    <img alt='八库' height='40' src='https://img.alicdn.com/tfs/TB1hC5cwVY7gK0jSZKzXXaikpXa-318-134.png'  /img>
    <img alt='大诚若谷' height='40'  src='https://img.alicdn.com/tfs/TB1VuPhw4D1gK0jSZFyXXciOVXa-294-124.png'  /img>
    <img alt='杭州华网信息' height='40'  src='https://img.alicdn.com/tfs/TB1FFX6FqL7gK0jSZFBXXXZZpXa-288-101.png'  /img>  
    <img alt='成都数智索' height='40'  src='https://img.alicdn.com/tfs/TB1oJKiw4D1gK0jSZFyXXciOVXa-2053-377.png'  /img>  
    <img alt='北京超图' height='40'  src='https://img.alicdn.com/tfs/TB1eKFXFEz1gK0jSZLeXXb9kVXa-163-54.png'  /img>
    <img alt='深圳易佰' height='40'  src='https://img.alicdn.com/tfs/TB1gkXaFrr1gK0jSZR0XXbP8XXa-187-57.png'  /img>
    <img alt='江西群享科技有限公司' height='40'  src='https://img.alicdn.com/tfs/TB1Qcd0p79l0K4jSZFKXXXFjpXa-372-125.png'  /img> 
    <img alt='宋城独木桥网络有限公司' height='40'  src='https://img.alicdn.com/tfs/TB1UKocmPMZ7e4jSZFOXXX7epXa-234-82.png'  /img> 
    <img alt='唯小宝（江苏）网络技术有限公司' height='40'  src='https://img.alicdn.com/tfs/TB1eswAZFP7gK0jSZFjXXc5aXXa-800-800.png'  /img> 
    <img alt='宿迁民丰农商银行' height='40'  src='https://img.alicdn.com/tfs/TB1bH5fw7L0gK0jSZFAXXcA9pXa-442-39.png'  /img>
    <img alt='杭州喜团科技' height='40'  src='https://img.alicdn.com/tfs/TB1IXqgwYj1gK0jSZFuXXcrHpXa-197-58.png'  /img>
    <img alt='上海海智在线' height='40' src='https://img.alicdn.com/tfs/TB1xAJUFy_1gK0jSZFqXXcpaXXa-320-80.jpg'  /img>
    <img alt='丞家（上海）公寓管理' height='40'  src='https://image.cjia.com/website/apartment/webresource/image/logo_8f2f47fe.png'  /img>
    <img alt='安徽国科新材科' height='40'  src='https://img.alicdn.com/tfs/TB1ICJfFuH2gK0jSZJnXXaT1FXa-654-232.png'  /img>
    <img alt='易点生活' height='40'  src='https://img.alicdn.com/tfs/TB1AdI5FeL2gK0jSZPhXXahvXXa-1518-542.jpg'  /img>
    <img alt='商银信支付' height='40' src='https://img.alicdn.com/tfs/TB1rxndw4n1gK0jSZKPXXXvUXXa-150-68.png'   /img>
    <img alt='钛师傅云' height='40'  src='https://www.tsfyun.com/images/logo.png'  /img>
    <img alt='广州力生信息' height='40'  src='https://img.alicdn.com/tfs/TB1m0FcFuH2gK0jSZFEXXcqMpXa-139-48.png'  /img>
    <img alt='杭州启舰科技有限公司' height='40'  src='http://www.qijian-tech.com/img/logo.33134af4.png'  /img>
    <img alt='上海美浮特' height='40'  src='https://img.alicdn.com/tfs/TB1uUtaFuT2gK0jSZFvXXXnFXXa-370-45.jpg'  /img>    
    <img alt='杭州中威慧云医疗科技有限公司' height='40'  src='https://img.alicdn.com/tfs/TB1iqo_FaL7gK0jSZFBXXXZZpXa-361-54.jpg'  /img> 
    <img alt='微链' height='40'  src='https://img.alicdn.com/tfs/TB14LhHmMgP7K4jSZFqXXamhVXa-300-135.png'  /img> 
    <img alt='海典软件' height='40'  src='https://img.alicdn.com/tfs/TB1KmosZNv1gK0jSZFFXXb0sXXa-247-61.png'  /img> 
    <img alt='易族智汇（北京）' height='40'  src='http://www.javamall.com.cn/images/logonew.jpg'  /img> 
    <img alt='佛山宅无限' height='40'  src='https://zwxnetwork.oss-cn-shenzhen.aliyuncs.com/static/temporary_official_website/logo.png'  /img>     
    <img alt='F5未来商店' height='40'  src='https://cdn.f5-store.cn/front_end/common_images/logo.png'  /img>  
    <img alt='甄品信息科技' height='40'  src='https://img.alicdn.com/tfs/TB1SxJWFEY1gK0jSZFCXXcwqXXa-185-65.png'  /img>  
    <img alt='行云全球汇跨境电商（杭州分部）' height='40'  src='http://www.xyb2b.com/_nuxt/img/5e5584f.png'  /img>  
    <img alt='世纪加华' height='40'  src='https://zhengxin-pub.bj.bcebos.com/logopic/a4ff4990e2ba2d57c90e8d16c649b952_fullsize.jpg?x-bce-process=image/resize,m_lfit,w_200'  /img>     
    <img alt='快陪练' height='40'  src='https://img.alicdn.com/tfs/TB1rhNRFAL0gK0jSZFtXXXQCXXa-321-96.png'  /img> 
    <img alt='西南石油大学' height='40'  src='https://dss2.bdstatic.com/6Ot1bjeh1BF3odCf/it/u=829617221,290823158&fm=74&app=80&f=JPEG&size=f121,121?sec=1880279984&t=b0b603710dd0af061a278d11cfe327ae'  /img> 
    <img alt='厦门服云信息科技有限公司' height='40'  src='https://img.alicdn.com/tfs/TB1zuAzZKL2gK0jSZFmXXc7iXXa-691-263.png'  /img> 
    <img alt='领课网络' height='40'  src='https://img.alicdn.com/tfs/TB18TNRFEz1gK0jSZLeXXb9kVXa-244-60.jpg'  /img> 
    <img alt='美通社' height='40'  src='https://img.alicdn.com/tfs/TB1i1JTFCf2gK0jSZFPXXXsopXa-151-60.png'  /img> 
    <img alt='睿维科技有限公司' height='40'  src='https://img.alicdn.com/tfs/TB1ztXXFpY7gK0jSZKzXXaikpXa-179-60.png'  /img> 
    <img alt='郑州信源信息技术' height='40'  src='https://img.alicdn.com/tfs/TB1SkJ9FuT2gK0jSZFvXXXnFXXa-266-56.png'  /img>     
    <img alt='荣怀集团' height='40'  src='https://img.alicdn.com/tfs/TB1AzbWgZKfxu4jSZPfXXb3dXXa-1117-382.png'  /img>  
    <img alt='浙江群集大数据科技有限公司' height='40'  src='https://img.alicdn.com/tfs/TB1HtFZFq61gK0jSZFlXXXDKFXa-1375-214.png'  /img>  
    <img alt='北京易点租有限公司' height='40'  src='https://img.alicdn.com/tfs/TB1nax.FuH2gK0jSZFEXXcqMpXa-336-154.png'  /img>  
    <img alt='浙江蕙康科技有限公司' height='40'  src='https://img.alicdn.com/tfs/TB1nS7IZNv1gK0jSZFFXXb0sXXa-716-193.png'  /img>  
    <img alt='致远创想' height='40'  src='https://img.alicdn.com/tfs/TB13aaKpA9l0K4jSZFKXXXFjpXa-300-300.png'  /img> 
    <img alt='深圳智荟物联技术有限公司' height='40'  src='https://img.alicdn.com/tfs/TB1To3amPMZ7e4jSZFOXXX7epXa-1228-500.png'  /img> 
    <img alt='源讯中国' height='40'  src='https://img.alicdn.com/tfs/TB1CZuKpA9l0K4jSZFKXXXFjpXa-283-92.png'  /img> 
    <img alt='武汉江寓生活服务有限公司' height='40'  src='https://img.alicdn.com/tfs/TB1E4slZFT7gK0jSZFpXXaTkpXa-268-268.png'  /img> 
    <img alt='大账房' height='40'  src='https://img.alicdn.com/tfs/TB1.sIyZKL2gK0jSZFmXXc7iXXa-121-121.png'  /img> 
    <img alt='上海阳光喔教育科技有限公司' height='40'  src='https://img.alicdn.com/tfs/TB1aUUcZHY1gK0jSZTEXXXDQVXa-246-72.png'  /img> 
    <img alt='北京新学道教育科技有限公司' height='40'  src='https://img.alicdn.com/tfs/TB1v3.gZLb2gK0jSZK9XXaEgFXa-240-240.png'  /img> 
    <img alt='北京悦途出行网络科技公司' height='40'  src='https://img.alicdn.com/tfs/TB1VHkrZHr1gK0jSZFDXXb9yVXa-248-80.png'  /img> 
    <img alt='国信易企签' height='40'  src='https://img.alicdn.com/tfs/TB1UTwmZFT7gK0jSZFpXXaTkpXa-201-85.png'  /img>  
    <img alt='上海意贝斯特信息技术有限公司' height='40'  src='https://img.alicdn.com/tfs/TB1kGElZUH1gK0jSZSyXXXtlpXa-126-48.png'  /img> 
    <img alt='御家汇' height='40'  src='https://img.alicdn.com/tfs/TB1kIIqZUY1gK0jSZFMXXaWcVXa-90-80.png'  /img> 
    <img alt='广州社众软件' height='40'  src='https://img.alicdn.com/tfs/TB1CawkZND1gK0jSZFsXXbldVXa-112-112.png'  /img> 
    <img alt='浩鲸科技' height='40'  src='https://img.alicdn.com/tfs/TB1fxZqZQL0gK0jSZFAXXcA9pXa-300-300.png'  /img> 
    <img alt='华宇信息' height='40'  src='https://img.alicdn.com/tfs/TB1q3UiZKL2gK0jSZPhXXahvXXa-802-271.png'  /img> 
    <img alt='中国云尚科技' height='40'  src='https://img.alicdn.com/tfs/TB1uf7bZQL0gK0jSZFtXXXQCXXa-303-65.png'  /img> 
    <img alt='卫宁健康' height='40'  src='https://img.alicdn.com/tfs/TB1WMgmZUY1gK0jSZFCXXcwqXXa-189-57.png'  /img> 
    <img alt='聚合联动' height='40'  src='https://img.alicdn.com/tfs/TB1gnllpnM11u4jSZPxXXahcXXa-150-60.png'  /img> 
    <img alt='熙菱信息' height='40'  src='https://img.alicdn.com/tfs/TB1NJmLpA9l0K4jSZFKXXXFjpXa-195-60.png'  /img> 
    <img alt='鲸算科技' height='40'  src='https://img.alicdn.com/tfs/TB1jfCLpA9l0K4jSZFKXXXFjpXa-514-220.png'  /img> 
    <img alt='杭州沃朴物联科技有限公司' height='40'  src='https://img.alicdn.com/tfs/TB1vxJ.ZVT7gK0jSZFpXXaTkpXa-309-51.png'  /img> 
    <img alt='深圳市臻络科技有限公司' height='40' width='240' src='https://img.alicdn.com/tfs/TB1v5eiZ.T1gK0jSZFrXXcNCXXa-500-41.png'  /img> 
</div>







