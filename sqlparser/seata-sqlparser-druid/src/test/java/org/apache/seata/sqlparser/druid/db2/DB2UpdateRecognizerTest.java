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
package org.apache.seata.sqlparser.druid.db2;

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
import org.apache.seata.sqlparser.SQLParsingException;
import org.apache.seata.sqlparser.SQLType;
import org.apache.seata.sqlparser.druid.AbstractRecognizerTest;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author GoodBoyCoder
 * @date 2021-10-25
 */
public class DB2UpdateRecognizerTest extends AbstractRecognizerTest {
    /**
     * Update recognizer test 0.
     */
    @Test
    public void updateRecognizerTest_0() {

        String sql = "UPDATE t1 SET name = 'name1' WHERE id = 'id1'";

        SQLStatement statement = getSQLStatement(sql);

        DB2UpdateRecognizer db2UpdateRecognizer = new DB2UpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, db2UpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", db2UpdateRecognizer.getTableName());
        Assertions.assertEquals(1, db2UpdateRecognizer.getUpdateColumns().size());
        Assertions.assertEquals("name", db2UpdateRecognizer.getUpdateColumns().get(0));
        Assertions.assertEquals("name1", db2UpdateRecognizer.getUpdateValues().get(0));
        Assertions.assertEquals("id = 'id1'", db2UpdateRecognizer.getWhereCondition());
    }

    /**
     * Update recognizer test 1.
     */
    @Test
    public void updateRecognizerTest_1() {

        String sql = "UPDATE t1 SET name1 = 'name1', name2 = 'name2' WHERE id = 'id1'";

        SQLStatement statement = getSQLStatement(sql);

        DB2UpdateRecognizer db2UpdateRecognizer = new DB2UpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, db2UpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", db2UpdateRecognizer.getTableName());
        Assertions.assertEquals(2, db2UpdateRecognizer.getUpdateColumns().size());
        Assertions.assertEquals("name1", db2UpdateRecognizer.getUpdateColumns().get(0));
        Assertions.assertEquals("name1", db2UpdateRecognizer.getUpdateValues().get(0));
        Assertions.assertEquals("name2", db2UpdateRecognizer.getUpdateColumns().get(1));
        Assertions.assertEquals("name2", db2UpdateRecognizer.getUpdateValues().get(1));
        Assertions.assertEquals("id = 'id1'", db2UpdateRecognizer.getWhereCondition());
    }

