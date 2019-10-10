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
import io.seata.common.util.StringUtils;

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
        /** standard escape */
        STANDARD('"'),
        /** mysql escape */
        MYSQL('`')
        ;
        public final char value;
        Escape(char value) {
            this.value = value;
        }
    }

    /**
     * del escape by db type
     * @param cols the cols
     * @param dbType the db type
     */
    public static void delEscape(List<String> cols, String dbType) {
        // sql standard
        // https://db.apache.org/derby/docs/10.1/ref/crefsqlj1003454.html
        // https://docs.oracle.com/javadb/10.8.3.0/ref/crefsqlj1003454.html
        // https://www.informit.com/articles/article.aspx?p=2036581&seqNum=2
        delEscape(cols, Escape.STANDARD);
        if (StringUtils.equalsIgnoreCase(dbType, JdbcConstants.MYSQL)) {
            delEscape(cols, Escape.MYSQL);
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
            if (col != null && col.charAt(0) == escape.value && col.charAt(col.length() - 1) == escape.value) {
                cols.set(i, delEscape(col, escape));
            }
        }
    }

    /**
     * del escape by db type
     * @param colName the column name
     * @param dbType the db type
     * @return
     */
    public static String delEscape(String colName, String dbType) {
        String newColName = delEscape(colName, Escape.STANDARD);
        if (StringUtils.equalsIgnoreCase(dbType, JdbcConstants.MYSQL)) {
            newColName = delEscape(newColName, Escape.MYSQL);
        }
        return newColName;
    }

    /**
     * del escape by escape
     * @param colName the column name
     * @param escape the escape
     * @return
     */
    public static String delEscape(String colName, Escape escape) {
        if (colName == null) {
            throw new NullPointerException("colName is null");
        }
        if (colName.isEmpty()) {
            return colName;
        }
        if (colName.charAt(0) == escape.value && colName.charAt(colName.length() - 1) == escape.value) {
            return colName.substring(1, colName.length() - 1);
        }
        return colName;
    }

    /**
     * add escape by db type
     * @param cols the column name list
     * @param dbType the db type
     */
    public static void addEscape(List<String> cols, String dbType) {
        if (cols == null || cols.isEmpty()) {
            throw new NullPointerException("cols is null");
        }
        for (int i = 0, len = cols.size(); i < len; i++) {
            String col = cols.get(i);
            if (col != null) {
                cols.set(i, addEscape(col, dbType));
            }
        }
    }

    /**
     * add escape by db type
     * @param colName the column name
     * @param dbType the db type
     * @return the colName left and right add escape
     */
    public static String addEscape(String colName, String dbType) {
        if (StringUtils.equalsIgnoreCase(dbType, JdbcConstants.MYSQL)) {
            return addEscape(colName, ColumnUtils.Escape.MYSQL);
        }
        return addEscape(colName, ColumnUtils.Escape.STANDARD);
    }

    /**
     * add escape
     * @param colName the column name
     * @param escape the escape
     */
    public static String addEscape(String colName, Escape escape) {
        if (colName == null) {
            throw new NullPointerException("colName is null");
        }
        if (colName.isEmpty()) {
            return colName;
        }
        if (colName.charAt(0) == escape.value && colName.charAt(colName.length() - 1) == escape.value) {
            return colName;
        }
        return String.format("%s%s%s", escape.value, colName, escape.value);
    }

}
