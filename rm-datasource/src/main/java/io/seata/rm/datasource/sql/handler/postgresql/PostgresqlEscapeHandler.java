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
package io.seata.rm.datasource.sql.handler.postgresql;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import io.seata.common.loader.LoadLevel;
import io.seata.common.util.StringUtils;
import io.seata.sqlparser.EscapeHandler;
import io.seata.sqlparser.struct.ColumnMeta;
import io.seata.sqlparser.struct.TableMeta;
import io.seata.sqlparser.util.JdbcConstants;

/**
 * The type postgresql keyword checker.
 *
 * @author japsercloud
 */
@LoadLevel(name = JdbcConstants.POSTGRESQL)
public class PostgresqlEscapeHandler implements EscapeHandler {

    private Set<String> keywordSet = Arrays.stream(PostgresqlEscapeHandler.PostgresqlKeyword.values())
            .map(PostgresqlEscapeHandler.PostgresqlKeyword::name).collect(Collectors.toSet());

    /**
     * postgresql keyword
     */
    private enum PostgresqlKeyword {
        /**
         * ALL is postgresql keyword
         */
        ALL("ALL"),
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
         * ARRAY is postgresql keyword
         */
        ARRAY("ARRAY"),
        /**
         * AS is postgresql keyword
         */
        AS("AS"),
        /**
         * ASC is postgresql keyword
         */
        ASC("ASC"),
        /**
         * ASYMMETRIC is postgresql keyword
         */
        ASYMMETRIC("ASYMMETRIC"),
        /**
         * BOTH is postgresql keyword
         */
        BOTH("BOTH"),
        /**
         * CASE is postgresql keyword
         */
        CASE("CASE"),
        /**
         * CAST is postgresql keyword
         */
        CAST("CAST"),
        /**
         * CHECK is postgresql keyword
         */
        CHECK("CHECK"),
        /**
         * COLLATE is postgresql keyword
         */
        COLLATE("COLLATE"),
        /**
         * COLUMN is postgresql keyword
         */
        COLUMN("COLUMN"),
        /**
         * CONSTRAINT is postgresql keyword
         */
        CONSTRAINT("CONSTRAINT"),
        /**
         * CREATE is postgresql keyword
         */
        CREATE("CREATE"),
        /**
         * CURRENT_CATALOG is postgresql keyword
         */
        CURRENT_CATALOG("CURRENT_CATALOG"),
        /**
         * CURRENT_DATE is postgresql keyword
         */
        CURRENT_DATE("CURRENT_DATE"),
        /**
         * CURRENT_ROLE is postgresql keyword
         */
        CURRENT_ROLE("CURRENT_ROLE"),
        /**
         * CURRENT_TIME is postgresql keyword
         */
        CURRENT_TIME("CURRENT_TIME"),
        /**
         * CURRENT_TIMESTAMP is postgresql keyword
         */
        CURRENT_TIMESTAMP("CURRENT_TIMESTAMP"),
        /**
         * CURRENT_USER is postgresql keyword
         */
        CURRENT_USER("CURRENT_USER"),
        /**
         * DEFAULT is postgresql keyword
         */
        DEFAULT("DEFAULT"),
        /**
         * DEFERRABLE is postgresql keyword
         */
        DEFERRABLE("DEFERRABLE"),
        /**
         * DESC is postgresql keyword
         */
        DESC("DESC"),
        /**
         * DISTINCT is postgresql keyword
         */
        DISTINCT("DISTINCT"),
        /**
         * DO is postgresql keyword
         */
        DO("DO"),
        /**
         * ELSE is postgresql keyword
         */
        ELSE("ELSE"),
        /**
         * END is postgresql keyword
         */
        END("END"),
        /**
         * EXCEPT is postgresql keyword
         */
        EXCEPT("EXCEPT"),
        /**
         * FALSE is postgresql keyword
         */
        FALSE("FALSE"),
        /**
         * FETCH is postgresql keyword
         */
        FETCH("FETCH"),
        /**
         * FOR is postgresql keyword
         */
        FOR("FOR"),
        /**
         * FOREIGN is postgresql keyword
         */
        FOREIGN("FOREIGN"),
        /**
         * FROM is postgresql keyword
         */
        FROM("FROM"),
        /**
         * GRANT is postgresql keyword
         */
        GRANT("GRANT"),
        /**
         * GROUP is postgresql keyword
         */
        GROUP("GROUP"),
        /**
         * HAVING is postgresql keyword
         */
        HAVING("HAVING"),
        /**
         * IN is postgresql keyword
         */
        IN("IN"),
        /**
         * INITIALLY is postgresql keyword
         */
        INITIALLY("INITIALLY"),
        /**
         * INTERSECT is postgresql keyword
         */
        INTERSECT("INTERSECT"),
        /**
         * INTO is postgresql keyword
         */
        INTO("INTO"),
        /**
         * LATERAL is postgresql keyword
         */
        LATERAL("LATERAL"),
        /**
         * LEADING is postgresql keyword
         */
        LEADING("LEADING"),
        /**
         * LIMIT is postgresql keyword
         */
        LIMIT("LIMIT"),
        /**
         * LOCALTIME is postgresql keyword
         */
        LOCALTIME("LOCALTIME"),
        /**
         * LOCALTIMESTAMP is postgresql keyword
         */
        LOCALTIMESTAMP("LOCALTIMESTAMP"),
        /**
         * NOT is postgresql keyword
         */
        NOT("NOT"),
        /**
         * NULL is postgresql keyword
         */
        NULL("NULL"),
        /**
         * OFFSET is postgresql keyword
         */
        OFFSET("OFFSET"),
        /**
         * ON is postgresql keyword
         */
        ON("ON"),
        /**
         * ONLY is postgresql keyword
         */
        ONLY("ONLY"),
        /**
         * OR is postgresql keyword
         */
        OR("OR"),
        /**
         * ORDER is postgresql keyword
         */
        ORDER("ORDER"),
        /**
         * PLACING is postgresql keyword
         */
        PLACING("PLACING"),
        /**
         * PRIMARY is postgresql keyword
         */
        PRIMARY("PRIMARY"),
        /**
         * REFERENCES is postgresql keyword
         */
        REFERENCES("REFERENCES"),
        /**
         * RETURNING is postgresql keyword
         */
        RETURNING("RETURNING"),
        /**
         * SELECT is postgresql keyword
         */
        SELECT("SELECT"),
        /**
         * SESSION_USER is postgresql keyword
         */
        SESSION_USER("SESSION_USER"),
        /**
         * SOME is postgresql keyword
         */
        SOME("SOME"),
        /**
         * SYMMETRIC is postgresql keyword
         */
        SYMMETRIC("SYMMETRIC"),
        /**
         * TABLE is postgresql keyword
         */
        TABLE("TABLE"),
        /**
         * THEN is postgresql keyword
         */
        THEN("THEN"),
        /**
         * TO is postgresql keyword
         */
        TO("TO"),
        /**
         * TRAILING is postgresql keyword
         */
        TRAILING("TRAILING"),
        /**
         * TRUE is postgresql keyword
         */
        TRUE("TRUE"),
        /**
         * UNION is postgresql keyword
         */
        UNION("UNION"),
        /**
         * UNIQUE is postgresql keyword
         */
        UNIQUE("UNIQUE"),
        /**
         * USER is postgresql keyword
         */
        USER("USER"),
        /**
         * USING is postgresql keyword
         */
        USING("USING"),
        /**
         * VARIADIC is postgresql keyword
         */
        VARIADIC("VARIADIC"),
        /**
         * WHEN is postgresql keyword
         */
        WHEN("WHEN"),
        /**
         * WHERE is postgresql keyword
         */
        WHERE("WHERE"),
        /**
         * WINDOW is postgresql keyword
         */
        WINDOW("WINDOW"),
        /**
         * WITH is postgresql keyword
         */
        WITH("WITH");
        /**
         * The Name.
         */
        public final String name;

        PostgresqlKeyword(String name) {
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
        if (null != tableMeta) {
            ColumnMeta columnMeta = tableMeta.getColumnMeta(columnName);
            if (null != columnMeta) {
                return columnMeta.isCaseSensitive();
            }
        } else if (!containsUppercase(columnName)) {
            return false;
        }
        return true;
    }

    private static boolean containsUppercase(String colName) {
        if (colName == null) {
            return false;
        }
        char[] chars = colName.toCharArray();
        for (char ch : chars) {
            if (ch >= 'A' && ch <= 'Z') {
                return true;
            }
        }
        return false;
    }
}
