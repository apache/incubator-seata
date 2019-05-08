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

        ArrayList<Object> paramAppender = new ArrayList<>();
        String whereCondition = mySQLUpdateRecognizer.getWhereCondition(new ParametersHolder() {
            @Override
            public ArrayList<Object>[] getParameters() {
                ArrayList<Object> idParam = new ArrayList<>();
                idParam.add("id1");
                return new ArrayList[]{idParam};
            }
        }, paramAppender);

        Assertions.assertEquals(Collections.singletonList("id1"), paramAppender);

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

        ArrayList<Object> paramAppender = new ArrayList<>();
        String whereCondition = mySQLUpdateRecognizer.getWhereCondition(new ParametersHolder() {
            @Override
            public ArrayList<Object>[] getParameters() {
                ArrayList<Object> id1Param = new ArrayList<>();
                id1Param.add("id1");
                ArrayList<Object> id2Param = new ArrayList<>();
                id2Param.add("id2");
                return new ArrayList[]{id1Param, id2Param};
            }
        }, paramAppender);

        Assertions.assertEquals(Arrays.asList("id1", "id2"), paramAppender);

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

        ArrayList<Object> paramAppender = new ArrayList<>();
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
        }, paramAppender);

        Assertions.assertEquals(Arrays.asList("id1", "id2", "name"), paramAppender);

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

        ArrayList<Object> paramAppender = new ArrayList<>();
        String whereCondition = mySQLUpdateRecognizer.getWhereCondition(new ParametersHolder() {
            @Override
            public ArrayList<Object>[] getParameters() {
                ArrayList<Object> id1Param = new ArrayList<>();
                id1Param.add("id1");
                ArrayList<Object> id2Param = new ArrayList<>();
                id2Param.add("id2");
                return new ArrayList[]{id1Param, id2Param};
            }
        }, paramAppender);

        Assertions.assertEquals(Arrays.asList("id1", "id2"), paramAppender);
        Assertions.assertEquals("id BETWEEN ? AND ?", whereCondition);
    }
}
