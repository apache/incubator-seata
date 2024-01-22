/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.seata.server.console.impl.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import io.seata.common.ConfigurationKeys;
import io.seata.common.Constants;
import io.seata.common.exception.StoreException;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.DateUtil;
import io.seata.common.util.IOUtil;
import io.seata.common.util.PageUtil;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.console.result.PageResult;
import io.seata.core.store.db.DataSourceProvider;
import io.seata.core.store.db.sql.lock.LockStoreSqlFactory;
import io.seata.server.console.param.GlobalLockParam;
import io.seata.server.console.service.GlobalLockService;
import io.seata.server.console.vo.GlobalLockVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import static io.seata.common.DefaultValues.DEFAULT_LOCK_DB_TABLE;


/**
 * Global Lock DB ServiceImpl
 *
 */
@Component
@org.springframework.context.annotation.Configuration
@ConditionalOnExpression("#{'db'.equals('${lockMode}')}")
public class GlobalLockDBServiceImpl implements GlobalLockService {

    private String lockTable;

    private String dbType;

    private DataSource dataSource;

    public GlobalLockDBServiceImpl() {
        Configuration configuration = ConfigurationFactory.getInstance();
        lockTable = configuration.getConfig(ConfigurationKeys.LOCK_DB_TABLE, DEFAULT_LOCK_DB_TABLE);
        dbType = configuration.getConfig(ConfigurationKeys.STORE_DB_TYPE);
        if (StringUtils.isBlank(dbType)) {
            throw new IllegalArgumentException(ConfigurationKeys.STORE_DB_TYPE + " should not be blank");
        }
        String dbDataSource = configuration.getConfig(ConfigurationKeys.STORE_DB_DATASOURCE_TYPE);
        if (StringUtils.isBlank(dbDataSource)) {
            throw new IllegalArgumentException(ConfigurationKeys.STORE_DB_DATASOURCE_TYPE + " should not be blank");
        }
        dataSource = EnhancedServiceLoader.load(DataSourceProvider.class, dbDataSource).provide();
    }

    @Override
    public PageResult<GlobalLockVO> query(GlobalLockParam param) {
        PageUtil.checkParam(param.getPageNum(), param.getPageSize());

        List<Object> sqlParamList = new ArrayList<>();
        String whereCondition = this.getWhereConditionByParam(param, sqlParamList);

        String sourceSql = LockStoreSqlFactory.getLogStoreSql(dbType).getAllLockSql(lockTable, whereCondition);
        String queryLockSql = PageUtil.pageSql(sourceSql, dbType, param.getPageNum(), param.getPageSize());
        String lockCountSql = PageUtil.countSql(sourceSql, dbType);

        List<GlobalLockVO> list = new ArrayList<>();
        int count = 0;

        Connection conn = null;
        PreparedStatement ps = null;
        PreparedStatement countPs = null;
        ResultSet rs = null;
        ResultSet countRs = null;
        try {
            conn = dataSource.getConnection();
            ps = conn.prepareStatement(queryLockSql);
            countPs = conn.prepareStatement(lockCountSql);
            PageUtil.setObject(ps, sqlParamList);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(GlobalLockVO.convert(rs));
            }
            PageUtil.setObject(countPs, sqlParamList);
            countRs = countPs.executeQuery();
            if (countRs.next()) {
                count = countRs.getInt(1);
            }
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(rs, countRs, ps, countPs, conn);
        }
        return PageResult.success(list, count, param.getPageNum(), param.getPageSize());
    }

    private String getWhereConditionByParam(GlobalLockParam param, List<Object> sqlParamList) {
        StringBuilder whereConditionBuilder = new StringBuilder();
        if (StringUtils.isNotBlank(param.getXid())) {
            whereConditionBuilder.append(" and xid = ? ");
            sqlParamList.add(param.getXid());
        }
        if (StringUtils.isNotBlank(param.getTableName())) {
            whereConditionBuilder.append(" and table_name = ? ");
            sqlParamList.add(param.getTableName());
        }
        if (StringUtils.isNotBlank(param.getTransactionId())) {
            whereConditionBuilder.append(" and transaction_id = ? ");
            sqlParamList.add(param.getTransactionId());
        }
        if (StringUtils.isNotBlank(param.getBranchId())) {
            whereConditionBuilder.append(" and branch_id = ? ");
            sqlParamList.add(param.getBranchId());
        }
        if (param.getTimeStart() != null) {
            whereConditionBuilder.append(" and FROM_UNIXTIME(begin_time/1000,'%Y-%m-%d %H:%i:%s') >= ? ");
            sqlParamList.add(DateUtil.formatDate(new Date(param.getTimeStart()), Constants.DEFAULT_DATE_FORMAT));
        }
        if (param.getTimeEnd() != null) {
            whereConditionBuilder.append(" and FROM_UNIXTIME(begin_time/1000,'%Y-%m-%d %H:%i:%s') <= ? ");
            sqlParamList.add(DateUtil.formatDate(new Date(param.getTimeEnd()), Constants.DEFAULT_DATE_FORMAT));
        }
        String whereCondition = whereConditionBuilder.toString();
        return whereCondition.replaceFirst("and", "where");
    }

}
