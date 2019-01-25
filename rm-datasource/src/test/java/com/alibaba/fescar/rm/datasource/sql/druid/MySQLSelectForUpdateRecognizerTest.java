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

import java.util.ArrayList;
import java.util.Collections;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.fescar.rm.datasource.ParametersHolder;

import org.junit.Assert;
import org.junit.Test;

public class MySQLSelectForUpdateRecognizerTest extends AbstractMySQLRecognizerTest {

    @Test
    public void selectForUpdateRecognizerTest_0() {

        String sql = "SELECT name FROM t1 WHERE id = 'id1' FOR UPDATE";

        SQLStatement statement = getSQLStatement(sql);

        MySQLSelectForUpdateRecognizer mySQLUpdateRecognizer = new MySQLSelectForUpdateRecognizer(sql, statement);

        Assert.assertEquals(sql, mySQLUpdateRecognizer.getOriginalSQL());
        Assert.assertEquals("t1", mySQLUpdateRecognizer.getTableName());
        Assert.assertEquals("id = 'id1'", mySQLUpdateRecognizer.getWhereCondition());
    }

    @Test
    public void selectForUpdateRecognizerTest_1() {

        String sql = "SELECT name FROM t1 WHERE id = ? FOR UPDATE";

        SQLStatement statement = getSQLStatement(sql);

        MySQLSelectForUpdateRecognizer mySQLUpdateRecognizer = new MySQLSelectForUpdateRecognizer(sql, statement);

        Assert.assertEquals(sql, mySQLUpdateRecognizer.getOriginalSQL());
        Assert.assertEquals("t1", mySQLUpdateRecognizer.getTableName());

        ArrayList<Object> paramAppender = new ArrayList<>();
        String whereCondition = mySQLUpdateRecognizer.getWhereCondition(new ParametersHolder() {
            @Override
            public ArrayList<Object>[] getParameters() {
                ArrayList<Object> idParam = new ArrayList<>();
                idParam.add("id1");
                return new ArrayList[] {idParam};
            }
        }, paramAppender);

        Assert.assertEquals(Collections.singletonList("id1"), paramAppender);
        Assert.assertEquals("id = ?", whereCondition);
    }

    @Test
    public void selectForUpdateRecognizerTest_3() {

        String sql = "SELECT name1, name2 FROM t1 WHERE id1 = ? FOR UPDATE";

        SQLStatement statement = getSQLStatement(sql);

        MySQLSelectForUpdateRecognizer mySQLUpdateRecognizer = new MySQLSelectForUpdateRecognizer(sql, statement);

        Assert.assertEquals(sql, mySQLUpdateRecognizer.getOriginalSQL());
        Assert.assertEquals("t1", mySQLUpdateRecognizer.getTableName());

        // test overflow parameters
        ArrayList<Object> paramAppender = new ArrayList<>();
        String whereCondition = mySQLUpdateRecognizer.getWhereCondition(new ParametersHolder() {
            @Override
            public ArrayList<Object>[] getParameters() {
                ArrayList<Object> id1Param = new ArrayList<>();
                id1Param.add("id1");
                ArrayList<Object> id2Param = new ArrayList<>();
                id2Param.add("id2");
                return new ArrayList[] {id1Param, id2Param};
            }
        }, paramAppender);

        Assert.assertEquals(Collections.singletonList("id1"), paramAppender);
        Assert.assertEquals("id1 = ?", whereCondition);
    }
}
