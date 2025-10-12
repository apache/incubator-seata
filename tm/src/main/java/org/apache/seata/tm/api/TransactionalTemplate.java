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
package org.apache.seata.tm.api;

import org.apache.seata.common.exception.FrameworkErrorCode;
import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.core.context.GlobalLockConfigHolder;
import org.apache.seata.core.exception.TmTransactionException;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.core.model.GlobalLockConfig;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.tm.api.transaction.Propagation;
import org.apache.seata.tm.api.transaction.SuspendedResourcesHolder;
import org.apache.seata.tm.api.transaction.TransactionHook;
import org.apache.seata.tm.api.transaction.TransactionHookManager;
import org.apache.seata.tm.api.transaction.TransactionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Template class for executing business logic within a global transaction context.
 *
 * <p>This class implements the Template Method pattern to provide a standardized
 * approach for managing global transactions. It handles all aspects of transaction
 * lifecycle including propagation, error handling, resource cleanup, and hook execution.</p>
 *
 * <h3>Core Responsibilities:</h3>
 * <ul>
 *   <li><b>Transaction Propagation</b>: Handles different propagation behaviors (REQUIRED, REQUIRES_NEW, etc.)</li>
 *   <li><b>Lifecycle Management</b>: Controls transaction begin, commit, rollback operations</li>
 *   <li><b>Error Handling</b>: Manages exceptions and determines rollback conditions</li>
 *   <li><b>Resource Management</b>: Handles transaction context binding/unbinding</li>
 *   <li><b>Hook Integration</b>: Triggers transaction lifecycle hooks</li>
 *   <li><b>Configuration Management</b>: Manages global lock configurations</li>
 * </ul>
 *
 * <h3>Transaction Propagation Support:</h3>
 * <ul>
 *   <li><b>REQUIRED</b>: Use existing transaction or create new one</li>
 *   <li><b>REQUIRES_NEW</b>: Always create new transaction, suspend existing</li>
 *   <li><b>SUPPORTS</b>: Use existing transaction, execute without if none</li>
 *   <li><b>NOT_SUPPORTED</b>: Execute without transaction, suspend existing</li>
 *   <li><b>NEVER</b>: Execute without transaction, fail if one exists</li>
 *   <li><b>MANDATORY</b>: Require existing transaction, fail if none exists</li>
 * </ul>
 *
 * <h3>Execution Flow:</h3>
 * <ol>
 *   <li>Extract transaction information from business executor</li>
 *   <li>Check current transaction context</li>
 *   <li>Apply transaction propagation rules</li>
 *   <li>Setup global lock configuration</li>
 *   <li>Begin transaction (if required by role)</li>
 *   <li>Execute business logic</li>
 *   <li>Handle completion (commit) or exceptions (rollback)</li>
 *   <li>Cleanup resources and trigger hooks</li>
 *   <li>Resume suspended transactions (if any)</li>
 * </ol>
 *
 * <h3>Error Handling Strategy:</h3>
 * <ul>
 *   <li>Business exceptions are evaluated against rollback rules</li>
 *   <li>Framework exceptions are automatically handled</li>
 *   <li>Transaction timeout is checked before commit</li>
 *   <li>Retry mechanisms are applied for infrastructure failures</li>
 * </ul>
 *
 * <h3>Hook Integration:</h3>
 * <p>The template integrates with {@link org.apache.seata.tm.api.transaction.TransactionHook} system to provide
 * extensibility points throughout the transaction lifecycle.</p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * TransactionalTemplate template = new TransactionalTemplate();
 * Object result = template.execute(new TransactionalExecutor() {
 *     @Override
 *     public Object execute() throws Throwable {
 *         // Your business logic here
 *         return businessService.doSomething();
 *     }
 *
 *     @Override
 *     public TransactionInfo getTransactionInfo() {
 *         return TransactionInfo.newBuilder()
 *             .setTimeOut(30000)
 *             .setName("business-operation")
 *             .setPropagation(Propagation.REQUIRED)
 *             .build();
 *     }
 * });
 * }</pre>
 *
 * <h3>Thread Safety:</h3>
 * <p>This class is thread-safe and can be used concurrently. Transaction context
 * is managed per-thread and does not interfere between threads.</p>
 *
 * @author Seata Team
 * @see TransactionalExecutor
 * @see GlobalTransaction
 * @see org.apache.seata.tm.api.transaction.TransactionInfo
 * @see org.apache.seata.tm.api.transaction.Propagation
 * @see org.apache.seata.tm.api.transaction.TransactionHook
 * @since 1.0.0
 */
