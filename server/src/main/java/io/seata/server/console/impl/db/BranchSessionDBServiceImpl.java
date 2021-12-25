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
import io.seata.core.store.db.DataSourceProvider;
import io.seata.core.store.db.sql.log.LogStoreSqlsFactory;
import io.seata.core.console.vo.BranchSessionVO;
import io.seata.core.console.result.PageResult;
import io.seata.server.console.service.BranchSessionService;
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
 * Branch Session DataBase ServiceImpl
 *
 * @author: zhongxiang.wang
 */
@Component
@org.springframework.context.annotation.Configuration
@ConditionalOnExpression("#{'db'.equals('${sessionMode}')}")
public class BranchSessionDBServiceImpl implements BranchSessionService {

    @Value("#{environment.getProperty('seata.store.db.branch-table')}")
    private String branchTable;
    @Value("#{environment.getProperty('seata.store.db.db-type')}")
    private String dbType;
    @Value("#{environment.getProperty('seata.store.db.datasource')}")
    private String dbDataSource;

    @Override
    public PageResult<BranchSessionVO> queryByXid(String xid) {
        List<BranchSessionVO> list = new ArrayList<>();
        DataSource dataSource = EnhancedServiceLoader.load(DataSourceProvider.class, dbDataSource).provide();
        String queryAllBranchSessionSQL = LogStoreSqlsFactory.getLogStoreSqls(dbType).getAllBranchSessionSQL(branchTable, xid);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(queryAllBranchSessionSQL);
             ResultSet resultSet = ps.executeQuery()) {
            while (resultSet.next()) {
                list.add(BranchSessionVO.convert(resultSet));
            }
        } catch (SQLException e) {
            throw new StoreException(e);
        }
        return PageResult.success(list, list.size(), 0, 0, 0);
    }

}
