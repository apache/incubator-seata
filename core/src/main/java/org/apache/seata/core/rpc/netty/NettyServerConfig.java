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
package org.apache.seata.core.rpc.netty;

import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollServerSocketChannel;
import org.apache.seata.common.DefaultValues;
import org.apache.seata.core.constants.ConfigurationKeys;

import static org.apache.seata.common.DefaultValues.DEFAULT_BOSS_THREAD_PREFIX;
import static org.apache.seata.common.DefaultValues.DEFAULT_BOSS_THREAD_SIZE;
import static org.apache.seata.common.DefaultValues.DEFAULT_EXECUTOR_THREAD_PREFIX;
import static org.apache.seata.common.DefaultValues.DEFAULT_NIO_WORKER_THREAD_PREFIX;
import static org.apache.seata.common.DefaultValues.DEFAULT_SHUTDOWN_TIMEOUT_SEC;

/**
 * The type Netty server config.
 */
public class NettyServerConfig extends NettyBaseConfig {
    private int serverListenPort = 0;
    private static final String EPOLL_WORKER_THREAD_PREFIX = "NettyServerEPollWorker";

    // Network Buffer Config
    private int serverSocketSendBufSize = CONFIG.getInt(
            ConfigurationKeys.TRANSPORT_PREFIX + "serverSocketSendBufSize",
            DefaultValues.DEFAULT_SERVER_SOCKET_SEND_BUF_SIZE);
    private int serverSocketResvBufSize = CONFIG.getInt(
            ConfigurationKeys.TRANSPORT_PREFIX + "serverSocketResvBufSize",
            DefaultValues.DEFAULT_SERVER_SOCKET_RESV_BUF_SIZE);
    private int writeBufferHighWaterMark = CONFIG.getInt(
            ConfigurationKeys.TRANSPORT_PREFIX + "writeBufferHighWaterMark",
            DefaultValues.DEFAULT_WRITE_BUFFER_HIGH_WATER_MARK);
    private int writeBufferLowWaterMark = CONFIG.getInt(
            ConfigurationKeys.TRANSPORT_PREFIX + "writeBufferLowWaterMark",
            DefaultValues.DEFAULT_WRITE_BUFFER_LOW_WATER_MARK);

    // Connection Management
    private int soBackLogSize =
            CONFIG.getInt(ConfigurationKeys.TRANSPORT_PREFIX + "soBackLogSize", DefaultValues.DEFAULT_SO_BACK_LOG_SIZE);
    private int serverChannelMaxIdleTimeSeconds = CONFIG.getInt(
            ConfigurationKeys.TRANSPORT_PREFIX + "serverChannelMaxIdleTimeSeconds",
            DefaultValues.DEFAULT_SERVER_CHANNEL_MAX_IDLE_TIME_SECONDS);

    // RPC Configuration
    private static final long RPC_TC_REQUEST_TIMEOUT =
            CONFIG.getLong(ConfigurationKeys.RPC_TC_REQUEST_TIMEOUT, DefaultValues.DEFAULT_RPC_TC_REQUEST_TIMEOUT);
    private static boolean ENABLE_TC_SERVER_BATCH_SEND_RESPONSE = CONFIG.getBoolean(
            ConfigurationKeys.ENABLE_TC_SERVER_BATCH_SEND_RESPONSE,
            DefaultValues.DEFAULT_ENABLE_TC_SERVER_BATCH_SEND_RESPONSE);

    // Thread Pool Config
    private int serverSelectorThreads = Integer.parseInt(System.getProperty(
            ConfigurationKeys.TRANSPORT_PREFIX + "serverSelectorThreads", String.valueOf(WORKER_THREAD_SIZE)));
    private int serverWorkerThreads = Integer.parseInt(System.getProperty(
            ConfigurationKeys.TRANSPORT_PREFIX + "serverWorkerThreads", String.valueOf(WORKER_THREAD_SIZE)));

