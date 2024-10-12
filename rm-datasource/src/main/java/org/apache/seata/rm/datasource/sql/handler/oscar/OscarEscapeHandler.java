/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.rm.datasource.sql.handler.oscar;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.sqlparser.EscapeHandler;
import org.apache.seata.sqlparser.struct.ColumnMeta;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.apache.seata.sqlparser.util.JdbcConstants;

/**
 * The type OSCAR keyword checker.
 *
 */
@LoadLevel(name = JdbcConstants.OSCAR)
public class OscarEscapeHandler implements EscapeHandler {

    protected Set<String> keywordSet = Arrays.stream(OscarKeyword.values()).map(OscarKeyword::name).collect(Collectors.toSet());

    /**
     * OSCAR keyword
     */
    private enum OscarKeyword {
        /**
         * ABORT is oscar keyword
         */
        ABORT("ABORT"),
        /**
         * ABSOLUTE is oscar keyword
         */
        ABSOLUTE("ABSOLUTE"),
        /**
         * ACCESS is oscar keyword
         */
        ACCESS("ACCESS"),
        /**
         * ACCESSED is oscar keyword
         */
        ACCESSED("ACCESSED"),
        /**
         * ACTION is oscar keyword
         */
        ACTION("ACTION"),
        /**
         * ADD is oscar keyword
         */
        ADD("ADD"),
        /**
         * ADMIN is oscar keyword
         */
        ADMIN("ADMIN"),
        /**
         * ADVISOR is oscar keyword
         */
        ADVISOR("ADVISOR"),
        /**
         * AFTER is oscar keyword
         */
        AFTER("AFTER"),
        /**
         * AGGREGATE is oscar keyword
         */
        AGGREGATE("AGGREGATE"),
        /**
         * ALTER is oscar keyword
         */
        ALTER("ALTER"),
        /**
         * ALWAYS is oscar keyword
         */
        ALWAYS("ALWAYS"),
        /**
         * ANALYSE is oscar keyword
         */
        ANALYSE("ANALYSE"),
        /**
         * ANALYZE is oscar keyword
         */
        ANALYZE("ANALYZE"),
        /**
         * ANALYZER is oscar keyword
         */
        ANALYZER("ANALYZER"),
        /**
         * APP is oscar keyword
         */
        APP("APP"),
        /**
         * ARCHIVE is oscar keyword
         */
        ARCHIVE("ARCHIVE"),
        /**
         * ARCHIVELOG is oscar keyword
         */
        ARCHIVELOG("ARCHIVELOG"),
        /**
         * ARE is oscar keyword
         */
        ARE("ARE"),
        /**
         * ARRAY is oscar keyword
         */
        ARRAY("ARRAY"),
        /**
         * ASC is oscar keyword
         */
        ASC("ASC"),
        /**
         * ASSERTION is oscar keyword
         */
        ASSERTION("ASSERTION"),
        /**
         * ASSIGNMENT is oscar keyword
         */
        ASSIGNMENT("ASSIGNMENT"),
        /**
         * AST is oscar keyword
         */
        AST("AST"),
        /**
         * ASYNC is oscar keyword
         */
        ASYNC("ASYNC"),
        /**
         * ATTRIBUTES is oscar keyword
         */
        ATTRIBUTES("ATTRIBUTES"),
        /**
         * AUDIT is oscar keyword
         */
        AUDIT("AUDIT"),
        /**
         * AUDITFILE is oscar keyword
         */
        AUDITFILE("AUDITFILE"),
        /**
         * AUTHID is oscar keyword
         */
        AUTHID("AUTHID"),
        /**
         * AUTHORIZATION is oscar keyword
         */
        AUTHORIZATION("AUTHORIZATION"),
        /**
         * AUTO is oscar keyword
         */
        AUTO("AUTO"),
        /**
         * AUTO_INCREMENT is oscar keyword
         */
        AUTO_INCREMENT("AUTO_INCREMENT"),
        /**
         * AUTOEXTEND is oscar keyword
         */
        AUTOEXTEND("AUTOEXTEND"),
        /**
         * BACKUP is oscar keyword
         */
        BACKUP("BACKUP"),
        /**
         * BACKWARD is oscar keyword
         */
        BACKWARD("BACKWARD"),
        /**
         * BASICANALYZER is oscar keyword
         */
        BASICANALYZER("BASICANALYZER"),
        /**
         * BATCHSIZE is oscar keyword
         */
        BATCHSIZE("BATCHSIZE"),
        /**
         * BEFORE is oscar keyword
         */
        BEFORE("BEFORE"),
        /**
         * BEGIN is oscar keyword
         */
        BEGIN("BEGIN"),
        /**
         * BETWEEN is oscar keyword
         */
        BETWEEN("BETWEEN"),
        /**
         * BIGINT is oscar keyword
         */
        BIGINT("BIGINT"),
        /**
         * BINARY is oscar keyword
         */
        BINARY("BINARY"),
        /**
         * BINLOG is oscar keyword
         */
        BINLOG("BINLOG"),
        /**
         * BIT is oscar keyword
         */
        BIT("BIT"),
        /**
         * BITMAP is oscar keyword
         */
        BITMAP("BITMAP"),
        /**
         * BLOCK is oscar keyword
         */
        BLOCK("BLOCK"),
        /**
         * BODY is oscar keyword
         */
        BODY("BODY"),
        /**
         * BOOLEAN is oscar keyword
         */
        BOOLEAN("BOOLEAN"),
        /**
         * BOTH is oscar keyword
         */
        BOTH("BOTH"),
        /**
         * BPCHAR is oscar keyword
         */
        BPCHAR("BPCHAR"),
        /**
         * BUFFER is oscar keyword
         */
        BUFFER("BUFFER"),
        /**
         * BUFFER_CACHE is oscar keyword
         */
        BUFFER_CACHE("BUFFER_CACHE"),
        /**
         * BUFFER_POOL is oscar keyword
         */
        BUFFER_POOL("BUFFER_POOL"),
        /**
         * BUILD is oscar keyword
         */
        BUILD("BUILD"),
        /**
         * BULK is oscar keyword
         */
        BULK("BULK"),
        /**
         * BY is oscar keyword
         */
        BY("BY"),
        /**
         * BYTE is oscar keyword
         */
        BYTE("BYTE"),
        /**
         * CACHE is oscar keyword
         */
        CACHE("CACHE"),
        /**
         * CALL is oscar keyword
         */
        CALL("CALL"),
        /**
         * CALLED is oscar keyword
         */
        CALLED("CALLED"),
        /**
         * CANCEL is oscar keyword
         */
        CANCEL("CANCEL"),
        /**
         * CASCADED is oscar keyword
         */
        CASCADED("CASCADED"),
        /**
         * CDC is oscar keyword
         */
        CDC("CDC"),
        /**
         * CHAIN is oscar keyword
         */
        CHAIN("CHAIN"),
        /**
         * CHANGE is oscar keyword
         */
        CHANGE("CHANGE"),
        /**
         * CHARACTERISTICS is oscar keyword
         */
        CHARACTERISTICS("CHARACTERISTICS"),
        /**
         * CHARACTERSET is oscar keyword
         */
        CHARACTERSET("CHARACTERSET"),
        /**
         * CHEAT is oscar keyword
         */
        CHEAT("CHEAT"),
        /**
         * CHECKPOINT is oscar keyword
         */
        CHECKPOINT("CHECKPOINT"),
        /**
         * CHINESEANALYZER is oscar keyword
         */
        CHINESEANALYZER("CHINESEANALYZER"),
        /**
         * CHUNK is oscar keyword
         */
        CHUNK("CHUNK"),
        /**
         * CJKANALYZER is oscar keyword
         */
        CJKANALYZER("CJKANALYZER"),
        /**
         * CLASS is oscar keyword
         */
        CLASS("CLASS"),
        /**
         * CLEAN is oscar keyword
         */
        CLEAN("CLEAN"),
        /**
         * CLOSE is oscar keyword
         */
        CLOSE("CLOSE"),
        /**
         * CLUSTER is oscar keyword
         */
        CLUSTER("CLUSTER"),
        /**
         * COLUMNS is oscar keyword
         */
        COLUMNS("COLUMNS"),
        /**
         * COMMENT is oscar keyword
         */
        COMMENT("COMMENT"),
        /**
         * COMMENTS is oscar keyword
         */
        COMMENTS("COMMENTS"),
        /**
         * COMMIT is oscar keyword
         */
        COMMIT("COMMIT"),
        /**
         * COMMITTED is oscar keyword
         */
        COMMITTED("COMMITTED"),
        /**
         * COMPILE is oscar keyword
         */
        COMPILE("COMPILE"),
        /**
         * COMPLETE is oscar keyword
         */
        COMPLETE("COMPLETE"),
        /**
         * COMPRESS is oscar keyword
         */
        COMPRESS("COMPRESS"),
        /**
         * CONCAT is oscar keyword
         */
        CONCAT("CONCAT"),
        /**
         * CONFIGURATION is oscar keyword
         */
        CONFIGURATION("CONFIGURATION"),
        /**
         * CONNECT is oscar keyword
         */
        CONNECT("CONNECT"),
        /**
         * CONNECT_BY_ISCYCLE is oscar keyword
         */
        CONNECT_BY_ISCYCLE("CONNECT_BY_ISCYCLE"),
        /**
         * CONNECT_BY_ISLEAF is oscar keyword
         */
        CONNECT_BY_ISLEAF("CONNECT_BY_ISLEAF"),
        /**
         * CONNECT_BY_ROOT is oscar keyword
         */
        CONNECT_BY_ROOT("CONNECT_BY_ROOT"),
        /**
         * CONSTRAINTS is oscar keyword
         */
        CONSTRAINTS("CONSTRAINTS"),
        /**
         * CONTENT is oscar keyword
         */
        CONTENT("CONTENT"),
        /**
         * CONTEXT is oscar keyword
         */
        CONTEXT("CONTEXT"),
        /**
         * CONTINUE is oscar keyword
         */
        CONTINUE("CONTINUE"),
        /**
         * CONTROLFILE is oscar keyword
         */
        CONTROLFILE("CONTROLFILE"),
        /**
         * CONVERSION is oscar keyword
         */
        CONVERSION("CONVERSION"),
        /**
         * COPY is oscar keyword
         */
        COPY("COPY"),
        /**
         * CROSS is oscar keyword
         */
        CROSS("CROSS"),
        /**
         * CSV is oscar keyword
         */
        CSV("CSV"),
        /**
         * CUBE is oscar keyword
         */
        CUBE("CUBE"),
        /**
         * CURRENT is oscar keyword
         */
        CURRENT("CURRENT"),
        /**
         * CURRENT_USER is oscar keyword
         */
        CURRENT_USER("CURRENT_USER"),
        /**
         * CURSOR is oscar keyword
         */
        CURSOR("CURSOR"),
        /**
         * CYCLE is oscar keyword
         */
        CYCLE("CYCLE"),
        /**
         * DATA is oscar keyword
         */
        DATA("DATA"),
        /**
         * DATABASE is oscar keyword
         */
        DATABASE("DATABASE"),
        /**
         * DATABASELINK is oscar keyword
         */
        DATABASELINK("DATABASELINK"),
        /**
         * DATAFILE is oscar keyword
         */
        DATAFILE("DATAFILE"),
        /**
         * DATAFILETYPE is oscar keyword
         */
        DATAFILETYPE("DATAFILETYPE"),
        /**
         * DATE is oscar keyword
         */
        DATE("DATE"),
        /**
         * DATE_ADD is oscar keyword
         */
        DATE_ADD("DATE_ADD"),
        /**
         * DATE_SUB is oscar keyword
         */
        DATE_SUB("DATE_SUB"),
        /**
         * DATEFORMAT is oscar keyword
         */
        DATEFORMAT("DATEFORMAT"),
        /**
         * DATETIME is oscar keyword
         */
        DATETIME("DATETIME"),
        /**
         * DAY is oscar keyword
         */
        DAY("DAY"),
        /**
         * DBA is oscar keyword
         */
        DBA("DBA"),
        /**
         * DEALLOCATE is oscar keyword
         */
        DEALLOCATE("DEALLOCATE"),
        /**
         * DEBUG is oscar keyword
         */
        DEBUG("DEBUG"),
        /**
         * DEC is oscar keyword
         */
        DEC("DEC"),
        /**
         * DECLARE is oscar keyword
         */
        DECLARE("DECLARE"),
        /**
         * DECODE is oscar keyword
         */
        DECODE("DECODE"),
        /**
         * DECRYPT is oscar keyword
         */
        DECRYPT("DECRYPT"),
        /**
         * DEFERRABLE is oscar keyword
         */
        DEFERRABLE("DEFERRABLE"),
        /**
         * DEFERRED is oscar keyword
         */
        DEFERRED("DEFERRED"),
        /**
         * DEFINER is oscar keyword
         */
        DEFINER("DEFINER"),
        /**
         * DELETE is oscar keyword
         */
        DELETE("DELETE"),
        /**
         * DELIMITED is oscar keyword
         */
        DELIMITED("DELIMITED"),
        /**
         * DELIMITER is oscar keyword
         */
        DELIMITER("DELIMITER"),
        /**
         * DELIMITERS is oscar keyword
         */
        DELIMITERS("DELIMITERS"),
        /**
         * DEMAND is oscar keyword
         */
        DEMAND("DEMAND"),
        /**
         * DENSE_RANK is oscar keyword
         */
        DENSE_RANK("DENSE_RANK"),
        /**
         * DESC is oscar keyword
         */
        DESC("DESC"),
        /**
         * DESCRIPTION is oscar keyword
         */
        DESCRIPTION("DESCRIPTION"),
        /**
         * DETERMINISTIC is oscar keyword
         */
        DETERMINISTIC("DETERMINISTIC"),
        /**
         * DIRECTORY is oscar keyword
         */
        DIRECTORY("DIRECTORY"),
        /**
         * DISABLE is oscar keyword
         */
        DISABLE("DISABLE"),
        /**
         * DOCUMENT is oscar keyword
         */
        DOCUMENT("DOCUMENT"),
        /**
         * DOMAIN is oscar keyword
         */
        DOMAIN("DOMAIN"),
        /**
         * DOUBLE is oscar keyword
         */
        DOUBLE("DOUBLE"),
        /**
         * DUMP is oscar keyword
         */
        DUMP("DUMP"),
        /**
         * EACH is oscar keyword
         */
        EACH("EACH"),
        /**
         * ELOG is oscar keyword
         */
        ELOG("ELOG"),
        /**
         * ELT is oscar keyword
         */
        ELT("ELT"),
        /**
         * EMPTY is oscar keyword
         */
        EMPTY("EMPTY"),
        /**
         * ENABLE is oscar keyword
         */
        ENABLE("ENABLE"),
        /**
         * ENCODING is oscar keyword
         */
        ENCODING("ENCODING"),
        /**
         * ENCRYPT is oscar keyword
         */
        ENCRYPT("ENCRYPT"),
        /**
         * ENCRYPTED is oscar keyword
         */
        ENCRYPTED("ENCRYPTED"),
        /**
         * ENCRYPTION is oscar keyword
         */
        ENCRYPTION("ENCRYPTION"),
        /**
         * END is oscar keyword
         */
        END("END"),
        /**
         * ERROR is oscar keyword
         */
        ERROR("ERROR"),
        /**
         * ERRORS is oscar keyword
         */
        ERRORS("ERRORS"),
        /**
         * ESCALATION is oscar keyword
         */
        ESCALATION("ESCALATION"),
        /**
         * ESCAPE is oscar keyword
         */
        ESCAPE("ESCAPE"),
        /**
         * EVENTS is oscar keyword
         */
        EVENTS("EVENTS"),
        /**
         * EXCHANGE is oscar keyword
         */
        EXCHANGE("EXCHANGE"),
        /**
         * EXCLUDING is oscar keyword
         */
        EXCLUDING("EXCLUDING"),
        /**
         * EXCLUSIVE is oscar keyword
         */
        EXCLUSIVE("EXCLUSIVE"),
        /**
         * EXEC is oscar keyword
         */
        EXEC("EXEC"),
        /**
         * EXECUTE is oscar keyword
         */
        EXECUTE("EXECUTE"),
        /**
         * EXPLAIN is oscar keyword
         */
        EXPLAIN("EXPLAIN"),
        /**
         * EXPORT is oscar keyword
         */
        EXPORT("EXPORT"),
        /**
         * EXTEND is oscar keyword
         */
        EXTEND("EXTEND"),
        /**
         * EXTERNALLY is oscar keyword
         */
        EXTERNALLY("EXTERNALLY"),
        /**
         * FAILOVER is oscar keyword
         */
        FAILOVER("FAILOVER"),
        /**
         * FALSE is oscar keyword
         */
        FALSE("FALSE"),
        /**
         * FAR is oscar keyword
         */
        FAR("FAR"),
        /**
         * FAST is oscar keyword
         */
        FAST("FAST"),
        /**
         * FAULT is oscar keyword
         */
        FAULT("FAULT"),
        /**
         * FETCH is oscar keyword
         */
        FETCH("FETCH"),
        /**
         * FIELD is oscar keyword
         */
        FIELD("FIELD"),
        /**
         * FIELDS is oscar keyword
         */
        FIELDS("FIELDS"),
        /**
         * FIELDTERMINATOR is oscar keyword
         */
        FIELDTERMINATOR("FIELDTERMINATOR"),
        /**
         * FILE is oscar keyword
         */
        FILE("FILE"),
        /**
         * FILESIZE is oscar keyword
         */
        FILESIZE("FILESIZE"),
        /**
         * FILL is oscar keyword
         */
        FILL("FILL"),
        /**
         * FILTER is oscar keyword
         */
        FILTER("FILTER"),
        /**
         * FIRE_TRIGGERS is oscar keyword
         */
        FIRE_TRIGGERS("FIRE_TRIGGERS"),
        /**
         * FIRST is oscar keyword
         */
        FIRST("FIRST"),
        /**
         * FIRSTROW is oscar keyword
         */
        FIRSTROW("FIRSTROW"),
        /**
         * FLUSH is oscar keyword
         */
        FLUSH("FLUSH"),
        /**
         * FOLLOWING is oscar keyword
         */
        FOLLOWING("FOLLOWING"),
        /**
         * FORCE is oscar keyword
         */
        FORCE("FORCE"),
        /**
         * FOREIGNKEY_CONSTRAINTS is oscar keyword
         */
        FOREIGNKEY_CONSTRAINTS("FOREIGNKEY_CONSTRAINTS"),
        /**
         * FOREVER is oscar keyword
         */
        FOREVER("FOREVER"),
        /**
         * FORMATFILE is oscar keyword
         */
        FORMATFILE("FORMATFILE"),
        /**
         * FORWARD is oscar keyword
         */
        FORWARD("FORWARD"),
        /**
         * FREELISTS is oscar keyword
         */
        FREELISTS("FREELISTS"),
        /**
         * FREEPOOLS is oscar keyword
         */
        FREEPOOLS("FREEPOOLS"),
        /**
         * FULL is oscar keyword
         */
        FULL("FULL"),
        /**
         * FULLTEXT is oscar keyword
         */
        FULLTEXT("FULLTEXT"),
        /**
         * FUNCTION is oscar keyword
         */
        FUNCTION("FUNCTION"),
        /**
         * G is oscar keyword
         */
        G("G"),
        /**
         * GB is oscar keyword
         */
        GB("GB"),
        /**
         * GBK is oscar keyword
         */
        GBK("GBK"),
        /**
         * GCOV is oscar keyword
         */
        GCOV("GCOV"),
        /**
         * GENERATED is oscar keyword
         */
        GENERATED("GENERATED"),
        /**
         * GEOGRAPHY is oscar keyword
         */
        GEOGRAPHY("GEOGRAPHY"),
        /**
         * GEOMETRY is oscar keyword
         */
        GEOMETRY("GEOMETRY"),
        /**
         * GET is oscar keyword
         */
        GET("GET"),
        /**
         * GETCLOBVAL is oscar keyword
         */
        GETCLOBVAL("GETCLOBVAL"),
        /**
         * GETSTRINGVAL is oscar keyword
         */
        GETSTRINGVAL("GETSTRINGVAL"),
        /**
         * GLOBAL is oscar keyword
         */
        GLOBAL("GLOBAL"),
        /**
         * GLOBAL_NAME is oscar keyword
         */
        GLOBAL_NAME("GLOBAL_NAME"),
        /**
         * GLOBALLY is oscar keyword
         */
        GLOBALLY("GLOBALLY"),
        /**
         * GREATEST is oscar keyword
         */
        GREATEST("GREATEST"),
        /**
         * GROUPING is oscar keyword
         */
        GROUPING("GROUPING"),
        /**
         * GROUPING_ID is oscar keyword
         */
        GROUPING_ID("GROUPING_ID"),
        /**
         * GUARANTEE is oscar keyword
         */
        GUARANTEE("GUARANTEE"),
        /**
         * HANDLER is oscar keyword
         */
        HANDLER("HANDLER"),
        /**
         * HASH is oscar keyword
         */
        HASH("HASH"),
        /**
         * HEADER is oscar keyword
         */
        HEADER("HEADER"),
        /**
         * HEAP is oscar keyword
         */
        HEAP("HEAP"),
        /**
         * HOLD is oscar keyword
         */
        HOLD("HOLD"),
        /**
         * HOUR is oscar keyword
         */
        HOUR("HOUR"),
        /**
         * IDENTIFIED is oscar keyword
         */
        IDENTIFIED("IDENTIFIED"),
        /**
         * IDENTITY is oscar keyword
         */
        IDENTITY("IDENTITY"),
        /**
         * IF is oscar keyword
         */
        IF("IF"),
        /**
         * IGNORE is oscar keyword
         */
        IGNORE("IGNORE"),
        /**
         * ILIKE is oscar keyword
         */
        ILIKE("ILIKE"),
        /**
         * IMMEDIATE is oscar keyword
         */
        IMMEDIATE("IMMEDIATE"),
        /**
         * IMMUTABLE is oscar keyword
         */
        IMMUTABLE("IMMUTABLE"),
        /**
         * IMPLICIT is oscar keyword
         */
        IMPLICIT("IMPLICIT"),
        /**
         * IMPORT is oscar keyword
         */
        IMPORT("IMPORT"),
        /**
         * IMPORT_POLCOL is oscar keyword
         */
        IMPORT_POLCOL("IMPORT_POLCOL"),
        /**
         * INCREMENT is oscar keyword
         */
        INCREMENT("INCREMENT"),
        /**
         * INDEX is oscar keyword
         */
        INDEX("INDEX"),
        /**
         * INDEXES is oscar keyword
         */
        INDEXES("INDEXES"),
        /**
         * INHERITS is oscar keyword
         */
        INHERITS("INHERITS"),
        /**
         * INIT is oscar keyword
         */
        INIT("INIT"),
        /**
         * INITIAL is oscar keyword
         */
        INITIAL("INITIAL"),
        /**
         * INITIALIZED is oscar keyword
         */
        INITIALIZED("INITIALIZED"),
        /**
         * INITIALLY is oscar keyword
         */
        INITIALLY("INITIALLY"),
        /**
         * INITRANS is oscar keyword
         */
        INITRANS("INITRANS"),
        /**
         * INNER is oscar keyword
         */
        INNER("INNER"),
        /**
         * INOUT is oscar keyword
         */
        INOUT("INOUT"),
        /**
         * INPUT is oscar keyword
         */
        INPUT("INPUT"),
        /**
         * INSENSITIVE is oscar keyword
         */
        INSENSITIVE("INSENSITIVE"),
        /**
         * INSERT is oscar keyword
         */
        INSERT("INSERT"),
        /**
         * INSTEAD is oscar keyword
         */
        INSTEAD("INSTEAD"),
        /**
         * INTERVAL is oscar keyword
         */
        INTERVAL("INTERVAL"),
        /**
         * INVALIDATE is oscar keyword
         */
        INVALIDATE("INVALIDATE"),
        /**
         * INVISIBLE is oscar keyword
         */
        INVISIBLE("INVISIBLE"),
        /**
         * INVOKER is oscar keyword
         */
        INVOKER("INVOKER"),
        /**
         * IP is oscar keyword
         */
        IP("IP"),
        /**
         * IS is oscar keyword
         */
        IS("IS"),
        /**
         * ISNULL is oscar keyword
         */
        ISNULL("ISNULL"),
        /**
         * ISOLATION is oscar keyword
         */
        ISOLATION("ISOLATION"),
        /**
         * JOIN is oscar keyword
         */
        JOIN("JOIN"),
        /**
         * JSON is oscar keyword
         */
        JSON("JSON"),
        /**
         * JSON_TABLE is oscar keyword
         */
        JSON_TABLE("JSON_TABLE"),
        /**
         * JSON_VALUE is oscar keyword
         */
        JSON_VALUE("JSON_VALUE"),
        /**
         * K is oscar keyword
         */
        K("K"),
        /**
         * KB is oscar keyword
         */
        KB("KB"),
        /**
         * KEEP is oscar keyword
         */
        KEEP("KEEP"),
        /**
         * KEEPIDENTITY is oscar keyword
         */
        KEEPIDENTITY("KEEPIDENTITY"),
        /**
         * KEEPNULLS is oscar keyword
         */
        KEEPNULLS("KEEPNULLS"),
        /**
         * KEY is oscar keyword
         */
        KEY("KEY"),
        /**
         * KEYSTORE is oscar keyword
         */
        KEYSTORE("KEYSTORE"),
        /**
         * KILL is oscar keyword
         */
        KILL("KILL"),
        /**
         * KILOBYTES_PER_BATCH is oscar keyword
         */
        KILOBYTES_PER_BATCH("KILOBYTES_PER_BATCH"),
        /**
         * KSTORE is oscar keyword
         */
        KSTORE("KSTORE"),
        /**
         * LABEL is oscar keyword
         */
        LABEL("LABEL"),
        /**
         * LANCOMPILER is oscar keyword
         */
        LANCOMPILER("LANCOMPILER"),
        /**
         * LANGUAGE is oscar keyword
         */
        LANGUAGE("LANGUAGE"),
        /**
         * LAST is oscar keyword
         */
        LAST("LAST"),
        /**
         * LASTROW is oscar keyword
         */
        LASTROW("LASTROW"),
        /**
         * LC_COLLATE is oscar keyword
         */
        LC_COLLATE("LC_COLLATE"),
        /**
         * LC_CTYPE is oscar keyword
         */
        LC_CTYPE("LC_CTYPE"),
        /**
         * LDRTRIM is oscar keyword
         */
        LDRTRIM("LDRTRIM"),
        /**
         * LEADING is oscar keyword
         */
        LEADING("LEADING"),
        /**
         * LEAK is oscar keyword
         */
        LEAK("LEAK"),
        /**
         * LEAST is oscar keyword
         */
        LEAST("LEAST"),
        /**
         * LEFT is oscar keyword
         */
        LEFT("LEFT"),
        /**
         * LESS is oscar keyword
         */
        LESS("LESS"),
        /**
         * LIFETIME is oscar keyword
         */
        LIFETIME("LIFETIME"),
        /**
         * LIKE is oscar keyword
         */
        LIKE("LIKE"),
        /**
         * LIMIT is oscar keyword
         */
        LIMIT("LIMIT"),
        /**
         * LIST is oscar keyword
         */
        LIST("LIST"),
        /**
         * LISTEN is oscar keyword
         */
        LISTEN("LISTEN"),
        /**
         * LOAD is oscar keyword
         */
        LOAD("LOAD"),
        /**
         * LOB is oscar keyword
         */
        LOB("LOB"),
        /**
         * LOCAL is oscar keyword
         */
        LOCAL("LOCAL"),
        /**
         * LOCATION is oscar keyword
         */
        LOCATION("LOCATION"),
        /**
         * LOCK is oscar keyword
         */
        LOCK("LOCK"),
        /**
         * LOCKED is oscar keyword
         */
        LOCKED("LOCKED"),
        /**
         * LOG is oscar keyword
         */
        LOG("LOG"),
        /**
         * LOGFILE is oscar keyword
         */
        LOGFILE("LOGFILE"),
        /**
         * LOGGING is oscar keyword
         */
        LOGGING("LOGGING"),
        /**
         * LOGICAL is oscar keyword
         */
        LOGICAL("LOGICAL"),
        /**
         * LONG is oscar keyword
         */
        LONG("LONG"),
        /**
         * LOOP is oscar keyword
         */
        LOOP("LOOP"),
        /**
         * LRTRIM is oscar keyword
         */
        LRTRIM("LRTRIM"),
        /**
         * LSN is oscar keyword
         */
        LSN("LSN"),
        /**
         * LTRIM is oscar keyword
         */
        LTRIM("LTRIM"),
        /**
         * M is oscar keyword
         */
        M("M"),
        /**
         * MAINTAIN_INDEX is oscar keyword
         */
        MAINTAIN_INDEX("MAINTAIN_INDEX"),
        /**
         * MAINTENANCE is oscar keyword
         */
        MAINTENANCE("MAINTENANCE"),
        /**
         * MANUAL is oscar keyword
         */
        MANUAL("MANUAL"),
        /**
         * MASKING is oscar keyword
         */
        MASKING("MASKING"),
        /**
         * MATCH is oscar keyword
         */
        MATCH("MATCH"),
        /**
         * MATCHED is oscar keyword
         */
        MATCHED("MATCHED"),
        /**
         * MATERIALIZED is oscar keyword
         */
        MATERIALIZED("MATERIALIZED"),
        /**
         * MAX is oscar keyword
         */
        MAX("MAX"),
        /**
         * MAXERRORS is oscar keyword
         */
        MAXERRORS("MAXERRORS"),
        /**
         * MAXEXTENDS is oscar keyword
         */
        MAXEXTENDS("MAXEXTENDS"),
        /**
         * MAXEXTENTS is oscar keyword
         */
        MAXEXTENTS("MAXEXTENTS"),
        /**
         * MAXSIZE is oscar keyword
         */
        MAXSIZE("MAXSIZE"),
        /**
         * MAXTRANS is oscar keyword
         */
        MAXTRANS("MAXTRANS"),
        /**
         * MAXVALUE is oscar keyword
         */
        MAXVALUE("MAXVALUE"),
        /**
         * MB is oscar keyword
         */
        MB("MB"),
        /**
         * MEMBER is oscar keyword
         */
        MEMBER("MEMBER"),
        /**
         * MEMORY is oscar keyword
         */
        MEMORY("MEMORY"),
        /**
         * MERGE is oscar keyword
         */
        MERGE("MERGE"),
        /**
         * MIN is oscar keyword
         */
        MIN("MIN"),
        /**
         * MINEXTENDS is oscar keyword
         */
        MINEXTENDS("MINEXTENDS"),
        /**
         * MINEXTENTS is oscar keyword
         */
        MINEXTENTS("MINEXTENTS"),
        /**
         * MINSIZE is oscar keyword
         */
        MINSIZE("MINSIZE"),
        /**
         * MINUS is oscar keyword
         */
        MINUS("MINUS"),
        /**
         * MINUTE is oscar keyword
         */
        MINUTE("MINUTE"),
        /**
         * MINVALUE is oscar keyword
         */
        MINVALUE("MINVALUE"),
        /**
         * MISSING is oscar keyword
         */
        MISSING("MISSING"),
        /**
         * MOD is oscar keyword
         */
        MOD("MOD"),
        /**
         * MODE is oscar keyword
         */
        MODE("MODE"),
        /**
         * MODIFY is oscar keyword
         */
        MODIFY("MODIFY"),
        /**
         * MONEY is oscar keyword
         */
        MONEY("MONEY"),
        /**
         * MONTH is oscar keyword
         */
        MONTH("MONTH"),
        /**
         * MOUNT is oscar keyword
         */
        MOUNT("MOUNT"),
        /**
         * MOVE is oscar keyword
         */
        MOVE("MOVE"),
        /**
         * MOVEMENT is oscar keyword
         */
        MOVEMENT("MOVEMENT"),
        /**
         * MULTICOLUMN is oscar keyword
         */
        MULTICOLUMN("MULTICOLUMN"),
        /**
         * MULTIPLE is oscar keyword
         */
        MULTIPLE("MULTIPLE"),
        /**
         * NAME is oscar keyword
         */
        NAME("NAME"),
        /**
         * NAMES is oscar keyword
         */
        NAMES("NAMES"),
        /**
         * NATURAL is oscar keyword
         */
        NATURAL("NATURAL"),
        /**
         * NCHAR is oscar keyword
         */
        NCHAR("NCHAR"),
        /**
         * NEVER is oscar keyword
         */
        NEVER("NEVER"),
        /**
         * NEWLINE is oscar keyword
         */
        NEWLINE("NEWLINE"),
        /**
         * NEXT is oscar keyword
         */
        NEXT("NEXT"),
        /**
         * NEXTVAL is oscar keyword
         */
        NEXTVAL("NEXTVAL"),
        /**
         * NO is oscar keyword
         */
        NO("NO"),
        /**
         * NOARCHIVELOG is oscar keyword
         */
        NOARCHIVELOG("NOARCHIVELOG"),
        /**
         * NOAUDIT is oscar keyword
         */
        NOAUDIT("NOAUDIT"),
        /**
         * NOCACHE is oscar keyword
         */
        NOCACHE("NOCACHE"),
        /**
         * NOCOMPRESS is oscar keyword
         */
        NOCOMPRESS("NOCOMPRESS"),
        /**
         * NOCOPY is oscar keyword
         */
        NOCOPY("NOCOPY"),
        /**
         * NOCYCLE is oscar keyword
         */
        NOCYCLE("NOCYCLE"),
        /**
         * NODE is oscar keyword
         */
        NODE("NODE"),
        /**
         * NOGUARANTEE is oscar keyword
         */
        NOGUARANTEE("NOGUARANTEE"),
        /**
         * NOLOGGING is oscar keyword
         */
        NOLOGGING("NOLOGGING"),
        /**
         * NOMAXVALUE is oscar keyword
         */
        NOMAXVALUE("NOMAXVALUE"),
        /**
         * NOMINVALUE is oscar keyword
         */
        NOMINVALUE("NOMINVALUE"),
        /**
         * NOMOUNT is oscar keyword
         */
        NOMOUNT("NOMOUNT"),
        /**
         * NORMAL is oscar keyword
         */
        NORMAL("NORMAL"),
        /**
         * NOTHING is oscar keyword
         */
        NOTHING("NOTHING"),
        /**
         * NOTIFY is oscar keyword
         */
        NOTIFY("NOTIFY"),
        /**
         * NOTNULL is oscar keyword
         */
        NOTNULL("NOTNULL"),
        /**
         * NOTRIM is oscar keyword
         */
        NOTRIM("NOTRIM"),
        /**
         * NOVALIDATE is oscar keyword
         */
        NOVALIDATE("NOVALIDATE"),
        /**
         * NOWAIT is oscar keyword
         */
        NOWAIT("NOWAIT"),
        /**
         * NVARCHAR2 is oscar keyword
         */
        NVARCHAR2("NVARCHAR2"),
        /**
         * NVL is oscar keyword
         */
        NVL("NVL"),
        /**
         * NVL2 is oscar keyword
         */
        NVL2("NVL2"),
        /**
         * OBJECT is oscar keyword
         */
        OBJECT("OBJECT"),
        /**
         * OF is oscar keyword
         */
        OF("OF"),
        /**
         * OFF is oscar keyword
         */
        OFF("OFF"),
        /**
         * OFFLINE is oscar keyword
         */
        OFFLINE("OFFLINE"),
        /**
         * OFFSET is oscar keyword
         */
        OFFSET("OFFSET"),
        /**
         * OIDS is oscar keyword
         */
        OIDS("OIDS"),
        /**
         * ONLINE is oscar keyword
         */
        ONLINE("ONLINE"),
        /**
         * OPEN is oscar keyword
         */
        OPEN("OPEN"),
        /**
         * OPERATOR is oscar keyword
         */
        OPERATOR("OPERATOR"),
        /**
         * OPTIMIZE is oscar keyword
         */
        OPTIMIZE("OPTIMIZE"),
        /**
         * OPTIMIZE_KSCACHE is oscar keyword
         */
        OPTIMIZE_KSCACHE("OPTIMIZE_KSCACHE"),
        /**
         * OPTION is oscar keyword
         */
        OPTION("OPTION"),
        /**
         * ORACLE is oscar keyword
         */
        ORACLE("ORACLE"),
        /**
         * ORDINALITY is oscar keyword
         */
        ORDINALITY("ORDINALITY"),
        /**
         * ORGANIZATION is oscar keyword
         */
        ORGANIZATION("ORGANIZATION"),
        /**
         * OSCAR is oscar keyword
         */
        OSCAR("OSCAR"),
        /**
         * OUT is oscar keyword
         */
        OUT("OUT"),
        /**
         * OUTER is oscar keyword
         */
        OUTER("OUTER"),
        /**
         * OUTLINE is oscar keyword
         */
        OUTLINE("OUTLINE"),
        /**
         * OVER is oscar keyword
         */
        OVER("OVER"),
        /**
         * OVERFLOW is oscar keyword
         */
        OVERFLOW("OVERFLOW"),
        /**
         * OVERLAPS is oscar keyword
         */
        OVERLAPS("OVERLAPS"),
        /**
         * OVERLAY is oscar keyword
         */
        OVERLAY("OVERLAY"),
        /**
         * OWNER is oscar keyword
         */
        OWNER("OWNER"),
        /**
         * PACKAGE is oscar keyword
         */
        PACKAGE("PACKAGE"),
        /**
         * PAGESIZE is oscar keyword
         */
        PAGESIZE("PAGESIZE"),
        /**
         * PARALLEL is oscar keyword
         */
        PARALLEL("PARALLEL"),
        /**
         * PARAMETER is oscar keyword
         */
        PARAMETER("PARAMETER"),
        /**
         * PARAMINFO is oscar keyword
         */
        PARAMINFO("PARAMINFO"),
        /**
         * PARTIAL is oscar keyword
         */
        PARTIAL("PARTIAL"),
        /**
         * PARTITION is oscar keyword
         */
        PARTITION("PARTITION"),
        /**
         * PARTITIONS is oscar keyword
         */
        PARTITIONS("PARTITIONS"),
        /**
         * PASSING is oscar keyword
         */
        PASSING("PASSING"),
        /**
         * PASSWORD is oscar keyword
         */
        PASSWORD("PASSWORD"),
        /**
         * PATH is oscar keyword
         */
        PATH("PATH"),
        /**
         * PCTFREE is oscar keyword
         */
        PCTFREE("PCTFREE"),
        /**
         * PCTINCREASE is oscar keyword
         */
        PCTINCREASE("PCTINCREASE"),
        /**
         * PCTTHRESHOLD is oscar keyword
         */
        PCTTHRESHOLD("PCTTHRESHOLD"),
        /**
         * PCTUSED is oscar keyword
         */
        PCTUSED("PCTUSED"),
        /**
         * PCTVERSION is oscar keyword
         */
        PCTVERSION("PCTVERSION"),
        /**
         * PENDANT is oscar keyword
         */
        PENDANT("PENDANT"),
        /**
         * PETENTION is oscar keyword
         */
        PETENTION("PETENTION"),
        /**
         * PFILE is oscar keyword
         */
        PFILE("PFILE"),
        /**
         * PIPELINED is oscar keyword
         */
        PIPELINED("PIPELINED"),
        /**
         * PIVOT is oscar keyword
         */
        PIVOT("PIVOT"),
        /**
         * PLACING is oscar keyword
         */
        PLACING("PLACING"),
        /**
         * PLS_INTEGER is oscar keyword
         */
        PLS_INTEGER("PLS_INTEGER"),
        /**
         * POLICY is oscar keyword
         */
        POLICY("POLICY"),
        /**
         * PORT is oscar keyword
         */
        PORT("PORT"),
        /**
         * POSITION is oscar keyword
         */
        POSITION("POSITION"),
        /**
         * PRECEDING is oscar keyword
         */
        PRECEDING("PRECEDING"),
        /**
         * PRECISION is oscar keyword
         */
        PRECISION("PRECISION"),
        /**
         * PREPARE is oscar keyword
         */
        PREPARE("PREPARE"),
        /**
         * PRESERVE is oscar keyword
         */
        PRESERVE("PRESERVE"),
        /**
         * PREVAL is oscar keyword
         */
        PREVAL("PREVAL"),
        /**
         * PRIMARY is oscar keyword
         */
        PRIMARY("PRIMARY"),
        /**
         * PRIOR is oscar keyword
         */
        PRIOR("PRIOR"),
        /**
         * PRIORITY is oscar keyword
         */
        PRIORITY("PRIORITY"),
        /**
         * PRIVILEGES is oscar keyword
         */
        PRIVILEGES("PRIVILEGES"),
        /**
         * PROCEDURAL is oscar keyword
         */
        PROCEDURAL("PROCEDURAL"),
        /**
         * PROCEDURE is oscar keyword
         */
        PROCEDURE("PROCEDURE"),
        /**
         * PUBLIC is oscar keyword
         */
        PUBLIC("PUBLIC"),
        /**
         * PURGE is oscar keyword
         */
        PURGE("PURGE"),
        /**
         * QU is oscar keyword
         */
        QU("QU"),
        /**
         * QUERY is oscar keyword
         */
        QUERY("QUERY"),
        /**
         * QUICK is oscar keyword
         */
        QUICK("QUICK"),
        /**
         * QUOTE is oscar keyword
         */
        QUOTE("QUOTE"),
        /**
         * RAC is oscar keyword
         */
        RAC("RAC"),
        /**
         * RANGE is oscar keyword
         */
        RANGE("RANGE"),
        /**
         * RATIO_TO_REPORT is oscar keyword
         */
        RATIO_TO_REPORT("RATIO_TO_REPORT"),
        /**
         * RAW is oscar keyword
         */
        RAW("RAW"),
        /**
         * READ is oscar keyword
         */
        READ("READ"),
        /**
         * READABLE is oscar keyword
         */
        READABLE("READABLE"),
        /**
         * READS is oscar keyword
         */
        READS("READS"),
        /**
         * READSIZE is oscar keyword
         */
        READSIZE("READSIZE"),
        /**
         * REBUILD is oscar keyword
         */
        REBUILD("REBUILD"),
        /**
         * RECHECK is oscar keyword
         */
        RECHECK("RECHECK"),
        /**
         * RECORDS is oscar keyword
         */
        RECORDS("RECORDS"),
        /**
         * RECOVERY is oscar keyword
         */
        RECOVERY("RECOVERY"),
        /**
         * RECREATE is oscar keyword
         */
        RECREATE("RECREATE"),
        /**
         * RECURSIVE is oscar keyword
         */
        RECURSIVE("RECURSIVE"),
        /**
         * RECYCLE is oscar keyword
         */
        RECYCLE("RECYCLE"),
        /**
         * REFRESH is oscar keyword
         */
        REFRESH("REFRESH"),
        /**
         * REGEXP is oscar keyword
         */
        REGEXP("REGEXP"),
        /**
         * REGION is oscar keyword
         */
        REGION("REGION"),
        /**
         * REJECT is oscar keyword
         */
        REJECT("REJECT"),
        /**
         * RELATIVE is oscar keyword
         */
        RELATIVE("RELATIVE"),
        /**
         * REMOVE is oscar keyword
         */
        REMOVE("REMOVE"),
        /**
         * RENAME is oscar keyword
         */
        RENAME("RENAME"),
        /**
         * REPEATABLE is oscar keyword
         */
        REPEATABLE("REPEATABLE"),
        /**
         * REPLACE is oscar keyword
         */
        REPLACE("REPLACE"),
        /**
         * RESET is oscar keyword
         */
        RESET("RESET"),
        /**
         * RESIZE is oscar keyword
         */
        RESIZE("RESIZE"),
        /**
         * RESOURCE is oscar keyword
         */
        RESOURCE("RESOURCE"),
        /**
         * RESTART is oscar keyword
         */
        RESTART("RESTART"),
        /**
         * RESTORE is oscar keyword
         */
        RESTORE("RESTORE"),
        /**
         * RESTRICT is oscar keyword
         */
        RESTRICT("RESTRICT"),
        /**
         * RESULT is oscar keyword
         */
        RESULT("RESULT"),
        /**
         * RESUME is oscar keyword
         */
        RESUME("RESUME"),
        /**
         * RETENTION is oscar keyword
         */
        RETENTION("RETENTION"),
        /**
         * RETURN is oscar keyword
         */
        RETURN("RETURN"),
        /**
         * RETURN_GENERATED_KEYS is oscar keyword
         */
        RETURN_GENERATED_KEYS("RETURN_GENERATED_KEYS"),
        /**
         * RETURNING is oscar keyword
         */
        RETURNING("RETURNING"),
        /**
         * RETURNS is oscar keyword
         */
        RETURNS("RETURNS"),
        /**
         * REUSE is oscar keyword
         */
        REUSE("REUSE"),
        /**
         * REVERSE is oscar keyword
         */
        REVERSE("REVERSE"),
        /**
         * REVOKE is oscar keyword
         */
        REVOKE("REVOKE"),
        /**
         * REWRITE is oscar keyword
         */
        REWRITE("REWRITE"),
        /**
         * RIGHT is oscar keyword
         */
        RIGHT("RIGHT"),
        /**
         * ROLE is oscar keyword
         */
        ROLE("ROLE"),
        /**
         * ROLLBACK is oscar keyword
         */
        ROLLBACK("ROLLBACK"),
        /**
         * ROLLUP is oscar keyword
         */
        ROLLUP("ROLLUP"),
        /**
         * ROW is oscar keyword
         */
        ROW("ROW"),
        /**
         * ROWDESCRIPTION is oscar keyword
         */
        ROWDESCRIPTION("ROWDESCRIPTION"),
        /**
         * ROWID is oscar keyword
         */
        ROWID("ROWID"),
        /**
         * ROWS is oscar keyword
         */
        ROWS("ROWS"),
        /**
         * ROWS_PER_BATCH is oscar keyword
         */
        ROWS_PER_BATCH("ROWS_PER_BATCH"),
        /**
         * ROWTERMINATOR is oscar keyword
         */
        ROWTERMINATOR("ROWTERMINATOR"),
        /**
         * ROWTYPE is oscar keyword
         */
        ROWTYPE("ROWTYPE"),
        /**
         * RTRIM is oscar keyword
         */
        RTRIM("RTRIM"),
        /**
         * RULE is oscar keyword
         */
        RULE("RULE"),
        /**
         * SAMPLE is oscar keyword
         */
        SAMPLE("SAMPLE"),
        /**
         * SAVEPOINT is oscar keyword
         */
        SAVEPOINT("SAVEPOINT"),
        /**
         * SCAN is oscar keyword
         */
        SCAN("SCAN"),
        /**
         * SCHEMA is oscar keyword
         */
        SCHEMA("SCHEMA"),
        /**
         * SCN is oscar keyword
         */
        SCN("SCN"),
        /**
         * SCROLL is oscar keyword
         */
        SCROLL("SCROLL"),
        /**
         * SECOND is oscar keyword
         */
        SECOND("SECOND"),
        /**
         * SECURITY is oscar keyword
         */
        SECURITY("SECURITY"),
        /**
         * SEGMENT is oscar keyword
         */
        SEGMENT("SEGMENT"),
        /**
         * SEPARATOR is oscar keyword
         */
        SEPARATOR("SEPARATOR"),
        /**
         * SEQUENCE is oscar keyword
         */
        SEQUENCE("SEQUENCE"),
        /**
         * SERIALIZABLE is oscar keyword
         */
        SERIALIZABLE("SERIALIZABLE"),
        /**
         * SESSION is oscar keyword
         */
        SESSION("SESSION"),
        /**
         * SETS is oscar keyword
         */
        SETS("SETS"),
        /**
         * SHARE is oscar keyword
         */
        SHARE("SHARE"),
        /**
         * SHOW is oscar keyword
         */
        SHOW("SHOW"),
        /**
         * SHRINK is oscar keyword
         */
        SHRINK("SHRINK"),
        /**
         * SHRINKLOG is oscar keyword
         */
        SHRINKLOG("SHRINKLOG"),
        /**
         * SHUTDOWN is oscar keyword
         */
        SHUTDOWN("SHUTDOWN"),
        /**
         * SIBLINGS is oscar keyword
         */
        SIBLINGS("SIBLINGS"),
        /**
         * SIGNED is oscar keyword
         */
        SIGNED("SIGNED"),
        /**
         * SILENTLY is oscar keyword
         */
        SILENTLY("SILENTLY"),
        /**
         * SIMILAR is oscar keyword
         */
        SIMILAR("SIMILAR"),
        /**
         * SIMPLE is oscar keyword
         */
        SIMPLE("SIMPLE"),
        /**
         * SINGLE is oscar keyword
         */
        SINGLE("SINGLE"),
        /**
         * SINGLEROW is oscar keyword
         */
        SINGLEROW("SINGLEROW"),
        /**
         * SIZE is oscar keyword
         */
        SIZE("SIZE"),
        /**
         * SKIP is oscar keyword
         */
        SKIP("SKIP"),
        /**
         * SMALLINT is oscar keyword
         */
        SMALLINT("SMALLINT"),
        /**
         * SPACE is oscar keyword
         */
        SPACE("SPACE"),
        /**
         * SPLIT is oscar keyword
         */
        SPLIT("SPLIT"),
        /**
         * STABLE is oscar keyword
         */
        STABLE("STABLE"),
        /**
         * STANDALONE is oscar keyword
         */
        STANDALONE("STANDALONE"),
        /**
         * STANDARDANALYZER is oscar keyword
         */
        STANDARDANALYZER("STANDARDANALYZER"),
        /**
         * START is oscar keyword
         */
        START("START"),
        /**
         * STARTFILE is oscar keyword
         */
        STARTFILE("STARTFILE"),
        /**
         * STARTPOS is oscar keyword
         */
        STARTPOS("STARTPOS"),
        /**
         * STARTTIME is oscar keyword
         */
        STARTTIME("STARTTIME"),
        /**
         * STARTUP is oscar keyword
         */
        STARTUP("STARTUP"),
        /**
         * STATEMENT is oscar keyword
         */
        STATEMENT("STATEMENT"),
        /**
         * STATIC is oscar keyword
         */
        STATIC("STATIC"),
        /**
         * STATISTICS is oscar keyword
         */
        STATISTICS("STATISTICS"),
        /**
         * STDIN is oscar keyword
         */
        STDIN("STDIN"),
        /**
         * STDOUT is oscar keyword
         */
        STDOUT("STDOUT"),
        /**
         * STOP is oscar keyword
         */
        STOP("STOP"),
        /**
         * STOPFILE is oscar keyword
         */
        STOPFILE("STOPFILE"),
        /**
         * STOPPOS is oscar keyword
         */
        STOPPOS("STOPPOS"),
        /**
         * STOPTIME is oscar keyword
         */
        STOPTIME("STOPTIME"),
        /**
         * STOPWORDS is oscar keyword
         */
        STOPWORDS("STOPWORDS"),
        /**
         * STORAGE is oscar keyword
         */
        STORAGE("STORAGE"),
        /**
         * STORE is oscar keyword
         */
        STORE("STORE"),
        /**
         * STORED is oscar keyword
         */
        STORED("STORED"),
        /**
         * STRICT is oscar keyword
         */
        STRICT("STRICT"),
        /**
         * SUBPARTITION is oscar keyword
         */
        SUBPARTITION("SUBPARTITION"),
        /**
         * SUBPARTITIONS is oscar keyword
         */
        SUBPARTITIONS("SUBPARTITIONS"),
        /**
         * SUBSTRING is oscar keyword
         */
        SUBSTRING("SUBSTRING"),
        /**
         * SUCCESSFUL is oscar keyword
         */
        SUCCESSFUL("SUCCESSFUL"),
        /**
         * SUSPEND is oscar keyword
         */
        SUSPEND("SUSPEND"),
        /**
         * SWITCHOVER is oscar keyword
         */
        SWITCHOVER("SWITCHOVER"),
        /**
         * SYNC is oscar keyword
         */
        SYNC("SYNC"),
        /**
         * SYSAUX is oscar keyword
         */
        SYSAUX("SYSAUX"),
        /**
         * SYSID is oscar keyword
         */
        SYSID("SYSID"),
        /**
         * SYSTEM is oscar keyword
         */
        SYSTEM("SYSTEM"),
        /**
         * T is oscar keyword
         */
        T("T"),
        /**
         * TABLESPACE is oscar keyword
         */
        TABLESPACE("TABLESPACE"),
        /**
         * TB is oscar keyword
         */
        TB("TB"),
        /**
         * TEMP is oscar keyword
         */
        TEMP("TEMP"),
        /**
         * TEMPFILE is oscar keyword
         */
        TEMPFILE("TEMPFILE"),
        /**
         * TEMPLATE is oscar keyword
         */
        TEMPLATE("TEMPLATE"),
        /**
         * TEMPORARY is oscar keyword
         */
        TEMPORARY("TEMPORARY"),
        /**
         * TERMINATED is oscar keyword
         */
        TERMINATED("TERMINATED"),
        /**
         * THAN is oscar keyword
         */
        THAN("THAN"),
        /**
         * TIMES is oscar keyword
         */
        TIMES("TIMES"),
        /**
         * TIMEZONE is oscar keyword
         */
        TIMEZONE("TIMEZONE"),
        /**
         * TINYINT is oscar keyword
         */
        TINYINT("TINYINT"),
        /**
         * TOAST is oscar keyword
         */
        TOAST("TOAST"),
        /**
         * TRACE is oscar keyword
         */
        TRACE("TRACE"),
        /**
         * TRACKING is oscar keyword
         */
        TRACKING("TRACKING"),
        /**
         * TRAIL is oscar keyword
         */
        TRAIL("TRAIL"),
        /**
         * TRAILING is oscar keyword
         */
        TRAILING("TRAILING"),
        /**
         * TRANSACTION is oscar keyword
         */
        TRANSACTION("TRANSACTION"),
        /**
         * TRANSACTIONAL is oscar keyword
         */
        TRANSACTIONAL("TRANSACTIONAL"),
        /**
         * TRANSFORMS is oscar keyword
         */
        TRANSFORMS("TRANSFORMS"),
        /**
         * TREAT is oscar keyword
         */
        TREAT("TREAT"),
        /**
         * TRIAL is oscar keyword
         */
        TRIAL("TRIAL"),
        /**
         * TRIGGER is oscar keyword
         */
        TRIGGER("TRIGGER"),
        /**
         * TRIGGERS is oscar keyword
         */
        TRIGGERS("TRIGGERS"),
        /**
         * TRIM is oscar keyword
         */
        TRIM("TRIM"),
        /**
         * TRUE is oscar keyword
         */
        TRUE("TRUE"),
        /**
         * TRUNCATE is oscar keyword
         */
        TRUNCATE("TRUNCATE"),
        /**
         * TRUSTED is oscar keyword
         */
        TRUSTED("TRUSTED"),
        /**
         * TUPLE is oscar keyword
         */
        TUPLE("TUPLE"),
        /**
         * TYPE is oscar keyword
         */
        TYPE("TYPE"),
        /**
         * UNBOUNDED is oscar keyword
         */
        UNBOUNDED("UNBOUNDED"),
        /**
         * UNCOMMITTED is oscar keyword
         */
        UNCOMMITTED("UNCOMMITTED"),
        /**
         * UNDO is oscar keyword
         */
        UNDO("UNDO"),
        /**
         * UNENCRYPTED is oscar keyword
         */
        UNENCRYPTED("UNENCRYPTED"),
        /**
         * UNKNOWN is oscar keyword
         */
        UNKNOWN("UNKNOWN"),
        /**
         * UNLIMITED is oscar keyword
         */
        UNLIMITED("UNLIMITED"),
        /**
         * UNLISTEN is oscar keyword
         */
        UNLISTEN("UNLISTEN"),
        /**
         * UNLOCK is oscar keyword
         */
        UNLOCK("UNLOCK"),
        /**
         * UNMAINTENANCE is oscar keyword
         */
        UNMAINTENANCE("UNMAINTENANCE"),
        /**
         * UNPIVOT is oscar keyword
         */
        UNPIVOT("UNPIVOT"),
        /**
         * UNSIGNED is oscar keyword
         */
        UNSIGNED("UNSIGNED"),
        /**
         * UNTIL is oscar keyword
         */
        UNTIL("UNTIL"),
        /**
         * UNUSABLE is oscar keyword
         */
        UNUSABLE("UNUSABLE"),
        /**
         * UP is oscar keyword
         */
        UP("UP"),
        /**
         * UPDATE is oscar keyword
         */
        UPDATE("UPDATE"),
        /**
         * UPDATELABEL is oscar keyword
         */
        UPDATELABEL("UPDATELABEL"),
        /**
         * UPDATEXML is oscar keyword
         */
        UPDATEXML("UPDATEXML"),
        /**
         * USAGE is oscar keyword
         */
        USAGE("USAGE"),
        /**
         * USE is oscar keyword
         */
        USE("USE"),
        /**
         * USER is oscar keyword
         */
        USER("USER"),
        /**
         * UTF8 is oscar keyword
         */
        UTF8("UTF8"),
        /**
         * UTF8MB4 is oscar keyword
         */
        UTF8MB4("UTF8MB4"),
        /**
         * VACUUM is oscar keyword
         */
        VACUUM("VACUUM"),
        /**
         * VALID is oscar keyword
         */
        VALID("VALID"),
        /**
         * VALIDATE is oscar keyword
         */
        VALIDATE("VALIDATE"),
        /**
         * VALIDATION is oscar keyword
         */
        VALIDATION("VALIDATION"),
        /**
         * VALIDATOR is oscar keyword
         */
        VALIDATOR("VALIDATOR"),
        /**
         * VALUE is oscar keyword
         */
        VALUE("VALUE"),
        /**
         * VALUES is oscar keyword
         */
        VALUES("VALUES"),
        /**
         * VARBINARY is oscar keyword
         */
        VARBINARY("VARBINARY"),
        /**
         * VARBIT is oscar keyword
         */
        VARBIT("VARBIT"),
        /**
         * VARCHAR is oscar keyword
         */
        VARCHAR("VARCHAR"),
        /**
         * VARCHAR2 is oscar keyword
         */
        VARCHAR2("VARCHAR2"),
        /**
         * VARYING is oscar keyword
         */
        VARYING("VARYING"),
        /**
         * VERBOSE is oscar keyword
         */
        VERBOSE("VERBOSE"),
        /**
         * VERSION is oscar keyword
         */
        VERSION("VERSION"),
        /**
         * VERSIONS is oscar keyword
         */
        VERSIONS("VERSIONS"),
        /**
         * VIEW is oscar keyword
         */
        VIEW("VIEW"),
        /**
         * VIRTUAL is oscar keyword
         */
        VIRTUAL("VIRTUAL"),
        /**
         * VISIBLE is oscar keyword
         */
        VISIBLE("VISIBLE"),
        /**
         * VOLATILE is oscar keyword
         */
        VOLATILE("VOLATILE"),
        /**
         * VOTEDISK is oscar keyword
         */
        VOTEDISK("VOTEDISK"),
        /**
         * WAIT is oscar keyword
         */
        WAIT("WAIT"),
        /**
         * WALLET is oscar keyword
         */
        WALLET("WALLET"),
        /**
         * WEIGHT is oscar keyword
         */
        WEIGHT("WEIGHT"),
        /**
         * WHEN is oscar keyword
         */
        WHEN("WHEN"),
        /**
         * WHENEVER is oscar keyword
         */
        WHENEVER("WHENEVER"),
        /**
         * WINDOW is oscar keyword
         */
        WINDOW("WINDOW"),
        /**
         * WORK is oscar keyword
         */
        WORK("WORK"),
        /**
         * XML is oscar keyword
         */
        XML("XML"),
        /**
         * XMLATTRIBUTES is oscar keyword
         */
        XMLATTRIBUTES("XMLATTRIBUTES"),
        /**
         * XMLCONCAT is oscar keyword
         */
        XMLCONCAT("XMLCONCAT"),
        /**
         * XMLELEMENT is oscar keyword
         */
        XMLELEMENT("XMLELEMENT"),
        /**
         * XMLFOREST is oscar keyword
         */
        XMLFOREST("XMLFOREST"),
        /**
         * XMLPARSE is oscar keyword
         */
        XMLPARSE("XMLPARSE"),
        /**
         * XMLPI is oscar keyword
         */
        XMLPI("XMLPI"),
        /**
         * XMLROOT is oscar keyword
         */
        XMLROOT("XMLROOT"),
        /**
         * XMLSERIALIZE is oscar keyword
         */
        XMLSERIALIZE("XMLSERIALIZE"),
        /**
         * XMLTABLE is oscar keyword
         */
        XMLTABLE("XMLTABLE"),
        /**
         * YEAR is oscar keyword
         */
        YEAR("YEAR"),
        /**
         * YES is oscar keyword
         */
        YES("YES"),
        /**
         * ZONE is oscar keyword
         */
        ZONE("ZONE");
        /**
         * The Name.
         */
        public final String name;
        OscarKeyword(String name) {
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
        if (StringUtils.isBlank(columnName)) {
            return false;
        }
        columnName = columnName.trim();
        if (containsEscape(columnName)) {
            return false;
        }
        boolean isKeyWord = checkIfKeyWords(columnName);
        if (isKeyWord) {
            return true;
        }
        // oscar
        // we are recommend table name and column name must uppercase.
        // if exists full uppercase, the table name or column name doesn't bundle escape symbol.
        //create\read    table TABLE "table" "TABLE"
        if (null != tableMeta) {
            ColumnMeta columnMeta = tableMeta.getColumnMeta(columnName);
            if (null != columnMeta) {
                return columnMeta.isCaseSensitive();
            }
        } else if (isUppercase(columnName)) {
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
