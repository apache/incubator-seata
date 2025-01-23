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
package org.apache.seata.saga.rm;

import org.apache.seata.common.exception.ExceptionUtil;
import org.apache.seata.common.exception.RepeatRegistrationException;
import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.Resource;
import org.apache.seata.integration.tx.api.remoting.TwoPhaseResult;
import org.apache.seata.rm.AbstractResourceManager;
import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.rm.tcc.api.BusinessActionContextUtil;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Saga annotation resource manager
 */
public class SagaAnnotationResourceManager extends AbstractResourceManager {

    /**
     * TCC resource cache
     */
    private final Map<String, Resource> resourceCache = new ConcurrentHashMap<>();

    @Override
    public void registerResource(Resource resource) {
        String resourceId = resource.getResourceId();
        SagaAnnotationResource newResource = (SagaAnnotationResource) resource;
        SagaAnnotationResource oldResource = (SagaAnnotationResource) resourceCache.get(resourceId);

        if (oldResource != null) {
            Object newResourceBean = newResource.getTargetBean();
            Object oldResourceBean = oldResource.getTargetBean();
            if (newResourceBean != oldResourceBean) {
                throw new RepeatRegistrationException(String.format("Same SagaAnnotation resource name <%s> between method1 <%s> of class1 <%s> and method2 <%s> of class2 <%s>, should be unique",
                        resourceId,
                        newResource.getActionName(),
                        newResourceBean.getClass().getName(),
                        oldResource.getActionName(),
                        oldResourceBean.getClass().getName()));
            }
        }

        resourceCache.put(resourceId, newResource);
        super.registerResource(newResource);
    }

    /**
     * saga branch commit
     *
     * @param branchType
     * @param xid             Transaction id.
     * @param branchId        Branch id.
     * @param resourceId      Resource id.
     * @param applicationData Application data bind with this branch.
     * @return BranchStatus
     */
    @Override
    public BranchStatus branchCommit(BranchType branchType, String xid, long branchId, String resourceId, String applicationData) {
        //impossible to reach here
        return BranchStatus.PhaseTwo_Committed;
    }

    @Override
    public BranchStatus branchRollback(BranchType branchType, String xid, long branchId, String resourceId, String applicationData) throws TransactionException {
        SagaAnnotationResource resource = (SagaAnnotationResource) resourceCache.get(resourceId);
        if (resource == null) {
            throw new ShouldNeverHappenException(String.format("SagaAnnotation resource is not exist, resourceId: %s", resourceId));
        }

        Object targetBean = resource.getTargetBean();
        Method compensationMethod = resource.getCompensationMethod();
        if (targetBean == null || compensationMethod == null) {
            throw new ShouldNeverHappenException(String.format("SagaAnnotation resource is not available, resourceId: %s", resourceId));
        }

        try {
            BusinessActionContext businessActionContext = BusinessActionContextUtil.getBusinessActionContext(xid, branchId, resourceId,
                    applicationData);
            Object[] args = this.getTwoPhaseRollbackArgs(resource, businessActionContext);
            BusinessActionContextUtil.setContext(businessActionContext);

            boolean result;
            Object ret = compensationMethod.invoke(targetBean, args);
            if (ret != null) {
                if (ret instanceof TwoPhaseResult) {
                    result = ((TwoPhaseResult) ret).isSuccess();
                } else {
                    result = (boolean) ret;
                }
            } else {
                result = true;
            }

            LOGGER.info("SagaAnnotation resource rollback result : {}, xid: {}, branchId: {}, resourceId: {}", result, xid, branchId, resourceId);
            return result ? BranchStatus.PhaseTwo_Rollbacked : BranchStatus.PhaseTwo_RollbackFailed_Retryable;
        } catch (Throwable t) {
            String msg = String.format("rollback SagaAnnotation resource error, resourceId: %s, xid: %s.", resourceId, xid);
            LOGGER.error(msg, ExceptionUtil.unwrap(t));
            return BranchStatus.PhaseTwo_RollbackFailed_Retryable;
        } finally {
            BusinessActionContextUtil.clear();
        }
    }

    @Override
    public Map<String, Resource> getManagedResources() {
        return resourceCache;
    }

    @Override
    public BranchType getBranchType() {
        return BranchType.SAGA_ANNOTATION;
    }

    private Object[] getTwoPhaseRollbackArgs(SagaAnnotationResource resource, BusinessActionContext businessActionContext) {
        String[] keys = resource.getPhaseTwoCompensationKeys();
        Class<?>[] argsRollbackClasses = resource.getCompensationArgsClasses();
        return getTwoPhaseMethodParams(keys, argsRollbackClasses, businessActionContext);
    }

    protected Object[] getTwoPhaseMethodParams(String[] keys, Class<?>[] argsClasses, BusinessActionContext businessActionContext) {
        Object[] args = new Object[argsClasses.length];
        for (int i = 0; i < argsClasses.length; i++) {
            if (argsClasses[i].equals(BusinessActionContext.class)) {
                args[i] = businessActionContext;
            } else {
                args[i] = businessActionContext.getActionContext(keys[i], argsClasses[i]);
            }
        }
        return args;
    }
}