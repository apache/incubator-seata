package io.seata.core.rpc.processor.server;

import io.seata.core.protocol.transaction.GlobalReportRequest;
import io.seata.core.protocol.transaction.GlobalReportResponse;
import io.seata.core.rpc.RpcContext;
import io.seata.core.rpc.SeataChannelServerManager;
import io.seata.core.rpc.TransactionMessageHandler;
import io.seata.core.rpc.processor.RpcMessageHandleContext;

/**
 * @author goodboycoder
 */
public class GlobalReportProcessor extends BaseServerOnRequestProcessor<GlobalReportRequest, GlobalReportResponse> {
    public GlobalReportProcessor(TransactionMessageHandler transactionMessageHandler) {
        super(transactionMessageHandler);
    }

    @Override
    protected GlobalReportResponse onRequestMessage(RpcMessageHandleContext ctx, GlobalReportRequest request) {
        RpcContext rpcContext = SeataChannelServerManager.getContextFromIdentified(ctx.channel());
        return (GlobalReportResponse) transactionMessageHandler.onRequest(request, rpcContext);
    }
}
