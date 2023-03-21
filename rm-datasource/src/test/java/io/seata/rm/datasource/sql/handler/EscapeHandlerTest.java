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

import io.seata.sqlparser.druid.mysql.MySQLUpdateRecognizer;
import io.seata.sqlparser.druid.oracle.OracleUpdateRecognizer;
import io.seata.sqlparser.druid.postgresql.PostgresqlUpdateRecognizer;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author slievrly
 */
public class EscapeHandlerTest {
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
        //oracle
        String sql2 = "update t set \"a\" = 1, \"b\" = 2, \"c\" = 3";
        List<SQLStatement> astsOracle = SQLUtils.parseStatements(sql2, JdbcConstants.ORACLE);
        OracleUpdateRecognizer regOracle = new OracleUpdateRecognizer(sql2, astsOracle.get(0));
        List<String> updateColOracle = regOracle.getUpdateColumnsUnEscape();
        for (String updateColumn : updateColOracle) {
            Assertions.assertFalse(updateColumn.contains("`"));
        }

        //postgresql
        String sql3 = "update t set \"a\" = 1, \"b\" = 2, \"c\" = 3";
        List<SQLStatement> astsPgsql = SQLUtils.parseStatements(sql2, JdbcConstants.POSTGRESQL);
        PostgresqlUpdateRecognizer regPgsql = new PostgresqlUpdateRecognizer(sql3, astsPgsql.get(0));
        List<String> updateColPgsql = regPgsql.getUpdateColumnsUnEscape();
        for (String updateColumn : updateColPgsql) {
            Assertions.assertFalse(updateColumn.contains("`"));
        }

    }
}
