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
package io.seata.rm.datasource;

import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.core.model.Result;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.sqlparser.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.AbstractUndoLogManager;
import io.seata.rm.datasource.undo.parser.FastjsonUndoLogParser;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.HashMap;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * The type Data compare utils.
 *
 * @author Geng Zhang
 */
public class DataCompareUtils {

    private DataCompareUtils() {

    }

    /**
     * Is field equals result.
     *
     * @param f0 the f 0
     * @param f1 the f 1
     * @return the result
     */
    public static Result<Boolean> isFieldEquals(Field f0, Field f1) {
        if (f0 == null) {
            return Result.build(f1 == null);
        } else {
            if (f1 == null) {
                return Result.build(false);
            } else {
                if (StringUtils.equalsIgnoreCase(f0.getName(), f1.getName())
                        && f0.getType() == f1.getType()) {
                    if (f0.getValue() == null) {
                        return Result.build(f1.getValue() == null);
                    } else {
                        if (f1.getValue() == null) {
                            return Result.buildWithParams(false, "Field not equals, name {}, new value is null", f0.getName());
                        } else {
                            String currentSerializer = AbstractUndoLogManager.getCurrentSerializer();
                            if (StringUtils.equals(currentSerializer, FastjsonUndoLogParser.NAME)) {
                                convertType(f0, f1);
                            }
                            boolean result = Objects.deepEquals(f0.getValue(), f1.getValue());
                            if (result) {
                                return Result.ok();
                            } else {
                                return Result.buildWithParams(false, "Field not equals, name {}, old value {}, new value {}", f0.getName(), f0.getValue(), f1.getValue());
                            }
                        }
                    }
                } else {
                    return Result.buildWithParams(false, "Field not equals, old name {} type {}, new name {} type {}", f0.getName(), f0.getType(), f1.getName(), f1.getType());
                }
            }
        }
    }

    private static void convertType(Field f0, Field f1) {
        int f0Type = f0.getType();
        int f1Type = f1.getType();
        if (f0Type == Types.DATE && f0.getValue().getClass().equals(String.class)) {
            String[] strings = f0.getValue().toString().split(" ");
            f0.setValue(Date.valueOf(strings[0]));
        }
        if (f1Type == Types.DATE && f1.getValue().getClass().equals(String.class)) {
            String[] strings = f1.getValue().toString().split(" ");
            f1.setValue(Date.valueOf(strings[0]));
        }
        if (f0Type == Types.TIME && f0.getValue().getClass().equals(String.class)) {
            f0.setValue(Time.valueOf(f0.getValue().toString()));
        }
        if (f1Type == Types.TIME && f1.getValue().getClass().equals(String.class)) {
            f1.setValue(Time.valueOf(f1.getValue().toString()));
        }
        if (f0Type == Types.TIMESTAMP && f0.getValue().getClass().equals(String.class)) {
            if (f1.getValue().getClass().equals(LocalDateTime.class)) {
                f0.setValue(LocalDateTime.parse(f0.getValue().toString()));
            } else {
                f0.setValue(Timestamp.valueOf(f0.getValue().toString()));
            }
        }
        if (f1Type == Types.TIMESTAMP && f1.getValue().getClass().equals(String.class)) {
            f1.setValue(Timestamp.valueOf(f1.getValue().toString()));
        }
        if (f0Type == Types.DECIMAL && f0.getValue().getClass().equals(Integer.class)) {
            f0.setValue(new BigDecimal(f0.getValue().toString()));
        }
        if (f1Type == Types.DECIMAL && f1.getValue().getClass().equals(Integer.class)) {
            f1.setValue(new BigDecimal(f1.getValue().toString()));
        }
        if (f0Type == Types.BIGINT && f0.getValue().getClass().equals(Integer.class)) {
            f0.setValue(Long.parseLong(f0.getValue().toString()));
        }
        if (f1Type == Types.BIGINT && f1.getValue().getClass().equals(Integer.class)) {
            f1.setValue(Long.parseLong(f1.getValue().toString()));
        }
    }

