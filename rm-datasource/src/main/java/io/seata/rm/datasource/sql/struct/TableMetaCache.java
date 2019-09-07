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
package io.seata.rm.datasource.sql.struct;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.StringUtils;
import io.seata.core.context.RootContext;
import io.seata.rm.datasource.AbstractConnectionProxy;
import io.seata.rm.datasource.DataSourceProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Table meta cache.
 *
 * @author sharajava
 */
public class TableMetaCache {

    private static final long CACHE_SIZE = 100000;

    private static final long EXPIRE_TIME = 900 * 1000;

    private static final Cache<String, TableMeta> TABLE_META_CACHE = Caffeine.newBuilder().maximumSize(CACHE_SIZE)
        .expireAfterWrite(EXPIRE_TIME, TimeUnit.MILLISECONDS).softValues().build();

    private static final Logger LOGGER = LoggerFactory.getLogger(TableMetaCache.class);

    /**
     * Gets table meta.
     *
     * @param dataSourceProxy the data source proxy
     * @param tableName       the table name
     * @return the table meta
     */
    public static TableMeta getTableMeta(final DataSourceProxy dataSourceProxy, final String tableName) {
        if (StringUtils.isNullOrEmpty(tableName)) {
            throw new IllegalArgumentException("TableMeta cannot be fetched without tableName");
        }

        TableMeta tmeta;
        final String key = getCacheKey(dataSourceProxy, tableName);
        tmeta = TABLE_META_CACHE.get(key, mappingFunction -> {
            try {
                return fetchSchema(dataSourceProxy.getTargetDataSource(), tableName);
            } catch (SQLException e) {
                LOGGER.error("get cache error:{}", e.getMessage(), e);
                return null;
            }
        });

        if (tmeta == null) {
            try {
                tmeta = fetchSchema(dataSourceProxy.getTargetDataSource(), tableName);
            } catch (SQLException e) {
                LOGGER.error("get table meta error:{}", e.getMessage(), e);
            }
        }

        if (tmeta == null) {
            throw new ShouldNeverHappenException(String.format("[xid:%s]get tablemeta failed", RootContext.getXID()));
        }
        return tmeta;
    }

    /**
     * Clear the table meta cache
     *
     * @param dataSourceProxy
     */
    public static void refresh(final DataSourceProxy dataSourceProxy) {
        ConcurrentMap<String, TableMeta> tableMetaMap = TABLE_META_CACHE.asMap();
        for (Entry<String, TableMeta> entry : tableMetaMap.entrySet()) {
            String key = getCacheKey(dataSourceProxy, entry.getValue().getTableName());
            if (entry.getKey().equals(key)) {
                try {
                    TableMeta tableMeta = fetchSchema(dataSourceProxy, entry.getValue().getTableName());
                    if (!tableMeta.equals(entry.getValue())) {
                        TABLE_META_CACHE.put(entry.getKey(), tableMeta);
                        LOGGER.info("table meta change was found, update table meta cache automatically.");
                    }
                } catch (SQLException e) {
                    LOGGER.error("get table meta error:{}", e.getMessage(), e);
                }
            }
        }
    }

    private static TableMeta fetchSchema(DataSource dataSource, String tableName) throws SQLException {
        return fetchSchemeInDefaultWay(dataSource, tableName);
    }

