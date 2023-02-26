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
package io.seata.rm.tcc.api.context;

import io.seata.common.Constants;
import io.seata.common.ContextStoreConstant;
import io.seata.common.exception.ContextReportException;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * default ContextReporter
 *
 * @author yangwenpeng
 */
public final class DefaultContextReporter implements ContextReporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultContextReporter.class);


    private static final ContextReporter DEFAULT_STORE_MANAGER = EnhancedServiceLoader.load(ContextReporter.class, ContextStoreConstant.STORE_TYPE_TC);

    private DefaultContextReporter() {
    }

    @Override
    public boolean report(BusinessActionContext context) {
        // check if reported
        if (context.getActionContextReported()) {
            throw new ContextReportException("context has bean reported, repeated reporting is not allowed.");
        }
        // check if updated
        if (!Boolean.TRUE.equals(context.getUpdated())) {
            LOGGER.info("Branch report context failed, because context not changed, xid={} branchId={}", context.getXid(), context.getBranchId());
            return false;
        }
        ContextReporter contextReporter = EnhancedServiceLoader.load(ContextReporter.class
            , context.getActionContext(Constants.TCC_ACTION_CONTEXT_REPORT_TYPE, String.class));
        // if not support, save context to TC
        if (!contextReporter.isSupport(context)) {
            return DEFAULT_STORE_MANAGER.report(context);
        }
        if (contextReporter.report(context)) {
            LOGGER.info("Branch report context successfully, xid={} branchId={}", context.getXid(), context.getBranchId());
            // reset to un_updated
            context.setUpdated(null);
            context.setActionContextReported(true);
            return true;
        }
        return false;
    }

    @Override
    public boolean isSupport(BusinessActionContext context) {
        ContextReporter contextReporter = EnhancedServiceLoader.load(ContextReporter.class
            , context.getActionContext(Constants.TCC_ACTION_CONTEXT_REPORT_TYPE, String.class));
        // do store
        return contextReporter.isSupport(context);
    }

    /**
     * Get default context reporter.
     *
     * @return the default context reporter.
     */
    public static DefaultContextReporter get() {
        return SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static DefaultContextReporter INSTANCE = new DefaultContextReporter();
    }
}
