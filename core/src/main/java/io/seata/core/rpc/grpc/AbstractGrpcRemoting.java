package io.seata.core.rpc.grpc;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.exception.FrameworkException;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.thread.PositiveAtomicCounter;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.MessageFuture;
import io.seata.core.protocol.MessageType;
import io.seata.core.protocol.MessageTypeAware;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.rpc.Disposable;
import io.seata.core.rpc.MessageIdGeneratorHelper;
import io.seata.core.rpc.SeataChannel;
import io.seata.core.rpc.SeataChannelUtil;
import io.seata.core.rpc.grpc.generated.GrpcRemoting;
import io.seata.core.rpc.hook.RpcHook;
import io.seata.core.rpc.processor.Pair;
import io.seata.core.rpc.processor.RemotingProcessor;
import io.seata.core.rpc.processor.RpcMessageHandleContext;
import io.seata.serializer.protobuf.convertor.PbConvertor;
import io.seata.serializer.protobuf.manager.ProtobufConvertManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * @author goodboycoder
 */
public abstract class AbstractGrpcRemoting implements Disposable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGrpcRemoting.class);

    /**
     * The Message executor.
     */
    protected final ThreadPoolExecutor messageExecutor;
    /**
     * This container holds all processors.
     * processor type {@link MessageType}
     */
    protected final HashMap<Integer/*MessageType*/, Pair<RemotingProcessor, ExecutorService>> processorTable = new HashMap<>(32);

    /**
     * For testing. When the thread pool is full, you can change this variable and share the stack
     */
    boolean allowDumpStack = false;

    /**
     * Id generator of this remoting
     */
    protected final PositiveAtomicCounter idGenerator = MessageIdGeneratorHelper.getIdGenerator();

    /**
     * Obtain the return result through MessageFuture blocking.
     *
     */
    protected final ConcurrentHashMap<Integer, MessageFuture> futures = new ConcurrentHashMap<>();

    protected final ScheduledExecutorService timerExecutor = new ScheduledThreadPoolExecutor(1,
            new NamedThreadFactory("timeoutChecker", 1, true));

    protected final List<RpcHook> rpcHooks = EnhancedServiceLoader.loadAll(RpcHook.class);


    private static final long NOT_WRITEABLE_CHECK_MILLS = 10L;

    /**
     * The Now mills.
     */
    protected volatile long nowMills = 0;
    private static final int TIMEOUT_CHECK_INTERVAL = 3000;
    public AbstractGrpcRemoting(ThreadPoolExecutor messageExecutor) {
        this.messageExecutor = messageExecutor;
    }

    public void init() {
        timerExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<Integer, MessageFuture> entry : futures.entrySet()) {
                    MessageFuture future = entry.getValue();
                    if (future.isTimeout()) {
                        futures.remove(entry.getKey());
                        RpcMessage rpcMessage = future.getRequestMessage();
                        future.setResultMessage(new TimeoutException(String
                                .format("msgId: %s ,msgType: %s ,msg: %s ,request timeout", rpcMessage.getId(), String.valueOf(rpcMessage.getMessageType()), rpcMessage.getBody().toString())));
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("timeout clear future: {}", entry.getValue().getRequestMessage().getBody());
                        }
                    }
                }

                nowMills = System.currentTimeMillis();
            }
        }, TIMEOUT_CHECK_INTERVAL, TIMEOUT_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy() {
        timerExecutor.shutdown();
        messageExecutor.shutdown();
    }

    public int getNextMessageId() {
        return idGenerator.incrementAndGet();
    }

    public ConcurrentHashMap<Integer, MessageFuture> getFutures() {
        return futures;
    }

    public void processMessage(RpcMessageHandleContext ctx, Object message) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("%s msgId:%s, body:%s", this, ctx.getMessageMeta().getMessageId(), message));
        }
        if (message instanceof AbstractMessage) {
            MessageTypeAware messageTypeAware = (MessageTypeAware) message;
            final Pair<RemotingProcessor, ExecutorService> pair = this.processorTable.get((int) messageTypeAware.getTypeCode());
            if (pair != null) {
                if (pair.getSecond() != null) {
                    try {
                        pair.getSecond().execute(() -> {
                            try {
                                pair.getFirst().process(ctx, message);
                            } catch (Throwable th) {
                                LOGGER.error(FrameworkErrorCode.NetDispatch.getErrCode(), th.getMessage(), th);
                            } finally {
                                MDC.clear();
                            }
                        });
                    } catch (RejectedExecutionException e) {
                        LOGGER.error(FrameworkErrorCode.ThreadPoolFull.getErrCode(),
                                "thread pool is full, current max pool size is " + messageExecutor.getActiveCount());
                        if (allowDumpStack) {
                            String name = ManagementFactory.getRuntimeMXBean().getName();
                            String pid = name.split("@")[0];
                            long idx = System.currentTimeMillis();
                            try {
                                String jstackFile = idx + ".log";
                                LOGGER.info("jstack command will dump to " + jstackFile);
                                Runtime.getRuntime().exec(String.format("jstack %s > %s", pid, jstackFile));
                            } catch (IOException exx) {
                                LOGGER.error(exx.getMessage());
                            }
                            allowDumpStack = false;
                        }
                    }
                } else {
                    try {
                        pair.getFirst().process(ctx, message);
                    } catch (Throwable th) {
                        LOGGER.error(FrameworkErrorCode.NetDispatch.getErrCode(), th.getMessage(), th);
                    }
                }
            } else {
                LOGGER.error("This message type [{}] has no processor.", messageTypeAware.getTypeCode());
            }
        } else {
            LOGGER.error("This rpcMessage body[{}] is not AbstractMessage type.", message);
        }
    }

    /**
     * rpc sync request
     * Obtain the return result through MessageFuture blocking.
     *
     * @param channel       netty channel
     * @param rpcMessage    rpc message
     * @param timeoutMillis rpc communication timeout
     * @return response message
     * @throws TimeoutException
     */
    protected Object sendSync(SeataChannel channel, RpcMessage rpcMessage, long timeoutMillis) throws TimeoutException {
        if (timeoutMillis <= 0) {
            throw new FrameworkException("timeout should more than 0ms");
        }
        if (channel == null) {
            LOGGER.warn("sendSync nothing, caused by null channel.");
            return null;
        }

        MessageFuture messageFuture = new MessageFuture();
        messageFuture.setRequestMessage(rpcMessage);
        messageFuture.setTimeout(timeoutMillis);
        futures.put(rpcMessage.getId(), messageFuture);

        String remoteAddr = SeataChannelUtil.getAddressFromChannel(channel);

        doBeforeRpcHooks(remoteAddr, rpcMessage);
        try {
            channel.sendMsg(convertToProtoMessage(rpcMessage));
            Object result = messageFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
            doAfterRpcHooks(remoteAddr, rpcMessage, result);
            return result;
        } catch (Exception exx) {
            LOGGER.error("wait response error:{},ip:{},request:{}", exx.getMessage(), channel.remoteAddress(),
                    rpcMessage.getBody());
            if (exx instanceof TimeoutException) {
                throw (TimeoutException) exx;
            } else {
                throw new RuntimeException(exx);
            }
        }
    }

    /**
     * rpc async request.
     *
     * @param channel    netty channel
     * @param rpcMessage rpc message
     */
    protected void sendAsync(SeataChannel channel, RpcMessage rpcMessage) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("write message:" + rpcMessage.getBody() + ", channel:" + channel + ",active?" + channel.isActive());
        }
        doBeforeRpcHooks(SeataChannelUtil.getAddressFromChannel(channel), rpcMessage);
        try {
            channel.sendMsg(convertToProtoMessage(rpcMessage));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void doBeforeRpcHooks(String remoteAddress, RpcMessage request) {
        for (RpcHook rpcHook: rpcHooks) {
            rpcHook.doBeforeRequest(remoteAddress, request);
        }
    }

    protected void doAfterRpcHooks(String remoteAddress, RpcMessage request, Object response) {
        for (RpcHook rpcHook: rpcHooks) {
            rpcHook.doAfterResponse(remoteAddress, request, response);
        }
    }

    private Message convertToProtoMessage(RpcMessage rpcMessage) {
        Object body = rpcMessage.getBody();
        final PbConvertor pbConvertor = ProtobufConvertManager.getInstance().fetchConvertor(body.getClass().getName());
        Message protoMessage = (Message) pbConvertor.convert2Proto(body);
        return GrpcRemoting.BiStreamMessage.newBuilder()
                .setID(rpcMessage.getId())
                .setMessageType(BiStreamMessageTypeHelper.getBiStreamMessageTypeByClass(protoMessage.getClass()))
                .setMessage(Any.pack(protoMessage))
                .build();
    }
}
