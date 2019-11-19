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

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import io.seata.saga.statelang.domain.State;
import io.seata.saga.statelang.domain.StateMachine;

/**
 * state machine
 *
 * @author lorne.cl
 */
public class StateMachineImpl implements StateMachine {

    private String id;
    private String tenantId;
    private String appName = "SEATA";
    private String name;
    private String comment;
    private String version;
    private String startState;
    private Status status = Status.AC;
    private String recoverStrategy;
    private boolean isPersist = true;
    private String type = "STATE_LANG";
    private transient String content;
    private Date gmtCreate;
    private Map<String, State> states = new LinkedHashMap<>();

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public String getStartState() {
        return startState;
    }

    @Override
    public void setStartState(String startState) {
        this.startState = startState;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public Map<String, State> getStates() {
        return states;
    }

    public void setStates(Map<String, State> states) {
        this.states = states;
    }

    @Override
    public State getState(String name) {
        return states.get(name);
    }

    public void putState(String stateName, State state) {
        this.states.put(stateName, state);
        if (state instanceof BaseState) {
            ((BaseState)state).setStateMachine(this);
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public String getRecoverStrategy() {
        return recoverStrategy;
    }

    @Override
    public void setRecoverStrategy(String recoverStrategy) {
        this.recoverStrategy = recoverStrategy;
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public boolean isPersist() {
        return isPersist;
    }

    public void setPersist(boolean persist) {
        isPersist = persist;
    }

    @Override
    public Date getGmtCreate() {
        return gmtCreate;
    }

    @Override
    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }
}