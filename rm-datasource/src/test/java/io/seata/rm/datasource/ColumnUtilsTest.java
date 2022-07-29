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
package io.seata.rm.datasource;

import io.seata.common.util.StringUtils;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Test cases for column util
 *
 * @author jsbxyyx
 * @author hsien999
 */
public class ColumnUtilsTest {

    @Test
    public void testDelEscapeByEscape() {
        List<String> testCols;
        // test all type of escapes
        for (ColumnUtils.Escape escape : ColumnUtils.Escape.values()) {
            String ch = String.valueOf(escape.value);

            // like: "id" | `id`
            testCols = Arrays.asList(ch + "id" + ch, "name");
            testCols = ColumnUtils.delEscape(testCols, escape);
            Assertions.assertEquals("id", testCols.get(0));
            Assertions.assertEquals("name", testCols.get(1));

            // like: "table".id | `table`.id
            testCols = Collections.singletonList(ch + "table" + ch + "." + "id");
            testCols = ColumnUtils.delEscape(testCols, escape);
            Assertions.assertEquals("table.id", testCols.get(0));

            // like: table."id" | table.`id`
            testCols = Collections.singletonList("table" + "." + ch + "id" + ch);
            testCols = ColumnUtils.delEscape(testCols, escape);
            Assertions.assertEquals("table.id", testCols.get(0));

            // like: "table"."id" | `table`.`id`
            testCols = Collections.singletonList(ch + "table" + ch + "." + ch + "id" + ch);
            testCols = ColumnUtils.delEscape(testCols, escape);
            Assertions.assertEquals("table.id", testCols.get(0));

            // follow cases demonstrates the lack of functionality
            // like: "id""123" => id""123
            testCols = Collections.singletonList(ch + "id" + ch + ch + "123" + ch);
            testCols = ColumnUtils.delEscape(testCols, escape);
            Assertions.assertEquals("id" + ch + ch + "123", testCols.get(0));

            // like: "table"".".id => table".".id
            testCols = Collections.singletonList(ch + "table" + ch + ch + "." + ch + "." + "id");
            testCols = ColumnUtils.delEscape(testCols, escape);
            Assertions.assertEquals("table" + ch + "." + ch + "." + "id", testCols.get(0));
        }
    }

    @Test
    public void testDelEscapeByDbType() {
        List<String> testCols;
        // test all type of escapes
        for (String dbType : Arrays.asList(JdbcConstants.MYSQL, JdbcConstants.ORACLE, JdbcConstants.POSTGRESQL,
            JdbcConstants.MARIADB, JdbcConstants.OCEANBASE, JdbcConstants.OCEANBASE_ORACLE)) {
            String ch = String.valueOf((isMysqlSeries(dbType) ?
                ColumnUtils.Escape.MYSQL : ColumnUtils.Escape.STANDARD).value);

            // like: "id" | `id`
            testCols = Arrays.asList(ch + "id" + ch, "name");
            testCols = ColumnUtils.delEscape(testCols, dbType);
            Assertions.assertEquals("id", testCols.get(0));
            Assertions.assertEquals("name", testCols.get(1));

            // like: "table".id | `table`.id
            testCols = Collections.singletonList(ch + "table" + ch + "." + "id");
            testCols = ColumnUtils.delEscape(testCols, dbType);
            Assertions.assertEquals("table.id", testCols.get(0));

            // like: table."id" | table.`id`
            testCols = Collections.singletonList("table" + "." + ch + "id" + ch);
            testCols = ColumnUtils.delEscape(testCols, dbType);
            Assertions.assertEquals("table.id", testCols.get(0));

            // like: "table"."id" | `table`.`id`
            testCols = Collections.singletonList(ch + "table" + ch + "." + ch + "id" + ch);
            testCols = ColumnUtils.delEscape(testCols, dbType);
            Assertions.assertEquals("table.id", testCols.get(0));

            // follow cases demonstrates the lack of functionality
            // like: "id""123" => id""123
            testCols = Collections.singletonList(ch + "id" + ch + ch + "123" + ch);
            testCols = ColumnUtils.delEscape(testCols, dbType);
            Assertions.assertEquals("id" + ch + ch + "123", testCols.get(0));

            // like: "table"".".id => table".".id
            testCols = Collections.singletonList(ch + "table" + ch + ch + "." + ch + "." + "id");
            testCols = ColumnUtils.delEscape(testCols, dbType);
            Assertions.assertEquals("table" + ch + "." + ch + "." + "id", testCols.get(0));
        }
    }

    private boolean isMysqlSeries(String dbType) {
        return StringUtils.equalsIgnoreCase(dbType, JdbcConstants.MYSQL) ||
            StringUtils.equalsIgnoreCase(dbType, JdbcConstants.H2) ||
            StringUtils.equalsIgnoreCase(dbType, JdbcConstants.MARIADB) ||
            StringUtils.equalsIgnoreCase(dbType, JdbcConstants.OCEANBASE);
    }

