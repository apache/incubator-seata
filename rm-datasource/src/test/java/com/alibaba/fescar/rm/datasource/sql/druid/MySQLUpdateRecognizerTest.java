/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.rm.datasource.sql.druid;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.fescar.rm.datasource.ParametersHolder;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

public class MySQLUpdateRecognizerTest extends AbstractMySQLRecognizerTest {

    @Test
    public void updateRecognizerTest_0() {

        String sql = "UPDATE t1 SET name = 'name1' WHERE id = 'id1'";

        SQLStatement statement = getSQLStatement(sql);

        MySQLUpdateRecognizer mySQLUpdateRecognizer = new MySQLUpdateRecognizer(sql, statement);

        Assert.assertEquals(sql, mySQLUpdateRecognizer.getOriginalSQL());
        Assert.assertEquals("t1", mySQLUpdateRecognizer.getTableName());
        Assert.assertEquals(1, mySQLUpdateRecognizer.getUpdateColumns().size());
        Assert.assertEquals("name", mySQLUpdateRecognizer.getUpdateColumns().get(0));
        Assert.assertEquals("name1", mySQLUpdateRecognizer.getUpdateValues().get(0));
        Assert.assertEquals("id = 'id1'", mySQLUpdateRecognizer.getWhereCondition());
    }

    @Test
    public void updateRecognizerTest_1() {

        String sql = "UPDATE t1 SET name1 = 'name1', name2 = 'name2' WHERE id = 'id1'";

        SQLStatement statement = getSQLStatement(sql);

        MySQLUpdateRecognizer mySQLUpdateRecognizer = new MySQLUpdateRecognizer(sql, statement);

        Assert.assertEquals(sql, mySQLUpdateRecognizer.getOriginalSQL());
        Assert.assertEquals("t1", mySQLUpdateRecognizer.getTableName());
        Assert.assertEquals(2, mySQLUpdateRecognizer.getUpdateColumns().size());
        Assert.assertEquals("name1", mySQLUpdateRecognizer.getUpdateColumns().get(0));
        Assert.assertEquals("name1", mySQLUpdateRecognizer.getUpdateValues().get(0));
        Assert.assertEquals("name2", mySQLUpdateRecognizer.getUpdateColumns().get(1));
        Assert.assertEquals("name2", mySQLUpdateRecognizer.getUpdateValues().get(1));
        Assert.assertEquals("id = 'id1'", mySQLUpdateRecognizer.getWhereCondition());
    }

    @Test
    public void updateRecognizerTest_2() {

        String sql = "UPDATE t1 SET name1 = 'name1', name2 = 'name2' WHERE id = ?";

        SQLStatement statement = getSQLStatement(sql);

        MySQLUpdateRecognizer mySQLUpdateRecognizer = new MySQLUpdateRecognizer(sql, statement);

        Assert.assertEquals(sql, mySQLUpdateRecognizer.getOriginalSQL());
        Assert.assertEquals("t1", mySQLUpdateRecognizer.getTableName());
        Assert.assertEquals(2, mySQLUpdateRecognizer.getUpdateColumns().size());
        Assert.assertEquals("name1", mySQLUpdateRecognizer.getUpdateColumns().get(0));
        Assert.assertEquals("name1", mySQLUpdateRecognizer.getUpdateValues().get(0));
        Assert.assertEquals("name2", mySQLUpdateRecognizer.getUpdateColumns().get(1));
        Assert.assertEquals("name2", mySQLUpdateRecognizer.getUpdateValues().get(1));

        ArrayList<Object> paramAppender = new ArrayList<>();
        String whereCondition = mySQLUpdateRecognizer.getWhereCondition(new ParametersHolder() {
            @Override
            public ArrayList<Object>[] getParameters() {
                ArrayList<Object> idParam = new ArrayList<>();
                idParam.add("id1");
                return new ArrayList[]{idParam};
            }
        }, paramAppender);

        Assert.assertEquals(Collections.singletonList("id1"), paramAppender);

        Assert.assertEquals("id = ?", whereCondition);
    }
}
