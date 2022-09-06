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

import io.seata.common.exception.NotSupportYetException;
import io.seata.common.loader.EnhancedServiceNotFoundException;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.druid.mysql.MySQLDeleteRecognizer;
import io.seata.sqlparser.druid.mysql.MySQLInsertRecognizer;
import io.seata.sqlparser.druid.mysql.MySQLSelectForUpdateRecognizer;
import io.seata.sqlparser.druid.mysql.MySQLUpdateRecognizer;
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
        List<SQLRecognizer> recognizer = SQLVisitorFactory.get("insert into t(id) values (1)", JdbcConstants.MYSQL);
        Assertions.assertEquals(recognizer.get(0).getClass().getName(), MySQLInsertRecognizer.class.getName());

        //test for mysql delete
        recognizer = SQLVisitorFactory.get("delete from t", JdbcConstants.MYSQL);
        Assertions.assertEquals(recognizer.get(0).getClass().getName(), MySQLDeleteRecognizer.class.getName());

        //test for mysql update
        recognizer = SQLVisitorFactory.get("update t set a = a", JdbcConstants.MYSQL);
        Assertions.assertEquals(recognizer.get(0).getClass().getName(), MySQLUpdateRecognizer.class.getName());

        //test for mysql select
        Assertions.assertThrows(NotSupportYetException.class, () -> SQLVisitorFactory.get("select * from t", JdbcConstants.MYSQL));

        //test for mysql select for update
        recognizer = SQLVisitorFactory.get("select * from t for update", JdbcConstants.MYSQL);
        Assertions.assertEquals(recognizer.get(0).getClass().getName(), MySQLSelectForUpdateRecognizer.class.getName());

        //test for oracle insert
        recognizer = SQLVisitorFactory.get("insert into t(id) values (1)", JdbcConstants.ORACLE);
        Assertions.assertEquals(recognizer.get(0).getClass().getName(), OracleInsertRecognizer.class.getName());

        //test for oracle delete
        recognizer = SQLVisitorFactory.get("delete from t", JdbcConstants.ORACLE);
        Assertions.assertEquals(recognizer.get(0).getClass().getName(), OracleDeleteRecognizer.class.getName());

        //test for oracle update
        recognizer = SQLVisitorFactory.get("update t set a = a", JdbcConstants.ORACLE);
        Assertions.assertEquals(recognizer.get(0).getClass().getName(), OracleUpdateRecognizer.class.getName());

        //test for oracle select
        Assertions.assertThrows(NotSupportYetException.class, () -> SQLVisitorFactory.get("select * from t", JdbcConstants.ORACLE));

        //test for oracle select for update
        recognizer = SQLVisitorFactory.get("select * from t for update", JdbcConstants.ORACLE);
        Assertions.assertEquals(recognizer.get(0).getClass().getName(), OracleSelectForUpdateRecognizer.class.getName());

        //test for do not support db
        Assertions.assertThrows(EnhancedServiceNotFoundException.class, () -> SQLVisitorFactory.get("select * from t", JdbcConstants.DB2));


        //TEST FOR Multi-SQL

        List<SQLRecognizer> sqlRecognizers;
        //test for mysql insert
        Assertions.assertThrows(NotSupportYetException.class, () -> SQLVisitorFactory.get("insert into t(id) values (1);insert into t(id) values (2)", JdbcConstants.MYSQL));
        //test for mysql insert and update
        Assertions.assertThrows(NotSupportYetException.class, () -> SQLVisitorFactory.get("insert into t(id) values (1);update t set a = t;", JdbcConstants.MYSQL));
        //test for mysql insert and deleted
        Assertions.assertThrows(NotSupportYetException.class, () -> SQLVisitorFactory.get("insert into t(id) values (1);delete from t where id = 1", JdbcConstants.MYSQL));
        //test for mysql delete
        sqlRecognizers = SQLVisitorFactory.get("delete from t where id =1 ; delete from t where id = 2", JdbcConstants.MYSQL);
        for (SQLRecognizer sqlRecognizer : sqlRecognizers) {
            Assertions.assertEquals(sqlRecognizer.getClass().getName(), MySQLDeleteRecognizer.class.getName());
        }
        //test for mysql update
        sqlRecognizers = SQLVisitorFactory.get("update t set a = a;update t set a = c;", JdbcConstants.MYSQL);
        for (SQLRecognizer sqlRecognizer : sqlRecognizers) {
            Assertions.assertEquals(sqlRecognizer.getClass().getName(), MySQLUpdateRecognizer.class.getName());
        }
        //test for mysql update and deleted
        Assertions.assertThrows(NotSupportYetException.class, () -> SQLVisitorFactory.get("update t set a = a where id =1;update t set a = c where id = 1;delete from t where id =1", JdbcConstants.MYSQL));
        //test for mysql select
        Assertions.assertThrows(NotSupportYetException.class, () -> SQLVisitorFactory.get("select * from d where id = 1; select * from t where id = 2", JdbcConstants.MYSQL));

        //test for mysql select for update
        Assertions.assertThrows(NotSupportYetException.class, () -> SQLVisitorFactory.get("select * from t for update; select * from t where id = 2", JdbcConstants.MYSQL));

        //test for oracle insert
        Assertions.assertThrows(NotSupportYetException.class, () -> SQLVisitorFactory.get("insert into t(id) values (1);insert into t(id) values (2)", JdbcConstants.ORACLE));

        //test for oracle delete and deleted
        sqlRecognizers = SQLVisitorFactory.get("delete from t where id =1 ; delete from t where id = 2", JdbcConstants.ORACLE);
        for (SQLRecognizer sqlRecognizer : sqlRecognizers) {
            Assertions.assertEquals(sqlRecognizer.getClass().getName(), OracleDeleteRecognizer.class.getName());
        }

        //test for oracle update
        sqlRecognizers = SQLVisitorFactory.get("update t set a = b where id =1 ;update t set a = c where id = 1;", JdbcConstants.ORACLE);
        for (SQLRecognizer sqlRecognizer : sqlRecognizers) {
            Assertions.assertEquals(sqlRecognizer.getClass().getName(), OracleUpdateRecognizer.class.getName());
        }

        //test for oracle select
        Assertions.assertThrows(NotSupportYetException.class, () -> SQLVisitorFactory.get("select * from b ; select * from t where id = 2", JdbcConstants.ORACLE));

        //test for oracle select for update
        //test for mysql select for update
        Assertions.assertThrows(NotSupportYetException.class, () -> SQLVisitorFactory.get("select * from t for update; select * from t where id = 2", JdbcConstants.ORACLE));

        //test for oracle insert and update
        Assertions.assertThrows(NotSupportYetException.class, () -> SQLVisitorFactory.get("insert into t(id) values (1);update t set a = t;", JdbcConstants.ORACLE));
        //test for oracle insert and deleted
        Assertions.assertThrows(NotSupportYetException.class, () -> SQLVisitorFactory.get("insert into t(id) values (1);delete from t where id = 1", JdbcConstants.ORACLE));
    }

    @Test
    public void testSqlRecognizerLoading() {
        List<SQLRecognizer> recognizers = SQLVisitorFactory.get("update t1 set name = 'test' where id = '1'", JdbcConstants.MYSQL);
        Assertions.assertNotNull(recognizers);
        Assertions.assertEquals(recognizers.size(), 1);
        SQLRecognizer recognizer = recognizers.get(0);
        Assertions.assertEquals(SQLType.UPDATE, recognizer.getSQLType());
        Assertions.assertEquals("t1", recognizer.getTableName());
    }
}
