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
package io.seata.rm.datasource.sql.druid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.expr.MySqlCharExpr;
import com.alibaba.druid.sql.dialect.mysql.ast.expr.MySqlOrderingExpr;
import com.alibaba.druid.util.JdbcConstants;

import io.seata.rm.datasource.ParametersHolder;

import io.seata.rm.datasource.sql.SQLParsingException;
import io.seata.rm.datasource.sql.SQLType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type My sql update recognizer test.
 */
public class MySQLUpdateRecognizerTest extends AbstractMySQLRecognizerTest {

    /**
     * Update recognizer test 0.
     */
    @Test
    public void updateRecognizerTest_0() {

        String sql = "UPDATE t1 SET name = 'name1' WHERE id = 'id1'";

        SQLStatement statement = getSQLStatement(sql);

        MySQLUpdateRecognizer mySQLUpdateRecognizer = new MySQLUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, mySQLUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", mySQLUpdateRecognizer.getTableName());
        Assertions.assertEquals(1, mySQLUpdateRecognizer.getUpdateColumns().size());
        Assertions.assertEquals("name", mySQLUpdateRecognizer.getUpdateColumns().get(0));
        Assertions.assertEquals("name1", mySQLUpdateRecognizer.getUpdateValues().get(0));
        Assertions.assertEquals("id = 'id1'", mySQLUpdateRecognizer.getWhereCondition());
    }

    /**
     * Update recognizer test 1.
     */
    @Test
    public void updateRecognizerTest_1() {

        String sql = "UPDATE t1 SET name1 = 'name1', name2 = 'name2' WHERE id = 'id1'";

        SQLStatement statement = getSQLStatement(sql);

        MySQLUpdateRecognizer mySQLUpdateRecognizer = new MySQLUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, mySQLUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", mySQLUpdateRecognizer.getTableName());
        Assertions.assertEquals(2, mySQLUpdateRecognizer.getUpdateColumns().size());
        Assertions.assertEquals("name1", mySQLUpdateRecognizer.getUpdateColumns().get(0));
        Assertions.assertEquals("name1", mySQLUpdateRecognizer.getUpdateValues().get(0));
        Assertions.assertEquals("name2", mySQLUpdateRecognizer.getUpdateColumns().get(1));
        Assertions.assertEquals("name2", mySQLUpdateRecognizer.getUpdateValues().get(1));
        Assertions.assertEquals("id = 'id1'", mySQLUpdateRecognizer.getWhereCondition());
    }

    /**
     * Update recognizer test 2.
     */
    @Test
    public void updateRecognizerTest_2() {

        String sql = "UPDATE t1 SET name1 = 'name1', name2 = 'name2' WHERE id = ?";

        SQLStatement statement = getSQLStatement(sql);

        MySQLUpdateRecognizer mySQLUpdateRecognizer = new MySQLUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, mySQLUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", mySQLUpdateRecognizer.getTableName());
        Assertions.assertEquals(2, mySQLUpdateRecognizer.getUpdateColumns().size());
        Assertions.assertEquals("name1", mySQLUpdateRecognizer.getUpdateColumns().get(0));
        Assertions.assertEquals("name1", mySQLUpdateRecognizer.getUpdateValues().get(0));
        Assertions.assertEquals("name2", mySQLUpdateRecognizer.getUpdateColumns().get(1));
        Assertions.assertEquals("name2", mySQLUpdateRecognizer.getUpdateValues().get(1));

        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = mySQLUpdateRecognizer.getWhereCondition(new ParametersHolder() {
            @Override
            public ArrayList<Object>[] getParameters() {
                ArrayList<Object> idParam = new ArrayList<>();
                idParam.add("id1");
                return new ArrayList[]{idParam};
            }
        }, paramAppenderList);

        Assertions.assertEquals(Collections.singletonList(Arrays.asList("id1")), paramAppenderList);

        Assertions.assertEquals("id = ?", whereCondition);
    }

