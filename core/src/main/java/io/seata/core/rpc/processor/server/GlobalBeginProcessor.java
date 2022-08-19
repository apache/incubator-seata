package io.seata.core.rpc.processor.server;

import io.seata.core.protocol.transaction.GlobalBeginRequest;
import io.seata.core.protocol.transaction.GlobalBeginResponse;
import io.seata.core.rpc.RpcContext;
import io.seata.core.rpc.SeataChannelServerManager;
import io.seata.core.rpc.TransactionMessageHandler;
import io.seata.core.rpc.processor.RpcMessageHandleContext;

/**
 * @author goodboycoder
 */
public class GlobalBeginProcessor extends BaseServerOnRequestProcessor<GlobalBeginRequest, GlobalBeginResponse> {

    public GlobalBeginProcessor(TransactionMessageHandler transactionMessageHandler) {
        super(transactionMessageHandler);
    }

    @Override
    protected GlobalBeginResponse onRequestMessage(RpcMessageHandleContext ctx, GlobalBeginRequest request) {
        RpcContext rpcContext = SeataChannelServerManager.getContextFromIdentified(ctx.channel());
        return (GlobalBeginResponse) transactionMessageHandler.onRequest(request, rpcContext);
    }
}
