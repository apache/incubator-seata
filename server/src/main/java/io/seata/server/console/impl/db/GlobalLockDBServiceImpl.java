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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import io.seata.common.exception.StoreException;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.StringUtils;
import io.seata.core.console.param.GlobalLockParam;
import io.seata.core.console.result.PageResult;
import io.seata.core.console.vo.GlobalLockVO;
import io.seata.core.store.db.DataSourceProvider;
import io.seata.core.store.db.sql.lock.LockStoreSqlFactory;
import io.seata.server.console.service.GlobalLockService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;


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
        PageUtil.checkParam(param.getPageNum(), param.getPageSize());

        StringBuilder whereConditionBuilder = new StringBuilder("");
        if (StringUtils.isNotBlank(param.getXid())) {
            whereConditionBuilder.append(" and xid = ").append(param.getXid());
        }
        if (StringUtils.isNotBlank(param.getTableName())) {
            whereConditionBuilder.append(" and table_name = ").append(param.getTableName());
        }
        if (StringUtils.isNotBlank(param.getTransactionId())) {
            whereConditionBuilder.append(" and transaction_id = ").append(param.getTransactionId());
        }
        if (StringUtils.isNotBlank(param.getBranchId())) {
            whereConditionBuilder.append(" and branch_id = ").append(param.getBranchId());
        }
        if (param.getTimeStart() != null) {
            whereConditionBuilder.append(" and gmt_create >= ").append(param.getTimeStart());
        }
        if (param.getTimeEnd() != null) {
            whereConditionBuilder.append(" and gmt_create <= ").append(param.getTimeEnd());
        }
        String whereCondition = whereConditionBuilder.toString();
        whereCondition = whereCondition.replaceFirst("and", "where");

        String sourceSql = LockStoreSqlFactory.getLogStoreSql(dbType).getAllLockSql(lockTable, whereCondition);
        String queryLockSql = PageUtil.pageSql(sourceSql, dbType, param.getPageNum(), param.getPageSize());
        String lockCountSql = PageUtil.countSql(sourceSql, dbType);

        List<GlobalLockVO> list = new ArrayList<>();
        int count = 0;

        DataSource dataSource = EnhancedServiceLoader.load(DataSourceProvider.class, dbDataSource).provide();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(queryLockSql);
             ResultSet resultSet = ps.executeQuery();
             PreparedStatement countPs = conn.prepareStatement(lockCountSql);
             ResultSet countResultSet = countPs.executeQuery()) {
            while (resultSet.next()) {
                list.add(GlobalLockVO.convert(resultSet));
            }
            if (countResultSet.next()) {
                count = countResultSet.getInt(1);
            }
        } catch (SQLException e) {
            throw new StoreException(e);
        }
        return PageResult.success(list, count, (count / param.getPageSize()) + 1, param.getPageNum(), param.getPageSize());
    }

}
