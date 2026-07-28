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
package org.apache.seata.rm.tcc.api;

import org.apache.seata.common.Constants;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.integration.tx.api.interceptor.ActionContextUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

/**
 * The type Business action context.
 */
public class BusinessActionContext implements Serializable {

    private static final long serialVersionUID = 6539226288677737991L;

    private String xid;

    private String branchId;

    private String actionName;

    /**
     * delay branch report while sharing params to tcc phase 2 to enhance performance
     *
     * @see BusinessActionContextUtil
     * @see org.apache.seata.rm.tcc.api.TwoPhaseBusinessAction
     */
    private Boolean isDelayReport;

    /**
     * mark that actionContext has been updated by business
     *
     * @see BusinessActionContextUtil
     */
    private Boolean isUpdated;

    /**
     * branch Type
     */
    private BranchType branchType;

    /**
     * action context
     */
    private Map<String, Object> actionContext;

    /**
     * whether the action context should stay tracked on mutation
     */
    private transient boolean actionContextTrackingEnabled;

    /**
     * Instantiates a new Business action context.
     */
    public BusinessActionContext() {}

    /**
     * Instantiates a new Business action context.
     *
     * @param xid           the xid
     * @param branchId      the branch id
     * @param actionContext the action context
     */
    public BusinessActionContext(String xid, String branchId, Map<String, Object> actionContext) {
        this.xid = xid;
        this.branchId = branchId;
        this.actionContext = actionContext;
    }

    /**
     * Gets action context.
     *
     * @param key the key
     * @return the action context
     */
    @Nullable
    public Object getActionContext(String key) {
        return actionContext.get(key);
    }

    /**
     * Gets action context.
     *
     * @param key         the key
     * @param targetClazz the target class
     * @param <T>         the target type
     * @return the action context of the target type
     */
    @Nullable
    public <T> T getActionContext(String key, @Nonnull Class<T> targetClazz) {
        Object value = actionContext.get(key);
        return ActionContextUtil.convertActionContext(key, value, targetClazz);
    }

    /**
     * Gets branch id.
     *
     * @return the branch id
     */
    public long getBranchId() {
        return branchId != null ? Long.parseLong(branchId) : -1;
    }

    /**
     * Sets branch id.
     *
     * @param branchId the branch id
     */
    public void setBranchId(long branchId) {
        this.branchId = String.valueOf(branchId);
    }

    /**
     * Sets branch id.
     *
     * @param branchId the branch id
     */
    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    /**
     * Gets action context.
     *
     * @return the action context
     */
    public Map<String, Object> getActionContext() {
        return actionContext;
    }

    /**
     * Sets action context.
     *
     * @param actionContext the action context
     */
    public void setActionContext(Map<String, Object> actionContext) {
        if (actionContextTrackingEnabled) {
            this.actionContext = actionContext == null
                    ? new TrackedActionContextMap(this)
                    : new TrackedActionContextMap(this, actionContext);
            return;
        }
        this.actionContext = actionContext;
    }

