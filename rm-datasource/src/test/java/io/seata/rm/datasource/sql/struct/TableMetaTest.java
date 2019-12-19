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

import com.google.common.collect.Lists;
import io.seata.common.exception.NotSupportYetException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author will
 */
public class TableMetaTest {

    @Test
    public void testTableMeta() {
        TableMeta tableMeta = new TableMeta();
        Assertions.assertEquals(tableMeta, tableMeta);
        Assertions.assertEquals(tableMeta, new TableMeta());
        Assertions.assertEquals(tableMeta.hashCode(), tableMeta.hashCode());
        Assertions.assertNotEquals(tableMeta, new String());

        TableMeta other = new TableMeta();
        other.setTableName("");
        Assertions.assertNotEquals(tableMeta, other);

        other = new TableMeta();
        other.getAllColumns().put("columnName", new ColumnMeta());
        Assertions.assertNotEquals(tableMeta, other);

        other = new TableMeta();
        other.getAllIndexes().put("indexName", new IndexMeta());
        Assertions.assertNotEquals(tableMeta, other);
    }

    @Test
    public void testGetColumnMeta() {
        TableMeta tableMeta = new TableMeta();
        tableMeta.getAllColumns().put("id", new ColumnMeta());
        tableMeta.getAllColumns().put("name", new ColumnMeta());
        Assertions.assertNull(tableMeta.getColumnMeta("`id`"));
        Assertions.assertNotNull(tableMeta.getColumnMeta("name"));
    }

    @Test
    public void testGetAutoIncreaseColumn() {
        TableMeta tableMeta = new TableMeta();
        ColumnMeta id = new ColumnMeta();
        id.setIsAutoincrement("YES");
        tableMeta.getAllColumns().put("id", id);
        Assertions.assertNotNull(tableMeta.getAutoIncreaseColumn());

        tableMeta = new TableMeta();
        tableMeta.getAllColumns().put("name", new ColumnMeta());
        Assertions.assertNull(tableMeta.getAutoIncreaseColumn());
    }

    @Test
    public void testGetPrimaryKeyMap() {
        TableMeta tableMeta = new TableMeta();
        IndexMeta primary = new IndexMeta();
        primary.setIndextype(IndexType.PRIMARY);
        primary.setValues(Lists.newArrayList(new ColumnMeta()));
        tableMeta.getAllIndexes().put("id", primary);
        Assertions.assertNotNull(tableMeta.getPrimaryKeyMap());

        Assertions.assertThrows(NotSupportYetException.class, () -> {
            IndexMeta primary2 = new IndexMeta();
            primary2.setIndextype(IndexType.PRIMARY);
            ColumnMeta columnMeta = new ColumnMeta();
            columnMeta.setColumnName("id2");
            primary2.setValues(Lists.newArrayList(columnMeta));
            tableMeta.getAllIndexes().put("id2", primary2);
            tableMeta.getPrimaryKeyMap();
        });
    }

    @Test
    public void testGetPrimaryKeyOnlyName() {
        TableMeta tableMeta = new TableMeta();
        IndexMeta primary = new IndexMeta();
        primary.setIndextype(IndexType.PRIMARY);
        primary.setValues(Lists.newArrayList(new ColumnMeta()));
        tableMeta.getAllIndexes().put("id", primary);
        Assertions.assertTrue(tableMeta.getPrimaryKeyOnlyName().size() >= 1);
    }

    @Test
    public void testGetPkName() {
        TableMeta tableMeta = new TableMeta();
        IndexMeta primary = new IndexMeta();
        primary.setIndextype(IndexType.PRIMARY);
        ColumnMeta columnMeta = new ColumnMeta();
        columnMeta.setColumnName("id");
        primary.setValues(Lists.newArrayList(columnMeta));
        tableMeta.getAllIndexes().put("id", primary);
        Assertions.assertEquals("id", tableMeta.getPkName());
    }

    @Test
    public void testContainsPK() {
        TableMeta tableMeta = new TableMeta();
        Assertions.assertFalse(tableMeta.containsPK(null));
        Throwable exception = Assertions.assertThrows(NotSupportYetException.class, () -> {
            tableMeta.containsPK(Lists.newArrayList("id"));
        });
        Assertions.assertEquals(tableMeta.getTableName() + " needs to contain the primary key.",
            exception.getMessage());
        IndexMeta primary = new IndexMeta();
        primary.setIndextype(IndexType.PRIMARY);
        ColumnMeta columnMeta = new ColumnMeta();
        columnMeta.setColumnName("id");
        primary.setValues(Lists.newArrayList(columnMeta));
        tableMeta.getAllIndexes().put("id", primary);
        Assertions.assertTrue(tableMeta.containsPK(Lists.newArrayList("id")));
        Assertions.assertTrue(tableMeta.containsPK(Lists.newArrayList("ID")));
    }
}
