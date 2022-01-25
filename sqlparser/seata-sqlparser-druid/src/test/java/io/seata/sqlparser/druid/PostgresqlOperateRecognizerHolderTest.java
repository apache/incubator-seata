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
package io.seata.sqlparser.druid;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import io.seata.common.exception.NotSupportYetException;
import io.seata.sqlparser.druid.postgresql.PostgresqlOperateRecognizerHolder;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author GoodBoyCoder
 */
public class PostgresqlOperateRecognizerHolderTest {
    @Test
    public void testGetUpdateRecognizer() {
        PostgresqlOperateRecognizerHolder holder = new PostgresqlOperateRecognizerHolder();
        // test with normal
        String sql = "update d.t set d.t.a = ?, d.t.b = ?, d.t.c = ?";
        SQLStatement ast = SQLUtils.parseStatements(sql, JdbcConstants.POSTGRESQL).get(0);
        Assertions.assertNotNull(holder.getUpdateRecognizer(sql, ast));

        String sql2 = "update table a inner join table b on a.id = b.pid set a.name = ?";
        SQLStatement ast2 = SQLUtils.parseStatements(sql2, JdbcConstants.POSTGRESQL).get(0);
        Assertions.assertThrows(NotSupportYetException.class, () -> holder.getUpdateRecognizer(sql2, ast2));

        String sql3 = "update t set id = 1 where id in (select id from b)";
        SQLStatement ast3 = SQLUtils.parseStatements(sql3, JdbcConstants.POSTGRESQL).get(0);
        Assertions.assertThrows(NotSupportYetException.class, () -> holder.getUpdateRecognizer(sql3, ast3));
    }

    @Test
    public void testGetInsertRecognizer() {
        PostgresqlOperateRecognizerHolder holder = new PostgresqlOperateRecognizerHolder();
        // test with normal
        String sql = "insert into a values (1, 2)";
        SQLStatement ast = SQLUtils.parseStatements(sql, JdbcConstants.POSTGRESQL).get(0);
        Assertions.assertNotNull(holder.getInsertRecognizer(sql, ast));

        String sql2 = "insert into a (id, name) values (1, 2), (3, 4)";
        SQLStatement ast2 = SQLUtils.parseStatements(sql2, JdbcConstants.POSTGRESQL).get(0);
        Assertions.assertNotNull(holder.getInsertRecognizer(sql2, ast2));

        String sql3 = "insert into a select * from b";
        SQLStatement ast3 = SQLUtils.parseStatements(sql3, JdbcConstants.POSTGRESQL).get(0);
        Assertions.assertThrows(NotSupportYetException.class, () -> holder.getInsertRecognizer(sql3, ast3));
    }

    @Test
    public void testGetDeleteRecognizer() {
        PostgresqlOperateRecognizerHolder holder = new PostgresqlOperateRecognizerHolder();
        // test with normal
        String sql = "delete t where id = ?";
        SQLStatement ast = SQLUtils.parseStatements(sql, JdbcConstants.POSTGRESQL).get(0);
        Assertions.assertNotNull(holder.getDeleteRecognizer(sql, ast));

        String sql2 = "delete t where id in (select id from b)";
        SQLStatement ast2 = SQLUtils.parseStatements(sql2, JdbcConstants.POSTGRESQL).get(0);
        Assertions.assertThrows(NotSupportYetException.class, () -> holder.getDeleteRecognizer(sql2, ast2));
    }

    @Test
    public void testGetSelectForUpdateRecognizer() {
        PostgresqlOperateRecognizerHolder holder = new PostgresqlOperateRecognizerHolder();
        // test with normal
        String sql = "select * from t for update";
        SQLStatement ast = SQLUtils.parseStatements(sql, JdbcConstants.POSTGRESQL).get(0);
        Assertions.assertNotNull(holder.getSelectForUpdateRecognizer(sql, ast));

        String sql2 = "select * from (select * from t) for update";
        SQLStatement ast2 = SQLUtils.parseStatements(sql2, JdbcConstants.POSTGRESQL).get(0);
        Assertions.assertThrows(NotSupportYetException.class, () -> holder.getSelectForUpdateRecognizer(sql2, ast2));
    }
}
