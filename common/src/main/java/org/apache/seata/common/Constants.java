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

import java.nio.charset.Charset;

/**
 * The type Constants.
 *
 */
public interface Constants {

    /**
     * The constant IP_PORT_SPLIT_CHAR.
     */
    String IP_PORT_SPLIT_CHAR = ":";
    /**
     * The constant CLIENT_ID_SPLIT_CHAR.
     */
    String CLIENT_ID_SPLIT_CHAR = ":";
    /**
     * The constant ENDPOINT_BEGIN_CHAR.
     */
    String ENDPOINT_BEGIN_CHAR = "/";
    /**
     * The constant DBKEYS_SPLIT_CHAR.
     */
    String DBKEYS_SPLIT_CHAR = ",";

    /**
     * The constant ROW_LOCK_KEY_SPLIT_CHAR.
     */
    String ROW_LOCK_KEY_SPLIT_CHAR = ";";

    /**
     * The constant HIDE_KEY_PREFIX_CHAR.
     */
    String HIDE_KEY_PREFIX_CHAR = ".";

    /**
     * the start time of transaction
     */
    String START_TIME = "start-time";

    /**
     * app name
     */
    String APP_NAME = "appName";

    /**
     * TCC start time
     */
    String ACTION_START_TIME = "action-start-time";

    /**
     * TCC name
     */
    String ACTION_NAME = "actionName";

    /**
     * Use TCC fence
     */
    String USE_COMMON_FENCE = "useTCCFence";

    /**
     * phase one method name
     */
    String PREPARE_METHOD = "sys::prepare";

    /**
     * phase two commit method name
     */
    String COMMIT_METHOD = "sys::commit";

    /**
     * phase two rollback method name
     */
    String ROLLBACK_METHOD = "sys::rollback";

    /**
     * host ip
     */
    String HOST_NAME = "host-name";

    /**
     * branch context
     */
    String TX_ACTION_CONTEXT = "actionContext";

    /**
     * isolation
     */
    String TX_ISOLATION = "isolation";

    /**
     * default charset name
     */
    String DEFAULT_CHARSET_NAME = "UTF-8";

    /**
     * default charset is utf-8
     */
    Charset DEFAULT_CHARSET = Charset.forName(DEFAULT_CHARSET_NAME);
    /**
     * The constant OBJECT_KEY_SPRING_APPLICATION_CONTEXT
     */
    String OBJECT_KEY_SPRING_APPLICATION_CONTEXT = "springApplicationContext";
    /**
     * The constant OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT
     */
    String OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT = "springConfigurableEnvironment";
    /**
     * The constant BEAN_NAME_SPRING_APPLICATION_CONTEXT_PROVIDER
     */
    String BEAN_NAME_SPRING_APPLICATION_CONTEXT_PROVIDER = "springApplicationContextProvider";
    /**
     * The constant BEAN_NAME_SPRING_FENCE_CONFIG
     */
    String BEAN_NAME_SPRING_FENCE_CONFIG = "SpringFenceConfig";
    /**
     * The constant BEAN_NAME_FAILURE_HANDLER
     */
    String BEAN_NAME_FAILURE_HANDLER = "failureHandler";
    /**
     * The constant SAGA_TRANS_NAME_PREFIX
     */
    String SAGA_TRANS_NAME_PREFIX = "$Saga_";

    /**
     * The constant RETRY_ROLLBACKING
     */
    String RETRY_ROLLBACKING = "RetryRollbacking";

    /**
     * The constant RETRY_COMMITTING
     */
    String RETRY_COMMITTING = "RetryCommitting";

    /**
     * The constant ASYNC_COMMITTING
     */
    String ASYNC_COMMITTING = "AsyncCommitting";

    /**
     * The constant TX_TIMEOUT_CHECK
     */
    String TX_TIMEOUT_CHECK = "TxTimeoutCheck";

    /**
     * The constant UNDOLOG_DELETE
     */
    String UNDOLOG_DELETE = "UndologDelete";

    /**
     * The constant SYNC_PROCESSING
     */
    String SYNC_PROCESSING = "SyncProcessing";

    /**
     * The constant Committing
     */
    String COMMITTING = "Committing";

    /**
     * The constant Rollbacking
     */
    String ROLLBACKING = "Rollbacking";

    /**
     * The constant AUTO_COMMIT
     */
    String AUTO_COMMIT = "autoCommit";

    /**
     * The constant SKIP_CHECK_LOCK
     */
    String SKIP_CHECK_LOCK = "skipCheckLock";

    /**
     * The constant REGISTRY_TYPE_SPLIT_CHAR.
     */
    String REGISTRY_TYPE_SPLIT_CHAR = ",";
    
    /**
     * phase two compensation method name
     */
    String COMPENSATION_METHOD = "sys::compensation";

    /**
     * phase STORE_REDIS_TYPE_PIPELINE
     */
    String STORE_REDIS_TYPE_PIPELINE = "pipeline";

    /**
     * The constant FASTJSON_JSON_PARSER_NAME
     */
    String FASTJSON_JSON_PARSER_NAME = "fastjson";

    /**
     * The constant JACKSON_JSON_PARSER_NAME
     */
    String JACKSON_JSON_PARSER_NAME = "jackson";

    /**
     * The constant GSON_JSON_PARSER_NAME
     */
    String GSON_JSON_PARSER_NAME = "gson";

    /**
     * The constant JACKSON_JSON_TEXT_PREFIX
     */
    String JACKSON_JSON_TEXT_PREFIX = "{\"@class\":";

    /**
     * The constant DEAD_LOCK_SQL_STATE
     */
    String DEAD_LOCK_SQL_STATE = "40001";

    /**
     * The constant DEAD_LOCK_ERROR_CODE
     */
    int DEAD_LOCK_ERROR_CODE = 1213;

}
