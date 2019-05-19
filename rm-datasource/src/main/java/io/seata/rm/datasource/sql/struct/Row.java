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
import java.util.List;

import io.seata.common.exception.NotSupportYetException;

/**
 * The type Row.
 *
 * @author sharajava
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
     * @return the Primary keys list
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

    /**
     * Non-primary keys list.
     *
     * @return the non-primary list
     */
    public List<Field> nonPrimaryKeys() {
        List<Field> nonPkFields = new ArrayList<>();
        for (Field field : fields) {
            if (KeyType.PrimaryKey != field.getKeyType()) {
                nonPkFields.add(field);
            }
        }
        return nonPkFields;
    }
}
