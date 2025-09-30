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

import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.tm.api.transaction.SuspendedResourcesHolder;

/**
 * Global transaction interface for managing distributed transactions in Seata.
 *
 * <p>Provides complete lifecycle management for global transactions including
 * begin, commit, rollback, suspend, and resume operations.</p>
 *
 * <p><b>Transaction Roles:</b></p>
 * <ul>
 *   <li><b>Launcher</b>: Can control transaction lifecycle (begin, commit, rollback)</li>
 *   <li><b>Participant</b>: Can only participate (register branches, query status)</li>
 * </ul>
 *
 * <p><b>Basic Usage:</b></p>
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
 * @author Seata Team
 * @see GlobalTransactionContext
 * @see DefaultGlobalTransaction
 * @since 1.0.0
 */
public interface GlobalTransaction extends BaseTransaction {

    /**
     * Begins a new global transaction with default timeout (60s) and name ("default").
     *
     * <p>Launcher role: Creates new transaction with TC server<br>
     * Participant role: Ignores the operation (already in transaction)</p>
     *
     * @throws TransactionException if transaction cannot be started
     * @throws IllegalStateException if transaction already exists in current thread
     */
    void begin() throws TransactionException;

    /**
     * Begins a new global transaction with specified timeout and default name.
     *
     * @param timeout transaction timeout in milliseconds (recommended: 1000-300000ms)
     * @throws TransactionException if transaction cannot be started
     * @throws IllegalArgumentException if timeout is not positive
     */
    void begin(int timeout) throws TransactionException;

    /**
     * Begins a new global transaction with specified timeout and name.
     *
     * @param timeout transaction timeout in milliseconds
     * @param name transaction name for identification (recommended format: "service-operation")
     * @throws TransactionException if transaction cannot be started
     * @throws IllegalArgumentException if timeout is not positive or name is null/empty
     */
    void begin(int timeout, String name) throws TransactionException;

    /**
     * Commits the global transaction using two-phase commit protocol.
     *
     * <p>Only Launcher role can execute commit. Participant role ignores this operation.</p>
     * <p>Includes automatic retry mechanism for network failures.</p>
     *
     * @throws TransactionException if commit fails after all retry attempts
     * @throws IllegalStateException if XID is null or invalid state
     */
    void commit() throws TransactionException;

    /**
     * Rolls back the global transaction.
     *
     * <p>Only Launcher role can execute rollback. Uses different strategies based on transaction mode:
     * AT (undo logs), TCC (cancel), SAGA (compensation), XA (rollback).</p>
     *
     * @throws TransactionException if rollback fails
     * @throws IllegalStateException if XID is null or invalid state
     */
    void rollback() throws TransactionException;

    /**
     * Suspends the transaction without cleaning context (can be resumed).
     *
     * @return SuspendedResourcesHolder for resume, or null if no transaction
     * @throws TransactionException if suspension fails
     */
    SuspendedResourcesHolder suspend() throws TransactionException;

    /**
     * Suspends the transaction with optional context cleanup.
     *
     * @param clean if true, performs cleanup (cannot resume); if false, allows resume
     * @return SuspendedResourcesHolder for resume (if clean=false), null otherwise
     * @throws TransactionException if suspension fails
     */
    SuspendedResourcesHolder suspend(boolean clean) throws TransactionException;

    /**
     * Resumes a previously suspended transaction.
     *
     * @param suspendedResourcesHolder holder containing suspended transaction context
     * @throws TransactionException if resume fails
     * @throws IllegalArgumentException if holder is null
     * @throws IllegalStateException if current thread already has transaction
     */
    void resume(SuspendedResourcesHolder suspendedResourcesHolder) throws TransactionException;

    /**
     * Queries current transaction status from TC server (remote call).
     *
     * @return current GlobalStatus from TC server
     * @throws TransactionException if status query fails
     * @throws IllegalStateException if XID is null
     */
    GlobalStatus getStatus() throws TransactionException;

    /**
     * Gets the global transaction identifier (XID).
     *
     * <p>Format: "IP:PORT:TRANSACTION_ID" (e.g., "192.168.1.100:8091:2012052108:40001")</p>
     *
     * @return XID string, or null if transaction hasn't begun
     */
    String getXid();

    /**
     * Reports transaction status to TC server (for manual status management).
     *
     * @param globalStatus status to report
     * @throws TransactionException if reporting fails
     * @throws IllegalArgumentException if globalStatus is null
     */
    void globalReport(GlobalStatus globalStatus) throws TransactionException;

    /**
     * Gets locally cached transaction status (fast, no network call).
     *
     * @return locally cached GlobalStatus
     */
    GlobalStatus getLocalStatus();

    /**
     * Gets the transaction role (Launcher or Participant).
     *
     * @return GlobalTransactionRole of this instance
     */
    GlobalTransactionRole getGlobalTransactionRole();

    /**
     * Gets the creation timestamp of this transaction instance.
     *
     * @return creation time in milliseconds since epoch
     */
    long getCreateTime();
}
