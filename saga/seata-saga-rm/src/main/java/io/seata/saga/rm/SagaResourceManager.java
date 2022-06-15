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
package io.seata.saga.rm;

import io.seata.common.Constants;
import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.exception.SkipCallbackWrapperException;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.GlobalStatus;
import io.seata.core.model.Resource;
import io.seata.rm.AbstractResourceManager;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextUtil;
import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.engine.exception.ForwardInvalidException;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.RecoverStrategy;
import io.seata.saga.statelang.domain.StateMachineInstance;
import io.seata.spring.fence.TCCFenceHandler;
import io.seata.spring.remoting.TwoPhaseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Saga resource manager
 *
 * @author lorne.cl
 */
public class SagaResourceManager extends AbstractResourceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SagaResourceManager.class);

    /**
     * Saga resource cache
     */
    private Map<String, Resource> sagaResourceCache = new ConcurrentHashMap<>();

    /**
     * Instantiates a new saga resource manager.
     */
    public SagaResourceManager() {
    }

    /**
     * registry saga resource
     *
     * @param resource The resource to be managed.
     */
    @Override
    public void registerResource(Resource resource) {
        SagaResource sagaResource = (SagaResource) resource;
        sagaResourceCache.put(sagaResource.getResourceId(), sagaResource);
        super.registerResource(sagaResource);
    }

    @Override
    public Map<String, Resource> getManagedResources() {
        return sagaResourceCache;
    }

    /**
     * SAGA branch commit
     *
     * @param branchType
     * @param xid             Transaction id.
     * @param branchId        Branch id.
     * @param resourceId      Resource id.
     * @param applicationData Application data bind with this branch.
     * @return
     * @throws TransactionException
     */
    @Override
    public BranchStatus branchCommit(BranchType branchType, String xid, long branchId, String resourceId,
                                     String applicationData) throws TransactionException {

        // Saga annotation mode
        SagaResource sagaResource = (SagaResource) sagaResourceCache.get(resourceId);
        if (sagaResource == null) {
            throw new ShouldNeverHappenException(String.format("Saga resource is not exist, resourceId: %s", resourceId));
        }
        if (sagaResource.isUseSagaAnnotationMode()) {
            return BranchStatus.PhaseTwo_Committed;
        }

        // Saga state machine mode
        try {
            StateMachineInstance machineInstance = StateMachineEngineHolder.getStateMachineEngine().forward(xid, null);

            if (ExecutionStatus.SU.equals(machineInstance.getStatus())
                    && machineInstance.getCompensationStatus() == null) {
                return BranchStatus.PhaseTwo_Committed;
            } else if (ExecutionStatus.SU.equals(machineInstance.getCompensationStatus())) {
                return BranchStatus.PhaseTwo_Rollbacked;
            } else if (ExecutionStatus.FA.equals(machineInstance.getCompensationStatus()) || ExecutionStatus.UN.equals(
                    machineInstance.getCompensationStatus())) {
                return BranchStatus.PhaseTwo_RollbackFailed_Retryable;
            } else if (ExecutionStatus.FA.equals(machineInstance.getStatus())
                    && machineInstance.getCompensationStatus() == null) {
                return BranchStatus.PhaseOne_Failed;
            }

        } catch (ForwardInvalidException e) {
            LOGGER.error("StateMachine forward failed, xid: " + xid, e);

            //if StateMachineInstanceNotExists stop retry
            if (FrameworkErrorCode.StateMachineInstanceNotExists.equals(e.getErrcode())) {
                return BranchStatus.PhaseTwo_Committed;
            }
        } catch (Exception e) {
            LOGGER.error("StateMachine forward failed, xid: " + xid, e);
        }
        return BranchStatus.PhaseTwo_CommitFailed_Retryable;
    }

    /**
     * SAGA branch rollback
     *
     * @param branchType      the branch type
     * @param xid             Transaction id.
     * @param branchId        Branch id.
     * @param resourceId      Resource id.
     * @param applicationData Application data bind with this branch.
     * @return
     * @throws TransactionException
     */
    @Override
    public BranchStatus branchRollback(BranchType branchType, String xid, long branchId, String resourceId,
                                       String applicationData) throws TransactionException {

        // Saga annotation mode
        SagaResource sagaResource = (SagaResource) sagaResourceCache.get(resourceId);
        if (sagaResource == null) {
            throw new ShouldNeverHappenException(String.format("Saga resource is not exist, resourceId: %s", resourceId));
        }
        if (sagaResource.isUseSagaAnnotationMode()) {
            return this.sagaCompensateBranch(xid, branchId, resourceId, applicationData, (SagaAnnotationResource) sagaResource);
        }

        // Saga state machine mode
        try {
            StateMachineInstance stateMachineInstance = StateMachineEngineHolder.getStateMachineEngine().reloadStateMachineInstance(xid);
            if (stateMachineInstance == null) {
                return BranchStatus.PhaseTwo_Rollbacked;
            }
            if (RecoverStrategy.Forward.equals(stateMachineInstance.getStateMachine().getRecoverStrategy())
                    && (GlobalStatus.TimeoutRollbacking.name().equals(applicationData)
                    || GlobalStatus.TimeoutRollbackRetrying.name().equals(applicationData))) {
                LOGGER.warn("Retry by custom recover strategy [Forward] on timeout, SAGA global[{}]", xid);
                return BranchStatus.PhaseTwo_CommitFailed_Retryable;
            }

            stateMachineInstance = StateMachineEngineHolder.getStateMachineEngine().compensate(xid,
                    null);
            if (ExecutionStatus.SU.equals(stateMachineInstance.getCompensationStatus())) {
                return BranchStatus.PhaseTwo_Rollbacked;
            }
        } catch (EngineExecutionException e) {
            LOGGER.error("StateMachine compensate failed, xid: " + xid, e);

            //if StateMachineInstanceNotExists stop retry
            if (FrameworkErrorCode.StateMachineInstanceNotExists.equals(e.getErrcode())) {
                return BranchStatus.PhaseTwo_Rollbacked;
            }
        } catch (Exception e) {
            LOGGER.error("StateMachine compensate failed, xid: " + xid, e);
        }
        return BranchStatus.PhaseTwo_RollbackFailed_Retryable;
    }

    private BranchStatus sagaCompensateBranch(String xid, long branchId, String resourceId, String applicationData, SagaAnnotationResource sagaAnnotationResource) {
        if (sagaAnnotationResource == null) {
            throw new ShouldNeverHappenException(String.format("Saga annotation resource is not exist, resourceId: %s", resourceId));
        }
        Object targetSagaBean = sagaAnnotationResource.getTargetBean();
        Method compensationMethod = sagaAnnotationResource.getCompensationMethod();
        if (targetSagaBean == null || compensationMethod == null) {
            throw new ShouldNeverHappenException(String.format("Saga target Bean or compensationMethod is not available, resourceId: %s", resourceId));
        }
        try {
            //BusinessActionContext
            BusinessActionContext businessActionContext = BusinessActionContextUtil.getBusinessActionContext(xid, branchId, resourceId,
                    applicationData);
            Object[] args = this.getTwoPhaseCompensationArgs(sagaAnnotationResource, businessActionContext);

            Object ret;
            boolean result;
            // add idempotent and anti hanging
            if (Boolean.TRUE.equals(businessActionContext.getActionContext(Constants.USE_TCC_FENCE))) {
                try {
                    result = TCCFenceHandler.rollbackFence(compensationMethod, targetSagaBean, xid, branchId,
                            args, sagaAnnotationResource.getActionName());
                } catch (SkipCallbackWrapperException | UndeclaredThrowableException e) {
                    throw e.getCause();
                }
            } else {
                ret = compensationMethod.invoke(targetSagaBean, args);
                if (ret != null) {
                    if (ret instanceof TwoPhaseResult) {
                        result = ((TwoPhaseResult) ret).isSuccess();
                    } else {
                        result = (boolean) ret;
                    }
                } else {
                    result = true;
                }
            }
            LOGGER.info("Saga resource compensation result : {}, xid: {}, branchId: {}, resourceId: {}", result, xid, branchId, resourceId);
            return result ? BranchStatus.PhaseTwo_Rollbacked : BranchStatus.PhaseTwo_RollbackFailed_Retryable;
        } catch (Throwable t) {
            String msg = String.format("compensation Saga resource error, resourceId: %s, xid: %s.", resourceId, xid);
            LOGGER.error(msg, t);
            return BranchStatus.PhaseTwo_RollbackFailed_Retryable;
        }
    }

    /**
     * get phase two compensate method's args
     *
     * @param sagaAnnotationResource sagaAnnotationResource
     * @param businessActionContext  businessActionContext
     * @return args
     */
    private Object[] getTwoPhaseCompensationArgs(SagaAnnotationResource sagaAnnotationResource, BusinessActionContext businessActionContext) {
        String[] keys = sagaAnnotationResource.getPhaseTwoCompensationKeys();
        Class<?>[] argsCommitClasses = sagaAnnotationResource.getCompensationArgsClasses();
        return BusinessActionContextUtil.getTwoPhaseMethodParams(keys, argsCommitClasses, businessActionContext);
    }

    @Override
    public BranchType getBranchType() {
        return BranchType.SAGA;
    }
}
