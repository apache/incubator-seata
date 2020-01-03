/*
 *  Copyright 1999-2019 Seata.io Group.
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
package io.seata.core.constants;

/**
 * The type Configuration keys.
 *
 * @author slievrly
 */
public class ConfigurationKeys {

    /**
     * The constant SERVICE_PREFIX.
     */
    public static final String SERVICE_PREFIX = "service.";

    /**
     * The constant STORE_PREFIX.
     */
    public static final String STORE_PREFIX = "store.";

    /**
     * The constant STORE_MODE.
     */
    public static final String STORE_MODE = STORE_PREFIX + "mode";

    /**
     * The constant STORE_FILE_PREFIX
     */
    public static final String STORE_FILE_PREFIX = STORE_PREFIX + "file.";

    /**
     * The constant STORE_FILE_DIR
     */
    public static final String STORE_FILE_DIR = STORE_FILE_PREFIX + "dir";

    /**
     * The constant SERVICE_GROUP_MAPPING_PREFIX.
     */
    public static final String SERVICE_GROUP_MAPPING_PREFIX = SERVICE_PREFIX + "vgroup_mapping.";
    /**
     * The constant GROUPLIST_POSTFIX.
     */
    public static final String GROUPLIST_POSTFIX = ".grouplist";
    /**
     * The constant SERVER_NODE_SPLIT_CHAR.
     */
    public static final String SERVER_NODE_SPLIT_CHAR = System.getProperty("line.separator");

    /**
     * The constant ENABLE_DEGRADE_POSTFIX.
     */
    public static final String ENABLE_DEGRADE_POSTFIX = "enableDegrade";

    /**
     * The constant CLIENT_PREFIX.
     */
    public static final String CLIENT_PREFIX = "client.";

    /**
     * The constant SERVER_PREFIX.
     */
    public static final String SERVER_PREFIX = "server.";

    /**
     * The constant TRANSPORT_PREFIX.
     */
    public static final String TRANSPORT_PREFIX = "transport.";

    /**
     * The constant CLIENT_RM_PREFIX.
     */
    public static final String CLIENT_RM_PREFIX = CLIENT_PREFIX + "rm.";

    /**
     * The constant CLIENT_ASYNC_COMMIT_BUFFER_LIMIT.
     */
    public static final String CLIENT_ASYNC_COMMIT_BUFFER_LIMIT = CLIENT_RM_PREFIX + "async.commit.buffer.limit";
    /**
     * The constant CLIENT_LOCK_RETRY_TIMES.
     */
    public static final String CLIENT_LOCK_RETRY_TIMES = CLIENT_RM_PREFIX + "lock.retry.times";
    /**
     * The constant CLIENT_LOCK_RETRY_INTERNAL.
     */
    public static final String CLIENT_LOCK_RETRY_INTERNAL = CLIENT_RM_PREFIX + "lock.retry.internal";

    /**
     * The constant SERVICE_SESSION_RELOAD_READ_SIZE
     */
    public static final String SERVICE_SESSION_RELOAD_READ_SIZE = STORE_FILE_PREFIX + "session.reload.read_size";

    /**
     * The constant CLIENT_REPORT_SUCCESS_ENABLE.
     */
    public static final String CLIENT_REPORT_SUCCESS_ENABLE = CLIENT_PREFIX + "report.success.enable";

    /**
     * The constant CLIENT_REPORT_RETRY_COUNT.
     */
    public static final String CLIENT_REPORT_RETRY_COUNT = CLIENT_RM_PREFIX + "report.retry.count";

    /**
     * The constant CLIENT_LOCK_RETRY_POLICY_BRANCH_ROLLBACK_ON_CONFLICT.
     */
    public static final String CLIENT_LOCK_RETRY_POLICY_BRANCH_ROLLBACK_ON_CONFLICT = CLIENT_RM_PREFIX
        + "lock.retry.policy.branch-rollback-on-conflict";

