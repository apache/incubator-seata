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
package org.apache.seata.sqlparser.druid.oceanbase;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.parser.SQLParserUtils;
import com.alibaba.druid.sql.parser.SQLStatementParser;
import org.apache.seata.sqlparser.SQLRecognizer;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OceanBaseOperateRecognizerHolderTest {

    private OceanBaseOperateRecognizerHolder recognizerHolder;

    @BeforeEach
    void setUp() {
        recognizerHolder = new OceanBaseOperateRecognizerHolder();
    }

    @Test
    void testGetDeleteRecognizer() {
        // Arrange
        String deleteSql = "DELETE FROM users WHERE id = ?";
        SQLStatementParser parser = SQLParserUtils.createSQLStatementParser(deleteSql, JdbcConstants.OCEANBASE);
        SQLStatement deleteStatement = parser.parseStatement();

        // Act
        SQLRecognizer deleteRecognizer = recognizerHolder.getDeleteRecognizer(deleteSql, deleteStatement);

        // Assert
        assertNotNull(deleteRecognizer);
        assertTrue(deleteRecognizer instanceof OceanBaseDeleteRecognizer);
    }

    @Test
    void testGetInsertRecognizer() {
        // Arrange
        String insertSql = "INSERT INTO users (id, name) VALUES (?, ?)";
        SQLStatementParser parser = SQLParserUtils.createSQLStatementParser(insertSql, JdbcConstants.OCEANBASE);
        SQLStatement insertStatement = parser.parseStatement();

        // Act
        SQLRecognizer insertRecognizer = recognizerHolder.getInsertRecognizer(insertSql, insertStatement);

        // Assert
        assertNotNull(insertRecognizer);
        assertTrue(insertRecognizer instanceof OceanBaseInsertRecognizer);
    }

    @Test
    void testGetUpdateRecognizer() {
        // Arrange
        String updateSql = "UPDATE users SET name = ? WHERE id = ?";
        SQLStatementParser parser = SQLParserUtils.createSQLStatementParser(updateSql, JdbcConstants.OCEANBASE);
        SQLStatement updateStatement = parser.parseStatement();

        // Act
        SQLRecognizer updateRecognizer = recognizerHolder.getUpdateRecognizer(updateSql, updateStatement);

        // Assert
        assertNotNull(updateRecognizer);
        assertTrue(updateRecognizer instanceof OceanBaseUpdateRecognizer);
    }
}
