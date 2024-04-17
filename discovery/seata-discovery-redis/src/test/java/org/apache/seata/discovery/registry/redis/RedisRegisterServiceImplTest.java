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
package org.apache.seata.discovery.registry.redis;

import org.apache.seata.common.util.NetUtil;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mockito.MockedStatic;
import org.mockito.internal.util.collections.Sets;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;


@EnabledIfSystemProperty(named = "redisCaseEnabled", matches = "true")
public class RedisRegisterServiceImplTest {

    private static RedisRegistryServiceImpl redisRegistryService;

    @BeforeAll
    public static void init() throws IOException {
        System.setProperty("config.type", "file");
        System.setProperty("config.file.name", "file.conf");
        System.setProperty("txServiceGroup", "default_tx_group");
        System.setProperty("service.vgroupMapping.default_tx_group", "default");
        System.setProperty("registry.redis.serverAddr", "127.0.0.1:6379");
        System.setProperty("registry.redis.cluster", "default");
        redisRegistryService = RedisRegistryServiceImpl.getInstance();
    }

    @Test
    public void testFlow() {

        redisRegistryService.register(new InetSocketAddress(NetUtil.getLocalIp(), 8091));

        Assertions.assertTrue(redisRegistryService.lookup("default_tx_group").size() > 0);

        redisRegistryService.unregister(new InetSocketAddress(NetUtil.getLocalIp(), 8091));

        Assertions.assertTrue(redisRegistryService.lookup("default_tx_group").size() > 0);
    }

    @Test
    public void testRemoveServerAddressByPushEmptyProtection()
            throws NoSuchFieldException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        MockedStatic<ConfigurationFactory> configurationFactoryMockedStatic = mockStatic(ConfigurationFactory.class);
        Configuration configuration = mock(Configuration.class);
        when(configuration.getConfig(anyString())).thenReturn("cluster");

        configurationFactoryMockedStatic.when(ConfigurationFactory::getInstance).thenReturn(configuration);

        Field field = RedisRegistryServiceImpl.class.getDeclaredField("CLUSTER_ADDRESS_MAP");
        field.setAccessible(true);

        ConcurrentMap<String, Set<InetSocketAddress>> CLUSTER_ADDRESS_MAP = (ConcurrentMap<String, Set<InetSocketAddress>>)field.get(null);
        CLUSTER_ADDRESS_MAP.put("cluster", Sets.newSet(NetUtil.toInetSocketAddress("127.0.0.1:8091")));

        Method method = RedisRegistryServiceImpl.class.getDeclaredMethod("removeServerAddressByPushEmptyProtection", String.class, String.class);
        method.setAccessible(true);
        method.invoke(redisRegistryService, "cluster", "127.0.0.1:8091");

        // test the push empty protection situation
        Assertions.assertEquals(1, CLUSTER_ADDRESS_MAP.get("cluster").size());


        when(configuration.getConfig(anyString())).thenReturn("mycluster");

        method.invoke(redisRegistryService, "cluster", "127.0.0.1:8091");
        configurationFactoryMockedStatic.close();

        // test the normal remove situation
        Assertions.assertEquals(0, CLUSTER_ADDRESS_MAP.get("cluster").size());
    }

}
