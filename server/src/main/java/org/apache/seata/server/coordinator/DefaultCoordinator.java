/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.server.coordinator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.netty.channel.Channel;
import org.apache.seata.common.DefaultValues;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.protocol.AbstractMessage;
import org.apache.seata.core.protocol.AbstractResultMessage;
import org.apache.seata.core.protocol.transaction.AbstractTransactionRequestToTC;
import org.apache.seata.core.protocol.transaction.AbstractTransactionResponse;
import org.apache.seata.core.protocol.transaction.BranchRegisterRequest;
import org.apache.seata.core.protocol.transaction.BranchRegisterResponse;
import org.apache.seata.core.protocol.transaction.BranchReportRequest;
import org.apache.seata.core.protocol.transaction.BranchReportResponse;
import org.apache.seata.core.protocol.transaction.GlobalBeginRequest;
import org.apache.seata.core.protocol.transaction.GlobalBeginResponse;
import org.apache.seata.core.protocol.transaction.GlobalCommitRequest;
import org.apache.seata.core.protocol.transaction.GlobalCommitResponse;
import org.apache.seata.core.protocol.transaction.GlobalLockQueryRequest;
import org.apache.seata.core.protocol.transaction.GlobalLockQueryResponse;
import org.apache.seata.core.protocol.transaction.GlobalReportRequest;
import org.apache.seata.core.protocol.transaction.GlobalReportResponse;
import org.apache.seata.core.protocol.transaction.GlobalRollbackRequest;
import org.apache.seata.core.protocol.transaction.GlobalRollbackResponse;
import org.apache.seata.core.protocol.transaction.GlobalStatusRequest;
import org.apache.seata.core.protocol.transaction.GlobalStatusResponse;
import org.apache.seata.core.protocol.transaction.UndoLogDeleteRequest;
import org.apache.seata.core.rpc.Disposable;
import org.apache.seata.core.rpc.RemotingServer;
import org.apache.seata.core.rpc.RpcContext;
import org.apache.seata.core.rpc.TransactionMessageHandler;
import org.apache.seata.core.rpc.netty.ChannelManager;
import org.apache.seata.core.rpc.netty.NettyRemotingServer;
import org.apache.seata.server.AbstractTCInboundHandler;
import org.apache.seata.server.metrics.MetricsPublisher;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionCondition;
import org.apache.seata.server.session.SessionHelper;
import org.apache.seata.server.session.SessionHolder;
import org.apache.seata.server.store.StoreConfig;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static org.apache.seata.common.Constants.ASYNC_COMMITTING;
import static org.apache.seata.common.Constants.COMMITTING;
import static org.apache.seata.common.Constants.RETRY_COMMITTING;
import static org.apache.seata.common.Constants.RETRY_ROLLBACKING;
import static org.apache.seata.common.Constants.ROLLBACKING;
import static org.apache.seata.common.Constants.SYNC_PROCESSING;
import static org.apache.seata.common.Constants.TX_TIMEOUT_CHECK;
import static org.apache.seata.common.Constants.UNDOLOG_DELETE;
import static org.apache.seata.common.DefaultValues.DEFAULT_ASYNC_COMMITTING_RETRY_PERIOD;
import static org.apache.seata.common.DefaultValues.DEFAULT_COMMITING_RETRY_PERIOD;
import static org.apache.seata.common.DefaultValues.DEFAULT_ENABLE_BRANCH_ASYNC_REMOVE;
import static org.apache.seata.common.DefaultValues.DEFAULT_MAX_COMMIT_RETRY_TIMEOUT;
import static org.apache.seata.common.DefaultValues.DEFAULT_MAX_ROLLBACK_RETRY_TIMEOUT;
import static org.apache.seata.common.DefaultValues.DEFAULT_ROLLBACKING_RETRY_PERIOD;
import static org.apache.seata.common.DefaultValues.DEFAULT_ROLLBACK_RETRY_TIMEOUT_UNLOCK_ENABLE;
import static org.apache.seata.common.DefaultValues.DEFAULT_TIMEOUT_RETRY_PERIOD;
import static org.apache.seata.common.DefaultValues.DEFAULT_UNDO_LOG_DELETE_PERIOD;

