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
package io.seata.core.rpc.netty;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.channel.Channel;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.core.protocol.MessageType;
import io.seata.core.rpc.ShutdownHook;
import io.seata.core.rpc.TransactionMessageHandler;
import io.seata.core.rpc.processor.server.BranchRegisterProcessor;
import io.seata.core.rpc.processor.server.BranchReportProcessor;
import io.seata.core.rpc.processor.server.GlobalBeginProcessor;
import io.seata.core.rpc.processor.server.GlobalCommitProcessor;
import io.seata.core.rpc.processor.server.GlobalLockQueryProcessor;
import io.seata.core.rpc.processor.server.GlobalReportProcessor;
import io.seata.core.rpc.processor.server.GlobalRollbackProcessor;
import io.seata.core.rpc.processor.server.GlobalStatusProcessor;
import io.seata.core.rpc.processor.server.MergedWarpMessageProcessor;
import io.seata.core.rpc.processor.server.RegRmProcessor;
import io.seata.core.rpc.processor.server.RegTmProcessor;
import io.seata.core.rpc.processor.server.ServerHeartbeatProcessor;
import io.seata.core.rpc.processor.server.ServerOnResponseProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The netty remoting server.
 *
 * @author slievrly
 * @author xingfudeshi@gmail.com
 * @author zhangchenghui.dev@gmail.com
 */
public class NettyRemotingServer extends AbstractNettyRemotingServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyRemotingServer.class);

    private TransactionMessageHandler transactionMessageHandler;

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private ThreadPoolExecutor branchResultMessageExecutor = new ThreadPoolExecutor(NettyServerConfig.getMinBranchResultPoolSize(),
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
        registerProcessor(MessageType.TYPE_BRANCH_REGISTER, new BranchRegisterProcessor(getHandler()), messageExecutor);
        registerProcessor(MessageType.TYPE_BRANCH_STATUS_REPORT, new BranchReportProcessor(getHandler()), messageExecutor);
        registerProcessor(MessageType.TYPE_GLOBAL_BEGIN, new GlobalBeginProcessor(getHandler()), messageExecutor);
        registerProcessor(MessageType.TYPE_GLOBAL_COMMIT, new GlobalCommitProcessor(getHandler()), messageExecutor);
        registerProcessor(MessageType.TYPE_GLOBAL_LOCK_QUERY, new GlobalLockQueryProcessor(getHandler()), messageExecutor);
        registerProcessor(MessageType.TYPE_GLOBAL_REPORT, new GlobalReportProcessor(getHandler()), messageExecutor);
        registerProcessor(MessageType.TYPE_GLOBAL_ROLLBACK, new GlobalRollbackProcessor(getHandler()), messageExecutor);
        registerProcessor(MessageType.TYPE_GLOBAL_STATUS, new GlobalStatusProcessor(getHandler()), messageExecutor);
        MergedWarpMessageProcessor mergedWarpMessageProcessor =
                new MergedWarpMessageProcessor(this, getHandler());
        ShutdownHook.getInstance().addDisposable(mergedWarpMessageProcessor);
        registerProcessor(MessageType.TYPE_SEATA_MERGE, mergedWarpMessageProcessor, messageExecutor);

        // 2. registry on response message processor
        ServerOnResponseProcessor onResponseProcessor =
                new ServerOnResponseProcessor(getHandler(), getFutures());
        registerProcessor(MessageType.TYPE_BRANCH_COMMIT_RESULT, onResponseProcessor, branchResultMessageExecutor);
        registerProcessor(MessageType.TYPE_BRANCH_ROLLBACK_RESULT, onResponseProcessor, branchResultMessageExecutor);

        // 3. registry rm message processor
        RegRmProcessor regRmProcessor = new RegRmProcessor(this);
        registerProcessor(MessageType.TYPE_REG_RM, regRmProcessor, messageExecutor);

        // 4. registry tm message processor
        RegTmProcessor regTmProcessor = new RegTmProcessor(this);
        registerProcessor(MessageType.TYPE_REG_CLT, regTmProcessor, null);

        // 5. registry heartbeat message processor
        ServerHeartbeatProcessor heartbeatMessageProcessor = new ServerHeartbeatProcessor(this);
        registerProcessor(MessageType.TYPE_HEARTBEAT_MSG, heartbeatMessageProcessor, null);
    }

    @Override
    public void destroy() {
        super.destroy();
        branchResultMessageExecutor.shutdown();
    }
}
