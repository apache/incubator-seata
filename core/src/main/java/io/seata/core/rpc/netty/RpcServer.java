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

import io.netty.channel.Channel;
import io.seata.core.protocol.HeartbeatMessage;
import io.seata.core.protocol.MessageType;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.rpc.ChannelManager;
import io.seata.core.rpc.netty.processor.NettyProcessor;
import io.seata.core.rpc.netty.processor.Pair;
import io.seata.core.rpc.netty.processor.server.ServerHeartbeatMessageProcessor;
import io.seata.core.rpc.netty.processor.server.RegRmMessageProcessor;
import io.seata.core.rpc.netty.processor.server.RegTmMessageProcessor;
import io.seata.core.rpc.netty.processor.server.TrxMessageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeoutException;

/**
 * The type Abstract rpc server.
 *
 * @author slievrly
 * @author zhangchenghui.dev@gmail.com
 */
public class RpcServer extends AbstractRpcRemotingServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServer.class);


    /**
     * Instantiates a new Abstract rpc server.
     *
     * @param messageExecutor the message executor
     */
    public RpcServer(ThreadPoolExecutor messageExecutor) {
        super(messageExecutor, new NettyServerConfig());
    }

    /**
     * Init.
     */
    @Override
    public void init() {
        // registry processor
        // 1. registry trx message processor
        TrxMessageProcessor trxMessageProcessor = new TrxMessageProcessor(this, getTransactionMessageHandler());
        // 1.1 request
        registerProcessor(MessageType.TYPE_BRANCH_REGISTER, trxMessageProcessor, messageExecutor);
        registerProcessor(MessageType.TYPE_BRANCH_STATUS_REPORT, trxMessageProcessor, messageExecutor);
        registerProcessor(MessageType.TYPE_GLOBAL_BEGIN, trxMessageProcessor, messageExecutor);
        registerProcessor(MessageType.TYPE_GLOBAL_COMMIT, trxMessageProcessor, messageExecutor);
        registerProcessor(MessageType.TYPE_GLOBAL_LOCK_QUERY, trxMessageProcessor, messageExecutor);
        registerProcessor(MessageType.TYPE_GLOBAL_REPORT, trxMessageProcessor, messageExecutor);
        registerProcessor(MessageType.TYPE_GLOBAL_ROLLBACK, trxMessageProcessor, messageExecutor);
        registerProcessor(MessageType.TYPE_GLOBAL_STATUS, trxMessageProcessor, messageExecutor);
        // 1.2 response
        registerProcessor(MessageType.TYPE_BRANCH_COMMIT_RESULT, trxMessageProcessor, messageExecutor);
        registerProcessor(MessageType.TYPE_BRANCH_REGISTER_RESULT, trxMessageProcessor, messageExecutor);
        registerProcessor(MessageType.TYPE_BRANCH_STATUS_REPORT_RESULT, trxMessageProcessor, messageExecutor);
        registerProcessor(MessageType.TYPE_BRANCH_ROLLBACK_RESULT, trxMessageProcessor, messageExecutor);
        registerProcessor(MessageType.TYPE_GLOBAL_BEGIN_RESULT, trxMessageProcessor, messageExecutor);
        registerProcessor(MessageType.TYPE_GLOBAL_COMMIT_RESULT, trxMessageProcessor, messageExecutor);
        registerProcessor(MessageType.TYPE_GLOBAL_LOCK_QUERY_RESULT, trxMessageProcessor, messageExecutor);
        registerProcessor(MessageType.TYPE_GLOBAL_REPORT_RESULT, trxMessageProcessor, messageExecutor);
        registerProcessor(MessageType.TYPE_GLOBAL_ROLLBACK_RESULT, trxMessageProcessor, messageExecutor);
        registerProcessor(MessageType.TYPE_GLOBAL_STATUS_RESULT, trxMessageProcessor, messageExecutor);
        registerProcessor(MessageType.TYPE_REG_RM_RESULT, trxMessageProcessor, messageExecutor);
        registerProcessor(MessageType.TYPE_REG_CLT_RESULT, trxMessageProcessor, messageExecutor);
        registerProcessor(MessageType.TYPE_SEATA_MERGE, trxMessageProcessor, messageExecutor);
        // 2. registry rm message processor
        RegRmMessageProcessor regRmMessageProcessor = new RegRmMessageProcessor(this, null);
        registerProcessor(MessageType.TYPE_REG_RM, regRmMessageProcessor, messageExecutor);
        // 3. registry tm message processor
        RegTmMessageProcessor regTmMessageProcessor = new RegTmMessageProcessor(this, null);
        registerProcessor(MessageType.TYPE_REG_CLT, regTmMessageProcessor, null);
        // 4. registry heartbeat message processor
        ServerHeartbeatMessageProcessor heartbeatMessageProcessor = new ServerHeartbeatMessageProcessor(this);
        registerProcessor(MessageType.TYPE_HEARTBEAT_MSG, heartbeatMessageProcessor, null);

        super.setChannelHandlers(new ServerHandler());
        super.init();
    }

    /**
     * Destroy.
     */
    @Override
    public void destroy() {
        super.destroy();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("destroyed rpcServer");
        }
    }


    /**
     * Send response.
     * rm reg,rpc reg,inner response
     *
     * @param request the request
     * @param channel the channel
     * @param msg     the msg
     */
    @Override
    public void sendResponse(RpcMessage request, Channel channel, Object msg) {
        Channel clientChannel = channel;
        if (!(msg instanceof HeartbeatMessage)) {
            clientChannel = ChannelManager.getSameClientChannel(channel);
        }
        if (clientChannel != null) {
            super.defaultSendResponse(request, clientChannel, msg);
        } else {
            throw new RuntimeException("channel is error. channel:" + clientChannel);
        }
    }

    /**
     * Send request with response object.
     * send syn request for rm
     *
     * @param resourceId the db key
     * @param clientId   the client ip
     * @param message    the message
     * @param timeout    the timeout
     * @return the object
     * @throws TimeoutException the timeout exception
     */
    @Override
    public Object sendSyncRequest(String resourceId, String clientId, Object message,
                                  long timeout) throws TimeoutException {
        Channel clientChannel = ChannelManager.getChannel(resourceId, clientId);
        if (clientChannel == null) {
            throw new RuntimeException("rm client is not connected. dbkey:" + resourceId
                + ",clientId:" + clientId);

        }
        return sendAsyncRequestWithResponse(null, clientChannel, message, timeout);
    }

    /**
     * Send request with response object.
     * send syn request for rm
     *
     * @param clientChannel the client channel
     * @param message       the message
     * @return the object
     * @throws TimeoutException the timeout exception
     */
    @Override
    public Object sendSyncRequest(Channel clientChannel, Object message) throws TimeoutException {
        return sendSyncRequest(clientChannel, message, NettyServerConfig.getRpcRequestTimeout());
    }

    /**
     * Send request with response object.
     * send syn request for rm
     *
     * @param clientChannel the client channel
     * @param message       the message
     * @param timeout       the timeout
     * @return the object
     * @throws TimeoutException the timeout exception
     */
    @Override
    public Object sendSyncRequest(Channel clientChannel, Object message, long timeout) throws TimeoutException {
        if (clientChannel == null) {
            throw new RuntimeException("rm client is not connected");

        }
        return sendAsyncRequestWithResponse(null, clientChannel, message, timeout);
    }

    /**
     * Send request with response object.
     *
     * @param resourceId the db key
     * @param clientId   the client ip
     * @param message    the msg
     * @return the object
     * @throws TimeoutException the timeout exception
     */
    @Override
    public Object sendSyncRequest(String resourceId, String clientId, Object message)
        throws TimeoutException {
        return sendSyncRequest(resourceId, clientId, message, NettyServerConfig.getRpcRequestTimeout());
    }

    /**
     * Send request with response object.
     *
     * @param channel the channel
     * @param message the msg
     * @return the object
     * @throws TimeoutException the timeout exception
     */
    @Override
    public Object sendASyncRequest(Channel channel, Object message) throws TimeoutException {
        return sendAsyncRequestWithoutResponse(channel, message);
    }

    @Override
    public void registerProcessor(int messageType, NettyProcessor processor, ExecutorService executor) {
        Pair<NettyProcessor, ExecutorService> pair = new Pair<>(processor, executor);
        this.processorTable.put(messageType, pair);
    }
}
