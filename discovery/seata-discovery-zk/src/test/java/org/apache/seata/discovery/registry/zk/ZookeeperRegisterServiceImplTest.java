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
package org.apache.seata.discovery.registry.zk;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.test.TestingServer;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.config.exception.ConfigNotFoundException;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

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

        CuratorFramework client = service.buildZkClient("127.0.0.1:2181", 5000, 5000);
        try {
            Stat stat = client.checkExists().forPath("/zookeeper");
            Assertions.assertTrue(stat!=null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
        CuratorCacheListener listener = (CuratorCacheListener.Type type, ChildData oldData, ChildData newdata) -> {
            List<String> list;
            try {
                list =service.getZkClient().getChildren().forPath(newdata.getPath());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            data.clear();
            data.addAll(list);
            latch.countDown();
        };
        service.subscribe("default", listener);
        final CountDownLatch latch2 = new CountDownLatch(1);
        final List<String> data2 = new ArrayList<>();

        CuratorCacheListener listener2 = (CuratorCacheListener.Type type, ChildData oldData, ChildData newdata) -> {
            List<String> list;
            try {
                list =service.getZkClient().getChildren().forPath(newdata.getPath());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
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

        CuratorFramework client = service.buildZkClient("127.0.0.1:2181", 5000, 5000);
        client.create().withMode(CreateMode.PERSISTENT).forPath("/registry/zk/cluster");
        client.create().withMode(CreateMode.EPHEMERAL).forPath("/registry/zk/cluster/127.0.0.1:8091");

        Field field = ZookeeperRegisterServiceImpl.class.getDeclaredField("zkClient");
        field.setAccessible(true);
        field.set(zookeeperRegisterService, client);


        System.setProperty("txServiceGroup", "default_tx_group");
        System.setProperty("service.vgroupMapping.default_tx_group", "cluster");


        List<InetSocketAddress> addressList = zookeeperRegisterService.lookup("default_tx_group");

        Assertions.assertEquals(addressList, Collections.singletonList(new InetSocketAddress("127.0.0.1", 8091)));
    }

    @Test
    public void testRemoveOfflineAddressesIfNecessaryNoRemoveCase() {
        Map<String, List<InetSocketAddress>> addresses = service.CURRENT_ADDRESS_MAP.computeIfAbsent("default_tx_group", k -> new HashMap<>());
        addresses.put("cluster", Collections.singletonList(new InetSocketAddress("127.0.0.1", 8091)));
        service.removeOfflineAddressesIfNecessary("default_tx_group","cluster", Collections.singletonList(new InetSocketAddress("127.0.0.1", 8091)));

        Assertions.assertEquals(1, service.CURRENT_ADDRESS_MAP.get("default_tx_group").get("cluster").size());
    }

    @Test
    public void testRemovePreventEmptyPushCase() {
        Map<String, List<InetSocketAddress>> addresses = service.CURRENT_ADDRESS_MAP.computeIfAbsent("default_tx_group", k -> new HashMap<>());

        addresses.put("cluster", Collections.singletonList(new InetSocketAddress("127.0.0.1", 8091)));

        service.removeOfflineAddressesIfNecessary("default_tx_group", "cluster", Collections.singletonList(new InetSocketAddress("127.0.0.2", 8091)));

        Assertions.assertEquals(1, service.CURRENT_ADDRESS_MAP.get("default_tx_group").get("cluster").size());
    }

    @Test
    public void testAliveLookup() {

        System.setProperty("txServiceGroup", "default_tx_group");
        System.setProperty("service.vgroupMapping.default_tx_group", "cluster");

        Map<String, List<InetSocketAddress>> addresses = service.CURRENT_ADDRESS_MAP.computeIfAbsent("default_tx_group", k -> new HashMap<>());
        addresses.put("cluster", Collections.singletonList(new InetSocketAddress("127.0.0.1", 8091)));
        List<InetSocketAddress> result = service.aliveLookup("default_tx_group");

        Assertions.assertEquals(result, Collections.singletonList(new InetSocketAddress("127.0.0.1", 8091)));
    }


    @Test
    public void tesRefreshAliveLookup() {

        System.setProperty("txServiceGroup", "default_tx_group");
        System.setProperty("service.vgroupMapping.default_tx_group", "cluster");


        service.refreshAliveLookup("default_tx_group",
                Collections.singletonList(new InetSocketAddress("127.0.0.2", 8091)));

        Assertions.assertEquals(service.CURRENT_ADDRESS_MAP.get("default_tx_group").get("cluster"),
                Collections.singletonList(new InetSocketAddress("127.0.0.2", 8091)));
    }
}