/**
 * The type Default coordinator.
 */
public class DefaultCoordinator extends AbstractTCInboundHandler implements TransactionMessageHandler, Disposable {

    protected static final Logger LOGGER = LoggerFactory.getLogger(DefaultCoordinator.class);

    private static final int TIMED_TASK_SHUTDOWN_MAX_WAIT_MILLS = 5000;

    private static final String TIME_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";

    /**
     * The constant COMMITTING_RETRY_PERIOD.
     */
    protected static final long COMMITTING_RETRY_PERIOD = CONFIG.getLong(ConfigurationKeys.COMMITING_RETRY_PERIOD,
            DEFAULT_COMMITING_RETRY_PERIOD);

    /**
     * The constant ASYNC_COMMITTING_RETRY_PERIOD.
     */
    protected static final long ASYNC_COMMITTING_RETRY_PERIOD = CONFIG.getLong(
            ConfigurationKeys.ASYNC_COMMITING_RETRY_PERIOD, DEFAULT_ASYNC_COMMITTING_RETRY_PERIOD);

    /**
     * The constant ROLLBACKING_RETRY_PERIOD.
     */
    protected static final long ROLLBACKING_RETRY_PERIOD = CONFIG.getLong(ConfigurationKeys.ROLLBACKING_RETRY_PERIOD,
            DEFAULT_ROLLBACKING_RETRY_PERIOD);

    /**
     * The constant TIMEOUT_RETRY_PERIOD.
     */
    protected static final long TIMEOUT_RETRY_PERIOD = CONFIG.getLong(ConfigurationKeys.TIMEOUT_RETRY_PERIOD,
            DEFAULT_TIMEOUT_RETRY_PERIOD);

    /**
     * The Transaction undo log delete period.
     */
    protected static final long UNDO_LOG_DELETE_PERIOD = CONFIG.getLong(
            ConfigurationKeys.TRANSACTION_UNDO_LOG_DELETE_PERIOD, DEFAULT_UNDO_LOG_DELETE_PERIOD);

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
    private static final int BRANCH_ASYNC_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;

    private static final long MAX_COMMIT_RETRY_TIMEOUT = ConfigurationFactory.getInstance().getLong(
            ConfigurationKeys.MAX_COMMIT_RETRY_TIMEOUT, DEFAULT_MAX_COMMIT_RETRY_TIMEOUT);

    private static final long MAX_ROLLBACK_RETRY_TIMEOUT = ConfigurationFactory.getInstance().getLong(
            ConfigurationKeys.MAX_ROLLBACK_RETRY_TIMEOUT, DEFAULT_MAX_ROLLBACK_RETRY_TIMEOUT);

    private static final boolean ROLLBACK_RETRY_TIMEOUT_UNLOCK_ENABLE = ConfigurationFactory.getInstance().getBoolean(
            ConfigurationKeys.ROLLBACK_RETRY_TIMEOUT_UNLOCK_ENABLE, DEFAULT_ROLLBACK_RETRY_TIMEOUT_UNLOCK_ENABLE);

    private static final int RETRY_DEAD_THRESHOLD = ConfigurationFactory.getInstance()
        .getInt(org.apache.seata.common.ConfigurationKeys.RETRY_DEAD_THRESHOLD, DefaultValues.DEFAULT_RETRY_DEAD_THRESHOLD);

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

    private final ScheduledThreadPoolExecutor syncProcessing =
        new ScheduledThreadPoolExecutor(1, new NamedThreadFactory(SYNC_PROCESSING, 1));

