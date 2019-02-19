package com.alibaba.fescar.config.zookeeper;

import com.alibaba.fescar.common.util.StringUtils;
import com.alibaba.fescar.config.AbstractConfiguration;
import com.alibaba.fescar.config.Configuration;
import com.alibaba.fescar.config.ConfigurationFactory;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author crazier.huang
 * @date 2019/2/18
 */
public class ZKConfiguration extends AbstractConfiguration<IZkDataListener> {
    private final static Logger logger = LoggerFactory.getLogger("ZKConfiguration");

    private static final String REGISTRY_TYPE = "zk";
    private static final String ZK_PATH_SPLIT_CHAR = "/";
    private static final String PRO_SERVER_ADDR_KEY = "serverAddr";
    private static final String FILE_ROOT_CONFIG = "config";
    private static final String ROOT_PATH = ZK_PATH_SPLIT_CHAR + FILE_ROOT_CONFIG;
    private static final Configuration FILE_CONFIG = ConfigurationFactory.FILE_INSTANCE;


    private static volatile ZkClient zkClient;
    public ZKConfiguration() {
        if(zkClient == null){
            zkClient = new ZkClient(FILE_CONFIG.getConfig(getZKAddrFileKey()));
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
        if(StringUtils.isEmpty(value)) return defaultValue;

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
        return false;
    }

    @Override
    public boolean removeConfig(String dataId, long timeoutMills) {
        String path = ROOT_PATH + ZK_PATH_SPLIT_CHAR + dataId;
        return zkClient.delete(path);
    }

    @Override
    public void addConfigListener(String dataId, IZkDataListener listener) {
        // TODO: 2019/2/18
    }

    @Override
    public void removeConfigListener(String dataId, IZkDataListener listener) {
        // TODO: 2019/2/18
    }

    @Override
    public List<IZkDataListener> getConfigListeners(String dataId) {
        // TODO: 2019/2/18

        return null;
    }

    public ZkClient getZkClient(){
        return zkClient;
    }


    private static String getZKAddrFileKey() {
        return FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE + FILE_CONFIG_SPLIT_CHAR
                + PRO_SERVER_ADDR_KEY;
    }
}
