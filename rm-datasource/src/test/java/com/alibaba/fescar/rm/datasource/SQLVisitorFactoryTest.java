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

package com.alibaba.fescar.rm.datasource;

import com.alibaba.druid.util.JdbcConstants;
import com.alibaba.fescar.rm.datasource.sql.SQLRecognizer;
import com.alibaba.fescar.rm.datasource.sql.SQLVisitorFactory;
import com.alibaba.fescar.rm.datasource.sql.druid.MySQLSelectForUpdateRecognizer;

import org.junit.Assert;
import org.junit.Test;

public class SQLVisitorFactoryTest {

    @Test
    public void testSqlRecognizing() {
        String dbType = JdbcConstants.MYSQL;
        String sql = "select a, b, c from t1 where id = 9";
        SQLRecognizer recognizer = SQLVisitorFactory.get(sql, dbType);
        Assert.assertNull(recognizer);

        sql = sql + " for update";
        recognizer = SQLVisitorFactory.get(sql, dbType);
        Assert.assertTrue(recognizer instanceof MySQLSelectForUpdateRecognizer);
    }
}
