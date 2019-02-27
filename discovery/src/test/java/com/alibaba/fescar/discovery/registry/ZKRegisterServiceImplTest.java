package com.alibaba.fescar.discovery.registry;

import com.alibaba.fescar.config.Configuration;
import com.alibaba.fescar.config.ConfigurationFactory;
import com.alibaba.fescar.config.zookeeper.ZKConfiguration;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;
import org.junit.Assert;
import org.testng.annotations.Test;

import java.net.InetSocketAddress;
import java.util.List;

import static org.testng.Assert.*;

/**
 * @author crazier.huang
 * @date 2019/2/27
 */
public class ZKRegisterServiceImplTest {

    private ZKRegisterServiceImpl instance = ZKRegisterServiceImpl.getInstance();
    private static final Configuration FILE_CONFIG = ConfigurationFactory.FILE_INSTANCE;
    private static final String ZK_PATH_SPLIT_CHAR = "/";
    private static final String FILE_ROOT_REGISTRY = "registry";
    private static final String FILE_CONFIG_SPLIT_CHAR = ".";
    private static final String REGISTRY_CLUSTER = "cluster";
    private static final String REGISTRY_TYPE = "zk";
    private static final String ROOT_PATH = ZK_PATH_SPLIT_CHAR+FILE_ROOT_REGISTRY+ZK_PATH_SPLIT_CHAR + REGISTRY_TYPE+ZK_PATH_SPLIT_CHAR;

    @Test
    public void testRegister() throws Exception {
        String path = ROOT_PATH + getClusterName();
        instance.register(new InetSocketAddress("127.0.0.1", 8081));
        instance.register(new InetSocketAddress("127.0.0.1", 8082));

        List<String> pathList = getClientInstance().getChildren(path);
        Assert.assertEquals(2,pathList.size());
    }
    @Test
    public void testUnregister() throws Exception {
        String path = ROOT_PATH + getClusterName();
        instance.unregister(new InetSocketAddress("127.0.0.1", 8082));
        List<String> pathList = getClientInstance().getChildren(path);
        Assert.assertEquals(1,pathList.size());
    }

    @Test
    public void testSubscribe() throws Exception {
        instance.subscribe("my_test_tx_group", new IZkChildListener() {
            @Override
            public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
                System.out.println("my_test_tx_group Subscribe "+parentPath);
            }
        });
        Thread.sleep(60000);
    }

    @Test
    public void testUnsubscribe() throws Exception {
        instance.unsubscribe("my_test_tx_group", new IZkChildListener() {
            @Override
            public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
                System.out.println("my_test_tx_group Subscribe "+parentPath);
            }
        });
    }

    @Test
    public void testLookup() throws Exception {
        List<InetSocketAddress> list = instance.lookup("my_test_tx_group");
        Assert.assertEquals(2,list.size());
    }

    private String getClusterName() {
        String clusterConfigName =  FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE + FILE_CONFIG_SPLIT_CHAR
                + REGISTRY_CLUSTER;
        return FILE_CONFIG.getConfig(clusterConfigName);

    }
    private ZkClient getClientInstance() {
        ZKConfiguration configuration = (ZKConfiguration) ConfigurationFactory.getInstance();
        return configuration.getZkClient();
    }
}