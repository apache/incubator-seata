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

package io.seata.rm.datasource.sql.handler;

import java.util.List;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;

import io.seata.sqlparser.druid.mysql.MySQLInsertRecognizer;
import io.seata.sqlparser.druid.mysql.MySQLUpdateRecognizer;
import io.seata.sqlparser.druid.oracle.OracleInsertRecognizer;
import io.seata.sqlparser.druid.oracle.OracleUpdateRecognizer;
import io.seata.sqlparser.druid.postgresql.PostgresqlInsertRecognizer;
import io.seata.sqlparser.druid.postgresql.PostgresqlUpdateRecognizer;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type Escape handler test.
 *
 * @author slievrly
 */
public class EscapeHandlerTest {
    /**
     * Test update columns escape.
     */
    @Test
    public void testUpdateColumnsEscape() {
        //mysql
        String sql1 = "update t set `a` = 1, `b` = 2, `c` = 3";
        List<SQLStatement> astsMysql = SQLUtils.parseStatements(sql1, JdbcConstants.MYSQL);
        MySQLUpdateRecognizer regMysql = new MySQLUpdateRecognizer(sql1, astsMysql.get(0));
        List<String> updateColMysql = regMysql.getUpdateColumnsUnEscape();
        for (String updateColumn : updateColMysql) {
            Assertions.assertFalse(updateColumn.contains("`"));
        }
        updateColMysql = regMysql.getUpdateColumns();
        for (String updateColumn : updateColMysql) {
            Assertions.assertTrue(updateColumn.contains("`"));
        }

        //oracle
        String sql2 = "update t set \"a\" = 1, \"b\" = 2, \"c\" = 3";
        List<SQLStatement> astsOracle = SQLUtils.parseStatements(sql2, JdbcConstants.ORACLE);
        OracleUpdateRecognizer regOracle = new OracleUpdateRecognizer(sql2, astsOracle.get(0));
        List<String> updateColOracle = regOracle.getUpdateColumnsUnEscape();
        for (String updateColumn : updateColOracle) {
            Assertions.assertFalse(updateColumn.contains("\""));
        }
        updateColOracle = regOracle.getUpdateColumns();
        for (String updateColumn : updateColOracle) {
            Assertions.assertTrue(updateColumn.contains("\""));
        }

        //postgresql
        String sql3 = "update t set \"a\" = 1, \"b\" = 2, \"c\" = 3";
        List<SQLStatement> astsPgsql = SQLUtils.parseStatements(sql2, JdbcConstants.POSTGRESQL);
        PostgresqlUpdateRecognizer regPgsql = new PostgresqlUpdateRecognizer(sql3, astsPgsql.get(0));
        List<String> updateColPgsql = regPgsql.getUpdateColumnsUnEscape();
        for (String updateColumn : updateColPgsql) {
            Assertions.assertFalse(updateColumn.contains("\""));
        }
        updateColPgsql = regPgsql.getUpdateColumns();
        for (String updateColumn : updateColPgsql) {
            Assertions.assertTrue(updateColumn.contains("\""));
        }

    }

    /**
     * Test insert columns escape.
     */
    @Test
    public void testInsertColumnsEscape() {
        String sql = "insert into t(`id`, `no`, `name`, `age`) values (1, 'no001', 'aaa', '20')";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        MySQLInsertRecognizer recognizer = new MySQLInsertRecognizer(sql, asts.get(0));
        List<String> insertColumns = recognizer.getInsertColumnsUnEscape();
        for (String insertColumn : insertColumns) {
            Assertions.assertFalse(insertColumn.contains("`"));
        }
        insertColumns = recognizer.getInsertColumns();
        for (String insertColumn : insertColumns) {
            Assertions.assertTrue(insertColumn.contains("`"));
        }

        //oracle
        String sql2 = "insert into t(\"id\", \"no\", \"name\", \"age\") values (1, 'no001', 'aaa', '20')";
        List<SQLStatement> astsOracle = SQLUtils.parseStatements(sql2, JdbcConstants.ORACLE);
        OracleInsertRecognizer regOracle = new OracleInsertRecognizer(sql2, astsOracle.get(0));
        List<String> insertColOracle = regOracle.getInsertColumnsUnEscape();
        for (String insertCol : insertColOracle) {
            Assertions.assertFalse(insertCol.contains("\""));
        }
        insertColOracle = regOracle.getInsertColumns();
        for (String insertCol : insertColOracle) {
            Assertions.assertTrue(insertCol.contains("\""));
        }

        //postgresql
        String sql3 = "insert into t(\"id\", \"no\", \"name\", \"age\") values (1, 'no001', 'aaa', '20')";
        List<SQLStatement> astsPgsql = SQLUtils.parseStatements(sql2, JdbcConstants.POSTGRESQL);
        PostgresqlInsertRecognizer regPgsql = new PostgresqlInsertRecognizer(sql3, astsPgsql.get(0));
        List<String> insertColPgsql = regPgsql.getInsertColumnsUnEscape();
        for (String insertCol : insertColPgsql) {
            Assertions.assertFalse(insertCol.contains("\""));
        }
        insertColPgsql = regPgsql.getInsertColumns();
        for (String insertCol : insertColPgsql) {
            Assertions.assertTrue(insertCol.contains("\""));
        }
    }
}
