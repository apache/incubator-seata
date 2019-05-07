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

import com.alibaba.druid.sql.ast.SQLStatement;
import io.seata.rm.datasource.ParametersHolder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type My sql delete recognizer test.
 *
 * @author hanwen created at 2019-01-25
 */
public class MySQLDeleteRecognizerTest extends AbstractMySQLRecognizerTest {

    /**
     * Delete recognizer test 0.
     */
    @Test
    public void deleteRecognizerTest_0() {

        String sql = "DELETE FROM t1 WHERE id = 'id1'";

        SQLStatement statement = getSQLStatement(sql);

        MySQLDeleteRecognizer mySQLDeleteRecognizer = new MySQLDeleteRecognizer(sql, statement);

        Assertions.assertEquals(sql, mySQLDeleteRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", mySQLDeleteRecognizer.getTableName());
        Assertions.assertEquals("id = 'id1'", mySQLDeleteRecognizer.getWhereCondition());
    }

    /**
     * Delete recognizer test 1.
     */
    @Test
    public void deleteRecognizerTest_1() {

        String sql = "DELETE FROM t1 WHERE id = ?";

        SQLStatement statement = getSQLStatement(sql);

        MySQLDeleteRecognizer mySQLDeleteRecognizer = new MySQLDeleteRecognizer(sql, statement);

        Assertions.assertEquals(sql, mySQLDeleteRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", mySQLDeleteRecognizer.getTableName());

        // test overflow parameters
        ArrayList<Object> paramAppender = new ArrayList<>();
        String whereCondition = mySQLDeleteRecognizer.getWhereCondition(new ParametersHolder() {
            @Override
            public ArrayList<Object>[] getParameters() {
                ArrayList<Object> idParam = new ArrayList<>();
                idParam.add("id1");
                return new ArrayList[] {idParam};
            }
        }, paramAppender);

        Assertions.assertEquals(Collections.singletonList("id1"), paramAppender);
        Assertions.assertEquals("id = ?", whereCondition);
    }

    /**
     * Delete recognizer test 2.
     */
    @Test
    public void deleteRecognizerTest_2() {

        String sql = "DELETE FROM t1 WHERE id IN (?, ?)";

        SQLStatement statement = getSQLStatement(sql);

        MySQLDeleteRecognizer mySQLDeleteRecognizer = new MySQLDeleteRecognizer(sql, statement);

        Assertions.assertEquals(sql, mySQLDeleteRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", mySQLDeleteRecognizer.getTableName());

        // test overflow parameters
        ArrayList<Object> paramAppender = new ArrayList<>();
        String whereCondition = mySQLDeleteRecognizer.getWhereCondition(new ParametersHolder() {
            @Override
            public ArrayList<Object>[] getParameters() {
                ArrayList<Object> idParam = new ArrayList<>();
                idParam.add("id1");
                ArrayList<Object> id2Param = new ArrayList<>();
                id2Param.add("id2");
                return new ArrayList[] {idParam, id2Param};
            }
        }, paramAppender);

        Assertions.assertEquals(Arrays.asList("id1", "id2"), paramAppender);
        Assertions.assertEquals("id IN (?, ?)", whereCondition);
    }

    /**
     * Delete recognizer test 3.
     */
    @Test
    public void deleteRecognizerTest_3() {

        String sql = "DELETE FROM t1 WHERE id between ? AND ?";

        SQLStatement statement = getSQLStatement(sql);

        MySQLDeleteRecognizer mySQLDeleteRecognizer = new MySQLDeleteRecognizer(sql, statement);

        Assertions.assertEquals(sql, mySQLDeleteRecognizer.getOriginalSQL());
        Assertions.assertEquals("t1", mySQLDeleteRecognizer.getTableName());

        // test overflow parameters
        ArrayList<Object> paramAppender = new ArrayList<>();
        String whereCondition = mySQLDeleteRecognizer.getWhereCondition(new ParametersHolder() {
            @Override
            public ArrayList<Object>[] getParameters() {
                ArrayList<Object> idParam = new ArrayList<>();
                idParam.add("id1");
                ArrayList<Object> id2Param = new ArrayList<>();
                id2Param.add("id2");
                return new ArrayList[] {idParam, id2Param};
            }
        }, paramAppender);

        Assertions.assertEquals(Arrays.asList("id1", "id2"), paramAppender);
        Assertions.assertEquals("id BETWEEN ? AND ?", whereCondition);
    }
}
