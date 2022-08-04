package io.seata.core.rpc.processor.server;

import io.seata.core.protocol.transaction.GlobalStatusRequest;
import io.seata.core.protocol.transaction.GlobalStatusResponse;
import io.seata.core.rpc.RpcContext;
import io.seata.core.rpc.SeataChannelServerManager;
import io.seata.core.rpc.TransactionMessageHandler;
import io.seata.core.rpc.processor.RpcMessageHandlerContext;

/**
 * @author goodboycoder
 */
public class GlobalStatusProcessor extends BaseServerOnRequestProcessor<GlobalStatusRequest, GlobalStatusResponse> {
    public GlobalStatusProcessor(TransactionMessageHandler transactionMessageHandler) {
        super(transactionMessageHandler);
    }

    @Override
    protected GlobalStatusResponse onRequestMessage(RpcMessageHandlerContext ctx, GlobalStatusRequest request) {
        RpcContext rpcContext = SeataChannelServerManager.getContextFromIdentified(ctx.channel());
        return (GlobalStatusResponse) transactionMessageHandler.onRequest(request, rpcContext);
    }
}
