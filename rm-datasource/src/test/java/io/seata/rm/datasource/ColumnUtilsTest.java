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

    }

}
