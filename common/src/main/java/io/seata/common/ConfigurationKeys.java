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
package io.seata.common;

/**
 * The type Configuration keys.
 *
 * @author slievrly
 */
public interface ConfigurationKeys {
    /**
     * The constant SEATA_FILE_ROOT_CONFIG
     */
    String SEATA_FILE_ROOT_CONFIG = "seata";
    /**
     * The constant FILE_ROOT_REGISTRY.
     */
    String FILE_ROOT_REGISTRY = "registry";

    /**
     * The constant FILE_ROOT_CONFIG.
     */
    String FILE_ROOT_CONFIG = "config";
    /**
     * The constant FILE_CONFIG_SPLIT_CHAR.
     */
    String FILE_CONFIG_SPLIT_CHAR = ".";

    /**
     * The constant FILE_ROOT_PREFIX_REGISTRY.
     */
    String FILE_ROOT_PREFIX_REGISTRY = FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR;

    /**
     * The constant FILE_ROOT_PREFIX_CONFIG.
     */
    String FILE_ROOT_PREFIX_CONFIG = FILE_ROOT_CONFIG + FILE_CONFIG_SPLIT_CHAR;

    /**
     * The constant SEATA_FILE_PREFIX_ROOT_CONFIG
     */
    String SEATA_FILE_PREFIX_ROOT_CONFIG = SEATA_FILE_ROOT_CONFIG + FILE_CONFIG_SPLIT_CHAR;

    /**
     * The constant FILE_ROOT_TYPE.
     */
    String FILE_ROOT_TYPE = "type";
    /**
     * The constant DATA_TYPE.
     */
    String DATA_TYPE = "dataType";

    /**
     * The constant SEATA_PREFIX.
     */
    String SEATA_PREFIX = SEATA_FILE_ROOT_CONFIG + ".";

    /**
     * The constant SERVICE_PREFIX.
     */
    String SERVICE_PREFIX = "service.";

    /**
     * The constant STORE_PREFIX.
     */
    String STORE_PREFIX = "store.";

    /**
     * The constant SESSION_PREFIX.
     */
    String SESSION_PREFIX = "session.";

    /**
     * The constant STORE_SESSION_PREFIX.
     */
    String STORE_SESSION_PREFIX = STORE_PREFIX + SESSION_PREFIX;

    /**
     * The constant MODE.
     */
    String MODE = "mode";

    /**
     * The constant STORE_MODE.
     */
    String STORE_MODE = STORE_PREFIX + MODE;

    /**
     * The constant SERVER_STORE_MODE.
     */
    String SERVER_STORE_MODE = SEATA_PREFIX + STORE_PREFIX + MODE;

    /**
     * The constant STORE_LOCK_MODE.
     */
    String STORE_LOCK_MODE = STORE_PREFIX + "lock." + MODE;

    /**
     * The constant SERVER_STORE_LOCK_MODE.
     */
    String SERVER_STORE_LOCK_MODE = SEATA_PREFIX + STORE_PREFIX + "lock." + MODE;

    /**
     * The constant STORE_SESSION_MODE.
     */
    String STORE_SESSION_MODE = STORE_SESSION_PREFIX + MODE;

    /**
     * The constant SERVER_STORE_SESSION_MODE.
     */
    String SERVER_STORE_SESSION_MODE = SEATA_PREFIX + STORE_SESSION_PREFIX + MODE;

    /**
     * The constant STORE_PUBLIC_KEY.
     */
    String STORE_PUBLIC_KEY = STORE_PREFIX + "publicKey";

    /**
     * The constant STORE_FILE_PREFIX
     */
    String STORE_FILE_PREFIX = STORE_PREFIX + "file.";

    /**
     * The constant STORE_FILE_DIR
     */
    String STORE_FILE_DIR = STORE_FILE_PREFIX + "dir";

    /**
     * The constant SERVICE_GROUP_MAPPING_PREFIX.
     */
    String SERVICE_GROUP_MAPPING_PREFIX = SERVICE_PREFIX + "vgroupMapping.";
    /**
     * The constant GROUPLIST_POSTFIX.
     */
    String GROUPLIST_POSTFIX = ".grouplist";
    /**
     * The constant SERVER_NODE_SPLIT_CHAR.
     */
    String SERVER_NODE_SPLIT_CHAR = System.getProperty("line.separator");