    /**
     * Enable automatic updated tracking for action context mutations.
     */
    public void enableActionContextTracking() {
        actionContextTrackingEnabled = true;
        if (actionContext == null) {
            actionContext = new TrackedActionContextMap(this);
        } else if (!(actionContext instanceof TrackedActionContextMap)) {
            actionContext = new TrackedActionContextMap(this, actionContext);
        }
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
     * Gets action name.
     *
     * @return the action name
     */
    public String getActionName() {
        return actionName;
    }

    /**
     * Sets action name.
     *
     * @param actionName the action name
     */
    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    /**
     * add actionContext
     *
     * @param key   the action context's key
     * @param value biz value
     * @return the action context is changed
     * @see BusinessActionContextUtil // the TCC API utils
     * @deprecated Don't use this method in the `Try` method. Please use {@link BusinessActionContextUtil#addContext}
     */
    @Deprecated
    public boolean addActionContext(String key, Object value) {
        if (value == null) {
            return false;
        }

        Object previousValue = this.actionContext.put(key, value);
        boolean isChanged = !value.equals(previousValue);
        if (isChanged) {
            this.setUpdated(true);
        }
        return isChanged;
    }

    public Boolean getDelayReport() {
        return isDelayReport;
    }

    public void setDelayReport(Boolean delayReport) {
        isDelayReport = delayReport;
    }

    public Boolean getUpdated() {
        return isUpdated;
    }

    public void setUpdated(Boolean updated) {
        isUpdated = updated;
    }

    public BranchType getBranchType() {
        return branchType;
    }

    public void setBranchType(BranchType branchType) {
        this.branchType = branchType;
    }

    /**
     * Gets action status.
     *
     * @return the action status
     */
    public String getActionStatus() {
        if (actionContext == null) {
            return null;
        }
        Object status = actionContext.get(Constants.ACTION_STATUS);
        return status != null ? status.toString() : null;
    }

    /**
     * Sets action status.
     *
     * @param status the action status
     */
    public void setActionStatus(String status) {
        if (actionContext != null && status != null) {
            actionContext.put(Constants.ACTION_STATUS, status);
        }
    }

    private void markUpdatedOnActionContextMutation() {
        setUpdated(true);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[xid:")
                .append(xid)
                .append(",branch_Id:")
                .append(branchId)
                .append(",action_name:")
                .append(actionName)
                .append(",is_delay_report:")
                .append(isDelayReport)
                .append(",is_updated:")
                .append(isDelayReport)
                .append(",branch_type:")
                .append(branchType)
                .append(",action_context:")
                .append(actionContext)
                .append("]");
        return sb.toString();
    }

    /**
     * The tracked action context map.
     */
    private static final class TrackedActionContextMap extends AbstractMap<String, Object> implements Serializable {

        private static final long serialVersionUID = 1L;

        private final BusinessActionContext owner;

        private final Map<String, Object> delegate;

        private TrackedActionContextMap(BusinessActionContext owner) {
            this.owner = owner;
            this.delegate = new HashMap<>(8);
        }

        private TrackedActionContextMap(BusinessActionContext owner, Map<String, Object> source) {
            this.owner = owner;
            this.delegate = new HashMap<>(source);
        }

        @Override
        public Object put(String key, Object value) {
            boolean hadKey = delegate.containsKey(key);
            Object previousValue = delegate.put(key, value);
            if (!hadKey || !Objects.equals(previousValue, value)) {
                owner.markUpdatedOnActionContextMutation();
            }
            return previousValue;
        }

        @Override
        public void putAll(Map<? extends String, ? extends Object> m) {
            Objects.requireNonNull(m, "m");
            if (m.isEmpty()) {
                return;
            }
            for (Map.Entry<? extends String, ? extends Object> entry : m.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        }

        @Override
        public Object remove(Object key) {
            boolean hadKey = delegate.containsKey(key);
            Object previousValue = delegate.remove(key);
            if (hadKey) {
                owner.markUpdatedOnActionContextMutation();
            }
            return previousValue;
        }

        @Override
        public void clear() {
            if (!delegate.isEmpty()) {
                delegate.clear();
                owner.markUpdatedOnActionContextMutation();
            }
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            return new AbstractSet<Entry<String, Object>>() {
                @Override
                public Iterator<Entry<String, Object>> iterator() {
                    Iterator<Entry<String, Object>> iterator =
                            delegate.entrySet().iterator();
                    return new Iterator<Entry<String, Object>>() {
                        @Override
                        public boolean hasNext() {
                            return iterator.hasNext();
                        }

                        @Override
                        public Entry<String, Object> next() {
                            Entry<String, Object> current = iterator.next();
                            return new TrackingEntry(current);
                        }

                        @Override
                        public void remove() {
                            iterator.remove();
                            owner.markUpdatedOnActionContextMutation();
                        }
                    };
                }

                // The following methods delegate directly to the underlying Map.
                @Override
                public int size() {
                    return delegate.size();
                }

                @Override
                public boolean remove(Object o) {
                    if (!(o instanceof Entry)) {
                        return false;
                    }
                    Entry<?, ?> entry = (Entry<?, ?>) o;
                    if (!delegate.containsKey(entry.getKey())) {
                        return false;
                    }
                    if (!Objects.equals(delegate.get(entry.getKey()), entry.getValue())) {
                        return false;
                    }
                    TrackedActionContextMap.this.remove(entry.getKey());
                    return true;
                }
            };
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean containsKey(Object key) {
            return delegate.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return delegate.containsValue(value);
        }

        @Override
        public Object get(Object key) {
            return delegate.get(key);
        }

        private final class TrackingEntry implements Entry<String, Object> {
            private final Entry<String, Object> delegateEntry;

            private TrackingEntry(Entry<String, Object> delegateEntry) {
                this.delegateEntry = delegateEntry;
            }

            @Override
            public String getKey() {
                return delegateEntry.getKey();
            }

            @Override
            public Object getValue() {
                return delegateEntry.getValue();
            }

            @Override
            public Object setValue(Object value) {
                Object previousValue = delegateEntry.setValue(value);
                if (!Objects.equals(previousValue, value)) {
                    owner.markUpdatedOnActionContextMutation();
                }
                return previousValue;
            }

            @Override
            public boolean equals(Object o) {
                return delegateEntry.equals(o);
            }

            @Override
            public int hashCode() {
                return delegateEntry.hashCode();
            }
        }
    }
}
