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


import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.sqlparser.struct.ColumnMeta;
import org.apache.seata.sqlparser.struct.IndexMeta;
import org.apache.seata.sqlparser.struct.IndexType;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.apache.seata.sqlparser.util.ColumnUtils;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author qingjiusanliangsan
 * @author GoodBoyCoder
 */
@LoadLevel(name = JdbcConstants.DB2)
public class DB2TableMetaCache extends AbstractTableMetaCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(DB2TableMetaCache.class);

    @Override
    protected String getCacheKey(Connection connection, String tableName, String resourceId) {
        StringBuilder cacheKey = new StringBuilder(resourceId);
        cacheKey.append(".");
        //remove single quote and separate it to catalogName and tableName
        String[] tableNameWithSchema = tableName.split("\\.");
        String defaultTableName = tableNameWithSchema[tableNameWithSchema.length - 1];

        if (defaultTableName.contains("\"")) {
            defaultTableName = defaultTableName.replace("\"", "");
        }
        DatabaseMetaData databaseMetaData;
        try {
            databaseMetaData = connection.getMetaData();
        } catch (SQLException e) {
            LOGGER.error("Could not get connection, use default cache key {}", e.getMessage(), e);
            return cacheKey.append(defaultTableName).toString();
        }

        try {
            //prevent duplicated cache key
            if (databaseMetaData.supportsMixedCaseIdentifiers()) {
                cacheKey.append(defaultTableName);
            } else {
                cacheKey.append(defaultTableName.toUpperCase());
            }
        } catch (SQLException e) {
            LOGGER.error("Could not get supportsMixedCaseIdentifiers in connection metadata, use default cache key {}", e.getMessage(), e);
            return cacheKey.append(defaultTableName).toString();
        }

        return cacheKey.toString();
    }

    @Override
    protected TableMeta fetchSchema(Connection connection, String tableName) throws SQLException {
        try {
            return resultSetMetaToSchema(connection, tableName);
        } catch (SQLException sqlEx) {
            throw sqlEx;
        } catch (Exception e) {
            throw new SQLException(String.format("Failed to fetch schema of %s", tableName), e);
        }
    }

    private TableMeta resultSetMetaToSchema(Connection connection, String tableName)
            throws SQLException {
        TableMeta tm = new TableMeta();
        tm.setTableName(tableName);

        tableName = ColumnUtils.delEscape(tableName, JdbcConstants.DB2);
        String[] schemaTable = tableName.split("\\.");
        //get catalog and schema from tableName
        String catalogName = "";
        String schemaName = "";
        if (schemaTable.length > 2) {
            catalogName = schemaTable[schemaTable.length - 3];
        } else if (schemaTable.length > 1) {
            schemaName = schemaTable[schemaTable.length - 2];
        }

        //If the tableName does not contain the required information, get from connection
        if (StringUtils.isBlank(catalogName)) {
            catalogName = connection.getCatalog();
        }
        if (StringUtils.isBlank(schemaName)) {
            schemaName = connection.getSchema();
        }

        DatabaseMetaData metaData = connection.getMetaData();
        String pureTableName = schemaTable[schemaTable.length - 1];
        if (metaData.storesLowerCaseIdentifiers()) {
            pureTableName = pureTableName.toLowerCase();
        } else if (metaData.storesUpperCaseIdentifiers()) {
            pureTableName = pureTableName.toUpperCase();
        }

        String querySql = "SELECT IDENTITY, GENERATED FROM SYSCAT.COLUMNS where TABSCHEMA = ? AND TABNAME = ?";
        try (PreparedStatement statement = connection.prepareStatement(querySql)) {
            statement.setString(1, schemaName);
            statement.setString(2, pureTableName);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                if ("Y".equals(resultSet.getString("IDENTITY")) &&
                        "A".equals(resultSet.getString("GENERATED"))) {
                    throw new SQLException("Identity column of type generated always as identity is not supported." +
                            " If necessary, it is recommended to use BY DEFAULT");
                }
            }
        }

        try (ResultSet rsColumns = metaData.getColumns(catalogName, schemaName, pureTableName, "%");
             ResultSet rsIndex = metaData.getIndexInfo(catalogName, schemaName, pureTableName, false, true);
             ResultSet rsPrimary = metaData.getPrimaryKeys(catalogName, schemaName, pureTableName)) {
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

                if (tm.getAllColumns().containsKey(col.getColumnName())) {
                    throw new NotSupportYetException("Not support the table has the same column name with different case yet");
                }
                tm.getAllColumns().put(col.getColumnName(), col);
            }

            while (rsIndex.next()) {
                String indexName = rsIndex.getString("INDEX_NAME");
                if (StringUtils.isNullOrEmpty(indexName)) {
                    continue;
                }

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
                    if (!index.isNonUnique()) {
                        index.setIndextype(IndexType.UNIQUE);
                    } else {
                        index.setIndextype(IndexType.NORMAL);
                    }
                    tm.getAllIndexes().put(indexName, index);
                }
            }

            while (rsPrimary.next()) {
                String pkIndexName = rsPrimary.getString("PK_NAME");
                if (tm.getAllIndexes().containsKey(pkIndexName)) {
                    tm.getAllIndexes().get(pkIndexName).setIndextype(IndexType.PRIMARY);
                }
            }
            if (tm.getAllIndexes().isEmpty()) {
                throw new ShouldNeverHappenException("Could not found any index in the table: " + tableName);
            }
        }
        return tm;
    }
}