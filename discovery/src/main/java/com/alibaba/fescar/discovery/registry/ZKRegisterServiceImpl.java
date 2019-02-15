package com.alibaba.fescar.discovery.registry;

import org.I0Itec.zkclient.ZkClient;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author crazier.huang
 * @date 2019/2/15
 */
public class ZKRegisterServiceImpl implements RegistryService<Object> {

    private static volatile ZKRegisterServiceImpl instance;
    private static ZkClient zkClient;

    private ZKRegisterServiceImpl() {}

    public static ZKRegisterServiceImpl getInstance() {
        if (null == instance) {
            synchronized (ZKRegisterServiceImpl.class) {
                if (null == instance) {
                    instance = new ZKRegisterServiceImpl();
                    zkClient = new ZkClient("10.29.23.74:2181");
                }
            }
        }
        return instance;
    }
    @Override
    public void register(InetSocketAddress address) throws Exception {
        String path = PREFIX_SERVICE_ROOT + CONFIG_SPLIT_CHAR + PREFIX_SERVICE_MAPPING ;
//        zkClient.exists()
    }

    @Override
    public void unregister(InetSocketAddress address) throws Exception {

    }

    @Override
    public void subscribe(String cluster, Object listener) throws Exception {

    }

    @Override
    public void unsubscribe(String cluster, Object listener) throws Exception {

    }

    @Override
    public List<InetSocketAddress> lookup(String key) throws Exception {
        return null;
    }
}
