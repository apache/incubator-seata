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
package org.apache.seata.sqlparser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class SQLTypeTest {

    @Test
    public void testValue() {
        assertEquals(0, SQLType.SELECT.value(), "SELECT value should be 0");
        assertEquals(1, SQLType.INSERT.value(), "INSERT value should be 1");
        // Add more assertions for other enum constants
    }

    @Test
    public void testValueOf() {
        assertEquals(SQLType.SELECT, SQLType.valueOf(0), "Should retrieve SELECT for value 0");
        assertEquals(SQLType.INSERT, SQLType.valueOf(1), "Should retrieve INSERT for value 1");
        // Add more assertions for other integer values
    }

    @Test
    public void testValueOfInvalid() {
        assertThrows(IllegalArgumentException.class, () -> SQLType.valueOf(100),
                "Should throw IllegalArgumentException for invalid value");
    }

}
