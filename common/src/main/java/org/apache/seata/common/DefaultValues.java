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

import java.time.Duration;

public interface DefaultValues {
    int DEFAULT_CLIENT_LOCK_RETRY_INTERVAL = 10;
    int DEFAULT_TM_DEGRADE_CHECK_ALLOW_TIMES = 10;
    int DEFAULT_CLIENT_LOCK_RETRY_TIMES = 30;
    boolean DEFAULT_CLIENT_LOCK_RETRY_POLICY_BRANCH_ROLLBACK_ON_CONFLICT = true;
    int DEFAULT_LOG_EXCEPTION_RATE = 100;
    int DEFAULT_CLIENT_ASYNC_COMMIT_BUFFER_LIMIT = 10000;
    int DEFAULT_TM_DEGRADE_CHECK_PERIOD = 2000;
    int DEFAULT_CLIENT_REPORT_RETRY_COUNT = 5;
    boolean DEFAULT_CLIENT_REPORT_SUCCESS_ENABLE = false;
    boolean DEFAULT_CLIENT_TABLE_META_CHECK_ENABLE = true;
    long DEFAULT_TABLE_META_CHECKER_INTERVAL = 60000L;
    boolean DEFAULT_TM_DEGRADE_CHECK = false;
    boolean DEFAULT_CLIENT_SAGA_BRANCH_REGISTER_ENABLE = false;

    /**
     * The default session store dir
     */
    String DEFAULT_SESSION_STORE_FILE_DIR = "sessionStore";
    boolean DEFAULT_CLIENT_SAGA_RETRY_PERSIST_MODE_UPDATE = false;
    boolean DEFAULT_CLIENT_SAGA_COMPENSATE_PERSIST_MODE_UPDATE = false;
    String DEFAULT_RAFT_SERIALIZATION = "jackson";
    String DEFAULT_RAFT_COMPRESSOR = "none";

    /**
     * Shutdown timeout default 3s
     */
    int DEFAULT_SHUTDOWN_TIMEOUT_SEC = 13;
    int DEFAULT_SELECTOR_THREAD_SIZE = 1;
    int DEFAULT_BOSS_THREAD_SIZE = 1;


    String DEFAULT_SELECTOR_THREAD_PREFIX = "NettyClientSelector";
    String DEFAULT_WORKER_THREAD_PREFIX = "NettyClientWorkerThread";
    @Deprecated
    boolean DEFAULT_ENABLE_CLIENT_BATCH_SEND_REQUEST = true;
    boolean DEFAULT_ENABLE_TM_CLIENT_BATCH_SEND_REQUEST = false;
    boolean DEFAULT_ENABLE_RM_CLIENT_BATCH_SEND_REQUEST = true;
    boolean DEFAULT_ENABLE_TC_SERVER_BATCH_SEND_RESPONSE = false;

    boolean DEFAULT_CLIENT_CHANNEL_CHECK_FAIL_FAST = true;

    String DEFAULT_BOSS_THREAD_PREFIX = "NettyBoss";
    String DEFAULT_NIO_WORKER_THREAD_PREFIX = "NettyServerNIOWorker";
    String DEFAULT_EXECUTOR_THREAD_PREFIX = "NettyServerBizHandler";
    String DEFAULT_PROTOCOL = "seata";

    boolean DEFAULT_TRANSPORT_HEARTBEAT = true;
    boolean DEFAULT_TRANSACTION_UNDO_DATA_VALIDATION = true;
    String DEFAULT_TRANSACTION_UNDO_LOG_SERIALIZATION = "jackson";
    boolean DEFAULT_ONLY_CARE_UPDATE_COLUMNS = true;
    /**
     * The constant  DEFAULT_TRANSACTION_UNDO_LOG_TABLE.
     */
    String DEFAULT_TRANSACTION_UNDO_LOG_TABLE = "undo_log";
    /**
     * The constant DEFAULT_STORE_DB_GLOBAL_TABLE.
     */
    String DEFAULT_STORE_DB_GLOBAL_TABLE = "global_table";

    /**
     * The constant DEFAULT_STORE_DB_BRANCH_TABLE.
     */
    String DEFAULT_STORE_DB_BRANCH_TABLE = "branch_table";

    /**
     * The constant DEFAULT_LOCK_DB_TABLE.
     */
    String DEFAULT_LOCK_DB_TABLE = "lock_table";

    /**
     * the constant DEFAULT_DISTRIBUTED_LOCK_DB_TABLE
     */
    String DEFAULT_DISTRIBUTED_LOCK_DB_TABLE = "distributed_lock";

