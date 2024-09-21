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
package org.apache.seata.spring.boot.autoconfigure.properties.server;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ServerPropertiesTest {

    @Test
    public void testMetricsProperties() {
        ServerProperties serverProperties = new ServerProperties();
        serverProperties.setXaerNotaRetryTimeout(1);
        serverProperties.setRetryDeadThreshold(1);
        serverProperties.setApplicationDataLimit(1);
        serverProperties.setServicePort(1);
        serverProperties.setEnableCheckAuth(true);
        serverProperties.setApplicationDataLimitCheck(true);
        serverProperties.setEnableParallelHandleBranch(true);
        serverProperties.setEnableParallelRequestHandle(true);
        serverProperties.setRollbackRetryTimeoutUnlockEnable(true);
        serverProperties.setMaxCommitRetryTimeout(1L);
        serverProperties.setMaxRollbackRetryTimeout(1L);

        Assertions.assertEquals(1, serverProperties.getXaerNotaRetryTimeout());
        Assertions.assertEquals(1, serverProperties.getRetryDeadThreshold());
        Assertions.assertEquals(1, serverProperties.getApplicationDataLimit());
        Assertions.assertEquals(1, serverProperties.getServicePort());
        Assertions.assertTrue(serverProperties.getEnableCheckAuth());
        Assertions.assertTrue(serverProperties.getApplicationDataLimitCheck());
        Assertions.assertTrue(serverProperties.getEnableParallelHandleBranch());
        Assertions.assertTrue(serverProperties.getEnableParallelRequestHandle());
        Assertions.assertTrue(serverProperties.getRollbackRetryTimeoutUnlockEnable());
        Assertions.assertEquals(1L, serverProperties.getMaxCommitRetryTimeout());
        Assertions.assertEquals(1L, serverProperties.getMaxRollbackRetryTimeout());
    }
}
