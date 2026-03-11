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

package com.alibaba.fescar.rm.datasource;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import com.alibaba.fescar.rm.datasource.exec.ExecuteTemplate;
import com.alibaba.fescar.rm.datasource.exec.StatementCallback;

/**
 * The type Prepared statement proxy.
 */
public class PreparedStatementProxy extends AbstractPreparedStatementProxy implements PreparedStatement, ParametersHolder {

    @Override
    public ArrayList<Object>[] getParameters() {
        return parameters;
    }

    private void init() throws SQLException {
        int paramCount = targetStatement.getParameterMetaData().getParameterCount();
        this.parameters = new ArrayList[paramCount];
        for (int i = 0; i < paramCount; i++) {
            parameters[i] = new ArrayList<>();
        }
    }

    /**
     * Instantiates a new Prepared statement proxy.
     *
     * @param connectionProxy the connection proxy
     * @param targetStatement the target statement
     * @param targetSQL       the target sql
     * @throws SQLException the sql exception
     */
    public PreparedStatementProxy(AbstractConnectionProxy connectionProxy, PreparedStatement targetStatement, String targetSQL) throws SQLException {
        super(connectionProxy, targetStatement, targetSQL);
        init();
    }

    @Override
    public boolean execute() throws SQLException {
        return ExecuteTemplate.execute(this, new StatementCallback<Boolean, PreparedStatement>() {
            @Override
            public Boolean execute(PreparedStatement statement, Object... args) throws SQLException {
                return statement.execute();
            }
        });
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        return ExecuteTemplate.execute(this, new StatementCallback<ResultSet, PreparedStatement>() {
            @Override
            public ResultSet execute(PreparedStatement statement, Object... args) throws SQLException {
                return statement.executeQuery();
            }
        });
    }

    @Override
    public int executeUpdate() throws SQLException {
        return ExecuteTemplate.execute(this, new StatementCallback<Integer, PreparedStatement>() {
            @Override
            public Integer execute(PreparedStatement statement, Object... args) throws SQLException {
                return statement.executeUpdate();
            }
        });
    }
}
