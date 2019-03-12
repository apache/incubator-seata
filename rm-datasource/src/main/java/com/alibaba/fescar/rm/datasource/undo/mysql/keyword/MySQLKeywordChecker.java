/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.fescar.rm.datasource.undo.mysql.keyword;

import com.alibaba.fescar.rm.datasource.undo.KeywordChecker;

import org.apache.commons.lang3.StringUtils;

/**
 * The type My sql keyword checker.
 *
 * @author Wu
 * @date 2019 /3/5 MySQL keyword checker
 */
public class MySQLKeywordChecker implements KeywordChecker {
    private static volatile KeywordChecker keywordChecker;

    private MySQLKeywordChecker() {}

    /**
     * get instance of type MySQL keyword checker
     *
     * @return instance
     */
    public static KeywordChecker getInstance() {
        if (keywordChecker == null) {
            synchronized (MySQLKeywordChecker.class) {
                if (keywordChecker == null) {
                    keywordChecker = new MySQLKeywordChecker();
                }
            }
        }
        return keywordChecker;
    }

    /**
     * MySQL keyword
     */
    private enum MySQLKeyword {
        /**
         * Select my sql keyword.
         */
        SELECT("SELECT"),
        /**
         * Delete my sql keyword.
         */
        DELETE("DELETE"),
        /**
         * Insert my sql keyword.
         */
        INSERT("INSERT"),
        /**
         * Update my sql keyword.
         */
        UPDATE("UPDATE"),

        /**
         * From my sql keyword.
         */
        FROM("FROM"),
        /**
         * Having my sql keyword.
         */
        HAVING("HAVING"),
        /**
         * Where my sql keyword.
         */
        WHERE("WHERE"),
        /**
         * Order my sql keyword.
         */
        ORDER("ORDER"),
        /**
         * By my sql keyword.
         */
        BY("BY"),
        /**
         * Group my sql keyword.
         */
        GROUP("GROUP"),
        /**
         * Into my sql keyword.
         */
        INTO("INTO"),
        /**
         * As my sql keyword.
         */
        AS("AS"),

        /**
         * Create my sql keyword.
         */
        CREATE("CREATE"),
        /**
         * Alter my sql keyword.
         */
        ALTER("ALTER"),
        /**
         * Drop my sql keyword.
         */
        DROP("DROP"),
        /**
         * Set my sql keyword.
         */
        SET("SET"),

        /**
         * Null my sql keyword.
         */
        NULL("NULL"),
        /**
         * Not my sql keyword.
         */
        NOT("NOT"),
        /**
         * Distinct my sql keyword.
         */
        DISTINCT("DISTINCT"),

        /**
         * Table my sql keyword.
         */
        TABLE("TABLE"),
        /**
         * Tablespace my sql keyword.
         */
        TABLESPACE("TABLESPACE"),
        /**
         * View my sql keyword.
         */
        VIEW("VIEW"),
        /**
         * Sequence my sql keyword.
         */
        SEQUENCE("SEQUENCE"),
        /**
         * Trigger my sql keyword.
         */
        TRIGGER("TRIGGER"),
        /**
         * User my sql keyword.
         */
        USER("USER"),
        /**
         * Index my sql keyword.
         */
        INDEX("INDEX"),
        /**
         * Session my sql keyword.
         */
        SESSION("SESSION"),
        /**
         * Procedure my sql keyword.
         */
        PROCEDURE("PROCEDURE"),
        /**
         * Function my sql keyword.
         */
        FUNCTION("FUNCTION"),

        /**
         * Primary my sql keyword.
         */
        PRIMARY("PRIMARY"),
        /**
         * Key my sql keyword.
         */
        KEY("KEY"),
        /**
         * Default my sql keyword.
         */
        DEFAULT("DEFAULT"),
        /**
         * Constraint my sql keyword.
         */
        CONSTRAINT("CONSTRAINT"),
        /**
         * Check my sql keyword.
         */
        CHECK("CHECK"),
        /**
         * Unique my sql keyword.
         */
        UNIQUE("UNIQUE"),
        /**
         * Foreign my sql keyword.
         */
        FOREIGN("FOREIGN"),
        /**
         * References my sql keyword.
         */
        REFERENCES("REFERENCES"),

        /**
         * Explain my sql keyword.
         */
        EXPLAIN("EXPLAIN"),
        /**
         * For my sql keyword.
         */
        FOR("FOR"),
        /**
         * If my sql keyword.
         */
        IF("IF"),
        /**
         * Sort my sql keyword.
         */
        SORT("SORT"),

        /**
         * All my sql keyword.
         */
        ALL("ALL"),
        /**
         * Union my sql keyword.
         */
        UNION("UNION"),
        /**
         * Except my sql keyword.
         */
        EXCEPT("EXCEPT"),
        /**
         * Intersect my sql keyword.
         */
        INTERSECT("INTERSECT"),
        /**
         * Minus my sql keyword.
         */
        MINUS("MINUS"),
        /**
         * Inner my sql keyword.
         */
        INNER("INNER"),
        /**
         * Left my sql keyword.
         */
        LEFT("LEFT"),
        /**
         * Right my sql keyword.
         */
        RIGHT("RIGHT"),
        /**
         * Full my sql keyword.
         */
        FULL("FULL"),
        /**
         * Outer my sql keyword.
         */
        OUTER("OUTER"),
        /**
         * Join my sql keyword.
         */
        JOIN("JOIN"),
        /**
         * On my sql keyword.
         */
        ON("ON"),
        /**
         * Schema my sql keyword.
         */
        SCHEMA("SCHEMA"),
        /**
         * Cast my sql keyword.
         */
        CAST("CAST"),
        /**
         * Column my sql keyword.
         */
        COLUMN("COLUMN"),
        /**
         * Use my sql keyword.
         */
        USE("USE"),
        /**
         * Database my sql keyword.
         */
        DATABASE("DATABASE"),
        /**
         * To my sql keyword.
         */
        TO("TO"),

