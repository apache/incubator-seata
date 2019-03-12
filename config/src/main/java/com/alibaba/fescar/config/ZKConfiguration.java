package com.alibaba.fescar.config;

import com.alibaba.fescar.common.exception.NotSupportYetException;
import com.alibaba.fescar.common.util.StringUtils;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import static com.alibaba.fescar.config.ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR;
import static com.alibaba.fescar.config.ConfigurationKeys.FILE_ROOT_REGISTRY;

/**
 * @author crazier.huang
 * @date 2019/2/18
 */
public class ZKConfiguration extends AbstractConfiguration<IZkDataListener> {
    private final static Logger LOGGER = LoggerFactory.getLogger(ZKConfiguration.class);

    private static final String REGISTRY_TYPE = "zk";
    private static final String ZK_PATH_SPLIT_CHAR = "/";
    private static final String FILE_ROOT_CONFIG = "config";
    private static final String ROOT_PATH = ZK_PATH_SPLIT_CHAR + FILE_ROOT_CONFIG;
    private static final Configuration FILE_CONFIG = ConfigurationFactory.FILE_INSTANCE;
    private static final String SERVER_ADDR_KEY = "serverAddr";
    private static final String SESSION_TIME_OUT_KEY = "session.timeout";
    private static final String CONNECT_TIME_OUT_KEY = "connect.timeout";
    private static final String FILE_CONFIG_KEY_PREFIX = FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE + FILE_CONFIG_SPLIT_CHAR;
    private static final Executor executor= Executors.newSingleThreadExecutor();
    private static volatile ZkClient zkClient;

    public ZKConfiguration () {
        if (zkClient == null) {
            zkClient = new ZkClient(FILE_CONFIG.getConfig(FILE_CONFIG_KEY_PREFIX + SERVER_ADDR_KEY),
                    FILE_CONFIG.getInt(FILE_CONFIG_KEY_PREFIX + SESSION_TIME_OUT_KEY),
                    FILE_CONFIG.getInt(FILE_CONFIG_KEY_PREFIX + CONNECT_TIME_OUT_KEY));
            if(!zkClient.exists(ROOT_PATH)){
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
            public String call() throws Exception {
                String path = ROOT_PATH + ZK_PATH_SPLIT_CHAR + dataId;
                String value = zkClient.readData(path);
                if(StringUtils.isNullOrEmpty(value)) {
                    return defaultValue;
                }
                return value;
            }
        });
        executor.execute(future);
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
            public Boolean call() throws Exception {
                String path = ROOT_PATH + ZK_PATH_SPLIT_CHAR + dataId;
                if(!zkClient.exists(path)){
                    zkClient.create(path, content, CreateMode.PERSISTENT);
                }else {
                    zkClient.writeData(path, content);
                }
                return true;
            }
        });
        executor.execute(future);
        try {
            return future.get(timeoutMills, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            LOGGER.warn("putConfig {} : {} is error or timeout", dataId, content);
            return false;
        }
    }

    @Override
    public boolean putConfigIfAbsent(String dataId, String content, long timeoutMills) {
        throw new NotSupportYetException("not support atom operation putConfigIfAbsent");
    }

    @Override
    public boolean removeConfig(String dataId, long timeoutMills) {
        FutureTask<Boolean> future = new FutureTask<Boolean>(new Callable<Boolean>() {
            public Boolean call() throws Exception {
                String path = ROOT_PATH + ZK_PATH_SPLIT_CHAR + dataId;
                return zkClient.delete(path);
            }
        });
        executor.execute(future);
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
        if(zkClient.exists(path)) {
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