    @Test
    public void testAddEscapeByDbType() {
        List<String> testCols;

        // case1: test for Mysql
        // only deal with keyword for Mysql
        testCols = Collections.singletonList("id");
        testCols = ColumnUtils.addEscape(testCols, JdbcConstants.MYSQL);
        Assertions.assertEquals("id", testCols.get(0));

        testCols = Collections.singletonList("ID");
        testCols = ColumnUtils.addEscape(testCols, JdbcConstants.MYSQL);
        Assertions.assertEquals("ID", testCols.get(0));

        testCols = Collections.singletonList("limit");
        testCols = ColumnUtils.addEscape(testCols, JdbcConstants.MYSQL);
        Assertions.assertEquals("`limit`", testCols.get(0));

        testCols = Collections.singletonList("LIMIT");
        testCols = ColumnUtils.addEscape(testCols, JdbcConstants.MYSQL);
        Assertions.assertEquals("`LIMIT`", testCols.get(0));

        testCols = Collections.singletonList("`TABLE`.id");
        testCols = ColumnUtils.addEscape(testCols, JdbcConstants.MYSQL);
        Assertions.assertEquals("`TABLE`.id", testCols.get(0));

        testCols = Collections.singletonList("table.`ID`");
        testCols = ColumnUtils.addEscape(testCols, JdbcConstants.MYSQL);
        Assertions.assertEquals("table.`ID`", testCols.get(0));


        // case2: test for Pgsql
        // deal with keyword for Pgsql
        testCols = Collections.singletonList("current_date");
        testCols = ColumnUtils.addEscape(testCols, JdbcConstants.POSTGRESQL);
        Assertions.assertEquals("\"current_date\"", testCols.get(0));

        testCols = Collections.singletonList("CURRENT_DATE");
        testCols = ColumnUtils.addEscape(testCols, JdbcConstants.POSTGRESQL);
        Assertions.assertEquals("\"CURRENT_DATE\"", testCols.get(0));

        // deal with case-sensitive for Pgsql
        testCols = Collections.singletonList("id");
        testCols = ColumnUtils.addEscape(testCols, JdbcConstants.POSTGRESQL);
        Assertions.assertEquals("id", testCols.get(0));

        testCols = Collections.singletonList("ID");
        testCols = ColumnUtils.addEscape(testCols, JdbcConstants.POSTGRESQL);
        Assertions.assertEquals("\"ID\"", testCols.get(0));

        testCols = Collections.singletonList("table.\"id\"");
        testCols = ColumnUtils.addEscape(testCols, JdbcConstants.POSTGRESQL);
        Assertions.assertEquals("table.\"id\"", testCols.get(0));

        testCols = Collections.singletonList("\"TABLE\".id");
        testCols = ColumnUtils.addEscape(testCols, JdbcConstants.POSTGRESQL);
        Assertions.assertEquals("\"TABLE\".\"id\"", testCols.get(0));


        // case3: test for Oracle
        // deal with keyword for Oracle
        testCols = Collections.singletonList("varchar2");
        testCols = ColumnUtils.addEscape(testCols, JdbcConstants.ORACLE);
        Assertions.assertEquals("\"varchar2\"", testCols.get(0));

        testCols = Collections.singletonList("VARCHAR2");
        testCols = ColumnUtils.addEscape(testCols, JdbcConstants.ORACLE);
        Assertions.assertEquals("\"VARCHAR2\"", testCols.get(0));

        // deal with case-sensitive for Oracle
        testCols = Collections.singletonList("id");
        testCols = ColumnUtils.addEscape(testCols, JdbcConstants.ORACLE);
        Assertions.assertEquals("\"id\"", testCols.get(0));

        testCols = Collections.singletonList("ID");
        testCols = ColumnUtils.addEscape(testCols, JdbcConstants.ORACLE);
        Assertions.assertEquals("ID", testCols.get(0));

        testCols = Collections.singletonList("id");
        testCols = ColumnUtils.addEscape(testCols, JdbcConstants.ORACLE);
        Assertions.assertEquals("\"id\"", testCols.get(0));

        testCols = Collections.singletonList("TABLE.\"ID\"");
        testCols = ColumnUtils.addEscape(testCols, JdbcConstants.ORACLE);
        Assertions.assertEquals("TABLE.\"ID\"", testCols.get(0));

        testCols = Collections.singletonList("\"TABLE\".id");
        testCols = ColumnUtils.addEscape(testCols, JdbcConstants.ORACLE);
        Assertions.assertEquals("\"TABLE\".\"id\"", testCols.get(0));


        // case4: test for OceanBase(Oracle mode)
        // deal with keyword for OceanBase(Oracle mode)
        testCols = Collections.singletonList("dual");
        testCols = ColumnUtils.addEscape(testCols, JdbcConstants.OCEANBASE_ORACLE);
        Assertions.assertEquals("\"dual\"", testCols.get(0));

        testCols = Collections.singletonList("DUAL");
        testCols = ColumnUtils.addEscape(testCols, JdbcConstants.OCEANBASE_ORACLE);
        Assertions.assertEquals("\"DUAL\"", testCols.get(0));

        // deal with case-sensitive for OceanBase(Oracle mode)
        testCols = Collections.singletonList("id");
        testCols = ColumnUtils.addEscape(testCols, JdbcConstants.OCEANBASE_ORACLE);
        Assertions.assertEquals("\"id\"", testCols.get(0));

        testCols = Collections.singletonList("ID");
        testCols = ColumnUtils.addEscape(testCols, JdbcConstants.OCEANBASE_ORACLE);
        Assertions.assertEquals("ID", testCols.get(0));

        testCols = Collections.singletonList("TABLE.\"ID\"");
        testCols = ColumnUtils.addEscape(testCols, JdbcConstants.OCEANBASE_ORACLE);
        Assertions.assertEquals("TABLE.\"ID\"", testCols.get(0));

        testCols = Collections.singletonList("\"TABLE\".id");
        testCols = ColumnUtils.addEscape(testCols, JdbcConstants.OCEANBASE_ORACLE);
        Assertions.assertEquals("\"TABLE\".\"id\"", testCols.get(0));
    }

}
