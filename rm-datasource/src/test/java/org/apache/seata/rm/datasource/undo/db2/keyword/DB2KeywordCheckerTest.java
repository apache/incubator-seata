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
package org.apache.seata.rm.datasource.undo.db2.keyword;

import org.apache.seata.sqlparser.EscapeHandler;
import org.apache.seata.sqlparser.EscapeHandlerFactory;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author GoodBoyCoder
 * @date 2021-10-25
 */
public class DB2KeywordCheckerTest {
    @Test
    public void testDB2KeywordChecker() {
        EscapeHandler keywordChecker = EscapeHandlerFactory.getEscapeHandler(JdbcConstants.DB2);
        Assertions.assertNotNull(keywordChecker);
    }
}
