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

import java.sql.SQLException;
import java.sql.Statement;

/**
 * The interface Statement callback.
 *
 * @param <T> the type parameter
 * @param <S> the type parameter
 */
public interface StatementCallback<T, S extends Statement> {

    /**
     * Execute t.
     *
     * @param statement the statement
     * @param args      the args
     * @return the t
     * @throws SQLException the sql exception
     */
    T execute(S statement, Object... args) throws SQLException;
}
