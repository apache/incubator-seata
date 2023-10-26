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
package io.seata.discovery.registry.zk;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.seata.common.util.NetUtil;
import io.seata.config.exception.ConfigNotFoundException;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * @author Geng Zhang
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

}
