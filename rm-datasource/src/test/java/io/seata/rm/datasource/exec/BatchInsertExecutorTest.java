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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.seata.common.exception.NotSupportYetException;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.PreparedStatementProxy;
import io.seata.rm.datasource.exec.mysql.MySQLInsertExecutor;
import io.seata.sqlparser.struct.TableMeta;
import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.struct.Null;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * batch insert executor test
 *
 * @author zjinlei
 */
public class BatchInsertExecutorTest {

    private static final String ID_COLUMN = "id";
    private static final String USER_ID_COLUMN = "user_id";
    private static final String USER_NAME_COLUMN = "user_name";
    private static final String USER_STATUS_COLUMN = "user_status";
    private static final List<Integer> PK_VALUES = Arrays.asList(100000001, 100000002, 100000003, 100000004, 100000005);


    private PreparedStatementProxy statementProxy;

    private SQLInsertRecognizer sqlInsertRecognizer;

    private TableMeta tableMeta;

    private MySQLInsertExecutor insertExecutor;

    private final int pkIndex = 1;
    private HashMap pkIndexMap;

    @BeforeEach
    public void init() {
        ConnectionProxy connectionProxy = mock(ConnectionProxy.class);
        when(connectionProxy.getDbType()).thenReturn(JdbcConstants.MYSQL);

        statementProxy = mock(PreparedStatementProxy.class);
        when(statementProxy.getConnectionProxy()).thenReturn(connectionProxy);

        StatementCallback statementCallback = mock(StatementCallback.class);
        sqlInsertRecognizer = mock(SQLInsertRecognizer.class);
        tableMeta = mock(TableMeta.class);
        insertExecutor = Mockito.spy(new MySQLInsertExecutor(statementProxy, statementCallback, sqlInsertRecognizer));

        pkIndexMap = new HashMap() {{
            put(ID_COLUMN, pkIndex);
        }};

        doReturn(pkIndexMap).when(insertExecutor).getPkIndex();
    }

    @Test
    public void testGetPkValuesByColumnOfJDBC() throws SQLException {
        mockInsertColumns();
        mockParameters();
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        when(tableMeta.getPrimaryKeyOnlyName()).thenReturn(Arrays.asList(new String[]{ID_COLUMN}));
        List<Object> pkValues = new ArrayList<>();
        pkValues.addAll(PK_VALUES);
        Map<String,List<Object>> pkValuesMap = insertExecutor.getPkValuesByColumn();
        Assertions.assertIterableEquals(pkValuesMap.keySet(), tableMeta.getPrimaryKeyOnlyName());
        Assertions.assertIterableEquals(pkValuesMap.get(ID_COLUMN), pkValues);
    }

    @Test
    public void testGetPkValuesByColumnAndAllRefOfJDBC() throws SQLException {
        mockInsertColumns();
        mockParametersWithAllRefOfJDBC();
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        when(tableMeta.getPrimaryKeyOnlyName()).thenReturn(Arrays.asList(ID_COLUMN));
        List<Object> pkValues = new ArrayList<>(PK_VALUES);
        Map<String,List<Object>> pkValuesMap = insertExecutor.getPkValuesByColumn();
        Assertions.assertIterableEquals(pkValues,pkValuesMap.get(ID_COLUMN) );
    }

    @Test
    public void testGetPkValuesByColumnAndPkRefOfJDBC() throws SQLException {
        mockInsertColumns();
        mockParametersWithPkRefOfJDBC();
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        when(tableMeta.getPrimaryKeyOnlyName()).thenReturn(Arrays.asList(new String[]{ID_COLUMN}));
        List<Object> pkValues = new ArrayList<>();
        pkValues.addAll(PK_VALUES);
        Map<String,List<Object>> pkValuesMap = insertExecutor.getPkValuesByColumn();
        Assertions.assertIterableEquals(pkValuesMap.keySet(), tableMeta.getPrimaryKeyOnlyName());
        Assertions.assertIterableEquals(pkValuesMap.get(ID_COLUMN), pkValues);
    }

