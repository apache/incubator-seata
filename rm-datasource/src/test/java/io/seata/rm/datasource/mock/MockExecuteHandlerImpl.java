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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import io.seata.sqlparser.druid.mysql.MySQLSelectForUpdateRecognizer;
import io.seata.sqlparser.util.JdbcConstants;
import com.alibaba.druid.mock.MockStatementBase;
import com.alibaba.druid.mock.handler.MockExecuteHandler;

/**
 * @author will
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
     * the mock column meta
     */
    private Object[][] mockColumnsMetasReturnValue;

    /**
     * Instantiate MockExecuteHandlerImpl
     * @param mockReturnValue
     */
    public MockExecuteHandlerImpl(List<String> mockReturnValueColumnLabels, Object[][] mockReturnValue, Object[][] mockColumnsMetasReturnValue) {
        this.mockReturnValueColumnLabels = mockReturnValueColumnLabels;
        this.mockReturnValue = mockReturnValue;
        this.mockColumnsMetasReturnValue = mockColumnsMetasReturnValue;
    }

    @Override
    public ResultSet executeQuery(MockStatementBase statement, String sql) throws SQLException {
        MockResultSet resultSet = new MockResultSet(statement);
        //mock the return value
        resultSet.mockResultSet(mockReturnValueColumnLabels, mockReturnValue);
        //mock the rs meta data
        List<SQLStatement> asts = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        List<Object[]> metas = new ArrayList<>();
        if(asts.get(0) instanceof SQLSelectStatement) {
            SQLSelectStatement ast = (SQLSelectStatement) asts.get(0);
            SQLSelectQueryBlock queryBlock = ast.getSelect().getQueryBlock();
            String tableName = "";
            if (queryBlock.getFrom() instanceof SQLExprTableSource) {
                MySQLSelectForUpdateRecognizer recognizer = new MySQLSelectForUpdateRecognizer(sql, ast);
                tableName = recognizer.getTableName();
            } else {
                //select * from t inner join t1...
                tableName = queryBlock.getFrom().toString();
            }
            for (Object[] meta : mockColumnsMetasReturnValue) {
                if (tableName.equalsIgnoreCase(meta[2].toString())) {
                    metas.add(meta);
                }
            }
        }
        if(metas.isEmpty()){
            //eg:select * from dual
            metas = Arrays.asList(mockColumnsMetasReturnValue);
        }
        resultSet.mockResultSetMetaData(metas.toArray(new Object[0][]));
        return resultSet;
    }
}
