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
import io.seata.core.rpc.processor.server.RegRmProcessor;
import io.seata.core.rpc.processor.server.RegTmProcessor;
import io.seata.core.rpc.processor.server.ServerHeartbeatProcessor;
import io.seata.core.rpc.processor.server.ServerOnRequestProcessor;
import io.seata.core.rpc.processor.server.ServerOnResponseProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        registerProcessor();
        setChannelHandlers(new ServerHandler());
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

    private void registerProcessor() {
        // 1. registry on request message processor
        ServerOnRequestProcessor onRequestProcessor =
            new ServerOnRequestProcessor(this, getTransactionMessageHandler());
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
            new ServerOnResponseProcessor(getTransactionMessageHandler(), getFutures());
        super.registerProcessor(MessageType.TYPE_BRANCH_COMMIT_RESULT, onResponseProcessor, messageExecutor);
        super.registerProcessor(MessageType.TYPE_BRANCH_ROLLBACK_RESULT, onResponseProcessor, messageExecutor);
        // 3. registry rm message processor
        RegRmProcessor regRmProcessor = new RegRmProcessor(this, null);
        super.registerProcessor(MessageType.TYPE_REG_RM, regRmProcessor, messageExecutor);
        // 4. registry tm message processor
        RegTmProcessor regTmProcessor = new RegTmProcessor(this, null);
        super.registerProcessor(MessageType.TYPE_REG_CLT, regTmProcessor, null);
        // 5. registry heartbeat message processor
        ServerHeartbeatProcessor heartbeatMessageProcessor = new ServerHeartbeatProcessor(this);
        super.registerProcessor(MessageType.TYPE_HEARTBEAT_MSG, heartbeatMessageProcessor, null);
    }
}