/*
 * Copyright 1999-2019 Seata.io Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.seata.rm.jedispool;

import static io.seata.common.Constants.REDIS_TX_LOG;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.StringUtils;
import io.seata.core.exception.TransactionException;

/**
 * The type Connection context.
 *
 * @author funkye
 */
public class JedisContext {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Map<String, String> applicationData = new HashMap<>(2, 1.0001f);
    /**
     * the lock keys buffer
     */
    private final Set<String> lockKeysBuffer = new LinkedHashSet<>();
    /**
     * the undo items buffer
     */
    private final List<KVUndolog> kvUndoItemsBuffer = new ArrayList<>();
    private String xid;
    private Long branchId;
    private boolean isGlobalLockRequire;

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
        lockKeysBuffer.add(lockKey);
    }

    /**
     * Append undo item.
     *
     * @param kvUndolog the kv undo log
     */
    void appendUndoItem(KVUndolog kvUndolog) {
        kvUndoItemsBuffer.add(kvUndolog);
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
                throw new ShouldNeverHappenException(
                    String.format("bind xid: %s, while current xid: %s", xid, this.xid));
            }
        }
    }

    /**
     * Has undo log boolean.
     *
     * @return the boolean
     */
    public boolean hasUndoLog() {
        return !kvUndoItemsBuffer.isEmpty();
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
     * Gets applicationData.
     *
     * @return the application data
     */
    public String getApplicationData() throws TransactionException {
        try {
            applicationData.put(REDIS_TX_LOG, MAPPER.writeValueAsString(getUndoItems()));
            return MAPPER.writeValueAsString(applicationData);
        } catch (JsonProcessingException e) {
            throw new TransactionException(e.getMessage(), e);
        }
    }

    public JedisContext reloadApplicationData(String applicationData) throws TransactionException {
        try {
            this.applicationData.putAll(MAPPER.readValue(applicationData, new TypeReference<Map<String, String>>() {}));
            String txLog = this.applicationData.get(REDIS_TX_LOG);
            if (StringUtils.isNotBlank(txLog)) {
                CollectionType javaType = MAPPER.getTypeFactory().constructCollectionType(List.class, KVUndolog.class);
                this.kvUndoItemsBuffer.addAll(MAPPER.readValue(txLog, javaType));
            }
        } catch (JsonProcessingException e) {
            throw new TransactionException(e.getMessage(), e);
        }
        return this;
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
        lockKeysBuffer.clear();
        kvUndoItemsBuffer.clear();
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
        Set<String> lockKeysBufferSet = lockKeysBuffer;

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
    public List<KVUndolog> getUndoItems() {
        return kvUndoItemsBuffer;
    }

    /**
     * Check whether all the before image is empty.
     *
     * @return if all is empty, return true
     */
    private boolean allBeforeImageEmpty() {
        for (KVUndolog undoLog : kvUndoItemsBuffer) {
            if (StringUtils.isNotBlank(undoLog.getBeforeValue())) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return StringUtils.toString(this);
    }

}
