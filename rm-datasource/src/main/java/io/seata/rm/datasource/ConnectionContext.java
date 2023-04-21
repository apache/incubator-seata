/*
 *  Copyright 1999-2019 Seata.io Group.
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
package io.seata.rm.datasource;

import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.seata.common.LockStrategyMode;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.core.context.GlobalLockConfigHolder;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.GlobalLockConfig;
import io.seata.rm.datasource.undo.SQLUndoLog;

import static io.seata.common.Constants.AUTO_COMMIT;
import static io.seata.common.Constants.SKIP_CHECK_LOCK;

/**
 * The type Connection context.
 *
 * @author sharajava
 */
public class ConnectionContext {
    private static final Savepoint DEFAULT_SAVEPOINT = new Savepoint() {
        @Override
        public int getSavepointId() throws SQLException {
            return 0;
        }

        @Override
        public String getSavepointName() throws SQLException {
            return "DEFAULT_SEATA_SAVEPOINT";
        }
    };

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String xid;
    private Long branchId;
    private boolean isGlobalLockRequire;
    private Savepoint currentSavepoint = DEFAULT_SAVEPOINT;
    private boolean autoCommitChanged;
    private final Map<String, Object> applicationData = new HashMap<>(2, 1.0001f);

    /**
     * the lock keys buffer
     */
    private final Map<Savepoint, Set<String>> lockKeysBuffer = new LinkedHashMap<>();
    /**
     * the undo items buffer
     */
    private final Map<Savepoint, List<SQLUndoLog>> sqlUndoItemsBuffer = new LinkedHashMap<>();

    private final List<Savepoint> savepoints = new ArrayList<>(8);

    /**
     * whether requires global lock in this connection
     *
     * @return
     */
    boolean isGlobalLockRequire() {
        return isGlobalLockRequire;
    }

    /**
     * set whether requires global lock in this connection
     *
     * @param isGlobalLockRequire
     */
    void setGlobalLockRequire(boolean isGlobalLockRequire) {
        this.isGlobalLockRequire = isGlobalLockRequire;
    }

    /**
     * Append lock key.
     *
     * @param lockKey the lock key
     */
    void appendLockKey(String lockKey) {
        lockKeysBuffer.computeIfAbsent(currentSavepoint, k -> new HashSet<>()).add(lockKey);
    }

    /**
     * Append undo item.
     *
     * @param sqlUndoLog the sql undo log
     */
    void appendUndoItem(SQLUndoLog sqlUndoLog) {
        sqlUndoItemsBuffer.computeIfAbsent(currentSavepoint, k -> new ArrayList<>()).add(sqlUndoLog);
    }

    /**
     * Append savepoint
     *
     * @param savepoint the savepoint
     */
    void appendSavepoint(Savepoint savepoint) {
        savepoints.add(savepoint);
        this.currentSavepoint = savepoint;
    }

    public void removeSavepoint(Savepoint savepoint) {
        List<Savepoint> afterSavepoints = getAfterSavepoints(savepoint);

        if (null == savepoint) {
            sqlUndoItemsBuffer.clear();
            lockKeysBuffer.clear();
        } else {

            for (Savepoint sp : afterSavepoints) {
                sqlUndoItemsBuffer.remove(sp);
                lockKeysBuffer.remove(sp);
            }
        }

        savepoints.removeAll(afterSavepoints);
        currentSavepoint = savepoints.size() == 0 ? DEFAULT_SAVEPOINT : savepoints.get(savepoints.size() - 1);
    }

    public void releaseSavepoint(Savepoint savepoint) {
        List<Savepoint> afterSavepoints = getAfterSavepoints(savepoint);
        savepoints.removeAll(afterSavepoints);
        currentSavepoint = savepoints.size() == 0 ? DEFAULT_SAVEPOINT : savepoints.get(savepoints.size() - 1);

        // move the undo items & lock keys to current savepoint
        for (Savepoint sp : afterSavepoints) {
            List<SQLUndoLog> savepointSQLUndoLogs = sqlUndoItemsBuffer.remove(sp);
            if (CollectionUtils.isNotEmpty(savepointSQLUndoLogs)) {
                sqlUndoItemsBuffer.computeIfAbsent(currentSavepoint, k -> new ArrayList<>(savepointSQLUndoLogs.size()))
                        .addAll(savepointSQLUndoLogs);
            }

            Set<String> savepointLockKeys = lockKeysBuffer.remove(sp);
            if (CollectionUtils.isNotEmpty(savepointLockKeys)) {
                lockKeysBuffer.computeIfAbsent(currentSavepoint, k -> new HashSet<>())
                        .addAll(savepointLockKeys);
            }
        }
    }

    /**
     * In global transaction boolean.
     *
     * @return the boolean
     */
    public boolean inGlobalTransaction() {
        return xid != null;
    }

    /**
     * Is branch registered boolean.
     *
     * @return the boolean
     */
    public boolean isBranchRegistered() {
        return branchId != null;
    }

