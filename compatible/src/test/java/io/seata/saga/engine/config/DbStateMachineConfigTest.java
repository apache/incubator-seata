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

package io.seata.saga.engine.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DbStateMachineConfigTest {

    private static final String APPLICATION_ID = "test";
    private static final String TX_SERVICE_GROUP = "testTxServiceGroup";
    private static final String ACCESS_KEY = "fakeAccessKey";
    private static final String SECRET_KEY = "fakeSecretKey";
    private static final String DB_TYPE = "mysql";
    private static final boolean RM_REPORT_SUCCESS_ENABLE = true;
    private static final boolean SAGA_BRANCH_REGISTER_ENABLE = true;
    private static final String TABLE_PREFIX = "test_";

    @Test
    public void testBuildDefaultSagaTransactionalTemplateThrowsException() {
        DbStateMachineConfig config = new DbStateMachineConfig();
        config.setApplicationId(APPLICATION_ID);
        config.setTxServiceGroup(TX_SERVICE_GROUP);
        config.setAccessKey(ACCESS_KEY);
        config.setSecretKey(SECRET_KEY);
        config.setDbType(DB_TYPE);
        config.setRmReportSuccessEnable(RM_REPORT_SUCCESS_ENABLE);
        config.setSagaBranchRegisterEnable(SAGA_BRANCH_REGISTER_ENABLE);
        config.setTablePrefix(TABLE_PREFIX);

        Assertions.assertEquals(APPLICATION_ID, config.getApplicationId());
        Assertions.assertEquals(TX_SERVICE_GROUP, config.getTxServiceGroup());
        Assertions.assertEquals(ACCESS_KEY, config.getAccessKey());
        Assertions.assertEquals(SECRET_KEY, config.getSecretKey());
        Assertions.assertEquals(DB_TYPE, config.getDbType());
        Assertions.assertTrue(config.isRmReportSuccessEnable());
        Assertions.assertTrue(config.isSagaBranchRegisterEnable());

        // can not find seata-server address, so it should throw an exception
        Assertions.assertThrows(RuntimeException.class, () -> config.buildDefaultSagaTransactionalTemplate());
    }
}
