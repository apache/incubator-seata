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
package io.seata.tm.api;

import io.seata.core.exception.TransactionException;
import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.model.GlobalStatus;
import io.seata.tm.api.transaction.SuspendedResourcesHolder;

/**
 * The type Default global transaction.
 */
@Deprecated
public class DefaultGlobalTransaction implements GlobalTransaction {
    private final org.apache.seata.tm.api.DefaultGlobalTransaction instance;

    DefaultGlobalTransaction() {
        this(null, GlobalStatus.UnKnown, GlobalTransactionRole.Launcher);
    }

    /**
     * Instantiates a new Default global transaction.
     *
     * @param xid    the xid
     * @param status the status
     * @param role   the role
     */
    DefaultGlobalTransaction(String xid, GlobalStatus status, GlobalTransactionRole role) {
        this.instance = new org.apache.seata.tm.api.DefaultGlobalTransaction(xid, status.convertGlobalStatus(),
            convertApacheSeataGlobalTransactionRole(role));
    }

    private static GlobalStatus convertIoSeataGlobalStatus(org.apache.seata.core.model.GlobalStatus globalStatus) {
        return GlobalStatus.get(globalStatus.getCode());
    }

    private static org.apache.seata.tm.api.GlobalTransactionRole convertApacheSeataGlobalTransactionRole(GlobalTransactionRole globalTransactionRole) {
        return org.apache.seata.tm.api.GlobalTransactionRole.valueOf(globalTransactionRole.name());
    }

    private static GlobalTransactionRole convertIoSeataGlobalTransactionRole(org.apache.seata.tm.api.GlobalTransactionRole globalTransactionRole) {
        return GlobalTransactionRole.valueOf(globalTransactionRole.name());
    }

    @Override
    public void begin() throws TransactionException {
        try {
            this.instance.begin();
        } catch (org.apache.seata.core.exception.TransactionException e) {
            throw new TransactionException(TransactionExceptionCode.valueOf(e.getCode().name()), e.getMessage(), e.getCause());
        }
    }

    @Override
    public void begin(int timeout) throws TransactionException {
        try {
            this.instance.begin(timeout);
        } catch (org.apache.seata.core.exception.TransactionException e) {
            throw new TransactionException(TransactionExceptionCode.valueOf(e.getCode().name()), e.getMessage(), e.getCause());
        }
    }

    @Override
    public void begin(int timeout, String name) throws TransactionException {
        try {
            this.instance.begin(timeout, name);
        } catch (org.apache.seata.core.exception.TransactionException e) {
            throw new TransactionException(TransactionExceptionCode.valueOf(e.getCode().name()), e.getMessage(), e.getCause());
        }
    }

    @Override
    public void commit() throws TransactionException {
        try {
            this.instance.commit();
        } catch (org.apache.seata.core.exception.TransactionException e) {
            throw new TransactionException(TransactionExceptionCode.valueOf(e.getCode().name()), e.getMessage(), e.getCause());
        }
    }

    @Override
    public void rollback() throws TransactionException {
        try {
            this.instance.rollback();
        } catch (org.apache.seata.core.exception.TransactionException e) {
            throw new TransactionException(TransactionExceptionCode.valueOf(e.getCode().name()), e.getMessage(), e.getCause());
        }
    }

    @Override
    public SuspendedResourcesHolder suspend() throws TransactionException {
        try {
            return new SuspendedResourcesHolder(this.instance.suspend().getXid());
        } catch (org.apache.seata.core.exception.TransactionException e) {
            throw new TransactionException(TransactionExceptionCode.valueOf(e.getCode().name()), e.getMessage(), e.getCause());
        }
    }

    @Override
    public SuspendedResourcesHolder suspend(boolean clean) throws TransactionException {
        try {
            return new SuspendedResourcesHolder(this.instance.suspend(clean).getXid());
        } catch (org.apache.seata.core.exception.TransactionException e) {
            throw new TransactionException(TransactionExceptionCode.valueOf(e.getCode().name()), e.getMessage(), e.getCause());
        }
    }

    @Override
    public void resume(SuspendedResourcesHolder suspendedResourcesHolder) throws TransactionException {
        try {
            this.instance.resume(suspendedResourcesHolder);
        } catch (org.apache.seata.core.exception.TransactionException e) {
            throw new TransactionException(TransactionExceptionCode.valueOf(e.getCode().name()), e.getMessage(), e.getCause());
        }
    }

    @Override
    public GlobalStatus getStatus() throws TransactionException {
        try {
            return convertIoSeataGlobalStatus(this.instance.getStatus());
        } catch (org.apache.seata.core.exception.TransactionException e) {
            throw new TransactionException(TransactionExceptionCode.valueOf(e.getCode().name()), e.getMessage(), e.getCause());
        }
    }

    @Override
    public String getXid() {
        return this.instance.getXid();
    }

    @Override
    public void globalReport(GlobalStatus globalStatus) throws TransactionException {
        try {
            this.instance.globalReport(globalStatus.convertGlobalStatus());
        } catch (org.apache.seata.core.exception.TransactionException e) {
            throw new TransactionException(TransactionExceptionCode.valueOf(e.getCode().name()), e.getMessage(), e.getCause());
        }
    }

    @Override
    public GlobalStatus getLocalStatus() {
        return convertIoSeataGlobalStatus(this.instance.getLocalStatus());
    }

    @Override
    public GlobalTransactionRole getGlobalTransactionRole() {
        return convertIoSeataGlobalTransactionRole(this.instance.getGlobalTransactionRole());
    }

    @Override
    public long getCreateTime() {
        return this.instance.getCreateTime();
    }

    public org.apache.seata.tm.api.DefaultGlobalTransaction getInstance() {
        return instance;
    }
}
