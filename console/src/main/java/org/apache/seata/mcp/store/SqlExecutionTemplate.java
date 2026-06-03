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
package org.apache.seata.mcp.store;

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.mcp.entity.vo.BusinessQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SqlExecutionTemplate {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlExecutionTemplate.class);

    private final SqlSafetyValidator sqlSafetyValidator;

    @Value("${seata.mcp.query.max-rows:500}")
    private int maxRows = 500;

    @Value("${seata.mcp.query.timeout-seconds:30}")
    private int timeoutSeconds = 30;

    @Value("${seata.mcp.query.fetch-size:100}")
    private int fetchSize = 100;

    public SqlExecutionTemplate(SqlSafetyValidator sqlSafetyValidator) {
        this.sqlSafetyValidator = sqlSafetyValidator;
    }

    private DataSource getDataSource(String resourceId) {
        try {
            return DataSourceFactory.getDataSource(resourceId);
        } catch (Exception e) {
            LOGGER.error("Failed to get the data source, resourceId: {}", resourceId);
            throw new StoreException("Unable to get the data source: " + resourceId);
        }
    }

    public BusinessQueryResult query(String resourceId, String sql, Object... params) {
        sqlSafetyValidator.validateMysqlSelect(sql);
        return doQuery(resourceId, sql, maxRows, true, params);
    }

    public BusinessQueryResult queryWithMaxRows(String resourceId, String sql, int queryMaxRows, Object... params) {
        sqlSafetyValidator.validateMysqlSelect(sql);
        int effectiveMaxRows = queryMaxRows <= 0 ? maxRows : Math.min(queryMaxRows, maxRows);
        return doQuery(resourceId, sql, effectiveMaxRows, true, params);
    }

    public BusinessQueryResult trustedQuery(String resourceId, String sql, Object... params) {
        return doQuery(resourceId, sql, maxRows, false, params);
    }

    private BusinessQueryResult doQuery(
            String resourceId, String sql, int effectiveMaxRows, boolean limitRows, Object... params) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        long start = System.currentTimeMillis();

        try {
            conn = getConnection(resourceId);
            conn.setReadOnly(true);
            ps = conn.prepareStatement(sql);
            ps.setQueryTimeout(timeoutSeconds);
            ps.setFetchSize(fetchSize);
            if (limitRows) {
                ps.setMaxRows(effectiveMaxRows + 1);
            }
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
            }

            rs = ps.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columns.add(metaData.getColumnLabel(i));
            }

            List<Map<String, Object>> results = new ArrayList<>();
            while (rs.next()) {
                if (limitRows && results.size() >= effectiveMaxRows) {
                    return buildResult(resourceId, columns, results, effectiveMaxRows, true, start);
                }
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = columns.get(i - 1);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                results.add(row);
            }

            return buildResult(resourceId, columns, results, effectiveMaxRows, false, start);
        } catch (SQLException e) {
            LOGGER.error("The query failed, resourceId: {}", resourceId);
            throw new StoreException("The query execution failed");
        } finally {
            closeResources(rs, ps, conn);
        }
    }

    public Map<String, Object> queryForObject(String resourceId, String sql, Object... params) {
        List<Map<String, Object>> results = query(resourceId, sql, params).getRows();
        return results.isEmpty() ? null : results.get(0);
    }

    private BusinessQueryResult buildResult(
            String resourceId,
            List<String> columns,
            List<Map<String, Object>> rows,
            int effectiveMaxRows,
            boolean truncated,
            long start) {
        BusinessQueryResult result = new BusinessQueryResult();
        result.setResourceId(resourceId);
        result.setColumns(columns);
        result.setRows(rows);
        result.setRowCount(rows.size());
        result.setTruncated(truncated);
        result.setMaxRows(effectiveMaxRows);
        result.setExecutionTimeMs(System.currentTimeMillis() - start);
        return result;
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

    private Connection getConnection(String resourceId) {
        try {
            return getDataSource(resourceId).getConnection();
        } catch (Exception e) {
            LOGGER.error("Get The Business DataSource Connection: {} failed", resourceId);
            DataSourceFactory.removeErrorDataSource(resourceId, e);
            throw new StoreException(e);
        }
    }
}
