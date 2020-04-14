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

import io.seata.rm.datasource.sql.SQLVisitorFactory;
import io.seata.sqlparser.ParametersHolder;
import io.seata.sqlparser.SQLSelectRecognizer;
import io.seata.sqlparser.SQLType;
import java.util.ArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author will
 */
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
            public ArrayList<Object>[] getParameters() {
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
