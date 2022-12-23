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

import io.seata.commonapi.api.BusinessActionContextUtil;
import io.seata.core.model.BranchType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * The type Business action context.
 */
@Deprecated
public class BusinessActionContext extends io.seata.commonapi.api.BusinessActionContext {

    private io.seata.commonapi.api.BusinessActionContext businessActionContext;

    /**
     * Instantiates a new Business action context.
     */
    @Deprecated
    public BusinessActionContext() {
        this.businessActionContext = new io.seata.commonapi.api.BusinessActionContext();
    }

    /**
     * Instantiates a new Business action context.
     *
     * @param xid           the xid
     * @param branchId      the branch id
     * @param actionContext the action context
     */
    @Deprecated
    public BusinessActionContext(String xid, String branchId, Map<String, Object> actionContext) {
        this.businessActionContext = new io.seata.commonapi.api.BusinessActionContext(xid, branchId, actionContext);
    }

    @Deprecated
    public BusinessActionContext(io.seata.commonapi.api.BusinessActionContext businessActionContext) {
        this.businessActionContext = businessActionContext;
    }

    /**
     * Gets action context.
     *
     * @param key the key
     * @return the action context
     */
    @Nullable
    public Object getActionContext(String key) {
        return businessActionContext.getActionContext(key);
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
        return businessActionContext.getActionContext(key, targetClazz);
    }

    /**
     * Gets branch id.
     *
     * @return the branch id
     */
    public long getBranchId() {
        return businessActionContext.getBranchId();
    }

    /**
     * Sets branch id.
     *
     * @param branchId the branch id
     */
    public void setBranchId(long branchId) {
        businessActionContext.setBranchId(branchId);
    }

    /**
     * Sets branch id.
     *
     * @param branchId the branch id
     */
    public void setBranchId(String branchId) {
        businessActionContext.setBranchId(branchId);
    }

    /**
     * Gets action context.
     *
     * @return the action context
     */
    public Map<String, Object> getActionContext() {
        return businessActionContext.getActionContext();
    }

    /**
     * Sets action context.
     *
     * @param actionContext the action context
     */
    public void setActionContext(Map<String, Object> actionContext) {
        businessActionContext.setActionContext(actionContext);
    }

    /**
     * Gets xid.
     *
     * @return the xid
     */
    public String getXid() {
        return businessActionContext.getXid();
    }

    /**
     * Sets xid.
     *
     * @param xid the xid
     */
    public void setXid(String xid) {
        businessActionContext.setXid(xid);
    }

    /**
     * Gets action name.
     *
     * @return the action name
     */
    public String getActionName() {
        return businessActionContext.getActionName();
    }

    /**
     * Sets action name.
     *
     * @param actionName the action name
     */
    public void setActionName(String actionName) {
        businessActionContext.setActionName(actionName);
    }

    /**
     * add actionContext
     *
     * @param key   the action context's key
     * @param value biz value
     * @return the action context is changed
     * @see io.seata.commonapi.api.BusinessActionContextUtil // the TCC API utils
     * @deprecated Don't use this method in the `Try` method. Please use {@link BusinessActionContextUtil#addContext}
     */
    @Deprecated
    public boolean addActionContext(String key, Object value) {
        return businessActionContext.addActionContext(key, value);
    }

    public Boolean getDelayReport() {
        return businessActionContext.getDelayReport();
    }

    public void setDelayReport(Boolean delayReport) {
        businessActionContext.setDelayReport(delayReport);
    }

    public Boolean getUpdated() {
        return businessActionContext.getUpdated();
    }

    public void setUpdated(Boolean updated) {
        businessActionContext.setUpdated(updated);
    }

    public BranchType getBranchType() {
        return businessActionContext.getBranchType();
    }

    public void setBranchType(BranchType branchType) {
        businessActionContext.setBranchType(branchType);
    }


}