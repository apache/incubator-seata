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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import io.netty.channel.Channel;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.core.protocol.MessageType;
import org.apache.seata.core.rpc.ShutdownHook;
import org.apache.seata.core.rpc.TransactionMessageHandler;
import org.apache.seata.core.rpc.processor.server.RegRmProcessor;
import org.apache.seata.core.rpc.processor.server.RegTmProcessor;
import org.apache.seata.core.rpc.processor.server.ServerHeartbeatProcessor;
import org.apache.seata.core.rpc.processor.server.ServerOnRequestProcessor;
import org.apache.seata.core.rpc.processor.server.ServerOnResponseProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The netty remoting server.
 *
 */
public class NettyRemotingServer extends AbstractNettyRemotingServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyRemotingServer.class);

    private TransactionMessageHandler transactionMessageHandler;

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private final ThreadPoolExecutor branchResultMessageExecutor = new ThreadPoolExecutor(NettyServerConfig.getMinBranchResultPoolSize(),
            NettyServerConfig.getMaxBranchResultPoolSize(), NettyServerConfig.getKeepAliveTime(), TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(NettyServerConfig.getMaxTaskQueueSize()),
            new NamedThreadFactory("BranchResultHandlerThread", NettyServerConfig.getMaxBranchResultPoolSize()), new ThreadPoolExecutor.CallerRunsPolicy());

    @Override
    public void init() {
        // registry processor
        registerProcessor();
        if (initialized.compareAndSet(false, true)) {
            super.init();
        }
    }

    /**
     * Instantiates a new Rpc remoting server.
     *
     * @param messageExecutor   the message executor
     */
    public NettyRemotingServer(ThreadPoolExecutor messageExecutor) {
        super(messageExecutor, new NettyServerConfig());
    }

    public NettyRemotingServer(ThreadPoolExecutor messageExecutor, NettyServerConfig nettyServerConfig) {
        super(messageExecutor, nettyServerConfig);
    }

    /**
     * Sets transactionMessageHandler.
     *
     * @param transactionMessageHandler the transactionMessageHandler
     */
    public void setHandler(TransactionMessageHandler transactionMessageHandler) {
        this.transactionMessageHandler = transactionMessageHandler;
    }

    public TransactionMessageHandler getHandler() {
        return transactionMessageHandler;
    }

    @Override
    public void destroyChannel(String serverAddress, Channel channel) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("will destroy channel:{},address:{}", channel, serverAddress);
        }
        channel.disconnect();
        channel.close();
    }

    private void registerProcessor() {
        // 1. registry on request message processor
        ServerOnRequestProcessor onRequestProcessor =
            new ServerOnRequestProcessor(this, getHandler());
        ShutdownHook.getInstance().addDisposable(onRequestProcessor);
        super.registerProcessor(MessageType.TYPE_BRANCH_REGISTER, onRequestProcessor, messageExecutor);
        super.registerProcessor(MessageType.TYPE_BRANCH_STATUS_REPORT, onRequestProcessor, messageExecutor);
        super.registerProcessor(MessageType.TYPE_GLOBAL_BEGIN, onRequestProcessor, messageExecutor);
        super.registerProcessor(MessageType.TYPE_GLOBAL_COMMIT, onRequestProcessor, messageExecutor);
        super.registerProcessor(MessageType.TYPE_GLOBAL_LOCK_QUERY, onRequestProcessor, messageExecutor);
        super.registerProcessor(MessageType.TYPE_GLOBAL_REPORT, onRequestProcessor, messageExecutor);
        super.registerProcessor(MessageType.TYPE_GLOBAL_ROLLBACK, onRequestProcessor, messageExecutor);
        super.registerProcessor(MessageType.TYPE_GLOBAL_STATUS, onRequestProcessor, messageExecutor);
        super.registerProcessor(MessageType.TYPE_SEATA_MERGE, onRequestProcessor, messageExecutor);
        // 2. registry on response message processor
        ServerOnResponseProcessor onResponseProcessor =
            new ServerOnResponseProcessor(getHandler(), getFutures());
        super.registerProcessor(MessageType.TYPE_BRANCH_COMMIT_RESULT, onResponseProcessor, branchResultMessageExecutor);
        super.registerProcessor(MessageType.TYPE_BRANCH_ROLLBACK_RESULT, onResponseProcessor, branchResultMessageExecutor);
        // 3. registry rm message processor
        RegRmProcessor regRmProcessor = new RegRmProcessor(this);
        super.registerProcessor(MessageType.TYPE_REG_RM, regRmProcessor, messageExecutor);
        // 4. registry tm message processor
        RegTmProcessor regTmProcessor = new RegTmProcessor(this);
        super.registerProcessor(MessageType.TYPE_REG_CLT, regTmProcessor, null);
        // 5. registry heartbeat message processor
        ServerHeartbeatProcessor heartbeatMessageProcessor = new ServerHeartbeatProcessor(this);
        super.registerProcessor(MessageType.TYPE_HEARTBEAT_MSG, heartbeatMessageProcessor, null);
    }

    @Override
    public void destroy() {
        super.destroy();
        branchResultMessageExecutor.shutdown();
    }
}
