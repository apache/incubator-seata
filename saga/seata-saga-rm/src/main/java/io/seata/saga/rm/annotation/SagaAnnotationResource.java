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
package io.seata.saga.rm.annotation;

import io.seata.saga.rm.SagaResource;

import java.lang.reflect.Method;

/**
 * Saga annotation mode resource (Only register application as a saga annotation mode resource)
 *
 * @author ruishansun
 */
public class SagaAnnotationResource extends SagaResource {

    private Object targetBean;

    private String actionName;

    private Method commitMethod;

    private Method compensationMethod;

    private String compensationMethodName;

    private Class<?>[] compensationArgsClasses;

    private String[] phaseTwoCompensationKeys;

    public Object getTargetBean() {
        return targetBean;
    }

    public void setTargetBean(Object targetBean) {
        this.targetBean = targetBean;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public Method getCommitMethod() {
        return commitMethod;
    }

    public void setCommitMethod(Method commitMethod) {
        this.commitMethod = commitMethod;
    }

    public Method getCompensationMethod() {
        return compensationMethod;
    }

    public void setCompensationMethod(Method compensationMethod) {
        this.compensationMethod = compensationMethod;
    }

    public String getCompensationMethodName() {
        return compensationMethodName;
    }

    public void setCompensationMethodName(String compensationMethodName) {
        this.compensationMethodName = compensationMethodName;
    }

    public Class<?>[] getCompensationArgsClasses() {
        return compensationArgsClasses;
    }

    public void setCompensationArgsClasses(Class<?>[] compensationArgsClasses) {
        this.compensationArgsClasses = compensationArgsClasses;
    }

    public String[] getPhaseTwoCompensationKeys() {
        return phaseTwoCompensationKeys;
    }

    public void setPhaseTwoCompensationKeys(String[] phaseTwoCompensationKeys) {
        this.phaseTwoCompensationKeys = phaseTwoCompensationKeys;
    }
}