    /**
     * The constant CLIENT_TABLE_META_CHECK_ENABLE.
     */
    public static final String CLIENT_TABLE_META_CHECK_ENABLE = CLIENT_RM_PREFIX + "table.meta.check.enable";

    /**
     * The constant CLIENT_TM_COMMIT_RETRY_TIMES.
     */
    public static final String CLIENT_TM_COMMIT_RETRY_COUNT = CLIENT_PREFIX + "tm.commit.retry.count";

    /**
     * The constant CLIENT_TM_ROLLBACK_RETRY_TIMES.
     */
    public static final String CLIENT_TM_ROLLBACK_RETRY_COUNT = CLIENT_PREFIX + "tm.rollback.retry.count";

    /**
     * The constant SERIALIZE_FOR_RPC.
     */
    public static final String SERIALIZE_FOR_RPC = TRANSPORT_PREFIX + "serialization";

    /**
     * The constant COMPRESSOR_FOR_RPC.
     *
     * @since 0.7.0
     */
    public static final String COMPRESSOR_FOR_RPC = TRANSPORT_PREFIX + "compressor";

    /**
     * The constant STORE_DB_PREFIX.
     */
    public static final String STORE_DB_PREFIX = "store.db.";

    /**
     * The constant STORE_DB_GLOBAL_TABLE.
     */
    public static final String STORE_DB_GLOBAL_TABLE = STORE_DB_PREFIX + "global.table";

    /**
     * The constant STORE_DB_BRANCH_TABLE.
     */
    public static final String STORE_DB_BRANCH_TABLE = STORE_DB_PREFIX + "branch.table";

    /**
     * The constant STORE_DB_GLOBAL_DEFAULT_TABLE.
     */
    public static final String STORE_DB_GLOBAL_DEFAULT_TABLE = "global_table";

    /**
     * The constant STORE_DB_BRANCH_DEFAULT_TABLE.
     */
    public static final String STORE_DB_BRANCH_DEFAULT_TABLE = "branch_table";

    /**
     * The constant STORE_DB_DATASOURCE_TYPE.
     */
    public static final String STORE_DB_DATASOURCE_TYPE = STORE_DB_PREFIX + "datasource";

    /**
     * The constant STORE_DB_TYPE.
     */
    public static final String STORE_DB_TYPE = STORE_DB_PREFIX + "db-type";
    /**
     * The constant STORE_DB_DRIVER_CLASS_NAME.
     */
    public static final String STORE_DB_DRIVER_CLASS_NAME = STORE_DB_PREFIX + "driver-class-name";

    /**
     * The constant STORE_DB_URL.
     */
    public static final String STORE_DB_URL = STORE_DB_PREFIX + "url";

    /**
     * The constant STORE_DB_USER.
     */
    public static final String STORE_DB_USER = STORE_DB_PREFIX + "user";

    /**
     * The constant STORE_DB_PASSWORD.
     */
    public static final String STORE_DB_PASSWORD = STORE_DB_PREFIX + "password";

    /**
     * The constant STORE_DB_MIN_CONN.
     */
    public static final String STORE_DB_MIN_CONN = STORE_DB_PREFIX + "min-conn";

    /**
     * The constant STORE_DB_MAX_CONN.
     */
    public static final String STORE_DB_MAX_CONN = STORE_DB_PREFIX + "max-conn";

    /**
     * The constant STORE_DB_LOG_QUERY_LIMIT.
     */
    public static final String STORE_DB_LOG_QUERY_LIMIT = STORE_DB_PREFIX + "query-limit";

    /**
     * The constant LOCK_DB_TABLE.
     */
    public static final String LOCK_DB_TABLE = STORE_DB_PREFIX + "lock-table";

    /**
     * The constant LOCK_DB_DEFAULT_TABLE.
     */
    public static final String LOCK_DB_DEFAULT_TABLE = "lock_table";

