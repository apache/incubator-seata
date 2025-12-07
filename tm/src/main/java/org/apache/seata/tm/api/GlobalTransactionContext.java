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

import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.GlobalStatus;

/**
 * Factory and utility class for managing GlobalTransaction instances.
 *
 * <p>Provides static factory methods to create, retrieve, and manage global transaction
 * instances. Serves as the primary entry point for transaction context management.</p>
 *
 * <p><b>Core Methods:</b></p>
 * <ul>
 *   <li><b>createNew()</b>: Create new transaction (Launcher role)</li>
 *   <li><b>getCurrent()</b>: Get current thread transaction (Participant role)</li>
 *   <li><b>getCurrentOrCreate()</b>: Get existing or create new (auto role assignment)</li>
 *   <li><b>reload()</b>: Reload transaction from XID (for recovery)</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create new transaction (Launcher role)
 * GlobalTransaction tx = GlobalTransactionContext.createNew();
 *
 * // Get current or create new (most common)
 * GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();
 *
 * // Check current transaction
 * GlobalTransaction current = GlobalTransactionContext.getCurrent();
 *
 * // Recovery scenario
 * GlobalTransaction recovered = GlobalTransactionContext.reload("xid");
 * }</pre>
 *
 * <p>Thread-safe. Transaction context managed per-thread using {@link org.apache.seata.core.context.RootContext}.</p>
 *
 * @author Seata Team
 * @see GlobalTransaction
 * @see DefaultGlobalTransaction
 * @since 1.0.0
 */
public class GlobalTransactionContext {

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     */
    private GlobalTransactionContext() {}

    /**
     * Creates a new GlobalTransaction instance for initiating a global transaction.
     *
     * <p>Creates transaction with Launcher role, UnKnown status, and null XID.
     * Must call {@link GlobalTransaction#begin()} to start the transaction.</p>
     *
     * <p><b>Use Cases:</b> New transactions, REQUIRES_NEW propagation, programmatic management</p>
     *
     * @return new GlobalTransaction instance with Launcher role
     */
    public static GlobalTransaction createNew() {
        return new DefaultGlobalTransaction();
    }

    /**
     * Retrieves the GlobalTransaction instance bound to the current thread.
     *
     * <p>Checks for active transaction in current thread by examining XID in RootContext.
     * Returns Participant role transaction if found.</p>
     *
     * <p><b>Participant Role:</b> Can participate in transaction but cannot control lifecycle
     * (begin/commit/rollback operations are ignored).</p>
     *
     * @return GlobalTransaction with Participant role if XID exists, null otherwise
     */
    public static GlobalTransaction getCurrent() {
        String xid = RootContext.getXID();
        if (xid == null) {
            return null;
        }
        return new DefaultGlobalTransaction(xid, GlobalStatus.Begin, GlobalTransactionRole.Participant);
    }

    /**
     * Gets current GlobalTransaction or creates new one if none exists.
     *
     * <p>Combines functionality of {@link #getCurrent()} and {@link #createNew()}:</p>
     * <ul>
     *   <li>If transaction exists: Returns Participant role transaction</li>
     *   <li>If no transaction: Creates new Launcher role transaction</li>
     * </ul>
     *
     * <p><b>Most commonly used method</b> for declarative transactions and REQUIRED propagation.</p>
     *
     * @return existing GlobalTransaction (Participant) or new GlobalTransaction (Launcher)
     */
    public static GlobalTransaction getCurrentOrCreate() {
        GlobalTransaction tx = getCurrent();
        if (tx == null) {
            return createNew();
        }
        return tx;
    }

    /**
     * Reloads a GlobalTransaction instance from XID for recovery purposes.
     *
     * <p>Creates transaction with Launcher role and provided XID. Used for transaction recovery,
     * manual intervention, and debugging. <b>Begin operations are disabled</b> to prevent
     * creating new transactions with existing XIDs.</p>
     *
     * <p><b>Supported Operations:</b> commit(), rollback(), getStatus(), getXid()</p>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * GlobalTransaction tx = GlobalTransactionContext.reload("192.168.1.1:8091:123456");
     * GlobalStatus status = tx.getStatus();
     * if (status == GlobalStatus.Committing) {
     *     tx.commit();
     * }
     * }</pre>
     *
     * @param xid the global transaction identifier to reload
     * @return reloaded GlobalTransaction with Launcher role but disabled begin operations
     * @throws TransactionException if XID is invalid or loading fails
     * @throws IllegalArgumentException if xid is null or empty
     */
    public static GlobalTransaction reload(String xid) throws TransactionException {
        return new DefaultGlobalTransaction(xid, GlobalStatus.UnKnown, GlobalTransactionRole.Launcher) {
            @Override
            public void begin(int timeout, String name) throws TransactionException {
                throw new IllegalStateException("Never BEGIN on a RELOADED GlobalTransaction. ");
            }
        };
    }
}
