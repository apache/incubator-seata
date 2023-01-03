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
import java.util.Collections;

/**
 * The type Multi Table records.
 *
 * @author renliangyu857
 */
public class MultiTableRecords extends TableRecordsAware {
    private Map<String, TableRecords> multiTableRecords = new HashMap<>(4);

    /**
     * add table records
     * @param tableName the table's name
     * @param tableRecords the table's records
     */
    public void addTableRecords(String tableName,TableRecords tableRecords) {
        multiTableRecords.put(tableName, tableRecords);
    }

    /**
     * get table records by table name
     * @param tableName the table name
     * @return the table records
     */
    public TableRecords getTableRecordsByTableName(String tableName) {
        return multiTableRecords.get(tableName);
    }

    /**
     * get table name list
     * @return the table name list
     */
    public List<String> getTableNames() {
        return new ArrayList<>(multiTableRecords.keySet());
    }

    /**
     * get multi table records
     * @return multi table records.key->tableName,value->tableRecords
     */
    public Map<String,TableRecords> getMultiTableRecords() {
        return Collections.unmodifiableMap(multiTableRecords);
    }
}
