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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.fastjson.JSON;

import io.seata.common.Constants;
import io.seata.common.exception.FrameworkException;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.StringUtils;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.Resource;
import io.seata.rm.AbstractResourceManager;
import io.seata.rm.tcc.api.BusinessActionContext;

/**
 * TCC resource manager
 *
 * @author zhangsen
 */
public class TCCResourceManager extends AbstractResourceManager {

    /**
     * TCC resource cache
     */
    private Map<String, Resource> tccResourceCache = new ConcurrentHashMap<String, Resource>();

    /**
     * Instantiates a new Tcc resource manager.
     */
    public TCCResourceManager() {
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
        synchronized (RESOURCE_LOCK) {
            RESOURCE_LOCK.notifyAll();
        }
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
     * @return
     * @throws TransactionException
     */
    @Override
    public BranchStatus branchCommit(BranchType branchType, String xid, long branchId, String resourceId,
                                     String applicationData) throws TransactionException {
        TCCResource tccResource = (TCCResource)tccResourceCache.get(resourceId);
        if (tccResource == null) {
            throw new ShouldNeverHappenException("TCC resource is not exist, resourceId:" + resourceId);
        }
        Object targetTCCBean = tccResource.getTargetBean();
        Method commitMethod = tccResource.getCommitMethod();
        if (targetTCCBean == null || commitMethod == null) {
            throw new ShouldNeverHappenException("TCC resource is not available, resourceId:" + resourceId);
        }
        try {
            boolean result = false;
            //BusinessActionContext
            BusinessActionContext businessActionContext = getBusinessActionContext(xid, branchId, resourceId,
                applicationData);
            Object ret = commitMethod.invoke(targetTCCBean, businessActionContext);
            LOGGER.info(
                "TCC resource commit result :" + ret + ", xid:" + xid + ", branchId:" + branchId + ", resourceId:"
                    + resourceId);
            if (ret != null) {
                if (ret instanceof TwoPhaseResult) {
                    result = ((TwoPhaseResult)ret).isSuccess();
                } else {
                    result = (boolean)ret;
                }
            }
            return result ? BranchStatus.PhaseTwo_Committed : BranchStatus.PhaseTwo_CommitFailed_Retryable;
        } catch (Throwable t) {
            String msg = String.format("commit TCC resource error, resourceId: %s, xid: %s.", resourceId, xid);
            LOGGER.error(msg, t);
            throw new FrameworkException(t, msg);
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
     * @return
     * @throws TransactionException
     */
    @Override
    public BranchStatus branchRollback(BranchType branchType, String xid, long branchId, String resourceId,
                                       String applicationData) throws TransactionException {
        TCCResource tccResource = (TCCResource)tccResourceCache.get(resourceId);
        if (tccResource == null) {
            throw new ShouldNeverHappenException("TCC resource is not exist, resourceId:" + resourceId);
        }
        Object targetTCCBean = tccResource.getTargetBean();
        Method rollbackMethod = tccResource.getRollbackMethod();
        if (targetTCCBean == null || rollbackMethod == null) {
            throw new ShouldNeverHappenException("TCC resource is not available, resourceId:" + resourceId);
        }
        try {
            boolean result = false;
            //BusinessActionContext
            BusinessActionContext businessActionContext = getBusinessActionContext(xid, branchId, resourceId,
                applicationData);
            Object ret = rollbackMethod.invoke(targetTCCBean, businessActionContext);
            LOGGER.info(
                "TCC resource rollback result :" + ret + ", xid:" + xid + ", branchId:" + branchId + ", resourceId:"
                    + resourceId);
            if (ret != null) {
                if (ret instanceof TwoPhaseResult) {
                    result = ((TwoPhaseResult)ret).isSuccess();
                } else {
                    result = (boolean)ret;
                }
            }
            return result ? BranchStatus.PhaseTwo_Rollbacked : BranchStatus.PhaseTwo_RollbackFailed_Retryable;
        } catch (Throwable t) {
            String msg = String.format("rollback TCC resource error, resourceId: %s, xid: %s.", resourceId, xid);
            LOGGER.error(msg, t);
            throw new FrameworkException(t, msg);
        }
    }

    /**
     * transfer tcc applicationData to BusinessActionContext
     *
     * @param xid             the xid
     * @param branchId        the branch id
     * @param resourceId      the resource id
     * @param applicationData the application data
     * @return business action context
     */
    protected BusinessActionContext getBusinessActionContext(String xid, long branchId, String resourceId,
                                                             String applicationData) {
        //transfer tcc applicationData to Context
        Map tccContext = StringUtils.isBlank(applicationData) ? new HashMap() : (Map)JSON.parse(applicationData);
        Map actionContextMap = (Map)tccContext.get(Constants.TCC_ACTION_CONTEXT);
        BusinessActionContext businessActionContext = new BusinessActionContext(
            xid, String.valueOf(branchId), actionContextMap);
        businessActionContext.setActionName(resourceId);
        return businessActionContext;
    }

    @Override
    public BranchType getBranchType() {
        return BranchType.TCC;
    }
}
