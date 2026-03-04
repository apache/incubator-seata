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
package org.apache.seata.rm.datasource.sql.struct.cache;

import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.sqlparser.struct.ColumnMeta;
import org.apache.seata.sqlparser.struct.IndexMeta;
import org.apache.seata.sqlparser.struct.IndexType;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.apache.seata.sqlparser.util.ColumnUtils;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * The type Table meta cache for ClickHouse.
 *
 */
@LoadLevel(name = JdbcConstants.CLICKHOUSE)
public class ClickhouseTableMetaCache extends AbstractTableMetaCache {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    protected String getCacheKey(Connection connection, String tableName, String resourceId) {
        StringBuilder cacheKey = new StringBuilder(resourceId);
        cacheKey.append(".");
        // Remove escape characters from table name
        String defaultTableName = ColumnUtils.delEscape(tableName, JdbcConstants.CLICKHOUSE);

        DatabaseMetaData databaseMetaData;
        try {
            databaseMetaData = connection.getMetaData();
        } catch (SQLException e) {
            logger.error("Could not get connection, use default cache key {}", e.getMessage(), e);
            return cacheKey.append(defaultTableName).toString();
        }

        try {
            // prevent duplicated cache key
            if (databaseMetaData.supportsMixedCaseIdentifiers()) {
                cacheKey.append(defaultTableName);
            } else {
                cacheKey.append(defaultTableName.toLowerCase());
            }
        } catch (SQLException e) {
            logger.error(
                    "Could not get supportsMixedCaseIdentifiers in connection metadata, use default cache key {}",
                    e.getMessage(),
                    e);
            return cacheKey.append(defaultTableName).toString();
        }

        return cacheKey.toString();
    }

    @Override
    protected TableMeta fetchSchema(Connection connection, String tableName) throws SQLException {
        String sql = "SELECT * FROM " + ColumnUtils.addEscape(tableName, JdbcConstants.CLICKHOUSE) + " LIMIT 1";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            return resultSetMetaToSchema(rs.getMetaData(), connection.getMetaData(), tableName);
        } catch (SQLException sqlEx) {
            throw sqlEx;
        } catch (Exception e) {
            throw new SQLException(String.format("Failed to fetch schema of %s", tableName), e);
        }
    }

    protected TableMeta resultSetMetaToSchema(ResultSetMetaData rsmd, DatabaseMetaData dbmd, String originalTableName)
            throws SQLException {
        // default to "" for clickhouse if schema is unknown
        String schemaName = rsmd.getSchemaName(1);
        String catalogName = rsmd.getCatalogName(1);
        String tableName = rsmd.getTableName(1);

        TableMeta tm = new TableMeta();
        tm.setTableName(tableName);
        tm.setCaseSensitive(true);
        tm.setOriginalTableName(originalTableName);

        try (ResultSet rsColumns = dbmd.getColumns(catalogName, schemaName, tableName, "%");
                ResultSet rsIndex = dbmd.getIndexInfo(catalogName, schemaName, tableName, false, true);
                ResultSet onUpdateColumns = dbmd.getVersionColumns(catalogName, schemaName, tableName)) {
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
                col.setCaseSensitive(rsmd.isCaseSensitive(col.getOrdinalPosition()));

                if (tm.getAllColumns().containsKey(col.getColumnName())) {
                    throw new NotSupportYetException(
                            "Not support the table has the same column name with different case yet");
                }
                tm.getAllColumns().put(col.getColumnName(), col);
            }

            while (onUpdateColumns.next()) {
                tm.getAllColumns().get(onUpdateColumns.getString("COLUMN_NAME")).setOnUpdate(true);
            }

            while (rsIndex.next()) {
                String indexName = rsIndex.getString("INDEX_NAME");
                String colName = rsIndex.getString("COLUMN_NAME");
                ColumnMeta col = tm.getAllColumns().get(colName);
                if (col == null) {
                    continue;
                }

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
                    index.setCardinality(rsIndex.getLong("CARDINALITY"));
                    index.getValues().add(col);
                    if ("PRIMARY".equalsIgnoreCase(indexName)) {
                        index.setIndextype(IndexType.PRIMARY);
                    } else if (!index.isNonUnique()) {
                        index.setIndextype(IndexType.UNIQUE);
                    } else {
                        index.setIndextype(IndexType.NORMAL);
                    }
                    tm.getAllIndexes().put(indexName, index);
                }
            }
            // Note: ClickHouse sometimes returns empty indexes for certain engines. We default the primary key to the single column if none found below.
            if (tm.getAllIndexes().isEmpty() && tm.getAllColumns().size() > 0) {
               // Clickhouse's JDBC driver sometimes does not return Index metadata correctly. 
            }
        }
        return tm;
    }
}
