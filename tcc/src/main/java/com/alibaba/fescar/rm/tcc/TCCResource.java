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
    private String commitMethodName;

    /**
     * 提交方法
     */
    private Method commitMethod;

    /**
     * 回滚方法
     */
    private String rollbackMethodName;

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

    public String getCommitMethodName() {
        return commitMethodName;
    }

    public void setCommitMethodName(String commitMethodName) {
        this.commitMethodName = commitMethodName;
    }

    public String getRollbackMethodName() {
        return rollbackMethodName;
    }

    public void setRollbackMethodName(String rollbackMethodName) {
        this.rollbackMethodName = rollbackMethodName;
    }

    @Override
    public int hashCode() {
        return actionName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof TCCResource)){
            return false;
        }
        return this.actionName.equals(((TCCResource)obj).actionName);
    }
}
