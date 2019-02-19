### Metrics
#### 设计思路
1. Fescar作为一个被集成的一致性框架，Metrics模块将尽可能少的使用第三方依赖以降低冲突风险；
2. Metrics模块将竭力争取更高的度量性能和更低的资源开销，以降低开启后带来的副作用；
3. 插件式——Metrics是否激活、如何发布，去取决于是否引入了对应的依赖，例如在TC Server中引入`fescar-metrics-prometheus`，则自动启用并将度量数据发布到[Prometheus](https://github.com/prometheus)；
4. 不使用Spring，使用SPI（Service Provider Interface）加载扩展；
5. 初始仅发布核心Transaction相关的指标，之后结合社区的需求，逐步完善运维方面的所有其他指标。

#### 模块说明
由1个核心API模块`fescar-metrics-api`和N个对接实现模块如`fescar-metrics-prometheus`构成：
- fescar-metrics-api 模块
此模块是Metrics的核心，将作为Fescar基础架构的一部分被TC、TM和RM引用，它内部**没有任何具体实现代码**，仅包含接口定义，定义的内容包括：
1. Meter类接口：`Gauge`、`Counter`、`Timer`...
2. 注册容器接口`Registry`
3. Measurement发布接口`Publisher`

>提示：Metrics本身在开源领域也已有很多实现，例如
>1. [Netflix-Spectator](https://github.com/Netflix/spectator)
>2. [Dropwizard-Metrics](https://github.com/dropwizard/metrics)
>3. [Dubbo-Metrics](https://github.com/dubbo/dubbo-metrics)
>它们有的轻而敏捷，有的重而强大，由于也是“实现”，因此不会纳入`fescar-metrics-api`中，避免实现绑定。

- fescar-metrics-prometheus 模块
这是我们默认提供的Metrics实现，不使用其它Metrics开源实现，并轻量级的实现了以下三个Meter：
| Meter类型 | 描述                                                  |
| --------- | ------------------------------------------------------------ |
| Gauge     | 单一最新值度量器，例如个数                                   |
| Counter   | 多Measurement输出计数器，将输出`total`（合计）, `count`（计数）, `max`（最大）, `average`（合计/计数）和`tps`（合计/时间间隔） |
| Timer     | 多Measurement输出计时器，将输出`total`（合计）, `count`（计数）, `max`（最大）, `average`（合计/计数），支持微妙为单位累计 |

>说明：
>1. 未来可能增加更丰富复杂的度量器例如Histogram，这是一种可以本地统计聚合75th, 90th, 95th, 98th, 99th,99.9th...的度量器，适合某些场合，但需要更多内存。
>2. 所有的计量器都将继承自Meter，所有的计量器执行measure()方法后，都将归一化的生成1或N个Measurement结果。

它也会实现一个内存的Registry和PrometheusExporter，将度量数据同步给Prometheus。

>说明：不同的监控系统，采集度量数据的方式不尽相同，例如Zabbix支持用zabbix-agent推送，Prometheus则推荐使用prometheus-server[拉取](https://prometheus.io/docs/practices/pushing/)的方式；同样数据交换协议也不同，因此往往需要逐一适配。

#### 如何使用
##### 引入依赖
如果需要开启TC的Metrics，只需要在`fescar-server`的pom中增加：
```xml
<dependencies>
	<dependency>
		<groupId>${project.groupId}</groupId>
		<artifactId>fescar-core</artifactId>
	</dependency>
	<!--导入依赖，启用Metrics-->
	<dependency>
		<groupId>${project.groupId}</groupId>
		<artifactId>fescar-metrics-prometheus</artifactId>
	</dependency>
	<dependency>
		<groupId>commons-lang</groupId>
		<artifactId>commons-lang</artifactId>
	</dependency>
	<dependency>
		<groupId>org.testng</groupId>
		<artifactId>testng</artifactId>
		<scope>test</scope>
	</dependency>
</dependencies>
```

之后启动TC，即可在`http://tc-server-ip:9898/metrics`上获取到Metrics的文本格式数据。

>提示：默认使用`9898`端口，Prometheus已登记的端口列表[在此](https://github.com/prometheus/prometheus/wiki/Default-port-allocations)，如果想更换端口，可通过`metrics.exporter.prometheus.port`配置修改。

##### 下载并启动Prometheus
下载完毕后，修改Prometheus的配置文件`prometheus.yml`，在`scrape_configs`中增加一项抓取Fescar的度量数据：
```yaml
scrape_configs:
  # The job name is added as a label `job=<job_name>` to any timeseries scraped from this config.
  - job_name: 'prometheus'

    # metrics_path defaults to '/metrics'
    # scheme defaults to 'http'.

    static_configs:
    - targets: ['localhost:9090']

  - job_name: 'fescar'

    # metrics_path defaults to '/metrics'
    # scheme defaults to 'http'.

    static_configs:
    - targets: ['tc-server-ip:9898']
```

##### 查看数据输出
推荐结合配置[Grafana](https://prometheus.io/docs/visualization/grafana/)获得更好的查询效果，初期Fescar导出的Metrics包括：
- TC :

| Metrics    | 描述    |
| ------ | --------- |
| fescar.transaction(role=tc,meter=gauge,status=active/committed/rollback) | 当前活动中/已提交/已回滚的事务总数  |
| fescar.transaction(role=tc,meter=counter,statistic=count,status=committed/rollback) | 当前周期内提交/回滚的事务数  |
| fescar.transaction(role=tc,meter=counter,statistic=tps,status=committed/rollback) | 当前周期内提交/回滚的事务TPS（transaction per second） |
| fescar.transaction(role=tc,meter=timer,statistic=total,status=committed/rollback) | 当前周期内提交/回滚的事务耗时总和 |
| fescar.transaction(role=tc,meter=timer,statistic=count,status=committed/rollback) | 当前周期内提交/回滚的事务数  |
| fescar.transaction(role=tc,meter=timer,statistic=average,status=committed/rollback) | 当前周期内提交/回滚的事务平均耗时   |
| fescar.transaction(role=tc,meter=timer,statistic=max,status=committed/rollback) | 当前周期内提交/回滚的事务最大耗时 |

>提示：fescar.transaction(role=tc,meter=counter,statistic=count,status=committed/rollback)和fescar.transaction(role=tc,meter=timer,statistic=count,status=committed/rollback)的值可能相同，但它们来源于两个不同的度量器。

- TM：
稍后实现，包括诸如：
fescar.transaction(role=tm,name={GlobalTransactionalName},meter=gauge,status=active/committed/rollback) : 以GlobalTransactionalName为维度区分不同Transactional的状态。

- RM：
稍后实现，包括诸如：
fescar.transaction(role=rm,name={BranchTransactionalName},mode=at/mt,meter=gauge,status=active/committed/rollback)：以BranchTransactionalName为维度以及AT/MT维度区分不同分支Transactional的状态。