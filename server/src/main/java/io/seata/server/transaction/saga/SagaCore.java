package io.seata.server.transaction.saga;

import io.netty.channel.Channel;
import io.seata.core.event.GlobalTransactionEvent;
import io.seata.core.exception.GlobalTransactionException;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.GlobalStatus;
import io.seata.core.protocol.transaction.BranchCommitRequest;
import io.seata.core.protocol.transaction.BranchCommitResponse;
import io.seata.core.protocol.transaction.BranchRollbackRequest;
import io.seata.core.protocol.transaction.BranchRollbackResponse;
import io.seata.core.rpc.ChannelManager;
import io.seata.core.rpc.ServerMessageSender;
import io.seata.server.coordinator.AbstractCore;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHelper;
import io.seata.server.session.SessionHolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Created by txg on 2019-12-26.
 */
public class SagaCore extends AbstractCore {

    public SagaCore(ServerMessageSender messageSender) {
        super(messageSender);
    }

    @Override
    public BranchType getBranchType() {
        return BranchType.SAGA;
    }

    @Override
    public void branchRegisterCheck(GlobalSession globalSession) throws GlobalTransactionException {
        // SAGA type accept forward(retry) operation, forward operation will register remaining branches
    }

    @Override
    public BranchStatus branchCommitSend(BranchCommitRequest request, long branchId, String resourceId,
                                         GlobalSession globalSession) throws IOException, TimeoutException {
        Map<String, Channel> channels = ChannelManager.getRmChannels();
        if (channels == null || channels.size() == 0) {
            LOGGER.error("Failed to commit SAGA global[" + globalSession.getXid() + ", RM channels is empty.");
            return BranchStatus.PhaseTwo_CommitFailed_Retryable;
        }
        String sagaResourceId = globalSession.getApplicationId() + "#" + globalSession
                .getTransactionServiceGroup();
        Channel sagaChannel = channels.get(sagaResourceId);
        if (sagaChannel == null) {
            LOGGER.error("Failed to commit SAGA global[" + globalSession.getXid()
                    + ", cannot find channel by resourceId[" + sagaResourceId + "]");
            return BranchStatus.PhaseTwo_CommitFailed_Retryable;
        }
        BranchCommitResponse response = (BranchCommitResponse) messageSender.sendSyncRequest(sagaChannel, request);
        return response.getBranchStatus();
    }

    @Override
    public BranchStatus branchRollbackSend(BranchRollbackRequest request, long branchId, String resourceId,
                                              GlobalSession globalSession) throws IOException, TimeoutException {
        Map<String, Channel> channels = ChannelManager.getRmChannels();
        if (channels == null || channels.size() == 0) {
            LOGGER.error("Failed to rollback SAGA global[" + globalSession.getXid() + ", RM channels is empty.");
            return BranchStatus.PhaseTwo_RollbackFailed_Retryable;
        }
        String sagaResourceId = globalSession.getApplicationId() + "#" + globalSession .getTransactionServiceGroup();
        Channel sagaChannel = channels.get(sagaResourceId);
        if (sagaChannel == null) {
            LOGGER.error("Failed to rollback SAGA global[" + globalSession.getXid()
                    + ", cannot find channel by resourceId[" + sagaResourceId + "]");
            return BranchStatus.PhaseTwo_RollbackFailed_Retryable;
        }
        BranchRollbackResponse response = (BranchRollbackResponse) messageSender.sendSyncRequest(sagaChannel, request);
        return response.getBranchStatus();
    }

    @Override
    public void doGlobalCommit(GlobalSession globalSession, boolean retrying) throws TransactionException {
        try {
            String sagaResourceId = globalSession.getApplicationId() + "#" + globalSession
                    .getTransactionServiceGroup();
            BranchStatus branchStatus = branchCommit(BranchType.SAGA, globalSession.getXid(),
                    -1, sagaResourceId, null);

            switch (branchStatus) {
                case PhaseTwo_Committed:
                    removeAllBranches(globalSession);
                    LOGGER.info("Successfully committed SAGA global[" + globalSession.getXid() + "]");
                    break;
                case PhaseTwo_Rollbacked:
                    LOGGER.info("Successfully rollbacked SAGA global[" + globalSession.getXid() + "]");
                    removeAllBranches(globalSession);
                    SessionHelper.endRollbacked(globalSession);
                    return;
                case PhaseTwo_RollbackFailed_Retryable:
                    LOGGER.error("By [{}], failed to rollback SAGA global [{}], will retry later.", branchStatus,
                            globalSession.getXid());
                    SessionHolder.getRetryCommittingSessionManager().removeGlobalSession(globalSession);
                    globalSession.queueToRetryRollback();
                    return;
                case PhaseOne_Failed:
                    LOGGER.error("By [{}], finish SAGA global [{}]", branchStatus, globalSession.getXid());
                    removeAllBranches(globalSession);
                    globalSession.changeStatus(GlobalStatus.Finished);
                    globalSession.end();
                    return;
                case PhaseTwo_CommitFailed_Unretryable:
                    if (globalSession.canBeCommittedAsync()) {
                        LOGGER.error("By [{}], failed to commit SAGA global [{}]", branchStatus,
                                globalSession.getXid());
                        break;
                    } else {
                        SessionHelper.endCommitFailed(globalSession);
                        LOGGER.error("Finally, failed to commit SAGA global[{}]", globalSession.getXid());
                        return;
                    }
                default:
                    if (!retrying) {
                        globalSession.queueToRetryCommit();
                        return;
                    } else {
                        LOGGER.error("Failed to commit SAGA global[{}], will retry later.", globalSession.getXid());
                        return;
                    }
            }
        } catch (Exception ex) {
            LOGGER.error("Failed to commit global[" + globalSession.getXid() + "]", ex);

            if (!retrying) {
                globalSession.queueToRetryRollback();
            }
            throw new TransactionException(ex);
        }

        SessionHelper.endCommitted(globalSession);

        //committed event
        eventBus.post(new GlobalTransactionEvent(globalSession.getTransactionId(), GlobalTransactionEvent.ROLE_TC,
                globalSession.getTransactionName(), globalSession.getBeginTime(), System.currentTimeMillis(),
                globalSession.getStatus()));

        LOGGER.info("Global[{}] committing is successfully done.", globalSession.getXid());
    }

