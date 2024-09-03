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
package io.seata.saga.engine.impl;

import java.util.Map;

import io.seata.saga.engine.AsyncCallback;
import io.seata.saga.engine.StateMachineConfig;
import io.seata.saga.engine.StateMachineEngine;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.proctrl.impl.ProcessContextImpl;
import io.seata.saga.statelang.domain.StateMachineInstance;
import io.seata.saga.statelang.domain.impl.StateMachineInstanceImpl;
import org.apache.seata.saga.engine.exception.EngineExecutionException;
import org.apache.seata.saga.engine.exception.ForwardInvalidException;

/**
 * ProcessCtrl-based state machine engine
 */
@Deprecated
public class ProcessCtrlStateMachineEngine implements StateMachineEngine {

    private final org.apache.seata.saga.engine.impl.ProcessCtrlStateMachineEngine actual =
            new org.apache.seata.saga.engine.impl.ProcessCtrlStateMachineEngine();

    @Override
    public StateMachineInstance start(String stateMachineName, String tenantId, Map<String, Object> startParams) throws EngineExecutionException {
        org.apache.seata.saga.statelang.domain.StateMachineInstance inst =
                actual.start(stateMachineName, tenantId, startParams);
        return StateMachineInstanceImpl.wrap(inst);
    }

    @Override
    public StateMachineInstance startWithBusinessKey(String stateMachineName, String tenantId, String businessKey, Map<String, Object> startParams) throws EngineExecutionException {
        org.apache.seata.saga.statelang.domain.StateMachineInstance inst =
                actual.startWithBusinessKey(stateMachineName, tenantId, businessKey, startParams);
        return StateMachineInstanceImpl.wrap(inst);
    }

    private org.apache.seata.saga.engine.AsyncCallback wrapCallback(AsyncCallback callback) {
        return new org.apache.seata.saga.engine.AsyncCallback() {
            @Override
            public void onFinished(org.apache.seata.saga.proctrl.ProcessContext context,
                                   org.apache.seata.saga.statelang.domain.StateMachineInstance stateMachineInstance) {
                ProcessContext compatibleContext = ProcessContextImpl.wrap((org.apache.seata.saga.proctrl.impl.ProcessContextImpl) context);
                callback.onFinished(compatibleContext, StateMachineInstanceImpl.wrap(stateMachineInstance));
            }

            @Override
            public void onError(org.apache.seata.saga.proctrl.ProcessContext context,
                                org.apache.seata.saga.statelang.domain.StateMachineInstance stateMachineInstance,
                                Exception exp) {
                ProcessContext compatibleContext = ProcessContextImpl.wrap((org.apache.seata.saga.proctrl.impl.ProcessContextImpl) context);
                callback.onError(compatibleContext, StateMachineInstanceImpl.wrap(stateMachineInstance), exp);
            }
        };
    }

    @Override
    public StateMachineInstance startAsync(String stateMachineName, String tenantId, Map<String, Object> startParams, AsyncCallback callback) throws EngineExecutionException {
        org.apache.seata.saga.statelang.domain.StateMachineInstance inst =
                actual.startAsync(stateMachineName, tenantId, startParams, wrapCallback(callback));
        return StateMachineInstanceImpl.wrap(inst);
    }

    @Override
    public StateMachineInstance startWithBusinessKeyAsync(String stateMachineName, String tenantId, String businessKey, Map<String, Object> startParams, AsyncCallback callback) throws EngineExecutionException {
        org.apache.seata.saga.statelang.domain.StateMachineInstance inst =
                actual.startWithBusinessKeyAsync(stateMachineName, tenantId, businessKey, startParams,
                        wrapCallback(callback));
        return StateMachineInstanceImpl.wrap(inst);
    }

    @Override
    public StateMachineInstance forward(String stateMachineInstId, Map<String, Object> replaceParams) throws ForwardInvalidException {
        org.apache.seata.saga.statelang.domain.StateMachineInstance inst =
                actual.forward(stateMachineInstId, replaceParams);
        return StateMachineInstanceImpl.wrap(inst);
    }

    @Override
    public StateMachineInstance forwardAsync(String stateMachineInstId, Map<String, Object> replaceParams, AsyncCallback callback) throws ForwardInvalidException {
        org.apache.seata.saga.statelang.domain.StateMachineInstance inst =
                actual.forwardAsync(stateMachineInstId, replaceParams, wrapCallback(callback));
        return StateMachineInstanceImpl.wrap(inst);
    }

    @Override
    public StateMachineInstance compensate(String stateMachineInstId, Map<String, Object> replaceParams) throws EngineExecutionException {
        org.apache.seata.saga.statelang.domain.StateMachineInstance inst =
                actual.compensate(stateMachineInstId, replaceParams);
        return StateMachineInstanceImpl.wrap(inst);
    }

    @Override
    public StateMachineInstance compensateAsync(String stateMachineInstId, Map<String, Object> replaceParams, AsyncCallback callback) throws EngineExecutionException {
        org.apache.seata.saga.statelang.domain.StateMachineInstance inst =
                actual.compensateAsync(stateMachineInstId, replaceParams, wrapCallback(callback));
        return StateMachineInstanceImpl.wrap(inst);
    }

    @Override
    public StateMachineInstance skipAndForward(String stateMachineInstId, Map<String, Object> replaceParams) throws EngineExecutionException {
        org.apache.seata.saga.statelang.domain.StateMachineInstance inst =
                actual.skipAndForward(stateMachineInstId, replaceParams);
        return StateMachineInstanceImpl.wrap(inst);
    }

    @Override
    public StateMachineInstance skipAndForwardAsync(String stateMachineInstId, AsyncCallback callback) throws EngineExecutionException {
        org.apache.seata.saga.statelang.domain.StateMachineInstance inst =
                actual.skipAndForwardAsync(stateMachineInstId, wrapCallback(callback));
        return StateMachineInstanceImpl.wrap(inst);
    }

    @Override
    public StateMachineConfig getStateMachineConfig() {
        return DefaultStateMachineConfig.wrap((org.apache.seata.saga.engine.impl.DefaultStateMachineConfig)
                actual.getStateMachineConfig());
    }

    public void setStateMachineConfig(StateMachineConfig stateMachineConfig) {
        actual.setStateMachineConfig(((DefaultStateMachineConfig) stateMachineConfig).unwrap());
    }

    @Override
    public StateMachineInstance reloadStateMachineInstance(String instId) {
        return StateMachineInstanceImpl.wrap(actual.reloadStateMachineInstance(instId));
    }
}
