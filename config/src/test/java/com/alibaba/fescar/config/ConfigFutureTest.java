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
import org.junit.Test;

/**
 * @author mawerss1@gmail.com
 * @Date 2019.03.17 11.12am
 */
public class ConfigFutureTest {

    private final String dataId = "dataid";
    private final String content = "content";
    private final String success = "success";

    private ConfigFuture configFuture = new ConfigFuture(dataId, content, ConfigFuture.ConfigOperation.GET);

    @Test
    public void testIsTimeOut() {
        configFuture.isTimeout();
    }

    @Test
    public void testGet() {
        Assert.assertEquals(content, configFuture.get());
        final SetResultTask setResultTask = new SetResultTask(configFuture, success);
        new Thread(setResultTask).start();
        Assert.assertEquals(success, configFuture.get());
    }

    static class SetResultTask implements Runnable {

        private ConfigFuture configFuture;

        private String result;

        public SetResultTask(ConfigFuture configFuture, String result) {
            this.configFuture = configFuture;
            this.result = result;
        }

        @Override
        public void run() {
            configFuture.setContent(result);
        }
    }

}