    /**
     * The constant ENABLE_DEGRADE_POSTFIX.
     */
    String ENABLE_DEGRADE_POSTFIX = "enableDegrade";

    /**
     * The constant CLIENT_PREFIX.
     */
    String CLIENT_PREFIX = "client.";

    /**
     * The constant SERVER_PREFIX.
     */
    String SERVER_PREFIX = "server.";

    /**
     * The constant TRANSPORT_PREFIX.
     */
    String TRANSPORT_PREFIX = "transport.";

    /**
     * The constant CLIENT_RM_PREFIX.
     */
    String CLIENT_RM_PREFIX = CLIENT_PREFIX + "rm.";

    /**
     * The constant CLIENT_ASYNC_COMMIT_BUFFER_LIMIT.
     */
    String CLIENT_ASYNC_COMMIT_BUFFER_LIMIT = CLIENT_RM_PREFIX + "asyncCommitBufferLimit";
    /**
     * The constant CLIENT_RM_LOCK_PREFIX.
     */
    String CLIENT_RM_LOCK_PREFIX = CLIENT_RM_PREFIX + "lock.";

    /**
     * The constant CLIENT_LOCK_RETRY_TIMES.
     */
    String CLIENT_LOCK_RETRY_TIMES = CLIENT_RM_LOCK_PREFIX + "retryTimes";
    /**
     * The constant CLIENT_LOCK_RETRY_INTERVAL.
     */
    String CLIENT_LOCK_RETRY_INTERVAL = CLIENT_RM_LOCK_PREFIX + "retryInterval";
    /**
     * The constant CLIENT_LOCK_RETRY_POLICY_BRANCH_ROLLBACK_ON_CONFLICT.
     */
    String CLIENT_LOCK_RETRY_POLICY_BRANCH_ROLLBACK_ON_CONFLICT = CLIENT_RM_LOCK_PREFIX + "retryPolicyBranchRollbackOnConflict";

    /**
     * The constant SERVICE_SESSION_RELOAD_READ_SIZE
     */
    String SERVICE_SESSION_RELOAD_READ_SIZE = STORE_FILE_PREFIX + "sessionReloadReadSize";

    /**
     * The constant CLIENT_REPORT_SUCCESS_ENABLE.
     */
    String CLIENT_REPORT_SUCCESS_ENABLE = CLIENT_RM_PREFIX + "reportSuccessEnable";

    /**
     * The constant CLIENT_SAGA_BRANCH_REGISTER_ENABLE.
     */
    String CLIENT_SAGA_BRANCH_REGISTER_ENABLE = CLIENT_RM_PREFIX + "sagaBranchRegisterEnable";

    /**
     * The constant CLIENT_SAGA_JSON_PARSER.
     */
    String CLIENT_SAGA_JSON_PARSER = CLIENT_RM_PREFIX + "sagaJsonParser";

    /**
     * The constant CLIENT_SAGA_RETRY_PERSIST_MODE_UPDATE.
     */
    String CLIENT_SAGA_RETRY_PERSIST_MODE_UPDATE = CLIENT_RM_PREFIX + "sagaRetryPersistModeUpdate";

    /**
     * The constant CLIENT_SAGA_COMPENSATE_PERSIST_MODE_UPDATE.
     */
    String CLIENT_SAGA_COMPENSATE_PERSIST_MODE_UPDATE = CLIENT_RM_PREFIX + "sagaCompensatePersistModeUpdate";

    /**
     * The constant CLIENT_REPORT_RETRY_COUNT.
     */
    String CLIENT_REPORT_RETRY_COUNT = CLIENT_RM_PREFIX + "reportRetryCount";

    /**
     * The constant CLIENT_TABLE_META_CHECK_ENABLE.
     */
    String CLIENT_TABLE_META_CHECK_ENABLE = CLIENT_RM_PREFIX + "tableMetaCheckEnable";

    /**
     * The constant CLIENT_TABLE_META_CHECKER_INTERVAL.
     */
    String CLIENT_TABLE_META_CHECKER_INTERVAL = CLIENT_RM_PREFIX + "tableMetaCheckerInterval";

