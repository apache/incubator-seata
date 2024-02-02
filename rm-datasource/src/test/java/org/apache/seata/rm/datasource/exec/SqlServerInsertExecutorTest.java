/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.rm.datasource.exec;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.seata.rm.datasource.exec.StatementCallback;
import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.rm.datasource.ConnectionProxy;
import org.apache.seata.rm.datasource.PreparedStatementProxy;
import org.apache.seata.rm.datasource.StatementProxy;
import org.apache.seata.rm.datasource.exec.sqlserver.SqlServerInsertExecutor;
import org.apache.seata.sqlparser.struct.ColumnMeta;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.apache.seata.sqlparser.SQLInsertRecognizer;
import org.apache.seata.sqlparser.struct.Null;
import org.apache.seata.sqlparser.struct.SqlSequenceExpr;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class SqlServerInsertExecutorTest {
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

    private SqlServerInsertExecutor insertExecutor;

    private final int pkIndex = 0;
    private HashMap<String, Integer> pkIndexMap;

    @BeforeEach
    public void init() {
        connectionProxy = mock(ConnectionProxy.class);
        when(connectionProxy.getDbType()).thenReturn(JdbcConstants.SQLSERVER);

        statementProxy = mock(PreparedStatementProxy.class);
        when(statementProxy.getConnectionProxy()).thenReturn(connectionProxy);

        statementCallback = mock(StatementCallback.class);
        sqlInsertRecognizer = mock(SQLInsertRecognizer.class);
        tableMeta = mock(TableMeta.class);
        insertExecutor = Mockito.spy(new SqlServerInsertExecutor(statementProxy, statementCallback, sqlInsertRecognizer));

        pkIndexMap = new HashMap<String, Integer>() {{
            put(ID_COLUMN, pkIndex);
        }};
    }

    @Test
    public void testPkValue_sequence() throws Exception {
        mockInsertColumns();
        SqlSequenceExpr expr = mockParametersPkWithSeq();
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        when(tableMeta.getPrimaryKeyOnlyName()).thenReturn(Arrays.asList(new String[]{ID_COLUMN}));
        List<Object> pkValuesSeq = new ArrayList<>();
        pkValuesSeq.add(PK_VALUE);

        doReturn(pkValuesSeq).when(insertExecutor).getPkValuesBySequence(expr);
        doReturn(pkIndexMap).when(insertExecutor).getPkIndex();

        Map<String, List<Object>> pkValuesByColumn = insertExecutor.getPkValuesByColumn();
        verify(insertExecutor).getPkValuesBySequence(expr);
        Assertions.assertEquals(pkValuesByColumn.get(ID_COLUMN), pkValuesSeq);
    }

    @Test
    public void testPkValue_auto() throws Exception {
        mockInsertColumns();
        mockParametersPkWithAuto();
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        when(tableMeta.getPrimaryKeyOnlyName()).thenReturn(Arrays.asList(new String[]{ID_COLUMN}));

        doReturn(Arrays.asList(new Object[]{PK_VALUE})).when(insertExecutor).getGeneratedKeys();
        Map<String, List<Object>> pkValuesByAuto = insertExecutor.getPkValues();

        verify(insertExecutor).getGeneratedKeys();
        Assertions.assertEquals(pkValuesByAuto.get(ID_COLUMN), Arrays.asList(new Object[]{PK_VALUE}));
    }

    @Test
    public void testStatement_pkValueByAuto_NotSupportYetException() throws Exception {
        mockInsertColumns();
        mockStatementInsertRows();

        statementProxy = mock(StatementProxy.class);
        when(statementProxy.getConnectionProxy()).thenReturn(connectionProxy);
        when(connectionProxy.getDbType()).thenReturn(JdbcConstants.ORACLE);

        insertExecutor = Mockito.spy(new SqlServerInsertExecutor(statementProxy, statementCallback, sqlInsertRecognizer));

        doReturn(tableMeta).when(insertExecutor).getTableMeta();

        Map<String, ColumnMeta> map = new HashMap<>();
        map.put(ID_COLUMN, mock(ColumnMeta.class));
        doReturn(map).when(tableMeta).getPrimaryKeyMap();

        ResultSet rs = mock(ResultSet.class);
        doReturn(rs).when(statementProxy).getGeneratedKeys();
        doReturn(false).when(rs).next();

        Assertions.assertThrows(NotSupportYetException.class, () -> insertExecutor.getGeneratedKeys());

        doReturn(pkIndexMap).when(insertExecutor).getPkIndex();

        Assertions.assertThrows(NotSupportYetException.class, () -> insertExecutor.getPkValuesByColumn());

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

    private SqlSequenceExpr mockParametersPkWithSeq() {
        SqlSequenceExpr expr = new SqlSequenceExpr("seq", "nextval");
        Map<Integer, ArrayList<Object>> parameters = new HashMap<>(4);
        ArrayList<Object> arrayList0 = new ArrayList<>();
        arrayList0.add(expr);
        ArrayList<Object> arrayList1 = new ArrayList<>();
        arrayList1.add("userId1");
        ArrayList<Object> arrayList2 = new ArrayList<>();
        arrayList2.add("userName1");
        ArrayList<Object> arrayList3 = new ArrayList<>();
        arrayList3.add("userStatus1");
        parameters.put(1, arrayList0);
        parameters.put(2, arrayList1);
        parameters.put(3, arrayList2);
        parameters.put(4, arrayList3);
        PreparedStatementProxy psp = (PreparedStatementProxy) this.statementProxy;
        when(psp.getParameters()).thenReturn(parameters);

        List<List<Object>> rows = new ArrayList<>();
        rows.add(Arrays.asList("?", "?", "?"));
        when(sqlInsertRecognizer.getInsertRows(pkIndexMap.values())).thenReturn(rows);

        return expr;
    }

    private void mockParametersPkWithAuto() {
        Map<Integer, ArrayList<Object>> parameters = new HashMap<>(4);
        ArrayList<Object> arrayList0 = new ArrayList<>();
        arrayList0.add(Null.get());
        ArrayList<Object> arrayList1 = new ArrayList<>();
        arrayList1.add("userId1");
        ArrayList<Object> arrayList2 = new ArrayList<>();
        arrayList2.add("userName1");
        ArrayList<Object> arrayList3 = new ArrayList<>();
        arrayList3.add("userStatus1");
        parameters.put(1, arrayList0);
        parameters.put(2, arrayList1);
        parameters.put(3, arrayList2);
        parameters.put(4, arrayList3);
        PreparedStatementProxy psp = (PreparedStatementProxy) this.statementProxy;
        when(psp.getParameters()).thenReturn(parameters);

        List<List<Object>> rows = new ArrayList<>();
        rows.add(Arrays.asList("?", "?", "?", "?"));
        when(sqlInsertRecognizer.getInsertRows(pkIndexMap.values())).thenReturn(rows);
    }

    private void mockStatementInsertRows() {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(Arrays.asList(Null.get(), "xx", "xx", "xx"));
        when(sqlInsertRecognizer.getInsertRows(pkIndexMap.values())).thenReturn(rows);
    }
}
