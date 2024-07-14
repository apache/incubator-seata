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
package org.apache.seata.server.console.impl.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.util.IOUtil;
import org.apache.seata.common.util.PageUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.common.result.PageResult;
import org.apache.seata.core.store.db.DataSourceProvider;
import org.apache.seata.core.store.db.sql.log.LogStoreSqlsFactory;
import org.apache.seata.server.console.param.GlobalSessionParam;
import org.apache.seata.server.console.service.BranchSessionService;
import org.apache.seata.server.console.service.GlobalSessionService;
import org.apache.seata.server.console.vo.BranchSessionVO;
import org.apache.seata.server.console.vo.GlobalSessionVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import static org.apache.seata.common.DefaultValues.DEFAULT_STORE_DB_GLOBAL_TABLE;

/**
 * Global Session DataBase ServiceImpl
 *
 */
@Component
@org.springframework.context.annotation.Configuration
@ConditionalOnExpression("#{'db'.equals('${sessionMode}')}")
public class GlobalSessionDBServiceImpl implements GlobalSessionService {

    private String globalTable;

    private String dbType;

    private DataSource dataSource;

    @Resource(type = BranchSessionService.class)
    private BranchSessionService branchSessionService;

    public GlobalSessionDBServiceImpl() {
        Configuration configuration = ConfigurationFactory.getInstance();
        globalTable = configuration.getConfig(ConfigurationKeys.STORE_DB_GLOBAL_TABLE, DEFAULT_STORE_DB_GLOBAL_TABLE);
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
    public PageResult<GlobalSessionVO> query(GlobalSessionParam param) {
        PageUtil.checkParam(param.getPageNum(), param.getPageSize());

        List<Object> sqlParamList = new ArrayList<>();
        String whereCondition = getWhereConditionByParam(param, sqlParamList);

        String sourceSql = LogStoreSqlsFactory.getLogStoreSqls(dbType).getAllGlobalSessionSql(globalTable, whereCondition);
        String querySessionSql = PageUtil.pageSql(sourceSql, dbType, param.getPageNum(), param.getPageSize());
        String sessionCountSql = PageUtil.countSql(sourceSql, dbType);

        List<GlobalSessionVO> list = new ArrayList<>();
        int count = 0;


        Connection conn = null;
        PreparedStatement ps = null;
        PreparedStatement countPs = null;
        ResultSet rs = null;
        ResultSet countRs = null;
        try {
            conn = dataSource.getConnection();
            ps = conn.prepareStatement(querySessionSql);
            countPs = conn.prepareStatement(sessionCountSql);
            PageUtil.setObject(ps, sqlParamList);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(GlobalSessionVO.convert(rs));
            }

            PageUtil.setObject(countPs, sqlParamList);
            countRs = countPs.executeQuery();
            if (countRs.next()) {
                count = countRs.getInt(1);
            }
            if (param.isWithBranch()) {
                for (GlobalSessionVO globalSessionVO : list) {
                    PageResult<BranchSessionVO> pageResp = branchSessionService.queryByXid(globalSessionVO.getXid());
                    globalSessionVO.setBranchSessionVOs(new HashSet<>(pageResp.getData()));
                }
            }
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(rs, countRs, ps, countPs, conn);
        }
        return PageResult.success(list, count, param.getPageNum(), param.getPageSize());
    }

    private String getWhereConditionByParam(GlobalSessionParam param, List<Object> sqlParamList) {
        StringBuilder whereConditionBuilder = new StringBuilder();
        if (StringUtils.isNotBlank(param.getXid())) {
            whereConditionBuilder.append(" and xid = ? ");
            sqlParamList.add(param.getXid());
        }
        if (StringUtils.isNotBlank(param.getApplicationId())) {
            whereConditionBuilder.append(" and application_id = ? ");
            sqlParamList.add(param.getApplicationId());
        }
        if (param.getStatus() != null) {
            whereConditionBuilder.append(" and status = ? ");
            sqlParamList.add(param.getStatus());
        }
        if (StringUtils.isNotBlank(param.getTransactionName())) {
            whereConditionBuilder.append(" and transaction_name = ? ");
            sqlParamList.add(param.getTransactionName());
        }
        if (param.getTimeStart() != null) {
            whereConditionBuilder.append(PageUtil.getTimeStartSql(this.dbType, "begin_time"));
            sqlParamList.add(param.getTimeStart() / 1000);
        }
        if (param.getTimeEnd() != null) {
            whereConditionBuilder.append(PageUtil.getTimeEndSql(this.dbType, "begin_time"));
            sqlParamList.add(param.getTimeEnd() / 1000);
        }
        String whereCondition = whereConditionBuilder.toString();
        return whereCondition.replaceFirst("and", "where");
    }

}
