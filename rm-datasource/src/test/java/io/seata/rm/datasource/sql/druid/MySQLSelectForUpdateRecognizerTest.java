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
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.util.JdbcConstants;

import io.seata.rm.datasource.ParametersHolder;

import io.seata.rm.datasource.sql.SQLParsingException;
import io.seata.rm.datasource.sql.SQLType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type My sql select for update recognizer test.
 */
public class MySQLSelectForUpdateRecognizerTest extends AbstractMySQLRecognizerTest {

    /**
     * Select for update recognizer test 0.
     */
    @Test
    public void selectForUpdateRecognizerTest_0() {

        String sql = "SELECT name FROM t1 WHERE id = 'id1' FOR UPDATE";

        SQLStatement statement = getSQLStatement(sql);

        MySQLSelectForUpdateRecognizer mySQLUpdateRecognizer = new MySQLSelectForUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, mySQLUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", mySQLUpdateRecognizer.getTableName());
        Assertions.assertEquals("id = 'id1'", mySQLUpdateRecognizer.getWhereCondition());
    }

    /**
     * Select for update recognizer test 1.
     */
    @Test
    public void selectForUpdateRecognizerTest_1() {

        String sql = "SELECT name FROM t1 WHERE id = ? FOR UPDATE";

        SQLStatement statement = getSQLStatement(sql);

        MySQLSelectForUpdateRecognizer mySQLUpdateRecognizer = new MySQLSelectForUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, mySQLUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", mySQLUpdateRecognizer.getTableName());

        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = mySQLUpdateRecognizer.getWhereCondition(new ParametersHolder() {
            @Override
            public ArrayList<Object>[] getParameters() {
                ArrayList<Object> idParam = new ArrayList<>();
                idParam.add("id1");
                return new ArrayList[] {idParam};
            }
        }, paramAppenderList);

        Assertions.assertEquals(Collections.singletonList(Arrays.asList("id1")), paramAppenderList);
        Assertions.assertEquals("id = ?", whereCondition);
    }

    /**
     * Select for update recognizer test 3.
     */
    @Test
    public void selectForUpdateRecognizerTest_3() {

        String sql = "SELECT name1, name2 FROM t1 WHERE id = ? FOR UPDATE";

        SQLStatement statement = getSQLStatement(sql);

        MySQLSelectForUpdateRecognizer mySQLUpdateRecognizer = new MySQLSelectForUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, mySQLUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", mySQLUpdateRecognizer.getTableName());

        // test overflow parameters
        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = mySQLUpdateRecognizer.getWhereCondition(new ParametersHolder() {
            @Override
            public ArrayList<Object>[] getParameters() {
                ArrayList<Object> id1Param = new ArrayList<>();
                id1Param.add("id1");
                return new ArrayList[] {id1Param};
            }
        }, paramAppenderList);

        Assertions.assertEquals(Collections.singletonList(Arrays.asList("id1")), paramAppenderList);
        Assertions.assertEquals("id = ?", whereCondition);
    }

    /**
     * Select for update recognizer test 4.
     */
    @Test
    public void selectForUpdateRecognizerTest_4() {

        String sql = "SELECT name1, name2 FROM t1 WHERE id IN (?,?) FOR UPDATE";

        SQLStatement statement = getSQLStatement(sql);

        MySQLSelectForUpdateRecognizer mySQLUpdateRecognizer = new MySQLSelectForUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, mySQLUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", mySQLUpdateRecognizer.getTableName());

        // test overflow parameters
        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = mySQLUpdateRecognizer.getWhereCondition(new ParametersHolder() {
            @Override
            public ArrayList<Object>[] getParameters() {
                ArrayList<Object> id1Param = new ArrayList<>();
                id1Param.add("id1");
                ArrayList<Object> id2Param = new ArrayList<>();
                id2Param.add("id2");
                return new ArrayList[] {id1Param, id2Param};
            }
        }, paramAppenderList);

        Assertions.assertEquals(Arrays.asList(Arrays.asList("id1", "id2")), paramAppenderList);
        Assertions.assertEquals("id IN (?, ?)", whereCondition);
    }

    /**
     * Select for update recognizer test 5.
     */
    @Test
    public void selectForUpdateRecognizerTest_5() {

        String sql = "SELECT name1, name2 FROM t1 WHERE id between ? and ? FOR UPDATE";

        SQLStatement statement = getSQLStatement(sql);

        MySQLSelectForUpdateRecognizer mySQLUpdateRecognizer = new MySQLSelectForUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, mySQLUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", mySQLUpdateRecognizer.getTableName());

        // test overflow parameters
        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = mySQLUpdateRecognizer.getWhereCondition(new ParametersHolder() {
            @Override
            public ArrayList<Object>[] getParameters() {
                ArrayList<Object> id1Param = new ArrayList<>();
                id1Param.add("id1");
                ArrayList<Object> id2Param = new ArrayList<>();
                id2Param.add("id2");
                return new ArrayList[] {id1Param, id2Param};
            }
        }, paramAppenderList);

        Assertions.assertEquals(Arrays.asList(Arrays.asList("id1", "id2")), paramAppenderList);
        Assertions.assertEquals("id BETWEEN ? AND ?", whereCondition);
    }

    @Test
    public void testGetWhereCondition_1() {
        String sql = "select * from t for update";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);

        MySQLSelectForUpdateRecognizer recognizer = new MySQLSelectForUpdateRecognizer(sql, asts.get(0));
        String whereCondition = recognizer.getWhereCondition();

        Assertions.assertEquals("", whereCondition);

        //test for select was null
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "select * from t for update";
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(s, JdbcConstants.MYSQL);
            SQLSelectStatement selectAst = (SQLSelectStatement) sqlStatements.get(0);
            selectAst.setSelect(null);
            new MySQLSelectForUpdateRecognizer(s, selectAst).getWhereCondition();
        });

        //test for query was null
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "select * from t";
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(s, JdbcConstants.MYSQL);
            SQLSelectStatement selectAst = (SQLSelectStatement) sqlStatements.get(0);
            selectAst.getSelect().setQuery(null);
            new MySQLSelectForUpdateRecognizer(s, selectAst).getWhereCondition();
        });
    }

    @Test
    public void testGetSqlType() {
        String sql = "select * from t where id = ? for update";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);

        MySQLSelectForUpdateRecognizer recognizer = new MySQLSelectForUpdateRecognizer(sql, asts.get(0));
        Assertions.assertEquals(recognizer.getSQLType(), SQLType.SELECT_FOR_UPDATE);
    }

    @Test
    public void testGetTableAlias() {
        String sql = "select * from t where id = ? for update";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);

        MySQLSelectForUpdateRecognizer recognizer = new MySQLSelectForUpdateRecognizer(sql, asts.get(0));
        Assertions.assertNull(recognizer.getTableAlias());
    }
}
