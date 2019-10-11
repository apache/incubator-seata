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
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.seata.common.util.NetUtil;
import io.seata.core.protocol.HeartbeatMessage;
import io.seata.core.protocol.RegisterRMRequest;
import io.seata.core.protocol.RegisterTMRequest;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.rpc.ChannelManager;
import io.seata.core.rpc.DefaultServerMessageListenerImpl;
import io.seata.core.rpc.RpcContext;
import io.seata.core.rpc.ServerMessageListener;
import io.seata.core.rpc.ServerMessageSender;
import io.seata.core.rpc.TransactionMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeoutException;

/**
 * The type Abstract rpc server.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2018 /10/15
 */
@Sharable
public class RpcServer extends AbstractRpcRemotingServer implements ServerMessageSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServer.class);

    /**
     * The Server message listener.
     */
    protected ServerMessageListener serverMessageListener;

    private TransactionMessageHandler transactionMessageHandler;
    private RegisterCheckAuthHandler checkAuthHandler;

    /**
     * Sets transactionMessageHandler.
     *
     * @param transactionMessageHandler the transactionMessageHandler
     */
    public void setHandler(TransactionMessageHandler transactionMessageHandler) {
        setHandler(transactionMessageHandler, null);
    }

    /**
     * Sets transactionMessageHandler.
     *
     * @param transactionMessageHandler the transactionMessageHandler
     * @param checkAuthHandler          the check auth handler
     */
    public void setHandler(TransactionMessageHandler transactionMessageHandler,
                           RegisterCheckAuthHandler checkAuthHandler) {
        this.transactionMessageHandler = transactionMessageHandler;
        this.checkAuthHandler = checkAuthHandler;
    }

    /**
     * Instantiates a new Abstract rpc server.
     *
     * @param messageExecutor the message executor
     */
    public RpcServer(ThreadPoolExecutor messageExecutor) {
        super(new NettyServerConfig(), messageExecutor);
    }

    /**
     * Gets server message listener.
     *
     * @return the server message listener
     */
    public ServerMessageListener getServerMessageListener() {
        return serverMessageListener;
    }

    /**
     * Sets server message listener.
     *
     * @param serverMessageListener the server message listener
     */
    public void setServerMessageListener(ServerMessageListener serverMessageListener) {
        this.serverMessageListener = serverMessageListener;
    }

    /**
     * Debug log.
     *
     * @param info the info
     */
    public void debugLog(String info) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(info);
        }
    }

    /**
     * Init.
     */
    @Override
    public void init() {
        super.init();
        setChannelHandlers(RpcServer.this);
        DefaultServerMessageListenerImpl defaultServerMessageListenerImpl = new DefaultServerMessageListenerImpl(
            transactionMessageHandler);
        defaultServerMessageListenerImpl.init();
        defaultServerMessageListenerImpl.setServerMessageSender(this);
        this.setServerMessageListener(defaultServerMessageListenerImpl);
        super.start();

    }

    private void closeChannelHandlerContext(ChannelHandlerContext ctx) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("closeChannelHandlerContext channel:" + ctx.channel());
        }
        ctx.disconnect();
        ctx.close();
    }

    /**
     * User event triggered.
     *
     * @param ctx the ctx
     * @param evt the evt
     * @throws Exception the exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            debugLog("idle:" + evt);
            IdleStateEvent idleStateEvent = (IdleStateEvent)evt;
            if (idleStateEvent.state() == IdleState.READER_IDLE) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("channel:" + ctx.channel() + " read idle.");
                }
                handleDisconnect(ctx);
                try {
                    closeChannelHandlerContext(ctx);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage());
                }
            }
        }
    }

    /**
     * Destroy.
     */
    @Override
    public void destroy() {
        super.destroy();
        super.shutdown();
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
            super.sendResponse(request, clientChannel, msg);
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
     * @param channel   the channel
     * @param message    the msg
     * @return the object
     * @throws TimeoutException the timeout exception
     */
    @Override
    public Object sendASyncRequest(Channel channel, Object message) throws IOException, TimeoutException {
       return sendAsyncRequestWithoutResponse(channel, message);
    }

    /**
     * Dispatch.
     *
     * @param request the request
     * @param ctx   the ctx
     */
    @Override
    public void dispatch(RpcMessage request, ChannelHandlerContext ctx) {
        Object msg = request.getBody();
        if (msg instanceof RegisterRMRequest) {
            serverMessageListener.onRegRmMessage(request, ctx, this,
                checkAuthHandler);
        } else {
            if (ChannelManager.isRegistered(ctx.channel())) {
                serverMessageListener.onTrxMessage(request, ctx, this);
            } else {
                try {
                    closeChannelHandlerContext(ctx);
                } catch (Exception exx) {
                    LOGGER.error(exx.getMessage());
                }
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(String.format("close a unhandled connection! [%s]", ctx.channel().toString()));
                }
            }
        }
    }

    /**
     * Channel inactive.
     *
     * @param ctx the ctx
     * @throws Exception the exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        debugLog("inactive:" + ctx);
        if (messageExecutor.isShutdown()) {
            return;
        }
        handleDisconnect(ctx);
        super.channelInactive(ctx);
    }

    private void handleDisconnect(ChannelHandlerContext ctx) {
        final String ipAndPort = NetUtil.toStringAddress(ctx.channel().remoteAddress());
        RpcContext rpcContext = ChannelManager.getContextFromIdentified(ctx.channel());
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(ipAndPort + " to server channel inactive.");
        }
        if (null != rpcContext && null != rpcContext.getClientRole()) {
            rpcContext.release();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("remove channel:" + ctx.channel() + "context:" + rpcContext);
            }
        } else {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("remove unused channel:" + ctx.channel());
            }
        }
    }

    /**
     * Channel read.
     *
     * @param ctx the ctx
     * @param msg the msg
     * @throws Exception the exception
     */
    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof RpcMessage) {
            RpcMessage rpcMessage = (RpcMessage) msg;
            debugLog("read:" + rpcMessage.getBody());
            if (rpcMessage.getBody() instanceof RegisterTMRequest) {
                serverMessageListener.onRegTmMessage(rpcMessage, ctx, this, checkAuthHandler);
                return;
            }
            if (rpcMessage.getBody() == HeartbeatMessage.PING) {
                serverMessageListener.onCheckMessage(rpcMessage, ctx, this);
                return;
            }
        }
        super.channelRead(ctx, msg);
    }

    /**
     * Exception caught.
     *
     * @param ctx   the ctx
     * @param cause the cause
     * @throws Exception the exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("channel exx:" + cause.getMessage() + ",channel:" + ctx.channel());
        }
        ChannelManager.releaseRpcContext(ctx.channel());
        super.exceptionCaught(ctx, cause);
    }
}