    /**
     * Update recognizer test 3.
     */
    @Test
    public void updateRecognizerTest_3() {

        String sql = "UPDATE t1 SET name1 = 'name1', name2 = 'name2' WHERE id in (?, ?)";

        SQLStatement statement = getSQLStatement(sql);

        MySQLUpdateRecognizer mySQLUpdateRecognizer = new MySQLUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, mySQLUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", mySQLUpdateRecognizer.getTableName());
        Assertions.assertEquals(2, mySQLUpdateRecognizer.getUpdateColumns().size());
        Assertions.assertEquals("name1", mySQLUpdateRecognizer.getUpdateColumns().get(0));
        Assertions.assertEquals("name1", mySQLUpdateRecognizer.getUpdateValues().get(0));
        Assertions.assertEquals("name2", mySQLUpdateRecognizer.getUpdateColumns().get(1));
        Assertions.assertEquals("name2", mySQLUpdateRecognizer.getUpdateValues().get(1));

        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = mySQLUpdateRecognizer.getWhereCondition(new ParametersHolder() {
            @Override
            public ArrayList<Object>[] getParameters() {
                ArrayList<Object> id1Param = new ArrayList<>();
                id1Param.add("id1");
                ArrayList<Object> id2Param = new ArrayList<>();
                id2Param.add("id2");
                return new ArrayList[]{id1Param, id2Param};
            }
        }, paramAppenderList);

        Assertions.assertEquals(Arrays.asList(Arrays.asList("id1", "id2")), paramAppenderList);

        Assertions.assertEquals("id IN (?, ?)", whereCondition);
    }

    /**
     * Update recognizer test 4.
     */
    @Test
    public void updateRecognizerTest_4() {

        String sql = "UPDATE t1 SET name1 = 'name1', name2 = 'name2' WHERE id in (?, ?) and name1 = ?";

        SQLStatement statement = getSQLStatement(sql);

        MySQLUpdateRecognizer mySQLUpdateRecognizer = new MySQLUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, mySQLUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", mySQLUpdateRecognizer.getTableName());
        Assertions.assertEquals(2, mySQLUpdateRecognizer.getUpdateColumns().size());
        Assertions.assertEquals("name1", mySQLUpdateRecognizer.getUpdateColumns().get(0));
        Assertions.assertEquals("name1", mySQLUpdateRecognizer.getUpdateValues().get(0));
        Assertions.assertEquals("name2", mySQLUpdateRecognizer.getUpdateColumns().get(1));
        Assertions.assertEquals("name2", mySQLUpdateRecognizer.getUpdateValues().get(1));

        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = mySQLUpdateRecognizer.getWhereCondition(new ParametersHolder() {
            @Override
            public ArrayList<Object>[] getParameters() {
                ArrayList<Object> id1Param = new ArrayList<>();
                id1Param.add("id1");
                ArrayList<Object> id2Param = new ArrayList<>();
                id2Param.add("id2");
                ArrayList<Object> name1Param = new ArrayList<>();
                name1Param.add("name");
                return new ArrayList[]{id1Param, id2Param, name1Param};
            }
        }, paramAppenderList);

        Assertions.assertEquals(Arrays.asList(Arrays.asList("id1", "id2", "name")), paramAppenderList);

        Assertions.assertEquals("id IN (?, ?)\nAND name1 = ?", whereCondition);
    }

    /**
     * Update recognizer test 5.
     */
    @Test
    public void updateRecognizerTest_5() {

        String sql = "UPDATE t1 SET name1 = 'name1', name2 = 'name2' WHERE id between ? and ?";

        SQLStatement statement = getSQLStatement(sql);

        MySQLUpdateRecognizer mySQLUpdateRecognizer = new MySQLUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, mySQLUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", mySQLUpdateRecognizer.getTableName());
        Assertions.assertEquals(2, mySQLUpdateRecognizer.getUpdateColumns().size());
        Assertions.assertEquals("name1", mySQLUpdateRecognizer.getUpdateColumns().get(0));
        Assertions.assertEquals("name1", mySQLUpdateRecognizer.getUpdateValues().get(0));
        Assertions.assertEquals("name2", mySQLUpdateRecognizer.getUpdateColumns().get(1));
        Assertions.assertEquals("name2", mySQLUpdateRecognizer.getUpdateValues().get(1));

        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = mySQLUpdateRecognizer.getWhereCondition(new ParametersHolder() {
            @Override
            public ArrayList<Object>[] getParameters() {
                ArrayList<Object> id1Param = new ArrayList<>();
                id1Param.add("id1");
                ArrayList<Object> id2Param = new ArrayList<>();
                id2Param.add("id2");
                return new ArrayList[]{id1Param, id2Param};
            }
        }, paramAppenderList);

        Assertions.assertEquals(Arrays.asList(Arrays.asList("id1", "id2")), paramAppenderList);
        Assertions.assertEquals("id BETWEEN ? AND ?", whereCondition);
    }

