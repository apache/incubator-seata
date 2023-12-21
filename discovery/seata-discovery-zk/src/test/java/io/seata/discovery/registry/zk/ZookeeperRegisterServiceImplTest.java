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
package io.seata.discovery.registry.zk;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.seata.common.util.NetUtil;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.config.exception.ConfigNotFoundException;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 */
public class ZookeeperRegisterServiceImplTest {
    protected static TestingServer server = null;

    @BeforeAll
    public static void adBeforeClass() throws Exception {
        server = new TestingServer(2181, true);
        server.start();
    }

    @AfterAll
    public static void adAfterClass() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    ZookeeperRegisterServiceImpl service = (ZookeeperRegisterServiceImpl) new ZookeeperRegistryProvider().provide();

    @Test
    public void getInstance() {
        ZookeeperRegisterServiceImpl service1 = ZookeeperRegisterServiceImpl.getInstance();
        Assertions.assertEquals(service1, service);
    }

    @Test
    public void buildZkTest() {
        ZkClient client = service.buildZkClient("127.0.0.1:2181", 5000, 5000);
        Assertions.assertTrue(client.exists("/zookeeper"));
    }

    @Test
    public void testAll() throws Exception {
        service.register(new InetSocketAddress(NetUtil.getLocalAddress(), 33333));

        Assertions.assertThrows(ConfigNotFoundException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                service.lookup("xxx");
            }
        });
        List<InetSocketAddress> lookup2 = service.doLookup("default");
        Assertions.assertEquals(1, lookup2.size());

        final List<String> data = new ArrayList<>();
        final CountDownLatch latch = new CountDownLatch(1);
        IZkChildListener listener = (s, list) -> {
            data.clear();
            data.addAll(list);
            latch.countDown();
        };
        service.subscribe("default", listener);
        final CountDownLatch latch2 = new CountDownLatch(1);
        final List<String> data2 = new ArrayList<>();
        IZkChildListener listener2 = (s, list) -> {
            data2.clear();
            data2.addAll(list);
            latch2.countDown();
        };
        service.subscribe("default", listener2);

        service.unregister(new InetSocketAddress(NetUtil.getLocalAddress(), 33333));
        latch2.await(1000, TimeUnit.MILLISECONDS);
        Assertions.assertEquals(0, data2.size());

        service.unsubscribe("default", listener);
        service.unsubscribe("default", listener2);
    }

    @Test
    public void testLookUp() throws Exception {
        ZookeeperRegisterServiceImpl zookeeperRegisterService = ZookeeperRegisterServiceImpl.getInstance();

        ZkClient client = service.buildZkClient("127.0.0.1:2181", 5000, 5000);
        client.createPersistent("/registry/zk/cluster");
        client.createEphemeral("/registry/zk/cluster/127.0.0.1:8091");

        Field field = ZookeeperRegisterServiceImpl.class.getDeclaredField("zkClient");
        field.setAccessible(true);
        field.set(zookeeperRegisterService, client);

        MockedStatic<ConfigurationFactory> configurationFactoryMockedStatic = Mockito.mockStatic(ConfigurationFactory.class);
        Configuration configuration = mock(Configuration.class);
        configurationFactoryMockedStatic.when(ConfigurationFactory::getInstance).thenReturn(configuration);
        when(configuration.getConfig(anyString())).thenReturn("cluster");

        List<InetSocketAddress> addressList = zookeeperRegisterService.lookup("group");

        configurationFactoryMockedStatic.close();

        Assertions.assertEquals(addressList, Collections.singletonList(new InetSocketAddress("127.0.0.1", 8091)));
    }

}
