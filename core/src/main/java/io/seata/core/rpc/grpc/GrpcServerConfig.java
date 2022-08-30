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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.common.ConfigurationKeys.GRPC_SERVER_SERVICE_PORT_CAMEL;
import static io.seata.common.DefaultValues.DEFAULT_RPC_TC_REQUEST_TIMEOUT;

/**
 * @author goodboycoder
 */
public class GrpcServerConfig extends BaseRpcConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcServerConfig.class);

    /**
     * Netty Server listen port
     */
    private static final int DEFAULT_LISTEN_PORT = 8591;

    /**
     * Server Rpc request timeout
     */
    private static final long RPC_TC_REQUEST_TIMEOUT = CONFIG.getLong(io.seata.common.ConfigurationKeys.GRPC_TC_REQUEST_TIMEOUT, DEFAULT_RPC_TC_REQUEST_TIMEOUT);

    /**
     * Configuration of the worker thread pool on the Seata server
     */
    private static int minServerPoolSize = Integer.parseInt(System.getProperty(
            ConfigurationKeys.GRPC_MIN_SERVER_POOL_SIZE, "50"));
    private static int maxServerPoolSize = Integer.parseInt(System.getProperty(
            ConfigurationKeys.GRPC_MAX_SERVER_POOL_SIZE, "500"));
    private static int maxTaskQueueSize = Integer.parseInt(System.getProperty(
            ConfigurationKeys.GRPC_MAX_TASK_QUEUE_SIZE, "20000"));
    private static int keepAliveTime = Integer.parseInt(System.getProperty(
            ConfigurationKeys.GRPC_KEEP_ALIVE_TIME, "500"));

    /**
     * Batch result response thread pool configuration
     */
    private static int minBranchResultPoolSize = Integer.parseInt(System.getProperty(
            ConfigurationKeys.GRPC_MIN_BRANCH_RESULT_POOL_SIZE, String.valueOf(WORKER_THREAD_SIZE)));
    private static int maxBranchResultPoolSize = Integer.parseInt(System.getProperty(
            ConfigurationKeys.GRPC_MAX_BRANCH_RESULT_POOL_SIZE, String.valueOf(WORKER_THREAD_SIZE)));

    /**
     * Gets default listen port.
     *
     * @return the default listen port
     */
    public int getDefaultListenPort() {
        return DEFAULT_LISTEN_PORT;
    }

    /**
     * Gets the config listen port.
     *
     * @return the listen port
     */
    public int getListenPort() {
        String strPort = CONFIG.getConfig(GRPC_SERVER_SERVICE_PORT_CAMEL);
        int port = 0;
        try {
            port = Integer.parseInt(strPort);
        } catch (NumberFormatException exx) {
            LOGGER.error("grpc server service port set error:{}", exx.toString());
        }
        if (0 >= port) {
            LOGGER.error("listen port: {} is invalid, will use default port:{}", port, getDefaultListenPort());
            port = getDefaultListenPort();
        }
        return port;
    }

    /**
     * Gets rpc request timeout.
     *
     * @return the rpc request timeout
     */
    public static long getRpcRequestTimeout() {
        return RPC_TC_REQUEST_TIMEOUT;
    }

    public static int getMinServerPoolSize() {
        return minServerPoolSize;
    }

    public static int getMaxServerPoolSize() {
        return maxServerPoolSize;
    }

    public static int getMaxTaskQueueSize() {
        return maxTaskQueueSize;
    }

    public static int getKeepAliveTime() {
        return keepAliveTime;
    }

    /**
     * Get the min size for branch result thread pool
     *
     * @return the int
     */
    public static int getMinBranchResultPoolSize() {
        return minBranchResultPoolSize;
    }

    /**
     * Get the max size for branch result thread pool
     *
     * @return the int
     */
    public static int getMaxBranchResultPoolSize() {
        return maxBranchResultPoolSize;
    }
}
