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
package io.seata.config.zk;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.seata.common.exception.NotSupportYetException;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.StringUtils;

import io.seata.config.AbstractConfiguration;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.config.ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR;

/**
 * @author crazier.huang
 * @date 2019/2/18
 */
public class ZookeeperConfiguration extends AbstractConfiguration<IZkDataListener> {
    private final static Logger LOGGER = LoggerFactory.getLogger(ZookeeperConfiguration.class);

    private static final String REGISTRY_TYPE = "zk";
    private static final String ZK_PATH_SPLIT_CHAR = "/";
    private static final String FILE_ROOT_CONFIG = "config";
    private static final String ROOT_PATH = ZK_PATH_SPLIT_CHAR + FILE_ROOT_CONFIG;
    private static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;
    private static final String SERVER_ADDR_KEY = "serverAddr";
    private static final String SESSION_TIMEOUT_KEY = "session.timeout";
    private static final String CONNECT_TIMEOUT_KEY = "connect.timeout";
    private static final int THREAD_POOL_NUM = 1;
    private static final int DEFAULT_SESSION_TIMEOUT = 6000;
    private static final int DEFAULT_CONNECT_TIMEOUT = 2000;
    private static final String FILE_CONFIG_KEY_PREFIX = FILE_ROOT_CONFIG + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE
        + FILE_CONFIG_SPLIT_CHAR;
    private static final ExecutorService CONFIG_EXECUTOR = new ThreadPoolExecutor(THREAD_POOL_NUM, THREAD_POOL_NUM,
        Integer.MAX_VALUE, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
        new NamedThreadFactory("ZKConfigThread", THREAD_POOL_NUM));
    private static volatile ZkClient zkClient;

    public ZookeeperConfiguration() {
        if (zkClient == null) {
            synchronized (ZookeeperConfiguration.class) {
                if (null == zkClient) {
                    zkClient = new ZkClient(FILE_CONFIG.getConfig(FILE_CONFIG_KEY_PREFIX + SERVER_ADDR_KEY),
                        FILE_CONFIG.getInt(FILE_CONFIG_KEY_PREFIX + SESSION_TIMEOUT_KEY, DEFAULT_SESSION_TIMEOUT),
                        FILE_CONFIG.getInt(FILE_CONFIG_KEY_PREFIX + CONNECT_TIMEOUT_KEY, DEFAULT_CONNECT_TIMEOUT));
                }
            }
            if (!zkClient.exists(ROOT_PATH)) {
                zkClient.createPersistent(ROOT_PATH, true);
            }
        }
    }

    @Override
    public String getTypeName() {
        return REGISTRY_TYPE;
    }

    @Override
    public String getConfig(String dataId, String defaultValue, long timeoutMills) {
        FutureTask<String> future = new FutureTask<String>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                String path = ROOT_PATH + ZK_PATH_SPLIT_CHAR + dataId;
                String value = zkClient.readData(path);
                if (StringUtils.isNullOrEmpty(value)) {
                    return defaultValue;
                }
                return value;
            }
        });
        CONFIG_EXECUTOR.execute(future);
        try {
            return future.get(timeoutMills, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            LOGGER.error("getConfig {} is error or timeout,return defaultValue {}", dataId, defaultValue);
            return defaultValue;
        }
    }

    @Override
    public boolean putConfig(String dataId, String content, long timeoutMills) {
        FutureTask<Boolean> future = new FutureTask<Boolean>(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                String path = ROOT_PATH + ZK_PATH_SPLIT_CHAR + dataId;
                if (!zkClient.exists(path)) {
                    zkClient.create(path, content, CreateMode.PERSISTENT);
                } else {
                    zkClient.writeData(path, content);
                }
                return true;
            }
        });
        CONFIG_EXECUTOR.execute(future);
        try {
            return future.get(timeoutMills, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            LOGGER.warn("putConfig {} : {} is error or timeout", dataId, content);
            return false;
        }
    }

    @Override
    public boolean putConfigIfAbsent(String dataId, String content, long timeoutMills) {
        throw new NotSupportYetException("not support atomic operation putConfigIfAbsent");
    }

    @Override
    public boolean removeConfig(String dataId, long timeoutMills) {
        FutureTask<Boolean> future = new FutureTask<Boolean>(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                String path = ROOT_PATH + ZK_PATH_SPLIT_CHAR + dataId;
                return zkClient.delete(path);
            }
        });
        CONFIG_EXECUTOR.execute(future);
        try {
            return future.get(timeoutMills, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            LOGGER.warn("removeConfig {} is error or timeout", dataId);
            return false;
        }

    }

    @Override
    public void addConfigListener(String dataId, IZkDataListener listener) {
        String path = ROOT_PATH + ZK_PATH_SPLIT_CHAR + dataId;
        if (zkClient.exists(path)) {
            zkClient.subscribeDataChanges(path, listener);
        }
    }

    @Override
    public void removeConfigListener(String dataId, IZkDataListener listener) {
        String path = ROOT_PATH + ZK_PATH_SPLIT_CHAR + dataId;
        if (zkClient.exists(path)) {
            zkClient.unsubscribeDataChanges(path, listener);
        }
    }

    @Override
    public List<IZkDataListener> getConfigListeners(String dataId) {
        throw new NotSupportYetException("not support getConfigListeners");
    }

}
