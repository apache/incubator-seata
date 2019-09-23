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
package io.seata.rm.datasource.sql.druid.oracle;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.dialect.oracle.ast.expr.OracleArgumentExpr;
import io.seata.rm.datasource.ParametersHolder;
import io.seata.rm.datasource.sql.SQLParsingException;
import io.seata.rm.datasource.sql.SQLType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author will
 * @date 2019/9/24
 */
public class OracleSelectForUpdateRecognizerTest {

    private static final String DB_TYPE = "oracle";

    @Test
    public void testGetSqlType() {
        String sql = "select * from t where id = ? for update";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OracleSelectForUpdateRecognizer recognizer = new OracleSelectForUpdateRecognizer(sql, asts.get(0));
        Assertions.assertEquals(recognizer.getSQLType(), SQLType.SELECT_FOR_UPDATE);
    }


    @Test
    public void testGetWhereCondition_0() {
        String sql = "select * from t for update";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OracleSelectForUpdateRecognizer recognizer = new OracleSelectForUpdateRecognizer(sql, asts.get(0));
        String whereCondition = recognizer.getWhereCondition(new ParametersHolder() {
            @Override
            public ArrayList<Object>[] getParameters() {
                return null;
            }
        }, new ArrayList<>());

        //test for no condition
        Assertions.assertEquals("", whereCondition);

        sql = "select * from t where id = ? for update";
        asts = SQLUtils.parseStatements(sql, DB_TYPE);

        recognizer = new OracleSelectForUpdateRecognizer(sql, asts.get(0));
        whereCondition = recognizer.getWhereCondition(new ParametersHolder() {
            @Override
            public ArrayList<Object>[] getParameters() {
                ArrayList<Object> idParam = new ArrayList<>();
                idParam.add(1);
                return new ArrayList[] {idParam};
            }
        }, new ArrayList<>());

        //test for normal sql
        Assertions.assertEquals("id = ?", whereCondition);

        sql = "select * from t where id in (?) for update";
        asts = SQLUtils.parseStatements(sql, DB_TYPE);
        recognizer = new OracleSelectForUpdateRecognizer(sql, asts.get(0));
        whereCondition = recognizer.getWhereCondition(new ParametersHolder() {
            @Override
            public ArrayList<Object>[] getParameters() {
                ArrayList<Object> idParam = new ArrayList<>();
                idParam.add(1);
                return new ArrayList[] {idParam};
            }
        }, new ArrayList<>());

        //test for sql with in
        Assertions.assertEquals("id IN (?)", whereCondition);

        sql = "select * from t where id between ? and ? for update";
        asts = SQLUtils.parseStatements(sql, DB_TYPE);
        recognizer = new OracleSelectForUpdateRecognizer(sql, asts.get(0));
        whereCondition = recognizer.getWhereCondition(new ParametersHolder() {
            @Override
            public ArrayList<Object>[] getParameters() {
                ArrayList<Object> idParam = new ArrayList<>();
                idParam.add(1);
                ArrayList<Object> idParam2 = new ArrayList<>();
                idParam.add(2);
                return new ArrayList[] {idParam, idParam2};
            }
        }, new ArrayList<>());
        //test for sql with in
        Assertions.assertEquals("id BETWEEN ? AND ?", whereCondition);

        //test for exception
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            String s = "select * from t where id in (?) for update";
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(s, DB_TYPE);
            SQLSelectStatement selectAst = (SQLSelectStatement) sqlStatements.get(0);
            SQLSelectQueryBlock queryBlock = selectAst.getSelect().getQueryBlock();
            queryBlock.setWhere(new OracleArgumentExpr());
            new OracleSelectForUpdateRecognizer(s, selectAst).getWhereCondition(new ParametersHolder() {
                @Override
                public ArrayList<Object>[] getParameters() {
                    return new ArrayList[0];
                }
            }, new ArrayList<>());
        });
    }

    @Test
    public void testGetWhereCondition_1() {
        String sql = "select * from t for update";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OracleSelectForUpdateRecognizer recognizer = new OracleSelectForUpdateRecognizer(sql, asts.get(0));
        String whereCondition = recognizer.getWhereCondition();

        //test for no condition
        Assertions.assertEquals("", whereCondition);

        sql = "select * from t where id = 1 for update";
       asts = SQLUtils.parseStatements(sql, DB_TYPE);

        recognizer = new OracleSelectForUpdateRecognizer(sql, asts.get(0));
        whereCondition = recognizer.getWhereCondition();

        //test for normal sql
        Assertions.assertEquals("id = 1", whereCondition);

        sql = "select * from t where id in (1) for update";
        asts = SQLUtils.parseStatements(sql, DB_TYPE);
        recognizer = new OracleSelectForUpdateRecognizer(sql, asts.get(0));
        whereCondition = recognizer.getWhereCondition();

        //test for sql with in
        Assertions.assertEquals("id IN (1)", whereCondition);

        sql = "select * from t where id between 1 and 2 for update";
        asts = SQLUtils.parseStatements(sql, DB_TYPE);
        recognizer = new OracleSelectForUpdateRecognizer(sql, asts.get(0));
        whereCondition = recognizer.getWhereCondition();
        //test for sql with in
        Assertions.assertEquals("id BETWEEN 1 AND 2", whereCondition);

        //test for select was null
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "select * from t for update";
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(s, DB_TYPE);
            SQLSelectStatement selectAst = (SQLSelectStatement) sqlStatements.get(0);
            selectAst.setSelect(null);
            new OracleSelectForUpdateRecognizer(s, selectAst).getWhereCondition();
        });

        //test for query was null
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "select * from t";
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(s, DB_TYPE);
            SQLSelectStatement selectAst = (SQLSelectStatement) sqlStatements.get(0);
            selectAst.getSelect().setQuery(null);
            new OracleSelectForUpdateRecognizer(s, selectAst).getWhereCondition();
        });

        //test for exception
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            String s = "select * from t where id in (1) for update";
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(s, DB_TYPE);
            SQLSelectStatement selectAst = (SQLSelectStatement) sqlStatements.get(0);
            SQLSelectQueryBlock queryBlock = selectAst.getSelect().getQueryBlock();
            queryBlock.setWhere(new OracleArgumentExpr());
            new OracleSelectForUpdateRecognizer(s, selectAst).getWhereCondition();
        });
    }

    @Test
    public void testGetTableAlias() {
        String sql = "select * from t where id = ? for update";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OracleSelectForUpdateRecognizer recognizer = new OracleSelectForUpdateRecognizer(sql, asts.get(0));
        Assertions.assertNull(recognizer.getTableAlias());
    }

    @Test
    public void testGetTableName() {
        String sql = "select * from t where id = ? for update";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OracleSelectForUpdateRecognizer recognizer = new OracleSelectForUpdateRecognizer(sql, asts.get(0));
        Assertions.assertEquals(recognizer.getTableName(), "t");
    }
}
