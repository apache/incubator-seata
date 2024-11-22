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
package org.apache.seata.server.instance;

import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.metadata.namingserver.Instance;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.server.Server;
import org.apache.seata.server.ServerRunner;
import org.apache.seata.server.session.SessionHolder;
import org.apache.seata.server.store.StoreConfig;
import org.apache.seata.server.store.VGroupMappingStoreManager;
import org.apache.seata.spring.boot.autoconfigure.properties.registry.RegistryNamingServerProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.registry.RegistryProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.apache.seata.common.ConfigurationKeys.META_PREFIX;
import static org.apache.seata.common.ConfigurationKeys.NAMING_SERVER;
import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;


@Component("serverInstance")
public class ServerInstance {
    @Resource
    private RegistryProperties registryProperties;

    protected static volatile ScheduledExecutorService EXECUTOR_SERVICE;

    @Resource
    private RegistryNamingServerProperties registryNamingServerProperties;

    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

    public void serverInstanceInit() {
        if (StringUtils.equals(registryProperties.getType(), NAMING_SERVER)) {
            VGroupMappingStoreManager vGroupMappingStoreManager = SessionHolder.getRootVGroupMappingManager();
            EXECUTOR_SERVICE = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("scheduledExcuter", 1, true));
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
            ServerRunner.addDisposable(EXECUTOR_SERVICE::shutdown);
        }
    }
}