    @Test
    public void testGetPkValuesByColumnAndPkUnRefOfJDBC() throws SQLException {
        mockInsertColumns();
        int pkId = PK_VALUES.get(0);
        mockParametersWithPkUnRefOfJDBC(pkId);
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        when(tableMeta.getPrimaryKeyOnlyName()).thenReturn(Arrays.asList(new String[]{ID_COLUMN}));
        List<Object> pkValues = new ArrayList<>();
        pkValues.add(pkId);
        Map<String,List<Object>> pkValuesMap = insertExecutor.getPkValuesByColumn();
        Assertions.assertIterableEquals(pkValuesMap.keySet(), tableMeta.getPrimaryKeyOnlyName());
        Assertions.assertIterableEquals(pkValuesMap.get(ID_COLUMN), pkValues);
    }

    //----------------mysql batch values (),(),()------------------------

    @Test
    public void testGetPkValuesByColumnAndAllRefOfMysql() throws SQLException {
        mockInsertColumns();
        mockParametersAllRefOfMysql();
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        Mockito.when(tableMeta.getPrimaryKeyOnlyName()).thenReturn(Arrays.asList(new String[]{ID_COLUMN}));
        List<Object> pkValues = new ArrayList<>();
        pkValues.addAll(PK_VALUES);
        Map<String,List<Object>> pkValuesMap = insertExecutor.getPkValuesByColumn();
        Assertions.assertIterableEquals(pkValuesMap.keySet(), tableMeta.getPrimaryKeyOnlyName());
        Assertions.assertIterableEquals(pkValuesMap.get(ID_COLUMN), pkValues);
    }

    @Test
    public void testGetPkValuesByColumnAndPkRefOfMysql() throws SQLException {
        mockInsertColumns();
        mockParametersWithPkRefOfMysql();
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        Mockito.when(tableMeta.getPrimaryKeyOnlyName()).thenReturn(Arrays.asList(new String[]{ID_COLUMN}));
        List<Object> pkValues = new ArrayList<>();
        pkValues.addAll(PK_VALUES);
        Map<String,List<Object>> pkValuesMap = insertExecutor.getPkValuesByColumn();
        Assertions.assertIterableEquals(pkValuesMap.keySet(), tableMeta.getPrimaryKeyOnlyName());
        Assertions.assertIterableEquals(pkValuesMap.get(ID_COLUMN), pkValues);
    }

    @Test
    public void testGetPkValuesByColumnAndPkUnRefOfMysql() throws SQLException {
        mockInsertColumns();
        mockParametersWithPkUnRefOfMysql();
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        Mockito.when(tableMeta.getPrimaryKeyOnlyName()).thenReturn(Arrays.asList(new String[]{ID_COLUMN}));
        List<Object> pkValues = new ArrayList<>();
        pkValues.addAll(PK_VALUES);
        Map<String,List<Object>> pkValuesMap = insertExecutor.getPkValuesByColumn();
        Assertions.assertIterableEquals(pkValuesMap.keySet(), tableMeta.getPrimaryKeyOnlyName());
        Assertions.assertIterableEquals(pkValuesMap.get(ID_COLUMN), pkValues);
    }

    @Test
    public void testGetPkValues_NotSupportYetException() {
        Assertions.assertThrows(NotSupportYetException.class, () -> {
            mockInsertColumns();
            mockParameters_with_number_and_insertRows_with_placeholde_null();
            doReturn(tableMeta).when(insertExecutor).getTableMeta();
            Mockito.when(tableMeta.getPrimaryKeyOnlyName()).thenReturn(Arrays.asList(new String[]{ID_COLUMN}));
            insertExecutor.getPkValuesByColumn();
        });
    }

    private void mockParameters_with_null_and_insertRows_with_placeholder_null() {
        Map<Integer,ArrayList<Object>> paramters = new HashMap<>(5);
        ArrayList arrayList0 = new ArrayList<>();
        arrayList0.add("userId1");
        ArrayList arrayList1 = new ArrayList<>();
        arrayList1.add(Null.get());
        ArrayList arrayList2 = new ArrayList<>();
        arrayList2.add("userName1");
        ArrayList arrayList3 = new ArrayList<>();
        arrayList3.add("userId2");
        ArrayList arrayList4 = new ArrayList<>();
        arrayList4.add("userName2");
        paramters.put(1, arrayList0);
        paramters.put(2, arrayList1);
        paramters.put(3, arrayList2);
        paramters.put(4, arrayList3);
        paramters.put(5, arrayList4);
        when(statementProxy.getParameters()).thenReturn(paramters);

        List<List<Object>> insertRows = new ArrayList<>();
        insertRows.add(Arrays.asList("?", "?", "?", "userStatus1"));
        insertRows.add(Arrays.asList("?", Null.get(), "?", "userStatus2"));
        when(sqlInsertRecognizer.getInsertRows(pkIndexMap.values())).thenReturn(insertRows);
    }

