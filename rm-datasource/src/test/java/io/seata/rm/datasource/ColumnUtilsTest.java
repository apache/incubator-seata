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

import io.seata.sqlparser.util.ColumnUtils;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jsbxyyx
 */
public class ColumnUtilsTest {

    @Test
    public void test_delEscape_byEscape() throws Exception {
        List<String> cols = new ArrayList<>();
        cols.add("`id`");
        cols.add("name");
        cols = ColumnUtils.delEscape(cols, JdbcConstants.MYSQL);
        Assertions.assertEquals("id", cols.get(0));
        Assertions.assertEquals("name", cols.get(1));

        List<String> cols2 = new ArrayList<>();
        cols2.add("\"id\"");
        cols2 = ColumnUtils.delEscape(cols2, JdbcConstants.ORACLE);
        Assertions.assertEquals("id", cols2.get(0));

        List<String> cols3 = new ArrayList<>();
        cols3.add("\"scheme\".\"id\"");
        cols3 = ColumnUtils.delEscape(cols3, JdbcConstants.ORACLE);
        Assertions.assertEquals("scheme.id", cols3.get(0));

        List<String> cols4 = new ArrayList<>();
        cols4.add("`scheme`.`id`");
        cols4 = ColumnUtils.delEscape(cols4, JdbcConstants.MYSQL);
        Assertions.assertEquals("scheme.id", cols4.get(0));

        List<String> cols5 = new ArrayList<>();
        cols5.add("\"scheme\".id");
        cols5 = ColumnUtils.delEscape(cols5, JdbcConstants.ORACLE);
        Assertions.assertEquals("scheme.id", cols5.get(0));

        List<String> cols6 = new ArrayList<>();
        cols6.add("\"tab\"\"le\"");
        cols6 = ColumnUtils.delEscape(cols6, JdbcConstants.ORACLE);
        Assertions.assertEquals("tab\"\"le", cols6.get(0));

        List<String> cols7 = new ArrayList<>();
        cols7.add("scheme.\"id\"");
        cols7 = ColumnUtils.delEscape(cols7, JdbcConstants.ORACLE);
        Assertions.assertEquals("scheme.id", cols7.get(0));

        List<String> cols8 = new ArrayList<>();
        cols8.add("`scheme`.id");
        cols8 = ColumnUtils.delEscape(cols8, JdbcConstants.ORACLE);
        Assertions.assertEquals("`scheme`.id", cols8.get(0));

        List<String> cols9 = new ArrayList<>();
        cols9.add("scheme.`id`");
        cols9 = ColumnUtils.delEscape(cols9, JdbcConstants.MYSQL);
        Assertions.assertEquals("scheme.id", cols9.get(0));

        Assertions.assertNull(ColumnUtils.delEscape((String) null, JdbcConstants.MYSQL));
    }

    @Test
    public void test_delEscape_byDbType() throws Exception {

        List<String> cols3 = new ArrayList<>();
        cols3.add("\"id\"");
        cols3 = ColumnUtils.delEscape(cols3, JdbcConstants.ORACLE);
        Assertions.assertEquals("id", cols3.get(0));

        List<String> cols4 = new ArrayList<>();
        cols4.add("`id`");
        cols4 = ColumnUtils.delEscape(cols4, JdbcConstants.MYSQL);
        Assertions.assertEquals("id", cols4.get(0));

        List<String> cols5 = new ArrayList<>();
        cols5.add("\"id\"");
        cols5 = ColumnUtils.delEscape(cols5, JdbcConstants.POSTGRESQL);
        Assertions.assertEquals("id", cols5.get(0));

        Assertions.assertEquals("id", ColumnUtils.delEscape("`id`", JdbcConstants.MYSQL));
        Assertions.assertEquals("id", ColumnUtils.delEscape("\"id\"", JdbcConstants.ORACLE));
        Assertions.assertEquals("id", ColumnUtils.delEscape("\"id\"", JdbcConstants.POSTGRESQL));
    }

