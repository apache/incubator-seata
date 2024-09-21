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
package org.apache.seata.spring.boot.autoconfigure.properties.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RmPropertiesTest {

    @Test
    public void testRmProperties() {
        RmProperties rmProperties = new RmProperties();
        rmProperties.setAsyncCommitBufferLimit(1);
        Assertions.assertEquals(1, rmProperties.getAsyncCommitBufferLimit());

        rmProperties.setReportRetryCount(1);
        Assertions.assertEquals(1, rmProperties.getReportRetryCount());

        rmProperties.setTableMetaCheckEnable(true);
        Assertions.assertTrue(rmProperties.isTableMetaCheckEnable());

        rmProperties.setReportSuccessEnable(true);
        Assertions.assertTrue(rmProperties.isReportSuccessEnable());

        rmProperties.setSagaBranchRegisterEnable(true);
        Assertions.assertTrue(rmProperties.isSagaBranchRegisterEnable());

        rmProperties.setSagaJsonParser("json");
        Assertions.assertEquals("json", rmProperties.getSagaJsonParser());

        rmProperties.setTableMetaCheckerInterval(1);
        Assertions.assertEquals(1, rmProperties.getTableMetaCheckerInterval());

        rmProperties.setSagaRetryPersistModeUpdate(true);
        Assertions.assertTrue(rmProperties.isSagaRetryPersistModeUpdate());

        rmProperties.setSagaCompensatePersistModeUpdate(true);
        Assertions.assertTrue(rmProperties.isSagaCompensatePersistModeUpdate());

        rmProperties.setTccActionInterceptorOrder(1);
        Assertions.assertEquals(1, rmProperties.getTccActionInterceptorOrder());

        rmProperties.setSqlParserType("type");
        Assertions.assertEquals("type", rmProperties.getSqlParserType());

        rmProperties.setBranchExecutionTimeoutXA(1);
        Assertions.assertEquals(1, rmProperties.getBranchExecutionTimeoutXA());

        rmProperties.setConnectionTwoPhaseHoldTimeoutXA(1);
        Assertions.assertEquals(1, rmProperties.getConnectionTwoPhaseHoldTimeoutXA());

        rmProperties.setApplicationDataLimitCheck(true);
        Assertions.assertTrue(rmProperties.getApplicationDataLimitCheck());

        rmProperties.setApplicationDataLimit(1);
        Assertions.assertEquals(1, rmProperties.getApplicationDataLimit());
    }
}