    private void mockParameters_with_number_and_insertRows_with_placeholde_null() {
        Map<Integer,ArrayList<Object>> paramters = new HashMap<>(5);
        ArrayList arrayList0 = new ArrayList<>();
        arrayList0.add("userId1");
        ArrayList arrayList1 = new ArrayList<>();
        arrayList1.add(PK_VALUES.get(0));
        ArrayList arrayList2 = new ArrayList<>();
        arrayList2.add("userName1");
        ArrayList arrayList3 = new ArrayList<>();
        arrayList3.add("userId2");
        ArrayList arrayList4 = new ArrayList<>();
        arrayList4.add("userName2");
        paramters.put(1, arrayList0);
        paramters.put(2, arrayList1);
        paramters.put(3, arrayList2);
        paramters.put(4, arrayList3);
        paramters.put(5, arrayList4);
        when(statementProxy.getParameters()).thenReturn(paramters);

        List<List<Object>> insertRows = new ArrayList<>();
        insertRows.add(Arrays.asList("?", "?", "?", "userStatus1"));
        insertRows.add(Arrays.asList("?", Null.get(), "?", "userStatus2"));
        when(sqlInsertRecognizer.getInsertRows(pkIndexMap.values())).thenReturn(insertRows);
    }

    private List<String> mockInsertColumns() {
        List<String> columns = new ArrayList<>();
        columns.add(USER_ID_COLUMN);
        columns.add(ID_COLUMN);
        columns.add(USER_NAME_COLUMN);
        columns.add(USER_STATUS_COLUMN);
        when(sqlInsertRecognizer.getInsertColumns()).thenReturn(columns);
        return columns;
    }

    private void mockParameters() {
        int PK_INDEX = 1;
        Map<Integer,ArrayList<Object>> paramters = new HashMap<>();
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
        ArrayList arrayList3 = new ArrayList<>();
        arrayList3.add("userStatus1");
        arrayList3.add("userStatus2");
        arrayList3.add("userStatus3");
        arrayList3.add("userStatus4");
        arrayList3.add("userStatus5");

        paramters.put(1, arrayList0);
        paramters.put(2, arrayList1);
        paramters.put(3, arrayList2);
        paramters.put(4, arrayList3);

        List<List<Object>> insertRows = new ArrayList<>();
        insertRows.add(Arrays.asList("?", "?", "?", "?"));

        when(statementProxy.getParameters()).thenReturn(paramters);
        when(sqlInsertRecognizer.getInsertRows(pkIndexMap.values())).thenReturn(insertRows);
        when(statementProxy.getParamsByIndex(PK_INDEX)).thenReturn(paramters.get(PK_INDEX + 1));
    }

