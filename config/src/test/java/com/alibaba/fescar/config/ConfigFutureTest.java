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

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;


/**
 * @author mawerss1@gmail.com
 * @date 2019/03/7
 */
public class ConfigFutureTest {

    private static final String DATA_ID = "dataId";
    private static final String CONTENT = "content";
    private static final String RESULT = "result";

    private static final long TIME_MILLS = 10L;
    private static final ConfigFuture.ConfigOperation GET_OPERATION = ConfigFuture.ConfigOperation.GET;

    private ConfigFuture configFuture;

    @Before
    public void initConfigFutureBeforeTest() {
        configFuture = new ConfigFuture(DATA_ID, CONTENT, GET_OPERATION, TIME_MILLS);
    }

    /**
     * Test get with get operation.
     */
    @Test
    public void testConfigGetWithGetOperation() {
        // return content value when future not set result
        Assert.assertEquals(CONTENT, configFuture.get());
        configFuture.setResult(RESULT);
        Assert.assertEquals(RESULT, configFuture.get());
    }

    /**
     * Test get with timeout argument
     */
    @Test
    public void testConfigGetCustomeOutTime() {
        Assert.assertEquals(CONTENT, configFuture.get(10L, TimeUnit.MILLISECONDS));
        configFuture.setResult(RESULT);
        Assert.assertEquals(RESULT, configFuture.get());
    }

    /**
     * Test get with put operation.
     */
    @Test
    public void testConfigGetWithPutOperation() {
        configFuture.setOperation(ConfigFuture.ConfigOperation.PUT);
        Assert.assertEquals(false, configFuture.get());
        configFuture.setResult(RESULT);
        Assert.assertEquals(RESULT, configFuture.get());
    }

    @Test
    public void testSetContent() {
        configFuture.setContent(CONTENT);
        Assert.assertEquals(CONTENT, configFuture.getContent());
    }

    @Test
    public void testGetContent() {
        configFuture.setContent(CONTENT);
        Assert.assertEquals(CONTENT, configFuture.getContent());
    }

    @Test
    public void testSetDataId() {
        configFuture.setDataId(DATA_ID);
        Assert.assertEquals(DATA_ID, configFuture.getDataId());
    }

    @Test
    public void testGetDataId() {
        configFuture.setDataId(DATA_ID);
        Assert.assertEquals(DATA_ID, configFuture.getDataId());
    }

    @Test
    public void testSetResult() {
        configFuture.setResult(RESULT);
        Assert.assertEquals(RESULT, configFuture.get());
    }

    @Test
    public void testGetOperation() {
        configFuture.setOperation(ConfigFuture.ConfigOperation.GET);
        Assert.assertEquals(ConfigFuture.ConfigOperation.GET, configFuture.getOperation());
    }

    @Test
    public void testSetOperation() {
        configFuture.setOperation(ConfigFuture.ConfigOperation.GET);
        Assert.assertEquals(ConfigFuture.ConfigOperation.GET, configFuture.getOperation());
    }

    @Test
    @Ignore
    public void testIsTimeout() {
        Assert.assertFalse(configFuture.isTimeout());
        //TIME_MILLS + 1 ensure timeout occur
        configFuture.get(TIME_MILLS + 1, TimeUnit.MILLISECONDS);
        Assert.assertTrue(configFuture.isTimeout());
    }

}
