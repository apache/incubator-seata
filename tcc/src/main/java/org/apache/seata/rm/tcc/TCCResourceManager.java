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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.seata.common.Constants;
import org.apache.seata.common.exception.ExceptionUtil;
import org.apache.seata.common.exception.RepeatRegistrationException;
import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.common.exception.SkipCallbackWrapperException;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.Resource;
import org.apache.seata.integration.tx.api.fence.DefaultCommonFenceHandler;
import org.apache.seata.integration.tx.api.fence.hook.TccHook;
import org.apache.seata.integration.tx.api.fence.hook.TccHookManager;
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
        String resourceId = resource.getResourceId();
        TCCResource newResource = (TCCResource) resource;
        TCCResource oldResource = getTCCResource(resourceId);

        if (oldResource != null) {
            Object newResourceBean = newResource.getTargetBean();
            Object oldResourceBean = oldResource.getTargetBean();
            if (newResourceBean != oldResourceBean) {
                throw new RepeatRegistrationException(String.format("Same TCC resource name <%s> between method1 <%s> of class1 <%s> and method2 <%s> of class2 <%s>, should be unique",
                        resourceId,
                        newResource.getPrepareMethod().getName(),
                        newResourceBean.getClass().getName(),
                        oldResource.getPrepareMethod().getName(),
                        oldResourceBean.getClass().getName()));
            }
        }

        tccResourceCache.put(resourceId, newResource);
        super.registerResource(newResource);
    }

    public TCCResource getTCCResource(String resourceId) {
        return (TCCResource) tccResourceCache.get(resourceId);
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
        TCCResource tccResource = getTCCResource(resourceId);
        if (tccResource == null) {
            throw new ShouldNeverHappenException(String.format("TCC resource is not exist, resourceId: %s", resourceId));
        }
        Object targetTCCBean = tccResource.getTargetBean();
        Method commitMethod = tccResource.getCommitMethod();
        if (targetTCCBean == null || commitMethod == null) {
            throw new ShouldNeverHappenException(String.format("TCC resource is not available, resourceId: %s", resourceId));
        }
        BusinessActionContext businessActionContext = null;
        try {
            //BusinessActionContext
            businessActionContext = BusinessActionContextUtil.getBusinessActionContext(xid, branchId, resourceId,
                    applicationData);

            Object[] args = this.getTwoPhaseCommitArgs(tccResource, businessActionContext);
            //share actionContext implicitly
            BusinessActionContextUtil.setContext(businessActionContext);
            doBeforeTccCommit(xid, branchId, tccResource.getActionName(), businessActionContext);
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
            LOGGER.error(msg, ExceptionUtil.unwrap(t));
            return BranchStatus.PhaseTwo_CommitFailed_Retryable;
        } finally {
            doAfterTccCommit(xid, branchId, tccResource.getActionName(), businessActionContext);
            // clear the action context
            BusinessActionContextUtil.clear();
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
        TCCResource tccResource = getTCCResource(resourceId);
        if (tccResource == null) {
            throw new ShouldNeverHappenException(String.format("TCC resource is not exist, resourceId: %s", resourceId));
        }
        Object targetTCCBean = tccResource.getTargetBean();
        Method rollbackMethod = tccResource.getRollbackMethod();
        if (targetTCCBean == null || rollbackMethod == null) {
            throw new ShouldNeverHappenException(String.format("TCC resource is not available, resourceId: %s", resourceId));
        }
        BusinessActionContext businessActionContext = null;
        try {
            //BusinessActionContext
            businessActionContext = BusinessActionContextUtil.getBusinessActionContext(xid, branchId, resourceId,
                    applicationData);
            Object[] args = this.getTwoPhaseRollbackArgs(tccResource, businessActionContext);
            //share actionContext implicitly
            BusinessActionContextUtil.setContext(businessActionContext);
            doBeforeTccRollback(xid, branchId, tccResource.getActionName(), businessActionContext);
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
            LOGGER.error(msg, ExceptionUtil.unwrap(t));
            return BranchStatus.PhaseTwo_RollbackFailed_Retryable;
        } finally {
            doAfterTccRollback(xid, branchId, tccResource.getActionName(), businessActionContext);
            // clear the action context
            BusinessActionContextUtil.clear();
        }
    }

    /**
     * to do some business operations before tcc rollback
     * @param xid          the xid
     * @param branchId     the branchId
     * @param actionName   the actionName
     * @param context      the business action context
     */
    private void doBeforeTccRollback(String xid, long branchId, String actionName, BusinessActionContext context) {
        List<TccHook> hooks = TccHookManager.getHooks();
        if (hooks.isEmpty()) {
            return;
        }
        for (TccHook hook : hooks) {
            try {
                hook.beforeTccRollback(xid, branchId, actionName, context);
            } catch (Exception e) {
                LOGGER.error("Failed execute beforeTccRollback in hook {}", e.getMessage(), e);
            }
        }
    }

    /**
     * to do some business operations after tcc rollback
     * @param xid          the xid
     * @param branchId     the branchId
     * @param actionName   the actionName
     * @param context      the business action context
     */
    private void doAfterTccRollback(String xid, long branchId, String actionName, BusinessActionContext context) {
        List<TccHook> hooks = TccHookManager.getHooks();
        if (hooks.isEmpty()) {
            return;
        }
        for (TccHook hook : hooks) {
            try {
                hook.afterTccRollback(xid, branchId, actionName, context);
            } catch (Exception e) {
                LOGGER.error("Failed execute afterTccRollback in hook {}", e.getMessage(), e);
            }
        }
    }

    /**
     * to do some business operations before tcc commit
     * @param xid          the xid
     * @param branchId     the branchId
     * @param actionName   the actionName
     * @param context      the business action context
     */
    private void doBeforeTccCommit(String xid, long branchId, String actionName, BusinessActionContext context) {
        List<TccHook> hooks = TccHookManager.getHooks();
        if (hooks.isEmpty()) {
            return;
        }
        for (TccHook hook : hooks) {
            try {
                hook.beforeTccCommit(xid, branchId, actionName, context);
            } catch (Exception e) {
                LOGGER.error("Failed execute beforeTccCommit in hook {}", e.getMessage(), e);
            }
        }
    }

    /**
     * to do some business operations after tcc commit
     * @param xid          the xid
     * @param branchId     the branchId
     * @param actionName   the actionName
     * @param context      the business action context
     */
    private void doAfterTccCommit(String xid, long branchId, String actionName, BusinessActionContext context) {
        List<TccHook> hooks = TccHookManager.getHooks();
        if (hooks.isEmpty()) {
            return;
        }
        for (TccHook hook : hooks) {
            try {
                hook.afterTccCommit(xid, branchId, actionName, context);
            } catch (Exception e) {
                LOGGER.error("Failed execute afterTccCommit in hook {}", e.getMessage(), e);
            }
        }
    }

    /**
     * get phase two commit method's args
     * @param tccResource tccResource
     * @param businessActionContext businessActionContext
     * @return args
     */
    protected Object[] getTwoPhaseCommitArgs(TCCResource tccResource, BusinessActionContext businessActionContext) {
        String[] keys = tccResource.getPhaseTwoCommitKeys();
        Class<?>[] argsCommitClasses = tccResource.getCommitArgsClasses();
        return getTwoPhaseMethodParams(keys, argsCommitClasses, businessActionContext);
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

    @Override
    public BranchType getBranchType() {
        return BranchType.TCC;
    }
}
