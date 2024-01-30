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
package org.apache.seata.rm.datasource.sql;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.apache.seata.sqlparser.SQLRecognizer;
import org.apache.seata.sqlparser.SQLRecognizerFactory;
import org.apache.seata.sqlparser.SqlParserType;

import java.util.List;


public class SQLVisitorFactory {
    /**
     * SQLRecognizerFactory.
     */
    private final static SQLRecognizerFactory SQL_RECOGNIZER_FACTORY;

    static {
        String sqlParserType = ConfigurationFactory.getInstance().getConfig(ConfigurationKeys.SQL_PARSER_TYPE, SqlParserType.SQL_PARSER_TYPE_DRUID);
        SQL_RECOGNIZER_FACTORY = EnhancedServiceLoader.load(SQLRecognizerFactory.class, sqlParserType);
    }

    /**
     * Get sql recognizer.
     *
     * @param sql    the sql
     * @param dbType the db type
     * @return the sql recognizer
     */
    public static List<SQLRecognizer> get(String sql, String dbType) {
        return SQL_RECOGNIZER_FACTORY.create(sql, dbType);
    }


}
