package com.alibaba.fescar.discovery.registry.zookeeper;

import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;

import java.util.List;

/**
 * @author crazier.huang
 * @date 2019/2/15
 */
public class ZkChildChangeListener implements IZkDataListener {

    @Override
    public void handleDataChange(String dataPath, Object data) throws Exception {

    }

    @Override
    public void handleDataDeleted(String dataPath) throws Exception {

    }
}
