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
package org.apache.seata.rm.datasource.sql.handler.db2;

import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.sqlparser.EscapeHandler;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.apache.seata.sqlparser.util.JdbcConstants;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * The type DB2 keyword checker.
 *
 * @author GoodBoyCoder
 */
@LoadLevel(name = JdbcConstants.DB2)
public class DB2EscapeHandler implements EscapeHandler {
    private Set<String> keywordSet = Arrays.stream(DB2EscapeHandler.DB2Keyword.values()).map(DB2EscapeHandler.DB2Keyword::name).collect(Collectors.toSet());

    /**
     * db2 keyword
     */
    private enum DB2Keyword {

        /**
         * ADD is db2 keyword
         */
        ADD("ADD"),
        /**
         * AFTER is db2 keyword
         */
        AFTER("AFTER"),
        /**
         * ALIAS is db2 keyword
         */
        ALIAS("ALIAS"),
        /**
         * ALL is db2 keyword
         */
        ALL("ALL"),
        /**
         * ALLOCATE is db2 keyword
         */
        ALLOCATE("ALLOCATE"),
        /**
         * ALLOW is db2 keyword
         */
        ALLOW("ALLOW"),
        /**
         * ALTER is db2 keyword
         */
        ALTER("ALTER"),
        /**
         * AND is db2 keyword
         */
        AND("AND"),
        /**
         * ANY is db2 keyword
         */
        ANY("ANY"),
        /**
         * APPLICATION is db2 keyword
         */
        APPLICATION("APPLICATION"),
        /**
         * AS is db2 keyword
         */
        AS("AS"),
        /**
         * ASSOCIATE is db2 keyword
         */
        ASSOCIATE("ASSOCIATE"),
        /**
         * ASUTIME is db2 keyword
         */
        ASUTIME("ASUTIME"),
        /**
         * AUDIT is db2 keyword
         */
        AUDIT("AUDIT"),
        /**
         * AUTHORIZATION is db2 keyword
         */
        AUTHORIZATION("AUTHORIZATION"),
        /**
         * AUX is db2 keyword
         */
        AUX("AUX"),
        /**
         * AUXILIARY is db2 keyword
         */
        AUXILIARY("AUXILIARY"),
        /**
         * BEFORE is db2 keyword
         */
        BEFORE("BEFORE"),
        /**
         * BEGIN is db2 keyword
         */
        BEGIN("BEGIN"),
        /**
         * BETWEEN is db2 keyword
         */
        BETWEEN("BETWEEN"),
        /**
         * BINARY is db2 keyword
         */
        BINARY("BINARY"),
        /**
         * BUFFERPOOL is db2 keyword
         */
        BUFFERPOOL("BUFFERPOOL"),
        /**
         * BY is db2 keyword
         */
        BY("BY"),
        /**
         * CACHE is db2 keyword
         */
        CACHE("CACHE"),
        /**
         * CALL is db2 keyword
         */
        CALL("CALL"),
        /**
         * CALLED is db2 keyword
         */
        CALLED("CALLED"),
        /**
         * CAPTURE is db2 keyword
         */
        CAPTURE("CAPTURE"),
        /**
         * CARDINALITY is db2 keyword
         */
        CARDINALITY("CARDINALITY"),
        /**
         * CASCADED is db2 keyword
         */
        CASCADED("CASCADED"),
        /**
         * CASE is db2 keyword
         */
        CASE("CASE"),
        /**
         * CAST is db2 keyword
         */
        CAST("CAST"),
        /**
         * CCSID is db2 keyword
         */
        CCSID("CCSID"),
        /**
         * CHAR is db2 keyword
         */
        CHAR("CHAR"),
        /**
         * CHARACTER is db2 keyword
         */
        CHARACTER("CHARACTER"),
        /**
         * CHECK is db2 keyword
         */
        CHECK("CHECK"),
        /**
         * CLOSE is db2 keyword
         */
        CLOSE("CLOSE"),
        /**
         * CLUSTER is db2 keyword
         */
        CLUSTER("CLUSTER"),
        /**
         * COLLECTION is db2 keyword
         */
        COLLECTION("COLLECTION"),
        /**
         * COLLID is db2 keyword
         */
        COLLID("COLLID"),
        /**
         * COLUMN is db2 keyword
         */
        COLUMN("COLUMN"),
        /**
         * COMMENT is db2 keyword
         */
        COMMENT("COMMENT"),
        /**
         * COMMIT is db2 keyword
         */
        COMMIT("COMMIT"),
        /**
         * CONCAT is db2 keyword
         */
        CONCAT("CONCAT"),
        /**
         * CONDITION is db2 keyword
         */
        CONDITION("CONDITION"),
        /**
         * CONNECT is db2 keyword
         */
        CONNECT("CONNECT"),
        /**
         * CONNECTION is db2 keyword
         */
        CONNECTION("CONNECTION"),
        /**
         * CONSTRAINT is db2 keyword
         */
        CONSTRAINT("CONSTRAINT"),
        /**
         * CONTAINS is db2 keyword
         */
        CONTAINS("CONTAINS"),
        /**
         * CONTINUE is db2 keyword
         */
        CONTINUE("CONTINUE"),
        /**
         * COUNT is db2 keyword
         */
        COUNT("COUNT"),
        /**
         * COUNT_BIG is db2 keyword
         */
        COUNT_BIG("COUNT_BIG"),
        /**
         * CREATE is db2 keyword
         */
        CREATE("CREATE"),
        /**
         * CROSS is db2 keyword
         */
        CROSS("CROSS"),
        /**
         * CURRENT is db2 keyword
         */
        CURRENT("CURRENT"),
        /**
         * CURRENT_DATE is db2 keyword
         */
        CURRENT_DATE("CURRENT_DATE"),
        /**
         * CURRENT_LC_CTYPE is db2 keyword
         */
        CURRENT_LC_CTYPE("CURRENT_LC_CTYPE"),
        /**
         * CURRENT_PATH is db2 keyword
         */
        CURRENT_PATH("CURRENT_PATH"),
        /**
         * CURRENT_SERVER is db2 keyword
         */
        CURRENT_SERVER("CURRENT_SERVER"),
        /**
         * CURRENT_TIME is db2 keyword
         */
        CURRENT_TIME("CURRENT_TIME"),
        /**
         * CURRENT_TIMESTAMP is db2 keyword
         */
        CURRENT_TIMESTAMP("CURRENT_TIMESTAMP"),
        /**
         * CURRENT_TIMEZONE is db2 keyword
         */
        CURRENT_TIMEZONE("CURRENT_TIMEZONE"),
        /**
         * CURRENT_USER is db2 keyword
         */
        CURRENT_USER("CURRENT_USER"),
        /**
         * CURSOR is db2 keyword
         */
        CURSOR("CURSOR"),
        /**
         * CYCLE is db2 keyword
         */
        CYCLE("CYCLE"),
        /**
         * DATA is db2 keyword
         */
        DATA("DATA"),
        /**
         * DATABASE is db2 keyword
         */
        DATABASE("DATABASE"),
        /**
         * DAY is db2 keyword
         */
        DAY("DAY"),
        /**
         * DAYS is db2 keyword
         */
        DAYS("DAYS"),
        /**
         * DB2GENERAL is db2 keyword
         */
        DB2GENERAL("DB2GENERAL"),
        /**
         * DB2GENRL is db2 keyword
         */
        DB2GENRL("DB2GENRL"),
        /**
         * DB2SQL is db2 keyword
         */
        DB2SQL("DB2SQL"),
        /**
         * DBINFO is db2 keyword
         */
        DBINFO("DBINFO"),
        /**
         * DECLARE is db2 keyword
         */
        DECLARE("DECLARE"),
        /**
         * DEFAULT is db2 keyword
         */
        DEFAULT("DEFAULT"),
        /**
         * DEFAULTS is db2 keyword
         */
        DEFAULTS("DEFAULTS"),
        /**
         * DEFINITION is db2 keyword
         */
        DEFINITION("DEFINITION"),
        /**
         * DELETE is db2 keyword
         */
        DELETE("DELETE"),
        /**
         * DESCRIPTOR is db2 keyword
         */
        DESCRIPTOR("DESCRIPTOR"),
        /**
         * DETERMINISTIC is db2 keyword
         */
        DETERMINISTIC("DETERMINISTIC"),
        /**
         * DISALLOW is db2 keyword
         */
        DISALLOW("DISALLOW"),
        /**
         * DISCONNECT is db2 keyword
         */
        DISCONNECT("DISCONNECT"),
        /**
         * DISTINCT is db2 keyword
         */
        DISTINCT("DISTINCT"),
        /**
         * DO is db2 keyword
         */
        DO("DO"),
        /**
         * ANY is db2 keyword
         */
        DOUBLE("DOUBLE"),
        /**
         * DROP is db2 keyword
         */
        DROP("DROP"),
        /**
         * DSNHATTR is db2 keyword
         */
        DSNHATTR("DSNHATTR"),
        /**
         * DSSIZE is db2 keyword
         */
        DSSIZE("DSSIZE"),
        /**
         * DYNAMIC is db2 keyword
         */
        DYNAMIC("DYNAMIC"),
        /**
         * EACH is db2 keyword
         */
        EACH("EACH"),
        /**
         * EDITPROC is db2 keyword
         */
        EDITPROC("EDITPROC"),
        /**
         * ELSE is db2 keyword
         */
        ELSE("ELSE"),
        /**
         * ELSEIF is db2 keyword
         */
        ELSEIF("ELSEIF"),
        /**
         * ENCODING is db2 keyword
         */
        ENCODING("ENCODING"),
        /**
         * END is db2 keyword
         */
        END("END"),
//        /**
//         * END-EXEC is db2 keyword
//         */
//        END-EXEC("END-EXEC"),
//        /**
//         * END-EXEC1 is db2 keyword
//         */
//        END-EXEC1("END-EXEC1"),
        /**
         * ERASE is db2 keyword
         */
        ERASE("ERASE"),
        /**
         * ESCAPE is db2 keyword
         */
        ESCAPE("ESCAPE"),
        /**
         * EXCEPT is db2 keyword
         */
        EXCEPT("EXCEPT"),
        /**
         * EXCEPTION is db2 keyword
         */
        EXCEPTION("EXCEPTION"),
        /**
         * EXCLUDING is db2 keyword
         */
        EXCLUDING("EXCLUDING"),
        /**
         * EXECUTE is db2 keyword
         */
        EXECUTE("EXECUTE"),
        /**
         * EXISTS is db2 keyword
         */
        EXISTS("EXISTS"),
        /**
         * EXIT is db2 keyword
         */
        EXIT("EXIT"),
        /**
         * EXTERNAL is db2 keyword
         */
        EXTERNAL("EXTERNAL"),
        /**
         * FENCED is db2 keyword
         */
        FENCED("FENCED"),
        /**
         * FETCH is db2 keyword
         */
        FETCH("FETCH"),
        /**
         * FIELDPROC is db2 keyword
         */
        FIELDPROC("FIELDPROC"),
        /**
         * FILE is db2 keyword
         */
        FILE("FILE"),
        /**
         * FINAL is db2 keyword
         */
        FINAL("FINAL"),
        /**
         * FOR is db2 keyword
         */
        FOR("FOR"),
        /**
         * FOREIGN is db2 keyword
         */
        FOREIGN("FOREIGN"),
        /**
         * FREE is db2 keyword
         */
        FREE("FREE"),
        /**
         * FROM is db2 keyword
         */
        FROM("FROM"),
        /**
         * FULL is db2 keyword
         */
        FULL("FULL"),
        /**
         * FUNCTION is db2 keyword
         */
        FUNCTION("FUNCTION"),
        /**
         * GENERAL is db2 keyword
         */
        GENERAL("GENERAL"),
        /**
         * GENERATED is db2 keyword
         */
        GENERATED("GENERATED"),
        /**
         * GET is db2 keyword
         */
        GET("GET"),
        /**
         * GLOBAL is db2 keyword
         */
        GLOBAL("GLOBAL"),
        /**
         * GO is db2 keyword
         */
        GO("GO"),
        /**
         * GOTO is db2 keyword
         */
        GOTO("GOTO"),
        /**
         * GRANT is db2 keyword
         */
        GRANT("GRANT"),
        /**
         * GRAPHIC is db2 keyword
         */
        GRAPHIC("GRAPHIC"),
        /**
         * GROUP is db2 keyword
         */
        GROUP("GROUP"),
        /**
         * HANDLER is db2 keyword
         */
        HANDLER("HANDLER"),
        /**
         * HAVING is db2 keyword
         */
        HAVING("HAVING"),
        /**
         * HOLD is db2 keyword
         */
        HOLD("HOLD"),
        /**
         * HOUR is db2 keyword
         */
        HOUR("HOUR"),
        /**
         * HOURS is db2 keyword
         */
        HOURS("HOURS"),
        /**
         * LEAVE is db2 keyword
         */
        LEAVE("LEAVE"),
        /**
         * LEFT is db2 keyword
         */
        LEFT("LEFT"),
        /**
         * LIKE is db2 keyword
         */
        LIKE("LIKE"),
        /**
         * LINKTYPE is db2 keyword
         */
        LINKTYPE("LINKTYPE"),
        /**
         * LOCAL is db2 keyword
         */
        LOCAL("LOCAL"),
        /**
         * LOCALE is db2 keyword
         */
        LOCALE("LOCALE"),
        /**
         * LOCATOR is db2 keyword
         */
        LOCATOR("LOCATOR"),
        /**
         * LOCATORS is db2 keyword
         */
        LOCATORS("LOCATORS"),
        /**
         * LOCK is db2 keyword
         */
        LOCK("LOCK"),
        /**
         * LOCKMAX is db2 keyword
         */
        LOCKMAX("LOCKMAX"),
        /**
         * LOCKSIZE is db2 keyword
         */
        LOCKSIZE("LOCKSIZE"),
        /**
         * LONG is db2 keyword
         */
        LONG("LONG"),
        /**
         * LOOP is db2 keyword
         */
        LOOP("LOOP"),
        /**
         * MAXVALUE is db2 keyword
         */
        MAXVALUE("MAXVALUE"),
        /**
         * MICROSECOND is db2 keyword
         */
        MICROSECOND("MICROSECOND"),
        /**
         * MICROSECONDS is db2 keyword
         */
        MICROSECONDS("MICROSECONDS"),
        /**
         * MINUTE is db2 keyword
         */
        MINUTE("MINUTE"),
        /**
         * MINUTES is db2 keyword
         */
        MINUTES("MINUTES"),
        /**
         * MINVALUE is db2 keyword
         */
        MINVALUE("MINVALUE"),
        /**
         * MODE is db2 keyword
         */
        MODE("MODE"),
        /**
         * MODIFIES is db2 keyword
         */
        MODIFIES("MODIFIES"),
        /**
         * MONTH is db2 keyword
         */
        MONTH("MONTH"),
        /**
         * MONTHS is db2 keyword
         */
        MONTHS("MONTHS"),
        /**
         * NEW is db2 keyword
         */
        NEW("NEW"),
        /**
         * NEW_TABLE is db2 keyword
         */
        NEW_TABLE("NEW_TABLE"),
        /**
         * NO is db2 keyword
         */
        NO("NO"),
        /**
         * NOCACHE is db2 keyword
         */
        NOCACHE("NOCACHE"),
        /**
         * NOCYCLE is db2 keyword
         */
        NOCYCLE("NOCYCLE"),
        /**
         * NODENAME is db2 keyword
         */
        NODENAME("NODENAME"),
        /**
         * NODENUMBER is db2 keyword
         */
        NODENUMBER("NODENUMBER"),
        /**
         * NOMAXVALUE is db2 keyword
         */
        NOMAXVALUE("NOMAXVALUE"),
        /**
         * NOMINVALUE is db2 keyword
         */
        NOMINVALUE("NOMINVALUE"),
        /**
         * NOORDER is db2 keyword
         */
        NOORDER("NOORDER"),
        /**
         * NOT is db2 keyword
         */
        NOT("NOT"),
        /**
         * NULL is db2 keyword
         */
        NULL("NULL"),
        /**
         * NULLS is db2 keyword
         */
        NULLS("NULLS"),
        /**
         * NUMPARTS is db2 keyword
         */
        NUMPARTS("NUMPARTS"),
        /**
         * OBID is db2 keyword
         */
        OBID("OBID"),
        /**
         * OF is db2 keyword
         */
        OF("OF"),
        /**
         * OLD is db2 keyword
         */
        OLD("OLD"),
        /**
         * OLD_TABLE is db2 keyword
         */
        OLD_TABLE("OLD_TABLE"),
        /**
         * ON is db2 keyword
         */
        ON("ON"),
        /**
         * OPEN is db2 keyword
         */
        OPEN("OPEN"),
        /**
         * OPTIMIZATION is db2 keyword
         */
        OPTIMIZATION("OPTIMIZATION"),
        /**
         * OPTIMIZE is db2 keyword
         */
        OPTIMIZE("OPTIMIZE"),
        /**
         * OPTION is db2 keyword
         */
        OPTION("OPTION"),
        /**
         * OR is db2 keyword
         */
        OR("OR"),
        /**
         * ORDER is db2 keyword
         */
        ORDER("ORDER"),
        /**
         * OUT is db2 keyword
         */
        OUT("OUT"),
        /**
         * OUTER is db2 keyword
         */
        OUTER("OUTER"),
        /**
         * OVERRIDING is db2 keyword
         */
        OVERRIDING("OVERRIDING"),
        /**
         * PACKAGE is db2 keyword
         */
        PACKAGE("PACKAGE"),
        /**
         * PARAMETER is db2 keyword
         */
        PARAMETER("PARAMETER"),
        /**
         * PART is db2 keyword
         */
        PART("PART"),
        /**
         * PARTITION is db2 keyword
         */
        PARTITION("PARTITION"),
        /**
         * PATH is db2 keyword
         */
        PATH("PATH"),
        /**
         * PIECESIZE is db2 keyword
         */
        PIECESIZE("PIECESIZE"),
        /**
         * PLAN is db2 keyword
         */
        PLAN("PLAN"),
        /**
         * POSITION is db2 keyword
         */
        POSITION("POSITION"),
        /**
         * PRECISION is db2 keyword
         */
        PRECISION("PRECISION"),
        /**
         * PREPARE is db2 keyword
         */
        PREPARE("PREPARE"),
        /**
         * PRIMARY is db2 keyword
         */
        PRIMARY("PRIMARY"),
        /**
         * PRIQTY is db2 keyword
         */
        PRIQTY("PRIQTY"),
        /**
         * PRIVILEGES is db2 keyword
         */
        PRIVILEGES("PRIVILEGES"),
        /**
         * PROCEDURE is db2 keyword
         */
        PROCEDURE("PROCEDURE"),
        /**
         * PROGRAM is db2 keyword
         */
        PROGRAM("PROGRAM"),
        /**
         * PSID is db2 keyword
         */
        PSID("PSID"),
        /**
         * QUERYNO is db2 keyword
         */
        QUERYNO("QUERYNO"),
        /**
         * READ is db2 keyword
         */
        READ("READ"),
        /**
         * READS is db2 keyword
         */
        READS("READS"),
        /**
         * RECOVERY is db2 keyword
         */
        RECOVERY("RECOVERY"),
        /**
         * REFERENCES is db2 keyword
         */
        REFERENCES("REFERENCES"),
        /**
         * REFERENCING is db2 keyword
         */
        REFERENCING("REFERENCING"),
        /**
         * RELEASE is db2 keyword
         */
        RELEASE("RELEASE"),
        /**
         * RENAME is db2 keyword
         */
        RENAME("RENAME"),
        /**
         * REPEAT is db2 keyword
         */
        REPEAT("REPEAT"),
        /**
         * RESET is db2 keyword
         */
        RESET("RESET"),
        /**
         * RESIGNAL is db2 keyword
         */
        RESIGNAL("RESIGNAL"),
        /**
         * RESTART is db2 keyword
         */
        RESTART("RESTART"),
        /**
         * RESTRICT is db2 keyword
         */
        RESTRICT("RESTRICT"),
        /**
         * RESULT is db2 keyword
         */
        RESULT("RESULT"),
        /**
         * RESULT_SET_LOCATOR is db2 keyword
         */
        RESULT_SET_LOCATOR("RESULT_SET_LOCATOR"),
        /**
         * RETURN is db2 keyword
         */
        RETURN("RETURN"),
        /**
         * RETURNS is db2 keyword
         */
        RETURNS("RETURNS"),
        /**
         * REVOKE is db2 keyword
         */
        REVOKE("REVOKE"),
        /**
         * RIGHT is db2 keyword
         */
        RIGHT("RIGHT"),
        /**
         * ROLLBACK is db2 keyword
         */
        ROLLBACK("ROLLBACK"),
        /**
         * ROUTINE is db2 keyword
         */
        ROUTINE("ROUTINE"),
        /**
         * ROW is db2 keyword
         */
        ROW("ROW"),
        /**
         * ROWS is db2 keyword
         */
        ROWS("ROWS"),
        /**
         * RRN is db2 keyword
         */
        RRN("RRN"),
        /**
         * RUN is db2 keyword
         */
        RUN("RUN"),
        /**
         * SAVEPOINT is db2 keyword
         */
        SAVEPOINT("SAVEPOINT"),
        /**
         * SCHEMA is db2 keyword
         */
        SCHEMA("SCHEMA"),
        /**
         * SCRATCHPAD is db2 keyword
         */
        SCRATCHPAD("SCRATCHPAD"),
        /**
         * SECOND is db2 keyword
         */
        SECOND("SECOND"),
        /**
         * SECONDS is db2 keyword
         */
        SECONDS("SECONDS"),
        /**
         * SECQTY is db2 keyword
         */
        SECQTY("SECQTY"),
        /**
         * SECURITY is db2 keyword
         */
        SECURITY("SECURITY"),
        /**
         * SELECT is db2 keyword
         */
        SELECT("SELECT"),
        /**
         * SENSITIVE is db2 keyword
         */
        SENSITIVE("SENSITIVE"),
        /**
         * SET is db2 keyword
         */
        SET("SET"),
        /**
         * SIGNAL is db2 keyword
         */
        SIGNAL("SIGNAL"),
        /**
         * SIMPLE is db2 keyword
         */
        SIMPLE("SIMPLE"),
        /**
         * SOME is db2 keyword
         */
        SOME("SOME"),
        /**
         * SOURCE is db2 keyword
         */
        SOURCE("SOURCE"),
        /**
         * SPECIFIC is db2 keyword
         */
        SPECIFIC("SPECIFIC"),
        /**
         * SQL is db2 keyword
         */
        SQL("SQL"),
        /**
         * SQLID is db2 keyword
         */
        SQLID("SQLID"),
        /**
         * STANDARD is db2 keyword
         */
        STANDARD("STANDARD"),
        /**
         * START is db2 keyword
         */
        START("START"),
        /**
         * STATIC is db2 keyword
         */
        STATIC("STATIC"),
        /**
         * STAY is db2 keyword
         */
        STAY("STAY"),
        /**
         * STOGROUP is db2 keyword
         */
        STOGROUP("STOGROUP"),
        /**
         * STORES is db2 keyword
         */
        STORES("STORES"),
        /**
         * STYLE is db2 keyword
         */
        STYLE("STYLE"),
        /**
         * SUBPAGES is db2 keyword
         */
        SUBPAGES("SUBPAGES"),
        /**
         * SUBSTRING is db2 keyword
         */
        SUBSTRING("SUBSTRING"),
        /**
         * SYNONYM is db2 keyword
         */
        SYNONYM("SYNONYM"),
        /**
         * SYSFUN is db2 keyword
         */
        SYSFUN("SYSFUN"),
        /**
         * SYSIBM is db2 keyword
         */
        SYSIBM("SYSIBM"),
        /**
         * SYSPROC is db2 keyword
         */
        SYSPROC("SYSPROC"),
        /**
         * SYSTEM  is db2 keyword
         */
        SYSTEM("SYSTEM "),
        /**
         * TABLE is db2 keyword
         */
        TABLE("TABLE"),
        /**
         * TABLESPACE is db2 keyword
         */
        TABLESPACE("TABLESPACE"),
        /**
         * THEN is db2 keyword
         */
        THEN("THEN"),
        /**
         * TO is db2 keyword
         */
        TO("TO"),
        /**
         * TRANSACTION is db2 keyword
         */
        TRANSACTION("TRANSACTION"),
        /**
         * TRIGGER is db2 keyword
         */
        TRIGGER("TRIGGER"),
        /**
         * TRIM  is db2 keyword
         */
        TRIM("TRIM"),
        /**
         * TYPE is db2 keyword
         */
        TYPE("TYPE"),
        /**
         * UNDO is db2 keyword
         */
        UNDO("UNDO"),
        /**
         * UNION is db2 keyword
         */
        UNION("UNION"),
        /**
         * UNIQUE is db2 keyword
         */
        UNIQUE("UNIQUE"),
        /**
         * UNTIL is db2 keyword
         */
        UNTIL("UNTIL"),
        /**
         * UPDATE is db2 keyword
         */
        UPDATE("UPDATE"),
        /**
         * USAGE is db2 keyword
         */
        USAGE("USAGE"),
        /**
         * USER is db2 keyword
         */
        USER("USER"),
        /**
         * USING is db2 keyword
         */
        USING("USING"),
        /**
         * VALIDPROC is db2 keyword
         */
        VALIDPROC("VALIDPROC"),
        /**
         * VALUES is db2 keyword
         */
        VALUES("VALUES"),
        /**
         * VARIABLE is db2 keyword
         */
        VARIABLE("VARIABLE"),
        /**
         * VARIANT is db2 keyword
         */
        VARIANT("VARIANT"),
        /**
         * VCAT is db2 keyword
         */
        VCAT("VCAT"),
        /**
         * VIEW is db2 keyword
         */
        VIEW("VIEW"),
        /**
         * VOLUMES is db2 keyword
         */
        VOLUMES("VOLUMES"),
        /**
         * WHEN is db2 keyword
         */
        WHEN("WHEN"),
        /**
         * WHERE is db2 keyword
         */
        WHERE("WHERE"),
        /**
         * WHILE is db2 keyword
         */
        WHILE("WHILE"),
        /**
         * WITH is db2 keyword
         */
        WITH("WITH"),
        /**
         * WLM is db2 keyword
         */
        WLM("WLM"),
        /**
         * WRITE is db2 keyword
         */
        WRITE("WRITE"),
        /**
         * YEAR is db2 keyword
         */
        YEAR("YEAR"),
        /**
         * YEARS is db2 keyword
         */
        YEARS("YEARS");


        /**
         * The Name.
         */
        public final String name;

        DB2Keyword(String name) {
            this.name = name;
        }
    }
    @Override
    public boolean checkIfKeyWords(String fieldOrTableName) {
        if (keywordSet.contains(fieldOrTableName)) {
            return true;
        }
        if (fieldOrTableName != null) {
            fieldOrTableName = fieldOrTableName.toUpperCase();
        }
        return keywordSet.contains(fieldOrTableName);
    }

    @Override
    public boolean checkIfNeedEscape(String columnName, TableMeta tableMeta) {
        return checkIfKeyWords(columnName);
    }
}
