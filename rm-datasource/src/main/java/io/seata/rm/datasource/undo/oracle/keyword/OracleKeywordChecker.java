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
package io.seata.rm.datasource.undo.oracle.keyword;

import io.seata.rm.datasource.undo.KeywordChecker;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The type oracle sql keyword checker.
 *
 * @author ccg
 * @date 2019/3/25 oracle keyword checker
 */
public class OracleKeywordChecker implements KeywordChecker {
    private static volatile KeywordChecker keywordChecker = null;
    private static volatile Set<String> keywordSet = null;

    private OracleKeywordChecker() {
    }

    /**
     * get instance of type oracle keyword checker
     *
     * @return instance
     */
    public static KeywordChecker getInstance() {
        if (keywordChecker == null) {
            synchronized (OracleKeywordChecker.class) {
                if (keywordChecker == null) {
                    keywordChecker = new OracleKeywordChecker();
                    keywordSet = Arrays.stream(OracleKeyword.values()).map(OracleKeyword::name).collect(Collectors.toSet());
                }
            }
        }
        return keywordChecker;
    }

    /**
     * oracle keyword
     */
    private enum OracleKeyword {
        /**
         * Select sql keyword.
         */
        SELECT("SELECT"),
        /**
         * Delete sql keyword.
         */
        DELETE("DELETE"),
        /**
         * Insert sql keyword.
         */
        INSERT("INSERT"),
        /**
         * Update sql keyword.
         */
        UPDATE("UPDATE"),

        /**
         * From sql keyword.
         */
        FROM("FROM"),
        /**
         * Having sql keyword.
         */
        HAVING("HAVING"),
        /**
         * Where sql keyword.
         */
        WHERE("WHERE"),
        /**
         * Order sql keyword.
         */
        ORDER("ORDER"),
        /**
         * By sql keyword.
         */
        BY("BY"),
        /**
         * Group sql keyword.
         */
        GROUP("GROUP"),
        /**
         * Into sql keyword.
         */
        INTO("INTO"),
        /**
         * As sql keyword.
         */
        AS("AS"),

        /**
         * Create sql keyword.
         */
        CREATE("CREATE"),
        /**
         * Alter sql keyword.
         */
        ALTER("ALTER"),
        /**
         * Drop sql keyword.
         */
        DROP("DROP"),
        /**
         * Set sql keyword.
         */
        SET("SET"),

        /**
         * Null sql keyword.
         */
        NULL("NULL"),
        /**
         * Not sql keyword.
         */
        NOT("NOT"),
        /**
         * Distinct sql keyword.
         */
        DISTINCT("DISTINCT"),

        /**
         * Table sql keyword.
         */
        TABLE("TABLE"),
        /**
         * Tablespace sql keyword.
         */
        TABLESPACE("TABLESPACE"),
        /**
         * View sql keyword.
         */
        VIEW("VIEW"),
        /**
         * Sequence sql keyword.
         */
        SEQUENCE("SEQUENCE"),
        /**
         * Trigger sql keyword.
         */
        TRIGGER("TRIGGER"),
        /**
         * User sql keyword.
         */
        USER("USER"),
        /**
         * Index sql keyword.
         */
        INDEX("INDEX"),
        /**
         * Session sql keyword.
         */
        SESSION("SESSION"),
        /**
         * Procedure sql keyword.
         */
        PROCEDURE("PROCEDURE"),
        /**
         * Function sql keyword.
         */
        FUNCTION("FUNCTION"),

        /**
         * Primary sql keyword.
         */
        PRIMARY("PRIMARY"),
        /**
         * Key sql keyword.
         */
        KEY("KEY"),
        /**
         * Default sql keyword.
         */
        DEFAULT("DEFAULT"),
        /**
         * Constraint sql keyword.
         */
        CONSTRAINT("CONSTRAINT"),
        /**
         * Check sql keyword.
         */
        CHECK("CHECK"),
        /**
         * Unique sql keyword.
         */
        UNIQUE("UNIQUE"),
        /**
         * Foreign sql keyword.
         */
        FOREIGN("FOREIGN"),
        /**
         * References sql keyword.
         */
        REFERENCES("REFERENCES"),

