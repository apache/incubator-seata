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

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.sqlparser.SqlParserType;
import io.seata.sqlparser.util.DbTypeParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author ggndnn
 */
public class DruidDbTypeParserTest {
    @Test
    public void testDruidDbTypeParserLoading() {
        String jdbcUrl = "jdbc:mysql://127.0.0.1:3306/seata";
        DruidDelegatingDbTypeParser dbTypeParser = (DruidDelegatingDbTypeParser) EnhancedServiceLoader.load(DbTypeParser.class, SqlParserType.SQL_PARSER_TYPE_DRUID);
        Assertions.assertNotNull(dbTypeParser);
        Assertions.assertEquals(DruidDelegatingDbTypeParser.class, dbTypeParser.getClass());
        String dbType = dbTypeParser.parseFromJdbcUrl(jdbcUrl);
        Assertions.assertEquals("mysql", dbType);

        DruidLoader druidLoaderForTest = new DruidLoaderForTest();
        dbTypeParser.setClassLoader(new DruidIsolationClassLoader(druidLoaderForTest));
        Assertions.assertThrows(NoClassDefFoundError.class, () -> dbTypeParser.parseFromJdbcUrl(jdbcUrl));
    }
}
