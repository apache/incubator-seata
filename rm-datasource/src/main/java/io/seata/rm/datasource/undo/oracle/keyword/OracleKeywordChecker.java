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
    private Set<String> keywordSet;

    private OracleKeywordChecker() {
        keywordSet = Arrays.stream(OracleKeyword.values()).map(OracleKeyword::name).collect(Collectors.toSet());
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
         * ACCESS is oracle keyword
         */
        ACCESS("ACCESS"),
        /**
         * ADD is oracle keyword
         */
        ADD("ADD"),
        /**
         * ALL is oracle keyword
         */
        ALL("ALL"),
        /**
         * ALTER is oracle keyword
         */
        ALTER("ALTER"),
        /**
         * AND is oracle keyword
         */
        AND("AND"),
        /**
         * ANY is oracle keyword
         */
        ANY("ANY"),
        /**
         * AS is oracle keyword
         */
        AS("AS"),
        /**
         * ASC is oracle keyword
         */
        ASC("ASC"),
        /**
         * AUDIT is oracle keyword
         */
        AUDIT("AUDIT"),
        /**
         * BETWEEN is oracle keyword
         */
        BETWEEN("BETWEEN"),
        /**
         * BY is oracle keyword
         */
        BY("BY"),
        /**
         * CHAR is oracle keyword
         */
        CHAR("CHAR"),
        /**
         * CHECK is oracle keyword
         */
        CHECK("CHECK"),
        /**
         * CLUSTER is oracle keyword
         */
        CLUSTER("CLUSTER"),
        /**
         * COLUMN is oracle keyword
         */
        COLUMN("COLUMN"),
        /**
         * COLUMN_VALUE is oracle keyword
         */
        COLUMN_VALUE("COLUMN_VALUE"),
        /**
         * COMMENT is oracle keyword
         */
        COMMENT("COMMENT"),
        /**
         * COMPRESS is oracle keyword
         */
        COMPRESS("COMPRESS"),
        /**
         * CONNECT is oracle keyword
         */
        CONNECT("CONNECT"),
        /**
         * CREATE is oracle keyword
         */
        CREATE("CREATE"),
        /**
         * CURRENT is oracle keyword
         */
        CURRENT("CURRENT"),
        /**
         * DATE is oracle keyword
         */
        DATE("DATE"),
        /**
         * DECIMAL is oracle keyword
         */
        DECIMAL("DECIMAL"),
        /**
         * DEFAULT is oracle keyword
         */
        DEFAULT("DEFAULT"),
        /**
         * DELETE is oracle keyword
         */
        DELETE("DELETE"),
        /**
         * DESC is oracle keyword
         */
        DESC("DESC"),
        /**
         * DISTINCT is oracle keyword
         */
        DISTINCT("DISTINCT"),
        /**
         * DROP is oracle keyword
         */
        DROP("DROP"),
        /**
         * ELSE is oracle keyword
         */
        ELSE("ELSE"),
        /**
         * EXCLUSIVE is oracle keyword
         */
        EXCLUSIVE("EXCLUSIVE"),
        /**
         * EXISTS is oracle keyword
         */
        EXISTS("EXISTS"),
        /**
         * FILE is oracle keyword
         */
        FILE("FILE"),
        /**
         * FLOAT is oracle keyword
         */
        FLOAT("FLOAT"),
        /**
         * FOR is oracle keyword
         */
        FOR("FOR"),
        /**
         * FROM is oracle keyword
         */
        FROM("FROM"),
        /**
         * GRANT is oracle keyword
         */
        GRANT("GRANT"),
        /**
         * GROUP is oracle keyword
         */
        GROUP("GROUP"),
        /**
         * HAVING is oracle keyword
         */
        HAVING("HAVING"),
        /**
         * IDENTIFIED is oracle keyword
         */
        IDENTIFIED("IDENTIFIED"),
        /**
         * IMMEDIATE is oracle keyword
         */
        IMMEDIATE("IMMEDIATE"),
        /**
         * IN is oracle keyword
         */
        IN("IN"),
        /**
         * INCREMENT is oracle keyword
         */
        INCREMENT("INCREMENT"),
        /**
         * INDEX is oracle keyword
         */
        INDEX("INDEX"),
        /**
         * INITIAL is oracle keyword
         */
        INITIAL("INITIAL"),
        /**
         * INSERT is oracle keyword
         */
        INSERT("INSERT"),
        /**
         * INTEGER is oracle keyword
         */
        INTEGER("INTEGER"),
        /**
         * INTERSECT is oracle keyword
         */
        INTERSECT("INTERSECT"),
        /**
         * INTO is oracle keyword
         */
        INTO("INTO"),
        /**
         * IS is oracle keyword
         */
        IS("IS"),
        /**
         * LEVEL is oracle keyword
         */
        LEVEL("LEVEL"),
        /**
         * LIKE is oracle keyword
         */
        LIKE("LIKE"),
        /**
         * LOCK is oracle keyword
         */
        LOCK("LOCK"),
        /**
         * LONG is oracle keyword
         */
        LONG("LONG"),
        /**
         * MAXEXTENTS is oracle keyword
         */
        MAXEXTENTS("MAXEXTENTS"),
        /**
         * MINUS is oracle keyword
         */
        MINUS("MINUS"),
        /**
         * MLSLABEL is oracle keyword
         */
        MLSLABEL("MLSLABEL"),
        /**
         * MODE is oracle keyword
         */
        MODE("MODE"),
        /**
         * MODIFY is oracle keyword
         */
        MODIFY("MODIFY"),
        /**
         * NESTED_TABLE_ID is oracle keyword
         */
        NESTED_TABLE_ID("NESTED_TABLE_ID"),
        /**
         * NOAUDIT is oracle keyword
         */
        NOAUDIT("NOAUDIT"),
        /**
         * NOCOMPRESS is oracle keyword
         */
        NOCOMPRESS("NOCOMPRESS"),
        /**
         * NOT is oracle keyword
         */
        NOT("NOT"),
        /**
         * NOWAIT is oracle keyword
         */
        NOWAIT("NOWAIT"),
        /**
         * NULL is oracle keyword
         */
        NULL("NULL"),
        /**
         * NUMBER is oracle keyword
         */
        NUMBER("NUMBER"),
        /**
         * OF is oracle keyword
         */
        OF("OF"),
        /**
         * OFFLINE is oracle keyword
         */
        OFFLINE("OFFLINE"),
        /**
         * ON is oracle keyword
         */
        ON("ON"),
        /**
         * ONLINE is oracle keyword
         */
        ONLINE("ONLINE"),
        /**
         * OPTION is oracle keyword
         */
        OPTION("OPTION"),
        /**
         * OR is oracle keyword
         */
        OR("OR"),
        /**
         * ORDER is oracle keyword
         */
        ORDER("ORDER"),
        /**
         * PCTFREE is oracle keyword
         */
        PCTFREE("PCTFREE"),
        /**
         * PRIOR is oracle keyword
         */
        PRIOR("PRIOR"),
        /**
         * PUBLIC is oracle keyword
         */
        PUBLIC("PUBLIC"),
        /**
         * RAW is oracle keyword
         */
        RAW("RAW"),
        /**
         * RENAME is oracle keyword
         */
        RENAME("RENAME"),
        /**
         * RESOURCE is oracle keyword
         */
        RESOURCE("RESOURCE"),
        /**
         * REVOKE is oracle keyword
         */
        REVOKE("REVOKE"),
        /**
         * ROW is oracle keyword
         */
        ROW("ROW"),
        /**
         * ROWID is oracle keyword
         */
        ROWID("ROWID"),
        /**
         * ROWNUM is oracle keyword
         */
        ROWNUM("ROWNUM"),
        /**
         * ROWS is oracle keyword
         */
        ROWS("ROWS"),
        /**
         * SELECT is oracle keyword
         */
        SELECT("SELECT"),
        /**
         * SESSION is oracle keyword
         */
        SESSION("SESSION"),
        /**
         * SET is oracle keyword
         */
        SET("SET"),
        /**
         * SHARE is oracle keyword
         */
        SHARE("SHARE"),
        /**
         * SIZE is oracle keyword
         */
        SIZE("SIZE"),
        /**
         * SMALLINT is oracle keyword
         */
        SMALLINT("SMALLINT"),
        /**
         * START is oracle keyword
         */
        START("START"),
        /**
         * SUCCESSFUL is oracle keyword
         */
        SUCCESSFUL("SUCCESSFUL"),
        /**
         * SYNONYM is oracle keyword
         */
        SYNONYM("SYNONYM"),
        /**
         * SYSDATE is oracle keyword
         */
        SYSDATE("SYSDATE"),
        /**
         * TABLE is oracle keyword
         */
        TABLE("TABLE"),
        /**
         * THEN is oracle keyword
         */
        THEN("THEN"),
        /**
         * TO is oracle keyword
         */
        TO("TO"),
        /**
         * TRIGGER is oracle keyword
         */
        TRIGGER("TRIGGER"),
        /**
         * UID is oracle keyword
         */
        UID("UID"),
        /**
         * UNION is oracle keyword
         */
        UNION("UNION"),
        /**
         * UNIQUE is oracle keyword
         */
        UNIQUE("UNIQUE"),
        /**
         * UPDATE is oracle keyword
         */
        UPDATE("UPDATE"),
        /**
         * USER is oracle keyword
         */
        USER("USER"),
        /**
         * VALIDATE is oracle keyword
         */
        VALIDATE("VALIDATE"),
        /**
         * VALUES is oracle keyword
         */
        VALUES("VALUES"),
        /**
         * VARCHAR is oracle keyword
         */
        VARCHAR("VARCHAR"),
        /**
         * VARCHAR2 is oracle keyword
         */
        VARCHAR2("VARCHAR2"),
        /**
         * VIEW is oracle keyword
         */
        VIEW("VIEW"),
        /**
         * WHENEVER is oracle keyword
         */
        WHENEVER("WHENEVER"),
        /**
         * WHERE is oracle keyword
         */
        WHERE("WHERE"),
        /**
         * WITH is oracle keyword
         */
        WITH("WITH");
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