    /**
     * The constant RECOVERY_PREFIX.
     */
    public static final String RECOVERY_PREFIX = SERVER_PREFIX + "recovery.";
    /**
     * The constant COMMITING_RETRY_PERIOD.
     */
    public static final String COMMITING_RETRY_PERIOD = RECOVERY_PREFIX + "committing-retry-period";

    /**
     * The constant ASYN_COMMITING_RETRY_PERIOD.
     */
    public static final String ASYN_COMMITING_RETRY_PERIOD = RECOVERY_PREFIX + "asyn-committing-retry-period";

    /**
     * The constant ROLLBACKING_RETRY_PERIOD.
     */
    public static final String ROLLBACKING_RETRY_PERIOD = RECOVERY_PREFIX + "rollbacking-retry-period";

    /**
     * The constant TIMEOUT_RETRY_PERIOD.
     */
    public static final String TIMEOUT_RETRY_PERIOD = RECOVERY_PREFIX + "timeout-retry-period";

    /**
     * The constant CLIENT_UNDO_PREFIX.
     */
    public static final String CLIENT_UNDO_PREFIX = "client.undo.";

    /**
     * The constant TRANSACTION_UNDO_DATA_VALIDATION.
     */
    public static final String TRANSACTION_UNDO_DATA_VALIDATION = CLIENT_UNDO_PREFIX + "data.validation";
    /**
     * The constant TRANSACTION_UNDO_LOG_SERIALIZATION.
     */
    public static final String TRANSACTION_UNDO_LOG_SERIALIZATION = CLIENT_UNDO_PREFIX + "log.serialization";

    /**
     * The constant METRICS_PREFIX.
     */
    public static final String METRICS_PREFIX = "metrics.";

    /**
     * The constant METRICS_ENABLED.
     */
    public static final String METRICS_ENABLED = "enabled";

    /**
     * The constant METRICS_REGISTRY_TYPE.
     */
    public static final String METRICS_REGISTRY_TYPE = "registry-type";

    /**
     * The constant METRICS_EXPORTER_LIST.
     */
    public static final String METRICS_EXPORTER_LIST = "exporter-list";

    /**
     * The constant SERVER_UNDO_PREFIX.
     */
    public static final String SERVER_UNDO_PREFIX = SERVER_PREFIX + "undo.";

    /**
     * The constant TRANSACTION_UNDO_LOG_SAVE_DAYS.
     */
    public static final String TRANSACTION_UNDO_LOG_SAVE_DAYS = SERVER_UNDO_PREFIX + "log.save.days";

    /**
     * The constant TRANSACTION_UNDO_LOG_DELETE_PERIOD
     */
    public static final String TRANSACTION_UNDO_LOG_DELETE_PERIOD = SERVER_UNDO_PREFIX + "log.delete.period";

    /**
     * The constant TRANSACTION_UNDO_LOG_TABLE
     */
    public static final String TRANSACTION_UNDO_LOG_TABLE = CLIENT_UNDO_PREFIX + "log.table";

    /**
     * The constant TRANSACTION_UNDO_LOG_EXCEPTION_RATE
     */
    public static final String TRANSACTION_LOG_EXCEPTION_RATE = CLIENT_PREFIX + "log.exceptionRate";

    /**
     * The constant TRANSACTION_UNDO_LOG_DEFAULT_TABLE.
     */
    public static final String TRANSACTION_UNDO_LOG_DEFAULT_TABLE = "undo_log";

    /**
     * The constant SUPPORT_PREFIX.
     */
    public static final String SUPPORT_PREFIX = "support.";
    /**
     * The constant SPRING_PREFIX.
     */
    public static final String SPRING_PREFIX = "spring.";
    /**
     * The constant DATASOURCE_PREFIX.
     */
    public static final String DATASOURCE_PREFIX = "datasource.";
    /**
     * The constant DATASOURCE_AUTOPROXY.
     */
    public static final String DATASOURCE_AUTOPROXY = CLIENT_PREFIX + SUPPORT_PREFIX + SPRING_PREFIX + DATASOURCE_PREFIX
        + "autoproxy";

