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
package io.seata.sqlparser.antlr;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLRecognizerFactory;
import io.seata.sqlparser.SqlParserType;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author zhihou
 */
public class AntlrIsolationTest {

    String TEST_SQL = "SELECT name,phone FROM t1 WHERE id = 1 and username = '11' and age = 'a' or hz = '1' or aa = 1 FOR UPDATE";

    @Test
    public void testAntlrIsolation() {
        AntlrDelegatingSQLRecognizerFactory recognizerFactory = (AntlrDelegatingSQLRecognizerFactory) EnhancedServiceLoader.load(SQLRecognizerFactory.class, SqlParserType.SQL_PARSER_TYPE_ANTLR);
        List<SQLRecognizer> sqlRecognizer = recognizerFactory.create(TEST_SQL, JdbcConstants.MYSQL);
        Assertions.assertNotNull(sqlRecognizer);
    }

}
