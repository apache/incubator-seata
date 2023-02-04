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
package org.apache.seata.saga.rm;

import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.Resource;
import org.apache.seata.rm.tcc.TCCResourceManager;
import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.rm.tcc.api.BusinessActionContextUtil;

import java.lang.reflect.Method;

/**
 * Saga resource manager
 *
 * @author leezongjie
 * @date 2022/11/26
 */
public class SagaAnnotationResourceManager extends TCCResourceManager {

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
        return BranchStatus.PhaseTwo_Committed;
    }

    protected Object[] getTwoPhaseRollbackArgs(Resource resource, BusinessActionContext businessActionContext) {
        String[] keys = ((SagaAnnotationResource) resource).getPhaseTwoRollbackKeys();
        Class<?>[] argsRollbackClasses = ((SagaAnnotationResource) resource).getRollbackArgsClasses();
        return BusinessActionContextUtil.getTwoPhaseMethodParams(keys, argsRollbackClasses, businessActionContext);
    }

    protected Object getTargetBean(Resource resource) {
        Object targetBean = ((SagaAnnotationResource) resource).getTargetBean();
        return targetBean;
    }

    protected Method getRollbackMethod(Resource resource) {
        Method rollbackMethod = ((SagaAnnotationResource) resource).getRollbackMethod();
        return rollbackMethod;
    }

    protected String resolveActionName(Resource resource) {
        return ((SagaAnnotationResource) resource).getActionName();
    }

    @Override
    public BranchType getBranchType() {
        return BranchType.SAGA_ANNOTATION;
    }
}