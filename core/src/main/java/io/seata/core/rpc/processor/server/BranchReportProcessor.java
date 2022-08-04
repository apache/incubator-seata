package io.seata.core.rpc.processor.server;

import io.seata.core.protocol.transaction.BranchReportRequest;
import io.seata.core.protocol.transaction.BranchReportResponse;
import io.seata.core.rpc.RpcContext;
import io.seata.core.rpc.SeataChannelServerManager;
import io.seata.core.rpc.TransactionMessageHandler;
import io.seata.core.rpc.processor.RpcMessageHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author goodboycoder
 */
public class BranchReportProcessor extends BaseServerOnRequestProcessor<BranchReportRequest, BranchReportResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BranchReportProcessor.class);

    public BranchReportProcessor(TransactionMessageHandler transactionMessageHandler) {
        super(transactionMessageHandler);
    }

    @Override
    protected BranchReportResponse onRequestMessage(RpcMessageHandlerContext ctx, BranchReportRequest request) {
        RpcContext rpcContext = SeataChannelServerManager.getContextFromIdentified(ctx.channel());
        return (BranchReportResponse) transactionMessageHandler.onRequest(request, rpcContext);
    }
}
