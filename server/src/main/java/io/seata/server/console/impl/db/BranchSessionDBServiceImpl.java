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

import io.seata.common.ConfigurationKeys;
import io.seata.common.exception.StoreException;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.IOUtil;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.console.result.PageResult;
import io.seata.core.store.db.DataSourceProvider;
import io.seata.core.store.db.sql.log.LogStoreSqlsFactory;
import io.seata.server.console.service.BranchSessionService;
import io.seata.server.console.vo.BranchSessionVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import static io.seata.common.DefaultValues.DEFAULT_STORE_DB_BRANCH_TABLE;

/**
 * Branch Session DataBase ServiceImpl
 *
 * @author zhongxiang.wang
 * @author lvekee 734843455@qq.com
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
        ResultSet rs = null;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(branchSessionSQL)) {
            ps.setObject(1, xid);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(BranchSessionVO.convert(rs));
            }
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(rs);
        }
        return PageResult.success(list, list.size(), 0, 0, 0);
    }

}
