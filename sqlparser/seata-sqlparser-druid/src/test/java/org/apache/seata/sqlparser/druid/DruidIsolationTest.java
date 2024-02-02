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
package org.apache.seata.sqlparser.druid;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.sqlparser.SQLRecognizer;
import org.apache.seata.sqlparser.SQLRecognizerFactory;
import org.apache.seata.sqlparser.SqlParserType;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


public class DruidIsolationTest {
    private final static String TEST_SQL = "insert into t_table_1 values(?, ?)";

    @Test
    public void testDruidIsolation() throws Exception {
        DruidDelegatingSQLRecognizerFactory recognizerFactory = (DruidDelegatingSQLRecognizerFactory) EnhancedServiceLoader.load(SQLRecognizerFactory.class, SqlParserType.SQL_PARSER_TYPE_DRUID);
        Assertions.assertNotNull(recognizerFactory);
        List<SQLRecognizer> sqlRecognizer = recognizerFactory.create(TEST_SQL, JdbcConstants.MYSQL);
        Assertions.assertNotNull(sqlRecognizer);
        DruidLoader druidLoaderForTest = new DruidLoaderForTest();
        recognizerFactory.setClassLoader(new DruidIsolationClassLoader(druidLoaderForTest));
        // because druid-test.jar not exists, so NoClassDefFoundError should be threw
        Assertions.assertThrows(NoClassDefFoundError.class, () -> recognizerFactory.create(TEST_SQL, JdbcConstants.MYSQL));
    }

    @AfterAll
    public static void afterClass(){
        DruidDelegatingSQLRecognizerFactory recognizerFactory = (DruidDelegatingSQLRecognizerFactory) EnhancedServiceLoader.load(SQLRecognizerFactory.class,
                               SqlParserType.SQL_PARSER_TYPE_DRUID);
        recognizerFactory.setClassLoader(DruidIsolationTest.class.getClassLoader());
    }
}
