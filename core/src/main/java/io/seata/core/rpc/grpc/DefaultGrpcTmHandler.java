package io.seata.core.rpc.grpc;

import io.seata.core.protocol.grpc.SeataGrpc.GlobalBeginRequest;
import io.seata.core.protocol.grpc.SeataGrpc.GlobalBeginResponse;
import io.seata.core.protocol.grpc.SeataGrpc.GlobalCommitRequest;
import io.seata.core.protocol.grpc.SeataGrpc.GlobalCommitResponse;
import io.seata.core.protocol.grpc.SeataGrpc.GlobalReportRequest;
import io.seata.core.protocol.grpc.SeataGrpc.GlobalReportResponse;
import io.seata.core.protocol.grpc.SeataGrpc.GlobalRollbackRequest;
import io.seata.core.protocol.grpc.SeataGrpc.GlobalRollbackResponse;
import io.seata.core.protocol.grpc.SeataGrpc.GlobalStatusRequest;
import io.seata.core.protocol.grpc.SeataGrpc.GlobalStatusResponse;

/**
 * @author xilou31
 **/
public class DefaultGrpcTmHandler extends AbstractGrpcTmHandler {
    @Override
    public GlobalBeginResponse handle(GlobalBeginRequest request) {
        return super.handle(request);
    }

    @Override
    public GlobalCommitResponse handle(GlobalCommitRequest request) {
        return super.handle(request);
    }

    @Override
    public GlobalReportResponse handle(GlobalReportRequest request) {
        return super.handle(request);
    }

    @Override
    public GlobalRollbackResponse handle(GlobalRollbackRequest request) {
        return super.handle(request);
    }

    @Override
    public GlobalStatusResponse handle(GlobalStatusRequest request) {
        return super.handle(request);
    }
}
