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
package io.seata.rm.datasource.mock;

import java.sql.ParameterMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.alibaba.druid.mock.MockStatementBase;
import com.alibaba.druid.util.jdbc.PreparedStatementBase;

/**
 * @author will
 */
public class MockPreparedStatement extends PreparedStatementBase implements MockStatementBase {

    private final String sql;

    private ParameterMetaData parameterMetaData;

    public MockPreparedStatement(MockConnection conn, String sql){
        super(conn);
        this.sql = sql;
        parameterMetaData = new MockParameterMetaData(sql);
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        MockConnection conn = getConnection();

        if (conn != null && conn.getDriver() != null) {
            return conn.getDriver().executeQuery(this, sql);
        }
        return null;
    }

    @Override
    public int executeUpdate() throws SQLException {
        return 0;
    }

    @Override
    public boolean execute() throws SQLException {
        return false;
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        return this.parameterMetaData;
    }

    public MockConnection getConnection() throws SQLException {
        return (MockConnection) super.getConnection();
    }
}
