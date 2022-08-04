package io.seata.core.rpc.processor.server;

import io.seata.core.protocol.transaction.GlobalCommitRequest;
import io.seata.core.protocol.transaction.GlobalCommitResponse;
import io.seata.core.rpc.RpcContext;
import io.seata.core.rpc.SeataChannelServerManager;
import io.seata.core.rpc.TransactionMessageHandler;
import io.seata.core.rpc.processor.RpcMessageHandlerContext;

/**
 * @author goodboycoder
 */
public class GlobalCommitProcessor extends BaseServerOnRequestProcessor<GlobalCommitRequest, GlobalCommitResponse> {

    public GlobalCommitProcessor(TransactionMessageHandler transactionMessageHandler) {
        super(transactionMessageHandler);
    }

    @Override
    protected GlobalCommitResponse onRequestMessage(RpcMessageHandlerContext ctx, GlobalCommitRequest request) {
        RpcContext rpcContext = SeataChannelServerManager.getContextFromIdentified(ctx.channel());
        return (GlobalCommitResponse) transactionMessageHandler.onRequest(request, rpcContext);
    }
}
