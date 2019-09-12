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
         * A is postgresql keyword
         */
        A("A"),
        /**
         * ABORT is postgresql keyword
         */
        ABORT("ABORT"),
        /**
         * ABS is postgresql keyword
         */
        ABS("ABS"),
        /**
         * ABSENT is postgresql keyword
         */
        ABSENT("ABSENT"),
        /**
         * ABSOLUTE is postgresql keyword
         */
        ABSOLUTE("ABSOLUTE"),
        /**
         * ACCESS is postgresql keyword
         */
        ACCESS("ACCESS"),
        /**
         * ACCORDING is postgresql keyword
         */
        ACCORDING("ACCORDING"),
        /**
         * ACTION is postgresql keyword
         */
        ACTION("ACTION"),
        /**
         * ADA is postgresql keyword
         */
        ADA("ADA"),
        /**
         * ADD is postgresql keyword
         */
        ADD("ADD"),
        /**
         * ADMIN is postgresql keyword
         */
        ADMIN("ADMIN"),
        /**
         * AFTER is postgresql keyword
         */
        AFTER("AFTER"),
        /**
         * AGGREGATE is postgresql keyword
         */
        AGGREGATE("AGGREGATE"),
        /**
         * ALL is postgresql keyword
         */
        ALL("ALL"),
        /**
         * ALLOCATE is postgresql keyword
         */
        ALLOCATE("ALLOCATE"),
        /**
         * ALSO is postgresql keyword
         */
        ALSO("ALSO"),
        /**
         * ALTER is postgresql keyword
         */
        ALTER("ALTER"),
        /**
         * ALWAYS is postgresql keyword
         */
        ALWAYS("ALWAYS"),
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
         * ASSIGNMENT is postgresql keyword
         */
        ASSIGNMENT("ASSIGNMENT"),
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
         * ATTACH is postgresql keyword
         */
        ATTACH("ATTACH"),
        /**
         * ATTRIBUTE is postgresql keyword
         */
        ATTRIBUTE("ATTRIBUTE"),
        /**
         * ATTRIBUTES is postgresql keyword
         */
        ATTRIBUTES("ATTRIBUTES"),
        /**
         * AUTHORIZATION is postgresql keyword
         */
        AUTHORIZATION("AUTHORIZATION"),
        /**
         * AVG is postgresql keyword
         */
        AVG("AVG"),
        /**
         * BACKWARD is postgresql keyword
         */
        BACKWARD("BACKWARD"),
        /**
         * BASE64 is postgresql keyword
         */
        BASE64("BASE64"),
        /**
         * BEFORE is postgresql keyword
         */
        BEFORE("BEFORE"),
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
         * BERNOULLI is postgresql keyword
         */
        BERNOULLI("BERNOULLI"),
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
         * BLOCKED is postgresql keyword
         */
        BLOCKED("BLOCKED"),
        /**
         * BOM is postgresql keyword
         */
        BOM("BOM"),
        /**
         * BOOLEAN is postgresql keyword
         */
        BOOLEAN("BOOLEAN"),
        /**
         * BOTH is postgresql keyword
         */
        BOTH("BOTH"),
        /**
         * BREADTH is postgresql keyword
         */
        BREADTH("BREADTH"),
        /**
         * BY is postgresql keyword
         */
        BY("BY"),
        /**
         * C is postgresql keyword
         */
        C("C"),
        /**
         * CACHE is postgresql keyword
         */
        CACHE("CACHE"),
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
         * CATALOG_NAME is postgresql keyword
         */
        CATALOG_NAME("CATALOG_NAME"),
        /**
         * CEIL is postgresql keyword
         */
        CEIL("CEIL"),
        /**
         * CEILING is postgresql keyword
         */
        CEILING("CEILING"),
        /**
         * CHAIN is postgresql keyword
         */
        CHAIN("CHAIN"),
        /**
         * CHAR is postgresql keyword
         */
        CHAR("CHAR"),
        /**
         * CHARACTER is postgresql keyword
         */
        CHARACTER("CHARACTER"),
        /**
         * CHARACTERISTICS is postgresql keyword
         */
        CHARACTERISTICS("CHARACTERISTICS"),
        /**
         * CHARACTERS is postgresql keyword
         */
        CHARACTERS("CHARACTERS"),
        /**
         * CHARACTER_LENGTH is postgresql keyword
         */
        CHARACTER_LENGTH("CHARACTER_LENGTH"),
        /**
         * CHARACTER_SET_CATALOG is postgresql keyword
         */
        CHARACTER_SET_CATALOG("CHARACTER_SET_CATALOG"),
        /**
         * CHARACTER_SET_NAME is postgresql keyword
         */
        CHARACTER_SET_NAME("CHARACTER_SET_NAME"),
        /**
         * CHARACTER_SET_SCHEMA is postgresql keyword
         */
        CHARACTER_SET_SCHEMA("CHARACTER_SET_SCHEMA"),
        /**
         * CHAR_LENGTH is postgresql keyword
         */
        CHAR_LENGTH("CHAR_LENGTH"),
        /**
         * CHECK is postgresql keyword
         */
        CHECK("CHECK"),
        /**
         * CHECKPOINT is postgresql keyword
         */
        CHECKPOINT("CHECKPOINT"),
        /**
         * CLASS is postgresql keyword
         */
        CLASS("CLASS"),
        /**
         * CLASS_ORIGIN is postgresql keyword
         */
        CLASS_ORIGIN("CLASS_ORIGIN"),
        /**
         * CLOB is postgresql keyword
         */
        CLOB("CLOB"),
        /**
         * CLOSE is postgresql keyword
         */
        CLOSE("CLOSE"),
        /**
         * CLUSTER is postgresql keyword
         */
        CLUSTER("CLUSTER"),
        /**
         * COALESCE is postgresql keyword
         */
        COALESCE("COALESCE"),
        /**
         * COBOL is postgresql keyword
         */
        COBOL("COBOL"),
        /**
         * COLLATE is postgresql keyword
         */
        COLLATE("COLLATE"),
        /**
         * COLLATION is postgresql keyword
         */
        COLLATION("COLLATION"),
        /**
         * COLLATION_CATALOG is postgresql keyword
         */
        COLLATION_CATALOG("COLLATION_CATALOG"),
        /**
         * COLLATION_NAME is postgresql keyword
         */
        COLLATION_NAME("COLLATION_NAME"),
        /**
         * COLLATION_SCHEMA is postgresql keyword
         */
        COLLATION_SCHEMA("COLLATION_SCHEMA"),
        /**
         * COLLECT is postgresql keyword
         */
        COLLECT("COLLECT"),
        /**
         * COLUMN is postgresql keyword
         */
        COLUMN("COLUMN"),
        /**
         * COLUMNS is postgresql keyword
         */
        COLUMNS("COLUMNS"),
        /**
         * COLUMN_NAME is postgresql keyword
         */
        COLUMN_NAME("COLUMN_NAME"),
        /**
         * COMMAND_FUNCTION is postgresql keyword
         */
        COMMAND_FUNCTION("COMMAND_FUNCTION"),
        /**
         * COMMAND_FUNCTION_CODE is postgresql keyword
         */
        COMMAND_FUNCTION_CODE("COMMAND_FUNCTION_CODE"),
        /**
         * COMMENT is postgresql keyword
         */
        COMMENT("COMMENT"),
        /**
         * COMMENTS is postgresql keyword
         */
        COMMENTS("COMMENTS"),
        /**
         * COMMIT is postgresql keyword
         */
        COMMIT("COMMIT"),
        /**
         * COMMITTED is postgresql keyword
         */
        COMMITTED("COMMITTED"),
        /**
         * CONCURRENTLY is postgresql keyword
         */
        CONCURRENTLY("CONCURRENTLY"),
        /**
         * CONDITION is postgresql keyword
         */
        CONDITION("CONDITION"),
        /**
         * CONDITION_NUMBER is postgresql keyword
         */
        CONDITION_NUMBER("CONDITION_NUMBER"),
        /**
         * CONFIGURATION is postgresql keyword
         */
        CONFIGURATION("CONFIGURATION"),
        /**
         * CONFLICT is postgresql keyword
         */
        CONFLICT("CONFLICT"),
        /**
         * CONNECT is postgresql keyword
         */
        CONNECT("CONNECT"),
        /**
         * CONNECTION is postgresql keyword
         */
        CONNECTION("CONNECTION"),
        /**
         * CONNECTION_NAME is postgresql keyword
         */
        CONNECTION_NAME("CONNECTION_NAME"),
        /**
         * CONSTRAINT is postgresql keyword
         */
        CONSTRAINT("CONSTRAINT"),
        /**
         * CONSTRAINTS is postgresql keyword
         */
        CONSTRAINTS("CONSTRAINTS"),
        /**
         * CONSTRAINT_CATALOG is postgresql keyword
         */
        CONSTRAINT_CATALOG("CONSTRAINT_CATALOG"),
        /**
         * CONSTRAINT_NAME is postgresql keyword
         */
        CONSTRAINT_NAME("CONSTRAINT_NAME"),
        /**
         * CONSTRAINT_SCHEMA is postgresql keyword
         */
        CONSTRAINT_SCHEMA("CONSTRAINT_SCHEMA"),
        /**
         * CONSTRUCTOR is postgresql keyword
         */
        CONSTRUCTOR("CONSTRUCTOR"),
        /**
         * CONTAINS is postgresql keyword
         */
        CONTAINS("CONTAINS"),
        /**
         * CONTENT is postgresql keyword
         */
        CONTENT("CONTENT"),
        /**
         * CONTINUE is postgresql keyword
         */
        CONTINUE("CONTINUE"),
        /**
         * CONTROL is postgresql keyword
         */
        CONTROL("CONTROL"),
        /**
         * CONVERSION is postgresql keyword
         */
        CONVERSION("CONVERSION"),
        /**
         * CONVERT is postgresql keyword
         */
        CONVERT("CONVERT"),
        /**
         * COPY is postgresql keyword
         */
        COPY("COPY"),
        /**
         * CORR is postgresql keyword
         */
        CORR("CORR"),
        /**
         * CORRESPONDING is postgresql keyword
         */
        CORRESPONDING("CORRESPONDING"),
        /**
         * COST is postgresql keyword
         */
        COST("COST"),
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
         * CSV is postgresql keyword
         */
        CSV("CSV"),
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
         * CURSOR_NAME is postgresql keyword
         */
        CURSOR_NAME("CURSOR_NAME"),
        /**
         * CYCLE is postgresql keyword
         */
        CYCLE("CYCLE"),
        /**
         * DATA is postgresql keyword
         */
        DATA("DATA"),
        /**
         * DATABASE is postgresql keyword
         */
        DATABASE("DATABASE"),
        /**
         * DATALINK is postgresql keyword
         */
        DATALINK("DATALINK"),
        /**
         * DATE is postgresql keyword
         */
        DATE("DATE"),
        /**
         * DATETIME_INTERVAL_CODE is postgresql keyword
         */
        DATETIME_INTERVAL_CODE("DATETIME_INTERVAL_CODE"),
        /**
         * DATETIME_INTERVAL_PRECISION is postgresql keyword
         */
        DATETIME_INTERVAL_PRECISION("DATETIME_INTERVAL_PRECISION"),
        /**
         * DAY is postgresql keyword
         */
        DAY("DAY"),
        /**
         * DB is postgresql keyword
         */
        DB("DB"),
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
         * DEFAULTS is postgresql keyword
         */
        DEFAULTS("DEFAULTS"),
        /**
         * DEFERRABLE is postgresql keyword
         */
        DEFERRABLE("DEFERRABLE"),
        /**
         * DEFERRED is postgresql keyword
         */
        DEFERRED("DEFERRED"),
        /**
         * DEFINED is postgresql keyword
         */
        DEFINED("DEFINED"),
        /**
         * DEFINER is postgresql keyword
         */
        DEFINER("DEFINER"),
        /**
         * DEGREE is postgresql keyword
         */
        DEGREE("DEGREE"),
        /**
         * DELETE is postgresql keyword
         */
        DELETE("DELETE"),
        /**
         * DELIMITER is postgresql keyword
         */
        DELIMITER("DELIMITER"),
        /**
         * DELIMITERS is postgresql keyword
         */
        DELIMITERS("DELIMITERS"),
        /**
         * DENSE_RANK is postgresql keyword
         */
        DENSE_RANK("DENSE_RANK"),
        /**
         * DEPENDS is postgresql keyword
         */
        DEPENDS("DEPENDS"),
        /**
         * DEPTH is postgresql keyword
         */
        DEPTH("DEPTH"),
        /**
         * DEREF is postgresql keyword
         */
        DEREF("DEREF"),
        /**
         * DERIVED is postgresql keyword
         */
        DERIVED("DERIVED"),
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
         * DETACH is postgresql keyword
         */
        DETACH("DETACH"),
        /**
         * DETERMINISTIC is postgresql keyword
         */
        DETERMINISTIC("DETERMINISTIC"),
        /**
         * DIAGNOSTICS is postgresql keyword
         */
        DIAGNOSTICS("DIAGNOSTICS"),
        /**
         * DICTIONARY is postgresql keyword
         */
        DICTIONARY("DICTIONARY"),
        /**
         * DISABLE is postgresql keyword
         */
        DISABLE("DISABLE"),
        /**
         * DISCARD is postgresql keyword
         */
        DISCARD("DISCARD"),
        /**
         * DISCONNECT is postgresql keyword
         */
        DISCONNECT("DISCONNECT"),
        /**
         * DISPATCH is postgresql keyword
         */
        DISPATCH("DISPATCH"),
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
         * DOCUMENT is postgresql keyword
         */
        DOCUMENT("DOCUMENT"),
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
         * DYNAMIC_FUNCTION is postgresql keyword
         */
        DYNAMIC_FUNCTION("DYNAMIC_FUNCTION"),
        /**
         * DYNAMIC_FUNCTION_CODE is postgresql keyword
         */
        DYNAMIC_FUNCTION_CODE("DYNAMIC_FUNCTION_CODE"),
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
         * EMPTY is postgresql keyword
         */
        EMPTY("EMPTY"),
        /**
         * ENABLE is postgresql keyword
         */
        ENABLE("ENABLE"),
        /**
         * ENCODING is postgresql keyword
         */
        ENCODING("ENCODING"),
        /**
         * ENCRYPTED is postgresql keyword
         */
        ENCRYPTED("ENCRYPTED"),
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
         * ENFORCED is postgresql keyword
         */
        ENFORCED("ENFORCED"),
        /**
         * ENUM is postgresql keyword
         */
        ENUM("ENUM"),
        /**
         * EQUALS is postgresql keyword
         */
        EQUALS("EQUALS"),
        /**
         * ESCAPE is postgresql keyword
         */
        ESCAPE("ESCAPE"),
        /**
         * EVENT is postgresql keyword
         */
        EVENT("EVENT"),
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
         * EXCLUDE is postgresql keyword
         */
        EXCLUDE("EXCLUDE"),
        /**
         * EXCLUDING is postgresql keyword
         */
        EXCLUDING("EXCLUDING"),
        /**
         * EXCLUSIVE is postgresql keyword
         */
        EXCLUSIVE("EXCLUSIVE"),
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
         * EXPLAIN is postgresql keyword
         */
        EXPLAIN("EXPLAIN"),
        /**
         * EXPRESSION is postgresql keyword
         */
        EXPRESSION("EXPRESSION"),
        /**
         * EXTENSION is postgresql keyword
         */
        EXTENSION("EXTENSION"),
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
         * FAMILY is postgresql keyword
         */
        FAMILY("FAMILY"),
        /**
         * FETCH is postgresql keyword
         */
        FETCH("FETCH"),
        /**
         * FILE is postgresql keyword
         */
        FILE("FILE"),
        /**
         * FILTER is postgresql keyword
         */
        FILTER("FILTER"),
        /**
         * FINAL is postgresql keyword
         */
        FINAL("FINAL"),
        /**
         * FIRST is postgresql keyword
         */
        FIRST("FIRST"),
        /**
         * FIRST_VALUE is postgresql keyword
         */
        FIRST_VALUE("FIRST_VALUE"),
        /**
         * FLAG is postgresql keyword
         */
        FLAG("FLAG"),
        /**
         * FLOAT is postgresql keyword
         */
        FLOAT("FLOAT"),
        /**
         * FLOOR is postgresql keyword
         */
        FLOOR("FLOOR"),
        /**
         * FOLLOWING is postgresql keyword
         */
        FOLLOWING("FOLLOWING"),
        /**
         * FOR is postgresql keyword
         */
        FOR("FOR"),
        /**
         * FORCE is postgresql keyword
         */
        FORCE("FORCE"),
        /**
         * FOREIGN is postgresql keyword
         */
        FOREIGN("FOREIGN"),
        /**
         * FORTRAN is postgresql keyword
         */
        FORTRAN("FORTRAN"),
        /**
         * FORWARD is postgresql keyword
         */
        FORWARD("FORWARD"),
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
         * FS is postgresql keyword
         */
        FS("FS"),
        /**
         * FULL is postgresql keyword
         */
        FULL("FULL"),
        /**
         * FUNCTION is postgresql keyword
         */
        FUNCTION("FUNCTION"),
        /**
         * FUNCTIONS is postgresql keyword
         */
        FUNCTIONS("FUNCTIONS"),
        /**
         * FUSION is postgresql keyword
         */
        FUSION("FUSION"),
        /**
         * G is postgresql keyword
         */
        G("G"),
        /**
         * GENERAL is postgresql keyword
         */
        GENERAL("GENERAL"),
        /**
         * GENERATED is postgresql keyword
         */
        GENERATED("GENERATED"),
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
         * GRANTED is postgresql keyword
         */
        GRANTED("GRANTED"),
        /**
         * GREATEST is postgresql keyword
         */
        GREATEST("GREATEST"),
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
         * HANDLER is postgresql keyword
         */
        HANDLER("HANDLER"),
        /**
         * HAVING is postgresql keyword
         */
        HAVING("HAVING"),
        /**
         * HEADER is postgresql keyword
         */
        HEADER("HEADER"),
        /**
         * HEX is postgresql keyword
         */
        HEX("HEX"),
        /**
         * HIERARCHY is postgresql keyword
         */
        HIERARCHY("HIERARCHY"),
        /**
         * HOLD is postgresql keyword
         */
        HOLD("HOLD"),
        /**
         * HOUR is postgresql keyword
         */
        HOUR("HOUR"),
        /**
         * ID is postgresql keyword
         */
        ID("ID"),
        /**
         * IDENTITY is postgresql keyword
         */
        IDENTITY("IDENTITY"),
        /**
         * IF is postgresql keyword
         */
        IF("IF"),
        /**
         * IGNORE is postgresql keyword
         */
        IGNORE("IGNORE"),
        /**
         * ILIKE is postgresql keyword
         */
        ILIKE("ILIKE"),
        /**
         * IMMEDIATE is postgresql keyword
         */
        IMMEDIATE("IMMEDIATE"),
        /**
         * IMMEDIATELY is postgresql keyword
         */
        IMMEDIATELY("IMMEDIATELY"),
        /**
         * IMMUTABLE is postgresql keyword
         */
        IMMUTABLE("IMMUTABLE"),
        /**
         * IMPLEMENTATION is postgresql keyword
         */
        IMPLEMENTATION("IMPLEMENTATION"),
        /**
         * IMPLICIT is postgresql keyword
         */
        IMPLICIT("IMPLICIT"),
        /**
         * IMPORT is postgresql keyword
         */
        IMPORT("IMPORT"),
        /**
         * IN is postgresql keyword
         */
        IN("IN"),
        /**
         * INCLUDE is postgresql keyword
         */
        INCLUDE("INCLUDE"),
        /**
         * INCLUDING is postgresql keyword
         */
        INCLUDING("INCLUDING"),
        /**
         * INCREMENT is postgresql keyword
         */
        INCREMENT("INCREMENT"),
        /**
         * INDENT is postgresql keyword
         */
        INDENT("INDENT"),
        /**
         * INDEX is postgresql keyword
         */
        INDEX("INDEX"),
        /**
         * INDEXES is postgresql keyword
         */
        INDEXES("INDEXES"),
        /**
         * INDICATOR is postgresql keyword
         */
        INDICATOR("INDICATOR"),
        /**
         * INHERIT is postgresql keyword
         */
        INHERIT("INHERIT"),
        /**
         * INHERITS is postgresql keyword
         */
        INHERITS("INHERITS"),
        /**
         * INITIALLY is postgresql keyword
         */
        INITIALLY("INITIALLY"),
        /**
         * INLINE is postgresql keyword
         */
        INLINE("INLINE"),
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
         * INSTANCE is postgresql keyword
         */
        INSTANCE("INSTANCE"),
        /**
         * INSTANTIABLE is postgresql keyword
         */
        INSTANTIABLE("INSTANTIABLE"),
        /**
         * INSTEAD is postgresql keyword
         */
        INSTEAD("INSTEAD"),
        /**
         * INT is postgresql keyword
         */
        INT("INT"),
        /**
         * INTEGER is postgresql keyword
         */
        INTEGER("INTEGER"),
        /**
         * INTEGRITY is postgresql keyword
         */
        INTEGRITY("INTEGRITY"),
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
         * INVOKER is postgresql keyword
         */
        INVOKER("INVOKER"),
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
         * K is postgresql keyword
         */
        K("K"),
        /**
         * KEY is postgresql keyword
         */
        KEY("KEY"),
        /**
         * KEY_MEMBER is postgresql keyword
         */
        KEY_MEMBER("KEY_MEMBER"),
        /**
         * KEY_TYPE is postgresql keyword
         */
        KEY_TYPE("KEY_TYPE"),
        /**
         * LABEL is postgresql keyword
         */
        LABEL("LABEL"),
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
         * LEAKPROOF is postgresql keyword
         */
        LEAKPROOF("LEAKPROOF"),
        /**
         * LEAST is postgresql keyword
         */
        LEAST("LEAST"),
        /**
         * LEFT is postgresql keyword
         */
        LEFT("LEFT"),
        /**
         * LENGTH is postgresql keyword
         */
        LENGTH("LENGTH"),
        /**
         * LEVEL is postgresql keyword
         */
        LEVEL("LEVEL"),
        /**
         * LIBRARY is postgresql keyword
         */
        LIBRARY("LIBRARY"),
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
         * LINK is postgresql keyword
         */
        LINK("LINK"),
        /**
         * LISTEN is postgresql keyword
         */
        LISTEN("LISTEN"),
        /**
         * LN is postgresql keyword
         */
        LN("LN"),
        /**
         * LOAD is postgresql keyword
         */
        LOAD("LOAD"),
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
         * LOCATION is postgresql keyword
         */
        LOCATION("LOCATION"),
        /**
         * LOCATOR is postgresql keyword
         */
        LOCATOR("LOCATOR"),
        /**
         * LOCK is postgresql keyword
         */
        LOCK("LOCK"),
        /**
         * LOCKED is postgresql keyword
         */
        LOCKED("LOCKED"),
        /**
         * LOGGED is postgresql keyword
         */
        LOGGED("LOGGED"),
        /**
         * LOWER is postgresql keyword
         */
        LOWER("LOWER"),
        /**
         * M is postgresql keyword
         */
        M("M"),
        /**
         * MAP is postgresql keyword
         */
        MAP("MAP"),
        /**
         * MAPPING is postgresql keyword
         */
        MAPPING("MAPPING"),
        /**
         * MATCH is postgresql keyword
         */
        MATCH("MATCH"),
        /**
         * MATCHED is postgresql keyword
         */
        MATCHED("MATCHED"),
        /**
         * MATERIALIZED is postgresql keyword
         */
        MATERIALIZED("MATERIALIZED"),
        /**
         * MAX is postgresql keyword
         */
        MAX("MAX"),
        /**
         * MAXVALUE is postgresql keyword
         */
        MAXVALUE("MAXVALUE"),
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
         * MESSAGE_LENGTH is postgresql keyword
         */
        MESSAGE_LENGTH("MESSAGE_LENGTH"),
        /**
         * MESSAGE_OCTET_LENGTH is postgresql keyword
         */
        MESSAGE_OCTET_LENGTH("MESSAGE_OCTET_LENGTH"),
        /**
         * MESSAGE_TEXT is postgresql keyword
         */
        MESSAGE_TEXT("MESSAGE_TEXT"),
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
         * MINVALUE is postgresql keyword
         */
        MINVALUE("MINVALUE"),
        /**
         * MOD is postgresql keyword
         */
        MOD("MOD"),
        /**
         * MODE is postgresql keyword
         */
        MODE("MODE"),
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
         * MORE is postgresql keyword
         */
        MORE("MORE"),
        /**
         * MOVE is postgresql keyword
         */
        MOVE("MOVE"),
        /**
         * MULTISET is postgresql keyword
         */
        MULTISET("MULTISET"),
        /**
         * MUMPS is postgresql keyword
         */
        MUMPS("MUMPS"),
        /**
         * NAME is postgresql keyword
         */
        NAME("NAME"),
        /**
         * NAMES is postgresql keyword
         */
        NAMES("NAMES"),
        /**
         * NAMESPACE is postgresql keyword
         */
        NAMESPACE("NAMESPACE"),
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
         * NESTING is postgresql keyword
         */
        NESTING("NESTING"),
        /**
         * NEW is postgresql keyword
         */
        NEW("NEW"),
        /**
         * NEXT is postgresql keyword
         */
        NEXT("NEXT"),
        /**
         * NFC is postgresql keyword
         */
        NFC("NFC"),
        /**
         * NFD is postgresql keyword
         */
        NFD("NFD"),
        /**
         * NFKC is postgresql keyword
         */
        NFKC("NFKC"),
        /**
         * NFKD is postgresql keyword
         */
        NFKD("NFKD"),
        /**
         * NIL is postgresql keyword
         */
        NIL("NIL"),
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
         * NORMALIZED is postgresql keyword
         */
        NORMALIZED("NORMALIZED"),
        /**
         * NOT is postgresql keyword
         */
        NOT("NOT"),
        /**
         * NOTHING is postgresql keyword
         */
        NOTHING("NOTHING"),
        /**
         * NOTIFY is postgresql keyword
         */
        NOTIFY("NOTIFY"),
        /**
         * NOTNULL is postgresql keyword
         */
        NOTNULL("NOTNULL"),
        /**
         * NOWAIT is postgresql keyword
         */
        NOWAIT("NOWAIT"),
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
         * NULLABLE is postgresql keyword
         */
        NULLABLE("NULLABLE"),
        /**
         * NULLIF is postgresql keyword
         */
        NULLIF("NULLIF"),
        /**
         * NULLS is postgresql keyword
         */
        NULLS("NULLS"),
        /**
         * NUMBER is postgresql keyword
         */
        NUMBER("NUMBER"),
        /**
         * NUMERIC is postgresql keyword
         */
        NUMERIC("NUMERIC"),
        /**
         * OBJECT is postgresql keyword
         */
        OBJECT("OBJECT"),
        /**
         * OCCURRENCES_REGEX is postgresql keyword
         */
        OCCURRENCES_REGEX("OCCURRENCES_REGEX"),
        /**
         * OCTETS is postgresql keyword
         */
        OCTETS("OCTETS"),
        /**
         * OCTET_LENGTH is postgresql keyword
         */
        OCTET_LENGTH("OCTET_LENGTH"),
        /**
         * OF is postgresql keyword
         */
        OF("OF"),
        /**
         * OFF is postgresql keyword
         */
        OFF("OFF"),
        /**
         * OFFSET is postgresql keyword
         */
        OFFSET("OFFSET"),
        /**
         * OIDS is postgresql keyword
         */
        OIDS("OIDS"),
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
         * OPERATOR is postgresql keyword
         */
        OPERATOR("OPERATOR"),
        /**
         * OPTION is postgresql keyword
         */
        OPTION("OPTION"),
        /**
         * OPTIONS is postgresql keyword
         */
        OPTIONS("OPTIONS"),
        /**
         * OR is postgresql keyword
         */
        OR("OR"),
        /**
         * ORDER is postgresql keyword
         */
        ORDER("ORDER"),
        /**
         * ORDERING is postgresql keyword
         */
        ORDERING("ORDERING"),
        /**
         * ORDINALITY is postgresql keyword
         */
        ORDINALITY("ORDINALITY"),
        /**
         * OTHERS is postgresql keyword
         */
        OTHERS("OTHERS"),
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
         * OVERRIDING is postgresql keyword
         */
        OVERRIDING("OVERRIDING"),
        /**
         * OWNED is postgresql keyword
         */
        OWNED("OWNED"),
        /**
         * OWNER is postgresql keyword
         */
        OWNER("OWNER"),
        /**
         * P is postgresql keyword
         */
        P("P"),
        /**
         * PAD is postgresql keyword
         */
        PAD("PAD"),
        /**
         * PARALLEL is postgresql keyword
         */
        PARALLEL("PARALLEL"),
        /**
         * PARAMETER is postgresql keyword
         */
        PARAMETER("PARAMETER"),
        /**
         * PARAMETER_MODE is postgresql keyword
         */
        PARAMETER_MODE("PARAMETER_MODE"),
        /**
         * PARAMETER_NAME is postgresql keyword
         */
        PARAMETER_NAME("PARAMETER_NAME"),
        /**
         * PARAMETER_ORDINAL_POSITION is postgresql keyword
         */
        PARAMETER_ORDINAL_POSITION("PARAMETER_ORDINAL_POSITION"),
        /**
         * PARAMETER_SPECIFIC_CATALOG is postgresql keyword
         */
        PARAMETER_SPECIFIC_CATALOG("PARAMETER_SPECIFIC_CATALOG"),
        /**
         * PARAMETER_SPECIFIC_NAME is postgresql keyword
         */
        PARAMETER_SPECIFIC_NAME("PARAMETER_SPECIFIC_NAME"),
        /**
         * PARAMETER_SPECIFIC_SCHEMA is postgresql keyword
         */
        PARAMETER_SPECIFIC_SCHEMA("PARAMETER_SPECIFIC_SCHEMA"),
        /**
         * PARSER is postgresql keyword
         */
        PARSER("PARSER"),
        /**
         * PARTIAL is postgresql keyword
         */
        PARTIAL("PARTIAL"),
        /**
         * PARTITION is postgresql keyword
         */
        PARTITION("PARTITION"),
        /**
         * PASCAL is postgresql keyword
         */
        PASCAL("PASCAL"),
        /**
         * PASSING is postgresql keyword
         */
        PASSING("PASSING"),
        /**
         * PASSTHROUGH is postgresql keyword
         */
        PASSTHROUGH("PASSTHROUGH"),
        /**
         * PASSWORD is postgresql keyword
         */
        PASSWORD("PASSWORD"),
        /**
         * PATH is postgresql keyword
         */
        PATH("PATH"),
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
         * PERMISSION is postgresql keyword
         */
        PERMISSION("PERMISSION"),
        /**
         * PLACING is postgresql keyword
         */
        PLACING("PLACING"),
        /**
         * PLANS is postgresql keyword
         */
        PLANS("PLANS"),
        /**
         * PLI is postgresql keyword
         */
        PLI("PLI"),
        /**
         * POLICY is postgresql keyword
         */
        POLICY("POLICY"),
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
         * PRECEDING is postgresql keyword
         */
        PRECEDING("PRECEDING"),
        /**
         * PRECISION is postgresql keyword
         */
        PRECISION("PRECISION"),
        /**
         * PREPARE is postgresql keyword
         */
        PREPARE("PREPARE"),
        /**
         * PREPARED is postgresql keyword
         */
        PREPARED("PREPARED"),
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
         * PROCEDURAL is postgresql keyword
         */
        PROCEDURAL("PROCEDURAL"),
        /**
         * PROCEDURE is postgresql keyword
         */
        PROCEDURE("PROCEDURE"),
        /**
         * PROCEDURES is postgresql keyword
         */
        PROCEDURES("PROCEDURES"),
        /**
         * PROGRAM is postgresql keyword
         */
        PROGRAM("PROGRAM"),
        /**
         * PUBLIC is postgresql keyword
         */
        PUBLIC("PUBLIC"),
        /**
         * PUBLICATION is postgresql keyword
         */
        PUBLICATION("PUBLICATION"),
        /**
         * QUOTE is postgresql keyword
         */
        QUOTE("QUOTE"),
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
         * REASSIGN is postgresql keyword
         */
        REASSIGN("REASSIGN"),
        /**
         * RECHECK is postgresql keyword
         */
        RECHECK("RECHECK"),
        /**
         * RECOVERY is postgresql keyword
         */
        RECOVERY("RECOVERY"),
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
         * REFRESH is postgresql keyword
         */
        REFRESH("REFRESH"),
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
         * REINDEX is postgresql keyword
         */
        REINDEX("REINDEX"),
        /**
         * RELATIVE is postgresql keyword
         */
        RELATIVE("RELATIVE"),
        /**
         * RELEASE is postgresql keyword
         */
        RELEASE("RELEASE"),
        /**
         * RENAME is postgresql keyword
         */
        RENAME("RENAME"),
        /**
         * REPEATABLE is postgresql keyword
         */
        REPEATABLE("REPEATABLE"),
        /**
         * REPLACE is postgresql keyword
         */
        REPLACE("REPLACE"),
        /**
         * REPLICA is postgresql keyword
         */
        REPLICA("REPLICA"),
        /**
         * REQUIRING is postgresql keyword
         */
        REQUIRING("REQUIRING"),
        /**
         * RESET is postgresql keyword
         */
        RESET("RESET"),
        /**
         * RESPECT is postgresql keyword
         */
        RESPECT("RESPECT"),
        /**
         * RESTART is postgresql keyword
         */
        RESTART("RESTART"),
        /**
         * RESTORE is postgresql keyword
         */
        RESTORE("RESTORE"),
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
         * RETURNED_CARDINALITY is postgresql keyword
         */
        RETURNED_CARDINALITY("RETURNED_CARDINALITY"),
        /**
         * RETURNED_LENGTH is postgresql keyword
         */
        RETURNED_LENGTH("RETURNED_LENGTH"),
        /**
         * RETURNED_OCTET_LENGTH is postgresql keyword
         */
        RETURNED_OCTET_LENGTH("RETURNED_OCTET_LENGTH"),
        /**
         * RETURNED_SQLSTATE is postgresql keyword
         */
        RETURNED_SQLSTATE("RETURNED_SQLSTATE"),
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
         * ROLE is postgresql keyword
         */
        ROLE("ROLE"),
        /**
         * ROLLBACK is postgresql keyword
         */
        ROLLBACK("ROLLBACK"),
        /**
         * ROLLUP is postgresql keyword
         */
        ROLLUP("ROLLUP"),
        /**
         * ROUTINE is postgresql keyword
         */
        ROUTINE("ROUTINE"),
        /**
         * ROUTINES is postgresql keyword
         */
        ROUTINES("ROUTINES"),
        /**
         * ROUTINE_CATALOG is postgresql keyword
         */
        ROUTINE_CATALOG("ROUTINE_CATALOG"),
        /**
         * ROUTINE_NAME is postgresql keyword
         */
        ROUTINE_NAME("ROUTINE_NAME"),
        /**
         * ROUTINE_SCHEMA is postgresql keyword
         */
        ROUTINE_SCHEMA("ROUTINE_SCHEMA"),
        /**
         * ROW is postgresql keyword
         */
        ROW("ROW"),
        /**
         * ROWS is postgresql keyword
         */
        ROWS("ROWS"),
        /**
         * ROW_COUNT is postgresql keyword
         */
        ROW_COUNT("ROW_COUNT"),
        /**
         * ROW_NUMBER is postgresql keyword
         */
        ROW_NUMBER("ROW_NUMBER"),
        /**
         * RULE is postgresql keyword
         */
        RULE("RULE"),
        /**
         * SAVEPOINT is postgresql keyword
         */
        SAVEPOINT("SAVEPOINT"),
        /**
         * SCALE is postgresql keyword
         */
        SCALE("SCALE"),
        /**
         * SCHEMA is postgresql keyword
         */
        SCHEMA("SCHEMA"),
        /**
         * SCHEMAS is postgresql keyword
         */
        SCHEMAS("SCHEMAS"),
        /**
         * SCHEMA_NAME is postgresql keyword
         */
        SCHEMA_NAME("SCHEMA_NAME"),
        /**
         * SCOPE is postgresql keyword
         */
        SCOPE("SCOPE"),
        /**
         * SCOPE_CATALOG is postgresql keyword
         */
        SCOPE_CATALOG("SCOPE_CATALOG"),
        /**
         * SCOPE_NAME is postgresql keyword
         */
        SCOPE_NAME("SCOPE_NAME"),
        /**
         * SCOPE_SCHEMA is postgresql keyword
         */
        SCOPE_SCHEMA("SCOPE_SCHEMA"),
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
         * SECURITY is postgresql keyword
         */
        SECURITY("SECURITY"),
        /**
         * SELECT is postgresql keyword
         */
        SELECT("SELECT"),
        /**
         * SELECTIVE is postgresql keyword
         */
        SELECTIVE("SELECTIVE"),
        /**
         * SELF is postgresql keyword
         */
        SELF("SELF"),
        /**
         * SENSITIVE is postgresql keyword
         */
        SENSITIVE("SENSITIVE"),
        /**
         * SEQUENCE is postgresql keyword
         */
        SEQUENCE("SEQUENCE"),
        /**
         * SEQUENCES is postgresql keyword
         */
        SEQUENCES("SEQUENCES"),
        /**
         * SERIALIZABLE is postgresql keyword
         */
        SERIALIZABLE("SERIALIZABLE"),
        /**
         * SERVER is postgresql keyword
         */
        SERVER("SERVER"),
        /**
         * SERVER_NAME is postgresql keyword
         */
        SERVER_NAME("SERVER_NAME"),
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
         * SETOF is postgresql keyword
         */
        SETOF("SETOF"),
        /**
         * SETS is postgresql keyword
         */
        SETS("SETS"),
        /**
         * SHARE is postgresql keyword
         */
        SHARE("SHARE"),
        /**
         * SHOW is postgresql keyword
         */
        SHOW("SHOW"),
        /**
         * SIMILAR is postgresql keyword
         */
        SIMILAR("SIMILAR"),
        /**
         * SIMPLE is postgresql keyword
         */
        SIMPLE("SIMPLE"),
        /**
         * SIZE is postgresql keyword
         */
        SIZE("SIZE"),
        /**
         * SKIP is postgresql keyword
         */
        SKIP("SKIP"),
        /**
         * SMALLINT is postgresql keyword
         */
        SMALLINT("SMALLINT"),
        /**
         * SNAPSHOT is postgresql keyword
         */
        SNAPSHOT("SNAPSHOT"),
        /**
         * SOME is postgresql keyword
         */
        SOME("SOME"),
        /**
         * SOURCE is postgresql keyword
         */
        SOURCE("SOURCE"),
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
         * SPECIFIC_NAME is postgresql keyword
         */
        SPECIFIC_NAME("SPECIFIC_NAME"),
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
         * STABLE is postgresql keyword
         */
        STABLE("STABLE"),
        /**
         * STANDALONE is postgresql keyword
         */
        STANDALONE("STANDALONE"),
        /**
         * START is postgresql keyword
         */
        START("START"),
        /**
         * STATE is postgresql keyword
         */
        STATE("STATE"),
        /**
         * STATEMENT is postgresql keyword
         */
        STATEMENT("STATEMENT"),
        /**
         * STATIC is postgresql keyword
         */
        STATIC("STATIC"),
        /**
         * STATISTICS is postgresql keyword
         */
        STATISTICS("STATISTICS"),
        /**
         * STDDEV_POP is postgresql keyword
         */
        STDDEV_POP("STDDEV_POP"),
        /**
         * STDDEV_SAMP is postgresql keyword
         */
        STDDEV_SAMP("STDDEV_SAMP"),
        /**
         * STDIN is postgresql keyword
         */
        STDIN("STDIN"),
        /**
         * STDOUT is postgresql keyword
         */
        STDOUT("STDOUT"),
        /**
         * STORAGE is postgresql keyword
         */
        STORAGE("STORAGE"),
        /**
         * STRICT is postgresql keyword
         */
        STRICT("STRICT"),
        /**
         * STRIP is postgresql keyword
         */
        STRIP("STRIP"),
        /**
         * STRUCTURE is postgresql keyword
         */
        STRUCTURE("STRUCTURE"),
        /**
         * STYLE is postgresql keyword
         */
        STYLE("STYLE"),
        /**
         * SUBCLASS_ORIGIN is postgresql keyword
         */
        SUBCLASS_ORIGIN("SUBCLASS_ORIGIN"),
        /**
         * SUBMULTISET is postgresql keyword
         */
        SUBMULTISET("SUBMULTISET"),
        /**
         * SUBSCRIPTION is postgresql keyword
         */
        SUBSCRIPTION("SUBSCRIPTION"),
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
         * SYSID is postgresql keyword
         */
        SYSID("SYSID"),
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
         * T is postgresql keyword
         */
        T("T"),
        /**
         * TABLE is postgresql keyword
         */
        TABLE("TABLE"),
        /**
         * TABLES is postgresql keyword
         */
        TABLES("TABLES"),
        /**
         * TABLESAMPLE is postgresql keyword
         */
        TABLESAMPLE("TABLESAMPLE"),
        /**
         * TABLESPACE is postgresql keyword
         */
        TABLESPACE("TABLESPACE"),
        /**
         * TABLE_NAME is postgresql keyword
         */
        TABLE_NAME("TABLE_NAME"),
        /**
         * TEMP is postgresql keyword
         */
        TEMP("TEMP"),
        /**
         * TEMPLATE is postgresql keyword
         */
        TEMPLATE("TEMPLATE"),
        /**
         * TEMPORARY is postgresql keyword
         */
        TEMPORARY("TEMPORARY"),
        /**
         * TEXT is postgresql keyword
         */
        TEXT("TEXT"),
        /**
         * THEN is postgresql keyword
         */
        THEN("THEN"),
        /**
         * TIES is postgresql keyword
         */
        TIES("TIES"),
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
         * TOKEN is postgresql keyword
         */
        TOKEN("TOKEN"),
        /**
         * TOP_LEVEL_COUNT is postgresql keyword
         */
        TOP_LEVEL_COUNT("TOP_LEVEL_COUNT"),
        /**
         * TRAILING is postgresql keyword
         */
        TRAILING("TRAILING"),
        /**
         * TRANSACTION is postgresql keyword
         */
        TRANSACTION("TRANSACTION"),
        /**
         * TRANSACTIONS_COMMITTED is postgresql keyword
         */
        TRANSACTIONS_COMMITTED("TRANSACTIONS_COMMITTED"),
        /**
         * TRANSACTIONS_ROLLED_BACK is postgresql keyword
         */
        TRANSACTIONS_ROLLED_BACK("TRANSACTIONS_ROLLED_BACK"),
        /**
         * TRANSACTION_ACTIVE is postgresql keyword
         */
        TRANSACTION_ACTIVE("TRANSACTION_ACTIVE"),
        /**
         * TRANSFORM is postgresql keyword
         */
        TRANSFORM("TRANSFORM"),
        /**
         * TRANSFORMS is postgresql keyword
         */
        TRANSFORMS("TRANSFORMS"),
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
         * TRIGGER_CATALOG is postgresql keyword
         */
        TRIGGER_CATALOG("TRIGGER_CATALOG"),
        /**
         * TRIGGER_NAME is postgresql keyword
         */
        TRIGGER_NAME("TRIGGER_NAME"),
        /**
         * TRIGGER_SCHEMA is postgresql keyword
         */
        TRIGGER_SCHEMA("TRIGGER_SCHEMA"),
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
         * TRUSTED is postgresql keyword
         */
        TRUSTED("TRUSTED"),
        /**
         * TYPE is postgresql keyword
         */
        TYPE("TYPE"),
        /**
         * TYPES is postgresql keyword
         */
        TYPES("TYPES"),
        /**
         * UESCAPE is postgresql keyword
         */
        UESCAPE("UESCAPE"),
        /**
         * UNBOUNDED is postgresql keyword
         */
        UNBOUNDED("UNBOUNDED"),
        /**
         * UNCOMMITTED is postgresql keyword
         */
        UNCOMMITTED("UNCOMMITTED"),
        /**
         * UNDER is postgresql keyword
         */
        UNDER("UNDER"),
        /**
         * UNENCRYPTED is postgresql keyword
         */
        UNENCRYPTED("UNENCRYPTED"),
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
         * UNLINK is postgresql keyword
         */
        UNLINK("UNLINK"),
        /**
         * UNLISTEN is postgresql keyword
         */
        UNLISTEN("UNLISTEN"),
        /**
         * UNLOGGED is postgresql keyword
         */
        UNLOGGED("UNLOGGED"),
        /**
         * UNNAMED is postgresql keyword
         */
        UNNAMED("UNNAMED"),
        /**
         * UNNEST is postgresql keyword
         */
        UNNEST("UNNEST"),
        /**
         * UNTIL is postgresql keyword
         */
        UNTIL("UNTIL"),
        /**
         * UNTYPED is postgresql keyword
         */
        UNTYPED("UNTYPED"),
        /**
         * UPDATE is postgresql keyword
         */
        UPDATE("UPDATE"),
        /**
         * UPPER is postgresql keyword
         */
        UPPER("UPPER"),
        /**
         * URI is postgresql keyword
         */
        URI("URI"),
        /**
         * USAGE is postgresql keyword
         */
        USAGE("USAGE"),
        /**
         * USER is postgresql keyword
         */
        USER("USER"),
        /**
         * USER_DEFINED_TYPE_CATALOG is postgresql keyword
         */
        USER_DEFINED_TYPE_CATALOG("USER_DEFINED_TYPE_CATALOG"),
        /**
         * USER_DEFINED_TYPE_CODE is postgresql keyword
         */
        USER_DEFINED_TYPE_CODE("USER_DEFINED_TYPE_CODE"),
        /**
         * USER_DEFINED_TYPE_NAME is postgresql keyword
         */
        USER_DEFINED_TYPE_NAME("USER_DEFINED_TYPE_NAME"),
        /**
         * USER_DEFINED_TYPE_SCHEMA is postgresql keyword
         */
        USER_DEFINED_TYPE_SCHEMA("USER_DEFINED_TYPE_SCHEMA"),
        /**
         * USING is postgresql keyword
         */
        USING("USING"),
        /**
         * VACUUM is postgresql keyword
         */
        VACUUM("VACUUM"),
        /**
         * VALID is postgresql keyword
         */
        VALID("VALID"),
        /**
         * VALIDATE is postgresql keyword
         */
        VALIDATE("VALIDATE"),
        /**
         * VALIDATOR is postgresql keyword
         */
        VALIDATOR("VALIDATOR"),
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
         * VERSION is postgresql keyword
         */
        VERSION("VERSION"),
        /**
         * VERSIONING is postgresql keyword
         */
        VERSIONING("VERSIONING"),
        /**
         * VIEW is postgresql keyword
         */
        VIEW("VIEW"),
        /**
         * VIEWS is postgresql keyword
         */
        VIEWS("VIEWS"),
        /**
         * VOLATILE is postgresql keyword
         */
        VOLATILE("VOLATILE"),
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
         * WHITESPACE is postgresql keyword
         */
        WHITESPACE("WHITESPACE"),
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
         * WRAPPER is postgresql keyword
         */
        WRAPPER("WRAPPER"),
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
         * XMLDECLARATION is postgresql keyword
         */
        XMLDECLARATION("XMLDECLARATION"),
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
         * XMLROOT is postgresql keyword
         */
        XMLROOT("XMLROOT"),
        /**
         * XMLSCHEMA is postgresql keyword
         */
        XMLSCHEMA("XMLSCHEMA"),
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
         * YES is postgresql keyword
         */
        YES("YES"),
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
