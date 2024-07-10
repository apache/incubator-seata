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
import java.util.List;

import javax.sql.DataSource;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.util.IOUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.common.result.PageResult;
import org.apache.seata.core.store.db.DataSourceProvider;
import org.apache.seata.core.store.db.sql.log.LogStoreSqlsFactory;
import org.apache.seata.server.console.service.BranchSessionService;
import org.apache.seata.server.console.vo.BranchSessionVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import static org.apache.seata.common.DefaultValues.DEFAULT_STORE_DB_BRANCH_TABLE;

/**
 * Branch Session DataBase ServiceImpl
 *
 */
@Component
@org.springframework.context.annotation.Configuration
@ConditionalOnExpression("#{'db'.equals('${sessionMode}')}")
public class BranchSessionDBServiceImpl implements BranchSessionService {

    private String branchTable;

    private String dbType;

    private DataSource dataSource;

    public BranchSessionDBServiceImpl() {
        Configuration configuration = ConfigurationFactory.getInstance();
        branchTable = configuration.getConfig(ConfigurationKeys.STORE_DB_BRANCH_TABLE, DEFAULT_STORE_DB_BRANCH_TABLE);
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
    public PageResult<BranchSessionVO> queryByXid(String xid) {
        if (StringUtils.isBlank(xid)) {
            throw new IllegalArgumentException("xid should not be blank");
        }

        String whereCondition = " where xid = ? ";
        String branchSessionSQL = LogStoreSqlsFactory.getLogStoreSqls(dbType).getAllBranchSessionSQL(branchTable, whereCondition);

        List<BranchSessionVO> list = new ArrayList<>();

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getConnection();
            ps = conn.prepareStatement(branchSessionSQL);
            ps.setObject(1, xid);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(BranchSessionVO.convert(rs));
            }
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(rs, ps, conn);
        }
        return PageResult.success(list, list.size(), 0, 0, 0);
    }

}