public class TransactionalTemplate {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionalTemplate.class);

    /**
     * Executes business logic within a global transaction context.
     *
     * <p>This is the main entry point of the template. It orchestrates the entire
     * transaction lifecycle based on the configuration provided by the business executor.</p>
     *
     * <p><b>Pre-execution Phase:</b></p>
     * <ol>
     *   <li>Validates transaction information</li>
     *   <li>Determines current transaction context</li>
     *   <li>Applies propagation behavior</li>
     *   <li>Configures global lock settings</li>
     * </ol>
     *
     * <p><b>Execution Phase:</b></p>
     * <ol>
     *   <li>Begins transaction (if Launcher role)</li>
     *   <li>Triggers before hooks</li>
     *   <li>Executes business logic</li>
     *   <li>Handles success/failure scenarios</li>
     * </ol>
     *
     * <p><b>Post-execution Phase:</b></p>
     * <ol>
     *   <li>Commits or rolls back transaction</li>
     *   <li>Triggers after hooks</li>
     *   <li>Cleans up resources</li>
     *   <li>Resumes suspended transactions</li>
     * </ol>
     *
     * <p><b>Propagation Behavior:</b></p>
     * <p>The method handles different propagation behaviors automatically:</p>
     * <ul>
     *   <li><b>REQUIRED</b>: Most common case, joins existing or creates new</li>
     *   <li><b>REQUIRES_NEW</b>: Creates isolated transaction</li>
     *   <li><b>SUPPORTS/NOT_SUPPORTED</b>: Optional transaction execution</li>
     *   <li><b>NEVER/MANDATORY</b>: Strict transaction requirements</li>
     * </ul>
     *
     * <p><b>Exception Handling:</b></p>
     * <p>Exceptions during business execution are handled according to rollback rules.
     * Infrastructure exceptions (like TransactionException) are handled separately
     * from business exceptions.</p>
     *
     * @param business the business executor containing logic and transaction configuration
     * @return the result returned by business logic execution
     * @throws Throwable any exception thrown by business logic (after transaction handling)
     * @throws TransactionalExecutor.ExecutionException for transaction infrastructure failures
     * @throws TransactionException for transaction operation failures
     * @throws IllegalStateException for invalid transaction states
     *
     * @see TransactionalExecutor#execute()
     * @see TransactionalExecutor#getTransactionInfo()
     * @see org.apache.seata.tm.api.transaction.Propagation
     */
    public Object execute(TransactionalExecutor business) throws Throwable {
        // 1. Get transactionInfo
        TransactionInfo txInfo = business.getTransactionInfo();
        if (txInfo == null) {
            throw new ShouldNeverHappenException("transactionInfo does not exist");
        }
        // 1.1 Get current transaction, if not null, the tx role is 'GlobalTransactionRole.Participant'.
        GlobalTransaction tx = GlobalTransactionContext.getCurrent();

        // 1.2 Handle the transaction propagation.
        Propagation propagation = txInfo.getPropagation();
        SuspendedResourcesHolder suspendedResourcesHolder = null;
        try {
            switch (propagation) {
                case NOT_SUPPORTED:
                    // If transaction is existing, suspend it.
                    if (existingTransaction(tx)) {
                        suspendedResourcesHolder = tx.suspend(false);
                    }
                    // Execute without transaction and return.
                    return business.execute();
                case REQUIRES_NEW:
                    // If transaction is existing, suspend it, and then begin new transaction.
                    if (existingTransaction(tx)) {
                        suspendedResourcesHolder = tx.suspend(false);
                    }
                    tx = GlobalTransactionContext.createNew();
                    // Continue and execute with new transaction
                    break;
                case SUPPORTS:
                    // If transaction is not existing, execute without transaction.
                    if (notExistingTransaction(tx)) {
                        return business.execute();
                    }
                    // Continue and execute with new transaction
                    break;
                case REQUIRED:
                    // If current transaction is existing, execute with current transaction,else create
                    tx = GlobalTransactionContext.getCurrentOrCreate();
                    break;
                case NEVER:
                    // If transaction is existing, throw exception.
                    if (existingTransaction(tx)) {
                        throw new TransactionException(String.format(
                                "Existing transaction found for transaction marked with propagation 'never', xid = %s",
                                tx.getXid()));
                    } else {
                        // Execute without transaction and return.
                        return business.execute();
                    }
                case MANDATORY:
                    // If transaction is not existing, throw exception.
                    if (notExistingTransaction(tx)) {
                        throw new TransactionException(
                                "No existing transaction found for transaction marked with propagation 'mandatory'");
                    }
                    // Continue and execute with current transaction.
                    break;
                default:
                    throw new TransactionException("Not Supported Propagation:" + propagation);
            }

            // set current tx config to holder
            GlobalLockConfig previousConfig = replaceGlobalLockConfig(txInfo);

            if (tx.getGlobalTransactionRole() == GlobalTransactionRole.Participant) {
                LOGGER.info("join into a existing global transaction,xid={}", tx.getXid());
            }

            try {
                // 2. If the tx role is 'GlobalTransactionRole.Launcher', send the request of beginTransaction to TC,
                //    else do nothing. Of course, the hooks will still be triggered.
                beginTransaction(txInfo, tx);

                Object rs;
                try {
                    // Do Your Business
                    rs = business.execute();
                } catch (Throwable ex) {
                    // 3. The needed business exception to rollback.
                    completeTransactionAfterThrowing(txInfo, tx, ex);
                    throw ex;
                }

                // 4. everything is fine, commit.
                commitTransaction(tx, txInfo);

                return rs;
            } finally {
                // 5. clear
                resumeGlobalLockConfig(previousConfig);
                triggerAfterCompletion(tx);
                cleanUp(tx);
            }
        } finally {
            // If the transaction is suspended, resume it.
            if (suspendedResourcesHolder != null) {
                tx.resume(suspendedResourcesHolder);
            }
        }
    }

    /**
     * Judge whether timeout
     *
     * @param beginTime the beginTime
     * @param txInfo          the transaction info
     * @return is timeout
     */
    private boolean isTimeout(long beginTime, TransactionInfo txInfo) {

        return (System.currentTimeMillis() - beginTime) > txInfo.getTimeOut();
    }

    private boolean existingTransaction(GlobalTransaction tx) {
        return tx != null;
    }

    private boolean notExistingTransaction(GlobalTransaction tx) {
        return tx == null;
    }

    private GlobalLockConfig replaceGlobalLockConfig(TransactionInfo info) {
        GlobalLockConfig myConfig = new GlobalLockConfig();
        myConfig.setLockRetryInterval(info.getLockRetryInterval());
        myConfig.setLockRetryTimes(info.getLockRetryTimes());
        myConfig.setLockStrategyMode(info.getLockStrategyMode());
        return GlobalLockConfigHolder.setAndReturnPrevious(myConfig);
    }

    private void resumeGlobalLockConfig(GlobalLockConfig config) {
        if (config != null) {
            GlobalLockConfigHolder.setAndReturnPrevious(config);
        } else {
            GlobalLockConfigHolder.remove();
        }
    }

    private void completeTransactionAfterThrowing(
            TransactionInfo txInfo, GlobalTransaction tx, Throwable originalException)
            throws TransactionalExecutor.ExecutionException, TransactionException {
        // roll back
        if (txInfo != null && txInfo.rollbackOn(originalException)) {
            rollbackTransaction(tx, originalException);
        } else {
            // not roll back on this exception, so commit
            commitTransaction(tx, txInfo);
        }
    }

    private void commitTransaction(GlobalTransaction tx, TransactionInfo txInfo)
            throws TransactionalExecutor.ExecutionException, TransactionException {
        if (tx.getGlobalTransactionRole() != GlobalTransactionRole.Launcher) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Ignore commit: just involved in global transaction [{}]", tx.getXid());
            }
            return;
        }
        if (isTimeout(tx.getCreateTime(), txInfo)) {
            // business execution timeout
            Exception exx = new TmTransactionException(
                    TransactionExceptionCode.TransactionTimeout,
                    String.format(
                            "client detected transaction timeout before commit, so change to rollback, xid = %s",
                            tx.getXid()));
            rollbackTransaction(tx, exx);
            return;
        }

        try {
            triggerBeforeCommit();
            tx.commit();
            GlobalStatus afterCommitStatus = tx.getLocalStatus();
            TransactionalExecutor.Code code = TransactionalExecutor.Code.Unknown;
            switch (afterCommitStatus) {
                case TimeoutRollbacking:
                    code = TransactionalExecutor.Code.Rollbacking;
                    break;
                case TimeoutRollbacked:
                    code = TransactionalExecutor.Code.RollbackDone;
                    break;
                case Finished:
                    code = TransactionalExecutor.Code.CommitFailure;
                    break;
                default:
            }
            Exception statusException = null;
            if (GlobalStatus.isTwoPhaseHeuristic(afterCommitStatus)) {
                statusException = new TmTransactionException(
                        TransactionExceptionCode.CommitHeuristic,
                        String.format("Global transaction[%s] not found, may be rollbacked.", tx.getXid()));
            } else if (GlobalStatus.isOnePhaseTimeout(afterCommitStatus)) {
                statusException = new TmTransactionException(
                        TransactionExceptionCode.TransactionTimeout,
                        String.format("Global transaction[%s] is timeout and will be rollback[TC].", tx.getXid()));
            }
            if (null != statusException) {
                throw new TransactionalExecutor.ExecutionException(tx, statusException, code);
            }
            triggerAfterCommit();
        } catch (TransactionException txe) {
            // 4.1 Failed to commit
            throw new TransactionalExecutor.ExecutionException(tx, txe, TransactionalExecutor.Code.CommitFailure);
        }
    }

    private void rollbackTransaction(GlobalTransaction tx, Throwable originalException)
            throws TransactionException, TransactionalExecutor.ExecutionException {
        if (tx.getGlobalTransactionRole() != GlobalTransactionRole.Launcher) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Ignore rollback: just involved in global transaction [{}]", tx.getXid());
            }
            return;
        }
        try {
            triggerBeforeRollback();
            tx.rollback();
            triggerAfterRollback();
        } catch (TransactionException txe) {
            // Failed to rollback
            throw new TransactionalExecutor.ExecutionException(
                    tx, txe, TransactionalExecutor.Code.RollbackFailure, originalException);
        }

        // # fix #5231
        TransactionalExecutor.Code code;
        switch (tx.getLocalStatus()) {
            case RollbackFailed:
            case TimeoutRollbackFailed:
            case RollbackRetryTimeout:
                code = TransactionalExecutor.Code.RollbackFailure;
                break;
            case Rollbacking:
            case RollbackRetrying:
            case TimeoutRollbacking:
            case TimeoutRollbackRetrying:
                code = TransactionalExecutor.Code.Rollbacking;
                break;
            case TimeoutRollbacked:
            case Rollbacked:
            // rollback transactions but do not exist are usually considered completed
            case Finished:
                code = TransactionalExecutor.Code.RollbackDone;
                break;
            default:
                code = TransactionalExecutor.Code.Unknown;
                LOGGER.warn("{} rollback in the state {}", tx.getXid(), tx.getLocalStatus());
        }
        throw new TransactionalExecutor.ExecutionException(tx, code, originalException);
    }

    private void beginTransaction(TransactionInfo txInfo, GlobalTransaction tx)
            throws TransactionalExecutor.ExecutionException {
        if (tx.getGlobalTransactionRole() != GlobalTransactionRole.Launcher) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Ignore begin: just involved in global transaction [{}]", tx.getXid());
            }
            return;
        }
        try {
            triggerBeforeBegin();
            tx.begin(txInfo.getTimeOut(), txInfo.getName());
            triggerAfterBegin();
        } catch (TransactionException txe) {
            throw new TransactionalExecutor.ExecutionException(tx, txe, TransactionalExecutor.Code.BeginFailure);
        }
    }

    private void triggerBeforeBegin() {
        for (TransactionHook hook : getCurrentHooks()) {
            try {
                hook.beforeBegin();
            } catch (Exception e) {
                LOGGER.error("Failed execute beforeBegin in hook {}", e.getMessage(), e);
            }
        }
    }

    private void triggerAfterBegin() {
        for (TransactionHook hook : getCurrentHooks()) {
            try {
                hook.afterBegin();
            } catch (Exception e) {
                LOGGER.error("Failed execute afterBegin in hook {}", e.getMessage(), e);
            }
        }
    }

    private void triggerBeforeRollback() {
        for (TransactionHook hook : getCurrentHooks()) {
            try {
                hook.beforeRollback();
            } catch (Exception e) {
                LOGGER.error("Failed execute beforeRollback in hook {}", e.getMessage(), e);
            }
        }
    }

    private void triggerAfterRollback() {
        for (TransactionHook hook : getCurrentHooks()) {
            try {
                hook.afterRollback();
            } catch (Exception e) {
                LOGGER.error("Failed execute afterRollback in hook {}", e.getMessage(), e);
            }
        }
    }

    private void triggerBeforeCommit() {
        for (TransactionHook hook : getCurrentHooks()) {
            try {
                hook.beforeCommit();
            } catch (Exception e) {
                LOGGER.error("Failed execute beforeCommit in hook {}", e.getMessage(), e);
            }
        }
    }

    private void triggerAfterCommit() {
        for (TransactionHook hook : getCurrentHooks()) {
            try {
                hook.afterCommit();
            } catch (Exception e) {
                LOGGER.error("Failed execute afterCommit in hook {}", e.getMessage(), e);
            }
        }
    }

    private void triggerAfterCompletion(GlobalTransaction tx) {
        if (tx == null || tx.getGlobalTransactionRole() == GlobalTransactionRole.Launcher) {
            for (TransactionHook hook : getCurrentHooks()) {
                try {
                    hook.afterCompletion();
                } catch (Exception e) {
                    LOGGER.error("Failed execute afterCompletion in hook {}", e.getMessage(), e);
                }
            }
        }
    }

    private void cleanUp(GlobalTransaction tx) {
        if (tx == null) {
            throw new FrameworkException(
                    "Global transaction does not exist. Unable to proceed without a valid global transaction context.",
                    FrameworkErrorCode.ObjectNotExists);
        }
        if (tx.getGlobalTransactionRole() == GlobalTransactionRole.Launcher) {
            TransactionHookManager.clear();
        }
    }

    private List<TransactionHook> getCurrentHooks() {
        return TransactionHookManager.getHooks();
    }
}
