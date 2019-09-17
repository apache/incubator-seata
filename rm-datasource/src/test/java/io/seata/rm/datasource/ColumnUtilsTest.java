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
    public void test_delBackticks() {
        List<String> cols = new ArrayList<>();
        cols.add("`id`");
        cols.add("name");
        ColumnUtils.delBackticks(cols);
        Assertions.assertEquals("id", cols.get(0));
        Assertions.assertEquals("name", cols.get(1));

        Assertions.assertThrows(NullPointerException.class, () -> {
            ColumnUtils.delBackticks(null);
        });
    }

    @Test
    public void test_addBackticks() {
        String col = "`id`";
        String newCol = ColumnUtils.addBackticks(col);
        Assertions.assertEquals(col, newCol);

        String col2 = "id";
        String newCol2 = ColumnUtils.addBackticks(col2);
        Assertions.assertEquals(col, newCol2);

        String col3 = "";
        String newCol3 = ColumnUtils.addBackticks(col3);
        Assertions.assertEquals(col3, newCol3);

        Assertions.assertThrows(NullPointerException.class, () -> {
            ColumnUtils.addBackticks(null);
        });
    }

}
