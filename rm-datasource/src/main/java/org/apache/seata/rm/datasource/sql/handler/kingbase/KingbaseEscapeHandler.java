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
package org.apache.seata.rm.datasource.sql.handler.kingbase;

import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.sqlparser.EscapeHandler;
import org.apache.seata.sqlparser.struct.ColumnMeta;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.apache.seata.sqlparser.util.JdbcConstants;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The type kingbase keyword checker.
 */
@LoadLevel(name = JdbcConstants.KINGBASE)
public class KingbaseEscapeHandler implements EscapeHandler {

    private Set<String> keywordSet = Arrays.stream(KingbaseEscapeHandler.KingbaseKeyword.values()).map(KingbaseEscapeHandler.KingbaseKeyword::name).collect(Collectors.toSet());

    /**
     * kingbase keyword
     */
    private enum KingbaseKeyword {
        /**
         * ACCESS is kingbase keyword
         */
        ACCESS("ACCESS"),
        /**
         * ADD is kingbase keyword
         */
        ADD("ADD"),
        /**
         * ALL is kingbase keyword
         */
        ALL("ALL"),
        /**
         * ALTER is kingbase keyword
         */
        ALTER("ALTER"),
        /**
         * AND is kingbase keyword
         */
        AND("AND"),
        /**
         * ANY is kingbase keyword
         */
        ANY("ANY"),
        /**
         * AS is kingbase keyword
         */
        AS("AS"),
        /**
         * ASC is kingbase keyword
         */
        ASC("ASC"),
        /**
         * AUDIT is kingbase keyword
         */
        AUDIT("AUDIT"),
        /**
         * BETWEEN is kingbase keyword
         */
        BETWEEN("BETWEEN"),
        /**
         * BY is kingbase keyword
         */
        BY("BY"),
        /**
         * CHAR is kingbase keyword
         */
        CHAR("CHAR"),
        /**
         * CHECK is kingbase keyword
         */
        CHECK("CHECK"),
        /**
         * CLUSTER is kingbase keyword
         */
        CLUSTER("CLUSTER"),
        /**
         * COLUMN is kingbase keyword
         */
        COLUMN("COLUMN"),
        /**
         * COLUMN_VALUE is kingbase keyword
         */
        COLUMN_VALUE("COLUMN_VALUE"),
        /**
         * COMMENT is kingbase keyword
         */
        COMMENT("COMMENT"),
        /**
         * COMPRESS is kingbase keyword
         */
        COMPRESS("COMPRESS"),
        /**
         * CONNECT is kingbase keyword
         */
        CONNECT("CONNECT"),
        /**
         * CREATE is kingbase keyword
         */
        CREATE("CREATE"),
        /**
         * CURRENT is kingbase keyword
         */
        CURRENT("CURRENT"),
        /**
         * DATE is kingbase keyword
         */
        DATE("DATE"),
        /**
         * DECIMAL is kingbase keyword
         */
        DECIMAL("DECIMAL"),
        /**
         * DEFAULT is kingbase keyword
         */
        DEFAULT("DEFAULT"),
        /**
         * DELETE is kingbase keyword
         */
        DELETE("DELETE"),
        /**
         * DESC is kingbase keyword
         */
        DESC("DESC"),
        /**
         * DISTINCT is kingbase keyword
         */
        DISTINCT("DISTINCT"),
        /**
         * DROP is kingbase keyword
         */
        DROP("DROP"),
        /**
         * ELSE is kingbase keyword
         */
        ELSE("ELSE"),
        /**
         * EXCLUSIVE is kingbase keyword
         */
        EXCLUSIVE("EXCLUSIVE"),
        /**
         * EXISTS is kingbase keyword
         */
        EXISTS("EXISTS"),
        /**
         * FILE is kingbase keyword
         */
        FILE("FILE"),
        /**
         * FLOAT is kingbase keyword
         */
        FLOAT("FLOAT"),
        /**
         * FOR is kingbase keyword
         */
        FOR("FOR"),
        /**
         * FROM is kingbase keyword
         */
        FROM("FROM"),
        /**
         * GRANT is kingbase keyword
         */
        GRANT("GRANT"),
        /**
         * GROUP is kingbase keyword
         */
        GROUP("GROUP"),
        /**
         * HAVING is kingbase keyword
         */
        HAVING("HAVING"),
        /**
         * IDENTIFIED is kingbase keyword
         */
        IDENTIFIED("IDENTIFIED"),
        /**
         * IMMEDIATE is kingbase keyword
         */
        IMMEDIATE("IMMEDIATE"),
        /**
         * IN is kingbase keyword
         */
        IN("IN"),
        /**
         * INCREMENT is kingbase keyword
         */
        INCREMENT("INCREMENT"),
        /**
         * INDEX is kingbase keyword
         */
        INDEX("INDEX"),
        /**
         * INITIAL is kingbase keyword
         */
        INITIAL("INITIAL"),
        /**
         * INSERT is kingbase keyword
         */
        INSERT("INSERT"),
        /**
         * INTEGER is kingbase keyword
         */
        INTEGER("INTEGER"),
        /**
         * INTERSECT is kingbase keyword
         */
        INTERSECT("INTERSECT"),
        /**
         * INTO is kingbase keyword
         */
        INTO("INTO"),
        /**
         * IS is kingbase keyword
         */
        IS("IS"),
        /**
         * LEVEL is kingbase keyword
         */
        LEVEL("LEVEL"),
        /**
         * LIKE is kingbase keyword
         */
        LIKE("LIKE"),
        /**
         * LOCK is kingbase keyword
         */
        LOCK("LOCK"),
        /**
         * LONG is kingbase keyword
         */
        LONG("LONG"),
        /**
         * MAXEXTENTS is kingbase keyword
         */
        MAXEXTENTS("MAXEXTENTS"),
        /**
         * MINUS is kingbase keyword
         */
        MINUS("MINUS"),
        /**
         * MLSLABEL is kingbase keyword
         */
        MLSLABEL("MLSLABEL"),
        /**
         * MODE is kingbase keyword
         */
        MODE("MODE"),
        /**
         * MODIFY is kingbase keyword
         */
        MODIFY("MODIFY"),
        /**
         * NESTED_TABLE_ID is kingbase keyword
         */
        NESTED_TABLE_ID("NESTED_TABLE_ID"),
        /**
         * NOAUDIT is kingbase keyword
         */
        NOAUDIT("NOAUDIT"),
        /**
         * NOCOMPRESS is kingbase keyword
         */
        NOCOMPRESS("NOCOMPRESS"),
        /**
         * NOT is kingbase keyword
         */
        NOT("NOT"),
        /**
         * NOWAIT is kingbase keyword
         */
        NOWAIT("NOWAIT"),
        /**
         * NULL is kingbase keyword
         */
        NULL("NULL"),
        /**
         * NUMBER is kingbase keyword
         */
        NUMBER("NUMBER"),
        /**
         * OF is kingbase keyword
         */
        OF("OF"),
        /**
         * OFFLINE is kingbase keyword
         */
        OFFLINE("OFFLINE"),
        /**
         * ON is kingbase keyword
         */
        ON("ON"),
        /**
         * ONLINE is kingbase keyword
         */
        ONLINE("ONLINE"),
        /**
         * OPTION is kingbase keyword
         */
        OPTION("OPTION"),
        /**
         * OR is kingbase keyword
         */
        OR("OR"),
        /**
         * ORDER is kingbase keyword
         */
        ORDER("ORDER"),
        /**
         * PCTFREE is kingbase keyword
         */
        PCTFREE("PCTFREE"),
        /**
         * PRIOR is kingbase keyword
         */
        PRIOR("PRIOR"),
        /**
         * PUBLIC is kingbase keyword
         */
        PUBLIC("PUBLIC"),
        /**
         * RAW is kingbase keyword
         */
        RAW("RAW"),
        /**
         * RENAME is kingbase keyword
         */
        RENAME("RENAME"),
        /**
         * RESOURCE is kingbase keyword
         */
        RESOURCE("RESOURCE"),
        /**
         * REVOKE is kingbase keyword
         */
        REVOKE("REVOKE"),
        /**
         * ROW is kingbase keyword
         */
        ROW("ROW"),
        /**
         * ROWID is kingbase keyword
         */
        ROWID("ROWID"),
        /**
         * ROWNUM is kingbase keyword
         */
        ROWNUM("ROWNUM"),
        /**
         * ROWS is kingbase keyword
         */
        ROWS("ROWS"),
        /**
         * SELECT is kingbase keyword
         */
        SELECT("SELECT"),
        /**
         * SESSION is kingbase keyword
         */
        SESSION("SESSION"),
        /**
         * SET is kingbase keyword
         */
        SET("SET"),
        /**
         * SHARE is kingbase keyword
         */
        SHARE("SHARE"),
        /**
         * SIZE is kingbase keyword
         */
        SIZE("SIZE"),
        /**
         * SMALLINT is kingbase keyword
         */
        SMALLINT("SMALLINT"),
        /**
         * START is kingbase keyword
         */
        START("START"),
        /**
         * SUCCESSFUL is kingbase keyword
         */
        SUCCESSFUL("SUCCESSFUL"),
        /**
         * SYNONYM is kingbase keyword
         */
        SYNONYM("SYNONYM"),
        /**
         * SYSDATE is kingbase keyword
         */
        SYSDATE("SYSDATE"),
        /**
         * TABLE is kingbase keyword
         */
        TABLE("TABLE"),
        /**
         * THEN is kingbase keyword
         */
        THEN("THEN"),
        /**
         * TO is kingbase keyword
         */
        TO("TO"),
        /**
         * TRIGGER is kingbase keyword
         */
        TRIGGER("TRIGGER"),
        /**
         * UID is kingbase keyword
         */
        UID("UID"),
        /**
         * UNION is kingbase keyword
         */
        UNION("UNION"),
        /**
         * UNIQUE is kingbase keyword
         */
        UNIQUE("UNIQUE"),
        /**
         * UPDATE is kingbase keyword
         */
        UPDATE("UPDATE"),
        /**
         * USER is kingbase keyword
         */
        USER("USER"),
        /**
         * VALIDATE is kingbase keyword
         */
        VALIDATE("VALIDATE"),
        /**
         * VALUES is kingbase keyword
         */
        VALUES("VALUES"),
        /**
         * VARCHAR is kingbase keyword
         */
        VARCHAR("VARCHAR"),
        /**
         * VARCHAR2 is kingbase keyword
         */
        VARCHAR2("VARCHAR2"),
        /**
         * VIEW is kingbase keyword
         */
        VIEW("VIEW"),
        /**
         * WHENEVER is kingbase keyword
         */
        WHENEVER("WHENEVER"),
        /**
         * WHERE is kingbase keyword
         */
        WHERE("WHERE"),
        /**
         * WITH is kingbase keyword
         */
        WITH("WITH");
        /**
         * The Name.
         */
        public final String name;

        KingbaseKeyword(String name) {
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
        // kingbase
        // we are recommend table name and column name must uppercase.
        // if exists full uppercase, the table name or column name doesn't bundle escape symbol.
        //create\read    table TABLE "table" "TABLE"
        //
        //table        √     √       ×       √
        //
        //TABLE        √     √       ×       √
        //
        //"table"      ×     ×       √       ×
        //
        //"TABLE"      √     √       ×       √
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
