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
package io.seata.saga.engine.store.db;

import io.seata.common.exception.FrameworkErrorCode;
import io.seata.core.context.RootContext;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.GlobalStatus;
import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.engine.store.StateLogStore;
import io.seata.saga.engine.serializer.ObjectSerializer;
import io.seata.saga.engine.serializer.impl.ExceptionSerializer;
import io.seata.saga.engine.serializer.impl.ParamsFastjsonSerializer;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.StateInstance;
import io.seata.saga.statelang.domain.StateMachineInstance;
import io.seata.saga.tm.SagaTransactionalTemplate;
import io.seata.tm.api.GlobalTransaction;
import io.seata.tm.api.GlobalTransactionContext;
import io.seata.tm.api.TransactionalExecutor.ExecutionException;
import io.seata.tm.api.transaction.TransactionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.seata.saga.engine.store.db.MybatisConfig.MAPPER_PREFIX;

/**
 * State machine logs and definitions persist to database and report status to TC (Transaction Coordinator)
 *
 * @author lorne.cl
 */
public class DbAndReportTcStateLogStore implements StateLogStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbAndReportTcStateLogStore.class);

    private SagaTransactionalTemplate           sagaTransactionalTemplate;
    private SqlSessionExecutor                  sqlSessionExecutor;
    private ObjectSerializer<Object, String>    paramsSerializer = new ParamsFastjsonSerializer();
    private ObjectSerializer<Exception, byte[]> exceptionSerializer = new ExceptionSerializer();
    private String defaultTenantId;

    @Override
    public void recordStateMachineStarted(StateMachineInstance machineInstance, ProcessContext context) {

        if(machineInstance != null){
            TransactionInfo transactionInfo = new TransactionInfo();
            transactionInfo.setTimeOut(sagaTransactionalTemplate.getTimeout());
            transactionInfo.setName(machineInstance.getStateMachine().getName());
            try {
                GlobalTransaction globalTransaction = sagaTransactionalTemplate.beginTransaction(transactionInfo);
                machineInstance.setId(globalTransaction.getXid());

                context.setVariable(DomainConstants.VAR_NAME_GLOBAL_TX, globalTransaction);
                Map<String, Object> machineContext =  machineInstance.getContext();
                if(machineContext != null){
                    machineContext.put(DomainConstants.VAR_NAME_GLOBAL_TX, globalTransaction);
                }
                context.setVariable(DomainConstants.VAR_NAME_ROOT_CONTEXT_HOLDER, RootContext.entries());

                // save to db
                machineInstance.setSerializedStartParams(paramsSerializer.serialize(machineInstance.getStartParams()));
                sqlSessionExecutor.insert(MAPPER_PREFIX + "recordStateMachineStarted", machineInstance);
            } catch (ExecutionException e) {

                String xid = null;
                if(e.getTransaction() != null){
                    xid = e.getTransaction().getXid();
                }
                throw new EngineExecutionException(e,  e.getCode()+ ", TransName:" +transactionInfo.getName() +", XID: " + xid + ", Reason: " + e.getMessage(), FrameworkErrorCode.TransactionManagerError);
            }
        }
    }

    @Override
    public void recordStateMachineFinished(StateMachineInstance machineInstance, ProcessContext context) {

        if(machineInstance != null){
            // save to db
            Map<String, Object> endParams = machineInstance.getEndParams();
            if(endParams != null){
                endParams.remove(DomainConstants.VAR_NAME_GLOBAL_TX);
            }

            machineInstance.setSerializedEndParams(paramsSerializer.serialize(machineInstance.getEndParams()));
            machineInstance.setSerializedException(exceptionSerializer.serialize(machineInstance.getException()));
            sqlSessionExecutor.update(MAPPER_PREFIX + "recordStateMachineFinished", machineInstance);

            try {
                GlobalTransaction globalTransaction = (GlobalTransaction)context.getVariable(DomainConstants.VAR_NAME_GLOBAL_TX);
                if(globalTransaction == null){
                    globalTransaction = GlobalTransactionContext.reload(machineInstance.getId());
                }

                if(globalTransaction == null){
                    throw new EngineExecutionException("Global transaction is not exists", FrameworkErrorCode.ObjectNotExists);
                }

                GlobalStatus globalStatus;

                if(ExecutionStatus.SU.equals(machineInstance.getStatus()) && machineInstance.getCompensationStatus() == null){
                    globalStatus = GlobalStatus.Committed;
                }
                else if(ExecutionStatus.SU.equals(machineInstance.getCompensationStatus())){
                    globalStatus = GlobalStatus.Rollbacked;
                }
                else if(ExecutionStatus.FA.equals(machineInstance.getCompensationStatus())
                            || ExecutionStatus.UN.equals(machineInstance.getCompensationStatus())){
                    globalStatus = GlobalStatus.RollbackRetrying;
                }
                else if(ExecutionStatus.FA.equals(machineInstance.getStatus()) && machineInstance.getCompensationStatus() == null){
                    globalStatus = GlobalStatus.Finished;
                }
                else if(ExecutionStatus.UN.equals(machineInstance.getStatus()) && machineInstance.getCompensationStatus() == null){
                    globalStatus = GlobalStatus.CommitRetrying;
                }
                else{
                    globalStatus = GlobalStatus.UnKnown;
                }
                sagaTransactionalTemplate.reportTransaction(globalTransaction, globalStatus);
            } catch (ExecutionException e) {
                LOGGER.error("Report transaction finish to server error: ", e.getCode()+ ", StateMachine:" + machineInstance.getStateMachine().getName() +", XID: "+machineInstance.getId()+", Reason: " + e.getMessage(), e);
            } catch (TransactionException e) {
                LOGGER.error("Report transaction finish to server error: " + e.getCode()+ ", StateMachine:" + machineInstance.getStateMachine().getName() +", XID: "+machineInstance.getId()+", Reason: " + e.getMessage(), e);
            }
            finally {
                // clear
                Map<String, String> rootContextEntries = (Map<String, String>)context.getVariable(DomainConstants.VAR_NAME_ROOT_CONTEXT_HOLDER);
                if(rootContextEntries != null){
                    rootContextEntries.clear();
                }
                sagaTransactionalTemplate.triggerAfterCompletion();
                sagaTransactionalTemplate.cleanUp();
            }
        }
    }

    @Override
    public void updateStateMachineRunningStatus(StateMachineInstance machineInstance, ProcessContext context) {

        if (machineInstance != null) {

            sqlSessionExecutor.update(MAPPER_PREFIX + "updateStateMachineRunningStatus", machineInstance);

            GlobalStatus globalStatus;
            if(DomainConstants.OPERATION_NAME_COMPENSATE.equals(context.getVariable(DomainConstants.VAR_NAME_OPERATION_NAME))){
                globalStatus = GlobalStatus.Rollbacking;
            }
            else{
                globalStatus = GlobalStatus.Committing;
            }

            try {
                GlobalTransaction globalTransaction = (GlobalTransaction)context.getVariable(DomainConstants.VAR_NAME_GLOBAL_TX);
                if(globalTransaction == null){
                    globalTransaction = GlobalTransactionContext.reload(machineInstance.getId());
                }

                if(globalTransaction == null){
                    throw new EngineExecutionException("Global transaction is not exists", FrameworkErrorCode.ObjectNotExists);
                }

                sagaTransactionalTemplate.reportTransaction(globalTransaction, globalStatus);
            } catch (ExecutionException e) {
                LOGGER.error("Report transaction status to server error: " + e.getCode()+ ", StateMachine:" + machineInstance.getStateMachine().getName() +", XID: "+machineInstance.getId()+", globalStatus:"+ globalStatus +", Reason: " + e.getMessage(), e);
            } catch (TransactionException e) {
                LOGGER.error("Report transaction status to server error: " + e.getCode()+ ", StateMachine:" + machineInstance.getStateMachine().getName() +", XID: "+machineInstance.getId()+", globalStatus:"+ globalStatus +", Reason: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void recordStateStarted(StateInstance stateInstance, ProcessContext context) {

        if (stateInstance != null) {

            try {
                GlobalTransaction globalTransaction = (GlobalTransaction)context.getVariable(DomainConstants.VAR_NAME_GLOBAL_TX);
                if(globalTransaction == null){
                    globalTransaction = GlobalTransactionContext.reload(stateInstance.getStateMachineInstance().getId());
                }

                if(globalTransaction == null){
                    throw new EngineExecutionException("Global transaction is not exists", FrameworkErrorCode.ObjectNotExists);
                }

                String resourceId = stateInstance.getStateMachineInstance().getStateMachine().getName() + "#" + stateInstance.getName();
                long branchId = sagaTransactionalTemplate.branchRegister(resourceId, null, globalTransaction.getXid(), null, null);
                stateInstance.setId(String.valueOf(branchId));
            } catch (TransactionException e) {
                throw new EngineExecutionException(e,  "Branch transaction error: " + e.getCode() + ", StateMachine:" + stateInstance.getStateMachineInstance().getStateMachine().getName() + ", XID: " + stateInstance.getStateMachineInstance().getId() + ", State:" + stateInstance.getName() + ", stateId: " + stateInstance.getId() + ", Reason: " + e.getMessage(), FrameworkErrorCode.TransactionManagerError);
            }

            stateInstance.setSerializedInputParams(paramsSerializer.serialize(stateInstance.getInputParams()));
            sqlSessionExecutor.insert(MAPPER_PREFIX + "recordStateStarted", stateInstance);
        }
    }

    @Override
    public void recordStateFinished(StateInstance stateInstance, ProcessContext context) {

        if (stateInstance != null) {

            stateInstance.setSerializedOutputParams(paramsSerializer.serialize(stateInstance.getOutputParams()));
            stateInstance.setSerializedException(exceptionSerializer.serialize(stateInstance.getException()));
            sqlSessionExecutor.update(MAPPER_PREFIX + "recordStateFinished", stateInstance);

            BranchStatus branchStatus = null;
            try {
                GlobalTransaction globalTransaction = (GlobalTransaction)context.getVariable(DomainConstants.VAR_NAME_GLOBAL_TX);
                if(globalTransaction == null){
                    globalTransaction = GlobalTransactionContext.reload(stateInstance.getStateMachineInstance().getId());
                }

                if(globalTransaction == null){
                    throw new EngineExecutionException("Global transaction is not exists", FrameworkErrorCode.ObjectNotExists);
                }

                if(ExecutionStatus.SU.equals(stateInstance.getStatus()) && stateInstance.getCompensationStatus() == null){
                    branchStatus = BranchStatus.PhaseTwo_Committed;
                }
                else if(ExecutionStatus.SU.equals(stateInstance.getCompensationStatus())){
                    branchStatus = BranchStatus.PhaseTwo_Rollbacked;
                }
                else if(ExecutionStatus.FA.equals(stateInstance.getCompensationStatus())
                        || ExecutionStatus.UN.equals(stateInstance.getCompensationStatus())){
                    branchStatus = BranchStatus.PhaseTwo_RollbackFailed_Retryable;
                }
                else if((ExecutionStatus.FA.equals(stateInstance.getStatus()) || ExecutionStatus.UN.equals(stateInstance.getStatus()))
                        && stateInstance.getCompensationStatus() == null){
                    branchStatus = BranchStatus.PhaseOne_Failed;
                }
                else{
                    branchStatus = BranchStatus.Unknown;
                }

                sagaTransactionalTemplate.branchReport(globalTransaction.getXid(), Long.parseLong(stateInstance.getId()), branchStatus, null);
            } catch (TransactionException e) {
                LOGGER.error("Report branch status to server error: " + e.getCode() +
                        ", StateMachine:" + stateInstance.getStateMachineInstance().getStateMachine().getName() +
                        ", StateName:" + stateInstance.getName() +
                        ", XID: "+stateInstance.getStateMachineInstance().getId() +
                        ", branchId: " + stateInstance.getId() +
                        ", branchStatus:" + branchStatus +
                        ", Reason: " + e.getMessage(), e);
            }
        }
    }


    @Override
    public StateMachineInstance getStateMachineInstance(String stateMachineInstanceId) {

        StateMachineInstance stateMachineInstance = sqlSessionExecutor.selectOne(MAPPER_PREFIX + "getStateMachineInstanceById", stateMachineInstanceId);
        if (stateMachineInstance == null) {
            return null;
        }
        List<StateInstance> stateInstanceList = queryStateInstanceListByMachineInstanceId(stateMachineInstanceId);
        for (StateInstance stateInstance : stateInstanceList) {
            stateMachineInstance.putStateInstance(stateInstance.getId(), stateInstance);
        }
        deserializeParamsAndException(stateMachineInstance);

        return stateMachineInstance;
    }

    @Override
    public StateMachineInstance getStateMachineInstanceByBusinessKey(String businessKey, String tenantId) {

        if(StringUtils.isEmpty(tenantId)){
            tenantId = defaultTenantId;
        }
        Map<String, String> params = new HashMap<>(2);
        params.put("businessKey", businessKey);
        params.put("tenantId", tenantId);
        StateMachineInstance stateMachineInstance = sqlSessionExecutor.selectOne(MAPPER_PREFIX + "getStateMachineInstanceByBusinessKey", params);
        if (stateMachineInstance == null) {
            return null;
        }
        List<StateInstance> stateInstanceList = queryStateInstanceListByMachineInstanceId(stateMachineInstance.getId());
        for (StateInstance stateInstance : stateInstanceList) {
            stateMachineInstance.putStateInstance(stateInstance.getId(), stateInstance);
        }
        deserializeParamsAndException(stateMachineInstance);

        return stateMachineInstance;
    }

    private void deserializeParamsAndException(StateMachineInstance stateMachineInstance) {
        byte[] serializedException = (byte[])stateMachineInstance.getSerializedException();
        if (serializedException != null) {
            stateMachineInstance.setException((Exception) exceptionSerializer.deserialize(serializedException));
        }

        String serializedStartParams = (String)stateMachineInstance.getSerializedStartParams();
        if(StringUtils.hasLength(serializedStartParams)){
            stateMachineInstance.setStartParams((Map<String, Object>) paramsSerializer.deserialize(serializedStartParams));
        }

        String serializedEndParams = (String)stateMachineInstance.getSerializedEndParams();
        if(StringUtils.hasLength(serializedEndParams)){
            stateMachineInstance.setEndParams((Map<String, Object>) paramsSerializer.deserialize(serializedEndParams));
        }
    }

    @Override
    public List<StateMachineInstance> queryStateMachineInstanceByParentId(String parentId) {
        return sqlSessionExecutor.selectList(MAPPER_PREFIX + "queryStateMachineInstanceByParentId", parentId);
    }

    @Override
    public StateInstance getStateInstance(String stateInstanceId, String machineInstId) {
        Map<String, String> params = new HashMap<>(2);
        params.put("machineInstanceId", machineInstId);
        params.put("id", stateInstanceId);
        StateInstance stateInstance = sqlSessionExecutor.selectOne(MAPPER_PREFIX + "getStateInstanceByIdAndMachineInstId", params);

        deserializeParamsAndException(stateInstance);
        return stateInstance;
    }

    private void deserializeParamsAndException(StateInstance stateInstance) {
        if (stateInstance != null) {
            String inputParams = (String) stateInstance.getSerializedInputParams();
            if(StringUtils.hasLength(inputParams)){
                stateInstance.setInputParams(paramsSerializer.deserialize(inputParams));
            }
            String outputParams = (String) stateInstance.getSerializedOutputParams();
            if(StringUtils.hasLength(outputParams)){
                stateInstance.setOutputParams(paramsSerializer.deserialize(outputParams));
            }
            byte[] serializedException = (byte[]) stateInstance.getSerializedException();
            if(serializedException != null){
                stateInstance.setException((Exception) exceptionSerializer.deserialize(serializedException));
            }
        }
    }

    @Override
    public List<StateInstance> queryStateInstanceListByMachineInstanceId(String stateMachineInstanceId) {

        List<StateInstance> stateInstanceList = sqlSessionExecutor.selectList(MAPPER_PREFIX + "queryStateInstanceListByMachineInstanceId", stateMachineInstanceId);

        if (stateInstanceList == null || stateInstanceList.size() == 0) {
            return stateInstanceList;
        }
        StateInstance lastStateInstance = stateInstanceList.get(stateInstanceList.size() - 1);
        if (lastStateInstance.getGmtEnd() == null) {
            lastStateInstance.setStatus(ExecutionStatus.RU);
        }
        Map<String, StateInstance> originStateMap = new HashMap<>();
        Map<String, StateInstance> compensateStateMap = new HashMap<>();
        Map<String, StateInstance> retriedStateMap = new HashMap<>();
        for (int i = 0; i < stateInstanceList.size(); i++) {
            StateInstance tempStateInstance = stateInstanceList.get(i);

            deserializeParamsAndException(tempStateInstance);

            if (StringUtils.hasText(tempStateInstance.getStateIdCompensatedFor())) {
                updateStateMap(compensateStateMap, tempStateInstance, tempStateInstance.getStateIdCompensatedFor());
            } else {
                if (StringUtils.hasText(tempStateInstance.getStateIdRetriedFor())) {
                    updateStateMap(retriedStateMap, tempStateInstance, tempStateInstance.getStateIdRetriedFor());
                }
                originStateMap.put(tempStateInstance.getId(), tempStateInstance);
            }
        }

        if (compensateStateMap.size() != 0) {
            for (StateInstance origState : originStateMap.values()) {
                origState.setCompensationState(compensateStateMap.get(origState.getId()));
            }
        }

        if (retriedStateMap.size() != 0) {
            for (StateInstance origState : originStateMap.values()) {
                if (retriedStateMap.containsKey(origState.getId())) {
                    origState.setIgnoreStatus(true);
                }
            }
        }
        return stateInstanceList;
    }

    private void updateStateMap(Map<String, StateInstance> resultMap, StateInstance newState, String key) {
        if (!resultMap.containsKey(key)) {

            resultMap.put(key, newState);
        } else if (newState.getGmtEnd().after(resultMap.get(key).getGmtEnd())) {

            resultMap.get(key).setIgnoreStatus(true);
            resultMap.remove(key);
            resultMap.put(key, newState);
        } else {

            newState.setIgnoreStatus(true);
        }

    }

    public void setSqlSessionExecutor(SqlSessionExecutor sqlSessionExecutor) {
        this.sqlSessionExecutor = sqlSessionExecutor;
    }

    public void setExceptionSerializer(ObjectSerializer<Exception, byte[]> exceptionSerializer) {
        this.exceptionSerializer = exceptionSerializer;
    }

    public SagaTransactionalTemplate getSagaTransactionalTemplate() {
        return sagaTransactionalTemplate;
    }

    public void setSagaTransactionalTemplate(SagaTransactionalTemplate sagaTransactionalTemplate) {
        this.sagaTransactionalTemplate = sagaTransactionalTemplate;
    }

    public ObjectSerializer<Object, String> getParamsSerializer() {
        return paramsSerializer;
    }

    public void setParamsSerializer(ObjectSerializer<Object, String> paramsSerializer) {
        this.paramsSerializer = paramsSerializer;
    }

    public String getDefaultTenantId() {
        return defaultTenantId;
    }

    public void setDefaultTenantId(String defaultTenantId) {
        this.defaultTenantId = defaultTenantId;
    }
}