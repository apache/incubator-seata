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
package io.seata.rm.datasource.undo.oceanbaseoracle.keyword;

import io.seata.common.loader.LoadLevel;
import io.seata.sqlparser.KeywordChecker;
import io.seata.sqlparser.util.JdbcConstants;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reserved keywords checker of OceanBaseOracle
 *
 * @author hsien999
 */
@LoadLevel(name = JdbcConstants.OCEANBASE_ORACLE)
public class OceanBaseOracleKeywordChecker implements KeywordChecker {

    private final Set<String> keywordSet = Arrays.stream(ReservedKeyword.values()).
        map(ReservedKeyword::name).collect(Collectors.toSet());


    /**
     * Check whether given field name and table name uses a keyword.
     *
     * @param fieldOrTableName the field or table name
     * @return whether the name uses a keyword
     */
    @Override
    public boolean check(String fieldOrTableName) {
        return keywordSet.contains(fieldOrTableName.toUpperCase());
    }

    /**
     * Check whether given field or table name uses a keyword or needs escapes to delimited.
     * in oracle mode, case is sensitive only when the escape is included
     * it's recommended to use full-uppercase names
     *
     * @param fieldOrTableName the field or table name
     * @return whether the name uses a keyword or needs escapes to delimited
     */
    @Override
    public boolean checkEscape(String fieldOrTableName) {
        // e.g. "in" Table.in "TABLE".In etc.
        return check(fieldOrTableName) || !isAllUpperCase(fieldOrTableName);
    }

    private boolean isAllUpperCase(String fieldOrTableName) {
        if (StringUtils.isEmpty(fieldOrTableName)) {
            return false;
        }
        for (char ch : fieldOrTableName.toCharArray()) {
            if (ch >= 'a' && ch <= 'z') {
                return false;
            }
        }
        return true;
    }