    /**
     * The constant TCC_ACTION_INTERCEPTOR_ORDER.
     */
    String TCC_ACTION_INTERCEPTOR_ORDER = CLIENT_RM_PREFIX + "tccActionInterceptorOrder";

    /**
     * The constant CLIENT_TM_PREFIX.
     */
    String CLIENT_TM_PREFIX = CLIENT_PREFIX + "tm.";
    /**
     * The constant CLIENT_TM_COMMIT_RETRY_TIMES.
     */
    String CLIENT_TM_COMMIT_RETRY_COUNT = CLIENT_TM_PREFIX + "commitRetryCount";

    /**
     * The constant CLIENT_TM_ROLLBACK_RETRY_TIMES.
     */
    String CLIENT_TM_ROLLBACK_RETRY_COUNT = CLIENT_TM_PREFIX + "rollbackRetryCount";

    /**
     * The constant DEFAULT_GLOBAL_TRANSACTION_TIMEOUT.
     */
    String DEFAULT_GLOBAL_TRANSACTION_TIMEOUT = CLIENT_TM_PREFIX + "defaultGlobalTransactionTimeout";

    /**
     * The constant SERIALIZE_FOR_RPC.
     */
    String SERIALIZE_FOR_RPC = TRANSPORT_PREFIX + "serialization";

    /**
     * The constant COMPRESSOR_FOR_RPC.
     *
     * @since 0.7.0
     */
    String COMPRESSOR_FOR_RPC = TRANSPORT_PREFIX + "compressor";

    /**
     * The constant STORE_DB_PREFIX.
     */
    String STORE_DB_PREFIX = "store.db.";

    /**
     * The constant STORE_REDIS_PREFIX.
     */
    String STORE_REDIS_PREFIX = "store.redis.";

    /**
     * The constant STORE_DB_GLOBAL_TABLE.
     */
    String STORE_DB_GLOBAL_TABLE = STORE_DB_PREFIX + "globalTable";

    /**
     * The constant STORE_DB_BRANCH_TABLE.
     */
    String STORE_DB_BRANCH_TABLE = STORE_DB_PREFIX + "branchTable";

    /**
     * The constant DISTRIBUTED_LOCK_DB_TABLE.
     */
    String DISTRIBUTED_LOCK_DB_TABLE = STORE_DB_PREFIX + "distributedLockTable";

    /**
     * The constant STORE_DB_DATASOURCE_TYPE.
     */
    String STORE_DB_DATASOURCE_TYPE = STORE_DB_PREFIX + "datasource";

    /**
     * The constant STORE_DB_TYPE.
     */
    String STORE_DB_TYPE = STORE_DB_PREFIX + "dbType";

    /**
     * The constant STORE_DB_DRIVER_CLASS_NAME.
     */
    String STORE_DB_DRIVER_CLASS_NAME = STORE_DB_PREFIX + "driverClassName";

    /**
     * The constant STORE_DB_MAX_WAIT.
     */
    String STORE_DB_MAX_WAIT = STORE_DB_PREFIX + "maxWait";

    /**
     * The constant STORE_DB_URL.
     */
    String STORE_DB_URL = STORE_DB_PREFIX + "url";

    /**
     * The constant STORE_DB_USER.
     */
    String STORE_DB_USER = STORE_DB_PREFIX + "user";

    /**
     * The constant STORE_DB_PASSWORD.
     */
    String STORE_DB_PASSWORD = STORE_DB_PREFIX + "password";

    /**
     * The constant STORE_DB_MIN_CONN.
     */
    String STORE_DB_MIN_CONN = STORE_DB_PREFIX + "minConn";

    /**
     * The constant STORE_DB_MAX_CONN.
     */
    String STORE_DB_MAX_CONN = STORE_DB_PREFIX + "maxConn";

    /**
     * The constant STORE_DB_LOG_QUERY_LIMIT.
     */
    String STORE_DB_LOG_QUERY_LIMIT = STORE_DB_PREFIX + "queryLimit";

    /**
     * The constant LOCK_DB_TABLE.
     */
    String LOCK_DB_TABLE = STORE_DB_PREFIX + "lockTable";

