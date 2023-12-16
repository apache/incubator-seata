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

import io.seata.common.util.NetUtil;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.discovery.registry.redis.RedisRegistryProvider;
import io.seata.discovery.registry.redis.RedisRegistryServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.internal.util.collections.Sets;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author laywin
 */
public class RedisRegisterServiceImplTest {


    @Test
    public void testRemoveServerAddressByPushEmptyProtection()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {

        System.setProperty("config.type", "file");
        System.setProperty("config.file.name", "aa");
        System.setProperty("registry.redis.serverAddr", "127.0.0.1:6379");

        MockedStatic<ConfigurationFactory> configurationFactoryMockedStatic = Mockito.mockStatic(ConfigurationFactory.class);
        Configuration configuration = mock(Configuration.class);
        when(configuration.getConfig(anyString())).thenReturn("cluster");

        configurationFactoryMockedStatic.when(ConfigurationFactory::getInstance).thenReturn(configuration);
        RedisRegistryServiceImpl redisRegistryService = (RedisRegistryServiceImpl) new RedisRegistryProvider().provide();

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
