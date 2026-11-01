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
package org.apache.seata.rm.datasource.sql.handler.sqlserver;

import org.apache.seata.sqlparser.util.ColumnUtils;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlServerEscapeHandlerTest {

    private final SqlServerEscapeHandler handler = new SqlServerEscapeHandler();

    @Test
    void checkIfKeyWordsIgnoresCase() {
        assertTrue(handler.checkIfKeyWords("SELECT"));
        assertTrue(handler.checkIfKeyWords("select"));
        assertTrue(handler.checkIfKeyWords("Select"));
        assertFalse(handler.checkIfKeyWords("username"));
        assertFalse(handler.checkIfKeyWords(null));
    }

    @Test
    void endIsTreatedAsKeyword() {
        assertTrue(handler.checkIfKeyWords("END"));
        assertTrue(handler.checkIfKeyWords("end"));
        assertTrue(handler.checkIfNeedEscape("end", null));
    }

    @Test
    void checkIfNeedEscapeOnlyForKeywords() {
        assertTrue(handler.checkIfNeedEscape("order", null));
        assertFalse(handler.checkIfNeedEscape("id", null));
        assertFalse(handler.checkIfNeedEscape("Name", null));
    }

    @Test
    void usesSquareBrackets() {
        assertEquals('[', handler.getEscapeSymbol().getLeftSymbol());
        assertEquals(']', handler.getEscapeSymbol().getRightSymbol());
        assertEquals("[order]", handler.addColNameEscape("order"));
        assertEquals("[end]", handler.addColNameEscape("end"));
        assertEquals("id", handler.addColNameEscape("id"));
        assertEquals("[dbo].[order]", handler.addColNameEscape("dbo.order"));
        assertEquals("order", handler.delColNameEscape("[order]"));
        assertEquals("dbo.order", handler.delColNameEscape("[dbo].[order]"));
    }

    @Test
    void containsEscapeDetectsBracketedNames() {
        assertTrue(handler.containsEscape("[order]"));
        assertFalse(handler.containsEscape("order"));
        assertFalse(handler.containsEscape("\"order\""));
    }

    @Test
    void columnUtilsEscapesEndForSqlServer() {
        assertEquals("[end]", ColumnUtils.addEscape("end", JdbcConstants.SQLSERVER));
        assertEquals("[END]", ColumnUtils.addEscape("END", JdbcConstants.SQLSERVER));
    }
}
