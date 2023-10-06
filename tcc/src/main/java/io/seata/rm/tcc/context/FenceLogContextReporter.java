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
import io.seata.integration.tx.api.fence.store.CommonFenceStore;
import io.seata.integration.tx.api.fence.store.db.CommonFenceStoreDataBaseDAO;
import io.seata.integration.tx.api.util.JsonUtil;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.context.ContextReporter;
import java.sql.Connection;
import java.util.Collections;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DataSourceUtils;

/**
 * store actionContext in tcc_fence_log
 *
 * @author yangwenpeng
 */
@LoadLevel(name = "fence")
public class FenceLogContextReporter implements ContextReporter {

    private static final CommonFenceStore COMMON_FENCE_DAO = CommonFenceStoreDataBaseDAO.getInstance();

    private static DataSource dataSource;

    @Override
    public boolean report(BusinessActionContext context) {
        // save context to fenceLog
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
    public boolean isSupport(BusinessActionContext context) {
        return Boolean.TRUE.equals(context.getActionContext(Constants.USE_COMMON_FENCE));
    }

    public static DataSource getDataSource() {
        return FenceLogContextReporter.dataSource;
    }

    public static void setDataSource(DataSource dataSource) {
        FenceLogContextReporter.dataSource = dataSource;
    }
}