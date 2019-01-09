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

package com.alibaba.fescar.server.session;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;

import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.core.model.BranchStatus;
import com.alibaba.fescar.core.model.BranchType;
import com.alibaba.fescar.core.model.GlobalStatus;
import com.alibaba.fescar.server.UUIDGenerator;
import com.alibaba.fescar.server.store.SessionStorable;

public class GlobalSession implements SessionLifecycle, SessionStorable {

    private long transactionId;

    private GlobalStatus status;

    private String applicationId;

    private String transactionServiceGroup;

    private String transactionName;

    private int timeout;

    private long beginTime;

    private boolean active;

    private ArrayList<BranchSession> branchSessions = new ArrayList<>();

    public boolean add(BranchSession branchSession) {
        return branchSessions.add(branchSession);
    }

    public boolean remove(BranchSession branchSession) {
        return branchSessions.remove(branchSession);
    }

    private ArrayList<SessionLifecycleListener> lifecycleListeners = new ArrayList<>();

    public boolean canBeCommittedAsync() {
        for (BranchSession branchSession : branchSessions) {
            if (branchSession.getBranchType() == BranchType.MT) {
                return false;
            }
        }
        return true;
    }

    public boolean isTimeout() {
        return (System.currentTimeMillis() - beginTime) > timeout;
    }

    @Override
    public void begin() throws TransactionException {
        this.status = GlobalStatus.Begin;
        this.beginTime = System.currentTimeMillis();
        this.active = true;
        for (SessionLifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onBegin(this);
        }
    }

    @Override
    public void changeStatus(GlobalStatus status) throws TransactionException {
        for (SessionLifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onStatusChange(this, status);
        }
        this.status = status;

    }

    @Override
    public void changeBranchStatus(BranchSession branchSession, BranchStatus status)
        throws TransactionException {
        for (SessionLifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onBranchStatusChange(this, branchSession, status);
        }
        branchSession.setStatus(status);
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void close() throws TransactionException {
        if (active) {
            for (SessionLifecycleListener lifecycleListener : lifecycleListeners) {
                lifecycleListener.onClose(this);
            }
        }
    }

    @Override
    public void end() throws TransactionException {
        for (SessionLifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onEnd(this);
        }

    }

    private void clean() throws TransactionException {
        for (BranchSession branchSession : branchSessions) {
            branchSession.unlock();
        }

    }

    public void closeAndClean() throws TransactionException {
        close();
        clean();

    }

    public void addSessionLifecycleListener(SessionLifecycleListener sessionLifecycleListener) {
        lifecycleListeners.add(sessionLifecycleListener);
    }

    public void removeSessionLifecycleListener(SessionLifecycleListener sessionLifecycleListener) {
        lifecycleListeners.remove(sessionLifecycleListener);
    }

    @Override
    public void addBranch(BranchSession branchSession) throws TransactionException {
        for (SessionLifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onAddBranch(this, branchSession);
        }
        branchSession.setStatus(BranchStatus.Registered);
        add(branchSession);
    }

    @Override
    public void removeBranch(BranchSession branchSession) throws TransactionException {
        for (SessionLifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onRemoveBranch(this, branchSession);
        }
        branchSession.unlock();
        remove(branchSession);
    }

    public BranchSession getBranch(long branchId) {
        synchronized (branchSessions) {
            for (BranchSession branchSession : branchSessions) {
                if (branchSession.getBranchId() == branchId) {
                    return branchSession;
                }
            }

            return null;
        }

    }

    public ArrayList<BranchSession> getSortedBranches() {
        ArrayList<BranchSession> sorted = new ArrayList();
        sorted.addAll(branchSessions);
        return sorted;
    }

    public ArrayList<BranchSession> getReverseSortedBranches() {
        ArrayList<BranchSession> reversed = new ArrayList();
        reversed.addAll(branchSessions);
        Collections.reverse(reversed);
        return reversed;
    }

    public GlobalSession() {}

    public GlobalSession(String applicationId, String transactionServiceGroup, String transactionName, int timeout) {
        this.transactionId = UUIDGenerator.generateUUID();
        this.status = GlobalStatus.Begin;

        this.applicationId = applicationId;
        this.transactionServiceGroup = transactionServiceGroup;
        this.transactionName = transactionName;
        this.timeout = timeout;
    }

    public long getTransactionId() {
        return transactionId;
    }

    public GlobalStatus getStatus() {
        return status;
    }

    void setStatus(GlobalStatus status) {
        this.status = status;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getTransactionServiceGroup() {
        return transactionServiceGroup;
    }

    public String getTransactionName() {
        return transactionName;
    }

    public int getTimeout() {
        return timeout;
    }

    public long getBeginTime() {
        return beginTime;
    }

    public static GlobalSession createGlobalSession(String applicationId, String txServiceGroup, String txName,
                                                    int timeout) {
        GlobalSession session = new GlobalSession(applicationId, txServiceGroup, txName, timeout);
        return session;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public byte[] encode() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(512);
        byteBuffer.putLong(transactionId);
        byteBuffer.putInt(timeout);
        if (null != applicationId) {
            byte[] byApplicationId = applicationId.getBytes();
            byteBuffer.putShort((short)byApplicationId.length);
            byteBuffer.put(byApplicationId);
        } else {
            byteBuffer.putShort((short)0);
        }
        if (null != transactionServiceGroup) {
            byte[] byServiceGroup = transactionServiceGroup.getBytes();
            byteBuffer.putShort((short)byServiceGroup.length);
            byteBuffer.put(byServiceGroup);
        } else {
            byteBuffer.putShort((short)0);
        }
        if (null != transactionName) {
            byte[] byTxName = transactionName.getBytes();
            byteBuffer.putShort((short)byTxName.length);
            byteBuffer.put(byTxName);
        } else {
            byteBuffer.putShort((short)0);
        }
        byteBuffer.putLong(beginTime);
        byteBuffer.flip();
        byte[] result = new byte[byteBuffer.limit()];
        byteBuffer.get(result);
        return result;
    }

    @Override
    public void decode(byte[] a) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(a);
        this.transactionId = byteBuffer.getLong();
        this.timeout = byteBuffer.getInt();
        short applicationIdLen = byteBuffer.getShort();
        if (applicationIdLen > 0) {
            byte[] byApplicationId = new byte[applicationIdLen];
            byteBuffer.get(byApplicationId);
            this.applicationId = new String(byApplicationId);
        }
        short serviceGroupLen = byteBuffer.getShort();
        if (serviceGroupLen > 0) {
            byte[] byServiceGroup = new byte[serviceGroupLen];
            byteBuffer.get(byServiceGroup);
            this.transactionServiceGroup = new String(byServiceGroup);
        }
        short txNameLen = byteBuffer.getShort();
        if (txNameLen > 0) {
            byte[] byTxName = new byte[txNameLen];
            byteBuffer.get(byTxName);
            this.transactionName = new String(byTxName);
        }
        this.beginTime = byteBuffer.getLong();
    }

    public boolean hasBranch() {
        return branchSessions.size() > 0;
    }
}
