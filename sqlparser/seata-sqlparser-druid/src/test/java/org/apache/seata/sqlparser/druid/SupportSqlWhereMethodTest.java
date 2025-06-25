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
package org.apache.seata.sqlparser.druid;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SupportSqlWhereMethodTest {

    @Test
    public void testGetInstance() {
        SupportSqlWhereMethod instance1 = SupportSqlWhereMethod.getInstance();
        SupportSqlWhereMethod instance2 = SupportSqlWhereMethod.getInstance();
        Assertions.assertInstanceOf(SupportSqlWhereMethod.class, instance1);
        Assertions.assertSame(instance1, instance2);
    }

    @Test
    public void testAddAndCheck() {
        SupportSqlWhereMethod supportSqlWhereMethod = SupportSqlWhereMethod.getInstance();

        // test default method
        Assertions.assertTrue(supportSqlWhereMethod.checkIsSupport("FIND_IN_SET"));
        Assertions.assertTrue(supportSqlWhereMethod.checkIsSupport("find_in_set"));

        // test add new method
        String newMethod = "CONCAT";
        supportSqlWhereMethod.add(newMethod);
        Assertions.assertTrue(supportSqlWhereMethod.checkIsSupport(newMethod));
        Assertions.assertTrue(supportSqlWhereMethod.checkIsSupport(newMethod.toLowerCase()));

        // test unsupported method
        Assertions.assertFalse(supportSqlWhereMethod.checkIsSupport("NOT_SUPPORT_METHOD"));
    }
}
