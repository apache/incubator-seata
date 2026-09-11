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
package io.seata.rm.datasource.sql.struct.cache;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import io.seata.common.exception.NotSupportYetException;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.loader.LoadLevel;
import io.seata.common.util.StringUtils;
import io.seata.sqlparser.struct.ColumnMeta;
import io.seata.sqlparser.struct.IndexMeta;
import io.seata.sqlparser.struct.IndexType;
import io.seata.sqlparser.struct.TableMeta;
import io.seata.sqlparser.util.JdbcConstants;

/**
 * The type Table meta cache.
 *
 * @author jaspercloud
 */
@LoadLevel(name = JdbcConstants.POSTGRESQL)
public class PostgresqlTableMetaCache extends AbstractTableMetaCache {

    @Override
    protected String getCacheKey(Connection connection, String tableName, String resourceId) {
        StringBuilder cacheKey = new StringBuilder(resourceId);
        cacheKey.append(".");

        //separate it to schemaName and tableName
        String[] tableNameWithSchema = tableName.split("\\.");
        String defaultTableName = tableNameWithSchema.length > 1 ? tableNameWithSchema[1] : tableNameWithSchema[0];

        //postgres does not implement supportsMixedCaseIdentifiers in DatabaseMetadata
        if (defaultTableName.contains("\"")) {
            cacheKey.append(defaultTableName.replace("\"", ""));
        } else {
            //postgres default store in lower case
            cacheKey.append(defaultTableName.toLowerCase());
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
            throw new SQLException("Failed to fetch schema of " + tableName, e);
        }
    }

    private TableMeta resultSetMetaToSchema(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData dbmd = connection.getMetaData();
        TableMeta tm = new TableMeta();
        tm.setTableName(tableName);
        String[] schemaTable = tableName.split("\\.");
        String schemaName = schemaTable.length > 1 ? schemaTable[0] : null;
        tableName = schemaTable.length > 1 ? schemaTable[1] : tableName;
        /*
         * use ResultSetMetaData to get the pure table name
         * can avoid the problem below
         *
         * select * from account_tbl
         * select * from account_TBL
         * select * from account_tbl
         * select * from account.account_tbl
         * select * from "select"
         * select * from "Select"
         * select * from "Sel""ect"
         * select * from "Sel'ect"
         * select * from TEST.test
         * select * from test.TEST
         * select * from "Test".test
         * select * from "Test"."Select"
         */
        if (schemaName != null) {
            if (schemaName.startsWith("\"") && schemaName.endsWith("\"")) {
                schemaName = schemaName.replaceAll("(^\")|(\"$)", "");
            } else {
                schemaName = schemaName.toLowerCase();
            }
        } else {
            schemaName = connection.getSchema();
        }

        if (tableName.startsWith("\"") && tableName.endsWith("\"")) {
            tableName = tableName.replaceAll("(^\")|(\"$)", "");
        } else {
            tableName = tableName.toLowerCase();
        }
        tm.setCaseSensitive(StringUtils.hasUpperCase(tableName));

        try (ResultSet rsColumns = dbmd.getColumns(null, schemaName, tableName, "%");
             ResultSet rsIndex = dbmd.getIndexInfo(null, schemaName, tableName, false, true);
             ResultSet rsPrimary = dbmd.getPrimaryKeys(null, schemaName, tableName)) {
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
                col.setCharOctetLength(rsColumns.getObject("CHAR_OCTET_LENGTH"));
                col.setOrdinalPosition(rsColumns.getInt("ORDINAL_POSITION"));
                col.setIsNullAble(rsColumns.getString("IS_NULLABLE"));
                col.setIsAutoincrement(rsColumns.getString("IS_AUTOINCREMENT"));
                col.setCaseSensitive(StringUtils.hasUpperCase(col.getColumnName()));

                if (tm.getAllColumns().containsKey(col.getColumnName())) {
                    throw new NotSupportYetException("Not support the table has the same column name with different case yet");
                }
                tm.getAllColumns().put(col.getColumnName(), col);
            }

            while (rsIndex.next()) {
                String indexName = rsIndex.getString("index_name");
                if (StringUtils.isNullOrEmpty(indexName)) {
                    continue;
                }
                String colName = rsIndex.getString("column_name");
                ColumnMeta col = tm.getAllColumns().get(colName);
                if (tm.getAllIndexes().containsKey(indexName)) {
                    IndexMeta index = tm.getAllIndexes().get(indexName);
                    index.getValues().add(col);
                } else {
                    IndexMeta index = new IndexMeta();
                    index.setIndexName(indexName);
                    index.setNonUnique(rsIndex.getBoolean("non_unique"));
                    index.setIndexQualifier(rsIndex.getString("index_qualifier"));
                    index.setIndexName(rsIndex.getString("index_name"));
                    index.setType(rsIndex.getShort("type"));
                    index.setOrdinalPosition(rsIndex.getShort("ordinal_position"));
                    index.setAscOrDesc(rsIndex.getString("asc_or_desc"));
                    index.setCardinality(rsIndex.getLong("cardinality"));
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
                String pkIndexName = rsPrimary.getString("pk_name");
                if (tm.getAllIndexes().containsKey(pkIndexName)) {
                    IndexMeta index = tm.getAllIndexes().get(pkIndexName);
                    index.setIndextype(IndexType.PRIMARY);
                }
            }
            if (tm.getAllIndexes().isEmpty()) {
                throw new ShouldNeverHappenException("Could not found any index in the table: " + tableName);
            }
        }

        return tm;
    }
}
