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
package org.apache.seata.rm.datasource.sql.druid.oscar;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.dialect.oracle.ast.expr.OracleBinaryDoubleExpr;
import org.apache.seata.sqlparser.SQLParsingException;
import org.apache.seata.sqlparser.SQLType;
import org.apache.seata.sqlparser.druid.oscar.OscarInsertRecognizer;
import org.apache.seata.sqlparser.struct.NotPlaceholderExpr;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

/**
 * The type Oscar insert recognizer test.
 */
public class OscarInsertRecognizerTest {

    private static final String DB_TYPE = "oscar";

    @Test
    public void testGetSqlType() {
        String sql = "insert into t(id) values (?)";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OscarInsertRecognizer recognizer = new OscarInsertRecognizer(sql, asts.get(0));
        Assertions.assertEquals(recognizer.getSQLType(), SQLType.INSERT);
    }

    @Test
    public void testGetTableAlias() {
        String sql = "insert into t(id) values (?)";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OscarInsertRecognizer recognizer = new OscarInsertRecognizer(sql, asts.get(0));
        Assertions.assertNull(recognizer.getTableAlias());
    }

    @Test
    public void testGetTableName() {
        String sql = "insert into t(id) values (?)";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OscarInsertRecognizer recognizer = new OscarInsertRecognizer(sql, asts.get(0));
        Assertions.assertEquals(recognizer.getTableName(), "t");
    }

    @Test
    public void testGetInsertColumns() {

        //test for no column
        String sql = "insert into t values (?)";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OscarInsertRecognizer recognizer = new OscarInsertRecognizer(sql, asts.get(0));
        List<String> insertColumns = recognizer.getInsertColumns();
        Assertions.assertNull(insertColumns);

        //test for normal
        sql = "insert into t(a) values (?)";
        asts = SQLUtils.parseStatements(sql, DB_TYPE);

        recognizer = new OscarInsertRecognizer(sql, asts.get(0));
        insertColumns = recognizer.getInsertColumns();
        Assertions.assertEquals(1, insertColumns.size());

        //test for exception
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "insert into t(a) values (?)";
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(s, DB_TYPE);
            SQLInsertStatement sqlInsertStatement = (SQLInsertStatement)sqlStatements.get(0);
            sqlInsertStatement.getColumns().add(new OracleBinaryDoubleExpr());

            OscarInsertRecognizer oscarInsertRecognizer = new OscarInsertRecognizer(s, sqlInsertStatement);
            oscarInsertRecognizer.getInsertColumns();
        });
    }

    @Test
    public void testGetInsertRows() {
        final int pkIndex = 0;
        //test for null value
        String sql = "insert into t(id, no, name, age, time) values (id_seq.nextval, null, 'a', ?, now())";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OscarInsertRecognizer recognizer = new OscarInsertRecognizer(sql, asts.get(0));
        List<List<Object>> insertRows = recognizer.getInsertRows(Collections.singletonList(pkIndex));
        Assertions.assertEquals(1, insertRows.size());

        //test for exception
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "insert into t(a) values (?)";
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(s, DB_TYPE);
            SQLInsertStatement sqlInsertStatement = (SQLInsertStatement)sqlStatements.get(0);
            sqlInsertStatement.getValuesList().get(0).getValues().set(pkIndex, new OracleBinaryDoubleExpr());

            OscarInsertRecognizer oscarInsertRecognizer = new OscarInsertRecognizer(s, sqlInsertStatement);
            oscarInsertRecognizer.getInsertRows(Collections.singletonList(pkIndex));
        });
    }

    @Test
    public void testNotPlaceholder_giveValidPkIndex() {
        String sql = "insert into test(create_time) values(sysdate)";
        List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, DB_TYPE);

        OscarInsertRecognizer oscar = new OscarInsertRecognizer(sql, sqlStatements.get(0));
        List<List<Object>> insertRows = oscar.getInsertRows(Collections.singletonList(-1));
        Assertions.assertTrue(insertRows.get(0).get(0) instanceof NotPlaceholderExpr);
    }
}
