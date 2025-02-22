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
package org.apache.seata.mockserver;

import java.util.concurrent.ThreadPoolExecutor;

import io.netty.channel.Channel;
import org.apache.seata.core.protocol.MessageType;
import org.apache.seata.core.rpc.TransactionMessageHandler;
import org.apache.seata.core.rpc.netty.AbstractNettyRemotingServer;
import org.apache.seata.core.rpc.netty.NettyServerConfig;
import org.apache.seata.mockserver.processor.MockHeartbeatProcessor;
import org.apache.seata.mockserver.processor.MockOnReqProcessor;
import org.apache.seata.mockserver.processor.MockOnRespProcessor;
import org.apache.seata.mockserver.processor.MockRegisterProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The mock netty remoting server.
 */
public class MockNettyRemotingServer extends AbstractNettyRemotingServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockNettyRemotingServer.class);

    private TransactionMessageHandler handler;

    /**
     * Sets handler.
     *
     * @param transactionMessageHandler the transaction message handler
     */
    public void setHandler(TransactionMessageHandler transactionMessageHandler) {
        this.handler = transactionMessageHandler;
    }

    @Override
    public void init() {
        // registry processor
        registerProcessor();
        super.init();
    }

    /**
     * Instantiates a new Rpc remoting server.
     *
     * @param messageExecutor the message executor
     */
    public MockNettyRemotingServer(ThreadPoolExecutor messageExecutor) {
        this(messageExecutor, new NettyServerConfig());
    }

    /**
     * Instantiates a new Mock netty remoting server.
     *
     * @param messageExecutor   the message executor
     * @param nettyServerConfig the netty server config
     */
    public MockNettyRemotingServer(ThreadPoolExecutor messageExecutor, NettyServerConfig nettyServerConfig) {
        super(messageExecutor, nettyServerConfig);
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
        MockOnReqProcessor onRequestProcessor = new MockOnReqProcessor(this, handler);
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
        MockOnRespProcessor onResponseProcessor = new MockOnRespProcessor(this, handler, getFutures());
        super.registerProcessor(MessageType.TYPE_BRANCH_COMMIT_RESULT, onResponseProcessor, messageExecutor);
        super.registerProcessor(MessageType.TYPE_BRANCH_ROLLBACK_RESULT, onResponseProcessor, messageExecutor);

        // 3. registry rm reg processor
        MockRegisterProcessor regRmProcessor = new MockRegisterProcessor(this, MockRegisterProcessor.Role.RM);
        super.registerProcessor(MessageType.TYPE_REG_RM, regRmProcessor, messageExecutor);

        // 4. registry tm reg processor
        MockRegisterProcessor regTmProcessor = new MockRegisterProcessor(this, MockRegisterProcessor.Role.TM);
        super.registerProcessor(MessageType.TYPE_REG_CLT, regTmProcessor, null);

        // 5. registry heartbeat message processor
        MockHeartbeatProcessor heartbeatMessageProcessor = new MockHeartbeatProcessor(this, handler);
        super.registerProcessor(MessageType.TYPE_HEARTBEAT_MSG, heartbeatMessageProcessor, null);
    }

    @Override
    public void destroy() {
        super.destroy();
    }
}
