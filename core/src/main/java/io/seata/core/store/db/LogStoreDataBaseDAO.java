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
package io.seata.core.store.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import io.seata.common.exception.StoreException;
import io.seata.common.executor.Initialize;
import io.seata.common.loader.LoadLevel;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.store.BranchTransactionDO;
import io.seata.core.store.GlobalTransactionDO;
import io.seata.core.store.LogStore;

/**
 * The type Log store data base dao.
 *
 * @author zhangsen
 * @data 2019 /4/2
 */
@LoadLevel(name = "db")
public class LogStoreDataBaseDAO implements LogStore, Initialize {

    /**
     * The constant CONFIG.
     */
    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();

    /**
     * The Log store data source.
     */
    protected DataSource logStoreDataSource = null;

    /**
     * The Global table.
     */
    protected String globalTable;

    /**
     * The Brach table.
     */
    protected String brachTable;

    private String dbType;

    /**
     * Instantiates a new Log store data base dao.
     */
    public LogStoreDataBaseDAO() {
    }

    /**
     * Instantiates a new Log store data base dao.
     *
     * @param logStoreDataSource the log store data source
     */
    public LogStoreDataBaseDAO(DataSource logStoreDataSource) {
        this.logStoreDataSource = logStoreDataSource;
    }

    @Override
    public void init() {
        globalTable = CONFIG.getConfig(ConfigurationKeys.STORE_DB_GLOBAL_TABLE,
            ConfigurationKeys.STORE_DB_GLOBAL_DEFAULT_TABLE);
        brachTable = CONFIG.getConfig(ConfigurationKeys.STORE_DB_BRANCH_TABLE,
            ConfigurationKeys.STORE_DB_BRANCH_DEFAULT_TABLE);
        dbType = CONFIG.getConfig(ConfigurationKeys.STORE_DB_TYPE);
        if (StringUtils.isBlank(dbType)) {
            throw new StoreException("there must be db type.");
        }
        if (logStoreDataSource == null) {
            throw new StoreException("there must be logStoreDataSource.");
        }
    }

