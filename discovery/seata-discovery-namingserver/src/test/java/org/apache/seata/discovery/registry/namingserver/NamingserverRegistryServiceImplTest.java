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
package org.apache.seata.discovery.registry.namingserver;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.common.util.HttpClientUtil;
import org.apache.seata.discovery.registry.RegistryService;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;


import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled
class NamingserverRegistryServiceImplTest {

    private static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;

    @BeforeAll
    public static void beforeClass() throws Exception {
        System.setProperty("registry.namingserver.namespace", "dev");
        System.setProperty("registry.namingserver.cluster", "cluster1");
        System.setProperty("registry.namingserver.serverAddr", "127.0.0.1:8080");
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        // 获取应用程序环境
        ConfigurableEnvironment environment = context.getEnvironment();
        MutablePropertySources propertySources = environment.getPropertySources();
        Properties customProperties = new Properties();
        customProperties.setProperty("seata.registry.namingserver.server-addr[0]", "127.0.0.1:8080");

        PropertiesPropertySource customPropertySource = new PropertiesPropertySource("customSource", customProperties);
        propertySources.addLast(customPropertySource);
        ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, environment);

    }


    @Test
    public void unregister1() throws Exception {
        NamingserverRegistryServiceImpl namingserverRegistryService = NamingserverRegistryServiceImpl.getInstance();
        InetSocketAddress inetSocketAddress = new InetSocketAddress("127.0.0.1", 8080);
        namingserverRegistryService.register(inetSocketAddress);
        namingserverRegistryService.unregister(inetSocketAddress);
    }


    @Test
    public void getNamingAddrsTest() {
        NamingserverRegistryServiceImpl namingserverRegistryService = NamingserverRegistryServiceImpl.getInstance();
        List<String> list = namingserverRegistryService.getNamingAddrs();
        assertEquals(list.size(), 1);
    }


    @Test
    public void getNamingAddrTest() {
        NamingserverRegistryServiceImpl namingserverRegistryService = NamingserverRegistryServiceImpl.getInstance();
        String addr = namingserverRegistryService.getNamingAddr();
        assertEquals(addr, "127.0.0.1:8080");
    }


    @Test
    public void convertTest() {
        InetSocketAddress inetSocketAddress = new InetSocketAddress("127.0.0.1", 8088);
        assertEquals(inetSocketAddress.getAddress().getHostAddress(), "127.0.0.1");
        assertEquals(inetSocketAddress.getPort(), 8088);
    }


    @Test
    public void testRegister1() throws Exception {

        RegistryService registryService = new NamingserverRegistryProvider().provide();


        InetSocketAddress inetSocketAddress1 = new InetSocketAddress("127.0.0.1", 8088);
        //1.register
        registryService.register(inetSocketAddress1);

        //2.create vGroup in cluster
        createGroupInCluster("dev", "group1", "cluster1");

        //3.get instances
        List<InetSocketAddress> list = registryService.lookup("group1");

        assertEquals(list.size(), 1);
        InetSocketAddress inetSocketAddress = list.get(0);
        assertEquals(inetSocketAddress.getAddress().getHostAddress(), "127.0.0.1");
        assertEquals(inetSocketAddress.getPort(), 8088);

        registryService.unregister(inetSocketAddress1);


    }


    @Test
    public void testRegister2() throws Exception {
        NamingserverRegistryServiceImpl registryService = (NamingserverRegistryServiceImpl) new NamingserverRegistryProvider().provide();
        InetSocketAddress inetSocketAddress1 = new InetSocketAddress("127.0.0.1", 8088);
        InetSocketAddress inetSocketAddress2 = new InetSocketAddress("127.0.0.1", 8088);
        //1.register
        registryService.register(inetSocketAddress1);
        registryService.register(inetSocketAddress2);

        //2.create vGroup in cluster
        String namespace = FILE_CONFIG.getConfig("registry.namingserver.namespace");
        createGroupInCluster(namespace, "group1", "cluster1");

        //3.get instances
        List list = registryService.lookup("group1");

        assertEquals(list.size(), 1);

        registryService.unregister(inetSocketAddress1);
        registryService.unregister(inetSocketAddress2);
        registryService.unsubscribe("group1");
    }


    @Test
    public void testRegister3() throws Exception {
        NamingserverRegistryServiceImpl registryService = (NamingserverRegistryServiceImpl) new NamingserverRegistryProvider().provide();
        InetSocketAddress inetSocketAddress1 = new InetSocketAddress("127.0.0.1", 8088);
        InetSocketAddress inetSocketAddress2 = new InetSocketAddress("127.0.0.1", 8089);
        InetSocketAddress inetSocketAddress3 = new InetSocketAddress("127.0.0.1", 8090);
        InetSocketAddress inetSocketAddress4 = new InetSocketAddress("127.0.0.1", 8091);
        //1.register
        registryService.register(inetSocketAddress1);
        registryService.register(inetSocketAddress2);
        registryService.register(inetSocketAddress3);
        registryService.register(inetSocketAddress4);

        //2.create vGroup in cluster
        String namespace = FILE_CONFIG.getConfig("registry.namingserver.namespace");
        createGroupInCluster(namespace, "group2", "cluster1");

        //3.get instances
        List list = registryService.lookup("group2");

        assertEquals(list.size(), 4);

        registryService.unregister(inetSocketAddress1);
        registryService.unregister(inetSocketAddress2);
        registryService.unregister(inetSocketAddress3);
        registryService.unregister(inetSocketAddress4);

        registryService.unsubscribe("group2");

    }


    @Test
    public void testUnregister() throws Exception {
        RegistryService registryService = new NamingserverRegistryProvider().provide();
        InetSocketAddress inetSocketAddress1 = new InetSocketAddress("127.0.0.1", 8088);
        //1.register
        registryService.register(inetSocketAddress1);

        //2.create vGroup in cluster
        String namespace = FILE_CONFIG.getConfig("registry.namingserver.namespace");
        createGroupInCluster(namespace, "group1", "cluster1");

        //3.get instances
        List list = registryService.lookup("group1");

        assertEquals(list.size(), 1);

        //4.unregister
        registryService.unregister(inetSocketAddress1);

        //5.get instances
        List list1 = registryService.lookup("group1");
        assertEquals(list1.size(), 0);

    }


    //    @Disabled
    @Test
    public void testWatch() throws Exception {
        NamingserverRegistryServiceImpl registryService = (NamingserverRegistryServiceImpl) new NamingserverRegistryProvider().provide();

        //1.注册cluster1下的一个节点
        InetSocketAddress inetSocketAddress1 = new InetSocketAddress("127.0.0.1", 8088);
        registryService.register(inetSocketAddress1);

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        int delaySeconds = 500;
        //2.延迟0.5s后在cluster1下创建事务分组group1
        executor.schedule(() -> {
            try {

                String namespace = FILE_CONFIG.getConfig("registry.namingserver.namespace");
                createGroupInCluster(namespace, "group1", "cluster1");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            executor.shutdown();  // 任务执行后关闭执行器
        }, delaySeconds, TimeUnit.MILLISECONDS);
        //3.watch事务分组group1
        long timestamp1 = System.currentTimeMillis();
        boolean needFetch = registryService.watch("group1");
        long timestamp2 = System.currentTimeMillis();
        //4.  0.5s后group1被映射到cluster1下，应该有数据在1s内推送到client端
        assert timestamp2 - timestamp1 < 1500;

        //5. 获取实例
        List<InetSocketAddress> list = registryService.lookup("group1");
        registryService.unsubscribe("group1");
        assertEquals(list.size(), 1);
        InetSocketAddress inetSocketAddress = list.get(0);
        assertEquals(inetSocketAddress.getAddress().getHostAddress(), "127.0.0.1");
        assertEquals(inetSocketAddress.getPort(), 8088);


    }

    //    @Disabled
    @Test
    public void testSubscribe() throws Exception {
        NamingserverRegistryServiceImpl registryService = NamingserverRegistryServiceImpl.getInstance();

        AtomicBoolean isNotified = new AtomicBoolean(false);
        //1.subscribe
        registryService.subscribe(vGroup -> {
            try {
                isNotified.set(true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, "group2");

        //2.register
        InetSocketAddress inetSocketAddress = new InetSocketAddress("127.0.0.1", 8088);
        registryService.register(inetSocketAddress);
        String namespace = FILE_CONFIG.getConfig("registry.namingserver.namespace");
        createGroupInCluster(namespace, "group2", "cluster1");

        //3.check
        assertEquals(isNotified.get(), true);
        registryService.unsubscribe("group2");
    }


    @Test
    public void testUnsubscribe() throws Exception {
        NamingserverRegistryServiceImpl registryService = (NamingserverRegistryServiceImpl) new NamingserverRegistryProvider().provide();


        NamingListenerimpl namingListenerimpl = new NamingListenerimpl();

        //1.subscribe
        registryService.subscribe(namingListenerimpl, "group1");

        //2.register
        InetSocketAddress inetSocketAddress = new InetSocketAddress("127.0.0.1", 8088);
        registryService.register(inetSocketAddress);
        String namespace = FILE_CONFIG.getConfig("registry.namingserver.namespace");
        createGroupInCluster(namespace, "group1", "cluster1");

        //3.check
        assertEquals(namingListenerimpl.isNotified, true);
        namingListenerimpl.setNotified(false);

        //4.unsubscribe
        registryService.unsubscribe(namingListenerimpl, "group1");

        //5.unregister

        registryService.unregister(inetSocketAddress);
        //5.check
        assertEquals(namingListenerimpl.isNotified, false);


    }


    public void createGroupInCluster(String namespace, String vGroup, String clusterName) throws Exception {
        Map<String, String> paraMap = new HashMap<>();
        paraMap.put("namespace", namespace);
        paraMap.put("vGroup", vGroup);
        paraMap.put("clusterName", clusterName);
        String url = "http://127.0.0.1:8080/naming/v1/createGroup";
        Map<String, String> header = new HashMap<>();
        header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        try {
            CloseableHttpResponse response = HttpClientUtil.doGet(url, paraMap, header, 30000);
        } catch (Exception e) {
            throw new RemoteException();
        }
    }

    private class NamingListenerimpl implements NamingListener {

        public boolean isNotified = false;

        public boolean isNotified() {
            return isNotified;
        }

        public void setNotified(boolean notified) {
            isNotified = notified;
        }

        @Override
        public void onEvent(String vGroup) {
            isNotified = true;
        }
    }
};
