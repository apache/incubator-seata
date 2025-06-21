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

import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.sqlparser.SQLRecognizer;
import org.apache.seata.sqlparser.SQLRecognizerFactory;
import org.apache.seata.sqlparser.SQLType;
import org.apache.seata.sqlparser.SqlParserType;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class DruidSQLRecognizerFactoryTest {
    @Test
    public void testSqlRecognizerCreation() {
        SQLRecognizerFactory recognizerFactory =
                EnhancedServiceLoader.load(SQLRecognizerFactory.class, SqlParserType.SQL_PARSER_TYPE_DRUID);
        Assertions.assertNotNull(recognizerFactory);
        List<SQLRecognizer> recognizers = recognizerFactory.create("delete from t1", JdbcConstants.MYSQL);
        Assertions.assertNotNull(recognizers);
        Assertions.assertEquals(recognizers.size(), 1);
        Assertions.assertEquals(SQLType.DELETE, recognizers.get(0).getSQLType());

        recognizers = recognizerFactory.create("delete from t1", JdbcConstants.MARIADB);
        Assertions.assertNotNull(recognizers);
        Assertions.assertEquals(recognizers.size(), 1);
        Assertions.assertEquals(SQLType.DELETE, recognizers.get(0).getSQLType());

        recognizers = recognizerFactory.create("delete from t1", JdbcConstants.POLARDBX);
        Assertions.assertNotNull(recognizers);
        Assertions.assertEquals(recognizers.size(), 1);
        Assertions.assertEquals(SQLType.DELETE, recognizers.get(0).getSQLType());

        recognizers = recognizerFactory.create("delete from t1", JdbcConstants.DM);
        Assertions.assertNotNull(recognizers);
        Assertions.assertEquals(recognizers.size(), 1);
        Assertions.assertEquals(SQLType.DELETE, recognizers.get(0).getSQLType());

        recognizers = recognizerFactory.create("delete from t1", JdbcConstants.KINGBASE);
        Assertions.assertNotNull(recognizers);
        Assertions.assertEquals(recognizers.size(), 1);
        Assertions.assertEquals(SQLType.DELETE, recognizers.get(0).getSQLType());

        recognizers = recognizerFactory.create("delete from t1", JdbcConstants.OSCAR);
        Assertions.assertNotNull(recognizers);
        Assertions.assertEquals(recognizers.size(), 1);
        Assertions.assertEquals(SQLType.DELETE, recognizers.get(0).getSQLType());

        // test sql syntax
        String sql = "update d.t set d.t.a = ?, d.t.b = ?, d.t.c = ?";
        Assertions.assertNotNull(recognizerFactory.create(sql, JdbcConstants.MYSQL));
        Assertions.assertNotNull(recognizerFactory.create(sql, JdbcConstants.MARIADB));
        Assertions.assertNotNull(recognizerFactory.create(sql, JdbcConstants.POLARDBX));
        Assertions.assertNotNull(recognizerFactory.create(sql, JdbcConstants.ORACLE));
        Assertions.assertNotNull(recognizerFactory.create(sql, JdbcConstants.POSTGRESQL));
        Assertions.assertNotNull(recognizerFactory.create(sql, JdbcConstants.DM));
        Assertions.assertNotNull(recognizerFactory.create(sql, JdbcConstants.KINGBASE));
        Assertions.assertNotNull(recognizerFactory.create(sql, JdbcConstants.OSCAR));

        String sql5 = "insert into a values (1, 2)";
        Assertions.assertNotNull(recognizerFactory.create(sql5, JdbcConstants.MYSQL));
        Assertions.assertNotNull(recognizerFactory.create(sql5, JdbcConstants.MARIADB));
        Assertions.assertNotNull(recognizerFactory.create(sql5, JdbcConstants.POLARDBX));
        Assertions.assertNotNull(recognizerFactory.create(sql5, JdbcConstants.ORACLE));
        Assertions.assertNotNull(recognizerFactory.create(sql5, JdbcConstants.POSTGRESQL));
        Assertions.assertNotNull(recognizerFactory.create(sql5, JdbcConstants.DM));
        Assertions.assertNotNull(recognizerFactory.create(sql5, JdbcConstants.KINGBASE));
        Assertions.assertNotNull(recognizerFactory.create(sql5, JdbcConstants.OSCAR));

        String sql6 = "insert into a (id, name) values (1, 2), (3, 4)";
        Assertions.assertNotNull(recognizerFactory.create(sql6, JdbcConstants.MYSQL));
        Assertions.assertNotNull(recognizerFactory.create(sql6, JdbcConstants.MARIADB));
        Assertions.assertNotNull(recognizerFactory.create(sql6, JdbcConstants.POLARDBX));
        Assertions.assertNotNull(recognizerFactory.create(sql6, JdbcConstants.ORACLE));
        Assertions.assertNotNull(recognizerFactory.create(sql6, JdbcConstants.POSTGRESQL));
        Assertions.assertNotNull(recognizerFactory.create(sql6, JdbcConstants.DM));
        Assertions.assertNotNull(recognizerFactory.create(sql6, JdbcConstants.KINGBASE));
        Assertions.assertNotNull(recognizerFactory.create(sql6, JdbcConstants.OSCAR));

        String sql8 = "delete from t where id = ?";
        Assertions.assertNotNull(recognizerFactory.create(sql8, JdbcConstants.MYSQL));
        Assertions.assertNotNull(recognizerFactory.create(sql8, JdbcConstants.MARIADB));
        Assertions.assertNotNull(recognizerFactory.create(sql8, JdbcConstants.POLARDBX));
        Assertions.assertNotNull(recognizerFactory.create(sql8, JdbcConstants.ORACLE));
        Assertions.assertNotNull(recognizerFactory.create(sql8, JdbcConstants.POSTGRESQL));
        Assertions.assertNotNull(recognizerFactory.create(sql8, JdbcConstants.DM));
        Assertions.assertNotNull(recognizerFactory.create(sql8, JdbcConstants.KINGBASE));
        Assertions.assertNotNull(recognizerFactory.create(sql8, JdbcConstants.OSCAR));

        String sql10 = "select * from t for update";
        Assertions.assertNotNull(recognizerFactory.create(sql10, JdbcConstants.MYSQL));
        Assertions.assertNotNull(recognizerFactory.create(sql10, JdbcConstants.MARIADB));
        Assertions.assertNotNull(recognizerFactory.create(sql10, JdbcConstants.POLARDBX));
        Assertions.assertNotNull(recognizerFactory.create(sql10, JdbcConstants.ORACLE));
        Assertions.assertNotNull(recognizerFactory.create(sql10, JdbcConstants.POSTGRESQL));
        Assertions.assertNotNull(recognizerFactory.create(sql10, JdbcConstants.KINGBASE));
        Assertions.assertNotNull(recognizerFactory.create(sql10, JdbcConstants.DM));
        Assertions.assertNotNull(recognizerFactory.create(sql10, JdbcConstants.OSCAR));
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

        // When dbtype are DM and SQLSERVER, druid cannot parse the sql syntax 'replace'
        try {
            recognizerFactory.create(sql3, JdbcConstants.DM);
        } catch (Exception e) {
            e.printStackTrace();
        }

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
}
