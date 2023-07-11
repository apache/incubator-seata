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
package io.seata.saga.statelang.builder;

import io.seata.saga.statelang.domain.StateMachine;

/**
 * A Java stream-based {@link StateMachine} builder.
 *
 * @author PTYin
 */
public interface StateMachineBuilder
{
    /**
     * Build a state machine
     *
     * @return built state machine
     */
    StateMachine build();

    /**
     * Configure states.
     *
     * @return configurer for states
     */
    StatesConfigurer withStates();

    /**
     * Configure name.
     *
     * @param name name of state machine
     * @return builder for chaining
     */
    StateMachineBuilder withName(String name);

    /**
     * Configure comment.
     *
     * @param comment comment of state machine
     * @return builder for chaining
     */
    StateMachineBuilder withComment(String comment);

    /**
     * Configure version.
     *
     * @param version version of state machine
     * @return builder for chaining
     */
    StateMachineBuilder withVersion(String version);

    /**
     * Configure start state.
     *
     * @param stateName initial state name of state machine
     * @return builder for chaining
     */
    StateMachineBuilder withStartState(String stateName);
}
