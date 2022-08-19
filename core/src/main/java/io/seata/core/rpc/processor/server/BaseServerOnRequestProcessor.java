package io.seata.core.rpc.processor.server;

import io.seata.core.protocol.AbstractMessage;
import io.seata.core.rpc.SeataChannelServerManager;
import io.seata.core.rpc.TransactionMessageHandler;
import io.seata.core.rpc.processor.MessageReply;
import io.seata.core.rpc.processor.RemotingProcessor;
import io.seata.core.rpc.processor.RpcMessageHandleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author goodboycoder
 */
public abstract class BaseServerOnRequestProcessor<T, S> implements RemotingProcessor<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BranchRegisterProcessor.class);

    protected final TransactionMessageHandler transactionMessageHandler;

    public BaseServerOnRequestProcessor(TransactionMessageHandler transactionMessageHandler) {
        this.transactionMessageHandler = transactionMessageHandler;
    }

    @Override
    public void process(RpcMessageHandleContext ctx, T request) throws Exception {
        if (SeataChannelServerManager.isRegistered(ctx.channel()) && request instanceof AbstractMessage) {
            S response = onRequestMessage(ctx, request);
            MessageReply messageReply = ctx.getMessageReply();
            if (null != messageReply) {
                messageReply.reply(response);
            }
        } else {
            try {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("closeChannelHandlerContext channel:" + ctx.channel());
                }
                ctx.disconnect();
                ctx.close();
            } catch (Exception exx) {
                LOGGER.error(exx.getMessage());
            }
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("close a unhandled connection! [%s]", ctx.channel().toString()));
            }
        }
    }

    protected abstract S onRequestMessage(RpcMessageHandleContext ctx, T request);
}