    private static TableMeta fetchSchemeInDefaultWay(DataSource dataSource, String tableName) throws SQLException {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getConnection();
            stmt = conn.createStatement();
            StringBuilder builder = new StringBuilder("SELECT * FROM ");
            builder.append(tableName);
            builder.append(" LIMIT 1");
            rs = stmt.executeQuery(builder.toString());
            ResultSetMetaData rsmd = rs.getMetaData();
            DatabaseMetaData dbmd = conn.getMetaData();

            return resultSetMetaToSchema(rsmd, dbmd, tableName);
        } catch (Exception e) {
            if (e instanceof SQLException) {
                throw e;
            }
            throw new SQLException("Failed to fetch schema of " + tableName, e);

        } finally {
            if (rs != null) {
                rs.close();
            }
            if (stmt != null) {
                stmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
    }

    private static TableMeta resultSetMetaToSchema(java.sql.ResultSet rs2, AbstractConnectionProxy conn,
                                                   String tableName) throws SQLException {
        TableMeta tm = new TableMeta();
        tm.setTableName(tableName);
        while (rs2.next()) {
            ColumnMeta col = new ColumnMeta();
            col.setTableName(tableName);
            col.setColumnName(rs2.getString("COLUMN_NAME"));
            String datatype = rs2.getString("DATA_TYPE");
            if (StringUtils.equalsIgnoreCase(datatype, "NUMBER")) {
                col.setDataType(java.sql.Types.BIGINT);
            } else if (StringUtils.equalsIgnoreCase(datatype, "VARCHAR2")) {
                col.setDataType(java.sql.Types.VARCHAR);
            } else if (StringUtils.equalsIgnoreCase(datatype, "CHAR")) {
                col.setDataType(java.sql.Types.CHAR);
            } else if (StringUtils.equalsIgnoreCase(datatype, "DATE")) {
                col.setDataType(java.sql.Types.DATE);
            }

            col.setColumnSize(rs2.getInt("DATA_LENGTH"));

            tm.getAllColumns().put(col.getColumnName(), col);
        }

        java.sql.Statement stmt = null;
        java.sql.ResultSet rs1 = null;
        try {
            stmt = conn.getTargetConnection().createStatement();
            rs1 = stmt.executeQuery(
                "select a.constraint_name,  a.column_name from user_cons_columns a, user_constraints b  where a"
                    + ".constraint_name = b.constraint_name and b.constraint_type = 'P' and a.table_name ='" + tableName
                    + "'");
            while (rs1.next()) {
                String indexName = rs1.getString(1);
                String colName = rs1.getString(2);
                ColumnMeta col = tm.getAllColumns().get(colName);

                if (tm.getAllIndexes().containsKey(indexName)) {
                    IndexMeta index = tm.getAllIndexes().get(indexName);
                    index.getValues().add(col);
                } else {
                    IndexMeta index = new IndexMeta();
                    index.setIndexName(indexName);
                    index.getValues().add(col);
                    index.setIndextype(IndexType.PRIMARY);
                    tm.getAllIndexes().put(indexName, index);

                }
            }
        } finally {
            if (rs1 != null) {
                rs1.close();
            }
            if (stmt != null) {
                stmt.close();
            }
        }

        return tm;
    }

    private static TableMeta resultSetMetaToSchema(ResultSetMetaData rsmd, DatabaseMetaData dbmd, String tableName)
        throws SQLException {
        String schemaName = rsmd.getSchemaName(1);
        String catalogName = rsmd.getCatalogName(1);

        TableMeta tm = new TableMeta();
        tm.setTableName(tableName);

        ResultSet rsColumns = dbmd.getColumns(catalogName, schemaName, tableName, "%");
        ResultSet rsIndex = dbmd.getIndexInfo(catalogName, schemaName, tableName, false, true);

        try {
            while (rsColumns.next()) {
                ColumnMeta col = new ColumnMeta();
                col.setTableCat(rsColumns.getString("TABLE_CAT"));
                col.setTableSchemaName(rsColumns.getString("TABLE_SCHEM"));
                col.setTableName(rsColumns.getString("TABLE_NAME"));
                col.setColumnName(rsColumns.getString("COLUMN_NAME"));
                col.setDataType(rsColumns.getInt("DATA_TYPE"));
                col.setDataTypeName(rsColumns.getString("TYPE_NAME"));
                col.setColumnSize(rsColumns.getInt("COLUMN_SIZE"));
                col.setDecimalDigits(rsColumns.getInt("DECIMAL_DIGITS"));
                col.setNumPrecRadix(rsColumns.getInt("NUM_PREC_RADIX"));
                col.setNullAble(rsColumns.getInt("NULLABLE"));
                col.setRemarks(rsColumns.getString("REMARKS"));
                col.setColumnDef(rsColumns.getString("COLUMN_DEF"));
                col.setSqlDataType(rsColumns.getInt("SQL_DATA_TYPE"));
                col.setSqlDatetimeSub(rsColumns.getInt("SQL_DATETIME_SUB"));
                col.setCharOctetLength(rsColumns.getInt("CHAR_OCTET_LENGTH"));
                col.setOrdinalPosition(rsColumns.getInt("ORDINAL_POSITION"));
                col.setIsNullAble(rsColumns.getString("IS_NULLABLE"));
                col.setIsAutoincrement(rsColumns.getString("IS_AUTOINCREMENT"));

                tm.getAllColumns().put(col.getColumnName(), col);
            }

            while (rsIndex.next()) {
                String indexName = rsIndex.getString("INDEX_NAME");
                String colName = rsIndex.getString("COLUMN_NAME");
                ColumnMeta col = tm.getAllColumns().get(colName);

                if (tm.getAllIndexes().containsKey(indexName)) {
                    IndexMeta index = tm.getAllIndexes().get(indexName);
                    index.getValues().add(col);
                } else {
                    IndexMeta index = new IndexMeta();
                    index.setIndexName(indexName);
                    index.setNonUnique(rsIndex.getBoolean("NON_UNIQUE"));
                    index.setIndexQualifier(rsIndex.getString("INDEX_QUALIFIER"));
                    index.setIndexName(rsIndex.getString("INDEX_NAME"));
                    index.setType(rsIndex.getShort("TYPE"));
                    index.setOrdinalPosition(rsIndex.getShort("ORDINAL_POSITION"));
                    index.setAscOrDesc(rsIndex.getString("ASC_OR_DESC"));
                    index.setCardinality(rsIndex.getInt("CARDINALITY"));
                    index.getValues().add(col);
                    if ("PRIMARY".equalsIgnoreCase(indexName)) {
                        index.setIndextype(IndexType.PRIMARY);
                    } else if (!index.isNonUnique()) {
                        index.setIndextype(IndexType.Unique);
                    } else {
                        index.setIndextype(IndexType.Normal);
                    }
                    tm.getAllIndexes().put(indexName, index);

                }
            }
            if (tm.getAllIndexes().isEmpty()) {
                throw new ShouldNeverHappenException("Could not found any index in the table: " + tableName);
            }
        } finally {
            if (rsColumns != null) {
                rsColumns.close();
            }
            if (rsIndex != null) {
                rsIndex.close();
            }
        }
        return tm;
    }

    /**
     * generate cache key
     *
     * @param dataSourceProxy
     * @param tableName
     * @return
     */
    private static String getCacheKey(DataSourceProxy dataSourceProxy, String tableName) {
        return dataSourceProxy.getResourceId() + "." + tableName;
    }
}
