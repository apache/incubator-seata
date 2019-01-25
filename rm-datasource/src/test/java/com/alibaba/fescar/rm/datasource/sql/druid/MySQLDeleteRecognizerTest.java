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

/**
 * @author hanwen
 * created at 2019-01-25
 */
public class MySQLDeleteRecognizerTest extends AbstractMySQLRecognizerTest {

    @Test
    public void deleteRecognizerTest_0() {

        String sql = "DELETE FROM t1 WHERE id = 'id1'";

        SQLStatement statement = getSQLStatement(sql);

        MySQLDeleteRecognizer mySQLDeleteRecognizer = new MySQLDeleteRecognizer(sql, statement);

        Assert.assertEquals(sql, mySQLDeleteRecognizer.getOriginalSQL());
        Assert.assertEquals("t1", mySQLDeleteRecognizer.getTableName());
        Assert.assertEquals("id = 'id1'", mySQLDeleteRecognizer.getWhereCondition());
    }

    @Test
    public void deleteRecognizerTest_1() {

        String sql = "DELETE FROM t1 WHERE id = ?";

        SQLStatement statement = getSQLStatement(sql);

        MySQLDeleteRecognizer mySQLDeleteRecognizer = new MySQLDeleteRecognizer(sql, statement);

        Assert.assertEquals(sql, mySQLDeleteRecognizer.getOriginalSQL());
        Assert.assertEquals("t1", mySQLDeleteRecognizer.getTableName());

        // test overflow parameters
        ArrayList<Object> paramAppender = new ArrayList<>();
        String whereCondition = mySQLDeleteRecognizer.getWhereCondition(new ParametersHolder() {
            @Override
            public ArrayList<Object>[] getParameters() {
                ArrayList<Object> idParam = new ArrayList<>();
                idParam.add("id1");
                ArrayList<Object> nameParam = new ArrayList<>();
                nameParam.add("name1");
                return new ArrayList[] {idParam, nameParam};
            }
        }, paramAppender);

        Assert.assertEquals(Collections.singletonList("id1"), paramAppender);
        Assert.assertEquals("id = ?", whereCondition);
    }
}
