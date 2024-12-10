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
package org.apache.seata.saga.rm;

import java.lang.reflect.Method;

import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.Resource;

/**
 * The type Saga annotation resource.
 */
public class SagaAnnotationResource implements Resource {

    private String resourceGroupId = "DEFAULT";

    private String appName;

    private String actionName;

    private Object targetBean;

    private String compensationMethodName;

    private Method compensationMethod;

    private Class<?>[] compensationArgsClasses;

    private String[] phaseTwoCompensationKeys;

    @Override
    public String getResourceGroupId() {
        return resourceGroupId;
    }

    /**
     * Sets resource group id.
     *
     * @param resourceGroupId the resource group id
     */
    public void setResourceGroupId(String resourceGroupId) {
        this.resourceGroupId = resourceGroupId;
    }

    @Override
    public String getResourceId() {
        return actionName;
    }

    @Override
    public BranchType getBranchType() {
        return BranchType.SAGA_ANNOTATION;
    }

    /**
     * Gets app name.
     *
     * @return the app name
     */
    public String getAppName() {
        return appName;
    }

    /**
     * Sets app name.
     *
     * @param appName the app name
     */
    public void setAppName(String appName) {
        this.appName = appName;
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
     * Gets target bean.
     *
     * @return the target bean
     */
    public Object getTargetBean() {
        return targetBean;
    }

    /**
     * Sets target bean.
     *
     * @param targetBean the target bean
     */
    public void setTargetBean(Object targetBean) {
        this.targetBean = targetBean;
    }

    /**
     * Gets compensation method.
     *
     * @return the rollback method
     */
    public Method getCompensationMethod() {
        return compensationMethod;
    }

    /**
     * Sets compensation method.
     *
     * @param compensationMethod the rollback method
     */
    public void setCompensationMethod(Method compensationMethod) {
        this.compensationMethod = compensationMethod;
    }

    /**
     * Gets compensation method name.
     *
     * @return the rollback method name
     */
    public String getCompensationMethodName() {
        return compensationMethodName;
    }

    /**
     * Sets compensation method name.
     *
     * @param compensationMethodName the rollback method name
     */
    public void setCompensationMethodName(String compensationMethodName) {
        this.compensationMethodName = compensationMethodName;
    }

    /**
     * get compensation method args
     *
     * @return class array
     */
    public Class<?>[] getCompensationArgsClasses() {
        return compensationArgsClasses;
    }

    /**
     * set compensation method args
     *
     * @param compensationArgsClasses rollbackArgsClasses
     */
    public void setCompensationArgsClasses(Class<?>[] compensationArgsClasses) {
        this.compensationArgsClasses = compensationArgsClasses;
    }

    /**
     * get compensation method args keys
     *
     * @return keys array
     */
    public String[] getPhaseTwoCompensationKeys() {
        return phaseTwoCompensationKeys;
    }

    /**
     * set compensation method args key
     *
     * @param phaseTwoCompensationKeys phaseTwoCompensationKeys
     */
    public void setPhaseTwoCompensationKeys(String[] phaseTwoCompensationKeys) {
        this.phaseTwoCompensationKeys = phaseTwoCompensationKeys;
    }

    @Override
    public int hashCode() {
        return actionName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SagaAnnotationResource)) {
            return false;
        }
        return this.actionName.equals(((SagaAnnotationResource) obj).actionName);
    }

}