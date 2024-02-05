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

import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.sqlparser.SqlParserType;
import org.apache.seata.sqlparser.util.DbTypeParser;

import java.lang.reflect.Constructor;


@LoadLevel(name = SqlParserType.SQL_PARSER_TYPE_DRUID)
public class DruidDelegatingDbTypeParser implements DbTypeParser {
    private DbTypeParser dbTypeParserImpl;

    public DruidDelegatingDbTypeParser() {
        setClassLoader(DruidIsolationClassLoader.get());
    }

    /**
     * Only for unit test
     *
     * @param classLoader classLoader
     */
    void setClassLoader(ClassLoader classLoader) {
        try {
            Class<?> druidDbTypeParserImplClass = classLoader.loadClass("org.apache.seata.sqlparser.druid.DruidDbTypeParserImpl");
            Constructor<?> implConstructor = druidDbTypeParserImplClass.getDeclaredConstructor();
            implConstructor.setAccessible(true);
            try {
                dbTypeParserImpl = (DbTypeParser) implConstructor.newInstance();
            } finally {
                implConstructor.setAccessible(false);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String parseFromJdbcUrl(String jdbcUrl) {
        return dbTypeParserImpl.parseFromJdbcUrl(jdbcUrl);
    }
}
