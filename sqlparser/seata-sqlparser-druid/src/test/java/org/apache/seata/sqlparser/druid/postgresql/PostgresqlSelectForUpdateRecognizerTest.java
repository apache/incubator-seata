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
package org.apache.seata.sqlparser.druid.postgresql;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import org.apache.seata.sqlparser.ParametersHolder;
import org.apache.seata.sqlparser.SQLType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class PostgresqlSelectForUpdateRecognizerTest {

    private static final String DB_TYPE = "postgresql";

    @Test
    public void testGetSqlType() {
        String sql = "select * from t where id = ? for update";

        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);
        PostgresqlSelectForUpdateRecognizer postgresqlSelectForUpdateRecognizer = new PostgresqlSelectForUpdateRecognizer(sql, asts.get(0));
        Assertions.assertEquals(postgresqlSelectForUpdateRecognizer.getSQLType(), SQLType.SELECT_FOR_UPDATE);
    }

    @Test
    public void testGetWhereCondition_0() {
        String sql = "select * from t for update";

        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);
        PostgresqlSelectForUpdateRecognizer postgresqlSelectForUpdateRecognizer = new PostgresqlSelectForUpdateRecognizer(sql, asts.get(0));
        String whereCondition = postgresqlSelectForUpdateRecognizer.getWhereCondition(new ParametersHolder() {
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

        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);
        PostgresqlSelectForUpdateRecognizer postgresqlSelectForUpdateRecognizer = new PostgresqlSelectForUpdateRecognizer(sql, asts.get(0));
        String whereCondition = postgresqlSelectForUpdateRecognizer.getWhereCondition();

        Assertions.assertEquals("", whereCondition);
    }

    @Test
    public void testGetTableAlias() {
        String sql = "select * from t where id = ? for update";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);
        PostgresqlSelectForUpdateRecognizer postgresqlSelectForUpdateRecognizer = new PostgresqlSelectForUpdateRecognizer(sql, asts.get(0));
        Assertions.assertNull(postgresqlSelectForUpdateRecognizer.getTableAlias());
    }

    @Test
    public void testGetTableName() {
        String sql = "select * from t where id = ? for update";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);
        PostgresqlSelectForUpdateRecognizer postgresqlSelectForUpdateRecognizer = new PostgresqlSelectForUpdateRecognizer(sql, asts.get(0));
        Assertions.assertEquals(postgresqlSelectForUpdateRecognizer.getTableName(), "t");
    }
}
