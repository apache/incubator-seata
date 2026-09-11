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
package io.seata.sqlparser.struct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

import io.seata.common.exception.NotSupportYetException;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.LowerCaseLinkHashMap;
import io.seata.sqlparser.util.ColumnUtils;

/**
 * The type Table meta.
 *
 * @author sharajava
 */
public class TableMeta {
    private String tableName;

    private boolean isCaseSensitive;

    /**
     * key: column name
     */
    private final Map<String, ColumnMeta> allColumns = new LowerCaseLinkHashMap<>();

    /**
     * key: index name
     */
    private final Map<String, IndexMeta> allIndexes = new LowerCaseLinkHashMap<>();

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
        return allColumns.get(colName);
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
     * Is case sensitive boolean.
     *
     * @return the boolean
     */
    public boolean isCaseSensitive() {
        return isCaseSensitive;
    }

    /**
     * Sets case sensitive.
     *
     * @param caseSensitive the case sensitive
     */
    public void setCaseSensitive(boolean caseSensitive) {
        isCaseSensitive = caseSensitive;
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
        Map<String, ColumnMeta> pk = new HashMap<>();
        allIndexes.forEach((key, index) -> {
            if (index.getIndextype().value() == IndexType.PRIMARY.value()) {
                for (ColumnMeta col : index.getValues()) {
                    pk.put(col.getColumnName(), col);
                }
            }
        });

        if (pk.size() < 1) {
            throw new NotSupportYetException(String.format("%s needs to contain the primary key.", tableName));
        }

        return pk;
    }

    /**
     * Gets case-insensitive primary key set
     *
     * @return case-insensitive, unmodifiable primary key set
     */
    public Set<String> getCaseInsensitivePKs() {
        Set<String> pks = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        allIndexes.forEach((key, index) -> {
            if (index.getIndextype().value() == IndexType.PRIMARY.value()) {
                for (ColumnMeta col : index.getValues()) {
                    pks.add(col.getColumnName());
                }
            }
        });
        if (pks.size() < 1) {
            throw new NotSupportYetException(String.format("%s needs to contain the primary key.", tableName));
        }
        return Collections.unmodifiableSet(pks);
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
     * Gets all the on update columns only name.
     *
     * @return all the on update columns only name
     */
    public List<String> getOnUpdateColumnsOnlyName() {
        return allColumns.values().stream().filter(ColumnMeta::isOnUpdate).map(ColumnMeta::getColumnName).collect(Collectors.toList());
    }

    /**
     * Gets add escape pk name.
     *
     * @param dbType the db type
     * @return escape pk name list
     */
    public List<String> getEscapePkNameList(String dbType) {
        return ColumnUtils.addEscape(getPrimaryKeyOnlyName(), dbType);
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


        //at least contain one pk
        if (cols.containsAll(pk)) {
            return true;
        } else {
            return CollectionUtils.toUpperList(cols).containsAll(CollectionUtils.toUpperList(pk));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TableMeta)) {
            return false;
        }
        TableMeta tableMeta = (TableMeta) o;
        if (!Objects.equals(tableMeta.tableName, this.tableName)) {
            return false;
        }
        if (!Objects.equals(tableMeta.allColumns, this.allColumns)) {
            return false;
        }
        if (!Objects.equals(tableMeta.allIndexes, this.allIndexes)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = Objects.hashCode(tableName);
        hash += Objects.hashCode(allColumns);
        hash += Objects.hashCode(allIndexes);
        hash += Objects.hashCode(isCaseSensitive);
        return hash;
    }
}
