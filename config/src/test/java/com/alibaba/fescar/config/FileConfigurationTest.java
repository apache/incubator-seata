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

/**
 * @Author: jimin.jm@alibaba-inc.com
 * @Project: fescar-all
 * @DateTime: 2018/12/21 14:03
 * @FileName: FileConfigurationTest
 * @Description:
 */
public class FileConfigurationTest {

    FileConfiguration fileConfiguration = new FileConfiguration();

    @org.junit.Test
    public void isRunAsClientProxy() throws Exception {

    }

    @org.junit.Test
    public void isRunAsServerProxy() throws Exception {

    }

    @org.junit.Test
    public void getConfig() throws Exception {

        String server = fileConfiguration.getConfig("transport.server");
        System.out.println(server);
        //TRANSPORT_SERVER_TYPE = TransportServerType.valueOf(CONFIG.getConfig("transport.server"));
    }

    @org.junit.Test
    public void getConfig1() throws Exception {
        String shareBossWorker = fileConfiguration.getConfig("transport.thread-factory.share-boss-worker");
        System.out.println(shareBossWorker);
    }

    @org.junit.Test
    public void getConfig2() throws Exception {

    }

    @org.junit.Test
    public void getConfig3() throws Exception {

    }

    @org.junit.Test
    public void putConfig() throws Exception {

    }

    @org.junit.Test
    public void putConfig1() throws Exception {

    }

    @org.junit.Test
    public void putConfigIfAbsent() throws Exception {

    }

    @org.junit.Test
    public void putConfigIfAbsent1() throws Exception {

    }

    @org.junit.Test
    public void removeConfig() throws Exception {

    }

    @org.junit.Test
    public void removeConfig1() throws Exception {

    }

    @org.junit.Test
    public void addConfigListener() throws Exception {

    }

    @org.junit.Test
    public void removeConfigListener() throws Exception {

    }

}
