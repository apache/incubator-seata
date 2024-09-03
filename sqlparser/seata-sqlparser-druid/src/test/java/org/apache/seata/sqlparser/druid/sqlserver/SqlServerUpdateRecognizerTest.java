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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.expr.MySqlCharExpr;
import com.alibaba.druid.sql.dialect.mysql.ast.expr.MySqlOrderingExpr;
import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.sqlparser.SQLParsingException;
import org.apache.seata.sqlparser.SQLType;
import org.apache.seata.sqlparser.druid.AbstractRecognizerTest;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type SqlServer update recognizer test.
 *
 */
public class SqlServerUpdateRecognizerTest extends AbstractRecognizerTest {
    @Override
    public String getDbType() {
        return JdbcConstants.SQLSERVER;
    }

    /**
     * Update recognizer test 0.(test with constant)
     */
    @Test
    public void updateRecognizerTest_0() {

        String sql = "UPDATE t1 SET name = 'name1' WHERE id = 'id1'";

        SQLStatement statement = getSQLStatement(sql);

        SqlServerUpdateRecognizer sqlServerUpdateRecognizer = new SqlServerUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, sqlServerUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals(SQLType.UPDATE, sqlServerUpdateRecognizer.getSQLType());
        Assertions.assertEquals("t1", sqlServerUpdateRecognizer.getTableName());
        Assertions.assertEquals(1, sqlServerUpdateRecognizer.getUpdateColumns().size());
        Assertions.assertEquals("name", sqlServerUpdateRecognizer.getUpdateColumns().get(0));
        Assertions.assertEquals("name1", sqlServerUpdateRecognizer.getUpdateValues().get(0));
        Assertions.assertEquals("id = 'id1'", sqlServerUpdateRecognizer.getWhereCondition());
    }

    /**
     * Update recognizer test 1.(test with multi columns)
     */
    @Test
    public void updateRecognizerTest_1() {

        String sql = "UPDATE t1 SET name1 = 'name1', name2 = 'name2' WHERE id = 'id1'";

        SQLStatement statement = getSQLStatement(sql);

        SqlServerUpdateRecognizer sqlServerUpdateRecognizer = new SqlServerUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, sqlServerUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals(SQLType.UPDATE, sqlServerUpdateRecognizer.getSQLType());
        Assertions.assertEquals("t1", sqlServerUpdateRecognizer.getTableName());
        Assertions.assertEquals(2, sqlServerUpdateRecognizer.getUpdateColumns().size());
        Assertions.assertEquals("name1", sqlServerUpdateRecognizer.getUpdateColumns().get(0));
        Assertions.assertEquals("name1", sqlServerUpdateRecognizer.getUpdateValues().get(0));
        Assertions.assertEquals("name2", sqlServerUpdateRecognizer.getUpdateColumns().get(1));
        Assertions.assertEquals("name2", sqlServerUpdateRecognizer.getUpdateValues().get(1));
        Assertions.assertEquals("id = 'id1'", sqlServerUpdateRecognizer.getWhereCondition());
    }

    /**
     * Update recognizer test 2.(test with placeholder)
     */
    @Test
    public void updateRecognizerTest_2() {

        String sql = "UPDATE t1 SET name1 = ?, name2 = ? WHERE id = ?";

        SQLStatement statement = getSQLStatement(sql);

        SqlServerUpdateRecognizer sqlServerUpdateRecognizer = new SqlServerUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, sqlServerUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", sqlServerUpdateRecognizer.getTableName());
        Assertions.assertEquals(2, sqlServerUpdateRecognizer.getUpdateColumns().size());
        Assertions.assertEquals("name1", sqlServerUpdateRecognizer.getUpdateColumns().get(0));
        Assertions.assertEquals("?", sqlServerUpdateRecognizer.getUpdateValues().get(0));
        Assertions.assertEquals("name2", sqlServerUpdateRecognizer.getUpdateColumns().get(1));
        Assertions.assertEquals("?", sqlServerUpdateRecognizer.getUpdateValues().get(1));

        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = sqlServerUpdateRecognizer.getWhereCondition(() -> {
            ArrayList<Object> idParam = new ArrayList<>();
            idParam.add("id1");
            Map<Integer, ArrayList<Object>> result = new HashMap<>();
            result.put(3, idParam);
            return result;
        }, paramAppenderList);

        Assertions.assertEquals(Collections.singletonList(Collections.singletonList("id1")), paramAppenderList);

        Assertions.assertEquals("id = ?", whereCondition);
    }

