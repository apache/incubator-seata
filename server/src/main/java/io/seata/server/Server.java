/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.server;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.seata.common.XID;
import io.seata.common.holder.ObjectHolder;
import io.seata.common.metadata.Instance;
import io.seata.common.metadata.Node;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.NetUtil;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.core.rpc.netty.NettyRemotingServer;
import io.seata.core.rpc.netty.NettyServerConfig;
import io.seata.server.coordinator.DefaultCoordinator;
import io.seata.server.lock.LockerManagerFactory;
import io.seata.server.metrics.MetricsManager;
import io.seata.server.session.SessionHolder;
import io.seata.server.store.StoreConfig;
import io.seata.server.store.VGroupMappingStoreManager;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;

import static io.seata.common.ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR;
import static io.seata.common.ConfigurationKeys.FILE_ROOT_REGISTRY;
import static io.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static io.seata.spring.boot.autoconfigure.StarterConstants.REGEX_SPLIT_CHAR;
import static io.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_PREFERED_NETWORKS;

/**
 * The type Server.
 *
 * @author slievrly
 */
public class Server {

    private static final String SEATA_ROOT_KEY = "seata";
    private static final String REGISTRY_TYPE = "namingserver";
    private static final String NAMESPACE_KEY = "namespace";
    private static final String DEFAULT_NAMESPACE = "public";
    private static final String CLUSTER_NAME_KEY = "cluster";
    private static final String DEFAULT_CLUSTER_NAME = "default";

    public static void metadataInit(){

        ConfigurableEnvironment environment = (ConfigurableEnvironment) ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);

        // load node properties
        Instance instance = Instance.getInstance();

        // load namespace
        String namespaceKey = String.join(FILE_CONFIG_SPLIT_CHAR, SEATA_ROOT_KEY, FILE_ROOT_REGISTRY, REGISTRY_TYPE, NAMESPACE_KEY);
        String namespace = environment.getProperty(namespaceKey, DEFAULT_NAMESPACE);
        instance.setNamespace(namespace);

        // load cluster name
        String clusterNameKey = String.join(FILE_CONFIG_SPLIT_CHAR, SEATA_ROOT_KEY, FILE_ROOT_REGISTRY, REGISTRY_TYPE, CLUSTER_NAME_KEY);
        String clusterName = environment.getProperty(clusterNameKey, DEFAULT_CLUSTER_NAME);
        instance.setClusterName(clusterName);

        // load cluster type
        String clusterType = String.valueOf(StoreConfig.getSessionMode());;
        instance.addMetadata("cluster-type", "raft".equals(clusterType) ? clusterType : "default");

        // load unit name
        instance.setUnit(String.valueOf(UUID.randomUUID()));

        // load node Endpoint

        instance.setControlEndpoint(new Node.Endpoint(NetUtil.getLocalIp(),Integer.parseInt(Objects.requireNonNull(environment.getProperty("server.port"))),"http"));

        // load metadata
        String prefix = "seata.registry.metadata.";
        for (PropertySource<?> propertySource : environment.getPropertySources()) {
            if (propertySource instanceof EnumerablePropertySource) {
                EnumerablePropertySource<?> enumerablePropertySource = (EnumerablePropertySource<?>) propertySource;
                for (String propertyName : enumerablePropertySource.getPropertyNames()) {
                    if (propertyName.startsWith(prefix)) {
                        instance.addMetadata(propertyName.substring(prefix.length()), enumerablePropertySource.getProperty(propertyName));
                    }
                }
            }
        }

        // load vgroup mapping relationship
        VGroupMappingStoreManager vGroupMappingStoreManager = SessionHolder.getRootVGroupMappingManager();
        instance.addMetadata("vGroup", vGroupMappingStoreManager.loadVGroups());
    }

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void start(String[] args) {
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
        //log store mode : file, db, redis
        SessionHolder.init();
        LockerManagerFactory.init();
        DefaultCoordinator coordinator = DefaultCoordinator.getInstance(nettyRemotingServer);
        coordinator.init();
        nettyRemotingServer.setHandler(coordinator);

        // let ServerRunner do destroy instead ShutdownHook, see https://github.com/seata/seata/issues/4028
        ServerRunner.addDisposable(coordinator);

        metadataInit();

        nettyRemotingServer.init();
    }
}
