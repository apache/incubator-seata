/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.spring.boot.autoconfigure.properties.server.store;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StoreRedisPropertiesTest {

    @Test
    public void testStoreRedisProperties() {
        StoreRedisProperties storeRedisProperties = new StoreRedisProperties();
        storeRedisProperties.setMode("mode");
        storeRedisProperties.setType("type");
        storeRedisProperties.setPassword("pwd");
        storeRedisProperties.setDatabase(1);
        storeRedisProperties.setMaxConn(1);
        storeRedisProperties.setMinConn(1);
        storeRedisProperties.setQueryLimit(1);
        storeRedisProperties.setMaxTotal(1);

        Assertions.assertEquals("mode", storeRedisProperties.getMode());
        Assertions.assertEquals("type", storeRedisProperties.getType());
        Assertions.assertEquals("pwd", storeRedisProperties.getPassword());
        Assertions.assertEquals(1, storeRedisProperties.getDatabase());
        Assertions.assertEquals(1, storeRedisProperties.getMaxConn());
        Assertions.assertEquals(1, storeRedisProperties.getMinConn());
        Assertions.assertEquals(1, storeRedisProperties.getQueryLimit());
        Assertions.assertEquals(1, storeRedisProperties.getMaxTotal());

        StoreRedisProperties.Single single = new StoreRedisProperties.Single();
        single.setHost("host");
        single.setPort(80);
        Assertions.assertEquals("host", single.getHost());
        Assertions.assertEquals(80, single.getPort());

        StoreRedisProperties.Sentinel sentinel = new StoreRedisProperties.Sentinel();
        sentinel.setSentinelHosts("host");
        sentinel.setMasterName("master");
        sentinel.setSentinelPassword("pwd");
        Assertions.assertEquals("host", sentinel.getSentinelHosts());
        Assertions.assertEquals("master", sentinel.getMasterName());
        Assertions.assertEquals("pwd", sentinel.getSentinelPassword());
    }
}
