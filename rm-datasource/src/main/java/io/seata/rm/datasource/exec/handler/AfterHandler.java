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
package io.seata.rm.datasource.exec.handler;

import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.sqlparser.SQLType;

import java.util.List;
import java.util.Map;

/**
 * @author: lyx
 */
public interface AfterHandler {
    /**
     * Build after select SQL to append in the SQL when build in before image
     *
     * @param beforeImage beforeImage
     * @return select SQL
     */
    String buildAfterSelectSQL(TableRecords beforeImage);

    /**
     * Gets build undo row
     *
     * @param beforeImage before image
     * @param afterImage  after image
     * @return Map<SQLType, List < Row>>
     */
    Map<SQLType, List<Row>> buildUndoRow(TableRecords beforeImage, TableRecords afterImage);
}
