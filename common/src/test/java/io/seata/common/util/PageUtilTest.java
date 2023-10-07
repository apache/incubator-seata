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
package io.seata.common.util;

import io.seata.common.exception.NotSupportYetException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The page util test.
 *
 * @author lvekee@734843455@qq.com
 */
public class PageUtilTest {

    @Test
    public void testPageSql() {
        String sourceSql = "select * from test where a = 1";

        String mysqlTargetSql = "select * from test where a = 1 limit 5 offset 0";

        String oracleTargetSql = "select * from " +
                "( select ROWNUM rn, temp.* from (select * from test where a = 1) temp )" +
                " where rn between 1 and 5";

        assertEquals(PageUtil.pageSql(sourceSql, "mysql", 1, 5), mysqlTargetSql);
        assertEquals(PageUtil.pageSql(sourceSql, "polardb-x", 1, 5), mysqlTargetSql);
        assertEquals(PageUtil.pageSql(sourceSql, "h2", 1, 5), mysqlTargetSql);
        assertEquals(PageUtil.pageSql(sourceSql, "postgresql", 1, 5), mysqlTargetSql);
        assertEquals(PageUtil.pageSql(sourceSql, "oceanbase", 1, 5), mysqlTargetSql);
        assertEquals(PageUtil.pageSql(sourceSql, "dm", 1, 5), mysqlTargetSql);
        assertEquals(PageUtil.pageSql(sourceSql, "oracle", 1, 5), oracleTargetSql);

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
        assertEquals(PageUtil.countSql(sourceSql, "oracle"), targetSql);

        assertThrows(NotSupportYetException.class, () -> PageUtil.countSql(sourceSql, "xxx"));
    }

}
