package io.seata.core.rpc.processor.server;

import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.transaction.BranchRegisterRequest;
import io.seata.core.protocol.transaction.BranchRegisterResponse;
import io.seata.core.rpc.SeataChannelServerManager;
import io.seata.core.rpc.TransactionMessageHandler;
import io.seata.core.rpc.processor.RemotingProcessor;
import io.seata.core.rpc.processor.RpcMessageHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author goodboycoder
 */
public abstract class BaseServerOnRequestProcessor<T, S> implements RemotingProcessor<T, S> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BranchRegisterProcessor.class);

    protected final TransactionMessageHandler transactionMessageHandler;

    public BaseServerOnRequestProcessor(TransactionMessageHandler transactionMessageHandler) {
        this.transactionMessageHandler = transactionMessageHandler;
    }

    @Override
    public S process(RpcMessageHandlerContext ctx, T request) throws Exception {
        if (SeataChannelServerManager.isRegistered(ctx.channel()) && request instanceof AbstractMessage) {
            return onRequestMessage(ctx, request);
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
        return null;
    }

    protected abstract S onRequestMessage(RpcMessageHandlerContext ctx, T request);
}
