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
package io.seata.rm.tcc;

import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.common.Constants;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.exception.SkipCallbackWrapperException;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.StringUtils;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.Resource;
import io.seata.integration.tx.api.fence.DefaultCommonFenceHandler;
import io.seata.integration.tx.api.remoting.TwoPhaseResult;
import io.seata.rm.AbstractResourceManager;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextUtil;
import io.seata.rm.tcc.api.context.ContextStoreManager;

import static io.seata.common.ContextStoreConstant.STORE_TYPE_TC;

/**
 * TCC resource manager
 *
 * @author zhangsen
 * @author Yujianfei
 * @author yangwenpeng fence、context
 */
public class TCCResourceManager extends AbstractResourceManager {

    /**
     * TCC resource cache
     */
    private Map<String, Resource> tccResourceCache = new ConcurrentHashMap<>();

    /**
     * Instantiates a new Tcc resource manager.
     */
    public TCCResourceManager() {
        // not do anything
    }

    /**
     * registry TCC resource
     *
     * @param resource The resource to be managed.
     */
    @Override
    public void registerResource(Resource resource) {
        TCCResource tccResource = (TCCResource)resource;
        tccResourceCache.put(tccResource.getResourceId(), tccResource);
        super.registerResource(tccResource);
    }

    @Override
    public Map<String, Resource> getManagedResources() {
        return tccResourceCache;
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
        TCCResource tccResource = (TCCResource)tccResourceCache.get(resourceId);
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
            BusinessActionContext businessActionContext = getBusinessActionContext(xid, branchId, resourceId, applicationData);

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

    private BusinessActionContext getBusinessActionContext(String xid, long branchId, String resourceId, String applicationData) {
        BusinessActionContext businessActionContext = BusinessActionContextUtil.getBusinessActionContext(xid, branchId, resourceId,
                applicationData);

        // get store manager
        String storeType = businessActionContext.getActionContext(Constants.TCC_ACTION_CONTEXT_STORE_TYPE, String.class);
        // default use TC if the storeType not found
        if (StringUtils.isBlank(storeType)) {
            storeType = STORE_TYPE_TC;
        }
        ContextStoreManager contextStoreManager = EnhancedServiceLoader.load(ContextStoreManager.class, storeType);
        //use the context that in TC to search from storeManager
        businessActionContext = contextStoreManager.searchContext(businessActionContext);
        return businessActionContext;
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
        TCCResource tccResource = (TCCResource)tccResourceCache.get(resourceId);
        if (tccResource == null) {
            throw new ShouldNeverHappenException(String.format("TCC resource is not exist, resourceId: %s", resourceId));
        }
        Object targetTCCBean = tccResource.getTargetBean();
        Method rollbackMethod = tccResource.getRollbackMethod();
        if (targetTCCBean == null || rollbackMethod == null) {
            throw new ShouldNeverHappenException(String.format("TCC resource is not available, resourceId: %s", resourceId));
        }
        try {
            //BusinessActionContext
            BusinessActionContext businessActionContext = getBusinessActionContext(xid, branchId, resourceId, applicationData);
            Object[] args = this.getTwoPhaseRollbackArgs(tccResource, businessActionContext);
            Object ret;
            boolean result;
            // add idempotent and anti hanging
            if (Boolean.TRUE.equals(businessActionContext.getActionContext(Constants.USE_COMMON_FENCE))) {
                try {
                    result = DefaultCommonFenceHandler.get().rollbackFence(rollbackMethod, targetTCCBean, xid, branchId,
                            args, tccResource.getActionName());
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
            LOGGER.info("TCC resource rollback result : {}, xid: {}, branchId: {}, resourceId: {}", result, xid, branchId, resourceId);
            return result ? BranchStatus.PhaseTwo_Rollbacked : BranchStatus.PhaseTwo_RollbackFailed_Retryable;
        } catch (Throwable t) {
            String msg = String.format("rollback TCC resource error, resourceId: %s, xid: %s.", resourceId, xid);
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
     * @param tccResource tccResource
     * @param businessActionContext businessActionContext
     * @return args
     */
    private Object[] getTwoPhaseRollbackArgs(TCCResource tccResource, BusinessActionContext businessActionContext) {
        String[] keys = tccResource.getPhaseTwoRollbackKeys();
        Class<?>[] argsRollbackClasses = tccResource.getRollbackArgsClasses();
        return BusinessActionContextUtil.getTwoPhaseMethodParams(keys, argsRollbackClasses, businessActionContext);
    }

    @Override
    public BranchType getBranchType() {
        return BranchType.TCC;
    }
}