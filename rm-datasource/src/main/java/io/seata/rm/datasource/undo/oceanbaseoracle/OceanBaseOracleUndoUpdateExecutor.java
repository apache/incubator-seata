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
package io.seata.rm.datasource.undo.oceanbaseoracle;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.CollectionUtils;
import io.seata.rm.datasource.ColumnUtils;
import io.seata.rm.datasource.SqlGenerateUtils;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.AbstractUndoExecutor;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.sqlparser.util.JdbcConstants;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Undo log executor for update operation in OceanBaseOracle
 *
 * @author hsien999
 */
public class OceanBaseOracleUndoUpdateExecutor extends AbstractUndoExecutor {

    public OceanBaseOracleUndoUpdateExecutor(SQLUndoLog sqlUndoLog) {
        super(sqlUndoLog);
    }

    @Override
    protected String buildUndoSQL() {
        // TODO support for modified pks
        // We assume that the set item in the update operation does not contain a primary key.
        // when the primary key was updated, it is unable to locate the primary key based on the before image directly

        TableRecords beforeImage = sqlUndoLog.getBeforeImage();
        List<Row> beforeImageRows = beforeImage.getRows();
        if (CollectionUtils.isEmpty(beforeImageRows)) {
            throw new ShouldNeverHappenException("Invalid undo log");
        }
        Row row = beforeImageRows.get(0);
        List<Field> nonPkFields = row.nonPrimaryKeys();
        // undo log of before image for update sql saves all fields from table meta(escapes required)
        String updateColumns = nonPkFields.stream()
            .map(field -> ColumnUtils.addEscape(field.getName(), JdbcConstants.OCEANBASE_ORACLE) + " = ?")
            .collect(Collectors.joining(", "));

        List<String> pkNameList = getOrderedPkList(beforeImage, row, JdbcConstants.OCEANBASE_ORACLE).stream()
            .map(Field::getName)
            .collect(Collectors.toList());
        String whereSql = SqlGenerateUtils.buildWhereConditionByPKs(pkNameList, JdbcConstants.OCEANBASE_ORACLE);

        // UPDATE test SET x = ?, y = ?, z = ? WHERE pk1 in (?) pk2 in (?)
        return "UPDATE " + sqlUndoLog.getTableName() + " SET " + updateColumns + " WHERE " + whereSql;
    }

    @Override
    protected TableRecords getUndoRows() {
        return sqlUndoLog.getBeforeImage();
    }
}
