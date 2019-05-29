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
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;

import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The type Data compare utils.
 *
 * @author Geng Zhang
 */
public class DataCompareUtils {

    /**
     * Is field equals.
     *
     * @param f0 the f0
     * @param f1  the f1
     * @return the boolean
     */
    public static boolean isFieldEquals(Field f0, Field f1) {
        if (f0 == null) {
            return f1 == null;
        } else {
            if (f1 == null) {
                return false;
            } else {
                if (StringUtils.equalsIgnoreCase(f0.getName(), f1.getName())
                        && f0.getType() == f1.getType()) {
                    if (f0.getValue() == null) {
                        return f1.getValue() == null;
                    } else {
                        if (f1.getValue() == null) {
                            return false;
                        } else {
                            int f0Type = f0.getType();
                            int f1Type = f1.getType();
                            if (f0Type == Types.TIMESTAMP && f0.getValue().getClass().equals(String.class)) {
                                f0.setValue(Timestamp.valueOf(f0.getValue().toString()));
                            }
                            if (f1Type == Types.TIMESTAMP && f1.getValue().getClass().equals(String.class)) {
                                f1.setValue(Timestamp.valueOf(f1.getValue().toString()));
                            }
                            return f0.getValue().equals(f1.getValue());
                        }
                    }
                } else {
                    return false;
                }
            }
        }
    }

    /**
     * Is image equals.
     *
     * @param beforeImage the before image
     * @param afterImage  the after image
     * @return boolean
     */
    public static boolean isRecordsEquals(TableRecords beforeImage, TableRecords afterImage) {
        if (beforeImage == null) {
            return afterImage == null;
        } else {
            if (afterImage == null) {
                return false;
            } else {
                if (beforeImage.getTableName().equalsIgnoreCase(afterImage.getTableName())
                        && CollectionUtils.isSizeEquals(beforeImage.getRows(), afterImage.getRows())) {
                    return compareRows(beforeImage.getTableMeta(), beforeImage.getRows(), afterImage.getRows());
                } else {
                    return false;
                }
            }
        }
    }


    /**
     * Is rows equals.
     *
     * @param tableMetaData the table meta data
     * @param oldRows       the old rows
     * @param newRows       the new rows
     * @return the boolean
     */
    public static boolean isRowsEquals(TableMeta tableMetaData, List<Row> oldRows, List<Row> newRows) {
        return CollectionUtils.isSizeEquals(oldRows, newRows) && compareRows(tableMetaData, oldRows, newRows);
    }

    private static boolean compareRows(TableMeta tableMetaData, List<Row> oldRows, List<Row> newRows) {
        // old row to map
        Map<String, Map<String, Field>> oldRowsMap = rowListToMap(oldRows, tableMetaData.getPkName());
        // new row to map
        Map<String, Map<String, Field>> newRowsMap = rowListToMap(newRows, tableMetaData.getPkName());
        // compare data
        for (String rowKey : oldRowsMap.keySet()) {
            Map<String, Field> oldRow = oldRowsMap.get(rowKey);
            Map<String, Field> newRow = newRowsMap.get(rowKey);
            if (newRow == null) {
                return false;
            }
            for (String fieldName : oldRow.keySet()) {
                Field oldField = oldRow.get(fieldName);
                Field newField = newRow.get(fieldName);
                if (newField == null) {
                    return false;
                }
                if (!isFieldEquals(oldField, newField)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static Map<String, Map<String, Field>> rowListToMap(List<Row> rowList, String primaryKey) {
        // {value of primaryKey, value of all columns}
        Map<String, Map<String, Field>> rowMap = new HashMap<>();
        for (Row row : rowList) {
            // {uppercase fieldName : field}
            Map<String, Field> colsMap = new HashMap<>();
            String rowKey = null;
            for (int j = 0; j < row.getFields().size(); j++) {
                Field field = row.getFields().get(j);
                if (field.getName().equalsIgnoreCase(primaryKey)) {
                    rowKey = String.valueOf(field.getValue());
                }
                colsMap.put(field.getName().trim().toUpperCase(), field);
            }
            rowMap.put(rowKey, colsMap);
        }
        return rowMap;
    }

}
