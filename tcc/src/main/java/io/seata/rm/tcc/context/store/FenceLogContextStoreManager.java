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
package io.seata.rm.tcc.context.store;

import com.alibaba.fastjson.JSON;
import io.seata.common.Constants;
import io.seata.common.loader.LoadLevel;
import io.seata.rm.tcc.TCCFenceHandler;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.store.TCCFenceStore;
import io.seata.rm.tcc.store.db.TCCFenceStoreDataBaseDAO;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Collections;

/**
 * store actionContext in tcc_fence_log
 *
 * @author yangwenpeng
 */
@LoadLevel(name = "fence")
public class FenceLogContextStoreManager extends AbstractContextStoreManager {

    private static final TCCFenceStore TCC_FENCE_DAO = TCCFenceStoreDataBaseDAO.getInstance();

    @Override
    protected boolean isSupport(BusinessActionContext context) {
        return Boolean.TRUE.equals(context.getActionContext(Constants.USE_TCC_FENCE));
    }

    @Override
    protected boolean doStore(BusinessActionContext context) {
        DataSource dataSource = TCCFenceHandler.getDataSource();
        Connection connection = DataSourceUtils.getConnection(dataSource);
        return TCC_FENCE_DAO.updateApplicationData(connection, context.getXid(), context.getBranchId()
                , JSON.toJSONString(Collections.singletonMap(Constants.TCC_ACTION_CONTEXT, context.getActionContext())));
    }
}
