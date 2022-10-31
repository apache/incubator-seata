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
package io.seata.sqlparser.druid;

import com.alibaba.druid.sql.ast.SQLStatement;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.sqlparser.druid.oracle.OracleInsertRecognizer;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author: lyx
 */
public class OracleInsertRecognizerTest extends AbstractRecognizerTest {

    @Test
    public void testGetHintColumnName() {
        String sql = "INSERT /*+ IGNORE_ROW_ON_DUPKEY_INDEX(TEST_TABLE(TEST_COLUMN)) */ INTO TEST_TABLE(TEST_COLUMN) VALUES ('TEST_COLUMN1')";
        SQLStatement statement = getSQLStatement(sql);
        OracleInsertRecognizer oracleInsertRecognizer = new OracleInsertRecognizer(sql, statement);
        Assertions.assertEquals("TEST_COLUMN", oracleInsertRecognizer.getHintColumnName());

        String sql01 = "INSERT /*+ IGNORE_ROW_ON_DUPKEY_INDEX(\"TEST_TABLE\"(\"TEST_COLUMN\")) */ INTO TEST_TABLE(TEST_COLUMN) VALUES ('TEST_COLUMN1')";
        SQLStatement statement01 = getSQLStatement(sql01);
        OracleInsertRecognizer oracleInsertRecognizer01 = new OracleInsertRecognizer(sql01, statement01);
        Assertions.assertEquals("\"TEST_COLUMN\"", oracleInsertRecognizer01.getHintColumnName());

        String sql02 = "INSERT /*+ IGNORE_ROW_ON_DUPKEY_INDEX(\"ERROR\"(\"TEST_COLUMN\")) */ INTO TEST_TABLE(TEST_COLUMN) VALUES ('TEST_COLUMN1')";
        SQLStatement statement02 = getSQLStatement(sql02);
        Assertions.assertThrows(ShouldNeverHappenException.class, () -> new OracleInsertRecognizer(sql01, statement02));
    }

    @Override
    public String getDbType() {
        return JdbcConstants.ORACLE;
    }
}
