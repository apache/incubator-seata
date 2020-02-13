# SEATA-conf

## dependencies
```XML
<dependency>
    <groupId>com.typesafe</groupId>
    <artifactId>config</artifactId>
</dependency>
```

seata的配置文件使用了：[com.typesafe.config](https://github.com/lightbend/config)


## 1. register.conf

### 1.1 `register.conf`加载
启动seata-server时：
```
RpcServer rpcServer = new RpcServer(WORKING_THREADS);
```

```JAVA
@Sharable
public class RpcServer extends AbstractRpcRemotingServer implements ServerMessageSender {
   public RpcServer(ThreadPoolExecutor messageExecutor) { 
       super(new NettyServerConfig(), messageExecutor);
   }
}
```

```JAVA
/**
 * The type Netty server config.
 * <p>vergilyn-comment, 2020-02-13 >>>> <br/>
 *    NettyServerConfig只是一些配置属性，主要的一些 加载/读取配置 在父类NettyBaseConfig
 * </p>
 * @author slievrly
 */
public class NettyServerConfig extends NettyBaseConfig {
    // 省略...
}
```

```JAVA
/**
 * The type Netty base config.
 * <p>vergilyn-comment, 2020-02-13 >>>> <br/>
 *   主要 初始化/加载 变量{@linkplain #CONFIG}， 注意静态代码块中的代码。
 * </p>
 * @author slievrly
 */
public class NettyBaseConfig {
     protected static final Configuration CONFIG = ConfigurationFactory.getInstance();
     
     // 省略...
}

```

```JAVA
/**
 * The type Configuration factory.
 * <p>vergilyn-comment, 2020-02-13 >>>> <br/>
 *   单例类，seata-server启动时读取服务端的`register.conf`（静态代码块）
 * </p>
 * @author slievrly
 * @author Geng Zhang
 */
public final class ConfigurationFactory {
    private static final String REGISTRY_CONF_PREFIX = "registry";
    private static final String REGISTRY_CONF_SUFFIX = ".conf";
    private static final String ENV_SYSTEM_KEY = "SEATA_ENV";
    public static final String ENV_PROPERTY_KEY = "seataEnv";

    private static final String SYSTEM_PROPERTY_SEATA_CONFIG_NAME = "seata.config.name";

    private static final String ENV_SEATA_CONFIG_NAME = "SEATA_CONFIG_NAME";

    public static final Configuration CURRENT_FILE_INSTANCE;
    
    static {
        String seataConfigName = System.getProperty(SYSTEM_PROPERTY_SEATA_CONFIG_NAME);
        if (null == seataConfigName) {
            seataConfigName = System.getenv(ENV_SEATA_CONFIG_NAME);
        }
        if (null == seataConfigName) {
            seataConfigName = REGISTRY_CONF_PREFIX;
        }
        String envValue = System.getProperty(ENV_PROPERTY_KEY);
        if (null == envValue) {
            envValue = System.getenv(ENV_SYSTEM_KEY);
        }
        Configuration configuration = (null == envValue) ? new FileConfiguration(seataConfigName + REGISTRY_CONF_SUFFIX,
            false) : new FileConfiguration(seataConfigName + "-" + envValue + REGISTRY_CONF_SUFFIX, false);
        Configuration extConfiguration = null;
        try {
            extConfiguration = EnhancedServiceLoader.load(ExtConfigurationProvider.class).provide(configuration);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("load extConfiguration:{}",
                    extConfiguration == null ? null : extConfiguration.getClass().getSimpleName());
            }
        } catch (Exception e) {
            LOGGER.warn("failed to load extConfiguration:{}", e.getMessage(), e);
        }
        CURRENT_FILE_INSTANCE = null == extConfiguration ? configuration : extConfiguration;
    }

    private static volatile Configuration instance = null;

    public static Configuration getInstance() {
        if (instance == null) {
            synchronized (Configuration.class) {
                if (instance == null) {
                    instance = buildConfiguration();
                }
            }
        }
        return instance;
    }
}
```

// NettyServerConfig extends NettyBaseConfig
// NettyBaseConfig存在静态代码块，以及一些`static final`变量。
protected static final Configuration CONFIG = ConfigurationFactory.getInstance();

// ConfigurationFactory 中也存在静态代码块

 static {
        String seataConfigName = System.getProperty(SYSTEM_PROPERTY_SEATA_CONFIG_NAME);
        if (null == seataConfigName) {
            seataConfigName = System.getenv(ENV_SEATA_CONFIG_NAME);
        }
        if (null == seataConfigName) {
            seataConfigName = REGISTRY_CONF_PREFIX;
        }
        String envValue = System.getProperty(ENV_PROPERTY_KEY);
        if (null == envValue) {
            envValue = System.getenv(ENV_SYSTEM_KEY);
        }
        Configuration configuration = (null == envValue) ? new FileConfiguration(seataConfigName + REGISTRY_CONF_SUFFIX,
            false) : new FileConfiguration(seataConfigName + "-" + envValue + REGISTRY_CONF_SUFFIX, false);
        Configuration extConfiguration = null;
        try {
            extConfiguration = EnhancedServiceLoader.load(ExtConfigurationProvider.class).provide(configuration);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("load extConfiguration:{}",
                    extConfiguration == null ? null : extConfiguration.getClass().getSimpleName());
            }
        } catch (Exception e) {
            LOGGER.warn("failed to load extConfiguration:{}", e.getMessage(), e);
        }
        CURRENT_FILE_INSTANCE = null == extConfiguration ? configuration : extConfiguration;
    }
