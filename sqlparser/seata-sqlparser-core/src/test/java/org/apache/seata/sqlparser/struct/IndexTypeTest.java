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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class IndexTypeTest {

    @Test
    public void testValue() {
        assertEquals(0, IndexType.PRIMARY.value(), "Value of PRIMARY index type should be 0");
        assertEquals(1, IndexType.NORMAL.value(), "Value of NORMAL index type should be 1");
        assertEquals(2, IndexType.UNIQUE.value(), "Value of UNIQUE index type should be 2");
        assertEquals(3, IndexType.FULL_TEXT.value(), "Value of FULL_TEXT index type should be 3");
    }

    @Test
    public void testValueOf() {
        assertEquals(IndexType.PRIMARY, IndexType.valueOf(0), "IndexType of value 0 should be PRIMARY");
        assertEquals(IndexType.NORMAL, IndexType.valueOf(1), "IndexType of value 1 should be NORMAL");
        assertEquals(IndexType.UNIQUE, IndexType.valueOf(2), "IndexType of value 2 should be UNIQUE");
        assertEquals(IndexType.FULL_TEXT, IndexType.valueOf(3), "IndexType of value 3 should be FULL_TEXT");
    }

    @Test
    public void testInvalidValueOf() {
        assertThrows(IllegalArgumentException.class, () -> IndexType.valueOf(4),
                "Should throw IllegalArgumentException for invalid value 4");
    }

}
