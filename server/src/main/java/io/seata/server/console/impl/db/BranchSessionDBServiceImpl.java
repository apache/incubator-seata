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
import io.seata.common.util.IOUtil;
import io.seata.core.constants.ServerTableColumnsName;
import io.seata.core.store.db.DataSourceProvider;
import io.seata.core.store.db.sql.log.LogStoreSqlsFactory;
import io.seata.core.store.db.vo.BranchSessionVO;
import io.seata.server.console.result.PageResult;
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
@ConditionalOnExpression("'${seata.store.session.mode}'.equals('db')")
public class BranchSessionDBServiceImpl implements BranchSessionService {

    @Value("${seata.store.db.branch-table}")
    protected String branchTable;
    @Value("${seata.store.db.db-type}")
    protected String dbType;
    @Value("${seata.store.db.datasource}")
    protected String dbDataSource;

    @Override
    public PageResult<BranchSessionVO> queryByXid(String xid) {
        List<BranchSessionVO> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet resultSet = null;
        DataSource dataSource = EnhancedServiceLoader.load(DataSourceProvider.class, dbDataSource).provide();
        String queryAllBranchSessionSQL = LogStoreSqlsFactory.getLogStoreSqls(dbType).getAllBranchSessionSQL(branchTable, xid);
        try {
            conn = dataSource.getConnection();
            ps = conn.prepareStatement(queryAllBranchSessionSQL);

            resultSet = ps.executeQuery();
            while (resultSet.next()) {
                list.add(this.convertBranchSessionVO(resultSet));
            }
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(ps, conn, resultSet);
        }
        return PageResult.success(list, list.size(), 0, 0, 0);
    }

    private BranchSessionVO convertBranchSessionVO(ResultSet rs) throws SQLException {
        BranchSessionVO branchSessionVO = new BranchSessionVO();
        branchSessionVO.setXid(rs.getString(ServerTableColumnsName.BRANCH_TABLE_XID));
        branchSessionVO.setTransactionId(rs.getLong(ServerTableColumnsName.BRANCH_TABLE_TRANSACTION_ID));
        branchSessionVO.setBranchId(rs.getLong(ServerTableColumnsName.BRANCH_TABLE_BRANCH_ID));
        branchSessionVO.setResourceGroupId(rs.getString(ServerTableColumnsName.BRANCH_TABLE_RESOURCE_GROUP_ID));
        branchSessionVO.setResourceId(rs.getString(ServerTableColumnsName.BRANCH_TABLE_RESOURCE_ID));
        branchSessionVO.setBranchType(rs.getString(ServerTableColumnsName.BRANCH_TABLE_BRANCH_TYPE));
        branchSessionVO.setStatus(rs.getInt(ServerTableColumnsName.BRANCH_TABLE_STATUS));
        branchSessionVO.setClientId(rs.getString(ServerTableColumnsName.BRANCH_TABLE_CLIENT_ID));
        branchSessionVO.setApplicationData(rs.getString(ServerTableColumnsName.BRANCH_TABLE_APPLICATION_DATA));
        branchSessionVO.setGmtCreate(rs.getDate(ServerTableColumnsName.BRANCH_TABLE_GMT_CREATE));
        branchSessionVO.setGmtModified(rs.getDate(ServerTableColumnsName.BRANCH_TABLE_GMT_MODIFIED));
        return branchSessionVO;
    }
}
