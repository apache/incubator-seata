/*
 *  Copyright 1999_2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE_2.0
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
    private Set<String> keywordSet;

    private PostgresqlKeywordChecker() {
        keywordSet = Arrays.stream(PostgresqlKeywordChecker.PostgresqlKeyword.values()).map(PostgresqlKeywordChecker.PostgresqlKeyword::name).collect(Collectors.toSet());
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
         * ABS is postgresql keyword
         */
        ABS("ABS"),
        /**
         * ABSOLUTE is postgresql keyword
         */
        ABSOLUTE("ABSOLUTE"),
        /**
         * ACTION is postgresql keyword
         */
        ACTION("ACTION"),
        /**
         * ADD is postgresql keyword
         */
        ADD("ADD"),
        /**
         * ALL is postgresql keyword
         */
        ALL("ALL"),
        /**
         * ALLOCATE is postgresql keyword
         */
        ALLOCATE("ALLOCATE"),
        /**
         * ALTER is postgresql keyword
         */
        ALTER("ALTER"),
        /**
         * ANALYSE is postgresql keyword
         */
        ANALYSE("ANALYSE"),
        /**
         * ANALYZE is postgresql keyword
         */
        ANALYZE("ANALYZE"),
        /**
         * AND is postgresql keyword
         */
        AND("AND"),
        /**
         * ANY is postgresql keyword
         */
        ANY("ANY"),
        /**
         * ARE is postgresql keyword
         */
        ARE("ARE"),
        /**
         * ARRAY is postgresql keyword
         */
        ARRAY("ARRAY"),
        /**
         * ARRAY_AGG is postgresql keyword
         */
        ARRAY_AGG("ARRAY_AGG"),
        /**
         * ARRAY_MAX_CARDINALITY is postgresql keyword
         */
        ARRAY_MAX_CARDINALITY("ARRAY_MAX_CARDINALITY"),
        /**
         * AS is postgresql keyword
         */
        AS("AS"),
        /**
         * ASC is postgresql keyword
         */
        ASC("ASC"),
        /**
         * ASENSITIVE is postgresql keyword
         */
        ASENSITIVE("ASENSITIVE"),
        /**
         * ASSERTION is postgresql keyword
         */
        ASSERTION("ASSERTION"),
        /**
         * ASYMMETRIC is postgresql keyword
         */
        ASYMMETRIC("ASYMMETRIC"),
        /**
         * AT is postgresql keyword
         */
        AT("AT"),
        /**
         * ATOMIC is postgresql keyword
         */
        ATOMIC("ATOMIC"),
        /**
         * AUTHORIZATION is postgresql keyword
         */
        AUTHORIZATION("AUTHORIZATION"),
        /**
         * AVG is postgresql keyword
         */
        AVG("AVG"),
        /**
         * BEGIN is postgresql keyword
         */
        BEGIN("BEGIN"),
        /**
         * BEGIN_FRAME is postgresql keyword
         */
        BEGIN_FRAME("BEGIN_FRAME"),
        /**
         * BEGIN_PARTITION is postgresql keyword
         */
        BEGIN_PARTITION("BEGIN_PARTITION"),
        /**
         * BETWEEN is postgresql keyword
         */
        BETWEEN("BETWEEN"),
        /**
         * BIGINT is postgresql keyword
         */
        BIGINT("BIGINT"),
        /**
         * BINARY is postgresql keyword
         */
        BINARY("BINARY"),
        /**
         * BIT is postgresql keyword
         */
        BIT("BIT"),
        /**
         * BIT_LENGTH is postgresql keyword
         */
        BIT_LENGTH("BIT_LENGTH"),
        /**
         * BLOB is postgresql keyword
         */
        BLOB("BLOB"),
        /**
         * BOOLEAN is postgresql keyword
         */
        BOOLEAN("BOOLEAN"),
        /**
         * BOTH is postgresql keyword
         */
        BOTH("BOTH"),
        /**
         * BY is postgresql keyword
         */
        BY("BY"),
        /**
         * CALL is postgresql keyword
         */
        CALL("CALL"),
        /**
         * CALLED is postgresql keyword
         */
        CALLED("CALLED"),
        /**
         * CARDINALITY is postgresql keyword
         */
        CARDINALITY("CARDINALITY"),
        /**
         * CASCADE is postgresql keyword
         */
        CASCADE("CASCADE"),
        /**
         * CASCADED is postgresql keyword
         */
        CASCADED("CASCADED"),
        /**
         * CASE is postgresql keyword
         */
        CASE("CASE"),
        /**
         * CAST is postgresql keyword
         */
        CAST("CAST"),
        /**
         * CATALOG is postgresql keyword
         */
        CATALOG("CATALOG"),
        /**
         * CEIL is postgresql keyword
         */
        CEIL("CEIL"),
        /**
         * CEILING is postgresql keyword
         */
        CEILING("CEILING"),
        /**
         * CHAR is postgresql keyword
         */
        CHAR("CHAR"),
        /**
         * CHARACTER is postgresql keyword
         */
        CHARACTER("CHARACTER"),
        /**
         * CHARACTER_LENGTH is postgresql keyword
         */
        CHARACTER_LENGTH("CHARACTER_LENGTH"),
        /**
         * CHAR_LENGTH is postgresql keyword
         */
        CHAR_LENGTH("CHAR_LENGTH"),
        /**
         * CHECK is postgresql keyword
         */
        CHECK("CHECK"),
        /**
         * CLOB is postgresql keyword
         */
        CLOB("CLOB"),
        /**
         * CLOSE is postgresql keyword
         */
        CLOSE("CLOSE"),
        /**
         * COALESCE is postgresql keyword
         */
        COALESCE("COALESCE"),
        /**
         * COLLATE is postgresql keyword
         */
        COLLATE("COLLATE"),
        /**
         * COLLATION is postgresql keyword
         */
        COLLATION("COLLATION"),
        /**
         * COLLECT is postgresql keyword
         */
        COLLECT("COLLECT"),
        /**
         * COLUMN is postgresql keyword
         */
        COLUMN("COLUMN"),
        /**
         * COMMIT is postgresql keyword
         */
        COMMIT("COMMIT"),
        /**
         * CONCURRENTLY is postgresql keyword
         */
        CONCURRENTLY("CONCURRENTLY"),
        /**
         * CONDITION is postgresql keyword
         */
        CONDITION("CONDITION"),
        /**
         * CONNECT is postgresql keyword
         */
        CONNECT("CONNECT"),
        /**
         * CONNECTION is postgresql keyword
         */
        CONNECTION("CONNECTION"),
        /**
         * CONSTRAINT is postgresql keyword
         */
        CONSTRAINT("CONSTRAINT"),
        /**
         * CONSTRAINTS is postgresql keyword
         */
        CONSTRAINTS("CONSTRAINTS"),
        /**
         * CONTAINS is postgresql keyword
         */
        CONTAINS("CONTAINS"),
        /**
         * CONTINUE is postgresql keyword
         */
        CONTINUE("CONTINUE"),
        /**
         * CONVERT is postgresql keyword
         */
        CONVERT("CONVERT"),
        /**
         * CORR is postgresql keyword
         */
        CORR("CORR"),
        /**
         * CORRESPONDING is postgresql keyword
         */
        CORRESPONDING("CORRESPONDING"),
        /**
         * COUNT is postgresql keyword
         */
        COUNT("COUNT"),
        /**
         * COVAR_POP is postgresql keyword
         */
        COVAR_POP("COVAR_POP"),
        /**
         * COVAR_SAMP is postgresql keyword
         */
        COVAR_SAMP("COVAR_SAMP"),
        /**
         * CREATE is postgresql keyword
         */
        CREATE("CREATE"),
        /**
         * CROSS is postgresql keyword
         */
        CROSS("CROSS"),
        /**
         * CUBE is postgresql keyword
         */
        CUBE("CUBE"),
        /**
         * CUME_DIST is postgresql keyword
         */
        CUME_DIST("CUME_DIST"),
        /**
         * CURRENT is postgresql keyword
         */
        CURRENT("CURRENT"),
        /**
         * CURRENT_CATALOG is postgresql keyword
         */
        CURRENT_CATALOG("CURRENT_CATALOG"),
        /**
         * CURRENT_DATE is postgresql keyword
         */
        CURRENT_DATE("CURRENT_DATE"),
        /**
         * CURRENT_DEFAULT_TRANSFORM_GROUP is postgresql keyword
         */
        CURRENT_DEFAULT_TRANSFORM_GROUP("CURRENT_DEFAULT_TRANSFORM_GROUP"),
        /**
         * CURRENT_PATH is postgresql keyword
         */
        CURRENT_PATH("CURRENT_PATH"),
        /**
         * CURRENT_ROLE is postgresql keyword
         */
        CURRENT_ROLE("CURRENT_ROLE"),
        /**
         * CURRENT_ROW is postgresql keyword
         */
        CURRENT_ROW("CURRENT_ROW"),
        /**
         * CURRENT_SCHEMA is postgresql keyword
         */
        CURRENT_SCHEMA("CURRENT_SCHEMA"),
        /**
         * CURRENT_TIME is postgresql keyword
         */
        CURRENT_TIME("CURRENT_TIME"),
        /**
         * CURRENT_TIMESTAMP is postgresql keyword
         */
        CURRENT_TIMESTAMP("CURRENT_TIMESTAMP"),
        /**
         * CURRENT_TRANSFORM_GROUP_FOR_TYPE is postgresql keyword
         */
        CURRENT_TRANSFORM_GROUP_FOR_TYPE("CURRENT_TRANSFORM_GROUP_FOR_TYPE"),
        /**
         * CURRENT_USER is postgresql keyword
         */
        CURRENT_USER("CURRENT_USER"),
        /**
         * CURSOR is postgresql keyword
         */
        CURSOR("CURSOR"),
        /**
         * CYCLE is postgresql keyword
         */
        CYCLE("CYCLE"),
        /**
         * DATALINK is postgresql keyword
         */
        DATALINK("DATALINK"),
        /**
         * DATE is postgresql keyword
         */
        DATE("DATE"),
        /**
         * DAY is postgresql keyword
         */
        DAY("DAY"),
        /**
         * DEALLOCATE is postgresql keyword
         */
        DEALLOCATE("DEALLOCATE"),
        /**
         * DEC is postgresql keyword
         */
        DEC("DEC"),
        /**
         * DECIMAL is postgresql keyword
         */
        DECIMAL("DECIMAL"),
        /**
         * DECLARE is postgresql keyword
         */
        DECLARE("DECLARE"),
        /**
         * DEFAULT is postgresql keyword
         */
        DEFAULT("DEFAULT"),
        /**
         * DEFERRABLE is postgresql keyword
         */
        DEFERRABLE("DEFERRABLE"),
        /**
         * DEFERRED is postgresql keyword
         */
        DEFERRED("DEFERRED"),
        /**
         * DELETE is postgresql keyword
         */
        DELETE("DELETE"),
        /**
         * DENSE_RANK is postgresql keyword
         */
        DENSE_RANK("DENSE_RANK"),
        /**
         * DEREF is postgresql keyword
         */
        DEREF("DEREF"),
        /**
         * DESC is postgresql keyword
         */
        DESC("DESC"),
        /**
         * DESCRIBE is postgresql keyword
         */
        DESCRIBE("DESCRIBE"),
        /**
         * DESCRIPTOR is postgresql keyword
         */
        DESCRIPTOR("DESCRIPTOR"),
        /**
         * DETERMINISTIC is postgresql keyword
         */
        DETERMINISTIC("DETERMINISTIC"),
        /**
         * DIAGNOSTICS is postgresql keyword
         */
        DIAGNOSTICS("DIAGNOSTICS"),
        /**
         * DISCONNECT is postgresql keyword
         */
        DISCONNECT("DISCONNECT"),
        /**
         * DISTINCT is postgresql keyword
         */
        DISTINCT("DISTINCT"),
        /**
         * DLNEWCOPY is postgresql keyword
         */
        DLNEWCOPY("DLNEWCOPY"),
        /**
         * DLPREVIOUSCOPY is postgresql keyword
         */
        DLPREVIOUSCOPY("DLPREVIOUSCOPY"),
        /**
         * DLURLCOMPLETE is postgresql keyword
         */
        DLURLCOMPLETE("DLURLCOMPLETE"),
        /**
         * DLURLCOMPLETEONLY is postgresql keyword
         */
        DLURLCOMPLETEONLY("DLURLCOMPLETEONLY"),
        /**
         * DLURLCOMPLETEWRITE is postgresql keyword
         */
        DLURLCOMPLETEWRITE("DLURLCOMPLETEWRITE"),
        /**
         * DLURLPATH is postgresql keyword
         */
        DLURLPATH("DLURLPATH"),
        /**
         * DLURLPATHONLY is postgresql keyword
         */
        DLURLPATHONLY("DLURLPATHONLY"),
        /**
         * DLURLPATHWRITE is postgresql keyword
         */
        DLURLPATHWRITE("DLURLPATHWRITE"),
        /**
         * DLURLSCHEME is postgresql keyword
         */
        DLURLSCHEME("DLURLSCHEME"),
        /**
         * DLURLSERVER is postgresql keyword
         */
        DLURLSERVER("DLURLSERVER"),
        /**
         * DLVALUE is postgresql keyword
         */
        DLVALUE("DLVALUE"),
        /**
         * DO is postgresql keyword
         */
        DO("DO"),
        /**
         * DOMAIN is postgresql keyword
         */
        DOMAIN("DOMAIN"),
        /**
         * DOUBLE is postgresql keyword
         */
        DOUBLE("DOUBLE"),
        /**
         * DROP is postgresql keyword
         */
        DROP("DROP"),
        /**
         * DYNAMIC is postgresql keyword
         */
        DYNAMIC("DYNAMIC"),
        /**
         * EACH is postgresql keyword
         */
        EACH("EACH"),
        /**
         * ELEMENT is postgresql keyword
         */
        ELEMENT("ELEMENT"),
        /**
         * ELSE is postgresql keyword
         */
        ELSE("ELSE"),
        /**
         * END is postgresql keyword
         */
        END("END"),
        /**
         * END-EXEC is postgresql keyword
         */
        END_EXEC("END-EXEC"),
        /**
         * END_FRAME is postgresql keyword
         */
        END_FRAME("END_FRAME"),
        /**
         * END_PARTITION is postgresql keyword
         */
        END_PARTITION("END_PARTITION"),
        /**
         * EQUALS is postgresql keyword
         */
        EQUALS("EQUALS"),
        /**
         * ESCAPE is postgresql keyword
         */
        ESCAPE("ESCAPE"),
        /**
         * EVERY is postgresql keyword
         */
        EVERY("EVERY"),
        /**
         * EXCEPT is postgresql keyword
         */
        EXCEPT("EXCEPT"),
        /**
         * EXCEPTION is postgresql keyword
         */
        EXCEPTION("EXCEPTION"),
        /**
         * EXEC is postgresql keyword
         */
        EXEC("EXEC"),
        /**
         * EXECUTE is postgresql keyword
         */
        EXECUTE("EXECUTE"),
        /**
         * EXISTS is postgresql keyword
         */
        EXISTS("EXISTS"),
        /**
         * EXP is postgresql keyword
         */
        EXP("EXP"),
        /**
         * EXTERNAL is postgresql keyword
         */
        EXTERNAL("EXTERNAL"),
        /**
         * EXTRACT is postgresql keyword
         */
        EXTRACT("EXTRACT"),
        /**
         * FALSE is postgresql keyword
         */
        FALSE("FALSE"),
        /**
         * FETCH is postgresql keyword
         */
        FETCH("FETCH"),
        /**
         * FILTER is postgresql keyword
         */
        FILTER("FILTER"),
        /**
         * FIRST is postgresql keyword
         */
        FIRST("FIRST"),
        /**
         * FIRST_VALUE is postgresql keyword
         */
        FIRST_VALUE("FIRST_VALUE"),
        /**
         * FLOAT is postgresql keyword
         */
        FLOAT("FLOAT"),
        /**
         * FLOOR is postgresql keyword
         */
        FLOOR("FLOOR"),
        /**
         * FOR is postgresql keyword
         */
        FOR("FOR"),
        /**
         * FOREIGN is postgresql keyword
         */
        FOREIGN("FOREIGN"),
        /**
         * FOUND is postgresql keyword
         */
        FOUND("FOUND"),
        /**
         * FRAME_ROW is postgresql keyword
         */
        FRAME_ROW("FRAME_ROW"),
        /**
         * FREE is postgresql keyword
         */
        FREE("FREE"),
        /**
         * FREEZE is postgresql keyword
         */
        FREEZE("FREEZE"),
        /**
         * FROM is postgresql keyword
         */
        FROM("FROM"),
        /**
         * FULL is postgresql keyword
         */
        FULL("FULL"),
        /**
         * FUNCTION is postgresql keyword
         */
        FUNCTION("FUNCTION"),
        /**
         * FUSION is postgresql keyword
         */
        FUSION("FUSION"),
        /**
         * GET is postgresql keyword
         */
        GET("GET"),
        /**
         * GLOBAL is postgresql keyword
         */
        GLOBAL("GLOBAL"),
        /**
         * GO is postgresql keyword
         */
        GO("GO"),
        /**
         * GOTO is postgresql keyword
         */
        GOTO("GOTO"),
        /**
         * GRANT is postgresql keyword
         */
        GRANT("GRANT"),
        /**
         * GROUP is postgresql keyword
         */
        GROUP("GROUP"),
        /**
         * GROUPING is postgresql keyword
         */
        GROUPING("GROUPING"),
        /**
         * GROUPS is postgresql keyword
         */
        GROUPS("GROUPS"),
        /**
         * HAVING is postgresql keyword
         */
        HAVING("HAVING"),
        /**
         * HOLD is postgresql keyword
         */
        HOLD("HOLD"),
        /**
         * HOUR is postgresql keyword
         */
        HOUR("HOUR"),
        /**
         * IDENTITY is postgresql keyword
         */
        IDENTITY("IDENTITY"),
        /**
         * ILIKE is postgresql keyword
         */
        ILIKE("ILIKE"),
        /**
         * IMMEDIATE is postgresql keyword
         */
        IMMEDIATE("IMMEDIATE"),
        /**
         * IMPORT is postgresql keyword
         */
        IMPORT("IMPORT"),
        /**
         * IN is postgresql keyword
         */
        IN("IN"),
        /**
         * INDICATOR is postgresql keyword
         */
        INDICATOR("INDICATOR"),
        /**
         * INITIALLY is postgresql keyword
         */
        INITIALLY("INITIALLY"),
        /**
         * INNER is postgresql keyword
         */
        INNER("INNER"),
        /**
         * INOUT is postgresql keyword
         */
        INOUT("INOUT"),
        /**
         * INPUT is postgresql keyword
         */
        INPUT("INPUT"),
        /**
         * INSENSITIVE is postgresql keyword
         */
        INSENSITIVE("INSENSITIVE"),
        /**
         * INSERT is postgresql keyword
         */
        INSERT("INSERT"),
        /**
         * INT is postgresql keyword
         */
        INT("INT"),
        /**
         * INTEGER is postgresql keyword
         */
        INTEGER("INTEGER"),
        /**
         * INTERSECT is postgresql keyword
         */
        INTERSECT("INTERSECT"),
        /**
         * INTERSECTION is postgresql keyword
         */
        INTERSECTION("INTERSECTION"),
        /**
         * INTERVAL is postgresql keyword
         */
        INTERVAL("INTERVAL"),
        /**
         * INTO is postgresql keyword
         */
        INTO("INTO"),
        /**
         * IS is postgresql keyword
         */
        IS("IS"),
        /**
         * ISNULL is postgresql keyword
         */
        ISNULL("ISNULL"),
        /**
         * ISOLATION is postgresql keyword
         */
        ISOLATION("ISOLATION"),
        /**
         * JOIN is postgresql keyword
         */
        JOIN("JOIN"),
        /**
         * KEY is postgresql keyword
         */
        KEY("KEY"),
        /**
         * LAG is postgresql keyword
         */
        LAG("LAG"),
        /**
         * LANGUAGE is postgresql keyword
         */
        LANGUAGE("LANGUAGE"),
        /**
         * LARGE is postgresql keyword
         */
        LARGE("LARGE"),
        /**
         * LAST is postgresql keyword
         */
        LAST("LAST"),
        /**
         * LAST_VALUE is postgresql keyword
         */
        LAST_VALUE("LAST_VALUE"),
        /**
         * LATERAL is postgresql keyword
         */
        LATERAL("LATERAL"),
        /**
         * LEAD is postgresql keyword
         */
        LEAD("LEAD"),
        /**
         * LEADING is postgresql keyword
         */
        LEADING("LEADING"),
        /**
         * LEFT is postgresql keyword
         */
        LEFT("LEFT"),
        /**
         * LEVEL is postgresql keyword
         */
        LEVEL("LEVEL"),
        /**
         * LIKE is postgresql keyword
         */
        LIKE("LIKE"),
        /**
         * LIKE_REGEX is postgresql keyword
         */
        LIKE_REGEX("LIKE_REGEX"),
        /**
         * LIMIT is postgresql keyword
         */
        LIMIT("LIMIT"),
        /**
         * LN is postgresql keyword
         */
        LN("LN"),
        /**
         * LOCAL is postgresql keyword
         */
        LOCAL("LOCAL"),
        /**
         * LOCALTIME is postgresql keyword
         */
        LOCALTIME("LOCALTIME"),
        /**
         * LOCALTIMESTAMP is postgresql keyword
         */
        LOCALTIMESTAMP("LOCALTIMESTAMP"),
        /**
         * LOWER is postgresql keyword
         */
        LOWER("LOWER"),
        /**
         * MATCH is postgresql keyword
         */
        MATCH("MATCH"),
        /**
         * MAX is postgresql keyword
         */
        MAX("MAX"),
        /**
         * MAX_CARDINALITY is postgresql keyword
         */
        MAX_CARDINALITY("MAX_CARDINALITY"),
        /**
         * MEMBER is postgresql keyword
         */
        MEMBER("MEMBER"),
        /**
         * MERGE is postgresql keyword
         */
        MERGE("MERGE"),
        /**
         * METHOD is postgresql keyword
         */
        METHOD("METHOD"),
        /**
         * MIN is postgresql keyword
         */
        MIN("MIN"),
        /**
         * MINUTE is postgresql keyword
         */
        MINUTE("MINUTE"),
        /**
         * MOD is postgresql keyword
         */
        MOD("MOD"),
        /**
         * MODIFIES is postgresql keyword
         */
        MODIFIES("MODIFIES"),
        /**
         * MODULE is postgresql keyword
         */
        MODULE("MODULE"),
        /**
         * MONTH is postgresql keyword
         */
        MONTH("MONTH"),
        /**
         * MULTISET is postgresql keyword
         */
        MULTISET("MULTISET"),
        /**
         * NAMES is postgresql keyword
         */
        NAMES("NAMES"),
        /**
         * NATIONAL is postgresql keyword
         */
        NATIONAL("NATIONAL"),
        /**
         * NATURAL is postgresql keyword
         */
        NATURAL("NATURAL"),
        /**
         * NCHAR is postgresql keyword
         */
        NCHAR("NCHAR"),
        /**
         * NCLOB is postgresql keyword
         */
        NCLOB("NCLOB"),
        /**
         * NEW is postgresql keyword
         */
        NEW("NEW"),
        /**
         * NEXT is postgresql keyword
         */
        NEXT("NEXT"),
        /**
         * NO is postgresql keyword
         */
        NO("NO"),
        /**
         * NONE is postgresql keyword
         */
        NONE("NONE"),
        /**
         * NORMALIZE is postgresql keyword
         */
        NORMALIZE("NORMALIZE"),
        /**
         * NOT is postgresql keyword
         */
        NOT("NOT"),
        /**
         * NOTNULL is postgresql keyword
         */
        NOTNULL("NOTNULL"),
        /**
         * NTH_VALUE is postgresql keyword
         */
        NTH_VALUE("NTH_VALUE"),
        /**
         * NTILE is postgresql keyword
         */
        NTILE("NTILE"),
        /**
         * NULL is postgresql keyword
         */
        NULL("NULL"),
        /**
         * NULLIF is postgresql keyword
         */
        NULLIF("NULLIF"),
        /**
         * NUMERIC is postgresql keyword
         */
        NUMERIC("NUMERIC"),
        /**
         * OCCURRENCES_REGEX is postgresql keyword
         */
        OCCURRENCES_REGEX("OCCURRENCES_REGEX"),
        /**
         * OCTET_LENGTH is postgresql keyword
         */
        OCTET_LENGTH("OCTET_LENGTH"),
        /**
         * OF is postgresql keyword
         */
        OF("OF"),
        /**
         * OFFSET is postgresql keyword
         */
        OFFSET("OFFSET"),
        /**
         * OLD is postgresql keyword
         */
        OLD("OLD"),
        /**
         * ON is postgresql keyword
         */
        ON("ON"),
        /**
         * ONLY is postgresql keyword
         */
        ONLY("ONLY"),
        /**
         * OPEN is postgresql keyword
         */
        OPEN("OPEN"),
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
         * OUT is postgresql keyword
         */
        OUT("OUT"),
        /**
         * OUTER is postgresql keyword
         */
        OUTER("OUTER"),
        /**
         * OUTPUT is postgresql keyword
         */
        OUTPUT("OUTPUT"),
        /**
         * OVER is postgresql keyword
         */
        OVER("OVER"),
        /**
         * OVERLAPS is postgresql keyword
         */
        OVERLAPS("OVERLAPS"),
        /**
         * OVERLAY is postgresql keyword
         */
        OVERLAY("OVERLAY"),
        /**
         * PAD is postgresql keyword
         */
        PAD("PAD"),
        /**
         * PARAMETER is postgresql keyword
         */
        PARAMETER("PARAMETER"),
        /**
         * PARTIAL is postgresql keyword
         */
        PARTIAL("PARTIAL"),
        /**
         * PARTITION is postgresql keyword
         */
        PARTITION("PARTITION"),
        /**
         * PERCENT is postgresql keyword
         */
        PERCENT("PERCENT"),
        /**
         * PERCENTILE_CONT is postgresql keyword
         */
        PERCENTILE_CONT("PERCENTILE_CONT"),
        /**
         * PERCENTILE_DISC is postgresql keyword
         */
        PERCENTILE_DISC("PERCENTILE_DISC"),
        /**
         * PERCENT_RANK is postgresql keyword
         */
        PERCENT_RANK("PERCENT_RANK"),
        /**
         * PERIOD is postgresql keyword
         */
        PERIOD("PERIOD"),
        /**
         * PLACING is postgresql keyword
         */
        PLACING("PLACING"),
        /**
         * PORTION is postgresql keyword
         */
        PORTION("PORTION"),
        /**
         * POSITION is postgresql keyword
         */
        POSITION("POSITION"),
        /**
         * POSITION_REGEX is postgresql keyword
         */
        POSITION_REGEX("POSITION_REGEX"),
        /**
         * POWER is postgresql keyword
         */
        POWER("POWER"),
        /**
         * PRECEDES is postgresql keyword
         */
        PRECEDES("PRECEDES"),
        /**
         * PRECISION is postgresql keyword
         */
        PRECISION("PRECISION"),
        /**
         * PREPARE is postgresql keyword
         */
        PREPARE("PREPARE"),
        /**
         * PRESERVE is postgresql keyword
         */
        PRESERVE("PRESERVE"),
        /**
         * PRIMARY is postgresql keyword
         */
        PRIMARY("PRIMARY"),
        /**
         * PRIOR is postgresql keyword
         */
        PRIOR("PRIOR"),
        /**
         * PRIVILEGES is postgresql keyword
         */
        PRIVILEGES("PRIVILEGES"),
        /**
         * PROCEDURE is postgresql keyword
         */
        PROCEDURE("PROCEDURE"),
        /**
         * PUBLIC is postgresql keyword
         */
        PUBLIC("PUBLIC"),
        /**
         * RANGE is postgresql keyword
         */
        RANGE("RANGE"),
        /**
         * RANK is postgresql keyword
         */
        RANK("RANK"),
        /**
         * READ is postgresql keyword
         */
        READ("READ"),
        /**
         * READS is postgresql keyword
         */
        READS("READS"),
        /**
         * REAL is postgresql keyword
         */
        REAL("REAL"),
        /**
         * RECURSIVE is postgresql keyword
         */
        RECURSIVE("RECURSIVE"),
        /**
         * REF is postgresql keyword
         */
        REF("REF"),
        /**
         * REFERENCES is postgresql keyword
         */
        REFERENCES("REFERENCES"),
        /**
         * REFERENCING is postgresql keyword
         */
        REFERENCING("REFERENCING"),
        /**
         * REGR_AVGX is postgresql keyword
         */
        REGR_AVGX("REGR_AVGX"),
        /**
         * REGR_AVGY is postgresql keyword
         */
        REGR_AVGY("REGR_AVGY"),
        /**
         * REGR_COUNT is postgresql keyword
         */
        REGR_COUNT("REGR_COUNT"),
        /**
         * REGR_INTERCEPT is postgresql keyword
         */
        REGR_INTERCEPT("REGR_INTERCEPT"),
        /**
         * REGR_R2 is postgresql keyword
         */
        REGR_R2("REGR_R2"),
        /**
         * REGR_SLOPE is postgresql keyword
         */
        REGR_SLOPE("REGR_SLOPE"),
        /**
         * REGR_SXX is postgresql keyword
         */
        REGR_SXX("REGR_SXX"),
        /**
         * REGR_SXY is postgresql keyword
         */
        REGR_SXY("REGR_SXY"),
        /**
         * REGR_SYY is postgresql keyword
         */
        REGR_SYY("REGR_SYY"),
        /**
         * RELATIVE is postgresql keyword
         */
        RELATIVE("RELATIVE"),
        /**
         * RELEASE is postgresql keyword
         */
        RELEASE("RELEASE"),
        /**
         * RESTRICT is postgresql keyword
         */
        RESTRICT("RESTRICT"),
        /**
         * RESULT is postgresql keyword
         */
        RESULT("RESULT"),
        /**
         * RETURN is postgresql keyword
         */
        RETURN("RETURN"),
        /**
         * RETURNING is postgresql keyword
         */
        RETURNING("RETURNING"),
        /**
         * RETURNS is postgresql keyword
         */
        RETURNS("RETURNS"),
        /**
         * REVOKE is postgresql keyword
         */
        REVOKE("REVOKE"),
        /**
         * RIGHT is postgresql keyword
         */
        RIGHT("RIGHT"),
        /**
         * ROLLBACK is postgresql keyword
         */
        ROLLBACK("ROLLBACK"),
        /**
         * ROLLUP is postgresql keyword
         */
        ROLLUP("ROLLUP"),
        /**
         * ROW is postgresql keyword
         */
        ROW("ROW"),
        /**
         * ROWS is postgresql keyword
         */
        ROWS("ROWS"),
        /**
         * ROW_NUMBER is postgresql keyword
         */
        ROW_NUMBER("ROW_NUMBER"),
        /**
         * SAVEPOINT is postgresql keyword
         */
        SAVEPOINT("SAVEPOINT"),
        /**
         * SCHEMA is postgresql keyword
         */
        SCHEMA("SCHEMA"),
        /**
         * SCOPE is postgresql keyword
         */
        SCOPE("SCOPE"),
        /**
         * SCROLL is postgresql keyword
         */
        SCROLL("SCROLL"),
        /**
         * SEARCH is postgresql keyword
         */
        SEARCH("SEARCH"),
        /**
         * SECOND is postgresql keyword
         */
        SECOND("SECOND"),
        /**
         * SECTION is postgresql keyword
         */
        SECTION("SECTION"),
        /**
         * SELECT is postgresql keyword
         */
        SELECT("SELECT"),
        /**
         * SENSITIVE is postgresql keyword
         */
        SENSITIVE("SENSITIVE"),
        /**
         * SESSION is postgresql keyword
         */
        SESSION("SESSION"),
        /**
         * SESSION_USER is postgresql keyword
         */
        SESSION_USER("SESSION_USER"),
        /**
         * SET is postgresql keyword
         */
        SET("SET"),
        /**
         * SIMILAR is postgresql keyword
         */
        SIMILAR("SIMILAR"),
        /**
         * SIZE is postgresql keyword
         */
        SIZE("SIZE"),
        /**
         * SMALLINT is postgresql keyword
         */
        SMALLINT("SMALLINT"),
        /**
         * SOME is postgresql keyword
         */
        SOME("SOME"),
        /**
         * SPACE is postgresql keyword
         */
        SPACE("SPACE"),
        /**
         * SPECIFIC is postgresql keyword
         */
        SPECIFIC("SPECIFIC"),
        /**
         * SPECIFICTYPE is postgresql keyword
         */
        SPECIFICTYPE("SPECIFICTYPE"),
        /**
         * SQL is postgresql keyword
         */
        SQL("SQL"),
        /**
         * SQLCODE is postgresql keyword
         */
        SQLCODE("SQLCODE"),
        /**
         * SQLERROR is postgresql keyword
         */
        SQLERROR("SQLERROR"),
        /**
         * SQLEXCEPTION is postgresql keyword
         */
        SQLEXCEPTION("SQLEXCEPTION"),
        /**
         * SQLSTATE is postgresql keyword
         */
        SQLSTATE("SQLSTATE"),
        /**
         * SQLWARNING is postgresql keyword
         */
        SQLWARNING("SQLWARNING"),
        /**
         * SQRT is postgresql keyword
         */
        SQRT("SQRT"),
        /**
         * START is postgresql keyword
         */
        START("START"),
        /**
         * STATIC is postgresql keyword
         */
        STATIC("STATIC"),
        /**
         * STDDEV_POP is postgresql keyword
         */
        STDDEV_POP("STDDEV_POP"),
        /**
         * STDDEV_SAMP is postgresql keyword
         */
        STDDEV_SAMP("STDDEV_SAMP"),
        /**
         * SUBMULTISET is postgresql keyword
         */
        SUBMULTISET("SUBMULTISET"),
        /**
         * SUBSTRING is postgresql keyword
         */
        SUBSTRING("SUBSTRING"),
        /**
         * SUBSTRING_REGEX is postgresql keyword
         */
        SUBSTRING_REGEX("SUBSTRING_REGEX"),
        /**
         * SUCCEEDS is postgresql keyword
         */
        SUCCEEDS("SUCCEEDS"),
        /**
         * SUM is postgresql keyword
         */
        SUM("SUM"),
        /**
         * SYMMETRIC is postgresql keyword
         */
        SYMMETRIC("SYMMETRIC"),
        /**
         * SYSTEM is postgresql keyword
         */
        SYSTEM("SYSTEM"),
        /**
         * SYSTEM_TIME is postgresql keyword
         */
        SYSTEM_TIME("SYSTEM_TIME"),
        /**
         * SYSTEM_USER is postgresql keyword
         */
        SYSTEM_USER("SYSTEM_USER"),
        /**
         * TABLE is postgresql keyword
         */
        TABLE("TABLE"),
        /**
         * TABLESAMPLE is postgresql keyword
         */
        TABLESAMPLE("TABLESAMPLE"),
        /**
         * TEMPORARY is postgresql keyword
         */
        TEMPORARY("TEMPORARY"),
        /**
         * THEN is postgresql keyword
         */
        THEN("THEN"),
        /**
         * TIME is postgresql keyword
         */
        TIME("TIME"),
        /**
         * TIMESTAMP is postgresql keyword
         */
        TIMESTAMP("TIMESTAMP"),
        /**
         * TIMEZONE_HOUR is postgresql keyword
         */
        TIMEZONE_HOUR("TIMEZONE_HOUR"),
        /**
         * TIMEZONE_MINUTE is postgresql keyword
         */
        TIMEZONE_MINUTE("TIMEZONE_MINUTE"),
        /**
         * TO is postgresql keyword
         */
        TO("TO"),
        /**
         * TRAILING is postgresql keyword
         */
        TRAILING("TRAILING"),
        /**
         * TRANSACTION is postgresql keyword
         */
        TRANSACTION("TRANSACTION"),
        /**
         * TRANSLATE is postgresql keyword
         */
        TRANSLATE("TRANSLATE"),
        /**
         * TRANSLATE_REGEX is postgresql keyword
         */
        TRANSLATE_REGEX("TRANSLATE_REGEX"),
        /**
         * TRANSLATION is postgresql keyword
         */
        TRANSLATION("TRANSLATION"),
        /**
         * TREAT is postgresql keyword
         */
        TREAT("TREAT"),
        /**
         * TRIGGER is postgresql keyword
         */
        TRIGGER("TRIGGER"),
        /**
         * TRIM is postgresql keyword
         */
        TRIM("TRIM"),
        /**
         * TRIM_ARRAY is postgresql keyword
         */
        TRIM_ARRAY("TRIM_ARRAY"),
        /**
         * TRUE is postgresql keyword
         */
        TRUE("TRUE"),
        /**
         * TRUNCATE is postgresql keyword
         */
        TRUNCATE("TRUNCATE"),
        /**
         * UESCAPE is postgresql keyword
         */
        UESCAPE("UESCAPE"),
        /**
         * UNION is postgresql keyword
         */
        UNION("UNION"),
        /**
         * UNIQUE is postgresql keyword
         */
        UNIQUE("UNIQUE"),
        /**
         * UNKNOWN is postgresql keyword
         */
        UNKNOWN("UNKNOWN"),
        /**
         * UNNEST is postgresql keyword
         */
        UNNEST("UNNEST"),
        /**
         * UPDATE is postgresql keyword
         */
        UPDATE("UPDATE"),
        /**
         * UPPER is postgresql keyword
         */
        UPPER("UPPER"),
        /**
         * USAGE is postgresql keyword
         */
        USAGE("USAGE"),
        /**
         * USER is postgresql keyword
         */
        USER("USER"),
        /**
         * USING is postgresql keyword
         */
        USING("USING"),
        /**
         * VALUE is postgresql keyword
         */
        VALUE("VALUE"),
        /**
         * VALUES is postgresql keyword
         */
        VALUES("VALUES"),
        /**
         * VALUE_OF is postgresql keyword
         */
        VALUE_OF("VALUE_OF"),
        /**
         * VARBINARY is postgresql keyword
         */
        VARBINARY("VARBINARY"),
        /**
         * VARCHAR is postgresql keyword
         */
        VARCHAR("VARCHAR"),
        /**
         * VARIADIC is postgresql keyword
         */
        VARIADIC("VARIADIC"),
        /**
         * VARYING is postgresql keyword
         */
        VARYING("VARYING"),
        /**
         * VAR_POP is postgresql keyword
         */
        VAR_POP("VAR_POP"),
        /**
         * VAR_SAMP is postgresql keyword
         */
        VAR_SAMP("VAR_SAMP"),
        /**
         * VERBOSE is postgresql keyword
         */
        VERBOSE("VERBOSE"),
        /**
         * VERSIONING is postgresql keyword
         */
        VERSIONING("VERSIONING"),
        /**
         * VIEW is postgresql keyword
         */
        VIEW("VIEW"),
        /**
         * WHEN is postgresql keyword
         */
        WHEN("WHEN"),
        /**
         * WHENEVER is postgresql keyword
         */
        WHENEVER("WHENEVER"),
        /**
         * WHERE is postgresql keyword
         */
        WHERE("WHERE"),
        /**
         * WIDTH_BUCKET is postgresql keyword
         */
        WIDTH_BUCKET("WIDTH_BUCKET"),
        /**
         * WINDOW is postgresql keyword
         */
        WINDOW("WINDOW"),
        /**
         * WITH is postgresql keyword
         */
        WITH("WITH"),
        /**
         * WITHIN is postgresql keyword
         */
        WITHIN("WITHIN"),
        /**
         * WITHOUT is postgresql keyword
         */
        WITHOUT("WITHOUT"),
        /**
         * WORK is postgresql keyword
         */
        WORK("WORK"),
        /**
         * WRITE is postgresql keyword
         */
        WRITE("WRITE"),
        /**
         * XML is postgresql keyword
         */
        XML("XML"),
        /**
         * XMLAGG is postgresql keyword
         */
        XMLAGG("XMLAGG"),
        /**
         * XMLATTRIBUTES is postgresql keyword
         */
        XMLATTRIBUTES("XMLATTRIBUTES"),
        /**
         * XMLBINARY is postgresql keyword
         */
        XMLBINARY("XMLBINARY"),
        /**
         * XMLCAST is postgresql keyword
         */
        XMLCAST("XMLCAST"),
        /**
         * XMLCOMMENT is postgresql keyword
         */
        XMLCOMMENT("XMLCOMMENT"),
        /**
         * XMLCONCAT is postgresql keyword
         */
        XMLCONCAT("XMLCONCAT"),
        /**
         * XMLDOCUMENT is postgresql keyword
         */
        XMLDOCUMENT("XMLDOCUMENT"),
        /**
         * XMLELEMENT is postgresql keyword
         */
        XMLELEMENT("XMLELEMENT"),
        /**
         * XMLEXISTS is postgresql keyword
         */
        XMLEXISTS("XMLEXISTS"),
        /**
         * XMLFOREST is postgresql keyword
         */
        XMLFOREST("XMLFOREST"),
        /**
         * XMLITERATE is postgresql keyword
         */
        XMLITERATE("XMLITERATE"),
        /**
         * XMLNAMESPACES is postgresql keyword
         */
        XMLNAMESPACES("XMLNAMESPACES"),
        /**
         * XMLPARSE is postgresql keyword
         */
        XMLPARSE("XMLPARSE"),
        /**
         * XMLPI is postgresql keyword
         */
        XMLPI("XMLPI"),
        /**
         * XMLQUERY is postgresql keyword
         */
        XMLQUERY("XMLQUERY"),
        /**
         * XMLSERIALIZE is postgresql keyword
         */
        XMLSERIALIZE("XMLSERIALIZE"),
        /**
         * XMLTABLE is postgresql keyword
         */
        XMLTABLE("XMLTABLE"),
        /**
         * XMLTEXT is postgresql keyword
         */
        XMLTEXT("XMLTEXT"),
        /**
         * XMLVALIDATE is postgresql keyword
         */
        XMLVALIDATE("XMLVALIDATE"),
        /**
         * YEAR is postgresql keyword
         */
        YEAR("YEAR"),
        /**
         * ZONE is postgresql keyword
         */
        ZONE("ZONE");
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
    }

    private String replace(String fieldOrTableName) {
        StringBuilder builder = new StringBuilder();
        builder.append("\"").append(fieldOrTableName).append("\"");
        String name = builder.toString();
        return name;
    }
}
