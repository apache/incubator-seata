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
package io.seata.rm.datasource.sql.constant;

/**
 * sql constant to support splicing sql
 *
 * @author lvekee
 */
public class SqlConstants {
    /**
     * symbol
     */
    public static final String SPACE = " ";
    public static final String LEFT_PARENTHESES = "(";
    public static final String RIGHT_PARENTHESES = ")";
    public static final String DOT = ".";
    public static final String COMMA = ",";
    public static final String COLON = ":";
    public static final String APOSTROPHE = "'";
    public static final String DOUBLE_QUOTES = "\"";
    public static final String EQUAL = "=";
    public static final String UNDERSCORE = "_";
    public static final String ASTERISK = "*";
    public static final String PLACEHOLDER = "?";
    public static final String COLUMN_SEPARATOR = "|";
    public static final String JOINER_DELIMITER = COMMA + SPACE;
    public static final String COMMA_TEM = SPACE + COMMA + SPACE;
    public static final String ASTERISK_TEM = SPACE + ASTERISK + SPACE;
    public static final String EQUAL_TEM = SPACE + EQUAL + SPACE;

    /**
     * SQL key
     */
    public static final String NULL = "NULL";
    public static final String PRIMARY = "PRIMARY";
    public static final String SELECT = "SELECT ";
    public static final String FROM = " FROM ";
    public static final String WHERE = " WHERE ";
    public static final String WHERE_PREFIX = " WHERE (";
    public static final String OR = " OR ";
    public static final String OR_PREFIX = " OR (";
    public static final String AND = " AND ";
    public static final String FOR_UPDATE = " FOR UPDATE";

    public static final String SELECT_ALL = "SELECT * ";
    public static final String SELECT_LAST_INSERT_ID = "SELECT LAST_INSERT_ID()";
    public static final String DEFAULT_PREFIX = " = DEFAULT(";
    public static final String PG_CURRVAL_PREFIX = "SELECT currval(";
    public static final String ORACLE_CURRVAL_SUFFIX = ".currval FROM DUAL";
    public static final String IS_PLACEHOLDER = " is ? ";
    public static final String EQUAL_PLACEHOLDER = " = ? ";
    public static final String SHOW_VARIABLES_AUTO_INCREMENT = "SHOW VARIABLES LIKE 'auto_increment_increment'";
}
