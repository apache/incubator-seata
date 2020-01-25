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
package io.seata.saga.statelang.domain.impl;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.ServiceTaskState;

/**
 * A state used to invoke a service
 *
 * @author lorne.cl
 */
public class ServiceTaskStateImpl extends AbstractTaskState implements ServiceTaskState {

    private String serviceType;
    private String serviceName;
    private String serviceMethod;
    private List<String> parameterTypes;
    private Method method;
    private List<Object> inputExpressions;
    private Map<String, Object> outputExpressions;
    private Map<Object, String> statusEvaluators;
    private boolean isAsync;

    public ServiceTaskStateImpl() {
        setType(DomainConstants.STATE_TYPE_SERVICE_TASK);
    }

    @Override
    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public String getServiceMethod() {
        return serviceMethod;
    }

    public void setServiceMethod(String serviceMethod) {
        this.serviceMethod = serviceMethod;
    }

    @Override
    public List<String> getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(List<String> parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public List<Object> getInputExpressions() {
        return inputExpressions;
    }

    public void setInputExpressions(List<Object> inputExpressions) {
        this.inputExpressions = inputExpressions;
    }

    public Map<String, Object> getOutputExpressions() {
        return outputExpressions;
    }

    public void setOutputExpressions(Map<String, Object> outputExpressions) {
        this.outputExpressions = outputExpressions;
    }

    public Map<Object, String> getStatusEvaluators() {
        return statusEvaluators;
    }

    public void setStatusEvaluators(Map<Object, String> statusEvaluators) {
        this.statusEvaluators = statusEvaluators;
    }

    public boolean isAsync() {
        return isAsync;
    }

    public void setAsync(boolean async) {
        isAsync = async;
    }
}