    // Seata and Grpc Protocol Thread Pool
    private static int minServerPoolSize =
            CONFIG.getInt(ConfigurationKeys.MIN_SERVER_POOL_SIZE, DefaultValues.DEFAULT_MIN_SERVER_POOL_SIZE);
    private static int maxServerPoolSize =
            CONFIG.getInt(ConfigurationKeys.MAX_SERVER_POOL_SIZE, DefaultValues.DEFAULT_MAX_SERVER_POOL_SIZE);
    private static int maxTaskQueueSize =
            CONFIG.getInt(ConfigurationKeys.MAX_TASK_QUEUE_SIZE, DefaultValues.DEFAULT_MAX_TASK_QUEUE_SIZE);
    private static int keepAliveTime =
            CONFIG.getInt(ConfigurationKeys.KEEP_ALIVE_TIME, DefaultValues.DEFAULT_KEEP_ALIVE_TIME);

    // HTTP Protocol Thread Pool
    private static int minHttpPoolSize =
            CONFIG.getInt(ConfigurationKeys.MIN_HTTP_POOL_SIZE, DefaultValues.DEFAULT_MIN_HTTP_POOL_SIZE);
    private static int maxHttpPoolSize =
            CONFIG.getInt(ConfigurationKeys.MAX_HTTP_POOL_SIZE, DefaultValues.DEFAULT_MAX_HTTP_POOL_SIZE);
    private static int maxHttpTaskQueueSize =
            CONFIG.getInt(ConfigurationKeys.MAX_HTTP_TASK_QUEUE_SIZE, DefaultValues.DEFAULT_MAX_HTTP_TASK_QUEUE_SIZE);
    private static int httpKeepAliveTime =
            CONFIG.getInt(ConfigurationKeys.HTTP_POOL_KEEP_ALIVE_TIME, DefaultValues.DEFAULT_HTTP_POOL_KEEP_ALIVE_TIME);

    // Branch Result Thread Pool
    private static int minBranchResultPoolSize = Integer.parseInt(System.getProperty(
            ConfigurationKeys.MIN_BRANCH_RESULT_POOL_SIZE, String.valueOf(WorkThreadMode.Pin.getValue())));
    private static int maxBranchResultPoolSize = Integer.parseInt(System.getProperty(
            ConfigurationKeys.MAX_BRANCH_RESULT_POOL_SIZE, String.valueOf(WorkThreadMode.Pin.getValue())));

    /**
     * The Server channel clazz.
     */
    public static final Class<? extends ServerChannel> SERVER_CHANNEL_CLAZZ = NettyBaseConfig.SERVER_CHANNEL_CLAZZ;

    /**
     * Gets server selector threads.
     *
     * @return the server selector threads
     */
    public int getServerSelectorThreads() {
        return serverSelectorThreads;
    }

    /**
     * Sets server selector threads.
     *
     * @param serverSelectorThreads the server selector threads
     */
    public void setServerSelectorThreads(int serverSelectorThreads) {
        this.serverSelectorThreads = serverSelectorThreads;
    }

    /**
     * Enable epoll boolean.
     *
     * @return the boolean
     */
    public static boolean enableEpoll() {
        return NettyBaseConfig.SERVER_CHANNEL_CLAZZ.equals(EpollServerSocketChannel.class) && Epoll.isAvailable();
    }

    /**
     * Gets server socket send buf size.
     *
     * @return the server socket send buf size
     */
    public int getServerSocketSendBufSize() {
        return serverSocketSendBufSize;
    }

    /**
     * Sets server socket send buf size.
     *
     * @param serverSocketSendBufSize the server socket send buf size
     */
    public void setServerSocketSendBufSize(int serverSocketSendBufSize) {
        this.serverSocketSendBufSize = serverSocketSendBufSize;
    }

    /**
     * Gets server socket resv buf size.
     *
     * @return the server socket resv buf size
     */
    public int getServerSocketResvBufSize() {
        return serverSocketResvBufSize;
    }

    /**
     * Sets server socket resv buf size.
     *
     * @param serverSocketResvBufSize the server socket resv buf size
     */
    public void setServerSocketResvBufSize(int serverSocketResvBufSize) {
        this.serverSocketResvBufSize = serverSocketResvBufSize;
    }

    /**
     * Gets server worker threads.
     *
     * @return the server worker threads
     */
    public int getServerWorkerThreads() {
        return serverWorkerThreads;
    }

    /**
     * Sets server worker threads.
     *
     * @param serverWorkerThreads the server worker threads
     */
    public void setServerWorkerThreads(int serverWorkerThreads) {
        this.serverWorkerThreads = serverWorkerThreads;
    }

    /**
     * Gets so back log size.
     *
     * @return the so back log size
     */
    public int getSoBackLogSize() {
        return soBackLogSize;
    }

