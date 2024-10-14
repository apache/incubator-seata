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
package org.apache.seata.common.util;

import org.apache.seata.common.exception.NotSupportYetException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The page util test.
 *
 */
public class PageUtilTest {

    @Test
    public void testPageSql() {
        String sourceSql = "select * from test where a = 1";

        String mysqlTargetSql = "select * from test where a = 1 limit 5 offset 0";

        String oracleTargetSql = "select * from " +
                "( select ROWNUM rn, temp.* from (select * from test where a = 1) temp )" +
                " where rn between 1 and 5";
        String sqlserverTargetSql = "select * from (select temp.*, ROW_NUMBER() OVER(ORDER BY gmt_create desc) AS rowId from (select * from test where a = 1) temp ) t where t.rowId between 1 and 5";

        assertEquals(PageUtil.pageSql(sourceSql, "mysql", 1, 5), mysqlTargetSql);
        assertEquals(PageUtil.pageSql(sourceSql, "h2", 1, 5), mysqlTargetSql);
        assertEquals(PageUtil.pageSql(sourceSql, "postgresql", 1, 5), mysqlTargetSql);
        assertEquals(PageUtil.pageSql(sourceSql, "oceanbase", 1, 5), mysqlTargetSql);
        assertEquals(PageUtil.pageSql(sourceSql, "dm", 1, 5), mysqlTargetSql);
        assertEquals(PageUtil.pageSql(sourceSql, "oscar", 1, 5), mysqlTargetSql);
        assertEquals(PageUtil.pageSql(sourceSql, "oracle", 1, 5), oracleTargetSql);
        assertEquals(PageUtil.pageSql(sourceSql, "sqlserver", 1, 5), sqlserverTargetSql);

        assertThrows(NotSupportYetException.class, () -> PageUtil.pageSql(sourceSql, "xxx", 1, 5));
    }

    @Test
    void testCountSql() {
        String sourceSql = "select * from test where a = 1";

        String targetSql = "select count(1) from test where a = 1";

        assertEquals(PageUtil.countSql(sourceSql, "mysql"), targetSql);
        assertEquals(PageUtil.countSql(sourceSql, "h2"), targetSql);
        assertEquals(PageUtil.countSql(sourceSql, "postgresql"), targetSql);
        assertEquals(PageUtil.countSql(sourceSql, "oceanbase"), targetSql);
        assertEquals(PageUtil.countSql(sourceSql, "dm"), targetSql);
        assertEquals(PageUtil.countSql(sourceSql, "oscar"), targetSql);
        assertEquals(PageUtil.countSql(sourceSql, "oracle"), targetSql);
        assertEquals(PageUtil.countSql(sourceSql, "sqlserver"), targetSql);

        assertThrows(NotSupportYetException.class, () -> PageUtil.countSql(sourceSql, "xxx"));
    }

}
