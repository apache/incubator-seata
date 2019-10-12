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
package io.seata.saga.engine.impl;

import io.seata.saga.engine.StatusDecisionStrategy;
import io.seata.saga.engine.pcext.utils.CompensationHolder;
import io.seata.saga.engine.utils.ExceptionUtils;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.StateInstance;
import io.seata.saga.statelang.domain.StateMachineInstance;
import io.seata.saga.engine.utils.ExceptionUtils.NetExceptionType;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default state machine execution status decision strategy
 *
 * @see StatusDecisionStrategy
 * @author lorne.cl
 */
public class DefaultStatusDecisionStrategy implements StatusDecisionStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultStatusDecisionStrategy.class);

    @Override
    public void decideOnEndState(ProcessContext context, StateMachineInstance stateMachineInstance, Exception exp) {

        if (ExecutionStatus.RU.equals(stateMachineInstance.getCompensationStatus())) {

            CompensationHolder compensationHolder = CompensationHolder.getCurrent(context, true);
            decideMachineCompensateStatus(stateMachineInstance, compensationHolder);
        } else {
            Object failEndStateFlag = context.getVariable(DomainConstants.VAR_NAME_FAIL_END_STATE_FLAG);
            boolean isComeFromFailEndState = (failEndStateFlag!= null && (Boolean)failEndStateFlag);
            decideMachineForwardExecutionStatus(stateMachineInstance, exp, isComeFromFailEndState);
        }

        if(stateMachineInstance.getCompensationStatus() != null
                && DomainConstants.OPERATION_NAME_FORWARD.equals(context.getVariable(DomainConstants.VAR_NAME_OPERATION_NAME))
                && ExecutionStatus.SU.equals(stateMachineInstance.getStatus())){

            stateMachineInstance.setCompensationStatus(ExecutionStatus.FA);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("StateMachine Instance[id:" + stateMachineInstance.getId() + ",name:" + stateMachineInstance.getStateMachine().getName()
                    + "] execute finish with status[" + stateMachineInstance.getStatus() + "], compensation status ["
                    + stateMachineInstance.getCompensationStatus() + "].");
        }
    }

    @Override
    public void decideOnTaskStateFail(ProcessContext context, StateMachineInstance stateMachineInstance, Exception exp) {
        if (!decideMachineForwardExecutionStatus(stateMachineInstance, exp, true)) {

            stateMachineInstance.setCompensationStatus(ExecutionStatus.UN);
        }
        stateMachineInstance.setRunning(false);
        stateMachineInstance.setGmtEnd(new Date());
        stateMachineInstance.setException(exp);
    }

    /**
     * decide machine compensate status
     *
     * @param stateMachineInstance
     * @param compensationHolder
     */
    public static void decideMachineCompensateStatus(StateMachineInstance stateMachineInstance, CompensationHolder compensationHolder) {
        if (stateMachineInstance.getStatus() == null || ExecutionStatus.RU.equals(stateMachineInstance.getStatus())) {

            stateMachineInstance.setStatus(ExecutionStatus.UN);
        }
        if (!compensationHolder.getStateStackNeedCompensation().isEmpty()) {

            boolean hasCompensateSUorUN = false;
            for (StateInstance forCompensateState : compensationHolder.getStatesForCompensation().values()) {
                if (ExecutionStatus.UN.equals(forCompensateState.getStatus()) || ExecutionStatus.SU.equals(forCompensateState.getStatus())) {
                    hasCompensateSUorUN = true;
                    break;
                }
            }
            if(hasCompensateSUorUN){
                stateMachineInstance.setCompensationStatus(ExecutionStatus.UN);
            }
            else{
                stateMachineInstance.setCompensationStatus(ExecutionStatus.FA);
            }
        } else {

            boolean hasCompensateError = false;
            for (StateInstance forCompensateState : compensationHolder.getStatesForCompensation().values()) {
                if (!ExecutionStatus.SU.equals(forCompensateState.getStatus())) {
                    hasCompensateError = true;
                    break;
                }

            }
            if (hasCompensateError) {
                stateMachineInstance.setCompensationStatus(ExecutionStatus.UN);
            } else {
                stateMachineInstance.setCompensationStatus(ExecutionStatus.SU);
            }
        }
    }

    /**
     * Determine the forward execution state of the state machine
     * @param stateMachineInstance
     * @param exp
     * @param specialPolicy
     * @return
     */
    @Override
    public boolean decideMachineForwardExecutionStatus(StateMachineInstance stateMachineInstance, Exception exp, boolean specialPolicy) {
        boolean result = false;

        if (stateMachineInstance.getStatus() == null || ExecutionStatus.RU.equals(stateMachineInstance.getStatus())) {
            result = true;

            List<StateInstance> stateList = stateMachineInstance.getStateList();

            boolean hasSetStatus = setMachineStatusBasedOnStateList(stateMachineInstance, stateList);

            if (!hasSetStatus) {
                setMachineStatusBasedOnException(stateMachineInstance, exp);
            }

            if (specialPolicy && ExecutionStatus.SU.equals(stateMachineInstance.getStatus())) {
                for (StateInstance stateInstance : stateMachineInstance.getStateList()) {
                    if (!stateInstance.isIgnoreStatus() && (stateInstance.isForUpdate() || stateInstance.isForCompensation())) {
                        stateMachineInstance.setStatus(ExecutionStatus.UN);
                        break;
                    }
                }
                if (ExecutionStatus.SU.equals(stateMachineInstance.getStatus())) {
                    stateMachineInstance.setStatus(ExecutionStatus.FA);
                }
            }
        }
        return result;

    }

    /**
     * set machine status based on state list
     * @param stateMachineInstance
     * @param stateList
     * @return
     */
    public static boolean setMachineStatusBasedOnStateList(StateMachineInstance stateMachineInstance, List<StateInstance> stateList) {
        boolean hasSetStatus = false;
        if (stateList != null && stateList.size() > 0) {

            boolean hasSuccessedUpdateService = false;

            boolean hasUnsuccessedAct = false;

            for (int i = stateList.size() - 1; i >= 0; i--) {
                StateInstance stateInstance = stateList.get(i);

                if (stateInstance.isIgnoreStatus() || stateInstance.isForCompensation()) {
                    continue;
                }
                if (ExecutionStatus.UN.equals(stateInstance.getStatus())) {
                    stateMachineInstance.setStatus(ExecutionStatus.UN);
                    hasSetStatus = true;
                } else if (ExecutionStatus.SU.equals(stateInstance.getStatus())) {
                    if (DomainConstants.STATE_TYPE_SERVICE_TASK.equals(stateInstance.getType())) {
                        if (stateInstance.isForUpdate() && !stateInstance.isForCompensation()) {
                            hasSuccessedUpdateService = true;
                        }
                    }
                } else if (ExecutionStatus.SK.equals(stateInstance.getStatus())) {
                    // ignore
                } else {
                    hasUnsuccessedAct = true;
                }
            }

            if (!hasSetStatus && hasUnsuccessedAct) {
                if (hasSuccessedUpdateService) {
                    stateMachineInstance.setStatus(ExecutionStatus.UN);
                } else {
                    stateMachineInstance.setStatus(ExecutionStatus.FA);
                }
                hasSetStatus = true;
            }
        }
        return hasSetStatus;
    }

    /**
     * set machine status based on net exception
     * @param stateMachineInstance
     * @param exp
     */
    public static void setMachineStatusBasedOnException(StateMachineInstance stateMachineInstance, Exception exp) {
        if (exp == null) {
            stateMachineInstance.setStatus(ExecutionStatus.SU);
        } else {
            NetExceptionType t = ExceptionUtils.getNetExceptionType(exp);
            if (t != null) {
                if (t.equals(NetExceptionType.CONNECT_EXCEPTION)
                        || t.equals(NetExceptionType.CONNECT_TIMEOUT_EXCEPTION)
                        || t.equals(NetExceptionType.NOT_NET_EXCEPTION)) {
                    stateMachineInstance.setStatus(ExecutionStatus.FA);
                } else if (t.equals(NetExceptionType.READ_TIMEOUT_EXCEPTION)) {
                    stateMachineInstance.setStatus(ExecutionStatus.UN);
                }
            } else {
                stateMachineInstance.setStatus(ExecutionStatus.UN);
            }
        }
    }
}