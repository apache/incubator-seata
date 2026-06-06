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

import org.apache.seata.common.XID;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.metadata.ClusterRole;
import org.apache.seata.common.metadata.Instance;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.store.SessionMode;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.spring.boot.autoconfigure.properties.registry.RegistryNamingServerProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GeneralInstanceStrategyTest extends BaseSpringBootTest {

    private GeneralInstanceStrategy strategy;
    private Instance previousInstance;
    private Object previousEnvironment;

    @BeforeEach
    void setUp() {
        previousInstance = copyInstance(Instance.getInstance());
        previousEnvironment = ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        strategy = new GeneralInstanceStrategy();
        RegistryNamingServerProperties namingProps = new RegistryNamingServerProperties();
        namingProps.setNamespace("public");
        namingProps.setCluster("default");

        ServerProperties serverProperties = new ServerProperties();
        serverProperties.setPort(8088);

        strategy.registryNamingServerProperties = namingProps;
        strategy.serverProperties = serverProperties;
    }

    @AfterEach
    void tearDown() {
        restoreInstance(previousInstance);
        restoreEnvironment(previousEnvironment);
    }

    @Test
    void serverInstanceInitShouldUseServicePortForControlEndpoint() {
        ConfigurableEnvironment environment = buildEnvironmentWithMeta();
        ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, environment);

        try (MockedStatic<XID> xidMock = Mockito.mockStatic(XID.class);
                MockedStatic<org.apache.seata.server.store.StoreConfig> storeConfigMock =
                        Mockito.mockStatic(org.apache.seata.server.store.StoreConfig.class)) {
            xidMock.when(XID::getIpAddress).thenReturn("10.0.0.2");
            xidMock.when(XID::getPort).thenReturn(7091);
            storeConfigMock
                    .when(org.apache.seata.server.store.StoreConfig::getSessionMode)
                    .thenReturn(SessionMode.DB);

            Instance instance = strategy.serverInstanceInit();

            assertEquals("public", instance.getNamespace());
            assertEquals("default", instance.getClusterName());
            Node.Endpoint control = instance.getControl();
            assertNotNull(control);
            assertEquals("10.0.0.2", control.getHost());
            assertEquals(7091, control.getPort());
            assertEquals("default", instance.getMetadata().get("cluster-type"));
        }
    }

    @Test
    void typeShouldReturnGeneral() {
        assertEquals(SeataInstanceStrategy.Type.GENERAL, strategy.type());
    }

    private ConfigurableEnvironment buildEnvironmentWithMeta() {
        Map<String, Object> map = new HashMap<>();
        map.put("meta.demo", "v");
        StandardEnvironment environment = new StandardEnvironment();
        MutablePropertySources sources = environment.getPropertySources();
        sources.addFirst(new MapPropertySource("testMeta", map));
        return environment;
    }

    private Instance copyInstance(Instance instance) {
        Instance snapshot = instance.clone();
        snapshot.setRole(instance.getRole());
        snapshot.setMetadata(new HashMap<>(instance.getMetadata()));
        return snapshot;
    }

    private void restoreInstance(Instance snapshot) {
        Instance instance = Instance.getInstance();
        instance.setNamespace(snapshot.getNamespace());
        instance.setClusterName(snapshot.getClusterName());
        instance.setUnit(snapshot.getUnit());
        instance.setControl(snapshot.getControl());
        instance.setTransaction(snapshot.getTransaction());
        instance.setInternal(snapshot.getInternal());
        instance.setHealthy(snapshot.isHealthy());
        instance.setWeight(snapshot.getWeight());
        instance.setTerm(snapshot.getTerm());
        instance.setTimestamp(snapshot.getTimestamp());
        instance.setMetadata(new HashMap<>(snapshot.getMetadata()));
        instance.setRole(snapshot.getRole());
        instance.setVersion(snapshot.getVersion());
    }

    private void restoreEnvironment(Object environment) {
        if (environment != null) {
            ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, environment);
            return;
        }
        removeObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
    }

    @SuppressWarnings("unchecked")
    private void removeObject(String objectKey) {
        try {
            Field objectMapField = ObjectHolder.class.getDeclaredField("OBJECT_MAP");
            objectMapField.setAccessible(true);
            ((Map<String, Object>) objectMapField.get(null)).remove(objectKey);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
