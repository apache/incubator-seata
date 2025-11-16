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

import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.model.TransactionManager;
import org.apache.seata.tm.TransactionManagerHolder;
import org.apache.seata.tm.api.transaction.SuspendedResourcesHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.seata.common.DefaultValues.DEFAULT_TM_COMMIT_RETRY_COUNT;
import static org.apache.seata.common.DefaultValues.DEFAULT_TM_ROLLBACK_RETRY_COUNT;

/**
 * Default implementation of {@link GlobalTransaction}.
 *
 * <p>This class provides the core implementation for managing global transactions in Seata.
 * It handles the complete lifecycle of a global transaction including begin, commit, rollback,
 * suspend and resume operations.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li>Role-based transaction management (Launcher vs Participant)</li>
 *   <li>Automatic retry mechanism for commit/rollback operations</li>
 *   <li>Transaction context binding and unbinding</li>
 *   <li>Suspend/resume support for transaction propagation</li>
 *   <li>Comprehensive logging and error handling</li>
 * </ul>
 *
 * <h3>Transaction Roles:</h3>
 * <ul>
 *   <li><b>Launcher</b>: The transaction initiator that controls the global transaction lifecycle</li>
 *   <li><b>Participant</b>: A participant that joins an existing global transaction</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * GlobalTransaction tx = GlobalTransactionContext.createNew();
 * try {
 *     tx.begin(30000, "order-service");
 *     // Execute business logic
 *     tx.commit();
 * } catch (Exception e) {
 *     tx.rollback();
 *     throw e;
 * }
 * }</pre>
 *
 * @see GlobalTransaction
 * @see GlobalTransactionContext
 * @see TransactionManager
 * @since 1.0.0
 */
