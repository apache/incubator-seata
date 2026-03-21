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
package org.apache.seata.sqlparser.druid;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleMultiInsertStatement;
import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.sqlparser.SQLRecognizer;
import org.apache.seata.sqlparser.SQLRecognizerFactory;
import org.apache.seata.sqlparser.SQLType;
import org.apache.seata.sqlparser.SqlParserType;
import org.apache.seata.sqlparser.druid.oracle.OracleOperateRecognizerHolder;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DruidSQLRecognizerFactoryTest {
    @Test
    public void testSqlRecognizerCreation() {
        SQLRecognizerFactory recognizerFactory =
                EnhancedServiceLoader.load(SQLRecognizerFactory.class, SqlParserType.SQL_PARSER_TYPE_DRUID);
        assertNotNull(recognizerFactory);
        List<SQLRecognizer> recognizers = recognizerFactory.create("delete from t1", JdbcConstants.MYSQL);
        assertNotNull(recognizers);
        Assertions.assertEquals(recognizers.size(), 1);
        Assertions.assertEquals(SQLType.DELETE, recognizers.get(0).getSQLType());

        recognizers = recognizerFactory.create("delete from t1", JdbcConstants.MARIADB);
        assertNotNull(recognizers);
        Assertions.assertEquals(recognizers.size(), 1);
        Assertions.assertEquals(SQLType.DELETE, recognizers.get(0).getSQLType());

        recognizers = recognizerFactory.create("delete from t1", JdbcConstants.POLARDBX);
        assertNotNull(recognizers);
        Assertions.assertEquals(recognizers.size(), 1);
        Assertions.assertEquals(SQLType.DELETE, recognizers.get(0).getSQLType());

        recognizers = recognizerFactory.create("delete from t1", JdbcConstants.DM);
        assertNotNull(recognizers);
        Assertions.assertEquals(recognizers.size(), 1);
        Assertions.assertEquals(SQLType.DELETE, recognizers.get(0).getSQLType());

        recognizers = recognizerFactory.create("delete from t1", JdbcConstants.KINGBASE);
        assertNotNull(recognizers);
        Assertions.assertEquals(recognizers.size(), 1);
        Assertions.assertEquals(SQLType.DELETE, recognizers.get(0).getSQLType());

        recognizers = recognizerFactory.create("delete from t1", JdbcConstants.OSCAR);
        assertNotNull(recognizers);
        Assertions.assertEquals(recognizers.size(), 1);
        Assertions.assertEquals(SQLType.DELETE, recognizers.get(0).getSQLType());

        // test sql syntax
        String sql = "update d.t set d.t.a = ?, d.t.b = ?, d.t.c = ?";
        assertNotNull(recognizerFactory.create(sql, JdbcConstants.MYSQL));
        assertNotNull(recognizerFactory.create(sql, JdbcConstants.MARIADB));
        assertNotNull(recognizerFactory.create(sql, JdbcConstants.POLARDBX));
        assertNotNull(recognizerFactory.create(sql, JdbcConstants.ORACLE));
        assertNotNull(recognizerFactory.create(sql, JdbcConstants.POSTGRESQL));
        assertNotNull(recognizerFactory.create(sql, JdbcConstants.DM));
        assertNotNull(recognizerFactory.create(sql, JdbcConstants.KINGBASE));
        assertNotNull(recognizerFactory.create(sql, JdbcConstants.OSCAR));

        String sql5 = "insert into a values (1, 2)";
        assertNotNull(recognizerFactory.create(sql5, JdbcConstants.MYSQL));
        assertNotNull(recognizerFactory.create(sql5, JdbcConstants.MARIADB));
        assertNotNull(recognizerFactory.create(sql5, JdbcConstants.POLARDBX));
        assertNotNull(recognizerFactory.create(sql5, JdbcConstants.ORACLE));
        assertNotNull(recognizerFactory.create(sql5, JdbcConstants.POSTGRESQL));
        assertNotNull(recognizerFactory.create(sql5, JdbcConstants.DM));
        assertNotNull(recognizerFactory.create(sql5, JdbcConstants.KINGBASE));
        assertNotNull(recognizerFactory.create(sql5, JdbcConstants.OSCAR));

        String sql6 = "insert into a (id, name) values (1, 2), (3, 4)";
        assertNotNull(recognizerFactory.create(sql6, JdbcConstants.MYSQL));
        assertNotNull(recognizerFactory.create(sql6, JdbcConstants.MARIADB));
        assertNotNull(recognizerFactory.create(sql6, JdbcConstants.POLARDBX));
        assertNotNull(recognizerFactory.create(sql6, JdbcConstants.ORACLE));
        assertNotNull(recognizerFactory.create(sql6, JdbcConstants.POSTGRESQL));
        assertNotNull(recognizerFactory.create(sql6, JdbcConstants.DM));
        assertNotNull(recognizerFactory.create(sql6, JdbcConstants.KINGBASE));
        assertNotNull(recognizerFactory.create(sql6, JdbcConstants.OSCAR));

        String sql8 = "delete from t where id = ?";
        assertNotNull(recognizerFactory.create(sql8, JdbcConstants.MYSQL));
        assertNotNull(recognizerFactory.create(sql8, JdbcConstants.MARIADB));
        assertNotNull(recognizerFactory.create(sql8, JdbcConstants.POLARDBX));
        assertNotNull(recognizerFactory.create(sql8, JdbcConstants.ORACLE));
        assertNotNull(recognizerFactory.create(sql8, JdbcConstants.POSTGRESQL));
        assertNotNull(recognizerFactory.create(sql8, JdbcConstants.DM));
        assertNotNull(recognizerFactory.create(sql8, JdbcConstants.KINGBASE));
        assertNotNull(recognizerFactory.create(sql8, JdbcConstants.OSCAR));

        String sql10 = "select * from t for update";
        assertNotNull(recognizerFactory.create(sql10, JdbcConstants.MYSQL));
        assertNotNull(recognizerFactory.create(sql10, JdbcConstants.MARIADB));
        assertNotNull(recognizerFactory.create(sql10, JdbcConstants.POLARDBX));
        assertNotNull(recognizerFactory.create(sql10, JdbcConstants.ORACLE));
        assertNotNull(recognizerFactory.create(sql10, JdbcConstants.POSTGRESQL));
        assertNotNull(recognizerFactory.create(sql10, JdbcConstants.KINGBASE));
        assertNotNull(recognizerFactory.create(sql10, JdbcConstants.DM));
        assertNotNull(recognizerFactory.create(sql10, JdbcConstants.OSCAR));
    }

    @Test
    public void testIsSqlSyntaxSupports() {
        SQLRecognizerFactory recognizerFactory =
                EnhancedServiceLoader.load(SQLRecognizerFactory.class, SqlParserType.SQL_PARSER_TYPE_DRUID);

        String sql1 = "delete from t where id in (select id from b)";
        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql1, JdbcConstants.MYSQL));
        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql1, JdbcConstants.MARIADB));
        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql1, JdbcConstants.POLARDBX));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql1, JdbcConstants.DM));

        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql1, JdbcConstants.SQLSERVER));
        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql1, JdbcConstants.KINGBASE));
        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql1, JdbcConstants.OSCAR));
        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql1, JdbcConstants.POSTGRESQL));
        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql1, JdbcConstants.ORACLE));

        String sql2 = "select * from (select * from t) for update";
        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql2, JdbcConstants.MYSQL));
        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql2, JdbcConstants.MARIADB));
        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql2, JdbcConstants.POLARDBX));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql2, JdbcConstants.DM));

        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql2, JdbcConstants.KINGBASE));
        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql2, JdbcConstants.OSCAR));
        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql2, JdbcConstants.POSTGRESQL));
        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql2, JdbcConstants.ORACLE));

        String sql3 = "replace into t (id,dr) values (1,'2'), (2,'3')";
        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql3, JdbcConstants.MYSQL));
        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql3, JdbcConstants.MARIADB));
        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql3, JdbcConstants.POLARDBX));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql3, JdbcConstants.DM));

        String sql5 = "insert into a select * from b";
        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql5, JdbcConstants.MYSQL));
        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql5, JdbcConstants.MARIADB));
        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql5, JdbcConstants.POLARDBX));
        Assertions.assertThrows(NotSupportYetException.class, () -> recognizerFactory.create(sql5, JdbcConstants.DM));

        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql5, JdbcConstants.SQLSERVER));
        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql5, JdbcConstants.KINGBASE));
        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql5, JdbcConstants.OSCAR));
        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql5, JdbcConstants.POSTGRESQL));
        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql5, JdbcConstants.ORACLE));

        String sql6 = "select * from (select * from t)";
        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql6, JdbcConstants.SQLSERVER));

        String sql7 = "update a set id = b.pid from b where a.id = b.id";
        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql7, JdbcConstants.POSTGRESQL));

        String sql8 = "update a set a.id = (select id from b where a.pid = b.pid)";
        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql8, JdbcConstants.ORACLE));
        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql8, JdbcConstants.KINGBASE));

        String sql9 = "update (select a.id,a.name from a inner join b on a.id = b.id) t set t.name = 'xxx'";
        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql9, JdbcConstants.ORACLE));
        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql9, JdbcConstants.KINGBASE));
    }

    @EnabledIfSystemProperty(
            named = "druid.version",
            matches = "(1\\.[3-9]\\..*)|(2\\..*)|(1\\.2\\.[5-9].*)|(1\\.2\\.[1-9][0-9].*)")
    @Test
    public void testIsSqlSyntaxSupportsForOscar() {
        SQLRecognizerFactory recognizerFactory =
                EnhancedServiceLoader.load(SQLRecognizerFactory.class, SqlParserType.SQL_PARSER_TYPE_DRUID);

        String sql1 = "delete from t where id in (select id from b)";

        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql1, JdbcConstants.OSCAR));

        String sql2 = "select * from (select * from t) for update";
        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql2, JdbcConstants.OSCAR));

        String sql5 = "insert into a select * from b";
        Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql5, JdbcConstants.OSCAR));
    }

    @Test
    public void testInsertFirstNotSupported() {
        SQLRecognizerFactory recognizerFactory =
                EnhancedServiceLoader.load(SQLRecognizerFactory.class, SqlParserType.SQL_PARSER_TYPE_DRUID);
        // Test that INSERT FIRST syntax should be rejected at the Factory level
        String sql = "INSERT FIRST "
                + "WHEN salary > 1000 THEN INTO high_earners (id, name, salary) VALUES (1, 'John', 2000) "
                + "WHEN salary <= 1000 THEN INTO low_earners (id, name, salary) VALUES (1, 'John', 800) "
                + "SELECT 1 FROM DUAL";

        NotSupportYetException exception = Assertions.assertThrows(
                NotSupportYetException.class, () -> recognizerFactory.create(sql, JdbcConstants.ORACLE));

        assertTrue(exception.getMessage().contains("INSERT FIRST not supported yet"));
    }

    @Test
    void testGetMultiInsertRecognizerDelegation() {
        // 1.sql
        String sql = "INSERT ALL INTO a(id) VALUES(1) INTO a(id) VALUES(2) SELECT 1 FROM dual";

        SQLStatement stmt = SQLUtils.parseSingleStatement(sql, "oracle");
        assertTrue(stmt instanceof OracleMultiInsertStatement);

        // 2. mock recognizerHolder and recognizer
        OracleOperateRecognizerHolder recognizerHolder = mock(OracleOperateRecognizerHolder.class);
        SQLRecognizer mockRecognizer = mock(SQLRecognizer.class);

        // 3. stub getMultiInsertRecognizer
        when(recognizerHolder.getMultiInsertRecognizer(sql, stmt)).thenReturn(mockRecognizer);

        SQLRecognizer recognizer =
                ((OracleOperateRecognizerHolder) recognizerHolder).getMultiInsertRecognizer(sql, stmt);

        assertNotNull(recognizer);
        assertSame(mockRecognizer, recognizer);

        verify(recognizerHolder, times(1)).getMultiInsertRecognizer(sql, stmt);
    }
}
