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

import java.util.List;

/**
 * column utils
 * @author jsbxyyx
 * @date 2019/09/17
 */
public final class ColumnUtils {

    public static final char BACKTICKS = '`';

    /**
     * del back quote
     * @param cols the cols
     * @throws NullPointerException if cols is null
     */
    public static void delBackticks(List<String> cols) {
        if (cols == null) {
            throw new NullPointerException("cols is null");
        }
        for (int i = 0, len = cols.size(); i < len; i++) {
            String col = cols.get(i);
            if (col != null && col.charAt(0) == BACKTICKS) {
                cols.set(i, col.substring(1, col.length() - 1));
            }
        }
    }

    /**
     * add back quote
     * @param col the column name
     */
    public static String addBackticks(String col) {
        if (col == null) {
            throw new NullPointerException("col is null");
        }
        if (col.isEmpty()) {
            return col;
        }
        if (col.charAt(0) == BACKTICKS) {
            return col;
        }
        return BACKTICKS + col + BACKTICKS;
    }
}
