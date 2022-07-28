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

import io.seata.common.exception.NotSupportYetException;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.loader.LoadLevel;
import io.seata.common.util.StringUtils;
import io.seata.rm.datasource.ColumnUtils;
import io.seata.rm.datasource.sql.struct.ColumnMeta;
import io.seata.rm.datasource.sql.struct.IndexMeta;
import io.seata.rm.datasource.sql.struct.IndexType;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.sqlparser.util.JdbcConstants;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Cache holder of table mata for OceanBaseOracle
 *
 * @author hsien999
 */
@LoadLevel(name = JdbcConstants.OCEANBASE_ORACLE)
public class OceanBaseOracleTableMetaCache extends AbstractTableMetaCache {

    @Override
    protected String getCacheKey(Connection connection, String tableName, String resourceId) {
        StringBuilder cacheKey = new StringBuilder(resourceId);
        cacheKey.append(".");
        // split `tableName` into schema name and table name
        String[] tableNameWithSchema = tableName.split("\\.");
        String defaultTableName = tableNameWithSchema[tableNameWithSchema.length - 1];
        // get unique table name by sensitivity
        cacheKey.append(getUniqueNameBySensitivity(defaultTableName));
        return cacheKey.toString();
    }

    @Override
    protected TableMeta fetchSchema(Connection connection, String tableName) throws SQLException {
        try {
            DatabaseMetaData dbMeta = connection.getMetaData();
            TableMeta tm = new TableMeta();
            // use origin table name
            tm.setTableName(tableName);

            // in oracle, default schema name = user name
            String[] schemaTable = tableName.split("\\.");
            String schemaName = schemaTable.length > 1 ? schemaTable[0] : dbMeta.getUserName();
            tableName = schemaTable.length > 1 ? schemaTable[1] : tableName;
            schemaName = getUniqueNameBySensitivity(schemaName);
            tableName = getUniqueNameBySensitivity(tableName);

            // catalog = "" retrieves descriptions without a catalog,
            // null means that the catalog name should not be used to narrow the search
            try (ResultSet rsColumns = dbMeta.getColumns(null, schemaName, tableName, "%");
                 ResultSet rsIndexes = dbMeta.getIndexInfo(null, schemaName, tableName, false, true);
                 ResultSet rsPks = dbMeta.getPrimaryKeys(null, schemaName, tableName)) {

                // 1. retrieves columns meta
                final Map<String, ColumnMeta> allColumns = tm.getAllColumns();
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

                    if (allColumns.containsKey(col.getColumnName())) {
                        throw new NotSupportYetException("Not support the table has the same column name with different case yet");
                    }
                    allColumns.put(col.getColumnName(), col);
                }
                if (allColumns.isEmpty()) {
                    throw new ShouldNeverHappenException(String.format("Could not find any columns in the table: %s", tableName));
                }

                // 2. retrieves index meta
                final Map<String, IndexMeta> allIndexes = tm.getAllIndexes();
                while (rsIndexes.next()) {
                    String indexName = rsIndexes.getString("INDEX_NAME");
                    if (StringUtils.isEmpty(indexName)) {
                        continue;
                    }
                    String colName = rsIndexes.getString("COLUMN_NAME");
                    ColumnMeta colMeta = allColumns.get(colName);

                    IndexMeta index;
                    if ((index = allIndexes.get(indexName)) != null) {
                        index.getValues().add(colMeta);
                    } else {
                        index = new IndexMeta();
                        index.setIndexName(indexName);
                        index.setNonUnique(rsIndexes.getBoolean("NON_UNIQUE"));
                        index.setIndexQualifier(rsIndexes.getString("INDEX_QUALIFIER"));
                        index.setIndexName(rsIndexes.getString("INDEX_NAME"));
                        index.setType(rsIndexes.getShort("TYPE"));
                        index.setOrdinalPosition(rsIndexes.getShort("ORDINAL_POSITION"));
                        index.setAscOrDesc(rsIndexes.getString("ASC_OR_DESC"));
                        index.setCardinality(rsIndexes.getInt("CARDINALITY"));
                        index.getValues().add(colMeta);
                        if (!index.isNonUnique()) {
                            index.setIndextype(IndexType.UNIQUE);
                        } else {
                            index.setIndextype(IndexType.NORMAL);
                        }
                        allIndexes.put(indexName, index);
                    }
                }
                if (allIndexes.isEmpty()) {
                    throw new ShouldNeverHappenException(String.format("Could not find any index in the table: %s", tableName));
                }

                // 1. create pk => set unique index on the pk columns by the same pk constraint
                // 2. create unique index, then create pk constraint on those columns => has different index names
                Set<String> pkNotIndexCols = new HashSet<>();
                while (rsPks.next()) {
                    String pkConstraintName = rsPks.getString("PK_NAME");
                    IndexMeta index;
                    if ((index = allIndexes.get(pkConstraintName)) != null) {
                        index.setIndextype(IndexType.PRIMARY);
                    } else {
                        pkNotIndexCols.add(rsPks.getString("COLUMN_NAME"));
                    }
                }

                // find the index that belong to the primary key constraint
                if (!pkNotIndexCols.isEmpty()) {
                    for (Map.Entry<String, IndexMeta> entry : allIndexes.entrySet()) {
                        IndexMeta index = entry.getValue();
                        int matchCols = 0;
                        if (index.getIndextype() == IndexType.UNIQUE) {
                            for (ColumnMeta col : index.getValues()) {
                                if (pkNotIndexCols.contains(col.getColumnName())) {
                                    matchCols++;
                                }
                            }
                            if (matchCols == pkNotIndexCols.size()) {
                                // if the pk constraint and the index have the same columns
                                index.setIndextype(IndexType.PRIMARY);
                                // each table has one primary key constraint only
                                break;
                            }
                        }
                    }
                }
            }
            return tm;
        } catch (SQLException sqlEx) {
            throw sqlEx;
        } catch (Exception e) {
            throw new SQLException(String.format("Failed to fetch schema of %s", tableName), e);
        }
    }

    private String getUniqueNameBySensitivity(String identifier) {
        // in oracle, just support like: "table" "Table" table etc.
        // (invalid: "ta"ble" "table'" etc.)
        String escape = String.valueOf(ColumnUtils.Escape.STANDARD);
        if (identifier.contains(escape)) {
            // 1. with escapes(quotation marks): case-sensitive
            return identifier.replace(escape, "");
        } else {
            // 2. default: case-insensitive
            return identifier.toUpperCase();
        }
    }
}
