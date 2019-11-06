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
package io.seata.discovery.registry.etcd3;


import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.lease.LeaseTimeToLiveResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.LeaseOption;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchResponse;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.NetUtil;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.discovery.registry.RegistryService;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.netty.util.CharsetUtil.UTF_8;

/**
 * @author xingfudeshi@gmail.com
 * @date 2019/04/18
 */
public class EtcdRegistryServiceImpl implements RegistryService<Watch.Listener> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EtcdRegistryServiceImpl.class);
    private static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;
    private static final String FILE_ROOT_REGISTRY = "registry";
    private static final String FILE_CONFIG_SPLIT_CHAR = ".";
    private static final String REGISTRY_TYPE = "etcd3";
    private static final String SERVER_ADDR_KEY = "serverAddr";
    private static final String REGISTRY_CLUSTER = "cluster";
    private static final String DEFAULT_CLUSTER_NAME = "default";
    private static final String REGISTRY_KEY_PREFIX = "registry-seata-";
    private static final String FILE_CONFIG_KEY_PREFIX = FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE + FILE_CONFIG_SPLIT_CHAR;
    private static final int MAP_INITIAL_CAPACITY = 8;
    private static final int THREAD_POOL_SIZE = 2;
    private ExecutorService executorService;
    /**
     * TTL for lease
     */
    private static final long TTL = 10;
    /**
     * interval for life keep
     */
    private final static long LIFE_KEEP_INTERVAL = 5;
    /**
     * critical value for life keep
     */
    private final static long LIFE_KEEP_CRITICAL = 6;
    private static volatile EtcdRegistryServiceImpl instance;
    private static volatile Client client;
    private ConcurrentMap<String, Pair<Long /*revision*/, List<InetSocketAddress>>> clusterAddressMap;
    private ConcurrentMap<String, Set<Watch.Listener>> listenerMap;
    private ConcurrentMap<String, EtcdWatcher> watcherMap;
    private static long leaseId = 0;
    private EtcdLifeKeeper lifeKeeper = null;
    private Future<Boolean> lifeKeeperFuture = null;
    /**
     * a endpoint for unit testing
     */
    public static final String TEST_ENDPONT = "etcd-test-lancher-endpoint";


    private EtcdRegistryServiceImpl() {
        clusterAddressMap = new ConcurrentHashMap<>(MAP_INITIAL_CAPACITY);
        listenerMap = new ConcurrentHashMap<>(MAP_INITIAL_CAPACITY);
        watcherMap = new ConcurrentHashMap<>(MAP_INITIAL_CAPACITY);
        executorService = new ThreadPoolExecutor(THREAD_POOL_SIZE, THREAD_POOL_SIZE, Integer.MAX_VALUE, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), new NamedThreadFactory("registry-etcd3", THREAD_POOL_SIZE));
    }

    /**
     * get etcd registry service instance
     *
     * @return instance
     */
    static EtcdRegistryServiceImpl getInstance() {
        if (null == instance) {
            synchronized (EtcdRegistryServiceImpl.class) {
                if (null == instance) {
                    instance = new EtcdRegistryServiceImpl();
                }
            }
        }
        return instance;
    }


    @Override
    public void register(InetSocketAddress address) throws Exception {
        NetUtil.validAddress(address);
        doRegister(address);
    }

    /**
     * do registry
     *
     * @param address
     */
    private void doRegister(InetSocketAddress address) throws Exception {
        PutOption putOption = PutOption.newBuilder().withLeaseId(getLeaseId()).build();
        getClient().getKVClient().put(buildRegistryKey(address), buildRegistryValue(address), putOption).get();
    }


    @Override
    public void unregister(InetSocketAddress address) throws Exception {
        NetUtil.validAddress(address);
        doUnregister(address);
    }

    /**
     * do unregister
     *
     * @param address
     * @throws Exception
     */
    private void doUnregister(InetSocketAddress address) throws Exception {
        getClient().getKVClient().delete(buildRegistryKey(address)).get();
    }

    @Override
    public void subscribe(String cluster, Watch.Listener listener) throws Exception {
        listenerMap.putIfAbsent(cluster, new HashSet<>());
        listenerMap.get(cluster).add(listener);
        EtcdWatcher watcher = watcherMap.computeIfAbsent(cluster, w -> new EtcdWatcher(cluster, listener));
        executorService.submit(watcher);
    }

    @Override
    public void unsubscribe(String cluster, Watch.Listener listener) throws Exception {
        Set<Watch.Listener> subscribeSet = listenerMap.get(cluster);
        if (null != subscribeSet) {
            Set<Watch.Listener> newSubscribeSet = new HashSet<>();
            for (Watch.Listener eventListener : subscribeSet) {
                if (!eventListener.equals(listener)) {
                    newSubscribeSet.add(eventListener);
                }
            }
            listenerMap.put(cluster, newSubscribeSet);
        }
        watcherMap.remove(cluster).stop();


    }

    @Override
    public List<InetSocketAddress> lookup(String key) throws Exception {
        final String cluster = getServiceGroup(key);
        if (null == cluster) {
            return null;
        }
        if (!listenerMap.containsKey(cluster)) {
            //1.refresh
            refreshCluster(cluster);
            //2.subscribe
            subscribe(cluster, new Watch.Listener() {
                @Override
                public void onNext(WatchResponse response) {
                    try {
                        refreshCluster(cluster);
                    } catch (Exception e) {
                        LOGGER.error("etcd watch listener", e);
                        throw new RuntimeException(e.getMessage());
                    }
                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onCompleted() {

                }
            });

        }
        return clusterAddressMap.get(cluster).getValue();
    }

    @Override
    public void close() throws Exception {
        if (null != lifeKeeper) {
            lifeKeeper.stop();
            if (null != lifeKeeperFuture) {
                lifeKeeperFuture.get(3, TimeUnit.SECONDS);
            }
        }

    }

    /**
     * refresh cluster
     *
     * @param cluster
     * @throws Exception
     */
    private void refreshCluster(String cluster) throws Exception {
        if (null == cluster) {
            return;
        }
        //1.get all available registries
        GetOption getOption = GetOption.newBuilder().withPrefix(buildRegistryKeyPrefix()).build();
        GetResponse getResponse = getClient().getKVClient().get(buildRegistryKeyPrefix(), getOption).get();
        //2.add to list
        List<InetSocketAddress> instanceList = getResponse.getKvs().stream().map(keyValue -> {
            String[] instanceInfo = keyValue.getValue().toString(UTF_8).split(":");
            return new InetSocketAddress(instanceInfo[0], Integer.parseInt(instanceInfo[1]));
        }).collect(Collectors.toList());
        clusterAddressMap.put(cluster, new Pair<>(getResponse.getHeader().getRevision(), instanceList));
    }

    /**
     * get client
     *
     * @return client
     */
    private Client getClient() {
        if (null == client) {
            synchronized (EtcdRegistryServiceImpl.class) {
                if (null == client) {
                    String testEndpoint = System.getProperty(TEST_ENDPONT);
                    if (StringUtils.isNotBlank(testEndpoint)) {
                        client = Client.builder().endpoints(testEndpoint).build();
                    } else {
                        client = Client.builder().endpoints(FILE_CONFIG.getConfig(FILE_CONFIG_KEY_PREFIX + SERVER_ADDR_KEY)).build();
                    }
                }
            }
        }
        return client;
    }

    /**
     * get service group
     *
     * @param key
     * @return clusterNameKey
     */
    private String getServiceGroup(String key) {
        String clusterNameKey = PREFIX_SERVICE_ROOT + CONFIG_SPLIT_CHAR + PREFIX_SERVICE_MAPPING + key;
        return ConfigurationFactory.getInstance().getConfig(clusterNameKey);
    }

    /**
     * get cluster name
     *
     * @return
     */
    private String getClusterName() {
        String clusterConfigName = FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE + FILE_CONFIG_SPLIT_CHAR
            + REGISTRY_CLUSTER;
        return FILE_CONFIG.getConfig(clusterConfigName, DEFAULT_CLUSTER_NAME);
    }

    /**
     * create a new lease id or return a existing lease id
     */
    private long getLeaseId() throws Exception {
        if (0 == leaseId) {
            //create a new lease
            leaseId = getClient().getLeaseClient().grant(TTL).get().getID();
            lifeKeeper = new EtcdLifeKeeper(leaseId);
            lifeKeeperFuture = executorService.submit(lifeKeeper);
        }
        return leaseId;
    }

    /**
     * build registry key
     *
     * @return registry key
     */
    private ByteSequence buildRegistryKey(InetSocketAddress address) {
        return ByteSequence.from(REGISTRY_KEY_PREFIX + getClusterName() + "-" + NetUtil.toStringAddress(address), UTF_8);
    }

    /**
     * build registry key prefix
     *
     * @return registry key prefix
     */
    private ByteSequence buildRegistryKeyPrefix() {
        return ByteSequence.from(REGISTRY_KEY_PREFIX + getClusterName(), UTF_8);
    }

    /**
     * build registry value
     *
     * @param address
     * @return registry value
     */
    private ByteSequence buildRegistryValue(InetSocketAddress address) {
        return ByteSequence.from(NetUtil.toStringAddress(address), UTF_8);
    }

    /**
     * the type etcd life keeper
     */
    private class EtcdLifeKeeper implements Callable<Boolean> {
        private final long leaseId;
        private final Lease leaseClient;
        private boolean running;


        public EtcdLifeKeeper(long leaseId) {
            this.leaseClient = getClient().getLeaseClient();
            this.leaseId = leaseId;
            this.running = true;

        }

        /**
         * process
         */
        private void process() {
            for (; ; ) {
                try {
                    //1.get TTL
                    LeaseTimeToLiveResponse leaseTimeToLiveResponse = this.leaseClient.timeToLive(this.leaseId, LeaseOption.DEFAULT).get();
                    final long tTl = leaseTimeToLiveResponse.getTTl();
                    if (tTl <= LIFE_KEEP_CRITICAL) {
                        //2.refresh the TTL
                        this.leaseClient.keepAliveOnce(this.leaseId).get();
                    }
                    TimeUnit.SECONDS.sleep(LIFE_KEEP_INTERVAL);
                } catch (Exception e) {
                    LOGGER.error("EtcdLifeKeeper", e);
                    throw new ShouldNeverHappenException("failed to renewal the lease.");
                }
            }
        }

        /**
         * stop this task
         */
        public void stop() {
            this.running = false;
        }

        @Override
        public Boolean call() {
            if (this.running) {
                process();
            }
            return this.running;
        }
    }

    /**
     * the type etcd watcher
     */
    private class EtcdWatcher implements Runnable {
        private final Watch.Listener listener;
        private Watch.Watcher watcher;
        private String cluster;

        public EtcdWatcher(String cluster, Watch.Listener listener) {
            this.cluster = cluster;
            this.listener = listener;
        }

        @Override
        public void run() {
            Watch watchClient = getClient().getWatchClient();
            WatchOption.Builder watchOptionBuilder = WatchOption.newBuilder().withPrefix(buildRegistryKeyPrefix());
            Pair<Long /*revision*/, List<InetSocketAddress>> addressPair = clusterAddressMap.get(cluster);
            if (Objects.nonNull(addressPair)) {
                // Maybe addressPair isn't newest now, but it's ok
                watchOptionBuilder.withRevision(addressPair.getKey());
            }
            this.watcher = watchClient.watch(buildRegistryKeyPrefix(), watchOptionBuilder.build(), this.listener);
        }

        /**
         * stop this task
         */
        public void stop() {
            this.watcher.close();
        }
    }

    private static class Pair<K,V> {

        /**
         * Key of this <code>Pair</code>.
         */
        private K key;

        /**
         * Value of this this <code>Pair</code>.
         */
        private V value;

        /**
         * Creates a new pair
         * @param key The key for this pair
         * @param value The value to use for this pair
         */
        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }

        /**
         * Gets the key for this pair.
         * @return key for this pair
         */
        public K getKey() { return key; }

        /**
         * Gets the value for this pair.
         * @return value for this pair
         */
        public V getValue() { return value; }
    }
}
