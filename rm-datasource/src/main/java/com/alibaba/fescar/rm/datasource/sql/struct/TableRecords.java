/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.fescar.rm.datasource.sql.struct;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fescar.common.exception.ShouldNeverHappenException;

/**
 * The type Table records.
 */
public class TableRecords {

    @JSONField(serialize = false)
    private TableMeta tableMeta;

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
     * @return the list
     */
    public List<Field> pkRows() {
        final String pkName = getTableMeta().getPkName();
        return new ArrayList<Field>() {
            {
                for (Row row : rows) {
                    List<Field> fields = row.getFields();
                    for (Field field : fields) {
                        if (field.getName().equalsIgnoreCase(pkName)) {
                            add(field);
                            break;
                        }
                    }
                }
            }
        };
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
        return new TableRecords(tableMeta) {
            @Override
            public int size() {
                return 0;
            }

            @Override
            public List<Field> pkRows() {
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
        };
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
                Field field = new Field();
                field.setName(col.getColumnName());
                if (tmeta.getPkName().equals(field.getName())) {
                    field.setKeyType(KeyType.PrimaryKey);
                }
                field.setType(col.getDataType());
                field.setValue(resultSet.getObject(i));
                fields.add(field);
            }

            Row row = new Row();
            row.setFields(fields);

            records.add(row);
        }
        return records;
    }
}
