/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.server.coordinator;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.DurationUtil;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.event.EventBus;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.GlobalStatus;
import io.seata.core.model.ResourceManagerInbound;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.AbstractResultMessage;
import io.seata.core.protocol.transaction.AbstractTransactionRequestToTC;
import io.seata.core.protocol.transaction.AbstractTransactionResponse;
import io.seata.core.protocol.transaction.BranchCommitRequest;
import io.seata.core.protocol.transaction.BranchCommitResponse;
import io.seata.core.protocol.transaction.BranchRegisterRequest;
import io.seata.core.protocol.transaction.BranchRegisterResponse;
import io.seata.core.protocol.transaction.BranchReportRequest;
import io.seata.core.protocol.transaction.BranchReportResponse;
import io.seata.core.protocol.transaction.BranchRollbackRequest;
import io.seata.core.protocol.transaction.BranchRollbackResponse;
import io.seata.core.protocol.transaction.GlobalBeginRequest;
import io.seata.core.protocol.transaction.GlobalBeginResponse;
import io.seata.core.protocol.transaction.GlobalCommitRequest;
import io.seata.core.protocol.transaction.GlobalCommitResponse;
import io.seata.core.protocol.transaction.GlobalLockQueryRequest;
import io.seata.core.protocol.transaction.GlobalLockQueryResponse;
import io.seata.core.protocol.transaction.GlobalRollbackRequest;
import io.seata.core.protocol.transaction.GlobalRollbackResponse;
import io.seata.core.protocol.transaction.GlobalStatusRequest;
import io.seata.core.protocol.transaction.GlobalStatusResponse;
import io.seata.core.rpc.Disposable;
import io.seata.core.rpc.RpcContext;
import io.seata.core.rpc.ServerMessageSender;
import io.seata.core.rpc.TransactionMessageHandler;
import io.seata.core.rpc.netty.RpcServer;
import io.seata.server.AbstractTCInboundHandler;
import io.seata.server.event.EventBusManager;
import io.seata.core.event.GlobalTransactionEvent;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.core.exception.TransactionExceptionCode.FailedToSendBranchCommitRequest;
import static io.seata.core.exception.TransactionExceptionCode.FailedToSendBranchRollbackRequest;

/**
 * The type Default coordinator.
 *
 * @author sharajava
 */
