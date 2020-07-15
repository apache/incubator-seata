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

import com.alibaba.druid.util.JdbcConstants;
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
        cols = ColumnUtils.delEscape(cols, ColumnUtils.Escape.MYSQL);
        Assertions.assertEquals("id", cols.get(0));
        Assertions.assertEquals("name", cols.get(1));

        List<String> cols2 = new ArrayList<>();
        cols2.add("\"id\"");
        cols2 = ColumnUtils.delEscape(cols2, ColumnUtils.Escape.STANDARD);
        Assertions.assertEquals("id", cols2.get(0));

        List<String> cols3 = new ArrayList<>();
        cols3.add("\"scheme\".\"id\"");
        cols3 = ColumnUtils.delEscape(cols3, ColumnUtils.Escape.STANDARD);
        Assertions.assertEquals("scheme.id", cols3.get(0));

        List<String> cols4 = new ArrayList<>();
        cols4.add("`scheme`.`id`");
        cols4 = ColumnUtils.delEscape(cols4, ColumnUtils.Escape.MYSQL);
        Assertions.assertEquals("scheme.id", cols4.get(0));

        List<String> cols5 = new ArrayList<>();
        cols5.add("\"scheme\".id");
        cols5 = ColumnUtils.delEscape(cols5, ColumnUtils.Escape.STANDARD);
        Assertions.assertEquals("scheme.id", cols5.get(0));

        List<String> cols6 = new ArrayList<>();
        cols6.add("\"tab\"\"le\"");
        cols6 = ColumnUtils.delEscape(cols6, ColumnUtils.Escape.STANDARD);
        Assertions.assertEquals("tab\"\"le", cols6.get(0));

        List<String> cols7 = new ArrayList<>();
        cols7.add("scheme.\"id\"");
        cols7 = ColumnUtils.delEscape(cols7, ColumnUtils.Escape.STANDARD);
        Assertions.assertEquals("scheme.id", cols7.get(0));

        List<String> cols8 = new ArrayList<>();
        cols8.add("`scheme`.id");
        cols8 = ColumnUtils.delEscape(cols8, ColumnUtils.Escape.MYSQL);
        Assertions.assertEquals("scheme.id", cols8.get(0));

        List<String> cols9 = new ArrayList<>();
        cols9.add("scheme.`id`");
        cols9 = ColumnUtils.delEscape(cols9, ColumnUtils.Escape.MYSQL);
        Assertions.assertEquals("scheme.id", cols9.get(0));

        Assertions.assertNull(ColumnUtils.delEscape((String) null, ColumnUtils.Escape.MYSQL));
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

        Assertions.assertEquals("id", ColumnUtils.delEscape("`id`", JdbcConstants.MYSQL));
        Assertions.assertEquals("id", ColumnUtils.delEscape("\"id\"", JdbcConstants.ORACLE));
    }

    @Test
    public void test_addEscape_byDbType() throws Exception {
        List<String> cols1 = new ArrayList<>();
        cols1.add("id");
        cols1 = ColumnUtils.addEscape(cols1, JdbcConstants.MYSQL);
        Assertions.assertEquals("id", cols1.get(0));

        List<String> cols2 = new ArrayList<>();
        cols2.add("`id`");
        cols2 = ColumnUtils.addEscape(cols2, JdbcConstants.MYSQL);
        Assertions.assertEquals("`id`", cols2.get(0));

        List<String> cols3 = new ArrayList<>();
        cols3.add("id");
        cols3 = ColumnUtils.addEscape(cols3, JdbcConstants.ORACLE);
        Assertions.assertEquals("\"id\"", cols3.get(0));

        List<String> cols4 = new ArrayList<>();
        cols4.add("\"id\"");
        cols4 = ColumnUtils.addEscape(cols4, JdbcConstants.ORACLE);
        Assertions.assertEquals("\"id\"", cols4.get(0));

        List<String> cols5 = new ArrayList<>();
        cols5.add("ID");
        cols5 = ColumnUtils.addEscape(cols5, JdbcConstants.ORACLE);
        Assertions.assertEquals("ID", cols5.get(0));

        List<String> cols6 = new ArrayList<>();
        cols6.add("\"SCHEME\".ID");
        cols6 = ColumnUtils.addEscape(cols6, JdbcConstants.ORACLE);
        Assertions.assertEquals("\"SCHEME\".\"ID\"", cols6.get(0));

        List<String> cols7 = new ArrayList<>();
        cols7.add("`SCHEME`.ID");
        cols7 = ColumnUtils.addEscape(cols7, JdbcConstants.MYSQL);
        Assertions.assertEquals("`SCHEME`.`ID`", cols7.get(0));

        List<String> cols8 = new ArrayList<>();
        cols8.add("SCHEME.`ID`");
        cols8 = ColumnUtils.addEscape(cols8, JdbcConstants.MYSQL);
        Assertions.assertEquals("`SCHEME`.`ID`", cols8.get(0));

        List<String> cols9 = new ArrayList<>();
        cols9.add("SCHEME.\"ID\"");
        cols9 = ColumnUtils.addEscape(cols8, JdbcConstants.ORACLE);
        Assertions.assertEquals("`SCHEME`.`ID`", cols8.get(0));

    }

}