    /**
     * The constant SERVER_RPC_PORT.
     */
    String SERVER_SERVICE_PORT_CAMEL = SERVER_PREFIX + "servicePort";

    /**
     * The constant SERVER_SERVICE_PORT_CONFIG.
     */
    String SERVER_SERVICE_PORT_CONFIG = SEATA_PREFIX + SERVER_PREFIX + "service-port";

    /**
     * The constant ENV_SEATA_PORT_KEY.
     */
    String ENV_SEATA_PORT_KEY = "SEATA_PORT";

    /**
     * The constant RECOVERY_PREFIX.
     */
    String RECOVERY_PREFIX = SERVER_PREFIX + "recovery.";

    /**
     * The constant COMMITING_RETRY_PERIOD.
     */
    String COMMITING_RETRY_PERIOD = RECOVERY_PREFIX + "committingRetryPeriod";

    /**
     * The constant ASYN_COMMITING_RETRY_PERIOD.
     */
    String ASYNC_COMMITING_RETRY_PERIOD = RECOVERY_PREFIX + "asyncCommittingRetryPeriod";

    /**
     * The constant ROLLBACKING_RETRY_PERIOD.
     */
    String ROLLBACKING_RETRY_PERIOD = RECOVERY_PREFIX + "rollbackingRetryPeriod";

    /**
     * The constant TIMEOUT_RETRY_PERIOD.
     */
    String TIMEOUT_RETRY_PERIOD = RECOVERY_PREFIX + "timeoutRetryPeriod";

    /**
     * The constant CLIENT_UNDO_PREFIX.
     */
    String CLIENT_UNDO_PREFIX = "client.undo.";

    /**
     * The constant TRANSACTION_UNDO_DATA_VALIDATION.
     */
    String TRANSACTION_UNDO_DATA_VALIDATION = CLIENT_UNDO_PREFIX + "dataValidation";

    /**
     * The constant TRANSACTION_UNDO_LOG_SERIALIZATION.
     */
    String TRANSACTION_UNDO_LOG_SERIALIZATION = CLIENT_UNDO_PREFIX + "logSerialization";

    /**
     * The constant TRANSACTION_UNDO_ONLY_CARE_UPDATE_COLUMNS.
     */
    String TRANSACTION_UNDO_ONLY_CARE_UPDATE_COLUMNS = CLIENT_UNDO_PREFIX + "onlyCareUpdateColumns";

    /**
     * the constant CLIENT_UNDO_COMPRESS_PREFIX
     */
    String CLIENT_UNDO_COMPRESS_PREFIX = CLIENT_UNDO_PREFIX + "compress.";

    /**
     * the constant CLIENT_UNDO_COMPRESS_TYPE
     */
    String CLIENT_UNDO_COMPRESS_TYPE = CLIENT_UNDO_COMPRESS_PREFIX + "type";

    /**
     * the constant CLIENT_UNDO_COMPRESS_ENABLE
     */
    String CLIENT_UNDO_COMPRESS_ENABLE = CLIENT_UNDO_COMPRESS_PREFIX + "enable";

    /**
     * the constant CLIENT_UNDO_COMPRESS_THRESHOLD
     */
    String CLIENT_UNDO_COMPRESS_THRESHOLD = CLIENT_UNDO_COMPRESS_PREFIX + "threshold";

    /**
     * The constant METRICS_PREFIX.
     */
    String METRICS_PREFIX = "metrics.";

    /**
     * The constant METRICS_ENABLED.
     */
    String METRICS_ENABLED = "enabled";

    /**
     * The constant METRICS_REGISTRY_TYPE.
     */
    String METRICS_REGISTRY_TYPE = "registryType";

    /**
     * The constant METRICS_EXPORTER_LIST.
     */
    String METRICS_EXPORTER_LIST = "exporterList";
    /**
     * The constant METRICS_EXPORTER_PROMETHEUS_PORT
     */
    String METRICS_EXPORTER_PROMETHEUS_PORT = "exporterPrometheusPort";

    /**
     * The constant SERVER_UNDO_PREFIX.
     */
    String SERVER_UNDO_PREFIX = SERVER_PREFIX + "undo.";

