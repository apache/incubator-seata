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
package io.seata.rm.datasource.exec;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.rm.datasource.StatementProxy;
import io.seata.sqlparser.SQLRecognizer;

import java.sql.Statement;

/**
 * @author jsbxyyx
 */
public class InsertExecutorFactory {

    /**
     * create insert executor
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizer     the sql recognizer
     * @param dbType            the db type
     * @param <T>               the statement
     * @param <S>               the statement
     * @return
     */
    public static <T, S extends Statement> Executor createInsertExecutor(StatementProxy<S> statementProxy,
                                                                         StatementCallback<T, S> statementCallback,
                                                                         SQLRecognizer sqlRecognizer,
                                                                         String dbType) {

        InsertExecutor executor = EnhancedServiceLoader.load(InsertExecutor.class, dbType,
                new Class[]{StatementProxy.class, StatementCallback.class, SQLRecognizer.class},
                new Object[]{statementProxy, statementCallback, sqlRecognizer});
        return executor;
    }

}
