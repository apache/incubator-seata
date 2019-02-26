package com.alibaba.fescar.discovery.registry;

import com.alibaba.fescar.config.Configuration;
import com.alibaba.fescar.config.ConfigurationFactory;
import com.alibaba.fescar.config.zookeeper.ZKConfiguration;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.CreateMode;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import static com.alibaba.fescar.common.Constants.IP_PORT_SPLIT_CHAR;

/**
 * @author crazier.huang
 * @date 2019/2/15
 */
public class ZKRegisterServiceImpl implements RegistryService<IZkChildListener> {


    private static volatile ZKRegisterServiceImpl instance;
    private static final Configuration FILE_CONFIG = ConfigurationFactory.FILE_INSTANCE;
    private static final String ZK_PATH_SPLIT_CHAR = "/";
    private static final String FILE_ROOT_REGISTRY = "registry";
    private static final String FILE_CONFIG_SPLIT_CHAR = ".";
    private static final String REGISTRY_CLUSTER = "cluster";
    private static final String REGISTRY_TYPE = "zk";
    private static final String ROOT_PATH = ZK_PATH_SPLIT_CHAR+FILE_ROOT_REGISTRY+ZK_PATH_SPLIT_CHAR + REGISTRY_TYPE+ZK_PATH_SPLIT_CHAR;

    private ZKRegisterServiceImpl() {}

    public static ZKRegisterServiceImpl getInstance() {
        if (null == instance) {
            synchronized (ZKRegisterServiceImpl.class) {
                if (null == instance) {
                    instance = new ZKRegisterServiceImpl();

                }
            }
        }
        return instance;
    }
    @Override
    public void register(InetSocketAddress address) throws Exception {
        String path = ROOT_PATH + getClusterName()+ZK_PATH_SPLIT_CHAR+getIPAndPort(address);
        getClientInstance().create(path,null, CreateMode.EPHEMERAL);
    }

    @Override
    public void unregister(InetSocketAddress address) throws Exception {
        String path = ROOT_PATH + getClusterName()+ZK_PATH_SPLIT_CHAR+getIPAndPort(address);
        getClientInstance().delete(path);
    }

    @Override
    public void subscribe(String cluster, IZkChildListener listener) throws Exception {
        String path = ROOT_PATH  + cluster + ZK_PATH_SPLIT_CHAR;
        getClientInstance().subscribeChildChanges(path,listener);
    }

    @Override
    public void unsubscribe(String cluster, IZkChildListener listener) throws Exception {
        String path = ROOT_PATH + cluster+ ZK_PATH_SPLIT_CHAR;
        getClientInstance().unsubscribeChildChanges(path,listener);

    }

    /**
     * 1. 根据key找到groupName -- key：service.vgroup_mapping.my_test_tx_group
     * 2. 根据groupName找到对应child
     * @param key the key
     * @return
     * @throws Exception
     */
    @Override
    public List<InetSocketAddress> lookup(String key) throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        String clusterName = config.getConfig(PREFIX_SERVICE_ROOT + CONFIG_SPLIT_CHAR + PREFIX_SERVICE_MAPPING + key);
        if (null == clusterName) {
            return null;
        }
        List<String> childPath = getClientInstance().getChildren(ROOT_PATH);
        if(!childPath.contains(clusterName)){
            return null;
        }
        List<InetSocketAddress> interSocketAddresses = new ArrayList<>();
        List<String> childClusterPath = getClientInstance().getChildren(ROOT_PATH+ZK_PATH_SPLIT_CHAR+clusterName);
        childClusterPath.forEach(path->{
            String[] ipAndPort = path.split(IP_PORT_SPLIT_CHAR);
            if (ipAndPort.length != 2) {
                throw new IllegalArgumentException("endpoint format should like ip:port");
            }
            interSocketAddresses.add(new InetSocketAddress(ipAndPort[0], Integer.valueOf(ipAndPort[1])));
        });
        return interSocketAddresses;
    }

    private ZkClient getClientInstance() {
        ZKConfiguration configuration = (ZKConfiguration) ConfigurationFactory.getInstance();
        return configuration.getZkClient();
    }

    private String getClusterName() {
        String clusterConfigName =  FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE + FILE_CONFIG_SPLIT_CHAR
                + REGISTRY_CLUSTER;
        return FILE_CONFIG.getConfig(clusterConfigName);

    }

    private String getIPAndPort(InetSocketAddress address) {
        String addr = address.getHostName() + IP_PORT_SPLIT_CHAR + address.getPort();
        return addr;
    }

}
