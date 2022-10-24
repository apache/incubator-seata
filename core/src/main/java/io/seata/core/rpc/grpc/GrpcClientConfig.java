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
package io.seata.core.rpc.grpc;

import io.seata.common.ConfigurationKeys;
import io.seata.core.rpc.BaseRpcConfig;
import io.seata.core.rpc.RpcChannelPoolKey;

import static io.seata.common.DefaultValues.DEFAULT_RPC_RM_REQUEST_TIMEOUT;
import static io.seata.common.DefaultValues.DEFAULT_RPC_TM_REQUEST_TIMEOUT;

/**
 * @author goodboycoder
 */
public class GrpcClientConfig extends BaseRpcConfig {

    private int clientWorkerThreads = WORKER_THREAD_SIZE;

    private static final String GRPC_DISPATCH_THREAD_PREFIX = "grpcDispatch";

    private static final long RPC_RM_REQUEST_TIMEOUT = CONFIG.getLong(ConfigurationKeys.GRPC_RM_REQUEST_TIMEOUT,
            CONFIG.getLong(ConfigurationKeys.RPC_RM_REQUEST_TIMEOUT, DEFAULT_RPC_RM_REQUEST_TIMEOUT));
    private static final long RPC_TM_REQUEST_TIMEOUT = CONFIG.getLong(ConfigurationKeys.RPC_RM_REQUEST_TIMEOUT,
            CONFIG.getLong(ConfigurationKeys.RPC_TM_REQUEST_TIMEOUT, DEFAULT_RPC_TM_REQUEST_TIMEOUT));

    private static final int MAX_CHECK_ALIVE_RETRY = 300;
    private static final int CHECK_ALIVE_INTERVAL = 10;

    /**
     * Related configuration of GenericKeyedObjectPool
     */
    private static final long MAX_ACQUIRE_CONN_MILLS = 60 * 1000L;
    private static final int DEFAULT_MAX_POOL_ACTIVE = 1;
    private static final int DEFAULT_MIN_POOL_IDLE = 0;
    private static final boolean DEFAULT_POOL_TEST_BORROW = true;
    private static final boolean DEFAULT_POOL_TEST_RETURN = true;
    private static final boolean DEFAULT_POOL_LIFO = true;

    private static final long KEEP_ALIVE_TIME = 6 * 60 * 1000;


    /**
     * Gets client worker threads.
     *
     * @return the client worker threads
     */
    public int getClientWorkerThreads() {
        return clientWorkerThreads;
    }

    /**
     * Gets client channel keepalive time.
     *
     * @return the client channel keepalive time
     */
    public long getKeepAliveTime() {
        return KEEP_ALIVE_TIME;
    }

    /**
     * Gets client max write idle time
     *
     * @return the client max write idle time
     */
    public int getMaxWriteIdleSeconds() {
        return MAX_WRITE_IDLE_SECONDS;
    }

    /**
     * Sets client worker threads.
     *
     * @param clientWorkerThreads the client worker threads
     */
    public void setClientWorkerThreads(int clientWorkerThreads) {
        this.clientWorkerThreads = clientWorkerThreads;
    }
    /**
     * Gets tm dispatch thread prefix.
     *
     * @return the tm dispatch thread prefix
     */
    public String getRmDispatchThreadPrefix() {
        return GRPC_DISPATCH_THREAD_PREFIX + "_" + RpcChannelPoolKey.TransactionRole.RMROLE.name();
    }

    /**
     * Gets tm dispatch thread prefix.
     *
     * @return the tm dispatch thread prefix
     */
    public String getTmDispatchThreadPrefix() {
        return GRPC_DISPATCH_THREAD_PREFIX + "_" + RpcChannelPoolKey.TransactionRole.TMROLE.name();
    }

    /**
     * Gets rpc RM sendAsyncRequestWithResponse time out.
     *
     * @return the rpc RM sendAsyncRequestWithResponse time out
     */
    public static long getRpcRmRequestTimeout() {
        return RPC_RM_REQUEST_TIMEOUT;
    }

    /**
     * Gets rpc TM sendAsyncRequestWithResponse time out.
     *
     * @return the rpc TM sendAsyncRequestWithResponse time out
     */
    public static long getRpcTmRequestTimeout() {
        return RPC_TM_REQUEST_TIMEOUT;
    }

    /**
     * Gets max check alive retry.
     *
     * @return the max check alive retry
     */
    public static int getMaxCheckAliveRetry() {
        return MAX_CHECK_ALIVE_RETRY;
    }

    /**
     * Gets check alive interval.
     *
     * @return the check alive interval
     */
    public static int getCheckAliveInterval() {
        return CHECK_ALIVE_INTERVAL;
    }

    /**
     * Get max acquire conn mills long.
     *
     * @return the long
     */
    public long getMaxAcquireConnMills() {
        return MAX_ACQUIRE_CONN_MILLS;
    }

    /**
     * Gets max pool active.
     *
     * @return the max pool active
     */
    public int getMaxPoolActive() {
        return DEFAULT_MAX_POOL_ACTIVE;
    }

    /**
     * Gets min pool idle.
     *
     * @return the min pool idle
     */
    public int getMinPoolIdle() {
        return DEFAULT_MIN_POOL_IDLE;
    }

    /**
     * Is pool test borrow boolean.
     *
     * @return the boolean
     */
    public boolean isPoolTestBorrow() {
        return DEFAULT_POOL_TEST_BORROW;
    }

    /**
     * Is pool test return boolean.
     *
     * @return the boolean
     */
    public boolean isPoolTestReturn() {
        return DEFAULT_POOL_TEST_RETURN;
    }

    /**
     * Is pool fifo boolean.
     *
     * @return the boolean
     */
    public boolean isPoolLifo() {
        return DEFAULT_POOL_LIFO;
    }


    /**
     * Whether to allow RmClient to send requests in batches
     * @return the boolean
     */
    public boolean isEnableRmClientBatchSendRequest() {
        return CONFIG.getBoolean(ConfigurationKeys.GRPC_ENABLE_RM_CLIENT_BATCH_SEND_REQUEST,
                CONFIG.getBoolean(ConfigurationKeys.ENABLE_CLIENT_BATCH_SEND_REQUEST, false));
    }

    /**
     * Whether to allow TmClient to send requests in batches
     * @return the boolean
     */
    public boolean isEnableTmClientBatchSendRequest() {
        return CONFIG.getBoolean(ConfigurationKeys.GRPC_ENABLE_TM_CLIENT_BATCH_SEND_REQUEST,
                CONFIG.getBoolean(ConfigurationKeys.ENABLE_CLIENT_BATCH_SEND_REQUEST, false));
    }
}