    /**
     * The constant TRANSACTION_UNDO_LOG_SAVE_DAYS.
     */
    String TRANSACTION_UNDO_LOG_SAVE_DAYS = SERVER_UNDO_PREFIX + "logSaveDays";

    /**
     * The constant TRANSACTION_UNDO_LOG_DELETE_PERIOD
     */
    String TRANSACTION_UNDO_LOG_DELETE_PERIOD = SERVER_UNDO_PREFIX + "logDeletePeriod";

    /**
     * The constant TRANSACTION_UNDO_LOG_TABLE
     */
    String TRANSACTION_UNDO_LOG_TABLE = CLIENT_UNDO_PREFIX + "logTable";
    /**
     * The constant LOG_PREFIX
     */
    String LOG_PREFIX = "log.";

    /**
     * The constant TRANSACTION_UNDO_LOG_EXCEPTION_RATE
     */
    String TRANSACTION_LOG_EXCEPTION_RATE = LOG_PREFIX + "exceptionRate";

    /**
     * The constant MAX_COMMIT_RETRY_TIMEOUT.
     */
    String MAX_COMMIT_RETRY_TIMEOUT = SERVER_PREFIX + "maxCommitRetryTimeout";

    /**
     * The constant MAX_ROLLBACK_RETRY_TIMEOUT.
     */
    String MAX_ROLLBACK_RETRY_TIMEOUT = SERVER_PREFIX + "maxRollbackRetryTimeout";

    /**
     * The constant ROLLBACK_RETRY_TIMEOUT_UNLOCK_ENABLE.
     * This configuration is deprecated, please use {@link #ROLLBACK_FAILED_UNLOCK_ENABLE} instead.
     */
    @Deprecated
    String ROLLBACK_RETRY_TIMEOUT_UNLOCK_ENABLE = SERVER_PREFIX + "rollbackRetryTimeoutUnlockEnable";

    /**
     * The constant ROLLBACK_FAILED_UNLOCK_ENABLE.
     */
    String ROLLBACK_FAILED_UNLOCK_ENABLE = SERVER_PREFIX + "rollbackFailedUnlockEnable";

    /**
     * the constant RETRY_DEAD_THRESHOLD
     */
    String RETRY_DEAD_THRESHOLD = SERVER_PREFIX + "retryDeadThreshold";

    /**
     * the constant DISTRIBUTED_LOCK_EXPIRE_TIME
     */
    String DISTRIBUTED_LOCK_EXPIRE_TIME = SERVER_PREFIX + "distributedLockExpireTime";

    /**
     * The constant MIN_SERVER_POOL_SIZE.
     */
    String MIN_SERVER_POOL_SIZE = TRANSPORT_PREFIX + "minServerPoolSize";

    /**
     * The constant MAX_SERVER_POOL_SIZE.
     */
    String MAX_SERVER_POOL_SIZE = TRANSPORT_PREFIX + "maxServerPoolSize";

    /**
     * The constant MIN_BRANCH_RESULT_POOL_SIZE.
     */
    String MIN_BRANCH_RESULT_POOL_SIZE = TRANSPORT_PREFIX + "minBranchResultPoolSize";

    /**
     * The constant MAX_BRANCH_RESULT_POOL_SIZE.
     */
    String MAX_BRANCH_RESULT_POOL_SIZE = TRANSPORT_PREFIX + "maxBranchResultPoolSize";

    /**
     * The constant MAX_TASK_QUEUE_SIZE.
     */
    String MAX_TASK_QUEUE_SIZE = TRANSPORT_PREFIX + "maxTaskQueueSize";

    /**
     * The constant KEEP_ALIVE_TIME.
     */
    String KEEP_ALIVE_TIME = TRANSPORT_PREFIX + "keepAliveTime";

    /**
     * The constant TRANSPORT_TYPE
     */
    String TRANSPORT_TYPE = TRANSPORT_PREFIX + "type";

    /**
     * The constant TRANSPORT_SERVER
     */
    String TRANSPORT_SERVER = TRANSPORT_PREFIX + "server";

    /**
     * The constant TRANSPORT_HEARTBEAT
     */
    String TRANSPORT_HEARTBEAT = TRANSPORT_PREFIX + "heartbeat";

