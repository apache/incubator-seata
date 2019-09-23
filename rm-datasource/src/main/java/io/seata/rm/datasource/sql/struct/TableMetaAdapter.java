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

import io.seata.common.util.CollectionUtils;
import io.seata.rm.datasource.ColumnUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jsbxyyx
 */
public class TableMetaAdapter extends TableMeta {

    private String dbType;

    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    @Override
    public String getTableName() {
        return ColumnUtils.addEscape(super.getTableName(), dbType);
    }

    @Override
    public Map<String, ColumnMeta> getPrimaryKeyMap() {
        Map<String, ColumnMeta> map = new LinkedHashMap<>();
        Map<String, ColumnMeta> primaryKeyMap = super.getPrimaryKeyMap();
        for (Map.Entry<String, ColumnMeta> entry : primaryKeyMap.entrySet()) {
            map.put(ColumnUtils.addEscape(entry.getKey(), dbType), ColumnMetaAdapter.createFromColumnMeta(dbType, entry.getValue()));
        }
        return map;
    }

    @Override
    public ColumnMeta getColumnMeta(String colName) {
        return ColumnMetaAdapter.createFromColumnMeta(dbType, super.getColumnMeta(colName));
    }

    @Override
    public boolean containsPK(List<String> cols) {
        if (cols == null) {
            return false;
        }

        List<String> pk = getPrimaryKeyOnlyName();
        if (pk.isEmpty()) {
            return false;
        }

        ColumnUtils.addEscape(cols, dbType);

        if (cols.containsAll(pk)) {
            return true;
        } else {
            return CollectionUtils.toUpperList(cols).containsAll(CollectionUtils.toUpperList(pk));
        }
    }

    /**
     * create table meta adapter from table meta
     * @param dbType the db type
     * @param tableMeta the table meta
     * @return the table meta adapter
     */
    public static TableMetaAdapter createFromTableMeta(String dbType, TableMeta tableMeta) {
        if (tableMeta == null) {
            throw new NullPointerException("tableMeta is null");
        }
        TableMetaAdapter adapter = new TableMetaAdapter();
        adapter.setDbType(dbType);
        adapter.setTableName(tableMeta.getTableName());
        adapter.getAllColumns().putAll(tableMeta.getAllColumns());
        adapter.getAllIndexes().putAll(tableMeta.getAllIndexes());
        return adapter;
    }
}
