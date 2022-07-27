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
package io.seata.sqlparser.druid.oceanbaseoracle;

import com.alibaba.druid.sql.ast.SQLStatement;
import io.seata.sqlparser.druid.AbstractRecognizerTest;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for recognizer holder of OceanBaseOracle
 *
 * @author hsien999
 */
public class OceanBaseOracleOperateRecognizerHolderTest extends AbstractRecognizerTest {
    @Override
    public String getDbType() {
        return JdbcConstants.OCEANBASE_ORACLE;
    }

    @Test
    public void getDeleteRecognizerTest() {
        String sql = "DELETE FROM t WHERE id = 1";
        SQLStatement sqlStatement = getSQLStatement(sql);
        Assertions.assertNotNull(new OceanBaseOracleOperateRecognizerHolder().getDeleteRecognizer(sql, sqlStatement));
    }

    @Test
    public void getInsertRecognizerTest() {
        String sql = "INSERT INTO t (name) VALUES ('test')";
        SQLStatement sqlStatement = getSQLStatement(sql);
        Assertions.assertNotNull(new OceanBaseOracleOperateRecognizerHolder().getInsertRecognizer(sql, sqlStatement));
    }

    @Test
    public void getUpdateRecognizerTest() {
        String sql = "UPDATE t SET name = 'test' WHERE id = 1";
        SQLStatement sqlStatement = getSQLStatement(sql);
        Assertions.assertNotNull(new OceanBaseOracleOperateRecognizerHolder().getUpdateRecognizer(sql, sqlStatement));
    }

    @Test
    public void getSelectForUpdateTest() {
        // common select without lock
        String sql = "SELECT name FROM t1 WHERE id = 1";
        SQLStatement sqlStatement = getSQLStatement(sql);
        Assertions.assertNull(new OceanBaseOracleOperateRecognizerHolder().getSelectForUpdateRecognizer(sql, sqlStatement));

        // select for update
        sql += " FOR UPDATE";
        sqlStatement = getSQLStatement(sql);
        Assertions.assertNotNull(new OceanBaseOracleOperateRecognizerHolder().getSelectForUpdateRecognizer(sql, sqlStatement));
    }
}
