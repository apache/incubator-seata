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
package io.seata.rm.datasource;

import com.alibaba.druid.util.JdbcConstants;
import io.seata.rm.datasource.sql.SQLRecognizer;
import io.seata.rm.datasource.sql.SQLVisitorFactory;
import io.seata.rm.datasource.sql.druid.MySQLSelectForUpdateRecognizer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type Sql visitor factory test.
 */
public class SQLVisitorFactoryTest {

    /**
     * Test sql recognizing.
     */
    @Test
    public void testSqlRecognizing() {
        String dbType = JdbcConstants.MYSQL;
        String sql = "select a, b, c from t1 where id = 9";
        SQLRecognizer recognizer = SQLVisitorFactory.get(sql, dbType);
        Assertions.assertNull(recognizer);

        sql = sql + " for update";
        recognizer = SQLVisitorFactory.get(sql, dbType);
        Assertions.assertTrue(recognizer instanceof MySQLSelectForUpdateRecognizer);
    }
}