    int DEFAULT_TM_COMMIT_RETRY_COUNT = 5;
    int DEFAULT_TM_ROLLBACK_RETRY_COUNT = 5;
    int DEFAULT_GLOBAL_TRANSACTION_TIMEOUT = 60000;

    String DEFAULT_TX_GROUP = "default_tx_group";
    @Deprecated
    String DEFAULT_TX_GROUP_OLD = "my_test_tx_group";
    String DEFAULT_TC_CLUSTER = "default";
    String DEFAULT_GROUPLIST = "127.0.0.1:8091";

    String DEFAULT_DATA_SOURCE_PROXY_MODE = "AT";

    boolean DEFAULT_DISABLE_GLOBAL_TRANSACTION = false;

    //currently not use and will be delete in the next version
    @Deprecated
    int SERVICE_DEFAULT_PORT = 8091;

    int SERVICE_OFFSET_SPRING_BOOT = 1000;

    String SERVER_PORT = "seata.server.port";

    String SERVER_DEFAULT_STORE_MODE = "file";

    String DEFAULT_SAGA_JSON_PARSER = "fastjson";

    // default tcc business action context json parser
    String DEFAULT_TCC_BUSINESS_ACTION_CONTEXT_JSON_PARSER = "fastjson";

    boolean DEFAULT_SERVER_ENABLE_CHECK_AUTH = true;

    String DEFAULT_LOAD_BALANCE = "XID";
    int VIRTUAL_NODES_DEFAULT = 10;

    String DEFAULT_SEATA_GROUP = "default";

    /**
     * the constant DEFAULT_CLIENT_UNDO_COMPRESS_ENABLE
     */
    boolean DEFAULT_CLIENT_UNDO_COMPRESS_ENABLE = true;

    /**
     * the constant DEFAULT_CLIENT_UNDO_COMPRESS_TYPE
     */
    String DEFAULT_CLIENT_UNDO_COMPRESS_TYPE = "zip";

    /**
     * the constant DEFAULT_CLIENT_UNDO_COMPRESS_THRESHOLD
     */
    String DEFAULT_CLIENT_UNDO_COMPRESS_THRESHOLD = "64k";


    /**
     * the constant DEFAULT_RETRY_DEAD_THRESHOLD
     */
    int DEFAULT_RETRY_DEAD_THRESHOLD = 2 * 60 * 1000 + 10 * 1000;

    /**
     * the constant TM_INTERCEPTOR_ORDER
     */
    int TM_INTERCEPTOR_ORDER = Integer.MIN_VALUE + 1000;

    /**
     * the constant TCC_ACTION_INTERCEPTOR_ORDER
     */
    int TCC_ACTION_INTERCEPTOR_ORDER = Integer.MIN_VALUE + 1000;

    /**
     * the constant SAGA_ACTION_INTERCEPTOR_ORDER
     */
    int SAGA_ACTION_INTERCEPTOR_ORDER = Integer.MIN_VALUE + 1000;

    /**
     * the constant DEFAULT_DISTRIBUTED_LOCK_EXPIRE
     */
    int DEFAULT_DISTRIBUTED_LOCK_EXPIRE = 10000;

    /**
     * the constant DEFAULT_COMMON_FENCE_CLEAN_PERIOD
     */
    int DEFAULT_COMMON_FENCE_CLEAN_PERIOD = 1;
    /**
     * the constant DEFAULT_COMMON_FENCE_LOG_TABLE_NAME
     */
    String DEFAULT_COMMON_FENCE_LOG_TABLE_NAME = "tcc_fence_log";
    /**
     * the constant COMMON_FENCE_BEAN_NAME
     */
    String COMMON_FENCE_BEAN_NAME = "tccFenceConfig";

    /**
     * the constant DEFAULT_RPC_RM_REQUEST_TIMEOUT
     */
    long DEFAULT_RPC_RM_REQUEST_TIMEOUT = Duration.ofSeconds(15).toMillis();

    /**
     * the constant DEFAULT_RPC_TM_REQUEST_TIMEOUT
     */
    long DEFAULT_RPC_TM_REQUEST_TIMEOUT = Duration.ofSeconds(30).toMillis();

    /**
     * the constant DEFAULT_RPC_TC_REQUEST_TIMEOUT
     */
    long DEFAULT_RPC_TC_REQUEST_TIMEOUT = Duration.ofSeconds(15).toMillis();

