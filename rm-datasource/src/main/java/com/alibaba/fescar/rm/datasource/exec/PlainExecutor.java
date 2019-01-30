/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.rm.datasource.exec;

import java.sql.Statement;

import com.alibaba.fescar.rm.datasource.StatementProxy;

/**
 * The type Plain executor.
 *
 * @param <T> the type parameter
 * @param <S> the type parameter
 */
public class PlainExecutor<T, S extends Statement> implements Executor {

    private StatementProxy<S> statementProxy;

    private StatementCallback<T, S> statementCallback;

    /**
     * Instantiates a new Plain executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     */
    public PlainExecutor(StatementProxy<S> statementProxy, StatementCallback<T, S> statementCallback) {
        this.statementProxy = statementProxy;
        this.statementCallback = statementCallback;
    }

    @Override
    public T execute(Object... args) throws Throwable {
        return statementCallback.execute(statementProxy.getTargetStatement(), args);
    }
}
