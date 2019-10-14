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

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.StringUtils;
import io.seata.rm.datasource.sql.struct.ColumnMeta;
import io.seata.rm.datasource.sql.struct.IndexMeta;
import io.seata.rm.datasource.sql.struct.IndexType;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableMetaCache;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * The type Table meta cache.
 *
 * @author ygy
 */
public class OracleTableMetaCache extends AbstractTableMetaCache {

    private static volatile TableMetaCache tableMetaCache = null;

    private OracleTableMetaCache() {
    }

    /**
     * get instance of type MySQL keyword checker
     *
     * @return instance
     */
    public static TableMetaCache getInstance() {
        if (tableMetaCache == null) {
            synchronized (OracleTableMetaCache.class) {
                if (tableMetaCache == null) {
                    tableMetaCache = new OracleTableMetaCache();
                }
            }
        }
        return tableMetaCache;
    }

    @Override
    protected TableMeta fetchSchema(DataSource dataSource, String tableName) throws SQLException {
        Connection conn = null;
        java.sql.Statement stmt = null;
        try {
            conn = dataSource.getConnection();
            stmt = conn.createStatement();
            DatabaseMetaData dbmd = conn.getMetaData();
            return resultSetMetaToSchema(dbmd, tableName);
        } catch (Exception e) {
            if (e instanceof SQLException) {
                throw e;
            }
            throw new SQLException("Failed to fetch schema of " + tableName, e);

        } finally {
            if (stmt != null) {
                stmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
    }

    private TableMeta resultSetMetaToSchema(DatabaseMetaData dbmd, String tableName) throws SQLException {
        TableMeta tm = new TableMeta();
        tm.setTableName(tableName);
        String[] schemaTable = tableName.split("\\.");
        String schemaName = schemaTable.length > 1 ? schemaTable[0] : dbmd.getUserName();
        tableName = schemaTable.length > 1 ? schemaTable[1] : tableName;
        if(tableName.contains("\"")){
            tableName = tableName.replace("\"", "");
            schemaName = schemaName.replace("\"", "");
        }else{
            tableName = tableName.toUpperCase();
        }

        ResultSet rsColumns = dbmd.getColumns("", schemaName, tableName, "%");
        ResultSet rsIndex = dbmd.getIndexInfo(null, schemaName, tableName, false, true);
        ResultSet rsPrimary = dbmd.getPrimaryKeys(null, schemaName, tableName);

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
                        index.setIndextype(IndexType.Unique);
                    } else {
                        index.setIndextype(IndexType.Normal);
                    }
                    tm.getAllIndexes().put(indexName, index);

                }
            }

            while (rsPrimary.next()) {
                String pkIndexName = rsPrimary.getString("PK_NAME");
                if (tm.getAllIndexes().containsKey(pkIndexName)) {
                    IndexMeta index = tm.getAllIndexes().get(pkIndexName);
                    index.setIndextype(IndexType.PRIMARY);
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
            if (rsPrimary != null) {
                rsPrimary.close();
            }
        }

        return tm;
    }

}
