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
import io.seata.common.loader.LoadLevel;
import io.seata.common.loader.Scope;
import io.seata.common.util.IOUtil;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.constants.ServerTableColumnsName;
import io.seata.core.store.db.sql.log.LogStoreSqlsFactory;
import io.seata.core.store.db.vo.GlobalSessionVO;
import io.seata.server.console.manager.GlobalSessionServiceManager;
import io.seata.server.console.result.PageResult;
import io.seata.server.console.service.GlobalSessionService;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static io.seata.common.DefaultValues.DEFAULT_STORE_DB_GLOBAL_TABLE;

/**
 * Global Session DataBase ServiceImpl
 * @author: zhongxiang.wang
 */
@LoadLevel(name = "db", scope = Scope.PROTOTYPE)
public class GlobalSessionDBServiceImpl implements GlobalSessionService {

    private static final Configuration CONFIG = ConfigurationFactory.getInstance();

    /**
     * the global session table
     */
    protected String globalTable;
    /**
     * the db type
     */
    protected String dbType;

    public GlobalSessionDBServiceImpl() {
        globalTable = CONFIG.getConfig(ConfigurationKeys.STORE_DB_GLOBAL_TABLE, DEFAULT_STORE_DB_GLOBAL_TABLE);
        dbType = CONFIG.getConfig(ConfigurationKeys.STORE_DB_TYPE);
        if (StringUtils.isBlank(dbType)) {
            throw new StoreException("there must be db type.");
        }
    }

    @Override
    public PageResult<GlobalSessionVO> queryAll() {
        List<GlobalSessionVO> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet resultSet = null;
        DataSource dataSource = GlobalSessionServiceManager.getDataSource();
        String queryAllGlobalSessionSQL = LogStoreSqlsFactory.getLogStoreSqls(dbType).getAllGlobalSessionSQL(globalTable);
        try {
            conn = dataSource.getConnection();
            ps = conn.prepareStatement(queryAllGlobalSessionSQL);

            resultSet = ps.executeQuery();
            while (resultSet.next()) {
                list.add(this.convertGlobalSessionVO(resultSet));
            }
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(ps, conn, resultSet);
        }
        return PageResult.success(list, list.size(), 0, 0, 0);
    }

    private GlobalSessionVO convertGlobalSessionVO(ResultSet rs) throws SQLException {
        GlobalSessionVO globalSessionVO = new GlobalSessionVO();
        globalSessionVO.setXid(rs.getString(ServerTableColumnsName.GLOBAL_TABLE_XID));
        globalSessionVO.setTransactionId(rs.getLong(ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_ID));
        globalSessionVO.setStatus(rs.getInt(ServerTableColumnsName.GLOBAL_TABLE_STATUS));
        globalSessionVO.setApplicationId(rs.getString(ServerTableColumnsName.GLOBAL_TABLE_APPLICATION_ID));
        globalSessionVO.setTransactionServiceGroup(rs.getString(ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_SERVICE_GROUP));
        globalSessionVO.setTransactionName(rs.getString(ServerTableColumnsName.GLOBAL_TABLE_TRANSACTION_NAME));
        globalSessionVO.setTimeout(rs.getLong(ServerTableColumnsName.GLOBAL_TABLE_TIMEOUT));
        globalSessionVO.setBeginTime(rs.getLong(ServerTableColumnsName.GLOBAL_TABLE_BEGIN_TIME));
        globalSessionVO.setApplicationData(rs.getString(ServerTableColumnsName.GLOBAL_TABLE_APPLICATION_DATA));
        globalSessionVO.setGmtCreate(rs.getDate(ServerTableColumnsName.GLOBAL_TABLE_GMT_CREATE));
        globalSessionVO.setGmtModified(rs.getDate(ServerTableColumnsName.GLOBAL_TABLE_GMT_MODIFIED));
        return globalSessionVO;
    }

    @Override
    public PageResult<GlobalSessionVO> queryByStatus(Integer status) {
        return null;
    }

    @Override
    public GlobalSessionVO queryByXid(String xid) {
        return null;
    }
}
