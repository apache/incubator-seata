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
package io.seata.rm.datasource.undo.oracle.keyword;

import com.alibaba.druid.util.JdbcConstants;

import io.seata.rm.datasource.undo.KeywordChecker;
import io.seata.rm.datasource.undo.KeywordCheckerFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author will
 */
public class OracleKeywordCheckerTest {

    @Test
    public void testOracleKeywordChecker() {
        KeywordChecker keywordChecker = KeywordCheckerFactory.getKeywordChecker(JdbcConstants.ORACLE);
        Assertions.assertNotNull(keywordChecker);
    }

    @Test
    public void testCheckAndReplate() {
        KeywordChecker keywordChecker = KeywordCheckerFactory.getKeywordChecker(JdbcConstants.ORACLE);
        Assertions.assertEquals(null, keywordChecker.checkAndReplace(null));
        Assertions.assertEquals("undo_log", keywordChecker.checkAndReplace("undo_log"));
        Assertions.assertEquals("TABLE", keywordChecker.checkAndReplace("TABLE"));
    }
}
