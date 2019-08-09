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
import java.sql.ResultSetMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import com.alibaba.druid.util.StringUtils;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.core.context.RootContext;
import io.seata.rm.datasource.DataSourceProxy;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * The type Table meta cache.
 */
public class TableMetaCacheOracle {

    private static final long CACHE_SIZE = 100000;

    private static final long EXPIRE_TIME = 900 * 1000;

    private static final Cache<String, TableMeta> TABLE_META_CACHE = Caffeine.newBuilder().maximumSize(CACHE_SIZE)
        .expireAfterWrite(EXPIRE_TIME, TimeUnit.MILLISECONDS).softValues().build();

    private static Logger logger = LoggerFactory.getLogger(TableMetaCacheOracle.class);

    /**
     * Gets table meta.
     *
     * @param dataSourceProxy the druid data source
     * @param tableName       the table name
     * @return the table meta
     */
    public static TableMeta getTableMeta(final DataSourceProxy dataSourceProxy, final String tableName) {
        if (StringUtils.isEmpty(tableName)) {
            throw new IllegalArgumentException("TableMeta cannot be fetched without tableName");
        }

        String dataSourceKey =  dataSourceProxy.getResourceId();

        TableMeta tmeta = null;
        final String key = dataSourceKey + "." + tableName;
        tmeta = TABLE_META_CACHE.get(key, mappingFunction -> {
            try {
                return fetchSchema(dataSourceProxy.getTargetDataSource(), tableName);
            } catch (SQLException e) {
                logger.error("get cache error !", e);
                return null;
            }
        });
        if (tmeta == null) {
            try {
                tmeta = fetchSchema(dataSourceProxy.getTargetDataSource(), tableName);
            } catch (SQLException e) {
            }
        }

        if (tmeta == null) {
            throw new ShouldNeverHappenException(String.format("[xid:%s]get tablemeta failed", RootContext.getXID()));
        }
        return tmeta;
    }

    private static TableMeta fetchSchema(DataSource dataSource, String tableName) throws SQLException {
        return fetchSchemeInDefaultWay(dataSource, tableName);
    }

    private static TableMeta fetchSchemeInDefaultWay(DataSource dataSource, String tableName)
        throws SQLException {
        Connection conn = null;
        java.sql.Statement stmt = null;
        try {
            conn = dataSource.getConnection();
            stmt = conn.createStatement();
            DatabaseMetaData dbmd = conn.getMetaData();
            return resultSetMetaToSchema(null, dbmd, tableName);
        } catch (Exception e) {
            if (e instanceof SQLException) {
                throw ((SQLException)e);
            }
            throw new SQLException("Failed to fetch schema of " + tableName, e);

        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    private static TableMeta resultSetMetaToSchema(ResultSetMetaData rsmd, DatabaseMetaData dbmd, String tableName)
        throws SQLException {
        tableName = tableName.toUpperCase();//转换大写，oracle表名要大写才能取元数据
        TableMeta tm = new TableMeta();
        tm.setTableName(tableName);
        String[] schemaTable = tableName.split("\\.");
        String schemaName = schemaTable.length>1?schemaTable[0]:dbmd.getUserName();
        tableName = schemaTable.length>1?schemaTable[1]:tableName;

        ResultSet rs1 = dbmd.getColumns("", schemaName, tableName, "%");
        while (rs1.next()) {
            ColumnMeta col = new ColumnMeta();
            col.setTableCat(rs1.getString("TABLE_CAT"));
            col.setTableSchemaName(rs1.getString("TABLE_SCHEM"));
            col.setTableName(rs1.getString("TABLE_NAME"));
            col.setColumnName(rs1.getString("COLUMN_NAME"));
            col.setDataType(rs1.getInt("DATA_TYPE"));
            col.setDataTypeName(rs1.getString("TYPE_NAME"));
            col.setColumnSize(rs1.getInt("COLUMN_SIZE"));
            col.setDecimalDigits(rs1.getInt("DECIMAL_DIGITS"));
            col.setNumPrecRadix(rs1.getInt("NUM_PREC_RADIX"));
            col.setNullAble(rs1.getInt("NULLABLE"));
            col.setRemarks(rs1.getString("REMARKS"));
            col.setColumnDef(rs1.getString("COLUMN_DEF"));
            col.setSqlDataType(rs1.getInt("SQL_DATA_TYPE"));
            col.setSqlDatetimeSub(rs1.getInt("SQL_DATETIME_SUB"));
            col.setCharOctetLength(rs1.getInt("CHAR_OCTET_LENGTH"));
            col.setOrdinalPosition(rs1.getInt("ORDINAL_POSITION"));
            col.setIsNullAble(rs1.getString("IS_NULLABLE"));
//            col.setIsAutoincrement(rs1.getString("IS_AUTOINCREMENT"));

            tm.getAllColumns().put(col.getColumnName(), col);
        }


        java.sql.ResultSet rs2 = dbmd.getIndexInfo(null, schemaName, tableName, false, true);

        String indexName = "";
        while (rs2.next()) {
            indexName = rs2.getString("INDEX_NAME");
            if( StringUtils.isEmpty(indexName) ){
                continue;
            }
            String colName = rs2.getString("COLUMN_NAME").toUpperCase();
            ColumnMeta col = tm.getAllColumns().get(colName);

            if (tm.getAllIndexes().containsKey(indexName)) {
                IndexMeta index = tm.getAllIndexes().get(indexName);
                index.getValues().add(col);
            } else {
                IndexMeta index = new IndexMeta();
                index.setIndexName(indexName);
                index.setNonUnique(rs2.getBoolean("NON_UNIQUE"));
                index.setIndexQualifier(rs2.getString("INDEX_QUALIFIER"));
                index.setIndexName(rs2.getString("INDEX_NAME"));
                index.setType(rs2.getShort("TYPE"));
                index.setOrdinalPosition(rs2.getShort("ORDINAL_POSITION"));
                index.setAscOrDesc(rs2.getString("ASC_OR_DESC"));
                index.setCardinality(rs2.getInt("CARDINALITY"));
                index.getValues().add(col);
                if ("PRIMARY".equalsIgnoreCase(indexName) || (
                        tableName+ "_pkey").equalsIgnoreCase(indexName)) {
                    index.setIndextype(IndexType.PRIMARY);
                } else if (index.isNonUnique() == false) {
                    index.setIndextype(IndexType.Unique);
                } else {
                    index.setIndextype(IndexType.Normal);
                }
                tm.getAllIndexes().put(indexName, index);

            }
        }
        ResultSet pk = dbmd.getPrimaryKeys(null, schemaName, tableName);
        while (pk.next()) {
            String pkIndexName = pk.getObject(6).toString();
            if (tm.getAllIndexes().containsKey(pkIndexName)) {
                IndexMeta index = tm.getAllIndexes().get(pkIndexName);
                index.setIndextype(IndexType.PRIMARY);
            }
        }
        IndexMeta index = tm.getAllIndexes().get(indexName);
        if (index.getIndextype().value() != 0) {
            if ("H2 JDBC Driver".equals(dbmd.getDriverName())) {
                if (indexName.length() > 11 && "PRIMARY_KEY".equalsIgnoreCase(indexName.substring(0, 11))) {
                    index.setIndextype(IndexType.PRIMARY);
                }
            } else if (dbmd.getDriverName() != null && dbmd.getDriverName().toLowerCase().indexOf("postgresql") >= 0) {
                if ((tableName + "_pkey").equalsIgnoreCase(indexName)) {
                    index.setIndextype(IndexType.PRIMARY);
                }
            }
        }
        return tm;
    }
}
