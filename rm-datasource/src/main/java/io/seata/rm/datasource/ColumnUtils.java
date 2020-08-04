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

import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.rm.datasource.undo.KeywordChecker;
import io.seata.rm.datasource.undo.KeywordCheckerFactory;
import io.seata.sqlparser.util.JdbcConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * column utils
 *
 * @author jsbxyyx
 */
public final class ColumnUtils {

    private static final String DOT = ".";

    /**
     * The escape
     */
    public enum Escape {
        /**
         * standard escape
         */
        STANDARD('"'),
        /**
         * mysql series escape
         */
        MYSQL('`');
        public final char value;

        Escape(char value) {
            this.value = value;
        }
    }

    /**
     * del escape by db type
     *
     * @param cols   the cols
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
     *
     * @param cols   the cols
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
     *
     * @param colName the column name
     * @param dbType  the db type
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
     *
     * @param colName the column name
     * @param escape  the escape
     * @return
     */
    public static String delEscape(String colName, Escape escape) {
        if (colName == null || colName.isEmpty()) {
            return colName;
        }

        if (colName.charAt(0) == escape.value && colName.charAt(colName.length() - 1) == escape.value) {
            // like "scheme"."id" `scheme`.`id`
            String str = escape.value + DOT + escape.value;
            int index = colName.indexOf(str);
            if (index > -1) {
                return colName.substring(1, index) + DOT + colName.substring(index + str.length(), colName.length() - 1);
            }
            return colName.substring(1, colName.length() - 1);
        } else {
            // like "scheme".id `scheme`.id
            String str = escape.value + DOT;
            int index = colName.indexOf(str);
            if (index > -1 && colName.charAt(0) == escape.value) {
                return colName.substring(1, index) + DOT + colName.substring(index + str.length());
            }
            // like scheme."id" scheme.`id`
            str = DOT + escape.value;
            index = colName.indexOf(str);
            if (index > -1 && colName.charAt(colName.length() - 1) == escape.value) {
                return colName.substring(0, index) + DOT + colName.substring(index + str.length(), colName.length() - 1);
            }
        }
        return colName;
    }

    /**
     * if necessary, add escape by db type
     * <pre>
     * mysql:
     *   only deal with keyword.
     * postgresql:
     *   only deal with keyword, contains uppercase character.
     * oracle:
     *   only deal with keyword, not full uppercase character.
     * </pre>
     *
     * @param cols   the column name list
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
     * if necessary, add escape by db type
     *
     * @param colName the column name
     * @param dbType  the db type
     * @return the colName left and right add escape
     */
    public static String addEscape(String colName, String dbType) {
        if (isMysqlSeries(dbType)) {
            return addEscape(colName, dbType, ColumnUtils.Escape.MYSQL);
        }
        return addEscape(colName, dbType, ColumnUtils.Escape.STANDARD);
    }

    /**
     * if necessary, add escape
     *
     * @param colName the column name
     * @param escape  the escape
     * @return
     */
    private static String addEscape(String colName, String dbType, Escape escape) {
        if (colName == null || colName.isEmpty()) {
            return colName;
        }
        if (colName.charAt(0) == escape.value && colName.charAt(colName.length() - 1) == escape.value) {
            return colName;
        }

        KeywordChecker keywordChecker = KeywordCheckerFactory.getKeywordChecker(dbType);
        if (keywordChecker != null) {
            boolean check = keywordChecker.checkEscape(colName);
            if (!check) {
                return colName;
            }
        }

        if (colName.contains(DOT)) {
            // like "scheme".id `scheme`.id
            String str = escape.value + DOT;
            int dotIndex = colName.indexOf(str);
            if (dotIndex > -1) {
                return new StringBuilder()
                        .append(colName.substring(0, dotIndex + str.length()))
                        .append(escape.value)
                        .append(colName.substring(dotIndex + str.length()))
                        .append(escape.value).toString();
            }
            // like scheme."id" scheme.`id`
            str = DOT + escape.value;
            dotIndex = colName.indexOf(str);
            if (dotIndex > -1) {
                return new StringBuilder()
                        .append(escape.value)
                        .append(colName.substring(0, dotIndex))
                        .append(escape.value)
                        .append(colName.substring(dotIndex))
                        .toString();
            }

            str = DOT;
            dotIndex = colName.indexOf(str);
            if (dotIndex > -1) {
                return new StringBuilder()
                        .append(escape.value)
                        .append(colName.substring(0, dotIndex))
                        .append(escape.value)
                        .append(DOT)
                        .append(escape.value)
                        .append(colName.substring(dotIndex + str.length()))
                        .append(escape.value).toString();
            }
        }

        char[] buf = new char[colName.length() + 2];
        buf[0] = escape.value;
        buf[buf.length - 1] = escape.value;

        colName.getChars(0, colName.length(), buf, 1);

        return new String(buf).intern();
    }

    private static boolean isMysqlSeries(String dbType) {
        return StringUtils.equalsIgnoreCase(dbType, JdbcConstants.MYSQL) ||
                StringUtils.equalsIgnoreCase(dbType, JdbcConstants.H2) ||
                StringUtils.equalsIgnoreCase(dbType, JdbcConstants.MARIADB);
    }

}
