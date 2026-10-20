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
package org.apache.seata.rm.datasource.sql.handler.oceanbase;

import org.apache.seata.sqlparser.struct.ColumnMeta;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OceanBaseEscapeHandlerBehaviorTest {
    @Test
    void escapesKeywordsAndCaseSensitiveIdentifiersOnlyOnce() {
        OceanBaseEscapeHandler handler = new OceanBaseEscapeHandler();
        assertTrue(handler.checkIfKeyWords("SELECT"));
        assertTrue(handler.checkIfKeyWords("select"));
        assertFalse(handler.checkIfKeyWords(null));
        assertFalse(handler.checkIfNeedEscape(null, null));
        assertFalse(handler.checkIfNeedEscape(" ", null));
        assertFalse(handler.checkIfNeedEscape("\"select\"", null));
        assertTrue(handler.checkIfNeedEscape(" select ", null));
        assertFalse(handler.checkIfNeedEscape("ORDERS", null));
        assertTrue(handler.checkIfNeedEscape("Orders", null));
        TableMeta meta = new TableMeta();
        ColumnMeta column = new ColumnMeta();
        column.setColumnName("ID");
        column.setCaseSensitive(false);
        meta.getAllColumns().put("ID", column);
        assertFalse(handler.checkIfNeedEscape("ID", meta));
        column.setCaseSensitive(true);
        assertTrue(handler.checkIfNeedEscape("ID", meta));
        assertTrue(handler.checkIfNeedEscape("MISSING", meta));
    }
}
