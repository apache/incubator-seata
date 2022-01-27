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
import java.util.Map;
import java.util.stream.Collectors;

import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.saga.engine.StateMachineConfig;
import io.seata.saga.engine.pcext.StateInstruction;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.proctrl.impl.ProcessContextImpl;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.ParallelState;
import io.seata.saga.statelang.domain.State;
import io.seata.saga.statelang.domain.StateInstance;
import io.seata.saga.statelang.domain.StateMachineInstance;
import io.seata.saga.statelang.domain.impl.AbstractTaskState;

/**
 * @author anselleeyy
 */
public class ParallelTaskUtils {

    public static final String PARALLEL_STATE_NAME_PATTERN = "-parallel-";

    public static String generateParallelSubStateName(ProcessContext context, String stateName) {
        if (StringUtils.isNotBlank(stateName)) {
            String parentStateName = (String) context.getVariable(DomainConstants.PARALLEL_START_STATE_NAME);
            int branchIndex = (int) context.getVariable(DomainConstants.PARALLEL_BRANCH_INDEX);
            return parentStateName + PARALLEL_STATE_NAME_PATTERN + branchIndex + "-" + stateName;
        }
        return stateName;
    }

    public static void initParallelContext(ProcessContext context, ParallelState currentState) {
        ParallelContextHolder contextHolder = ParallelContextHolder.getCurrent(context, true);
        // init parallel context
        context.setVariable(DomainConstants.VAR_NAME_IS_PARALLEL_STATE, true);
        context.setVariable(DomainConstants.PARALLEL_START_STATE_NAME, currentState.getName());
        contextHolder.setInitBranches(new ArrayList<>(currentState.getBranches()));
    }

    public static void clearParallelContext(ProcessContext context) {
        context.removeVariable(DomainConstants.PARALLEL_SEMAPHORE);
        context.removeVariable(DomainConstants.VAR_NAME_IS_PARALLEL_STATE);
        context.removeVariable(DomainConstants.PARALLEL_START_STATE_NAME);
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
        LinkedList<String> branchesNeedToBeForwarded = new LinkedList<>();

        // reload failed branches
        branchesNeedToBeForwarded.addFirst(reloadParallelIndex(lastForwardState.getName()));
        executedBranchList.add(reloadParallelIndex(lastForwardState.getName()));
        for (StateInstance stateInstance : forwardStateList) {
            if (!stateInstance.isIgnoreStatus()) {
                String index = reloadParallelIndex(stateInstance.getName());
                if (!ExecutionStatus.SU.equals(stateInstance.getStatus())) {
                    stateInstance.setIgnoreStatus(true);
                    branchesNeedToBeForwarded.addFirst(index);
                }
                executedBranchList.add(index);
            }
        }

        contextHolder.setForwardBranches(branchesNeedToBeForwarded);
        contextHolder.setExecutedBranches(executedBranchList);

    }

    public static String reloadParallelIndex(String stateInstanceName) {
        return stateInstanceName.split("-")[2];
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

    public static void putContextToParent(ProcessContext context, List<ProcessContext> asyncExecutionInstances,
                                          State state) {
        Map<String, Object> contextVariables =
            (Map<String, Object>) context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONTEXT);
        if (CollectionUtils.isNotEmpty(asyncExecutionInstances)) {

            StateMachineConfig stateMachineConfig =
                (StateMachineConfig) context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);

            List<Map<String, Object>> asyncContextVariables = new ArrayList<>();
            for (ProcessContext processContext : asyncExecutionInstances) {
                StateInstance stateInstance =
                    (StateInstance) processContext.getVariable(DomainConstants.VAR_NAME_STATE_INST);
                asyncContextVariables.add((Map<String, Object>) stateInstance.getOutputParams());
            }
            Map<String, Object> outputVariablesToContext =
                ParameterUtils.createOutputParams(stateMachineConfig.getExpressionFactoryManager(),
                    (AbstractTaskState) state, asyncContextVariables);
            contextVariables.putAll(outputVariablesToContext);
        }
    }

    public static ProcessContext createTempContext(ProcessContext context, String currentBranchStateName,
                                                   int branchIndex) {

        ProcessContextImpl tempContext = new ProcessContextImpl();
        tempContext.setParent(context);
        tempContext.setInstruction(copyInstruction(context.getInstruction(StateInstruction.class),
            currentBranchStateName));
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
