/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.rm.datasource.sql.struct;

import java.util.*;

import com.alibaba.fescar.common.exception.NotSupportYetException;

/**
 * The type Row.
 */
public class Row {

    private List<Field> fields = new ArrayList<Field>();

    /**
     * Instantiates a new Row.
     */
    public Row() {
    }

    /**
     * Gets fields.
     *
     * @return the fields
     */
    public List<Field> getFields() {
        return fields;
    }

    /**
     * Sets fields.
     *
     * @param fields the fields
     */
    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    /**
     * Add.
     *
     * @param field the field
     */
    public void add(Field field) {
        fields.add(field);
    }

    /**
     * Primary keys list.
     *
     * @return the list
     */
    public List<Field> primaryKeys() {
        List<Field> pkFields = new ArrayList<>();
        for (Field field : fields) {
            if (KeyType.PrimaryKey == field.getKeyType()) {
                pkFields.add(field);
            }
        }
        if (pkFields.size() > 1) {
            throw new NotSupportYetException("Multi-PK");
        }
        return pkFields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Row row = (Row) o;
        int countSelf = 0, countOther = 0;
        for (Field field : this.fields) {
            if (field.getValue() == null) continue;
            countSelf++;
        }
        for (Field field : row.fields) {
            if (field.getValue() == null) continue;
            countOther++;
        }
        if (countSelf != countOther) return false;

        Map<String, Field> fieldMap = new HashMap<>(row.fields.size() << 1);
        for (Field field : row.fields) {
            fieldMap.put(field.getName(), field);
        }
        for (Field field : row.fields) {
            Field otherField = fieldMap.get(field.getName());
            if (otherField == null) {
                if (field.getValue() != null) {
                    return false;
                }
            } else if (!field.equals(otherField)){
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fields);
    }
}
