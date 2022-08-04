package io.seata.core.rpc.processor.server;

import io.seata.core.protocol.transaction.BranchRegisterRequest;
import io.seata.core.protocol.transaction.BranchRegisterResponse;
import io.seata.core.rpc.RpcContext;
import io.seata.core.rpc.SeataChannelServerManager;
import io.seata.core.rpc.TransactionMessageHandler;
import io.seata.core.rpc.processor.RpcMessageHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author goodboycoder
 */
public class BranchRegisterProcessor extends BaseServerOnRequestProcessor<BranchRegisterRequest, BranchRegisterResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BranchRegisterProcessor.class);

    public BranchRegisterProcessor(TransactionMessageHandler transactionMessageHandler) {
        super(transactionMessageHandler);
    }

    @Override
    protected BranchRegisterResponse onRequestMessage(RpcMessageHandlerContext ctx, BranchRegisterRequest branchRegisterRequest) {
        RpcContext rpcContext = SeataChannelServerManager.getContextFromIdentified(ctx.channel());
        return (BranchRegisterResponse) transactionMessageHandler.onRequest(branchRegisterRequest, rpcContext);
    }
}
