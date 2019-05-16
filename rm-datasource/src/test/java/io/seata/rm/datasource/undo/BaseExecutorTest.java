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
package io.seata.rm.datasource.undo;

import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.KeyType;
import io.seata.rm.datasource.sql.struct.Row;

/**
 * @author Geng Zhang
 */
public class BaseExecutorTest {

    protected static Field addField(Row row, String name, int type, Object value) {
        Field field = new Field(name, type, value);
        if (name.equalsIgnoreCase("id")) {
            field.setKeyType(KeyType.PrimaryKey);
        }
        row.add(field);
        return field;
    }
}
