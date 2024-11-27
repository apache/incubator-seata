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
package org.apache.seata.saga.statelang.parser.utils;

import org.apache.seata.common.util.StringUtils;
import org.apache.seata.saga.statelang.domain.ChoiceState;
import org.apache.seata.saga.statelang.domain.State;
import org.apache.seata.saga.statelang.domain.TaskState;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Util class for parsing StateMachine
 *
 */
public class StateMachineUtils {
    public static Set<String> getAllPossibleSubsequentStates(State state) {
        Set<String> subsequentStates = new HashSet<>();
        // Next state
        subsequentStates.add(state.getNext());
        switch (state.getType()) {
            case SCRIPT_TASK:
            case SERVICE_TASK:
            case SUB_STATE_MACHINE:
            case SUB_MACHINE_COMPENSATION:
                // Next state in catches
                Optional.ofNullable(((TaskState) state).getCatches())
                        .ifPresent(c -> c.forEach(e -> subsequentStates.add(e.getNext())));
                break;

            case CHOICE:
                // Choice state
                Optional.ofNullable(((ChoiceState) state).getChoices())
                        .ifPresent(c -> c.forEach(e -> subsequentStates.add(e.getNext())));
                // Default choice
                subsequentStates.add(((ChoiceState) state).getDefault());
                break;
            default:
                // Otherwise do nothing
        }
        return subsequentStates.stream().filter(StringUtils::isNotBlank).collect(Collectors.toSet());
    }
}
