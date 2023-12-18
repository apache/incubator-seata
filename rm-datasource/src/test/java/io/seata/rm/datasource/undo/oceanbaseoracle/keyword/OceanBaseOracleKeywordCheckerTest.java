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
package io.seata.rm.datasource.undo.oceanbaseoracle.keyword;


import io.seata.sqlparser.KeywordChecker;
import io.seata.sqlparser.KeywordCheckerFactory;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test cases for keyword checker of OceanBaseOracle
 *
 * @author hsien999
 */
public class OceanBaseOracleKeywordCheckerTest {
    @Test
    public void testOracleKeywordChecker() {
        KeywordChecker keywordChecker = KeywordCheckerFactory.getKeywordChecker(JdbcConstants.OCEANBASE_ORACLE);
        Assertions.assertNotNull(keywordChecker);
        Assertions.assertTrue(keywordChecker.check("dual"));
        Assertions.assertTrue(keywordChecker.check("Dual"));
        Assertions.assertTrue(keywordChecker.check("DUAL"));
        Assertions.assertFalse(keywordChecker.check("id"));
    }
}