    @Override
    public void doGlobalRollback(GlobalSession globalSession, boolean retrying) throws TransactionException {
        try {
            String sagaResourceId = globalSession.getApplicationId() + "#" + globalSession
                    .getTransactionServiceGroup();
            BranchStatus branchStatus = branchRollback(BranchType.SAGA,
                    globalSession.getXid(), -1, sagaResourceId, null);

            switch (branchStatus) {
                case PhaseTwo_Rollbacked:
                    removeAllBranches(globalSession);
                    LOGGER.info("Successfully rollbacked SAGA global[{}]",globalSession.getXid());
                    break;
                case PhaseTwo_RollbackFailed_Unretryable:
                    SessionHelper.endRollbackFailed(globalSession);
                    LOGGER.error("Failed to rollback SAGA global[{}]", globalSession.getXid());
                    return;
                default:
                    LOGGER.error("Failed to rollback SAGA global[{}]", globalSession.getXid());
                    if (!retrying) {
                        globalSession.queueToRetryRollback();
                    }
                    return;
            }
        } catch (Exception ex) {
            LOGGER.error("Failed to rollback global[{}]", globalSession.getXid(), ex);
            if (!retrying) {
                globalSession.queueToRetryRollback();
            }
            throw new TransactionException(ex);
        }

        SessionHelper.endRollbacked(globalSession);

        //rollbacked event
        eventBus.post(new GlobalTransactionEvent(globalSession.getTransactionId(), GlobalTransactionEvent.ROLE_TC,
                globalSession.getTransactionName(), globalSession.getBeginTime(), System.currentTimeMillis(),
                globalSession.getStatus()));

        LOGGER.info("Successfully rollback global, xid = {}", globalSession.getXid());
    }


    @Override
    public void doGlobalReport(GlobalSession globalSession, String xid, GlobalStatus globalStatus) throws TransactionException {
        if (GlobalStatus.Committed.equals(globalStatus)) {
            removeAllBranches(globalSession);
            SessionHelper.endCommitted(globalSession);
            LOGGER.info("Global[{}] committed", globalSession.getXid());
        } else if (GlobalStatus.Rollbacked.equals(globalStatus)
                || GlobalStatus.Finished.equals(globalStatus)) {
            removeAllBranches(globalSession);
            SessionHelper.endRollbacked(globalSession);
            LOGGER.info("Global[{}] rollbacked", globalSession.getXid());
        } else {
            globalSession.changeStatus(globalStatus);
            LOGGER.info("Global[{}] reporting is successfully done. status[{}]", globalSession.getXid(), globalSession.getStatus());

            if (GlobalStatus.RollbackRetrying.equals(globalStatus)
                    || GlobalStatus.TimeoutRollbackRetrying.equals(globalStatus)
                    || GlobalStatus.UnKnown.equals(globalStatus)) {
                globalSession.queueToRetryRollback();
                LOGGER.info("Global[{}] will retry rollback", globalSession.getXid());
            } else if (GlobalStatus.CommitRetrying.equals(globalStatus)) {
                globalSession.queueToRetryCommit();
                LOGGER.info("Global[{}] will retry commit", globalSession.getXid());
            }
        }
    }


    /**
     * remove all branches
     *
     * @param globalSession
     * @throws TransactionException
     */
    private void removeAllBranches(GlobalSession globalSession) throws TransactionException {
        ArrayList<BranchSession> branchSessions = globalSession.getSortedBranches();
        for (BranchSession branchSession : branchSessions) {
            globalSession.removeBranch(branchSession);
        }
    }
}