public class DefaultGlobalTransaction implements GlobalTransaction {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultGlobalTransaction.class);

    /**
     * Default global transaction timeout in milliseconds (60 seconds).
     * This value is used when no explicit timeout is specified.
     */
    private static final int DEFAULT_GLOBAL_TX_TIMEOUT = 60000;

    /**
     * Default global transaction name.
     * Used when no explicit transaction name is provided.
     */
    private static final String DEFAULT_GLOBAL_TX_NAME = "default";

    /**
     * The transaction manager instance used for communicating with TC server.
     * This is obtained from {@link org.apache.seata.tm.TransactionManagerHolder} during construction.
     */
    private TransactionManager transactionManager;

    /**
     * Global transaction identifier (XID).
     * This is assigned by TC server when the transaction begins.
     */
    private String xid;

    /**
     * Current status of the global transaction.
     * @see GlobalStatus
     */
    private GlobalStatus status;

    /**
     * Role of this transaction instance.
     * Determines whether this instance can control the transaction lifecycle.
     * @see GlobalTransactionRole
     */
    private GlobalTransactionRole role;

    /**
     * Timestamp when this transaction was created.
     * Used to calculate timeout and for monitoring purposes.
     *
     * @see System#currentTimeMillis()
     */
    private long createTime;

    /**
     * Maximum number of retry attempts for commit operations.
     * Configurable via {@link ConfigurationKeys#CLIENT_TM_COMMIT_RETRY_COUNT}.
     */
    private static final int COMMIT_RETRY_COUNT = ConfigurationFactory.getInstance()
            .getInt(ConfigurationKeys.CLIENT_TM_COMMIT_RETRY_COUNT, DEFAULT_TM_COMMIT_RETRY_COUNT);

    /**
     * Maximum number of retry attempts for rollback operations.
     * Configurable via {@link ConfigurationKeys#CLIENT_TM_ROLLBACK_RETRY_COUNT}.
     */
    private static final int ROLLBACK_RETRY_COUNT = ConfigurationFactory.getInstance()
            .getInt(ConfigurationKeys.CLIENT_TM_ROLLBACK_RETRY_COUNT, DEFAULT_TM_ROLLBACK_RETRY_COUNT);

    /**
     * Creates a new DefaultGlobalTransaction instance with default values.
     *
     * <p>This constructor creates a transaction with:</p>
     * <ul>
     *   <li>XID: null (will be assigned when transaction begins)</li>
     *   <li>Status: {@link GlobalStatus#UnKnown}</li>
     *   <li>Role: {@link GlobalTransactionRole#Launcher}</li>
     * </ul>
     *
     * <p>This is typically used when creating a new global transaction that
     * will be the initiator of the transaction.</p>
     */
    DefaultGlobalTransaction() {
        this(null, GlobalStatus.UnKnown, GlobalTransactionRole.Launcher);
    }

    /**
     * Creates a new DefaultGlobalTransaction instance with specified parameters.
     *
     * <p>This constructor is typically used when:</p>
     * <ul>
     *   <li>Joining an existing global transaction (Participant role)</li>
     *   <li>Reloading a transaction from XID</li>
     *   <li>Creating transaction instances for testing</li>
     * </ul>
     *
     * @param xid    the global transaction identifier, may be null for new transactions
     * @param status the initial status of the transaction
     * @param role   the role of this transaction instance (Launcher or Participant)
     *
     * @see GlobalTransactionRole
     * @see GlobalStatus
     */
    public DefaultGlobalTransaction(String xid, GlobalStatus status, GlobalTransactionRole role) {
        this.transactionManager = TransactionManagerHolder.get();
        this.xid = xid;
        this.status = status;
        this.role = role;
    }

    /**
     * Begins a new global transaction with default timeout and name.
     *
     * <p>This method uses:</p>
     * <ul>
     *   <li>Timeout: {@value #DEFAULT_GLOBAL_TX_TIMEOUT} milliseconds</li>
     *   <li>Name: {@value #DEFAULT_GLOBAL_TX_NAME}</li>
     * </ul>
     *
     * @throws TransactionException if the transaction cannot be started
     * @see #begin(int, String)
     */
    @Override
    public void begin() throws TransactionException {
        begin(DEFAULT_GLOBAL_TX_TIMEOUT);
    }

    /**
     * Begins a new global transaction with specified timeout and default name.
     *
     * @param timeout the transaction timeout in milliseconds, must be positive
     * @throws TransactionException if the transaction cannot be started
     * @see #begin(int, String)
     */
    @Override
    public void begin(int timeout) throws TransactionException {
        begin(timeout, DEFAULT_GLOBAL_TX_NAME);
    }

    /**
     * Begins a new global transaction with specified timeout and name.
     *
     * <p>This method performs the following operations:</p>
     * <ol>
     *   <li>Records the creation timestamp</li>
     *   <li>Validates the transaction role and state</li>
     *   <li>Ensures no existing transaction is bound to current thread</li>
     *   <li>Sends begin request to TC server via TransactionManager</li>
     *   <li>Binds the received XID to current thread context</li>
     *   <li>Updates the transaction status to {@link GlobalStatus#Begin}</li>
     * </ol>
     *
     * <p><b>Role Behavior:</b></p>
     * <ul>
     *   <li><b>Launcher</b>: Initiates a new global transaction</li>
     *   <li><b>Participant</b>: Ignores the begin operation (just logs and returns)</li>
     * </ul>
     *
     * @param timeout the transaction timeout in milliseconds, must be positive
     * @param name    the transaction name for identification and monitoring
     *
     * @throws TransactionException if the transaction cannot be started
     * @throws IllegalStateException if a global transaction already exists in current thread
     *
     * @see org.apache.seata.core.context.RootContext#bind(String)
     * @see org.apache.seata.core.model.TransactionManager#begin(String, String, String, int)
     */
    @Override
    public void begin(int timeout, String name) throws TransactionException {
        this.createTime = System.currentTimeMillis();

        // Participants don't initiate transactions, they just join existing ones
        if (role != GlobalTransactionRole.Launcher) {
            assertXIDNotNull();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Ignore Begin(): just involved in global transaction [{}]", xid);
            }
            return;
        }

        // Ensure this is a clean transaction start
        assertXIDNull();
        String currentXid = RootContext.getXID();
        if (currentXid != null) {
            throw new IllegalStateException("Global transaction already exists,"
                    + " can't begin a new global transaction, currentXid = " + currentXid);
        }

        // Request XID from TC server and bind to thread context
        xid = transactionManager.begin(null, null, name, timeout);
        status = GlobalStatus.Begin;
        RootContext.bind(xid);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Begin new global transaction [{}]", xid);
        }
    }

    /**
     * Commits the global transaction.
     *
     * <p>This method attempts to commit the global transaction by coordinating with TC server.
     * The commit process involves:</p>
     * <ol>
     *   <li>Role validation (only Launcher can commit)</li>
     *   <li>XID validation</li>
     *   <li>Sending commit request to TC server with retry mechanism</li>
     *   <li>Cleaning up transaction context</li>
     * </ol>
     *
     * <p><b>Role Behavior:</b></p>
     * <ul>
     *   <li><b>Launcher</b>: Initiates the commit process and coordinates with TC</li>
     *   <li><b>Participant</b>: Ignores the commit operation (logs and returns)</li>
     * </ul>
     *
     * <p><b>Retry Mechanism:</b></p>
     * <p>The commit operation includes automatic retry functionality to handle temporary
     * network issues or TC server unavailability. The retry count is configurable via
     * {@link ConfigurationKeys#CLIENT_TM_COMMIT_RETRY_COUNT}.</p>
     *
     * <p><b>Context Cleanup:</b></p>
     * <p>After the commit attempt (successful or failed), the transaction context
     * is automatically cleaned up by calling {@link #suspend(boolean)} with clean=true.</p>
     *
     * @throws TransactionException if the commit fails after all retry attempts
     * @throws IllegalStateException if XID is null
     *
     * @see org.apache.seata.core.model.TransactionManager#commit(String)
     * @see #suspend(boolean)
     */
    @SuppressWarnings("lgtm[java/constant-comparison]")
    @Override
    public void commit() throws TransactionException {
        // Only Launchers can commit global transactions
        if (role == GlobalTransactionRole.Participant) {
            // Participant has no responsibility of committing
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Ignore Commit(): just involved in global transaction [{}]", xid);
            }
            return;
        }

        assertXIDNotNull();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("transaction {} will be commit", xid);
        }

        // Determine retry count (use configured value or default)
        int retry = COMMIT_RETRY_COUNT <= 0 ? DEFAULT_TM_COMMIT_RETRY_COUNT : COMMIT_RETRY_COUNT;
        try {
            // Retry loop for commit operation
            while (retry > 0) {
                try {
                    retry--;
                    status = transactionManager.commit(xid);
                    break; // Success, exit retry loop
                } catch (Throwable ex) {
                    LOGGER.error(
                            "Failed to report global commit [{}],Retry Countdown: {}, reason: {}",
                            this.getXid(),
                            retry,
                            ex.getMessage());
                    if (retry == 0) {
                        throw new TransactionException("Failed to report global commit", ex);
                    }
                }
            }
        } finally {
            // Clean up transaction context if this is the bound transaction
            if (xid.equals(RootContext.getXID())) {
                suspend(true);
            }
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[{}] commit status: {}", xid, status);
        }
    }

    /**
     * Rolls back the global transaction.
     *
     * <p>This method attempts to rollback the global transaction by coordinating with TC server.
     * The rollback process involves:</p>
     * <ol>
     *   <li>Role validation (only Launcher can rollback)</li>
     *   <li>XID validation</li>
     *   <li>Sending rollback request to TC server with retry mechanism</li>
     *   <li>Cleaning up transaction context</li>
     * </ol>
     *
     * <p><b>Role Behavior:</b></p>
     * <ul>
     *   <li><b>Launcher</b>: Initiates the rollback process and coordinates with TC</li>
     *   <li><b>Participant</b>: Ignores the rollback operation (logs and returns)</li>
     * </ul>
     *
     * <p><b>Retry Mechanism:</b></p>
     * <p>The rollback operation includes automatic retry functionality to handle temporary
     * network issues or TC server unavailability. The retry count is configurable via
     * {@link ConfigurationKeys#CLIENT_TM_ROLLBACK_RETRY_COUNT}.</p>
     *
     * <p><b>Context Cleanup:</b></p>
     * <p>After the rollback attempt (successful or failed), the transaction context
     * is automatically cleaned up by calling {@link #suspend(boolean)} with clean=true.</p>
     *
     * <p><b>Error Handling:</b></p>
     * <p>Rollback operations are generally more tolerant of failures compared to commits,
     * as the system should eventually reach a consistent state through timeout mechanisms
     * or manual intervention.</p>
     *
     * @throws TransactionException if the rollback fails after all retry attempts
     * @throws IllegalStateException if XID is null
     *
     * @see org.apache.seata.core.model.TransactionManager#rollback(String)
     * @see #suspend(boolean)
     */
    @SuppressWarnings("lgtm[java/constant-comparison]")
    @Override
    public void rollback() throws TransactionException {
        // Only Launchers can rollback global transactions
        if (role == GlobalTransactionRole.Participant) {
            // Participant has no responsibility of rollback
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Ignore Rollback(): just involved in global transaction [{}]", xid);
            }
            return;
        }

        assertXIDNotNull();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("transaction {} will be rollback", xid);
        }

        // Determine retry count (use configured value or default)
        int retry = ROLLBACK_RETRY_COUNT <= 0 ? DEFAULT_TM_ROLLBACK_RETRY_COUNT : ROLLBACK_RETRY_COUNT;
        try {
            // Retry loop for rollback operation
            while (retry > 0) {
                try {
                    retry--;
                    status = transactionManager.rollback(xid);
                    break; // Success, exit retry loop
                } catch (Throwable ex) {
                    LOGGER.error(
                            "Failed to report global rollback [{}],Retry Countdown: {}, reason: {}",
                            this.getXid(),
                            retry,
                            ex.getMessage());
                    if (retry == 0) {
                        throw new TransactionException("Failed to report global rollback", ex);
                    }
                }
            }
        } finally {
            // Clean up transaction context if this is the bound transaction
            if (xid.equals(RootContext.getXID())) {
                suspend(true);
            }
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[{}] rollback status: {}", xid, status);
        }
    }

    /**
     * Suspends the global transaction without cleaning the context.
     *
     * <p>This is equivalent to calling {@link #suspend(boolean)} with clean=false.
     * The suspended transaction can be resumed later using {@link #resume(SuspendedResourcesHolder)}.</p>
     *
     * <p><b>Use Cases:</b></p>
     * <ul>
     *   <li>Transaction propagation (REQUIRES_NEW, NOT_SUPPORTED)</li>
     *   <li>Temporary transaction context switching</li>
     *   <li>Nested transaction scenarios</li>
     * </ul>
     *
     * @return SuspendedResourcesHolder containing the suspended XID, or null if no transaction is bound
     * @throws TransactionException if suspension fails
     *
     * @see #suspend(boolean)
     * @see #resume(SuspendedResourcesHolder)
     */
    @Override
    public SuspendedResourcesHolder suspend() throws TransactionException {
        return suspend(false);
    }

    /**
     * Suspends the global transaction with optional context cleanup.
     *
     * <p>This method unbinds the current transaction from the thread context.
     * The behavior depends on the clean parameter:</p>
     *
     * <p><b>Clean Mode (clean=true):</b></p>
     * <ul>
     *   <li>Unbinds XID from thread context</li>
     *   <li>Returns null (no resume capability)</li>
     *   <li>Used for transaction completion (commit/rollback)</li>
     *   <li>Logs "transaction end" message</li>
     * </ul>
     *
     * <p><b>Suspend Mode (clean=false):</b></p>
     * <ul>
     *   <li>Unbinds XID from thread context</li>
     *   <li>Returns SuspendedResourcesHolder for later resume</li>
     *   <li>Used for transaction propagation scenarios</li>
     *   <li>Logs "suspending current transaction" message</li>
     * </ul>
     *
     * <p><b>Implementation Details:</b></p>
     * <p>The method first retrieves the current XID before unbinding to ensure
     * proper logging association. If no transaction is currently bound, returns null.</p>
     *
     * @param clean if true, performs cleanup without return holder; if false, returns holder for resume
     * @return SuspendedResourcesHolder for resume (if clean=false and transaction exists), otherwise null
     * @throws TransactionException if suspension fails
     *
     * @see org.apache.seata.core.context.RootContext#getXID()
     * @see org.apache.seata.core.context.RootContext#unbind()
     * @see org.apache.seata.tm.api.transaction.SuspendedResourcesHolder
     */
    @Override
    public SuspendedResourcesHolder suspend(boolean clean) throws TransactionException {
        // In order to associate the following logs with XID, first get and then unbind.
        String xid = RootContext.getXID();
        if (xid != null) {
            if (LOGGER.isInfoEnabled()) {
                if (clean) {
                    LOGGER.info("transaction end, xid = {}", xid);
                } else {
                    LOGGER.info("suspending current transaction, xid = {}", xid);
                }
            }
            RootContext.unbind();
            return clean ? null : new SuspendedResourcesHolder(xid);
        } else {
            return null;
        }
    }

    @Override
    public void resume(SuspendedResourcesHolder suspendedResourcesHolder) throws TransactionException {
        if (suspendedResourcesHolder == null) {
            return;
        }
        String xid = suspendedResourcesHolder.getXid();
        RootContext.bind(xid);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Resuming the transaction,xid = {}", xid);
        }
    }

    @Override
    public GlobalStatus getStatus() throws TransactionException {
        if (xid == null) {
            return GlobalStatus.UnKnown;
        }
        status = transactionManager.getStatus(xid);
        return status;
    }

    @Override
    public String getXid() {
        return xid;
    }

    @Override
    public void globalReport(GlobalStatus globalStatus) throws TransactionException {
        assertXIDNotNull();

        if (globalStatus == null) {
            throw new IllegalStateException();
        }

        status = transactionManager.globalReport(xid, globalStatus);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[{}] report status: {}", xid, status);
        }

        if (xid.equals(RootContext.getXID())) {
            suspend(true);
        }
    }

    @Override
    public GlobalStatus getLocalStatus() {
        return status;
    }

    @Override
    public GlobalTransactionRole getGlobalTransactionRole() {
        return role;
    }

    @Override
    public long getCreateTime() {
        return createTime;
    }

    private void assertXIDNotNull() {
        if (xid == null) {
            throw new IllegalStateException();
        }
    }

    private void assertXIDNull() {
        if (xid != null) {
            throw new IllegalStateException();
        }
    }
}
