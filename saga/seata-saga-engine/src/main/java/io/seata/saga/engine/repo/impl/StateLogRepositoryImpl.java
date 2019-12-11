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
package io.seata.saga.engine.repo.impl;

import java.util.List;

import io.seata.saga.engine.repo.StateLogRepository;
import io.seata.saga.engine.store.StateLogStore;
import io.seata.saga.statelang.domain.StateInstance;
import io.seata.saga.statelang.domain.StateMachineInstance;

/**
 * State Log Repository
 *
 * @author lorne.cl
 */
public class StateLogRepositoryImpl implements StateLogRepository {

    private StateLogStore stateLogStore;

    @Override
    public StateMachineInstance getStateMachineInstance(String stateMachineInstanceId) {
        if (stateLogStore == null) {
            return null;
        }
        return stateLogStore.getStateMachineInstance(stateMachineInstanceId);
    }

    @Override
    public StateMachineInstance getStateMachineInstanceByBusinessKey(String businessKey, String tenantId) {
        if (stateLogStore == null) {
            return null;
        }
        return stateLogStore.getStateMachineInstanceByBusinessKey(businessKey, tenantId);
    }

    @Override
    public List<StateMachineInstance> queryStateMachineInstanceByParentId(String parentId) {
        if (stateLogStore == null) {
            return null;
        }
        return stateLogStore.queryStateMachineInstanceByParentId(parentId);
    }

    @Override
    public StateInstance getStateInstance(String stateInstanceId, String machineInstId) {
        if (stateLogStore == null) {
            return null;
        }
        return stateLogStore.getStateInstance(stateInstanceId, machineInstId);
    }

    @Override
    public List<StateInstance> queryStateInstanceListByMachineInstanceId(String stateMachineInstanceId) {
        if (stateLogStore == null) {
            return null;
        }
        return stateLogStore.queryStateInstanceListByMachineInstanceId(stateMachineInstanceId);
    }

    public void setStateLogStore(StateLogStore stateLogStore) {
        this.stateLogStore = stateLogStore;
    }
}