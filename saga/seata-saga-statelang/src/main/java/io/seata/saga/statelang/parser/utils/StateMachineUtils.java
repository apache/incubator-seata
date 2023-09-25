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

package io.seata.saga.statelang.parser.utils;

import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.util.StringUtils;
import io.seata.saga.statelang.domain.ChoiceState;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.ForkState;
import io.seata.saga.statelang.domain.State;
import io.seata.saga.statelang.domain.StateMachine;
import io.seata.saga.statelang.domain.TaskState;
import io.seata.saga.statelang.domain.impl.ForkStateImpl;
import io.seata.saga.statelang.parser.ParserException;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * Util class for parsing StateMachine
 *
 * @author ptyin
 */
public class StateMachineUtils {

    /**
     * Generate {@link State} list for each parallel branch.
     *
     * @param forkState Fork state
     */
    public static void generateBranchStatesAndPairedJoin(ForkStateImpl forkState) {
        Map<String, Set<String>> allBranchStates = forkState.getAllBranchStates();
        StateMachine stateMachine = forkState.getStateMachine();

        if (allBranchStates != null) {
            if (forkState.getPairedJoinState() == null) {
                throw new ParserException(String.format("Fork state [%s] has no paired join state which is mandatory",
                        forkState.getName()));
            }
            return;
        }
        allBranchStates = new HashMap<>();
        for (String branch : forkState.getBranches()) {
            Set<String> branchStates = new HashSet<>();
            Stack<String> stateToVisit = new Stack<>();

            stateToVisit.add(branch);
            while (!stateToVisit.isEmpty()) {
                String stateName = stateToVisit.pop();
                // Pass if the state name is blank or visited
                if (StringUtils.isBlank(stateName) || branchStates.contains(stateName)) {
                    continue;
                }
                State branchState = stateMachine.getState(stateName);
                if (branchState == null) {
                    throw new ParserException(
                            String.format("State [%s] in branch [%s] of fork state [%s] cannot be found",
                                    stateName, branch, forkState.getName()), FrameworkErrorCode.ObjectNotExists
                    );
                }

                branchStates.add(stateName);
                if (DomainConstants.STATE_TYPE_FORK.equals(branchState.getType())) {
                    // Recursively generate for sub fork state
                    generateBranchStatesAndPairedJoin((ForkStateImpl) branchState);
                    State innerJoinState = stateMachine.getState(((ForkStateImpl) branchState).getPairedJoinState());
                    stateToVisit.addAll(getAllPossibleSubsequentStates(innerJoinState));
                } else if (DomainConstants.STATE_TYPE_JOIN.equals(branchState.getType())) {
                    if (StringUtils.isNotBlank(forkState.getPairedJoinState())) {
                        if (!forkState.getPairedJoinState().equals(branchState.getName())) {
                            throw new ParserException(String.format(
                                    "Fork state [%s] has two or more paired join state: [%s] and [%s]",
                                    forkState.getName(), forkState.getPairedJoinState(), branchState.getName()
                            ));
                        }
                    } else {
                        forkState.setPairedJoinState(branchState.getName());
                    }
                } else {
                    stateToVisit.addAll(getAllPossibleSubsequentStates(branchState));
                }
            }
            allBranchStates.put(branch, branchStates);
        }
        // Set all branch states of fork state
        forkState.setAllBranchStates(allBranchStates);
        if (forkState.getPairedJoinState() == null) {
            throw new ParserException(String.format("Fork state [%s] has no paired join state which is mandatory",
                    forkState.getName()));
        }
    }

    public static Set<String> getAllPossibleSubsequentStates(State state) {
        Set<String> subsequentStates = new HashSet<>();
        // Next state
        subsequentStates.add(state.getNext());
        switch (state.getType()) {
            case DomainConstants.STATE_TYPE_SCRIPT_TASK:
            case DomainConstants.STATE_TYPE_SERVICE_TASK:
            case DomainConstants.STATE_TYPE_SUB_STATE_MACHINE:
            case DomainConstants.STATE_TYPE_SUB_MACHINE_COMPENSATION:
                // Next state in catches
                Optional.ofNullable(((TaskState) state).getCatches())
                        .ifPresent(c -> c.forEach(e -> subsequentStates.add(e.getNext())));
                break;

            case DomainConstants.STATE_TYPE_CHOICE:
                // Choice state
                Optional.ofNullable(((ChoiceState) state).getChoices())
                        .ifPresent(c -> c.forEach(e -> subsequentStates.add(e.getNext())));
                // Default choice
                subsequentStates.add(((ChoiceState) state).getDefault());
                break;

            case DomainConstants.STATE_TYPE_FORK:
                // All branches in child parallel task
                subsequentStates.addAll(((ForkState) state).getBranches());
                break;
            default:
                // Otherwise do nothing
        }
        return subsequentStates;
    }

    public static Map<String, ForkStateImpl> getStateToParentForkMap(StateMachine stateMachine) {
        List<ForkStateImpl> forkStates = stateMachine.getStates().values().stream()
                .filter(state -> DomainConstants.STATE_TYPE_FORK.equals(state.getType()))
                .map(state -> (ForkStateImpl) state)
                .collect(Collectors.toList());

        Map<String, ForkStateImpl> stateNameToParentForkMap = new HashMap<>();
        List<Set<String>> forkStateChildren = forkStates.stream()
                .map(state -> state.getAllBranchStates().values().stream()
                        .flatMap(Collection::stream).collect(Collectors.toSet()))
                .collect(Collectors.toList());

        for (int i = 0; i < forkStates.size(); i++) {
            ForkStateImpl forkState = forkStates.get(i);
            Set<String> children = forkStateChildren.get(i);
            for (String stateName: children) {
                stateNameToParentForkMap.put(stateName, forkState);
            }
        }
        return stateNameToParentForkMap;
    }
}
