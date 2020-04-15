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
package io.seata.rm.datasource.sql.druid.postgresql;

import io.seata.sqlparser.ParametersHolder;
import io.seata.sqlparser.SQLDeleteRecognizer;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLType;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;

import io.seata.rm.datasource.sql.SQLVisitorFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author will
 */
public class PostgresqlDeleteRecognizerTest {

    private static final String DB_TYPE = "postgresql";

    @Test
    public void testGetSqlType() {
        String sql = "delete from t where id = ?";
        List<SQLRecognizer> sqlRecognizers = SQLVisitorFactory.get(sql, DB_TYPE);
        SQLDeleteRecognizer recognizer = (SQLDeleteRecognizer) sqlRecognizers.get(0);
        Assertions.assertEquals(recognizer.getSQLType(), SQLType.DELETE);
    }

    @Test
    public void testGetTableAlias() {
        String sql = "delete from t where id = ?";
        List<SQLRecognizer> sqlRecognizers = SQLVisitorFactory.get(sql, DB_TYPE);
        SQLDeleteRecognizer recognizer = (SQLDeleteRecognizer) sqlRecognizers.get(0);
        Assertions.assertNull(recognizer.getTableAlias());
    }

    @Test
    public void testGetTableName() {
        String sql = "delete from t where id = ?";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);
        List<SQLRecognizer> sqlRecognizers = SQLVisitorFactory.get(sql, DB_TYPE);
        SQLDeleteRecognizer recognizer = (SQLDeleteRecognizer) sqlRecognizers.get(0);
        Assertions.assertEquals(recognizer.getTableName(), "t");
    }

    @Test
    public void testGetWhereCondition_0() {
        String sql = "delete from t";

        List<SQLRecognizer> sqlRecognizers = SQLVisitorFactory.get(sql, DB_TYPE);
        SQLDeleteRecognizer recognizer = (SQLDeleteRecognizer) sqlRecognizers.get(0);
        String whereCondition = recognizer.getWhereCondition(new ParametersHolder() {
            @Override
            public ArrayList<Object>[] getParameters() {
                return null;
            }
        }, new ArrayList<>());

        //test for no condition
        Assertions.assertEquals("", whereCondition);
    }

    @Test
    public void testGetWhereCondition_1() {
        String sql = "delete from t";

        List<SQLRecognizer> sqlRecognizers = SQLVisitorFactory.get(sql, DB_TYPE);
        SQLDeleteRecognizer recognizer = (SQLDeleteRecognizer) sqlRecognizers.get(0);
        String whereCondition = recognizer.getWhereCondition();

        //test for no condition
        Assertions.assertEquals("", whereCondition);
    }
}
