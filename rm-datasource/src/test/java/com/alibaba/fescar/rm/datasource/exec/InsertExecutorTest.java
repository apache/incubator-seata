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

import com.alibaba.fescar.common.exception.NotSupportYetException;
import com.alibaba.fescar.common.exception.ShouldNeverHappenException;
import com.alibaba.fescar.rm.datasource.PreparedStatementProxy;
import com.alibaba.fescar.rm.datasource.sql.SQLInsertRecognizer;
import com.alibaba.fescar.rm.datasource.sql.struct.*;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.anyString;

/**
 * @author guoyao
 * @date 2019/3/21
 */
public class InsertExecutorTest {

    private static final String ID_COLUMN = "id";
    private static final String USER_ID_COLUMN = "user_id";
    private static final String USER_NAME_COLUMN = "user_name";
    private static final Integer PK_VALUE = 100;

    private PreparedStatementProxy statementProxy;

    private SQLInsertRecognizer sqlInsertRecognizer;

    private StatementCallback statementCallback;

    private TableMeta tableMeta;

    private InsertExecutor insertExecutor;

    @Before
    public void init() {
        statementProxy = mock(PreparedStatementProxy.class);
        statementCallback = mock(StatementCallback.class);
        sqlInsertRecognizer = mock(SQLInsertRecognizer.class);
        tableMeta =  mock(TableMeta.class);
        insertExecutor = spy(new InsertExecutor(statementProxy, statementCallback, sqlInsertRecognizer));
    }

    @Test
    public void testBeforeImage() throws SQLException {
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        TableRecords tableRecords = insertExecutor.beforeImage();
        Assertions.assertThat(tableRecords.getRows()).isEmpty();
        Assertions.assertThat(tableRecords.size()).isEqualTo(0);
        try {
            tableRecords.add(new Row());
        } catch (Exception e) {
            Assertions.assertThat(e instanceof UnsupportedOperationException).isTrue();
        }
        try {
            tableRecords.getTableMeta();
        } catch (Exception e) {
            Assertions.assertThat(e instanceof UnsupportedOperationException).isTrue();
        }
    }

    @Test
    public void testAfterImage_ByColumn() throws SQLException {
        doReturn(true).when(insertExecutor).containsPK();
        List<Object> pkValues = new ArrayList<>();
        pkValues.add(PK_VALUE);
        doReturn(pkValues).when(insertExecutor).getPkValuesByColumn();
        TableRecords tableRecords = new TableRecords();
        doReturn(tableRecords).when(insertExecutor).getTableRecords(pkValues);
        TableRecords resultTableRecords = insertExecutor.afterImage(new TableRecords());
        Assertions.assertThat(resultTableRecords).isEqualTo(tableRecords);
    }

    @Test
    public void testAfterImage_ByAuto() throws SQLException {
        doReturn(false).when(insertExecutor).containsPK();
        List<Object> pkValues = new ArrayList<>();
        pkValues.add(PK_VALUE);
        doReturn(pkValues).when(insertExecutor).getPkValuesByAuto();
        TableRecords tableRecords = new TableRecords();
        doReturn(tableRecords).when(insertExecutor).getTableRecords(pkValues);
        TableRecords resultTableRecords = insertExecutor.afterImage(new TableRecords());
        Assertions.assertThat(resultTableRecords).isEqualTo(tableRecords);
    }

    @Test(expected = SQLException.class)
    public void testAfterImage_Exception() throws SQLException {
        doReturn(false).when(insertExecutor).containsPK();
        List<Object> pkValues = new ArrayList<>();
        pkValues.add(PK_VALUE);
        doReturn(pkValues).when(insertExecutor).getPkValuesByAuto();
        doReturn(null).when(insertExecutor).getTableRecords(pkValues);
        insertExecutor.afterImage(new TableRecords());
    }

    @Test
    public void testContainsPK() {
        List<String> insertColumns = mockInsertColumns();
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        when(tableMeta.containsPK(insertColumns)).thenReturn(true);
        Assertions.assertThat(insertExecutor.containsPK()).isTrue();
        when(tableMeta.containsPK(insertColumns)).thenReturn(false);
        Assertions.assertThat(insertExecutor.containsPK()).isFalse();
    }

    @Test
    public void testGetPkValuesByColumn() throws SQLException {
        mockInsertColumns();
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        when(tableMeta.getPkName()).thenReturn(ID_COLUMN);
        List<Object> pkValues = new ArrayList<>();
        pkValues.add(PK_VALUE);
        when(statementProxy.getParamsByIndex(0)).thenReturn(pkValues);
        List pkValuesByColumn=insertExecutor.getPkValuesByColumn();
        Assertions.assertThat(pkValuesByColumn).isEqualTo(pkValues);
    }

    @Test(expected = ShouldNeverHappenException.class)
    public void testGetPkValuesByColumn_Exception() throws SQLException {
        mockInsertColumns();
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        when(tableMeta.getPkName()).thenReturn(ID_COLUMN);
        when(statementProxy.getParamsByIndex(0)).thenReturn(null);
        insertExecutor.getPkValuesByColumn();
    }

    @Test
    public void testGetPkValuesByColumn_PkValue_Null() throws SQLException {
        mockInsertColumns();
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        when(tableMeta.getPkName()).thenReturn(ID_COLUMN);
        List<Object> pkValuesNull = new ArrayList<>();
        pkValuesNull.add(Null.get());
        when(statementProxy.getParamsByIndex(0)).thenReturn(pkValuesNull);
        List<Object> pkValuesAuto = new ArrayList<>();
        pkValuesAuto.add(PK_VALUE);
        //mock getPkValuesByAuto
        doReturn(pkValuesAuto).when(insertExecutor).getPkValuesByAuto();
        List pkValuesByColumn = insertExecutor.getPkValuesByColumn();
        //pk value = Null so getPkValuesByAuto
        verify(insertExecutor).getPkValuesByAuto();
        Assertions.assertThat(pkValuesByColumn).isEqualTo(pkValuesAuto);
    }

