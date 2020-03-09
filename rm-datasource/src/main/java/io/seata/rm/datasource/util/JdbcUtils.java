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
package io.seata.rm.datasource.util;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.sqlparser.SqlParserType;
import io.seata.sqlparser.util.DbTypeParser;

/**
 * @author ggndnn
 */
public final class JdbcUtils {
    private static volatile DbTypeParser dbTypeParser;

    private JdbcUtils() {
    }

    public static String getDbType(String jdbcUrl) {
        return getDbTypeParser().parseFromJdbcUrl(jdbcUrl).toLowerCase();
    }

    static DbTypeParser getDbTypeParser() {
        if (dbTypeParser == null) {
            synchronized (JdbcUtils.class) {
                if (dbTypeParser == null) {
                    String sqlparserType = ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.SQL_PARSER_TYPE, SqlParserType.SQL_PARSER_TYPE_DRUID);
                    dbTypeParser = EnhancedServiceLoader.load(DbTypeParser.class, sqlparserType);
                }
            }
        }
        return dbTypeParser;
    }
}
