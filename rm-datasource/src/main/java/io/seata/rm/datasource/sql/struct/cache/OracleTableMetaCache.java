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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The type Table meta cache.
 *
 * @author ygy
 */
@LoadLevel(name = JdbcConstants.ORACLE)
public class OracleTableMetaCache extends AbstractTableMetaCache {

    @Override
    protected String getCacheKey(Connection connection, String tableName, String resourceId) {
        StringBuilder cacheKey = new StringBuilder(resourceId);
        cacheKey.append(".");

        //separate it to schemaName and tableName
        String[] tableNameWithSchema = tableName.split("\\.");
        String defaultTableName = tableNameWithSchema.length > 1 ? tableNameWithSchema[1] : tableNameWithSchema[0];

        //oracle does not implement supportsMixedCaseIdentifiers in DatabaseMetadata
        if (defaultTableName.contains("\"")) {
            cacheKey.append(defaultTableName.replace("\"", ""));
        } else {
            // oracle default store in upper case
            cacheKey.append(defaultTableName.toUpperCase());
        }

        return cacheKey.toString();
    }

    @Override
    protected TableMeta fetchSchema(Connection connection, String tableName) throws SQLException {
        try {
            return resultSetMetaToSchema(connection.getMetaData(), tableName);
        } catch (SQLException sqlEx) {
            throw sqlEx;
        } catch (Exception e) {
            throw new SQLException(String.format("Failed to fetch schema of %s", tableName), e);
        }
    }

    protected TableMeta resultSetMetaToSchema(DatabaseMetaData dbmd, String tableName) throws SQLException {
        TableMeta tm = new TableMeta();
        tm.setTableName(tableName);
        String[] schemaTable = tableName.split("\\.");
        String schemaName = schemaTable.length > 1 ? schemaTable[0] : dbmd.getUserName();
        tableName = schemaTable.length > 1 ? schemaTable[1] : tableName;
        if (schemaName.contains("\"")) {
            schemaName = schemaName.replace("\"", "");
        } else {
            schemaName = schemaName.toUpperCase();
        }

        if (tableName.contains("\"")) {
            tableName = tableName.replace("\"", "");

        } else {
            tableName = tableName.toUpperCase();
        }
        tm.setCaseSensitive(StringUtils.hasLowerCase(tableName));

        try (ResultSet rsColumns = dbmd.getColumns("", schemaName, tableName, "%");
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
                col.setCharOctetLength(rsColumns.getInt("CHAR_OCTET_LENGTH"));
                col.setOrdinalPosition(rsColumns.getInt("ORDINAL_POSITION"));
                col.setIsNullAble(rsColumns.getString("IS_NULLABLE"));
                col.setCaseSensitive(StringUtils.hasLowerCase(col.getColumnName()));

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
                    index.setCardinality(rsIndex.getLong("CARDINALITY"));
                    index.getValues().add(col);
                    if (!index.isNonUnique()) {
                        index.setIndextype(IndexType.UNIQUE);
                    } else {
                        index.setIndextype(IndexType.NORMAL);
                    }
                    tm.getAllIndexes().put(indexName, index);

                }
            }
            if (tm.getAllIndexes().isEmpty()) {
                throw new ShouldNeverHappenException(String.format("Could not found any index in the table: %s", tableName));
            }
            // when we create a primary key constraint oracle will uses and existing unique index.
            // if we create a unique index before create a primary constraint in the same column will cause the problem
            // that primary key constraint name was different from the unique name.
            List<String> pkcol = new ArrayList<>();
            while (rsPrimary.next()) {
                String pkConstraintName = rsPrimary.getString("PK_NAME");
                if (tm.getAllIndexes().containsKey(pkConstraintName)) {
                    IndexMeta index = tm.getAllIndexes().get(pkConstraintName);
                    index.setIndextype(IndexType.PRIMARY);
                } else {
                    //save the columns that constraint primary key name was different from unique index name
                    pkcol.add(rsPrimary.getString("COLUMN_NAME"));
                }
            }
            //find the index that belong to the primary key constraint
            if (!pkcol.isEmpty()) {
                int matchCols = 0;
                for (Map.Entry<String, IndexMeta> entry : tm.getAllIndexes().entrySet()) {
                    IndexMeta index = entry.getValue();
                    // only the unique index and all the unique index's columes same as primary key columes,
                    // it belongs to primary key
                    if (index.getIndextype().value() == IndexType.UNIQUE.value()) {
                        for (ColumnMeta col : index.getValues()) {
                            if (pkcol.contains(col.getColumnName())) {
                                matchCols++;
                            }
                        }
                        if (matchCols == pkcol.size()) {
                            index.setIndextype(IndexType.PRIMARY);
                            // each table only has one primary key
                            break;
                        } else {
                            matchCols = 0;
                        }
                    }
                }
            }
        }
        return tm;
    }
}
