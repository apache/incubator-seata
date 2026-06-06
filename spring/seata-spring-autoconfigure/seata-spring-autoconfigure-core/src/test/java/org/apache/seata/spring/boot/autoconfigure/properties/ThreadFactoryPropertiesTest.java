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
package org.apache.seata.spring.boot.autoconfigure.properties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ThreadFactoryPropertiesTest {

    @Test
    public void testThreadFactoryProperties() {
        ThreadFactoryProperties threadFactoryProperties = new ThreadFactoryProperties();
        threadFactoryProperties.setBossThreadPrefix("boss");
        threadFactoryProperties.setWorkerThreadPrefix("worker");
        threadFactoryProperties.setWorkerThreadSize("1");
        threadFactoryProperties.setClientSelectorThreadPrefix("prefix");
        threadFactoryProperties.setClientWorkerThreadPrefix("prefix");
        threadFactoryProperties.setServerExecutorThreadPrefix("prefix");
        threadFactoryProperties.setBossThreadSize(1);
        threadFactoryProperties.setClientSelectorThreadSize(1);
        threadFactoryProperties.setShareBossWorker(true);

        Assertions.assertEquals("boss", threadFactoryProperties.getBossThreadPrefix());
        Assertions.assertEquals("worker", threadFactoryProperties.getWorkerThreadPrefix());
        Assertions.assertEquals("1", threadFactoryProperties.getWorkerThreadSize());
        Assertions.assertEquals("prefix", threadFactoryProperties.getClientSelectorThreadPrefix());
        Assertions.assertEquals("prefix", threadFactoryProperties.getClientWorkerThreadPrefix());
        Assertions.assertEquals("prefix", threadFactoryProperties.getServerExecutorThreadPrefix());
        Assertions.assertEquals(1, threadFactoryProperties.getBossThreadSize());
        Assertions.assertEquals(1, threadFactoryProperties.getClientSelectorThreadSize());
        Assertions.assertEquals(true, threadFactoryProperties.isShareBossWorker());
    }
}
