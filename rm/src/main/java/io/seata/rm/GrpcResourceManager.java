package io.seata.rm;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.Resource;
import io.seata.core.model.ResourceManager;
import io.seata.core.model.grpc.SeataGrpc.BranchCommitRequest;
import io.seata.core.model.grpc.SeataGrpc.BranchCommitResponse;
import io.seata.core.model.grpc.SeataGrpc.BranchRegisterRequest;
import io.seata.core.model.grpc.SeataGrpc.BranchReportRequest;
import io.seata.core.model.grpc.SeataGrpc.BranchRollbackRequest;
import io.seata.core.model.grpc.SeataGrpc.BranchSession;
import io.seata.core.model.grpc.SeataGrpc.GlobalLockQueryRequest;
import io.seata.core.rpc.grpc.RmGrpcRemotingClient;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author xilou31
 **/
public class GrpcResourceManager implements ResourceManager {
    private static volatile GrpcResourceManager instance;

    @Override
    public void registerResource(Resource resource) {

    }

    @Override
    public void unregisterResource(Resource resource) {

    }

    @Override
    public Map<String, Resource> getManagedResources() {
        return null;
    }

    @Override
    public BranchType getBranchType() {
        return null;
    }

    @Override
    public BranchStatus branchCommit(BranchType branchType, String xid, long branchId, String resourceId, String applicationData) throws TransactionException {
        BranchCommitRequest request = BranchCommitRequest.newBuilder()
                .setBranchType(BranchSession.BranchType.valueOf(branchType.name()))
                .setXID(xid)
                .setBranchID(branchId)
                .setResourceID(resourceId)
                .setApplicationData(ByteString.copyFrom(applicationData, StandardCharsets.UTF_8))
                .build();
        StreamObserver<BranchCommitResponse> observer = new StreamObserver<BranchCommitResponse>() {
            @Override
            public void onNext(BranchCommitResponse branchCommitResponse) {
                System.out.println("received Message" + branchCommitResponse.getMessage());
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("error " + throwable.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("completed");
            }
        };
        RmGrpcRemotingClient.getAsyncStub().branchCommit(request, observer);
        return null;
    }

    @Override
    public BranchStatus branchRollback(BranchType branchType, String xid, long branchId, String resourceId, String applicationData) throws TransactionException {
        BranchRollbackRequest request = BranchRollbackRequest.newBuilder()
                .setBranchType(BranchSession.BranchType.valueOf(branchType.name()))
                .setXID(xid)
                .setBranchID(branchId)
                .setResourceID(resourceId)
                .setApplicationData(ByteString.copyFrom(applicationData, StandardCharsets.UTF_8))
                .build();
//        return BranchStatus.valueOf(RmGrpcRemotingClient.getInstance().
//                .branchRollback(request)
//                .getBranchStatus().name());
        return null;
    }

    @Override
    public Long branchRegister(BranchType branchType, String resourceId, String clientId, String xid, String applicationData, String lockKeys) throws TransactionException {
        BranchRegisterRequest request = BranchRegisterRequest.newBuilder()
                .setBranchType(BranchSession.BranchType.valueOf(branchType.name()))
                .setResourceID(resourceId)
                .setAddressing(clientId)
                .setXID(xid)
                .setApplicationData(ByteString.copyFrom(applicationData, StandardCharsets.UTF_8))
                .setLockKey(lockKeys)
                .build();
        return RmGrpcRemotingClient.getSyncStub()
                .branchRegister(request)
                .getBranchID();
    }

    @Override
    public void branchReport(BranchType branchType, String xid, long branchId, BranchStatus status, String applicationData) throws TransactionException {
        BranchReportRequest request = BranchReportRequest.newBuilder()
                .setBranchType(BranchSession.BranchType.valueOf(branchType.name()))
                .setXID(xid)
                .setBranchID(branchId)
                .setBranchStatus(BranchSession.BranchStatus.valueOf(status.name()))
                .setApplicationData(ByteString.copyFrom(applicationData, StandardCharsets.UTF_8))
                .build();
        RmGrpcRemotingClient.getSyncStub()
                .branchReport(request);
    }

    @Override
    public boolean lockQuery(BranchType branchType, String resourceId, String xid, String lockKeys) throws TransactionException {
        GlobalLockQueryRequest request = GlobalLockQueryRequest.newBuilder()
                .setBranchType(BranchSession.BranchType.valueOf(branchType.name()))
                .setResourceID(resourceId)
                .setXID(xid)
                .setLockKey(lockKeys)
                .build();
        return RmGrpcRemotingClient.getSyncStub()
                .lockQuery(request)
                .getLockable();
    }

    public static GrpcResourceManager getInstance() {
        if (instance == null) {
            instance = new GrpcResourceManager();
        }
        return instance;
    }
}