    /**
     * Bind.
     *
     * @param xid the xid
     */
    void bind(String xid) {
        if (xid == null) {
            throw new IllegalArgumentException("xid should not be null");
        }
        if (!inGlobalTransaction()) {
            setXid(xid);
        } else {
            if (!this.xid.equals(xid)) {
                throw new ShouldNeverHappenException(String.format("bind xid: %s, while current xid: %s", xid, this.xid));
            }
        }
    }

    /**
     * Has undo log boolean.
     *
     * @return the boolean
     */
    public boolean hasUndoLog() {
        return !sqlUndoItemsBuffer.isEmpty();
    }

    /**
     * Gets lock keys buffer.
     *
     * @return the lock keys buffer
     */
    public boolean hasLockKey() {
        return !lockKeysBuffer.isEmpty();
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
    void setXid(String xid) {
        this.xid = xid;
    }

    /**
     * Gets branch id.
     *
     * @return the branch id
     */
    public Long getBranchId() {
        return branchId;
    }

    /**
     * Sets branch id.
     *
     * @param branchId the branch id
     */
    void setBranchId(Long branchId) {
        this.branchId = branchId;
    }

    /**
     * is seata change targetConnection autoCommit
     *
     * @return the boolean
     */
    public boolean isAutoCommitChanged() {
        return this.autoCommitChanged;
    }

    /**
     * set seata change targetConnection autoCommit record
     *
     * @param autoCommitChanged the boolean
     */
    public void setAutoCommitChanged(boolean autoCommitChanged) {
        this.autoCommitChanged = autoCommitChanged;
    }

    /**
     * Gets applicationData.
     *
     * @return the application data
     */
    public String getApplicationData() throws TransactionException {
        GlobalLockConfig globalLockConfig = GlobalLockConfigHolder.getCurrentGlobalLockConfig();
        // lock retry times > 1 & skip first check lock / before image is empty
        Optional.ofNullable(globalLockConfig).ifPresent(lockConfig -> {
            if ((lockConfig.getLockRetryTimes() == -1 || lockConfig.getLockRetryTimes() > 1)
                && (lockConfig.getLockStrategyMode() == LockStrategyMode.OPTIMISTIC || allBeforeImageEmpty())) {
                if (!applicationData.containsKey(SKIP_CHECK_LOCK)) {
                    this.applicationData.put(SKIP_CHECK_LOCK, true);
                } else {
                    this.applicationData.put(SKIP_CHECK_LOCK, false);
                }
            }
        });

        boolean autoCommit = this.isAutoCommitChanged();
        // when transaction are enabled, it must be false
        if (!autoCommit) {
            this.applicationData.put(AUTO_COMMIT, autoCommit);
        }

        if (!this.applicationData.isEmpty()) {
            try {
                return MAPPER.writeValueAsString(this.applicationData);
            } catch (JsonProcessingException e) {
                throw new TransactionException(e.getMessage(), e);
            }
        }

        return null;
    }

    /**
     * Reset.
     */
    public void reset() {
        this.reset(null);
    }

    /**
     * Reset.
     *
     * @param xid the xid
     */
    void reset(String xid) {
        this.xid = xid;
        branchId = null;
        this.isGlobalLockRequire = false;
        savepoints.clear();
        lockKeysBuffer.clear();
        sqlUndoItemsBuffer.clear();
        this.autoCommitChanged = false;
        applicationData.clear();
    }

    /**
     * Build lock keys string.
     *
     * @return the string
     */
    public String buildLockKeys() {
        if (lockKeysBuffer.isEmpty()) {
            return null;
        }
        Set<String> lockKeysBufferSet = new HashSet<>();
        for (Set<String> lockKeys : lockKeysBuffer.values()) {
            lockKeysBufferSet.addAll(lockKeys);
        }

        if (lockKeysBufferSet.isEmpty()) {
            return null;
        }

        StringBuilder appender = new StringBuilder();
        Iterator<String> iterable = lockKeysBufferSet.iterator();
        while (iterable.hasNext()) {
            appender.append(iterable.next());
            if (iterable.hasNext()) {
                appender.append(";");
            }
        }
        return appender.toString();
    }

    /**
     * Gets undo items.
     *
     * @return the undo items
     */
    public List<SQLUndoLog> getUndoItems() {
        List<SQLUndoLog> undoItems = new ArrayList<>();
        for (List<SQLUndoLog> items : sqlUndoItemsBuffer.values()) {
            undoItems.addAll(items);
        }
        return undoItems;
    }


    /**
     * Get the savepoints after target savepoint(include the param savepoint)
     *
     * @param savepoint the target savepoint
     * @return after savepoints
     */
    private List<Savepoint> getAfterSavepoints(Savepoint savepoint) {
        if (null == savepoint) {
            return new ArrayList<>(savepoints);
        }

        return new ArrayList<>(savepoints.subList(savepoints.indexOf(savepoint), savepoints.size()));
    }

    /**
     * Check whether all the before image is empty.
     *
     * @return if all is empty, return true
     */
    private boolean allBeforeImageEmpty() {
        for (List<SQLUndoLog> sqlUndoLogs : sqlUndoItemsBuffer.values()) {
            for (SQLUndoLog undoLog : sqlUndoLogs) {
                if (null != undoLog.getBeforeImage() && undoLog.getBeforeImage().size() != 0) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return StringUtils.toString(this);
    }

}
