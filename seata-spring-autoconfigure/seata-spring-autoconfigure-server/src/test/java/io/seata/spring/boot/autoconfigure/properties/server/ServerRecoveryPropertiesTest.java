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
package io.seata.spring.boot.autoconfigure.properties.server;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 */
public class ServerRecoveryPropertiesTest {

    @Test
    public void testServerRecoveryProperties() {
        ServerRecoveryProperties serverRecoveryProperties = new ServerRecoveryProperties();
        serverRecoveryProperties.setAsyncCommittingRetryPeriod(1L);
        serverRecoveryProperties.setCommittingRetryPeriod(1L);
        serverRecoveryProperties.setRollbackingRetryPeriod(1L);
        serverRecoveryProperties.setTimeoutRetryPeriod(1L);

        Assertions.assertEquals(1L, serverRecoveryProperties.getAsyncCommittingRetryPeriod());
        Assertions.assertEquals(1L, serverRecoveryProperties.getCommittingRetryPeriod());
        Assertions.assertEquals(1L, serverRecoveryProperties.getRollbackingRetryPeriod());
        Assertions.assertEquals(1L, serverRecoveryProperties.getTimeoutRetryPeriod());
    }
}