    @Override
    public GlobalTransactionDO queryGlobalTransactionDO(String xid) {
        String sql = LogStoreSqls.getQueryGlobalTransactionSQL(globalTable, dbType);
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = logStoreDataSource.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement(sql);
            ps.setString(1, xid);
            rs = ps.executeQuery();
            if (rs.next()) {
                return convertGlobalTransactionDO(rs);
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    @Override
    public GlobalTransactionDO queryGlobalTransactionDO(long transactionId) {
        String sql = LogStoreSqls.getQueryGlobalTransactionSQLByTransactionId(globalTable, dbType);
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = logStoreDataSource.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement(sql);
            ps.setLong(1, transactionId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return convertGlobalTransactionDO(rs);
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    @Override
    public List<GlobalTransactionDO> queryGlobalTransactionDO(int[] statuses, int limit) {
        List<GlobalTransactionDO> ret = new ArrayList<GlobalTransactionDO>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = logStoreDataSource.getConnection();
            conn.setAutoCommit(true);

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < statuses.length; i++) {
                sb.append("?");
                if (i != (statuses.length - 1)) {
                    sb.append(", ");
                }
            }

            String sql = LogStoreSqls.getQueryGlobalTransactionSQLByStatus(globalTable, dbType, sb.toString());
            ps = conn.prepareStatement(sql);
            for (int i = 0; i < statuses.length; i++) {
                int status = statuses[i];
                ps.setInt(i + 1, status);
            }
            ps.setInt(statuses.length + 1, limit);
            rs = ps.executeQuery();
            while (rs.next()) {
                ret.add(convertGlobalTransactionDO(rs));
            }
            return ret;
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    @Override
    public boolean insertGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        String sql = LogStoreSqls.getInsertGlobalTransactionSQL(globalTable, dbType);
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = logStoreDataSource.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement(sql);
            ps.setString(1, globalTransactionDO.getXid());
            ps.setLong(2, globalTransactionDO.getTransactionId());
            ps.setInt(3, globalTransactionDO.getStatus());
            ps.setString(4, globalTransactionDO.getApplicationId());
            ps.setString(5, globalTransactionDO.getTransactionServiceGroup());
            ps.setString(6, globalTransactionDO.getTransactionName());
            ps.setInt(7, globalTransactionDO.getTimeout());
            ps.setLong(8, globalTransactionDO.getBeginTime());
            ps.setString(9, globalTransactionDO.getApplicationData());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    @Override
    public boolean updateGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        String sql = LogStoreSqls.getUpdateGlobalTransactionStatusSQL(globalTable, dbType);
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = logStoreDataSource.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement(sql);
            ps.setInt(1, globalTransactionDO.getStatus());
            ps.setString(2, globalTransactionDO.getXid());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    @Override
    public boolean deleteGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) {
        String sql = LogStoreSqls.getDeleteGlobalTransactionSQL(globalTable, dbType);
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = logStoreDataSource.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement(sql);
            ps.setString(1, globalTransactionDO.getXid());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    @Override
    public List<BranchTransactionDO> queryBranchTransactionDO(String xid) {
        List<BranchTransactionDO> rets = new ArrayList<>();
        String sql = LogStoreSqls.getQureyBranchTransaction(brachTable, dbType);
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = logStoreDataSource.getConnection();
            conn.setAutoCommit(true);

            ps = conn.prepareStatement(sql);
            ps.setString(1, xid);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rets.add(convertBranchTransactionDO(rs));
            }
            return rets;
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    @Override
    public boolean insertBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
        String sql = LogStoreSqls.getInsertBranchTransactionSQL(brachTable, dbType);
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = logStoreDataSource.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement(sql);
            ps.setString(1, branchTransactionDO.getXid());
            ps.setLong(2, branchTransactionDO.getTransactionId());
            ps.setLong(3, branchTransactionDO.getBranchId());
            ps.setString(4, branchTransactionDO.getResourceGroupId());
            ps.setString(5, branchTransactionDO.getResourceId());
            ps.setString(6, branchTransactionDO.getLockKey());
            ps.setString(7, branchTransactionDO.getBranchType());
            ps.setInt(8, branchTransactionDO.getStatus());
            ps.setString(9, branchTransactionDO.getClientId());
            ps.setString(10, branchTransactionDO.getApplicationData());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    @Override
    public boolean updateBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
        String sql = LogStoreSqls.getUpdateBranchTransactionStatusSQL(brachTable, dbType);
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = logStoreDataSource.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement(sql);
            ps.setInt(1, branchTransactionDO.getStatus());
            ps.setString(2, branchTransactionDO.getXid());
            ps.setLong(3, branchTransactionDO.getBranchId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    @Override
    public boolean deleteBranchTransactionDO(BranchTransactionDO branchTransactionDO) {
        String sql = LogStoreSqls.getDeleteBranchTransactionByBranchIdSQL(brachTable, dbType);
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = logStoreDataSource.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement(sql);
            ps.setString(1, branchTransactionDO.getXid());
            ps.setLong(2, branchTransactionDO.getBranchId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    private GlobalTransactionDO convertGlobalTransactionDO(ResultSet rs) throws SQLException {
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        globalTransactionDO.setXid(rs.getString("xid"));
        globalTransactionDO.setStatus(rs.getInt("status"));
        globalTransactionDO.setApplicationId(rs.getString("application_id"));
        globalTransactionDO.setBeginTime(rs.getLong("begin_time"));
        globalTransactionDO.setTimeout(rs.getInt("timeout"));
        globalTransactionDO.setTransactionId(rs.getLong("transaction_id"));
        globalTransactionDO.setTransactionName(rs.getString("transaction_name"));
        globalTransactionDO.setTransactionServiceGroup(rs.getString("transaction_service_group"));
        globalTransactionDO.setApplicationData(rs.getString("application_data"));
        globalTransactionDO.setGmtCreate(rs.getTimestamp("gmt_create"));
        globalTransactionDO.setGmtModified(rs.getTimestamp("gmt_modified"));
        return globalTransactionDO;
    }

    private BranchTransactionDO convertBranchTransactionDO(ResultSet rs) throws SQLException {
        BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
        branchTransactionDO.setResourceGroupId(rs.getString("resource_group_id"));
        branchTransactionDO.setStatus(rs.getInt("status"));
        branchTransactionDO.setApplicationData(rs.getString("application_data"));
        branchTransactionDO.setClientId(rs.getString("client_id"));
        branchTransactionDO.setLockKey(rs.getString("lock_key"));
        branchTransactionDO.setXid(rs.getString("xid"));
        branchTransactionDO.setResourceId(rs.getString("resource_id"));
        branchTransactionDO.setBranchId(rs.getLong("branch_id"));
        branchTransactionDO.setBranchType(rs.getString("branch_type"));
        branchTransactionDO.setTransactionId(rs.getLong("transaction_id"));
        branchTransactionDO.setGmtCreate(rs.getTimestamp("gmt_create"));
        branchTransactionDO.setGmtModified(rs.getTimestamp("gmt_modified"));
        return branchTransactionDO;
    }

    /**
     * Sets log store data source.
     *
     * @param logStoreDataSource the log store data source
     */
    public void setLogStoreDataSource(DataSource logStoreDataSource) {
        this.logStoreDataSource = logStoreDataSource;
    }

    /**
     * Sets global table.
     *
     * @param globalTable the global table
     */
    public void setGlobalTable(String globalTable) {
        this.globalTable = globalTable;
    }

    /**
     * Sets brach table.
     *
     * @param brachTable the brach table
     */
    public void setBrachTable(String brachTable) {
        this.brachTable = brachTable;
    }

    /**
     * Sets db type.
     *
     * @param dbType the db type
     */
    public void setDbType(String dbType) {
        this.dbType = dbType;
    }
}