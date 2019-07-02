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

import com.alibaba.druid.util.StringUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.core.context.RootContext;
import io.seata.rm.datasource.AbstractConnectionProxy;
import io.seata.rm.datasource.DataSourceProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.concurrent.TimeUnit;

/**
 * The type Table meta cache.
 */
@SuppressWarnings("Duplicates")
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

        String dataSourceKey = dataSourceProxy.getResourceId();

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
        ResultSet rs = null;
        try {
            conn = dataSource.getConnection();
            stmt = conn.createStatement();
            StringBuffer sb = new StringBuffer("SELECT * FROM " + tableName + " where rownum=1");
            rs = stmt.executeQuery(sb.toString());
            ResultSetMetaData rsmd = rs.getMetaData();
            DatabaseMetaData dbmd = conn.getMetaData();
            return resultSetMetaToSchema(rsmd, dbmd, conn, tableName);
        } catch (Exception e) {
            if (e instanceof SQLException) {
                throw ((SQLException) e);
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

    private static TableMeta resultSetMetaToSchema(ResultSet rs2, AbstractConnectionProxy conn,
                                                   String tablename) throws SQLException {
        String tableName = tablename;

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
        ResultSet rs1 = null;
        try {
            stmt = conn.getTargetConnection().createStatement();
            rs1 = stmt.executeQuery(
                    String.format("select a.constraint_name, a.column_name from user_cons_columns a, user_constraints b  where a.constraint_name = b.constraint_name and b.constraint_type = 'P' and a.table_name ='%s'", tableName));
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

    private static TableMeta resultSetMetaToSchema(ResultSetMetaData rsmd, DatabaseMetaData dbmd, Connection conn, String tableName)
            throws SQLException {
        String schemaName = rsmd.getSchemaName(1);
        String catalogName = rsmd.getCatalogName(1);
        logger.debug("schemaName {}", schemaName);
        logger.debug("catalogName {}", catalogName);

        TableMeta tm = new TableMeta();
        tm.setTableName(tableName);

        ResultSet rs1 = dbmd.getColumns("", dbmd.getUserName(), tableName, "%");
        while (rs1.next()) {
            ColumnMeta col = new ColumnMeta();
            col.setTableCat(rs1.getString("TABLE_CAT"));
            col.setTableSchemaName(rs1.getString("TABLE_SCHEM"));
            col.setTableName(rs1.getString("TABLE_NAME"));
            col.setColumnName(rs1.getString("COLUMN_NAME"));
            int dataType = rs1.getInt("DATA_TYPE");
            String typeName = rs1.getString("TYPE_NAME");
            if (dataType == Types.TIMESTAMP) {
                logger.debug("dateType:{}", dataType);
                logger.debug("data_type_name:{}", typeName);
            }
            col.setDataType(dataType);
            col.setDataTypeName(typeName);
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


        ResultSet rs2 = dbmd.getIndexInfo(null, dbmd.getUserName(), tableName, false, true);

        String indexName = "";
        while (rs2.next()) {
            indexName = rs2.getString("INDEX_NAME");
            if (StringUtils.isEmpty(indexName)) {
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
                index.setType(rs2.getShort("TYPE"));
                index.setOrdinalPosition(rs2.getShort("ORDINAL_POSITION"));
                index.setAscOrDesc(rs2.getString("ASC_OR_DESC"));
                index.setCardinality(rs2.getInt("CARDINALITY"));
                index.getValues().add(col);
                if ("PRIMARY".equalsIgnoreCase(indexName) || (
                        tableName + "_pkey").equalsIgnoreCase(indexName)) {
                    index.setIndextype(IndexType.PRIMARY);
                } else if (!index.isNonUnique()) {
                    index.setIndextype(IndexType.Unique);
                } else {
                    index.setIndextype(IndexType.Normal);
                }
                tm.getAllIndexes().put(indexName, index);
            }
        }

        Statement statement = conn.createStatement();
        String sql = String.format("select a.constraint_name,a.column_name ,b.index_name from user_cons_columns a, user_constraints b where a.constraint_name = b.constraint_name and b.constraint_type = 'P' and a.table_name = '%s'", tableName);

        ResultSet pkResult = statement.executeQuery(sql);
        while (pkResult.next()) {
            String pkIndexName = pkResult.getString("index_name");
            String constraintName = pkResult.getString("constraint_name");
            if (tm.getAllIndexes().containsKey(pkIndexName)) {
                IndexMeta index = tm.getAllIndexes().get(pkIndexName);
                index.setIndextype(IndexType.PRIMARY);
            } else {
                IndexMeta index = new IndexMeta();
                index.setIndexName(constraintName);
                index.setNonUnique(false);
                String columnName = pkResult.getString("column_name");
                ColumnMeta col = tm.getAllColumns().get(columnName);
                index.setOrdinalPosition((short) 1);
                index.setAscOrDesc(null);
                index.setType((short) 1);
                index.setIndexQualifier(null);
                index.setCardinality(0);
                index.getValues().add(col);
                index.setIndextype(IndexType.PRIMARY);
                tm.getAllIndexes().put(indexName, index);
            }
        }
 /*       ResultSet pk = dbmd.getPrimaryKeys(null, dbmd.getUserName(), tableName);
        while (pk.next()) {
            String pkIndexName = pk.getObject(6).toString();
            if (tm.getAllIndexes().containsKey(pkIndexName)) {
                IndexMeta index = tm.getAllIndexes().get(pkIndexName);
                index.setIndextype(IndexType.PRIMARY);
            } else {
                IndexMeta index = new IndexMeta();
                index.setIndexName(pkIndexName);
                index.setNonUnique(true);
                String columnName = pk.getString("COLUMN_NAME");
                index.setOrdinalPosition(pk.getShort("key_seq"));
                index.setAscOrDesc(null);
                index.setType((short) 1);
                index.setIndexQualifier(null);
                index.setCardinality(0);
                ColumnMeta col = tm.getAllColumns().get(columnName);
                index.getValues().add(col);
                index.setIndextype(IndexType.PRIMARY);
                tm.getAllIndexes().put(indexName, index);
            }
        }*/
        return tm;
    }
}
