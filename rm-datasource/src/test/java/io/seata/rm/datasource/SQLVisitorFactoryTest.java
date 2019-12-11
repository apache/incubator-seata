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
package io.seata.rm.datasource;

import com.alibaba.druid.util.JdbcConstants;
import io.seata.rm.datasource.sql.SQLRecognizer;
import io.seata.rm.datasource.sql.SQLVisitorFactory;
import io.seata.rm.datasource.sql.druid.MySQLDeleteRecognizer;
import io.seata.rm.datasource.sql.druid.MySQLInsertRecognizer;
import io.seata.rm.datasource.sql.druid.MySQLSelectForUpdateRecognizer;

import io.seata.rm.datasource.sql.druid.MySQLUpdateRecognizer;
import io.seata.rm.datasource.sql.druid.oracle.OracleDeleteRecognizer;
import io.seata.rm.datasource.sql.druid.oracle.OracleInsertRecognizer;
import io.seata.rm.datasource.sql.druid.oracle.OracleSelectForUpdateRecognizer;
import io.seata.rm.datasource.sql.druid.oracle.OracleUpdateRecognizer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.get("", JdbcConstants.MYSQL);
        });

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
    }
}
