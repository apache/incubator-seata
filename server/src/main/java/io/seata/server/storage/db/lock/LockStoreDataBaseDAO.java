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
package io.seata.server.storage.db.lock;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import io.seata.common.exception.DataAccessException;
import io.seata.common.exception.StoreException;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.IOUtil;
import io.seata.common.util.LambdaUtils;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.constants.ServerTableColumnsName;
import io.seata.core.store.LockDO;
import io.seata.core.store.LockStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.core.constants.DefaultValues.DEFAULT_LOCK_DB_TABLE;

/**
 * The type Data base lock store.
 *
 * @author zhangsen
 */
public class LockStoreDataBaseDAO implements LockStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(LockStoreDataBaseDAO.class);

    /**
     * The constant CONFIG.
     */
    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();

    /**
     * The Log store data source.
     */
    protected DataSource logStoreDataSource = null;

    /**
     * The Lock table.
     */
    protected String lockTable;

    /**
     * The Db type.
     */
    protected String dbType;

    /**
     * Instantiates a new Data base lock store dao.
     *
     * @param logStoreDataSource the log store data source
     */
    public LockStoreDataBaseDAO(DataSource logStoreDataSource) {
        this.logStoreDataSource = logStoreDataSource;
        lockTable = CONFIG.getConfig(ConfigurationKeys.LOCK_DB_TABLE, DEFAULT_LOCK_DB_TABLE);
        dbType = CONFIG.getConfig(ConfigurationKeys.STORE_DB_TYPE);
        if (StringUtils.isBlank(dbType)) {
            throw new StoreException("there must be db type.");
        }
        if (logStoreDataSource == null) {
            throw new StoreException("there must be logStoreDataSource.");
        }
    }

    @Override
    public boolean acquireLock(LockDO lockDO) {
        return acquireLock(Collections.singletonList(lockDO));
    }

    @Override
    public boolean acquireLock(List<LockDO> lockDOs) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        Set<String> dbExistedRowKeys = new HashSet<>();
        boolean originalAutoCommit = true;
        if (lockDOs.size() > 1) {
            lockDOs = lockDOs.stream().filter(LambdaUtils.distinctByKey(LockDO::getRowKey)).collect(Collectors.toList());
        }
        try {
            conn = logStoreDataSource.getConnection();
            if (originalAutoCommit = conn.getAutoCommit()) {
                conn.setAutoCommit(false);
            }
            //check lock
            StringJoiner sj = new StringJoiner(",");
            for (int i = 0; i < lockDOs.size(); i++) {
                sj.add("?");
            }
            boolean canLock = true;
            //query
            String checkLockSQL = LockStoreSqls.getCheckLockableSql(lockTable, sj.toString(), dbType);
            ps = conn.prepareStatement(checkLockSQL);
            for (int i = 0; i < lockDOs.size(); i++) {
                ps.setString(i + 1, lockDOs.get(i).getRowKey());
            }
            rs = ps.executeQuery();
            String currentXID = lockDOs.get(0).getXid();
            while (rs.next()) {
                String dbXID = rs.getString(ServerTableColumnsName.LOCK_TABLE_XID);
                if (!StringUtils.equals(dbXID, currentXID)) {
                    if (LOGGER.isInfoEnabled()) {
                        String dbPk = rs.getString(ServerTableColumnsName.LOCK_TABLE_PK);
                        String dbTableName = rs.getString(ServerTableColumnsName.LOCK_TABLE_TABLE_NAME);
                        Long dbBranchId = rs.getLong(ServerTableColumnsName.LOCK_TABLE_BRANCH_ID);
                        LOGGER.info("Global lock on [{}:{}] is holding by xid {} branchId {}", dbTableName, dbPk, dbXID,
                            dbBranchId);
                    }
                    canLock &= false;
                    break;
                }
                dbExistedRowKeys.add(rs.getString(ServerTableColumnsName.LOCK_TABLE_ROW_KEY));
            }

            if (!canLock) {
                conn.rollback();
                return false;
            }
            List<LockDO> unrepeatedLockDOs = null;
            if (CollectionUtils.isNotEmpty(dbExistedRowKeys)) {
                unrepeatedLockDOs = lockDOs.stream().filter(lockDO -> !dbExistedRowKeys.contains(lockDO.getRowKey()))
                    .collect(Collectors.toList());
            } else {
                unrepeatedLockDOs = lockDOs;
            }
            if (CollectionUtils.isEmpty(unrepeatedLockDOs)) {
                conn.rollback();
                return true;
            }
            //lock
            if (unrepeatedLockDOs.size() == 1) {
                LockDO lockDO = unrepeatedLockDOs.get(0);
                if (!doAcquireLock(conn, lockDO)) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("Global lock acquire failed, xid {} branchId {} pk {}", lockDO.getXid(), lockDO.getBranchId(), lockDO.getPk());
                    }
                    conn.rollback();
                    return false;
                }
            } else {
                if (!doAcquireLocks(conn, unrepeatedLockDOs)) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("Global lock batch acquire failed, xid {} branchId {} pks {}", unrepeatedLockDOs.get(0).getXid(),
                            unrepeatedLockDOs.get(0).getBranchId(), unrepeatedLockDOs.stream().map(lockDO -> lockDO.getPk()).collect(Collectors.toList()));
                    }
                    conn.rollback();
                    return false;
                }
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(rs, ps);
            if (conn != null) {
                try {
                    if (originalAutoCommit) {
                        conn.setAutoCommit(true);
                    }
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    @Override
    public boolean unLock(LockDO lockDO) {
        return unLock(Collections.singletonList(lockDO));
    }

    @Override
    public boolean unLock(List<LockDO> lockDOs) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = logStoreDataSource.getConnection();
            conn.setAutoCommit(true);

            StringJoiner sj = new StringJoiner(",");
            for (int i = 0; i < lockDOs.size(); i++) {
                sj.add("?");
            }
            //batch release lock
            String batchDeleteSQL = LockStoreSqls.getBatchDeleteLockSql(lockTable, sj.toString(), dbType);
            ps = conn.prepareStatement(batchDeleteSQL);
            ps.setString(1, lockDOs.get(0).getXid());
            for (int i = 0; i < lockDOs.size(); i++) {
                ps.setString(i + 2, lockDOs.get(i).getRowKey());
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(ps, conn);
        }
        return true;
    }

    @Override
    public boolean unLock(String xid, Long branchId) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = logStoreDataSource.getConnection();
            conn.setAutoCommit(true);
            //batch release lock by branch
            String batchDeleteSQL = LockStoreSqls.getBatchDeleteLockSqlByBranch(lockTable, dbType);
            ps = conn.prepareStatement(batchDeleteSQL);
            ps.setString(1, xid);
            ps.setLong(2, branchId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(ps, conn);
        }
        return true;
    }

    @Override
    public boolean unLock(String xid, List<Long> branchIds) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = logStoreDataSource.getConnection();
            conn.setAutoCommit(true);
            StringJoiner sj = new StringJoiner(",");
            branchIds.forEach(branchId -> sj.add("?"));
            //batch release lock by branch list
            String batchDeleteSQL = LockStoreSqls.getBatchDeleteLockSqlByBranchs(lockTable, sj.toString(), dbType);
            ps = conn.prepareStatement(batchDeleteSQL);
            ps.setString(1, xid);
            for (int i = 0; i < branchIds.size(); i++) {
                ps.setLong(i + 2, branchIds.get(i));
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(ps, conn);
        }
        return true;
    }

    @Override
    public boolean isLockable(List<LockDO> lockDOs) {
        Connection conn = null;
        try {
            conn = logStoreDataSource.getConnection();
            conn.setAutoCommit(true);
            if (!checkLockable(conn, lockDOs)) {
                return false;
            }
            return true;
        } catch (SQLException e) {
            throw new DataAccessException(e);
        } finally {
            IOUtil.close(conn);
        }
    }

    /**
     * Do acquire lock boolean.
     *
     * @param conn   the conn
     * @param lockDO the lock do
     * @return the boolean
     */
    protected boolean doAcquireLock(Connection conn, LockDO lockDO) {
        PreparedStatement ps = null;
        try {
            //insert
            String insertLockSQL = LockStoreSqls.getInsertLockSQL(lockTable, dbType);
            ps = conn.prepareStatement(insertLockSQL);
            ps.setString(1, lockDO.getXid());
            ps.setLong(2, lockDO.getTransactionId());
            ps.setLong(3, lockDO.getBranchId());
            ps.setString(4, lockDO.getResourceId());
            ps.setString(5, lockDO.getTableName());
            ps.setString(6, lockDO.getPk());
            ps.setString(7, lockDO.getRowKey());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(ps);
        }
    }

    /**
     * Do acquire lock boolean.
     *
     * @param conn    the conn
     * @param lockDOs the lock do list
     * @return the boolean
     */
    protected boolean doAcquireLocks(Connection conn, List<LockDO> lockDOs) {
        PreparedStatement ps = null;
        try {
            //insert
            String insertLockSQL = LockStoreSqls.getInsertLockSQL(lockTable, dbType);
            ps = conn.prepareStatement(insertLockSQL);
            for (LockDO lockDO : lockDOs) {
                ps.setString(1, lockDO.getXid());
                ps.setLong(2, lockDO.getTransactionId());
                ps.setLong(3, lockDO.getBranchId());
                ps.setString(4, lockDO.getResourceId());
                ps.setString(5, lockDO.getTableName());
                ps.setString(6, lockDO.getPk());
                ps.setString(7, lockDO.getRowKey());
                ps.addBatch();
            }
            return ps.executeBatch().length == lockDOs.size();
        } catch (SQLException e) {
            LOGGER.error("Global lock batch acquire error: {}", e.getMessage(), e);
            //return false,let the caller go to conn.rollabck()
            return false;
        } finally {
            IOUtil.close(ps);
        }
    }

    /**
     * Check lock boolean.
     *
     * @param conn    the conn
     * @param lockDOs the lock do
     * @return the boolean
     */
    protected boolean checkLockable(Connection conn, List<LockDO> lockDOs) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            StringJoiner sj = new StringJoiner(",");
            for (int i = 0; i < lockDOs.size(); i++) {
                sj.add("?");
            }

            //query
            String checkLockSQL = LockStoreSqls.getCheckLockableSql(lockTable, sj.toString(), dbType);
            ps = conn.prepareStatement(checkLockSQL);
            for (int i = 0; i < lockDOs.size(); i++) {
                ps.setString(i + 1, lockDOs.get(i).getRowKey());
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                String xid = rs.getString("xid");
                if (!StringUtils.equals(xid, lockDOs.get(0).getXid())) {
                    return false;
                }
            }
            return true;
        } catch (SQLException e) {
            throw new DataAccessException(e);
        } finally {
            IOUtil.close(rs, ps);
        }
    }

    /**
     * Sets lock table.
     *
     * @param lockTable the lock table
     */
    public void setLockTable(String lockTable) {
        this.lockTable = lockTable;
    }

    /**
     * Sets db type.
     *
     * @param dbType the db type
     */
    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    /**
     * Sets log store data source.
     *
     * @param logStoreDataSource the log store data source
     */
    public void setLogStoreDataSource(DataSource logStoreDataSource) {
        this.logStoreDataSource = logStoreDataSource;
    }
}
