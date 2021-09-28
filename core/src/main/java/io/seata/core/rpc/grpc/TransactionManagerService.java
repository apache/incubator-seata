package io.seata.core.rpc.grpc;

import io.grpc.stub.StreamObserver;
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
import io.seata.core.protocol.grpc.TransactionManagerServiceGrpc;

/**
 * @author xilou31
 **/

public class TransactionManagerService extends TransactionManagerServiceGrpc.TransactionManagerServiceImplBase {
    private static DefaultGrpcTmHandler handler = new DefaultGrpcTmHandler();

    @Override
    public void begin(GlobalBeginRequest request, StreamObserver<GlobalBeginResponse> responseObserver) {
        GlobalBeginResponse response = handler.handle(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void commit(GlobalCommitRequest request, StreamObserver<GlobalCommitResponse> responseObserver) {
        GlobalCommitResponse response = handler.handle(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getStatus(GlobalStatusRequest request, StreamObserver<GlobalStatusResponse> responseObserver) {
        GlobalStatusResponse response = handler.handle(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void globalReport(GlobalReportRequest request, StreamObserver<GlobalReportResponse> responseObserver) {
        GlobalReportResponse response = handler.handle(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void rollback(GlobalRollbackRequest request, StreamObserver<GlobalRollbackResponse> responseObserver) {
        GlobalRollbackResponse response = handler.handle(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
