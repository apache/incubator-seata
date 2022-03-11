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
package io.seata.rm.datasource.undo.dm.keyword;

import io.seata.common.loader.LoadLevel;
import io.seata.rm.datasource.undo.KeywordChecker;
import io.seata.sqlparser.util.JdbcConstants;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The type dm sql keyword checker.
 *
 * @author chengxiaoxiao
 */
@LoadLevel(name = JdbcConstants.DM)
public class DmKeywordChecker implements KeywordChecker {

    private Set<String> keywordSet = Arrays.stream(DmKeyword.values()).map(DmKeyword::name).collect(Collectors.toSet());

    /**
     * dm keyword
     */
    private enum DmKeyword {

        /**
         * ABORT is dm keyword
         */
        ABORT("ABORT"),
        /**
         * ABSOLUTE is dm keyword
         */
        ABSOLUTE("ABSOLUTE"),
        /**
         * ABSTRACT is dm keyword
         */
        ABSTRACT("ABSTRACT"),
        /**
         * ACCESSED is dm keyword
         */
        ACCESSED("ACCESSED"),
        /**
         * ACCOUNT is dm keyword
         */
        ACCOUNT("ACCOUNT"),
        /**
         * ACROSS is dm keyword
         */
        ACROSS("ACROSS"),
        /**
         * ACTION is dm keyword
         */
        ACTION("ACTION"),
        /**
         * ADD is dm keyword
         */
        ADD("ADD"),
        /**
         * ADMIN is dm keyword
         */
        ADMIN("ADMIN"),
        /**
         * AFTER is dm keyword
         */
        AFTER("AFTER"),
        /**
         * AGGREGATE is dm keyword
         */
        AGGREGATE("AGGREGATE"),
        /**
         * ALL is dm keyword
         */
        ALL("ALL"),
        /**
         * ALLOW_DATETIME is dm keyword
         */
        ALLOW_DATETIME("ALLOW_DATETIME"),
        /**
         * ALLOW_IP is dm keyword
         */
        ALLOW_IP("ALLOW_IP"),
        /**
         * ALTER is dm keyword
         */
        ALTER("ALTER"),
        /**
         * ANALYZE is dm keyword
         */
        ANALYZE("ANALYZE"),
        /**
         * AND is dm keyword
         */
        AND("AND"),
        /**
         * ANY is dm keyword
         */
        ANY("ANY"),
        /**
         * ARCHIVE is dm keyword
         */
        ARCHIVE("ARCHIVE"),
        /**
         * ARCHIVEDIR is dm keyword
         */
        ARCHIVEDIR("ARCHIVEDIR"),
        /**
         * ARCHIVELOG is dm keyword
         */
        ARCHIVELOG("ARCHIVELOG"),
        /**
         * ARCHIVESTYLE is dm keyword
         */
        ARCHIVESTYLE("ARCHIVESTYLE"),
        /**
         * ARRAY is dm keyword
         */
        ARRAY("ARRAY"),
        /**
         * ARRAYLEN is dm keyword
         */
        ARRAYLEN("ARRAYLEN"),
        /**
         * AS is dm keyword
         */
        AS("AS"),
        /**
         * ASC is dm keyword
         */
        ASC("ASC"),
        /**
         * ASENSITIVE is dm keyword
         */
        ASENSITIVE("ASENSITIVE"),
        /**
         * ASSIGN is dm keyword
         */
        ASSIGN("ASSIGN"),
        /**
         * ASYNCHRONOUS is dm keyword
         */
        ASYNCHRONOUS("ASYNCHRONOUS"),
        /**
         * AT is dm keyword
         */
        AT("AT"),
        /**
         * ATTACH is dm keyword
         */
        ATTACH("ATTACH"),
        /**
         * AUDIT is dm keyword
         */
        AUDIT("AUDIT"),
        /**
         * AUTHID is dm keyword
         */
        AUTHID("AUTHID"),
        /**
         * AUTHORIZATION is dm keyword
         */
        AUTHORIZATION("AUTHORIZATION"),
        /**
         * AUTO is dm keyword
         */
        AUTO("AUTO"),
        /**
         * AUTOEXTEND is dm keyword
         */
        AUTOEXTEND("AUTOEXTEND"),
        /**
         * AUTONOMOUS_TRANSACTION is dm keyword
         */
        AUTONOMOUS_TRANSACTION("AUTONOMOUS_TRANSACTION"),
        /**
         * AVG is dm keyword
         */
        AVG("AVG"),
        /**
         * BACKED is dm keyword
         */
        BACKED("BACKED"),
        /**
         * BACKUP is dm keyword
         */
        BACKUP("BACKUP"),
        /**
         * BACKUPDIR is dm keyword
         */
        BACKUPDIR("BACKUPDIR"),
        /**
         * BACKUPINFO is dm keyword
         */
        BACKUPINFO("BACKUPINFO"),
        /**
         * BACKSET is dm keyword
         */
        BACKSET("BACKSET"),
        /**
         * BADFILE is dm keyword
         */
        BADFILE("BADFILE"),
        /**
         * BAKFILE is dm keyword
         */
        BAKFILE("BAKFILE"),
        /**
         * BASE is dm keyword
         */
        BASE("BASE"),
        /**
         * BEFORE is dm keyword
         */
        BEFORE("BEFORE"),
        /**
         * BEGIN is dm keyword
         */
        BEGIN("BEGIN"),
        /**
         * BETWEEN is dm keyword
         */
        BETWEEN("BETWEEN"),
        /**
         * BIGDATEDIFF is dm keyword
         */
        BIGDATEDIFF("BIGDATEDIFF"),
        /**
         * BIGINT is dm keyword
         */
        BIGINT("BIGINT"),
        /**
         * BINARY is dm keyword
         */
        BINARY("BINARY"),
        /**
         * BIT is dm keyword
         */
        BIT("BIT"),
        /**
         * BITMAP is dm keyword
         */
        BITMAP("BITMAP"),
        /**
         * BLOB is dm keyword
         */
        BLOB("BLOB"),
        /**
         * BLOCK is dm keyword
         */
        BLOCK("BLOCK"),
        /**
         * BOOL is dm keyword
         */
        BOOL("BOOL"),
        /**
         * BOOLEAN is dm keyword
         */
        BOOLEAN("BOOLEAN"),
        /**
         * BOTH is dm keyword
         */
        BOTH("BOTH"),
        /**
         * BRANCH is dm keyword
         */
        BRANCH("BRANCH"),
        /**
         * BREAK is dm keyword
         */
        BREAK("BREAK"),
        /**
         * BSTRING is dm keyword
         */
        BSTRING("BSTRING"),
        /**
         * BTREE is dm keyword
         */
        BTREE("BTREE"),
        /**
         * BUFFER is dm keyword
         */
        BUFFER("BUFFER"),
        /**
         * BUILD is dm keyword
         */
        BUILD("BUILD"),
        /**
         * BULK is dm keyword
         */
        BULK("BULK"),
        /**
         * BY is dm keyword
         */
        BY("BY"),
        /**
         * BYTE is dm keyword
         */
        BYTE("BYTE"),
        /**
         * C is dm keyword
         */
        C("C"),
        /**
         * CACHE is dm keyword
         */
        CACHE("CACHE"),
        /**
         * CALCULATE is dm keyword
         */
        CALCULATE("CALCULATE"),
        /**
         * CALL is dm keyword
         */
        CALL("CALL"),
        /**
         * CASCADE is dm keyword
         */
        CASCADE("CASCADE"),
        /**
         * CASCADED is dm keyword
         */
        CASCADED("CASCADED"),
        /**
         * CASE is dm keyword
         */
        CASE("CASE"),
        /**
         * CAST is dm keyword
         */
        CAST("CAST"),
        /**
         * CATALOG is dm keyword
         */
        CATALOG("CATALOG"),
        /**
         * CATCH is dm keyword
         */
        CATCH("CATCH"),
        /**
         * CHAIN is dm keyword
         */
        CHAIN("CHAIN"),
        /**
         * CHAR is dm keyword
         */
        CHAR("CHAR"),
        /**
         * CHARACTER is dm keyword
         */
        CHARACTER("CHARACTER"),
        /**
         * CHARACTERISTICS is dm keyword
         */
        CHARACTERISTICS("CHARACTERISTICS"),
        /**
         * CHECK is dm keyword
         */
        CHECK("CHECK"),
        /**
         * CIPHER is dm keyword
         */
        CIPHER("CIPHER"),
        /**
         * CLASS is dm keyword
         */
        CLASS("CLASS"),
        /**
         * CLOB is dm keyword
         */
        CLOB("CLOB"),
        /**
         * CLOSE is dm keyword
         */
        CLOSE("CLOSE"),
        /**
         * CLUSTER is dm keyword
         */
        CLUSTER("CLUSTER"),
        /**
         * CLUSTERBTR is dm keyword
         */
        CLUSTERBTR("CLUSTERBTR"),
        /**
         * COLLATE is dm keyword
         */
        COLLATE("COLLATE"),
        /**
         * COLLATION is dm keyword
         */
        COLLATION("COLLATION"),
        /**
         * COLLECT is dm keyword
         */
        COLLECT("COLLECT"),
        /**
         * COLUMN is dm keyword
         */
        COLUMN("COLUMN"),
        /**
         * COLUMNS is dm keyword
         */
        COLUMNS("COLUMNS"),
        /**
         * COMMENT is dm keyword
         */
        COMMENT("COMMENT"),
        /**
         * COMMIT is dm keyword
         */
        COMMIT("COMMIT"),
        /**
         * COMMITTED is dm keyword
         */
        COMMITTED("COMMITTED"),
        /**
         * COMMITWORK is dm keyword
         */
        COMMITWORK("COMMITWORK"),
        /**
         * COMPILE is dm keyword
         */
        COMPILE("COMPILE"),
        /**
         * COMPLETE is dm keyword
         */
        COMPLETE("COMPLETE"),
        /**
         * COMPRESS is dm keyword
         */
        COMPRESS("COMPRESS"),
        /**
         * COMPRESSED is dm keyword
         */
        COMPRESSED("COMPRESSED"),
        /**
         * CONNECT is dm keyword
         */
        CONNECT("CONNECT"),
        /**
         * CONNECT_BY_IS_CYCLE is dm keyword
         */
        CONNECT_BY_IS_CYCLE("CONNECT_BY_IS_CYCLE"),
        /**
         * CONNECT_BY_ISLEAF is dm keyword
         */
        CONNECT_BY_ISLEAF("CONNECT_BY_ISLEAF"),
        /**
         * CONNECT_BY_ROOT is dm keyword
         */
        CONNECT_BY_ROOT("CONNECT_BY_ROOT"),
        /**
         * CONNECT_IDLE_TIME is dm keyword
         */
        CONNECT_IDLE_TIME("CONNECT_IDLE_TIME"),
        /**
         * CONNECT_TIME is dm keyword
         */
        CONNECT_TIME("CONNECT_TIME"),
        /**
         * CONST is dm keyword
         */
        CONST("CONST"),
        /**
         * CONSTANT is dm keyword
         */
        CONSTANT("CONSTANT"),
        /**
         * CONSTRAINT is dm keyword
         */
        CONSTRAINT("CONSTRAINT"),
        /**
         * CONSTRAINTS is dm keyword
         */
        CONSTRAINTS("CONSTRAINTS"),
        /**
         * CONSTRUCTOR is dm keyword
         */
        CONSTRUCTOR("CONSTRUCTOR"),
        /**
         * CONTAINS is dm keyword
         */
        CONTAINS("CONTAINS"),
        /**
         * CONTEXT is dm keyword
         */
        CONTEXT("CONTEXT"),
        /**
         * CONTINUE is dm keyword
         */
        CONTINUE("CONTINUE"),
        /**
         * CONVERT is dm keyword
         */
        CONVERT("CONVERT"),
        /**
         * COPY is dm keyword
         */
        COPY("COPY"),
        /**
         * CORRESPONDING is dm keyword
         */
        CORRESPONDING("CORRESPONDING"),
        /**
         * COUNT is dm keyword
         */
        COUNT("COUNT"),
        /**
         * COUNTER is dm keyword
         */
        COUNTER("COUNTER"),
        /**
         * CPU_PER_CALL is dm keyword
         */
        CPU_PER_CALL("CPU_PER_CALL"),
        /**
         * CPU_PER_SESSION is dm keyword
         */
        CPU_PER_SESSION("CPU_PER_SESSION"),
        /**
         * CREATE is dm keyword
         */
        CREATE("CREATE"),
        /**
         * CROSS is dm keyword
         */
        CROSS("CROSS"),
        /**
         * CRYPTO is dm keyword
         */
        CRYPTO("CRYPTO"),
        /**
         * CTLFILE is dm keyword
         */
        CTLFILE("CTLFILE"),
        /**
         * CUBE is dm keyword
         */
        CUBE("CUBE"),
        /**
         * CUMULATIVE is dm keyword
         */
        CUMULATIVE("CUMULATIVE"),
        /**
         * CURRENT is dm keyword
         */
        CURRENT("CURRENT"),
        /**
         * CURRENT_SCHEMA is dm keyword
         */
        CURRENT_SCHEMA("CURRENT_SCHEMA"),
        /**
         * CURRENT_USER is dm keyword
         */
        CURRENT_USER("CURRENT_USER"),
        /**
         * CURSOR is dm keyword
         */
        CURSOR("CURSOR"),
        /**
         * CYCLE is dm keyword
         */
        CYCLE("CYCLE"),
        /**
         * D is dm keyword
         */
        D("D"),
        /**
         * DANGLING is dm keyword
         */
        DANGLING("DANGLING"),
        /**
         * DATA is dm keyword
         */
        DATA("DATA"),
        /**
         * DATABASE is dm keyword
         */
        DATABASE("DATABASE"),
        /**
         * DATAFILE is dm keyword
         */
        DATAFILE("DATAFILE"),
        /**
         * DATE is dm keyword
         */
        DATE("DATE"),
        /**
         * DATEADD is dm keyword
         */
        DATEADD("DATEADD"),
        /**
         * DATEDIFF is dm keyword
         */
        DATEDIFF("DATEDIFF"),
        /**
         * DATEPART is dm keyword
         */
        DATEPART("DATEPART"),
        /**
         * DATETIME is dm keyword
         */
        DATETIME("DATETIME"),
        /**
         * DAY is dm keyword
         */
        DAY("DAY"),
        /**
         * DBFILE is dm keyword
         */
        DBFILE("DBFILE"),
        /**
         * DDL is dm keyword
         */
        DDL("DDL"),
        /**
         * DDL_CLONE is dm keyword
         */
        DDL_CLONE("DDL_CLONE"),
        /**
         * DEBUG is dm keyword
         */
        DEBUG("DEBUG"),
        /**
         * DEC is dm keyword
         */
        DEC("DEC"),
        /**
         * DECIMAL is dm keyword
         */
        DECIMAL("DECIMAL"),
        /**
         * DECLARE is dm keyword
         */
        DECLARE("DECLARE"),
        /**
         * DECODE is dm keyword
         */
        DECODE("DECODE"),
        /**
         * DEFAULT is dm keyword
         */
        DEFAULT("DEFAULT"),
        /**
         * DEFERRABLE is dm keyword
         */
        DEFERRABLE("DEFERRABLE"),
        /**
         * DEFERRED is dm keyword
         */
        DEFERRED("DEFERRED"),
        /**
         * DEFAULT is dm keyword
         */
        DEFINER("DEFINER"),
        /**
         * DELETE is dm keyword
         */
        DELETE("DELETE"),
        /**
         * DELETING is dm keyword
         */
        DELETING("DELETING"),
        /**
         * DELIMITED is dm keyword
         */
        DELIMITED("DELIMITED"),
        /**
         * DELTA is dm keyword
         */
        DELTA("DELTA"),
        /**
         * DEMAND is dm keyword
         */
        DEMAND("DEMAND"),
        /**
         * DENSE_RANK is dm keyword
         */
        DENSE_RANK("DENSE_RANK"),
        /**
         * DEREF is dm keyword
         */
        DEREF("DEREF"),
        /**
         * DESC is dm keyword
         */
        DESC("DESC"),
        /**
         * DETACH is dm keyword
         */
        DETACH("DETACH"),
        /**
         * DETERMINISTIC is dm keyword
         */
        DETERMINISTIC("DETERMINISTIC"),
        /**
         * DEVICE is dm keyword
         */
        DEVICE("DEVICE"),
        /**
         * DIAGNOSTICS is dm keyword
         */
        DIAGNOSTICS("DIAGNOSTICS"),
        /**
         * DICTIONARY is dm keyword
         */
        DICTIONARY("DICTIONARY"),
        /**
         * DISABLE is dm keyword
         */
        DISABLE("DISABLE"),
        /**
         * DISCONNECT is dm keyword
         */
        DISCONNECT("DISCONNECT"),
        /**
         * DISKSPACE is dm keyword
         */
        DISKSPACE("DISKSPACE"),
        /**
         * DISTINCT is dm keyword
         */
        DISTINCT("DISTINCT"),
        /**
         * DISTRIBUTED is dm keyword
         */
        DISTRIBUTED("DISTRIBUTED"),
        /**
         * DO is dm keyword
         */
        DO("DO"),
        /**
         * DOMAIN is dm keyword
         */
        DOMAIN("DOMAIN"),
        /**
         * DOUBLE is dm keyword
         */
        DOUBLE("DOUBLE"),
        /**
         * DOWN is dm keyword
         */
        DOWN("DOWN"),
        /**
         * DROP is dm keyword
         */
        DROP("DROP"),
        /**
         * DUMP is dm keyword
         */
        DUMP("DUMP"),
        /**
         * E is dm keyword
         */
        E("E"),
        /**
         * EACH is dm keyword
         */
        EACH("EACH"),
        /**
         * ELSE is dm keyword
         */
        ELSE("ELSE"),
        /**
         * ELSEIF is dm keyword
         */
        ELSEIF("ELSEIF"),
        /**
         * ELSIF is dm keyword
         */
        ELSIF("ELSIF"),
        /**
         * ENABLE is dm keyword
         */
        ENABLE("ENABLE"),
        /**
         * ENCRYPT is dm keyword
         */
        ENCRYPT("ENCRYPT"),
        /**
         * ENCRYPTION is dm keyword
         */
        ENCRYPTION("ENCRYPTION"),
        /**
         * END is dm keyword
         */
        END("END"),
        /**
         * EQU is dm keyword
         */
        EQU("EQU"),
        /**
         * ERROR is dm keyword
         */
        ERROR("ERROR"),
        /**
         * ERRORS is dm keyword
         */
        ERRORS("ERRORS"),
        /**
         * ESCAPE is dm keyword
         */
        ESCAPE("ESCAPE"),
        /**
         * EVENTINFO is dm keyword
         */
        EVENTINFO("EVENTINFO"),
        /**
         * EVENTS is dm keyword
         */
        EVENTS("EVENTS"),
        /**
         * EXCEPT is dm keyword
         */
        EXCEPT("EXCEPT"),
        /**
         * EXCEPTION is dm keyword
         */
        EXCEPTION("EXCEPTION"),
        /**
         * EXCEPTIONS is dm keyword
         */
        EXCEPTIONS("EXCEPTIONS"),
        /**
         * EXCEPTION_INIT is dm keyword
         */
        EXCEPTION_INIT("EXCEPTION_INIT"),
        /**
         * EXCHANGE is dm keyword
         */
        EXCHANGE("EXCHANGE"),
        /**
         * EXCLUDE is dm keyword
         */
        EXCLUDE("EXCLUDE"),
        /**
         * EXCLUDING is dm keyword
         */
        EXCLUDING("EXCLUDING"),
        /**
         * EXCLUSIVE is dm keyword
         */
        EXCLUSIVE("EXCLUSIVE"),
        /**
         * EXEC is dm keyword
         */
        EXEC("EXEC"),
        /**
         * EXECUTE is dm keyword
         */
        EXECUTE("EXECUTE"),
        /**
         * EXISTS is dm keyword
         */
        EXISTS("EXISTS"),
        /**
         * EXIT is dm keyword
         */
        EXIT("EXIT"),
        /**
         * EXPLAIN is dm keyword
         */
        EXPLAIN("EXPLAIN"),
        /**
         * EXTENDS is dm keyword
         */
        EXTENDS("EXTENDS"),
        /**
         * EXTERN is dm keyword
         */
        EXTERN("EXTERN"),
        /**
         * EXTERNAL is dm keyword
         */
        EXTERNAL("EXTERNAL"),
        /**
         * EXTERNALLY is dm keyword
         */
        EXTERNALLY("EXTERNALLY"),
        /**
         * EXTRACT is dm keyword
         */
        EXTRACT("EXTRACT"),
        /**
         * F is dm keyword
         */
        F("F"),
        /**
         * FAILED_LOGIN_ATTEMPS is dm keyword
         */
        FAILED_LOGIN_ATTEMPS("FAILED_LOGIN_ATTEMPS"),
        /**
         * FAST is dm keyword
         */
        FAST("FAST"),
        /**
         * FETCH is dm keyword
         */
        FETCH("FETCH"),
        /**
         * FIELDS is dm keyword
         */
        FIELDS("FIELDS"),
        /**
         * FILE is dm keyword
         */
        FILE("FILE"),
        /**
         * FILEGROUP is dm keyword
         */
        FILEGROUP("FILEGROUP"),
        /**
         * FILESIZE is dm keyword
         */
        FILESIZE("FILESIZE"),
        /**
         * FILLFACTOR is dm keyword
         */
        FILLFACTOR("FILLFACTOR"),
        /**
         * FINAL is dm keyword
         */
        FINAL("FINAL"),
        /**
         * FINALLY is dm keyword
         */
        FINALLY("FINALLY"),
        /**
         * FIRST is dm keyword
         */
        FIRST("FIRST"),
        /**
         * FLOAT is dm keyword
         */
        FLOAT("FLOAT"),
        /**
         * FOLLOWING is dm keyword
         */
        FOLLOWING("FOLLOWING"),
        /**
         * FOR is dm keyword
         */
        FOR("FOR"),
        /**
         * FORALL is dm keyword
         */
        FORALL("FORALL"),
        /**
         * FORCE is dm keyword
         */
        FORCE("FORCE"),
        /**
         * FOREIGN is dm keyword
         */
        FOREIGN("FOREIGN"),
        /**
         * FREQUENCE is dm keyword
         */
        FREQUENCE("FREQUENCE"),
        /**
         * FROM is dm keyword
         */
        FROM("FROM"),
        /**
         * FULL is dm keyword
         */
        FULL("FULL"),
        /**
         * FULLY is dm keyword
         */
        FULLY("FULLY"),
        /**
         * FUNCTION is dm keyword
         */
        FUNCTION("FUNCTION"),
        /**
         * GET is dm keyword
         */
        GET("GET"),
        /**
         * GLOBAL is dm keyword
         */
        GLOBAL("GLOBAL"),
        /**
         * GLOBALLY is dm keyword
         */
        GLOBALLY("GLOBALLY"),
        /**
         * GOTO is dm keyword
         */
        GOTO("GOTO"),
        /**
         * GRANT is dm keyword
         */
        GRANT("GRANT"),
        /**
         * GROUP is dm keyword
         */
        GROUP("GROUP"),
        /**
         * GROUPING is dm keyword
         */
        GROUPING("GROUPING"),
        /**
         * HASH is dm keyword
         */
        HASH("HASH"),
        /**
         * HAVING is dm keyword
         */
        HAVING("HAVING"),
        /**
         * HEXTORAW is dm keyword
         */
        HEXTORAW("HEXTORAW"),
        /**
         * HOLD is dm keyword
         */
        HOLD("HOLD"),
        /**
         * HOUR is dm keyword
         */
        HOUR("HOUR"),
        /**
         * HUGE is dm keyword
         */
        HUGE("HUGE"),
        /**
         * IDENTIFIED is dm keyword
         */
        IDENTIFIED("IDENTIFIED"),
        /**
         * IDENTITY is dm keyword
         */
        IDENTITY("IDENTITY"),
        /**
         * IDENTITY_INSERT is dm keyword
         */
        IDENTITY_INSERT("IDENTITY_INSERT"),
        /**
         * IF is dm keyword
         */
        IF("IF"),
        /**
         * IMAGE is dm keyword
         */
        IMAGE("IMAGE"),
        /**
         * IMMEDIATE is dm keyword
         */
        IMMEDIATE("IMMEDIATE"),
        /**
         * IN is dm keyword
         */
        IN("IN"),
        /**
         * INCLUDE is dm keyword
         */
        INCLUDE("INCLUDE"),
        /**
         * INCLUDING is dm keyword
         */
        INCLUDING("INCLUDING"),
        /**
         * INCREASE is dm keyword
         */
        INCREASE("INCREASE"),
        /**
         * INCREMENT is dm keyword
         */
        INCREMENT("INCREMENT"),
        /**
         * INDEX is dm keyword
         */
        INDEX("INDEX"),
        /**
         * INDEXES is dm keyword
         */
        INDEXES("INDEXES"),
        /**
         * INDICES is dm keyword
         */
        INDICES("INDICES"),
        /**
         * INITIAL is dm keyword
         */
        INITIAL("INITIAL"),
        /**
         * INITIALIZED is dm keyword
         */
        INITIALIZED("INITIALIZED"),
        /**
         * INITIALLY is dm keyword
         */
        INITIALLY("INITIALLY"),
        /**
         * INLINE is dm keyword
         */
        INLINE("INLINE"),
        /**
         * INNER is dm keyword
         */
        INNER("INNER"),
        /**
         * INNERID is dm keyword
         */
        INNERID("INNERID"),
        /**
         * INPUT is dm keyword
         */
        INPUT("INPUT"),
        /**
         * INSENSITIVE is dm keyword
         */
        INSENSITIVE("INSENSITIVE"),
        /**
         * INSERT is dm keyword
         */
        INSERT("INSERT"),
        /**
         * INSERTING is dm keyword
         */
        INSERTING("INSERTING"),
        /**
         * INSTANTIABLE is dm keyword
         */
        INSTANTIABLE("INSTANTIABLE"),
        /**
         * INSTEAD is dm keyword
         */
        INSTEAD("INSTEAD"),
        /**
         * INT is dm keyword
         */
        INT("INT"),
        /**
         * INTEGER is dm keyword
         */
        INTEGER("INTEGER"),
        /**
         * INTENT is dm keyword
         */
        INTENT("INTENT"),
        /**
         * INTERNAL is dm keyword
         */
        INTERNAL("INTERNAL"),
        /**
         * INTERSECT is dm keyword
         */
        INTERSECT("INTERSECT"),
        /**
         * INTERVAL is dm keyword
         */
        INTERVAL("INTERVAL"),
        /**
         * INTO is dm keyword
         */
        INTO("INTO"),
        /**
         * INVISIBLE is dm keyword
         */
        INVISIBLE("INVISIBLE"),
        /**
         * IS is dm keyword
         */
        IS("IS"),
        /**
         * ISOLATION is dm keyword
         */
        ISOLATION("ISOLATION"),
        /**
         * JAVA is dm keyword
         */
        JAVA("JAVA"),
        /**
         * JOB is dm keyword
         */
        JOB("JOB"),
        /**
         * JOIN is dm keyword
         */
        JOIN("JOIN"),
        /**
         * KEEP is dm keyword
         */
        KEEP("KEEP"),
        /**
         * KEY is dm keyword
         */
        KEY("KEY"),
        /**
         * LABEL is dm keyword
         */
        LABEL("LABEL"),
        /**
         * LARGE is dm keyword
         */
        LARGE("LARGE"),
        /**
         * LAST is dm keyword
         */
        LAST("LAST"),
        /**
         * LEADING is dm keyword
         */
        LEADING("LEADING"),
        /**
         * LEFT is dm keyword
         */
        LEFT("LEFT"),
        /**
         * LESS is dm keyword
         */
        LESS("LESS"),
        /**
         * LEVEL is dm keyword
         */
        LEVEL("LEVEL"),
        /**
         * LEXER is dm keyword
         */
        LEXER("LEXER"),
        /**
         * LIKE is dm keyword
         */
        LIKE("LIKE"),
        /**
         * LIMIT is dm keyword
         */
        LIMIT("LIMIT"),
        /**
         * LINK is dm keyword
         */
        LINK("LINK"),
        /**
         * LIST is dm keyword
         */
        LIST("LIST"),
        /**
         * LNNVL is dm keyword
         */
        LNNVL("LNNVL"),
        /**
         * LOB is dm keyword
         */
        LOB("LOB"),
        /**
         * LOCAL is dm keyword
         */
        LOCAL("LOCAL"),
        /**
         * LOCALLY is dm keyword
         */
        LOCALLY("LOCALLY"),
        /**
         * LOCK is dm keyword
         */
        LOCK("LOCK"),
        /**
         * LOCKED is dm keyword
         */
        LOCKED("LOCKED"),
        /**
         * LOG is dm keyword
         */
        LOG("LOG"),
        /**
         * LOGFILE is dm keyword
         */
        LOGFILE("LOGFILE"),
        /**
         * LOGGING is dm keyword
         */
        LOGGING("LOGGING"),
        /**
         * LOGIN is dm keyword
         */
        LOGIN("LOGIN"),
        /**
         * LOGOFF is dm keyword
         */
        LOGOFF("LOGOFF"),
        /**
         * LOGON is dm keyword
         */
        LOGON("LOGON"),
        /**
         * LOGOUT is dm keyword
         */
        LOGOUT("LOGOUT"),
        /**
         * LONG is dm keyword
         */
        LONG("LONG"),
        /**
         * LONGVARBINARY is dm keyword
         */
        LONGVARBINARY("LONGVARBINARY"),
        /**
         * LONGVARCHAR is dm keyword
         */
        LONGVARCHAR("LONGVARCHAR"),
        /**
         * LOOP is dm keyword
         */
        LOOP("LOOP"),
        /**
         * LSN is dm keyword
         */
        LSN("LSN"),
        /**
         * MANUAL is dm keyword
         */
        MANUAL("MANUAL"),
        /**
         * MAP is dm keyword
         */
        MAP("MAP"),
        /**
         * MAPPED is dm keyword
         */
        MAPPED("MAPPED"),
        /**
         * MATCH is dm keyword
         */
        MATCH("MATCH"),
        /**
         * MATCHED is dm keyword
         */
        MATCHED("MATCHED"),
        /**
         * MATERIALIZED is dm keyword
         */
        MATERIALIZED("MATERIALIZED"),
        /**
         * MAX is dm keyword
         */
        MAX("MAX"),
        /**
         * MAXPIECESIZE is dm keyword
         */
        MAXPIECESIZE("MAXPIECESIZE"),
        /**
         * MAXSIZE is dm keyword
         */
        MAXSIZE("MAXSIZE"),
        /**
         * MAXVALUE is dm keyword
         */
        MAXVALUE("MAXVALUE"),
        /**
         * MEMBER is dm keyword
         */
        MEMBER("MEMBER"),
        /**
         * MEMORY is dm keyword
         */
        MEMORY("MEMORY"),
        /**
         * MEM_SPACE is dm keyword
         */
        MEM_SPACE("MEM_SPACE"),
        /**
         * MERGE is dm keyword
         */
        MERGE("MERGE"),
        /**
         * MIN is dm keyword
         */
        MIN("MIN"),
        /**
         * MINEXTENTS is dm keyword
         */
        MINEXTENTS("MINEXTENTS"),
        /**
         * MINUS is dm keyword
         */
        MINUS("MINUS"),
        /**
         * MINUTE is dm keyword
         */
        MINUTE("MINUTE"),
        /**
         * MINVALUE is dm keyword
         */
        MINVALUE("MINVALUE"),
        /**
         * MIRROR is dm keyword
         */
        MIRROR("MIRROR"),
        /**
         * MOD is dm keyword
         */
        MOD("MOD"),
        /**
         * MODE is dm keyword
         */
        MODE("MODE"),
        /**
         * MODIFY is dm keyword
         */
        MODIFY("MODIFY"),
        /**
         * MONEY is dm keyword
         */
        MONEY("MONEY"),
        /**
         * MONITORING is dm keyword
         */
        MONITORING("MONITORING"),
        /**
         * MONTH is dm keyword
         */
        MONTH("MONTH"),
        /**
         * MOUNT is dm keyword
         */
        MOUNT("MOUNT"),
        /**
         * MOVEMENT is dm keyword
         */
        MOVEMENT("MOVEMENT"),
        /**
         * NATIONAL is dm keyword
         */
        NATIONAL("NATIONAL"),
        /**
         * NATURAL is dm keyword
         */
        NATURAL("NATURAL"),
        /**
         * NCHAR is dm keyword
         */
        NCHAR("NCHAR"),
        /**
         * NCHARACTER is dm keyword
         */
        NCHARACTER("NCHARACTER"),
        /**
         * NEVER is dm keyword
         */
        NEVER("NEVER"),
        /**
         * NEW is dm keyword
         */
        NEW("NEW"),
        /**
         * NEXT is dm keyword
         */
        NEXT("NEXT"),
        /**
         * NO is dm keyword
         */
        NO("NO"),
        /**
         * NOARCHIVELOG is dm keyword
         */
        NOARCHIVELOG("NOARCHIVELOG"),
        /**
         * NOAUDIT is dm keyword
         */
        NOAUDIT("NOAUDIT"),
        /**
         * NOBRANCH is dm keyword
         */
        NOBRANCH("NOBRANCH"),
        /**
         * NOCACHE is dm keyword
         */
        NOCACHE("NOCACHE"),
        /**
         * NOCOPY is dm keyword
         */
        NOCOPY("NOCOPY"),
        /**
         * NOCYCLE is dm keyword
         */
        NOCYCLE("NOCYCLE"),
        /**
         * NOLOGGING is dm keyword
         */
        NOLOGGING("NOLOGGING"),
        /**
         * NOMAXVALUE is dm keyword
         */
        NOMAXVALUE("NOMAXVALUE"),
        /**
         * NOMINVALUE is dm keyword
         */
        NOMINVALUE("NOMINVALUE"),
        /**
         * NOMONITORING is dm keyword
         */
        NOMONITORING("NOMONITORING"),
        /**
         * NONE is dm keyword
         */
        NONE("NONE"),
        /**
         * NOORDER is dm keyword
         */
        NOORDER("NOORDER"),
        /**
         * NORMAL is dm keyword
         */
        NORMAL("NORMAL"),
        /**
         * NOSORT is dm keyword
         */
        NOSORT("NOSORT"),
        /**
         * NOT is dm keyword
         */
        NOT("NOT"),
        /**
         * NOT_ALLOW_DATETIME is dm keyword
         */
        NOT_ALLOW_DATETIME("NOT_ALLOW_DATETIME"),
        /**
         * NOT_ALLOW_IP is dm keyword
         */
        NOT_ALLOW_IP("NOT_ALLOW_IP"),
        /**
         * NOWAIT is dm keyword
         */
        NOWAIT("NOWAIT"),
        /**
         * NULL is dm keyword
         */
        NULL("NULL"),
        /**
         * NULLS is dm keyword
         */
        NULLS("NULLS"),
        /**
         * NUMBER is dm keyword
         */
        NUMBER("NUMBER"),
        /**
         * NUMERIC is dm keyword
         */
        NUMERIC("NUMERIC"),
        /**
         * OBJECT is dm keyword
         */
        OBJECT("OBJECT"),
        /**
         * OF is dm keyword
         */
        OF("OF"),
        /**
         * OFF is dm keyword
         */
        OFF("OFF"),
        /**
         * OFFLINE is dm keyword
         */
        OFFLINE("OFFLINE"),
        /**
         * OFFSET is dm keyword
         */
        OFFSET("OFFSET"),
        /**
         * OLD is dm keyword
         */
        OLD("OLD"),
        /**
         * ON is dm keyword
         */
        ON("ON"),
        /**
         * ONCE is dm keyword
         */
        ONCE("ONCE"),
        /**
         * ONLINE is dm keyword
         */
        ONLINE("ONLINE"),
        /**
         * ONLY is dm keyword
         */
        ONLY("ONLY"),
        /**
         * OPEN is dm keyword
         */
        OPEN("OPEN"),
        /**
         * OPTIMIZE is dm keyword
         */
        OPTIMIZE("OPTIMIZE"),
        /**
         * OPTION is dm keyword
         */
        OPTION("OPTION"),
        /**
         * OR is dm keyword
         */
        OR("OR"),
        /**
         * ORDER is dm keyword
         */
        ORDER("ORDER"),
        /**
         * OUT is dm keyword
         */
        OUT("OUT"),
        /**
         * OUTER is dm keyword
         */
        OUTER("OUTER"),
        /**
         * OVER is dm keyword
         */
        OVER("OVER"),
        /**
         * OVERLAPS is dm keyword
         */
        OVERLAPS("OVERLAPS"),
        /**
         * OVERLAY is dm keyword
         */
        OVERLAY("OVERLAY"),
        /**
         * OVERRIDE is dm keyword
         */
        OVERRIDE("OVERRIDE"),
        /**
         * OVERRIDING is dm keyword
         */
        OVERRIDING("OVERRIDING"),
        /**
         * PACKAGE is dm keyword
         */
        PACKAGE("PACKAGE"),
        /**
         * PAD is dm keyword
         */
        PAD("PAD"),
        /**
         * PAGE is dm keyword
         */
        PAGE("PAGE"),
        /**
         * PARALLEL is dm keyword
         */
        PARALLEL("PARALLEL"),
        /**
         * PARALLEL_ENABLE is dm keyword
         */
        PARALLEL_ENABLE("PARALLEL_ENABLE"),
        /**
         * PARMS is dm keyword
         */
        PARMS("PARMS"),
        /**
         * PARTIAL is dm keyword
         */
        PARTIAL("PARTIAL"),
        /**
         * PARTITION is dm keyword
         */
        PARTITION("PARTITION"),
        /**
         * PARTITIONS is dm keyword
         */
        PARTITIONS("PARTITIONS"),
        /**
         * PASSWORD_GRACE_TIME is dm keyword
         */
        PASSWORD_GRACE_TIME("PASSWORD_GRACE_TIME"),
        /**
         * PASSWORD_LIFE_TIME is dm keyword
         */
        PASSWORD_LIFE_TIME("PASSWORD_LIFE_TIME"),
        /**
         * PASSWORD_LOCK_TIME is dm keyword
         */
        PASSWORD_LOCK_TIME("PASSWORD_LOCK_TIME"),
        /**
         * PASSWORD_POLICY is dm keyword
         */
        PASSWORD_POLICY("PASSWORD_POLICY"),
        /**
         * PASSWORD_REUSE_MAX is dm keyword
         */
        PASSWORD_REUSE_MAX("PASSWORD_REUSE_MAX"),
        /**
         * PASSWORD_REUSE_TIME is dm keyword
         */
        PASSWORD_REUSE_TIME("PASSWORD_REUSE_TIME"),
        /**
         * PATH is dm keyword
         */
        PATH("PATH"),
        /**
         * PENDANT is dm keyword
         */
        PENDANT("PENDANT"),
        /**
         * PERCENT is dm keyword
         */
        PERCENT("PERCENT"),
        /**
         * PIPE is dm keyword
         */
        PIPE("PIPE"),
        /**
         * PIPELINED is dm keyword
         */
        PIPELINED("PIPELINED"),
        /**
         * PIVOT is dm keyword
         */
        PIVOT("PIVOT"),
        /**
         * PLACING is dm keyword
         */
        PLACING("PLACING"),
        /**
         * PLS_INTEGER is dm keyword
         */
        PLS_INTEGER("PLS_INTEGER"),
        /**
         * PRAGMA is dm keyword
         */
        PRAGMA("PRAGMA"),
        /**
         * PRECEDING is dm keyword
         */
        PRECEDING("PRECEDING"),
        /**
         * PRECISION is dm keyword
         */
        PRECISION("PRECISION"),
        /**
         * PRESERVE is dm keyword
         */
        PRESERVE("PRESERVE"),
        /**
         * PRIMARY is dm keyword
         */
        PRIMARY("PRIMARY"),
        /**
         * PRINT is dm keyword
         */
        PRINT("PRINT"),
        /**
         * PRIOR is dm keyword
         */
        PRIOR("PRIOR"),
        /**
         * PRIVATE is dm keyword
         */
        PRIVATE("PRIVATE"),
        /**
         * PRIVILEGE is dm keyword
         */
        PRIVILEGE("PRIVILEGE"),
        /**
         * PRIVILEGES is dm keyword
         */
        PRIVILEGES("PRIVILEGES"),
        /**
         * PROCEDURE is dm keyword
         */
        PROCEDURE("PROCEDURE"),
        /**
         * PROTECTED is dm keyword
         */
        PROTECTED("PROTECTED"),
        /**
         * PUBLIC is dm keyword
         */
        PUBLIC("PUBLIC"),
        /**
         * PURGE is dm keyword
         */
        PURGE("PURGE"),
        /**
         * QUERY_REWRITE_INTEGRITY is dm keyword
         */
        QUERY_REWRITE_INTEGRITY("QUERY_REWRITE_INTEGRITY"),
        /**
         * RAISE is dm keyword
         */
        RAISE("RAISE"),
        /**
         * RANDOMLY is dm keyword
         */
        RANDOMLY("RANDOMLY"),
        /**
         * RANGE is dm keyword
         */
        RANGE("RANGE"),
        /**
         * RAWTOHEX is dm keyword
         */
        RAWTOHEX("RAWTOHEX"),
        /**
         * READ is dm keyword
         */
        READ("READ"),
        /**
         * READONLY is dm keyword
         */
        READONLY("READONLY"),
        /**
         * READ_PER_CALL is dm keyword
         */
        READ_PER_CALL("READ_PER_CALL"),
        /**
         * READ_PER_SESSION is dm keyword
         */
        READ_PER_SESSION("READ_PER_SESSION"),
        /**
         * REAL is dm keyword
         */
        REAL("REAL"),
        /**
         * REBUILD is dm keyword
         */
        REBUILD("REBUILD"),
        /**
         * RECORD is dm keyword
         */
        RECORD("RECORD"),
        /**
         * RECORDS is dm keyword
         */
        RECORDS("RECORDS"),
        /**
         * REF is dm keyword
         */
        REF("REF"),
        /**
         * REFERENCE is dm keyword
         */
        REFERENCE("REFERENCE"),
        /**
         * REFERENCES is dm keyword
         */
        REFERENCES("REFERENCES"),
        /**
         * REFERENCING is dm keyword
         */
        REFERENCING("REFERENCING"),
        /**
         * REFRESH is dm keyword
         */
        REFRESH("REFRESH"),
        /**
         * RELATED is dm keyword
         */
        RELATED("RELATED"),
        /**
         * RELATIVE is dm keyword
         */
        RELATIVE("RELATIVE"),
        /**
         * RENAME is dm keyword
         */
        RENAME("RENAME"),
        /**
         * REPEAT is dm keyword
         */
        REPEAT("REPEAT"),
        /**
         * REPEATABLE is dm keyword
         */
        REPEATABLE("REPEATABLE"),
        /**
         * REPLACE is dm keyword
         */
        REPLACE("REPLACE"),
        /**
         * REPLAY is dm keyword
         */
        REPLAY("REPLAY"),
        /**
         * REPLICATE is dm keyword
         */
        REPLICATE("REPLICATE"),
        /**
         * RESIZE is dm keyword
         */
        RESIZE("RESIZE"),
        /**
         * RESTORE is dm keyword
         */
        RESTORE("RESTORE"),
        /**
         * RESTRICT is dm keyword
         */
        RESTRICT("RESTRICT"),
        /**
         * RESULT is dm keyword
         */
        RESULT("RESULT"),
        /**
         * RESULT_CACHE is dm keyword
         */
        RESULT_CACHE("RESULT_CACHE"),
        /**
         * RETURN is dm keyword
         */
        RETURN("RETURN"),
        /**
         * RETURNING is dm keyword
         */
        RETURNING("RETURNING"),
        /**
         * REVERSE is dm keyword
         */
        REVERSE("REVERSE"),
        /**
         * REVOKE is dm keyword
         */
        REVOKE("REVOKE"),
        /**
         * RIGHT is dm keyword
         */
        RIGHT("RIGHT"),
        /**
         * ROLE is dm keyword
         */
        ROLE("ROLE"),
        /**
         * ROLLBACK is dm keyword
         */
        ROLLBACK("ROLLBACK"),
        /**
         * ROLLFILE is dm keyword
         */
        ROLLFILE("ROLLFILE"),
        /**
         * ROLLUP is dm keyword
         */
        ROLLUP("ROLLUP"),
        /**
         * ROOT is dm keyword
         */
        ROOT("ROOT"),
        /**
         * ROW is dm keyword
         */
        ROW("ROW"),
        /**
         * ROWCOUNT is dm keyword
         */
        ROWCOUNT("ROWCOUNT"),
        /**
         * ROWID is dm keyword
         */
        ROWID("ROWID"),
        /**
         * ROWNUM is dm keyword
         */
        ROWNUM("ROWNUM"),
        /**
         * ROWS is dm keyword
         */
        ROWS("ROWS"),
        /**
         * RULE is dm keyword
         */
        RULE("RULE"),
        /**
         * SALT is dm keyword
         */
        SALT("SALT"),
        /**
         * SAMPLE is dm keyword
         */
        SAMPLE("SAMPLE"),
        /**
         * SAVE is dm keyword
         */
        SAVE("SAVE"),
        /**
         * SAVEPOINT is dm keyword
         */
        SAVEPOINT("SAVEPOINT"),
        /**
         * SBYTE is dm keyword
         */
        SBYTE("SBYTE"),
        /**
         * SCHEMA is dm keyword
         */
        SCHEMA("SCHEMA"),
        /**
         * SCOPE is dm keyword
         */
        SCOPE("SCOPE"),
        /**
         * SCROLL is dm keyword
         */
        SCROLL("SCROLL"),
        /**
         * SEALED is dm keyword
         */
        SEALED("SEALED"),
        /**
         * SECOND is dm keyword
         */
        SECOND("SECOND"),
        /**
         * SECTION is dm keyword
         */
        SECTION("SECTION"),
        /**
         * SEED is dm keyword
         */
        SEED("SEED"),
        /**
         * SELECT is dm keyword
         */
        SELECT("SELECT"),
        /**
         * SELF is dm keyword
         */
        SELF("SELF"),
        /**
         * SENSITIVE is dm keyword
         */
        SENSITIVE("SENSITIVE"),
        /**
         * SEQUENCE is dm keyword
         */
        SEQUENCE("SEQUENCE"),
        /**
         * SERERR is dm keyword
         */
        SERERR("SERERR"),
        /**
         * SERIALIZABLE is dm keyword
         */
        SERIALIZABLE("SERIALIZABLE"),
        /**
         * SERVER is dm keyword
         */
        SERVER("SERVER"),
        /**
         * SESSION is dm keyword
         */
        SESSION("SESSION"),
        /**
         * SESSION_PER_USER is dm keyword
         */
        SESSION_PER_USER("SESSION_PER_USER"),
        /**
         * SET is dm keyword
         */
        SET("SET"),
        /**
         * SETS is dm keyword
         */
        SETS("SETS"),
        /**
         * SHARE is dm keyword
         */
        SHARE("SHARE"),
        /**
         * SHORT is dm keyword
         */
        SHORT("SHORT"),
        /**
         * SHUTDOWN is dm keyword
         */
        SHUTDOWN("SHUTDOWN"),
        /**
         * SIBLINGS is dm keyword
         */
        SIBLINGS("SIBLINGS"),
        /**
         * SIMPLE is dm keyword
         */
        SIMPLE("SIMPLE"),
        /**
         * SINCE is dm keyword
         */
        SINCE("SINCE"),
        /**
         * SIZE is dm keyword
         */
        SIZE("SIZE"),
        /**
         * SIZEOF is dm keyword
         */
        SIZEOF("SIZEOF"),
        /**
         * SKIP is dm keyword
         */
        SKIP("SKIP"),
        /**
         * SMALLINT is dm keyword
         */
        SMALLINT("SMALLINT"),
        /**
         * SNAPSHOT is dm keyword
         */
        SNAPSHOT("SNAPSHOT"),
        /**
         * SOME is dm keyword
         */
        SOME("SOME"),
        /**
         * SOUND is dm keyword
         */
        SOUND("SOUND"),
        /**
         * SPACE is dm keyword
         */
        SPACE("SPACE"),
        /**
         * SPATIAL is dm keyword
         */
        SPATIAL("SPATIAL"),
        /**
         * SPFILE is dm keyword
         */
        SPFILE("SPFILE"),
        /**
         * SPLIT is dm keyword
         */
        SPLIT("SPLIT"),
        /**
         * SQL is dm keyword
         */
        SQL("SQL"),
        /**
         * STANDBY is dm keyword
         */
        STANDBY("STANDBY"),
        /**
         * STARTUP is dm keyword
         */
        STARTUP("STARTUP"),
        /**
         * STAT is dm keyword
         */
        STAT("STAT"),
        /**
         * STATEMENT is dm keyword
         */
        STATEMENT("STATEMENT"),
        /**
         * STATIC is dm keyword
         */
        STATIC("STATIC"),
        /**
         * STDDEV is dm keyword
         */
        STDDEV("STDDEV"),
        /**
         * STORAGE is dm keyword
         */
        STORAGE("STORAGE"),
        /**
         * STORE is dm keyword
         */
        STORE("STORE"),
        /**
         * STRING is dm keyword
         */
        STRING("STRING"),
        /**
         * STRUCT is dm keyword
         */
        STRUCT("STRUCT"),
        /**
         * STYLE is dm keyword
         */
        STYLE("STYLE"),
        /**
         * SUBPARTITION is dm keyword
         */
        SUBPARTITION("SUBPARTITION"),
        /**
         * SUBPARTITIONS is dm keyword
         */
        SUBPARTITIONS("SUBPARTITIONS"),
        /**
         * SUBSTRING is dm keyword
         */
        SUBSTRING("SUBSTRING"),
        /**
         * SUBTYPE is dm keyword
         */
        SUBTYPE("SUBTYPE"),
        /**
         * SUCCESSFUL is dm keyword
         */
        SUCCESSFUL("SUCCESSFUL"),
        /**
         * SUM is dm keyword
         */
        SUM("SUM"),
        /**
         * SUSPEND is dm keyword
         */
        SUSPEND("SUSPEND"),
        /**
         * SWITCH is dm keyword
         */
        SWITCH("SWITCH"),
        /**
         * SYNC is dm keyword
         */
        SYNC("SYNC"),
        /**
         * SYNCHRONOUS is dm keyword
         */
        SYNCHRONOUS("SYNCHRONOUS"),
        /**
         * SYNONYM is dm keyword
         */
        SYNONYM("SYNONYM"),
        /**
         * SYSDATE is dm keyword
         */
        SYSDATE("SYSDATE"),
        /**
         * SYSTEM is dm keyword
         */
        SYSTEM("SYSTEM"),
        /**
         * SYS_CONNECT_BY_PATH is dm keyword
         */
        SYS_CONNECT_BY_PATH("SYS_CONNECT_BY_PATH"),
        /**
         * TABLE is dm keyword
         */
        TABLE("TABLE"),
        /**
         * TABLESPACE is dm keyword
         */
        TABLESPACE("TABLESPACE"),
        /**
         * TASK is dm keyword
         */
        TASK("TASK"),
        /**
         * TEMPLATE is dm keyword
         */
        TEMPLATE("TEMPLATE"),
        /**
         * TEMPORARY is dm keyword
         */
        TEMPORARY("TEMPORARY"),
        /**
         * TEXT is dm keyword
         */
        TEXT("TEXT"),
        /**
         * THAN is dm keyword
         */
        THAN("THAN"),
        /**
         * THEN is dm keyword
         */
        THEN("THEN"),
        /**
         * THREAD is dm keyword
         */
        THREAD("THREAD"),
        /**
         * THROW is dm keyword
         */
        THROW("THROW"),
        /**
         * TIES is dm keyword
         */
        TIES("TIES"),
        /**
         * TIME is dm keyword
         */
        TIME("TIME"),
        /**
         * TIMER is dm keyword
         */
        TIMER("TIMER"),
        /**
         * TIMES is dm keyword
         */
        TIMES("TIMES"),
        /**
         * THTIMESTAMPEN is dm keyword
         */
        TIMESTAMP("TIMESTAMP"),
        /**
         * TIMESTAMPADD is dm keyword
         */
        TIMESTAMPADD("TIMESTAMPADD"),
        /**
         * TIMESTAMPDIFF is dm keyword
         */
        TIMESTAMPDIFF("TIMESTAMPDIFF"),
        /**
         * TIME_ZONE is dm keyword
         */
        TIME_ZONE("TIME_ZONE"),
        /**
         * TINYINT is dm keyword
         */
        TINYINT("TINYINT"),
        /**
         * TO is dm keyword
         */
        TO("TO"),
        /**
         * TOP is dm keyword
         */
        TOP("TOP"),
        /**
         * TRACE is dm keyword
         */
        TRACE("TRACE"),
        /**
         * TRAILING is dm keyword
         */
        TRAILING("TRAILING"),
        /**
         * TRANSACTION is dm keyword
         */
        TRANSACTION("TRANSACTION"),
        /**
         * TRANSACTIONAL is dm keyword
         */
        TRANSACTIONAL("TRANSACTIONAL"),
        /**
         * TRIGGER is dm keyword
         */
        TRIGGER("TRIGGER"),
        /**
         * TRIGGERS is dm keyword
         */
        TRIGGERS("TRIGGERS"),
        /**
         * TRIM is dm keyword
         */
        TRIM("TRIM"),
        /**
         * TRUNCATE is dm keyword
         */
        TRUNCATE("TRUNCATE"),
        /**
         * TRUNCSIZE is dm keyword
         */
        TRUNCSIZE("TRUNCSIZE"),
        /**
         * TRXID is dm keyword
         */
        TRXID("TRXID"),
        /**
         * TRY is dm keyword
         */
        TRY("TRY"),
        /**
         * TYPE is dm keyword
         */
        TYPE("TYPE"),
        /**
         * TYPEDEF is dm keyword
         */
        TYPEDEF("TYPEDEF"),
        /**
         * TYPEOF is dm keyword
         */
        TYPEOF("TYPEOF"),
        /**
         * UINT is dm keyword
         */
        UINT("UINT"),
        /**
         * ULONG is dm keyword
         */
        ULONG("ULONG"),
        /**
         * UNBOUNDED is dm keyword
         */
        UNBOUNDED("UNBOUNDED"),
        /**
         * UNCOMMITTED is dm keyword
         */
        UNCOMMITTED("UNCOMMITTED"),
        /**
         * UNDER is dm keyword
         */
        UNDER("UNDER"),
        /**
         * UNION is dm keyword
         */
        UNION("UNION"),
        /**
         * UNIQUE is dm keyword
         */
        UNIQUE("UNIQUE"),
        /**
         * UNLIMITED is dm keyword
         */
        UNLIMITED("UNLIMITED"),
        /**
         * UNLOCK is dm keyword
         */
        UNLOCK("UNLOCK"),
        /**
         * UNPIVOT is dm keyword
         */
        UNPIVOT("UNPIVOT"),
        /**
         * UNTIL is dm keyword
         */
        UNTIL("UNTIL"),
        /**
         * UNUSABLE is dm keyword
         */
        UNUSABLE("UNUSABLE"),
        /**
         * UP is dm keyword
         */
        UP("UP"),
        /**
         * UPDATE is dm keyword
         */
        UPDATE("UPDATE"),
        /**
         * UPDATING is dm keyword
         */
        UPDATING("UPDATING"),
        /**
         * USAGE is dm keyword
         */
        USAGE("USAGE"),
        /**
         * USER is dm keyword
         */
        USER("USER"),
        /**
         * USE_HASH is dm keyword
         */
        USE_HASH("USE_HASH"),
        /**
         * USE_MERGE is dm keyword
         */
        USE_MERGE("USE_MERGE"),
        /**
         * USE_NL is dm keyword
         */
        USE_NL("USE_NL"),
        /**
         * USE_NL_WITH_INDEX is dm keyword
         */
        USE_NL_WITH_INDEX("USE_NL_WITH_INDEX"),
        /**
         * USHORT is dm keyword
         */
        USHORT("USHORT"),
        /**
         * USING is dm keyword
         */
        USING("USING"),
        /**
         * VALUE is dm keyword
         */
        VALUE("VALUE"),
        /**
         * VALUES is dm keyword
         */
        VALUES("VALUES"),
        /**
         * VARBINARY is dm keyword
         */
        VARBINARY("VARBINARY"),
        /**
         * VARCHAR is dm keyword
         */
        VARCHAR("VARCHAR"),
        /**
         * VARCHAR2 is dm keyword
         */
        VARCHAR2("VARCHAR2"),
        /**
         * VARIANCE is dm keyword
         */
        VARIANCE("VARIANCE"),
        /**
         * VARRAY is dm keyword
         */
        VARRAY("VARRAY"),
        /**
         * VARYING is dm keyword
         */
        VARYING("VARYING"),
        /**
         * VERIFY is dm keyword
         */
        VERIFY("VERIFY"),
        /**
         * VERSIONS is dm keyword
         */
        VERSIONS("VERSIONS"),
        /**
         * VERSIONS_STARTTIME is dm keyword
         */
        VERSIONS_STARTTIME("VERSIONS_STARTTIME"),
        /**
         * VERSIONS_ENDTIME is dm keyword
         */
        VERSIONS_ENDTIME("VERSIONS_ENDTIME"),
        /**
         * VERSIONS_STARTTRXID is dm keyword
         */
        VERSIONS_STARTTRXID("VERSIONS_STARTTRXID"),
        /**
         * VERSIONS_ENDTRXID is dm keyword
         */
        VERSIONS_ENDTRXID("VERSIONS_ENDTRXID"),
        /**
         * VERSIONS_OPERATION is dm keyword
         */
        VERSIONS_OPERATION("VERSIONS_OPERATION"),
        /**
         * VERTICAL is dm keyword
         */
        VERTICAL("VERTICAL"),
        /**
         * VIEW is dm keyword
         */
        VIEW("VIEW"),
        /**
         * VIRTUAL is dm keyword
         */
        VIRTUAL("VIRTUAL"),
        /**
         * VISIBLE is dm keyword
         */
        VISIBLE("VISIBLE"),
        /**
         * VOID is dm keyword
         */
        VOID("VOID"),
        /**
         * VOLATILE is dm keyword
         */
        VOLATILE("VOLATILE"),
        /**
         * VSIZE is dm keyword
         */
        VSIZE("VSIZE"),
        /**
         * WAIT is dm keyword
         */
        WAIT("WAIT"),
        /**
         * WEEK is dm keyword
         */
        WEEK("WEEK"),
        /**
         * WHEN is dm keyword
         */
        WHEN("WHEN"),
        /**
         * WHENEVER is dm keyword
         */
        WHENEVER("WHENEVER"),
        /**
         * WHERE is dm keyword
         */
        WHERE("WHERE"),
        /**
         * WITH is dm keyword
         */
        WITH("WITH"),
        /**
         * WHILE is dm keyword
         */
        WHILE("WHILE"),
        /**
         * WITHIN is dm keyword
         */
        WITHIN("WITHIN"),
        /**
         * WITHOUT is dm keyword
         */
        WITHOUT("WITHOUT"),
        /**
         * WORK is dm keyword
         */
        WORK("WORK"),
        /**
         * WRAPPED is dm keyword
         */
        WRAPPED("WRAPPED"),
        /**
         * WRITE is dm keyword
         */
        WRITE("WRITE"),
        /**
         * XML is dm keyword
         */
        XML("XML"),
        /**
         * YEAR is dm keyword
         */
        YEAR("YEAR"),
        /**
         * ZONE is dm keyword
         */
        ZONE("ZONE");
        /**
         * The Name.
         */
        public final String name;

        DmKeyword(String name) {
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
        boolean check = check(fieldOrTableName);
        // dm
        // we are recommend table name and column name must uppercase.
        // if exists full uppercase, the table name or column name does't bundle escape symbol.
        if (!check && isUppercase(fieldOrTableName)) {
            return false;
        }
        return true;
    }

    private static boolean isUppercase(String fieldOrTableName) {
        if (fieldOrTableName == null) {
            return false;
        }
        char[] chars = fieldOrTableName.toCharArray();
        for (char ch : chars) {
            if (ch >= 'a' && ch <= 'z') {
                return false;
            }
        }
        return true;
    }
}
