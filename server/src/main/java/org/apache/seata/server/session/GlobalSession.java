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
package org.apache.seata.server.session;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.Constants;
import org.apache.seata.common.DefaultValues;
import org.apache.seata.common.XID;
import org.apache.seata.common.util.BufferUtils;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.exception.GlobalTransactionException;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.model.LockStatus;
import org.apache.seata.common.util.UUIDGenerator;
import org.apache.seata.server.cluster.raft.RaftServerManager;
import org.apache.seata.server.lock.LockerManagerFactory;
import org.apache.seata.server.store.SessionStorable;
import org.apache.seata.server.store.StoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.seata.core.model.GlobalStatus.AsyncCommitting;
import static org.apache.seata.core.model.GlobalStatus.CommitRetrying;
import static org.apache.seata.core.model.GlobalStatus.Committing;

/**
 * The type Global session.
 *
 */
public class GlobalSession implements SessionLifecycle, SessionStorable {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalSession.class);

    private static final int MAX_GLOBAL_SESSION_SIZE = StoreConfig.getMaxGlobalSessionSize();

    private static ThreadLocal<ByteBuffer> byteBufferThreadLocal = ThreadLocal.withInitial(() -> ByteBuffer.allocate(
        MAX_GLOBAL_SESSION_SIZE));

    /**
     * ThreadLocal should be optimize.
     * It is tied to the current threading model. threadlocal's public set method does nothing to protect it from abuse.
     */
    private static final ThreadLocal<GlobalStatus> EXPECTED_STATUS_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * If the global session's status is (Rollbacking or Committing) and currentTime - createTime >= RETRY_DEAD_THRESHOLD
     *  then the tx will be remand as need to retry rollback
     */
    private static final int RETRY_DEAD_THRESHOLD = ConfigurationFactory.getInstance()
            .getInt(ConfigurationKeys.RETRY_DEAD_THRESHOLD, DefaultValues.DEFAULT_RETRY_DEAD_THRESHOLD);

    private String xid;

    private long transactionId;

    private volatile GlobalStatus status;

    private String applicationId;

    private String transactionServiceGroup;

    private String transactionName;

    private int timeout;

    private long beginTime;

    private String applicationData;

    private final boolean lazyLoadBranch;

    private volatile boolean active = true;

    private List<BranchSession> branchSessions;

    private GlobalSessionLock globalSessionLock = new GlobalSessionLock();

    private Set<SessionLifecycleListener> lifecycleListeners = new HashSet<>(2);

    /**
     * Add boolean.
     *
     * @param branchSession the branch session
     * @return the boolean
     */
    public boolean add(BranchSession branchSession) {
        if (null != branchSessions) {
            return branchSessions.add(branchSession);
        } else {
            // db and redis no need to deal with
            return true;
        }
    }

    /**
     * Remove boolean.
     *
     * @param branchSession the branch session
     * @return the boolean
     */
    public boolean remove(BranchSession branchSession) {
        synchronized (this) {
            return branchSessions.remove(branchSession);
        }
    }

    /**
     * Remove boolean.
     *
     * @param branchId the long
     * @return the boolean
     */
    public boolean remove(Long branchId) {
        return this.remove(this.getBranch(branchId));
    }

    /**
     * Can be committed async boolean.
     *
     * @return the boolean
     */
    public boolean canBeCommittedAsync() {
        List<BranchSession> branchSessions = getBranchSessions();
        for (BranchSession branchSession : branchSessions) {
            if (!branchSession.canBeCommittedAsync()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Has AT branch
     *
     * @return the boolean
     */
    public boolean hasATBranch() {
        List<BranchSession> branchSessions = getBranchSessions();
        for (BranchSession branchSession : branchSessions) {
            if (branchSession.getBranchType() == BranchType.AT) {
                return true;
            }
        }
        return false;
    }

    /**
     * Is saga type transaction
     *
     * @return is saga
     */
    public boolean isSaga() {
        List<BranchSession> branchSessions = getBranchSessions();
        if (branchSessions.size() > 0) {
            return BranchType.SAGA == branchSessions.get(0).getBranchType();
        } else {
            return StringUtils.isNotBlank(transactionName)
                && transactionName.startsWith(Constants.SAGA_TRANS_NAME_PREFIX);
        }
    }

    /**
     * Is timeout boolean.
     *
     * @return the boolean
     */
    public boolean isTimeout() {
        return (System.currentTimeMillis() - beginTime) > timeout;
    }

    /**
     * prevent could not handle committing and rollbacking transaction
     * @return if true retry commit or roll back
     */
    public boolean isDeadSession() {
        return (System.currentTimeMillis() - beginTime) > RETRY_DEAD_THRESHOLD;
    }

    /**
     * prevent could not handle committing and rollbacking transaction
     * @return time to dead session. if not greater than 0, then deadSession
     */
    public long timeToDeadSession() {
        return beginTime + RETRY_DEAD_THRESHOLD - System.currentTimeMillis();
    }

    @Override
    public void begin() throws TransactionException {
        this.status = GlobalStatus.Begin;
        this.beginTime = System.currentTimeMillis();
        this.active = true;
        SessionHolder.getRootSessionManager().onBegin(this);
        for (SessionLifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onBegin(this);
        }
    }

    @Override
    public void changeGlobalStatus(GlobalStatus status) throws TransactionException {
        if (GlobalStatus.Rollbacking == status || GlobalStatus.TimeoutRollbacking == status) {
            LockerManagerFactory.getLockManager().updateLockStatus(xid, LockStatus.Rollbacking);
        }
        SessionHolder.getRootSessionManager().onStatusChange(this, status);
        // set session status after update successfully
        this.status = status;
        for (SessionLifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onStatusChange(this, status);
        }
    }

    @Override
    public void changeBranchStatus(BranchSession branchSession, BranchStatus status) throws TransactionException {
        SessionHolder.getRootSessionManager().onBranchStatusChange(this, branchSession, status);
        for (SessionLifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onBranchStatusChange(this, branchSession, status);
        }
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void close() throws TransactionException {
        if (active) {
            SessionHolder.getRootSessionManager().onClose(this);
            for (SessionLifecycleListener lifecycleListener : lifecycleListeners) {
                lifecycleListener.onClose(this);
            }
        }
    }

    @Override
    public void end() throws TransactionException {
        if (GlobalStatus.isTwoPhaseSuccess(status)) {
            // TODO: Non AT mode does not need to be unlocked
            // Clean locks first
            clean();
            SessionHolder.getRootSessionManager().onSuccessEnd(this);
            for (SessionLifecycleListener lifecycleListener : lifecycleListeners) {
                lifecycleListener.onSuccessEnd(this);
            }
        } else {
            SessionHolder.getRootSessionManager().onFailEnd(this);
            for (SessionLifecycleListener lifecycleListener : lifecycleListeners) {
                lifecycleListener.onFailEnd(this);
            }
        }
    }

    public void clean() throws TransactionException {
        if (!LockerManagerFactory.getLockManager().releaseGlobalSessionLock(this)) {
            throw new TransactionException("UnLock globalSession error, xid = " + this.xid);
        }
    }

    /**
     * Close and clean.
     *
     * @throws TransactionException the transaction exception
     */
    public void closeAndClean() throws TransactionException {
        close();
        if (this.hasATBranch()) {
            clean();
        }
    }

    /**
     * Add session lifecycle listener.
     *
     * @param sessionLifecycleListener the session lifecycle listener
     */
    public void addSessionLifecycleListener(SessionLifecycleListener sessionLifecycleListener) {
        lifecycleListeners.add(sessionLifecycleListener);
    }

    /**
     * Remove session lifecycle listener.
     *
     * @param sessionLifecycleListener the session lifecycle listener
     */
    public void removeSessionLifecycleListener(SessionLifecycleListener sessionLifecycleListener) {
        lifecycleListeners.remove(sessionLifecycleListener);
    }

    @Override
    public void addBranch(BranchSession branchSession) throws TransactionException {
        SessionHolder.getRootSessionManager().onAddBranch(this, branchSession);
        for (SessionLifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onAddBranch(this, branchSession);
        }
        if (!RaftServerManager.isRaftMode()) {
            add(branchSession);
        }
    }

    public void loadBranchs() {
        if (branchSessions == null && isLazyLoadBranch()) {
            synchronized (this) {
                if (branchSessions == null && isLazyLoadBranch()) {
                    branchSessions = new ArrayList<>();
                    Optional.ofNullable(SessionHolder.getRootSessionManager().findGlobalSession(xid, true))
                        .ifPresent(globalSession -> branchSessions.addAll(globalSession.getBranchSessions()));
                }
            }
        }
    }

    @Override
    public void unlockBranch(BranchSession branchSession) throws TransactionException {
        // do not unlock if global status in (Committing, CommitRetrying, AsyncCommitting),
        // because it's already unlocked in 'DefaultCore.commit()'
        if (this.status != Committing && this.status != CommitRetrying && this.status != AsyncCommitting) {
            if (!branchSession.unlock()) {
                throw new TransactionException(
                    "Unlock branch lock failed, xid = " + this.xid + ", branchId = " + branchSession.getBranchId());
            }
        }
    }

    @Override
    public void removeBranch(BranchSession branchSession) throws TransactionException {
        SessionHolder.getRootSessionManager().onRemoveBranch(this, branchSession);
        for (SessionLifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onRemoveBranch(this, branchSession);
        }

        if (!RaftServerManager.isRaftMode()) {
            this.remove(branchSession);
        }

    }

    @Override
    public void removeAndUnlockBranch(BranchSession branchSession) throws TransactionException {
        unlockBranch(branchSession);
        removeBranch(branchSession);
    }

    /**
     * Gets branch.
     *
     * @param branchId the branch id
     * @return the branch
     */
    public BranchSession getBranch(long branchId) {
        synchronized (this) {
            List<BranchSession> branchSessions = getBranchSessions();
            for (BranchSession branchSession : branchSessions) {
                if (branchSession.getBranchId() == branchId) {
                    return branchSession;
                }
            }

            return null;
        }
    }

    /**
     * Gets sorted branches.
     *
     * @return the sorted branches
     */
    public List<BranchSession> getSortedBranches() {
        return new ArrayList<>(getBranchSessions());
    }

    /**
     * Gets reverse sorted branches.
     *
     * @return the reverse sorted branches
     */
    public List<BranchSession> getReverseSortedBranches() {
        List<BranchSession> reversed = new ArrayList<>(getBranchSessions());
        Collections.reverse(reversed);
        return reversed;
    }

    /**
     * Instantiates a new Global session.
     */
    public GlobalSession() {
        this.lazyLoadBranch = false;
    }

    /**
     * Instantiates a new Global session.
     *
     * @param applicationId           the application id
     * @param transactionServiceGroup the transaction service group
     * @param transactionName         the transaction name
     * @param timeout                 the timeout
     * @param lazyLoadBranch          the lazy load branch
     */
    public GlobalSession(String applicationId, String transactionServiceGroup, String transactionName, int timeout, boolean lazyLoadBranch) {
        this.transactionId = UUIDGenerator.generateUUID();
        this.status = GlobalStatus.Begin;
        this.lazyLoadBranch = lazyLoadBranch;
        if (!lazyLoadBranch) {
            this.branchSessions = new ArrayList<>();
        }
        this.applicationId = applicationId;
        this.transactionServiceGroup = transactionServiceGroup;
        this.transactionName = transactionName;
        this.timeout = timeout;
        this.xid = XID.generateXID(transactionId);
    }

    /**
     * Instantiates a new Global session.
     *
     * @param applicationId           the application id
     * @param transactionServiceGroup the transaction service group
     * @param transactionName         the transaction name
     * @param timeout                 the timeout
     */
    public GlobalSession(String applicationId, String transactionServiceGroup, String transactionName, int timeout) {
        this(applicationId, transactionServiceGroup, transactionName, timeout, false);
    }

    /**
     * Gets transaction id.
     *
     * @return the transaction id
     */
    public long getTransactionId() {
        return transactionId;
    }

    /**
     * Sets transaction id.
     *
     * @param transactionId the transaction id
     */
    public void setTransactionId(long transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * Gets status.
     *
     * @return the status
     */
    public GlobalStatus getStatus() {
        return status;
    }

    /**
     * Sets status.
     *
     * @param status the status
     */
    public void setStatus(GlobalStatus status) {
        this.status = status;
    }

    /**
     * Gets xid.
     *
     * @return the xid
     */
    public String getXid() {
        return xid;
    }

    /**
     * Sets xid.
     *
     * @param xid the xid
     */
    public void setXid(String xid) {
        this.xid = xid;
    }

    /**
     * Gets application id.
     *
     * @return the application id
     */
    public String getApplicationId() {
        return applicationId;
    }

    /**
     * Gets transaction service group.
     *
     * @return the transaction service group
     */
    public String getTransactionServiceGroup() {
        return transactionServiceGroup;
    }

    /**
     * Gets transaction name.
     *
     * @return the transaction name
     */
    public String getTransactionName() {
        return transactionName;
    }

    /**
     * Gets timeout.
     *
     * @return the timeout
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Gets begin time.
     *
     * @return the begin time
     */
    public long getBeginTime() {
        return beginTime;
    }

    /**
     * Sets begin time.
     *
     * @param beginTime the begin time
     */
    public void setBeginTime(long beginTime) {
        this.beginTime = beginTime;
    }

    /**
     * Gets application data.
     *
     * @return the application data
     */
    public String getApplicationData() {
        return applicationData;
    }

    /**
     * Sets application data.
     *
     * @param applicationData the application data
     */
    public void setApplicationData(String applicationData) {
        this.applicationData = applicationData;
    }

    public boolean isLazyLoadBranch() {
        return lazyLoadBranch;
    }

    /**
     * Create global session global session.
     *
     * @param applicationId  the application id
     * @param txServiceGroup the tx service group
     * @param txName         the tx name
     * @param timeout        the timeout
     * @return the global session
     */
    public static GlobalSession createGlobalSession(String applicationId, String txServiceGroup, String txName,
        int timeout) {
        GlobalSession session = new GlobalSession(applicationId, txServiceGroup, txName, timeout, false);
        return session;
    }

    /**
     * Sets active.
     *
     * @param active the active
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public byte[] encode() {
        byte[] byApplicationIdBytes = applicationId != null ? applicationId.getBytes() : null;

        byte[] byServiceGroupBytes = transactionServiceGroup != null ? transactionServiceGroup.getBytes() : null;

        byte[] byTxNameBytes = transactionName != null ? transactionName.getBytes() : null;

        byte[] xidBytes = xid != null ? xid.getBytes() : null;

        byte[] applicationDataBytes = applicationData != null ? applicationData.getBytes() : null;

        int size = calGlobalSessionSize(byApplicationIdBytes, byServiceGroupBytes, byTxNameBytes, xidBytes,
            applicationDataBytes);

        if (size > MAX_GLOBAL_SESSION_SIZE) {
            throw new RuntimeException("global session size exceeded, size : " + size + " byte, maxGlobalSessionSize : " +
                MAX_GLOBAL_SESSION_SIZE + " byte");
        }
        ByteBuffer byteBuffer = byteBufferThreadLocal.get();
        //recycle
        byteBuffer.clear();

        byteBuffer.putLong(transactionId);
        byteBuffer.putInt(timeout);
        if (byApplicationIdBytes != null) {
            byteBuffer.putShort((short)byApplicationIdBytes.length);
            byteBuffer.put(byApplicationIdBytes);
        } else {
            byteBuffer.putShort((short)0);
        }
        if (byServiceGroupBytes != null) {
            byteBuffer.putShort((short)byServiceGroupBytes.length);
            byteBuffer.put(byServiceGroupBytes);
        } else {
            byteBuffer.putShort((short)0);
        }
        if (byTxNameBytes != null) {
            byteBuffer.putShort((short)byTxNameBytes.length);
            byteBuffer.put(byTxNameBytes);
        } else {
            byteBuffer.putShort((short)0);
        }
        if (xidBytes != null) {
            byteBuffer.putInt(xidBytes.length);
            byteBuffer.put(xidBytes);
        } else {
            byteBuffer.putInt(0);
        }
        if (applicationDataBytes != null) {
            byteBuffer.putInt(applicationDataBytes.length);
            byteBuffer.put(applicationDataBytes);
        } else {
            byteBuffer.putInt(0);
        }
        byteBuffer.putLong(beginTime);
        byteBuffer.put((byte)status.getCode());
        BufferUtils.flip(byteBuffer);
        byte[] result = new byte[byteBuffer.limit()];
        byteBuffer.get(result);
        return result;
    }

    private int calGlobalSessionSize(byte[] byApplicationIdBytes, byte[] byServiceGroupBytes, byte[] byTxNameBytes,
        byte[] xidBytes, byte[] applicationDataBytes) {
        return 8 // transactionId
            + 4 // timeout
            + 2 // byApplicationIdBytes.length
            + 2 // byServiceGroupBytes.length
            + 2 // byTxNameBytes.length
            + 4 // xidBytes.length
            + 4 // applicationDataBytes.length
            + 8 // beginTime
            + 1 // statusCode
            + (byApplicationIdBytes == null ? 0 : byApplicationIdBytes.length)
            + (byServiceGroupBytes == null ? 0 : byServiceGroupBytes.length)
            + (byTxNameBytes == null ? 0 : byTxNameBytes.length)
            + (xidBytes == null ? 0 : xidBytes.length)
            + (applicationDataBytes == null ? 0 : applicationDataBytes.length);
    }

    @Override
    public void decode(byte[] a) {
        this.branchSessions = new ArrayList<>();
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
        int xidLen = byteBuffer.getInt();
        if (xidLen > 0) {
            byte[] xidBytes = new byte[xidLen];
            byteBuffer.get(xidBytes);
            this.xid = new String(xidBytes);
        }
        int applicationDataLen = byteBuffer.getInt();
        if (applicationDataLen > 0) {
            byte[] applicationDataLenBytes = new byte[applicationDataLen];
            byteBuffer.get(applicationDataLenBytes);
            this.applicationData = new String(applicationDataLenBytes);
        }

        this.beginTime = byteBuffer.getLong();
        this.status = GlobalStatus.get(byteBuffer.get());
    }

    /**
     * Has branch boolean.
     *
     * @return the boolean
     */
    public boolean hasBranch() {
        return getBranchSessions().size() > 0;
    }

    public void lock() throws TransactionException {
        globalSessionLock.lock();
    }

    public void unlock() {
        globalSessionLock.unlock();
    }

    private static class GlobalSessionLock {

        private Lock globalSessionLock = new ReentrantLock();

        private static final int GLOBAL_SESSION_LOCK_TIME_OUT_MILLS = 2 * 1000;

        public void lock() throws TransactionException {
            try {
                if (globalSessionLock.tryLock(GLOBAL_SESSION_LOCK_TIME_OUT_MILLS, TimeUnit.MILLISECONDS)) {
                    return;
                }
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted error", e);
            }
            throw new GlobalTransactionException(TransactionExceptionCode.FailedLockGlobalTranscation, "Lock global session failed");
        }

        public void unlock() {
            globalSessionLock.unlock();
        }
    }

    @FunctionalInterface
    public interface LockRunnable {

        void run() throws TransactionException;
    }

    @FunctionalInterface
    public interface LockCallable<V> {

        V call() throws TransactionException;
    }

    public List<BranchSession> getBranchSessions() {
        loadBranchs();
        return branchSessions;
    }

    public void asyncCommit() throws TransactionException {
        changeGlobalStatus(GlobalStatus.AsyncCommitting);
    }

    public void queueToRetryCommit() throws TransactionException {
        changeGlobalStatus(GlobalStatus.CommitRetrying);
    }

    public void queueToRetryRollback() throws TransactionException {
        GlobalStatus currentStatus = this.getStatus();
        GlobalStatus newStatus;
        if (SessionStatusValidator.isTimeoutGlobalStatus(currentStatus)) {
            newStatus = GlobalStatus.TimeoutRollbackRetrying;
        } else {
            newStatus = GlobalStatus.RollbackRetrying;
        }
        changeGlobalStatus(newStatus);
    }

    public void setExpectedStatusFromCurrent() {
        EXPECTED_STATUS_THREAD_LOCAL.set(this.status);
    }

    public void cleanExpectedStatus() {
        EXPECTED_STATUS_THREAD_LOCAL.remove();
    }

    public GlobalStatus getExpectedStatus() {
        return EXPECTED_STATUS_THREAD_LOCAL.get();
    }

    @Override
    public String toString() {
        return "GlobalSession{" + "xid='" + xid + '\'' + ", transactionId=" + transactionId + ", status=" + status
            + ", applicationId='" + applicationId + '\'' + ", transactionServiceGroup='" + transactionServiceGroup
            + '\'' + ", transactionName='" + transactionName + '\'' + ", timeout=" + timeout + ", beginTime="
            + beginTime + ", applicationData='" + applicationData + '\'' + ", lazyLoadBranch=" + lazyLoadBranch
            + ", active=" + active + ", branchSessions=" + branchSessions + ", globalSessionLock=" + globalSessionLock
            + ", lifecycleListeners=" + lifecycleListeners + '}';
    }
}
