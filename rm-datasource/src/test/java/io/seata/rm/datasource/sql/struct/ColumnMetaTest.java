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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author will
 * @date 2019/9/28
 */
public class ColumnMetaTest {

    @Test
    public void testColumnMeta() {
        ColumnMeta columnMeta = new ColumnMeta();
        Assertions.assertNotNull(columnMeta.toString());
        Assertions.assertEquals(columnMeta, new ColumnMeta());
        columnMeta.setIsAutoincrement("Yes");
        Assertions.assertTrue(columnMeta.isAutoincrement());
        Assertions.assertEquals(columnMeta, columnMeta);
        Assertions.assertEquals(columnMeta.hashCode(), columnMeta.hashCode());
        Assertions.assertNotEquals(columnMeta, new String());

        ColumnMeta other = new ColumnMeta();
        other.setTableCat("");
        Assertions.assertNotEquals(columnMeta, other);

        other = new ColumnMeta();
        other.setTableSchemaName("");
        Assertions.assertNotEquals(columnMeta, other);

        other = new ColumnMeta();
        other.setTableName("");
        Assertions.assertNotEquals(columnMeta, other);

        other = new ColumnMeta();
        other.setColumnName("");
        Assertions.assertNotEquals(columnMeta, other);

        other = new ColumnMeta();
        other.setDataType(1);
        Assertions.assertNotEquals(columnMeta, other);

        other = new ColumnMeta();
        other.setDataTypeName("");
        Assertions.assertNotEquals(columnMeta, other);

        other = new ColumnMeta();
        other.setColumnSize(1);
        Assertions.assertNotEquals(columnMeta, other);

        other = new ColumnMeta();
        other.setDecimalDigits(1);
        Assertions.assertNotEquals(columnMeta, other);

        other = new ColumnMeta();
        other.setNumPrecRadix(1);
        Assertions.assertNotEquals(columnMeta, other);

        other = new ColumnMeta();
        other.setNullAble(1);
        Assertions.assertNotEquals(columnMeta, other);

        other = new ColumnMeta();
        other.setRemarks("");
        Assertions.assertNotEquals(columnMeta, other);

        other = new ColumnMeta();
        other.setColumnDef("");
        Assertions.assertNotEquals(columnMeta, other);

        other = new ColumnMeta();
        other.setSqlDataType(1);
        Assertions.assertNotEquals(columnMeta, other);

        other = new ColumnMeta();
        other.setSqlDatetimeSub(1);
        Assertions.assertNotEquals(columnMeta, other);

        other = new ColumnMeta();
        other.setCharOctetLength(1);
        Assertions.assertNotEquals(columnMeta, other);

        other = new ColumnMeta();
        other.setOrdinalPosition(1);
        Assertions.assertNotEquals(columnMeta, other);

        other = new ColumnMeta();
        other.setIsNullAble("");
        Assertions.assertNotEquals(columnMeta, other);

        other = new ColumnMeta();
        other.setIsAutoincrement("");
        Assertions.assertNotEquals(columnMeta, other);

    }
}
