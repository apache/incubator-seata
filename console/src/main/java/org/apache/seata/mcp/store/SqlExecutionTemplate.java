package org.apache.seata.mcp.store;

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SqlExecutionTemplate {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlExecutionTemplate.class);


    /**
     * Obtain the data source with the specified resourceId
     * @return DataSource instance
     */
    private DataSource getDataSource(String resourceId) {
        try {
            return DataSourceFactory.getDataSource(resourceId);
        } catch (Exception e) {
            LOGGER.error("Failed to get the data source, resourceId: {}",resourceId , e);
            throw new StoreException("Unable to get the data source: " + resourceId);
        }
    }

    private boolean validateQuerySql(String sql) {
        if(sql == null || StringUtils.isEmpty(sql)){
            return false;
        }
        return !sql.contains("DELETE") && !sql.contains("UPDATE") && !sql.contains("INSERT");
    }

    private boolean validateUpdateSql(String sql) {
        if(sql == null || StringUtils.isBlank(sql)){
            return false;
        }
        return !sql.contains("SELECT");
    }

    /**
     * Execute the query and return <Map>the list result
     *
     * @param resourceId of the data source
     * @param sql SQL query statement
     * @param params parameters
     * @return List of query results
     */
    public List<Map<String, Object>> query(String resourceId, String sql, Object... params) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            if(!validateQuerySql(sql)){
                throw new StoreException("The query valid failed: "+ sql);
            }
            conn = getDataSource(resourceId).getConnection();
            ps = conn.prepareStatement(sql);
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
            }

            rs = ps.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            List<Map<String, Object>> results = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                results.add(row);
            }

            return results;
        } catch (SQLException e) {
            LOGGER.error("The query failed, resourceId: {}, sql: {}", resourceId, sql, e);
            throw new StoreException("The query execution failed: " + e.getMessage());
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    /**
     * PERFORM AN UPDATE OPERATION (INSERT, UPDATE, DELETE)
     *
     * @param resourceId of the data source
     * @param sql SQL update statement
     * @param params parameters
     * @return The number of rows affected
     */
    public int update(String resourceId, String sql, Object... params) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            if(!validateUpdateSql(sql)){
                throw new StoreException("The query valid failed: "+ sql);
            }
            conn = getDataSource(resourceId).getConnection();
            ps = conn.prepareStatement(sql);

            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
            }

            return ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Failed to perform the update, resourceId: {}, sql: {}", resourceId, sql, e);
            throw new StoreException("The update failed to be executed: " + e.getMessage());
        } finally {
            closeResources(null, ps, conn);
        }
    }

    /**
     * Perform update operations in bulk
     *
     * @param resourceId of the data source
     * @param sql SQL statements
     * @param batchParams batch parameter list
     * @return Array of the number of rows affected
     */
    public int[] batchUpdate(String resourceId, String sql, List<Object[]> batchParams) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            if(!validateUpdateSql(sql)){
                throw new StoreException("The query valid failed: "+ sql);
            }
            conn = getDataSource(resourceId).getConnection();
            conn.setAutoCommit(false);

            ps = conn.prepareStatement(sql);

            for (Object[] params : batchParams) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
                ps.addBatch();
            }

            int[] results = ps.executeBatch();
            conn.commit();
            return results;
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                LOGGER.error("The rollback transaction failed", ex);
            }
            LOGGER.error("Failed to perform a bulk update, resourceId: {}, sql: {}", resourceId, sql, e);
            throw new StoreException("The batch update failed to be executed: " + e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                LOGGER.error("Restoring the connection auto-commit failed", e);
            }
            closeResources(null, ps, conn);
        }
    }

    /**
     * Execute a query that returns a single object
     *
     * @param resourceId of the data source
     * @param sql SQL query statement
     * @param params parameters
     * @return A single Map result, if there is no result, null will be returned
     */
    public Map<String, Object> queryForObject(String resourceId, String sql, Object... params) {
        List<Map<String, Object>> results = query(resourceId, sql, params);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Perform transactional operations
     *
     * @param resourceId of the data source
     * @param operations transaction operation interface
     * @return Result of the operation
     */
    public <T> T executeTransaction(String resourceId, TransactionOperation<T> operations) {
        Connection conn = null;

        try {
            conn = getDataSource(resourceId).getConnection();
            conn.setAutoCommit(false);

            T result = operations.execute(conn);

            conn.commit();
            return result;
        } catch (Exception e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                LOGGER.error("The rollback transaction failed", ex);
            }
            LOGGER.error("Failed to perform the transaction, resourceId: {}", resourceId, e);
            throw new StoreException("The transaction operation failed: " + e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true); // 恢复自动提交
                }
            } catch (SQLException e) {
                LOGGER.error("Restoring the connection auto-commit failed", e);
            }
            closeConnection(conn);
        }
    }


    public interface TransactionOperation<T> {
        T execute(Connection connection) throws SQLException;
    }


    private void closeResources(ResultSet rs, Statement stmt, Connection conn) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                LOGGER.warn("fail to close ResultSet", e);
            }
        }

        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                LOGGER.warn("fail to close Statement", e);
            }
        }

        closeConnection(conn);
    }


    private void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                LOGGER.warn("fail to close Connection", e);
            }
        }
    }
}
