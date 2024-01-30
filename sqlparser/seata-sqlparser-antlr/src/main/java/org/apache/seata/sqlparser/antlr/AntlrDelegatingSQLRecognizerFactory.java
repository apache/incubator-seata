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
package org.apache.seata.sqlparser.antlr;

import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.sqlparser.SQLParsingException;
import org.apache.seata.sqlparser.SQLRecognizer;
import org.apache.seata.sqlparser.SQLRecognizerFactory;
import org.apache.seata.sqlparser.SqlParserType;

import java.lang.reflect.Constructor;
import java.util.List;


@LoadLevel(name = SqlParserType.SQL_PARSER_TYPE_ANTLR)
public class AntlrDelegatingSQLRecognizerFactory implements SQLRecognizerFactory {

    private volatile SQLRecognizerFactory recognizerFactoryImpl;

    public AntlrDelegatingSQLRecognizerFactory() {
        setClassLoader();
    }

    /**
     * Only for unit test
     *
     */
    void setClassLoader() {
        try {
            Class<?> recognizerFactoryImplClass = ClassLoader.getSystemClassLoader().loadClass("org.apache.seata.sqlparser.antlr.mysql.AntlrMySQLRecognizerFactory");
            Constructor<?> implConstructor = recognizerFactoryImplClass.getDeclaredConstructor();
            implConstructor.setAccessible(true);
            try {
                recognizerFactoryImpl = (SQLRecognizerFactory) implConstructor.newInstance();
            } finally {
                implConstructor.setAccessible(false);
            }
        } catch (Exception e) {
            throw new SQLParsingException(e);
        }
    }

    @Override
    public List<SQLRecognizer> create(String sql, String dbType) {
        return recognizerFactoryImpl.create(sql, dbType);
    }
}
