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
package org.apache.seata.common;

import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The type Constants test.
 */
public class ConstantsTest {

    @Test
    public void testStringConstants() {
        assertEquals(":", Constants.IP_PORT_SPLIT_CHAR);
        assertEquals(":", Constants.CLIENT_ID_SPLIT_CHAR);
        assertEquals("/", Constants.ENDPOINT_BEGIN_CHAR);
        assertEquals(",", Constants.DBKEYS_SPLIT_CHAR);
        assertEquals(";", Constants.ROW_LOCK_KEY_SPLIT_CHAR);
        assertEquals(".", Constants.HIDE_KEY_PREFIX_CHAR);
        assertEquals("start-time", Constants.START_TIME);
        assertEquals("appName", Constants.APP_NAME);
        assertEquals("action-start-time", Constants.ACTION_START_TIME);
        assertEquals("actionName", Constants.ACTION_NAME);
        assertEquals("useTCCFence", Constants.USE_COMMON_FENCE);
        assertEquals("sys::prepare", Constants.PREPARE_METHOD);
        assertEquals("sys::commit", Constants.COMMIT_METHOD);
        assertEquals("sys::rollback", Constants.ROLLBACK_METHOD);
        assertEquals("host-name", Constants.HOST_NAME);
        assertEquals("actionContext", Constants.TX_ACTION_CONTEXT);
        assertEquals("isolation", Constants.TX_ISOLATION);
        assertEquals("UTF-8", Constants.DEFAULT_CHARSET_NAME);
        assertEquals("springApplicationContext", Constants.OBJECT_KEY_SPRING_APPLICATION_CONTEXT);
        assertEquals("springConfigurableEnvironment", Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        assertEquals("springApplicationContextProvider", Constants.BEAN_NAME_SPRING_APPLICATION_CONTEXT_PROVIDER);
        assertEquals("SpringFenceConfig", Constants.BEAN_NAME_SPRING_FENCE_CONFIG);
        assertEquals("failureHandler", Constants.BEAN_NAME_FAILURE_HANDLER);
        assertEquals("$Saga_", Constants.SAGA_TRANS_NAME_PREFIX);
        assertEquals("RetryRollbacking", Constants.RETRY_ROLLBACKING);
        assertEquals("RetryCommitting", Constants.RETRY_COMMITTING);
        assertEquals("AsyncCommitting", Constants.ASYNC_COMMITTING);
        assertEquals("TxTimeoutCheck", Constants.TX_TIMEOUT_CHECK);
        assertEquals("UndologDelete", Constants.UNDOLOG_DELETE);
        assertEquals("SyncProcessing", Constants.SYNC_PROCESSING);
        assertEquals("Committing", Constants.COMMITTING);
        assertEquals("Rollbacking", Constants.ROLLBACKING);
        assertEquals("END", Constants.END);
        assertEquals("autoCommit", Constants.AUTO_COMMIT);
        assertEquals("skipCheckLock", Constants.SKIP_CHECK_LOCK);
        assertEquals(",", Constants.REGISTRY_TYPE_SPLIT_CHAR);
        assertEquals("sys::compensation", Constants.COMPENSATION_METHOD);
        assertEquals("pipeline", Constants.STORE_REDIS_TYPE_PIPELINE);
        assertEquals("fastjson", Constants.FASTJSON_JSON_PARSER_NAME);
        assertEquals("jackson", Constants.JACKSON_JSON_PARSER_NAME);
        assertEquals("gson", Constants.GSON_JSON_PARSER_NAME);
        assertEquals("{\"@class\":", Constants.JACKSON_JSON_TEXT_PREFIX);
        assertEquals("jackson", DefaultValues.DEFAULT_TCC_BUSINESS_ACTION_CONTEXT_JSON_PARSER);
        assertEquals("40001", Constants.DEAD_LOCK_SQL_STATE);
        assertEquals("X-SEATA-RAFT-GROUP", Constants.RAFT_GROUP_HEADER);
    }

    @Test
    public void testIntegerConstants() {
        assertEquals(1213, Constants.DEAD_LOCK_ERROR_CODE);
    }

    @Test
    public void testCharsetConstants() {
        assertNotNull(Constants.DEFAULT_CHARSET);
        assertEquals(Charset.forName("UTF-8"), Constants.DEFAULT_CHARSET);
    }
}