        /**
         * Explain sql keyword.
         */
        EXPLAIN("EXPLAIN"),
        /**
         * For sql keyword.
         */
        FOR("FOR"),
        /**
         * If sql keyword.
         */
        IF("IF"),
        /**
         * Sort sql keyword.
         */
        SORT("SORT"),

        /**
         * All sql keyword.
         */
        ALL("ALL"),
        /**
         * Union sql keyword.
         */
        UNION("UNION"),
        /**
         * Except sql keyword.
         */
        EXCEPT("EXCEPT"),
        /**
         * Intersect sql keyword.
         */
        INTERSECT("INTERSECT"),
        /**
         * Minus sql keyword.
         */
        MINUS("MINUS"),
        /**
         * Inner sql keyword.
         */
        INNER("INNER"),
        /**
         * Left sql keyword.
         */
        LEFT("LEFT"),
        /**
         * Right sql keyword.
         */
        RIGHT("RIGHT"),
        /**
         * Full sql keyword.
         */
        FULL("FULL"),
        /**
         * Outer sql keyword.
         */
        OUTER("OUTER"),
        /**
         * Join sql keyword.
         */
        JOIN("JOIN"),
        /**
         * On sql keyword.
         */
        ON("ON"),
        /**
         * Schema sql keyword.
         */
        SCHEMA("SCHEMA"),
        /**
         * Cast sql keyword.
         */
        CAST("CAST"),
        /**
         * Column sql keyword.
         */
        COLUMN("COLUMN"),
        /**
         * Use sql keyword.
         */
        USE("USE"),
        /**
         * Database sql keyword.
         */
        DATABASE("DATABASE"),
        /**
         * To sql keyword.
         */
        TO("TO"),

        /**
         * And sql keyword.
         */
        AND("AND"),
        /**
         * Or sql keyword.
         */
        OR("OR"),
        /**
         * Xor sql keyword.
         */
        XOR("XOR"),
        /**
         * Case sql keyword.
         */
        CASE("CASE"),
        /**
         * When sql keyword.
         */
        WHEN("WHEN"),
        /**
         * Then sql keyword.
         */
        THEN("THEN"),
        /**
         * Else sql keyword.
         */
        ELSE("ELSE"),
        /**
         * Elsif sql keyword.
         */
        ELSIF("ELSIF"),
        /**
         * End sql keyword.
         */
        END("END"),
        /**
         * Exists sql keyword.
         */
        EXISTS("EXISTS"),
        /**
         * In sql keyword.
         */
        IN("IN"),
        /**
         * Contains sql keyword.
         */
        CONTAINS("CONTAINS"),
        /**
         * Rlike sql keyword.
         */
        RLIKE("RLIKE"),
        /**
         * Fulltext sql keyword.
         */
        FULLTEXT("FULLTEXT"),

        /**
         * New sql keyword.
         */
        NEW("NEW"),
        /**
         * Asc sql keyword.
         */
        ASC("ASC"),
        /**
         * Desc sql keyword.
         */
        DESC("DESC"),
        /**
         * Is sql keyword.
         */
        IS("IS"),
        /**
         * Like sql keyword.
         */
        LIKE("LIKE"),
        /**
         * Escape sql keyword.
         */
        ESCAPE("ESCAPE"),
        /**
         * Between sql keyword.
         */
        BETWEEN("BETWEEN"),
        /**
         * Values sql keyword.
         */
        VALUES("VALUES"),
        /**
         * Interval sql keyword.
         */
        INTERVAL("INTERVAL"),

        /**
         * Lock sql keyword.
         */
        LOCK("LOCK"),
       
        /**
         * Some sql keyword.
         */
        

        SOME("SOME");


        /**
         * The Name.
         */
        public final String name;

        OracleKeyword(String name) {
            this.name = name;
        }
    }


    @Override
    public boolean check(String fieldOrTableName) {
        if (keywordSet.contains(fieldOrTableName)) {
            return true;
        }
        if (null != fieldOrTableName) {
            fieldOrTableName = fieldOrTableName.toUpperCase();
        }
        return keywordSet.contains(fieldOrTableName);

    }

    @Override
    public String checkAndReplace(String fieldOrTableName) {
        return check(fieldOrTableName)? fieldOrTableName :fieldOrTableName;
//        return check(fieldOrTableName)?"`" + fieldOrTableName + "`":fieldOrTableName;
    }
}
