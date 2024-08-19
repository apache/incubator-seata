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
import org.apache.seata.common.exception.FrameworkErrorCode;
import org.apache.seata.common.exception.SkipCallbackWrapperException;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.model.TccLocalTxActive;
import org.apache.seata.integration.tx.api.fence.constant.CommonFenceConstant;
import org.apache.seata.integration.tx.api.fence.exception.CommonFenceException;
import org.apache.seata.rm.fence.SpringFenceHandler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceUtils;

import java.sql.Connection;

public class TccFencePrepareInterceptor extends SpringFenceHandler implements MethodInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(TccFencePrepareInterceptor.class);

    @Override
    public Object invoke(@NotNull MethodInvocation invocation) throws Throwable {
        TccLocalTxActive tccLocalTxActive = RootContext.getTccLocalTxActive();
        if (tccLocalTxActive == null || TccLocalTxActive.UN_ACTIVE == tccLocalTxActive) {
            // The tcc transaction is not activated on the service side.
            return invocation.proceed();
        }

        // The tcc transaction is activated on the service side.
        // Do not wrap business exceptions to avoid invalidation of transactional annotations on the business side
        String xid = null, resourceId = null;
        Long branchId = null;
        try {
            xid = RootContext.getXID();
            branchId = Long.valueOf(RootContext.getBranchId());
            resourceId = RootContext.getResourceId();
            Connection conn = DataSourceUtils.getConnection(dataSource);
            boolean result = insertCommonFenceLog(conn, xid, branchId, resourceId, CommonFenceConstant.STATUS_TRIED);
            LOGGER.info("Common fence prepare result: {}. xid: {}, branchId: {}, resourceId: {}", result, xid, branchId, resourceId);
            if (result) {
                return invocation.proceed();
            } else {
                throw new CommonFenceException(String.format("Insert common fence record error, prepare fence failed. xid= %s, branchId= %s, resourceId= %s", xid, branchId, resourceId),
                        FrameworkErrorCode.InsertRecordError);
            }
        } catch (CommonFenceException e) {
            if (e.getErrcode() == FrameworkErrorCode.DuplicateKeyException) {
                LOGGER.error("Branch transaction has already rollbacked before,prepare fence failed. xid= {},branchId = {},resourceId = {}", xid, branchId, resourceId);
                addToLogCleanQueue(xid, branchId);
            }
            throw new SkipCallbackWrapperException(e);
        }
    }
}
