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
package io.seata.sqlparser.druid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.seata.sqlparser.druid.mariadb.MariadbUpdateRecognizer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.expr.MySqlCharExpr;
import com.alibaba.druid.sql.dialect.mysql.ast.expr.MySqlOrderingExpr;

import io.seata.sqlparser.ParametersHolder;
import io.seata.sqlparser.SQLParsingException;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.util.JdbcConstants;

/**
 * The type Mariadb update recognizer test.
 */
public class MariadbUpdateRecognizerTest extends AbstractRecognizerTest {

    /**
     * Update recognizer test 0.
     */
    @Test
    public void updateRecognizerTest_0() {

        String sql = "UPDATE t1 SET name = 'name1' WHERE id = 'id1'";

        SQLStatement statement = getSQLStatement(sql);

        MariadbUpdateRecognizer mariadbUpdateRecognizer = new MariadbUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, mariadbUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", mariadbUpdateRecognizer.getTableName());
        Assertions.assertEquals(1, mariadbUpdateRecognizer.getUpdateColumns().size());
        Assertions.assertEquals("name", mariadbUpdateRecognizer.getUpdateColumns().get(0));
        Assertions.assertEquals("name1", mariadbUpdateRecognizer.getUpdateValues().get(0));
        Assertions.assertEquals("id = 'id1'", mariadbUpdateRecognizer.getWhereCondition());
    }

    /**
     * Update recognizer test 1.
     */
    @Test
    public void updateRecognizerTest_1() {

        String sql = "UPDATE t1 SET name1 = 'name1', name2 = 'name2' WHERE id = 'id1'";

        SQLStatement statement = getSQLStatement(sql);

        MariadbUpdateRecognizer mariadbUpdateRecognizer = new MariadbUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, mariadbUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", mariadbUpdateRecognizer.getTableName());
        Assertions.assertEquals(2, mariadbUpdateRecognizer.getUpdateColumns().size());
        Assertions.assertEquals("name1", mariadbUpdateRecognizer.getUpdateColumns().get(0));
        Assertions.assertEquals("name1", mariadbUpdateRecognizer.getUpdateValues().get(0));
        Assertions.assertEquals("name2", mariadbUpdateRecognizer.getUpdateColumns().get(1));
        Assertions.assertEquals("name2", mariadbUpdateRecognizer.getUpdateValues().get(1));
        Assertions.assertEquals("id = 'id1'", mariadbUpdateRecognizer.getWhereCondition());
    }

