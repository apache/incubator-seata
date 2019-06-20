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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.seata.common.exception.NotSupportYetException;
import io.seata.common.util.CollectionUtils;

/**
 * The type Table meta.
 *
 * @author sharajava
 */
public class TableMeta {
    private String tableName;

    private Map<String, ColumnMeta> allColumns = new HashMap<String, ColumnMeta>();
    private Map<String, IndexMeta> allIndexes = new HashMap<String, IndexMeta>();

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
     * Gets column meta.
     *
     * @param colName the col name
     * @return the column meta
     */
    public ColumnMeta getColumnMeta(String colName) {
        ColumnMeta col = allColumns.get(colName);
        if (col == null) {
            if (colName.charAt(0) == '`') {
                col = allColumns.get(colName.substring(1, colName.length() - 1));
            } else { col = allColumns.get("`" + colName + "`"); }
        }
        return col;
    }

    /**
     * Gets all columns.
     *
     * @return the all columns
     */
    public Map<String, ColumnMeta> getAllColumns() {
        return allColumns;
    }

    /**
     * Gets all indexes.
     *
     * @return the all indexes
     */
    public Map<String, IndexMeta> getAllIndexes() {
        return allIndexes;
    }

    /**
     * Gets auto increase column.
     *
     * @return the auto increase column
     */
    public ColumnMeta getAutoIncreaseColumn() {
        // TODO: how about auto increment but not pk?
        for (Entry<String, ColumnMeta> entry : allColumns.entrySet()) {
            ColumnMeta col = entry.getValue();
            if ("YES".equalsIgnoreCase(col.getIsAutoincrement())) {
                return col;
            }
        }
        return null;
    }

    /**
     * Gets primary key map.
     *
     * @return the primary key map
     */
    public Map<String, ColumnMeta> getPrimaryKeyMap() {
        Map<String, ColumnMeta> pk = new HashMap<String, ColumnMeta>();
        for (Entry<String, IndexMeta> entry : allIndexes.entrySet()) {
            IndexMeta index = entry.getValue();
            if (index.getIndextype().value() == IndexType.PRIMARY.value()) {
                for (ColumnMeta col : index.getValues()) {
                    pk.put(col.getColumnName(), col);
                }
            }
        }

        if (pk.size() > 1) {
            throw new NotSupportYetException("Multi PK");
        }

        return pk;
    }

    /**
     * Gets primary key only name.
     *
     * @return the primary key only name
     */
    @SuppressWarnings("serial")
    public List<String> getPrimaryKeyOnlyName() {
        List<String> list = new ArrayList<>();
        for (Entry<String, ColumnMeta> entry : getPrimaryKeyMap().entrySet()) {
            list.add(entry.getKey());
        }
        return list;
    }

    /**
     * Gets pk name.
     *
     * @return the pk name
     */
    public String getPkName() {
        return getPrimaryKeyOnlyName().get(0);
    }

    /**
     * Contains pk boolean.
     *
     * @param cols the cols
     * @return the boolean
     */
    public boolean containsPK(List<String> cols) {
        if (cols == null) {
            return false;
        }

        List<String> pk = getPrimaryKeyOnlyName();
        if (pk.isEmpty()) {
            return false;
        }

        if (cols.containsAll(pk)) {
            return true;
        } else {
            return CollectionUtils.toUpperList(cols).containsAll(CollectionUtils.toUpperList(pk));
        }
    }

    /**
     * Gets create table sql.
     *
     * @return the create table sql
     */
    public String getCreateTableSQL() {
        StringBuilder sb = new StringBuilder("CREATE TABLE");
        sb.append(String.format(" `%s` ", getTableName()));
        sb.append("(");

        boolean flag = true;
        Map<String, ColumnMeta> allColumns = getAllColumns();
        for (Entry<String, ColumnMeta> entry : allColumns.entrySet()) {
            if (flag) {
                flag = false;
            } else {
                sb.append(",");
            }

            ColumnMeta col = entry.getValue();
            sb.append(String.format(" `%s` ", col.getColumnName()));
            sb.append(col.getDataTypeName());
            if (col.getColumnSize() > 0) {
                sb.append(String.format("(%d)", col.getColumnSize()));
            }

            if (col.getColumnDef() != null && col.getColumnDef().length() > 0) {
                sb.append(String.format(" default '%s'", col.getColumnDef()));
            }

            if (col.getIsNullAble() != null && col.getIsNullAble().length() > 0) {
                sb.append(" ");
                sb.append(col.getIsNullAble());
            }
        }

        Map<String, IndexMeta> allIndexes = getAllIndexes();
        for (Entry<String, IndexMeta> entry : allIndexes.entrySet()) {
            sb.append(", ");

            IndexMeta index = entry.getValue();
            switch (index.getIndextype()) {
                case FullText:
                    break;
                case Normal:
                    sb.append(String.format("KEY `%s`", index.getIndexName()));
                    break;
                case PRIMARY:
                    sb.append("PRIMARY KEY");
                    break;
                case Unique:
                    sb.append(String.format("UNIQUE KEY `%s`", index.getIndexName()));
                    break;
                default:
                    break;
            }

            sb.append(" (");
            boolean f = true;
            for (ColumnMeta c : index.getValues()) {
                if (f) {
                    f = false;
                } else {
                    sb.append(",");
                }

                sb.append(String.format("`%s`", c.getColumnName()));
            }
            sb.append(")");
        }
        sb.append(")");

        return sb.toString();
    }
}
