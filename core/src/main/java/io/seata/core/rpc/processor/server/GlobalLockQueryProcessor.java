package io.seata.core.rpc.processor.server;

import io.seata.core.protocol.transaction.GlobalLockQueryRequest;
import io.seata.core.protocol.transaction.GlobalLockQueryResponse;
import io.seata.core.rpc.RpcContext;
import io.seata.core.rpc.SeataChannelServerManager;
import io.seata.core.rpc.TransactionMessageHandler;
import io.seata.core.rpc.processor.RpcMessageHandlerContext;

/**
 * @author goodboycoder
 */
public class GlobalLockQueryProcessor extends BaseServerOnRequestProcessor<GlobalLockQueryRequest, GlobalLockQueryResponse> {

    public GlobalLockQueryProcessor(TransactionMessageHandler transactionMessageHandler) {
        super(transactionMessageHandler);
    }

    @Override
    protected GlobalLockQueryResponse onRequestMessage(RpcMessageHandlerContext ctx, GlobalLockQueryRequest request) {
        RpcContext rpcContext = SeataChannelServerManager.getContextFromIdentified(ctx.channel());
        return (GlobalLockQueryResponse) transactionMessageHandler.onRequest(request, rpcContext);
    }
}
