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
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.expr.MySqlOrderingExpr;
import org.apache.seata.sqlparser.SQLType;
import org.apache.seata.sqlparser.druid.AbstractRecognizerTest;
import org.apache.seata.sqlparser.druid.BaseRecognizer;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author GoodBoyCoder
 * @date 2021-10-25
 */
public class DB2DeleteRecognizerTest extends AbstractRecognizerTest {
    @Test
    public void testVMarker() {
        Assertions.assertEquals("?", new BaseRecognizer.VMarker().toString());
    }

    /**
     * Delete recognizer test 1.
     */
    @Test
    public void deleteRecognizerTest_1() {

        String sql = "DELETE FROM t1 WHERE id = ?";

        SQLStatement statement = getSQLStatement(sql);

        DB2DeleteRecognizer db2DeleteRecognizer = new DB2DeleteRecognizer(sql, statement);

        Assertions.assertEquals(sql, db2DeleteRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", db2DeleteRecognizer.getTableName());

        // test overflow parameters
        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = db2DeleteRecognizer.getWhereCondition(() -> {
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
     * Delete recognizer test 2.
     */
    @Test
    public void deleteRecognizerTest_2() {

        String sql = "DELETE FROM t1 WHERE id IN (?, ?)";

        SQLStatement statement = getSQLStatement(sql);

        DB2DeleteRecognizer db2DeleteRecognizer = new DB2DeleteRecognizer(sql, statement);

        Assertions.assertEquals(sql, db2DeleteRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", db2DeleteRecognizer.getTableName());

        // test overflow parameters
        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = db2DeleteRecognizer.getWhereCondition(() -> {
            ArrayList<Object> idParam = new ArrayList<>();
            idParam.add("id1");
            ArrayList<Object> id2Param = new ArrayList<>();
            id2Param.add("id2");
            Map<Integer, ArrayList<Object>> result = new HashMap<>();
            result.put(1, idParam);
            result.put(2, id2Param);
            return result;
        }, paramAppenderList);

        Assertions.assertEquals(Arrays.asList(Arrays.asList("id1", "id2")), paramAppenderList);
        Assertions.assertEquals("id IN (?, ?)", whereCondition);
    }

    /**
     * Delete recognizer test 3.
     */
    @Test
    public void deleteRecognizerTest_3() {

        String sql = "DELETE FROM t1 WHERE id between ? AND ?";

        SQLStatement statement = getSQLStatement(sql);

        DB2DeleteRecognizer db2DeleteRecognizer = new DB2DeleteRecognizer(sql, statement);

        Assertions.assertEquals(sql, db2DeleteRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", db2DeleteRecognizer.getTableName());

        // test overflow parameters
        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String whereCondition = db2DeleteRecognizer.getWhereCondition(() -> {
            ArrayList<Object> idParam = new ArrayList<>();
            idParam.add("id1");
            ArrayList<Object> id2Param = new ArrayList<>();
            id2Param.add("id2");
            Map<Integer, ArrayList<Object>> result = new HashMap<>();
            result.put(1, idParam);
            result.put(2, id2Param);
            return result;
        }, paramAppenderList);

        Assertions.assertEquals(Collections.singletonList(Arrays.asList("id1", "id2")), paramAppenderList);
        Assertions.assertEquals("id BETWEEN ? AND ?", whereCondition);
    }

    @Test
    public void testGetSqlType() {
        String sql = "delete from t where id = ?";
        SQLStatement ast = getSQLStatement(sql);

        DB2DeleteRecognizer recognizer = new DB2DeleteRecognizer(sql, ast);
        Assertions.assertEquals(recognizer.getSQLType(), SQLType.DELETE);
    }

    @Test
    public void testGetTableAlias() {
        String sql = "delete from t where id = ?";
        SQLStatement ast = getSQLStatement(sql);

        DB2DeleteRecognizer recognizer = new DB2DeleteRecognizer(sql, ast);
        Assertions.assertNull(recognizer.getTableAlias());
    }

    @Test
    public void testGetWhereCondition_0() {
        String sql = "delete from t";
        SQLStatement ast = getSQLStatement(sql);

        DB2DeleteRecognizer recognizer = new DB2DeleteRecognizer(sql, ast);
        String whereCondition = recognizer.getWhereCondition(() -> null, new ArrayList<>());

        //test for no condition
        Assertions.assertEquals("", whereCondition);

        sql = "delete from t where id = ?";
        ast = getSQLStatement(sql);

        recognizer = new DB2DeleteRecognizer(sql, ast);
        whereCondition = recognizer.getWhereCondition(() -> {
            ArrayList<Object> idParam = new ArrayList<>();
            idParam.add(1);
            Map<Integer, ArrayList<Object>> result = new HashMap<>();
            result.put(1, idParam);
            return result;
        }, new ArrayList<>());

        //test for normal sql
        Assertions.assertEquals("id = ?", whereCondition);

        sql = "delete from t where id in (?)";
        ast = getSQLStatement(sql);
        recognizer = new DB2DeleteRecognizer(sql, ast);
        whereCondition = recognizer.getWhereCondition(() -> {
            ArrayList<Object> idParam = new ArrayList<>();
            idParam.add(1);
            Map<Integer, ArrayList<Object>> result = new HashMap<>();
            result.put(1, idParam);
            return result;
        }, new ArrayList<>());

        //test for sql with in
        Assertions.assertEquals("id IN (?)", whereCondition);

        sql = "delete from t where id between ? and ?";
        ast = getSQLStatement(sql);
        recognizer = new DB2DeleteRecognizer(sql, ast);
        whereCondition = recognizer.getWhereCondition(() -> {
            ArrayList<Object> idParam = new ArrayList<>();
            idParam.add(1);
            ArrayList<Object> idParam2 = new ArrayList<>();
            idParam.add(2);
            Map<Integer, ArrayList<Object>> result = new HashMap<>();
            result.put(1, idParam);
            result.put(2, idParam2);
            return result;
        }, new ArrayList<>());
        //test for sql with in
        Assertions.assertEquals("id BETWEEN ? AND ?", whereCondition);

        //test for exception
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            String s = "delete from t where id in (?)";
            SQLDeleteStatement deleteAst = (SQLDeleteStatement) getSQLStatement(s);
            deleteAst.setWhere(new MySqlOrderingExpr());
            new DB2DeleteRecognizer(s, deleteAst).getWhereCondition(HashMap::new, new ArrayList<>());
        });
    }

    @Test
    public void testGetWhereCondition_1() {

        String sql = "delete from t";
        SQLStatement ast = getSQLStatement(sql);

        DB2DeleteRecognizer recognizer = new DB2DeleteRecognizer(sql, ast);
        String whereCondition = recognizer.getWhereCondition();

        //test for no condition
        Assertions.assertEquals("", whereCondition);

        sql = "delete from t where id = 1";
        ast = getSQLStatement(sql);

        recognizer = new DB2DeleteRecognizer(sql, ast);
        whereCondition = recognizer.getWhereCondition();

        //test for normal sql
        Assertions.assertEquals("id = 1", whereCondition);

        sql = "delete from t where id in (1)";
        ast = getSQLStatement(sql);
        recognizer = new DB2DeleteRecognizer(sql, ast);
        whereCondition = recognizer.getWhereCondition();

        //test for sql with in
        Assertions.assertEquals("id IN (1)", whereCondition);

        sql = "delete from t where id between 1 and 2";
        ast = getSQLStatement(sql);
        recognizer = new DB2DeleteRecognizer(sql, ast);
        whereCondition = recognizer.getWhereCondition();
        //test for sql with in
        Assertions.assertEquals("id BETWEEN 1 AND 2", whereCondition);

        //test for exception
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            String s = "delete from t where id in (1)";
            SQLDeleteStatement deleteAst = (SQLDeleteStatement) getSQLStatement(s);
            deleteAst.setWhere(new MySqlOrderingExpr());
            new DB2DeleteRecognizer(s, deleteAst).getWhereCondition();
        });
    }

    @Override
    public String getDbType() {
        return JdbcConstants.DB2;
    }
}
