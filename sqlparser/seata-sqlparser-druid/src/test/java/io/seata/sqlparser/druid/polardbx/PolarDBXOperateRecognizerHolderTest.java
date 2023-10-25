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
package io.seata.sqlparser.druid.polardbx;

import com.alibaba.druid.sql.ast.SQLStatement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for recognizer holder of PolarDB-X
 *
 * @author hsien999
 */
public class PolarDBXOperateRecognizerHolderTest extends AbstractPolarDBXRecognizerTest {
    @Test
    public void getDeleteRecognizerTest() {
        String sql = "DELETE FROM t WHERE id = 1";
        SQLStatement sqlStatement = getSQLStatement(sql);
        Assertions.assertNotNull(new PolarDBXOperateRecognizerHolder().getDeleteRecognizer(sql, sqlStatement));
    }

    @Test
    public void getInsertRecognizerTest() {
        String sql = "INSERT INTO t (name) VALUES ('test')";
        SQLStatement sqlStatement = getSQLStatement(sql);
        Assertions.assertNotNull(new PolarDBXOperateRecognizerHolder().getInsertRecognizer(sql, sqlStatement));
    }

    @Test
    public void getUpdateRecognizerTest() {
        String sql = "UPDATE t SET name = 'test' WHERE id = 1";
        SQLStatement sqlStatement = getSQLStatement(sql);
        Assertions.assertNotNull(new PolarDBXOperateRecognizerHolder().getUpdateRecognizer(sql, sqlStatement));
    }

    @Test
    public void getSelectForUpdateTest() {
        // common select without lock
        String sql = "SELECT name FROM t1 WHERE id = 1";
        SQLStatement sqlStatement = getSQLStatement(sql);
        Assertions.assertNull(new PolarDBXOperateRecognizerHolder().getSelectForUpdateRecognizer(sql, sqlStatement));

        // select for update
        sql += " FOR UPDATE";
        sqlStatement = getSQLStatement(sql);
        Assertions.assertNotNull(new PolarDBXOperateRecognizerHolder().getSelectForUpdateRecognizer(sql, sqlStatement));
    }
}
