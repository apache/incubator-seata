package io.seata.core.rpc.grpc;


import io.grpc.stub.StreamObserver;
import io.seata.core.model.grpc.BranchTransactionServiceGrpc;
import io.seata.core.model.grpc.SeataGrpc.BranchCommitRequest;
import io.seata.core.model.grpc.SeataGrpc.BranchCommitResponse;
import io.seata.core.model.grpc.SeataGrpc.BranchRollbackRequest;
import io.seata.core.model.grpc.SeataGrpc.BranchRollbackResponse;
import io.seata.core.model.grpc.SeataGrpc.BranchSession.BranchType;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xilou31
 **/
public class BranchTransactionService extends BranchTransactionServiceGrpc.BranchTransactionServiceImplBase {
    private static ConcurrentHashMap<BranchType, AbstractGrpcTcHandler> handlerMap = new ConcurrentHashMap<>();

    @Override
    public void branchCommit(BranchCommitRequest request, StreamObserver<BranchCommitResponse> responseObserver) {
        AbstractGrpcTcHandler handler = handlerMap.get(request.getBranchType());
        BranchCommitResponse response = handler.handle(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void branchRollback(BranchRollbackRequest request, StreamObserver<BranchRollbackResponse> responseObserver) {
        AbstractGrpcTcHandler handler = handlerMap.get(request.getBranchType());
        BranchRollbackResponse response = handler.handle(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
