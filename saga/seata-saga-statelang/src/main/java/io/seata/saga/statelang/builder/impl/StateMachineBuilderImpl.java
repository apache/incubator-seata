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

package io.seata.saga.statelang.builder.impl;

import io.seata.saga.statelang.builder.StateMachineBuilder;
import io.seata.saga.statelang.builder.StatesConfigurer;
import io.seata.saga.statelang.domain.RecoverStrategy;
import io.seata.saga.statelang.domain.StateMachine;
import io.seata.saga.statelang.domain.impl.StateMachineImpl;
import io.seata.saga.statelang.parser.utils.StateMachineUtils;

/**
 * Default implementation for {@link StateMachineBuilder}.
 *
 * @author ptyin
 */
public class StateMachineBuilderImpl implements StateMachineBuilder {

    private final StateMachineImpl stateMachine = new StateMachineImpl();

    @Override
    public StateMachine build() {
        StateMachineUtils.parseAfterAll(stateMachine);
        return stateMachine;
    }

    @Override
    public StatesConfigurer withStates() {
        StatesConfigurerImpl statesConfigurer = new StatesConfigurerImpl();
        statesConfigurer.setParent(this);
        statesConfigurer.setStateMachine(stateMachine);
        return statesConfigurer;
    }

    @Override
    public StateMachineBuilder withName(String name) {
        stateMachine.setName(name);
        return this;
    }

    @Override
    public StateMachineBuilder withComment(String comment) {
        stateMachine.setComment(comment);
        return this;
    }

    @Override
    public StateMachineBuilder withVersion(String version) {
        stateMachine.setVersion(version);
        return this;
    }

    @Override
    public StateMachineBuilder withStartState(String stateName) {
        stateMachine.setStartState(stateName);
        return this;
    }

    @Override
    public StateMachineBuilder withRecoverStrategy(RecoverStrategy recoverStrategy) {
        stateMachine.setRecoverStrategy(recoverStrategy);
        return this;
    }

    @Override
    public StateMachineBuilder withPersist(boolean persist) {
        stateMachine.setPersist(persist);
        return this;
    }

    @Override
    public StateMachineBuilder withRetryPersistModeUpdate(boolean retryPersistModeUpdate) {
        stateMachine.setRetryPersistModeUpdate(retryPersistModeUpdate);
        return this;
    }

    @Override
    public StateMachineBuilder withCompensatePersistModeUpdate(boolean compensatePersistModeUpdate) {
        stateMachine.setCompensatePersistModeUpdate(compensatePersistModeUpdate);
        return this;
    }
}
