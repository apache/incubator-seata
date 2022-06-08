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

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.netty.channel.Channel;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.DurationUtil;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.context.RootContext;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.GlobalStatus;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.AbstractResultMessage;
import io.seata.core.protocol.transaction.AbstractTransactionRequestToTC;
import io.seata.core.protocol.transaction.AbstractTransactionResponse;
import io.seata.core.protocol.transaction.BranchRegisterRequest;
import io.seata.core.protocol.transaction.BranchRegisterResponse;
import io.seata.core.protocol.transaction.BranchReportRequest;
import io.seata.core.protocol.transaction.BranchReportResponse;
import io.seata.core.protocol.transaction.GlobalBeginRequest;
import io.seata.core.protocol.transaction.GlobalBeginResponse;
import io.seata.core.protocol.transaction.GlobalCommitRequest;
import io.seata.core.protocol.transaction.GlobalCommitResponse;
import io.seata.core.protocol.transaction.GlobalLockQueryRequest;
import io.seata.core.protocol.transaction.GlobalLockQueryResponse;
import io.seata.core.protocol.transaction.GlobalReportRequest;
import io.seata.core.protocol.transaction.GlobalReportResponse;
import io.seata.core.protocol.transaction.GlobalRollbackRequest;
import io.seata.core.protocol.transaction.GlobalRollbackResponse;
import io.seata.core.protocol.transaction.GlobalStatusRequest;
import io.seata.core.protocol.transaction.GlobalStatusResponse;
import io.seata.core.protocol.transaction.UndoLogDeleteRequest;
import io.seata.core.rpc.Disposable;
import io.seata.core.rpc.RemotingServer;
import io.seata.core.rpc.RpcContext;
import io.seata.core.rpc.TransactionMessageHandler;
import io.seata.core.rpc.netty.ChannelManager;
import io.seata.core.rpc.netty.NettyRemotingServer;
import io.seata.server.AbstractTCInboundHandler;
import io.seata.server.metrics.MetricsPublisher;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionCondition;
import io.seata.server.session.SessionHelper;
import io.seata.server.session.SessionHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static io.seata.common.Constants.ASYNC_COMMITTING;
import static io.seata.common.Constants.RETRY_COMMITTING;
import static io.seata.common.Constants.RETRY_ROLLBACKING;
import static io.seata.common.Constants.TX_TIMEOUT_CHECK;
import static io.seata.common.Constants.UNDOLOG_DELETE;

/**
 * The type Default coordinator.
 */