    @Test(expected = NotSupportYetException.class)
    public void testGetPkValuesByAuto_NotSupportYetException() throws SQLException {
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        Map<String, ColumnMeta> columnMetaMap = new HashMap<>();
        columnMetaMap.put(ID_COLUMN, new ColumnMeta());
        columnMetaMap.put(USER_ID_COLUMN, new ColumnMeta());
        when(tableMeta.getPrimaryKeyMap()).thenReturn(columnMetaMap);
        insertExecutor.getPkValuesByAuto();
    }

    @Test(expected = ShouldNeverHappenException.class)
    public void testGetPkValuesByAuto_ShouldNeverHappenException() throws SQLException {
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        Map<String, ColumnMeta> columnMetaMap = new HashMap<>();
        ColumnMeta columnMeta = mock(ColumnMeta.class);
        columnMetaMap.put(ID_COLUMN, columnMeta);
        when(columnMeta.isAutoincrement()).thenReturn(false);
        when(tableMeta.getPrimaryKeyMap()).thenReturn(columnMetaMap);
        insertExecutor.getPkValuesByAuto();
    }

    @Test(expected = SQLException.class)
    public void testGetPkValuesByAuto_SQLException() throws SQLException {
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        ColumnMeta columnMeta = mock(ColumnMeta.class);
        Map<String, ColumnMeta> columnMetaMap = new HashMap<>();
        columnMetaMap.put(ID_COLUMN, columnMeta);
        when(columnMeta.isAutoincrement()).thenReturn(true);
        when(tableMeta.getPrimaryKeyMap()).thenReturn(columnMetaMap);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        when(statementProxy.getTargetStatement()).thenReturn(preparedStatement);
        when(preparedStatement.getGeneratedKeys()).thenThrow(new SQLException());
        insertExecutor.getPkValuesByAuto();
    }

    @Test
    public void testGetPkValuesByAuto_GeneratedKeys_NoResult() throws SQLException {
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        ColumnMeta columnMeta = mock(ColumnMeta.class);
        Map<String, ColumnMeta> columnMetaMap = new HashMap<>();
        columnMetaMap.put(ID_COLUMN, columnMeta);
        when(columnMeta.isAutoincrement()).thenReturn(true);
        when(tableMeta.getPrimaryKeyMap()).thenReturn(columnMetaMap);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        when(statementProxy.getTargetStatement()).thenReturn(preparedStatement);
        ResultSet resultSet = mock(ResultSet.class);
        when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);
        when(resultSet.getObject(1)).thenReturn(PK_VALUE);
        List pkValuesByAuto = insertExecutor.getPkValuesByAuto();
        Assertions.assertThat(pkValuesByAuto).isEmpty();
    }

    @Test
    public void testGetPkValuesByAuto_GeneratedKeys_HasResult() throws SQLException {
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        ColumnMeta columnMeta = mock(ColumnMeta.class);
        Map<String, ColumnMeta> columnMetaMap = new HashMap<>();
        columnMetaMap.put(ID_COLUMN, columnMeta);
        when(columnMeta.isAutoincrement()).thenReturn(true);
        when(tableMeta.getPrimaryKeyMap()).thenReturn(columnMetaMap);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        when(statementProxy.getTargetStatement()).thenReturn(preparedStatement);
        ResultSet resultSet = mock(ResultSet.class);
        when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        when(resultSet.getObject(1)).thenReturn(PK_VALUE);
        List<Object> pkValues = new ArrayList<>();
        pkValues.add(PK_VALUE);
        List pkValuesByAuto = insertExecutor.getPkValuesByAuto();
        Assertions.assertThat(pkValuesByAuto).isEqualTo(pkValues);
    }

    @Test
    public void testGetPkValuesByAuto_ExecuteQuery_HasResult() throws SQLException {
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        ColumnMeta columnMeta = mock(ColumnMeta.class);
        Map<String, ColumnMeta> columnMetaMap = new HashMap<>();
        columnMetaMap.put(ID_COLUMN, columnMeta);
        when(columnMeta.isAutoincrement()).thenReturn(true);
        when(tableMeta.getPrimaryKeyMap()).thenReturn(columnMetaMap);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        when(statementProxy.getTargetStatement()).thenReturn(preparedStatement);
        when(preparedStatement.getGeneratedKeys()).thenThrow(new SQLException("", InsertExecutor.ERR_SQL_STATE));
        ResultSet resultSet = mock(ResultSet.class);
        when(preparedStatement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        when(resultSet.getObject(1)).thenReturn(PK_VALUE);
        List<Object> pkValues = new ArrayList<>();
        pkValues.add(PK_VALUE);
        List pkValuesByAuto = insertExecutor.getPkValuesByAuto();
        Assertions.assertThat(pkValuesByAuto).isEqualTo(pkValues);
    }

    private List<String> mockInsertColumns() {
        List<String> columns = new ArrayList<>();
        columns.add(ID_COLUMN);
        columns.add(USER_ID_COLUMN);
        columns.add(USER_NAME_COLUMN);
        when(sqlInsertRecognizer.getInsertColumns()).thenReturn(columns);
        return columns;
    }
}