    /**
     * The constant THREAD_FACTORY_PREFIX
     */
    String THREAD_FACTORY_PREFIX = TRANSPORT_PREFIX + "threadFactory.";

    /**
     * The constant BOSS_THREAD_PREFIX
     */
    String BOSS_THREAD_PREFIX = THREAD_FACTORY_PREFIX + "bossThreadPrefix";

    /**
     * The constant WORKER_THREAD_PREFIX
     */
    String WORKER_THREAD_PREFIX = THREAD_FACTORY_PREFIX + "workerThreadPrefix";

    /**
     * The constant SERVER_EXECUTOR_THREAD_PREFIX
     */
    String SERVER_EXECUTOR_THREAD_PREFIX = THREAD_FACTORY_PREFIX + "serverExecutorThreadPrefix";

    /**
     * The constant SHARE_BOSS_WORKER
     */
    String SHARE_BOSS_WORKER = THREAD_FACTORY_PREFIX + "shareBossWorker";

    /**
     * The constant CLIENT_SELECTOR_THREAD_PREFIX
     */
    String CLIENT_SELECTOR_THREAD_PREFIX = THREAD_FACTORY_PREFIX + "clientSelectorThreadPrefix";

    /**
     * The constant CLIENT_SELECTOR_THREAD_SIZE
     */
    String CLIENT_SELECTOR_THREAD_SIZE = THREAD_FACTORY_PREFIX + "clientSelectorThreadSize";

    /**
     * The constant CLIENT_WORKER_THREAD_PREFIX
     */
    String CLIENT_WORKER_THREAD_PREFIX = THREAD_FACTORY_PREFIX + "clientWorkerThreadPrefix";

    /**
     * The constant BOSS_THREAD_SIZE
     */
    String BOSS_THREAD_SIZE = THREAD_FACTORY_PREFIX + "bossThreadSize";

    /**
     * The constant WORKER_THREAD_SIZE
     */
    String WORKER_THREAD_SIZE = THREAD_FACTORY_PREFIX + "workerThreadSize";

    /**
     * The constant SHUTDOWN_PREFIX
     */
    String SHUTDOWN_PREFIX = TRANSPORT_PREFIX + "shutdown.";

    /**
     * The constant SHUTDOWN_WAIT
     */
    String SHUTDOWN_WAIT = SHUTDOWN_PREFIX + "wait";

    /**
     * The constant ENABLE_CLIENT_BATCH_SEND_REQUEST
     */
    @Deprecated
    String ENABLE_CLIENT_BATCH_SEND_REQUEST = TRANSPORT_PREFIX + "enableClientBatchSendRequest";

    /**
     * The constant ENABLE_TM_CLIENT_BATCH_SEND_REQUEST
     */
    String ENABLE_TM_CLIENT_BATCH_SEND_REQUEST = TRANSPORT_PREFIX + "enableTmClientBatchSendRequest";

    /**
     * The constant ENABLE_RM_CLIENT_BATCH_SEND_REQUEST
     */
    String ENABLE_RM_CLIENT_BATCH_SEND_REQUEST = TRANSPORT_PREFIX + "enableRmClientBatchSendRequest";

    /**
     * The constant ENABLE_TC_SERVER_BATCH_SEND_RESPONSE
     */
    String ENABLE_TC_SERVER_BATCH_SEND_RESPONSE = TRANSPORT_PREFIX + "enableTcServerBatchSendResponse";

    /**
     * The constant DISABLE_GLOBAL_TRANSACTION.
     */
    String DISABLE_GLOBAL_TRANSACTION = SERVICE_PREFIX + "disableGlobalTransaction";

    /**
     * The constant SQL_PARSER_TYPE.
     */
    String SQL_PARSER_TYPE = CLIENT_RM_PREFIX + "sqlParserType";

    /**
     * The constant STORE_REDIS_MODE.
     */
    String STORE_REDIS_MODE = STORE_REDIS_PREFIX + "mode";

    /**
     * The constant STORE_REDIS_HOST.
     */
    String STORE_REDIS_HOST = STORE_REDIS_PREFIX + "host";

