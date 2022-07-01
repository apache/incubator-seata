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
package io.seata.saga.rm.annotation;

import io.seata.common.Constants;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.exception.SkipCallbackWrapperException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.Resource;
import io.seata.rm.AbstractResourceManager;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextUtil;
import io.seata.spring.fence.CommonFenceHandler;
import io.seata.spring.remoting.TwoPhaseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Saga annotation resource manager
 *
 * @author ruishansun
 */
public class SagaAnnotationResourceManager extends AbstractResourceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SagaAnnotationResourceManager.class);

    /**
     * Saga annotation resource cache
     */
    private final Map<String, Resource> sagaAnnotationResourceCache = new ConcurrentHashMap<>();

    /**
     * Instantiates a new saga annotation resource manager.
     */
    public SagaAnnotationResourceManager() {
    }

    /**
     * registry saga annotation resource
     *
     * @param resource The resource to be managed.
     */
    @Override
    public void registerResource(Resource resource) {
        SagaAnnotationResource sagaAnnotationResource = (SagaAnnotationResource) resource;
        sagaAnnotationResourceCache.put(sagaAnnotationResource.getResourceId(), sagaAnnotationResource);
        super.registerResource(sagaAnnotationResource);
    }

    @Override
    public Map<String, Resource> getManagedResources() {
        return sagaAnnotationResourceCache;
    }

    @Override
    public BranchStatus branchCommit(BranchType branchType, String xid, long branchId, String resourceId, String applicationData) {

        // Saga annotation mode
        return BranchStatus.PhaseTwo_Committed;
    }

    @Override
    public BranchStatus branchRollback(BranchType branchType, String xid, long branchId, String resourceId, String applicationData) {
        // Saga annotation mode
        SagaAnnotationResource sagaAnnotationResource = (SagaAnnotationResource) sagaAnnotationResourceCache.get(resourceId);
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
            BusinessActionContext businessActionContext = BusinessActionContextUtil.getBusinessActionContext(xid, branchId, resourceId, applicationData);
            Object[] args = this.getTwoPhaseCompensationArgs(sagaAnnotationResource, businessActionContext);

            Object ret;
            boolean result;
            // add idempotent and anti hanging
            if (Boolean.TRUE.equals(businessActionContext.getActionContext(Constants.USE_COMMON_FENCE))) {
                try {
                    result = CommonFenceHandler.rollbackFence(compensationMethod, targetSagaBean, xid, branchId, args, sagaAnnotationResource.getActionName());
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
        return BranchType.SAGA_ANNOTATION;
    }
}
