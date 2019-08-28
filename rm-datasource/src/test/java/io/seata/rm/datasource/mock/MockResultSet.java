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

import com.alibaba.druid.mock.MockStatementBase;
import com.alibaba.druid.util.jdbc.ResultSetMetaDataBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * @author will
 * @date 2019/8/14
 */
public class MockResultSet extends com.alibaba.druid.mock.MockResultSet {

    private List<String> columnLabels;

    private static final Logger LOGGER = LoggerFactory.getLogger(MockResultSet.class);

    public MockResultSet(Statement statement) {
        this(statement, null);
    }

    /**
     * Instantiates a new Mock result set.
     *
     * @param statement    the statement
     * @param columnLabels the column labels
     */
    public MockResultSet(Statement statement, List<String> columnLabels) {
        super(statement);
        this.columnLabels = columnLabels;
    }

    /**
     * mock result set
     * @param stmt
     * @param labels
     * @return
     */
    public MockResultSet mockResultSet(MockStatementBase stmt, List<String> labels, Object[][] metas){
        if(metas.length < 1){
            LOGGER.error("please initialize the column meta and index meta");
            throw new RuntimeException("please initialize the column meta and index meta");
        }
        MockResultSet resultSet = new MockResultSet(stmt, labels);

        List<ResultSetMetaDataBase.ColumnMetaData> columns = null;
        try {
            columns = resultSet.getMockMetaData().getColumns();
        } catch (SQLException e) {
            LOGGER.error("Could not get columns:{}", e.getMessage(), e);
        }
        for (String columnLabel : columnLabels) {
            ResultSetMetaDataBase.ColumnMetaData column = new ResultSetMetaDataBase.ColumnMetaData();
            column.setColumnName(columnLabel);
            columns.add(column);
        }

        for (Object[] columnMeta : metas) {
            resultSet.getRows().add(columnMeta);
        }

        return resultSet;
    }

    @Override
    public int findColumn(String columnLabel) throws SQLException {
        return columnLabels.indexOf(columnLabel) + 1;
    }
}
