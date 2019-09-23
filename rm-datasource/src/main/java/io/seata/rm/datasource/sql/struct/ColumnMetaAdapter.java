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

import io.seata.rm.datasource.ColumnUtils;

/**
 * @author jsbxyyx
 */
public class ColumnMetaAdapter extends ColumnMeta {

    private String dbType;

    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    @Override
    public String getColumnName() {
        return ColumnUtils.addEscape(super.getColumnName(), dbType);
    }

    @Override
    protected String getTableName() {
        return ColumnUtils.addEscape(super.getTableName(), dbType);
    }

    /**
     * create column meta adapter from column meta
     * @param dbType the db type
     * @param columnMeta the column meta
     * @return the column meta adapter
     */
    public static ColumnMetaAdapter createFromColumnMeta(String dbType, ColumnMeta columnMeta) {
        if (columnMeta == null) {
            throw new NullPointerException("columnMeta is null");
        }
        ColumnMetaAdapter adapter = new ColumnMetaAdapter();
        adapter.setDbType(dbType);
        adapter.setCharOctetLength(columnMeta.getCharOctetLength());
        adapter.setColumnDef(columnMeta.getColumnDef());
        adapter.setColumnName(columnMeta.getColumnName());
        adapter.setColumnSize(columnMeta.getColumnSize());
        adapter.setDataType(columnMeta.getDataType());
        adapter.setDataTypeName(columnMeta.getDataTypeName());
        adapter.setDecimalDigits(columnMeta.getDecimalDigits());
        adapter.setIsAutoincrement(columnMeta.getIsAutoincrement());
        adapter.setIsNullAble(columnMeta.getIsNullAble());
        adapter.setNullAble(columnMeta.getNullAble());
        adapter.setNumPrecRadix(columnMeta.getNumPrecRadix());
        adapter.setOrdinalPosition(columnMeta.getOrdinalPosition());
        adapter.setRemarks(columnMeta.getRemarks());
        adapter.setSqlDataType(columnMeta.getSqlDataType());
        adapter.setSqlDatetimeSub(columnMeta.getSqlDatetimeSub());
        adapter.setTableCat(columnMeta.getTableCat());
        adapter.setTableName(columnMeta.getTableName());
        adapter.setTableSchemaName(columnMeta.getTableSchemaName());
        return adapter;
    }
}
