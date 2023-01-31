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
package io.seata.integration.tx.api.fence.store.db;

import io.seata.common.DefaultValues;
import io.seata.common.exception.DataAccessException;
import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.exception.StoreException;
import io.seata.common.util.IOUtil;
import io.seata.integration.tx.api.fence.store.db.sql.CommonFenceStoreSqls;
import io.seata.integration.tx.api.fence.exception.CommonFenceException;
import io.seata.integration.tx.api.fence.store.CommonFenceDO;
import io.seata.integration.tx.api.fence.store.CommonFenceStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The type Common Fence store data base dao
 *
 * @author kaka2code
 */
public class CommonFenceStoreDataBaseDAO implements CommonFenceStore {

    /**
     * Common fence log table name
     */
    private String logTableName = DefaultValues.DEFAULT_COMMON_FENCE_LOG_TABLE_NAME;

    private static volatile CommonFenceStoreDataBaseDAO instance = null;

    private CommonFenceStoreDataBaseDAO() {}

    public static CommonFenceStore getInstance() {
        if (instance == null) {
            synchronized (CommonFenceStore.class) {
                if (instance == null) {
                    instance = new CommonFenceStoreDataBaseDAO();
                }
            }
        }
        return instance;
    }

    @Override
    public CommonFenceDO queryCommonFenceDO(Connection conn, String xid, Long branchId) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            String sql = CommonFenceStoreSqls.getQuerySQLByBranchIdAndXid(logTableName);
            ps = conn.prepareStatement(sql);
            ps.setString(1, xid);
            ps.setLong(2, branchId);
            rs = ps.executeQuery();
            if (rs.next()) {
                CommonFenceDO commonFenceDO = new CommonFenceDO();
                commonFenceDO.setXid(rs.getString("xid"));
                commonFenceDO.setBranchId(rs.getLong("branch_id"));
                commonFenceDO.setStatus(rs.getInt("status"));
                return commonFenceDO;
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new DataAccessException(e);
        } finally {
            IOUtil.close(rs, ps);
        }
    }

    @Override
    public Set<String> queryEndStatusXidsByDate(Connection conn, Date datetime, int limit) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            String sql = CommonFenceStoreSqls.getQueryEndStatusSQLByDate(logTableName);
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, new Timestamp(datetime.getTime()));
            ps.setInt(2, limit);
            rs = ps.executeQuery();
            Set<String> xids = new HashSet<>(limit);
            while (rs.next()) {
                xids.add(rs.getString("xid"));
            }
            return xids;
        } catch (SQLException e) {
            throw new DataAccessException(e);
        } finally {
            IOUtil.close(rs, ps);
        }
    }

    @Override
    public boolean insertCommonFenceDO(Connection conn, CommonFenceDO commonFenceDO) {
        PreparedStatement ps = null;
        try {
            Timestamp now = new Timestamp(System.currentTimeMillis());

            String sql = CommonFenceStoreSqls.getInsertLocalTCCLogSQL(logTableName);
            ps = conn.prepareStatement(sql);
            ps.setString(1, commonFenceDO.getXid());
            ps.setLong(2, commonFenceDO.getBranchId());
            ps.setString(3, commonFenceDO.getActionName());
            ps.setInt(4, commonFenceDO.getStatus());
            ps.setTimestamp(5, now);
            ps.setTimestamp(6, now);
            return ps.executeUpdate() > 0;
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new CommonFenceException(String.format("Insert tcc fence record duplicate key exception. xid= %s, branchId= %s", commonFenceDO.getXid(), commonFenceDO.getBranchId()),
                    FrameworkErrorCode.DuplicateKeyException);
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(ps);
        }
    }

    @Override
    public boolean updateCommonFenceDO(Connection conn, String xid, Long branchId, int newStatus, int oldStatus) {
        PreparedStatement ps = null;
        try {
            String sql = CommonFenceStoreSqls.getUpdateStatusSQLByBranchIdAndXid(logTableName);
            ps = conn.prepareStatement(sql);
            ps.setInt(1, newStatus);
            // gmt_modified
            ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            ps.setString(3, xid);
            ps.setLong(4, branchId);
            ps.setInt(5, oldStatus);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(ps);
        }
    }

    @Override
    public boolean deleteCommonFenceDO(Connection conn, String xid, Long branchId) {
        PreparedStatement ps = null;
        try {
            String sql = CommonFenceStoreSqls.getDeleteSQLByBranchIdAndXid(logTableName);
            ps = conn.prepareStatement(sql);
            ps.setString(1, xid);
            ps.setLong(2, branchId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(ps);
        }
    }

    @Override
    public int deleteTCCFenceDO(Connection conn, List<String> xids) {
        PreparedStatement ps = null;
        try {
            String paramsPlaceHolder = org.apache.commons.lang.StringUtils.repeat("?", ",", xids.size());
            String sql = CommonFenceStoreSqls.getDeleteSQLByXids(logTableName, paramsPlaceHolder);
            ps = conn.prepareStatement(sql);
            for (int i = 0; i < xids.size(); i++) {
                ps.setString(i + 1, xids.get(i));
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(ps);
        }
    }

    @Override
    public int deleteCommonFenceDOByDate(Connection conn, Date datetime) {
        PreparedStatement ps = null;
        try {
            String sql = CommonFenceStoreSqls.getDeleteSQLByDateAndStatus(logTableName);
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, new Timestamp(datetime.getTime()));
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(ps);
        }
    }

    @Override
    public void setLogTableName(String logTableName) {
        this.logTableName = logTableName;
    }
}