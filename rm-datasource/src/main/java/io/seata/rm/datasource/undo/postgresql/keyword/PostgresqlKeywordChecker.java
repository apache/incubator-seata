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
package io.seata.rm.datasource.undo.postgresql.keyword;

import io.seata.rm.datasource.undo.KeywordChecker;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The type postgresql undo delete executor.
 *
 * @author japsercloud
 */
public class PostgresqlKeywordChecker implements KeywordChecker {
    private static volatile KeywordChecker keywordChecker = null;
    private static volatile Set<String> keywordSet = null;

    private PostgresqlKeywordChecker() {
    }

    /**
     * get instance of type postgresql keyword checker
     *
     * @return instance
     */
    public static KeywordChecker getInstance() {
        if (keywordChecker == null) {
            synchronized (PostgresqlKeywordChecker.class) {
                if (keywordChecker == null) {
                    keywordChecker = new PostgresqlKeywordChecker();
                    keywordSet = Arrays.stream(PostgresqlKeyword.values()).map(PostgresqlKeyword::name).collect(Collectors.toSet());
                }
            }
        }
        return keywordChecker;
    }

    /**
     * postgresql keyword
     */
    private enum PostgresqlKeyword {
        /**
         * ACCESS is postgresql keyword
         */
        ACCESS("ACCESS"),
        /**
         * ADD is postgresql keyword
         */
        ADD("ADD"),
        /**
         * ALL is postgresql keyword
         */
        ALL("ALL"),
        /**
         * ALTER is postgresql keyword
         */
        ALTER("ALTER"),
        /**
         * AND is postgresql keyword
         */
        AND("AND"),
        /**
         * ANY is postgresql keyword
         */
        ANY("ANY"),
        /**
         * AS is postgresql keyword
         */
        AS("AS"),
        /**
         * ASC is postgresql keyword
         */
        ASC("ASC"),
        /**
         * AUDIT is postgresql keyword
         */
        AUDIT("AUDIT"),
        /**
         * BETWEEN is postgresql keyword
         */
        BETWEEN("BETWEEN"),
        /**
         * BY is postgresql keyword
         */
        BY("BY"),
        /**
         * CHAR is postgresql keyword
         */
        CHAR("CHAR"),
        /**
         * CHECK is postgresql keyword
         */
        CHECK("CHECK"),
        /**
         * CLUSTER is postgresql keyword
         */
        CLUSTER("CLUSTER"),
        /**
         * COLUMN is postgresql keyword
         */
        COLUMN("COLUMN"),
        /**
         * COLUMN_VALUE is postgresql keyword
         */
        COLUMN_VALUE("COLUMN_VALUE"),
        /**
         * COMMENT is postgresql keyword
         */
        COMMENT("COMMENT"),
        /**
         * COMPRESS is postgresql keyword
         */
        COMPRESS("COMPRESS"),
        /**
         * CONNECT is postgresql keyword
         */
        CONNECT("CONNECT"),
        /**
         * CREATE is postgresql keyword
         */
        CREATE("CREATE"),
        /**
         * CURRENT is postgresql keyword
         */
        CURRENT("CURRENT"),
        /**
         * DATE is postgresql keyword
         */
        DATE("DATE"),
        /**
         * DECIMAL is postgresql keyword
         */
        DECIMAL("DECIMAL"),
        /**
         * DEFAULT is postgresql keyword
         */
        DEFAULT("DEFAULT"),
        /**
         * DELETE is postgresql keyword
         */
        DELETE("DELETE"),
        /**
         * DESC is postgresql keyword
         */
        DESC("DESC"),
        /**
         * DISTINCT is postgresql keyword
         */
        DISTINCT("DISTINCT"),
        /**
         * DROP is postgresql keyword
         */
        DROP("DROP"),
        /**
         * ELSE is postgresql keyword
         */
        ELSE("ELSE"),
        /**
         * EXCLUSIVE is postgresql keyword
         */
        EXCLUSIVE("EXCLUSIVE"),
        /**
         * EXISTS is postgresql keyword
         */
        EXISTS("EXISTS"),
        /**
         * FILE is postgresql keyword
         */
        FILE("FILE"),
        /**
         * FLOAT is postgresql keyword
         */
        FLOAT("FLOAT"),
        /**
         * FOR is postgresql keyword
         */
        FOR("FOR"),
        /**
         * FROM is postgresql keyword
         */
        FROM("FROM"),
        /**
         * GRANT is postgresql keyword
         */
        GRANT("GRANT"),
        /**
         * GROUP is postgresql keyword
         */
        GROUP("GROUP"),
        /**
         * HAVING is postgresql keyword
         */
        HAVING("HAVING"),
        /**
         * IDENTIFIED is postgresql keyword
         */
        IDENTIFIED("IDENTIFIED"),
        /**
         * IMMEDIATE is postgresql keyword
         */
        IMMEDIATE("IMMEDIATE"),
        /**
         * IN is postgresql keyword
         */
        IN("IN"),
        /**
         * INCREMENT is postgresql keyword
         */
        INCREMENT("INCREMENT"),
        /**
         * INDEX is postgresql keyword
         */
        INDEX("INDEX"),
        /**
         * INITIAL is postgresql keyword
         */
        INITIAL("INITIAL"),
        /**
         * INSERT is postgresql keyword
         */
        INSERT("INSERT"),
        /**
         * INTEGER is postgresql keyword
         */
        INTEGER("INTEGER"),
        /**
         * INTERSECT is postgresql keyword
         */
        INTERSECT("INTERSECT"),
        /**
         * INTO is postgresql keyword
         */
        INTO("INTO"),
        /**
         * IS is postgresql keyword
         */
        IS("IS"),
        /**
         * LEVEL is postgresql keyword
         */
        LEVEL("LEVEL"),
        /**
         * LIKE is postgresql keyword
         */
        LIKE("LIKE"),
        /**
         * LOCK is postgresql keyword
         */
        LOCK("LOCK"),
        /**
         * LONG is postgresql keyword
         */
        LONG("LONG"),
        /**
         * MAXEXTENTS is postgresql keyword
         */
        MAXEXTENTS("MAXEXTENTS"),
        /**
         * MINUS is postgresql keyword
         */
        MINUS("MINUS"),
        /**
         * MLSLABEL is postgresql keyword
         */
        MLSLABEL("MLSLABEL"),
        /**
         * MODE is postgresql keyword
         */
        MODE("MODE"),
        /**
         * MODIFY is postgresql keyword
         */
        MODIFY("MODIFY"),
        /**
         * NESTED_TABLE_ID is postgresql keyword
         */
        NESTED_TABLE_ID("NESTED_TABLE_ID"),
        /**
         * NOAUDIT is postgresql keyword
         */
        NOAUDIT("NOAUDIT"),
        /**
         * NOCOMPRESS is postgresql keyword
         */
        NOCOMPRESS("NOCOMPRESS"),
        /**
         * NOT is postgresql keyword
         */
        NOT("NOT"),
        /**
         * NOWAIT is postgresql keyword
         */
        NOWAIT("NOWAIT"),
        /**
         * NULL is postgresql keyword
         */
        NULL("NULL"),
        /**
         * NUMBER is postgresql keyword
         */
        NUMBER("NUMBER"),
        /**
         * OF is postgresql keyword
         */
        OF("OF"),
        /**
         * OFFLINE is postgresql keyword
         */
        OFFLINE("OFFLINE"),
        /**
         * ON is postgresql keyword
         */
        ON("ON"),
        /**
         * ONLINE is postgresql keyword
         */
        ONLINE("ONLINE"),
        /**
         * OPTION is postgresql keyword
         */
        OPTION("OPTION"),
        /**
         * OR is postgresql keyword
         */
        OR("OR"),
        /**
         * ORDER is postgresql keyword
         */
        ORDER("ORDER"),
        /**
         * PCTFREE is postgresql keyword
         */
        PCTFREE("PCTFREE"),
        /**
         * PRIOR is postgresql keyword
         */
        PRIOR("PRIOR"),
        /**
         * PUBLIC is postgresql keyword
         */
        PUBLIC("PUBLIC"),
        /**
         * RAW is postgresql keyword
         */
        RAW("RAW"),
        /**
         * RENAME is postgresql keyword
         */
        RENAME("RENAME"),
        /**
         * RESOURCE is postgresql keyword
         */
        RESOURCE("RESOURCE"),
        /**
         * REVOKE is postgresql keyword
         */
        REVOKE("REVOKE"),
        /**
         * ROW is postgresql keyword
         */
        ROW("ROW"),
        /**
         * ROWID is postgresql keyword
         */
        ROWID("ROWID"),
        /**
         * ROWNUM is postgresql keyword
         */
        ROWNUM("ROWNUM"),
        /**
         * ROWS is postgresql keyword
         */
        ROWS("ROWS"),
        /**
         * SELECT is postgresql keyword
         */
        SELECT("SELECT"),
        /**
         * SESSION is postgresql keyword
         */
        SESSION("SESSION"),
        /**
         * SET is postgresql keyword
         */
        SET("SET"),
        /**
         * SHARE is postgresql keyword
         */
        SHARE("SHARE"),
        /**
         * SIZE is postgresql keyword
         */
        SIZE("SIZE"),
        /**
         * SMALLINT is postgresql keyword
         */
        SMALLINT("SMALLINT"),
        /**
         * START is postgresql keyword
         */
        START("START"),
        /**
         * SUCCESSFUL is postgresql keyword
         */
        SUCCESSFUL("SUCCESSFUL"),
        /**
         * SYNONYM is postgresql keyword
         */
        SYNONYM("SYNONYM"),
        /**
         * SYSDATE is postgresql keyword
         */
        SYSDATE("SYSDATE"),
        /**
         * TABLE is postgresql keyword
         */
        TABLE("TABLE"),
        /**
         * THEN is postgresql keyword
         */
        THEN("THEN"),
        /**
         * TO is postgresql keyword
         */
        TO("TO"),
        /**
         * TRIGGER is postgresql keyword
         */
        TRIGGER("TRIGGER"),
        /**
         * UID is postgresql keyword
         */
        UID("UID"),
        /**
         * UNION is postgresql keyword
         */
        UNION("UNION"),
        /**
         * UNIQUE is postgresql keyword
         */
        UNIQUE("UNIQUE"),
        /**
         * UPDATE is postgresql keyword
         */
        UPDATE("UPDATE"),
        /**
         * USER is postgresql keyword
         */
        USER("USER"),
        /**
         * VALIDATE is postgresql keyword
         */
        VALIDATE("VALIDATE"),
        /**
         * VALUES is postgresql keyword
         */
        VALUES("VALUES"),
        /**
         * VARCHAR is postgresql keyword
         */
        VARCHAR("VARCHAR"),
        /**
         * VARCHAR2 is postgresql keyword
         */
        VARCHAR2("VARCHAR2"),
        /**
         * VIEW is postgresql keyword
         */
        VIEW("VIEW"),
        /**
         * WHENEVER is postgresql keyword
         */
        WHENEVER("WHENEVER"),
        /**
         * WHERE is postgresql keyword
         */
        WHERE("WHERE"),
        /**
         * WITH is postgresql keyword
         */
        WITH("WITH");
        /**
         * The Name.
         */
        public final String name;

        PostgresqlKeyword(String name) {
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
        return check(fieldOrTableName) ? replace(fieldOrTableName) : fieldOrTableName;
//        return check(fieldOrTableName)?"`" + fieldOrTableName + "`":fieldOrTableName;
    }

    private String replace(String fieldOrTableName) {
        StringBuilder builder = new StringBuilder();
        builder.append("\"").append(fieldOrTableName).append("\"");
        String name = builder.toString();
        return name;
    }
}
