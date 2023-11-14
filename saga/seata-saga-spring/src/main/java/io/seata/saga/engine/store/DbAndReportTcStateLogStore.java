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
package io.seata.saga.engine.store;

import io.seata.common.Constants;
import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.util.StringUtils;
import io.seata.core.context.RootContext;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.GlobalStatus;
import io.seata.saga.engine.StateMachineConfig;
import io.seata.saga.engine.config.AbstractStateMachineConfig;
import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.engine.store.db.DbStateLogStore;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.StateInstance;
import io.seata.saga.statelang.domain.StateMachineInstance;
import io.seata.saga.tm.SagaTransactionalTemplate;
import io.seata.tm.api.GlobalTransaction;
import io.seata.tm.api.TransactionalExecutor.ExecutionException;
import io.seata.tm.api.transaction.TransactionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * State machine logs and definitions persist to database and report status to TC (Transaction Coordinator)
 * TODO：move to engine as a base StateLogStore impl
 *
 * @author lorne.cl, wt-better
 */
public class DbAndReportTcStateLogStore extends DbStateLogStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbAndReportTcStateLogStore.class);

    private SagaTransactionalTemplate sagaTransactionalTemplate;

    @Override
    public void beginTransaction(StateMachineInstance machineInstance, ProcessContext context) {
        if (sagaTransactionalTemplate != null) {
            StateMachineConfig stateMachineConfig = (StateMachineConfig) context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);
            TransactionInfo transactionInfo = new TransactionInfo();
            transactionInfo.setTimeOut(stateMachineConfig.getTransOperationTimeout());
            transactionInfo.setName(Constants.SAGA_TRANS_NAME_PREFIX + machineInstance.getStateMachine().getName());
            try {
                GlobalTransaction globalTransaction = sagaTransactionalTemplate.beginTransaction(transactionInfo);
                machineInstance.setId(globalTransaction.getXid());

                context.setVariable(DomainConstants.VAR_NAME_GLOBAL_TX, globalTransaction);
                Map<String, Object> machineContext = machineInstance.getContext();
                if (machineContext != null) {
                    machineContext.put(DomainConstants.VAR_NAME_GLOBAL_TX, globalTransaction);
                }
            } catch (ExecutionException e) {
                String xid = null;
                if (e.getTransaction() != null) {
                    xid = e.getTransaction().getXid();
                }
                throw new EngineExecutionException(e, e.getCode() + ", TransName:" + transactionInfo.getName() + ", XID: " + xid + ", Reason: " + e.getMessage(), FrameworkErrorCode.TransactionManagerError);
            } finally {
                if (Boolean.TRUE.equals(context.getVariable(DomainConstants.VAR_NAME_IS_ASYNC_EXECUTION))) {
                    RootContext.unbind();
                    RootContext.unbindBranchType();
                }
            }
        }
    }

    @Override
    public void reportTransactionFinished(StateMachineInstance machineInstance, ProcessContext context) {
        if (sagaTransactionalTemplate != null) {
            GlobalTransaction globalTransaction = null;
            try {
                globalTransaction = getGlobalTransaction(machineInstance, context);
                if (globalTransaction == null) {

                    throw new EngineExecutionException("Global transaction is not exists", FrameworkErrorCode.ObjectNotExists);
                }

                GlobalStatus globalStatus = getGlobalStatus(machineInstance);
                sagaTransactionalTemplate.reportTransaction(globalTransaction, globalStatus);
            } catch (ExecutionException e) {
                LOGGER.error("Report transaction finish to server error: {}, StateMachine: {}, XID: {}, Reason: {}", e.getCode(), machineInstance.getStateMachine().getName(), machineInstance.getId(), e.getMessage(), e);
            } catch (TransactionException e) {
                LOGGER.error("Report transaction finish to server error: {}, StateMachine: {}, XID: {}, Reason: {}", e.getCode(), machineInstance.getStateMachine().getName(), machineInstance.getId(), e.getMessage(), e);
            } finally {
                // clear
                RootContext.unbind();
                RootContext.unbindBranchType();
                sagaTransactionalTemplate.triggerAfterCompletion(globalTransaction);
                sagaTransactionalTemplate.cleanUp();
            }
        }
    }

    private static GlobalStatus getGlobalStatus(StateMachineInstance machineInstance) {
        GlobalStatus globalStatus;
        if (ExecutionStatus.SU.equals(machineInstance.getStatus()) && machineInstance.getCompensationStatus() == null) {
            globalStatus = GlobalStatus.Committed;
        } else if (ExecutionStatus.SU.equals(machineInstance.getCompensationStatus())) {
            globalStatus = GlobalStatus.Rollbacked;
        } else if (ExecutionStatus.FA.equals(machineInstance.getCompensationStatus()) || ExecutionStatus.UN.equals(machineInstance.getCompensationStatus())) {
            globalStatus = GlobalStatus.RollbackRetrying;
        } else if (ExecutionStatus.FA.equals(machineInstance.getStatus()) && machineInstance.getCompensationStatus() == null) {
            globalStatus = GlobalStatus.Finished;
        } else if (ExecutionStatus.UN.equals(machineInstance.getStatus()) && machineInstance.getCompensationStatus() == null) {
            globalStatus = GlobalStatus.CommitRetrying;
        } else {
            globalStatus = GlobalStatus.UnKnown;
        }
        return globalStatus;
    }

    @Override
    public void branchRegister(StateInstance stateInstance, ProcessContext context) {
        if (sagaTransactionalTemplate != null) {
            StateMachineConfig stateMachineConfig = (StateMachineConfig) context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);

            if (stateMachineConfig instanceof AbstractStateMachineConfig && !((AbstractStateMachineConfig) stateMachineConfig).isSagaBranchRegisterEnable()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("sagaBranchRegisterEnable = false, skip register branch. state[" + stateInstance.getName() + "]");
                }
                return;
            }

            //Register branch
            try {
                StateMachineInstance machineInstance = stateInstance.getStateMachineInstance();
                GlobalTransaction globalTransaction = getGlobalTransaction(machineInstance, context);
                if (globalTransaction == null) {
                    throw new EngineExecutionException("Global transaction is not exists", FrameworkErrorCode.ObjectNotExists);
                }

                String resourceId = stateInstance.getStateMachineInstance().getStateMachine().getName() + "#" + stateInstance.getName();
                long branchId = sagaTransactionalTemplate.branchRegister(resourceId, null, globalTransaction.getXid(), null, null);
                stateInstance.setId(String.valueOf(branchId));
            } catch (TransactionException e) {
                throw new EngineExecutionException(e, "Branch transaction error: " + e.getCode() + ", StateMachine:" + stateInstance.getStateMachineInstance().getStateMachine().getName() + ", XID: " + stateInstance.getStateMachineInstance().getId() + ", State:" + stateInstance.getName() + ", stateId: " + stateInstance.getId() + ", Reason: " + e.getMessage(), FrameworkErrorCode.TransactionManagerError);
            } catch (ExecutionException e) {
                throw new EngineExecutionException(e, "Branch transaction error: " + e.getCode() + ", StateMachine:" + stateInstance.getStateMachineInstance().getStateMachine().getName() + ", XID: " + stateInstance.getStateMachineInstance().getId() + ", State:" + stateInstance.getName() + ", stateId: " + stateInstance.getId() + ", Reason: " + e.getMessage(), FrameworkErrorCode.TransactionManagerError);
            }
        }
    }

    protected GlobalTransaction getGlobalTransaction(StateMachineInstance machineInstance, ProcessContext context) throws ExecutionException, TransactionException {
        GlobalTransaction globalTransaction = (GlobalTransaction) context.getVariable(DomainConstants.VAR_NAME_GLOBAL_TX);
        if (globalTransaction == null) {
            String xid;
            String parentId = machineInstance.getParentId();
            if (StringUtils.isEmpty(parentId)) {
                xid = machineInstance.getId();
            } else {
                xid = parentId.substring(0, parentId.lastIndexOf(DomainConstants.SEPERATOR_PARENT_ID));
            }
            globalTransaction = sagaTransactionalTemplate.reloadTransaction(xid);
            if (globalTransaction != null) {
                context.setVariable(DomainConstants.VAR_NAME_GLOBAL_TX, globalTransaction);
            }
        }
        return globalTransaction;
    }

    @Override
    public void branchReport(StateInstance stateInstance, ProcessContext context) {
        if (sagaTransactionalTemplate != null) {
            StateMachineConfig stateMachineConfig = (StateMachineConfig) context.getVariable(DomainConstants.VAR_NAME_STATEMACHINE_CONFIG);

            if (stateMachineConfig instanceof AbstractStateMachineConfig && !((AbstractStateMachineConfig) stateMachineConfig).isSagaBranchRegisterEnable()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("sagaBranchRegisterEnable = false, skip branch report. state[" + stateInstance.getName() + "]");
                }
                return;
            }

            BranchStatus branchStatus = null;
            //find out the original state instance, only the original state instance is registered on the server, and its status should
            // be reported.
            StateInstance originalStateInst = null;
            if (StringUtils.hasLength(stateInstance.getStateIdRetriedFor())) {

                if (isUpdateMode(stateInstance, context)) {
                    originalStateInst = stateInstance;
                } else {
                    originalStateInst = findOutOriginalStateInstanceOfRetryState(stateInstance);
                }

                if (ExecutionStatus.SU.equals(stateInstance.getStatus())) {
                    branchStatus = BranchStatus.PhaseTwo_Committed;
                } else if (ExecutionStatus.FA.equals(stateInstance.getStatus()) || ExecutionStatus.UN.equals(stateInstance.getStatus())) {
                    branchStatus = BranchStatus.PhaseOne_Failed;
                } else {
                    branchStatus = BranchStatus.Unknown;
                }

            } else if (StringUtils.hasLength(stateInstance.getStateIdCompensatedFor())) {

                if (isUpdateMode(stateInstance, context)) {
                    originalStateInst = stateInstance.getStateMachineInstance().getStateMap().get(stateInstance.getStateIdCompensatedFor());
                } else {
                    originalStateInst = findOutOriginalStateInstanceOfCompensateState(stateInstance);
                }
            }

            if (originalStateInst == null) {
                originalStateInst = stateInstance;
            }

            if (branchStatus == null) {
                if (ExecutionStatus.SU.equals(originalStateInst.getStatus()) && originalStateInst.getCompensationStatus() == null) {
                    branchStatus = BranchStatus.PhaseTwo_Committed;
                } else if (ExecutionStatus.SU.equals(originalStateInst.getCompensationStatus())) {
                    branchStatus = BranchStatus.PhaseTwo_Rollbacked;
                } else if (ExecutionStatus.FA.equals(originalStateInst.getCompensationStatus()) || ExecutionStatus.UN.equals(originalStateInst.getCompensationStatus())) {
                    branchStatus = BranchStatus.PhaseTwo_RollbackFailed_Retryable;
                } else if ((ExecutionStatus.FA.equals(originalStateInst.getStatus()) || ExecutionStatus.UN.equals(originalStateInst.getStatus())) && originalStateInst.getCompensationStatus() == null) {
                    branchStatus = BranchStatus.PhaseOne_Failed;
                } else {
                    branchStatus = BranchStatus.Unknown;
                }
            }

            try {
                StateMachineInstance machineInstance = stateInstance.getStateMachineInstance();
                GlobalTransaction globalTransaction = getGlobalTransaction(machineInstance, context);

                if (globalTransaction == null) {
                    throw new EngineExecutionException("Global transaction is not exists", FrameworkErrorCode.ObjectNotExists);
                }

                sagaTransactionalTemplate.branchReport(globalTransaction.getXid(), Long.parseLong(originalStateInst.getId()), branchStatus, null);
            } catch (TransactionException e) {
                LOGGER.error("Report branch status to server error: {}, StateMachine:{}, StateName:{}, XID: {}, branchId: {}, branchStatus:{}," + " Reason:{} ", e.getCode(), originalStateInst.getStateMachineInstance().getStateMachine().getName(), originalStateInst.getName(), originalStateInst.getStateMachineInstance().getId(), originalStateInst.getId(), branchStatus, e.getMessage(), e);
            } catch (ExecutionException e) {
                LOGGER.error("Report branch status to server error: {}, StateMachine:{}, StateName:{}, XID: {}, branchId: {}, branchStatus:{}," + " Reason:{} ", e.getCode(), originalStateInst.getStateMachineInstance().getStateMachine().getName(), originalStateInst.getName(), originalStateInst.getStateMachineInstance().getId(), originalStateInst.getId(), branchStatus, e.getMessage(), e);
            }
        }
    }

    private StateInstance findOutOriginalStateInstanceOfRetryState(StateInstance stateInstance) {
        StateInstance originalStateInst;
        Map<String, StateInstance> stateInstanceMap = stateInstance.getStateMachineInstance().getStateMap();
        originalStateInst = stateInstanceMap.get(stateInstance.getStateIdRetriedFor());
        while (StringUtils.hasLength(originalStateInst.getStateIdRetriedFor())) {
            originalStateInst = stateInstanceMap.get(originalStateInst.getStateIdRetriedFor());
        }
        return originalStateInst;
    }

    private StateInstance findOutOriginalStateInstanceOfCompensateState(StateInstance stateInstance) {
        StateInstance originalStateInst;
        Map<String, StateInstance> stateInstanceMap = stateInstance.getStateMachineInstance().getStateMap();
        originalStateInst = stateInstance.getStateMachineInstance().getStateMap().get(stateInstance.getStateIdCompensatedFor());
        while (StringUtils.hasLength(originalStateInst.getStateIdRetriedFor())) {
            originalStateInst = stateInstanceMap.get(originalStateInst.getStateIdRetriedFor());
        }
        return originalStateInst;
    }

    @Override
    public void clearUp() {
        super.clearUp();
        if (sagaTransactionalTemplate != null) {
            sagaTransactionalTemplate.cleanUp();
        }
    }

    public SagaTransactionalTemplate getSagaTransactionalTemplate() {
        return sagaTransactionalTemplate;
    }

    public void setSagaTransactionalTemplate(SagaTransactionalTemplate sagaTransactionalTemplate) {
        this.sagaTransactionalTemplate = sagaTransactionalTemplate;
    }

}
