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
 * Global transaction.
 *
 */
public interface GlobalTransaction extends BaseTransaction {

    /**
     * Begin a new global transaction with default timeout and name.
     *
     * @throws TransactionException Any exception that fails this will be wrapped with TransactionException and thrown
     * out.
     */
    void begin() throws TransactionException;

    /**
     * Begin a new global transaction with given timeout and default name.
     *
     * @param timeout Global transaction timeout in MILLISECONDS
     * @throws TransactionException Any exception that fails this will be wrapped with TransactionException and thrown
     * out.
     */
    void begin(int timeout) throws TransactionException;

    /**
     * Begin a new global transaction with given timeout and given name.
     *
     * @param timeout Given timeout in MILLISECONDS.
     * @param name    Given name.
     * @throws TransactionException Any exception that fails this will be wrapped with TransactionException and thrown
     * out.
     */
    void begin(int timeout, String name) throws TransactionException;

    /**
     * Commit the global transaction.
     *
     * @throws TransactionException Any exception that fails this will be wrapped with TransactionException and thrown
     * out.
     */
    void commit() throws TransactionException;

    /**
     * Rollback the global transaction.
     *
     * @throws TransactionException Any exception that fails this will be wrapped with TransactionException and thrown
     * out.
     */
    void rollback() throws TransactionException;

    /**
     * Suspend the global transaction.
     *
     * @return the SuspendedResourcesHolder which holds the suspend resources
     * @throws TransactionException Any exception that fails this will be wrapped with TransactionException and thrown
     * @see SuspendedResourcesHolder
     */
    SuspendedResourcesHolder suspend() throws TransactionException;

    /**
     * Suspend the global transaction.
     *
     * @param clean the clean if true, clean the transaction context. otherwise,suspend only
     * @return the SuspendedResourcesHolder which holds the suspend resources
     * @throws TransactionException Any exception that fails this will be wrapped with TransactionException and thrown
     * @see SuspendedResourcesHolder
     */
    SuspendedResourcesHolder suspend(boolean clean) throws TransactionException;

    /**
     * Resume the global transaction.
     *
     * @param suspendedResourcesHolder the suspended resources to resume
     * @throws TransactionException Any exception that fails this will be wrapped with TransactionException and thrown
     * out.
     * @see SuspendedResourcesHolder
     */
    void resume(SuspendedResourcesHolder suspendedResourcesHolder) throws TransactionException;

    /**
     * Ask TC for current status of the corresponding global transaction.
     *
     * @return Status of the corresponding global transaction.
     * @throws TransactionException Any exception that fails this will be wrapped with TransactionException and thrown
     * out.
     * @see GlobalStatus
     */
    GlobalStatus getStatus() throws TransactionException;

    /**
     * Get XID.
     *
     * @return XID. xid
     */
    String getXid();

    /**
     * report the global transaction status.
     *
     * @param globalStatus global status.
     *
     * @throws TransactionException Any exception that fails this will be wrapped with TransactionException and thrown
     * out.
     */
    void globalReport(GlobalStatus globalStatus) throws TransactionException;

    /**
     * local status of the global transaction.
     *
     * @return Status of the corresponding global transaction.
     * @see GlobalStatus
     */
    GlobalStatus getLocalStatus();

    /**
     * get global transaction role.
     *
     * @return global transaction Role.
     * @see GlobalTransactionRole
     */
    GlobalTransactionRole getGlobalTransactionRole();

    /**
     * get create time
     *
     * @return create time
     */
    long getCreateTime();
}
