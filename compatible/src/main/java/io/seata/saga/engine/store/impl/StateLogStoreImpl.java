package io.seata.saga.engine.store.impl;

import io.seata.saga.engine.store.StateLogStore;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.proctrl.impl.ProcessContextImpl;
import io.seata.saga.statelang.domain.StateInstance;
import io.seata.saga.statelang.domain.StateMachineInstance;
import io.seata.saga.statelang.domain.impl.StateInstanceImpl;
import io.seata.saga.statelang.domain.impl.StateMachineInstanceImpl;
import org.apache.seata.common.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StateLogStoreImpl implements StateLogStore {

    private final org.apache.seata.saga.engine.store.StateLogStore actual;

    private StateLogStoreImpl(org.apache.seata.saga.engine.store.StateLogStore actual) {
        this.actual = actual;
    }

    @Override
    public void recordStateMachineStarted(StateMachineInstance machineInstance, ProcessContext context) {
        actual.recordStateMachineStarted(((StateMachineInstanceImpl) machineInstance).unwrap(), ((ProcessContextImpl) context).unwrap());
    }

    @Override
    public void recordStateMachineFinished(StateMachineInstance machineInstance, ProcessContext context) {
        actual.recordStateMachineFinished(((StateMachineInstanceImpl) machineInstance).unwrap(), ((ProcessContextImpl) context).unwrap());
    }

    @Override
    public void recordStateMachineRestarted(StateMachineInstance machineInstance, ProcessContext context) {
        actual.recordStateMachineRestarted(((StateMachineInstanceImpl) machineInstance).unwrap(), ((ProcessContextImpl) context).unwrap());
    }

    @Override
    public void recordStateStarted(StateInstance stateInstance, ProcessContext context) {
        actual.recordStateStarted(((StateInstanceImpl) stateInstance).unwrap(), ((ProcessContextImpl) context).unwrap());
    }

    @Override
    public void recordStateFinished(StateInstance stateInstance, ProcessContext context) {
        actual.recordStateFinished(((StateInstanceImpl) stateInstance).unwrap(), ((ProcessContextImpl) context).unwrap());
    }

    @Override
    public StateMachineInstance getStateMachineInstance(String stateMachineInstanceId) {
        org.apache.seata.saga.statelang.domain.StateMachineInstance stateMachineInstance = actual.getStateMachineInstance(stateMachineInstanceId);
        return StateMachineInstanceImpl.wrap(stateMachineInstance);
    }

    @Override
    public StateMachineInstance getStateMachineInstanceByBusinessKey(String businessKey, String tenantId) {
        org.apache.seata.saga.statelang.domain.StateMachineInstance stateMachineInstance = actual.getStateMachineInstanceByBusinessKey(businessKey, tenantId);
        return StateMachineInstanceImpl.wrap(stateMachineInstance);
    }

    @Override
    public List<StateMachineInstance> queryStateMachineInstanceByParentId(String parentId) {
        List<org.apache.seata.saga.statelang.domain.StateMachineInstance> stateMachineInstances = actual.queryStateMachineInstanceByParentId(parentId);
        if (CollectionUtils.isEmpty(stateMachineInstances)) {
            return new ArrayList<>();
        }
        return stateMachineInstances.stream().map(StateMachineInstanceImpl::wrap).collect(Collectors.toList());
    }

    @Override
    public StateInstance getStateInstance(String stateInstanceId, String machineInstId) {
        org.apache.seata.saga.statelang.domain.StateInstance stateInstance = actual.getStateInstance(stateInstanceId, machineInstId);
        if (stateInstance == null) {
            return null;
        }
        return StateInstanceImpl.wrap(stateInstance);
    }

    @Override
    public List<StateInstance> queryStateInstanceListByMachineInstanceId(String stateMachineInstanceId) {
        List<org.apache.seata.saga.statelang.domain.StateInstance> stateInstances = actual.queryStateInstanceListByMachineInstanceId(stateMachineInstanceId);
        if (CollectionUtils.isEmpty(stateInstances)) {
            return new ArrayList<>();
        }
        return stateInstances.stream().map(StateInstanceImpl::wrap).collect(Collectors.toList());
    }

    @Override
    public void clearUp(ProcessContext context) {
        actual.clearUp(((ProcessContextImpl) context).unwrap());
    }

    public static StateLogStore wrap(org.apache.seata.saga.engine.store.StateLogStore actual) {
        return new StateLogStoreImpl(actual);
    }

    public org.apache.seata.saga.engine.store.StateLogStore unwrap() {
        return actual;
    }
}