    @Test
    public void testGetSqlType() {
        String sql = "update t set n = ?";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);

        MySQLUpdateRecognizer recognizer = new MySQLUpdateRecognizer(sql, asts.get(0));
        Assertions.assertEquals(recognizer.getSQLType(), SQLType.UPDATE);
    }

    @Test
    public void testGetUpdateColumns() {
        // test with normal
        String sql = "update t set a = ?, b = ?, c = ?";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        MySQLUpdateRecognizer recognizer = new MySQLUpdateRecognizer(sql, asts.get(0));
        List<String> updateColumns = recognizer.getUpdateColumns();
        Assertions.assertEquals(updateColumns.size(), 3);

        // test with alias
        sql = "update t set a.a = ?, a.b = ?, a.c = ?";
        asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        recognizer = new MySQLUpdateRecognizer(sql, asts.get(0));
        updateColumns = recognizer.getUpdateColumns();
        Assertions.assertEquals(updateColumns.size(), 3);

        //test with error
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "update t set a = a";
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(s, JdbcConstants.MYSQL);
            SQLUpdateStatement sqlUpdateStatement = (SQLUpdateStatement) sqlStatements.get(0);
            List<SQLUpdateSetItem> updateSetItems = sqlUpdateStatement.getItems();
            for (SQLUpdateSetItem updateSetItem : updateSetItems) {
                updateSetItem.setColumn(new MySqlCharExpr());
            }
            MySQLUpdateRecognizer oracleUpdateRecognizer = new MySQLUpdateRecognizer(s, sqlUpdateStatement);
            oracleUpdateRecognizer.getUpdateColumns();
        });
    }

    @Test
    public void testGetUpdateValues() {
        // test with normal
        String sql = "update t set a = ?, b = ?, c = ?";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        MySQLUpdateRecognizer recognizer = new MySQLUpdateRecognizer(sql, asts.get(0));
        List<Object> updateValues = recognizer.getUpdateValues();
        Assertions.assertEquals(updateValues.size(), 3);

        // test with values
        sql = "update t set a = 1, b = 2, c = 3";
        asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        recognizer = new MySQLUpdateRecognizer(sql, asts.get(0));
        updateValues = recognizer.getUpdateValues();
        Assertions.assertEquals(updateValues.size(), 3);

        // test with error
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "update t set a = ?";
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(s, JdbcConstants.MYSQL);
            SQLUpdateStatement sqlUpdateStatement = (SQLUpdateStatement)sqlStatements.get(0);
            List<SQLUpdateSetItem> updateSetItems = sqlUpdateStatement.getItems();
            for (SQLUpdateSetItem updateSetItem : updateSetItems) {
                updateSetItem.setValue(new MySqlOrderingExpr());
            }
            MySQLUpdateRecognizer oracleUpdateRecognizer = new MySQLUpdateRecognizer(s, sqlUpdateStatement);
            oracleUpdateRecognizer.getUpdateValues();
        });
    }

    @Test
    public void testGetTableAlias() {
        String sql = "update t set a = ?, b = ?, c = ?";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);

        MySQLUpdateRecognizer recognizer = new MySQLUpdateRecognizer(sql, asts.get(0));
        Assertions.assertNull(recognizer.getTableAlias());
    }
}