    private final GlobalStatus[] retryRollbackingStatuses = new GlobalStatus[] {
        GlobalStatus.TimeoutRollbacking,
        GlobalStatus.TimeoutRollbackRetrying, GlobalStatus.RollbackRetrying};

    private final GlobalStatus[] retryCommittingStatuses = new GlobalStatus[] {GlobalStatus.CommitRetrying, GlobalStatus.Committed};

    private final GlobalStatus[] rollbackingStatuses = new GlobalStatus[] {GlobalStatus.Rollbacking};
    private final GlobalStatus[] committingStatuses = new GlobalStatus[] {GlobalStatus.Committing};

    private final ThreadPoolExecutor branchRemoveExecutor;

    private RemotingServer remotingServer;

    private final DefaultCore core;

    private static volatile DefaultCoordinator instance;

    /**
     * Instantiates a new Default coordinator.
     *
     * @param remotingServer the remoting server
     */
    protected DefaultCoordinator(RemotingServer remotingServer) {
        if (remotingServer == null) {
            throw new IllegalArgumentException("RemotingServer not allowed be null.");
        }
        this.remotingServer = remotingServer;
        this.core = new DefaultCore(remotingServer);
        boolean enableBranchAsyncRemove = CONFIG.getBoolean(
                ConfigurationKeys.ENABLE_BRANCH_ASYNC_REMOVE, DEFAULT_ENABLE_BRANCH_ASYNC_REMOVE);
        // create branchRemoveExecutor
        if (enableBranchAsyncRemove && StoreConfig.getSessionMode() != StoreConfig.SessionMode.FILE) {
            branchRemoveExecutor = new ThreadPoolExecutor(BRANCH_ASYNC_POOL_SIZE, BRANCH_ASYNC_POOL_SIZE,
                    Integer.MAX_VALUE, TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(
                            CONFIG.getInt(ConfigurationKeys.SESSION_BRANCH_ASYNC_QUEUE_SIZE, DEFAULT_BRANCH_ASYNC_QUEUE_SIZE)
                    ), new NamedThreadFactory("branchSessionRemove", BRANCH_ASYNC_POOL_SIZE),
                    new ThreadPoolExecutor.CallerRunsPolicy());
        } else {
            branchRemoveExecutor = null;
        }
    }

