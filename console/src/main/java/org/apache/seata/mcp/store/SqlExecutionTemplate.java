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
import org.apache.seata.common.result.PageResult;
import org.apache.seata.common.util.IOUtil;
import org.apache.seata.common.util.PageUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.mcp.entity.vo.UndoLogVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class SqlExecutionTemplate {

    private static final Pattern SELECT_PATTERN =
            Pattern.compile("^\\s*SELECT\\b.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlExecutionTemplate.class);

    private DataSource getDataSource(String resourceId) {
        try {
            return DataSourceFactory.getDataSource(resourceId);
        } catch (Exception e) {
            LOGGER.error("Failed to get the data source, resourceId: {}", resourceId, e);
            throw new StoreException("Unable to get the data source: " + resourceId);
        }
    }

    private boolean validateQuerySql(String sql) {
        if (sql == null || StringUtils.isBlank(sql)) {
            return false;
        }
        return SELECT_PATTERN.matcher(sql).matches();
    }

    public List<Map<String, Object>> query(String resourceId, String sql, Object... params) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            if (!validateQuerySql(sql)) {
                throw new StoreException("The query valid failed,Only query operations are allowed：" + sql);
            }
            conn = getConnection(resourceId);
            if (params == null || params.length == 0) {
                if ((sql.contains("where") || sql.contains("WHERE"))) {
                    throw new StoreException(
                            "Query contains WHERE clause but no parameters were provided. This may lead to unintended full table scans and is not allowed.");
                }
            }
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
            LOGGER.info("User query business datasource with sql: {}", sql);
            closeResources(rs, ps, conn);
        }
    }

    public PageResult<UndoLogVO> queryForUndoLogs(
            String resourceId, String sql, Integer pageNum, Integer pageSize, Object... params) {
        Connection conn = null;
        PreparedStatement ps = null;
        PreparedStatement countPs = null;
        ResultSet rs = null;
        ResultSet countRs = null;
        List<UndoLogVO> data = new ArrayList<>();
        int count = 0;

        try {
            if (!validateQuerySql(sql)) {
                throw new StoreException("The query valid failed,Only query operations are allowed：" + sql);
            }
            conn = getConnection(resourceId);
            if (params == null || params.length == 0) {
                if ((sql.contains("where") || sql.contains("WHERE"))) {
                    sql = sql.replaceAll("(?i)\\bWHERE\\b.*", "").trim();
                }
            }
            ps = conn.prepareStatement(sql);
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
            }

            rs = ps.executeQuery();
            while (rs.next()) {
                data.add(UndoLogVO.convert(rs));
            }

            // count query
            sql = PageUtil.countSql(sql, "mysql");
            countPs = conn.prepareStatement(sql);
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    countPs.setObject(i + 1, params[i]);
                }
            }
            countRs = countPs.executeQuery();
            while (countRs.next()) {
                count = countRs.getInt("count(1)");
            }
            return PageResult.success(data, count, pageNum, pageSize);
        } catch (SQLException e) {
            LOGGER.error("The query failed, resourceId: {}, sql: {}", resourceId, sql, e);
            throw new StoreException("The query execution failed: " + e.getMessage());
        } finally {
            LOGGER.info("User query business datasource with sql: {}", sql);
            IOUtil.close(rs, countPs, ps, conn);
        }
    }

    public Map<String, Object> queryForObject(String resourceId, String sql, Object... params) {
        List<Map<String, Object>> results = query(resourceId, sql, params);
        return results.isEmpty() ? null : results.get(0);
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
            LOGGER.error("Get The Business DataSource Connection: {} failed due to: {}", resourceId, e.getMessage());
            DataSourceFactory.removeErrorDataSource(resourceId, e);
            throw new StoreException(e);
        }
    }
}
