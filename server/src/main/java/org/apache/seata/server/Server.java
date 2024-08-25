/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.server;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import org.apache.seata.common.XID;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.metadata.namingserver.Instance;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.common.util.UUIDGenerator;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.rpc.netty.NettyRemotingServer;
import org.apache.seata.core.rpc.netty.NettyServerConfig;
import org.apache.seata.server.coordinator.DefaultCoordinator;
import org.apache.seata.server.lock.LockerManagerFactory;
import org.apache.seata.server.metrics.MetricsManager;
import org.apache.seata.server.session.SessionHolder;
import org.apache.seata.server.store.StoreConfig;
import org.apache.seata.server.store.VGroupMappingStoreManager;
import org.apache.seata.spring.boot.autoconfigure.properties.registry.RegistryNamingServerProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.registry.RegistryProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.GenericWebApplicationContext;


import static org.apache.seata.common.ConfigurationKeys.META_PREFIX;
import static org.apache.seata.common.ConfigurationKeys.NAMING_SERVER;
import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_APPLICATION_CONTEXT;
import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.REGEX_SPLIT_CHAR;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_PREFERED_NETWORKS;

/**
 * The type Server.
 */
@Component("seataServer")
public class Server {
    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

    protected static final ScheduledExecutorService EXECUTOR_SERVICE = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("scheduledExcuter", 1, true));

    @Resource
    RegistryNamingServerProperties registryNamingServerProperties;

    @Resource
    RegistryProperties registryProperties;

    public void metadataInit() {
        VGroupMappingStoreManager vGroupMappingStoreManager = SessionHolder.getRootVGroupMappingManager();
        if (StringUtils.equals(registryProperties.getType(), NAMING_SERVER)) {
            ConfigurableEnvironment environment = (ConfigurableEnvironment) ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);

            // load node properties
            Instance instance = Instance.getInstance();
            // load namespace
            String namespace = registryNamingServerProperties.getNamespace();
            instance.setNamespace(namespace);
            // load cluster name
            String clusterName = registryNamingServerProperties.getCluster();
            instance.setClusterName(clusterName);

            // load cluster type
            String clusterType = String.valueOf(StoreConfig.getSessionMode());
            instance.addMetadata("cluster-type", "raft".equals(clusterType) ? clusterType : "default");

            // load unit name
            instance.setUnit(String.valueOf(UUID.randomUUID()));

            instance.setTerm(System.currentTimeMillis());

            // load node Endpoint
            instance.setControl(new Node.Endpoint(NetUtil.getLocalIp(), Integer.parseInt(Objects.requireNonNull(environment.getProperty("server.port"))), "http"));

            // load metadata
            for (PropertySource<?> propertySource : environment.getPropertySources()) {
                if (propertySource instanceof EnumerablePropertySource) {
                    EnumerablePropertySource<?> enumerablePropertySource = (EnumerablePropertySource<?>) propertySource;
                    for (String propertyName : enumerablePropertySource.getPropertyNames()) {
                        if (propertyName.startsWith(META_PREFIX)) {
                            instance.addMetadata(propertyName.substring(META_PREFIX.length()), enumerablePropertySource.getProperty(propertyName));
                        }
                    }
                }
            }
            // load vgroup mapping relationship
            instance.addMetadata("vGroup", vGroupMappingStoreManager.loadVGroups());

            EXECUTOR_SERVICE.scheduleAtFixedRate(() -> {
                try {
                    vGroupMappingStoreManager.notifyMapping();
                } catch (Exception e) {
                    LOGGER.error("Naming server register Exception", e);
                }
            }, registryNamingServerProperties.getHeartbeatPeriod(),  registryNamingServerProperties.getHeartbeatPeriod(), TimeUnit.MILLISECONDS);
        }
    }


    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public void start(String[] args) {
        //initialize the parameter parser
        //Note that the parameter parser should always be the first line to execute.
        //Because, here we need to parse the parameters needed for startup.
        ParameterParser parameterParser = new ParameterParser(args);

        //initialize the metrics
        MetricsManager.get().init();

        ThreadPoolExecutor workingThreads = new ThreadPoolExecutor(NettyServerConfig.getMinServerPoolSize(),
                NettyServerConfig.getMaxServerPoolSize(), NettyServerConfig.getKeepAliveTime(), TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(NettyServerConfig.getMaxTaskQueueSize()),
                new NamedThreadFactory("ServerHandlerThread", NettyServerConfig.getMaxServerPoolSize()), new ThreadPoolExecutor.CallerRunsPolicy());

        //127.0.0.1 and 0.0.0.0 are not valid here.
        if (NetUtil.isValidIp(parameterParser.getHost(), false)) {
            XID.setIpAddress(parameterParser.getHost());
        } else {
            String preferredNetworks = ConfigurationFactory.getInstance().getConfig(REGISTRY_PREFERED_NETWORKS);
            if (StringUtils.isNotBlank(preferredNetworks)) {
                XID.setIpAddress(NetUtil.getLocalIp(preferredNetworks.split(REGEX_SPLIT_CHAR)));
            } else {
                XID.setIpAddress(NetUtil.getLocalIp());
            }
        }
        NettyRemotingServer nettyRemotingServer = new NettyRemotingServer(workingThreads);
        XID.setPort(nettyRemotingServer.getListenPort());
        UUIDGenerator.init(parameterParser.getServerNode());
        ConfigurableListableBeanFactory beanFactory =
                ((GenericWebApplicationContext) ObjectHolder.INSTANCE
                        .getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT)).getBeanFactory();
        DefaultCoordinator coordinator = DefaultCoordinator.getInstance(nettyRemotingServer);
        if (coordinator instanceof ApplicationListener) {
            beanFactory.registerSingleton(NettyRemotingServer.class.getName(), nettyRemotingServer);
            beanFactory.registerSingleton(DefaultCoordinator.class.getName(), coordinator);
            ((GenericWebApplicationContext) ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_APPLICATION_CONTEXT))
                    .addApplicationListener((ApplicationListener<?>) coordinator);
        }
        //log store mode : file, db, redis
        SessionHolder.init();
        LockerManagerFactory.init();
        coordinator.init();
        nettyRemotingServer.setHandler(coordinator);

        // let ServerRunner do destroy instead ShutdownHook, see https://github.com/seata/seata/issues/4028
        ServerRunner.addDisposable(coordinator);
        metadataInit();

        nettyRemotingServer.init();
    }
}