    @Test
    public void test_addEscape_byDbType() throws Exception {
        List<String> cols = new ArrayList<>();
        cols.add("id");
        cols = ColumnUtils.addEscape(cols, JdbcConstants.MYSQL);
        Assertions.assertEquals("id", cols.get(0));

        cols = new ArrayList<>();
        cols.add("`id`");
        cols = ColumnUtils.addEscape(cols, JdbcConstants.MYSQL);
        Assertions.assertEquals("`id`", cols.get(0));

        cols = new ArrayList<>();
        cols.add("from");
        cols = ColumnUtils.addEscape(cols, JdbcConstants.MYSQL);
        Assertions.assertEquals("`from`", cols.get(0));

        cols = new ArrayList<>();
        cols.add("scheme.id");
        cols = ColumnUtils.addEscape(cols, JdbcConstants.MYSQL);
        Assertions.assertEquals("scheme.id", cols.get(0));

        cols = new ArrayList<>();
        cols.add("`scheme`.id");
        cols = ColumnUtils.addEscape(cols, JdbcConstants.MYSQL);
        Assertions.assertEquals("`scheme`.id", cols.get(0));

        cols = new ArrayList<>();
        cols.add("scheme.`id`");
        cols = ColumnUtils.addEscape(cols, JdbcConstants.MYSQL);
        Assertions.assertEquals("scheme.`id`", cols.get(0));


        cols = new ArrayList<>();
        cols.add("id");
        cols = ColumnUtils.addEscape(cols, JdbcConstants.ORACLE);
        Assertions.assertEquals("\"id\"", cols.get(0));

        cols = new ArrayList<>();
        cols.add("\"id\"");
        cols = ColumnUtils.addEscape(cols, JdbcConstants.ORACLE);
        Assertions.assertEquals("\"id\"", cols.get(0));

        cols = new ArrayList<>();
        cols.add("from");
        cols = ColumnUtils.addEscape(cols, JdbcConstants.ORACLE);
        Assertions.assertEquals("\"from\"", cols.get(0));

        cols = new ArrayList<>();
        cols.add("FROM");
        cols = ColumnUtils.addEscape(cols, JdbcConstants.ORACLE);
        Assertions.assertEquals("\"FROM\"", cols.get(0));

        cols = new ArrayList<>();
        cols.add("ID");
        cols = ColumnUtils.addEscape(cols, JdbcConstants.ORACLE);
        Assertions.assertEquals("ID", cols.get(0));

        cols = new ArrayList<>();
        cols.add("\"SCHEME\".ID");
        cols = ColumnUtils.addEscape(cols, JdbcConstants.ORACLE);
        Assertions.assertEquals("\"SCHEME\".ID", cols.get(0));

        cols = new ArrayList<>();
        cols.add("\"scheme\".id");
        cols = ColumnUtils.addEscape(cols, JdbcConstants.ORACLE);
        Assertions.assertEquals("\"scheme\".\"id\"", cols.get(0));

        cols = new ArrayList<>();
        cols.add("SCHEME.\"ID\"");
        cols = ColumnUtils.addEscape(cols, JdbcConstants.ORACLE);
        Assertions.assertEquals("SCHEME.\"ID\"", cols.get(0));

        cols = new ArrayList<>();
        cols.add("scheme.id");
        cols = ColumnUtils.addEscape(cols, JdbcConstants.ORACLE);
        Assertions.assertEquals("\"scheme\".\"id\"", cols.get(0));


        cols = new ArrayList<>();
        cols.add("id");
        cols = ColumnUtils.addEscape(cols, JdbcConstants.POSTGRESQL);
        Assertions.assertEquals("id", cols.get(0));

        cols = new ArrayList<>();
        cols.add("Id");
        cols = ColumnUtils.addEscape(cols, JdbcConstants.POSTGRESQL);
        Assertions.assertEquals("\"Id\"", cols.get(0));

        cols = new ArrayList<>();
        cols.add("from");
        cols = ColumnUtils.addEscape(cols, JdbcConstants.POSTGRESQL);
        Assertions.assertEquals("\"from\"", cols.get(0));

        cols = new ArrayList<>();
        cols.add("FROM");
        cols = ColumnUtils.addEscape(cols, JdbcConstants.POSTGRESQL);
        Assertions.assertEquals("\"FROM\"", cols.get(0));

        cols = new ArrayList<>();
        cols.add("scheme.Id");
        cols = ColumnUtils.addEscape(cols, JdbcConstants.POSTGRESQL);
        Assertions.assertEquals("\"scheme\".\"Id\"", cols.get(0));

        cols = new ArrayList<>();
        cols.add("SCHEME.\"ID\"");
        cols = ColumnUtils.addEscape(cols, JdbcConstants.POSTGRESQL);
        Assertions.assertEquals("SCHEME.\"ID\"", cols.get(0));

        cols = new ArrayList<>();
        cols.add("\"SCHEME\".ID");
        cols = ColumnUtils.addEscape(cols, JdbcConstants.POSTGRESQL);
        Assertions.assertEquals("\"SCHEME\".\"ID\"", cols.get(0));

        cols = new ArrayList<>();
        cols.add("scheme.id");
        cols = ColumnUtils.addEscape(cols, JdbcConstants.POSTGRESQL);
        Assertions.assertEquals("scheme.id", cols.get(0));

        cols = new ArrayList<>();
        cols.add("schEme.id");
        cols = ColumnUtils.addEscape(cols, JdbcConstants.POSTGRESQL);
        Assertions.assertEquals("schEme.id", cols.get(0));

    }

}
