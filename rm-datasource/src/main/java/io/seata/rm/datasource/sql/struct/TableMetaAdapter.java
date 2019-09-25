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

import java.util.List;

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
    public void setTableName(String tableName) {
        if (tableName == null) {
            return ;
        }
        super.setTableName(ColumnUtils.addEscape(tableName, dbType));
    }

    @Override
    public ColumnMeta getColumnMeta(String colName) {
        if (colName == null) {
            return null;
        }
        ColumnMeta col = getAllColumns().get(ColumnUtils.addEscape(colName, dbType));
        return col;
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

}