    /**
     * Is records equals result.
     *
     * @param beforeImage the before image
     * @param afterImage  the after image
     * @return the result
     */
    public static Result<Boolean> isRecordsEquals(TableRecords beforeImage, TableRecords afterImage) {
        if (beforeImage == null) {
            return Result.build(afterImage == null, null);
        } else {
            if (afterImage == null) {
                return Result.build(false, null);
            }
            if (beforeImage.getTableName().equalsIgnoreCase(afterImage.getTableName())
                    && CollectionUtils.isSizeEquals(beforeImage.getRows(), afterImage.getRows())) {
                //when image is EmptyTableRecords, getTableMeta will throw an exception
                if (CollectionUtils.isEmpty(beforeImage.getRows())) {
                    return Result.ok();
                }
                return compareRows(beforeImage.getTableMeta(), beforeImage.getRows(), afterImage.getRows());
            } else {
                return Result.build(false, null);
            }
        }
    }

    /**
     * Is rows equals result.
     *
     * @param tableMetaData the table meta data
     * @param oldRows       the old rows
     * @param newRows       the new rows
     * @return the result
     */
    public static Result<Boolean> isRowsEquals(TableMeta tableMetaData, List<Row> oldRows, List<Row> newRows) {
        if (!CollectionUtils.isSizeEquals(oldRows, newRows)) {
            return Result.build(false, null);
        }
        return compareRows(tableMetaData, oldRows, newRows);
    }

    private static Result<Boolean> compareRows(TableMeta tableMetaData, List<Row> oldRows, List<Row> newRows) {
        // old row to map
        Map<String, Map<String, Field>> oldRowsMap = rowListToMap(oldRows, tableMetaData.getPrimaryKeyOnlyName());
        // new row to map
        Map<String, Map<String, Field>> newRowsMap = rowListToMap(newRows, tableMetaData.getPrimaryKeyOnlyName());
        // compare data
        for (Map.Entry<String, Map<String, Field>> oldEntry : oldRowsMap.entrySet()) {
            String key = oldEntry.getKey();
            Map<String, Field> oldRow = oldEntry.getValue();
            Map<String, Field> newRow = newRowsMap.get(key);
            if (newRow == null) {
                return Result.buildWithParams(false, "compare row failed, rowKey {}, reason [newRow is null]", key);
            }
            for (Map.Entry<String, Field> oldRowEntry : oldRow.entrySet()) {
                String fieldName = oldRowEntry.getKey();
                Field oldField = oldRowEntry.getValue();
                Field newField = newRow.get(fieldName);
                if (newField == null) {
                    return Result.buildWithParams(false, "compare row failed, rowKey {}, fieldName {}, reason [newField is null]", key, fieldName);
                }
                Result<Boolean> oldEqualsNewFieldResult = isFieldEquals(oldField, newField);
                if (!oldEqualsNewFieldResult.getResult()) {
                    return oldEqualsNewFieldResult;
                }
            }
        }
        return Result.ok();
    }

    /**
     * Row list to map map.
     *
     * @param rowList        the row list
     * @param primaryKeyList the primary key list
     * @return the map
     */
    public static Map<String, Map<String, Field>> rowListToMap(List<Row> rowList, List<String> primaryKeyList) {
        // {value of primaryKey, value of all columns}
        Map<String, Map<String, Field>> rowMap = new HashMap<>();
        for (Row row : rowList) {
            //ensure the order of column
            List<Field> rowFieldList = row.getFields().stream()
                    .sorted(Comparator.comparing(Field::getName))
                    .collect(Collectors.toList());
            // {uppercase fieldName : field}
            Map<String, Field> colsMap = new HashMap<>();
            StringBuilder rowKey = new StringBuilder();
            boolean firstUnderline = false;
            for (int j = 0; j < rowFieldList.size(); j++) {
                Field field = rowFieldList.get(j);
                if (primaryKeyList.stream().anyMatch(e -> field.getName().equals(e))) {
                    if (firstUnderline && j > 0) {
                        rowKey.append("_");
                    }
                    rowKey.append(String.valueOf(field.getValue()));
                    firstUnderline = true;
                }
                colsMap.put(field.getName().trim().toUpperCase(), field);
            }
            rowMap.put(rowKey.toString(), colsMap);
        }
        return rowMap;
    }


}
