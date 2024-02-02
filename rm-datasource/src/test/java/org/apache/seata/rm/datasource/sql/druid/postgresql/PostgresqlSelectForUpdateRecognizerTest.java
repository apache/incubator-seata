/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.rm.datasource.sql.druid.postgresql;

import java.util.ArrayList;
import java.util.Map;

import org.apache.seata.rm.datasource.sql.SQLVisitorFactory;
import org.apache.seata.sqlparser.ParametersHolder;
import org.apache.seata.sqlparser.SQLSelectRecognizer;
import org.apache.seata.sqlparser.SQLType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class PostgresqlSelectForUpdateRecognizerTest {

    private static final String DB_TYPE = "postgresql";

    @Test
    public void testGetSqlType() {
        String sql = "select * from t where id = ? for update";

        SQLSelectRecognizer recognizer = (SQLSelectRecognizer) SQLVisitorFactory.get(sql, DB_TYPE).get(0);
        Assertions.assertEquals(recognizer.getSQLType(), SQLType.SELECT_FOR_UPDATE);
    }

    @Test
    public void testGetWhereCondition_0() {
        String sql = "select * from t for update";

        SQLSelectRecognizer recognizer = (SQLSelectRecognizer) SQLVisitorFactory.get(sql, DB_TYPE).get(0);
        String whereCondition = recognizer.getWhereCondition(new ParametersHolder() {
            @Override
            public Map<Integer, ArrayList<Object>> getParameters() {
                return null;
            }
        }, new ArrayList<>());
        Assertions.assertEquals("", whereCondition);
    }

    @Test
    public void testGetWhereCondition_1() {
        String sql = "select * from t for update";

        SQLSelectRecognizer recognizer = (SQLSelectRecognizer) SQLVisitorFactory.get(sql, DB_TYPE).get(0);
        String whereCondition = recognizer.getWhereCondition();

        Assertions.assertEquals("", whereCondition);
    }

    @Test
    public void testGetTableAlias() {
        String sql = "select * from t where id = ? for update";
        SQLSelectRecognizer recognizer = (SQLSelectRecognizer) SQLVisitorFactory.get(sql, DB_TYPE).get(0);
        Assertions.assertNull(recognizer.getTableAlias());
    }

    @Test
    public void testGetTableName() {
        String sql = "select * from t where id = ? for update";
        SQLSelectRecognizer recognizer = (SQLSelectRecognizer) SQLVisitorFactory.get(sql, DB_TYPE).get(0);
        Assertions.assertEquals(recognizer.getTableName(), "t");
    }
}