    /**
     * Update recognizer test 3.
     */
    @Test
    public void updateRecognizerTest_3() {

        String sql = "UPDATE t1 SET name1 = 'name1', name2 = 'name2' WHERE id in (?, ?)";

        SQLStatement statement = getSQLStatement(sql);

        SqlServerUpdateRecognizer sqlServerUpdateRecognizer = new SqlServerUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, sqlServerUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", sqlServerUpdateRecognizer.getTableName());
        Assertions.assertEquals(2, sqlServerUpdateRecognizer.getUpdateColumns().size());
        Assertions.assertEquals("name1", sqlServerUpdateRecognizer.getUpdateColumns().get(0));
        Assertions.assertEquals("name1", sqlServerUpdateRecognizer.getUpdateValues().get(0));
        Assertions.assertEquals("name2", sqlServerUpdateRecognizer.getUpdateColumns().get(1));
        Assertions.assertEquals("name2", sqlServerUpdateRecognizer.getUpdateValues().get(1));

        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = sqlServerUpdateRecognizer.getWhereCondition(() -> {
            ArrayList<Object> id1Param = new ArrayList<>();
            id1Param.add("id1");
            ArrayList<Object> id2Param = new ArrayList<>();
            id2Param.add("id2");
            Map<Integer, ArrayList<Object>> result = new HashMap<>();
            result.put(1, id1Param);
            result.put(2, id2Param);
            return result;
        }, paramAppenderList);

        Assertions.assertEquals(Collections.singletonList(Arrays.asList("id1", "id2")), paramAppenderList);

        Assertions.assertEquals("id IN (?, ?)", whereCondition);
    }

    /**
     * Update recognizer test 4.
     */
    @Test
    public void updateRecognizerTest_4() {

        String sql = "UPDATE t1 SET name1 = 'name1', name2 = 'name2' WHERE id between ? and ?";

        SQLStatement statement = getSQLStatement(sql);

        SqlServerUpdateRecognizer sqlServerUpdateRecognizer = new SqlServerUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, sqlServerUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", sqlServerUpdateRecognizer.getTableName());
        Assertions.assertEquals(2, sqlServerUpdateRecognizer.getUpdateColumns().size());
        Assertions.assertEquals("name1", sqlServerUpdateRecognizer.getUpdateColumns().get(0));
        Assertions.assertEquals("name1", sqlServerUpdateRecognizer.getUpdateValues().get(0));
        Assertions.assertEquals("name2", sqlServerUpdateRecognizer.getUpdateColumns().get(1));
        Assertions.assertEquals("name2", sqlServerUpdateRecognizer.getUpdateValues().get(1));

        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = sqlServerUpdateRecognizer.getWhereCondition(() -> {
            ArrayList<Object> id1Param = new ArrayList<>();
            id1Param.add("id1");
            ArrayList<Object> id2Param = new ArrayList<>();
            id2Param.add("id2");
            Map<Integer, ArrayList<Object>> result = new HashMap<>();
            result.put(1, id1Param);
            result.put(2, id2Param);
            return result;
        }, paramAppenderList);

        Assertions.assertEquals(Collections.singletonList(Arrays.asList("id1", "id2")), paramAppenderList);
        Assertions.assertEquals("id BETWEEN ? AND ?", whereCondition);
    }

    @Test
    public void testGetSqlType() {
        String sql = "update t set n = ?";
        SQLStatement sqlStatement = getSQLStatement(sql);

        SqlServerUpdateRecognizer sqlServerUpdateRecognizer = new SqlServerUpdateRecognizer(sql, sqlStatement);
        Assertions.assertEquals(sqlServerUpdateRecognizer.getSQLType(), SQLType.UPDATE);
    }