    /**
     * Sets so back log size.
     *
     * @param soBackLogSize the so back log size
     */
    public void setSoBackLogSize(int soBackLogSize) {
        this.soBackLogSize = soBackLogSize;
    }

    /**
     * Gets write buffer high water mark.
     *
     * @return the write buffer high water mark
     */
    public int getWriteBufferHighWaterMark() {
        return writeBufferHighWaterMark;
    }

    /**
     * Sets write buffer high water mark.
     *
     * @param writeBufferHighWaterMark the write buffer high water mark
     */
    public void setWriteBufferHighWaterMark(int writeBufferHighWaterMark) {
        this.writeBufferHighWaterMark = writeBufferHighWaterMark;
    }

    /**
     * Gets write buffer low water mark.
     *
     * @return the write buffer low water mark
     */
    public int getWriteBufferLowWaterMark() {
        return writeBufferLowWaterMark;
    }

    /**
     * Sets write buffer low water mark.
     *
     * @param writeBufferLowWaterMark the write buffer low water mark
     */
    public void setWriteBufferLowWaterMark(int writeBufferLowWaterMark) {
        this.writeBufferLowWaterMark = writeBufferLowWaterMark;
    }

    /**
     * Gets listen port.
     *
     * @return the listen port
     */
    public int getServerListenPort() {
        return serverListenPort;
    }

    public void setServerListenPort(int port) {
        this.serverListenPort = port;
    }

    /**
     * Gets channel max read idle seconds.
     *
     * @return the channel max read idle seconds
     */
    public int getChannelMaxReadIdleSeconds() {
        return MAX_READ_IDLE_SECONDS;
    }

    /**
     * Gets server channel max idle time seconds.
     *
     * @return the server channel max idle time seconds
     */
    public int getServerChannelMaxIdleTimeSeconds() {
        return serverChannelMaxIdleTimeSeconds;
    }

    /**
     * Gets rpc request timeout.
     *
     * @return the rpc request timeout
     */
    public static long getRpcRequestTimeout() {
        return RPC_TC_REQUEST_TIMEOUT;
    }

    /**
     * Get boss thread prefix string.
     *
     * @return the string
     */
    public String getBossThreadPrefix() {
        return CONFIG.getConfig(ConfigurationKeys.BOSS_THREAD_PREFIX, DEFAULT_BOSS_THREAD_PREFIX);
    }

    /**
     * Get worker thread prefix string.
     *
     * @return the string
     */
    public String getWorkerThreadPrefix() {
        return CONFIG.getConfig(
                ConfigurationKeys.WORKER_THREAD_PREFIX,
                enableEpoll() ? EPOLL_WORKER_THREAD_PREFIX : DEFAULT_NIO_WORKER_THREAD_PREFIX);
    }

    /**
     * Get executor thread prefix string.
     *
     * @return the string
     */
    public String getExecutorThreadPrefix() {
        return CONFIG.getConfig(ConfigurationKeys.SERVER_EXECUTOR_THREAD_PREFIX, DEFAULT_EXECUTOR_THREAD_PREFIX);
    }

    /**
     * Get boss thread size int.
     *
     * @return the int
     */
    public int getBossThreadSize() {
        return CONFIG.getInt(ConfigurationKeys.BOSS_THREAD_SIZE, DEFAULT_BOSS_THREAD_SIZE);
    }

    /**
     * Get the timeout seconds of shutdown.
     *
     * @return the int
     */
    public int getServerShutdownWaitTime() {
        return CONFIG.getInt(ConfigurationKeys.SHUTDOWN_WAIT, DEFAULT_SHUTDOWN_TIMEOUT_SEC);
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

    public static int getMinHttpPoolSize() {
        return minHttpPoolSize;
    }

    public static int getMaxHttpPoolSize() {
        return maxHttpPoolSize;
    }

    public static int getMaxHttpTaskQueueSize() {
        return maxHttpTaskQueueSize;
    }

    public static int getHttpKeepAliveTime() {
        return httpKeepAliveTime;
    }

    /**
     * Get the tc server batch send response enable
     *
     * @return true or false
     */
    public static boolean isEnableTcServerBatchSendResponse() {
        return ENABLE_TC_SERVER_BATCH_SEND_RESPONSE;
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
