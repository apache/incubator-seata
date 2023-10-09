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

import java.util.List;

/**
 * State machine validator used to validate rules.
 *
 * @author ptyin
 */
public class StateMachineValidator {
    public void validate(StateMachine stateMachine) {
        List<Rule> rules = RuleFactory.getRules();
        for (Rule rule: rules) {
            boolean pass;
            try {
                pass = rule.validate(stateMachine);
            } catch (Throwable e) {
                throw new ValidationException(rule, "Exception occurs", e);
            }
            if (!pass) {
                throw new ValidationException(rule, "Failed");
            }
        }
    }
}