    private void mockParametersAllRefOfMysql() {

        Map<Integer,ArrayList<Object>> paramters = new HashMap(20);
        ArrayList arrayList1 = new ArrayList<>();
        arrayList1.add("userId1");
        ArrayList arrayList2 = new ArrayList<>();
        arrayList2.add(100000001);
        ArrayList arrayList3 = new ArrayList<>();
        arrayList3.add("userName1");
        ArrayList arrayList4 = new ArrayList<>();
        arrayList4.add("userStatus1");

        ArrayList arrayList5 = new ArrayList<>();
        arrayList5.add("userId2");
        ArrayList arrayList6 = new ArrayList<>();
        arrayList6.add(100000002);
        ArrayList arrayList7 = new ArrayList<>();
        arrayList7.add("userName2");
        ArrayList arrayList8 = new ArrayList<>();
        arrayList8.add("userStatus2");

        ArrayList arrayList9 = new ArrayList<>();
        arrayList9.add("userId3");
        ArrayList arrayList10 = new ArrayList<>();
        arrayList10.add(100000003);
        ArrayList arrayList11 = new ArrayList<>();
        arrayList11.add("userName3");
        ArrayList arrayList12 = new ArrayList<>();
        arrayList12.add("userStatus3");

        ArrayList arrayList13 = new ArrayList<>();
        arrayList13.add("userId4");
        ArrayList arrayList14 = new ArrayList<>();
        arrayList14.add(100000004);
        ArrayList arrayList15 = new ArrayList<>();
        arrayList15.add("userName4");
        ArrayList arrayList16 = new ArrayList<>();
        arrayList16.add("userStatus4");

        ArrayList arrayList17 = new ArrayList<>();
        arrayList17.add("userId5");
        ArrayList arrayList18 = new ArrayList<>();
        arrayList18.add(100000005);
        ArrayList arrayList19 = new ArrayList<>();
        arrayList19.add("userName5");
        ArrayList arrayList20 = new ArrayList<>();
        arrayList20.add("userStatus5");


        paramters.put(1,arrayList1);
        paramters.put(2,arrayList2);
        paramters.put(3,arrayList3);
        paramters.put(4,arrayList4);
        paramters.put(5,arrayList5);
        paramters.put(6,arrayList6);
        paramters.put(7,arrayList7);
        paramters.put(8,arrayList8);
        paramters.put(9,arrayList9);
        paramters.put(10,arrayList10);
        paramters.put(11,arrayList11);
        paramters.put(12,arrayList12);
        paramters.put(13,arrayList13);
        paramters.put(14,arrayList14);
        paramters.put(15,arrayList15);
        paramters.put(16,arrayList16);
        paramters.put(17,arrayList17);
        paramters.put(18,arrayList18);
        paramters.put(19,arrayList19);
        paramters.put(20,arrayList20);
        List<List<Object>> insertRows = new ArrayList<>();
        insertRows.add(Arrays.asList("?", "?", "?", "?"));
        insertRows.add(Arrays.asList("?", "?", "?", "?"));
        insertRows.add(Arrays.asList("?", "?", "?", "?"));
        insertRows.add(Arrays.asList("?", "?", "?", "?"));
        insertRows.add(Arrays.asList("?", "?", "?", "?"));
        when(statementProxy.getParameters()).thenReturn(paramters);
        when(sqlInsertRecognizer.getInsertRows(pkIndexMap.values())).thenReturn(insertRows);
        when(statementProxy.getParameters()).thenReturn(paramters);
    }

    private void mockParametersWithPkRefOfMysql() {

        Map<Integer,ArrayList<Object>> paramters = new HashMap<>(10);
        ArrayList arrayList1 = new ArrayList<>();
        arrayList1.add("userId1");
        ArrayList arrayList2 = new ArrayList<>();
        arrayList2.add(100000001);
        ArrayList arrayList3 = new ArrayList<>();
        arrayList3.add("userId2");
        ArrayList arrayList4 = new ArrayList<>();
        arrayList4.add(100000002);
        ArrayList arrayList5 = new ArrayList<>();
        arrayList5.add("userId3");
        ArrayList arrayList6 = new ArrayList<>();
        arrayList6.add(100000003);
        ArrayList arrayList7 = new ArrayList<>();
        arrayList7.add("userId4");
        ArrayList arrayList8 = new ArrayList<>();
        arrayList8.add(100000004);
        ArrayList arrayList9 = new ArrayList<>();
        arrayList9.add("userId5");
        ArrayList arrayList10 = new ArrayList<>();
        arrayList10.add(100000005);
        paramters.put(1,arrayList1);
        paramters.put(2,arrayList2);
        paramters.put(3,arrayList3);
        paramters.put(4,arrayList4);
        paramters.put(5,arrayList5);
        paramters.put(6,arrayList6);
        paramters.put(7,arrayList7);
        paramters.put(8,arrayList8);
        paramters.put(9,arrayList9);
        paramters.put(10,arrayList10);
        List<List<Object>> insertRows = new ArrayList<>();
        insertRows.add(Arrays.asList("?", "?", "1", "11"));
        insertRows.add(Arrays.asList("?", "?", "2", "22"));
        insertRows.add(Arrays.asList("?", "?", "3", "33"));
        insertRows.add(Arrays.asList("?", "?", "4", "44"));
        insertRows.add(Arrays.asList("?", "?", "5", "55"));
        when(statementProxy.getParameters()).thenReturn(paramters);
        when(sqlInsertRecognizer.getInsertRows(pkIndexMap.values())).thenReturn(insertRows);
        when(statementProxy.getParameters()).thenReturn(paramters);
    }

