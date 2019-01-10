FESCAR（Fast & Easy Commit And Rollback） 是一个用于微服务架构的分布式事务解决方案，它的特点是高性能且易于使用，旨在实现简单并快速的事务提交与回滚。

**微服务架构中的分布式事务问题**

从传统的单体应用说起，假设一个单体应用的业务由 3 个模块构成，三者使用单个本地数据源。

![Monolithic App](https://camo.githubusercontent.com/400c70938b835e0b8ecc70ca61b3504e2aba891e/68747470733a2f2f63646e2e6e6c61726b2e636f6d2f6c61726b2f302f323031382f706e672f31383836322f313534353239363737303234342d34636564663337652d396463362d346663302d613937662d6634323430623964383634302e706e67)

这样的话本地事务很自然就可以保证数据一致性。

但是在微服务架构中就不这么简单了，这 3 个模块被设计为 3 个不同数据源之上的 3 个服务，每个服务对应一个数据库。

本地事务当然也可以保证每个服务中的数据一致性，但是扩展到整个应用、整个业务逻辑范围来看，情况如何呢？

![Microservices Problem](https://camo.githubusercontent.com/5729bd180e2c5b4a54f93c9d0a40dc06117e8565/68747470733a2f2f63646e2e6e6c61726b2e636f6d2f6c61726b2f302f323031382f706e672f31383836322f313534353239363738313233312d34303239646139632d383830332d343361342d616332662d3663386231653265613434382e706e67)

**FESCAR 机制**

FESCAR 就是用于解决上述微服务架构中的事务问题的解决方案。

![FESCAR solution](https://camo.githubusercontent.com/b3a71332ae0a91db7f8616286a69b879fcbea672/68747470733a2f2f63646e2e6e6c61726b2e636f6d2f6c61726b2f302f323031382f706e672f31383836322f313534353239363739313037342d33626365376263652d303235652d343563332d393338362d3762393531333564616465382e706e67)

如下图所示，分布式事务是一个全局事务（Global Transaction），由一批分支事务（Branch Transation）组成，通常分支事务只是本地事务。

![Global & Branch](https://camo.githubusercontent.com/9d741875c9d7f99887fc4075b9fd4a4b67b69ade/68747470733a2f2f63646e2e6e6c61726b2e636f6d2f6c61726b2f302f323031382f706e672f31383836322f313534353031353435343937392d61313865313666362d656434312d343466312d396337612d6264383263346435666639392e706e67)

FESCAR 中有三大基本组件：

- **Transaction Coordinator(TC)：**维护全局和分支事务的状态，驱动全局事务提交与回滚。

- **Transaction Manager(TM)：**定义全局事务的范围：开始、提交或回滚全局事务。

- **Resource Manager(RM)：**管理分支事务处理的资源，与 TC 通信以注册分支事务并报告分支事务的状态，并驱动分支事务提交或回滚。

![Model](https://camo.githubusercontent.com/e8fd6ca40ba959962a537cbc1be58013486cf46c/68747470733a2f2f63646e2e6e6c61726b2e636f6d2f6c61726b2f302f323031382f706e672f31383836322f313534353031333931353238362d34613930663064662d356664612d343165312d393165302d3261613364333331633033352e706e67)

FESCAR 管理分布式事务的典型生命周期：

1. TM 要求 TC 开始新的全局事务，TC 生成表示全局事务的 XID。

2. XID 通过微服务的调用链传播。

3. RM 在 TC 中将本地事务注册为 XID 的相应全局事务的分支。

4. TM 要求 TC 提交或回滚 XID 的相应全局事务。

5. TC 驱动 XID 的相应全局事务下的所有分支事务，完成分支提交或回滚。

![Typical Process](https://camo.githubusercontent.com/0384806afd7c10544c258ae13717e4229942aa13/68747470733a2f2f63646e2e6e6c61726b2e636f6d2f6c61726b2f302f323031382f706e672f31383836322f313534353239363931373838312d32366661626562392d373166612d346633652d386137612d6663333137643333383966342e706e67)

**演进历史**

- **TXC：**Taobao Transaction Constructor，阿里巴巴中间件团队自 2014 年起启动该项目，以满足应用程序架构从单一服务变为微服务所导致的分布式事务问题。

- **GTS：**Global Transaction Service，2016 年 TXC 作为阿里中间件的产品，更名为 GTS 发布。

- **FESCAR：**2019 年开始基于 TXC/GTS 开源 FESCAR。

## Quick Start

[Quick Start](https://github.com/alibaba/fescar/wiki/Quick-Start)

## 文档

完整文档：[FESCAR wiki page](https://github.com/alibaba/fescar/wiki)

## bug 报告

请按照[模板](http://github.com/TBD)提交 issue。

## Contributing

欢迎参与开源贡献，查看 [CONTRIBUTING](https://github.com/alibaba/fescar/blob/master/CONTRIBUTING.md) 文档了解参与方式。

## Contact

- [Gitter](https://gitter.im/alibaba/fescar)：TBD。FESCAR 用于社区消息传播、协作和发掘的 IM 工具。
- [Twitter](https://twitter.com/fescar)：TBD。
- [Weibo](https://weibo.com/u/xxxxxxx)：TBD。
- [Segmentfault](https://segmentfault.com/t/fescar)：TBD。
- Email Group：
  - TBD：FESCAR 使用讨论区。
  - TBD：FESCAR 开发者讨论区（API、特性等方面）。
  - TBD：Commit 通知，频率非常高。

**钉钉**

[](https://camo.githubusercontent.com/ab17303aee47ec0ee165f1831d293467cb2bedda/68747470733a2f2f75706c6f61642d696d616765732e6a69616e7368752e696f2f75706c6f61645f696d616765732f343432303736372d346539356231383661316131626662612e706e673f696d6167654d6f6772322f6175746f2d6f7269656e742f7374726970253743696d61676556696577322f322f772f31323430)![dingding.png](https://camo.githubusercontent.com/ab17303aee47ec0ee165f1831d293467cb2bedda/68747470733a2f2f75706c6f61642d696d616765732e6a69616e7368752e696f2f75706c6f61645f696d616765732f343432303736372d346539356231383661316131626662612e706e673f696d6167654d6f6772322f6175746f2d6f7269656e742f7374726970253743696d61676556696577322f322f772f31323430)[](https://camo.githubusercontent.com/ab17303aee47ec0ee165f1831d293467cb2bedda/68747470733a2f2f75706c6f61642d696d616765732e6a69616e7368752e696f2f75706c6f61645f696d616765732f343432303736372d346539356231383661316131626662612e706e673f696d6167654d6f6772322f6175746f2d6f7269656e742f7374726970253743696d61676556696577322f322f772f31323430)

## License

FESCAR 以 Apache 2.0 license 开源，详情查看 [LICENSE](https://github.com/alibaba/fescar/blob/master/LICENSE) 文档。
