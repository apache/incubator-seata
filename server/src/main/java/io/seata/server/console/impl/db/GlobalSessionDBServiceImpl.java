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
package io.seata.server.console.impl.db;

import io.seata.common.exception.NotSupportYetException;
import io.seata.core.constants.ServerTableColumnsName;
import io.seata.core.store.db.vo.GlobalSessionVO;
import io.seata.server.console.result.PageResult;
import io.seata.server.console.result.SingleResult;
import io.seata.server.console.service.GlobalSessionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Global Session DataBase ServiceImpl
 *
 * @author: zhongxiang.wang
 */
@Component
@org.springframework.context.annotation.Configuration
@ConditionalOnExpression("'${seata.store.session.mode}'.equals('db')")
public class GlobalSessionDBServiceImpl implements GlobalSessionService {

    @Value("${seata.store.db.global-table}")
    protected String globalTable;
    @Value("${seata.store.db.db-type}")
    protected String dbType;
    @Value("${seata.store.db.datasource}")
    protected String dbDataSource;

    @Override
    public PageResult<GlobalSessionVO> queryAll(String applicationId, boolean withBranch) {
        throw new NotSupportYetException();
    }

    @Override
    public PageResult<GlobalSessionVO> queryByStatus(String applicationId, Integer status, boolean withBranch) {
        throw new NotSupportYetException();
    }

    @Override
    public SingleResult<GlobalSessionVO> queryByXid(String xid, boolean withBranch) {
        throw new NotSupportYetException();
    }

    private GlobalSessionVO convertGlobalSessionVO(ResultSet rs) throws SQLException {
        GlobalSessionVO globalSessionVO = new GlobalSessionVO();
        globalSessionVO.setXid(rs.getString(ServerTableColumnsName.GLOBAL_TABLE_XID));
        globalSessionVO.setTransactionId(rs.getLong(ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_ID));
        globalSessionVO.setStatus(rs.getInt(ServerTableColumnsName.GLOBAL_TABLE_STATUS));
        globalSessionVO.setApplicationId(rs.getString(ServerTableColumnsName.GLOBAL_TABLE_APPLICATION_ID));
        globalSessionVO.setTransactionServiceGroup(rs.getString(ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_SERVICE_GROUP));
        globalSessionVO.setTransactionName(rs.getString(ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_NAME));
        globalSessionVO.setTimeout(rs.getLong(ServerTableColumnsName.GLOBAL_TABLE_TIMEOUT));
        globalSessionVO.setBeginTime(rs.getLong(ServerTableColumnsName.GLOBAL_TABLE_BEGIN_TIME));
        globalSessionVO.setApplicationData(rs.getString(ServerTableColumnsName.GLOBAL_TABLE_APPLICATION_DATA));
        globalSessionVO.setGmtCreate(rs.getDate(ServerTableColumnsName.GLOBAL_TABLE_GMT_CREATE));
        globalSessionVO.setGmtModified(rs.getDate(ServerTableColumnsName.GLOBAL_TABLE_GMT_MODIFIED));
        return globalSessionVO;
    }
}
