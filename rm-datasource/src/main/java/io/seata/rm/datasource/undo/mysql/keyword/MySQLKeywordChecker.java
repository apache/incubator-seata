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
package io.seata.rm.datasource.undo.mysql.keyword;

import io.seata.rm.datasource.undo.KeywordChecker;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The type MySQL keyword checker.
 *
 * @author xingfudeshi@gmail.com
 * @date 2019/3/5 MySQL keyword checker
 */
public class MySQLKeywordChecker implements KeywordChecker {
    private static volatile KeywordChecker keywordChecker = null;
    private Set<String> keywordSet;

    private MySQLKeywordChecker() {
        keywordSet = Arrays.stream(MySQLKeyword.values()).map(MySQLKeyword::name).collect(Collectors.toSet());
    }

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
         * ACCESSIBLE is mysql keyword.
         */
        ACCESSIBLE("ACCESSIBLE"),
        /**
         * ADD is mysql keyword.
         */
        ADD("ADD"),
        /**
         * ALL is mysql keyword.
         */
        ALL("ALL"),
        /**
         * ALTER is mysql keyword.
         */
        ALTER("ALTER"),
        /**
         * ANALYZE is mysql keyword.
         */
        ANALYZE("ANALYZE"),
        /**
         * AND is mysql keyword.
         */
        AND("AND"),
        /**
         * ARRAY is mysql keyword.
         */
        ARRAY("ARRAY"),
        /**
         * AS is mysql keyword.
         */
        AS("AS"),
        /**
         * ASC is mysql keyword.
         */
        ASC("ASC"),
        /**
         * ASENSITIVE is mysql keyword.
         */
        ASENSITIVE("ASENSITIVE"),
        /**
         * BEFORE is mysql keyword.
         */
        BEFORE("BEFORE"),
        /**
         * BETWEEN is mysql keyword.
         */
        BETWEEN("BETWEEN"),
        /**
         * BIGINT is mysql keyword.
         */
        BIGINT("BIGINT"),
        /**
         * BINARY is mysql keyword.
         */
        BINARY("BINARY"),
        /**
         * BLOB is mysql keyword.
         */
        BLOB("BLOB"),
        /**
         * BOTH is mysql keyword.
         */
        BOTH("BOTH"),
        /**
         * BY is mysql keyword.
         */
        BY("BY"),
        /**
         * CALL is mysql keyword.
         */
        CALL("CALL"),
        /**
         * CASCADE is mysql keyword.
         */
        CASCADE("CASCADE"),
        /**
         * CASE is mysql keyword.
         */
        CASE("CASE"),
        /**
         * CHANGE is mysql keyword.
         */
        CHANGE("CHANGE"),
        /**
         * CHAR is mysql keyword.
         */
        CHAR("CHAR"),
        /**
         * CHARACTER is mysql keyword.
         */
        CHARACTER("CHARACTER"),
        /**
         * CHECK is mysql keyword.
         */
        CHECK("CHECK"),
        /**
         * COLLATE is mysql keyword.
         */
        COLLATE("COLLATE"),
        /**
         * COLUMN is mysql keyword.
         */
        COLUMN("COLUMN"),
        /**
         * CONDITION is mysql keyword.
         */
        CONDITION("CONDITION"),
        /**
         * CONSTRAINT is mysql keyword.
         */
        CONSTRAINT("CONSTRAINT"),
        /**
         * CONTINUE is mysql keyword.
         */
        CONTINUE("CONTINUE"),
        /**
         * CONVERT is mysql keyword.
         */
        CONVERT("CONVERT"),
        /**
         * CREATE is mysql keyword.
         */
        CREATE("CREATE"),
        /**
         * CROSS is mysql keyword.
         */
        CROSS("CROSS"),
        /**
         * CUBE is mysql keyword.
         */
        CUBE("CUBE"),
        /**
         * CUME_DIST is mysql keyword.
         */
        CUME_DIST("CUME_DIST"),
        /**
         * CURRENT_DATE is mysql keyword.
         */
        CURRENT_DATE("CURRENT_DATE"),
        /**
         * CURRENT_TIME is mysql keyword.
         */
        CURRENT_TIME("CURRENT_TIME"),
        /**
         * CURRENT_TIMESTAMP is mysql keyword.
         */
        CURRENT_TIMESTAMP("CURRENT_TIMESTAMP"),
        /**
         * CURRENT_USER is mysql keyword.
         */
        CURRENT_USER("CURRENT_USER"),
        /**
         * CURSOR is mysql keyword.
         */
        CURSOR("CURSOR"),
        /**
         * DATABASE is mysql keyword.
         */
        DATABASE("DATABASE"),
        /**
         * DATABASES is mysql keyword.
         */
        DATABASES("DATABASES"),
        /**
         * DAY_HOUR is mysql keyword.
         */
        DAY_HOUR("DAY_HOUR"),
        /**
         * DAY_MICROSECOND is mysql keyword.
         */
        DAY_MICROSECOND("DAY_MICROSECOND"),
        /**
         * DAY_MINUTE is mysql keyword.
         */
        DAY_MINUTE("DAY_MINUTE"),
        /**
         * DAY_SECOND is mysql keyword.
         */
        DAY_SECOND("DAY_SECOND"),
        /**
         * DEC is mysql keyword.
         */
        DEC("DEC"),
        /**
         * DECIMAL is mysql keyword.
         */
        DECIMAL("DECIMAL"),
        /**
         * DECLARE is mysql keyword.
         */
        DECLARE("DECLARE"),
        /**
         * DEFAULT is mysql keyword.
         */
        DEFAULT("DEFAULT"),
        /**
         * DELAYED is mysql keyword.
         */
        DELAYED("DELAYED"),
        /**
         * DELETE is mysql keyword.
         */
        DELETE("DELETE"),
        /**
         * DENSE_RANK is mysql keyword.
         */
        DENSE_RANK("DENSE_RANK"),
        /**
         * DESC is mysql keyword.
         */
        DESC("DESC"),
        /**
         * DESCRIBE is mysql keyword.
         */
        DESCRIBE("DESCRIBE"),
        /**
         * DETERMINISTIC is mysql keyword.
         */
        DETERMINISTIC("DETERMINISTIC"),
        /**
         * DISTINCT is mysql keyword.
         */
        DISTINCT("DISTINCT"),
        /**
         * DISTINCTROW is mysql keyword.
         */
        DISTINCTROW("DISTINCTROW"),
        /**
         * DIV is mysql keyword.
         */
        DIV("DIV"),
        /**
         * DOUBLE is mysql keyword.
         */
        DOUBLE("DOUBLE"),
        /**
         * DROP is mysql keyword.
         */
        DROP("DROP"),
        /**
         * DUAL is mysql keyword.
         */
        DUAL("DUAL"),
        /**
         * EACH is mysql keyword.
         */
        EACH("EACH"),
        /**
         * ELSE is mysql keyword.
         */
        ELSE("ELSE"),
        /**
         * ELSEIF is mysql keyword.
         */
        ELSEIF("ELSEIF"),
        /**
         * EMPTY is mysql keyword.
         */
        EMPTY("EMPTY"),
        /**
         * ENCLOSED is mysql keyword.
         */
        ENCLOSED("ENCLOSED"),
        /**
         * ESCAPED is mysql keyword.
         */
        ESCAPED("ESCAPED"),
        /**
         * EXCEPT is mysql keyword.
         */
        EXCEPT("EXCEPT"),
        /**
         * EXISTS is mysql keyword.
         */
        EXISTS("EXISTS"),
        /**
         * EXIT is mysql keyword.
         */
        EXIT("EXIT"),
        /**
         * EXPLAIN is mysql keyword.
         */
        EXPLAIN("EXPLAIN"),
        /**
         * FALSE is mysql keyword.
         */
        FALSE("FALSE"),
        /**
         * FETCH is mysql keyword.
         */
        FETCH("FETCH"),
        /**
         * FIRST_VALUE is mysql keyword.
         */
        FIRST_VALUE("FIRST_VALUE"),
        /**
         * FLOAT is mysql keyword.
         */
        FLOAT("FLOAT"),
        /**
         * FLOAT4 is mysql keyword.
         */
        FLOAT4("FLOAT4"),
        /**
         * FLOAT8 is mysql keyword.
         */
        FLOAT8("FLOAT8"),
        /**
         * FOR is mysql keyword.
         */
        FOR("FOR"),
        /**
         * FORCE is mysql keyword.
         */
        FORCE("FORCE"),
        /**
         * FOREIGN is mysql keyword.
         */
        FOREIGN("FOREIGN"),
        /**
         * FROM is mysql keyword.
         */
        FROM("FROM"),
        /**
         * FULLTEXT is mysql keyword.
         */
        FULLTEXT("FULLTEXT"),
        /**
         * FUNCTION is mysql keyword.
         */
        FUNCTION("FUNCTION"),
        /**
         * GENERATED is mysql keyword.
         */
        GENERATED("GENERATED"),
        /**
         * GET is mysql keyword.
         */
        GET("GET"),
        /**
         * GRANT is mysql keyword.
         */
        GRANT("GRANT"),
        /**
         * GROUP is mysql keyword.
         */
        GROUP("GROUP"),
        /**
         * GROUPING is mysql keyword.
         */
        GROUPING("GROUPING"),
        /**
         * GROUPS is mysql keyword.
         */
        GROUPS("GROUPS"),
        /**
         * HAVING is mysql keyword.
         */
        HAVING("HAVING"),
        /**
         * HIGH_PRIORITY is mysql keyword.
         */
        HIGH_PRIORITY("HIGH_PRIORITY"),
        /**
         * HOUR_MICROSECOND is mysql keyword.
         */
        HOUR_MICROSECOND("HOUR_MICROSECOND"),
        /**
         * HOUR_MINUTE is mysql keyword.
         */
        HOUR_MINUTE("HOUR_MINUTE"),
        /**
         * HOUR_SECOND is mysql keyword.
         */
        HOUR_SECOND("HOUR_SECOND"),
        /**
         * IF is mysql keyword.
         */
        IF("IF"),
        /**
         * IGNORE is mysql keyword.
         */
        IGNORE("IGNORE"),
        /**
         * IN is mysql keyword.
         */
        IN("IN"),
        /**
         * INDEX is mysql keyword.
         */
        INDEX("INDEX"),
        /**
         * INFILE is mysql keyword.
         */
        INFILE("INFILE"),
        /**
         * INNER is mysql keyword.
         */
        INNER("INNER"),
        /**
         * INOUT is mysql keyword.
         */
        INOUT("INOUT"),
        /**
         * INSENSITIVE is mysql keyword.
         */
        INSENSITIVE("INSENSITIVE"),
        /**
         * INSERT is mysql keyword.
         */
        INSERT("INSERT"),
        /**
         * INT is mysql keyword.
         */
        INT("INT"),
        /**
         * INT1 is mysql keyword.
         */
        INT1("INT1"),
        /**
         * INT2 is mysql keyword.
         */
        INT2("INT2"),
        /**
         * INT3 is mysql keyword.
         */
        INT3("INT3"),
        /**
         * INT4 is mysql keyword.
         */
        INT4("INT4"),
        /**
         * INT8 is mysql keyword.
         */
        INT8("INT8"),
        /**
         * INTEGER is mysql keyword.
         */
        INTEGER("INTEGER"),
        /**
         * INTERVAL is mysql keyword.
         */
        INTERVAL("INTERVAL"),
        /**
         * INTO is mysql keyword.
         */
        INTO("INTO"),
        /**
         * IO_AFTER_GTIDS is mysql keyword.
         */
        IO_AFTER_GTIDS("IO_AFTER_GTIDS"),
        /**
         * IO_BEFORE_GTIDS is mysql keyword.
         */
        IO_BEFORE_GTIDS("IO_BEFORE_GTIDS"),
        /**
         * IS is mysql keyword.
         */
        IS("IS"),
        /**
         * ITERATE is mysql keyword.
         */
        ITERATE("ITERATE"),
        /**
         * JOIN is mysql keyword.
         */
        JOIN("JOIN"),
        /**
         * JSON_TABLE is mysql keyword.
         */
        JSON_TABLE("JSON_TABLE"),
        /**
         * KEY is mysql keyword.
         */
        KEY("KEY"),
        /**
         * KEYS is mysql keyword.
         */
        KEYS("KEYS"),
        /**
         * KILL is mysql keyword.
         */
        KILL("KILL"),
        /**
         * LAG is mysql keyword.
         */
        LAG("LAG"),
        /**
         * LAST_VALUE is mysql keyword.
         */
        LAST_VALUE("LAST_VALUE"),
        /**
         * LATERAL is mysql keyword.
         */
        LATERAL("LATERAL"),
        /**
         * LEAD is mysql keyword.
         */
        LEAD("LEAD"),
        /**
         * LEADING is mysql keyword.
         */
        LEADING("LEADING"),
        /**
         * LEAVE is mysql keyword.
         */
        LEAVE("LEAVE"),
        /**
         * LEFT is mysql keyword.
         */
        LEFT("LEFT"),
        /**
         * LIKE is mysql keyword.
         */
        LIKE("LIKE"),
        /**
         * LIMIT is mysql keyword.
         */
        LIMIT("LIMIT"),
        /**
         * LINEAR is mysql keyword.
         */
        LINEAR("LINEAR"),
        /**
         * LINES is mysql keyword.
         */
        LINES("LINES"),
        /**
         * LOAD is mysql keyword.
         */
        LOAD("LOAD"),
        /**
         * LOCALTIME is mysql keyword.
         */
        LOCALTIME("LOCALTIME"),
        /**
         * LOCALTIMESTAMP is mysql keyword.
         */
        LOCALTIMESTAMP("LOCALTIMESTAMP"),
        /**
         * LOCK is mysql keyword.
         */
        LOCK("LOCK"),
        /**
         * LONG is mysql keyword.
         */
        LONG("LONG"),
        /**
         * LONGBLOB is mysql keyword.
         */
        LONGBLOB("LONGBLOB"),
        /**
         * LONGTEXT is mysql keyword.
         */
        LONGTEXT("LONGTEXT"),
        /**
         * LOOP is mysql keyword.
         */
        LOOP("LOOP"),
        /**
         * LOW_PRIORITY is mysql keyword.
         */
        LOW_PRIORITY("LOW_PRIORITY"),
        /**
         * MASTER_BIND is mysql keyword.
         */
        MASTER_BIND("MASTER_BIND"),
        /**
         * MASTER_SSL_VERIFY_SERVER_CERT is mysql keyword.
         */
        MASTER_SSL_VERIFY_SERVER_CERT("MASTER_SSL_VERIFY_SERVER_CERT"),
        /**
         * MATCH is mysql keyword.
         */
        MATCH("MATCH"),
        /**
         * MAXVALUE is mysql keyword.
         */
        MAXVALUE("MAXVALUE"),
        /**
         * MEDIUMBLOB is mysql keyword.
         */
        MEDIUMBLOB("MEDIUMBLOB"),
        /**
         * MEDIUMINT is mysql keyword.
         */
        MEDIUMINT("MEDIUMINT"),
        /**
         * MEDIUMTEXT is mysql keyword.
         */
        MEDIUMTEXT("MEDIUMTEXT"),
        /**
         * MEMBER is mysql keyword.
         */
        MEMBER("MEMBER"),
        /**
         * MIDDLEINT is mysql keyword.
         */
        MIDDLEINT("MIDDLEINT"),
        /**
         * MINUTE_MICROSECOND is mysql keyword.
         */
        MINUTE_MICROSECOND("MINUTE_MICROSECOND"),
        /**
         * MINUTE_SECOND is mysql keyword.
         */
        MINUTE_SECOND("MINUTE_SECOND"),
        /**
         * MOD is mysql keyword.
         */
        MOD("MOD"),
        /**
         * MODIFIES is mysql keyword.
         */
        MODIFIES("MODIFIES"),
        /**
         * NATURAL is mysql keyword.
         */
        NATURAL("NATURAL"),
        /**
         * NOT is mysql keyword.
         */
        NOT("NOT"),
        /**
         * NO_WRITE_TO_BINLOG is mysql keyword.
         */
        NO_WRITE_TO_BINLOG("NO_WRITE_TO_BINLOG"),
        /**
         * NTH_VALUE is mysql keyword.
         */
        NTH_VALUE("NTH_VALUE"),
        /**
         * NTILE is mysql keyword.
         */
        NTILE("NTILE"),
        /**
         * NULL is mysql keyword.
         */
        NULL("NULL"),
        /**
         * NUMERIC is mysql keyword.
         */
        NUMERIC("NUMERIC"),
        /**
         * OF is mysql keyword.
         */
        OF("OF"),
        /**
         * ON is mysql keyword.
         */
        ON("ON"),
        /**
         * OPTIMIZE is mysql keyword.
         */
        OPTIMIZE("OPTIMIZE"),
        /**
         * OPTIMIZER_COSTS is mysql keyword.
         */
        OPTIMIZER_COSTS("OPTIMIZER_COSTS"),
        /**
         * OPTION is mysql keyword.
         */
        OPTION("OPTION"),
        /**
         * OPTIONALLY is mysql keyword.
         */
        OPTIONALLY("OPTIONALLY"),
        /**
         * OR is mysql keyword.
         */
        OR("OR"),
        /**
         * ORDER is mysql keyword.
         */
        ORDER("ORDER"),
        /**
         * OUT is mysql keyword.
         */
        OUT("OUT"),
        /**
         * OUTER is mysql keyword.
         */
        OUTER("OUTER"),
        /**
         * OUTFILE is mysql keyword.
         */
        OUTFILE("OUTFILE"),
        /**
         * OVER is mysql keyword.
         */
        OVER("OVER"),
        /**
         * PARTITION is mysql keyword.
         */
        PARTITION("PARTITION"),
        /**
         * PERCENT_RANK is mysql keyword.
         */
        PERCENT_RANK("PERCENT_RANK"),
        /**
         * PRECISION is mysql keyword.
         */
        PRECISION("PRECISION"),
        /**
         * PRIMARY is mysql keyword.
         */
        PRIMARY("PRIMARY"),
        /**
         * PROCEDURE is mysql keyword.
         */
        PROCEDURE("PROCEDURE"),
        /**
         * PURGE is mysql keyword.
         */
        PURGE("PURGE"),
        /**
         * RANGE is mysql keyword.
         */
        RANGE("RANGE"),
        /**
         * RANK is mysql keyword.
         */
        RANK("RANK"),
        /**
         * READ is mysql keyword.
         */
        READ("READ"),
        /**
         * READS is mysql keyword.
         */
        READS("READS"),
        /**
         * READ_WRITE is mysql keyword.
         */
        READ_WRITE("READ_WRITE"),
        /**
         * REAL is mysql keyword.
         */
        REAL("REAL"),
        /**
         * RECURSIVE is mysql keyword.
         */
        RECURSIVE("RECURSIVE"),
        /**
         * REFERENCES is mysql keyword.
         */
        REFERENCES("REFERENCES"),
        /**
         * REGEXP is mysql keyword.
         */
        REGEXP("REGEXP"),
        /**
         * RELEASE is mysql keyword.
         */
        RELEASE("RELEASE"),
        /**
         * RENAME is mysql keyword.
         */
        RENAME("RENAME"),
        /**
         * REPEAT is mysql keyword.
         */
        REPEAT("REPEAT"),
        /**
         * REPLACE is mysql keyword.
         */
        REPLACE("REPLACE"),
        /**
         * REQUIRE is mysql keyword.
         */
        REQUIRE("REQUIRE"),
        /**
         * RESIGNAL is mysql keyword.
         */
        RESIGNAL("RESIGNAL"),
        /**
         * RESTRICT is mysql keyword.
         */
        RESTRICT("RESTRICT"),
        /**
         * RETURN is mysql keyword.
         */
        RETURN("RETURN"),
        /**
         * REVOKE is mysql keyword.
         */
        REVOKE("REVOKE"),
        /**
         * RIGHT is mysql keyword.
         */
        RIGHT("RIGHT"),
        /**
         * RLIKE is mysql keyword.
         */
        RLIKE("RLIKE"),
        /**
         * ROW is mysql keyword.
         */
        ROW("ROW"),
        /**
         * ROWS is mysql keyword.
         */
        ROWS("ROWS"),
        /**
         * ROW_NUMBER is mysql keyword.
         */
        ROW_NUMBER("ROW_NUMBER"),
        /**
         * SCHEMA is mysql keyword.
         */
        SCHEMA("SCHEMA"),
        /**
         * SCHEMAS is mysql keyword.
         */
        SCHEMAS("SCHEMAS"),
        /**
         * SECOND_MICROSECOND is mysql keyword.
         */
        SECOND_MICROSECOND("SECOND_MICROSECOND"),
        /**
         * SELECT is mysql keyword.
         */
        SELECT("SELECT"),
        /**
         * SENSITIVE is mysql keyword.
         */
        SENSITIVE("SENSITIVE"),
        /**
         * SEPARATOR is mysql keyword.
         */
        SEPARATOR("SEPARATOR"),
        /**
         * SET is mysql keyword.
         */
        SET("SET"),
        /**
         * SHOW is mysql keyword.
         */
        SHOW("SHOW"),
        /**
         * SIGNAL is mysql keyword.
         */
        SIGNAL("SIGNAL"),
        /**
         * SMALLINT is mysql keyword.
         */
        SMALLINT("SMALLINT"),
        /**
         * SPATIAL is mysql keyword.
         */
        SPATIAL("SPATIAL"),
        /**
         * SPECIFIC is mysql keyword.
         */
        SPECIFIC("SPECIFIC"),
        /**
         * SQL is mysql keyword.
         */
        SQL("SQL"),
        /**
         * SQLEXCEPTION is mysql keyword.
         */
        SQLEXCEPTION("SQLEXCEPTION"),
        /**
         * SQLSTATE is mysql keyword.
         */
        SQLSTATE("SQLSTATE"),
        /**
         * SQLWARNING is mysql keyword.
         */
        SQLWARNING("SQLWARNING"),
        /**
         * SQL_BIG_RESULT is mysql keyword.
         */
        SQL_BIG_RESULT("SQL_BIG_RESULT"),
        /**
         * SQL_CALC_FOUND_ROWS is mysql keyword.
         */
        SQL_CALC_FOUND_ROWS("SQL_CALC_FOUND_ROWS"),
        /**
         * SQL_SMALL_RESULT is mysql keyword.
         */
        SQL_SMALL_RESULT("SQL_SMALL_RESULT"),
        /**
         * SSL is mysql keyword.
         */
        SSL("SSL"),
        /**
         * STARTING is mysql keyword.
         */
        STARTING("STARTING"),
        /**
         * STORED is mysql keyword.
         */
        STORED("STORED"),
        /**
         * STRAIGHT_JOIN is mysql keyword.
         */
        STRAIGHT_JOIN("STRAIGHT_JOIN"),
        /**
         * SYSTEM is mysql keyword.
         */
        SYSTEM("SYSTEM"),
        /**
         * TABLE is mysql keyword.
         */
        TABLE("TABLE"),
        /**
         * TERMINATED is mysql keyword.
         */
        TERMINATED("TERMINATED"),
        /**
         * THEN is mysql keyword.
         */
        THEN("THEN"),
        /**
         * TINYBLOB is mysql keyword.
         */
        TINYBLOB("TINYBLOB"),
        /**
         * TINYINT is mysql keyword.
         */
        TINYINT("TINYINT"),
        /**
         * TINYTEXT is mysql keyword.
         */
        TINYTEXT("TINYTEXT"),
        /**
         * TO is mysql keyword.
         */
        TO("TO"),
        /**
         * TRAILING is mysql keyword.
         */
        TRAILING("TRAILING"),
        /**
         * TRIGGER is mysql keyword.
         */
        TRIGGER("TRIGGER"),
        /**
         * TRUE is mysql keyword.
         */
        TRUE("TRUE"),
        /**
         * UNDO is mysql keyword.
         */
        UNDO("UNDO"),
        /**
         * UNION is mysql keyword.
         */
        UNION("UNION"),
        /**
         * UNIQUE is mysql keyword.
         */
        UNIQUE("UNIQUE"),
        /**
         * UNLOCK is mysql keyword.
         */
        UNLOCK("UNLOCK"),
        /**
         * UNSIGNED is mysql keyword.
         */
        UNSIGNED("UNSIGNED"),
        /**
         * UPDATE is mysql keyword.
         */
        UPDATE("UPDATE"),
        /**
         * USAGE is mysql keyword.
         */
        USAGE("USAGE"),
        /**
         * USE is mysql keyword.
         */
        USE("USE"),
        /**
         * USING is mysql keyword.
         */
        USING("USING"),
        /**
         * UTC_DATE is mysql keyword.
         */
        UTC_DATE("UTC_DATE"),
        /**
         * UTC_TIME is mysql keyword.
         */
        UTC_TIME("UTC_TIME"),
        /**
         * UTC_TIMESTAMP is mysql keyword.
         */
        UTC_TIMESTAMP("UTC_TIMESTAMP"),
        /**
         * VALUES is mysql keyword.
         */
        VALUES("VALUES"),
        /**
         * VARBINARY is mysql keyword.
         */
        VARBINARY("VARBINARY"),
        /**
         * VARCHAR is mysql keyword.
         */
        VARCHAR("VARCHAR"),
        /**
         * VARCHARACTER is mysql keyword.
         */
        VARCHARACTER("VARCHARACTER"),
        /**
         * VARYING is mysql keyword.
         */
        VARYING("VARYING"),
        /**
         * VIRTUAL is mysql keyword.
         */
        VIRTUAL("VIRTUAL"),
        /**
         * WHEN is mysql keyword.
         */
        WHEN("WHEN"),
        /**
         * WHERE is mysql keyword.
         */
        WHERE("WHERE"),
        /**
         * WHILE is mysql keyword.
         */
        WHILE("WHILE"),
        /**
         * WINDOW is mysql keyword.
         */
        WINDOW("WINDOW"),
        /**
         * WITH is mysql keyword.
         */
        WITH("WITH"),
        /**
         * WRITE is mysql keyword.
         */
        WRITE("WRITE"),
        /**
         * XOR is mysql keyword.
         */
        XOR("XOR"),
        /**
         * YEAR_MONTH is mysql keyword.
         */
        YEAR_MONTH("YEAR_MONTH"),
        /**
         * ZEROFILL is mysql keyword.
         */
        ZEROFILL("ZEROFILL");
        /**
         * The Name.
         */
        public final String name;

        MySQLKeyword(String name) {
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
        return check(fieldOrTableName) ? "`" + fieldOrTableName + "`" : fieldOrTableName;
    }

}
