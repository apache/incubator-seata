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
package io.seata.rm.datasource.undo.sqlserver.keyword;

import io.seata.common.loader.LoadLevel;
import io.seata.rm.datasource.undo.KeywordChecker;
import io.seata.sqlparser.util.JdbcConstants;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The type SQLServer keyword checker.
 *
 * @author xingfudeshi@gmail.com
 */
@LoadLevel(name = JdbcConstants.SQLSERVER)
public class SQLServerKeywordChecker implements KeywordChecker {

    private Set<String> keywordSet = Arrays.stream(SQLServerKeywordChecker.SQLServerKeyword.values()).map(SQLServerKeyword::name).collect(Collectors.toSet());

    /**
     * sqlserver keyword
     */
    private enum SQLServerKeyword {
        /**
         * ADD is sqlserver keyword.
         */
        ADD("ADD"),
        /**
         * ALL is sqlserver keyword.
         */
        ALL("ALL"),
        /**
         * ALTER is sqlserver keyword.
         */
        ALTER("ALTER"),
        /**
         * AND is sqlserver keyword.
         */
        AND("AND"),
        /**
         * ANY is sqlserver keyword.
         */
        ANY("ANY"),
        /**
         * AS is sqlserver keyword.
         */
        AS("AS"),
        /**
         * ASC is sqlserver keyword.
         */
        ASC("ASC"),
        /**
         * AUTHORIZATION is sqlserver keyword.
         */
        AUTHORIZATION("AUTHORIZATION"),
        /**
         * BACKUP is sqlserver keyword.
         */
        BACKUP("BACKUP"),
        /**
         * BEGIN is sqlserver keyword.
         */
        BEGIN("BEGIN"),
        /**
         * BETWEEN is sqlserver keyword.
         */
        BETWEEN("BETWEEN"),
        /**
         * BREAK is sqlserver keyword.
         */
        BREAK("BREAK"),
        /**
         * BROWSE is sqlserver keyword.
         */
        BROWSE("BROWSE"),
        /**
         * BULK is sqlserver keyword.
         */
        BULK("BULK"),
        /**
         * BY is sqlserver keyword.
         */
        BY("BY"),
        /**
         * CASCADE is sqlserver keyword.
         */
        CASCADE("CASCADE"),
        /**
         * CASE is sqlserver keyword.
         */
        CASE("CASE"),
        /**
         * CHECK is sqlserver keyword.
         */
        CHECK("CHECK"),
        /**
         * CHECKPOINT is sqlserver keyword.
         */
        CHECKPOINT("CHECKPOINT"),
        /**
         * CLOSE is sqlserver keyword.
         */
        CLOSE("CLOSE"),
        /**
         * CLUSTERED is sqlserver keyword.
         */
        CLUSTERED("CLUSTERED"),
        /**
         * COALESCE is sqlserver keyword.
         */
        COALESCE("COALESCE"),
        /**
         * COLLATE is sqlserver keyword.
         */
        COLLATE("COLLATE"),
        /**
         * COLUMN is sqlserver keyword.
         */
        COLUMN("COLUMN"),
        /**
         * COMMIT is sqlserver keyword.
         */
        COMMIT("COMMIT"),
        /**
         * COMPUTE is sqlserver keyword.
         */
        COMPUTE("COMPUTE"),
        /**
         * CONSTRAINT is sqlserver keyword.
         */
        CONSTRAINT("CONSTRAINT"),
        /**
         * CONTAINS is sqlserver keyword.
         */
        CONTAINS("CONTAINS"),
        /**
         * CONTAINSTABLE is sqlserver keyword.
         */
        CONTAINSTABLE("CONTAINSTABLE"),
        /**
         * CONTINUE is sqlserver keyword.
         */
        CONTINUE("CONTINUE"),
        /**
         * CONVERT is sqlserver keyword.
         */
        CONVERT("CONVERT"),
        /**
         * CREATE is sqlserver keyword.
         */
        CREATE("CREATE"),
        /**
         * CROSS is sqlserver keyword.
         */
        CROSS("CROSS"),
        /**
         * CURRENT is sqlserver keyword.
         */
        CURRENT("CURRENT"),
        /**
         * CURRENT_DATE is sqlserver keyword.
         */
        CURRENT_DATE("CURRENT_DATE"),
        /**
         * CURRENT_TIME is sqlserver keyword.
         */
        CURRENT_TIME("CURRENT_TIME"),
        /**
         * CURRENT_TIMESTAMP is sqlserver keyword.
         */
        CURRENT_TIMESTAMP("CURRENT_TIMESTAMP"),
        /**
         * CURRENT_USER is sqlserver keyword.
         */
        CURRENT_USER("CURRENT_USER"),
        /**
         * CURSOR is sqlserver keyword.
         */
        CURSOR("CURSOR"),
        /**
         * DATABASE is sqlserver keyword.
         */
        DATABASE("DATABASE"),
        /**
         * DBCC is sqlserver keyword.
         */
        DBCC("DBCC"),
        /**
         * DEALLOCATE is sqlserver keyword.
         */
        DEALLOCATE("DEALLOCATE"),
        /**
         * DECLARE is sqlserver keyword.
         */
        DECLARE("DECLARE"),
        /**
         * DEFAULT is sqlserver keyword.
         */
        DEFAULT("DEFAULT"),
        /**
         * DELETE is sqlserver keyword.
         */
        DELETE("DELETE"),
        /**
         * DENY is sqlserver keyword.
         */
        DENY("DENY"),
        /**
         * DESC is sqlserver keyword.
         */
        DESC("DESC"),
        /**
         * DISK is sqlserver keyword.
         */
        DISK("DISK"),
        /**
         * DISTINCT is sqlserver keyword.
         */
        DISTINCT("DISTINCT"),
        /**
         * DISTRIBUTED is sqlserver keyword.
         */
        DISTRIBUTED("DISTRIBUTED"),
        /**
         * DOUBLE is sqlserver keyword.
         */
        DOUBLE("DOUBLE"),
        /**
         * DROP is sqlserver keyword.
         */
        DROP("DROP"),
        /**
         * DUMMY is sqlserver keyword.
         */
        DUMMY("DUMMY"),
        /**
         * DUMP is sqlserver keyword.
         */
        DUMP("DUMP"),
        /**
         * ELSE is sqlserver keyword.
         */
        ELSE("ELSE"),
        /**
         * END is sqlserver keyword.
         */
        END("END"),
        /**
         * ERRLVL is sqlserver keyword.
         */
        ERRLVL("ERRLVL"),
        /**
         * ESCAPED is sqlserver keyword.
         */
        ESCAPED("ESCAPED"),
        /**
         * EXCEPT is sqlserver keyword.
         */
        EXCEPT("EXCEPT"),
        /**
         * EXEC is sqlserver keyword.
         */
        EXEC("EXEC"),
        /**
         * EXECUTE is sqlserver keyword.
         */
        EXECUTE("EXECUTE"),
        /**
         * EXISTS is sqlserver keyword.
         */
        EXISTS("EXISTS"),
        /**
         * EXIT is sqlserver keyword.
         */
        EXIT("EXIT"),
        /**
         * FETCH is sqlserver keyword.
         */
        FETCH("FETCH"),
        /**
         * FILE is sqlserver keyword.
         */
        FILE("FILE"),
        /**
         * FILLFACTOR is sqlserver keyword.
         */
        FILLFACTOR("FILLFACTOR"),
        /**
         * FOR is sqlserver keyword.
         */
        FOR("FOR"),
        /**
         * FOREIGN is sqlserver keyword.
         */
        FOREIGN("FOREIGN"),
        /**
         * FREETEXT is sqlserver keyword.
         */
        FREETEXT("FREETEXT"),
        /**
         * FREETEXTTABLE is sqlserver keyword.
         */
        FREETEXTTABLE("FREETEXTTABLE"),
        /**
         * FROM is sqlserver keyword.
         */
        FROM("FROM"),
        /**
         * FULL is sqlserver keyword.
         */
        FULL("FULL"),
        /**
         * FUNCTION is sqlserver keyword.
         */
        FUNCTION("FUNCTION"),
        /**
         * GOTO is sqlserver keyword.
         */
        GOTO("GOTO"),
        /**
         * GRANT is sqlserver keyword.
         */
        GRANT("GRANT"),
        /**
         * GROUP is sqlserver keyword.
         */
        GROUP("GROUP"),
        /**
         * HAVING is sqlserver keyword.
         */
        HAVING("HAVING"),
        /**
         * HOLDLOCK is sqlserver keyword.
         */
        HOLDLOCK("HOLDLOCK"),
        /**
         * IDENTITY is sqlserver keyword.
         */
        IDENTITY("IDENTITY"),
        /**
         * IDENTITY_INSERT is sqlserver keyword.
         */
        IDENTITY_INSERT("IDENTITY_INSERT"),
        /**
         * IDENTITYCOL is sqlserver keyword.
         */
        IDENTITYCOL("IDENTITYCOL"),
        /**
         * IF is sqlserver keyword.
         */
        IF("IF"),
        /**
         * IN is sqlserver keyword.
         */
        IN("IN"),
        /**
         * INDEX is sqlserver keyword.
         */
        INDEX("INDEX"),
        /**
         * INNER is sqlserver keyword.
         */
        INNER("INNER"),
        /**
         * INSERT is sqlserver keyword.
         */
        INSERT("INSERT"),
        /**
         * INTERSECT is sqlserver keyword.
         */
        INTERSECT("INTERSECT"),
        /**
         * INTO is sqlserver keyword.
         */
        INTO("INTO"),
        /**
         * IS is sqlserver keyword.
         */
        IS("IS"),
        /**
         * JOIN is sqlserver keyword.
         */
        JOIN("JOIN"),
        /**
         * KEY is sqlserver keyword.
         */
        KEY("KEY"),
        /**
         * KILL is sqlserver keyword.
         */
        KILL("KILL"),
        /**
         * LEFT is sqlserver keyword.
         */
        LEFT("LEFT"),
        /**
         * LIKE is sqlserver keyword.
         */
        LIKE("LIKE"),
        /**
         * LIKENO is sqlserver keyword.
         */
        LIKENO("LIKENO"),
        /**
         * LOAD is sqlserver keyword.
         */
        LOAD("LOAD"),
        /**
         * NATIONAL is sqlserver keyword.
         */
        NATIONAL("NATIONAL"),
        /**
         * NOCHECK is sqlserver keyword.
         */
        NOCHECK("NOCHECK"),
        /**
         * NOCHECK is sqlserver keyword.
         */
        NONCLUSTERED("NONCLUSTERED"),
        /**
         * NOCHECK is sqlserver keyword.
         */
        NOT("NOT"),
        /**
         * NOCHECK is sqlserver keyword.
         */
        NULL("NULL"),
        /**
         * NULLIF is sqlserver keyword.
         */
        NULLIF("NULLIF"),
        /**
         * OF is sqlserver keyword.
         */
        OF("OF"),
        /**
         * OFF is sqlserver keyword.
         */
        OFF("OFF"),
        /**
         * OFFSETS is sqlserver keyword.
         */
        OFFSETS("OFFSETS"),
        /**
         * ON is sqlserver keyword.
         */
        ON("ON"),
        /**
         * OPEN is sqlserver keyword.
         */
        OPEN("OPEN"),
        /**
         * OPENDATASOURCE is sqlserver keyword.
         */
        OPENDATASOURCE("OPENDATASOURCE"),
        /**
         * OPENQUERY is sqlserver keyword.
         */
        OPENQUERY("OPENQUERY"),
        /**
         * OPENROWSET is sqlserver keyword.
         */
        OPENROWSET("OPENROWSET"),
        /**
         * OPENXML is sqlserver keyword.
         */
        OPENXML("OPENXML"),
        /**
         * OPTION is sqlserver keyword.
         */
        OPTION("OPTION"),
        /**
         * OR is sqlserver keyword.
         */
        OR("OR"),
        /**
         * ORDER is sqlserver keyword.
         */
        ORDER("ORDER"),
        /**
         * OUTER is sqlserver keyword.
         */
        OUTER("OUTER"),
        /**
         * OVER is sqlserver keyword.
         */
        OVER("OVER"),
        /**
         * PERCENT is sqlserver keyword.
         */
        PERCENT("PERCENT"),
        /**
         * PLAN is sqlserver keyword.
         */
        PLAN("PLAN"),
        /**
         * PRECISION is sqlserver keyword.
         */
        PRECISION("PRECISION"),
        /**
         * PRIMARY is sqlserver keyword.
         */
        PRIMARY("PRIMARY"),
        /**
         * PRINT is sqlserver keyword.
         */
        PRINT("PRINT"),
        /**
         * PROC is sqlserver keyword.
         */
        PROC("PROC"),
        /**
         * PROCEDURE is sqlserver keyword.
         */
        PROCEDURE("PROCEDURE"),
        /**
         * PUBLIC is sqlserver keyword.
         */
        PUBLIC("PUBLIC"),
        /**
         * RAISERROR is sqlserver keyword.
         */
        RAISERROR("RAISERROR"),
        /**
         * READ is sqlserver keyword.
         */
        READ("READ"),
        /**
         * READTEXT is sqlserver keyword.
         */
        READTEXT("READTEXT"),
        /**
         * RECONFIGURE is sqlserver keyword.
         */
        RECONFIGURE("RECONFIGURE"),
        /**
         * REFERENCES is sqlserver keyword.
         */
        REFERENCES("REFERENCES"),
        /**
         * REPLICATION is sqlserver keyword.
         */
        REPLICATION("REPLICATION"),
        /**
         * RESTORE is sqlserver keyword.
         */
        RESTORE("RESTORE"),
        /**
         * RESTRICT is sqlserver keyword.
         */
        RESTRICT("RESTRICT"),
        /**
         * RETURN is sqlserver keyword.
         */
        RETURN("RETURN"),
        /**
         * REVOKE is sqlserver keyword.
         */
        REVOKE("REVOKE"),
        /**
         * RIGHT is sqlserver keyword.
         */
        RIGHT("RIGHT"),
        /**
         * ROLLBACK is sqlserver keyword.
         */
        ROLLBACK("ROLLBACK"),
        /**
         * ROWCOUNT is sqlserver keyword.
         */
        ROWCOUNT("ROWCOUNT"),
        /**
         * ROWGUIDCOL is sqlserver keyword.
         */
        ROWGUIDCOL("ROWGUIDCOL"),
        /**
         * RULE is sqlserver keyword.
         */
        RULE("RULE"),
        /**
         * SAVE is sqlserver keyword.
         */
        SAVE("SAVE"),
        /**
         * SCHEMA is sqlserver keyword.
         */
        SCHEMA("SCHEMA"),
        /**
         * SELECT is sqlserver keyword.
         */
        SELECT("SELECT"),
        /**
         * SESSION_USER is sqlserver keyword.
         */
        SESSION_USER("SESSION_USER"),
        /**
         * SET is sqlserver keyword.
         */
        SET("SET"),
        /**
         * SETUSER is sqlserver keyword.
         */
        SETUSER("SETUSER"),
        /**
         * SHUTDOWN is sqlserver keyword.
         */
        SHUTDOWN("SHUTDOWN"),
        /**
         * SOME is sqlserver keyword.
         */
        SOME("SOME"),
        /**
         * STATISTICS is sqlserver keyword.
         */
        STATISTICS("STATISTICS"),
        /**
         * SYSTEM_USER is sqlserver keyword.
         */
        SYSTEM_USER("SYSTEM_USER"),
        /**
         * TABLE is sqlserver keyword.
         */
        TABLE("TABLE"),
        /**
         * TEXTSIZE is sqlserver keyword.
         */
        TEXTSIZE("TEXTSIZE"),
        /**
         * THEN is sqlserver keyword.
         */
        THEN("THEN"),
        /**
         * TO is sqlserver keyword.
         */
        TO("TO"),
        /**
         * TOP is sqlserver keyword.
         */
        TOP("TOP"),
        /**
         * TRAN is sqlserver keyword.
         */
        TRAN("TRAN"),
        /**
         * TRANSACTION is sqlserver keyword.
         */
        TRANSACTION("TRANSACTION"),
        /**
         * TRIGGER is sqlserver keyword.
         */
        TRIGGER("TRIGGER"),
        /**
         * TRUNCATE is sqlserver keyword.
         */
        TRUNCATE("TRUNCATE"),
        /**
         * TSEQUAL is sqlserver keyword.
         */
        TSEQUAL("TSEQUAL"),
        /**
         * UNION is sqlserver keyword.
         */
        UNION("UNION"),
        /**
         * UNIQUE is sqlserver keyword.
         */
        UNIQUE("UNIQUE"),
        /**
         * UPDATE is sqlserver keyword.
         */
        UPDATE("UPDATE"),
        /**
         * UPDATETEXT is sqlserver keyword.
         */
        UPDATETEXT("UPDATETEXT"),
        /**
         * USE is sqlserver keyword.
         */
        USE("USE"),
        /**
         * USER is sqlserver keyword.
         */
        USER("USER"),
        /**
         * VALUES is sqlserver keyword.
         */
        VALUES("VALUES"),
        /**
         * VARYING is sqlserver keyword.
         */
        VARYING("VARYING"),
        /**
         * VIEW is sqlserver keyword.
         */
        VIEW("VIEW"),
        /**
         * WAITFOR is sqlserver keyword.
         */
        WAITFOR("WAITFOR"),
        /**
         * WHEN is sqlserver keyword.
         */
        WHEN("WHEN"),
        /**
         * WHERE is sqlserver keyword.
         */
        WHERE("WHERE"),
        /**
         * WHILE is sqlserver keyword.
         */
        WHILE("WHILE"),
        /**
         * WITH is sqlserver keyword.
         */
        WITH("WITH"),
        /**
         * WRITETEXT is sqlserver keyword.
         */
        WRITETEXT("WRITETEXT");
        /**
         * The Name.
         */
        public final String keywordName;

        SQLServerKeyword(String keywordName) {
            this.keywordName = keywordName;
        }

        public final String getKeywordName() {
            return keywordName;
        }
    }


    @Override
    public boolean check(String fieldOrTableName) {
        if (keywordSet.contains(fieldOrTableName)) {
            return true;
        }
        if (fieldOrTableName != null) {
            fieldOrTableName = fieldOrTableName.toUpperCase();
        }
        return keywordSet.contains(fieldOrTableName);

    }

    @Override
    public boolean checkEscape(String fieldOrTableName) {
        return check(fieldOrTableName);
    }

    public String checkAndReplace(String fieldOrTableName) {
        return check(fieldOrTableName) ? "`" + fieldOrTableName + "`" : fieldOrTableName;
    }

}
