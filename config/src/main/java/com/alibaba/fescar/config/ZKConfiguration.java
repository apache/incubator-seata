package com.alibaba.fescar.config;

import com.alibaba.fescar.common.exception.NotSupportYetException;
import com.alibaba.fescar.common.util.StringUtils;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.alibaba.fescar.config.ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR;
import static com.alibaba.fescar.config.ConfigurationKeys.FILE_ROOT_REGISTRY;

/**
 * @author crazier.huang
 * @date 2019/2/18
 */
public class ZKConfiguration extends AbstractConfiguration<IZkDataListener> {
    private final static Logger logger = LoggerFactory.getLogger("ZKConfiguration");

    private static final String REGISTRY_TYPE = "zk";
    private static final String ZK_PATH_SPLIT_CHAR = "/";
    private static final String PRO_SERVER_ADDR_KEY = "serverAddr";
    private static final String ZK_SESSION_TIMEOUT_KEY = "session.timeout";
    private static final String ZK_CONNECT_TIMEOUT_KEY = "connect.timeout";
    private static final String FILE_ROOT_CONFIG = "config";
    private static final String ROOT_PATH = ZK_PATH_SPLIT_CHAR + FILE_ROOT_CONFIG;
    private static final Configuration FILE_CONFIG = ConfigurationFactory.FILE_INSTANCE;

    private static volatile ZkClient zkClient;
    public ZKConfiguration() {
        if(zkClient == null){
            zkClient = new ZkClient(FILE_CONFIG.getConfig(getZKAddrFileKey()),
                    FILE_CONFIG.getInt(getSessionTimeOutKey()),
                    FILE_CONFIG.getInt(getConnectTimeOutKey())
            );
            if(!zkClient.exists(ROOT_PATH)){
                zkClient.create(ROOT_PATH,null, CreateMode.PERSISTENT);
            }
        }
    }

    @Override
    public String getTypeName() {
        return REGISTRY_TYPE;
    }

    @Override
    public String getConfig(String dataId, String defaultValue, long timeoutMills) {
        String path = ROOT_PATH + ZK_PATH_SPLIT_CHAR + dataId;
        String value = zkClient.readData(path);
        if(StringUtils.isEmpty(value)) {
            return defaultValue;
        }

        return value;
    }

    @Override
    public boolean putConfig(String dataId, String content, long timeoutMills) {
        String path = ROOT_PATH + ZK_PATH_SPLIT_CHAR + dataId;
        if(!zkClient.exists(path)){
            zkClient.create(path,content,CreateMode.PERSISTENT);
        }else {
            zkClient.writeData(path,content);
        }

        return true;
    }

    @Override
    public boolean putConfigIfAbsent(String dataId, String content, long timeoutMills) {
        throw new NotSupportYetException("not support putConfigIfAbsent");
    }

    @Override
    public boolean removeConfig(String dataId, long timeoutMills) {
        String path = ROOT_PATH + ZK_PATH_SPLIT_CHAR + dataId;
        return zkClient.delete(path);
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
        throw new NotSupportYetException("not support putConfigIfAbsent");
    }

    public ZkClient getZkClient(){
        return zkClient;
    }

    // TODO: 2019/3/4 Registry and config configuration are independent of each other  this key??
    private static String getZKAddrFileKey() {
        return FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE + FILE_CONFIG_SPLIT_CHAR
                + PRO_SERVER_ADDR_KEY;
    }
    private static String getSessionTimeOutKey() {
        return FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE + FILE_CONFIG_SPLIT_CHAR
                + ZK_SESSION_TIMEOUT_KEY;
    }
    private static String getConnectTimeOutKey() {
        return FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE + FILE_CONFIG_SPLIT_CHAR
                + ZK_CONNECT_TIMEOUT_KEY;
    }
}
