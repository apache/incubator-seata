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
package org.apache.seata.rm.tcc;

import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.seata.common.Constants;
import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.common.exception.SkipCallbackWrapperException;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.Resource;
import org.apache.seata.integration.tx.api.fence.DefaultCommonFenceHandler;
import org.apache.seata.integration.tx.api.remoting.TwoPhaseResult;
import org.apache.seata.rm.AbstractResourceManager;
import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.rm.tcc.api.BusinessActionContextUtil;

/**
 * TCC resource manager
 *
 */
public class TCCResourceManager extends AbstractResourceManager {

    /**
     * TCC resource cache
     */
    private Map<String, Resource> resourceCache = new ConcurrentHashMap<>();

    /**
     * Instantiates a new Tcc resource manager.
     */
    public TCCResourceManager() {
        // not do anything
    }

    /**
     * registry resource
     *
     * @param resource The resource to be managed.
     */
    @Override
    public void registerResource(Resource resource) {
        resourceCache.put(resource.getResourceId(), resource);
        super.registerResource(resource);
    }

    @Override
    public Map<String, Resource> getManagedResources() {
        return resourceCache;
    }

    /**
     * TCC branch commit
     *
     * @param branchType
     * @param xid             Transaction id.
     * @param branchId        Branch id.
     * @param resourceId      Resource id.
     * @param applicationData Application data bind with this branch.
     * @return BranchStatus
     * @throws TransactionException TransactionException
     */
    @Override
    public BranchStatus branchCommit(BranchType branchType, String xid, long branchId, String resourceId,
                                     String applicationData) throws TransactionException {
        TCCResource tccResource = (TCCResource)resourceCache.get(resourceId);
        if (tccResource == null) {
            throw new ShouldNeverHappenException(String.format("TCC resource is not exist, resourceId: %s", resourceId));
        }
        Object targetTCCBean = tccResource.getTargetBean();
        Method commitMethod = tccResource.getCommitMethod();
        if (targetTCCBean == null || commitMethod == null) {
            throw new ShouldNeverHappenException(String.format("TCC resource is not available, resourceId: %s", resourceId));
        }
        try {
            //BusinessActionContext
            BusinessActionContext businessActionContext = BusinessActionContextUtil.getBusinessActionContext(xid, branchId, resourceId,
                    applicationData);

            Object[] args = this.getTwoPhaseCommitArgs(tccResource, businessActionContext);
            Object ret;
            boolean result;
            // add idempotent and anti hanging
            if (Boolean.TRUE.equals(businessActionContext.getActionContext(Constants.USE_COMMON_FENCE))) {
                try {
                    result = DefaultCommonFenceHandler.get().commitFence(commitMethod, targetTCCBean, xid, branchId, args);
                } catch (SkipCallbackWrapperException | UndeclaredThrowableException e) {
                    throw e.getCause();
                }
            } else {
                ret = commitMethod.invoke(targetTCCBean, args);
                if (ret != null) {
                    if (ret instanceof TwoPhaseResult) {
                        result = ((TwoPhaseResult)ret).isSuccess();
                    } else {
                        result = (boolean)ret;
                    }
                } else {
                    result = true;
                }
            }
            LOGGER.info("TCC resource commit result : {}, xid: {}, branchId: {}, resourceId: {}", result, xid, branchId, resourceId);
            return result ? BranchStatus.PhaseTwo_Committed : BranchStatus.PhaseTwo_CommitFailed_Retryable;
        } catch (Throwable t) {
            String msg = String.format("commit TCC resource error, resourceId: %s, xid: %s.", resourceId, xid);
            LOGGER.error(msg, t);
            return BranchStatus.PhaseTwo_CommitFailed_Retryable;
        }
    }

