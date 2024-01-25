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
package org.apache.seata.sqlparser.druid.sqlserver;

import com.alibaba.druid.sql.ast.SQLStatement;
import org.apache.seata.sqlparser.druid.AbstractRecognizerTest;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type SqlServer operate holder test.
 *
 */
public class SqlServerOperateRecognizerHolderTest extends AbstractRecognizerTest {

    @Override
    public String getDbType() {
        return JdbcConstants.SQLSERVER;
    }

    @Test
    public void getDeleteRecognizerTest() {
        String sql = "DELETE FROM t1 WHERE id = 'id1'";
        SQLStatement sqlStatement = getSQLStatement(sql);
        Assertions.assertNotNull(new SqlServerOperateRecognizerHolder().getDeleteRecognizer(sql, sqlStatement));
    }

    @Test
    public void getInsertRecognizerTest() {
        String sql = "INSERT INTO t (name) VALUES ('name1')";
        SQLStatement sqlStatement = getSQLStatement(sql);
        Assertions.assertNotNull(new SqlServerOperateRecognizerHolder().getInsertRecognizer(sql, sqlStatement));
    }

    @Test
    public void getUpdateRecognizerTest() {
        String sql = "UPDATE t1 SET name = 'name1' WHERE id = 'id1'";
        SQLStatement sqlStatement = getSQLStatement(sql);
        Assertions.assertNotNull(new SqlServerOperateRecognizerHolder().getUpdateRecognizer(sql, sqlStatement));
    }

    @Test
    public void getSelectForUpdateTest() {
        //test with lock
        String sql = "SELECT name FROM t1 WITH (ROWLOCK, UPDLOCK) WHERE id = 'id1'";
        SQLStatement sqlStatement = getSQLStatement(sql);
        Assertions.assertNotNull(new SqlServerOperateRecognizerHolder().getSelectForUpdateRecognizer(sql, sqlStatement));

        //test with no lock
        sql = "SELECT name FROM t1 WHERE id = 'id1'";
        sqlStatement = getSQLStatement(sql);
        Assertions.assertNull(new SqlServerOperateRecognizerHolder().getSelectForUpdateRecognizer(sql, sqlStatement));
    }
}
