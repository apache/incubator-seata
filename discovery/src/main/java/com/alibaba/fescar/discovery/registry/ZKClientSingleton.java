package com.alibaba.fescar.discovery.registry;

import com.alibaba.fescar.config.Configuration;
import com.alibaba.fescar.config.ConfigurationFactory;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.CreateMode;

/**
 * 作者: crazier.huang
 * 项目: fescar
 * 日期: 2019/3/6 Wednesday
 * 说明:
 */
public class ZKClientSingleton {

    private static ZkClient zkClient = null;
    private static final Configuration CONFIG = ConfigurationFactory.getInstance();
    private static final String PRO_SERVER_ADDR_KEY = "register.zk.serverAddr";
    private static final String PRO_SERVER_SESSION_TIME_OUT = "register.zk.session.timeout";
    private static final String PRO_SERVER_CONNECT_TIME_OUT = "register.zk.connect.timeout";
    private static final String ZK_PATH_SPLIT_CHAR = "/";
    private static final String FILE_ROOT_REGISTRY = "registry";
    private static final String REGISTRY_TYPE = "zk";
    private static final String ROOT_PATH = ZK_PATH_SPLIT_CHAR+FILE_ROOT_REGISTRY+ZK_PATH_SPLIT_CHAR + REGISTRY_TYPE+ZK_PATH_SPLIT_CHAR;

    private ZKClientSingleton() {
    }

    public static ZkClient getInstance() {
        if (zkClient == null) {
            zkClient = new ZkClient(CONFIG.getConfig(PRO_SERVER_ADDR_KEY),
                    CONFIG.getInt(PRO_SERVER_SESSION_TIME_OUT),
                    CONFIG.getInt(PRO_SERVER_CONNECT_TIME_OUT)
            );
            if(!zkClient.exists(ROOT_PATH)){
                zkClient.createPersistent(ROOT_PATH,true);
            }

        }
        return zkClient ;
    }
}
