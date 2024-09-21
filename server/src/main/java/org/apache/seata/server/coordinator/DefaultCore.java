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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.seata.common.DefaultValues;
import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.logger.StackTraceLogger;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.rpc.RemotingServer;
import org.apache.seata.server.metrics.MetricsPublisher;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionHelper;
import org.apache.seata.server.session.SessionHolder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static org.apache.seata.common.ConfigurationKeys.ENABLE_PARALLEL_HANDLE_BRANCH_KEY;
import static org.apache.seata.common.ConfigurationKeys.XAER_NOTA_RETRY_TIMEOUT;
import static org.apache.seata.server.session.BranchSessionHandler.CONTINUE;

/**
 * The type Default core.
 *
 */
public class DefaultCore implements Core {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCore.class);

    private static final int RETRY_XAER_NOTA_TIMEOUT = ConfigurationFactory.getInstance().getInt(XAER_NOTA_RETRY_TIMEOUT,
            DefaultValues.DEFAULT_XAER_NOTA_RETRY_TIMEOUT);

    private static Map<BranchType, AbstractCore> coreMap = new ConcurrentHashMap<>();

    private static final boolean PARALLEL_HANDLE_BRANCH =
            ConfigurationFactory.getInstance().getBoolean(ENABLE_PARALLEL_HANDLE_BRANCH_KEY, false);

    /**
     * get the Default core.
     *
     * @param remotingServer the remoting server
     */
    public DefaultCore(RemotingServer remotingServer) {
        List<AbstractCore> allCore = EnhancedServiceLoader.loadAll(AbstractCore.class,
            new Class[] {RemotingServer.class}, new Object[] {remotingServer});
        if (CollectionUtils.isNotEmpty(allCore)) {
            for (AbstractCore core : allCore) {
                coreMap.put(core.getHandleBranchType(), core);
            }
        }
    }

    /**
     * get core
     *
     * @param branchType the branchType
     * @return the core
     */
    public AbstractCore getCore(BranchType branchType) {
        AbstractCore core = coreMap.get(branchType);
        if (core == null) {
            throw new NotSupportYetException("unsupported type:" + branchType.name());
        }
        return core;
    }

    /**
     * only for mock
     *
     * @param branchType the branchType
     * @param core       the core
     */
    public void mockCore(BranchType branchType, AbstractCore core) {
        coreMap.put(branchType, core);
    }

    @Override
    public Long branchRegister(BranchType branchType, String resourceId, String clientId, String xid,
                               String applicationData, String lockKeys) throws TransactionException {
        return getCore(branchType).branchRegister(branchType, resourceId, clientId, xid,
            applicationData, lockKeys);
    }

    @Override
    public void branchReport(BranchType branchType, String xid, long branchId, BranchStatus status,
                             String applicationData) throws TransactionException {
        getCore(branchType).branchReport(branchType, xid, branchId, status, applicationData);
    }

    @Override
    public boolean lockQuery(BranchType branchType, String resourceId, String xid, String lockKeys)
        throws TransactionException {
        return getCore(branchType).lockQuery(branchType, resourceId, xid, lockKeys);
    }

    @Override
    public BranchStatus branchCommit(GlobalSession globalSession, BranchSession branchSession) throws TransactionException {
        return getCore(branchSession.getBranchType()).branchCommit(globalSession, branchSession);
    }

    @Override
    public BranchStatus branchRollback(GlobalSession globalSession, BranchSession branchSession) throws TransactionException {
        return getCore(branchSession.getBranchType()).branchRollback(globalSession, branchSession);
    }

    @Override
    public String begin(String applicationId, String transactionServiceGroup, String name, int timeout)
        throws TransactionException {
        GlobalSession session = GlobalSession.createGlobalSession(applicationId, transactionServiceGroup, name, timeout);
        MDC.put(RootContext.MDC_KEY_XID, session.getXid());

        session.begin();

        // transaction start event
        MetricsPublisher.postSessionDoingEvent(session, false);

        return session.getXid();
    }



    @Override
    public GlobalStatus commit(String xid) throws TransactionException {
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        if (globalSession == null) {
            return GlobalStatus.Finished;
        }

        if (globalSession.isTimeout()) {
            LOGGER.info("TC detected timeout, xid = {}", globalSession.getXid());
            return GlobalStatus.TimeoutRollbacking;
        }

        // just lock changeStatus
        boolean shouldCommit = SessionHolder.lockAndExecute(globalSession, () -> {
            boolean shouldCommitNow = false;
            if (globalSession.getStatus() == GlobalStatus.Begin) {
                // Highlight: Firstly, close the session, then no more branch can be registered.
                globalSession.close();
                if (globalSession.canBeCommittedAsync()) {
                    globalSession.asyncCommit();
                    MetricsPublisher.postSessionDoneEvent(globalSession, GlobalStatus.Committed, false, false);
                } else {
                    globalSession.changeGlobalStatus(GlobalStatus.Committing);
                    shouldCommitNow = true;
                }
                //clean session after changing status successfully.
                globalSession.clean();
            }
            return shouldCommitNow;
        });

        if (shouldCommit) {
            boolean success = doGlobalCommit(globalSession, false);
            //If successful and all remaining branches can be committed asynchronously, do async commit.
            if (success && globalSession.hasBranch() && globalSession.canBeCommittedAsync()) {
                globalSession.asyncCommit();
                return GlobalStatus.Committed;
            } else {
                return globalSession.getStatus();
            }
        } else {
            return globalSession.getStatus() == GlobalStatus.AsyncCommitting ? GlobalStatus.Committed : globalSession.getStatus();
        }
    }

    @Override
    public boolean doGlobalCommit(GlobalSession globalSession, boolean retrying) throws TransactionException {
        boolean success = true;
        // start committing event
        MetricsPublisher.postSessionDoingEvent(globalSession, retrying);

        if (globalSession.isSaga()) {
            success = getCore(BranchType.SAGA).doGlobalCommit(globalSession, retrying);
        } else {
            List<BranchSession> branchSessions = globalSession.getSortedBranches();
            Boolean result = SessionHelper.forEach(branchSessions, branchSession -> {
                // if not retrying, skip the canBeCommittedAsync branches
                if (!retrying && branchSession.canBeCommittedAsync()) {
                    return CONTINUE;
                }

                BranchStatus currentStatus = branchSession.getStatus();
                if (currentStatus == BranchStatus.PhaseOne_Failed) {
                    SessionHelper.removeBranch(globalSession, branchSession, !retrying);
                    return CONTINUE;
                }
                // Only databases with read-only optimization, such as Oracle,
                // will report the RDONLY status during XA transactions.
                // At this point, the branch transaction can be ignored.
                if (currentStatus == BranchStatus.PhaseOne_RDONLY
                        && branchSession.getBranchType() == BranchType.XA) {
                    SessionHelper.removeBranch(globalSession, branchSession, !retrying);
                    return CONTINUE;
                }
                try {
                    BranchStatus branchStatus = getCore(branchSession.getBranchType()).branchCommit(globalSession, branchSession);
                    if (isXaerNotaTimeout(globalSession,branchStatus)) {
                        LOGGER.info("Commit branch XAER_NOTA retry timeout, xid = {} branchId = {}", globalSession.getXid(), branchSession.getBranchId());
                        branchStatus = BranchStatus.PhaseTwo_Committed;
                    }
                    switch (branchStatus) {
                        case PhaseTwo_Committed:
                            SessionHelper.removeBranch(globalSession, branchSession, !retrying);
                            LOGGER.info("Commit branch transaction successfully, xid = {} branchId = {}", globalSession.getXid(), branchSession.getBranchId());
                            return CONTINUE;
                        case PhaseTwo_CommitFailed_Unretryable:
                            //not at branch
                            SessionHelper.endCommitFailed(globalSession, retrying);
                            LOGGER.error("Committing global transaction[{}] finally failed, caused by branch transaction[{}] commit failed.", globalSession.getXid(), branchSession.getBranchId());
                            return false;

                        default:
                            if (!retrying) {
                                globalSession.queueToRetryCommit();
                                return false;
                            }
                            if (globalSession.canBeCommittedAsync()) {
                                LOGGER.error("Committing branch transaction[{}], status:{} and will retry later",
                                    branchSession.getBranchId(), branchStatus);
                                return CONTINUE;
                            } else {
                                LOGGER.error(
                                    "Committing global transaction[{}] failed, caused by branch transaction[{}] commit failed, will retry later.", globalSession.getXid(), branchSession.getBranchId());
                                return false;
                            }
                    }
                } catch (Exception ex) {
                    String commitInfo = retrying ? "Global commit continue" : "Global commit failed";
                    StackTraceLogger.error(LOGGER, ex, "Committing branch transaction exception:retrying={}, {}, {}",
                        new String[] {String.valueOf(retrying), branchSession.toString(), commitInfo});
                    if (!retrying) {
                        globalSession.queueToRetryCommit();
                        throw new TransactionException(ex);
                    }
                }
                return CONTINUE;
            }, PARALLEL_HANDLE_BRANCH && branchSessions.size() >= 2);
            // Return if the result is not null
            if (result != null) {
                return result;
            }
            //If has branch and not all remaining branches can be committed asynchronously,
            //do print log and return false
            if (globalSession.hasBranch() && !globalSession.canBeCommittedAsync()) {
                LOGGER.info("Committing global transaction is NOT done, xid = {}.", globalSession.getXid());
                return false;
            }
        }
        // if it succeeds and there is no branch, retrying=true is the asynchronous state when retrying. EndCommitted is
        // executed to improve concurrency performance, and the global transaction ends..
        if (success && globalSession.getBranchSessions().isEmpty()) {
            SessionHelper.endCommitted(globalSession, retrying);
            LOGGER.info("Committing global transaction is successfully done, xid = {}.", globalSession.getXid());
        }
        return success;
    }

    @Override
    public GlobalStatus rollback(String xid) throws TransactionException {
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        if (globalSession == null) {
            return GlobalStatus.Finished;
        }
        // just lock changeStatus
        boolean shouldRollBack = SessionHolder.lockAndExecute(globalSession, () -> {
            globalSession.close(); // Highlight: Firstly, close the session, then no more branch can be registered.
            if (globalSession.getStatus() == GlobalStatus.Begin) {
                globalSession.changeGlobalStatus(GlobalStatus.Rollbacking);
                return true;
            }
            return false;
        });
        if (!shouldRollBack) {
            return globalSession.getStatus();
        }
        
        boolean rollbackSuccess = doGlobalRollback(globalSession, false);
        return rollbackSuccess ? GlobalStatus.Rollbacked : globalSession.getStatus();
    }

    @Override
    public boolean doGlobalRollback(GlobalSession globalSession, boolean retrying) throws TransactionException {
        boolean success = true;
        // start rollback event
        MetricsPublisher.postSessionDoingEvent(globalSession, retrying);

        if (globalSession.isSaga()) {
            success = getCore(BranchType.SAGA).doGlobalRollback(globalSession, retrying);
        } else {
            List<BranchSession> branchSessions = globalSession.getReverseSortedBranches();
            Boolean result = SessionHelper.forEach(branchSessions, branchSession -> {
                BranchStatus currentBranchStatus = branchSession.getStatus();
                if (currentBranchStatus == BranchStatus.PhaseOne_Failed) {
                    SessionHelper.removeBranch(globalSession, branchSession, !retrying);
                    return CONTINUE;
                }
                try {
                    BranchStatus branchStatus = branchRollback(globalSession, branchSession);
                    if (isXaerNotaTimeout(globalSession, branchStatus)) {
                        LOGGER.info("Rollback branch XAER_NOTA retry timeout, xid = {} branchId = {}", globalSession.getXid(), branchSession.getBranchId());
                        branchStatus = BranchStatus.PhaseTwo_Rollbacked;
                    }
                    switch (branchStatus) {
                        case PhaseTwo_Rollbacked:
                            SessionHelper.removeBranch(globalSession, branchSession, !retrying);
                            LOGGER.info("Rollback branch transaction successfully, xid = {} branchId = {}", globalSession.getXid(), branchSession.getBranchId());
                            return CONTINUE;
                        case PhaseTwo_RollbackFailed_Unretryable:
                            SessionHelper.endRollbackFailed(globalSession, retrying);
                            LOGGER.error("Rollback branch transaction fail and stop retry, xid = {} branchId = {}", globalSession.getXid(), branchSession.getBranchId());
                            return false;
                        default:
                            LOGGER.error("Rollback branch transaction fail and will retry, xid = {} branchId = {}", globalSession.getXid(), branchSession.getBranchId());
                            if (!retrying) {
                                globalSession.queueToRetryRollback();
                            }
                            return false;
                    }
                } catch (Exception ex) {
                    StackTraceLogger.error(LOGGER, ex,
                        "Rollback branch transaction exception, xid = {} ,branchId = {} ,retrying={} ,exception = {}, global rollback failed",
                        new String[] {globalSession.getXid(), String.valueOf(branchSession.getBranchId()), String.valueOf(retrying), ex.getMessage()});
                    if (!retrying) {
                        globalSession.queueToRetryRollback();
                    }
                    throw new TransactionException(ex);
                }
            }, PARALLEL_HANDLE_BRANCH && branchSessions.size() >= 2);
            // Return if the result is not null
            if (result != null) {
                return result;
            }
        }

        // In db mode, lock and branch data residual problems may occur.
        // Therefore, execution needs to be delayed here and cannot be executed synchronously.
        if (success) {
            SessionHelper.endRollbacked(globalSession, retrying);
            LOGGER.info("Rollback global transaction successfully, xid = {}.", globalSession.getXid());
        }
        return success;
    }

    @Override
    public GlobalStatus getStatus(String xid) throws TransactionException {
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid, false);
        if (globalSession == null) {
            return GlobalStatus.Finished;
        } else {
            return globalSession.getStatus();
        }
    }

    @Override
    public GlobalStatus globalReport(String xid, GlobalStatus globalStatus) throws TransactionException {
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        if (globalSession == null) {
            return globalStatus;
        }
        doGlobalReport(globalSession, xid, globalStatus);
        return globalSession.getStatus();
    }

    @Override
    public void doGlobalReport(GlobalSession globalSession, String xid, GlobalStatus globalStatus) throws TransactionException {
        if (globalSession.isSaga()) {
            getCore(BranchType.SAGA).doGlobalReport(globalSession, xid, globalStatus);
        }
    }

    private boolean isXaerNotaTimeout(GlobalSession globalSession, BranchStatus branchStatus) {
        if (BranchStatus.PhaseTwo_CommitFailed_XAER_NOTA_Retryable.equals(branchStatus) ||
                BranchStatus.PhaseTwo_RollbackFailed_XAER_NOTA_Retryable.equals(branchStatus)) {
            return System.currentTimeMillis() > globalSession.getBeginTime() + globalSession.getTimeout() +
                    Math.max(RETRY_XAER_NOTA_TIMEOUT, globalSession.getTimeout());
        } else {
            return false;
        }
    }
}
