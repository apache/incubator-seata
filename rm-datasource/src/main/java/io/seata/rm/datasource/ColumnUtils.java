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
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * column utils
 * @author jsbxyyx
 */
public final class ColumnUtils {

    /**
     * The escape
     */
    public enum Escape {
        /** standard escape */
        STANDARD('"'),
        /** mysql series escape */
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
     * @return
     */
    public static List<String> delEscape(List<String> cols, String dbType) {
        // sql standard
        // https://db.apache.org/derby/docs/10.1/ref/crefsqlj1003454.html
        // https://docs.oracle.com/javadb/10.8.3.0/ref/crefsqlj1003454.html
        // https://www.informit.com/articles/article.aspx?p=2036581&seqNum=2
        List<String> newCols = delEscape(cols, Escape.STANDARD);
        if (isMysqlSeries(dbType)) {
            newCols = delEscape(newCols, Escape.MYSQL);
        }
        return newCols;
    }

    /**
     * del escape
     * @param cols the cols
     * @param escape the escape
     * @return delete the column list element left and right escape.
     */
    public static List<String> delEscape(List<String> cols, Escape escape) {
        if (CollectionUtils.isEmpty(cols)) {
            return cols;
        }
        List<String> newCols = new ArrayList<>(cols.size());
        for (int i = 0, len = cols.size(); i < len; i++) {
            String col = cols.get(i);
            col = delEscape(col, escape);
            newCols.add(col);
        }
        return newCols;
    }

    /**
     * del escape by db type
     * @param colName the column name
     * @param dbType the db type
     * @return
     */
    public static String delEscape(String colName, String dbType) {
        String newColName = delEscape(colName, Escape.STANDARD);
        if (isMysqlSeries(dbType)) {
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
        if (colName == null || colName.isEmpty()) {
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
     * @return
     */
    public static List<String> addEscape(List<String> cols, String dbType) {
        if (CollectionUtils.isEmpty(cols)) {
            return cols;
        }
        List<String> newCols = new ArrayList<>(cols.size());
        for (int i = 0, len = cols.size(); i < len; i++) {
            String col = cols.get(i);
            col = addEscape(col, dbType);
            newCols.add(col);
        }
        return newCols;
    }

    /**
     * add escape by db type
     * @param colName the column name
     * @param dbType the db type
     * @return the colName left and right add escape
     */
    public static String addEscape(String colName, String dbType) {
        if (isMysqlSeries(dbType)) {
            return addEscape(colName, ColumnUtils.Escape.MYSQL);
        }
        return addEscape(colName, ColumnUtils.Escape.STANDARD);
    }

    /**
     * add escape
     * @param colName the column name
     * @param escape the escape
     * @return
     */
    public static String addEscape(String colName, Escape escape) {
        if (colName == null || colName.isEmpty()) {
            return colName;
        }
        if (colName.charAt(0) == escape.value && colName.charAt(colName.length() - 1) == escape.value) {
            return colName;
        }
        return String.format("%s%s%s", escape.value, colName, escape.value);
    }

    private static boolean isMysqlSeries(String dbType) {
        return StringUtils.equalsIgnoreCase(dbType, JdbcConstants.MYSQL) ||
            StringUtils.equalsIgnoreCase(dbType, JdbcConstants.H2) ||
            StringUtils.equalsIgnoreCase(dbType, JdbcConstants.MARIADB);
    }
}
