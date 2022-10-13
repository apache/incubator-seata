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
package io.seata.sqlparser.util;

import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.sqlparser.KeywordChecker;
import io.seata.sqlparser.KeywordCheckerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Column Utils
 *
 * @author jsbxyyx
 */
public final class ColumnUtils {
    /**
     * SQL Identifier Syntax Standards:
     * <a href="https://db.apache.org/derby/docs/10.1/ref/crefsqlj1003454.html">SQL92</a>
     * <a href="https://www.informit.com/articles/article.aspx?p=2036581&seqNum=2">MySql</a>
     */
    private static final String DOT = ".";

    /**
     * Delete escapes to the column name in list
     * (No feasibility verification for deletion)
     *
     * <p>
     * 1. do not consider schema name here, e.g.
     * => in mysql: SELECT * FROM `sampdb`.`member` WHERE `sampdb`.`member`.`member_id` > 100;
     * 2. do not support names that contain escape and dot yet, e.g.
     * a legal name like `table.``123`.id for mysql or "id.""123" for pgsql will return an error result.
     *
     * @param cols   the column name list
     * @param dbType the db type
     * @return the list of column name without escapes
     */
    public static List<String> delEscape(List<String> cols, String dbType) {
        List<String> newCols = delEscape(cols, Escape.STANDARD);
        if (isMysqlSeries(dbType)) {
            newCols = delEscape(newCols, Escape.MYSQL);
        }
        return newCols;
    }

    /**
     * Delete escapes to the column name in list
     *
     * @param cols   the column name list
     * @param escape the escape
     * @return the list of column name without escapes
     */
    public static List<String> delEscape(List<String> cols, Escape escape) {
        if (CollectionUtils.isEmpty(cols)) {
            return cols;
        }
        List<String> newCols = new ArrayList<>(cols.size());
        for (String col : cols) {
            col = delEscape(col, escape);
            newCols.add(col);
        }
        return newCols;
    }

    /**
     * Delete escapes to the column name
     *
     * @param colName the column name
     * @param dbType  the db type
     * @return the column name without escapes
     */
    public static String delEscape(String colName, String dbType) {
        String newColName = delEscape(colName, Escape.STANDARD);
        if (isMysqlSeries(dbType)) {
            newColName = delEscape(newColName, Escape.MYSQL);
        }
        return newColName;
    }

    /**
     * Delete escapes to the column name
     *
     * @param colName the column name
     * @param escape  the escape
     * @return the column name without escapes
     */
    public static String delEscape(String colName, Escape escape) {
        if (colName == null || colName.isEmpty()) {
            return colName;
        }

        String split;
        int splitIdx, nameLen = colName.length();
        if (nameLen > 2) {
            if (colName.charAt(0) == escape.value && colName.charAt(nameLen - 1) == escape.value) {
                // like: "table"."id" | `table`.`id`
                split = escape.value + DOT + escape.value;
                if ((splitIdx = colName.indexOf(split)) > -1) {
                    return colName.substring(1, splitIdx) + DOT
                        + colName.substring(splitIdx + split.length(), nameLen - 1);
                }
                // like: "id" | `id`
                return colName.substring(1, nameLen - 1);
            } else {
                // like: "table".id | `table`.id
                split = escape.value + DOT;
                if ((splitIdx = colName.indexOf(split)) > -1 && colName.charAt(0) == escape.value) {
                    return colName.substring(1, splitIdx) + DOT
                        + colName.substring(splitIdx + split.length());
                }
                // like: table."id" | table.`id`
                split = DOT + escape.value;
                if ((splitIdx = colName.indexOf(split)) > -1 && colName.charAt(nameLen - 1) == escape.value) {
                    return colName.substring(0, splitIdx) + DOT
                        + colName.substring(splitIdx + split.length(), nameLen - 1);
                }
            }
        }
        return colName;
    }

    /**
     * Add escapes to the column name in list, if necessary
     * <p>
     * 1. Mysql: only deal with keyword.
     * 2. Postgresql: deal with keyword, or that contains upper character.
     * 3. Oracle/OceanBase(Oracle mode): deal with keyword, or that contains lower character.
     * <p>
     * 1. do not consider schema name here, e.g.
     * => in mysql: SELECT * FROM `sampdb`.`member` WHERE `sampdb`.`member`.`member_id` > 100;
     * 2. do not support names that contain escape and dot yet, e.g.
     * a legal name like `table.``123`.id for mysql or "id.""123" for pgsql will return an error result.
     *
     * @param cols   the column name list
     * @param dbType the db type
     * @return the list of column name without escapes
     */
    public static List<String> addEscape(List<String> cols, String dbType) {
        if (CollectionUtils.isEmpty(cols)) {
            return cols;
        }
        List<String> newCols = new ArrayList<>(cols.size());
        for (String col : cols) {
            newCols.add(addEscape(col, dbType));
        }
        return newCols;
    }

