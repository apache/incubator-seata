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
package io.seata.rm.datasource.exec.handler.after;

import com.google.common.base.Joiner;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.loader.LoadLevel;
import io.seata.common.util.CollectionUtils;
import io.seata.rm.datasource.exec.constant.SQLTypeConstant;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.sqlparser.SQLType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: lyx
 */
@LoadLevel(name = SQLTypeConstant.INSERT_ON_DUPLICATE_UPDATE)
public class InsertOrUpdateHandler extends BaseAfterHandler {

    private static final String COLUMN_SEPARATOR = "|";

    @Override
    public String buildAfterSelectSQL(TableRecords beforeImage) {
        List<Row> rows = beforeImage.getRows();
        Map<String, ArrayList<Object>> primaryValueMap = new HashMap<>();
        Map<Integer, ArrayList<Object>> primaryIndexValueMap = new HashMap<>();
        rows.forEach(m -> {
            List<Field> fields = m.primaryKeys();
            fields.forEach(f -> {
                ArrayList<Object> values = primaryValueMap.computeIfAbsent(f.getName(), v -> new ArrayList<>());
                values.add(f.getValue());
            });
        });
        StringBuilder afterImageSql = new StringBuilder();
        if (CollectionUtils.isNotEmpty(primaryValueMap)) {
            afterImageSql.append(" OR ").append("(").append(Joiner.on(",").join(primaryValueMap.keySet())).append(")");
            afterImageSql.append(" IN(");
            primaryValueMap.forEach((k, v) -> {
                for (int i = 0; i < v.size(); i++) {
                    primaryIndexValueMap.computeIfAbsent(i, e -> new ArrayList<>()).add(v.get(i));
                }
            });
            primaryIndexValueMap.values().forEach(indexValue -> afterImageSql.append("(").append(Joiner.on(",").join(indexValue)).append(")").append(","));
            afterImageSql.deleteCharAt(afterImageSql.length() - 1);
            afterImageSql.append(")");
        }
        return afterImageSql.toString();
    }

    @Override
    public Map<SQLType, List<Row>> buildUndoRow(TableRecords beforeImage, TableRecords afterImage) {
        Map<SQLType, List<Row>> result = new HashMap<>(2, 1F);
        List<Row> beforeImageRows = beforeImage.getRows();
        List<String> beforePrimaryValues = new ArrayList<>(beforeImageRows.size());
        for (Row r : beforeImageRows) {
            String primaryValue = "";
            for (Field f : r.primaryKeys()) {
                primaryValue = primaryValue + f.getValue() + COLUMN_SEPARATOR;
            }
            beforePrimaryValues.add(primaryValue);
        }
        List<Row> insertRows = new ArrayList<>();
        List<Row> updateRows = new ArrayList<>();
        List<Row> afterImageRows = afterImage.getRows();
        for (Row r : afterImageRows) {
            String primaryValue = "";
            for (Field f : r.primaryKeys()) {
                primaryValue = primaryValue + f.getValue() + COLUMN_SEPARATOR;
            }
            if (beforePrimaryValues.contains(primaryValue)) {
                updateRows.add(r);
            } else {
                insertRows.add(r);
            }
        }
        if (CollectionUtils.isNotEmpty(updateRows)) {
            if (beforeImage.getRows().size() != updateRows.size()) {
                throw new ShouldNeverHappenException("Before image size is not equaled to after image size, probably because you updated the primary keys.");
            }
            result.put(SQLType.UPDATE, updateRows);
        }
        if (CollectionUtils.isNotEmpty(insertRows)) {
            result.put(SQLType.INSERT, insertRows);
        }
        return result;
    }
}
