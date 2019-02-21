package com.alibaba.fescar.discovery.registry.zookeeper;

import com.alibaba.fescar.common.util.StringUtils;
import com.alibaba.fescar.config.Configuration;
import com.alibaba.fescar.config.ConfigurationFactory;
import com.alibaba.fescar.config.zookeeper.ZKConfiguration;
import com.alibaba.fescar.discovery.registry.RegistryService;
import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.CreateMode;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.alibaba.fescar.common.Constants.IP_PORT_SPLIT_CHAR;
import static com.alibaba.fescar.config.ConfigurationFactory.FILE_ROOT_REGISTRY;

/**
 * @author crazier.huang
 * @date 2019/2/15
 */
public class ZKRegisterServiceImpl implements RegistryService<IZkChildListener> {

    private static volatile ZKRegisterServiceImpl instance;
    private static final String ZK_PATH_SPLIT_CHAR = "/";
    private static final String ROOT_PATH = ZK_PATH_SPLIT_CHAR + FILE_ROOT_REGISTRY;;

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

    }

    @Override
    public void unsubscribe(String cluster, IZkChildListener listener) throws Exception {

    }

    @Override
    public List<InetSocketAddress> lookup(String key) throws Exception {
        String clusterName = getClusterName();
        String rootPath = ROOT_PATH+ZK_PATH_SPLIT_CHAR+clusterName;
        List<InetSocketAddress> interSocketAddresses = new ArrayList<>();
        if(StringUtils.isEmpty(clusterName)){
            return null;
        }
        List<String> pathList = getClientInstance().getChildren(rootPath);
        if(CollectionUtils.isEmpty(pathList)){
            return null;
        }
        pathList.forEach(path->{
            String[] ipAndPort = path.split(IP_PORT_SPLIT_CHAR);
            if (ipAndPort.length != 2) {
                throw new IllegalArgumentException("endpoint format should like ip:port");
            }
            interSocketAddresses.add(new InetSocketAddress(ipAndPort[0], Integer.valueOf(ipAndPort[1])));
        });


        return interSocketAddresses;
    }

    private ZkClient getClientInstance() {
        // TODO: 2019/2/19zookeeper 的client try catch
        ZKConfiguration configuration = (ZKConfiguration) ConfigurationFactory.getInstance();

        return configuration.getZkClient();
    }

    private String getClusterName() {
        // TODO: 2019/2/19 获取集群名称
        return null;
    }

    private String getIPAndPort(InetSocketAddress address) {
        if (null == address.getHostName() || 0 == address.getPort()) {
            throw new IllegalArgumentException("invalid address:" + address);
        }

        String addr = address.getHostName() + IP_PORT_SPLIT_CHAR + address.getPort();
        return addr;
    }

}
