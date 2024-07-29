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
package org.apache.seata.rm.fence.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.seata.common.exception.ExceptionUtil;
import org.apache.seata.common.exception.FrameworkErrorCode;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.model.TccLocalTxActive;
import org.apache.seata.integration.tx.api.fence.constant.CommonFenceConstant;
import org.apache.seata.integration.tx.api.fence.exception.CommonFenceException;
import org.apache.seata.integration.tx.api.fence.store.CommonFenceDO;
import org.apache.seata.integration.tx.api.remoting.TwoPhaseResult;
import org.apache.seata.rm.fence.SpringFenceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceUtils;

import java.sql.Connection;

public class TccFenceCommitInterceptor extends SpringFenceHandler implements MethodInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(TccFenceCommitInterceptor.class);

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        TccLocalTxActive tccLocalTxActive = RootContext.getTccLocalTxActive();
        if (tccLocalTxActive == null || TccLocalTxActive.UN_ACTIVE == tccLocalTxActive) {
            // The tcc transaction is not activated on the service side.
            return invocation.proceed();
        }
        // The tcc transaction is activated on the service side.
        // Do not wrap business exceptions to avoid invalidation of transactional annotations on the business side
        String xid = RootContext.getXID();
        Long branchId = Long.valueOf(RootContext.getBranchId());
        String resourceId = RootContext.getResourceId();
        Connection conn = DataSourceUtils.getConnection(dataSource);
        CommonFenceDO commonFenceDO = COMMON_FENCE_DAO.queryCommonFenceDO(conn, xid, branchId);
        if (commonFenceDO == null) {
            throw new CommonFenceException(String.format("Common fence record not exists, commit fence method failed. xid= %s, branchId= %s, resourceId= %s", xid, branchId, resourceId),
                    FrameworkErrorCode.RecordNotExists);
        }
        if (CommonFenceConstant.STATUS_COMMITTED == commonFenceDO.getStatus()) {
            LOGGER.info("Branch transaction has already committed before. idempotency rejected. xid: {}, branchId: {}, resourceId: {}, status: {}", xid, branchId, resourceId, commonFenceDO.getStatus());
            boolean result = true;
            RootContext.bindTccCommitResult(result);
            return result;
        }
        if (CommonFenceConstant.STATUS_ROLLBACKED == commonFenceDO.getStatus() || CommonFenceConstant.STATUS_SUSPENDED == commonFenceDO.getStatus()) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Branch transaction status is unexpected. xid: {}, branchId: {}, resourceId: {}, status: {}", xid, branchId, resourceId, commonFenceDO.getStatus());
            }
            boolean result = false;
            RootContext.bindTccCommitResult(result);
            return result;
        }
        boolean result = updateStatusAndInvokeTargetMethodForCommit(conn, invocation, xid, branchId, CommonFenceConstant.STATUS_COMMITTED);
        LOGGER.info("Common fence commit result: {}. xid: {}, branchId: {}, resourceId: {}", result, xid, branchId, resourceId);
        RootContext.bindTccCommitResult(result);
        return result;
    }


    /**
     * Update Common Fence status and invoke target method for commit
     *
     * @param conn       connection
     * @param invocation invocation
     * @param xid        the global transaction id
     * @param branchId   the branch transaction id
     * @param status     the common fence status
     * @return the boolean
     */
    protected static boolean updateStatusAndInvokeTargetMethodForCommit(Connection conn, MethodInvocation invocation,
                                                                        String xid, Long branchId, int status) throws Throwable {
        boolean result = COMMON_FENCE_DAO.updateCommonFenceDO(conn, xid, branchId, status, CommonFenceConstant.STATUS_TRIED);
        if (result) {
            try {
                // invoke two phase method
                Object ret = invocation.proceed();
                if (null != ret) {
                    if (ret instanceof TwoPhaseResult) {
                        result = ((TwoPhaseResult) ret).isSuccess();
                    } else {
                        result = (boolean) ret;
                    }
                    // If the business execution result is false, the transaction will be rolled back
                    if (!result) {
                        // Trigger rollback
                        throw new RuntimeException("the tcc fence tx failed to commit, please try again");
                    }
                }
            } catch (Exception e) {
                throw ExceptionUtil.unwrap(e);
            }
        }
        return result;
    }
}
