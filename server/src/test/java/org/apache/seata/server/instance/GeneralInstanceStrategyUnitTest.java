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
import org.apache.seata.common.metadata.Instance;
import org.apache.seata.common.store.SessionMode;
import org.apache.seata.common.thread.ThreadPoolExecutorFactory;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.session.SessionHolder;
import org.apache.seata.server.store.*;
import org.apache.seata.spring.boot.autoconfigure.properties.registry.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.*;

import java.util.*;
import java.util.concurrent.*;

import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GeneralInstanceStrategyUnitTest extends BaseSpringBootTest {
    @Test
    void instanceMetadataAndHeartbeatLifecycle() {
        Object old = ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        ScheduledExecutorService previous = GeneralInstanceStrategy.EXECUTOR_SERVICE;
        StandardEnvironment environment = new StandardEnvironment();
        Map<String, Object> values = new HashMap<>();
        values.put(org.apache.seata.common.ConfigurationKeys.META_PREFIX + "zone", "east");
        values.put("unrelated", "ignored");
        environment.getPropertySources().addFirst(new MapPropertySource("test", values));
        ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, environment);
        Instance instance = mock(Instance.class);
        ScheduledThreadPoolExecutor executor = mock(ScheduledThreadPoolExecutor.class);
        VGroupMappingStoreManager mappings = mock(VGroupMappingStoreManager.class);
        try (MockedStatic<Instance> instances = mockStatic(Instance.class);
                MockedStatic<StoreConfig> config = mockStatic(StoreConfig.class);
                MockedStatic<SessionHolder> holder = mockStatic(SessionHolder.class);
                MockedStatic<ThreadPoolExecutorFactory> pools = mockStatic(ThreadPoolExecutorFactory.class)) {
            instances.when(Instance::getInstance).thenReturn(instance);
            config.when(StoreConfig::getSessionMode).thenReturn(SessionMode.FILE);
            holder.when(SessionHolder::getRootVGroupMappingManager).thenReturn(mappings);
            pools.when(() -> ThreadPoolExecutorFactory.newScheduledThreadPoolExecutor("scheduledExecutor", 1, true))
                    .thenReturn(executor);
            GeneralInstanceStrategy strategy = new GeneralInstanceStrategy();
            strategy.registryProperties = new RegistryProperties();
            strategy.registryNamingServerProperties = new RegistryNamingServerProperties();
            strategy.registryNamingServerProperties.setNamespace("tenant");
            strategy.registryNamingServerProperties.setCluster("east");
            strategy.registryNamingServerProperties.setHeartbeatPeriod(1000);
            ServerProperties properties = new ServerProperties();
            properties.setPort(8080);
            strategy.applicationContext = mock(ApplicationContext.class);
            when(strategy.applicationContext.getBean(ServerProperties.class)).thenReturn(properties);
            strategy.postConstruct();
            assertSame(instance, strategy.serverInstanceInit());
            verify(instance).setNamespace("tenant");
            verify(instance).setClusterName("east");
            verify(instance).addMetadata("cluster-type", "default");
            verify(instance).addMetadata("zone", "east");
            verify(instance, never()).addMetadata(eq("unrelated"), any());
            assertEquals(SeataInstanceStrategy.Type.GENERAL, strategy.type());
            strategy.registryProperties.setType("file");
            strategy.init();
            verifyNoInteractions(executor);
            strategy.registryProperties.setType(org.apache.seata.common.ConfigurationKeys.NAMING_SERVER);
            strategy.init();
            strategy.init();
            ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
            verify(executor).scheduleAtFixedRate(task.capture(), eq(1000L), eq(1000L), eq(TimeUnit.MILLISECONDS));
            when(instance.getTerm()).thenReturn(1L);
            task.getValue().run();
            verify(mappings).notifyMapping();
            doThrow(new IllegalStateException("offline")).when(mappings).notifyMapping();
            assertDoesNotThrow(task.getValue()::run);
            strategy.destroy();
            verify(executor).shutdown();
        } finally {
            GeneralInstanceStrategy.EXECUTOR_SERVICE = previous;
            ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, old);
        }
    }
}