    /**
     * Reserved words in OceanBase(Oracle mode)
     * something different from oracle: COLUMN_VALUE CASE CONNECT_BY_ROOT DUAL MLSLABEL NESTED_TABLE_ID
     * NESTED_TABLE_ID NOTFOUND PRIVILEGES SQL_CALC_FOUND_ROWS
     */
    private enum ReservedKeyword {
        /**
         * ACCESS is the keyword of OceanBase in Oracle mode
         */
        ACCESS("ACCESS"),
        /**
         * ADD is the keyword of OceanBase in Oracle mode
         */
        ADD("ADD"),
        /**
         * ALL is the keyword of OceanBase in Oracle mode
         */
        ALL("ALL"),
        /**
         * ALTER is the keyword of OceanBase in Oracle mode
         */
        ALTER("ALTER"),
        /**
         * AND is the keyword of OceanBase in Oracle mode
         */
        AND("AND"),
        /**
         * ANY is the keyword of OceanBase in Oracle mode
         */
        ANY("ANY"),
        /**
         * AS is the keyword of OceanBase in Oracle mode
         */
        AS("AS"),
        /**
         * ASC is the keyword of OceanBase in Oracle mode
         */
        ASC("ASC"),
        /**
         * AUDIT is the keyword of OceanBase in Oracle mode
         */
        AUDIT("AUDIT"),
        /**
         * BETWEEN is the keyword of OceanBase in Oracle mode
         */
        BETWEEN("BETWEEN"),
        /**
         * BY is the keyword of OceanBase in Oracle mode
         */
        BY("BY"),
        /**
         * CHAR is the keyword of OceanBase in Oracle mode
         */
        CHAR("CHAR"),
        /**
         * CHECK is the keyword of OceanBase in Oracle mode
         */
        CHECK("CHECK"),
        /**
         * CLUSTER is the keyword of OceanBase in Oracle mode
         */
        CLUSTER("CLUSTER"),
        /**
         * COLUMN is the keyword of OceanBase in Oracle mode
         */
        COLUMN("COLUMN"),
        /**
         * COMMENT is the keyword of OceanBase in Oracle mode
         */
        COMMENT("COMMENT"),
        /**
         * COMPRESS is the keyword of OceanBase in Oracle mode
         */
        COMPRESS("COMPRESS"),
        /**
         * CONNECT is the keyword of OceanBase in Oracle mode
         */
        CONNECT("CONNECT"),
        /**
         * CREATE is the keyword of OceanBase in Oracle mode
         */
        CREATE("CREATE"),
        /**
         * CURRENT is the keyword of OceanBase in Oracle mode
         */
        CURRENT("CURRENT"),
        /**
         * CASE is the keyword of OceanBase in Oracle mode
         */
        CASE("CASE"),
        /**
         * CONNECT_BY_ROOT is the keyword of OceanBase in Oracle mode
         */
        CONNECT_BY_ROOT("CONNECT_BY_ROOT"),
        /**
         * DATE is the keyword of OceanBase in Oracle mode
         */
        DATE("DATE"),
        /**
         * DECIMAL is the keyword of OceanBase in Oracle mode
         */
        DECIMAL("DECIMAL"),
        /**
         * DEFAULT is the keyword of OceanBase in Oracle mode
         */
        DEFAULT("DEFAULT"),
        /**
         * DELETE is the keyword of OceanBase in Oracle mode
         */
        DELETE("DELETE"),
        /**
         * DESC is the keyword of OceanBase in Oracle mode
         */
        DESC("DESC"),
        /**
         * DISTINCT is the keyword of OceanBase in Oracle mode
         */
        DISTINCT("DISTINCT"),
        /**
         * DROP is the keyword of OceanBase in Oracle mode
         */
        DROP("DROP"),
        /**
         * DUAL is the keyword of OceanBase in Oracle mode
         */
        DUAL("DUAL"),
        /**
         * ELSE is the keyword of OceanBase in Oracle mode
         */
        ELSE("ELSE"),
        /**
         * EXCLUSIVE is the keyword of OceanBase in Oracle mode
         */
        EXCLUSIVE("EXCLUSIVE"),
        /**
         * EXISTS is the keyword of OceanBase in Oracle mode
         */
        EXISTS("EXISTS"),
        /**
         * FILE is the keyword of OceanBase in Oracle mode
         */
        FILE("FILE"),
        /**
         * FLOAT is the keyword of OceanBase in Oracle mode
         */
        FLOAT("FLOAT"),
        /**
         * FOR is the keyword of OceanBase in Oracle mode
         */
        FOR("FOR"),
        /**
         * FROM is the keyword of OceanBase in Oracle mode
         */
        FROM("FROM"),
        /**
         * GRANT is the keyword of OceanBase in Oracle mode
         */
        GRANT("GRANT"),
        /**
         * GROUP is the keyword of OceanBase in Oracle mode
         */
        GROUP("GROUP"),
        /**
         * HAVING is the keyword of OceanBase in Oracle mode
         */
        HAVING("HAVING"),
        /**
         * IDENTIFIED is the keyword of OceanBase in Oracle mode
         */
        IDENTIFIED("IDENTIFIED"),
        /**
         * IMMEDIATE is the keyword of OceanBase in Oracle mode
         */
        IMMEDIATE("IMMEDIATE"),
        /**
         * IN is the keyword of OceanBase in Oracle mode
         */
        IN("IN"),
        /**
         * INCREMENT is the keyword of OceanBase in Oracle mode
         */
        INCREMENT("INCREMENT"),
        /**
         * INDEX is the keyword of OceanBase in Oracle mode
         */
        INDEX("INDEX"),
        /**
         * INITIAL is the keyword of OceanBase in Oracle mode
         */
        INITIAL("INITIAL"),
        /**
         * INSERT is the keyword of OceanBase in Oracle mode
         */
        INSERT("INSERT"),
        /**
         * INTEGER is the keyword of OceanBase in Oracle mode
         */
        INTEGER("INTEGER"),
        /**
         * INTERSECT is the keyword of OceanBase in Oracle mode
         */
        INTERSECT("INTERSECT"),
        /**
         * INTO is the keyword of OceanBase in Oracle mode
         */
        INTO("INTO"),
        /**
         * IS is the keyword of OceanBase in Oracle mode
         */
        IS("IS"),
        /**
         * LEVEL is the keyword of OceanBase in Oracle mode
         */
        LEVEL("LEVEL"),
        /**
         * LIKE is the keyword of OceanBase in Oracle mode
         */
        LIKE("LIKE"),
        /**
         * LOCK is the keyword of OceanBase in Oracle mode
         */
        LOCK("LOCK"),
        /**
         * LONG is the keyword of OceanBase in Oracle mode
         */
        LONG("LONG"),
        /**
         * MAXEXTENTS is the keyword of OceanBase in Oracle mode
         */
        MAXEXTENTS("MAXEXTENTS"),
        /**
         * MINUS is the keyword of OceanBase in Oracle mode
         */
        MINUS("MINUS"),
        /**
         * MODE is the keyword of OceanBase in Oracle mode
         */
        MODE("MODE"),
        /**
         * MODIFY is the keyword of OceanBase in Oracle mode
         */
        MODIFY("MODIFY"),
        /**
         * NOAUDIT is the keyword of OceanBase in Oracle mode
         */
        NOAUDIT("NOAUDIT"),
        /**
         * NOCOMPRESS is the keyword of OceanBase in Oracle mode
         */
        NOCOMPRESS("NOCOMPRESS"),
        /**
         * NOT is the keyword of OceanBase in Oracle mode
         */
        NOT("NOT"),
        /**
         * NOTFOUND is the keyword of OceanBase in Oracle mode
         */
        NOTFOUND("NOTFOUND"),
        /**
         * NOWAIT is the keyword of OceanBase in Oracle mode
         */
        NOWAIT("NOWAIT"),
        /**
         * NULL is the keyword of OceanBase in Oracle mode
         */
        NULL("NULL"),
        /**
         * NUMBER is the keyword of OceanBase in Oracle mode
         */
        NUMBER("NUMBER"),
        /**
         * OF is the keyword of OceanBase in Oracle mode
         */
        OF("OF"),
        /**
         * OFFLINE is the keyword of OceanBase in Oracle mode
         */
        OFFLINE("OFFLINE"),
        /**
         * ON is the keyword of OceanBase in Oracle mode
         */
        ON("ON"),
        /**
         * ONLINE is the keyword of OceanBase in Oracle mode
         */
        ONLINE("ONLINE"),
        /**
         * OPTION is the keyword of OceanBase in Oracle mode
         */
        OPTION("OPTION"),
        /**
         * OR is the keyword of OceanBase in Oracle mode
         */
        OR("OR"),
        /**
         * ORDER is the keyword of OceanBase in Oracle mode
         */
        ORDER("ORDER"),
        /**
         * PCTFREE is the keyword of OceanBase in Oracle mode
         */
        PCTFREE("PCTFREE"),
        /**
         * PRIOR is the keyword of OceanBase in Oracle mode
         */
        PRIOR("PRIOR"),
        /**
         * PRIVILEGES is the keyword of OceanBase in Oracle mode
         */
        PRIVILEGES("PRIVILEGES"),
        /**
         * PUBLIC is the keyword of OceanBase in Oracle mode
         */
        PUBLIC("PUBLIC"),
        /**
         * RAW is the keyword of OceanBase in Oracle mode
         */
        RAW("RAW"),
        /**
         * RENAME is the keyword of OceanBase in Oracle mode
         */
        RENAME("RENAME"),
        /**
         * RESOURCE is the keyword of OceanBase in Oracle mode
         */
        RESOURCE("RESOURCE"),
        /**
         * REVOKE is the keyword of OceanBase in Oracle mode
         */
        REVOKE("REVOKE"),
        /**
         * ROW is the keyword of OceanBase in Oracle mode
         */
        ROW("ROW"),
        /**
         * ROWID is the keyword of OceanBase in Oracle mode
         */
        ROWID("ROWID"),
        /**
         * ROWLABEL is the keyword of OceanBase in Oracle mode
         */
        ROWLABEL("ROWLABEL"),
        /**
         * ROWNUM is the keyword of OceanBase in Oracle mode
         */
        ROWNUM("ROWNUM"),
        /**
         * ROWS is the keyword of OceanBase in Oracle mode
         */
        ROWS("ROWS"),
        /**
         * START is the keyword of OceanBase in Oracle mode
         */
        START("START"),
        /**
         * SELECT is the keyword of OceanBase in Oracle mode
         */
        SELECT("SELECT"),
        /**
         * SESSION is the keyword of OceanBase in Oracle mode
         */
        SESSION("SESSION"),
        /**
         * SET is the keyword of OceanBase in Oracle mode
         */
        SET("SET"),
        /**
         * SHARE is the keyword of OceanBase in Oracle mode
         */
        SHARE("SHARE"),
        /**
         * SIZE is the keyword of OceanBase in Oracle mode
         */
        SIZE("SIZE"),
        /**
         * SMALLINT is the keyword of OceanBase in Oracle mode
         */
        SMALLINT("SMALLINT"),
        /**
         * SUCCESSFUL is the keyword of OceanBase in Oracle mode
         */
        SUCCESSFUL("SUCCESSFUL"),
        /**
         * SYNONYM is the keyword of OceanBase in Oracle mode
         */
        SYNONYM("SYNONYM"),
        /**
         * SYSDATE is the keyword of OceanBase in Oracle mode
         */
        SYSDATE("SYSDATE"),
        /**
         * SQL_CALC_FOUND_ROWS is the keyword of OceanBase in Oracle mode
         */
        SQL_CALC_FOUND_ROWS("SQL_CALC_FOUND_ROWS"),
        /**
         * TABLE is the keyword of OceanBase in Oracle mode
         */
        TABLE("TABLE"),
        /**
         * THEN is the keyword of OceanBase in Oracle mode
         */
        THEN("THEN"),
        /**
         * TO is the keyword of OceanBase in Oracle mode
         */
        TO("TO"),
        /**
         * TRIGGER is the keyword of OceanBase in Oracle mode
         */
        TRIGGER("TRIGGER"),
        /**
         * UID is the keyword of OceanBase in Oracle mode
         */
        UID("UID"),
        /**
         * UNION is the keyword of OceanBase in Oracle mode
         */
        UNION("UNION"),
        /**
         * UNIQUE is the keyword of OceanBase in Oracle mode
         */
        UNIQUE("UNIQUE"),
        /**
         * UPDATE is the keyword of OceanBase in Oracle mode
         */
        UPDATE("UPDATE"),
        /**
         * USER is the keyword of OceanBase in Oracle mode
         */
        USER("USER"),
        /**
         * VALIDATE is the keyword of OceanBase in Oracle mode
         */
        VALIDATE("VALIDATE"),
        /**
         * VALUES is the keyword of OceanBase in Oracle mode
         */
        VALUES("VALUES"),
        /**
         * VARCHAR is the keyword of OceanBase in Oracle mode
         */
        VARCHAR("VARCHAR"),
        /**
         * VARCHAR2 is the keyword of OceanBase in Oracle mode
         */
        VARCHAR2("VARCHAR2"),
        /**
         * VIEW is the keyword of OceanBase in Oracle mode
         */
        VIEW("VIEW"),
        /**
         * WHENEVER is the keyword of OceanBase in Oracle mode
         */
        WHENEVER("WHENEVER"),
        /**
         * WHERE is the keyword of OceanBase in Oracle mode
         */
        WHERE("WHERE"),
        /**
         * WITH is the keyword of OceanBase in Oracle mode
         */
        WITH("WITH");
        public final String name;

        ReservedKeyword(String name) {
            this.name = name;
        }
    }
}