        /**
         * And my sql keyword.
         */
        AND("AND"),
        /**
         * Or my sql keyword.
         */
        OR("OR"),
        /**
         * Xor my sql keyword.
         */
        XOR("XOR"),
        /**
         * Case my sql keyword.
         */
        CASE("CASE"),
        /**
         * When my sql keyword.
         */
        WHEN("WHEN"),
        /**
         * Then my sql keyword.
         */
        THEN("THEN"),
        /**
         * Else my sql keyword.
         */
        ELSE("ELSE"),
        /**
         * Elsif my sql keyword.
         */
        ELSIF("ELSIF"),
        /**
         * End my sql keyword.
         */
        END("END"),
        /**
         * Exists my sql keyword.
         */
        EXISTS("EXISTS"),
        /**
         * In my sql keyword.
         */
        IN("IN"),
        /**
         * Contains my sql keyword.
         */
        CONTAINS("CONTAINS"),
        /**
         * Rlike my sql keyword.
         */
        RLIKE("RLIKE"),
        /**
         * Fulltext my sql keyword.
         */
        FULLTEXT("FULLTEXT"),

        /**
         * New my sql keyword.
         */
        NEW("NEW"),
        /**
         * Asc my sql keyword.
         */
        ASC("ASC"),
        /**
         * Desc my sql keyword.
         */
        DESC("DESC"),
        /**
         * Is my sql keyword.
         */
        IS("IS"),
        /**
         * Like my sql keyword.
         */
        LIKE("LIKE"),
        /**
         * Escape my sql keyword.
         */
        ESCAPE("ESCAPE"),
        /**
         * Between my sql keyword.
         */
        BETWEEN("BETWEEN"),
        /**
         * Values my sql keyword.
         */
        VALUES("VALUES"),
        /**
         * Interval my sql keyword.
         */
        INTERVAL("INTERVAL"),

        /**
         * Lock my sql keyword.
         */
        LOCK("LOCK"),
        /**
         * Some my sql keyword.
         */
        SOME("SOME"),
        /**
         * Any my sql keyword.
         */
        ANY("ANY"),
        /**
         * Truncate my sql keyword.
         */
        TRUNCATE("TRUNCATE"),

        /**
         * Return my sql keyword.
         */
        RETURN("RETURN"),

        /**
         * True my sql keyword.
         */
        // mysql
        TRUE("TRUE"),
        /**
         * False my sql keyword.
         */
        FALSE("FALSE"),
        /**
         * Limit my sql keyword.
         */
        LIMIT("LIMIT"),
        /**
         * Kill my sql keyword.
         */
        KILL("KILL"),
        /**
         * Identified my sql keyword.
         */
        IDENTIFIED("IDENTIFIED"),
        /**
         * Password my sql keyword.
         */
        PASSWORD("PASSWORD"),
        /**
         * Algorithm my sql keyword.
         */
        ALGORITHM("ALGORITHM"),
        /**
         * Dual my sql keyword.
         */
        DUAL("DUAL"),
        /**
         * Binary my sql keyword.
         */
        BINARY("BINARY"),
        /**
         * Show my sql keyword.
         */
        SHOW("SHOW"),
        /**
         * Replace my sql keyword.
         */
        REPLACE("REPLACE"),

        /**
         * The While.
         */
        // MySql procedure add by zz
        WHILE("WHILE"),
        /**
         * Do my sql keyword.
         */
        DO("DO"),
        /**
         * Leave my sql keyword.
         */
        LEAVE("LEAVE"),
        /**
         * Iterate my sql keyword.
         */
        ITERATE("ITERATE"),
        /**
         * Repeat my sql keyword.
         */
        REPEAT("REPEAT"),
        /**
         * Until my sql keyword.
         */
        UNTIL("UNTIL"),
        /**
         * Open my sql keyword.
         */
        OPEN("OPEN"),
        /**
         * Close my sql keyword.
         */
        CLOSE("CLOSE"),
        /**
         * Out my sql keyword.
         */
        OUT("OUT"),
        /**
         * Inout my sql keyword.
         */
        INOUT("INOUT"),
        /**
         * Exit my sql keyword.
         */
        EXIT("EXIT"),
        /**
         * Undo my sql keyword.
         */
        UNDO("UNDO"),
        /**
         * Sqlstate my sql keyword.
         */
        SQLSTATE("SQLSTATE"),
        /**
         * Condition my sql keyword.
         */
        CONDITION("CONDITION"),
        /**
         * Div my sql keyword.
         */
        DIV("DIV");

        /**
         * The Name.
         */
        public final String name;

        MySQLKeyword() {
            this(null);
        }

        MySQLKeyword(String name) {
            this.name = name;
        }
    }

    @Override
    public boolean check(String fieldOrTableName) {
        try {
            if (StringUtils.isNotBlank(fieldOrTableName)) {
                MySQLKeyword.valueOf(fieldOrTableName.toUpperCase());
                return true;
            }
        } catch (IllegalArgumentException e) {
            //do nothing
        }
        return false;
    }

    @Override
    public String checkAndReplace(String fieldOrTableName) {
        return check(fieldOrTableName) ? "`" + fieldOrTableName + "`" : fieldOrTableName;
    }
}
