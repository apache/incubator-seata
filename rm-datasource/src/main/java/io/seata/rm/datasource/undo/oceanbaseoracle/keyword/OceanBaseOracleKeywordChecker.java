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
import io.seata.rm.datasource.undo.KeywordChecker;
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

    private final Set<String> keywordSet = Arrays.stream(OceanBaseOracleKeywordChecker.ReservedKeyword.values()).
        map(OceanBaseOracleKeywordChecker.ReservedKeyword::name).collect(Collectors.toSet());


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
        // like: "in" Table.in "TABLE".In etc.
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
        ACCESS("ACCESS"),
        ADD("ADD"),
        ALL("ALL"),
        ALTER("ALTER"),
        AND("AND"),
        ANY("ANY"),
        AS("AS"),
        ASC("ASC"),
        AUDIT("AUDIT"),
        BETWEEN("BETWEEN"),
        BY("BY"),
        CHAR("CHAR"),
        CHECK("CHECK"),
        CLUSTER("CLUSTER"),
        COLUMN("COLUMN"),
        COMMENT("COMMENT"),
        COMPRESS("COMPRESS"),
        CONNECT("CONNECT"),
        CREATE("CREATE"),
        CURRENT("CURRENT"),
        CASE("CASE"),
        CONNECT_BY_ROOT("CONNECT_BY_ROOT"),
        DATE("DATE"),
        DECIMAL("DECIMAL"),
        DEFAULT("DEFAULT"),
        DELETE("DELETE"),
        DESC("DESC"),
        DISTINCT("DISTINCT"),
        DROP("DROP"),
        DUAL("DUAL"),
        ELSE("ELSE"),
        EXCLUSIVE("EXCLUSIVE"),
        EXISTS("EXISTS"),
        FILE("FILE"),
        FLOAT("FLOAT"),
        FOR("FOR"),
        FROM("FROM"),
        GRANT("GRANT"),
        GROUP("GROUP"),
        HAVING("HAVING"),
        IDENTIFIED("IDENTIFIED"),
        IMMEDIATE("IMMEDIATE"),
        IN("IN"),
        INCREMENT("INCREMENT"),
        INDEX("INDEX"),
        INITIAL("INITIAL"),
        INSERT("INSERT"),
        INTEGER("INTEGER"),
        INTERSECT("INTERSECT"),
        INTO("INTO"),
        IS("IS"),
        LEVEL("LEVEL"),
        LIKE("LIKE"),
        LOCK("LOCK"),
        LONG("LONG"),
        MAXEXTENTS("MAXEXTENTS"),
        MINUS("MINUS"),
        MODE("MODE"),
        MODIFY("MODIFY"),
        NOAUDIT("NOAUDIT"),
        NOCOMPRESS("NOCOMPRESS"),
        NOT("NOT"),
        NOTFOUND("NOTFOUND"),
        NOWAIT("NOWAIT"),
        NULL("NULL"),
        NUMBER("NUMBER"),
        OF("OF"),
        OFFLINE("OFFLINE"),
        ON("ON"),
        ONLINE("ONLINE"),
        OPTION("OPTION"),
        OR("OR"),
        ORDER("ORDER"),
        PCTFREE("PCTFREE"),
        PRIOR("PRIOR"),
        PRIVILEGES("PRIVILEGES"),
        PUBLIC("PUBLIC"),
        RAW("RAW"),
        RENAME("RENAME"),
        RESOURCE("RESOURCE"),
        REVOKE("REVOKE"),
        ROW("ROW"),
        ROWID("ROWID"),
        ROWLABEL("ROWLABEL"),
        ROWNUM("ROWNUM"),
        ROWS("ROWS"),
        START("START"),
        SELECT("SELECT"),
        SESSION("SESSION"),
        SET("SET"),
        SHARE("SHARE"),
        SIZE("SIZE"),
        SMALLINT("SMALLINT"),
        SUCCESSFUL("SUCCESSFUL"),
        SYNONYM("SYNONYM"),
        SYSDATE("SYSDATE"),
        SQL_CALC_FOUND_ROWS("SQL_CALC_FOUND_ROWS"),
        TABLE("TABLE"),
        THEN("THEN"),
        TO("TO"),
        TRIGGER("TRIGGER"),
        UID("UID"),
        UNION("UNION"),
        UNIQUE("UNIQUE"),
        UPDATE("UPDATE"),
        USER("USER"),
        VALIDATE("VALIDATE"),
        VALUES("VALUES"),
        VARCHAR("VARCHAR"),
        VARCHAR2("VARCHAR2"),
        VIEW("VIEW"),
        WHENEVER("WHENEVER"),
        WHERE("WHERE"),
        WITH("WITH");
        public final String name;

        ReservedKeyword(String name) {
            this.name = name;
        }
    }
}
