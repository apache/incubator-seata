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
package io.seata.core.constants;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
/**
 * The DBType Test
 *
 * @author zhongxiang.wang
 */
public class DBTypeTest {
    @Test
    public void testValueOf() {
        // Test valid DBType values
        assertEquals(DBType.MYSQL, DBType.valueof("MYSQL"));
        assertEquals(DBType.ORACLE, DBType.valueof("ORACLE"));
        assertEquals(DBType.DB2, DBType.valueof("DB2"));

        // Test invalid DBType value
        try {
            DBType.valueof("INVALID");
            fail("Expected IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            assertEquals("unknown dbtype:INVALID", e.getMessage());
        }
    }
}
