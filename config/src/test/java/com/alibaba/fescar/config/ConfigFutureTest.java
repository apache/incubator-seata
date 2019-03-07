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

package com.alibaba.fescar.config;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author mawerss1@gmail.com
 * @Date 2019/03/7
 */
public class ConfigFutureTest {

    private final String dataId = "dataId";
    private final String content = "content";
    private final String result = "result";

    private final long timeMills = 10l;
    private final ConfigFuture.ConfigOperation getOperation = ConfigFuture.ConfigOperation.GET;

    private ConfigFuture configFuture;


    @Before
    public void initConfigFutureBeforeTest() {
        configFuture = new ConfigFuture(dataId, content, getOperation,timeMills);
    }

    /**
     * Test get with get operation.
     */
    @Test
    public void testGetWithGetOperation() {
        Assert.assertEquals(content, configFuture.get());
        startSetResultThread(result);
        Assert.assertEquals(result, configFuture.get());
    }

    /**
     * Test get with put operation.
     */
    @Test
    public void testGetWithPutOperation() {
        configFuture.setOperation(ConfigFuture.ConfigOperation.PUT);
        Assert.assertEquals(Boolean.FALSE, configFuture.get());
        startSetResultThread(result);
        Assert.assertEquals(result, configFuture.get());
    }

    /**
     * Test get with timeout argument.
     */
    @Test
    public void testGetWithTimeOut() {
        long outTime = 200L;
        new Thread(() -> {
            try {
                Thread.sleep(outTime + 100);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
            configFuture.setResult(result);
        });
        final Object result = configFuture.get(outTime, TimeUnit.MILLISECONDS);
        Assert.assertEquals(content, result);
    }

    @Test
    public void testSetContent() {
        configFuture.setContent(content);
    }

    @Test
    public void testGetContent() {
        configFuture.setContent(content);
        Assert.assertEquals(content, configFuture.getContent());
    }

    @Test
    public void testSetDataId() {
        configFuture.setDataId(dataId);
        Assert.assertEquals(dataId, configFuture.getDataId());
    }

    @Test
    public void testGetDataId() {
        configFuture.setDataId(dataId);
        Assert.assertEquals(dataId, configFuture.getDataId());
    }

    @Test
    public void testSetResult() {
        configFuture.setResult(result);
    }

    @Test
    public void testGetOperation() {
        configFuture.setOperation(ConfigFuture.ConfigOperation.GET);
        Assert.assertEquals(ConfigFuture.ConfigOperation.GET, configFuture.getOperation());
    }

    @Test
    public void testSetOperation() {
        configFuture.setOperation(ConfigFuture.ConfigOperation.GET);
    }

    @Test
    public void testIsTimeout() throws InterruptedException {
        Thread.sleep(timeMills * 2);
        Assert.assertTrue(configFuture.isTimeout());
    }

    private void startSetResultThread(Object result) {
        new Thread(() -> configFuture.setResult(this.result)).start();
    }

}
