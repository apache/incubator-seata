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

package io.seata.saga.statelang.validator;

import io.seata.saga.statelang.domain.StateMachine;

/**
 * Validation rule interface, use SPI to inject rules
 *
 * @author ptyin
 */
public interface Rule {
    /**
     * Validate a state machine
     *
     * @param stateMachine state machine
     * @return true if passes, false if fails
     */
    boolean validate(StateMachine stateMachine);

    /**
     * Get the rule name
     *
     * @return name of the rule
     */
    String getName();

    /**
     * Get hints why validation passes or fails. Use this method to show more messages about validation result.
     *
     * @return hint of the rule
     */
    String getHint();
}
