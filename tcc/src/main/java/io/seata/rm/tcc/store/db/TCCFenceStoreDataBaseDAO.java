package io.seata.rm.tcc.store.db;

import io.seata.common.exception.DataAccessException;
import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.exception.StoreException;
import io.seata.common.util.IOUtil;
import io.seata.rm.tcc.config.TCCFenceConfig;
import io.seata.rm.tcc.exception.TCCFenceException;
import io.seata.rm.tcc.store.TCCFenceDO;
import io.seata.rm.tcc.store.TCCFenceStore;
import io.seata.rm.tcc.store.db.sql.TCCFenceStoreSqls;
import org.yaml.snakeyaml.constructor.DuplicateKeyException;

import javax.sql.DataSource;
import java.sql.*;

/**
 * The type TCC Fence store data base dao
 *
 * @author cebbank
 */
public class TCCFenceStoreDataBaseDAO implements TCCFenceStore {

    private static TCCFenceStoreDataBaseDAO instance = null;

    public static synchronized TCCFenceStore getInstance() {
        if (instance == null) {
            instance = new TCCFenceStoreDataBaseDAO();
        }
        return instance;
    }

    @Override
    public TCCFenceDO queryTCCFenceDO(DataSource dataSource, String xid, Long branchId) {
        String sql = TCCFenceStoreSqls.getQuerySQLByBranchIdAndXid(TCCFenceConfig.getLogTableName());
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement(sql);
            ps.setString(1, xid);
            ps.setLong(2, branchId);
            rs = ps.executeQuery();
            if (rs.next()) {
                TCCFenceDO tccFenceDO = new TCCFenceDO();
                tccFenceDO.setXid(rs.getString("xid"));
                tccFenceDO.setBranchId(rs.getLong("branch_id"));
                tccFenceDO.setStatus(rs.getInt("status"));
                return tccFenceDO;
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new DataAccessException(e);
        } finally {
            IOUtil.close(rs, ps, conn);
        }
    }

    @Override
    public boolean insertTCCFenceDO(DataSource dataSource, TCCFenceDO tccFenceDO) {
        String sql = TCCFenceStoreSqls.getInsertLocalTCCLogSQL(TCCFenceConfig.getLogTableName());
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement(sql);
            ps.setString(1, tccFenceDO.getXid());
            ps.setLong(2, tccFenceDO.getBranchId());
            ps.setInt(3, tccFenceDO.getStatus());
            ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            return ps.executeUpdate() > 0;
        } catch (DuplicateKeyException e) {
            throw new TCCFenceException(String.format("Insert tcc fence record duplicate key exception. xid= %s, branchId= %s", tccFenceDO.getXid(), tccFenceDO.getBranchId()),
                    FrameworkErrorCode.DuplicateKeyException);
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(ps, conn);
        }
    }

    @Override
    public boolean updateTCCFenceDO(DataSource dataSource, String xid, Long branchId, int newStatus, int oldStatus) {
        String sql = TCCFenceStoreSqls.getUpdateStatusSQLByBranchIdAndXid(TCCFenceConfig.getLogTableName());
        PreparedStatement ps = null;
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(true);
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
            IOUtil.close(ps, conn);
        }
    }

    @Override
    public boolean deleteTCCFenceDO(DataSource dataSource, String xid, Long branchId) {
        String sql = TCCFenceStoreSqls.getDeleteSQLByBranchIdAndXid(TCCFenceConfig.getLogTableName());
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement(sql);
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
    public boolean deleteTCCFenceDOByDate(DataSource dataSource, Timestamp datetime) {
        String sql = TCCFenceStoreSqls.getDeleteSQLByDateAndStatus(TCCFenceConfig.getLogTableName());
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, datetime);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new StoreException(e);
        } finally {
            IOUtil.close(ps, conn);
        }
        return true;
    }
}
