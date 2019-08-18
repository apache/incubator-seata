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

import io.seata.rm.datasource.PreparedStatementProxy;
import io.seata.rm.datasource.sql.SQLInsertRecognizer;
import io.seata.rm.datasource.sql.struct.TableMeta;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * @author zjinlei
 * @date 2019/07/16
 */
public class BatchInsertExecutorTest {

    private static final String ID_COLUMN = "id";
    private static final String USER_ID_COLUMN = "user_id";
    private static final String USER_NAME_COLUMN = "user_name";
    private static final List<Integer> PK_VALUES = Arrays.asList(100000001, 100000002, 100000003, 100000004, 100000005);

    private PreparedStatementProxy statementProxy;

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
    }

    @Test
    public void testGetPkValuesByColumn() throws SQLException {
        mockInsertColumns();
        mockParameters();
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        when(tableMeta.getPkName()).thenReturn(ID_COLUMN);
        List<Object> pkValues = new ArrayList<>();
        pkValues.addAll(PK_VALUES);
        List<Integer> pkValuesByColumn = insertExecutor.getPkValuesByColumn();
        Assertions.assertIterableEquals(pkValuesByColumn, pkValues);
    }

    @Test
    public void testGetPkValuesByColumnAndSomeStatement() throws SQLException {
        mockInsertColumns();
        mockParametersWithSomeStatement();
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        when(tableMeta.getPkName()).thenReturn(ID_COLUMN);
        List<Object> pkValues = new ArrayList<>();
        pkValues.addAll(PK_VALUES);
        List<Integer> pkValuesByColumn = insertExecutor.getPkValuesByColumn();
        Assertions.assertIterableEquals(pkValuesByColumn, pkValues);
    }

    @Test
    public void testGetPkValuesByColumnAndAllStatementInPS() throws SQLException {
        mockInsertColumns();
        mockParametersWithAllStatementInPS();
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        when(tableMeta.getPkName()).thenReturn(ID_COLUMN);
        List<Object> pkValues = new ArrayList<>();
        pkValues.addAll(PK_VALUES);
        List<Integer> pkValuesByColumn = insertExecutor.getPkValuesByColumn();
        Assertions.assertIterableEquals(pkValuesByColumn, pkValues);
    }

    private List<String> mockInsertColumns() {
        List<String> columns = new ArrayList<>();
        columns.add(USER_ID_COLUMN);
        columns.add(ID_COLUMN);
        columns.add(USER_NAME_COLUMN);
        when(sqlInsertRecognizer.getInsertColumns()).thenReturn(columns);
        return columns;
    }

    private void mockParameters() {
        int PK_INDEX = 1;
        ArrayList<Object>[] paramters = new ArrayList[3];
        ArrayList arrayList0 = new ArrayList<>();
        arrayList0.add("userId1");
        arrayList0.add("userId2");
        arrayList0.add("userId3");
        arrayList0.add("userId4");
        arrayList0.add("userId5");
        ArrayList arrayList1 = new ArrayList<>();
        arrayList1.add(PK_VALUES.get(0));
        arrayList1.add(PK_VALUES.get(1));
        arrayList1.add(PK_VALUES.get(2));
        arrayList1.add(PK_VALUES.get(3));
        arrayList1.add(PK_VALUES.get(4));
        ArrayList arrayList2 = new ArrayList<>();
        arrayList2.add("userName1");
        arrayList2.add("userName2");
        arrayList2.add("userName3");
        arrayList2.add("userName4");
        arrayList2.add("userName5");

        paramters[0] = arrayList0;
        paramters[1] = arrayList1;
        paramters[2] = arrayList2;
        when(statementProxy.getParameters()).thenReturn(paramters);
        when(statementProxy.getParamsByIndex(PK_INDEX)).thenReturn(paramters[PK_INDEX]);
    }

    private void mockParametersWithSomeStatement() {
        int PK_INDEX = 1;
        ArrayList<Object>[] paramters = new ArrayList[2];
        ArrayList arrayList0 = new ArrayList<>();
        arrayList0.add("userId1");
        arrayList0.add("userId2");
        arrayList0.add("userId3");
        arrayList0.add("userId4");
        arrayList0.add("userId5");
        ArrayList arrayList1 = new ArrayList<>();
        arrayList1.add(PK_VALUES.get(0));
        arrayList1.add(PK_VALUES.get(1));
        arrayList1.add(PK_VALUES.get(2));
        arrayList1.add(PK_VALUES.get(3));
        arrayList1.add(PK_VALUES.get(4));
        paramters[0] = arrayList0;
        paramters[1] = arrayList1;

        List<List<Object>> insertRows = new ArrayList<>();
        insertRows.add(Arrays.asList("?", "?", "userName1"));
        insertRows.add(Arrays.asList("?", "?", "userName2"));
        insertRows.add(Arrays.asList("?", "?", "userName3"));
        insertRows.add(Arrays.asList("?", "?", "userName4"));
        insertRows.add(Arrays.asList("?", "?", "userName5"));
        when(statementProxy.getParameters()).thenReturn(paramters);
        when(sqlInsertRecognizer.getInsertRows()).thenReturn(insertRows);
        when(statementProxy.getParamsByIndex(PK_INDEX)).thenReturn(paramters[PK_INDEX]);
    }

    private void mockParametersWithAllStatementInPS() {
        ArrayList<Object>[] paramters = new ArrayList[0];

        List<List<Object>> insertRows = new ArrayList<>();
        insertRows.add(Arrays.asList("userId1", PK_VALUES.get(0), "userName1"));
        insertRows.add(Arrays.asList("userId2", PK_VALUES.get(1), "userName2"));
        insertRows.add(Arrays.asList("userId3", PK_VALUES.get(2), "userName3"));
        insertRows.add(Arrays.asList("userId4", PK_VALUES.get(3), "userName4"));
        insertRows.add(Arrays.asList("userId5", PK_VALUES.get(4), "userName5"));
        when(statementProxy.getParameters()).thenReturn(paramters);
        when(sqlInsertRecognizer.getInsertRows()).thenReturn(insertRows);
    }
}