    /**
     * The constant MAX_COMMIT_RETRY_TIMEOUT.
     */
    public static final String MAX_COMMIT_RETRY_TIMEOUT = SERVER_PREFIX + "max.commit.retry.timeout";

    /**
     * The constant MAX_ROLLBACK_RETRY_TIMEOUT.
     */
    public static final String MAX_ROLLBACK_RETRY_TIMEOUT = SERVER_PREFIX + "max.rollback.retry.timeout";

    /**
     * The constant TRANSPORT_TYPE
     */
    public static final String TRANSPORT_TYPE = TRANSPORT_PREFIX + "type";

    /**
     * The constant TRANSPORT_SERVER
     */
    public static final String TRANSPORT_SERVER = TRANSPORT_PREFIX + "server";

    /**
     * The constant TRANSPORT_HEARTBEAT
     */
    public static final String TRANSPORT_HEARTBEAT = TRANSPORT_PREFIX + "heartbeat";

    /**
     * The constant THREAD_FACTORY_PREFIX
     */
    public static final String THREAD_FACTORY_PREFIX = TRANSPORT_PREFIX + "thread-factory.";

    /**
     * The constant BOSS_THREAD_PREFIX
     */
    public static final String BOSS_THREAD_PREFIX = THREAD_FACTORY_PREFIX + "boss-thread-prefix";

    /**
     * The constant WORKER_THREAD_PREFIX
     */
    public static final String WORKER_THREAD_PREFIX = THREAD_FACTORY_PREFIX + "worker-thread-prefix";

    /**
     * The constant SERVER_EXECUTOR_THREAD_PREFIX
     */
    public static final String SERVER_EXECUTOR_THREAD_PREFIX = THREAD_FACTORY_PREFIX + "server-executor-thread-prefix";

    /**
     * The constant SHARE_BOSS_WORKER
     */
    public static final String SHARE_BOSS_WORKER = THREAD_FACTORY_PREFIX + "share-boss-worker";

    /**
     * The constant CLIENT_SELECTOR_THREAD_PREFIX
     */
    public static final String CLIENT_SELECTOR_THREAD_PREFIX = THREAD_FACTORY_PREFIX + "client-selector-thread-prefix";

    /**
     * The constant CLIENT_SELECTOR_THREAD_SIZE
     */
    public static final String CLIENT_SELECTOR_THREAD_SIZE = THREAD_FACTORY_PREFIX + "client-selector-thread-size";

    /**
     * The constant CLIENT_WORKER_THREAD_PREFIX
     */
    public static final String CLIENT_WORKER_THREAD_PREFIX = THREAD_FACTORY_PREFIX + "client-worker-thread-prefix";

    /**
     * The constant BOSS_THREAD_SIZE
     */
    public static final String BOSS_THREAD_SIZE = THREAD_FACTORY_PREFIX + "boss-thread-size";

    /**
     * The constant WORKER_THREAD_SIZE
     */
    public static final String WORKER_THREAD_SIZE = THREAD_FACTORY_PREFIX + "worker-thread-size";

    /**
     * The constant SHUTDOWN_PREFIX
     */
    public static final String SHUTDOWN_PREFIX = TRANSPORT_PREFIX + "shutdown.";

    /**
     * The constant SHUNDOWN_WAIT
     */
    public static final String SHUNDOWN_WAIT = SHUTDOWN_PREFIX + "wait";

    /**
     * The constant ENABLE_CLIENT_BATCH_SEND_REQUEST
     */
    public static final String ENABLE_CLIENT_BATCH_SEND_REQUEST = TRANSPORT_PREFIX + "enable-client-batch-send-request";

    /**
     * The constant DISABLE_GLOBAL_TRANSACTION.
     */
    public static final String DISABLE_GLOBAL_TRANSACTION = SERVICE_PREFIX + "disableGlobalTransaction";
}
