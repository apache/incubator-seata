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
package org.apache.seata.saga.engine.tm;

import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.saga.engine.sequence.UUIDSeqGenerator;
import org.apache.seata.tm.api.GlobalTransaction;
import org.apache.seata.tm.api.GlobalTransactionRole;
import org.apache.seata.tm.api.transaction.SuspendedResourcesHolder;

/**
 * MockGlobalTransaction
 */
public class MockGlobalTransaction implements GlobalTransaction {

    private String xid;
    private GlobalStatus status;
    private long createTime;

    private static UUIDSeqGenerator uuidSeqGenerator = new UUIDSeqGenerator();

    public MockGlobalTransaction() {}

    public MockGlobalTransaction(String xid) {
        this.xid = xid;
    }

    public MockGlobalTransaction(String xid, GlobalStatus status) {
        this.xid = xid;
        this.status = status;
    }

    @Override
    public void begin() throws TransactionException {
        begin(60000);
    }

    @Override
    public void begin(int timeout) throws TransactionException {
        this.createTime = System.currentTimeMillis();
        status = GlobalStatus.Begin;
        xid = uuidSeqGenerator.generate(null);
        RootContext.bind(xid);
    }

    @Override
    public void begin(int timeout, String name) throws TransactionException {

    }

    @Override
    public void commit() throws TransactionException {

    }

    @Override
    public void rollback() throws TransactionException {

    }

    @Override
    public SuspendedResourcesHolder suspend() throws TransactionException {
        return null;
    }

    @Override
    public SuspendedResourcesHolder suspend(boolean clean)
            throws TransactionException {
        return null;
    }

    @Override
    public void resume(SuspendedResourcesHolder suspendedResourcesHolder)
            throws TransactionException {

    }

    @Override
    public GlobalStatus getStatus() throws TransactionException {
        return status;
    }

    @Override
    public String getXid() {
        return xid;
    }

    @Override
    public void globalReport(GlobalStatus globalStatus) throws TransactionException {

    }

    @Override
    public GlobalStatus getLocalStatus() {
        return status;
    }

    @Override
    public GlobalTransactionRole getGlobalTransactionRole() {
        return GlobalTransactionRole.Launcher;
    }

    @Override
    public long getCreateTime() {
        return createTime;
    }
}