    /**
     * Update recognizer test 2.
     */
    @Test
    public void updateRecognizerTest_2() {

        String sql = "UPDATE t1 SET name1 = 'name1', name2 = 'name2' WHERE id = ?";

        SQLStatement statement = getSQLStatement(sql);

        MariadbUpdateRecognizer mariadbUpdateRecognizer = new MariadbUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, mariadbUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", mariadbUpdateRecognizer.getTableName());
        Assertions.assertEquals(2, mariadbUpdateRecognizer.getUpdateColumns().size());
        Assertions.assertEquals("name1", mariadbUpdateRecognizer.getUpdateColumns().get(0));
        Assertions.assertEquals("name1", mariadbUpdateRecognizer.getUpdateValues().get(0));
        Assertions.assertEquals("name2", mariadbUpdateRecognizer.getUpdateColumns().get(1));
        Assertions.assertEquals("name2", mariadbUpdateRecognizer.getUpdateValues().get(1));

        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = mariadbUpdateRecognizer.getWhereCondition(() -> {
            ArrayList<Object> idParam = new ArrayList<>();
            idParam.add("id1");
            Map result = new HashMap();
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

        MariadbUpdateRecognizer mariadbUpdateRecognizer = new MariadbUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, mariadbUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", mariadbUpdateRecognizer.getTableName());
        Assertions.assertEquals(2, mariadbUpdateRecognizer.getUpdateColumns().size());
        Assertions.assertEquals("name1", mariadbUpdateRecognizer.getUpdateColumns().get(0));
        Assertions.assertEquals("name1", mariadbUpdateRecognizer.getUpdateValues().get(0));
        Assertions.assertEquals("name2", mariadbUpdateRecognizer.getUpdateColumns().get(1));
        Assertions.assertEquals("name2", mariadbUpdateRecognizer.getUpdateValues().get(1));

        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = mariadbUpdateRecognizer.getWhereCondition(() -> {
            ArrayList<Object> id1Param = new ArrayList<>();
            id1Param.add("id1");
            ArrayList<Object> id2Param = new ArrayList<>();
            id2Param.add("id2");
            Map result = new HashMap();
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

        MariadbUpdateRecognizer mariadbUpdateRecognizer = new MariadbUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, mariadbUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", mariadbUpdateRecognizer.getTableName());
        Assertions.assertEquals(2, mariadbUpdateRecognizer.getUpdateColumns().size());
        Assertions.assertEquals("name1", mariadbUpdateRecognizer.getUpdateColumns().get(0));
        Assertions.assertEquals("name1", mariadbUpdateRecognizer.getUpdateValues().get(0));
        Assertions.assertEquals("name2", mariadbUpdateRecognizer.getUpdateColumns().get(1));
        Assertions.assertEquals("name2", mariadbUpdateRecognizer.getUpdateValues().get(1));

        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = mariadbUpdateRecognizer.getWhereCondition(new ParametersHolder() {
            @Override
            public Map<Integer,ArrayList<Object>> getParameters() {
                ArrayList<Object> id1Param = new ArrayList<>();
                id1Param.add("id1");
                ArrayList<Object> id2Param = new ArrayList<>();
                id2Param.add("id2");
                ArrayList<Object> name1Param = new ArrayList<>();
                name1Param.add("name");
                Map result = new HashMap();
                result.put(1, id1Param);
                result.put(2, id2Param);
                result.put(3, name1Param);
                return result;
            }
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

        MariadbUpdateRecognizer mariadbUpdateRecognizer = new MariadbUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, mariadbUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", mariadbUpdateRecognizer.getTableName());
        Assertions.assertEquals(2, mariadbUpdateRecognizer.getUpdateColumns().size());
        Assertions.assertEquals("name1", mariadbUpdateRecognizer.getUpdateColumns().get(0));
        Assertions.assertEquals("name1", mariadbUpdateRecognizer.getUpdateValues().get(0));
        Assertions.assertEquals("name2", mariadbUpdateRecognizer.getUpdateColumns().get(1));
        Assertions.assertEquals("name2", mariadbUpdateRecognizer.getUpdateValues().get(1));

        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = mariadbUpdateRecognizer.getWhereCondition(new ParametersHolder() {
            @Override
            public Map<Integer,ArrayList<Object>> getParameters() {
                ArrayList<Object> id1Param = new ArrayList<>();
                id1Param.add("id1");
                ArrayList<Object> id2Param = new ArrayList<>();
                id2Param.add("id2");
                Map result = new HashMap();
                result.put(1, id1Param);
                result.put(2, id2Param);
                return result;
            }
        }, paramAppenderList);

        Assertions.assertEquals(Collections.singletonList(Arrays.asList("id1", "id2")), paramAppenderList);
        Assertions.assertEquals("id BETWEEN ? AND ?", whereCondition);
    }

    @Test
    public void testGetSqlType() {
        String sql = "update t set n = ?";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MARIADB);

        MariadbUpdateRecognizer recognizer = new MariadbUpdateRecognizer(sql, asts.get(0));
        Assertions.assertEquals(recognizer.getSQLType(), SQLType.UPDATE);
    }

    @Test
    public void testGetUpdateColumns() {
        // test with normal
        String sql = "update t set a = ?, b = ?, c = ?";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MARIADB);
        MariadbUpdateRecognizer recognizer = new MariadbUpdateRecognizer(sql, asts.get(0));
        List<String> updateColumns = recognizer.getUpdateColumns();
        Assertions.assertEquals(updateColumns.size(), 3);

        // test with alias
        sql = "update t set a.a = ?, a.b = ?, a.c = ?";
        asts = SQLUtils.parseStatements(sql, JdbcConstants.MARIADB);
        recognizer = new MariadbUpdateRecognizer(sql, asts.get(0));
        updateColumns = recognizer.getUpdateColumns();
        Assertions.assertEquals(updateColumns.size(), 3);

        //test with error
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "update t set a = a";
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(s, JdbcConstants.MARIADB);
            SQLUpdateStatement sqlUpdateStatement = (SQLUpdateStatement) sqlStatements.get(0);
            List<SQLUpdateSetItem> updateSetItems = sqlUpdateStatement.getItems();
            for (SQLUpdateSetItem updateSetItem : updateSetItems) {
                updateSetItem.setColumn(new MySqlCharExpr());
            }
            MariadbUpdateRecognizer mariadbUpdateRecognizer = new MariadbUpdateRecognizer(s, sqlUpdateStatement);
            mariadbUpdateRecognizer.getUpdateColumns();
        });
    }


    @Test
    public void testGetUpdateDatabaseNameColumns() {
        // test with normal
        String sql = "update d.t set d.t.a = ?, d.t.b = ?, d.t.c = ?";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MARIADB);
        MariadbUpdateRecognizer recognizer = new MariadbUpdateRecognizer(sql, asts.get(0));
        List<String> updateColumns = recognizer.getUpdateColumns();
        Assertions.assertEquals(updateColumns.size(), 3);

        // test with alias
        sql = "update t set a.a = ?, a.b = ?, a.c = ?";
        asts = SQLUtils.parseStatements(sql, JdbcConstants.MARIADB);
        recognizer = new MariadbUpdateRecognizer(sql, asts.get(0));
        updateColumns = recognizer.getUpdateColumns();
        Assertions.assertEquals(updateColumns.size(), 3);

        //test with error
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "update t set a = a";
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(s, JdbcConstants.MARIADB);
            SQLUpdateStatement sqlUpdateStatement = (SQLUpdateStatement) sqlStatements.get(0);
            List<SQLUpdateSetItem> updateSetItems = sqlUpdateStatement.getItems();
            for (SQLUpdateSetItem updateSetItem : updateSetItems) {
                updateSetItem.setColumn(new MySqlCharExpr());
            }
            MariadbUpdateRecognizer mariadbUpdateRecognizer = new MariadbUpdateRecognizer(s, sqlUpdateStatement);
            mariadbUpdateRecognizer.getUpdateColumns();
        });
    }

    @Test
    public void testGetUpdateValues() {
        // test with normal
        String sql = "update t set a = ?, b = ?, c = ?";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MARIADB);
        MariadbUpdateRecognizer recognizer = new MariadbUpdateRecognizer(sql, asts.get(0));
        List<Object> updateValues = recognizer.getUpdateValues();
        Assertions.assertEquals(updateValues.size(), 3);

        // test with values
        sql = "update t set a = 1, b = 2, c = 3";
        asts = SQLUtils.parseStatements(sql, JdbcConstants.MARIADB);
        recognizer = new MariadbUpdateRecognizer(sql, asts.get(0));
        updateValues = recognizer.getUpdateValues();
        Assertions.assertEquals(updateValues.size(), 3);

        // test with error
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "update t set a = ?";
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(s, JdbcConstants.MARIADB);
            SQLUpdateStatement sqlUpdateStatement = (SQLUpdateStatement)sqlStatements.get(0);
            List<SQLUpdateSetItem> updateSetItems = sqlUpdateStatement.getItems();
            for (SQLUpdateSetItem updateSetItem : updateSetItems) {
                updateSetItem.setValue(new MySqlOrderingExpr());
            }
            MariadbUpdateRecognizer mariadbUpdateRecognizer = new MariadbUpdateRecognizer(s, sqlUpdateStatement);
            mariadbUpdateRecognizer.getUpdateValues();
        });
    }

    @Test
    public void testGetTableAlias() {
        String sql = "update t set a = ?, b = ?, c = ?";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MARIADB);

        MariadbUpdateRecognizer recognizer = new MariadbUpdateRecognizer(sql, asts.get(0));
        Assertions.assertNull(recognizer.getTableAlias());
    }

    @Test
    public void testUpdateJoinSql() {
        String sql = "update t1 inner join t2 on t1.id = t2.id set name = ?, age = ?";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MARIADB);
        MariadbUpdateRecognizer recognizer = new MariadbUpdateRecognizer(sql, asts.get(0));
        String tableName = recognizer.getTableName();
        Assertions.assertEquals("t1 INNER JOIN t2 ON t1.id = t2.id#t1#t2",tableName);
    }

    @Override
    public String getDbType() {
        return JdbcConstants.MARIADB;
    }

    @Test
    public void testGetUpdateColumns_2() {
        String sql = "update t set `a` = 1, `b` = 2, `c` = 3";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MARIADB);
        MariadbUpdateRecognizer recognizer = new MariadbUpdateRecognizer(sql, asts.get(0));
        List<String> updateColumns = recognizer.getUpdateColumns();
        for (String updateColumn : updateColumns) {
            Assertions.assertTrue(updateColumn.contains("`"));
        }
    }

}
