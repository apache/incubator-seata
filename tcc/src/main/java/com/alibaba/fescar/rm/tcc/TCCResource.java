package com.alibaba.fescar.rm.tcc;

import com.alibaba.fescar.core.model.BranchType;
import com.alibaba.fescar.core.model.Resource;

import java.lang.reflect.Method;

/**
 *
 *
 * @author zhangsen
 */
public class TCCResource implements Resource {

    private String resourceGroupId = "DEFAULT";

    /**
     * 应用名称
     */
    private String appName;

    /**
     * TCC 参与者名称，全局唯一
     */
    private String actionName;

    /**
     * TCC 服务实现类
     */
    private Object targetBean;

    /**
     * Prepare method
     */
    private Method prepareMethod;

    /**
     * 提交方法
     */
    private Method commitMethod;

    /**
     * 回滚方法
     */
    private Method rollbackMethod;


    @Override
    public String getResourceGroupId() {
        return resourceGroupId;
    }

    public void setResourceGroupId(String resourceGroupId) {
        this.resourceGroupId = resourceGroupId;
    }

    @Override
    public String getResourceId() {
        return actionName;
    }

    @Override
    public BranchType getBranchType() {
        return BranchType.TCC;
    }


    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public Object getTargetBean() {
        return targetBean;
    }

    public void setTargetBean(Object targetBean) {
        this.targetBean = targetBean;
    }

    public Method getPrepareMethod() {
        return prepareMethod;
    }

    public void setPrepareMethod(Method prepareMethod) {
        this.prepareMethod = prepareMethod;
    }

    public Method getCommitMethod() {
        return commitMethod;
    }

    public void setCommitMethod(Method commitMethod) {
        this.commitMethod = commitMethod;
    }

    public Method getRollbackMethod() {
        return rollbackMethod;
    }

    public void setRollbackMethod(Method rollbackMethod) {
        this.rollbackMethod = rollbackMethod;
    }
}
