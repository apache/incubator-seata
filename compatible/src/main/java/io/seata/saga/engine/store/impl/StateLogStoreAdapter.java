package io.seata.saga.engine.store.impl;

import io.seata.saga.engine.store.StateLogStore;
import io.seata.saga.proctrl.impl.ProcessContextImpl;
import io.seata.saga.statelang.domain.impl.StateInstanceImpl;
import io.seata.saga.statelang.domain.impl.StateMachineInstanceImpl;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.saga.proctrl.HierarchicalProcessContext;
import org.apache.seata.saga.proctrl.ProcessContext;
import org.apache.seata.saga.statelang.domain.StateInstance;
import org.apache.seata.saga.statelang.domain.StateMachineInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StateLogStoreAdapter implements org.apache.seata.saga.engine.store.StateLogStore {

    private final StateLogStore actual;

    public StateLogStoreAdapter(StateLogStore actual) {
        this.actual = actual;
    }

    @Override
    public void recordStateMachineStarted(StateMachineInstance machineInstance, ProcessContext context) {
        io.seata.saga.statelang.domain.StateMachineInstance wrapStateMachineInstance = StateMachineInstanceImpl.wrap(machineInstance);
        io.seata.saga.proctrl.ProcessContext wrapContext = ProcessContextImpl.wrap((HierarchicalProcessContext) context);
        actual.recordStateMachineStarted(wrapStateMachineInstance, wrapContext);
    }

    @Override
    public void recordStateMachineFinished(StateMachineInstance machineInstance, ProcessContext context) {
        io.seata.saga.statelang.domain.StateMachineInstance wrapStateMachineInstance = StateMachineInstanceImpl.wrap(machineInstance);
        io.seata.saga.proctrl.ProcessContext wrapContext = ProcessContextImpl.wrap((HierarchicalProcessContext) context);
        actual.recordStateMachineFinished(wrapStateMachineInstance, wrapContext);
    }

    @Override
    public void recordStateMachineRestarted(StateMachineInstance machineInstance, ProcessContext context) {
        io.seata.saga.statelang.domain.StateMachineInstance wrapStateMachineInstance = StateMachineInstanceImpl.wrap(machineInstance);
        io.seata.saga.proctrl.ProcessContext wrapContext = ProcessContextImpl.wrap((HierarchicalProcessContext) context);
        actual.recordStateMachineRestarted(wrapStateMachineInstance, wrapContext);
    }

    @Override
    public void recordStateStarted(StateInstance stateInstance, ProcessContext context) {
        io.seata.saga.statelang.domain.StateInstance wrapStateInstance = StateInstanceImpl.wrap(stateInstance);
        io.seata.saga.proctrl.ProcessContext wrapContext = ProcessContextImpl.wrap((HierarchicalProcessContext) context);
        actual.recordStateStarted(wrapStateInstance, wrapContext);
    }

    @Override
    public void recordStateFinished(StateInstance stateInstance, ProcessContext context) {
        io.seata.saga.statelang.domain.StateInstance wrapStateInstance = StateInstanceImpl.wrap(stateInstance);
        io.seata.saga.proctrl.ProcessContext wrapContext = ProcessContextImpl.wrap((HierarchicalProcessContext) context);
        actual.recordStateFinished(wrapStateInstance, wrapContext);
    }

    @Override
    public StateMachineInstance getStateMachineInstance(String stateMachineInstanceId) {
        io.seata.saga.statelang.domain.StateMachineInstance stateMachineInstance = actual.getStateMachineInstance(stateMachineInstanceId);
        return ((StateMachineInstanceImpl) stateMachineInstance).unwrap();
    }

    @Override
    public StateMachineInstance getStateMachineInstanceByBusinessKey(String businessKey, String tenantId) {
        io.seata.saga.statelang.domain.StateMachineInstance stateMachineInstance = actual.getStateMachineInstanceByBusinessKey(businessKey, tenantId);
        return ((StateMachineInstanceImpl) stateMachineInstance).unwrap();
    }

    @Override
    public List<StateMachineInstance> queryStateMachineInstanceByParentId(String parentId) {
        List<io.seata.saga.statelang.domain.StateMachineInstance> stateMachineInstances = actual.queryStateMachineInstanceByParentId(parentId);
        if (CollectionUtils.isEmpty(stateMachineInstances)) {
            return new ArrayList<>();
        }

        return stateMachineInstances.stream().map(stateMachineInstance -> ((StateMachineInstanceImpl)stateMachineInstance).unwrap()).collect(Collectors.toList());
    }

    @Override
    public StateInstance getStateInstance(String stateInstanceId, String machineInstId) {
        io.seata.saga.statelang.domain.StateInstance stateInstance = actual.getStateInstance(stateInstanceId, machineInstId);
        return ((StateInstanceImpl) stateInstance).unwrap();
    }

    @Override
    public List<StateInstance> queryStateInstanceListByMachineInstanceId(String stateMachineInstanceId) {
        List<io.seata.saga.statelang.domain.StateInstance> stateInstances = actual.queryStateInstanceListByMachineInstanceId(stateMachineInstanceId);
        if (CollectionUtils.isEmpty(stateInstances)) {
            return new ArrayList<>();
        }
        return stateInstances.stream().map(stateInstance -> ((StateInstanceImpl)stateInstance).unwrap()).collect(Collectors.toList());
    }

    @Override
    public void clearUp(ProcessContext context) {
        io.seata.saga.proctrl.ProcessContext wrapContext = ProcessContextImpl.wrap((HierarchicalProcessContext) context);
        actual.clearUp(wrapContext);
    }

    public StateLogStore getActual() {
        return actual;
    }
}