    private void mockParametersWithPkUnRefOfMysql() {

        Map<Integer,ArrayList<Object>> paramters = new HashMap<>(10);
        ArrayList arrayList1 = new ArrayList<>();
        arrayList1.add("userId1");
        ArrayList arrayList2 = new ArrayList<>();
        arrayList2.add(100000001);
        ArrayList arrayList3 = new ArrayList<>();
        arrayList3.add("userId2");
        ArrayList arrayList4 = new ArrayList<>();
        arrayList4.add(100000002);
        ArrayList arrayList5 = new ArrayList<>();
        arrayList5.add("userId3");
        ArrayList arrayList6 = new ArrayList<>();
        arrayList6.add(100000003);
        ArrayList arrayList7 = new ArrayList<>();
        arrayList7.add("userId4");
        ArrayList arrayList8 = new ArrayList<>();
        arrayList8.add(100000004);
        ArrayList arrayList9 = new ArrayList<>();
        arrayList9.add("userId5");
        ArrayList arrayList10 = new ArrayList<>();
        arrayList10.add(100000005);
        paramters.put(1,arrayList1);
        paramters.put(2,arrayList2);
        paramters.put(3,arrayList3);
        paramters.put(4,arrayList4);
        paramters.put(5,arrayList5);
        paramters.put(6,arrayList6);
        paramters.put(7,arrayList7);
        paramters.put(8,arrayList8);
        paramters.put(9,arrayList9);
        paramters.put(10,arrayList10);
        List<List<Object>> insertRows = new ArrayList<>();
        insertRows.add(Arrays.asList("?", 100000001, "?", "1"));
        insertRows.add(Arrays.asList("?", 100000002, "?", "2"));
        insertRows.add(Arrays.asList("?", 100000003, "?", "3"));
        insertRows.add(Arrays.asList("?", 100000004, "?", "4"));
        insertRows.add(Arrays.asList("?", 100000005, "?", "5"));
        when(statementProxy.getParameters()).thenReturn(paramters);
        when(sqlInsertRecognizer.getInsertRows(pkIndexMap.values())).thenReturn(insertRows);
    }


    private void mockParametersWithAllRefOfJDBC() {
        int PK_INDEX = 1;
        Map<Integer,ArrayList<Object>> paramters = new HashMap<>(4);
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
        ArrayList arrayList3 = new ArrayList<>();
        arrayList3.add("userStatus1");
        arrayList3.add("userStatus2");
        arrayList3.add("userStatus3");
        arrayList3.add("userStatus4");
        arrayList3.add("userStatus5");
        paramters.put(1,arrayList0);
        paramters.put(2,arrayList1);
        paramters.put(3,arrayList2);
        paramters.put(4,arrayList3);

        List<List<Object>> insertRows = new ArrayList<>();
        insertRows.add(Arrays.asList("?", "?", "?", "?"));
        when(statementProxy.getParameters()).thenReturn(paramters);
        when(statementProxy.getParamsByIndex(PK_INDEX)).thenReturn(paramters.get(PK_INDEX + 1));
        doReturn(insertRows).when(sqlInsertRecognizer).getInsertRows(pkIndexMap.values());
    }


    private void mockParametersWithPkRefOfJDBC() {
        int PK_INDEX = 1;
        Map<Integer,ArrayList<Object>> paramters = new HashMap<>(2);
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
        paramters.put(1, arrayList0);
        paramters.put(2, arrayList1);

        List<List<Object>> insertRows = new ArrayList<>();
        insertRows.add(Arrays.asList("?", "?", "userName1", "userStatus1"));
        when(statementProxy.getParameters()).thenReturn(paramters);
        when(sqlInsertRecognizer.getInsertRows(pkIndexMap.values())).thenReturn(insertRows);
        when(statementProxy.getParamsByIndex(PK_INDEX)).thenReturn(paramters.get(PK_INDEX + 1));
    }


    private void mockParametersWithPkUnRefOfJDBC(int pkId) {
        Map<Integer,ArrayList<Object>> paramters = new HashMap<>(2);
        ArrayList arrayList0 = new ArrayList<>();
        arrayList0.add("userId1");
        ArrayList arrayList1 = new ArrayList<>();
        arrayList1.add("userName1");
        paramters.put(1, arrayList0);
        paramters.put(2, arrayList1);

        List<List<Object>> insertRows = new ArrayList<>();
        insertRows.add(Arrays.asList("?", pkId, "?", "userStatus"));
        when(statementProxy.getParameters()).thenReturn(paramters);
        when(sqlInsertRecognizer.getInsertRows(pkIndexMap.values())).thenReturn(insertRows);
    }

}
