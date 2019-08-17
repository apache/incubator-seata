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
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.alibaba.druid.mock.MockStatementBase;
import com.alibaba.druid.util.jdbc.ResultSetMetaDataBase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock driver
 * @author will
 * @date 2019/8/14
 */
public class MockDriver extends com.alibaba.druid.mock.MockDriver {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockDriver.class);

    private Object[][] columnMetas = null;

    private Object[][] indexMetas = null;

    public MockDriver(Object[][] columnMetas, Object[][] indexMetas) {
        this.columnMetas = columnMetas;
        this.indexMetas = indexMetas;
    }

    /**
     * Instantiate a new MockConnection
     * @param driver
     * @param url
     * @param connectProperties
     * @return
     */
    @Override
    public MockConnection createMockConnection(com.alibaba.druid.mock.MockDriver driver, String url,
                                               Properties connectProperties) {
        return new MockConnection(this, url, connectProperties);
    }

    /**
     * mock column meta result set
     * @param stmt
     * @return
     */
    public MockResultSet mockColumnMetaResultSet(MockStatementBase stmt, List<String> labels){
        return mockResultSet(stmt, labels, columnMetas);
    }

    /**
     * mock index meta result set
     * @param stmt
     * @return
     */
    public MockResultSet mockIndexMetaResultSet(MockStatementBase stmt, List<String> labels){
        return mockResultSet(stmt, labels, indexMetas);
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
            LOGGER.error(e.getMessage());
        }
        for (String columnLabel : labels) {
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
    public ResultSet executeQuery(MockStatementBase stmt, String sql) throws SQLException {
        return super.executeQuery(stmt, sql);
    }



    /**
     * customize the column metas
     * @param columnMetas
     */
    public void setColumnMetas(Object[][] columnMetas) {
        this.columnMetas = columnMetas;
    }

    /**
     * customize the index metas
     * @param indexMetas
     */
    public void setIndexMetas(Object[][] indexMetas) {
        this.indexMetas = indexMetas;
    }
}