    @Test
    public void testGetUpdateColumns() {
        // test with normal
        String sql = "update t set a = ?, b = ?, c = ?";
        SQLStatement sqlStatement = getSQLStatement(sql);

        SqlServerUpdateRecognizer recognizer = new SqlServerUpdateRecognizer(sql, sqlStatement);
        List<String> updateColumns = recognizer.getUpdateColumns();
        Assertions.assertEquals(updateColumns.size(), 3);

        // test with alias
        sql = "update t set a.a = ?, a.b = ?, a.c = ?";
        sqlStatement = getSQLStatement(sql);
        recognizer = new SqlServerUpdateRecognizer(sql, sqlStatement);
        updateColumns = recognizer.getUpdateColumns();
        Assertions.assertEquals(updateColumns.size(), 3);

        sql = "update d.t set d.t.a = ?, d.t.b = ?, d.t.c = ?";
        sqlStatement = getSQLStatement(sql);
        recognizer = new SqlServerUpdateRecognizer(sql, sqlStatement);
        updateColumns = recognizer.getUpdateColumns();
        Assertions.assertEquals(updateColumns.size(), 3);

        //test with error
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "update t set a = a";
            SQLStatement sqlStatement1 = getSQLStatement(s);
            SQLUpdateStatement sqlUpdateStatement = (SQLUpdateStatement) sqlStatement1;
            List<SQLUpdateSetItem> updateSetItems = sqlUpdateStatement.getItems();
            for (SQLUpdateSetItem updateSetItem : updateSetItems) {
                updateSetItem.setColumn(new MySqlCharExpr());
            }
            SqlServerUpdateRecognizer sqlServerUpdateRecognizer = new SqlServerUpdateRecognizer(s, sqlUpdateStatement);
            sqlServerUpdateRecognizer.getUpdateColumns();
        });
    }

    @Test
    public void testGetUpdateValues() {
        // test with normal
        String sql = "update t set a = ?, b = ?, c = ?";
        SQLStatement sqlStatement = getSQLStatement(sql);

        SqlServerUpdateRecognizer recognizer = new SqlServerUpdateRecognizer(sql, sqlStatement);
        List<Object> updateValues = recognizer.getUpdateValues();
        Assertions.assertEquals(updateValues.size(), 3);

        // test with values
        sql = "update t set a = 1, b = 2, c = 3";
        sqlStatement = getSQLStatement(sql);

        recognizer = new SqlServerUpdateRecognizer(sql, sqlStatement);
        updateValues = recognizer.getUpdateValues();
        Assertions.assertEquals(updateValues.size(), 3);

        // test with default、method、NULL
        sql = "update t set a = default, b = now(), c = null";
        sqlStatement = getSQLStatement(sql);

        recognizer = new SqlServerUpdateRecognizer(sql, sqlStatement);
        updateValues = recognizer.getUpdateValues();
        Assertions.assertEquals(updateValues.size(), 3);

        // test with sequence
        sql = "update t set a = next value for t1.id";
        sqlStatement = getSQLStatement(sql);

        recognizer = new SqlServerUpdateRecognizer(sql, sqlStatement);
        updateValues = recognizer.getUpdateValues();
        Assertions.assertEquals(updateValues.size(), 1);

        //test with top
        Assertions.assertThrows(NotSupportYetException.class, () -> {
            String s = "update top(1) t set a = ?";
            SQLStatement sqlStatement1 = getSQLStatement(s);

            SqlServerUpdateRecognizer sqlServerUpdateRecognizer = new SqlServerUpdateRecognizer(s, sqlStatement1);
            sqlServerUpdateRecognizer.getUpdateValues();
        });

        // test with error
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "update t set a = ?";
            SQLStatement sqlStatement1 = getSQLStatement(s);
            SQLUpdateStatement sqlUpdateStatement = (SQLUpdateStatement) sqlStatement1;
            List<SQLUpdateSetItem> updateSetItems = sqlUpdateStatement.getItems();
            for (SQLUpdateSetItem updateSetItem : updateSetItems) {
                updateSetItem.setValue(new MySqlOrderingExpr());
            }
            SqlServerUpdateRecognizer sqlServerUpdateRecognizer = new SqlServerUpdateRecognizer(s, sqlUpdateStatement);
            sqlServerUpdateRecognizer.getUpdateValues();
        });
    }

    @Test
    public void testGetTableAlias() {
        String sql = "update t set a = ?, b = ?, c = ?";
        SQLStatement sqlStatement = getSQLStatement(sql);

        SqlServerUpdateRecognizer recognizer = new SqlServerUpdateRecognizer(sql, sqlStatement);
        Assertions.assertNull(recognizer.getTableAlias());

        sql = "update t t1 set a = ?";
        sqlStatement = getSQLStatement(sql);
        recognizer = new SqlServerUpdateRecognizer(sql, sqlStatement);
        Assertions.assertEquals("t1", recognizer.getTableAlias());
    }

    @Test
    public void testGetTableName() {
        String sql = "update t set a = ?, b = ?, c = ?";
        SQLStatement sqlStatement = getSQLStatement(sql);

        SqlServerUpdateRecognizer recognizer = new SqlServerUpdateRecognizer(sql, sqlStatement);
        Assertions.assertEquals("t", recognizer.getTableName());

        //test for alias
        sql = "update t t1 set a = ?";
        sqlStatement = getSQLStatement(sql);
        recognizer = new SqlServerUpdateRecognizer(sql, sqlStatement);
        Assertions.assertEquals("t", recognizer.getTableName());
    }
}
