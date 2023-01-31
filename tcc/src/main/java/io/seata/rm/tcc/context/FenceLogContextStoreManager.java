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
import io.seata.common.loader.LoadLevel;
import io.seata.common.util.StringUtils;
import io.seata.integration.tx.api.fence.DefaultCommonFenceHandler;
import io.seata.integration.tx.api.fence.store.CommonFenceStore;
import io.seata.integration.tx.api.fence.store.db.CommonFenceStoreDataBaseDAO;
import io.seata.integration.tx.api.util.JsonUtil;
import io.seata.rm.tcc.api.BusinessActionContext;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Collections;
import java.util.Map;

/**
 * store actionContext in tcc_fence_log
 *
 * @author yangwenpeng
 */
@LoadLevel(name = "fence")
public class FenceLogContextStoreManager extends AbstractContextStoreManager {

    private static final CommonFenceStore COMMON_FENCE_DAO = CommonFenceStoreDataBaseDAO.getInstance();

    @Override
    protected boolean doStore(BusinessActionContext context) {

        // save context to fenceLog
        DataSource dataSource = DefaultCommonFenceHandler.get().getDataSource();
        Connection connection = null;
        try {
            connection = DataSourceUtils.getConnection(dataSource);
            return COMMON_FENCE_DAO.updateApplicationData(connection, context.getXid(), context.getBranchId()
                    , JsonUtil.toJSONString(Collections.singletonMap(Constants.TX_ACTION_CONTEXT, context.getActionContext())));
        } finally {
            if (connection != null) {
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
        }
    }

    @Override
    protected BusinessActionContext doSearch(BusinessActionContext context) {
        // search tcc context from fenceLog
        DataSource dataSource = DefaultCommonFenceHandler.get().getDataSource();
        Connection connection = null;
        try {
            connection = DataSourceUtils.getConnection(dataSource);
            String applicationData = COMMON_FENCE_DAO.queryApplicationData(connection, context.getXid(), context.getBranchId());
            if (StringUtils.isNotBlank(applicationData)) {
                Map tccContext = JsonUtil.parseObject(applicationData, Map.class);
                Map actionContextMap = (Map) tccContext.get(Constants.TX_ACTION_CONTEXT);
                context.getActionContext().putAll(actionContextMap);
            }
            return context;
        } finally {
            if (connection != null) {
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
        }
    }

    @Override
    protected boolean isSupport(BusinessActionContext context) {
        return Boolean.TRUE.equals(context.getActionContext(Constants.USE_COMMON_FENCE));
    }
}