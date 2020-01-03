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
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.exception.FrameworkException;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.thread.PositiveAtomicCounter;
import io.seata.core.protocol.HeartbeatMessage;
import io.seata.core.protocol.MergeMessage;
import io.seata.core.protocol.MessageFuture;
import io.seata.core.protocol.ProtocolConstants;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.rpc.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.SocketAddress;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The type Abstract rpc remoting.
 *
 * @author slievrly
 */
public abstract class AbstractRpcRemoting extends ChannelDuplexHandler implements Disposable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRpcRemoting.class);
    /**
     * The Timer executor.
     */
    protected final ScheduledExecutorService timerExecutor = new ScheduledThreadPoolExecutor(1,
        new NamedThreadFactory("timeoutChecker", 1, true));
    /**
     * The Message executor.
     */
    protected final ThreadPoolExecutor messageExecutor;

    /**
     * Id generator of this remoting
     */
    protected final PositiveAtomicCounter idGenerator = new PositiveAtomicCounter();

    /**
     * The Futures.
     */
    protected final ConcurrentHashMap<Integer, MessageFuture> futures = new ConcurrentHashMap<>();
    /**
     * The Basket map.
     */
    protected final ConcurrentHashMap<String, BlockingQueue<RpcMessage>> basketMap = new ConcurrentHashMap<>();

    private static final long NOT_WRITEABLE_CHECK_MILLS = 10L;
    /**
     * The Merge lock.
     */
    protected final Object mergeLock = new Object();
    /**
     * The Now mills.
     */
    protected volatile long nowMills = 0;
    private static final int TIMEOUT_CHECK_INTERNAL = 3000;
    private final Object lock = new Object();
    /**
     * The Is sending.
     */
    protected volatile boolean isSending = false;
    private String group = "DEFAULT";
    /**
     * The Merge msg map.
     */
    protected final Map<Integer, MergeMessage> mergeMsgMap = new ConcurrentHashMap<>();
    /**
     * The Channel handlers.
     */
    protected ChannelHandler[] channelHandlers;

    /**
     * Instantiates a new Abstract rpc remoting.
     *
     * @param messageExecutor the message executor
     */
    public AbstractRpcRemoting(ThreadPoolExecutor messageExecutor) {
        this.messageExecutor = messageExecutor;
    }

    /**
     * Gets next message id.
     *
     * @return the next message id
     */
    public int getNextMessageId() {
        return idGenerator.incrementAndGet();
    }

    /**
     * Init.
     */
    public void init() {
        timerExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<Integer, MessageFuture> entry : futures.entrySet()) {
                    if (entry.getValue().isTimeout()) {
                        futures.remove(entry.getKey());
                        entry.getValue().setResultMessage(null);
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("timeout clear future: {}", entry.getValue().getRequestMessage().getBody());
                        }
                    }
                }

                nowMills = System.currentTimeMillis();
            }
        }, TIMEOUT_CHECK_INTERNAL, TIMEOUT_CHECK_INTERNAL, TimeUnit.MILLISECONDS);
    }

    /**
     * Destroy.
     */
    @Override
    public void destroy() {
        timerExecutor.shutdown();
        messageExecutor.shutdown();
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) {
        synchronized (lock) {
            if (ctx.channel().isWritable()) {
                lock.notifyAll();
            }
        }

        ctx.fireChannelWritabilityChanged();
    }

    /**
     * Send async request with response object.
     *
     * @param channel the channel
     * @param msg     the msg
     * @return the object
     * @throws TimeoutException the timeout exception
     */
    protected Object sendAsyncRequestWithResponse(Channel channel, Object msg) throws TimeoutException {
        return sendAsyncRequestWithResponse(null, channel, msg, NettyClientConfig.getRpcRequestTimeout());
    }

    /**
     * Send async request with response object.
     *
     * @param address the address
     * @param channel the channel
     * @param msg     the msg
     * @param timeout the timeout
     * @return the object
     * @throws TimeoutException the timeout exception
     */
    protected Object sendAsyncRequestWithResponse(String address, Channel channel, Object msg, long timeout) throws
        TimeoutException {
        if (timeout <= 0) {
            throw new FrameworkException("timeout should more than 0ms");
        }
        return sendAsyncRequest(address, channel, msg, timeout);
    }

    /**
     * Send async request without response object.
     *
     * @param channel the channel
     * @param msg     the msg
     * @return the object
     * @throws TimeoutException the timeout exception
     */
    protected Object sendAsyncRequestWithoutResponse(Channel channel, Object msg) throws
        TimeoutException {
        return sendAsyncRequest(null, channel, msg, 0);
    }

    private Object sendAsyncRequest(String address, Channel channel, Object msg, long timeout)
        throws TimeoutException {
        if (channel == null) {
            LOGGER.warn("sendAsyncRequestWithResponse nothing, caused by null channel.");
            return null;
        }
        final RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(getNextMessageId());
        rpcMessage.setMessageType(ProtocolConstants.MSGTYPE_RESQUEST_ONEWAY);
        rpcMessage.setCodec(ProtocolConstants.CONFIGURED_CODEC);
        rpcMessage.setCompressor(ProtocolConstants.CONFIGURED_COMPRESSOR);
        rpcMessage.setBody(msg);

        final MessageFuture messageFuture = new MessageFuture();
        messageFuture.setRequestMessage(rpcMessage);
        messageFuture.setTimeout(timeout);
        futures.put(rpcMessage.getId(), messageFuture);

        if (address != null) {
            /*
            The batch send.
            Object From big to small: RpcMessage -> MergedWarpMessage -> AbstractMessage
            @see AbstractRpcRemotingClient.MergedSendRunnable
            */
            if (NettyClientConfig.isEnableClientBatchSendRequest()) {
                ConcurrentHashMap<String, BlockingQueue<RpcMessage>> map = basketMap;
                BlockingQueue<RpcMessage> basket = map.get(address);
                if (basket == null) {
                    map.putIfAbsent(address, new LinkedBlockingQueue<>());
                    basket = map.get(address);
                }
                basket.offer(rpcMessage);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("offer message: {}", rpcMessage.getBody());
                }
                if (!isSending) {
                    synchronized (mergeLock) {
                        mergeLock.notifyAll();
                    }
                }
            } else {
                // the single send.
                sendSingleRequest(channel, msg, rpcMessage);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("send this msg[{}] by single send.", msg);
                }
            }
        } else {
            sendSingleRequest(channel, msg, rpcMessage);
        }
        if (timeout > 0) {
            try {
                return messageFuture.get(timeout, TimeUnit.MILLISECONDS);
            } catch (Exception exx) {
                LOGGER.error("wait response error:{},ip:{},request:{}", exx.getMessage(), address, msg);
                if (exx instanceof TimeoutException) {
                    throw (TimeoutException) exx;
                } else {
                    throw new RuntimeException(exx);
                }
            }
        } else {
            return null;
        }
    }

    private void sendSingleRequest(Channel channel, Object msg, RpcMessage rpcMessage) {
        ChannelFuture future;
        channelWritableCheck(channel, msg);
        future = channel.writeAndFlush(rpcMessage);
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (!future.isSuccess()) {
                    MessageFuture messageFuture = futures.remove(rpcMessage.getId());
                    if (messageFuture != null) {
                        messageFuture.setResultMessage(future.cause());
                    }
                    destroyChannel(future.channel());
                }
            }
        });
    }

    /**
     * Send request.
     *
     * @param channel the channel
     * @param msg     the msg
     */
    protected void sendRequest(Channel channel, Object msg) {
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setMessageType(msg instanceof HeartbeatMessage ?
            ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST
            : ProtocolConstants.MSGTYPE_RESQUEST);
        rpcMessage.setCodec(ProtocolConstants.CONFIGURED_CODEC);
        rpcMessage.setCompressor(ProtocolConstants.CONFIGURED_COMPRESSOR);
        rpcMessage.setBody(msg);
        rpcMessage.setId(getNextMessageId());
        if (msg instanceof MergeMessage) {
            mergeMsgMap.put(rpcMessage.getId(), (MergeMessage) msg);
        }
        channelWritableCheck(channel, msg);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("write message:" + rpcMessage.getBody() + ", channel:" + channel + ",active?"
                + channel.isActive() + ",writable?" + channel.isWritable() + ",isopen?" + channel.isOpen());
        }
        channel.writeAndFlush(rpcMessage);
    }

    /**
     * Send response.
     *
     * @param request the msg id
     * @param channel the channel
     * @param msg     the msg
     */
    protected void sendResponse(RpcMessage request, Channel channel, Object msg) {
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setMessageType(msg instanceof HeartbeatMessage ?
            ProtocolConstants.MSGTYPE_HEARTBEAT_RESPONSE :
            ProtocolConstants.MSGTYPE_RESPONSE);
        rpcMessage.setCodec(request.getCodec()); // same with request
        rpcMessage.setCompressor(request.getCompressor());
        rpcMessage.setBody(msg);
        rpcMessage.setId(request.getId());
        channelWritableCheck(channel, msg);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("send response:" + rpcMessage.getBody() + ",channel:" + channel);
        }
        channel.writeAndFlush(rpcMessage);
    }

    private void channelWritableCheck(Channel channel, Object msg) {
        int tryTimes = 0;
        synchronized (lock) {
            while (!channel.isWritable()) {
                try {
                    tryTimes++;
                    if (tryTimes > NettyClientConfig.getMaxNotWriteableRetry()) {
                        destroyChannel(channel);
                        throw new FrameworkException("msg:" + ((msg == null) ? "null" : msg.toString()),
                            FrameworkErrorCode.ChannelIsNotWritable);
                    }
                    lock.wait(NOT_WRITEABLE_CHECK_MILLS);
                } catch (InterruptedException exx) {
                    LOGGER.error(exx.getMessage());
                }
            }
        }
    }

    /**
     * For testing. When the thread pool is full, you can change this variable and share the stack
     */
    boolean allowDumpStack = false;

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof RpcMessage) {
            final RpcMessage rpcMessage = (RpcMessage) msg;
            if (rpcMessage.getMessageType() == ProtocolConstants.MSGTYPE_RESQUEST
                || rpcMessage.getMessageType() == ProtocolConstants.MSGTYPE_RESQUEST_ONEWAY) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format("%s msgId:%s, body:%s", this, rpcMessage.getId(), rpcMessage.getBody()));
                }
                try {
                    AbstractRpcRemoting.this.messageExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                dispatch(rpcMessage, ctx);
                            } catch (Throwable th) {
                                LOGGER.error(FrameworkErrorCode.NetDispatch.getErrCode(), th.getMessage(), th);
                            }
                        }
                    });
                } catch (RejectedExecutionException e) {
                    LOGGER.error(FrameworkErrorCode.ThreadPoolFull.getErrCode(),
                        "thread pool is full, current max pool size is " + messageExecutor.getActiveCount());
                    if (allowDumpStack) {
                        String name = ManagementFactory.getRuntimeMXBean().getName();
                        String pid = name.split("@")[0];
                        int idx = new Random().nextInt(100);
                        try {
                            Runtime.getRuntime().exec("jstack " + pid + " >d:/" + idx + ".log");
                        } catch (IOException exx) {
                            LOGGER.error(exx.getMessage());
                        }
                        allowDumpStack = false;
                    }
                }
            } else {
                MessageFuture messageFuture = futures.remove(rpcMessage.getId());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String
                        .format("%s msgId:%s, future :%s, body:%s", this, rpcMessage.getId(), messageFuture,
                            rpcMessage.getBody()));
                }
                if (messageFuture != null) {
                    messageFuture.setResultMessage(rpcMessage.getBody());
                } else {
                    try {
                        AbstractRpcRemoting.this.messageExecutor.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    dispatch(rpcMessage, ctx);
                                } catch (Throwable th) {
                                    LOGGER.error(FrameworkErrorCode.NetDispatch.getErrCode(), th.getMessage(), th);
                                }
                            }
                        });
                    } catch (RejectedExecutionException e) {
                        LOGGER.error(FrameworkErrorCode.ThreadPoolFull.getErrCode(),
                            "thread pool is full, current max pool size is " + messageExecutor.getActiveCount());
                    }
                }
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error(FrameworkErrorCode.ExceptionCaught.getErrCode(),
            ctx.channel() + " connect exception. " + cause.getMessage(),
            cause);
        try {
            destroyChannel(ctx.channel());
        } catch (Exception e) {
            LOGGER.error("failed to close channel {}: {}", ctx.channel(), e.getMessage(), e);
        }
    }

    /**
     * Dispatch.
     *
     * @param request the request
     * @param ctx     the ctx
     */
    public abstract void dispatch(RpcMessage request, ChannelHandlerContext ctx);

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise future) throws Exception {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(ctx + " will closed");
        }
        super.close(ctx, future);
    }

    /**
     * Add channel pipeline last.
     *
     * @param channel  the channel
     * @param handlers the handlers
     */
    protected void addChannelPipelineLast(Channel channel, ChannelHandler... handlers) {
        if (null != channel && null != handlers) {
            channel.pipeline().addLast(handlers);
        }
    }

    /**
     * Sets channel handlers.
     *
     * @param handlers the handlers
     */
    protected void setChannelHandlers(ChannelHandler... handlers) {
        this.channelHandlers = handlers;
    }

    /**
     * Gets group.
     *
     * @return the group
     */
    public String getGroup() {
        return group;
    }

    /**
     * Sets group.
     *
     * @param group the group
     */
    public void setGroup(String group) {
        this.group = group;
    }

    /**
     * Destroy channel.
     *
     * @param channel the channel
     */
    public void destroyChannel(Channel channel) {
        destroyChannel(getAddressFromChannel(channel), channel);
    }

    /**
     * Destroy channel.
     *
     * @param serverAddress the server address
     * @param channel       the channel
     */
    public abstract void destroyChannel(String serverAddress, Channel channel);

    /**
     * Gets address from context.
     *
     * @param ctx the ctx
     * @return the address from context
     */
    protected String getAddressFromContext(ChannelHandlerContext ctx) {
        return getAddressFromChannel(ctx.channel());
    }

    /**
     * Gets address from channel.
     *
     * @param channel the channel
     * @return the address from channel
     */
    protected String getAddressFromChannel(Channel channel) {
        SocketAddress socketAddress = channel.remoteAddress();
        String address = socketAddress.toString();
        if (socketAddress.toString().indexOf(NettyClientConfig.getSocketAddressStartChar()) == 0) {
            address = socketAddress.toString().substring(NettyClientConfig.getSocketAddressStartChar().length());
        }
        return address;
    }
}
