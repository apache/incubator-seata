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
package org.apache.seata.sqlparser.druid.oscar;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.dialect.oscar.ast.stmt.OscarSelectQueryBlock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Test cases for recognizer holder of Oscar
 *
 */
public class OscarOperateRecognizerHolderTest extends AbstractOscarRecognizerTest {
    @Test
    public void getDeleteRecognizerTest() {
        String sql = "DELETE FROM t WHERE id = 1";
        List<SQLStatement> stats = SQLUtils.parseStatements(sql, getDbType());
        SQLStatement sqlStatement = stats.get(0);
        Assertions.assertNotNull(new OscarOperateRecognizerHolder().getDeleteRecognizer(sql, sqlStatement));
    }

    @Test
    public void getInsertRecognizerTest() {
        String sql = "INSERT INTO t (name) VALUES ('test')";
        SQLStatement sqlStatement = getSQLStatement(sql);
        Assertions.assertNotNull(new OscarOperateRecognizerHolder().getInsertRecognizer(sql, sqlStatement));
    }

    @Test
    public void getUpdateRecognizerTest() {
        String sql = "UPDATE t SET name = 'test' WHERE id = 1";
        SQLStatement sqlStatement = getSQLStatement(sql);
        Assertions.assertNotNull(new OscarOperateRecognizerHolder().getUpdateRecognizer(sql, sqlStatement));
    }

    @Test
    public void getSelectForUpdateTest() {
        // not select clause
        String sql = "DELETE FROM t WHERE id = 1";
        SQLStatement sqlStatement = getSQLStatement(sql);
        Assertions.assertNull(new OscarOperateRecognizerHolder().getSelectForUpdateRecognizer(sql, sqlStatement));

        // common select without lock
        sql = "SELECT name FROM t1 WHERE id = 1";
        sqlStatement = getSQLStatement(sql);
        Assertions.assertNull(new OscarOperateRecognizerHolder().getSelectForUpdateRecognizer(sql, sqlStatement));

        // set select is null
        SQLSelectStatement selectStatement = (SQLSelectStatement) getSQLStatement(sql);
        selectStatement.setSelect(null);
        Assertions.assertNull(new OscarOperateRecognizerHolder().getSelectForUpdateRecognizer(sql, sqlStatement));

        // set select query is null
        selectStatement = (SQLSelectStatement) getSQLStatement(sql);
        selectStatement.getSelect().setQuery(null);
        Assertions.assertNull(new OscarOperateRecognizerHolder().getSelectForUpdateRecognizer(sql, sqlStatement));

        // select for update
        sql += " FOR UPDATE";
        selectStatement = (SQLSelectStatement) getSQLStatement(sql);
        Assertions.assertNotNull(new OscarOperateRecognizerHolder().getSelectForUpdateRecognizer(sql, selectStatement));

        // set getForClause is null
        OscarSelectQueryBlock queryBlock =
                (OscarSelectQueryBlock) selectStatement.getSelect().getFirstQueryBlock();
        queryBlock.setForClause(null);
        Assertions.assertNull(new OscarOperateRecognizerHolder().getSelectForUpdateRecognizer(sql, selectStatement));
    }
}
