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
package org.apache.seata.core.rpc;

import io.netty.channel.ChannelHandlerContext;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.core.auth.AuthResult;
import org.apache.seata.core.protocol.*;
import org.apache.seata.core.rpc.netty.ChannelManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * The type Default server message listener.
 *
 */
@Deprecated
public class DefaultServerMessageListenerImpl implements ServerMessageListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultServerMessageListenerImpl.class);
    private static BlockingQueue<String> logQueue = new LinkedBlockingQueue<>();
    private RemotingServer remotingServer;
    private final TransactionMessageHandler transactionMessageHandler;
    private static final int MAX_LOG_SEND_THREAD = 1;
    private static final int MAX_LOG_TAKE_SIZE = 1024;
    private static final long KEEP_ALIVE_TIME = 0L;
    private static final String THREAD_PREFIX = "batchLoggerPrint";
    private static final long BUSY_SLEEP_MILLS = 5L;

    /**
     * Instantiates a new Default server message listener.
     *
     * @param transactionMessageHandler the transaction message handler
     */
    public DefaultServerMessageListenerImpl(TransactionMessageHandler transactionMessageHandler) {
        this.transactionMessageHandler = transactionMessageHandler;
    }

    @Override
    public void onTrxMessage(RpcMessage request, ChannelHandlerContext ctx) {
        Object message = request.getBody();
        RpcContext rpcContext = ChannelManager.getContextFromIdentified(ctx.channel());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("server received:{},clientIp:{},vgroup:{}", message,
                NetUtil.toIpAddress(ctx.channel().remoteAddress()), rpcContext.getTransactionServiceGroup());
        } else {
            try {
                logQueue.put(message + ",clientIp:" + NetUtil.toIpAddress(ctx.channel().remoteAddress()) + ",vgroup:"
                    + rpcContext.getTransactionServiceGroup());
            } catch (InterruptedException e) {
                LOGGER.error("put message to logQueue error: {}", e.getMessage(), e);
            }
        }
        if (!(message instanceof AbstractMessage)) {
            return;
        }
        if (message instanceof MergedWarpMessage) {
            AbstractResultMessage[] results = new AbstractResultMessage[((MergedWarpMessage) message).msgs.size()];
            for (int i = 0; i < results.length; i++) {
                final AbstractMessage subMessage = ((MergedWarpMessage) message).msgs.get(i);
                results[i] = transactionMessageHandler.onRequest(subMessage, rpcContext);
            }
            MergeResultMessage resultMessage = new MergeResultMessage();
            resultMessage.setMsgs(results);
            getServerMessageSender().sendAsyncResponse(request, ctx.channel(), resultMessage);
        } else if (message instanceof AbstractResultMessage) {
            transactionMessageHandler.onResponse((AbstractResultMessage) message, rpcContext);
        } else {
            // the single send request message
            final AbstractMessage msg = (AbstractMessage) message;
            AbstractResultMessage result = transactionMessageHandler.onRequest(msg, rpcContext);
            getServerMessageSender().sendAsyncResponse(request, ctx.channel(), result);
        }
    }

    @Override
    public void onRegRmMessage(RpcMessage rpcMessage, ChannelHandlerContext ctx, RegisterCheckAuthHandler checkAuthHandler) {
        RegisterRMRequest message = (RegisterRMRequest) rpcMessage.getBody();
        String ipAndPort = NetUtil.toStringAddress(ctx.channel().remoteAddress());
        RegisterRMResponse response = new RegisterRMResponse(false);
        try {
            AuthResult authResult = (checkAuthHandler != null) ? checkAuthHandler.regResourceManagerCheckAuth(message) : null;
            if (checkAuthHandler == null || authResult.getResultCode().equals(ResultCode.Success)
                    || authResult.getResultCode().equals(ResultCode.AccessTokenNearExpiration)) {
                ChannelManager.registerRMChannel(message, ctx.channel());
                Version.putChannelVersion(ctx.channel(), message.getVersion());
                response.setIdentified(true);
                response.setResultCode(checkAuthHandler == null ? ResultCode.Success : authResult.getResultCode());
                response.setExtraData(checkAuthHandler.fetchNewToken(authResult));
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("RM checkAuth for client:{},vgroup:{},applicationId:{} is OK",
                            ipAndPort, message.getTransactionServiceGroup(), message.getApplicationId());
                }
            } else {
                if (authResult.getResultCode().equals(ResultCode.Failed)) {
                    response.setMsg("RM checkAuth failed!Please check your username/password or token.");
                } else if (authResult.getResultCode().equals(ResultCode.AccessTokenExpired)) {
                    response.setMsg("RM checkAuth failed! The access token has been expired.");
                } else if (authResult.getResultCode().equals(ResultCode.RefreshTokenExpired)) {
                    response.setMsg("RM checkAuth failed! The refresh token has been expired.");
                }
                response.setResultCode(authResult.getResultCode());
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("RM checkAuth for client:{},vgroup:{},applicationId:{} is FAIL",
                            ipAndPort, message.getTransactionServiceGroup(), message.getApplicationId());
                }
            }
        } catch (IncompatibleVersionException e) {
            LOGGER.error("RM register fail, error message:{}", e.getMessage());
            response.setResultCode(ResultCode.Failed);
        }
        remotingServer.sendAsyncResponse(rpcMessage, ctx.channel(), response);
        if (response.isIdentified() && LOGGER.isInfoEnabled()) {
            LOGGER.info("RM register success,message:{},channel:{},client version:{}", message, ctx.channel(),
                    message.getVersion());
        }
    }

    @Override
    public void onRegTmMessage(RpcMessage rpcMessage, ChannelHandlerContext ctx, RegisterCheckAuthHandler checkAuthHandler) {
        RegisterTMRequest message = (RegisterTMRequest) rpcMessage.getBody();
        String ipAndPort = NetUtil.toStringAddress(ctx.channel().remoteAddress());
        Version.putChannelVersion(ctx.channel(), message.getVersion());
        RegisterTMResponse response = new RegisterTMResponse(false);
        try {
            AuthResult authResult = (checkAuthHandler != null) ? checkAuthHandler.regTransactionManagerCheckAuth(message) : null;
            if (checkAuthHandler == null || authResult.getResultCode().equals(ResultCode.Success)
                    || authResult.getResultCode().equals(ResultCode.AccessTokenNearExpiration)) {
                ChannelManager.registerTMChannel(message, ctx.channel());
                Version.putChannelVersion(ctx.channel(), message.getVersion());
                response.setIdentified(true);
                response.setResultCode(checkAuthHandler == null ? ResultCode.Success : authResult.getResultCode());
                response.setExtraData(checkAuthHandler.fetchNewToken(authResult));
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("TM checkAuth for client:{},vgroup:{},applicationId:{} is OK",
                            ipAndPort, message.getTransactionServiceGroup(), message.getApplicationId());
                }
            } else {
                if (authResult.getResultCode().equals(ResultCode.Failed)) {
                    response.setMsg("TM checkAuth failed!Please check your username/password.");
                } else if (authResult.getResultCode().equals(ResultCode.AccessTokenExpired)) {
                    response.setMsg("TM checkAuth failed! The access token has been expired.");
                } else if (authResult.getResultCode().equals(ResultCode.RefreshTokenExpired)) {
                    response.setMsg("TM checkAuth failed! The refresh token has been expired.");
                }
                response.setResultCode(authResult.getResultCode());
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("TM checkAuth for client:{},vgroup:{},applicationId:{} is FAIL",
                            ipAndPort, message.getTransactionServiceGroup(), message.getApplicationId());
                }
            }
        } catch (IncompatibleVersionException e) {
            LOGGER.error("TM register fail, error message:{}", e.getMessage());
            response.setResultCode(ResultCode.Failed);
        }
        remotingServer.sendAsyncResponse(rpcMessage, ctx.channel(), response);
        if (response.isIdentified() && LOGGER.isInfoEnabled()) {
            LOGGER.info("TM register success,message:{},channel:{},client version:{}", message, ctx.channel(),
                    message.getVersion());
        }
    }

    @Override
    public void onCheckMessage(RpcMessage request, ChannelHandlerContext ctx) {
        try {
            getServerMessageSender().sendAsyncResponse(request, ctx.channel(), HeartbeatMessage.PONG);
        } catch (Throwable throwable) {
            LOGGER.error("send response error: {}", throwable.getMessage(), throwable);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received PING from {}", ctx.channel().remoteAddress());
        }
    }

    /**
     * Init.
     */
    public void init() {
        ExecutorService mergeSendExecutorService = new ThreadPoolExecutor(MAX_LOG_SEND_THREAD, MAX_LOG_SEND_THREAD,
            KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(),
            new NamedThreadFactory(THREAD_PREFIX, MAX_LOG_SEND_THREAD, true));
        mergeSendExecutorService.submit(new BatchLogRunnable());
    }

    /**
     * Gets server message sender.
     *
     * @return the server message sender
     */
    public RemotingServer getServerMessageSender() {
        if (remotingServer == null) {
            throw new IllegalArgumentException("serverMessageSender must not be null");
        }
        return remotingServer;
    }

    /**
     * Sets server message sender.
     *
     * @param remotingServer the remoting server
     */
    public void setServerMessageSender(RemotingServer remotingServer) {
        this.remotingServer = remotingServer;
    }

    /**
     * The type Batch log runnable.
     */
    static class BatchLogRunnable implements Runnable {

        @Override
        public void run() {
            List<String> logList = new ArrayList<>();
            while (true) {
                try {
                    logList.add(logQueue.take());
                    logQueue.drainTo(logList, MAX_LOG_TAKE_SIZE);
                    if (LOGGER.isInfoEnabled()) {
                        for (String str : logList) {
                            LOGGER.info(str);
                        }
                    }
                    logList.clear();
                    TimeUnit.MILLISECONDS.sleep(BUSY_SLEEP_MILLS);
                } catch (InterruptedException exx) {
                    LOGGER.error("batch log busy sleep error:{}", exx.getMessage(), exx);
                }

            }
        }
    }

}
