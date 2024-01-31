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
package org.apache.seata.saga.statelang.validator.impl;

import org.apache.seata.saga.statelang.domain.State;
import org.apache.seata.saga.statelang.domain.StateMachine;
import org.apache.seata.saga.statelang.parser.utils.StateMachineUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Rule to check if the state machine can terminate in finite time, i.e. if there is an infinite loop
 *
 */
public class FiniteTerminationRule extends AbstractRule {

    @Override
    public boolean validate(StateMachine stateMachine) {
        String stateName = stateMachine.getStartState();
        State startState = stateMachine.getState(stateMachine.getStartState());
        String notFoundHintTemplate = "State [%s] is not defined in state machine";
        if (startState == null) {
            hint = String.format(notFoundHintTemplate, stateMachine.getStartState());
            return false;
        }

        DisjointSet disjointSet = new DisjointSet(stateMachine.getStates().keySet());
        Set<String> visited = new HashSet<>();
        Map<String, Set<String>> nextStateNameMap = new HashMap<>();

        iterate(stateMachine, stateName, disjointSet, visited, nextStateNameMap, new Stack<>());

        Map<String, Set<String>> rootToStateNames = new HashMap<>();
        for (String disjointStateName : stateMachine.getStates().keySet()) {
            String root = disjointSet.find(disjointStateName);
            if (!rootToStateNames.containsKey(root)) {
                rootToStateNames.put(root, new HashSet<>());
            }
            rootToStateNames.get(root).add(disjointStateName);
        }

        for (Set<String> cycleStateNames : rootToStateNames.values()) {
            if (cycleStateNames.size() <= 1) {
                // Not in a cycle
                continue;
            }
            boolean noOutgoingFlow = true;
            for (String cycleStateName : cycleStateNames) {
                if (!cycleStateNames.containsAll(nextStateNameMap.get(cycleStateName))) {
                    // There is at least an outgoing flow not in this cycle
                    noOutgoingFlow = false;
                    break;
                }
            }
            if (noOutgoingFlow) {
                hint = String.format("There is a infinite loop [%s] without outgoing flow to end",
                        String.join(", ", cycleStateNames));
                return false;
            }
        }
        return true;
    }

    private static void iterate(
            StateMachine stateMachine,
            String stateName,
            DisjointSet disjointSet,
            Set<String> visited,
            Map<String, Set<String>> nextStateNameMap,
            Stack<String> currentPathWithoutCycles
    ) {
        State state = stateMachine.getState(stateName);

        if (visited.contains(stateName)) {
            // If it has ever been visited before, means it is in a cycle
            if (currentPathWithoutCycles.size() > 1) {
                // Union all states in a cycle
                int curr = currentPathWithoutCycles.size() - 1;
                do {
                    disjointSet.union(currentPathWithoutCycles.get(curr), currentPathWithoutCycles.get(--curr));
                } while (!currentPathWithoutCycles.get(curr).equals(stateName));
            }
        } else {
            Set<String> nextStateNames = StateMachineUtils.getAllPossibleSubsequentStates(state);
            nextStateNameMap.put(stateName, nextStateNames);

            visited.add(stateName);
            currentPathWithoutCycles.push(stateName);
            for (String nextStateName: nextStateNames) {
                iterate(stateMachine, nextStateName, disjointSet, visited, nextStateNameMap, currentPathWithoutCycles);
            }
            currentPathWithoutCycles.pop();
            visited.remove(stateName);
        }

    }

    private static class DisjointSet {
        Map<String, String> parent = new HashMap<>();
        Map<String, Integer> rank = new HashMap<>();

        DisjointSet(Collection<String> stateNames) {
            for (String stateName : stateNames) {
                parent.put(stateName, stateName);
                rank.put(stateName, 0);
            }
        }

        String find(String state) {
            if (parent.get(state).equals(state)) {
                return state;
            }

            String root = find(parent.get(state));
            parent.put(state, root);
            return root;
        }

        void union(String i, String j) {
            String parentI = find(i), parentJ = find(j);
            int rankI = rank.get(parentI), rankJ = rank.get(parentJ);

            if (!parentI.equals(parentJ)) {

                if (rankI > rankJ) {
                    parent.put(parentJ, parentI);
                } else {
                    parent.put(parentI, parentJ);
                    if (rankI == rankJ) {
                        rank.put(parentI, rankI + 1);
                    }
                }
            }
        }
    }
}
