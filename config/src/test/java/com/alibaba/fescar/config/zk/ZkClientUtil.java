package com.alibaba.fescar.config.zk;/**
 * @author JiaWenQing
 * @date 2019-2-21
 */

import org.I0Itec.zkclient.*;
import org.I0Itec.zkclient.exception.ZkException;
import org.I0Itec.zkclient.exception.ZkInterruptedException;
import org.I0Itec.zkclient.exception.ZkTimeoutException;
import org.I0Itec.zkclient.serialize.ZkSerializer;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName ZkClientUtil
 * @Description
 * @Author JiaWenQing
 * @Date 2019-2-21 15:55
 **/
public class ZkClientUtil  extends ZkClient{
    public ZkClientUtil(String serverstring) {
        super(serverstring);
    }

    public ZkClientUtil(String zkServers, int connectionTimeout) {
        super(zkServers, connectionTimeout);
    }

    public ZkClientUtil(String zkServers, int sessionTimeout, int connectionTimeout) {
        super(zkServers, sessionTimeout, connectionTimeout);
    }

    public ZkClientUtil(String zkServers, int sessionTimeout, int connectionTimeout, ZkSerializer zkSerializer) {
        super(zkServers, sessionTimeout, connectionTimeout, zkSerializer);
    }

    public ZkClientUtil(String zkServers, int sessionTimeout, int connectionTimeout, ZkSerializer zkSerializer, long operationRetryTimeout) {
        super(zkServers, sessionTimeout, connectionTimeout, zkSerializer, operationRetryTimeout);
    }

    public ZkClientUtil(IZkConnection connection) {
        super(connection);
    }

    public ZkClientUtil(IZkConnection connection, int connectionTimeout) {
        super(connection, connectionTimeout);
    }

    public ZkClientUtil(IZkConnection zkConnection, int connectionTimeout, ZkSerializer zkSerializer) {
        super(zkConnection, connectionTimeout, zkSerializer);
    }

    public ZkClientUtil(IZkConnection zkConnection, int connectionTimeout, ZkSerializer zkSerializer, long operationRetryTimeout) {
        super(zkConnection, connectionTimeout, zkSerializer, operationRetryTimeout);
    }

    @Override
    protected List<String> getChildren(String path, boolean watch) {
        return super.getChildren(path, watch);
    }

    @Override
    protected Set<IZkDataListener> getDataListener(String path) {
        return super.getDataListener(path);
    }

    @Override
    protected boolean exists(String path, boolean watch) {
        return super.exists(path, watch);
    }

    @Override
    protected <T> T readData(String path, Stat stat, boolean watch) {
        return super.readData(path, stat, watch);
    }
}
