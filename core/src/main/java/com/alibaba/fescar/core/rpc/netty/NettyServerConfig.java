/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.fescar.core.rpc.netty;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollServerSocketChannel;

/**
 * The type Netty server config.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2018 /9/12
 */
public class NettyServerConfig extends NettyBaseConfig {

    private int serverSelectorThreads = WORKER_THREAD_SIZE;
    private int serverSocketSendBufSize = 153600;
    private int serverSocketResvBufSize = 153600;
    private int serverWorkerThreads = WORKER_THREAD_SIZE;
    private int soBackLogSize = 1024;
    private int writeBufferHighWaterMark = 67108864;
    private int writeBufferLowWaterMark = 1048576;
    private static final int DEFAULT_LISTEN_PORT = 8091;
    private static final int RPC_REQUEST_TIMEOUT = 30 * 1000;
    private boolean enableServerPooledByteBufAllocator = true;
    private int serverChannelMaxIdleTimeSeconds = 30;
    private static final String DEFAULT_BOSS_THREAD_PREFIX = "NettyBoss";
    private static final String EPOLL_WORKER_THREAD_PREFIX = "NettyServerEPollWorker";
    private static final String NIO_WORKER_THREAD_PREFIX = "NettyServerNIOWorker";
    private static final String DEFAULT_EXECUTOR_THREAD_PREFIX = "NettyServerBizHandler";
    private static final int DEFAULT_BOSS_THREAD_SIZE = 1;
    /**
     * The Server channel clazz.
     */
    public final Class<? extends ServerChannel> SERVER_CHANNEL_CLAZZ = NettyBaseConfig.SERVER_CHANNEL_CLAZZ;

    /**
     * The constant DIRECT_BYTE_BUF_ALLOCATOR.
     */
    public static final PooledByteBufAllocator DIRECT_BYTE_BUF_ALLOCATOR =
        new PooledByteBufAllocator(
            true,
            WORKER_THREAD_SIZE,
            WORKER_THREAD_SIZE,
            2048 * 64,
            10,
            512,
            256,
            64,
            true,
            0
        );

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
        return NettyBaseConfig.SERVER_CHANNEL_CLAZZ.equals(EpollServerSocketChannel.class)
            && Epoll.isAvailable();

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
    public int getDefaultListenPort() {
        return DEFAULT_LISTEN_PORT;
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
     * Is enable server pooled byte buf allocator boolean.
     *
     * @return the boolean
     */
    public boolean isEnableServerPooledByteBufAllocator() {
        return enableServerPooledByteBufAllocator;
    }

    /**
     * Sets enable server pooled byte buf allocator.
     *
     * @param enableServerPooledByteBufAllocator the enable server pooled byte buf allocator
     */
    public void setEnableServerPooledByteBufAllocator(boolean enableServerPooledByteBufAllocator) {
        this.enableServerPooledByteBufAllocator = enableServerPooledByteBufAllocator;
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
    public static int getRpcRequestTimeout() {
        return RPC_REQUEST_TIMEOUT;
    }

    /**
     * Get boss thread prefix string.
     *
     * @return the string
     */
    public String getBossThreadPrefix() {
        return CONFIG.getConfig("transport.thread-factory.boss-thread-prefix", DEFAULT_BOSS_THREAD_PREFIX);
    }

    /**
     * Get worker thread prefix string.
     *
     * @return the string
     */
    public String getWorkerThreadPrefix() {
        return CONFIG.getConfig("transport.thread-factory.worker-thread-prefix",
            enableEpoll() ? EPOLL_WORKER_THREAD_PREFIX : NIO_WORKER_THREAD_PREFIX);
    }

    /**
     * Get executor thread prefix string.
     *
     * @return the string
     */
    public String getExecutorThreadPrefix() {
        return CONFIG.getConfig("transport.thread-factory.server-executor-thread-prefix",
            DEFAULT_EXECUTOR_THREAD_PREFIX);
    }

    /**
     * Get boss thread size int.
     *
     * @return the int
     */
    public int getBossThreadSize() {
        return CONFIG.getInt("transport.thread-factory.boss-thread-size", DEFAULT_BOSS_THREAD_SIZE);
    }

}
