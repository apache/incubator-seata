/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.fescar.tm.api;

import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.core.model.GlobalStatus;

/**
 * Global transaction.
 */
public interface GlobalTransaction {

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

}