    /**
     * the constant DEFAULT_APPLICATION_DATA_SIZE_LIMIT
     */
    int DEFAULT_APPLICATION_DATA_SIZE_LIMIT = 64000;

    /**
     * the constant DEFAULT_XAER_NOTA_RETRY_TIMEOUT
     */
    int DEFAULT_XAER_NOTA_RETRY_TIMEOUT = 60000;

    /**
     * the constant DEFAULT_XA_BRANCH_EXECUTION_TIMEOUT
     */
    int DEFAULT_XA_BRANCH_EXECUTION_TIMEOUT = 60000;

    /**
     * the constant DEFAULT_XA_TWO_PHASE_WAIT_TIMEOUT
     */
    int DEFAULT_XA_CONNECTION_TWO_PHASE_HOLD_TIMEOUT = 10000;

    /**
     * the constant DEFAULT_SERVER_RAFT_ELECTION_TIMEOUT_MS
     */
    int DEFAULT_SERVER_RAFT_ELECTION_TIMEOUT_MS = 1000;
    /**
     * the constant DEFAULT_COMMITING_RETRY_PERIOD
     */
    int DEFAULT_COMMITING_RETRY_PERIOD = 1000;

    /**
     * the constant DEFAULT_ASYNC_COMMITTING_RETRY_PERIOD
     */
    int DEFAULT_ASYNC_COMMITTING_RETRY_PERIOD = 1000;

    /**
     * the constant DEFAULT_ROLLBACKING_RETRY_PERIOD
     */
    int DEFAULT_ROLLBACKING_RETRY_PERIOD = 1000;

    /**
     * the constant DEFAULT_TIMEOUT_RETRY_PERIOD
     */
    int DEFAULT_TIMEOUT_RETRY_PERIOD = 1000;

    /**
     * the constant DEFAULT_UNDO_LOG_DELETE_PERIOD
     */
    long DEFAULT_UNDO_LOG_DELETE_PERIOD = 24 * 60 * 60 * 1000;

    /**
     * the constant DEFAULT_SERVICE_SESSION_RELOAD_READ_SIZE
     */
    int DEFAULT_SERVICE_SESSION_RELOAD_READ_SIZE = 100;

    /**
     * the constant DEFAULT_PROMETHEUS_PORT
     */
    int DEFAULT_PROMETHEUS_PORT = 9898;

    /**
     * the const DEFAULT_METRICS_ENABLED
     */
    boolean DEFAULT_METRICS_ENABLED = true;

    /**
     * the const DEFAULT_METRICS_REGISTRY_TYPE
     */
    String DEFAULT_METRICS_REGISTRY_TYPE = "compact";

    /**
     * the const DEFAULT_METRICS_EXPORTER_LIST
     */
    String DEFAULT_METRICS_EXPORTER_LIST = "prometheus";

    /**
     * the const DEFAULT_MAX_COMMIT_RETRY_TIMEOUT
     */
    long DEFAULT_MAX_COMMIT_RETRY_TIMEOUT = -1L;

    /**
     * the const DEFAULT_MAX_ROLLBACK_RETRY_TIMEOUT
     */
    long DEFAULT_MAX_ROLLBACK_RETRY_TIMEOUT = -1L;

    /**
     * the const DEFAULT_ROLLBACK_RETRY_TIMEOUT_UNLOCK_ENABLE
     */
    boolean DEFAULT_ROLLBACK_RETRY_TIMEOUT_UNLOCK_ENABLE = false;

    /**
     * DEFAULT_DISTRIBUTED_LOCK_EXPIRE_TIME
     */
    long DEFAULT_DISTRIBUTED_LOCK_EXPIRE_TIME = 10000;

    /**
     * DEFAULT_ENABLE_BRANCH_ASYNC_REMOVE
     */
    boolean DEFAULT_ENABLE_BRANCH_ASYNC_REMOVE = false;

    int DEFAULT_DB_MAX_CONN = 100;

    int DEFAULT_DB_MIN_CONN = 10;

    int DEFAULT_REDIS_MAX_IDLE = 100;

    int DEFAULT_REDIS_MAX_TOTAL = 100;

    int DEFAULT_REDIS_MIN_IDLE = 10;

    int DEFAULT_QUERY_LIMIT = 1000;

    /**
     * Default druid location in classpath
     */
    String DRUID_LOCATION = "lib/sqlparser/druid.jar";

    int DEFAULT_ROCKET_MQ_MSG_TIMEOUT = 60 * 1000;
}