public class DefaultCoordinator extends AbstractTCInboundHandler implements TransactionMessageHandler, Disposable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCoordinator.class);

    private static final int TIMED_TASK_SHUTDOWN_MAX_WAIT_MILLS = 5000;

    /**
     * The constant COMMITTING_RETRY_PERIOD.
     */
    protected static final long COMMITTING_RETRY_PERIOD = CONFIG.getLong(ConfigurationKeys.COMMITING_RETRY_PERIOD,
        1000L);

    /**
     * The constant ASYNC_COMMITTING_RETRY_PERIOD.
     */
    protected static final long ASYNC_COMMITTING_RETRY_PERIOD = CONFIG.getLong(
        ConfigurationKeys.ASYN_COMMITING_RETRY_PERIOD, 1000L);

    /**
     * The constant ROLLBACKING_RETRY_PERIOD.
     */
    protected static final long ROLLBACKING_RETRY_PERIOD = CONFIG.getLong(ConfigurationKeys.ROLLBACKING_RETRY_PERIOD,
        1000L);

    /**
     * The constant TIMEOUT_RETRY_PERIOD.
     */
    protected static final long TIMEOUT_RETRY_PERIOD = CONFIG.getLong(ConfigurationKeys.TIMEOUT_RETRY_PERIOD, 1000L);

    /**
     * The Transaction undo log delete period.
     */
    protected static final long UNDO_LOG_DELETE_PERIOD = CONFIG.getLong(
            ConfigurationKeys.TRANSACTION_UNDO_LOG_DELETE_PERIOD, 24 * 60 * 60 * 1000);

    /**
     * The Transaction undo log delay delete period
     */
    protected static final long UNDO_LOG_DELAY_DELETE_PERIOD = 3 * 60 * 1000;

    private static final int ALWAYS_RETRY_BOUNDARY = 0;

    /**
     * default branch async queue size
     */
    private static final int DEFAULT_BRANCH_ASYNC_QUEUE_SIZE = 5000;

    /**
     * the pool size of branch asynchronous remove thread pool
     */
    private static final int BRANCH_ASYNC_POOL_SIZE = Runtime.getRuntime().availableProcessors();

    private static final Duration MAX_COMMIT_RETRY_TIMEOUT = ConfigurationFactory.getInstance().getDuration(
            ConfigurationKeys.MAX_COMMIT_RETRY_TIMEOUT, DurationUtil.DEFAULT_DURATION, 100);

    private static final Duration MAX_ROLLBACK_RETRY_TIMEOUT = ConfigurationFactory.getInstance().getDuration(
            ConfigurationKeys.MAX_ROLLBACK_RETRY_TIMEOUT, DurationUtil.DEFAULT_DURATION, 100);

    private static final boolean ROLLBACK_RETRY_TIMEOUT_UNLOCK_ENABLE = ConfigurationFactory.getInstance().getBoolean(
            ConfigurationKeys.ROLLBACK_RETRY_TIMEOUT_UNLOCK_ENABLE, false);

    private final ScheduledThreadPoolExecutor retryRollbacking =
        new ScheduledThreadPoolExecutor(1, new NamedThreadFactory(RETRY_ROLLBACKING, 1));

    private final ScheduledThreadPoolExecutor retryCommitting =
        new ScheduledThreadPoolExecutor(1, new NamedThreadFactory(RETRY_COMMITTING, 1));

    private final ScheduledThreadPoolExecutor asyncCommitting =
        new ScheduledThreadPoolExecutor(1, new NamedThreadFactory(ASYNC_COMMITTING, 1));

    private final ScheduledThreadPoolExecutor timeoutCheck =
        new ScheduledThreadPoolExecutor(1, new NamedThreadFactory(TX_TIMEOUT_CHECK, 1));

    private final ScheduledThreadPoolExecutor undoLogDelete =
        new ScheduledThreadPoolExecutor(1, new NamedThreadFactory(UNDOLOG_DELETE, 1));

    private final GlobalStatus[] rollbackingStatuses = new GlobalStatus[] {GlobalStatus.TimeoutRollbacking,
        GlobalStatus.TimeoutRollbackRetrying, GlobalStatus.RollbackRetrying, GlobalStatus.Rollbacking};

    private final GlobalStatus[] retryCommittingStatuses =
        new GlobalStatus[] {GlobalStatus.Committing, GlobalStatus.CommitRetrying};

    private final ThreadPoolExecutor branchRemoveExecutor = new ThreadPoolExecutor(BRANCH_ASYNC_POOL_SIZE, BRANCH_ASYNC_POOL_SIZE,
            Integer.MAX_VALUE, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(
                    CONFIG.getInt(ConfigurationKeys.SESSION_BRANCH_ASYNC_QUEUE_SIZE, DEFAULT_BRANCH_ASYNC_QUEUE_SIZE)
            ), new NamedThreadFactory("branchSessionRemove", BRANCH_ASYNC_POOL_SIZE),
            new ThreadPoolExecutor.CallerRunsPolicy());

    private RemotingServer remotingServer;

    private final DefaultCore core;

    private static volatile DefaultCoordinator instance;

    /**
     * Instantiates a new Default coordinator.
     *
     * @param remotingServer the remoting server
     */
    private DefaultCoordinator(RemotingServer remotingServer) {
        if (remotingServer == null) {
            throw new IllegalArgumentException("RemotingServer not allowed be null.");
        }
        this.remotingServer = remotingServer;
        this.core = new DefaultCore(remotingServer);
    }

    public static DefaultCoordinator getInstance(RemotingServer remotingServer) {
        if (null == instance) {
            synchronized (DefaultCoordinator.class) {
                if (null == instance) {
                    instance = new DefaultCoordinator(remotingServer);
                }
            }
        }
        return instance;
    }

    public static DefaultCoordinator getInstance() {
        if (null == instance) {
            throw new IllegalArgumentException("The instance has not been created.");
        }
        return instance;
    }

    /**
     * Asynchronous remove branch
     *
     * @param globalSession the globalSession
     * @param branchSession the branchSession
     */
    public void doBranchRemoveAsync(GlobalSession globalSession, BranchSession branchSession) {
        if (globalSession == null) {
            return;
        }
        branchRemoveExecutor.execute(new BranchRemoveTask(globalSession, branchSession));
    }

    /**
     * Asynchronous remove all branch
     *
     * @param globalSession the globalSession
     */
    public void doBranchRemoveAllAsync(GlobalSession globalSession) {
        if (globalSession == null) {
            return;
        }
        branchRemoveExecutor.execute(new BranchRemoveTask(globalSession));
    }

    @Override
    protected void doGlobalBegin(GlobalBeginRequest request, GlobalBeginResponse response, RpcContext rpcContext)
            throws TransactionException {
        response.setXid(core.begin(rpcContext.getApplicationId(), rpcContext.getTransactionServiceGroup(),
                request.getTransactionName(), request.getTimeout()));
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Begin new global transaction applicationId: {},transactionServiceGroup: {}, transactionName: {},timeout:{},xid:{}",
                    rpcContext.getApplicationId(), rpcContext.getTransactionServiceGroup(), request.getTransactionName(), request.getTimeout(), response.getXid());
        }
    }

    @Override
    protected void doGlobalCommit(GlobalCommitRequest request, GlobalCommitResponse response, RpcContext rpcContext)
            throws TransactionException {
        MDC.put(RootContext.MDC_KEY_XID, request.getXid());
        response.setGlobalStatus(core.commit(request.getXid()));
    }

    @Override
    protected void doGlobalRollback(GlobalRollbackRequest request, GlobalRollbackResponse response,
                                    RpcContext rpcContext) throws TransactionException {
        MDC.put(RootContext.MDC_KEY_XID, request.getXid());
        response.setGlobalStatus(core.rollback(request.getXid()));
    }

    @Override
    protected void doGlobalStatus(GlobalStatusRequest request, GlobalStatusResponse response, RpcContext rpcContext)
            throws TransactionException {
        MDC.put(RootContext.MDC_KEY_XID, request.getXid());
        response.setGlobalStatus(core.getStatus(request.getXid()));
    }

    @Override
    protected void doGlobalReport(GlobalReportRequest request, GlobalReportResponse response, RpcContext rpcContext)
            throws TransactionException {
        MDC.put(RootContext.MDC_KEY_XID, request.getXid());
        response.setGlobalStatus(core.globalReport(request.getXid(), request.getGlobalStatus()));
    }

    @Override
    protected void doBranchRegister(BranchRegisterRequest request, BranchRegisterResponse response,
                                    RpcContext rpcContext) throws TransactionException {
        MDC.put(RootContext.MDC_KEY_XID, request.getXid());
        response.setBranchId(
                core.branchRegister(request.getBranchType(), request.getResourceId(), rpcContext.getClientId(),
                        request.getXid(), request.getApplicationData(), request.getLockKey()));
    }

    @Override
    protected void doBranchReport(BranchReportRequest request, BranchReportResponse response, RpcContext rpcContext)
            throws TransactionException {
        MDC.put(RootContext.MDC_KEY_XID, request.getXid());
        MDC.put(RootContext.MDC_KEY_BRANCH_ID, String.valueOf(request.getBranchId()));
        core.branchReport(request.getBranchType(), request.getXid(), request.getBranchId(), request.getStatus(),
                request.getApplicationData());
    }

    @Override
    protected void doLockCheck(GlobalLockQueryRequest request, GlobalLockQueryResponse response, RpcContext rpcContext)
            throws TransactionException {
        MDC.put(RootContext.MDC_KEY_XID, request.getXid());
        response.setLockable(
                core.lockQuery(request.getBranchType(), request.getResourceId(), request.getXid(), request.getLockKey()));
    }

    /**
     * Timeout check.
     */
    protected void timeoutCheck() {
        SessionCondition sessionCondition = new SessionCondition(GlobalStatus.Begin);
        sessionCondition.setLazyLoadBranch(true);
        Collection<GlobalSession> beginGlobalsessions =
            SessionHolder.getRootSessionManager().findGlobalSessions(sessionCondition);
        if (CollectionUtils.isEmpty(beginGlobalsessions)) {
            return;
        }
        if (!beginGlobalsessions.isEmpty() && LOGGER.isDebugEnabled()) {
            LOGGER.debug("Global transaction timeout check begin, size: {}", beginGlobalsessions.size());
        }
        SessionHelper.forEach(beginGlobalsessions, globalSession -> {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        globalSession.getXid() + " " + globalSession.getStatus() + " " + globalSession.getBeginTime() + " "
                                + globalSession.getTimeout());
            }
            SessionHolder.lockAndExecute(globalSession, () -> {
                if (globalSession.getStatus() != GlobalStatus.Begin || !globalSession.isTimeout()) {
                    return false;
                }

                LOGGER.info("Global transaction[{}] is timeout and will be rollback.", globalSession.getXid());

                globalSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
                globalSession.close();
                globalSession.setStatus(GlobalStatus.TimeoutRollbacking);

                globalSession.addSessionLifecycleListener(SessionHolder.getRetryRollbackingSessionManager());
                SessionHolder.getRetryRollbackingSessionManager().addGlobalSession(globalSession);

                // transaction timeout and start rollbacking event
                MetricsPublisher.postSessionDoingEvent(globalSession, GlobalStatus.TimeoutRollbacking.name(), false, false);

                return true;
            });
        });
        if (!beginGlobalsessions.isEmpty() && LOGGER.isDebugEnabled()) {
            LOGGER.debug("Global transaction timeout check end. ");
        }

    }


    /**
     * Handle retry rollbacking.
     */
    protected void handleRetryRollbacking() {
        SessionCondition sessionCondition = new SessionCondition(rollbackingStatuses);
        sessionCondition.setLazyLoadBranch(true);
        Collection<GlobalSession> rollbackingSessions =
            SessionHolder.getRetryRollbackingSessionManager().findGlobalSessions(sessionCondition);
        if (CollectionUtils.isEmpty(rollbackingSessions)) {
            return;
        }
        long now = System.currentTimeMillis();
        SessionHelper.forEach(rollbackingSessions, rollbackingSession -> {
            try {
                // prevent repeated rollback
                if (rollbackingSession.getStatus().equals(GlobalStatus.Rollbacking)
                    && !rollbackingSession.isDeadSession()) {
                    // The function of this 'return' is 'continue'.
                    return;
                }
                if (isRetryTimeout(now, MAX_ROLLBACK_RETRY_TIMEOUT.toMillis(), rollbackingSession.getBeginTime())) {
                    if (ROLLBACK_RETRY_TIMEOUT_UNLOCK_ENABLE) {
                        rollbackingSession.clean();
                    }
                    // Prevent thread safety issues
                    SessionHolder.getRetryRollbackingSessionManager().removeGlobalSession(rollbackingSession);
                    LOGGER.error("Global transaction rollback retry timeout and has removed [{}]", rollbackingSession.getXid());

                    SessionHelper.endRollbackFailed(rollbackingSession, true);

                    // rollback retry timeout event
                    MetricsPublisher.postSessionDoneEvent(rollbackingSession, GlobalStatus.RollbackRetryTimeout, true, false);

                    //The function of this 'return' is 'continue'.
                    return;
                }
                rollbackingSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
                core.doGlobalRollback(rollbackingSession, true);
            } catch (TransactionException ex) {
                LOGGER.info("Failed to retry rollbacking [{}] {} {}", rollbackingSession.getXid(), ex.getCode(), ex.getMessage());
            }
        });
    }

    /**
     * Handle retry committing.
     */
    protected void handleRetryCommitting() {
        SessionCondition retryCommittingSessionCondition = new SessionCondition(retryCommittingStatuses);
        retryCommittingSessionCondition.setLazyLoadBranch(true);
        Collection<GlobalSession> committingSessions =
            SessionHolder.getRetryCommittingSessionManager().findGlobalSessions(retryCommittingSessionCondition);
        if (CollectionUtils.isEmpty(committingSessions)) {
            return;
        }
        long now = System.currentTimeMillis();
        SessionHelper.forEach(committingSessions, committingSession -> {
            try {
                // prevent repeated commit
                if (committingSession.getStatus().equals(GlobalStatus.Committing)
                    && !committingSession.isDeadSession()) {
                    // The function of this 'return' is 'continue'.
                    return;
                }
                if (isRetryTimeout(now, MAX_COMMIT_RETRY_TIMEOUT.toMillis(), committingSession.getBeginTime())) {
                    // Prevent thread safety issues
                    SessionHolder.getRetryCommittingSessionManager().removeGlobalSession(committingSession);
                    LOGGER.error("Global transaction commit retry timeout and has removed [{}]", committingSession.getXid());

                    // commit retry timeout event
                    MetricsPublisher.postSessionDoneEvent(committingSession, GlobalStatus.CommitRetryTimeout, true, false);

                    //The function of this 'return' is 'continue'.
                    return;
                }
                committingSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
                core.doGlobalCommit(committingSession, true);
            } catch (TransactionException ex) {
                LOGGER.info("Failed to retry committing [{}] {} {}", committingSession.getXid(), ex.getCode(), ex.getMessage());
            }
        });
    }

    /**
     * Handle async committing.
     */
    protected void handleAsyncCommitting() {
        SessionCondition sessionCondition = new SessionCondition(GlobalStatus.AsyncCommitting);
        Collection<GlobalSession> asyncCommittingSessions =
                SessionHolder.getAsyncCommittingSessionManager().findGlobalSessions(sessionCondition);
        if (CollectionUtils.isEmpty(asyncCommittingSessions)) {
            return;
        }
        SessionHelper.forEach(asyncCommittingSessions, asyncCommittingSession -> {
            try {
                asyncCommittingSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
                core.doGlobalCommit(asyncCommittingSession, true);
            } catch (TransactionException ex) {
                LOGGER.error("Failed to async committing [{}] {} {}", asyncCommittingSession.getXid(), ex.getCode(), ex.getMessage(), ex);
            }
        });
    }

    /**
     * Undo log delete.
     */
    protected void undoLogDelete() {
        Map<String, Channel> rmChannels = ChannelManager.getRmChannels();
        if (rmChannels == null || rmChannels.isEmpty()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("no active rm channels to delete undo log");
            }
            return;
        }
        short saveDays = CONFIG.getShort(ConfigurationKeys.TRANSACTION_UNDO_LOG_SAVE_DAYS,
                UndoLogDeleteRequest.DEFAULT_SAVE_DAYS);
        for (Map.Entry<String, Channel> channelEntry : rmChannels.entrySet()) {
            String resourceId = channelEntry.getKey();
            UndoLogDeleteRequest deleteRequest = new UndoLogDeleteRequest();
            deleteRequest.setResourceId(resourceId);
            deleteRequest.setSaveDays(saveDays > 0 ? saveDays : UndoLogDeleteRequest.DEFAULT_SAVE_DAYS);
            try {
                remotingServer.sendAsyncRequest(channelEntry.getValue(), deleteRequest);
            } catch (Exception e) {
                LOGGER.error("Failed to async delete undo log resourceId = {}, exception: {}", resourceId, e.getMessage());
            }
        }
    }

    private boolean isRetryTimeout(long now, long timeout, long beginTime) {
        return timeout >= ALWAYS_RETRY_BOUNDARY && now - beginTime > timeout;
    }

    /**
     * Init.
     */
    public void init() {
        retryRollbacking.scheduleAtFixedRate(
            () -> SessionHolder.distributedLockAndExecute(RETRY_ROLLBACKING, this::handleRetryRollbacking), 0,
            ROLLBACKING_RETRY_PERIOD, TimeUnit.MILLISECONDS);

        retryCommitting.scheduleAtFixedRate(
            () -> SessionHolder.distributedLockAndExecute(RETRY_COMMITTING, this::handleRetryCommitting), 0,
            COMMITTING_RETRY_PERIOD, TimeUnit.MILLISECONDS);

        asyncCommitting.scheduleAtFixedRate(
            () -> SessionHolder.distributedLockAndExecute(ASYNC_COMMITTING, this::handleAsyncCommitting), 0,
            ASYNC_COMMITTING_RETRY_PERIOD, TimeUnit.MILLISECONDS);

        timeoutCheck.scheduleAtFixedRate(
            () -> SessionHolder.distributedLockAndExecute(TX_TIMEOUT_CHECK, this::timeoutCheck), 0,
            TIMEOUT_RETRY_PERIOD, TimeUnit.MILLISECONDS);

        undoLogDelete.scheduleAtFixedRate(
            () -> SessionHolder.distributedLockAndExecute(UNDOLOG_DELETE, this::undoLogDelete),
            UNDO_LOG_DELAY_DELETE_PERIOD, UNDO_LOG_DELETE_PERIOD, TimeUnit.MILLISECONDS);
    }

    @Override
    public AbstractResultMessage onRequest(AbstractMessage request, RpcContext context) {
        if (!(request instanceof AbstractTransactionRequestToTC)) {
            throw new IllegalArgumentException();
        }
        AbstractTransactionRequestToTC transactionRequest = (AbstractTransactionRequestToTC) request;
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
        undoLogDelete.shutdown();
        branchRemoveExecutor.shutdown();
        try {
            retryRollbacking.awaitTermination(TIMED_TASK_SHUTDOWN_MAX_WAIT_MILLS, TimeUnit.MILLISECONDS);
            retryCommitting.awaitTermination(TIMED_TASK_SHUTDOWN_MAX_WAIT_MILLS, TimeUnit.MILLISECONDS);
            asyncCommitting.awaitTermination(TIMED_TASK_SHUTDOWN_MAX_WAIT_MILLS, TimeUnit.MILLISECONDS);
            timeoutCheck.awaitTermination(TIMED_TASK_SHUTDOWN_MAX_WAIT_MILLS, TimeUnit.MILLISECONDS);
            undoLogDelete.awaitTermination(TIMED_TASK_SHUTDOWN_MAX_WAIT_MILLS, TimeUnit.MILLISECONDS);
            branchRemoveExecutor.awaitTermination(TIMED_TASK_SHUTDOWN_MAX_WAIT_MILLS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignore) {

        }
        // 2. second close netty flow
        if (remotingServer instanceof NettyRemotingServer) {
            ((NettyRemotingServer) remotingServer).destroy();
        }
        // 3. third destroy SessionHolder
        SessionHolder.destroy();
        instance = null;
    }

    /**
     * only used for mock test
     * @param remotingServer
     */
    public void setRemotingServer(RemotingServer remotingServer) {
        this.remotingServer = remotingServer;
    }

    /**
     * the task to remove branchSession
     */
    static class BranchRemoveTask implements Runnable {

        /**
         * the globalSession
         */
        private final GlobalSession globalSession;

        /**
         * the branchSession
         */
        private final BranchSession branchSession;

        /**
         * If you use this construct, the task will remove the branchSession provided by the parameter
         * @param globalSession the globalSession
         */
        public BranchRemoveTask(GlobalSession globalSession, BranchSession branchSession) {
            this.globalSession = globalSession;
            this.branchSession = branchSession;
        }

        /**
         * If you use this construct, the task will remove all branchSession
         * @param globalSession the globalSession
         */
        public BranchRemoveTask(GlobalSession globalSession) {
            this.globalSession = globalSession;
            this.branchSession = null;
        }

        @Override
        public void run() {
            if (globalSession == null) {
                return;
            }
            try {
                MDC.put(RootContext.MDC_KEY_XID, globalSession.getXid());
                if (branchSession != null) {
                    doRemove(branchSession);
                } else {
                    globalSession.getSortedBranches().forEach(this::doRemove);
                }
            } catch (Exception unKnowException) {
                LOGGER.error("Asynchronous delete branchSession error, xid = {}", globalSession.getXid(), unKnowException);
            } finally {
                MDC.remove(RootContext.MDC_KEY_XID);
            }
        }

        private void doRemove(BranchSession bt) {
            try {
                MDC.put(RootContext.MDC_KEY_BRANCH_ID, String.valueOf(bt.getBranchId()));
                globalSession.removeBranch(bt);
                LOGGER.info("Asynchronous delete branchSession successfully, xid = {}, branchId = {}",
                        globalSession.getXid(), bt.getBranchId());
            } catch (TransactionException transactionException) {
                LOGGER.error("Asynchronous delete branchSession error, xid = {}, branchId = {}",
                        globalSession.getXid(), bt.getBranchId(), transactionException);
            } finally {
                MDC.remove(RootContext.MDC_KEY_BRANCH_ID);
            }
        }
    }
}