    /**
     * TCC branch rollback
     *
     * @param branchType      the branch type
     * @param xid             Transaction id.
     * @param branchId        Branch id.
     * @param resourceId      Resource id.
     * @param applicationData Application data bind with this branch.
     * @return BranchStatus
     * @throws TransactionException TransactionException
     */
    @Override
    public BranchStatus branchRollback(BranchType branchType, String xid, long branchId, String resourceId,
                                       String applicationData) throws TransactionException {
        Resource resource = resourceCache.get(resourceId);
        if (resource == null) {
            throw new ShouldNeverHappenException(String.format("%s resource is not exist, resourceId: %s", getBranchType(), resourceId));
        }
        Object targetTCCBean = getTargetBean(resource);
        Method rollbackMethod = getRollbackMethod(resource);
        if (targetTCCBean == null || rollbackMethod == null) {
            throw new ShouldNeverHappenException(String.format("%s resource is not available, resourceId: %s", getBranchType(), resourceId));
        }
        try {
            //BusinessActionContext
            BusinessActionContext businessActionContext = BusinessActionContextUtil.getBusinessActionContext(xid, branchId, resourceId,
                    applicationData);
            Object[] args = this.getTwoPhaseRollbackArgs(resource, businessActionContext);
            Object ret;
            boolean result;
            // add idempotent and anti hanging
            if (Boolean.TRUE.equals(businessActionContext.getActionContext(Constants.USE_COMMON_FENCE))) {
                try {
                    result = DefaultCommonFenceHandler.get().rollbackFence(rollbackMethod, targetTCCBean, xid, branchId,
                            args, resolveActionName(resource));
                } catch (SkipCallbackWrapperException | UndeclaredThrowableException e) {
                    throw e.getCause();
                }
            } else {
                ret = rollbackMethod.invoke(targetTCCBean, args);
                if (ret != null) {
                    if (ret instanceof TwoPhaseResult) {
                        result = ((TwoPhaseResult)ret).isSuccess();
                    } else {
                        result = (boolean)ret;
                    }
                } else {
                    result = true;
                }
            }
            LOGGER.info("{} resource rollback result : {}, xid: {}, branchId: {}, resourceId: {}", getBranchType(), result, xid, branchId, resourceId);
            return result ? BranchStatus.PhaseTwo_Rollbacked : BranchStatus.PhaseTwo_RollbackFailed_Retryable;
        } catch (Throwable t) {
            String msg = String.format("rollback %s resource error, resourceId: %s, xid: %s.", getBranchType(), resourceId, xid);
            LOGGER.error(msg, t);
            return BranchStatus.PhaseTwo_RollbackFailed_Retryable;
        }
    }

    /**
     * get phase two commit method's args
     * @param tccResource tccResource
     * @param businessActionContext businessActionContext
     * @return args
     */
    private Object[] getTwoPhaseCommitArgs(TCCResource tccResource, BusinessActionContext businessActionContext) {
        String[] keys = tccResource.getPhaseTwoCommitKeys();
        Class<?>[] argsCommitClasses = tccResource.getCommitArgsClasses();
        return BusinessActionContextUtil.getTwoPhaseMethodParams(keys, argsCommitClasses, businessActionContext);
    }

    /**
     * get phase two rollback method's args
     * @param resource resource
     * @param businessActionContext businessActionContext
     * @return args
     */
    protected Object[] getTwoPhaseRollbackArgs(Resource resource, BusinessActionContext businessActionContext) {
        String[] keys = ((TCCResource) resource).getPhaseTwoRollbackKeys();
        Class<?>[] argsRollbackClasses = ((TCCResource) resource).getRollbackArgsClasses();
        return BusinessActionContextUtil.getTwoPhaseMethodParams(keys, argsRollbackClasses, businessActionContext);
    }

    protected Object getTargetBean(Resource resource) {
        Object targetTCCBean = ((TCCResource) resource).getTargetBean();
        return targetTCCBean;
    }

    protected Method getRollbackMethod(Resource resource) {
        Method rollbackMethod = ((TCCResource) resource).getRollbackMethod();
        return rollbackMethod;
    }

    protected String resolveActionName(Resource resource) {
        return ((TCCResource) resource).getActionName();
    }

    @Override
    public BranchType getBranchType() {
        return BranchType.TCC;
    }
}
