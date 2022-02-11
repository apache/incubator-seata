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
package io.seata.sqlparser;

import io.seata.common.ConfigurationKeys;
import io.seata.common.loader.EnhancedServiceNotFoundException;
import io.seata.common.util.ReflectionUtil;
import io.seata.config.ConfigurationFactory;

/**
 * The sql parser configuration factory
 *
 * @author wang.liang
 */
public final class SqlParserConfigurationFactory {

    private SqlParserConfigurationFactory() {
    }


    private static final String ONE_ANTLR_CLASS_NAME = "io.seata.sqlparser.antlr.SQLOperateRecognizerHolder";


    /**
     * Get sql parser type
     *
     * @return the sql parser type
     * @throws EnhancedServiceNotFoundException the enhanced service not found exception
     */
    public static String getSqlParserType() {
        String sqlParserType = ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.SQL_PARSER_TYPE, SqlParserType.SQL_PARSER_TYPE_DRUID);
        if (SqlParserType.SQL_PARSER_TYPE_ANTLR.equalsIgnoreCase(sqlParserType)) {
            try {
                ReflectionUtil.getClassByName(ONE_ANTLR_CLASS_NAME);
            } catch (ClassNotFoundException e) {
                throw new EnhancedServiceNotFoundException("Cannot find SQL parser for 'ANTLR'. " +
                        "Please manually reference 'io.seata:seata-sqlparser-antlr' dependency ", e);
            }
        }
        return sqlParserType;
    }
}
