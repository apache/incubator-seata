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
public abstract class AbstractGrpcTmHandler {
    public GlobalBeginResponse handle(GlobalBeginRequest request) {
        return null;
    }

    public GlobalRollbackResponse handle(GlobalRollbackRequest request) {
        return null;
    }

    public GlobalCommitResponse handle(GlobalCommitRequest request) {
        return null;
    }

    public GlobalReportResponse handle(GlobalReportRequest request) {
        return null;
    }

    public GlobalStatusResponse handle(GlobalStatusRequest request) {
        return null;
    }
}
