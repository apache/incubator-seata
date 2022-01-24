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
package io.seata.saga.engine.pcext.utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.saga.engine.pcext.StateInstruction;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.proctrl.impl.ProcessContextImpl;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.ParallelState;
import io.seata.saga.statelang.domain.State;
import io.seata.saga.statelang.domain.StateInstance;
import io.seata.saga.statelang.domain.StateMachine;
import io.seata.saga.statelang.domain.StateMachineInstance;

/**
 * @author anselleeyy
 */
public class ParallelTaskUtils {

    public static final String PARALLEL_STATE_NAME_PATTERN = "-parallel-";

    public static String generateParallelSubStateName(ProcessContext context, String stateName) {
        if (StringUtils.isNotBlank(stateName)) {
            String parentStateName = (String) context.getVariable(DomainConstants.PARALLEL_PARENT_STATE_NAME);
            int branchIndex = (int) context.getVariable(DomainConstants.PARALLEL_BRANCH_INDEX);
            return parentStateName + PARALLEL_STATE_NAME_PATTERN + branchIndex + "-" + stateName;
        }
        return stateName;
    }
    
    public static void initParallelContext(ProcessContext context, ParallelState currentState) {
        ParallelContextHolder contextHolder = ParallelContextHolder.getCurrent(context, true);
        // init parallel context
        context.setVariable(DomainConstants.VAR_NAME_IS_PARALLEL_STATE, true);
        context.setVariable(DomainConstants.PARALLEL_PARENT_STATE_NAME, currentState.getName());
        contextHolder.setInitBranches(new ArrayList<>(currentState.getBranches()));
    }
    
    public static void clearParallelContext(ProcessContext context) {
        context.removeVariable(DomainConstants.PARALLEL_SEMAPHORE);
        context.removeVariable(DomainConstants.VAR_NAME_IS_PARALLEL_STATE);
        context.removeVariable(DomainConstants.PARALLEL_PARENT_STATE_NAME);
        ParallelContextHolder.clearCurrent(context);
    }

    /**
     * reload parallel context while forward
     *
     * @param context
     * @param forwardStateName
     */
    public static void reloadParallelContext(ProcessContext context, String forwardStateName) {

        StateMachineInstance stateMachineInstance =
            (StateMachineInstance) context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_INST);
        StateInstance lastForwardState = (StateInstance) context.getVariable(DomainConstants.VAR_NAME_STATE_INST);

        List<StateInstance> actList = stateMachineInstance.getStateList();
        List<StateInstance> forwardStateList =
            actList.stream().filter(e -> forwardStateName.equals(EngineUtils.getOriginStateName(e))).collect(Collectors.toList());

        ParallelContextHolder contextHolder = ParallelContextHolder.getCurrent(context, true);

        List<String> executedBranchList = new ArrayList<>();
        LinkedList<String> failEndBranches = new LinkedList<>();

        // reload failed branches
        failEndBranches.addFirst(reloadParallelIndex(lastForwardState.getName()));
        executedBranchList.add(reloadParallelIndex(lastForwardState.getName()));
        for (StateInstance stateInstance : forwardStateList) {
            if (!stateInstance.isIgnoreStatus()) {
                String index = reloadParallelIndex(stateInstance.getName());
                if (!ExecutionStatus.SU.equals(stateInstance.getStatus())) {
                    stateInstance.setIgnoreStatus(true);
                    failEndBranches.addFirst(index);
                }
                executedBranchList.add(index);
            }
        }

        contextHolder.setForwardBranches(failEndBranches);
        contextHolder.setExecutedBranches(executedBranchList);

    }

    public static List<String> acquireUnExecutedBranches(ProcessContext context) {
        ParallelContextHolder contextHolder = ParallelContextHolder.getCurrent(context, true);
        List<String> initBranchList = contextHolder.getInitBranches();
        List<String> executedBranchList = contextHolder.getExecutedBranches();
        List<String> unExecutedBranches = new ArrayList<>();
        for (int i = 0; i < initBranchList.size(); i++) {
            if (!executedBranchList.contains(String.valueOf(i))) {
                unExecutedBranches.add(String.valueOf(i));
            }
        }
        return unExecutedBranches;
    }

    /**
     * decide current exception route for parallel publish over
     *
     * @param parallelContextList
     * @param stateMachine
     * @return route if current exception route not null
     */
    public static String decideCurrentExceptionRoute(List<ProcessContext> parallelContextList, StateMachine stateMachine) {

        String route = null;
        if (CollectionUtils.isNotEmpty(parallelContextList)) {

            for (ProcessContext processContext : parallelContextList) {
                String next = (String)processContext.getVariable(DomainConstants.VAR_NAME_CURRENT_EXCEPTION_ROUTE);
                if (StringUtils.isNotBlank(next)) {

                    // compensate must be execute
                    State state = stateMachine.getState(next);
                    if (DomainConstants.STATE_TYPE_COMPENSATION_TRIGGER.equals(state.getType())) {
                        route = next;
                        break;
                    } else if (null == route) {
                        route = next;
                    }
                }
            }
        }
        return route;
    }

    public static String reloadParallelIndex(String stateInstanceName) {
        return stateInstanceName.split("-")[2];
    }

    public static ProcessContext createTempContext(ProcessContext context, String currentBranchStateName, int branchIndex) {

        ProcessContextImpl tempContext = new ProcessContextImpl();
        tempContext.setParent(context);
        tempContext.setInstruction(copyInstruction(context.getInstruction(StateInstruction.class), currentBranchStateName));
        tempContext.setVariableLocally(DomainConstants.PARALLEL_BRANCH_INDEX, branchIndex);

        return tempContext;
    }

    public static StateInstruction copyInstruction(StateInstruction instruction, String stateName) {
        StateInstruction targetInstruction = new StateInstruction();
        targetInstruction.setStateMachineName(instruction.getStateMachineName());
        targetInstruction.setTenantId(instruction.getTenantId());
        targetInstruction.setStateName(stateName);
        targetInstruction.setTemporaryState(instruction.getTemporaryState());
        return targetInstruction;
    }

}
