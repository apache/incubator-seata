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
package io.seata.rm.tcc.context;

import io.seata.common.Constants;
import io.seata.common.exception.FrameworkException;
import io.seata.common.loader.LoadLevel;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.integration.tx.api.util.JsonUtil;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.tcc.api.BusinessActionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

/**
 * report the context to TC
 *
 * @author yangwenpeng
 */
@LoadLevel(name = "tc")
public class ReportContextStoreManager extends AbstractContextStoreManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportContextStoreManager.class);

    @Override
    protected boolean doStore(BusinessActionContext context) {
        try {
            // branch report
            DefaultResourceManager.get().branchReport(
                    context.getBranchType(),
                    context.getXid(),
                    context.getBranchId(),
                    BranchStatus.Registered,
                    JsonUtil.toJSONString(Collections.singletonMap(Constants.TX_ACTION_CONTEXT, context.getActionContext()))
            );
            return true;
        } catch (TransactionException e) {
            String msg = String.format("TCC branch update error, xid: %s", context.getXid());
            LOGGER.error("{}, error: {}", msg, e.getMessage());
            throw new FrameworkException(e, msg);
        }
    }

    @Override
    protected BusinessActionContext doSearch(BusinessActionContext context) {
        return context;
    }
}