```




## 1.file.conf
完整参考： [seata-server file.conf.example](../server/src/main/resources/file.conf.example)。  
需要优先执行的script在：[script](../script)

### 1.1 `file.conf`加载时机


## 1. transport
```
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
## 2. service
```
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
## 3. client
```
client {
  rm {
    async.commit.buffer.limit = 10000
    lock {
      retry.internal = 10
      retry.times = 30
      retry.policy.branch-rollback-on-conflict = true
    }
    report.retry.count = 5
    table.meta.check.enable = false
    report.success.enable = true
  }
  tm {
    commit.retry.count = 5
    rollback.retry.count = 5
  }
  undo {
    data.validation = true
    log.serialization = "jackson"
    log.table = "undo_log"
  }
  log {
    exceptionRate = 100
  }
  support {
    # auto proxy the DataSource bean
    spring.datasource.autoproxy = false
  }
}
```
## 4. store
```
## transaction log store
store {
  ## store mode: file、db
  mode = "file"
  ## file store property
  file {
    ## store location dir
    dir = "sessionStore"
    # branch session size , if exceeded first try compress lockkey, still exceeded throws exceptions
    max-branch-session-size = 16384
    # globe session size , if exceeded throws exceptions
    max-global-session-size = 512
    # file buffer size , if exceeded allocate new buffer
    file-write-buffer-cache-size = 16384
    # when recover batch read size
    session.reload.read_size = 100
    # async, sync
    flush-disk-mode = async
  }

  ## database store property
  db {
    ## the implement of javax.sql.DataSource, such as DruidDataSource(druid)/BasicDataSource(dbcp) etc.
    datasource = "dbcp"
    ## mysql/oracle/h2/oceanbase etc.
    db-type = "mysql"
    driver-class-name = "com.mysql.jdbc.Driver"
    url = "jdbc:mysql://127.0.0.1:3306/seata"
    user = "mysql"
    password = "mysql"
    min-conn = 1
    max-conn = 10
    global.table = "global_table"
    branch.table = "branch_table"
    lock-table = "lock_table"
    query-limit = 100
  }
}
```

以上配置在seata-server，用于seata-server记录事务log。现在只支持2中模式： file、db。

`db`模式下，需要先在数据库创建保存数据的表：`global_table`、`branch_table`、`lock_table`。  
表名可以修改，其建表语句在 [db](../script/server/db)

相关源码：


## 5. server
```
server {
  recovery {
    #schedule committing retry period in milliseconds
    committing-retry-period = 1000
    #schedule asyn committing retry period in milliseconds
    asyn-committing-retry-period = 1000
    #schedule rollbacking retry period in milliseconds
    rollbacking-retry-period = 1000
    #schedule timeout retry period in milliseconds
    timeout-retry-period = 1000
  }
  undo {
    log.save.days = 7
    #schedule delete expired undo_log in milliseconds
    log.delete.period = 86400000
  }
  #unit ms,s,m,h,d represents milliseconds, seconds, minutes, hours, days, default permanent
  max.commit.retry.timeout = "-1"
  max.rollback.retry.timeout = "-1"
}
```
## 6. metrics
```TEXT
## metrics settings
metrics {
  enabled = false
  registry-type = "compact"
  # multi exporters use comma divided
  exporter-list = "prometheus"
  exporter-prometheus-port = 9898
}
```