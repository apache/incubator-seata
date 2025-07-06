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
package org.apache.seata.discovery.registry.etcd3;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.api.RangeResponse;
import io.etcd.jetcd.api.ResponseHeader;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.lease.LeaseTimeToLiveResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.options.WatchOption;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.config.exception.ConfigNotFoundException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.seata.common.DefaultValues.DEFAULT_TX_GROUP;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EtcdRegistryServiceImplMockTest {

    @Mock
    private Client mockClient;

    @Mock
    private KV mockKVClient;

    @Mock
    private Lease mockLeaseClient;

    @Mock
    private Watch mockWatchClient;

    @Mock
    private Watch.Watcher mockWatcher;

    @Mock
    Configuration configuration;

    private EtcdRegistryServiceImpl registryService;
    private ExecutorService executorService;

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8091;
    private static final String CLUSTER_NAME = "default";

    @BeforeEach
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        MockitoAnnotations.openMocks(this);
        registryService = (EtcdRegistryServiceImpl) spy(new EtcdRegistryProvider().provide());

        // mock client
        when(mockClient.getLeaseClient()).thenReturn(mockLeaseClient);
        when(mockClient.getWatchClient()).thenReturn(mockWatchClient);
        when(mockClient.getKVClient()).thenReturn(mockKVClient);

        // inject spy executorService
        Field executorServiceField = EtcdRegistryServiceImpl.class.getDeclaredField("executorService");
        executorServiceField.setAccessible(true);
        executorService = spy((ExecutorService) executorServiceField.get(registryService));
        executorServiceField.set(registryService, executorService);

        // inject mock client
        Field clientField = EtcdRegistryServiceImpl.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(registryService, mockClient);
    }

    @BeforeAll
    public static void beforeClass() {
        String endPoint = String.format("http://%s:%s", HOST, PORT);
        System.setProperty(EtcdRegistryServiceImpl.TEST_ENDPONT, endPoint);
    }

    @AfterAll
    public static void afterClass() {
        System.setProperty(EtcdRegistryServiceImpl.TEST_ENDPONT, "");
    }

    @Order(1)
    @Test
    public void testRegister() throws Exception {
        long leaseId = 1L;
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8091);

        // Mock lease grant response
        LeaseGrantResponse leaseGrantResponse = mock(LeaseGrantResponse.class);
        when(leaseGrantResponse.getID()).thenReturn(leaseId);
        when(mockLeaseClient.grant(anyLong())).thenReturn(CompletableFuture.completedFuture(leaseGrantResponse));

        // Mock put response
        when(mockKVClient.put(any(), any(), any(PutOption.class))).thenReturn(CompletableFuture.completedFuture(null));

        // timeToLive response
        io.etcd.jetcd.api.LeaseTimeToLiveResponse timeToLiveResponseApi =
                io.etcd.jetcd.api.LeaseTimeToLiveResponse.newBuilder()
                        .setID(leaseId)
                        .setTTL(6)
                        .build();
        when(mockLeaseClient.timeToLive(eq(leaseId), any()))
                .thenReturn(CompletableFuture.completedFuture(new LeaseTimeToLiveResponse(timeToLiveResponseApi)));

        // keepAlive response
        io.etcd.jetcd.api.LeaseKeepAliveResponse leaseKeepAliveResponse =
                io.etcd.jetcd.api.LeaseKeepAliveResponse.newBuilder().build();
        when(mockLeaseClient.keepAliveOnce(eq(leaseId)))
                .thenReturn(CompletableFuture.completedFuture(new LeaseKeepAliveResponse(leaseKeepAliveResponse)));

        // Act
        registryService.register(address);

        // verify the method to register the new service is called
        verify(mockKVClient, times(1)).put(any(), any(), any(PutOption.class));

        // verify lifeKeeper task is submitted
        verify(executorService, times(1)).submit(any(Callable.class));
    }

    @Order(2)
    @Test
    public void testUnregister() throws Exception {
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8091);

        // Mock delete response
        when(mockKVClient.delete(any())).thenReturn(CompletableFuture.completedFuture(null));

        // Act
        registryService.unregister(address);

        // Verify
        verify(mockKVClient, times(1)).delete(any());
    }

    @Order(3)
    @Test
    public void testLookup() throws Exception {
        List<String> services = Arrays.asList("127.0.0.1:8091", "127.0.0.1:8092", "127.0.0.1:8093");
        GetResponse mockGetResponse = createMockGetResponse(services);
        when(mockKVClient.get(any(ByteSequence.class), any(GetOption.class)))
                .thenReturn(CompletableFuture.completedFuture(mockGetResponse));

        try (MockedStatic<ConfigurationFactory> mockConfig = Mockito.mockStatic(ConfigurationFactory.class)) {
            // 1. run success case
            mockConfig.when(ConfigurationFactory::getInstance).thenReturn(configuration);
            when(configuration.getConfig("service.vgroupMapping.default_tx_group"))
                    .thenReturn(CLUSTER_NAME);
            List<InetSocketAddress> lookup = registryService.lookup(DEFAULT_TX_GROUP);
            List<String> lookupServices = lookup.stream()
                    .map(address -> address.getHostString() + ":" + address.getPort())
                    .collect(Collectors.toList());

            // assert
            assertEquals(lookupServices, services);

            // 2. config not found case
            when(configuration.getConfig(any())).thenReturn(null);
            Assertions.assertThrows(ConfigNotFoundException.class, () -> {
                registryService.lookup(DEFAULT_TX_GROUP);
            });
        }
    }

    @Order(4)
    @Test
    public void testSubscribe() throws Exception {
        Watch.Listener mockListener = mock(Watch.Listener.class);
        registryService.subscribe(CLUSTER_NAME, mockListener);

        // verify watcher task is submitted
        verify(executorService, times(1)).submit(any(Runnable.class));
    }

    @Order(5)
    @Test
    public void testUnsubscribe() throws Exception {
        Watch.Listener mockListener = mock(Watch.Listener.class);
        CountDownLatch latch = new CountDownLatch(1);

        when(mockWatchClient.watch(any(), any(WatchOption.class), any(Watch.Listener.class)))
                .thenAnswer(invocation -> {
                    latch.countDown();
                    return mockWatcher;
                });

        registryService.subscribe(DEFAULT_TX_GROUP, mockListener);
        latch.await(1, TimeUnit.SECONDS);

        registryService.unsubscribe(DEFAULT_TX_GROUP, mockListener);
        assertEquals(0, latch.getCount(), "Latch should be 0");
    }

    @Order(6)
    @Test
    public void testClose() throws Exception {
        // 1.condition: executorService shutdown with exception
        when(executorService.isShutdown()).thenReturn(false);
        when(executorService.awaitTermination(5, TimeUnit.SECONDS))
                .thenThrow(new InterruptedException("Test interruption"));
        registryService.close();

        verify(executorService).shutdown();
        verify(executorService).shutdownNow();
        verify(mockClient).close();

        Mockito.reset(executorService);
        Field executorServiceField = EtcdRegistryServiceImpl.class.getDeclaredField("executorService");
        executorServiceField.setAccessible(true);
        executorServiceField.set(registryService, executorService);

        // 2.condition: executorService normal shutdown
        when(executorService.isShutdown()).thenReturn(false);
        when(executorService.awaitTermination(5, TimeUnit.SECONDS)).thenReturn(false);
        registryService.close();

        Field clientField = EtcdRegistryServiceImpl.class.getDeclaredField("client");
        clientField.setAccessible(true);
        assertNull(clientField.get(null));
        assertNull(executorServiceField.get(registryService));
    }

    private GetResponse createMockGetResponse(List<String> addresses) {
        // Create mock ResponseHeader
        ResponseHeader mockHeader =
                ResponseHeader.newBuilder().setRevision(12345L).build();

        // Create mock KeyValue list
        List<KeyValue> mockKeyValues = addresses.stream()
                .map(address -> {
                    KeyValue mockKeyValue = mock(KeyValue.class);
                    when(mockKeyValue.getValue()).thenReturn(ByteSequence.from(address, UTF_8));
                    return mockKeyValue;
                })
                .collect(Collectors.toList());

        // Create mock RangeResponse
        RangeResponse mockRangeResponse =
                RangeResponse.newBuilder().setHeader(mockHeader).build();

        // Create mock GetResponse
        GetResponse mockGetResponse = spy(new GetResponse(mockRangeResponse, ByteSequence.EMPTY));
        when(mockGetResponse.getKvs()).thenReturn(mockKeyValues);
        return mockGetResponse;
    }
}
