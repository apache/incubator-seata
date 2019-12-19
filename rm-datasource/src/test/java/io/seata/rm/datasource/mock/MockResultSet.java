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

import com.google.common.collect.Lists;
import io.seata.rm.datasource.sql.struct.ColumnMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * @author will
 */
public class MockResultSet extends ResultSetBase {

    private List<ColumnMeta> columnMetas;

    private int rowIndex = -1;

    /**
     * the column label
     */
    private List<String> columnLabels;

    /**
     * the return value
     */
    private List<Object[]> rows;

    private static final Logger LOGGER = LoggerFactory.getLogger(MockResultSet.class);

    /**
     * Instantiates a new Mock result set.
     * @param statement
     */
    public MockResultSet(Statement statement) {
        super(statement);
        this.rows = new ArrayList<>();
        this.columnMetas = Lists.newArrayList();
    }

    /**
     * mock result set
     * @param mockColumnLabels
     * @param mockReturnValue
     * @return
     */
    public MockResultSet mockResultSet(List<String> mockColumnLabels, Object[][] mockReturnValue){
        this.columnLabels = mockColumnLabels;
        for (int i = 0; i < mockReturnValue.length; i++) {
            Object[] row = mockReturnValue[i];
            this.getRows().add(row);
        }
        return this;
    }

    public void mockResultSetMetaData(Object[][] mockColumnsMetasReturnValue) {
        for (Object[] meta : mockColumnsMetasReturnValue) {
            ColumnMeta columnMeta = new ColumnMeta();
            columnMeta.setTableName(meta[2].toString());
            columnMeta.setColumnName(meta[3].toString());
            this.columnMetas.add(columnMeta);
        }
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return new MockResultSetMetaData(columnMetas);
    }

    public MockResultSetMetaData getMockMetaData() {
        return new MockResultSetMetaData(columnMetas);
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

    @Override
    public Blob getBlob(String columnLabel) throws SQLException {
        return getBlob(findColumn(columnLabel));
    }

    @Override
    public Blob getBlob(int columnIndex) throws SQLException {
        byte[] bytes = getObjectInternal(columnIndex).toString().getBytes();
        return new MockBlob();
    }

    @Override
    public Clob getClob(String columnLabel) throws SQLException {
        return getClob(findColumn(columnLabel));
    }

    @Override
    public Clob getClob(int columnIndex) throws SQLException {
        char[] chars = getObjectInternal(columnIndex).toString().toCharArray();
        return new MockClob();
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
