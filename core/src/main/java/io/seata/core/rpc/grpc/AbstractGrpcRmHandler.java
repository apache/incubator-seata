package io.seata.core.rpc.grpc;

import io.seata.core.protocol.grpc.SeataGrpc.BranchRegisterRequest;
import io.seata.core.protocol.grpc.SeataGrpc.BranchRegisterResponse;
import io.seata.core.protocol.grpc.SeataGrpc.BranchReportRequest;
import io.seata.core.protocol.grpc.SeataGrpc.BranchReportResponse;
import io.seata.core.protocol.grpc.SeataGrpc.GlobalLockQueryRequest;
import io.seata.core.protocol.grpc.SeataGrpc.GlobalLockQueryResponse;

/**
 * @author xilou31
 **/
public abstract class AbstractGrpcRmHandler {
    public BranchRegisterResponse handle(BranchRegisterRequest request) {
        return null;
    }

    public GlobalLockQueryResponse handle(GlobalLockQueryRequest request) {
        return null;
    }

    public BranchReportResponse handle(BranchReportRequest request) {
        return null;
    }
}