    public static DefaultCoordinator getInstance(RemotingServer remotingServer) {
        if (null == instance) {
            synchronized (DefaultCoordinator.class) {
                if (null == instance) {
                    StoreConfig.SessionMode storeMode = StoreConfig.getSessionMode();
                    instance = Objects.equals(StoreConfig.SessionMode.RAFT, storeMode)
                        ? new RaftCoordinator(remotingServer) : new DefaultCoordinator(remotingServer);
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

                LOGGER.warn("Global transaction[{}] is timeout and will be rollback,transaction begin time:{} and now:{}", globalSession.getXid(),
                    DateFormatUtils.format(globalSession.getBeginTime(), TIME_FORMAT_PATTERN), DateFormatUtils.format(System.currentTimeMillis(), TIME_FORMAT_PATTERN));

                globalSession.close();
                globalSession.changeGlobalStatus(GlobalStatus.TimeoutRollbacking);

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
        SessionCondition sessionCondition = new SessionCondition(retryRollbackingStatuses);
        sessionCondition.setLazyLoadBranch(true);
        Collection<GlobalSession> rollbackingSessions =
            SessionHolder.getRootSessionManager().findGlobalSessions(sessionCondition);
        if (CollectionUtils.isEmpty(rollbackingSessions)) {
            return;
        }
        long now = System.currentTimeMillis();
        SessionHelper.forEach(rollbackingSessions, rollbackingSession -> {
            try {
                if (isRetryTimeout(now, MAX_ROLLBACK_RETRY_TIMEOUT, rollbackingSession.getBeginTime())) {
                    if (ROLLBACK_RETRY_TIMEOUT_UNLOCK_ENABLE) {
                        rollbackingSession.clean();
                    }

                    SessionHelper.endRollbackFailed(rollbackingSession, true, true);

                    //The function of this 'return' is 'continue'.
                    return;
                }
                core.doGlobalRollback(rollbackingSession, true);
            } catch (TransactionException ex) {
                LOGGER.error("Failed to retry rollbacking [{}] {} {}", rollbackingSession.getXid(), ex.getCode(), ex.getMessage());
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
            SessionHolder.getRootSessionManager().findGlobalSessions(retryCommittingSessionCondition);
        if (CollectionUtils.isEmpty(committingSessions)) {
            return;
        }
        long now = System.currentTimeMillis();
        SessionHelper.forEach(committingSessions, committingSession -> {
            try {
                if (isRetryTimeout(now, MAX_COMMIT_RETRY_TIMEOUT, committingSession.getBeginTime())) {

                    // commit retry timeout event
                    SessionHelper.endCommitFailed(committingSession, true, true);

                    //The function of this 'return' is 'continue'.
                    return;
                }
                if (GlobalStatus.Committed.equals(committingSession.getStatus())
                    && committingSession.getBranchSessions().isEmpty()) {
                    SessionHelper.endCommitted(committingSession,true);
                }
                core.doGlobalCommit(committingSession, true);
            } catch (TransactionException ex) {
                LOGGER.error("Failed to retry committing [{}] {} {}", committingSession.getXid(), ex.getCode(), ex.getMessage());
            }
        });
    }

    /**
     * Handle async committing.
     */
    protected void handleAsyncCommitting() {
        SessionCondition sessionCondition = new SessionCondition(GlobalStatus.AsyncCommitting);
        Collection<GlobalSession> asyncCommittingSessions =
                SessionHolder.getRootSessionManager().findGlobalSessions(sessionCondition);
        if (CollectionUtils.isEmpty(asyncCommittingSessions)) {
            return;
        }
        SessionHelper.forEach(asyncCommittingSessions, asyncCommittingSession -> {
            try {
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
     * Handle rollbacking by scheduled.
     */
    protected void handleRollbackingByScheduled() {
        SessionCondition sessionCondition = new SessionCondition(rollbackingStatuses);
        sessionCondition.setLazyLoadBranch(true);
        List<GlobalSession> rollbackingSessions =
            SessionHolder.getRootSessionManager().findGlobalSessions(sessionCondition);
        if (CollectionUtils.isEmpty(rollbackingSessions)) {
            rollbackingSchedule(RETRY_DEAD_THRESHOLD);
            return;
        }
        long delay = ROLLBACKING_RETRY_PERIOD;
        rollbackingSessions.sort(Comparator.comparingLong(GlobalSession::getBeginTime));
        List<GlobalSession> needDoRollbackingSessions = new ArrayList<>();
        for (GlobalSession rollbackingSession : rollbackingSessions) {
            long time = rollbackingSession.timeToDeadSession();
            if (time <= 0) {
                needDoRollbackingSessions.add(rollbackingSession);
            } else {
                delay = Math.max(time, ROLLBACKING_RETRY_PERIOD);
                break;
            }
        }
        long now = System.currentTimeMillis();
        SessionHelper.forEach(needDoRollbackingSessions, rollbackingSession -> {
            try {
                if (isRetryTimeout(now, MAX_ROLLBACK_RETRY_TIMEOUT, rollbackingSession.getBeginTime())) {
                    if (ROLLBACK_RETRY_TIMEOUT_UNLOCK_ENABLE) {
                        rollbackingSession.clean();
                    }

                    SessionHelper.endRollbackFailed(rollbackingSession, true, true);

                    //The function of this 'return' is 'continue'.
                    return;
                }
                core.doGlobalRollback(rollbackingSession, true);
            } catch (TransactionException ex) {
                LOGGER.error("Failed to handle rollbacking [{}] {} {}", rollbackingSession.getXid(), ex.getCode(), ex.getMessage());
            }
        });
        rollbackingSchedule(delay);
    }

    private void rollbackingSchedule(long delay) {
        syncProcessing.schedule(
            () -> {
                boolean called = SessionHolder.distributedLockAndExecute(ROLLBACKING, this::handleRollbackingByScheduled);
                if (!called) {
                    rollbackingSchedule(ROLLBACKING_RETRY_PERIOD);
                }
            },
            delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Handle committing by scheduled.
     */
    protected void handleCommittingByScheduled() {
        SessionCondition sessionCondition = new SessionCondition(committingStatuses);
        sessionCondition.setLazyLoadBranch(true);
        List<GlobalSession> committingSessions =
            SessionHolder.getRootSessionManager().findGlobalSessions(sessionCondition);
        if (CollectionUtils.isEmpty(committingSessions)) {
            committingSchedule(RETRY_DEAD_THRESHOLD);
            return;
        }
        long delay = COMMITTING_RETRY_PERIOD;
        committingSessions.sort(Comparator.comparingLong(GlobalSession::getBeginTime));
        List<GlobalSession> needDoCommittingSessions = new ArrayList<>();
        for (GlobalSession committingSession : committingSessions) {
            long time = committingSession.timeToDeadSession();
            if (time <= 0) {
                needDoCommittingSessions.add(committingSession);
            } else {
                delay = Math.max(time, COMMITTING_RETRY_PERIOD);
                break;
            }
        }
        long now = System.currentTimeMillis();
        SessionHelper.forEach(needDoCommittingSessions, committingSession -> {
            try {
                if (isRetryTimeout(now, MAX_COMMIT_RETRY_TIMEOUT, committingSession.getBeginTime())) {

                    // commit retry timeout event
                    SessionHelper.endCommitFailed(committingSession, true, true);

                    //The function of this 'return' is 'continue'.
                    return;
                }
                core.doGlobalCommit(committingSession, true);
            } catch (TransactionException ex) {
                LOGGER.error("Failed to handle committing [{}] {} {}", committingSession.getXid(), ex.getCode(), ex.getMessage());
            }
        });
        committingSchedule(delay);
    }

    private void committingSchedule(long delay) {
        syncProcessing.schedule(
            () -> {
                boolean called = SessionHolder.distributedLockAndExecute(COMMITTING, this::handleCommittingByScheduled);
                if (!called) {
                    committingSchedule(COMMITTING_RETRY_PERIOD);
                }
            },
            delay, TimeUnit.MILLISECONDS);
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

        rollbackingSchedule(0);

        committingSchedule(0);
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
        if (branchRemoveExecutor != null) {
            branchRemoveExecutor.shutdown();
        }
        try {
            retryRollbacking.awaitTermination(TIMED_TASK_SHUTDOWN_MAX_WAIT_MILLS, TimeUnit.MILLISECONDS);
            retryCommitting.awaitTermination(TIMED_TASK_SHUTDOWN_MAX_WAIT_MILLS, TimeUnit.MILLISECONDS);
            asyncCommitting.awaitTermination(TIMED_TASK_SHUTDOWN_MAX_WAIT_MILLS, TimeUnit.MILLISECONDS);
            timeoutCheck.awaitTermination(TIMED_TASK_SHUTDOWN_MAX_WAIT_MILLS, TimeUnit.MILLISECONDS);
            undoLogDelete.awaitTermination(TIMED_TASK_SHUTDOWN_MAX_WAIT_MILLS, TimeUnit.MILLISECONDS);
            if (branchRemoveExecutor != null) {
                branchRemoveExecutor.awaitTermination(TIMED_TASK_SHUTDOWN_MAX_WAIT_MILLS, TimeUnit.MILLISECONDS);
            }
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
            if (branchSession == null) {
                throw new IllegalArgumentException("BranchSession can`t be null!");
            }
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
                    globalSession.getSortedBranches().parallelStream().forEach(this::doRemove);
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
