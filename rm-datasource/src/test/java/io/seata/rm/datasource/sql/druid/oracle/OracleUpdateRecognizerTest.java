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
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.sql.dialect.oracle.ast.expr.OracleArgumentExpr;
import com.alibaba.druid.sql.dialect.oracle.ast.expr.OracleCursorExpr;
import io.seata.rm.datasource.ParametersHolder;
import io.seata.rm.datasource.sql.SQLParsingException;
import io.seata.rm.datasource.sql.SQLType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author will
 */
public class OracleUpdateRecognizerTest {

    private static final String DB_TYPE = "oracle";

    @Test
    public void testGetSqlType() {
        String sql = "update t set n = ?";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OracleUpdateRecognizer recognizer = new OracleUpdateRecognizer(sql, asts.get(0));
        Assertions.assertEquals(recognizer.getSQLType(), SQLType.UPDATE);
    }

    @Test
    public void testGetUpdateColumns() {
        // test with normal
        String sql = "update t set a = ?, b = ?, c = ?";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);
        OracleUpdateRecognizer recognizer = new OracleUpdateRecognizer(sql, asts.get(0));
        List<String> updateColumns = recognizer.getUpdateColumns();
        Assertions.assertEquals(updateColumns.size(), 3);

        // test with alias
        sql = "update t set a.a = ?, a.b = ?, a.c = ?";
        asts = SQLUtils.parseStatements(sql, DB_TYPE);
        recognizer = new OracleUpdateRecognizer(sql, asts.get(0));
        updateColumns = recognizer.getUpdateColumns();
        Assertions.assertEquals(updateColumns.size(), 3);

        //test with error
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "update t set a = a";
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(s, DB_TYPE);
            SQLUpdateStatement sqlUpdateStatement = (SQLUpdateStatement) sqlStatements.get(0);
            List<SQLUpdateSetItem> updateSetItems = sqlUpdateStatement.getItems();
            for (SQLUpdateSetItem updateSetItem : updateSetItems) {
                updateSetItem.setColumn(new OracleCursorExpr());
            }
            OracleUpdateRecognizer oracleUpdateRecognizer = new OracleUpdateRecognizer(s, sqlUpdateStatement);
            oracleUpdateRecognizer.getUpdateColumns();
        });
    }

    @Test
    public void testGetUpdateValues() {
        // test with normal
        String sql = "update t set a = ?, b = ?, c = ?";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);
        OracleUpdateRecognizer recognizer = new OracleUpdateRecognizer(sql, asts.get(0));
        List<Object> updateValues = recognizer.getUpdateValues();
        Assertions.assertEquals(updateValues.size(), 3);

        // test with values
        sql = "update t set a = 1, b = 2, c = 3";
        asts = SQLUtils.parseStatements(sql, DB_TYPE);
        recognizer = new OracleUpdateRecognizer(sql, asts.get(0));
        updateValues = recognizer.getUpdateValues();
        Assertions.assertEquals(updateValues.size(), 3);

        // test with error
        Assertions.assertThrows(SQLParsingException.class, () -> {
            String s = "update t set a = ?";
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(s, DB_TYPE);
            SQLUpdateStatement sqlUpdateStatement = (SQLUpdateStatement)sqlStatements.get(0);
            List<SQLUpdateSetItem> updateSetItems = sqlUpdateStatement.getItems();
            for (SQLUpdateSetItem updateSetItem : updateSetItems) {
                updateSetItem.setValue(new OracleCursorExpr());
            }
            OracleUpdateRecognizer oracleUpdateRecognizer = new OracleUpdateRecognizer(s, sqlUpdateStatement);
            oracleUpdateRecognizer.getUpdateValues();
        });
    }

    @Test
    public void testGetWhereCondition_0() {

        String sql = "update t set a = 1";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OracleUpdateRecognizer recognizer = new OracleUpdateRecognizer(sql, asts.get(0));
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

        String sql = "update t set a = 1";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OracleUpdateRecognizer recognizer = new OracleUpdateRecognizer(sql, asts.get(0));
        String whereCondition = recognizer.getWhereCondition();

        Assertions.assertEquals("", whereCondition);
    }

    @Test
    public void testGetTableAlias() {
        String sql = "update t set a = ?, b = ?, c = ?";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OracleUpdateRecognizer recognizer = new OracleUpdateRecognizer(sql, asts.get(0));
        Assertions.assertNull(recognizer.getTableAlias());
    }

    @Test
    public void testGetTableName() {
        String sql = "update t set a = ?, b = ?, c = ?";
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, DB_TYPE);

        OracleUpdateRecognizer recognizer = new OracleUpdateRecognizer(sql, asts.get(0));
        Assertions.assertEquals(recognizer.getTableName(), "t");
    }
}
