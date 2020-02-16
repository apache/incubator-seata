# SEATA-conf

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

### 2.1 `registry`(seata-server)
**只有 seata-server 需要配置该项**，表明：将seata-server注册到哪个注册中心。

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


### 3.1 `transport`
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

seata-client:
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

    # share-boss-worker = false # 暂时未发现其相关实现，因为暂时没有任何意义
    client-selector-thread-prefix = "NettyClientSelector"
    client-selector-thread-size = 1
    client-worker-thread-prefix = "NettyClientWorkerThread"
    # netty boss thread size,will not be used for UDT
    # boss-thread-size = 1 # 2020-02-16只在`io.seata.core.rpc.netty.AbstractRpcRemotingServer`用到
    #auto default pin or 8
    worker-thread-size = 8
  }
  serialization = "seata"
  compressor = "none"
}
```

比较特别的配置项`worker-thread-size`，可以配置成"Auto | Pin | BusyPin | Default | 数字"（不区分大小写），程序会去判断计算出相应的worker-thread-size。  
代码参考`io.seata.core.rpc.netty.NettyBaseConfig`的静态代码块。

跟踪代码 [seata-spring-boot-starter/SeataAutoConfiguration.java](../../seata-spring-boot-starter/src/main/java/io/seata/spring/boot/autoconfigure/SeataAutoConfiguration.java)。  
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

继续跟踪代码`TMClient.init(applicationId, txServiceGroup)`直到 [o.seata.core.rpc.netty.TmRpcClient#getInstance()](../../core/src/main/java/io/seata/core/rpc/netty/TmRpcClient.java#L81-L103)：
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