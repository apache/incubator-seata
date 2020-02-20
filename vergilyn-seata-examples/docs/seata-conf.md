# SEATA-conf
[seata参数配置 1.0.0版本](https://seata.io/zh-cn/docs/user/configurations.html)

以SEATA源码中的配置作为参考：
- [ParameterParser.java](../../server/src/main/java/io/seata/server/ParameterParser.java)
- [registry.conf.example](../../server/src/main/resources/registry.conf.example)
- [file.conf.example](../../server/src/main/resources/file.conf.example)

总结：
`register.conf`和`file.conf`中的配置项，有些是seata-server独有，有些是seata-client独有，
有些可能是2边都需要配置但用途不一样。

2020-02-16 >>>>
`register.conf`和`file.conf`在 seata-server 和 seata-client 都需要配置。
但是，其中具体的配置不一定都需要。
例如，在seata-server需要配置 `register.conf`的`registry`。但是对于seata-client，这个配置完全无用。

## 1. seata启动参数
源码：
- [ParameterParser.java](../../server/src/main/java/io/seata/server/ParameterParser.java)

```CMD
--host, -h              The ip to register to registry center.
--port, -p              The port to listen(default: 8091).
--storeMode, -m         log store mode : file, db
--serverNode, -n        server node id, such as 1, 2, 3. default is 1
--seataEnv, -e          The name used for multi-configuration isolation
```

比较常用到的`port`，用于修改seata-server的端口。

## 2. register.conf(seata-server, seata-client)
- [registry.conf.example](../../server/src/main/resources/registry.conf.example)

seata-server 和 seata-client 都可以配置该文件。

### 2.1 `registry`(seata-server, seata-client)
**seata-server 和 seata-client 都需要配置该项。**  
2020-02-18 >>>>  
坑！神坑！无比坑！  
最初在写examples时，以为seata-client不需要`registry`配置，导致逻辑问题：全局事务不会回滚（程序不会报任何错，因为其实存在默认配置）
更坑的！其实是配置文件解析顺序问题，当引入`seata-spring-boot-starter`时要特别小心！！！
[github issues#2265](https://github.com/seata/seata/issues/2265)

2020-02-19 >>>>
通过设置`seata.enable = false`可以禁用spring-boot的auto-configuration

seata-server:  
表明将seata-server注册到哪个注册中心。
seata源码参考：`io.seata.discovery.registry.nacos.NacosRegistryServiceImpl#register()`

seata-client:  
表示seata-client需要从哪个注册中心，去获取需要的seata-server的信息。
seata源码参考：`io.seata.discovery.registry.nacos.NacosRegistryServiceImpl#lookup()`
（通过源码可知，seata-client中暂时用不到 registry.nacos.cluster）

```hocon
registry {
  # file 、nacos 、eureka、redis、zk、consul、etcd3、sofa
  type = "file"

  nacos {
    serverAddr = "localhost"
    namespace = ""
    cluster = "default"
  }
  eureka {
    serviceUrl = "http://localhost:8761/eureka"
    application = "default"
    weight = "1"
  }
  redis {
    serverAddr = "localhost:6379"
    db = "0"
  }
  zk {
    cluster = "default"
    serverAddr = "127.0.0.1:2181"
    session.timeout = 6000
    connect.timeout = 2000
  }
  consul {
    cluster = "default"
    serverAddr = "127.0.0.1:8500"
  }
  etcd3 {
    cluster = "default"
    serverAddr = "http://localhost:2379"
  }
  sofa {
    serverAddr = "127.0.0.1:9603"
    application = "default"
    region = "DEFAULT_ZONE"
    datacenter = "DefaultDataCenter"
    cluster = "default"
    group = "SEATA_GROUP"
    addressWaitTime = "3000"
  }
  file {
    name = "file.conf"
  }
}
```

虽然启动参数中提供了`--host`，但个人觉得一般都还是通过在`register.conf`中指定seata-server注册到哪个服务注册中心。

1. `register.type`  
seata-server提供了多种注册方式，其对应的源码参考：[RegistryProvider.java](../../discovery/seata-discovery-core/src/main/java/io/seata/discovery/registry/RegistryProvider.java)

```
interface io.seata.discovery.registry.RegistryProvider
    implements, io.seata.discovery.registry.consul.ConsulRegistryProvider    , registry.type = "Consul"
    implements, io.seata.discovery.registry.custom.CustomRegistryProvider    , registry.type = "Custom"
    implements, io.seata.discovery.registry.etcd3.EtcdRegistryProvider       , registry.type = "Etcd3"
    implements, io.seata.discovery.registry.eureka.EurekaRegistryProvider    , registry.type = "Eureka"
    implements, io.seata.discovery.registry.nacos.NacosRegistryProvider      , registry.type = "Nacos"
    implements, io.seata.discovery.registry.redis.RedisRegistryProvider      , registry.type = "Redis"
    implements, io.seata.discovery.registry.sofa.SofaRegistryProvider        , registry.type = "Sofa"
    implements, io.seata.discovery.registry.zk.ZookeeperRegistryProvider     , registry.type = "ZK"
    
interface io.seata.discovery.registry.RegistryService<T>
    implements, io.seata.discovery.registry.consul.ConsulRegistryServiceImpl
    implements, io.seata.discovery.registry.etcd3.EtcdRegistryServiceImpl
    implements, io.seata.discovery.registry.eureka.EurekaRegistryServiceImpl
    implements, io.seata.discovery.registry.FileRegistryServiceImpl
    implements, io.seata.discovery.registry.nacos.NacosRegistryServiceImpl
    implements, io.seata.discovery.registry.redis.RedisRegistryServiceImpl
    implements, io.seata.discovery.registry.sofa.SofaRegistryServiceImpl
    implements, io.seata.discovery.registry.zk.ZookeeperRegisterServiceImpl
```

备注：如果`register.type = custom`，需要自己实现接口`RegistryService<T>`。

在seata-server启动时，通过`io.seata.core.rpc.netty.AbstractRpcRemotingServer#start() --内部调用--> io.seata.discovery.registry.RegistryService#register(...)`
将seata-server注册到相应的服务注册中心。

### 2.2 `config`(seata-server)
seata-server 和 seata-client 都需要配置该配置项。

seata-server: 指明seata-server需要从哪里读取seata-server需要的配置。  
例如 事务日志的存储模式、是否启用metrics、一些定时任务的参数等。

seata-client: 指明seata-client需要从哪里去读取关于seata的一些相关配置项。  
例如seata-server的ip和port，连接seata-server的连接参数等。  
（类似在项目中配置mysql的信息。这个可能没表述清楚，表达能力有限，先意会）。

```HOCON
config {
  # file、nacos 、apollo、zk、consul、etcd3
  type = "file"

  nacos {
    serverAddr = "localhost"
    namespace = ""
  }
  consul {
    serverAddr = "127.0.0.1:8500"
  }
  apollo {
    app.id = "seata-server"
    apollo.meta = "http://192.168.1.204:8801"
  }
  zk {
    serverAddr = "127.0.0.1:2181"
    session.timeout = 6000
    connect.timeout = 2000
  }
  etcd3 {
    serverAddr = "http://localhost:2379"
  }
  file {
    name = "file.conf"
  }
}
```

1. `config.type`
指定seata-server从哪里获取配置（seata-server需要的配置）。
```
interface io.seata.config.ConfigurationProvider
    implements, io.seata.config.apollo.ApolloConfigurationProvider,             config.type = "Apollo"
    implements, io.seata.config.consul.ConsulConfigurationProvider,             config.type = "Consul"
    implements, io.seata.config.custom.CustomConfigurationProvider,             config.type = "Custom"
    implements, io.seata.config.etcd3.EtcdConfigurationProvider,                config.type = "Etcd3"
    implements, io.seata.config.nacos.NacosConfigurationProvider,               config.type = "Nacos"
    implements, io.seata.config.springcloud.SpringCloudConfigurationProvider,   config.type = "SpringCloudConfig"
    implements, io.seata.config.zk.ZookeeperConfigurationProvider,              config.type = "ZK"
```

在seata-server启动时，通过`io.seata.config.ConfigurationFactory#buildConfiguration()`
判断根据不同的`register.type`调用不同的`ConfigurationProvider`实现，需要获取配置时再调用具体的实现。

## 3. file.conf(seata-server, seata-client)

`NettyBaseConfig`其子类分为：`NettyClientConfig`、`NettyServerConfig`。

### 3.1 `transport`(seata-server, seata-client)
```HOCON
transport {
  # tcp udt unix-domain-socket
  type = "TCP"
  #NIO NATIVE
  server = "NIO"
  #enable heartbeat
  heartbeat = true
  #thread factory for netty
  thread-factory {
    boss-thread-prefix = "NettyBoss"
    worker-thread-prefix = "NettyServerNIOWorker"
    server-executor-thread-prefix = "NettyServerBizHandler"
    share-boss-worker = false
    client-selector-thread-prefix = "NettyClientSelector"
    client-selector-thread-size = 1
    client-worker-thread-prefix = "NettyClientWorkerThread"
    # netty boss thread size,will not be used for UDT
    boss-thread-size = 1
    #auto default pin or 8
    worker-thread-size = 8
  }
  shutdown {
    # when destroy server, wait seconds
    wait = 3
  }
  serialization = "seata"
  compressor = "none"
}
```

其中部分参数需要了解以下类才能理解其含义：
- netty的很多相关知识！！！！
- io.netty.channel.nio.NioEventLoopGroup.NioEventLoopGroup
- io.netty.channel.epoll.EpollEventLoopGroup
- io.netty.util.concurrent.DefaultEventExecutorGroup.DefaultEventExecutorGroup
- java.util.concurrent.ThreadPoolExecutor

netty在seata中的相关应用源码：
- io.seata.core.rpc.netty.RpcClientBootstrap
- io.seata.core.rpc.netty.AbstractRpcRemotingServer

1. `transport.thread-factory.share-boss-worker`  
暂时未发现其相关实现。


#### 3.1.1 seata-server
（精简过后，个人认为seata-server只需要配置的transport的配置项）
```HOCON
transport {
  # tcp udt unix-domain-socket
  type = "TCP"
  #NIO NATIVE
  server = "NIO"
  #enable heartbeat
  heartbeat = true
  #thread factory for netty
  thread-factory {
    boss-thread-prefix = "NettyBoss"
    worker-thread-prefix = "NettyServerNIOWorker"
    server-executor-thread-prefix = "NettyServerBizHandler"
    # netty boss thread size,will not be used for UDT
    boss-thread-size = 1
    #auto default pin or 8
    worker-thread-size = 8
  }
  shutdown {
    # when destroy server, wait seconds
    wait = 3
  }
  serialization = "seata"
  compressor = "none"
}
```
1. `server-executor-thread-prefix`
暂未发现源码中有相关的用途。

2. 代码入口
`io.seata.server.Server#main(...)`

3. `transport.shutdown.wait`
```JAVA
package io.seata.core.rpc.netty;

public abstract class AbstractRpcRemotingServer extends AbstractRpcRemoting implements RemotingServer {
    private final ServerBootstrap serverBootstrap;

     @Override
    public void shutdown() {
        try {
            if (initialized.get()) {
                RegistryFactory.getInstance().unregister(new InetSocketAddress(XID.getIpAddress(), XID.getPort()));
                RegistryFactory.getInstance().close();

                /* vergilyn-question, 2020-02-16 >>>> 为什么要Thread.sleep一会？
                 *   个人猜测，是为了确保seata-server已经从registry-center中移除，registry-center不会再发送相关请求到当前（准备）关闭的seata-server。
                 */                //wait a few seconds for server transport
                TimeUnit.SECONDS.sleep(nettyServerConfig.getServerShutdownWaitTime());
            }

            /* vergilyn-comment, 2020-02-17 >>>> 优雅的关闭netty
             *   不管是`EpollEventLoopGroup`还是`NioEventLoopGroup`其默认参数都是(2, 15, SECONDS)
             *   表示，Netty默认在2秒的静默时间内如果没有任务，则关闭；否则15秒截止时间到达时关闭。
             */
            this.eventLoopGroupBoss.shutdownGracefully();
            this.eventLoopGroupWorker.shutdownGracefully();
        } catch (Exception exx) {
            LOGGER.error(exx.getMessage());
        }
    }
}

```

4. 代码跟踪示例
例如`boss-thread-size` (备注 netty多线程模型之Reactor)
```JAVA
this.eventLoopGroupBoss = new io.netty.channel.epoll.EpollEventLoopGroup(nettyServerConfig.getBossThreadSize(),
    new NamedThreadFactory(nettyServerConfig.getBossThreadPrefix(), nettyServerConfig.getBossThreadSize()));

this.eventLoopGroupWorker = new io.netty.channel.epoll.EpollEventLoopGroup(nettyServerConfig.getServerWorkerThreads(),
    new NamedThreadFactory(nettyServerConfig.getWorkerThreadPrefix(),
        nettyServerConfig.getServerWorkerThreads()));
```

#### 3.1.2 seata-client
（精简过后，个人认为seata-client只需要配置的transport的配置项）
```HOCON
transport {
  # tcp udt unix-domain-socket
  type = "TCP"
  #NIO NATIVE
  server = "NIO"
  #enable heartbeat
  heartbeat = true
  #thread factory for netty
  thread-factory {
    client-selector-thread-prefix = "NettyClientSelector"
    client-selector-thread-size = 1
    client-worker-thread-prefix = "NettyClientWorkerThread"
    #auto default pin or 8
    worker-thread-size = 8
  }
  serialization = "seata"
  compressor = "none"
  
  # seata-client是否支持批量发送请求到seata-server
  enable-client-batch-send-request = true
}
```

1. 方便查看配置项的方法，在代码`io.seata.core.constants.ConfigurationKeys`中找到对应配置项进行代码跟踪。

2. `worker-thread-size`，可以配置成"Auto | Pin | BusyPin | Default | 数字"（不区分大小写），seata会去判断计算出相应的worker-thread-size。  
代码参考`io.seata.core.rpc.netty.NettyBaseConfig`的静态代码块 和 `io.seata.core.rpc.netty.NettyBaseConfig.WorkThreadMode`。

3. 一个配置项，多个用途。
例如`worker-thread-size`  
用途一：
```java
// io.seata.core.rpc.netty.TmRpcClient#getInstance()
new java.util.concurrent.ThreadPoolExecutor(
        nettyClientConfig.getClientWorkerThreads(), nettyClientConfig.getClientWorkerThreads(),
        KEEP_ALIVE_TIME, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(MAX_QUEUE_SIZE),
        new NamedThreadFactory(nettyClientConfig.getTmDispatchThreadPrefix(),
            nettyClientConfig.getClientWorkerThreads()),
        RejectedPolicies.runsOldestTaskPolicy());
```

用途二：
```java
// io.seata.core.rpc.netty.RpcClientBootstrap.start
new io.netty.util.concurrent.DefaultEventExecutorGroup(nettyClientConfig.getClientWorkerThreads(),
                new NamedThreadFactory(getThreadPrefix(nettyClientConfig.getClientWorkerThreadPrefix()),
                    nettyClientConfig.getClientWorkerThreads()));
```

4. 跟踪代码示例
入口代码[seata-spring-boot-starter/SeataAutoConfiguration.java](../../seata-spring-boot-starter/src/main/java/io/seata/spring/boot/autoconfigure/SeataAutoConfiguration.java)。  
可以发现其入口是`new GlobalTransactionScanner(..)`，`GlobalTransactionScanner implements InitializingBean`并且重写了`afterPropertiesSet()`方法。
```java
package io.seata.spring.annotation;

public class GlobalTransactionScanner extends AbstractAutoProxyCreator
    implements InitializingBean, ApplicationContextAware,
    DisposableBean {
    
    /**
     * <p>vergilyn-comment, 2020-02-16 >>>> <br/>
     *   此时seata-client会去读取配置文件`register.conf`和`file.conf`
     * </p>
     */
    private final boolean disableGlobalTransaction = ConfigurationFactory.getInstance().getBoolean(
        ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION, false);

    @Override
    public void afterPropertiesSet() {
        if (disableGlobalTransaction) {
            return;
        }
        initClient();

    }
    
    private void initClient() {
        //init TM
        TMClient.init(applicationId, txServiceGroup);
        
        //init RM
        RMClient.init(applicationId, txServiceGroup);
        
        registerSpringShutdownHook();
    }
}
```
TMClient、RMClient基本一样，其对应的RmRpcClient、TmRpcClient都实现了AbstractRpcRemotingClient。

跟踪代码`TMClient.init(applicationId, txServiceGroup)`直到 [o.seata.core.rpc.netty.TmRpcClient#getInstance()](../../core/src/main/java/io/seata/core/rpc/netty/TmRpcClient.java#L81-L103)：
```java
public final class TmRpcClient extends AbstractRpcRemotingClient {
    
     public static TmRpcClient getInstance() {
        if (null == instance) {
            synchronized (TmRpcClient.class) {
                if (null == instance) {
                    NettyClientConfig nettyClientConfig = new NettyClientConfig();
                    
                    /* vergilyn-comment, 2020-02-16 >>>>
                     *   1. corePoolSize = maximumPoolSize
                     *   2. keepAliveTime 为 Integer.Max 秒
                     *   3. workQueue 最大 2000，且不可配置！
                     *   4. threadFactory，只是定义了线程名字，不太需要关心。
                     *   5. RejectedExecutionHandler
                     */
                    final ThreadPoolExecutor messageExecutor = new ThreadPoolExecutor(
                        nettyClientConfig.getClientWorkerThreads(), nettyClientConfig.getClientWorkerThreads(),
                        KEEP_ALIVE_TIME, TimeUnit.SECONDS,
                        new LinkedBlockingQueue<>(MAX_QUEUE_SIZE),
                        new NamedThreadFactory(nettyClientConfig.getTmDispatchThreadPrefix(),
                            nettyClientConfig.getClientWorkerThreads()),
                        RejectedPolicies.runsOldestTaskPolicy());
                    
                    instance = new TmRpcClient(nettyClientConfig, null, messageExecutor);
                }
            }
        }
        return instance;
    }
}

public class NettyClientConfig extends NettyBaseConfig {
    private int clientWorkerThreads = WORKER_THREAD_SIZE;

    public int getClientWorkerThreads() {
        return clientWorkerThreads;
    }
}

public class NettyBaseConfig {
    /**
     * The constant CONFIG.
     * <br/> vergilyn-comment, 2020-02-16 >>>> `file.conf`的配置
     */
    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();
    
    static {
        // 简单转换后的源码
        WORKER_THREAD_SIZE = CONFIG.getConfig("transport.thread-factory.worker-thread-size");
    }
}
```

继续阅读源码，在`io.seata.core.rpc.netty.AbstractRpcRemotingClient`的构造函数中会调用`new RpcClientBootstrap(...)`：
```JAVA
public class RpcClientBootstrap implements RemotingClient {
    
    public RpcClientBootstrap(NettyClientConfig nettyClientConfig, final EventExecutorGroup eventExecutorGroup,
                              ChannelHandler channelHandler, NettyPoolKey.TransactionRole transactionRole) {
        if (null == nettyClientConfig) {
            nettyClientConfig = new NettyClientConfig();
        }
        this.nettyClientConfig = nettyClientConfig;
        int selectorThreadSizeThreadSize = this.nettyClientConfig.getClientSelectorThreadSize();
        this.transactionRole = transactionRole;
        this.eventLoopGroupWorker = new NioEventLoopGroup(selectorThreadSizeThreadSize,
            new NamedThreadFactory(getThreadPrefix(this.nettyClientConfig.getClientSelectorThreadPrefix()),
                selectorThreadSizeThreadSize));
        this.defaultEventExecutorGroup = eventExecutorGroup;
        this.channelHandler = channelHandler;
    }
}

```



2020-02-16 >>>>
seata-client中，一般都是通过`new NettyClientConfig()`来获得配置（`register.conf`和`file.conf`中的配置）。  
并且，这个对象其实也会在程序运行期间一直被引用，那么，为什么不直接写成 单例类 ？
（其实无关紧要，只是觉得可能这样修改后的"代码"更好）

### 3.2 `service`
```HOCON
service {
  #transaction service group mapping
  vgroup_mapping.my_test_tx_group = "default"
  #only support when registry.type=file, please don't set multiple addresses
  default.grouplist = "127.0.0.1:8091"
  #degrade, current not support
  enableDegrade = false
  #disable seata
  disableGlobalTransaction = false
}
```

1. SEATA现在只支持简单的 LoadBalance，所以暂时"please don't set multiple addresses"
```
interface io.seata.discovery.loadbalance.LoadBalance
    implements, abstract class io.seata.discovery.loadbalance.AbstractLoadBalance
        extends, io.seata.discovery.loadbalance.RoundRobinLoadBalance
        extends, io.seata.discovery.loadbalance.RandomLoadBalance
```


#### 3.2.1 seata-server

#### 3.2.2 seata-client
```hocon
service {
  #transaction service group mapping
  vgroup_mapping.my_test_tx_group = "default"
  #only support when registry.type=file, please don't set multiple addresses
  default.grouplist = "127.0.0.1:8091"
  #degrade, current not support
  enableDegrade = false
  #disable seata
  disableGlobalTransaction = false
}
```

1. `vgroup_mapping.my_test_tx_group`及`default.grouplist`
这是关联的key。例如，registry-center使用的是nacos。
seata-client启动时，通过跟踪代码`io.seata.spring.annotation.GlobalTransactionScanner#initClient()` (例如 TMClient#init(...))
```JAVA
public abstract class AbstractRpcRemotingClient extends AbstractRpcRemoting
    implements RegisterMsgListener, ClientMessageSender {
    
     @Override
    public void init() {
        clientBootstrap.start();

        // vergilyn-comment, 2020-02-18 >>>> 每隔5ms连接seata-server，会使用到`file.conf`的"service.vgroup_mapping.my_test_tx_group"
        timerExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                clientChannelManager.reconnect(getTransactionServiceGroup());
            }
        }, SCHEDULE_INTERVAL_MILLS, SCHEDULE_INTERVAL_MILLS, TimeUnit.SECONDS);

        super.init();
    }
}
```

2. `enableDegrade`
参考： `io.seata.core.rpc.netty.TmRpcClient#init()`  
暂时不未实现任何功能！

3. `disableGlobalTransaction`
```java
package io.seata.spring.annotation;

public class GlobalTransactionScanner extends AbstractAutoProxyCreator
    implements InitializingBean, ApplicationContextAware,
    DisposableBean {
    
    private final boolean disableGlobalTransaction = ConfigurationFactory.getInstance().getBoolean(
        ConfigurationKeys.DISABLE_GLOBAL_TRANSACTION, false);
    
    @Override
    public void afterPropertiesSet() {
        if (disableGlobalTransaction) {
            return;
        }
        initClient();

    }
    
    private void initClient() {
        //init TM
        TMClient.init(applicationId, txServiceGroup);
        //init RM
        RMClient.init(applicationId, txServiceGroup);

        registerSpringShutdownHook();
    }
}
```

估计是为了让seata-client可以方便禁用全局事务，当想用的时候再快速启用。





2020-02-18 >>>>
SEATA源码中，对`register.conf`和`file.conf`的 key 引用没有统一。  
比如，大多数用的都是`io.seata.core.constants.ConfigurationKeys`。
但是`io.seata.discovery.registry.RegistryService`中又定义了`service.vgroup_mapping`。

2020-02-19 >>>>
service.vgroup_mapping.my_test_tx_group	事务群组（附录1）	my_test_tx_group为分组，配置项值为TC集群名
service.default.grouplist	TC服务列表（附录2）	**仅注册中心为file时使用**
service.enableDegrade	降级开关（待实现）	默认false。业务侧根据连续错误数自动降级不走seata事务

事务分组说明。
1.事务分组是什么？
事务分组是seata的资源逻辑，类似于服务实例。在file.conf中的my_test_tx_group就是一个事务分组。

2.通过事务分组如何找到后端集群？
首先程序中配置了事务分组（GlobalTransactionScanner 构造方法的txServiceGroup参数），  
程序会通过用户配置的配置中心去寻找service.vgroup_mapping.事务分组配置项，取得配置项的值就是TC集群的名称。  
拿到集群名称程序通过一定的前后缀+集群名称去构造服务名，各配置中心的服务名实现不同。  
拿到服务名去相应的注册中心去拉取相应服务名的服务列表，获得后端真实的TC服务列表。

3.为什么这么设计，不直接取服务名？
这里多了一层获取事务分组到映射集群的配置。这样设计后，事务分组可以作为资源的逻辑隔离单位，当发生故障时可以快速failover。