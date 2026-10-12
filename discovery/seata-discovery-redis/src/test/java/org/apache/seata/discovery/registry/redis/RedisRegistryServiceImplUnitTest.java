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

import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.config.exception.ConfigNotFoundException;
import org.apache.seata.discovery.registry.RegistryHeartBeats;
import org.apache.seata.discovery.registry.RegistryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import redis.clients.jedis.*;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RedisRegistryServiceImplUnitTest {
    private static final String GROUP = "redis-unit-group";
    private final Map<String, String> properties = new HashMap<>();
    private RedisRegistryServiceImpl service;
    private Jedis jedis;
    private Pipeline pipeline;
    private ScheduledExecutorService subscriptions;
    private ScheduledExecutorService updates;
    private MockedConstruction<JedisPool> pools;
    private MockedStatic<RegistryHeartBeats> heartbeats;
    private MockedStatic<ConfigurationFactory> configurations;
    private Configuration configuration;

    private final Map<String, Object> savedState = new HashMap<>();
    private final Map<String, Map> savedMaps = new HashMap<>();

    @BeforeEach
    void setUp() throws Exception {
        properties.put("config.type", System.getProperty("config.type"));
        System.setProperty("config.type", "file");
        properties.put("config.file.name", System.getProperty("config.file.name"));
        System.setProperty("config.file.name", "file.conf");
        property("serverAddr", "127.0.0.1:6379");
        property("cluster", "unit");
        property("db", "2");
        property("password", "");
        for (String key : Arrays.asList(
                "max-idle",
                "min-idle",
                "max-active",
                "max-total",
                "max-wait",
                "num-tests-per-eviction-run",
                "time-between-eviction-runs-millis",
                "min-evictable-idle-time-millis")) {
            property(key, "10");
        }
        {
            Field field = RedisRegistryServiceImpl.class.getDeclaredField("instance");
            field.setAccessible(true);
            savedState.put("instance", field.get(null));
        }
        {
            Field field = RedisRegistryServiceImpl.class.getDeclaredField("jedisPool");
            field.setAccessible(true);
            savedState.put("jedisPool", field.get(null));
        }
        for (String key : Arrays.asList("LISTENER_SERVICE_MAP", "CLUSTER_ADDRESS_MAP")) {
            savedMaps.put(key, new HashMap(map(key)));
        }
        set("instance", null);
        map("LISTENER_SERVICE_MAP").clear();
        map("CLUSTER_ADDRESS_MAP").clear();
        jedis = mock(Jedis.class);
        pipeline = mock(Pipeline.class);
        when(jedis.pipelined()).thenReturn(pipeline);
        when(jedis.scan(anyString(), any(ScanParams.class))).thenReturn(new ScanResult<>("0", Collections.emptyList()));
        pools = mockConstruction(
                JedisPool.class, (pool, context) -> when(pool.getResource()).thenReturn(jedis));
        heartbeats = mockStatic(RegistryHeartBeats.class);
        configuration = mock(Configuration.class);
        configurations = mockStatic(ConfigurationFactory.class);
        configurations.when(ConfigurationFactory::getInstance).thenReturn(configuration);
        when(configuration.getConfig("service.vgroupMapping." + GROUP)).thenReturn("unit");
        service = RedisRegistryServiceImpl.getInstance();
        subscriptions = mock(ScheduledExecutorService.class);
        updates = mock(ScheduledExecutorService.class);
        replaceExecutor("threadPoolExecutorForSubscribe", subscriptions);
        replaceExecutor("threadPoolExecutorForUpdateMap", updates);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (service != null) {
            service.close();
        }
        set("instance", null);
        set("jedisPool", null);
        map("LISTENER_SERVICE_MAP").clear();
        map("CLUSTER_ADDRESS_MAP").clear();
        for (Map.Entry<String, Object> entry : savedState.entrySet()) {
            set(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Map> entry : savedMaps.entrySet()) {
            map(entry.getKey()).putAll(entry.getValue());
        }
        RegistryService.CURRENT_ADDRESS_MAP.remove(GROUP);
        RegistryService.SERVICE_GROUP_NAME.remove("service.vgroupMapping." + GROUP);
        if (configurations != null) {
            configurations.close();
        }
        if (heartbeats != null) {
            heartbeats.close();
        }
        if (pools != null) {
            pools.close();
        }
        properties.forEach((key, value) -> {
            if (value == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, value);
            }
        });
    }

    @Test
    void registersRefreshesAndUnregistersWithExpectedRedisCommands() throws Exception {
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8091);
        assertSame(service, RedisRegistryServiceImpl.getInstance());
        service.register(address);
        verify(pipeline).setex(eq("registry.redis.unit_127.0.0.1:8091"), eq(5L), anyString());
        verify(pipeline).publish("registry.redis.unit", "127.0.0.1:8091-register");
        ArgumentCaptor<RegistryHeartBeats.ReRegister> callback =
                ArgumentCaptor.forClass(RegistryHeartBeats.ReRegister.class);
        heartbeats.verify(
                () -> RegistryHeartBeats.addHeartBeat(eq("redis"), eq(address), eq(2000L), callback.capture()));
        callback.getValue().register(address);
        verify(pipeline, times(2)).setex(eq("registry.redis.unit_127.0.0.1:8091"), eq(5L), anyString());
        verify(pipeline, times(1)).publish("registry.redis.unit", "127.0.0.1:8091-register");
        service.unregister(address);
        verify(pipeline).hdel("registry.redis.unit", "127.0.0.1:8091");
        verify(pipeline).publish("registry.redis.unit", "127.0.0.1:8091-unregister");
        verify(pipeline, times(3)).sync();
        verify(pipeline, times(3)).close();
        verify(jedis, times(3)).close();
    }

    @Test
    void rejectsMissingGroupAndInvalidAddresses() {
        when(configuration.getConfig("service.vgroupMapping." + GROUP)).thenReturn(null);
        assertThrows(ConfigNotFoundException.class, () -> service.lookup(GROUP));
        assertThrows(IllegalArgumentException.class, () -> service.register(new InetSocketAddress("127.0.0.1", 0)));
        assertThrows(IllegalArgumentException.class, () -> service.unregister(new InetSocketAddress("127.0.0.1", 0)));
        verifyNoInteractions(jedis);
    }

    @Test
    void scansAllPagesAndProcessesNotificationsWithEmptyProtection() throws Exception {
        when(jedis.scan(eq("0"), any(ScanParams.class)))
                .thenReturn(new ScanResult<>("17", Collections.singletonList("registry.redis.unit_127.0.0.1:8091")));
        when(jedis.scan(eq("17"), any(ScanParams.class)))
                .thenReturn(new ScanResult<>("0", Collections.singletonList("registry.redis.unit_127.0.0.2:8091")));
        assertEquals(2, service.lookup(GROUP).size());
        verify(jedis).scan(eq("17"), any(ScanParams.class));
        RedisListener listener = listeners().get("unit").get(0);
        listener.onEvent("127.0.0.2:8091-unregister");
        assertEquals(Collections.singletonList(new InetSocketAddress("127.0.0.1", 8091)), service.lookup(GROUP));
        when(configuration.getConfig("txServiceGroup")).thenReturn(GROUP);
        listener.onEvent("127.0.0.1:8091-unregister");
        assertEquals(1, service.lookup(GROUP).size());
        when(configuration.getConfig("service.vgroupMapping." + GROUP)).thenReturn("other");
        listener.onEvent("127.0.0.1:8091-unregister");
        assertTrue(service.lookupByCluster("unit").isEmpty());
        listener.onEvent("127.0.0.3:8091-register");
        assertEquals(
                Collections.singletonList(new InetSocketAddress("127.0.0.3", 8091)), service.lookupByCluster("unit"));
        when(configuration.getConfig("txServiceGroup")).thenReturn("");
        listener.onEvent("127.0.0.3:8091-unregister");
        assertTrue(service.lookupByCluster("unit").isEmpty());
        assertThrows(ShouldNeverHappenException.class, () -> listener.onEvent("127.0.0.3:8091-unknown"));
        verify(subscriptions, times(1))
                .scheduleAtFixedRate(any(Runnable.class), eq(0L), eq(1L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void scheduledRefreshPreservesLastKnownAddressesAndDispatchesDespiteListenerFailure() throws Exception {
        service.lookup(GROUP);
        ArgumentCaptor<Runnable> refresh = ArgumentCaptor.forClass(Runnable.class);
        verify(updates).scheduleAtFixedRate(refresh.capture(), eq(0L), eq(2000L), eq(TimeUnit.MILLISECONDS));
        when(jedis.scan(anyString(), any(ScanParams.class)))
                .thenReturn(new ScanResult<>("0", Collections.singletonList("registry.redis.unit_127.0.0.1:8091")))
                .thenReturn(new ScanResult<>("0", Collections.singletonList("registry.redis.unit_127.0.0.1:8091")))
                .thenReturn(new ScanResult<>("0", Collections.emptyList()))
                .thenThrow(new IllegalStateException("redis unavailable"));
        for (int i = 0; i < 4; i++) {
            refresh.getValue().run();
        }
        assertEquals(1, service.lookup(GROUP).size());
        RedisListener failing = mock(RedisListener.class);
        RedisListener following = mock(RedisListener.class);
        doThrow(new IllegalStateException("listener failed")).when(failing).onEvent(anyString());
        listeners().get("unit").add(failing);
        listeners().get("unit").add(following);
        ArgumentCaptor<Runnable> subscribe = ArgumentCaptor.forClass(Runnable.class);
        verify(subscriptions).scheduleAtFixedRate(subscribe.capture(), eq(0L), eq(1L), eq(TimeUnit.MILLISECONDS));
        subscribe.getValue().run();
        ArgumentCaptor<JedisPubSub> pubSub = ArgumentCaptor.forClass(JedisPubSub.class);
        verify(jedis).subscribe(pubSub.capture(), eq("registry.redis.unit"));
        pubSub.getValue().onMessage("registry.redis.unit", "127.0.0.2:8091-register");
        verify(following).onEvent("127.0.0.2:8091-register");
        assertEquals(2, service.lookup(GROUP).size());
        doThrow(new IllegalStateException("disconnected")).when(jedis).subscribe(any(JedisPubSub.class), anyString());
        assertDoesNotThrow(subscribe.getValue()::run);
        service.unsubscribe("unit", following);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void closesExecutorsOnTerminationTimeoutOrInterruption(int mode) throws Exception {
        for (ScheduledExecutorService executor : Arrays.asList(subscriptions, updates)) {
            if (mode == 2) {
                when(executor.awaitTermination(5, TimeUnit.SECONDS)).thenThrow(new InterruptedException());
            } else {
                when(executor.awaitTermination(5, TimeUnit.SECONDS)).thenReturn(mode == 0);
            }
        }
        service.close();
        for (ScheduledExecutorService executor : Arrays.asList(subscriptions, updates)) {
            verify(executor).shutdown();
            verify(executor, times(mode == 0 ? 0 : 1)).shutdownNow();
        }
        verify(pools.constructed().get(0)).destroy();
        heartbeats.verify(() -> RegistryHeartBeats.close("redis"));
    }

    @Test
    void acceptsPasswordAndDefaultPoolSettings() throws Exception {
        service.close();
        property("password", "secret");
        for (String key : new ArrayList<>(properties.keySet())) {
            if (key.startsWith("registry.redis.")
                    && !key.endsWith("password")
                    && !key.endsWith("serverAddr")
                    && !key.endsWith("cluster")) {
                System.setProperty(key, "0");
            }
        }
        set("instance", null);
        service = RedisRegistryServiceImpl.getInstance();
        assertNotNull(service);
        assertEquals(2, pools.constructed().size());
    }

    private void property(String key, String value) {
        key = "registry.redis." + key;
        if (!properties.containsKey(key)) {
            properties.put(key, System.getProperty(key));
        }
        System.setProperty(key, value);
    }

    private void replaceExecutor(String name, ScheduledExecutorService replacement) throws Exception {
        Field field = RedisRegistryServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        ((ScheduledExecutorService) field.get(service)).shutdownNow();
        field.set(service, replacement);
    }

    private static void set(String name, Object value) throws Exception {
        Field field = RedisRegistryServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    private static Map map(String name) throws Exception {
        Field field = RedisRegistryServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        return (Map) field.get(null);
    }

    private Map<String, List<RedisListener>> listeners() throws Exception {
        return map("LISTENER_SERVICE_MAP");
    }
}
