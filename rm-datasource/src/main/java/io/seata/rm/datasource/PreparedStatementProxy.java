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
package io.seata.rm.datasource;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

import io.seata.rm.datasource.exec.ExecuteTemplate;
import io.seata.sqlparser.ParametersHolder;

/**
 * The type Prepared statement proxy.
 *
 * @author sharajava
 */
public class PreparedStatementProxy extends AbstractPreparedStatementProxy
    implements PreparedStatement, ParametersHolder {

    @Override
    public Map<Integer,ArrayList<Object>> getParameters() {
        return parameters;
    }

    /**
     * Instantiates a new Prepared statement proxy.
     *
     * @param connectionProxy the connection proxy
     * @param targetStatement the target statement
     * @param targetSQL       the target sql
     * @throws SQLException the sql exception
     */
    public PreparedStatementProxy(AbstractConnectionProxy connectionProxy, PreparedStatement targetStatement,
                                  String targetSQL) throws SQLException {
        super(connectionProxy, targetStatement, targetSQL);
    }

    @Override
    public boolean execute() throws SQLException {
        return ExecuteTemplate.execute(this, (statement, args) -> statement.execute());
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        return ExecuteTemplate.execute(this, (statement, args) -> statement.executeQuery());
    }

    @Override
    public int executeUpdate() throws SQLException {
        return ExecuteTemplate.execute(this, (statement, args) -> statement.executeUpdate());
    }
}
