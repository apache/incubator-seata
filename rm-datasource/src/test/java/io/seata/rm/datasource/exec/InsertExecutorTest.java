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
package io.seata.rm.datasource.exec;

import com.alibaba.druid.util.JdbcConstants;
import io.seata.common.exception.NotSupportYetException;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.PreparedStatementProxy;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.sql.SQLInsertRecognizer;
import io.seata.rm.datasource.sql.struct.ColumnMeta;
import io.seata.rm.datasource.sql.struct.Null;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author guoyao
 * @date 2019/3/21
 */
public class InsertExecutorTest {

    private static final String ID_COLUMN = "id";
    private static final String USER_ID_COLUMN = "user_id";
    private static final String USER_NAME_COLUMN = "user_name";
    private static final String USER_STATUS_COLUMN = "user_status";
    private static final Integer PK_VALUE = 100;

    private ConnectionProxy connectionProxy;

    private StatementProxy statementProxy;

    private SQLInsertRecognizer sqlInsertRecognizer;

    private StatementCallback statementCallback;

    private TableMeta tableMeta;

    private InsertExecutor insertExecutor;

    @BeforeEach
    public void init() {
        statementProxy = mock(PreparedStatementProxy.class);
        statementCallback = mock(StatementCallback.class);
        sqlInsertRecognizer = mock(SQLInsertRecognizer.class);
        tableMeta = mock(TableMeta.class);
        insertExecutor = Mockito.spy(new InsertExecutor(statementProxy, statementCallback, sqlInsertRecognizer));

        connectionProxy = mock(ConnectionProxy.class);
        when(statementProxy.getConnectionProxy()).thenReturn(connectionProxy);
        when(connectionProxy.getDbType()).thenReturn(JdbcConstants.MYSQL);
    }

