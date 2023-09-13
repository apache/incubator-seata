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

import io.seata.sqlparser.druid.mariadb.MariadbSelectForUpdateRecognizer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;

import io.seata.sqlparser.ParametersHolder;
import io.seata.sqlparser.SQLParsingException;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.util.JdbcConstants;

/**
 * The type Mariadb select for update recognizer test.
 */
public class MariadbSelectForUpdateRecognizerTest extends AbstractRecognizerTest {

    /**
     * Select for update recognizer test 0.
     */
    @Test
    public void selectForUpdateRecognizerTest_0() {

        String sql = "SELECT name FROM t1 WHERE id = 'id1' FOR UPDATE";

        SQLStatement statement = getSQLStatement(sql);

        MariadbSelectForUpdateRecognizer selectForUpdateRecognizer = new MariadbSelectForUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, selectForUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", selectForUpdateRecognizer.getTableName());
        Assertions.assertEquals("id = 'id1'", selectForUpdateRecognizer.getWhereCondition());
    }

    /**
     * Select for update recognizer test 1.
     */
    @Test
    public void selectForUpdateRecognizerTest_1() {

        String sql = "SELECT name FROM t1 WHERE id = ? FOR UPDATE";

        SQLStatement statement = getSQLStatement(sql);

        MariadbSelectForUpdateRecognizer selectForUpdateRecognizer = new MariadbSelectForUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, selectForUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", selectForUpdateRecognizer.getTableName());

        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = selectForUpdateRecognizer.getWhereCondition(new ParametersHolder() {
            @Override
            public Map<Integer,ArrayList<Object>> getParameters() {
                ArrayList<Object> idParam = new ArrayList<>();
                idParam.add("id1");
                Map result = new HashMap();
                result.put(1, idParam);
                return result;
            }
        }, paramAppenderList);

        Assertions.assertEquals(Collections.singletonList(Collections.singletonList("id1")), paramAppenderList);
        Assertions.assertEquals("id = ?", whereCondition);
    }

    /**
     * Select for update recognizer test 3.
     */
    @Test
    public void selectForUpdateRecognizerTest_3() {

        String sql = "SELECT name1, name2 FROM t1 WHERE id = ? FOR UPDATE";

        SQLStatement statement = getSQLStatement(sql);

        MariadbSelectForUpdateRecognizer selectForUpdateRecognizer = new MariadbSelectForUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, selectForUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", selectForUpdateRecognizer.getTableName());

        // test overflow parameters
        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = selectForUpdateRecognizer.getWhereCondition(new ParametersHolder() {
            @Override
            public Map<Integer,ArrayList<Object>> getParameters() {
                ArrayList<Object> id1Param = new ArrayList<>();
                id1Param.add("id1");
                Map result = new HashMap();
                result.put(1, id1Param);
                return result;
            }
        }, paramAppenderList);

        Assertions.assertEquals(Collections.singletonList(Collections.singletonList("id1")), paramAppenderList);
        Assertions.assertEquals("id = ?", whereCondition);
    }

    /**
     * Select for update recognizer test 4.
     */
    @Test
    public void selectForUpdateRecognizerTest_4() {

        String sql = "SELECT name1, name2 FROM t1 WHERE id IN (?,?) FOR UPDATE";

        SQLStatement statement = getSQLStatement(sql);

        MariadbSelectForUpdateRecognizer selectForUpdateRecognizer = new MariadbSelectForUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, selectForUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", selectForUpdateRecognizer.getTableName());

        // test overflow parameters
        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = selectForUpdateRecognizer.getWhereCondition(new ParametersHolder() {
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
        Assertions.assertEquals("id IN (?, ?)", whereCondition);
    }

    /**
     * Select for update recognizer test 5.
     */
    @Test
    public void selectForUpdateRecognizerTest_5() {

        String sql = "SELECT name1, name2 FROM t1 WHERE id between ? and ? FOR UPDATE";

        SQLStatement statement = getSQLStatement(sql);

        MariadbSelectForUpdateRecognizer selectForUpdateRecognizer = new MariadbSelectForUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, selectForUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", selectForUpdateRecognizer.getTableName());

        // test overflow parameters
        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = selectForUpdateRecognizer.getWhereCondition(new ParametersHolder() {
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
    public void testGetWhereCondition_1() {
        String sql = "select * from t for update";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MARIADB);

        MariadbSelectForUpdateRecognizer recognizer = new MariadbSelectForUpdateRecognizer(sql, asts.get(0));
        String whereCondition = recognizer.getWhereCondition();

        Assertions.assertEquals("", whereCondition);

        //test for select was null
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "select * from t for update";
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(s, JdbcConstants.MARIADB);
            SQLSelectStatement selectAst = (SQLSelectStatement) sqlStatements.get(0);
            selectAst.setSelect(null);
            new MariadbSelectForUpdateRecognizer(s, selectAst).getWhereCondition();
        });

        //test for query was null
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "select * from t";
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(s, JdbcConstants.MARIADB);
            SQLSelectStatement selectAst = (SQLSelectStatement) sqlStatements.get(0);
            selectAst.getSelect().setQuery(null);
            new MariadbSelectForUpdateRecognizer(s, selectAst).getWhereCondition();
        });
    }

    @Test
    public void testGetSqlType() {
        String sql = "select * from t where id = ? for update";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MARIADB);

        MariadbSelectForUpdateRecognizer recognizer = new MariadbSelectForUpdateRecognizer(sql, asts.get(0));
        Assertions.assertEquals(recognizer.getSQLType(), SQLType.SELECT_FOR_UPDATE);
    }

    @Test
    public void testGetTableAlias() {
        String sql = "select * from t where id = ? for update";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MARIADB);

        MariadbSelectForUpdateRecognizer recognizer = new MariadbSelectForUpdateRecognizer(sql, asts.get(0));
        Assertions.assertNull(recognizer.getTableAlias());
    }

    @Override
    public String getDbType() {
        return JdbcConstants.MARIADB;
    }
}
