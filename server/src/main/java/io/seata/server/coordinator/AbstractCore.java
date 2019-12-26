package io.seata.server.coordinator;

import io.seata.core.event.EventBus;
import io.seata.core.exception.BranchTransactionException;
import io.seata.core.exception.GlobalTransactionException;
import io.seata.core.exception.TransactionException;
import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.GlobalStatus;
import io.seata.core.protocol.transaction.BranchCommitRequest;
import io.seata.core.protocol.transaction.BranchCommitResponse;
import io.seata.core.protocol.transaction.BranchRollbackRequest;
import io.seata.core.protocol.transaction.BranchRollbackResponse;
import io.seata.core.rpc.ServerMessageSender;
import io.seata.server.event.EventBusManager;
import io.seata.server.lock.LockManager;
import io.seata.server.lock.LockerManagerFactory;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHelper;
import io.seata.server.session.SessionHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static io.seata.core.exception.TransactionExceptionCode.*;

/**
 * Created by txg on 2019-12-24.
 */
public abstract class AbstractCore implements Core1 {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractCore.class);

    protected LockManager lockManager = LockerManagerFactory.getLockManager();

    protected EventBus eventBus = EventBusManager.get();

    protected ServerMessageSender messageSender;

    public AbstractCore(ServerMessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @Override
    public Long branchRegister(BranchType branchType, String resourceId, String clientId, String xid,
                               String applicationData, String lockKeys) throws TransactionException {
        GlobalSession globalSession = assertGlobalSessionNotNull(xid, false);
        return globalSession.lockAndExcute(() -> {
            if (!globalSession.isActive()) {
                throw new GlobalTransactionException(GlobalTransactionNotActive, String
                        .format("Could not register branch into global session xid = %s status = %s",
                                globalSession.getXid(), globalSession.getStatus()));
            }
            branchRegisterCheck(globalSession);
            globalSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
            BranchSession branchSession = SessionHelper.newBranchByGlobal(globalSession, branchType, resourceId,
                    applicationData, lockKeys, clientId);
            if (!branchSession.lock()) {
                throw new BranchTransactionException(LockKeyConflict, String
                        .format("Global lock acquire failed xid = %s branchId = %s", globalSession.getXid(),
                                branchSession.getBranchId()));
            }
            try {
                globalSession.addBranch(branchSession);
            } catch (RuntimeException ex) {
                branchSession.unlock();
                throw new BranchTransactionException(FailedToAddBranch, String
                        .format("Failed to store branch xid = %s branchId = %s", globalSession.getXid(),
                                branchSession.getBranchId()), ex);
            }
            LOGGER.info("Successfully register branch xid = {}, branchId = {}", globalSession.getXid(),
                    branchSession.getBranchId());
            return branchSession.getBranchId();
        });
    }

    protected void branchRegisterCheck(GlobalSession globalSession) throws GlobalTransactionException {
        if (globalSession.getStatus() != GlobalStatus.Begin) {
            throw new GlobalTransactionException(GlobalTransactionStatusInvalid, String
                    .format("Could not register branch into global session xid = %s status = %s while expecting %s",
                            globalSession.getXid(), globalSession.getStatus(), GlobalStatus.Begin));
        }
    }

    private GlobalSession assertGlobalSessionNotNull(String xid, boolean withBranchSessions)
            throws TransactionException {
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid, withBranchSessions);
        if (globalSession == null) {
            throw new GlobalTransactionException(TransactionExceptionCode.GlobalTransactionNotExist,
                    String.format("Could not found global transaction xid = %s", xid));
        }
        return globalSession;
    }

    @Override
    public void branchReport(BranchType branchType, String xid, long branchId, BranchStatus status,
                             String applicationData) throws TransactionException {
        GlobalSession globalSession = assertGlobalSessionNotNull(xid, true);
        BranchSession branchSession = globalSession.getBranch(branchId);
        if (branchSession == null) {
            throw new BranchTransactionException(BranchTransactionNotExist,
                    String.format("Could not found branch session xid = %s branchId = %s", xid, branchId));
        }
        globalSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
        globalSession.changeBranchStatus(branchSession, status);

        LOGGER.info("Successfully branch report xid = {}, branchId = {}", globalSession.getXid(),
                branchSession.getBranchId());
    }

    @Override
    public boolean lockQuery(BranchType branchType, String resourceId, String xid, String lockKeys)
            throws TransactionException {
        return true;
    }

    @Override
    public BranchStatus branchCommit(BranchType branchType, String xid, long branchId, String resourceId,
                                     String applicationData) throws TransactionException {
        try {
            BranchCommitRequest request = new BranchCommitRequest();
            request.setXid(xid);
            request.setBranchId(branchId);
            request.setResourceId(resourceId);
            request.setApplicationData(applicationData);
            request.setBranchType(branchType);

            GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
            if (globalSession == null) {
                return BranchStatus.PhaseTwo_Committed;
            }
            return branchCommitSend(request, branchId, resourceId, globalSession);
        } catch (IOException | TimeoutException e) {
            throw new BranchTransactionException(FailedToSendBranchCommitRequest,
                    String.format("Send branch commit failed, xid = %s branchId = %s", xid, branchId), e);
        }
    }

    protected BranchStatus branchCommitSend(BranchCommitRequest request, long branchId, String resourceId,
                                         GlobalSession globalSession) throws IOException, TimeoutException {
        BranchSession branchSession = globalSession.getBranch(branchId);
        if (null != branchSession) {
            BranchCommitResponse response = (BranchCommitResponse) messageSender.sendSyncRequest(resourceId,
                    branchSession.getClientId(), request);
            return response.getBranchStatus();
        } else {
            return BranchStatus.PhaseTwo_Committed;
        }
    }

    @Override
    public BranchStatus branchRollback(BranchType branchType, String xid, long branchId, String resourceId,
                                       String applicationData) throws TransactionException {
        try {
            BranchRollbackRequest request = new BranchRollbackRequest();
            request.setXid(xid);
            request.setBranchId(branchId);
            request.setResourceId(resourceId);
            request.setApplicationData(applicationData);
            request.setBranchType(branchType);

            GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
            if (globalSession == null) {
                return BranchStatus.PhaseTwo_Rollbacked;
            }
            return branchRollbackSend(request, branchId, resourceId, globalSession);
        } catch (IOException | TimeoutException e) {
            throw new BranchTransactionException(FailedToSendBranchRollbackRequest,
                    String.format("Send branch rollback failed, xid = %s branchId = %s", xid, branchId), e);
        }
    }

    protected BranchStatus branchRollbackSend(BranchRollbackRequest request, long branchId, String resourceId,
                                            GlobalSession globalSession) throws IOException, TimeoutException {
        BranchSession branchSession = globalSession.getBranch(branchId);

        BranchRollbackResponse response = (BranchRollbackResponse) messageSender.sendSyncRequest(resourceId,
                branchSession.getClientId(), request);
        return response.getBranchStatus();
    }

    @Override
    public String begin(String applicationId, String transactionServiceGroup, String name, int timeout)
            throws TransactionException {
        return null;
    }

    @Override
    public GlobalStatus commit(String xid) throws TransactionException {
        return null;
    }

    @Override
    public void doGlobalCommit(GlobalSession globalSession, boolean retrying) throws TransactionException {

    }

    @Override
    public GlobalStatus globalReport(String xid, GlobalStatus globalStatus) throws TransactionException {
        return null;
    }

    @Override
    public GlobalStatus rollback(String xid) throws TransactionException {
        return null;
    }

    @Override
    public void doGlobalRollback(GlobalSession globalSession, boolean retrying) throws TransactionException {

    }

    @Override
    public GlobalStatus getStatus(String xid) throws TransactionException {
        return null;
    }

    @Override
    public void doGlobalReport(GlobalSession globalSession, String xid, GlobalStatus globalStatus) throws TransactionException {

    }
}