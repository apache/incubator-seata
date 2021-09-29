package io.seata.core.rpc.grpc;

import io.seata.core.model.grpc.SeataGrpc.GlobalBeginRequest;
import io.seata.core.model.grpc.SeataGrpc.GlobalBeginResponse;
import io.seata.core.model.grpc.SeataGrpc.GlobalCommitRequest;
import io.seata.core.model.grpc.SeataGrpc.GlobalCommitResponse;
import io.seata.core.model.grpc.SeataGrpc.GlobalReportRequest;
import io.seata.core.model.grpc.SeataGrpc.GlobalReportResponse;
import io.seata.core.model.grpc.SeataGrpc.GlobalRollbackRequest;
import io.seata.core.model.grpc.SeataGrpc.GlobalRollbackResponse;
import io.seata.core.model.grpc.SeataGrpc.GlobalStatusRequest;
import io.seata.core.model.grpc.SeataGrpc.GlobalStatusResponse;

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
