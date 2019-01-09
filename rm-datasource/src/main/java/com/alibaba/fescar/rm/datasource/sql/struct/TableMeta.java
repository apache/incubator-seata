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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.alibaba.fescar.common.exception.NotSupportYetException;

public class TableMeta {
    private String tableName;

    private Map<String, ColumnMeta> allColumns = new HashMap<String, ColumnMeta>();
    private Map<String, IndexMeta> allIndexes = new HashMap<String, IndexMeta>();

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public ColumnMeta getColumnMeta(String colName) {
        String s = colName.toUpperCase();
        ColumnMeta col = allColumns.get(s);
        if (col == null) {
            if (colName.charAt(0) == '`') {
                col = allColumns.get(s.substring(1, colName.length() - 1));
            } else { col = allColumns.get("`" + s + "`"); }
        }
        return col;
    }

    public Map<String, ColumnMeta> getAllColumns() {
        return allColumns;
    }

    public Map<String, IndexMeta> getAllIndexes() {
        return allIndexes;
    }

    public ColumnMeta getAutoIncreaseColumn() {
        // TODO: how about auto increment but not pk?
        for (Entry<String, ColumnMeta> entry : allColumns.entrySet()) {
            ColumnMeta col = entry.getValue();
            if ("YES".equalsIgnoreCase(col.getIsAutoincrement()) == true) {
                return col;
            }
        }
        return null;
    }

    public Map<String, ColumnMeta> getPrimaryKeyMap() {
        Map<String, ColumnMeta> pk = new HashMap<String, ColumnMeta>();
        for (Entry<String, IndexMeta> entry : allIndexes.entrySet()) {
            IndexMeta index = entry.getValue();
            if (index.getIndextype().value() == IndexType.PRIMARY.value()) {
                for (ColumnMeta col : index.getValues()) {
                    pk.put(col.getColumnName().toUpperCase(), col);
                }
            }
        }

        if (pk.size() > 1) {
            throw new NotSupportYetException("Multi PK");
        }

        return pk;
    }

    @SuppressWarnings("serial")
    public List<String> getPrimaryKeyOnlyName() {
        return new ArrayList<String>() {
            {
                for (Entry<String, ColumnMeta> entry : getPrimaryKeyMap().entrySet()) {
                    add(entry.getKey());
                }
            }
        };
    }

    public String getPkName() {
        return getPrimaryKeyOnlyName().get(0);
    }

    public boolean containsPK(List<String> cols) {
        if (cols == null) {
            return false;
        }

        List<String> pk = getPrimaryKeyOnlyName();
        if (pk.isEmpty()) {
            return false;
        }

        return cols.containsAll(pk);
    }

    public String getCreateTableSQL() {
        StringBuilder sb = new StringBuilder("CREATE TABLE");
        sb.append(String.format(" `%s` ", getTableName()));
        sb.append("(");

        boolean flag = true;
        Map<String, ColumnMeta> allColumns = getAllColumns();
        for (Entry<String, ColumnMeta> entry : allColumns.entrySet()) {
            if (flag == true) {
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
                    sb.append(String.format("PRIMARY KEY"));
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
                if (f == true) {
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
