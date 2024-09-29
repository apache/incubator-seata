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
package org.apache.seata.rm.datasource.sql.druid.kingbase;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import org.apache.seata.sqlparser.ParametersHolder;
import org.apache.seata.sqlparser.SQLParsingException;
import org.apache.seata.sqlparser.SQLType;
import org.apache.seata.sqlparser.druid.kingbase.KingbaseSelectForUpdateRecognizer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class KingbaseSelectForUpdateRecognizerTest {

    private static final String DB_TYPE = "kingbase";

    @Test
    public void testGetSqlType() {
        String sql = "select * from t where id = ? for update";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        KingbaseSelectForUpdateRecognizer recognizer = new KingbaseSelectForUpdateRecognizer(sql, asts.get(0));
        Assertions.assertEquals(recognizer.getSQLType(), SQLType.SELECT_FOR_UPDATE);
    }


    @Test
    public void testGetWhereCondition_0() {
        String sql = "select * from t for update";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        KingbaseSelectForUpdateRecognizer recognizer = new KingbaseSelectForUpdateRecognizer(sql, asts.get(0));
        String whereCondition = recognizer.getWhereCondition(new ParametersHolder() {
            @Override
            public Map<Integer,ArrayList<Object>> getParameters() {
                return null;
            }
        }, new ArrayList<>());
        Assertions.assertEquals("", whereCondition);
    }

    @Test
    public void testGetWhereCondition_1() {
        String sql = "select * from t for update";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        KingbaseSelectForUpdateRecognizer recognizer = new KingbaseSelectForUpdateRecognizer(sql, asts.get(0));
        String whereCondition = recognizer.getWhereCondition();

        Assertions.assertEquals("", whereCondition);

        //test for select was null
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "select * from t for update";
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(s, DB_TYPE);
            SQLSelectStatement selectAst = (SQLSelectStatement) sqlStatements.get(0);
            selectAst.setSelect(null);
            new KingbaseSelectForUpdateRecognizer(s, selectAst).getWhereCondition();
        });

        //test for query was null
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "select * from t";
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(s, DB_TYPE);
            SQLSelectStatement selectAst = (SQLSelectStatement) sqlStatements.get(0);
            selectAst.getSelect().setQuery(null);
            new KingbaseSelectForUpdateRecognizer(s, selectAst).getWhereCondition();
        });
    }

    @Test
    public void testGetTableAlias() {
        String sql = "select * from t where id = ? for update";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        KingbaseSelectForUpdateRecognizer recognizer = new KingbaseSelectForUpdateRecognizer(sql, asts.get(0));
        Assertions.assertNull(recognizer.getTableAlias());
    }

    @Test
    public void testGetTableName() {
        String sql = "select * from t where id = ? for update";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        KingbaseSelectForUpdateRecognizer recognizer = new KingbaseSelectForUpdateRecognizer(sql, asts.get(0));
        Assertions.assertEquals(recognizer.getTableName(), "t");
    }
}