public class DefaultCoordinator extends AbstractTCInboundHandler
    implements TransactionMessageHandler, ResourceManagerInbound, Disposable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCoordinator.class);

    private static final int TIMED_TASK_SHUTDOWN_MAX_WAIT_MILLS = 5000;

    /**
     * The Committing retry delay.
     */
    protected int committingRetryDelay = CONFIG.getInt(ConfigurationKeys.COMMITING_RETRY_DELAY, 5);

    /**
     * The Asyn committing retry delay.
     */
    protected int asynCommittingRetryDelay = CONFIG.getInt(ConfigurationKeys.ASYN_COMMITING_RETRY_DELAY, 5);

    /**
     * The Rollbacking retry delay.
     */
    protected int rollbackingRetryDelay = CONFIG.getInt(ConfigurationKeys.ROLLBACKING_RETRY_DELAY, 5);

    /**
     * The Timeout retry delay.
     */
    protected int timeoutRetryDelay = CONFIG.getInt(ConfigurationKeys.TIMEOUT_RETRY_DELAY, 5);

    private static final int ALWAYS_RETRY_BOUNDARY = 0;

    private static final Duration MAX_COMMIT_RETRY_TIMEOUT = ConfigurationFactory.getInstance().getDuration(
        ConfigurationKeys.SERVICE_PREFIX + "max.commit.retry.timeout", DurationUtil.DEFAULT_DURATION, 100);

    private static final Duration MAX_ROLLBACK_RETRY_TIMEOUT = ConfigurationFactory.getInstance().getDuration(
        ConfigurationKeys.SERVICE_PREFIX + "max.rollback.retry.timeout", DurationUtil.DEFAULT_DURATION, 100);

    private ScheduledThreadPoolExecutor retryRollbacking = new ScheduledThreadPoolExecutor(1,
        new NamedThreadFactory("RetryRollbacking", 1));

    private ScheduledThreadPoolExecutor retryCommitting = new ScheduledThreadPoolExecutor(1,
        new NamedThreadFactory("RetryCommitting", 1));

    private ScheduledThreadPoolExecutor asyncCommitting = new ScheduledThreadPoolExecutor(1,
        new NamedThreadFactory("AsyncCommitting", 1));

    private ScheduledThreadPoolExecutor timeoutCheck = new ScheduledThreadPoolExecutor(1,
        new NamedThreadFactory("TxTimeoutCheck", 1));

    private ServerMessageSender messageSender;

    private Core core = CoreFactory.get();

    private EventBus eventBus = EventBusManager.get();

    /**
     * Instantiates a new Default coordinator.
     *
     * @param messageSender the message sender
     */
    public DefaultCoordinator(ServerMessageSender messageSender) {
        this.messageSender = messageSender;
        core.setResourceManagerInbound(this);
    }

    @Override
    protected void doGlobalBegin(GlobalBeginRequest request, GlobalBeginResponse response, RpcContext rpcContext)
        throws TransactionException {
        response.setXid(core.begin(rpcContext.getApplicationId(), rpcContext.getTransactionServiceGroup(),
            request.getTransactionName(), request.getTimeout()));
    }

    @Override
    protected void doGlobalCommit(GlobalCommitRequest request, GlobalCommitResponse response, RpcContext rpcContext)
        throws TransactionException {
        response.setGlobalStatus(core.commit(request.getXid()));

    }

    @Override
    protected void doGlobalRollback(GlobalRollbackRequest request, GlobalRollbackResponse response,
                                    RpcContext rpcContext) throws TransactionException {
        response.setGlobalStatus(core.rollback(request.getXid()));

    }

    @Override
    protected void doGlobalStatus(GlobalStatusRequest request, GlobalStatusResponse response, RpcContext rpcContext)
        throws TransactionException {
        response.setGlobalStatus(core.getStatus(request.getXid()));
    }

    @Override
    protected void doBranchRegister(BranchRegisterRequest request, BranchRegisterResponse response,
                                    RpcContext rpcContext) throws TransactionException {
        response.setBranchId(
            core.branchRegister(request.getBranchType(), request.getResourceId(), rpcContext.getClientId(),
                request.getXid(), request.getApplicationData(), request.getLockKey()));

    }

    @Override
    protected void doBranchReport(BranchReportRequest request, BranchReportResponse response, RpcContext rpcContext)
        throws TransactionException {
        core.branchReport(request.getBranchType(), request.getXid(), request.getBranchId(),
            request.getStatus(),
            request.getApplicationData());

    }

    @Override
    protected void doLockCheck(GlobalLockQueryRequest request, GlobalLockQueryResponse response, RpcContext rpcContext)
        throws TransactionException {
        response.setLockable(core.lockQuery(request.getBranchType(), request.getResourceId(),
            request.getXid(), request.getLockKey()));
    }

    @Override
    public BranchStatus branchCommit(BranchType branchType, String xid, long branchId, String resourceId,
                                     String applicationData)
        throws TransactionException {
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
            BranchSession branchSession = globalSession.getBranch(branchId);

            BranchCommitResponse response = (BranchCommitResponse)messageSender.sendSyncRequest(resourceId,
                branchSession.getClientId(), request);
            return response.getBranchStatus();
        } catch (IOException | TimeoutException e) {
            throw new TransactionException(FailedToSendBranchCommitRequest, branchId + "/" + xid, e);
        }
    }

    @Override
    public BranchStatus branchRollback(BranchType branchType, String xid, long branchId, String resourceId,
                                       String applicationData)
        throws TransactionException {
        try {
            BranchRollbackRequest
                request = new BranchRollbackRequest();
            request.setXid(xid);
            request.setBranchId(branchId);
            request.setResourceId(resourceId);
            request.setApplicationData(applicationData);
            request.setBranchType(branchType);

            GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
            if (globalSession == null) {
                return BranchStatus.PhaseTwo_Rollbacked;
            }
            BranchSession branchSession = globalSession.getBranch(branchId);

            BranchRollbackResponse response = (BranchRollbackResponse)messageSender.sendSyncRequest(resourceId,
                branchSession.getClientId(), request);
            return response.getBranchStatus();
        } catch (IOException | TimeoutException e) {
            throw new TransactionException(FailedToSendBranchRollbackRequest, branchId + "/" + xid, e);
        }
    }

    /**
     * Timeout check.
     *
     * @throws TransactionException the transaction exception
     */
    protected void timeoutCheck() throws TransactionException {
        Collection<GlobalSession> allSessions = SessionHolder.getRootSessionManager().allSessions();
        if (CollectionUtils.isEmpty(allSessions)) {
            return;
        }
        if (allSessions.size() > 0 && LOGGER.isDebugEnabled()) {
            LOGGER.debug("Transaction Timeout Check Begin: " + allSessions.size());
        }
        for (GlobalSession globalSession : allSessions) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(globalSession.getXid() + " " + globalSession.getStatus() + " " +
                    globalSession.getBeginTime() + " " + globalSession.getTimeout());
            }
            boolean shouldTimeout = globalSession.lockAndExcute(() -> {
                if (globalSession.getStatus() != GlobalStatus.Begin || !globalSession.isTimeout()) {
                    return false;
                }
                globalSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
                globalSession.close();
                globalSession.changeStatus(GlobalStatus.TimeoutRollbacking);

                //transaction timeout and start rollbacking event
                eventBus.post(
                    new GlobalTransactionEvent(globalSession.getTransactionId(), GlobalTransactionEvent.ROLE_TC,
                        globalSession.getTransactionName(), globalSession.getBeginTime(), null,
                        globalSession.getStatus()));

                return true;
            });
            if (!shouldTimeout) {
                continue;
            }
            LOGGER.info(
                "Global transaction[" + globalSession.getXid() + "] is timeout and will be rolled back.");

            globalSession.addSessionLifecycleListener(SessionHolder.getRetryRollbackingSessionManager());
            SessionHolder.getRetryRollbackingSessionManager().addGlobalSession(globalSession);

        }
        if (allSessions.size() > 0 && LOGGER.isDebugEnabled()) {
            LOGGER.debug("Transaction Timeout Check End. ");
        }

    }

    /**
     * Handle retry rollbacking.
     */
    protected void handleRetryRollbacking() {
        Collection<GlobalSession> rollbackingSessions = SessionHolder.getRetryRollbackingSessionManager().allSessions();
        if (CollectionUtils.isEmpty(rollbackingSessions)) {
            return;
        }
        long now = System.currentTimeMillis();
        for (GlobalSession rollbackingSession : rollbackingSessions) {
            try {
                if (isRetryTimeout(now, MAX_ROLLBACK_RETRY_TIMEOUT.toMillis(), rollbackingSession.getBeginTime())) {
                    /**
                     * Prevent thread safety issues
                     */
                    SessionHolder.getRetryCommittingSessionManager().removeGlobalSession(rollbackingSession);
                    LOGGER.error("GlobalSession rollback retry timeout [{}]", rollbackingSession.getXid());
                    continue;
                }
                rollbackingSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
                core.doGlobalRollback(rollbackingSession, true);
            } catch (TransactionException ex) {
                LOGGER.info("Failed to retry rollbacking [{}] {} {}",
                    rollbackingSession.getXid(), ex.getCode(), ex.getMessage());
            }
        }
    }

    /**
     * Handle retry committing.
     */
    protected void handleRetryCommitting() {
        Collection<GlobalSession> committingSessions = SessionHolder.getRetryCommittingSessionManager().allSessions();
        if (CollectionUtils.isEmpty(committingSessions)) {
            return;
        }
        long now = System.currentTimeMillis();
        for (GlobalSession committingSession : committingSessions) {
            try {
                if (isRetryTimeout(now, MAX_COMMIT_RETRY_TIMEOUT.toMillis(), committingSession.getBeginTime())) {
                    /**
                     * Prevent thread safety issues
                     */
                    SessionHolder.getRetryCommittingSessionManager().removeGlobalSession(committingSession);
                    LOGGER.error("GlobalSession commit retry timeout [{}]", committingSession.getXid());
                    continue;
                }
                committingSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
                core.doGlobalCommit(committingSession, true);
            } catch (TransactionException ex) {
                LOGGER.info("Failed to retry committing [{}] {} {}",
                    committingSession.getXid(), ex.getCode(), ex.getMessage());
            }
        }
    }

    private boolean isRetryTimeout(long now, long timeout, long beginTime) {
        /**
         * Start timing when the session begin
         */
        if (timeout >= ALWAYS_RETRY_BOUNDARY &&
            now - beginTime > timeout) {
            return true;
        }
        return false;
    }

    /**
     * Handle async committing.
     */
    protected void handleAsyncCommitting() {
        Collection<GlobalSession> asyncCommittingSessions = SessionHolder.getAsyncCommittingSessionManager()
            .allSessions();
        if (CollectionUtils.isEmpty(asyncCommittingSessions)) {
            return;
        }
        for (GlobalSession asyncCommittingSession : asyncCommittingSessions) {
            try {
                asyncCommittingSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
                core.doGlobalCommit(asyncCommittingSession, true);
            } catch (TransactionException ex) {
                LOGGER.info("Failed to async committing [{}] {} {}",
                    asyncCommittingSession.getXid(), ex.getCode(), ex.getMessage());
            }
        }
    }

    /**
     * Init.
     */
    public void init() {
        retryRollbacking.scheduleAtFixedRate(() -> {
            try {
                handleRetryRollbacking();
            } catch (Exception e) {
                LOGGER.info("Exception retry rollbacking ... ", e);
            }
        }, 0, rollbackingRetryDelay, TimeUnit.SECONDS);

        retryCommitting.scheduleAtFixedRate(() -> {
            try {
                handleRetryCommitting();
            } catch (Exception e) {
                LOGGER.info("Exception retry committing ... ", e);
            }
        }, 0, committingRetryDelay, TimeUnit.SECONDS);

        asyncCommitting.scheduleAtFixedRate(() -> {
            try {
                handleAsyncCommitting();
            } catch (Exception e) {
                LOGGER.info("Exception async committing ... ", e);
            }
        }, 0, asynCommittingRetryDelay, TimeUnit.SECONDS);

        timeoutCheck.scheduleAtFixedRate(() -> {
            try {
                timeoutCheck();
            } catch (Exception e) {
                LOGGER.info("Exception timeout checking ... ", e);
            }
        }, 0, timeoutRetryDelay, TimeUnit.SECONDS);
    }

    @Override
    public AbstractResultMessage onRequest(AbstractMessage request, RpcContext context) {
        if (!(request instanceof AbstractTransactionRequestToTC)) {
            throw new IllegalArgumentException();
        }
        AbstractTransactionRequestToTC transactionRequest = (AbstractTransactionRequestToTC)request;
        transactionRequest.setTCInboundHandler(this);

        return transactionRequest.handle(context);
    }

    @Override
    public void onResponse(AbstractResultMessage response, RpcContext context) {
        if (!(response instanceof AbstractTransactionResponse)) {
            throw new IllegalArgumentException();
        }

    }

    @Override
    public void destroy() {
        // 1. first shutdown timed task
        retryRollbacking.shutdown();
        retryCommitting.shutdown();
        asyncCommitting.shutdown();
        timeoutCheck.shutdown();
        try {
            retryRollbacking.awaitTermination(TIMED_TASK_SHUTDOWN_MAX_WAIT_MILLS, TimeUnit.MILLISECONDS);
            retryCommitting.awaitTermination(TIMED_TASK_SHUTDOWN_MAX_WAIT_MILLS, TimeUnit.MILLISECONDS);
            asyncCommitting.awaitTermination(TIMED_TASK_SHUTDOWN_MAX_WAIT_MILLS, TimeUnit.MILLISECONDS);
            timeoutCheck.awaitTermination(TIMED_TASK_SHUTDOWN_MAX_WAIT_MILLS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignore) {

        }
        // 2. second close netty flow
        if (messageSender instanceof RpcServer) {
            ((RpcServer)messageSender).destroy();
        }
        // 3. last destroy SessionHolder
        SessionHolder.destory();
    }
}
