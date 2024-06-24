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
package io.seata.saga.statelang.domain.impl;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import io.seata.saga.statelang.domain.RecoverStrategy;
import io.seata.saga.statelang.domain.State;
import io.seata.saga.statelang.domain.StateMachine;

/**
 * The type State machine.
 */
@Deprecated
public class StateMachineImpl implements StateMachine {

    private final org.apache.seata.saga.statelang.domain.StateMachine actual;

    private StateMachineImpl(org.apache.seata.saga.statelang.domain.StateMachine actual) {
        this.actual = actual;
    }

    @Override
    public String getName() {
        return actual.getName();
    }

    @Override
    public String getComment() {
        return actual.getComment();
    }

    @Override
    public String getStartState() {
        return actual.getStartState();
    }

    @Override
    public void setStartState(String startState) {
        actual.setStartState(startState);
    }

    @Override
    public String getVersion() {
        return actual.getVersion();
    }

    @Override
    public void setVersion(String version) {
        actual.setVersion(version);
    }

    @Override
    public Map<String, State> getStates() {
        Map<String, org.apache.seata.saga.statelang.domain.State> states = actual.getStates();
        if (states == null) {
            return null;
        }

        Map<String, State> resultMap = new LinkedHashMap<>();
        for (Map.Entry<String, org.apache.seata.saga.statelang.domain.State> entry : states.entrySet()) {
            org.apache.seata.saga.statelang.domain.State state = entry.getValue();
            resultMap.put(entry.getKey(), StateImpl.wrap(state));
        }
        return resultMap;
    }

    @Override
    public State getState(String name) {
        org.apache.seata.saga.statelang.domain.State state = actual.getState(name);
        return StateImpl.wrap(state);
    }

    @Override
    public String getId() {
        return actual.getId();
    }

    @Override
    public void setId(String id) {
        actual.setId(id);
    }

    @Override
    public String getTenantId() {
        return actual.getTenantId();
    }

    @Override
    public void setTenantId(String tenantId) {
        actual.setTenantId(tenantId);
    }

    @Override
    public String getAppName() {
        return actual.getAppName();
    }

    @Override
    public String getType() {
        return actual.getType();
    }

    @Override
    public Status getStatus() {
        org.apache.seata.saga.statelang.domain.StateMachine.Status status = actual.getStatus();
        return Status.wrap(status);
    }

    @Override
    public RecoverStrategy getRecoverStrategy() {
        org.apache.seata.saga.statelang.domain.RecoverStrategy recoverStrategy = actual.getRecoverStrategy();
        return RecoverStrategy.wrap(recoverStrategy);
    }

    @Override
    public void setRecoverStrategy(RecoverStrategy recoverStrategy) {
        org.apache.seata.saga.statelang.domain.RecoverStrategy unwrap = recoverStrategy.unwrap();
        actual.setRecoverStrategy(unwrap);
    }

    @Override
    public boolean isPersist() {
        return actual.isPersist();
    }

    @Override
    public Boolean isRetryPersistModeUpdate() {
        return actual.isRetryPersistModeUpdate();
    }

    @Override
    public Boolean isCompensatePersistModeUpdate() {
        return actual.isCompensatePersistModeUpdate();
    }

    @Override
    public String getContent() {
        return actual.getContent();
    }

    @Override
    public void setContent(String content) {
        actual.setContent(content);
    }

    @Override
    public Date getGmtCreate() {
        return actual.getGmtCreate();
    }

    @Override
    public void setGmtCreate(Date date) {
        actual.setGmtCreate(date);
    }

    /**
     * Wrap state machine.
     *
     * @param target the target
     * @return the state machine
     */
    public static StateMachineImpl wrap(org.apache.seata.saga.statelang.domain.StateMachine target) {
        if (target == null) {
            return null;
        }
        return new StateMachineImpl(target);
    }

    /**
     * Unwrap org . apache . seata . saga . statelang . domain . state machine.
     *
     * @return the org . apache . seata . saga . statelang . domain . state machine
     */
    public org.apache.seata.saga.statelang.domain.StateMachine unwrap() {
        return actual;
    }
}
