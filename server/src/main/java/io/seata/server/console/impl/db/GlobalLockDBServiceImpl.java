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

import io.seata.common.exception.StoreException;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.core.console.param.GlobalLockParam;
import io.seata.core.store.db.DataSourceProvider;
import io.seata.core.store.db.sql.lock.LockStoreSqlFactory;
import io.seata.core.console.vo.GlobalLockVO;
import io.seata.server.console.service.GlobalLockService;
import io.seata.core.console.result.PageResult;
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
    public PageResult<GlobalLockVO> query(GlobalLockParam param) {
        List<GlobalLockVO> list = new ArrayList<>();
        DataSource dataSource = EnhancedServiceLoader.load(DataSourceProvider.class, dbDataSource).provide();
        //!!! need to check the param to assemble the sql
        //!!! need to note that if all input parameters are empty
        //!!! it is a sample,you need to solve different input parameters
        String queryAllLockSQL = LockStoreSqlFactory.getLogStoreSql(dbType).getAllLockSQL(lockTable, param.getTableName());
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(queryAllLockSQL);
             ResultSet resultSet = ps.executeQuery()) {
            while (resultSet.next()) {
                list.add(GlobalLockVO.convert(resultSet));
            }
        } catch (SQLException e) {
            throw new StoreException(e);
        }
        return PageResult.success(list, list.size(), 0, 0, 0);
    }

}
