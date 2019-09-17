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

    /**
     * The escape
     */
    public enum Escape {
        /** mysql escape */
        MYSQL_ESCAPE('`'),
        /** oracle escape */
        ORACLE_ESCAPE('"'),
        ;
        public final char value;
        Escape(char value) {
            this.value = value;
        }
    }

    /**
     * del escape
     * @param cols the cols
     * @param escape the escape
     * @throws NullPointerException if cols is null
     */
    public static void delEscape(List<String> cols, Escape escape) {
        if (cols == null) {
            throw new NullPointerException("cols is null");
        }
        for (int i = 0, len = cols.size(); i < len; i++) {
            String col = cols.get(i);
            if (col != null && col.charAt(0) == escape.value) {
                cols.set(i, col.substring(1, col.length() - 1));
            }
        }
    }

    /**
     * add escape
     * @param col the column name
     * @param escape the escape
     */
    public static String addEscape(String col, Escape escape) {
        if (col == null) {
            throw new NullPointerException("col is null");
        }
        if (col.isEmpty()) {
            return col;
        }
        if (col.charAt(0) == escape.value) {
            return col;
        }
        return String.format("%s%s%s", escape.value, col, escape.value);
    }

}
