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
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.launcher.EtcdCluster;
import io.etcd.jetcd.launcher.EtcdClusterFactory;
import io.etcd.jetcd.options.DeleteOption;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.watch.WatchResponse;
import org.apache.seata.discovery.registry.RegistryService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.netty.util.CharsetUtil.UTF_8;
import static org.apache.seata.common.DefaultValues.DEFAULT_TX_GROUP;
import static org.assertj.core.api.Assertions.assertThat;

@Disabled
public class EtcdRegistryServiceImplTest {
    private static final String REGISTRY_KEY_PREFIX = "registry-seata-";
    private static final String CLUSTER_NAME = "default";
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8091;

    private static EtcdCluster etcd;
    private static Client client;
    private static List<URI> clientEndpoints;

    @BeforeAll
    public static void beforeAll() {
        etcd = EtcdClusterFactory.buildCluster(CLUSTER_NAME, 1, false);
        etcd.start();
        clientEndpoints = etcd.getClientEndpoints();
        client = Client.builder().endpoints(clientEndpoints).build();
    }

    @AfterAll
    public static void afterAll() {
        if (client != null) {
            client.close();
        }
        if (etcd != null) {
            etcd.close();
        }
        System.clearProperty(EtcdRegistryServiceImpl.TEST_ENDPONT);
    }

    @BeforeEach
    public void setUp() {
        String endpoint = clientEndpoints.get(0).toString();
        System.setProperty(EtcdRegistryServiceImpl.TEST_ENDPONT, endpoint);
    }

    @AfterEach
    public void tearDown() throws Exception {
        KV kvClient = client.getKVClient();
        ByteSequence keyPrefix = buildRegistryKeyPrefix();
        DeleteOption deleteOption =
                DeleteOption.newBuilder().withPrefix(keyPrefix).build();
        kvClient.delete(keyPrefix, deleteOption).get();
    }

    @Test
    public void testRegister() throws Exception {
        RegistryService registryService = new EtcdRegistryProvider().provide();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(HOST, PORT);
        // 1. Register the service instance.
        registryService.register(inetSocketAddress);
        // 2. Verify the registration by directly querying etcd.
        GetOption getOption =
                GetOption.newBuilder().withPrefix(buildRegistryKeyPrefix()).build();
        long count = client.getKVClient().get(buildRegistryKeyPrefix(), getOption).get().getKvs().stream()
                .filter(keyValue -> {
                    String[] instanceInfo = keyValue.getValue().toString(UTF_8).split(":");
                    return HOST.equals(instanceInfo[0]) && PORT == Integer.parseInt(instanceInfo[1]);
                })
                .count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    public void testUnregister() throws Exception {
        RegistryService registryService = new EtcdRegistryProvider().provide();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(HOST, PORT);
        // 1.register
        registryService.register(inetSocketAddress);
        // 2. Verify it was registered successfully.
        GetOption getOption =
                GetOption.newBuilder().withPrefix(buildRegistryKeyPrefix()).build();
        long count = client.getKVClient().get(buildRegistryKeyPrefix(), getOption).get().getKvs().stream()
                .filter(keyValue -> {
                    String[] instanceInfo = keyValue.getValue().toString(UTF_8).split(":");
                    return HOST.equals(instanceInfo[0]) && PORT == Integer.parseInt(instanceInfo[1]);
                })
                .count();
        assertThat(count).isEqualTo(1);
        // 3. Unregister the instance.
        registryService.unregister(inetSocketAddress);
        // 4. Verify it was successfully removed from etcd.
        count = client.getKVClient().get(buildRegistryKeyPrefix(), getOption).get().getKvs().stream()
                .filter(keyValue -> {
                    String[] instanceInfo = keyValue.getValue().toString(UTF_8).split(":");
                    return HOST.equals(instanceInfo[0]) && PORT == Integer.parseInt(instanceInfo[1]);
                })
                .count();
        assertThat(count).isEqualTo(0);
    }

    @Test
    public void testSubscribe() throws Exception {
        RegistryService registryService = new EtcdRegistryProvider().provide();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(HOST, PORT);
        // 1.register
        registryService.register(inetSocketAddress);
        // 2.subscribe
        EtcdListener etcdListener = new EtcdListener();
        registryService.subscribe(DEFAULT_TX_GROUP, etcdListener);
        // 3. Delete the instance key and verify the listener is notified.
        DeleteOption deleteOption =
                DeleteOption.newBuilder().withPrefix(buildRegistryKeyPrefix()).build();
        client.getKVClient().delete(buildRegistryKeyPrefix(), deleteOption).get();
        assertThat(etcdListener.isNotified()).isTrue();
    }

    @Test
    public void testUnsubscribe() throws Exception {
        RegistryService registryService = new EtcdRegistryProvider().provide();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(HOST, PORT);
        // 1.register
        registryService.register(inetSocketAddress);
        // 2.subscribe
        EtcdListener etcdListener = new EtcdListener();
        registryService.subscribe(DEFAULT_TX_GROUP, etcdListener);
        // 3.delete instance,see if the listener can be notified
        DeleteOption deleteOption =
                DeleteOption.newBuilder().withPrefix(buildRegistryKeyPrefix()).build();
        client.getKVClient().delete(buildRegistryKeyPrefix(), deleteOption).get();
        assertThat(etcdListener.isNotified()).isTrue();
        // 4.unsubscribe
        registryService.unsubscribe(DEFAULT_TX_GROUP, etcdListener);
        // 5.reset
        etcdListener.reset();
        // 6.put instance,the listener should not be notified
        client.getKVClient()
                .put(buildRegistryKeyPrefix(), ByteSequence.from("test", UTF_8))
                .get();
        assertThat(etcdListener.isNotified()).isFalse();
    }

    @Test
    public void testLookup() throws Exception {
        RegistryService registryService = new EtcdRegistryProvider().provide();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(HOST, PORT);
        // 1.register
        registryService.register(inetSocketAddress);
        // 2.lookup
        List<InetSocketAddress> inetSocketAddresses = registryService.lookup(DEFAULT_TX_GROUP);
        // 3.Verify that the correct instance is returned.
        assertThat(inetSocketAddresses).hasSize(1);
        assertThat(inetSocketAddresses.get(0).getAddress().getHostAddress()).isEqualTo(HOST);
        assertThat(inetSocketAddresses.get(0).getPort()).isEqualTo(PORT);
    }

    /**
     * Builds the etcd key prefix for a given service group.
     * The key prefix includes the transaction service group as is standard in Seata.
     * @return ByteSequence of the prefix
     */
    private ByteSequence buildRegistryKeyPrefix() {
        return ByteSequence.from(REGISTRY_KEY_PREFIX + DEFAULT_TX_GROUP, UTF_8);
    }

    /**
     * Listener implementation for testing subscription notifications.
     */
    private static class EtcdListener implements Watch.Listener {
        private volatile boolean notified = false;

        @Override
        public void onNext(WatchResponse response) {
            notified = true;
        }

        @Override
        public void onError(Throwable throwable) {
            // No-op for this test
        }

        @Override
        public void onCompleted() {
            // No-op for this test
        }

        /**
         * Waits for a short period to allow the async notification to arrive.
         * @return true if a notification was received.
         */
        public boolean isNotified() throws InterruptedException {
            // Give some time for the watch event to be processed
            TimeUnit.SECONDS.sleep(1);
            return notified;
        }

        /**
         * Resets the notification flag for subsequent assertions.
         */
        public void reset() {
            this.notified = false;
        }
    }
}
