package com.alibaba.fescar.rm.tcc.api;

import java.io.Serializable;
import java.util.Map;

/**
 * TCC Method context
 *
 * @author zhangsen
 *
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

    public BusinessActionContext() {
    }

    public BusinessActionContext(String xid, String actionName, Map<String, Object> actionContext) {
        this.xid = xid;
        this.actionName = actionName;
        this.setActionContext(actionContext);
    }

    /**
     * 获取action级别的参数
     * 
     * @param key
     * @return
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

    public Map<String, Object> getActionContext() {
        return actionContext;
    }

    public void setActionContext(Map<String, Object> actionContext) {
        this.actionContext = actionContext;
    }

    public String getXid() {
        return xid;
    }

    public void setXid(String xid) {
        this.xid = xid;
    }

    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[xid:").append(xid)
            .append(",branch_Id:").append(branchId).append(",action_name:").append(actionName)
            .append(",action_context:")
            .append(actionContext).append("]");
        return sb.toString();
    }
}
