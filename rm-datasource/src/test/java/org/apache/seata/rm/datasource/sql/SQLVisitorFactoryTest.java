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
package org.apache.seata.rm.datasource.sql;

import org.apache.seata.rm.datasource.sql.SQLVisitorFactory;
import org.apache.seata.common.loader.EnhancedServiceNotFoundException;
import org.apache.seata.sqlparser.SQLRecognizer;
import org.apache.seata.sqlparser.SQLType;
import org.apache.seata.sqlparser.druid.mariadb.MariadbDeleteRecognizer;
import org.apache.seata.sqlparser.druid.mariadb.MariadbInsertRecognizer;
import org.apache.seata.sqlparser.druid.mariadb.MariadbSelectForUpdateRecognizer;
import org.apache.seata.sqlparser.druid.mariadb.MariadbUpdateRecognizer;
import org.apache.seata.sqlparser.druid.mysql.MySQLDeleteRecognizer;
import org.apache.seata.sqlparser.druid.mysql.MySQLInsertRecognizer;
import org.apache.seata.sqlparser.druid.mysql.MySQLSelectForUpdateRecognizer;
import org.apache.seata.sqlparser.druid.mysql.MySQLUpdateRecognizer;
import org.apache.seata.sqlparser.druid.oracle.OracleDeleteRecognizer;
import org.apache.seata.sqlparser.druid.oracle.OracleInsertRecognizer;
import org.apache.seata.sqlparser.druid.oracle.OracleSelectForUpdateRecognizer;
import org.apache.seata.sqlparser.druid.oracle.OracleUpdateRecognizer;
import org.apache.seata.sqlparser.druid.polardbx.PolarDBXDeleteRecognizer;
import org.apache.seata.sqlparser.druid.polardbx.PolarDBXInsertRecognizer;
import org.apache.seata.sqlparser.druid.polardbx.PolarDBXSelectForUpdateRecognizer;
import org.apache.seata.sqlparser.druid.polardbx.PolarDBXUpdateRecognizer;
import org.apache.seata.sqlparser.druid.sqlserver.SqlServerDeleteRecognizer;
import org.apache.seata.sqlparser.druid.sqlserver.SqlServerInsertRecognizer;
import org.apache.seata.sqlparser.druid.sqlserver.SqlServerUpdateRecognizer;
import org.apache.seata.sqlparser.util.JdbcConstants;
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

        //test for mysql/mariadb/polardb-x insert
        String sql = "insert into t(id) values (1)";
        List<SQLRecognizer> recognizer = SQLVisitorFactory.get(sql, JdbcConstants.MYSQL);
        Assertions.assertEquals(recognizer.get(0).getClass().getName(), MySQLInsertRecognizer.class.getName());
        recognizer = SQLVisitorFactory.get(sql, JdbcConstants.MARIADB);
        Assertions.assertEquals(recognizer.get(0).getClass().getName(), MariadbInsertRecognizer.class.getName());
        recognizer = SQLVisitorFactory.get(sql, JdbcConstants.POLARDBX);
        Assertions.assertEquals(recognizer.get(0).getClass().getName(), PolarDBXInsertRecognizer.class.getName());

        //test for oracle insert
        sql = "insert into t(id) values (1)";
        recognizer = SQLVisitorFactory.get(sql, JdbcConstants.ORACLE);
        Assertions.assertEquals(recognizer.get(0).getClass().getName(), OracleInsertRecognizer.class.getName());

        //test for mysql/mariadb/polardb-x delete
        sql = "delete from t";
        recognizer = SQLVisitorFactory.get(sql, JdbcConstants.MYSQL);
        Assertions.assertEquals(recognizer.get(0).getClass().getName(), MySQLDeleteRecognizer.class.getName());
        recognizer = SQLVisitorFactory.get(sql, JdbcConstants.MARIADB);
        Assertions.assertEquals(recognizer.get(0).getClass().getName(), MariadbDeleteRecognizer.class.getName());
        recognizer = SQLVisitorFactory.get(sql, JdbcConstants.POLARDBX);
        Assertions.assertEquals(recognizer.get(0).getClass().getName(), PolarDBXDeleteRecognizer.class.getName());

        //test for mysql/mariadb/polardb-x update
        sql = "update t set a = a";
        recognizer = SQLVisitorFactory.get(sql, JdbcConstants.MYSQL);
        Assertions.assertEquals(recognizer.get(0).getClass().getName(), MySQLUpdateRecognizer.class.getName());
        recognizer = SQLVisitorFactory.get(sql, JdbcConstants.MARIADB);
        Assertions.assertEquals(recognizer.get(0).getClass().getName(), MariadbUpdateRecognizer.class.getName());
        recognizer = SQLVisitorFactory.get(sql, JdbcConstants.POLARDBX);
        Assertions.assertEquals(recognizer.get(0).getClass().getName(), PolarDBXUpdateRecognizer.class.getName());

        //test for mysql/mariadb/polardb-x select
        sql = "select * from t";
        recognizer = SQLVisitorFactory.get(sql, JdbcConstants.MYSQL);
        Assertions.assertNull(recognizer);
        recognizer = SQLVisitorFactory.get(sql, JdbcConstants.MARIADB);
        Assertions.assertNull(recognizer);
        recognizer = SQLVisitorFactory.get(sql, JdbcConstants.POLARDBX);
        Assertions.assertNull(recognizer);

        //test for mysql/mariadb/polardb-x select for update
        sql = "select * from t for update";
        recognizer = SQLVisitorFactory.get(sql, JdbcConstants.MYSQL);
        Assertions.assertEquals(recognizer.get(0).getClass().getName(), MySQLSelectForUpdateRecognizer.class.getName());
        recognizer = SQLVisitorFactory.get(sql, JdbcConstants.MARIADB);
        Assertions.assertEquals(recognizer.get(0).getClass().getName(), MariadbSelectForUpdateRecognizer.class.getName());
        recognizer = SQLVisitorFactory.get(sql, JdbcConstants.POLARDBX);
        Assertions.assertEquals(recognizer.get(0).getClass().getName(), PolarDBXSelectForUpdateRecognizer.class.getName());

        //test for sqlserver insert
        sql = "insert into t(id) values (1)";
        recognizer = SQLVisitorFactory.get(sql, JdbcConstants.SQLSERVER);
        Assertions.assertEquals(recognizer.get(0).getClass().getName(), SqlServerInsertRecognizer.class.getName());

        //test for sqlserver delete
        sql = "delete from t";
        recognizer = SQLVisitorFactory.get(sql, JdbcConstants.SQLSERVER);
        Assertions.assertEquals(recognizer.get(0).getClass().getName(), SqlServerDeleteRecognizer.class.getName());

        //test for sqlserver update
        sql = "update t set a = a";
        recognizer = SQLVisitorFactory.get(sql, JdbcConstants.SQLSERVER);
        Assertions.assertEquals(recognizer.get(0).getClass().getName(), SqlServerUpdateRecognizer.class.getName());

        //test for sqlserver select
        sql = "select * from t";
        recognizer = SQLVisitorFactory.get(sql, JdbcConstants.SQLSERVER);
        Assertions.assertNull(recognizer);

        //test for oracle delete
        sql = "delete from t";
        recognizer = SQLVisitorFactory.get(sql, JdbcConstants.ORACLE);
        Assertions.assertEquals(recognizer.get(0).getClass().getName(), OracleDeleteRecognizer.class.getName());

        //test for oracle update
        sql = "update t set a = a";
        recognizer = SQLVisitorFactory.get(sql, JdbcConstants.ORACLE);
        Assertions.assertEquals(recognizer.get(0).getClass().getName(), OracleUpdateRecognizer.class.getName());

        //test for oracle select
        sql = "select * from t";
        recognizer = SQLVisitorFactory.get(sql, JdbcConstants.ORACLE);
        Assertions.assertNull(recognizer);

        //test for oracle select for update
        sql = "select * from t for update";
        recognizer = SQLVisitorFactory.get(sql, JdbcConstants.ORACLE);
        Assertions.assertEquals(recognizer.get(0).getClass().getName(), OracleSelectForUpdateRecognizer.class.getName());

        //test for do not support db
        Assertions.assertThrows(EnhancedServiceNotFoundException.class, () -> {
            SQLVisitorFactory.get("select * from t", JdbcConstants.DB2);
        });


        //TEST FOR Multi-SQL

        List<SQLRecognizer> sqlRecognizers;
        //test for mysql/mariadb/polardb-x insert
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.get("insert into t(id) values (1);insert into t(id) values (2)", JdbcConstants.MYSQL);
        });
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.get("insert into t(id) values (1);insert into t(id) values (2)", JdbcConstants.MARIADB);
        });
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.get("insert into t(id) values (1);insert into t(id) values (2)", JdbcConstants.POLARDBX);
        });

        //test for mysql/mariadb/polardb-x insert and update
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.get("insert into t(id) values (1);update t set a = t;", JdbcConstants.MYSQL);
        });
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.get("insert into t(id) values (1);update t set a = t;", JdbcConstants.MARIADB);
        });
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.get("insert into t(id) values (1);update t set a = t;", JdbcConstants.POLARDBX);
        });

        //test for mysql insert and deleted
        //test for mysql/mariadb/polardb-x insert and deleted
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.get("insert into t(id) values (1);delete from t where id = 1", JdbcConstants.MYSQL);
        });
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.get("insert into t(id) values (1);delete from t where id = 1", JdbcConstants.MARIADB);
        });
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.get("insert into t(id) values (1);delete from t where id = 1", JdbcConstants.POLARDBX);
        });

        //test for mysql/mariadb/polardb-x delete
        sql = "delete from t where id =1 ; delete from t where id = 2";
        sqlRecognizers = SQLVisitorFactory.get(sql, JdbcConstants.MYSQL);
        for (SQLRecognizer sqlRecognizer : sqlRecognizers) {
            Assertions.assertEquals(sqlRecognizer.getClass().getName(), MySQLDeleteRecognizer.class.getName());
        }
        sqlRecognizers = SQLVisitorFactory.get(sql, JdbcConstants.MARIADB);
        for (SQLRecognizer sqlRecognizer : sqlRecognizers) {
            Assertions.assertEquals(sqlRecognizer.getClass().getName(), MariadbDeleteRecognizer.class.getName());
        }
        sqlRecognizers = SQLVisitorFactory.get(sql, JdbcConstants.POLARDBX);
        for (SQLRecognizer sqlRecognizer : sqlRecognizers) {
            Assertions.assertEquals(sqlRecognizer.getClass().getName(), PolarDBXDeleteRecognizer.class.getName());
        }

        //test for mysql/mariadb/polardb-x update
        sql = "update t set a = a;update t set a = c;";
        sqlRecognizers = SQLVisitorFactory.get(sql, JdbcConstants.MYSQL);
        for (SQLRecognizer sqlRecognizer : sqlRecognizers) {
            Assertions.assertEquals(sqlRecognizer.getClass().getName(), MySQLUpdateRecognizer.class.getName());
        }
        sqlRecognizers = SQLVisitorFactory.get(sql, JdbcConstants.MARIADB);
        for (SQLRecognizer sqlRecognizer : sqlRecognizers) {
            Assertions.assertEquals(sqlRecognizer.getClass().getName(), MariadbUpdateRecognizer.class.getName());
        }
        sqlRecognizers = SQLVisitorFactory.get(sql, JdbcConstants.POLARDBX);
        for (SQLRecognizer sqlRecognizer : sqlRecognizers) {
            Assertions.assertEquals(sqlRecognizer.getClass().getName(), PolarDBXUpdateRecognizer.class.getName());
        }

        //test for mysql/mariadb/polardb-x update and deleted
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.get("update t set a = a where id =1;update t set a = c where id = 1;delete from t where id =1", JdbcConstants.MYSQL);
        });
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.get("update t set a = a where id =1;update t set a = c where id = 1;delete from t where id =1", JdbcConstants.MARIADB);
        });
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.get("update t set a = a where id =1;update t set a = c where id = 1;delete from t where id =1", JdbcConstants.POLARDBX);
        });

        //test for mysql/mariadb/polardb-x select
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.get("select * from d where id = 1; select * from t where id = 2", JdbcConstants.MYSQL);
        });
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.get("select * from d where id = 1; select * from t where id = 2", JdbcConstants.MARIADB);
        });
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.get("select * from d where id = 1; select * from t where id = 2", JdbcConstants.POLARDBX);
        });

        //test for mysql/mariadb/polardb-x select for update
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.get("select * from t for update; select * from t where id = 2", JdbcConstants.MYSQL);
        });
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.get("select * from t for update; select * from t where id = 2", JdbcConstants.MARIADB);
        });
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.get("select * from t for update; select * from t where id = 2", JdbcConstants.POLARDBX);
        });

        //test for oracle insert
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.get("insert into t(id) values (1);insert into t(id) values (2)", JdbcConstants.ORACLE);
        });

        //test for oracle delete and deleted
        sql = "delete from t where id =1 ; delete from t where id = 2";
        sqlRecognizers = SQLVisitorFactory.get(sql, JdbcConstants.ORACLE);
        for (SQLRecognizer sqlRecognizer : sqlRecognizers) {
            Assertions.assertEquals(sqlRecognizer.getClass().getName(), OracleDeleteRecognizer.class.getName());
        }

        //test for oracle update
        sql = "update t set a = b where id =1 ;update t set a = c where id = 1;";
        sqlRecognizers = SQLVisitorFactory.get(sql, JdbcConstants.ORACLE);
        for (SQLRecognizer sqlRecognizer : sqlRecognizers) {
            Assertions.assertEquals(sqlRecognizer.getClass().getName(), OracleUpdateRecognizer.class.getName());
        }

        //test for oracle select
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.get("select * from b ; select * from t where id = 2", JdbcConstants.ORACLE);
        });

        //test for oracle select for update
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.get("select * from t for update; select * from t where id = 2", JdbcConstants.ORACLE);
        });

        //test for oracle insert and update
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.get("insert into t(id) values (1);update t set a = t;", JdbcConstants.ORACLE);
        });
        //test for oracle insert and deleted
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.get("insert into t(id) values (1);delete from t where id = 1", JdbcConstants.ORACLE);
        });
        //test for sqlserver select
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.get("select * from d where id = 1; select * from t where id = 2", JdbcConstants.SQLSERVER);
        });

        //test for sqlserver select for update
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            SQLVisitorFactory.get("select * from t WITH(UPDLOCK); select * from t where id = 2", JdbcConstants.SQLSERVER);
        });

    }

    @Test
    public void testSqlRecognizerLoading() {
        List<SQLRecognizer> recognizers = SQLVisitorFactory.get("update t1 set name = 'test' where id = '1'", JdbcConstants.MYSQL);
        Assertions.assertNotNull(recognizers);
        Assertions.assertEquals(recognizers.size(), 1);
        SQLRecognizer recognizer = recognizers.get(0);
        Assertions.assertEquals(SQLType.UPDATE, recognizer.getSQLType());
        Assertions.assertEquals("t1", recognizer.getTableName());
        recognizers = SQLVisitorFactory.get("update t1 set name = 'test' where id = '1'", JdbcConstants.MARIADB);
        Assertions.assertNotNull(recognizers);
        Assertions.assertEquals(recognizers.size(), 1);
        recognizer = recognizers.get(0);
        Assertions.assertEquals(SQLType.UPDATE, recognizer.getSQLType());
        Assertions.assertEquals("t1", recognizer.getTableName());
    }
}
