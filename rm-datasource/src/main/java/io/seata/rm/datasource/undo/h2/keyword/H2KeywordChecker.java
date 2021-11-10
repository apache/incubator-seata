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
package io.seata.rm.datasource.undo.h2.keyword;

import io.seata.common.loader.LoadLevel;
import io.seata.rm.datasource.undo.KeywordChecker;
import io.seata.sqlparser.util.JdbcConstants;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The type H2 keyword checker.
 *
 * @author hongyan
 */
@LoadLevel(name = JdbcConstants.H2)
public class H2KeywordChecker implements KeywordChecker {

    private Set<String> keywordSet = Arrays.stream(H2Keyword.values()).map(H2Keyword::name).collect(Collectors.toSet());

    /**
     * H2 keyword
     */
    private enum H2Keyword {
        /**
         * ACCESSIBLE is H2 keyword.
         */
        ACCESSIBLE("ACCESSIBLE"),
        /**
         * ADD is H2 keyword.
         */
        ADD("ADD"),
        /**
         * ALL is H2 keyword.
         */
        ALL("ALL"),
        /**
         * ALTER is H2 keyword.
         */
        ALTER("ALTER"),
        /**
         * ANALYZE is H2 keyword.
         */
        ANALYZE("ANALYZE"),
        /**
         * AND is H2 keyword.
         */
        AND("AND"),
        /**
         * ARRAY is H2 keyword.
         */
        ARRAY("ARRAY"),
        /**
         * AS is H2 keyword.
         */
        AS("AS"),
        /**
         * ASC is H2 keyword.
         */
        ASC("ASC"),
        /**
         * ASENSITIVE is H2 keyword.
         */
        ASENSITIVE("ASENSITIVE"),
        /**
         * BEFORE is H2 keyword.
         */
        BEFORE("BEFORE"),
        /**
         * BETWEEN is H2 keyword.
         */
        BETWEEN("BETWEEN"),
        /**
         * BIGINT is H2 keyword.
         */
        BIGINT("BIGINT"),
        /**
         * BINARY is H2 keyword.
         */
        BINARY("BINARY"),
        /**
         * BLOB is H2 keyword.
         */
        BLOB("BLOB"),
        /**
         * BOTH is H2 keyword.
         */
        BOTH("BOTH"),
        /**
         * BY is H2 keyword.
         */
        BY("BY"),
        /**
         * CALL is H2 keyword.
         */
        CALL("CALL"),
        /**
         * CASCADE is H2 keyword.
         */
        CASCADE("CASCADE"),
        /**
         * CASE is H2 keyword.
         */
        CASE("CASE"),
        /**
         * CHANGE is H2 keyword.
         */
        CHANGE("CHANGE"),
        /**
         * CHAR is H2 keyword.
         */
        CHAR("CHAR"),
        /**
         * CHARACTER is H2 keyword.
         */
        CHARACTER("CHARACTER"),
        /**
         * CHECK is H2 keyword.
         */
        CHECK("CHECK"),
        /**
         * COLLATE is H2 keyword.
         */
        COLLATE("COLLATE"),
        /**
         * COLUMN is H2 keyword.
         */
        COLUMN("COLUMN"),
        /**
         * CONDITION is H2 keyword.
         */
        CONDITION("CONDITION"),
        /**
         * CONSTRAINT is H2 keyword.
         */
        CONSTRAINT("CONSTRAINT"),
        /**
         * CONTINUE is H2 keyword.
         */
        CONTINUE("CONTINUE"),
        /**
         * CONVERT is H2 keyword.
         */
        CONVERT("CONVERT"),
        /**
         * CREATE is H2 keyword.
         */
        CREATE("CREATE"),
        /**
         * CROSS is H2 keyword.
         */
        CROSS("CROSS"),
        /**
         * CUBE is H2 keyword.
         */
        CUBE("CUBE"),
        /**
         * CUME_DIST is H2 keyword.
         */
        CUME_DIST("CUME_DIST"),
        /**
         * CURRENT_DATE is H2 keyword.
         */
        CURRENT_DATE("CURRENT_DATE"),
        /**
         * CURRENT_TIME is H2 keyword.
         */
        CURRENT_TIME("CURRENT_TIME"),
        /**
         * CURRENT_TIMESTAMP is H2 keyword.
         */
        CURRENT_TIMESTAMP("CURRENT_TIMESTAMP"),
        /**
         * CURRENT_USER is H2 keyword.
         */
        CURRENT_USER("CURRENT_USER"),
        /**
         * CURSOR is H2 keyword.
         */
        CURSOR("CURSOR"),
        /**
         * DATABASE is H2 keyword.
         */
        DATABASE("DATABASE"),
        /**
         * DATABASES is H2 keyword.
         */
        DATABASES("DATABASES"),
        /**
         * DAY_HOUR is H2 keyword.
         */
        DAY_HOUR("DAY_HOUR"),
        /**
         * DAY_MICROSECOND is H2 keyword.
         */
        DAY_MICROSECOND("DAY_MICROSECOND"),
        /**
         * DAY_MINUTE is H2 keyword.
         */
        DAY_MINUTE("DAY_MINUTE"),
        /**
         * DAY_SECOND is H2 keyword.
         */
        DAY_SECOND("DAY_SECOND"),
        /**
         * DEC is H2 keyword.
         */
        DEC("DEC"),
        /**
         * DECIMAL is H2 keyword.
         */
        DECIMAL("DECIMAL"),
        /**
         * DECLARE is H2 keyword.
         */
        DECLARE("DECLARE"),
        /**
         * DEFAULT is H2 keyword.
         */
        DEFAULT("DEFAULT"),
        /**
         * DELAYED is H2 keyword.
         */
        DELAYED("DELAYED"),
        /**
         * DELETE is H2 keyword.
         */
        DELETE("DELETE"),
        /**
         * DENSE_RANK is H2 keyword.
         */
        DENSE_RANK("DENSE_RANK"),
        /**
         * DESC is H2 keyword.
         */
        DESC("DESC"),
        /**
         * DESCRIBE is H2 keyword.
         */
        DESCRIBE("DESCRIBE"),
        /**
         * DETERMINISTIC is H2 keyword.
         */
        DETERMINISTIC("DETERMINISTIC"),
        /**
         * DISTINCT is H2 keyword.
         */
        DISTINCT("DISTINCT"),
        /**
         * DISTINCTROW is H2 keyword.
         */
        DISTINCTROW("DISTINCTROW"),
        /**
         * DIV is H2 keyword.
         */
        DIV("DIV"),
        /**
         * DOUBLE is H2 keyword.
         */
        DOUBLE("DOUBLE"),
        /**
         * DROP is H2 keyword.
         */
        DROP("DROP"),
        /**
         * DUAL is H2 keyword.
         */
        DUAL("DUAL"),
        /**
         * EACH is H2 keyword.
         */
        EACH("EACH"),
        /**
         * ELSE is H2 keyword.
         */
        ELSE("ELSE"),
        /**
         * ELSEIF is H2 keyword.
         */
        ELSEIF("ELSEIF"),
        /**
         * EMPTY is H2 keyword.
         */
        EMPTY("EMPTY"),
        /**
         * ENCLOSED is H2 keyword.
         */
        ENCLOSED("ENCLOSED"),
        /**
         * ESCAPED is H2 keyword.
         */
        ESCAPED("ESCAPED"),
        /**
         * EXCEPT is H2 keyword.
         */
        EXCEPT("EXCEPT"),
        /**
         * EXISTS is H2 keyword.
         */
        EXISTS("EXISTS"),
        /**
         * EXIT is H2 keyword.
         */
        EXIT("EXIT"),
        /**
         * EXPLAIN is H2 keyword.
         */
        EXPLAIN("EXPLAIN"),
        /**
         * FALSE is H2 keyword.
         */
        FALSE("FALSE"),
        /**
         * FETCH is H2 keyword.
         */
        FETCH("FETCH"),
        /**
         * FIRST_VALUE is H2 keyword.
         */
        FIRST_VALUE("FIRST_VALUE"),
        /**
         * FLOAT is H2 keyword.
         */
        FLOAT("FLOAT"),
        /**
         * FLOAT4 is H2 keyword.
         */
        FLOAT4("FLOAT4"),
        /**
         * FLOAT8 is H2 keyword.
         */
        FLOAT8("FLOAT8"),
        /**
         * FOR is H2 keyword.
         */
        FOR("FOR"),
        /**
         * FORCE is H2 keyword.
         */
        FORCE("FORCE"),
        /**
         * FOREIGN is H2 keyword.
         */
        FOREIGN("FOREIGN"),
        /**
         * FROM is H2 keyword.
         */
        FROM("FROM"),
        /**
         * FULLTEXT is H2 keyword.
         */
        FULLTEXT("FULLTEXT"),
        /**
         * FUNCTION is H2 keyword.
         */
        FUNCTION("FUNCTION"),
        /**
         * GENERATED is H2 keyword.
         */
        GENERATED("GENERATED"),
        /**
         * GET is H2 keyword.
         */
        GET("GET"),
        /**
         * GRANT is H2 keyword.
         */
        GRANT("GRANT"),
        /**
         * GROUP is H2 keyword.
         */
        GROUP("GROUP"),
        /**
         * GROUPING is H2 keyword.
         */
        GROUPING("GROUPING"),
        /**
         * GROUPS is H2 keyword.
         */
        GROUPS("GROUPS"),
        /**
         * HAVING is H2 keyword.
         */
        HAVING("HAVING"),
        /**
         * HIGH_PRIORITY is H2 keyword.
         */
        HIGH_PRIORITY("HIGH_PRIORITY"),
        /**
         * HOUR_MICROSECOND is H2 keyword.
         */
        HOUR_MICROSECOND("HOUR_MICROSECOND"),
        /**
         * HOUR_MINUTE is H2 keyword.
         */
        HOUR_MINUTE("HOUR_MINUTE"),
        /**
         * HOUR_SECOND is H2 keyword.
         */
        HOUR_SECOND("HOUR_SECOND"),
        /**
         * IF is H2 keyword.
         */
        IF("IF"),
        /**
         * IGNORE is H2 keyword.
         */
        IGNORE("IGNORE"),
        /**
         * IN is H2 keyword.
         */
        IN("IN"),
        /**
         * INDEX is H2 keyword.
         */
        INDEX("INDEX"),
        /**
         * INFILE is H2 keyword.
         */
        INFILE("INFILE"),
        /**
         * INNER is H2 keyword.
         */
        INNER("INNER"),
        /**
         * INOUT is H2 keyword.
         */
        INOUT("INOUT"),
        /**
         * INSENSITIVE is H2 keyword.
         */
        INSENSITIVE("INSENSITIVE"),
        /**
         * INSERT is H2 keyword.
         */
        INSERT("INSERT"),
        /**
         * INT is H2 keyword.
         */
        INT("INT"),
        /**
         * INT1 is H2 keyword.
         */
        INT1("INT1"),
        /**
         * INT2 is H2 keyword.
         */
        INT2("INT2"),
        /**
         * INT3 is H2 keyword.
         */
        INT3("INT3"),
        /**
         * INT4 is H2 keyword.
         */
        INT4("INT4"),
        /**
         * INT8 is H2 keyword.
         */
        INT8("INT8"),
        /**
         * INTEGER is H2 keyword.
         */
        INTEGER("INTEGER"),
        /**
         * INTERVAL is H2 keyword.
         */
        INTERVAL("INTERVAL"),
        /**
         * INTO is H2 keyword.
         */
        INTO("INTO"),
        /**
         * IO_AFTER_GTIDS is H2 keyword.
         */
        IO_AFTER_GTIDS("IO_AFTER_GTIDS"),
        /**
         * IO_BEFORE_GTIDS is H2 keyword.
         */
        IO_BEFORE_GTIDS("IO_BEFORE_GTIDS"),
        /**
         * IS is H2 keyword.
         */
        IS("IS"),
        /**
         * ITERATE is H2 keyword.
         */
        ITERATE("ITERATE"),
        /**
         * JOIN is H2 keyword.
         */
        JOIN("JOIN"),
        /**
         * JSON_TABLE is H2 keyword.
         */
        JSON_TABLE("JSON_TABLE"),
        /**
         * KEY is H2 keyword.
         */
        KEY("KEY"),
        /**
         * KEYS is H2 keyword.
         */
        KEYS("KEYS"),
        /**
         * KILL is H2 keyword.
         */
        KILL("KILL"),
        /**
         * LAG is H2 keyword.
         */
        LAG("LAG"),
        /**
         * LAST_VALUE is H2 keyword.
         */
        LAST_VALUE("LAST_VALUE"),
        /**
         * LATERAL is H2 keyword.
         */
        LATERAL("LATERAL"),
        /**
         * LEAD is H2 keyword.
         */
        LEAD("LEAD"),
        /**
         * LEADING is H2 keyword.
         */
        LEADING("LEADING"),
        /**
         * LEAVE is H2 keyword.
         */
        LEAVE("LEAVE"),
        /**
         * LEFT is H2 keyword.
         */
        LEFT("LEFT"),
        /**
         * LIKE is H2 keyword.
         */
        LIKE("LIKE"),
        /**
         * LIMIT is H2 keyword.
         */
        LIMIT("LIMIT"),
        /**
         * LINEAR is H2 keyword.
         */
        LINEAR("LINEAR"),
        /**
         * LINES is H2 keyword.
         */
        LINES("LINES"),
        /**
         * LOAD is H2 keyword.
         */
        LOAD("LOAD"),
        /**
         * LOCALTIME is H2 keyword.
         */
        LOCALTIME("LOCALTIME"),
        /**
         * LOCALTIMESTAMP is H2 keyword.
         */
        LOCALTIMESTAMP("LOCALTIMESTAMP"),
        /**
         * LOCK is H2 keyword.
         */
        LOCK("LOCK"),
        /**
         * LONG is H2 keyword.
         */
        LONG("LONG"),
        /**
         * LONGBLOB is H2 keyword.
         */
        LONGBLOB("LONGBLOB"),
        /**
         * LONGTEXT is H2 keyword.
         */
        LONGTEXT("LONGTEXT"),
        /**
         * LOOP is H2 keyword.
         */
        LOOP("LOOP"),
        /**
         * LOW_PRIORITY is H2 keyword.
         */
        LOW_PRIORITY("LOW_PRIORITY"),
        /**
         * MASTER_BIND is H2 keyword.
         */
        MASTER_BIND("MASTER_BIND"),
        /**
         * MASTER_SSL_VERIFY_SERVER_CERT is H2 keyword.
         */
        MASTER_SSL_VERIFY_SERVER_CERT("MASTER_SSL_VERIFY_SERVER_CERT"),
        /**
         * MATCH is H2 keyword.
         */
        MATCH("MATCH"),
        /**
         * MAXVALUE is H2 keyword.
         */
        MAXVALUE("MAXVALUE"),
        /**
         * MEDIUMBLOB is H2 keyword.
         */
        MEDIUMBLOB("MEDIUMBLOB"),
        /**
         * MEDIUMINT is H2 keyword.
         */
        MEDIUMINT("MEDIUMINT"),
        /**
         * MEDIUMTEXT is H2 keyword.
         */
        MEDIUMTEXT("MEDIUMTEXT"),
        /**
         * MEMBER is H2 keyword.
         */
        MEMBER("MEMBER"),
        /**
         * MIDDLEINT is H2 keyword.
         */
        MIDDLEINT("MIDDLEINT"),
        /**
         * MINUTE_MICROSECOND is H2 keyword.
         */
        MINUTE_MICROSECOND("MINUTE_MICROSECOND"),
        /**
         * MINUTE_SECOND is H2 keyword.
         */
        MINUTE_SECOND("MINUTE_SECOND"),
        /**
         * MOD is H2 keyword.
         */
        MOD("MOD"),
        /**
         * MODIFIES is H2 keyword.
         */
        MODIFIES("MODIFIES"),
        /**
         * NATURAL is H2 keyword.
         */
        NATURAL("NATURAL"),
        /**
         * NOT is H2 keyword.
         */
        NOT("NOT"),
        /**
         * NO_WRITE_TO_BINLOG is H2 keyword.
         */
        NO_WRITE_TO_BINLOG("NO_WRITE_TO_BINLOG"),
        /**
         * NTH_VALUE is H2 keyword.
         */
        NTH_VALUE("NTH_VALUE"),
        /**
         * NTILE is H2 keyword.
         */
        NTILE("NTILE"),
        /**
         * NULL is H2 keyword.
         */
        NULL("NULL"),
        /**
         * NUMERIC is H2 keyword.
         */
        NUMERIC("NUMERIC"),
        /**
         * OF is H2 keyword.
         */
        OF("OF"),
        /**
         * ON is H2 keyword.
         */
        ON("ON"),
        /**
         * OPTIMIZE is H2 keyword.
         */
        OPTIMIZE("OPTIMIZE"),
        /**
         * OPTIMIZER_COSTS is H2 keyword.
         */
        OPTIMIZER_COSTS("OPTIMIZER_COSTS"),
        /**
         * OPTION is H2 keyword.
         */
        OPTION("OPTION"),
        /**
         * OPTIONALLY is H2 keyword.
         */
        OPTIONALLY("OPTIONALLY"),
        /**
         * OR is H2 keyword.
         */
        OR("OR"),
        /**
         * ORDER is H2 keyword.
         */
        ORDER("ORDER"),
        /**
         * OUT is H2 keyword.
         */
        OUT("OUT"),
        /**
         * OUTER is H2 keyword.
         */
        OUTER("OUTER"),
        /**
         * OUTFILE is H2 keyword.
         */
        OUTFILE("OUTFILE"),
        /**
         * OVER is H2 keyword.
         */
        OVER("OVER"),
        /**
         * PARTITION is H2 keyword.
         */
        PARTITION("PARTITION"),
        /**
         * PERCENT_RANK is H2 keyword.
         */
        PERCENT_RANK("PERCENT_RANK"),
        /**
         * PRECISION is H2 keyword.
         */
        PRECISION("PRECISION"),
        /**
         * PRIMARY is H2 keyword.
         */
        PRIMARY("PRIMARY"),
        /**
         * PROCEDURE is H2 keyword.
         */
        PROCEDURE("PROCEDURE"),
        /**
         * PURGE is H2 keyword.
         */
        PURGE("PURGE"),
        /**
         * RANGE is H2 keyword.
         */
        RANGE("RANGE"),
        /**
         * RANK is H2 keyword.
         */
        RANK("RANK"),
        /**
         * READ is H2 keyword.
         */
        READ("READ"),
        /**
         * READS is H2 keyword.
         */
        READS("READS"),
        /**
         * READ_WRITE is H2 keyword.
         */
        READ_WRITE("READ_WRITE"),
        /**
         * REAL is H2 keyword.
         */
        REAL("REAL"),
        /**
         * RECURSIVE is H2 keyword.
         */
        RECURSIVE("RECURSIVE"),
        /**
         * REFERENCES is H2 keyword.
         */
        REFERENCES("REFERENCES"),
        /**
         * REGEXP is H2 keyword.
         */
        REGEXP("REGEXP"),
        /**
         * RELEASE is H2 keyword.
         */
        RELEASE("RELEASE"),
        /**
         * RENAME is H2 keyword.
         */
        RENAME("RENAME"),
        /**
         * REPEAT is H2 keyword.
         */
        REPEAT("REPEAT"),
        /**
         * REPLACE is H2 keyword.
         */
        REPLACE("REPLACE"),
        /**
         * REQUIRE is H2 keyword.
         */
        REQUIRE("REQUIRE"),
        /**
         * RESIGNAL is H2 keyword.
         */
        RESIGNAL("RESIGNAL"),
        /**
         * RESTRICT is H2 keyword.
         */
        RESTRICT("RESTRICT"),
        /**
         * RETURN is H2 keyword.
         */
        RETURN("RETURN"),
        /**
         * REVOKE is H2 keyword.
         */
        REVOKE("REVOKE"),
        /**
         * RIGHT is H2 keyword.
         */
        RIGHT("RIGHT"),
        /**
         * RLIKE is H2 keyword.
         */
        RLIKE("RLIKE"),
        /**
         * ROW is H2 keyword.
         */
        ROW("ROW"),
        /**
         * ROWS is H2 keyword.
         */
        ROWS("ROWS"),
        /**
         * ROW_NUMBER is H2 keyword.
         */
        ROW_NUMBER("ROW_NUMBER"),
        /**
         * SCHEMA is H2 keyword.
         */
        SCHEMA("SCHEMA"),
        /**
         * SCHEMAS is H2 keyword.
         */
        SCHEMAS("SCHEMAS"),
        /**
         * SECOND_MICROSECOND is H2 keyword.
         */
        SECOND_MICROSECOND("SECOND_MICROSECOND"),
        /**
         * SELECT is H2 keyword.
         */
        SELECT("SELECT"),
        /**
         * SENSITIVE is H2 keyword.
         */
        SENSITIVE("SENSITIVE"),
        /**
         * SEPARATOR is H2 keyword.
         */
        SEPARATOR("SEPARATOR"),
        /**
         * SET is H2 keyword.
         */
        SET("SET"),
        /**
         * SHOW is H2 keyword.
         */
        SHOW("SHOW"),
        /**
         * SIGNAL is H2 keyword.
         */
        SIGNAL("SIGNAL"),
        /**
         * SMALLINT is H2 keyword.
         */
        SMALLINT("SMALLINT"),
        /**
         * SPATIAL is H2 keyword.
         */
        SPATIAL("SPATIAL"),
        /**
         * SPECIFIC is H2 keyword.
         */
        SPECIFIC("SPECIFIC"),
        /**
         * SQL is H2 keyword.
         */
        SQL("SQL"),
        /**
         * SQLEXCEPTION is H2 keyword.
         */
        SQLEXCEPTION("SQLEXCEPTION"),
        /**
         * SQLSTATE is H2 keyword.
         */
        SQLSTATE("SQLSTATE"),
        /**
         * SQLWARNING is H2 keyword.
         */
        SQLWARNING("SQLWARNING"),
        /**
         * SQL_BIG_RESULT is H2 keyword.
         */
        SQL_BIG_RESULT("SQL_BIG_RESULT"),
        /**
         * SQL_CALC_FOUND_ROWS is H2 keyword.
         */
        SQL_CALC_FOUND_ROWS("SQL_CALC_FOUND_ROWS"),
        /**
         * SQL_SMALL_RESULT is H2 keyword.
         */
        SQL_SMALL_RESULT("SQL_SMALL_RESULT"),
        /**
         * SSL is H2 keyword.
         */
        SSL("SSL"),
        /**
         * STARTING is H2 keyword.
         */
        STARTING("STARTING"),
        /**
         * STORED is H2 keyword.
         */
        STORED("STORED"),
        /**
         * STRAIGHT_JOIN is H2 keyword.
         */
        STRAIGHT_JOIN("STRAIGHT_JOIN"),
        /**
         * SYSTEM is H2 keyword.
         */
        SYSTEM("SYSTEM"),
        /**
         * TABLE is H2 keyword.
         */
        TABLE("TABLE"),
        /**
         * TERMINATED is H2 keyword.
         */
        TERMINATED("TERMINATED"),
        /**
         * THEN is H2 keyword.
         */
        THEN("THEN"),
        /**
         * TINYBLOB is H2 keyword.
         */
        TINYBLOB("TINYBLOB"),
        /**
         * TINYINT is H2 keyword.
         */
        TINYINT("TINYINT"),
        /**
         * TINYTEXT is H2 keyword.
         */
        TINYTEXT("TINYTEXT"),
        /**
         * TO is H2 keyword.
         */
        TO("TO"),
        /**
         * TRAILING is H2 keyword.
         */
        TRAILING("TRAILING"),
        /**
         * TRIGGER is H2 keyword.
         */
        TRIGGER("TRIGGER"),
        /**
         * TRUE is H2 keyword.
         */
        TRUE("TRUE"),
        /**
         * UNDO is H2 keyword.
         */
        UNDO("UNDO"),
        /**
         * UNION is H2 keyword.
         */
        UNION("UNION"),
        /**
         * UNIQUE is H2 keyword.
         */
        UNIQUE("UNIQUE"),
        /**
         * UNLOCK is H2 keyword.
         */
        UNLOCK("UNLOCK"),
        /**
         * UNSIGNED is H2 keyword.
         */
        UNSIGNED("UNSIGNED"),
        /**
         * UPDATE is H2 keyword.
         */
        UPDATE("UPDATE"),
        /**
         * USAGE is H2 keyword.
         */
        USAGE("USAGE"),
        /**
         * USE is H2 keyword.
         */
        USE("USE"),
        /**
         * USING is H2 keyword.
         */
        USING("USING"),
        /**
         * UTC_DATE is H2 keyword.
         */
        UTC_DATE("UTC_DATE"),
        /**
         * UTC_TIME is H2 keyword.
         */
        UTC_TIME("UTC_TIME"),
        /**
         * UTC_TIMESTAMP is H2 keyword.
         */
        UTC_TIMESTAMP("UTC_TIMESTAMP"),
        /**
         * VALUES is H2 keyword.
         */
        VALUES("VALUES"),
        /**
         * VARBINARY is H2 keyword.
         */
        VARBINARY("VARBINARY"),
        /**
         * VARCHAR is H2 keyword.
         */
        VARCHAR("VARCHAR"),
        /**
         * VARCHARACTER is H2 keyword.
         */
        VARCHARACTER("VARCHARACTER"),
        /**
         * VARYING is H2 keyword.
         */
        VARYING("VARYING"),
        /**
         * VIRTUAL is H2 keyword.
         */
        VIRTUAL("VIRTUAL"),
        /**
         * WHEN is H2 keyword.
         */
        WHEN("WHEN"),
        /**
         * WHERE is H2 keyword.
         */
        WHERE("WHERE"),
        /**
         * WHILE is H2 keyword.
         */
        WHILE("WHILE"),
        /**
         * WINDOW is H2 keyword.
         */
        WINDOW("WINDOW"),
        /**
         * WITH is H2 keyword.
         */
        WITH("WITH"),
        /**
         * WRITE is H2 keyword.
         */
        WRITE("WRITE"),
        /**
         * XOR is H2 keyword.
         */
        XOR("XOR"),
        /**
         * YEAR_MONTH is H2 keyword.
         */
        YEAR_MONTH("YEAR_MONTH"),
        /**
         * ZEROFILL is H2 keyword.
         */
        ZEROFILL("ZEROFILL");
        /**
         * The Name.
         */
        public final String name;

        H2Keyword(String name) {
            this.name = name;
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

}