    /**
     * The constant STORE_REDIS_PORT.
     */
    String STORE_REDIS_PORT = STORE_REDIS_PREFIX + "port";

    /**
     * The constant STORE_REDIS_SINGLE_PREFIX.
     */
    String STORE_REDIS_SINGLE_PREFIX = STORE_REDIS_PREFIX + "single.";

    /**
     * The constant STORE_REDIS_SINGLE_HOST.
     */
    String STORE_REDIS_SINGLE_HOST = STORE_REDIS_SINGLE_PREFIX + "host";

    /**
     * The constant STORE_MIN_Conn.
     */
    String STORE_REDIS_MIN_CONN = STORE_REDIS_PREFIX + "minConn";

    /**
     * The constant STORE_REDIS_SINGLE_PORT.
     */
    String STORE_REDIS_SINGLE_PORT = STORE_REDIS_SINGLE_PREFIX + "port";

    /**
     * The constant STORE_REDIS_MAX_CONN.
     */
    String STORE_REDIS_MAX_CONN = STORE_REDIS_PREFIX + "maxConn";

    /**
     * the constant STORE_REDIS_MAX_TOTAL
     */
    String STORE_REDIS_MAX_TOTAL = STORE_REDIS_PREFIX + "maxTotal";

    /**
     * The constant STORE_REDIS_DATABASE.
     */
    String STORE_REDIS_DATABASE = STORE_REDIS_PREFIX + "database";

    /**
     * The constant STORE_REDIS_PASSWORD.
     */
    String STORE_REDIS_PASSWORD = STORE_REDIS_PREFIX + "password";

    /**
     * The constant STORE_REDIS_QUERY_LIMIT.
     */
    String STORE_REDIS_QUERY_LIMIT = STORE_REDIS_PREFIX + "queryLimit";

    /**
     * The constant REDIS_SENTINEL_MODE.
     */
    String REDIS_SENTINEL_MODE = "sentinel";

    /**
     * The constant REDIS_SINGLE_MODE.
     */
    String REDIS_SINGLE_MODE = "single";

    /**
     * The constant STORE_REDIS_SENTINEL_PREFIX.
     */
    String STORE_REDIS_SENTINEL_PREFIX = STORE_REDIS_PREFIX + "sentinel.";

    /**
     * STORE_REDIS_SENTINEL_MASTERNAME.
     */
    String STORE_REDIS_SENTINEL_MASTERNAME = STORE_REDIS_SENTINEL_PREFIX + "masterName";

    /**
     * STORE_REDIS_SENTINEL_HOST.
     */
    String STORE_REDIS_SENTINEL_HOST = STORE_REDIS_SENTINEL_PREFIX + "sentinelHosts";

    /**
     * STORE_REDIS_SENTINEL_PASSWORD.
     */
    String STORE_REDIS_SENTINEL_PASSWORD = STORE_REDIS_SENTINEL_PREFIX + "sentinelPassword";

    /**
     * The constant CLIENT_DEGRADE_CHECK_PERIOD.
     */
    String CLIENT_DEGRADE_CHECK_PERIOD = CLIENT_TM_PREFIX + "degradeCheckPeriod";

    /**
     * The constant CLIENT_DEGRADE_CHECK.
     */
    String CLIENT_DEGRADE_CHECK = CLIENT_TM_PREFIX + "degradeCheck";
    /**
     * The constant CLIENT_DEGRADE_CHECK_ALLOW_TIMES.
     */
    String CLIENT_DEGRADE_CHECK_ALLOW_TIMES = CLIENT_TM_PREFIX + "degradeCheckAllowTimes";

    /**
     * The constant GLOBAL_TRANSACTION_INTERCEPTOR_ORDER.
     */
    String TM_INTERCEPTOR_ORDER = CLIENT_TM_PREFIX + "interceptorOrder";

    /**
     * The constant ACCESS_KEY.
     */
    String ACCESS_KEY = "accesskey";

    /**
     * The constant SECRET_KEY.
     */
    String SECRET_KEY = "secretkey";

    /**
     * The constant SEATA_ACCESS_KEY.
     */
    String SEATA_ACCESS_KEY = SEATA_PREFIX + ACCESS_KEY;

