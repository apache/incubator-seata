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

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.rm.datasource.sql.serial.SerialArray;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import javax.sql.rowset.serial.SerialDatalink;
import javax.sql.rowset.serial.SerialJavaObject;
import javax.sql.rowset.serial.SerialRef;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.JDBCType;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The type Table records.
 *
 * @author sharajava
 */
public class TableRecords {

    private transient TableMeta tableMeta;

    private String tableName;

    private List<Row> rows = new ArrayList<Row>();

    /**
     * Gets table name.
     *
     * @return the table name
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Sets table name.
     *
     * @param tableName the table name
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * Gets rows.
     *
     * @return the rows
     */
    public List<Row> getRows() {
        return rows;
    }

    /**
     * Sets rows.
     *
     * @param rows the rows
     */
    public void setRows(List<Row> rows) {
        this.rows = rows;
    }

    /**
     * Instantiates a new Table records.
     */
    public TableRecords() {

    }

    /**
     * Instantiates a new Table records.
     *
     * @param tableMeta the table meta
     */
    public TableRecords(TableMeta tableMeta) {
        setTableMeta(tableMeta);
    }

    /**
     * Sets table meta.
     *
     * @param tableMeta the table meta
     */
    public void setTableMeta(TableMeta tableMeta) {
        if (this.tableMeta != null) {
            throw new ShouldNeverHappenException();
        }
        this.tableMeta = tableMeta;
        this.tableName = tableMeta.getTableName();
    }

    /**
     * Size int.
     *
     * @return the int
     */
    public int size() {
        return rows.size();
    }

    /**
     * Add.
     *
     * @param row the row
     */
    public void add(Row row) {
        rows.add(row);
    }

    /**
     * Pk rows list.
     *
     * @return return a list. each element of list is a map,the map hold the pk column name as a key and field as the value
     */
    public List<Map<String,Field>> pkRows() {
        final Map<String, ColumnMeta> primaryKeyMap = getTableMeta().getPrimaryKeyMap();
        List<Map<String,Field>> pkRows = new ArrayList<>();
        for (Row row : rows) {
            List<Field> fields = row.getFields();
            Map<String,Field> rowMap = new HashMap<>(3);
            for (Field field : fields) {
                if (primaryKeyMap.containsKey(field.getName())) {
                    rowMap.put(field.getName(),field);
                }
            }
            pkRows.add(rowMap);
        }
        return pkRows;
    }

    /**
     * Gets table meta.
     *
     * @return the table meta
     */
    public TableMeta getTableMeta() {
        return tableMeta;
    }

    /**
     * Empty table records.
     *
     * @param tableMeta the table meta
     * @return the table records
     */
    public static TableRecords empty(TableMeta tableMeta) {
        return new EmptyTableRecords(tableMeta);
    }

    /**
     * Build records table records.
     *
     * @param tmeta     the tmeta
     * @param resultSet the result set
     * @return the table records
     * @throws SQLException the sql exception
     */
    public static TableRecords buildRecords(TableMeta tmeta, ResultSet resultSet) throws SQLException {
        TableRecords records = new TableRecords(tmeta);
        ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
        int columnCount = resultSetMetaData.getColumnCount();

        while (resultSet.next()) {
            List<Field> fields = new ArrayList<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                String colName = resultSetMetaData.getColumnName(i);
                ColumnMeta col = tmeta.getColumnMeta(colName);
                int dataType = col.getDataType();
                Field field = new Field();
                field.setName(col.getColumnName());
                if (tmeta.getPrimaryKeyMap().containsKey(colName)) {
                    field.setKeyType(KeyType.PRIMARY_KEY);
                }
                field.setType(dataType);
                // mysql will not run in this code
                // cause mysql does not use java.sql.Blob, java.sql.sql.Clob to process Blob and Clob column
                if (dataType == JDBCType.BLOB.getVendorTypeNumber()) {
                    Blob blob = resultSet.getBlob(i);
                    if (blob != null) {
                        field.setValue(new SerialBlob(blob));
                    }
                } else if (dataType == JDBCType.CLOB.getVendorTypeNumber()) {
                    Clob clob = resultSet.getClob(i);
                    if (clob != null) {
                        field.setValue(new SerialClob(clob));
                    }
                } else if (dataType == JDBCType.ARRAY.getVendorTypeNumber()) {
                    Array array = resultSet.getArray(i);
                    if (array != null) {
                        field.setValue(new SerialArray(array));
                    }
                } else if (dataType == JDBCType.REF.getVendorTypeNumber()) {
                    Ref ref = resultSet.getRef(i);
                    if (ref != null) {
                        field.setValue(new SerialRef(ref));
                    }
                } else if (dataType == JDBCType.DATALINK.getVendorTypeNumber()) {
                    java.net.URL url = resultSet.getURL(i);
                    if (url != null) {
                        field.setValue(new SerialDatalink(url));
                    }
                } else if (dataType == JDBCType.JAVA_OBJECT.getVendorTypeNumber()) {
                    Object object = resultSet.getObject(i);
                    if (object != null) {
                        field.setValue(new SerialJavaObject(object));
                    }
                } else {
                    // JDBCType.DISTINCT, JDBCType.STRUCT etc...
                    field.setValue(resultSet.getObject(i));
                }

                fields.add(field);
            }

            Row row = new Row();
            row.setFields(fields);

            records.add(row);
        }
        return records;
    }

    public static class EmptyTableRecords extends TableRecords {

        public EmptyTableRecords() {}

        public EmptyTableRecords(TableMeta tableMeta) {
            this.setTableMeta(tableMeta);
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public List<Map<String,Field>> pkRows() {
            return new ArrayList<>();
        }

        @Override
        public void add(Row row) {
            throw new UnsupportedOperationException("xxx");
        }

        @Override
        public TableMeta getTableMeta() {
            throw new UnsupportedOperationException("xxx");
        }
    }
}
