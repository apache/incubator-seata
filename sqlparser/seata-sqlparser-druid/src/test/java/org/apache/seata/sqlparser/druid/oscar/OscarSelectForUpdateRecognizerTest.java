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

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import org.apache.seata.sqlparser.SQLParsingException;
import org.apache.seata.sqlparser.SQLType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

/**
 * The type Oscar select for update recognizer test.
 */
public class OscarSelectForUpdateRecognizerTest extends AbstractOscarRecognizerTest {

    @Test
    public void testGetSqlType() {
        String sql = "select * from t where id = ? for update";
        SQLStatement sqlStatement = getSQLStatement(sql);

        OscarSelectForUpdateRecognizer recognizer = new OscarSelectForUpdateRecognizer(sql, sqlStatement);
        Assertions.assertEquals(recognizer.getSQLType(), SQLType.SELECT_FOR_UPDATE);
    }

    @Test
    public void testGetWhereCondition_0() {
        String sql = "select * from t for update";
        SQLStatement sqlStatement = getSQLStatement(sql);

        OscarSelectForUpdateRecognizer recognizer = new OscarSelectForUpdateRecognizer(sql, sqlStatement);
        String whereCondition = recognizer.getWhereCondition(() -> null, new ArrayList<>());
        Assertions.assertEquals("", whereCondition);
    }

    @Test
    public void testGetWhereCondition_1() {
        String sql = "select * from t for update";
        SQLStatement sqlStatement = getSQLStatement(sql);

        OscarSelectForUpdateRecognizer recognizer = new OscarSelectForUpdateRecognizer(sql, sqlStatement);
        String whereCondition = recognizer.getWhereCondition();

        Assertions.assertEquals("", whereCondition);

        // test for select was null
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "select * from t for update";
            SQLSelectStatement selectAst = (SQLSelectStatement) getSQLStatement(s);
            selectAst.setSelect(null);
            new OscarSelectForUpdateRecognizer(s, selectAst).getWhereCondition();
        });

        // test for query was null
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "select * from t";
            SQLSelectStatement selectAst = (SQLSelectStatement) getSQLStatement(s);
            selectAst.getSelect().setQuery(null);
            new OscarSelectForUpdateRecognizer(s, selectAst).getWhereCondition();
        });
    }

    @Test
    public void testGetTableAlias() {
        String sql = "select * from t where id = ? for update";
        SQLStatement sqlStatement = getSQLStatement(sql);

        OscarSelectForUpdateRecognizer recognizer = new OscarSelectForUpdateRecognizer(sql, sqlStatement);
        Assertions.assertNull(recognizer.getTableAlias());
    }

    @Test
    public void testGetTableName() {
        String sql = "select * from t where id = ? for update";
        SQLStatement sqlStatement = getSQLStatement(sql);

        OscarSelectForUpdateRecognizer recognizer = new OscarSelectForUpdateRecognizer(sql, sqlStatement);
        Assertions.assertEquals(recognizer.getTableName(), "t");
    }
}
