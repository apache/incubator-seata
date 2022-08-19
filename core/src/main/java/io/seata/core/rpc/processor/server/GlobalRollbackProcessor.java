package io.seata.core.rpc.processor.server;

import io.seata.core.protocol.transaction.GlobalRollbackRequest;
import io.seata.core.protocol.transaction.GlobalRollbackResponse;
import io.seata.core.rpc.RpcContext;
import io.seata.core.rpc.SeataChannelServerManager;
import io.seata.core.rpc.TransactionMessageHandler;
import io.seata.core.rpc.processor.RpcMessageHandleContext;

/**
 * @author goodboycoder
 */
public class GlobalRollbackProcessor extends BaseServerOnRequestProcessor<GlobalRollbackRequest, GlobalRollbackResponse> {
    public GlobalRollbackProcessor(TransactionMessageHandler transactionMessageHandler) {
        super(transactionMessageHandler);
    }

    @Override
    protected GlobalRollbackResponse onRequestMessage(RpcMessageHandleContext ctx, GlobalRollbackRequest request) {
        RpcContext rpcContext = SeataChannelServerManager.getContextFromIdentified(ctx.channel());
        return (GlobalRollbackResponse) transactionMessageHandler.onRequest(request, rpcContext);
    }
}