    /**
     * Update recognizer test 2.
     */
    @Test
    public void updateRecognizerTest_2() {

        String sql = "UPDATE t1 SET name1 = 'name1', name2 = 'name2' WHERE id = ?";

        SQLStatement statement = getSQLStatement(sql);

        DB2UpdateRecognizer db2UpdateRecognizer = new DB2UpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, db2UpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", db2UpdateRecognizer.getTableName());
        Assertions.assertEquals(2, db2UpdateRecognizer.getUpdateColumns().size());
        Assertions.assertEquals("name1", db2UpdateRecognizer.getUpdateColumns().get(0));
        Assertions.assertEquals("name1", db2UpdateRecognizer.getUpdateValues().get(0));
        Assertions.assertEquals("name2", db2UpdateRecognizer.getUpdateColumns().get(1));
        Assertions.assertEquals("name2", db2UpdateRecognizer.getUpdateValues().get(1));

        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = db2UpdateRecognizer.getWhereCondition(() -> {
            ArrayList<Object> idParam = new ArrayList<>();
            idParam.add("id1");
            Map<Integer, ArrayList<Object>> result = new HashMap<>();
            result.put(1, idParam);
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

        DB2UpdateRecognizer db2UpdateRecognizer = new DB2UpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, db2UpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", db2UpdateRecognizer.getTableName());
        Assertions.assertEquals(2, db2UpdateRecognizer.getUpdateColumns().size());
        Assertions.assertEquals("name1", db2UpdateRecognizer.getUpdateColumns().get(0));
        Assertions.assertEquals("name1", db2UpdateRecognizer.getUpdateValues().get(0));
        Assertions.assertEquals("name2", db2UpdateRecognizer.getUpdateColumns().get(1));
        Assertions.assertEquals("name2", db2UpdateRecognizer.getUpdateValues().get(1));

        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = db2UpdateRecognizer.getWhereCondition(() -> {
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

        String sql = "UPDATE t1 SET name1 = 'name1', name2 = 'name2' WHERE id in (?, ?) and name1 = ?";

        SQLStatement statement = getSQLStatement(sql);

        DB2UpdateRecognizer db2UpdateRecognizer = new DB2UpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, db2UpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", db2UpdateRecognizer.getTableName());
        Assertions.assertEquals(2, db2UpdateRecognizer.getUpdateColumns().size());
        Assertions.assertEquals("name1", db2UpdateRecognizer.getUpdateColumns().get(0));
        Assertions.assertEquals("name1", db2UpdateRecognizer.getUpdateValues().get(0));
        Assertions.assertEquals("name2", db2UpdateRecognizer.getUpdateColumns().get(1));
        Assertions.assertEquals("name2", db2UpdateRecognizer.getUpdateValues().get(1));

        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = db2UpdateRecognizer.getWhereCondition(() -> {
            ArrayList<Object> id1Param = new ArrayList<>();
            id1Param.add("id1");
            ArrayList<Object> id2Param = new ArrayList<>();
            id2Param.add("id2");
            ArrayList<Object> name1Param = new ArrayList<>();
            name1Param.add("name");
            Map<Integer, ArrayList<Object>> result = new HashMap<>();
            result.put(1, id1Param);
            result.put(2, id2Param);
            result.put(3, name1Param);
            return result;
        }, paramAppenderList);

        Assertions.assertEquals(Collections.singletonList(Arrays.asList("id1", "id2", "name")), paramAppenderList);

        Assertions.assertEquals("id IN (?, ?)\nAND name1 = ?", whereCondition);
    }

    /**
     * Update recognizer test 5.
     */
    @Test
    public void updateRecognizerTest_5() {

        String sql = "UPDATE t1 SET name1 = 'name1', name2 = 'name2' WHERE id between ? and ?";

        SQLStatement statement = getSQLStatement(sql);

        DB2UpdateRecognizer db2UpdateRecognizer = new DB2UpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, db2UpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", db2UpdateRecognizer.getTableName());
        Assertions.assertEquals(2, db2UpdateRecognizer.getUpdateColumns().size());
        Assertions.assertEquals("name1", db2UpdateRecognizer.getUpdateColumns().get(0));
        Assertions.assertEquals("name1", db2UpdateRecognizer.getUpdateValues().get(0));
        Assertions.assertEquals("name2", db2UpdateRecognizer.getUpdateColumns().get(1));
        Assertions.assertEquals("name2", db2UpdateRecognizer.getUpdateValues().get(1));

        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = db2UpdateRecognizer.getWhereCondition(() -> {
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
        SQLStatement statement = getSQLStatement(sql);

        DB2UpdateRecognizer recognizer = new DB2UpdateRecognizer(sql, statement);
        Assertions.assertEquals(recognizer.getSQLType(), SQLType.UPDATE);
    }

    @Test
    public void testGetUpdateColumns() {
        // test with normal
        String sql = "update t set a = ?, b = ?, c = ?";
        SQLStatement statement = getSQLStatement(sql);

        DB2UpdateRecognizer recognizer = new DB2UpdateRecognizer(sql, statement);
        List<String> updateColumns = recognizer.getUpdateColumns();
        Assertions.assertEquals(updateColumns.size(), 3);

        // test with alias
        sql = "update t set a.a = ?, a.b = ?, a.c = ?";
        statement = getSQLStatement(sql);
        recognizer = new DB2UpdateRecognizer(sql, statement);
        updateColumns = recognizer.getUpdateColumns();
        Assertions.assertEquals(updateColumns.size(), 3);

        //test with error
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "update t set a = a";
            SQLUpdateStatement sqlUpdateStatement = (SQLUpdateStatement) getSQLStatement(s);
            List<SQLUpdateSetItem> updateSetItems = sqlUpdateStatement.getItems();
            for (SQLUpdateSetItem updateSetItem : updateSetItems) {
                updateSetItem.setColumn(new MySqlCharExpr());
            }
            DB2UpdateRecognizer db2UpdateRecognizer = new DB2UpdateRecognizer(s, sqlUpdateStatement);
            db2UpdateRecognizer.getUpdateColumns();
        });
    }


    @Test
    public void testGetUpdateDatabaseNameColumns() {
        // test with normal
        String sql = "update d.t set d.t.a = ?, d.t.b = ?, d.t.c = ?";
        SQLStatement statement = getSQLStatement(sql);
        DB2UpdateRecognizer recognizer = new DB2UpdateRecognizer(sql, statement);
        List<String> updateColumns = recognizer.getUpdateColumns();
        Assertions.assertEquals(updateColumns.size(), 3);

        // test with alias
        sql = "update t set a.a = ?, a.b = ?, a.c = ?";
        statement = getSQLStatement(sql);
        recognizer = new DB2UpdateRecognizer(sql, statement);
        updateColumns = recognizer.getUpdateColumns();
        Assertions.assertEquals(updateColumns.size(), 3);

        //test with error
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "update t set a = a";
            SQLUpdateStatement sqlUpdateStatement = (SQLUpdateStatement) getSQLStatement(s);
            List<SQLUpdateSetItem> updateSetItems = sqlUpdateStatement.getItems();
            for (SQLUpdateSetItem updateSetItem : updateSetItems) {
                updateSetItem.setColumn(new MySqlCharExpr());
            }
            DB2UpdateRecognizer db2UpdateRecognizer = new DB2UpdateRecognizer(s, sqlUpdateStatement);
            db2UpdateRecognizer.getUpdateColumns();
        });
    }

    @Test
    public void testGetUpdateValues() {
        // test with normal
        String sql = "update t set a = ?, b = ?, c = ?";
        SQLStatement ast = getSQLStatement(sql);
        DB2UpdateRecognizer recognizer = new DB2UpdateRecognizer(sql, ast);
        List<Object> updateValues = recognizer.getUpdateValues();
        Assertions.assertEquals(updateValues.size(), 3);

        // test with values
        sql = "update t set a = 1, b = 2, c = 3";
        ast = getSQLStatement(sql);
        recognizer = new DB2UpdateRecognizer(sql, ast);
        updateValues = recognizer.getUpdateValues();
        Assertions.assertEquals(updateValues.size(), 3);

        // test with error
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "update t set a = ?";
            SQLUpdateStatement sqlUpdateStatement = (SQLUpdateStatement) getSQLStatement(s);
            List<SQLUpdateSetItem> updateSetItems = sqlUpdateStatement.getItems();
            for (SQLUpdateSetItem updateSetItem : updateSetItems) {
                updateSetItem.setValue(new MySqlOrderingExpr());
            }
            DB2UpdateRecognizer db2UpdateRecognizer = new DB2UpdateRecognizer(s, sqlUpdateStatement);
            db2UpdateRecognizer.getUpdateValues();
        });
    }

    @Test
    public void testGetTableAlias() {
        String sql = "update t set a = ?, b = ?, c = ?";
        SQLStatement asts = getSQLStatement(sql);

        DB2UpdateRecognizer recognizer = new DB2UpdateRecognizer(sql, asts);
        Assertions.assertNull(recognizer.getTableAlias());
    }

    @Override
    public String getDbType() {
        return JdbcConstants.DB2;
    }
}
