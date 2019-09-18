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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jsbxyyx
 * @date 2019/09/17
 */
public class ColumnUtilsTest {

    @Test
    public void test_delEscape() {
        List<String> cols = new ArrayList<>();
        cols.add("`id`");
        cols.add("name");
        ColumnUtils.delEscape(cols, ColumnUtils.Escape.MYSQL);
        Assertions.assertEquals("id", cols.get(0));
        Assertions.assertEquals("name", cols.get(1));

        List<String> cols2 = new ArrayList<>();
        cols2.add("\"id\"");
        ColumnUtils.delEscape(cols2, ColumnUtils.Escape.STANDARD);
        Assertions.assertEquals("id", cols2.get(0));

        Assertions.assertThrows(NullPointerException.class, () -> {
            ColumnUtils.delEscape(null, ColumnUtils.Escape.MYSQL);
        });
    }

    @Test
    public void test_addEscape() {
        String col = "`id`";
        String newCol = ColumnUtils.addEscape(col, ColumnUtils.Escape.MYSQL);
        Assertions.assertEquals(col, newCol);

        String col_s = "\"id\"";
        String newCol_s = ColumnUtils.addEscape(col_s, ColumnUtils.Escape.STANDARD);
        Assertions.assertEquals(col_s, newCol_s);

        String col2 = "id";
        String newCol2 = ColumnUtils.addEscape(col2, ColumnUtils.Escape.MYSQL);
        Assertions.assertEquals("`" + col2 + "`", newCol2);

        String col2_s = "id";
        String newCol2_s = ColumnUtils.addEscape(col2_s, ColumnUtils.Escape.STANDARD);
        Assertions.assertEquals("\"" + col2_s + "\"", newCol2_s);

        String col3 = "";
        String newCol3 = ColumnUtils.addEscape(col3, ColumnUtils.Escape.MYSQL);
        Assertions.assertEquals(col3, newCol3);

        Assertions.assertThrows(NullPointerException.class, () -> {
            ColumnUtils.addEscape(null, ColumnUtils.Escape.MYSQL);
        });
    }

}