    @Test
    public void testBeforeImage() throws SQLException {
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        TableRecords tableRecords = insertExecutor.beforeImage();
        Assertions.assertEquals(tableRecords.size(), 0);
        try {
            tableRecords.add(new Row());
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof UnsupportedOperationException);
        }
        try {
            tableRecords.getTableMeta();
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof UnsupportedOperationException);
        }
    }

    @Test
    public void testAfterImage_ByColumn() throws SQLException {
        doReturn(true).when(insertExecutor).containsPK();
        List<Object> pkValues = new ArrayList<>();
        pkValues.add(PK_VALUE);
        doReturn(pkValues).when(insertExecutor).getPkValuesByColumn();
        TableRecords tableRecords = new TableRecords();
        doReturn(tableRecords).when(insertExecutor).buildTableRecords(pkValues);
        TableRecords resultTableRecords = insertExecutor.afterImage(new TableRecords());
        Assertions.assertEquals(resultTableRecords, tableRecords);
    }

    @Test
    public void testAfterImage_ByAuto() throws SQLException {
        doReturn(false).when(insertExecutor).containsPK();
        doReturn(true).when(insertExecutor).containsColumns();
        List<Object> pkValues = new ArrayList<>();
        pkValues.add(PK_VALUE);
        doReturn(pkValues).when(insertExecutor).getPkValuesByAuto();
        TableRecords tableRecords = new TableRecords();
        doReturn(tableRecords).when(insertExecutor).buildTableRecords(pkValues);
        TableRecords resultTableRecords = insertExecutor.afterImage(new TableRecords());
        Assertions.assertEquals(resultTableRecords, tableRecords);
    }

    @Test
    public void testAfterImage_Exception() {
        Assertions.assertThrows(SQLException.class, () -> {
            doReturn(false).when(insertExecutor).containsPK();
            doReturn(true).when(insertExecutor).containsColumns();
            List<Object> pkValues = new ArrayList<>();
            pkValues.add(PK_VALUE);
            doReturn(pkValues).when(insertExecutor).getPkValuesByAuto();
            doReturn(null).when(insertExecutor).buildTableRecords(pkValues);
            insertExecutor.afterImage(new TableRecords());
        });
    }

    @Test
    public void testContainsPK() {
        List<String> insertColumns = mockInsertColumns();
        mockInsertRows();
        mockParameters();
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        when(tableMeta.containsPK(insertColumns)).thenReturn(true);
        Assertions.assertTrue(insertExecutor.containsPK());
        when(tableMeta.containsPK(insertColumns)).thenReturn(false);
        Assertions.assertFalse(insertExecutor.containsPK());
    }

    @Test
    public void testGetPkValuesByColumn() throws SQLException {
        mockInsertColumns();
        mockInsertRows();
        mockParametersOfOnePk();
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        when(tableMeta.getPkName()).thenReturn(ID_COLUMN);
        List<Object> pkValues = new ArrayList<>();
        pkValues.add(PK_VALUE);
        List pkValuesByColumn = insertExecutor.getPkValuesByColumn();
        Assertions.assertEquals(pkValuesByColumn, pkValues);
    }

    @Test
    public void testGetPkValuesByColumn_Exception() {
        Assertions.assertThrows(ShouldNeverHappenException.class, () -> {
            mockInsertColumns();
            mockParameters();
            doReturn(tableMeta).when(insertExecutor).getTableMeta();
            when(tableMeta.getPkName()).thenReturn(ID_COLUMN);
            insertExecutor.getPkValuesByColumn();
        });
    }

    @Test
    public void testGetPkValuesByColumn_PkValue_Null() throws SQLException {
        mockInsertColumns();
        mockInsertRows();
        mockParametersPkWithNull();
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        when(tableMeta.getPkName()).thenReturn(ID_COLUMN);
        List<Object> pkValuesAuto = new ArrayList<>();
        pkValuesAuto.add(PK_VALUE);
        //mock getPkValuesByAuto
        doReturn(pkValuesAuto).when(insertExecutor).getPkValuesByAuto();
        List pkValuesByColumn = insertExecutor.getPkValuesByColumn();
        //pk value = Null so getPkValuesByAuto
        verify(insertExecutor).getPkValuesByAuto();
        Assertions.assertEquals(pkValuesByColumn, pkValuesAuto);
    }

    @Test
    public void testGetPkValuesByAuto_NotSupportYetException() {
        Assertions.assertThrows(NotSupportYetException.class, () -> {
            doReturn(tableMeta).when(insertExecutor).getTableMeta();
            Map<String, ColumnMeta> columnMetaMap = new HashMap<>();
            columnMetaMap.put(ID_COLUMN, new ColumnMeta());
            columnMetaMap.put(USER_ID_COLUMN, new ColumnMeta());
            when(tableMeta.getPrimaryKeyMap()).thenReturn(columnMetaMap);
            insertExecutor.getPkValuesByAuto();
        });
    }

    @Test
    public void testGetPkValuesByAuto_ShouldNeverHappenException() {
        Assertions.assertThrows(ShouldNeverHappenException.class, () -> {
            doReturn(tableMeta).when(insertExecutor).getTableMeta();
            Map<String, ColumnMeta> columnMetaMap = new HashMap<>();
            ColumnMeta columnMeta = mock(ColumnMeta.class);
            columnMetaMap.put(ID_COLUMN, columnMeta);
            when(columnMeta.isAutoincrement()).thenReturn(false);
            when(tableMeta.getPrimaryKeyMap()).thenReturn(columnMetaMap);
            insertExecutor.getPkValuesByAuto();
        });
    }

    @Test
    public void testGetPkValuesByAuto_SQLException() {
        Assertions.assertThrows(SQLException.class, () -> {
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
        });
    }

    @Test
    public void testGetPkValuesByAuto_SQLException_WarnLog() throws SQLException {
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        ColumnMeta columnMeta = mock(ColumnMeta.class);
        Map<String, ColumnMeta> columnMetaMap = new HashMap<>();
        columnMetaMap.put(ID_COLUMN, columnMeta);
        when(columnMeta.isAutoincrement()).thenReturn(true);
        when(tableMeta.getPrimaryKeyMap()).thenReturn(columnMetaMap);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        when(statementProxy.getTargetStatement()).thenReturn(preparedStatement);
        SQLException e = new SQLException("test warn log", InsertExecutor.ERR_SQL_STATE, 1);
        when(preparedStatement.getGeneratedKeys()).thenThrow(e);
        ResultSet genKeys = mock(ResultSet.class);
        when(statementProxy.getTargetStatement().executeQuery("SELECT LAST_INSERT_ID()")).thenReturn(genKeys);
        Assertions.assertTrue(insertExecutor.getPkValuesByAuto().isEmpty());
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
        Assertions.assertEquals(pkValuesByAuto.size(),0);
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
        Assertions.assertEquals(pkValuesByAuto, pkValues);
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
        Assertions.assertEquals(pkValuesByAuto, pkValues);
    }

    private List<String> mockInsertColumns() {
        List<String> columns = new ArrayList<>();
        columns.add(ID_COLUMN);
        columns.add(USER_ID_COLUMN);
        columns.add(USER_NAME_COLUMN);
        columns.add(USER_STATUS_COLUMN);
        when(sqlInsertRecognizer.getInsertColumns()).thenReturn(columns);
        return columns;
    }

    private void mockParameters() {
        ArrayList<Object>[] paramters = new ArrayList[4];
        ArrayList arrayList0 = new ArrayList<>();
        arrayList0.add(PK_VALUE);
        ArrayList arrayList1 = new ArrayList<>();
        arrayList1.add("userId1");
        ArrayList arrayList2 = new ArrayList<>();
        arrayList2.add("userName1");
        ArrayList arrayList3 = new ArrayList<>();
        arrayList3.add("userStatus1");
        paramters[0] = arrayList0;
        paramters[1] = arrayList1;
        paramters[2] = arrayList2;
        paramters[3] = arrayList3;
        PreparedStatementProxy psp = (PreparedStatementProxy) this.statementProxy;
        when(psp.getParameters()).thenReturn(paramters);
    }

    private void mockParametersPkWithNull() {
        ArrayList<Object>[] paramters = new ArrayList[4];
        ArrayList arrayList0 = new ArrayList<>();
        arrayList0.add(Null.get());
        ArrayList arrayList1 = new ArrayList<>();
        arrayList1.add("userId1");
        ArrayList arrayList2 = new ArrayList<>();
        arrayList2.add("userName1");
        ArrayList arrayList3 = new ArrayList<>();
        arrayList3.add("userStatus1");
        paramters[0] = arrayList0;
        paramters[1] = arrayList1;
        paramters[2] = arrayList2;
        paramters[3] = arrayList3;
        PreparedStatementProxy psp = (PreparedStatementProxy) this.statementProxy;
        when(psp.getParameters()).thenReturn(paramters);
    }

    private void mockParametersOfOnePk() {
        ArrayList<Object>[] paramters = new ArrayList[1];
        ArrayList arrayList1 = new ArrayList<>();
        arrayList1.add(PK_VALUE);
        paramters[0] = arrayList1;
        PreparedStatementProxy psp = (PreparedStatementProxy) this.statementProxy;
        when(psp.getParameters()).thenReturn(paramters);
    }

    private void mockInsertRows() {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(Arrays.asList("?", "?", "?", "?"));
        when(sqlInsertRecognizer.getInsertRows()).thenReturn(rows);
    }
}