    /**
     * Add escapes to the column name, if necessary
     *
     * @param colName column name
     * @param dbType  the db type
     * @return the column name with escapes
     */
    public static String addEscape(String colName, String dbType) {
        if (isMysqlSeries(dbType)) {
            return addEscape(colName, dbType, ColumnUtils.Escape.MYSQL);
        }
        return addEscape(colName, dbType, ColumnUtils.Escape.STANDARD);
    }

    /**
     * Add escapes to the column name, if necessary
     *
     * @param colName column name
     * @param dbType  the db type
     * @param escape  the escape
     * @return the column name with escapes
     */
    private static String addEscape(String colName, String dbType, Escape escape) {
        if (StringUtils.isEmpty(colName)) {
            return colName;
        }

        // check if column name has escapes
        // if it is, return the original value, otherwise check the keyword
        int nameLen = colName.length();
        if (nameLen > 1
            && colName.charAt(0) == escape.value
            && colName.charAt(nameLen - 1) == escape.value) {
            // like: "table"."id" | `table`.`id` | "id" | `id`
            return colName;
        }

        // check if the column name is a keyword,
        // if it is, add escapes, otherwise return the original name
        KeywordChecker keywordChecker = KeywordCheckerFactory.getKeywordChecker(dbType);
        if (keywordChecker != null && !keywordChecker.checkEscape(colName)) {
            return colName;
        }

        // check if the column name is prefixed with other names
        int dotIdx = colName.indexOf(DOT);
        if (dotIdx > -1) {
            // if the column name contains dot
            boolean tbNameWithEscape = dotIdx > 1
                && colName.charAt(dotIdx - 1) == escape.value;
            boolean colNameWithEscape = dotIdx < nameLen - 2
                && colName.charAt(dotIdx + 1) == escape.value;
            if (tbNameWithEscape && colNameWithEscape) {
                // like: "table"."id" | `table`.`id`
                return colName;
            } else {
                StringBuilder escapeNameSb = new StringBuilder();
                if (tbNameWithEscape) {
                    // like: "table".id | `table`.id
                    escapeNameSb.append(colName, 0, dotIdx + 1)
                        .append(escape.value)
                        .append(colName, dotIdx + 1, nameLen)
                        .append(escape.value);
                } else if (colNameWithEscape) {
                    // like: table."id" | table.`id`
                    escapeNameSb.append(escape.value)
                        .append(colName, 0, dotIdx)
                        .append(escape.value)
                        .append(colName, dotIdx, nameLen);
                } else {
                    // like: table.id
                    escapeNameSb.append(escape.value)
                        .append(colName, 0, dotIdx)
                        .append(escape.value)
                        .append(DOT)
                        .append(escape.value)
                        .append(colName, dotIdx + 1, nameLen)
                        .append(escape.value);
                }
                return escapeNameSb.toString();
            }
        } else {
            // if only the column name is included
            // like: id
            char[] buf = new char[nameLen + 2];
            buf[0] = escape.value;
            buf[buf.length - 1] = escape.value;
            colName.getChars(0, colName.length(), buf, 1);
            return new String(buf).intern();
        }
    }

    private static boolean isMysqlSeries(String dbType) {
        return StringUtils.equalsIgnoreCase(dbType, JdbcConstants.MYSQL) ||
            StringUtils.equalsIgnoreCase(dbType, JdbcConstants.H2) ||
            StringUtils.equalsIgnoreCase(dbType, JdbcConstants.MARIADB) ||
            StringUtils.equalsIgnoreCase(dbType, JdbcConstants.OCEANBASE);
    }

    /**
     * Escape character of different SQL type
     */
    public enum Escape {
        /**
         * standard escape
         */
        STANDARD('"'),
        /**
         * mysql escape
         */
        MYSQL('`');
        /**
         * escape character
         */
        public final char value;

        Escape(char value) {
            this.value = value;
        }
    }

}
