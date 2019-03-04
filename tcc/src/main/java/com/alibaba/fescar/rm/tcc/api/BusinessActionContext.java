package com.alibaba.fescar.rm.tcc.api;

import java.io.Serializable;
import java.util.Map;

/**
 * TCC参与者上下文信息
 * @author zhangsen
 *
 */
public class BusinessActionContext implements Serializable {

    /**  */
    private static final long       serialVersionUID = 6539226288677737991L;

    /**
     * 事务号
     */
    private String                  txId;

    /**
     * 业务活动参与者编号，全局唯一
     */
    private String                  actionId;

    /**
     * 当前参与者的名称
     */
    private String                  actionName;
    /**
     * 发起方的上下文信息，某些场景下参与者可以感知发起方的全局上下文信息用来做一些特殊控制，比如
     * 1.事务开启时间
     * 2.事务开启的服务器机器名
     * 3.数据库的failover信息
     * 4.其他任何业务需要透传的参数
     */
    private BusinessActivityContext activityContext;

    /**
     * 参与者自己的参数信息，通过@BusinessActionContextParameter设置
     * 比如一阶段，二阶段都需要的分库信息
     */
    private Map<String, Object> actionContext;

    public BusinessActionContext() {
    }

    public BusinessActionContext(String txId, String actionName, Map<String, Object> actionContext) {
        this.txId = txId;
        this.actionName = actionName;
        this.setActionContext(actionContext);
    }

    public BusinessActionContext(String txId, String actionId, BusinessActivityContext activityContext,
                                 Map<String, Object> actionContext) {
        this.txId = txId;
        this.setActionId(actionId);
        this.setActivityContext(activityContext);
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
     * 获取activity级别的参数
     * 
     * @param key
     * @return
     */
    public Object getActivityContext(String key) {
        return activityContext.getContext(key);
    }

    /**
     * 获取本次分布式事务的开启时间
     * @return
     */
    public Long getStartTime() {
        return getActivityContext().fetchStartTime();
    }
    
    public String getXid(){
    	return txId;
    }
    
    public void setXid(String txId) {
        this.txId = txId;
    }

    public String getTxId() {
        return txId;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public String getActionId() {
        return actionId;
    }

    public void setActionId(String actionId) {
        this.actionId = actionId;
    }
    
    /**
     * Gets branch id.
     *
     * @return the branch id
     */
    public long getBranchId() {
        return actionId!=null?Long.valueOf(actionId):-1;
    }

    /**
     * Sets branch id.
     *
     * @param branchId the branch id
     */
    public void setBranchId(long branchId) {
        this.actionId = String.valueOf(branchId);
    }

    public Map<String, Object> getActionContext() {
        return actionContext;
    }

    public void setActionContext(Map<String, Object> actionContext) {
        this.actionContext = actionContext;
    }

    public BusinessActivityContext getActivityContext() {
        return activityContext;
    }

    public void setActivityContext(BusinessActivityContext activityContext) {
        this.activityContext = activityContext;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[tx_id:").append(txId)
            .append(",action_id:").append(actionId).append(",action_name:").append(actionName)
            .append(",activity_context:").append(activityContext).append(",action_context:")
            .append(actionContext).append("]");
        return sb.toString();
    }
}
