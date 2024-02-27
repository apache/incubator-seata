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
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
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
public class DB2SelectForUpdateRecognizerTest extends AbstractRecognizerTest {
    /**
     * Select for update recognizer test 0.
     */
    @Test
    public void selectForUpdateRecognizerTest_0() {

        String sql = "SELECT name FROM t1 WHERE id = 'id1' FOR UPDATE";

        SQLStatement statement = getSQLStatement(sql);

        DB2SelectForUpdateRecognizer db2SelectForUpdateRecognizer = new DB2SelectForUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, db2SelectForUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", db2SelectForUpdateRecognizer.getTableName());
        Assertions.assertEquals("id = 'id1'", db2SelectForUpdateRecognizer.getWhereCondition());
    }

    /**
     * Select for update recognizer test 1.
     */
    @Test
    public void selectForUpdateRecognizerTest_1() {

        String sql = "SELECT name FROM t1 WHERE id = ? FOR UPDATE";

        SQLStatement statement = getSQLStatement(sql);

        DB2SelectForUpdateRecognizer db2SelectForUpdateRecognizer = new DB2SelectForUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, db2SelectForUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", db2SelectForUpdateRecognizer.getTableName());

        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = db2SelectForUpdateRecognizer.getWhereCondition(() -> {
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
     * Select for update recognizer test 3.
     */
    @Test
    public void selectForUpdateRecognizerTest_3() {

        String sql = "SELECT name1, name2 FROM t1 WHERE id = ? FOR UPDATE";

        SQLStatement statement = getSQLStatement(sql);

        DB2SelectForUpdateRecognizer db2SelectForUpdateRecognizer = new DB2SelectForUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, db2SelectForUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", db2SelectForUpdateRecognizer.getTableName());

        // test overflow parameters
        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = db2SelectForUpdateRecognizer.getWhereCondition(() -> {
            ArrayList<Object> id1Param = new ArrayList<>();
            id1Param.add("id1");
            Map<Integer, ArrayList<Object>> result = new HashMap<>();
            result.put(1, id1Param);
            return result;
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

        DB2SelectForUpdateRecognizer db2SelectForUpdateRecognizer = new DB2SelectForUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, db2SelectForUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", db2SelectForUpdateRecognizer.getTableName());

        // test overflow parameters
        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = db2SelectForUpdateRecognizer.getWhereCondition(() -> {
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
     * Select for update recognizer test 5.
     */
    @Test
    public void selectForUpdateRecognizerTest_5() {

        String sql = "SELECT name1, name2 FROM t1 WHERE id between ? and ? FOR UPDATE";

        SQLStatement statement = getSQLStatement(sql);

        DB2SelectForUpdateRecognizer db2SelectForUpdateRecognizer = new DB2SelectForUpdateRecognizer(sql, statement);

        Assertions.assertEquals(sql, db2SelectForUpdateRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", db2SelectForUpdateRecognizer.getTableName());

        // test overflow parameters
        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = db2SelectForUpdateRecognizer.getWhereCondition(() -> {
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
    public void testGetWhereCondition_1() {
        String sql = "select * from t for update";
        SQLStatement ast = getSQLStatement(sql);

        DB2SelectForUpdateRecognizer recognizer = new DB2SelectForUpdateRecognizer(sql, ast);
        String whereCondition = recognizer.getWhereCondition();

        Assertions.assertEquals("", whereCondition);

        //test for select was null
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "select * from t for update";
            SQLSelectStatement selectAst = (SQLSelectStatement) getSQLStatement(s);
            selectAst.setSelect(null);
            new DB2SelectForUpdateRecognizer(s, selectAst).getWhereCondition();
        });

        //test for query was null
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "select * from t";
            SQLSelectStatement selectAst = (SQLSelectStatement) getSQLStatement(s);
            selectAst.getSelect().setQuery(null);
            new DB2SelectForUpdateRecognizer(s, selectAst).getWhereCondition();
        });
    }

    @Test
    public void testGetSqlType() {
        String sql = "select * from t where id = ? for update";
        SQLStatement statement = getSQLStatement(sql);

        DB2SelectForUpdateRecognizer recognizer = new DB2SelectForUpdateRecognizer(sql, statement);
        Assertions.assertEquals(recognizer.getSQLType(), SQLType.SELECT_FOR_UPDATE);
    }

    @Test
    public void testGetTableAlias() {
        String sql = "select * from t where id = ? for update";
        SQLStatement statement = getSQLStatement(sql);

        DB2SelectForUpdateRecognizer recognizer = new DB2SelectForUpdateRecognizer(sql, statement);
        Assertions.assertNull(recognizer.getTableAlias());
    }

    @Override
    public String getDbType() {
        return JdbcConstants.DB2;
    }
}
