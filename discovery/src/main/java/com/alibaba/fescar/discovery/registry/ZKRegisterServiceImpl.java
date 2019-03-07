package com.alibaba.fescar.discovery.registry;

import com.alibaba.fescar.common.util.NetUtil;
import com.alibaba.fescar.config.Configuration;
import com.alibaba.fescar.config.ConfigurationFactory;
import com.alibaba.fescar.config.ZKConfiguration;
import com.alibaba.nacos.client.naming.utils.NetUtils;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import static com.alibaba.fescar.common.Constants.IP_PORT_SPLIT_CHAR;

/**
 * zookeeper path as /registry/zk/
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
        String path = ROOT_PATH + getClusterName()+ZK_PATH_SPLIT_CHAR+NetUtil.toStringAddress(address);
        getClientInstance().createPersistent(path,true);
    }

    @Override
    public void unregister(InetSocketAddress address) throws Exception {
        String path = ROOT_PATH + getClusterName()+ZK_PATH_SPLIT_CHAR+NetUtil.toStringAddress(address);
        getClientInstance().delete(path);
    }

    @Override
    public void subscribe(String cluster, IZkChildListener listener) throws Exception {
        if (null == cluster) {
            return ;
        }
        String path = ROOT_PATH  + cluster;
        if(!getClientInstance().exists(path)) {return;}
        getClientInstance().subscribeChildChanges(path,listener);
    }

    @Override
    public void unsubscribe(String cluster, IZkChildListener listener) throws Exception {
        if (null == cluster) {
            return ;
        }
        String path = ROOT_PATH + cluster;
        if(!getClientInstance().exists(path)) {return;}
        getClientInstance().unsubscribeChildChanges(path,listener);

    }

    /**
     * @param key the key
     * @return
     * @throws Exception
     */
    @Override
    public List<InetSocketAddress> lookup(String key) throws Exception {
        String clusterName = getServiceGroup(key);
        if (null == clusterName) {
            return null;
        }
        Boolean exist = getClientInstance().exists(ROOT_PATH+clusterName);
        if(!exist){
            return null;
        }
        List<InetSocketAddress> interSocketAddresses = new ArrayList<>();
        List<String> childClusterPath = getClientInstance().getChildren(ROOT_PATH+clusterName);
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
        return ZKClientSingleton.getInstance();
    }

    private String getClusterName() {
        String clusterConfigName =  FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE + FILE_CONFIG_SPLIT_CHAR
                + REGISTRY_CLUSTER;
        return FILE_CONFIG.getConfig(clusterConfigName);
    }

    private String getServiceGroup(String key){
        Configuration configuration = ConfigurationFactory.getInstance();
        String clusterName = PREFIX_SERVICE_ROOT + CONFIG_SPLIT_CHAR + PREFIX_SERVICE_MAPPING + key;
        return configuration.getConfig(clusterName);
    }


}
