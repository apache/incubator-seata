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
@ConditionalOnExpression("#{'db'.equals('${lockMode}')}")
public class GlobalLockDBServiceImpl implements GlobalLockService {

    @Value("#{environment.getProperty('seata.store.db.lock-table')}")
    private String lockTable;
    @Value("#{environment.getProperty('seata.store.db.db-type')}")
    private String dbType;
    @Value("#{environment.getProperty('seata.store.db.datasource')}")
    private String dbDataSource;

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
                list.add(GlobalLockVO.convert(resultSet));
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


}
