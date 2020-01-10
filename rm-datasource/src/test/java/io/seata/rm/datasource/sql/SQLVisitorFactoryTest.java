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
package io.seata.rm.datasource.sql;

import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.druid.MySQLDeleteRecognizer;
import io.seata.sqlparser.druid.MySQLInsertRecognizer;
import io.seata.sqlparser.druid.MySQLSelectForUpdateRecognizer;
import io.seata.sqlparser.druid.MySQLUpdateRecognizer;
import io.seata.sqlparser.druid.oracle.OracleDeleteRecognizer;
import io.seata.sqlparser.druid.oracle.OracleInsertRecognizer;
import io.seata.sqlparser.druid.oracle.OracleSelectForUpdateRecognizer;
import io.seata.sqlparser.druid.oracle.OracleUpdateRecognizer;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * The type Sql visitor factory test.
 */
public class SQLVisitorFactoryTest {
    /**
     * Test sql recognizing.
     */
    @Test
    public void testSqlRecognizing() {

        //test for ast was null
        Assertions.assertThrows(UnsupportedOperationException.class, () -> SQLVisitorFactory.get("", JdbcConstants.MYSQL));

        //test for mysql insert
        String sql = "insert into t(id) values (1)";
        SQLRecognizer recognizer = SQLVisitorFactory.get(sql, JdbcConstants.MYSQL);
        Assertions.assertTrue(recognizer instanceof MySQLInsertRecognizer);

        //test for mysql delete
        sql = "delete from t";
        recognizer = SQLVisitorFactory.get(sql, JdbcConstants.MYSQL);
        Assertions.assertTrue(recognizer instanceof MySQLDeleteRecognizer);

        //test for mysql update
        sql = "update t set a = a";
        recognizer = SQLVisitorFactory.get(sql, JdbcConstants.MYSQL);
        Assertions.assertTrue(recognizer instanceof MySQLUpdateRecognizer);

        //test for mysql select
        sql = "select * from t";
        recognizer = SQLVisitorFactory.get(sql, JdbcConstants.MYSQL);
        Assertions.assertNull(recognizer);

        //test for mysql select for update
        sql = "select * from t for update";
        recognizer = SQLVisitorFactory.get(sql, JdbcConstants.MYSQL);
        Assertions.assertTrue(recognizer instanceof MySQLSelectForUpdateRecognizer);

        //test for oracle insert
        sql = "insert into t(id) values (1)";
        recognizer = SQLVisitorFactory.get(sql, JdbcConstants.ORACLE);
        Assertions.assertTrue(recognizer instanceof OracleInsertRecognizer);

        //test for oracle delete
        sql = "delete from t";
        recognizer = SQLVisitorFactory.get(sql, JdbcConstants.ORACLE);
        Assertions.assertTrue(recognizer instanceof OracleDeleteRecognizer);

        //test for oracle update
        sql = "update t set a = a";
        recognizer = SQLVisitorFactory.get(sql, JdbcConstants.ORACLE);
        Assertions.assertTrue(recognizer instanceof OracleUpdateRecognizer);

        //test for oracle select
        sql = "select * from t";
        recognizer = SQLVisitorFactory.get(sql, JdbcConstants.ORACLE);
        Assertions.assertNull(recognizer);

        //test for oracle select for update
        sql = "select * from t for update";
        recognizer = SQLVisitorFactory.get(sql, JdbcConstants.ORACLE);
        Assertions.assertTrue(recognizer instanceof OracleSelectForUpdateRecognizer);

        //test for do not support db
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.get("select * from t", JdbcConstants.DB2);
        });


        //TEST FOR Multi-SQL

        List<SQLRecognizer> sqlRecognizers = null;
        //test for mysql insert
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.getMulti("insert into t(id) values (1);insert into t(id) values (2)", JdbcConstants.MYSQL);
        });
        //test for mysql insert and update
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.getMulti("insert into t(id) values (1);update t set a = t;", JdbcConstants.MYSQL);
        });
        //test for mysql insert and deleted
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.getMulti("insert into t(id) values (1);delete from t where id = 1", JdbcConstants.MYSQL);
        });
        //test for mysql delete
        sql = "delete from t where id =1 ; delete from t where id = 2";
        sqlRecognizers = SQLVisitorFactory.getMulti(sql, JdbcConstants.MYSQL);
        for (SQLRecognizer sqlRecognizer : sqlRecognizers) {
            Assertions.assertTrue(sqlRecognizer instanceof MySQLDeleteRecognizer);
        }
        //test for mysql update
        sql = "update t set a = a;update t set a = c;";
        sqlRecognizers = SQLVisitorFactory.getMulti(sql, JdbcConstants.MYSQL);
        for (SQLRecognizer sqlRecognizer : sqlRecognizers) {
            Assertions.assertTrue(sqlRecognizer instanceof MySQLUpdateRecognizer);
        }
        //test for mysql update and deleted
        sql = "update t set a = a where id =1;update t set a = c where id = 1;delete from t where id =1 ";
        sqlRecognizers = SQLVisitorFactory.getMulti(sql, JdbcConstants.MYSQL);
        Assertions.assertTrue(sqlRecognizers.get(0) instanceof MySQLUpdateRecognizer);
        Assertions.assertTrue(sqlRecognizers.get(1) instanceof MySQLUpdateRecognizer);
        Assertions.assertTrue(sqlRecognizers.get(2) instanceof MySQLDeleteRecognizer);
        //test for mysql select
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.getMulti("select * from d where id = 1; select * from t where id = 2", JdbcConstants.MYSQL);
        });

        //test for mysql select for update
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.getMulti("select * from t for update; select * from t where id = 2", JdbcConstants.MYSQL);
        });

        //test for oracle insert
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.getMulti("insert into t(id) values (1);insert into t(id) values (2)", JdbcConstants.ORACLE);
        });

        //test for oracle delete and deleted
        sql = "delete from t where id =1 ; delete from t where id = 2";
        sqlRecognizers = SQLVisitorFactory.getMulti(sql, JdbcConstants.ORACLE);
        for (SQLRecognizer sqlRecognizer : sqlRecognizers) {
            Assertions.assertTrue(sqlRecognizer instanceof OracleDeleteRecognizer);
        }

        //test for oracle update
        sql = "update t set a = b where id =1 ;update t set a = c where id = 1;";
        sqlRecognizers = SQLVisitorFactory.getMulti(sql, JdbcConstants.ORACLE);
        for (SQLRecognizer sqlRecognizer : sqlRecognizers) {
            Assertions.assertTrue(sqlRecognizer instanceof OracleUpdateRecognizer);
        }

        //test for oracle select
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.getMulti("select * from b ; select * from t where id = 2", JdbcConstants.ORACLE);
        });

        //test for oracle select for update
        //test for mysql select for update
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.getMulti("select * from t for update; select * from t where id = 2", JdbcConstants.ORACLE);
        });

        //test for oracle insert and update
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.getMulti("insert into t(id) values (1);update t set a = t;", JdbcConstants.ORACLE);
        });
        //test for oracle insert and deleted
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.getMulti("insert into t(id) values (1);delete from t where id = 1", JdbcConstants.ORACLE);
        });




        Assertions.assertThrows(UnsupportedOperationException.class, () -> SQLVisitorFactory.get("select * from t", JdbcConstants.DB2));
    }

    @Test
    public void testSqlRecognizerLoading() {
        SQLRecognizer recognizer = SQLVisitorFactory.get("update t1 set name = 'test' where id = '1'", JdbcConstants.MYSQL);
        Assertions.assertNotNull(recognizer);
        Assertions.assertEquals(SQLType.UPDATE, recognizer.getSQLType());
        Assertions.assertEquals("t1", recognizer.getTableName());
    }
}
