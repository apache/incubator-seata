/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.alibaba.fescar.config.zk;

import com.alibaba.fescar.config.Configuration;
import com.alibaba.fescar.config.zookeeper.ZKConfigListener;
import com.alibaba.fescar.config.zookeeper.ZKConfiguration;
import com.alibaba.fescar.config.zookeeper.ZKDataConfigListener;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.junit.Assert;
import org.junit.Before;
import org.testng.annotations.Test;

import java.util.List;

/**
 * @Author: jimin.jm@alibaba-inc.com
 * @Project: feats-all
 * @DateTime: 2019/2/1 7:16 PM
 * @FileName: NacosConfigurationTest
 * @Description:
 */
public class ZKConfigurationTest {
    //private static final Configuration CONFIGURATION = new NacosConfiguration();

    private static final String INT_DATAID = "transport.thread-factory.client-selector-thread-size";
    private static final String LONG_DATAID = "transport.thread-factory.worker-thread-size";
    private static final String BOOLEAN_DATAID = "service.disable";
    private static final String STRING_DATAID = "transport.type";
    private static final String PUT_DATAID = "transport.mock";
    private static final String NOT_EXIST_DATAID = "service.yyy.xxx";

    private Configuration configuration = new ZKConfiguration();

    @Test
    public void testPutConfig() {
        configuration.putConfig(INT_DATAID,"55");
    }

    @Test
    public void testGetConfig() {
        String result = configuration.getConfig(INT_DATAID,"3232");
        Assert.assertEquals("22",result);
    }

    @Test
    public void testRemoveConfig() {
        boolean flag = configuration.removeConfig(INT_DATAID);
        Assert.assertEquals(true,flag);
    }

    @Test
    public void testPutConfigIfAbsent() {
    }

    @Test
    public void testAddConfigListener() throws InterruptedException {
        configuration.addConfigListener(INT_DATAID, new IZkDataListener() {
            @Override
            public void handleDataChange(String dataPath, Object data) throws Exception {
                System.out.println("dataPath");
            }

            @Override
            public void handleDataDeleted(String dataPath) throws Exception {

            }
        });

        Thread.sleep(2000000);
//        configuration.addConfigListener(INT_DATAID,new ZKConfigListener());
//        List<IZkDataListener> zkDataListeners =  configuration.getConfigListeners(INT_DATAID);
//        Assert.assertEquals(2,zkDataListeners.size());

    }

    @Test
    public void testRemoveConfigListener() {
        IZkDataListener listener = new ZKDataConfigListener();
        configuration.addConfigListener(INT_DATAID,listener);
        configuration.addConfigListener(INT_DATAID,new ZKConfigListener());
        List<IZkDataListener> zkDataListeners =  configuration.getConfigListeners("22");
        System.out.println(zkDataListeners.size());

        configuration.removeConfigListener("22",listener);
        Assert.assertEquals(1,zkDataListeners.size());
    }

    @Test
    public void testGetConfigListeners() throws InterruptedException {
        Thread.sleep(2000);
        List<IZkDataListener> zkDataListeners =  configuration.getConfigListeners(INT_DATAID);
        Assert.assertEquals(1,zkDataListeners.size());
    }

    @Test
    public void testGetTypeName() {
    }
}