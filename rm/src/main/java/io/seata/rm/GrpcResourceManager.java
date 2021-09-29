package io.seata.rm;

import com.google.protobuf.ByteString;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.Resource;
import io.seata.core.model.ResourceManager;
import io.seata.core.model.grpc.SeataGrpc.BranchCommitRequest;
import io.seata.core.model.grpc.SeataGrpc.BranchRegisterRequest;
import io.seata.core.model.grpc.SeataGrpc.BranchReportRequest;
import io.seata.core.model.grpc.SeataGrpc.BranchRollbackRequest;
import io.seata.core.model.grpc.SeataGrpc.BranchSession;
import io.seata.core.model.grpc.SeataGrpc.GlobalLockQueryRequest;
import io.seata.core.rpc.grpc.GrpcRemotingServer;
import io.seata.core.rpc.grpc.RmGrpcRemotingClient;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author xilou31
 **/
public class GrpcResourceManager implements ResourceManager {
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
        return BranchStatus.valueOf(GrpcRemotingServer.getInstance()
                .branchCommit(request)
                .getBranchStatus().name());
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
        return BranchStatus.valueOf(GrpcRemotingServer.getInstance()
                .branchRollback(request)
                .getBranchStatus().name());
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
        return RmGrpcRemotingClient.getInstance()
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
        RmGrpcRemotingClient.getInstance()
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
        return RmGrpcRemotingClient.getInstance()
                .lockQuery(request)
                .getLockable();
    }
}
