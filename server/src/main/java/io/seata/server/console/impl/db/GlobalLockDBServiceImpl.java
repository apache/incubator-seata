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
import io.seata.common.exception.StoreException;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.IOUtil;
import io.seata.core.constants.ServerTableColumnsName;
import io.seata.core.store.db.DataSourceProvider;
import io.seata.core.store.db.sql.lock.LockStoreSqlFactory;
import io.seata.core.store.db.vo.GlobalLockVO;
import io.seata.server.console.service.GlobalLockService;
import io.seata.server.console.result.PageResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


/**
 * Global Lock DB ServiceImpl
 *
 * @author: zhongxiang.wang
 */
@Component
@org.springframework.context.annotation.Configuration
@ConditionalOnExpression("'${seata.store.lock.mode}'.equals('db')")
public class GlobalLockDBServiceImpl implements GlobalLockService {

    @Value("${seata.store.db.lock-table}")
    protected String lockTable;
    @Value("${seata.store.db.db-type}")
    protected String dbType;
    @Value("${seata.store.db.datasource}")
    protected String dbDataSource;

    @Override
    public PageResult<GlobalLockVO> queryByTable(String tableName) {
        List<GlobalLockVO> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet resultSet = null;
        DataSource dataSource = EnhancedServiceLoader.load(DataSourceProvider.class, dbDataSource).provide();
        String queryAllLockSQL = LockStoreSqlFactory.getLogStoreSql(dbType).getAllLockSQL(lockTable, tableName);
        try {
            conn = dataSource.getConnection();
            ps = conn.prepareStatement(queryAllLockSQL);
            resultSet = ps.executeQuery();
            while (resultSet.next()) {
                list.add(this.convertGlobalLockVO(resultSet));
            }
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(ps, conn, resultSet);
        }
        return PageResult.success(list, list.size(), 0, 0, 0);
    }


    @Override
    public PageResult<GlobalLockVO> queryByXid(String xid) {
        throw new NotSupportYetException();
    }


    private GlobalLockVO convertGlobalLockVO(ResultSet rs) throws SQLException {
        GlobalLockVO globalLockVO = new GlobalLockVO();
        globalLockVO.setRowKey(rs.getString(ServerTableColumnsName.LOCK_TABLE_ROW_KEY));
        globalLockVO.setXid(rs.getString(ServerTableColumnsName.LOCK_TABLE_XID));
        globalLockVO.setTransactionId(rs.getLong(ServerTableColumnsName.LOCK_TABLE_TRANSACTION_ID));
        globalLockVO.setBranchId(rs.getLong(ServerTableColumnsName.LOCK_TABLE_BRANCH_ID));
        globalLockVO.setResourceId(rs.getString(ServerTableColumnsName.LOCK_TABLE_RESOURCE_ID));
        globalLockVO.setTableName(rs.getString(ServerTableColumnsName.LOCK_TABLE_TABLE_NAME));
        globalLockVO.setPk(rs.getString(ServerTableColumnsName.LOCK_TABLE_PK));
        globalLockVO.setGmtCreate(rs.getTimestamp(ServerTableColumnsName.LOCK_TABLE_GMT_CREATE));
        globalLockVO.setGmtModified(rs.getTimestamp(ServerTableColumnsName.LOCK_TABLE_GMT_MODIFIED));
        return globalLockVO;
    }
}
