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
package com.alibaba.fescar.rm.tcc.api;

import java.io.Serializable;
import java.util.Map;

/**
 * TCC Method context
 *
 * @author zhangsen
 */
public class BusinessActionContext implements Serializable {

    /**  */
    private static final long       serialVersionUID = 6539226288677737991L;

    /**
     * xid
     */
    private String                  xid;

    /**
     * the branch id
     */
    private String                  branchId;

    /**
     * tcc bean name
     */
    private String                  actionName;

    /**
     * TCC's parameters witch is set by @BusinessActionContextParameter
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
     * @param xid the xid
     * @param actionName the action name
     * @param actionContext the action context
     */
    public BusinessActionContext(String xid, String actionName, Map<String, Object> actionContext) {
        this.xid = xid;
        this.actionName = actionName;
        this.setActionContext(actionContext);
    }

    /**
     * 获取action级别的参数
     *
     * @param key the key
     * @return action context
     */
    public Object getActionContext(String key) {
        return actionContext.get(key);
    }

    /**
     * Gets branch id.
     *
     * @return the branch id
     */
    public long getBranchId() {
        return branchId!=null?Long.valueOf(branchId):-1;
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
     * Sets branch id.
     *
     * @param branchId the branch id
     */
    public void setBranchId(String branchId) {
        this.branchId = branchId;
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[xid:").append(xid)
            .append(",branch_Id:").append(branchId).append(",action_name:").append(actionName)
            .append(",action_context:")
            .append(actionContext).append("]");
        return sb.toString();
    }
}
