/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.sqlparser.struct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class IndexMetaTest {
    @Test
    public void testGetSetValues() {
        IndexMeta indexMeta = new IndexMeta();
        List<ColumnMeta> values = Arrays.asList(new ColumnMeta(), new ColumnMeta());
        indexMeta.setValues(values);
        assertEquals(values, indexMeta.getValues());
    }

    @Test
    public void testIsSetNonUnique() {
        IndexMeta indexMeta = new IndexMeta();
        indexMeta.setNonUnique(true);
        assertTrue(indexMeta.isNonUnique());
        indexMeta.setNonUnique(false);
        assertFalse(indexMeta.isNonUnique());
    }

    @Test
    public void testGetSetIndexQualifier() {
        IndexMeta indexMeta = new IndexMeta();
        String indexQualifier = "qualifier";
        indexMeta.setIndexQualifier(indexQualifier);
        assertEquals(indexQualifier, indexMeta.getIndexQualifier());
    }

    @Test
    public void testGetSetIndexName() {
        IndexMeta indexMeta = new IndexMeta();
        String indexName = "indexName";
        indexMeta.setIndexName(indexName);
        assertEquals(indexName, indexMeta.getIndexName());
    }

    @Test
    public void testGetSetType() {
        IndexMeta indexMeta = new IndexMeta();
        short type = 1;
        indexMeta.setType(type);
        assertEquals(type, indexMeta.getType());
    }

    @Test
    public void testGetSetAscOrDesc() {
        IndexMeta indexMeta = new IndexMeta();
        String ascOrDesc = "A";
        indexMeta.setAscOrDesc(ascOrDesc);
        assertEquals(ascOrDesc, indexMeta.getAscOrDesc());
    }

    @Test
    public void testGetSetCardinality() {
        IndexMeta indexMeta = new IndexMeta();
        long cardinality = 100L;
        indexMeta.setCardinality(cardinality);
        assertEquals(cardinality, indexMeta.getCardinality());
    }

    @Test
    public void testGetSetOrdinalPosition() {
        IndexMeta indexMeta = new IndexMeta();
        int ordinalPosition = 1;
        indexMeta.setOrdinalPosition(ordinalPosition);
        assertEquals(ordinalPosition, indexMeta.getOrdinalPosition());
    }

    @Test
    public void testGetSetIndextype() {
        IndexMeta indexMeta = new IndexMeta();
        IndexType indextype = IndexType.NORMAL;
        indexMeta.setIndextype(indextype);
        assertEquals(indextype, indexMeta.getIndextype(), "Indextype should be set and retrieved correctly");
    }

    @Test
    public void testEqualsSameObject() {
        IndexMeta indexMeta = new IndexMeta();
        assertTrue(indexMeta.equals(indexMeta), "An object should be equal to itself");
    }

    @Test
    public void testEqualsNullObject() {
        IndexMeta indexMeta = new IndexMeta();
        assertFalse(indexMeta.equals(null), "An object should not be equal to null");
    }

    @Test
    public void testEqualsDifferentType() {
        IndexMeta indexMeta = new IndexMeta();
        String differentType = "I am not an IndexMeta";
        assertFalse(indexMeta.equals(differentType), "An object should not be equal to an object of a different type");
    }

    @Test
    public void testEqualsIdenticalObjects() {
        IndexType indexType = IndexType.PRIMARY;
        IndexMeta indexMeta1 = new IndexMeta();
        indexMeta1.setValues(Arrays.asList(new ColumnMeta(), new ColumnMeta()));
        indexMeta1.setNonUnique(true);
        indexMeta1.setIndexQualifier("qualifier");
        indexMeta1.setIndexName("indexName");
        indexMeta1.setType((short) 1);
        indexMeta1.setIndextype(indexType);
        indexMeta1.setAscOrDesc("A");
        indexMeta1.setOrdinalPosition(1);

        IndexType indexType2 = IndexType.PRIMARY;
        IndexMeta indexMeta2 = new IndexMeta();
        indexMeta2.setValues(Arrays.asList(new ColumnMeta(), new ColumnMeta()));
        indexMeta2.setNonUnique(true);
        indexMeta2.setIndexQualifier("qualifier");
        indexMeta2.setIndexName("indexName");
        indexMeta2.setType((short) 1);
        indexMeta2.setIndextype(indexType2);
        indexMeta2.setAscOrDesc("A");
        indexMeta2.setOrdinalPosition(1);

        assertTrue(indexMeta1.equals(indexMeta2), "Two objects with identical field values should be equal");
    }

    @Test
    public void testEqualsDifferentObjects() {
        IndexMeta indexMeta1 = new IndexMeta();
        indexMeta1.setValues(Arrays.asList(new ColumnMeta(), new ColumnMeta(), new ColumnMeta()));
        indexMeta1.setNonUnique(true);
        indexMeta1.setIndexQualifier("qualifier");
        indexMeta1.setIndexName("indexName");
        indexMeta1.setType((short) 1);
        indexMeta1.setIndextype(IndexType.UNIQUE);
        indexMeta1.setAscOrDesc("A");
        indexMeta1.setOrdinalPosition(1);

        IndexMeta indexMeta2 = new IndexMeta();
        indexMeta2.setValues(Arrays.asList(new ColumnMeta(), new ColumnMeta(), new ColumnMeta()));
        indexMeta2.setNonUnique(false);
        indexMeta2.setIndexQualifier("differentQualifier");
        indexMeta2.setIndexName("differentIndexName");
        indexMeta2.setType((short) 2);
        indexMeta2.setIndextype(IndexType.FULL_TEXT);
        indexMeta2.setAscOrDesc("D");
        indexMeta2.setOrdinalPosition(2);

        assertFalse(indexMeta1.equals(indexMeta2), "Two objects with different field values should not be equal");
    }

    @Test
    public void testHashCodeConsistency() {
        ColumnMeta columnMeta1 = new ColumnMeta();
        columnMeta1.setTableCat("tableCat");
        columnMeta1.setTableSchemaName("schemaName");
        columnMeta1.setTableName("tableName");
        columnMeta1.setColumnName("columnName");
        columnMeta1.setDataType(1);
        columnMeta1.setDataTypeName("dataTypeName");
        columnMeta1.setColumnSize(100);
        columnMeta1.setDecimalDigits(10);
        columnMeta1.setNumPrecRadix(10);
        columnMeta1.setNullAble(1);
        columnMeta1.setRemarks("remarks");
        columnMeta1.setColumnDef("columnDef");
        columnMeta1.setSqlDataType(1);
        columnMeta1.setSqlDatetimeSub(1);
        columnMeta1.setCharOctetLength(100);
        columnMeta1.setOrdinalPosition(1);
        columnMeta1.setIsNullAble("YES");
        columnMeta1.setIsAutoincrement("YES");
        columnMeta1.setOnUpdate(true);
        columnMeta1.setCaseSensitive(true);

        ColumnMeta columnMeta2 = new ColumnMeta();
        columnMeta2.setTableCat("tableCat");
        columnMeta2.setTableSchemaName("schemaName");
        columnMeta2.setTableName("tableName");
        columnMeta2.setColumnName("columnName");
        columnMeta2.setDataType(1);
        columnMeta2.setDataTypeName("dataTypeName");
        columnMeta2.setColumnSize(100);
        columnMeta2.setDecimalDigits(10);
        columnMeta2.setNumPrecRadix(10);
        columnMeta2.setNullAble(1);
        columnMeta2.setRemarks("remarks");
        columnMeta2.setColumnDef("columnDef");
        columnMeta2.setSqlDataType(1);
        columnMeta2.setSqlDatetimeSub(1);
        columnMeta2.setCharOctetLength(100);
        columnMeta2.setOrdinalPosition(1);
        columnMeta2.setIsNullAble("YES");
        columnMeta2.setIsAutoincrement("YES");
        columnMeta2.setOnUpdate(true);
        columnMeta2.setCaseSensitive(true);

        // Check if hash codes are consistent
        assertEquals(columnMeta1.hashCode(), columnMeta2.hashCode(), "Hash codes should be the same for equal objects");
    }

    @Test
    public void testHashCodeDifference() {
        ColumnMeta columnMeta1 = new ColumnMeta();
        columnMeta1.setTableCat("tableCat1");
        columnMeta1.setTableSchemaName("schemaName1");
        columnMeta1.setTableName("tableName1");
        columnMeta1.setColumnName("columnName1");
        columnMeta1.setDataType(1);
        columnMeta1.setDataTypeName("dataTypeName1");
        columnMeta1.setColumnSize(100);
        columnMeta1.setDecimalDigits(10);
        columnMeta1.setNumPrecRadix(10);
        columnMeta1.setNullAble(1);
        columnMeta1.setRemarks("remarks1");
        columnMeta1.setColumnDef("columnDef1");
        columnMeta1.setSqlDataType(1);
        columnMeta1.setSqlDatetimeSub(1);
        columnMeta1.setCharOctetLength(100);
        columnMeta1.setOrdinalPosition(1);
        columnMeta1.setIsNullAble("YES");
        columnMeta1.setIsAutoincrement("YES");
        columnMeta1.setOnUpdate(true);
        columnMeta1.setCaseSensitive(true);

        ColumnMeta columnMeta2 = new ColumnMeta();
        columnMeta2.setTableCat("tableCat2");
        columnMeta2.setTableSchemaName("schemaName2");
        columnMeta2.setTableName("tableName2");
        columnMeta2.setColumnName("columnName2");
        columnMeta2.setDataType(2);
        columnMeta2.setDataTypeName("dataTypeName2");
        columnMeta2.setColumnSize(200);
        columnMeta2.setDecimalDigits(20);
        columnMeta2.setNumPrecRadix(20);
        columnMeta2.setNullAble(0);
        columnMeta2.setRemarks("remarks2");
        columnMeta2.setColumnDef("columnDef2");
        columnMeta2.setSqlDataType(2);
        columnMeta2.setSqlDatetimeSub(2);
        columnMeta2.setCharOctetLength(200);
        columnMeta2.setOrdinalPosition(2);
        columnMeta2.setIsNullAble("NO");
        columnMeta2.setIsAutoincrement("NO");
        columnMeta2.setOnUpdate(false);
        columnMeta2.setCaseSensitive(false);

        // Check if hash codes are different for different objects
        assertNotEquals(columnMeta1.hashCode(), columnMeta2.hashCode(),
                "Hash codes should be different for non-equal objects");
    }

}
