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
package io.seata.core.rpc;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.NetUtil;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.AbstractResultMessage;
import io.seata.core.protocol.HeartbeatMessage;
import io.seata.core.protocol.MergeResultMessage;
import io.seata.core.protocol.MergedWarpMessage;
import io.seata.core.protocol.RegisterRMRequest;
import io.seata.core.protocol.RegisterRMResponse;
import io.seata.core.protocol.RegisterTMRequest;
import io.seata.core.protocol.RegisterTMResponse;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.protocol.Version;
import io.seata.core.rpc.netty.RegisterCheckAuthHandler;

import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Default server message listener.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2018 /10/18
 */
public class DefaultServerMessageListenerImpl implements ServerMessageListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultServerMessageListenerImpl.class);
    private static BlockingQueue<String> messageStrings = new LinkedBlockingQueue<String>();
    private ServerMessageSender serverMessageSender;
    private final TransactionMessageHandler transactionMessageHandler;
    private static final int MAX_LOG_SEND_THREAD = 1;
    private static final long KEEP_ALIVE_TIME = 0L;
    private static final String THREAD_PREFIX = "batchLoggerPrint";
    private static final long IDLE_CHECK_MILLS = 3L;
    private static final String BATCH_LOG_SPLIT = "\n";

    /**
     * Instantiates a new Default server message listener.
     *
     * @param transactionMessageHandler the transaction message handler
     */
    public DefaultServerMessageListenerImpl(TransactionMessageHandler transactionMessageHandler) {
        this.transactionMessageHandler = transactionMessageHandler;
    }

    @Override
    public void onTrxMessage(RpcMessage request, ChannelHandlerContext ctx, ServerMessageSender sender) {
        Object message = request.getBody();
        RpcContext rpcContext = ChannelManager.getContextFromIdentified(ctx.channel());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                "server received:{},clientIp:{},vgroup:{}", message, NetUtil.toIpAddress(ctx.channel().remoteAddress())
                    ,rpcContext.getTransactionServiceGroup());
        } else {
            messageStrings.offer(
                message + ",clientIp:" + NetUtil.toIpAddress(ctx.channel().remoteAddress()) + ",vgroup:" + rpcContext
                    .getTransactionServiceGroup());
        }
        if (!(message instanceof AbstractMessage)) { return; }
        if (message instanceof MergedWarpMessage) {
            AbstractResultMessage[] results = new AbstractResultMessage[((MergedWarpMessage)message).msgs.size()];
            for (int i = 0; i < results.length; i++) {
                final AbstractMessage subMessage = ((MergedWarpMessage)message).msgs.get(i);
                results[i] = transactionMessageHandler.onRequest(subMessage, rpcContext);
            }
            MergeResultMessage resultMessage = new MergeResultMessage();
            resultMessage.setMsgs(results);
            sender.sendResponse(request, ctx.channel(), resultMessage);
        } else if (message instanceof AbstractResultMessage) {
            transactionMessageHandler.onResponse((AbstractResultMessage)message, rpcContext);
        }
    }

    @Override
    public void onRegRmMessage(RpcMessage request, ChannelHandlerContext ctx,
                               ServerMessageSender sender, RegisterCheckAuthHandler checkAuthHandler) {
        RegisterRMRequest message = (RegisterRMRequest) request.getBody();
        boolean isSuccess = false;
        try {
            if (null == checkAuthHandler || checkAuthHandler.regResourceManagerCheckAuth(message)) {
                ChannelManager.registerRMChannel(message, ctx.channel());
                Version.putChannelVersion(ctx.channel(), message.getVersion());
                isSuccess = true;
            }
        } catch (Exception exx) {
            isSuccess = false;
            LOGGER.error(exx.getMessage());
        }
        sender.sendResponse(request, ctx.channel(), new RegisterRMResponse(isSuccess));
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("rm register success,message:{},channel:{}", message, ctx.channel());
        }
    }

    @Override
    public void onRegTmMessage(RpcMessage request, ChannelHandlerContext ctx,
                               ServerMessageSender sender, RegisterCheckAuthHandler checkAuthHandler) {
        RegisterTMRequest message = (RegisterTMRequest) request.getBody();
        String ipAndPort = NetUtil.toStringAddress(ctx.channel().remoteAddress());
        Version.putChannelVersion(ctx.channel(), message.getVersion());
        boolean isSuccess = false;
        try {
            if (null == checkAuthHandler || checkAuthHandler.regTransactionManagerCheckAuth(message)) {
                ChannelManager.registerTMChannel(message, ctx.channel());
                Version.putChannelVersion(ctx.channel(), message.getVersion());
                isSuccess = true;
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(String
                        .format("checkAuth for client:%s vgroup:%s ok", ipAndPort,
                            message.getTransactionServiceGroup()));
                }
            }
        } catch (Exception exx) {
            isSuccess = false;
            LOGGER.error(exx.getMessage());
        }
        //FIXME please add success or fail
        sender.sendResponse(request, ctx.channel(),
            new RegisterTMResponse(isSuccess));
    }

    @Override
    public void onCheckMessage(RpcMessage request, ChannelHandlerContext ctx, ServerMessageSender sender) {
        try {
            sender.sendResponse(request, ctx.channel(), HeartbeatMessage.PONG);
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
    public ServerMessageSender getServerMessageSender() {
        return serverMessageSender;
    }

    /**
     * Sets server message sender.
     *
     * @param serverMessageSender the server message sender
     */
    public void setServerMessageSender(ServerMessageSender serverMessageSender) {
        this.serverMessageSender = serverMessageSender;
    }

    /**
     * The type Batch log runnable.
     */
    static class BatchLogRunnable implements Runnable {

        @Override
        public void run() {
            while (true) {
                if (messageStrings.size() > 0) {
                    StringBuilder builder = new StringBuilder();
                    while (!messageStrings.isEmpty()) {
                        builder.append(messageStrings.poll()).append(BATCH_LOG_SPLIT);
                    }
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(builder.toString());
                    }
                }
                try {
                    Thread.sleep(IDLE_CHECK_MILLS);
                } catch (InterruptedException exx) {
                    LOGGER.error(exx.getMessage());
                }
            }
        }
    }

}
