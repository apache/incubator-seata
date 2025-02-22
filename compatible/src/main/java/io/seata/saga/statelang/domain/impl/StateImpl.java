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

import java.util.Map;

import io.seata.saga.statelang.domain.State;
import io.seata.saga.statelang.domain.StateMachine;
import io.seata.saga.statelang.domain.StateType;

/**
 * The type State.
 */
@Deprecated
public class StateImpl implements State {

    private final org.apache.seata.saga.statelang.domain.State actual;

    private StateImpl(org.apache.seata.saga.statelang.domain.State actual) {
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
    public StateType getType() {
        return StateType.wrap(actual.getType());
    }

    @Override
    public String getNext() {
        return actual.getNext();
    }

    @Override
    public Map<String, Object> getExtensions() {
        return actual.getExtensions();
    }

    @Override
    public StateMachine getStateMachine() {
        org.apache.seata.saga.statelang.domain.StateMachine stateMachine = actual.getStateMachine();
        return StateMachineImpl.wrap(stateMachine);
    }

    /**
     * Wrap state.
     *
     * @param target the target
     * @return the state
     */
    public static StateImpl wrap(org.apache.seata.saga.statelang.domain.State target) {
        if (target == null) {
            return null;
        }
        return new StateImpl(target);
    }

    /**
     * Unwrap org . apache . seata . saga . statelang . domain . state.
     *
     * @return the org . apache . seata . saga . statelang . domain . state
     */
    public org.apache.seata.saga.statelang.domain.State unwrap() {
        return actual;
    }
}
