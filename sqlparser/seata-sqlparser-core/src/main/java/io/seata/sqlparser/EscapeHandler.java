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
package io.seata.sqlparser;

import io.seata.common.util.StringUtils;
import io.seata.sqlparser.struct.TableMeta;

/**
 * The interface Keyword checker.
 *
 * @author Wu
 */
public interface EscapeHandler {

    String DOT = ".";
    /**
     * check whether given field name and table name use keywords
     *
     * @param fieldOrTableName the field or table name
     * @return boolean
     */
    boolean checkIfKeyWords(String fieldOrTableName);


    /**
     * check whether given field or table name use keywords. the method has database special logic.
     * @param columnName the column or table name
     * @param tableMeta the tableMeta
     * @return true: need to escape. false: no need to escape.
     */
    boolean checkIfNeedEscape(String columnName, TableMeta tableMeta);

    default char getEscapeSymbol() {
        return '"';
    }

    /**
     * check fieldOrTableName if contains escape
     * @param fieldOrTableName
     * @return true if contains,otherwise return false
     */
    default boolean containsEscape(String fieldOrTableName) {
        fieldOrTableName = fieldOrTableName.trim();
        char escapeSymbol = getEscapeSymbol();
        if (fieldOrTableName.charAt(0) == escapeSymbol && fieldOrTableName.charAt(fieldOrTableName.length() - 1) == escapeSymbol) {
            return true;
        }
        return false;
    }

    /**
     * add escape if colName is keywords
     * @param colName colName
     * @return colName
     */
    default String addColNameEscape(String colName) {
        return addColNameEscape(colName, null);
    }

    /**
     * add escape if colName is keywords
     * @param colName colName
     * @param tableMeta tableMeta
     * @return colName
     */
    default String addColNameEscape(String colName, TableMeta tableMeta) {
        String colNameToCheck = colName;
        if (colName.contains(DOT)) {
            colNameToCheck = colName.substring(colName.lastIndexOf(DOT) + 1);
        }

        boolean needEscape = checkIfNeedEscape(colNameToCheck, tableMeta);
        if (!needEscape) {
            return colName;
        }
        char escapeChar = getEscapeSymbol();
        if (colName.contains(DOT)) {
            // like "scheme".id `scheme`.id
            String str = escapeChar + DOT;
            int dotIndex = colName.indexOf(str);
            if (dotIndex > -1) {
                return new StringBuilder().append(colName, 0, dotIndex + str.length()).append(escapeChar)
                    .append(colName.substring(dotIndex + str.length())).append(escapeChar).toString();
            }
            // like scheme."id" scheme.`id`
            str = DOT + escapeChar;
            dotIndex = colName.indexOf(str);
            if (dotIndex > -1) {
                return new StringBuilder().append(escapeChar).append(colName, 0, dotIndex).append(escapeChar)
                    .append(colName.substring(dotIndex)).toString();
            }

            str = DOT;
            dotIndex = colName.indexOf(str);
            if (dotIndex > -1) {
                return new StringBuilder().append(escapeChar).append(colName, 0, dotIndex).append(escapeChar)
                    .append(DOT).append(escapeChar).append(colName.substring(dotIndex + str.length())).append(
                        escapeChar).toString();
            }
        }

        char[] buf = new char[colName.length() + 2];
        buf[0] = escapeChar;
        buf[buf.length - 1] = escapeChar;

        colName.getChars(0, colName.length(), buf, 1);

        return new String(buf).intern();

    }

    /**
     *
     *  https://db.apache.org/derby/docs/10.1/ref/crefsqlj1003454.html
     *  https://docs.oracle.com/javadb/10.8.3.0/ref/crefsqlj1003454.html
     *  https://www.informit.com/articles/article.aspx?p=2036581&seqNum=2
     * @param colName
     * @return
     */
    default String delColNameEscape(String colName) {
        if (StringUtils.isBlank(colName)) {
            return colName;
        }
        char escapeChar = getEscapeSymbol();
        if (colName.charAt(0) == escapeChar && colName.charAt(colName.length() - 1) == escapeChar) {
            // like "scheme"."id" `scheme`.`id`
            String str = escapeChar + DOT + escapeChar;
            int index = colName.indexOf(str);
            if (index > -1) {
                return colName.substring(1, index) + DOT + colName.substring(index + str.length(),
                    colName.length() - 1);
            }
            return colName.substring(1, colName.length() - 1);
        } else {
            // like "scheme".id `scheme`.id
            String str = escapeChar + DOT;
            int index = colName.indexOf(str);
            if (index > -1 && colName.charAt(0) == escapeChar) {
                return colName.substring(1, index) + DOT + colName.substring(index + str.length());
            }
            // like scheme."id" scheme.`id`
            str = DOT + escapeChar;
            index = colName.indexOf(str);
            if (index > -1 && colName.charAt(colName.length() - 1) == escapeChar) {
                return colName.substring(0, index) + DOT + colName.substring(index + str.length(),
                    colName.length() - 1);
            }
        }
        return colName;
    }

}
