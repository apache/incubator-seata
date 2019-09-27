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

import com.alibaba.druid.util.jdbc.ResultSetBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * @author will
 * @date 2019/8/14
 */
public class MockResultSet extends ResultSetBase {

    private List<String> columnLabels;

    private int rowIndex = -1;

    private List<Object[]> rows;

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
        this.rows = new ArrayList<Object[]>();
        super.metaData = new MockResultSetMetaData();
    }

    /**
     * mock result set
     * @param metas
     * @return
     */
    public MockResultSet mockResultSet(Object[][] metas){
        if(metas.length < 1){
            return this;
        }

        for (Object[] columnMeta : metas) {
            this.getRows().add(columnMeta);
        }

        return this;
    }

    public MockResultSetMetaData getMockMetaData() {
        return (MockResultSetMetaData) metaData;
    }

    @Override
    public boolean next() throws SQLException {
        if (closed) {
            throw new SQLException();
        }

        if (rowIndex < rows.size() - 1) {
            rowIndex++;
            return true;
        }
        return false;
    }

    @Override
    public int findColumn(String columnLabel) throws SQLException {
        return columnLabels.indexOf(columnLabel) + 1;
    }

    public Object getObjectInternal(int columnIndex) {
        Object[] row = rows.get(rowIndex);
        Object obj = row[columnIndex - 1];
        return obj;
    }

    @Override
    public boolean previous() throws SQLException {
        if (closed) {
            throw new SQLException();
        }

        if (rowIndex >= 0) {
            rowIndex--;
            return true;
        }
        return false;
    }

    @Override
    public void updateObject(int columnIndex, Object x) throws SQLException {
        Object[] row = rows.get(rowIndex);
        row[columnIndex - 1] = x;
    }

    public List<Object[]> getRows() {
        return rows;
    }
}
