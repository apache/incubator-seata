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

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import org.apache.seata.common.metadata.Instance;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.server.session.SessionHolder;
import org.apache.seata.server.store.VGroupMappingStoreManager;
import org.apache.seata.spring.boot.autoconfigure.properties.registry.RegistryNamingServerProperties;
import org.apache.seata.spring.boot.autoconfigure.properties.registry.RegistryProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.ApplicationContext;


import static org.apache.seata.common.ConfigurationKeys.NAMING_SERVER;

public abstract class AbstractSeataInstanceStrategy implements SeataInstanceStrategy {

    @Resource
    protected RegistryProperties registryProperties;

    protected ServerProperties serverProperties;

    @Resource
    protected ApplicationContext applicationContext;

    @Resource
    protected RegistryNamingServerProperties registryNamingServerProperties;

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected static volatile ScheduledExecutorService EXECUTOR_SERVICE;

    protected AtomicBoolean init = new AtomicBoolean(false);
    @PostConstruct
    public void postConstruct() {
        this.serverProperties = applicationContext.getBean(ServerProperties.class);
    }

    @Override
    public void init() {
        if (!StringUtils.equals(registryProperties.getType(), NAMING_SERVER)) {
            return;
        }
        Instance instance = serverInstanceInit();
        if (init.compareAndSet(false, true)) {
            VGroupMappingStoreManager vGroupMappingStoreManager = SessionHolder.getRootVGroupMappingManager();
            // load vgroup mapping relationship
            instance.addMetadata("vGroup", vGroupMappingStoreManager.loadVGroups());
            EXECUTOR_SERVICE = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("scheduledExcuter", 1, true));
            EXECUTOR_SERVICE.scheduleAtFixedRate(() -> {
                try {
                    if (instance.getTerm() > 0) {
                        SessionHolder.getRootVGroupMappingManager().notifyMapping();
                    }
                } catch (Exception e) {
                    logger.error("Naming server register Exception", e);
                }
            }, registryNamingServerProperties.getHeartbeatPeriod(), registryNamingServerProperties.getHeartbeatPeriod(),
                TimeUnit.MILLISECONDS);
        }
    }

    @PreDestroy
    public void destroy() {
        Optional.ofNullable(EXECUTOR_SERVICE).ifPresent(ScheduledExecutorService::shutdown);
    }

}
