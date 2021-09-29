package io.seata.core.rpc.grpc;

import io.seata.core.model.grpc.SeataGrpc.BranchCommitRequest;
import io.seata.core.model.grpc.SeataGrpc.BranchCommitResponse;
import io.seata.core.model.grpc.SeataGrpc.BranchRollbackRequest;
import io.seata.core.model.grpc.SeataGrpc.BranchRollbackResponse;

/**
 * @author xilou31
 **/
public abstract class AbstractGrpcTcHandler {
    public BranchCommitResponse handle(BranchCommitRequest request) {
        return null;
    }

    public BranchRollbackResponse handle(BranchRollbackRequest request) {
        return null;
    }
}
