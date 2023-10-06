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
package io.seata.rm.tcc.api;

import java.io.Serializable;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.seata.core.model.BranchType;
import io.seata.integration.tx.api.interceptor.ActionContextUtil;


/**
 * The type Business action context.
 */
public class BusinessActionContext implements Serializable {

    private static final long serialVersionUID = 6539226288677737991L;

    private String xid;

    private String branchId;

    private String actionName;

    /**
     * mark that actionContext has been updated by business
     *
     * @see BusinessActionContextUtil
     */
    private Boolean isUpdated;

    /**
     * mark that actionContext has been reported
     *
     */
    private Boolean isActionContextReported;

    /**
     * branch Type
     */
    private BranchType branchType;

    /**
     * action context
     */
    private Map<String, Object> actionContext;

    /**
     * Instantiates a new Business action context.
     */
    public BusinessActionContext() {
    }

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
        this.actionContext = actionContext;
        if (this.actionContext != actionContext) {
            this.actionContext = actionContext;
            this.setUpdated(true);
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

    public boolean getActionContextReported() {
        return Boolean.TRUE.equals(isActionContextReported);
    }

    public void setActionContextReported(Boolean reported) {
        isActionContextReported = reported;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[xid:").append(xid)
                .append(",branch_Id:").append(branchId)
                .append(",action_name:").append(actionName)
                .append(",is_updated:").append(isUpdated)
                .append(",is_reported").append(isActionContextReported)
                .append(",branch_type:").append(branchType)
                .append(",action_context:")
                .append(actionContext).append("]");
        return sb.toString();
    }
}