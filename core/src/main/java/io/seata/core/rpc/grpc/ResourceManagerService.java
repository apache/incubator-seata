package io.seata.core.rpc.grpc;

import io.grpc.stub.StreamObserver;
import io.seata.core.protocol.grpc.ResourceManagerServiceGrpc;
import io.seata.core.protocol.grpc.SeataGrpc.BranchRegisterRequest;
import io.seata.core.protocol.grpc.SeataGrpc.BranchRegisterResponse;
import io.seata.core.protocol.grpc.SeataGrpc.BranchReportRequest;
import io.seata.core.protocol.grpc.SeataGrpc.BranchReportResponse;
import io.seata.core.protocol.grpc.SeataGrpc.BranchSession.BranchType;
import io.seata.core.protocol.grpc.SeataGrpc.GlobalLockQueryRequest;
import io.seata.core.protocol.grpc.SeataGrpc.GlobalLockQueryResponse;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xilou31
 **/
public class ResourceManagerService extends ResourceManagerServiceGrpc.ResourceManagerServiceImplBase {
    private static ConcurrentHashMap<BranchType, AbstractGrpcRmHandler> handlerMap = new ConcurrentHashMap<>();

    @Override
    public void branchRegister(BranchRegisterRequest request, StreamObserver<BranchRegisterResponse> responseObserver) {
        AbstractGrpcRmHandler handler = handlerMap.get(request.getBranchType());
        BranchRegisterResponse response = handler.handle(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void branchReport(BranchReportRequest request, StreamObserver<BranchReportResponse> responseObserver) {
        AbstractGrpcRmHandler handler = handlerMap.get(request.getBranchType());
        BranchReportResponse response = handler.handle(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void lockQuery(GlobalLockQueryRequest request, StreamObserver<GlobalLockQueryResponse> responseObserver) {
        AbstractGrpcRmHandler handler = handlerMap.get(request.getBranchType());
        GlobalLockQueryResponse response = handler.handle(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