    /**
     * The constant SEATA_SECRET_KEY.
     */
    String SEATA_SECRET_KEY = SEATA_PREFIX + SECRET_KEY;

    /**
     * The constant EXTRA_DATA_SPLIT_CHAR.
     */
    String EXTRA_DATA_SPLIT_CHAR = "\n";
    /**
     * The constant EXTRA_DATA_KV_CHAR.
     */
    String EXTRA_DATA_KV_CHAR = "=";

    /**
     * The constant SERVER_ENABLE_CHECK_AUTH.
     */
    String SERVER_ENABLE_CHECK_AUTH = SERVER_PREFIX + "enableCheckAuth";

    /**
     * The constant APPLICATION_ID.
     */
    String APPLICATION_ID = "applicationId";

    /**
     * The constant TX_SERVICE_GROUP.
     */
    String TX_SERVICE_GROUP = "txServiceGroup";

    /**
     * The constant DATA_SOURCE_PROXY_MODE.
     */
    String DATA_SOURCE_PROXY_MODE = "dataSourceProxyMode";

    /**
     * The constant TCC_PREFIX
     */
    String TCC_PREFIX = "tcc.";

    /**
     * The constant TCC_FENCE_PREFIX
     */
    String TCC_FENCE_PREFIX = TCC_PREFIX + "fence.";

    /**
     * The constant TCC_FENCE_CLEAN_PERIOD
     */
    String TCC_FENCE_CLEAN_PERIOD = TCC_FENCE_PREFIX + "cleanPeriod";

    /**
     * The constant TCC_FENCE_LOG_TABLE_NAME
     */
    String TCC_FENCE_LOG_TABLE_NAME = TCC_FENCE_PREFIX + "logTableName";

    /**
     * The constant rpcRmRequestTimeout
     */
    String RPC_RM_REQUEST_TIMEOUT = TRANSPORT_PREFIX + "rpcRmRequestTimeout";

    /**
     * The constant RPC_TM_REQUEST_TIMEOUT
     */
    String RPC_TM_REQUEST_TIMEOUT = TRANSPORT_PREFIX + "rpcTmRequestTimeout";

    /**
     * The constant RPC_TM_REQUEST_TIMEOUT
     */
    String RPC_TC_REQUEST_TIMEOUT = TRANSPORT_PREFIX + "rpcTcRequestTimeout";

    /**
     * The constant SESSION_BRANCH_ASYNC_QUEUE_SIZE
     */
    String SESSION_BRANCH_ASYNC_QUEUE_SIZE = SERVER_PREFIX + SESSION_PREFIX + "branchAsyncQueueSize";

    /**
     * The constant ENABLE_BRANCH_ASYNC_REMOVE
     */
    String ENABLE_BRANCH_ASYNC_REMOVE = SERVER_PREFIX + SESSION_PREFIX + "enableBranchAsyncRemove";

    /**
     * The constant IS_USE_CLOUD_NAMESPACE_PARSING.
     */
    String IS_USE_CLOUD_NAMESPACE_PARSING = "isUseCloudNamespaceParsing";

    /**
     * The constant IS_USE_ENDPOINT_PARSING_RULE.
     */
    String IS_USE_ENDPOINT_PARSING_RULE = "isUseEndpointParsingRule";

    /**
     * The constant XAER_NOTA_RETRY_TIMEOUT
     */
    String XAER_NOTA_RETRY_TIMEOUT = SERVER_PREFIX + "xaerNotaRetryTimeout";

    /**
     * The constant XA_BRANCH_EXECUTION_TIMEOUT
     */
    String XA_BRANCH_EXECUTION_TIMEOUT = CLIENT_RM_PREFIX + "branchExecutionTimeoutXA";

    /**
     * The constant XA_CONNECTION_TWO_PHASE_HOLD_TIMEOUT
     */
    String XA_CONNECTION_TWO_PHASE_HOLD_TIMEOUT = CLIENT_RM_PREFIX + "connectionTwoPhaseHoldTimeoutXA";

    /**
     * The constant ENABLE_PARALLEL_REQUEST_HANDLE_KEY
     */
    String ENABLE_PARALLEL_REQUEST_HANDLE_KEY = SERVER_PREFIX + "enableParallelRequestHandle";

}
