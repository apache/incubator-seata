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

import io.seata.common.exception.FrameworkException;
import io.seata.saga.statelang.builder.*;
import io.seata.saga.statelang.domain.State;
import io.seata.saga.statelang.domain.impl.StateMachineImpl;

/**
 * Default implementation for  {@link StatesConfigurer}.
 *
 * @author ptyin
 */
public class StatesConfigurerImpl implements StatesConfigurer {

    private StateMachineBuilder parent;

    private StateMachineImpl stateMachine;

    @Override
    @SuppressWarnings("unchecked")
    public <B extends StateBuilder<B, ?>> B build(Class<B> clazz) {
        B builder;
        try {
            builder = (B) StateBuilderFactory.getStateBuilder(clazz);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new FrameworkException(e, "Given builder class is inappropriate or not implemented yet.");
        } catch (ClassCastException e) {
            throw new FrameworkException(e, "StateBuilderFactory got a wrong state builder.");
        }
        ((BaseStateBuilder<B, ?>) builder).setParent(this);
        return builder;
    }

    @Override
    public StatesConfigurer add(State state) {
        stateMachine.putState(state.getName(), state);
        return this;
    }

    @Override
    public StateMachineBuilder configure() {
        return parent;
    }

    public void setParent(StateMachineBuilder parent) {
        this.parent = parent;
    }

    public void setStateMachine(StateMachineImpl stateMachine) {
        this.stateMachine = stateMachine;
    }
}
