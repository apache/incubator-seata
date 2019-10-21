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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import com.alibaba.druid.mock.MockStatementBase;
import com.alibaba.druid.mock.handler.MockExecuteHandler;

/**
  * @author will
  * @date 2019/8/16
  */
public class MockExecuteHandlerImpl implements MockExecuteHandler {

    /**
     * the mock value of return value
     */
    private Object[][] mockReturnValue;

    /**
     * the mock column labels of return value
     */
    private List<String> mockReturnValueColumnLabels;

    /**
     * Instantiate MockExecuteHandlerImpl
     * @param mockReturnValue
     */
    public MockExecuteHandlerImpl(List<String> mockReturnValueColumnLabels, Object[][] mockReturnValue) {
        this.mockReturnValueColumnLabels = mockReturnValueColumnLabels;
        this.mockReturnValue = mockReturnValue;
    }

    @Override
    public ResultSet executeQuery(MockStatementBase statement, String sql) throws SQLException {
        MockResultSet resultSet = new MockResultSet(statement);

        //mock the return value
        resultSet.mockResultSet(mockReturnValueColumnLabels, mockReturnValue);

        return resultSet;
    }
}
