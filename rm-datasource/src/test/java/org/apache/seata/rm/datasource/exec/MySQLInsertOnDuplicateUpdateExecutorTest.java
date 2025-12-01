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

import com.google.common.collect.Lists;
import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.common.util.LowerCaseLinkHashMap;
import org.apache.seata.rm.datasource.ConnectionProxy;
import org.apache.seata.rm.datasource.PreparedStatementProxy;
import org.apache.seata.rm.datasource.StatementProxy;
import org.apache.seata.rm.datasource.exec.mysql.MySQLInsertOnDuplicateUpdateExecutor;
import org.apache.seata.rm.datasource.sql.struct.Field;
import org.apache.seata.rm.datasource.sql.struct.KeyType;
import org.apache.seata.rm.datasource.sql.struct.Row;
import org.apache.seata.rm.datasource.sql.struct.TableRecords;
import org.apache.seata.rm.datasource.undo.SQLUndoLog;
import org.apache.seata.sqlparser.SQLInsertRecognizer;
import org.apache.seata.sqlparser.SQLType;
import org.apache.seata.sqlparser.struct.ColumnMeta;
import org.apache.seata.sqlparser.struct.IndexMeta;
import org.apache.seata.sqlparser.struct.IndexType;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MySQLInsertOnDuplicateUpdateExecutorTest {

    protected static final String ID_COLUMN = "id";
    protected static final String USER_ID_COLUMN = "user_id";
    protected static final String USER_NAME_COLUMN = "user_name";
    protected static final String USER_STATUS_COLUMN = "user_status";
    protected static final Integer PK_VALUE = 100;
    protected final int pkIndex = 0;
    protected StatementProxy statementProxy;
    protected SQLInsertRecognizer sqlInsertRecognizer;
    protected TableMeta tableMeta;
    protected MySQLInsertOnDuplicateUpdateExecutor insertOrUpdateExecutor;
    protected HashMap<String, Integer> pkIndexMap;

    @BeforeEach
    public void init() {
        ConnectionProxy connectionProxy = mock(ConnectionProxy.class);
        when(connectionProxy.getDbType()).thenReturn(JdbcConstants.MYSQL);

        statementProxy = mock(PreparedStatementProxy.class);
        when(statementProxy.getConnectionProxy()).thenReturn(connectionProxy);

        StatementCallback statementCallback = mock(StatementCallback.class);
        sqlInsertRecognizer = mock(SQLInsertRecognizer.class);
        tableMeta = mock(TableMeta.class);
        insertOrUpdateExecutor = Mockito.spy(
                new MySQLInsertOnDuplicateUpdateExecutor(statementProxy, statementCallback, sqlInsertRecognizer));

        pkIndexMap = new HashMap<String, Integer>() {
            {
                put(ID_COLUMN, pkIndex);
            }
        };
    }

    @Test
    public void TestBuildImageParameters() {
        mockParameters();
        List<List<Object>> rows = new ArrayList<>();
        rows.add(Arrays.asList("?", "?", "?", "?"));
        rows.add(Arrays.asList("?", "?", "?", "?"));
        when(sqlInsertRecognizer.getInsertRows(pkIndexMap.values())).thenReturn(rows);
        mockInsertColumns();
        doReturn(pkIndexMap).when(insertOrUpdateExecutor).getPkIndex();
        Map<String, ArrayList<Object>> imageParameterMap =
                insertOrUpdateExecutor.buildImageParameters(sqlInsertRecognizer);
        Assertions.assertEquals(
                imageParameterMap.toString(), mockImageParameterMap().toString());
    }

    @Test
    public void TestBuildImageParameters_contain_constant() {
        mockImageParameterMap_contain_constant();
        List<List<Object>> insertRows = new ArrayList<>();
        insertRows.add(Arrays.asList("?", "?", "?", "userStatus1"));
        insertRows.add(Arrays.asList("?", "?", "?", "userStatus2"));
        when(sqlInsertRecognizer.getInsertRows(pkIndexMap.values())).thenReturn(insertRows);
        mockInsertColumns();
        doReturn(pkIndexMap).when(insertOrUpdateExecutor).getPkIndex();
        Map<String, ArrayList<Object>> imageParameterMap =
                insertOrUpdateExecutor.buildImageParameters(sqlInsertRecognizer);
        Assertions.assertEquals(
                imageParameterMap.toString(), mockImageParameterMap().toString());
    }

    @Test
    public void testBuildImageSQL() {
        String selectSQLStr =
                "SELECT *  FROM null WHERE (user_id = ? )  OR (id = ? )  OR (user_id = ? )  OR (id = ? ) ";
        String paramAppenderListStr = "[[userId1, 100], [userId2, 101]]";
        mockImageParameterMap_contain_constant();
        List<List<Object>> insertRows = new ArrayList<>();
        insertRows.add(Arrays.asList("?", "?", "?", "userStatus1"));
        insertRows.add(Arrays.asList("?", "?", "?", "userStatus2"));
        when(sqlInsertRecognizer.getInsertRows(pkIndexMap.values())).thenReturn(insertRows);
        mockInsertColumns();
        mockAllIndexes();
        doReturn(pkIndexMap).when(insertOrUpdateExecutor).getPkIndex();
        String selectSQL = insertOrUpdateExecutor.buildImageSQL(tableMeta);
        Assertions.assertEquals(selectSQLStr, selectSQL);
        Assertions.assertEquals(
                paramAppenderListStr,
                insertOrUpdateExecutor.getParamAppenderList().toString());
    }

    @Test
    public void testBeforeImage() {
        mockImageParameterMap_contain_constant();
        List<List<Object>> insertRows = new ArrayList<>();
        insertRows.add(Arrays.asList("?,?,?,userStatus1"));
        insertRows.add(Arrays.asList("?,?,?,userStatus2"));
        when(sqlInsertRecognizer.getInsertRows(pkIndexMap.values())).thenReturn(insertRows);
        mockInsertColumns();
        mockAllIndexes();
        doReturn(tableMeta).when(insertOrUpdateExecutor).getTableMeta();
        try {
            TableRecords tableRecords = new TableRecords();
            String selectSQL = insertOrUpdateExecutor.buildImageSQL(tableMeta);
            ArrayList<List<Object>> paramAppenderList = insertOrUpdateExecutor.getParamAppenderList();
            doReturn(tableRecords)
                    .when(insertOrUpdateExecutor)
                    .buildTableRecords2(tableMeta, selectSQL, paramAppenderList, Collections.emptyList());
            TableRecords tableRecordsResult = insertOrUpdateExecutor.beforeImage();
            Assertions.assertEquals(tableRecords, tableRecordsResult);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    @Test
    public void testBeforeImageWithNoUnique() {
        mockImageParameterMap_contain_constant();
        List<List<Object>> insertRows = new ArrayList<>();
        insertRows.add(Arrays.asList("?,?,?,userStatus1"));
        insertRows.add(Arrays.asList("?,?,?,userStatus2"));
        when(sqlInsertRecognizer.getInsertRows(pkIndexMap.values())).thenReturn(insertRows);
        mockInsertColumns();
        mockAllIndexes();
        doReturn(tableMeta).when(insertOrUpdateExecutor).getTableMeta();
        Assertions.assertThrows(NotSupportYetException.class, () -> {
            insertOrUpdateExecutor.beforeImage();
        });
    }

    @Test
    public void testBuildImageParametersWithUpdatePrimaryKey() {
        mockParameters();
        mockInsertColumns();
        List<String> duplicateKeyUpdateColumns = new ArrayList<>();
        duplicateKeyUpdateColumns.add("id");
        when(sqlInsertRecognizer.getDuplicateKeyUpdate()).thenReturn(duplicateKeyUpdateColumns);

        mockAllIndexes();
        doReturn(tableMeta).when(insertOrUpdateExecutor).getTableMeta();
        doReturn(pkIndexMap).when(insertOrUpdateExecutor).getPkIndex();

        List<List<Object>> rows = new ArrayList<>();
        rows.add(Arrays.asList("?", "?", "?", "?"));
        when(sqlInsertRecognizer.getInsertRows(pkIndexMap.values())).thenReturn(rows);

        Assertions.assertThrows(ShouldNeverHappenException.class, () -> {
            insertOrUpdateExecutor.buildImageParameters(sqlInsertRecognizer);
        });
    }

    @Test
    public void testBuildImageParametersWithEmptyInsertColumns() {
        mockParameters();
        when(sqlInsertRecognizer.getInsertColumns()).thenReturn(null);

        Map<String, ColumnMeta> allColumns = new LinkedHashMap<>();
        allColumns.put("id", new ColumnMeta());
        allColumns.put("user_id", new ColumnMeta());
        allColumns.put("user_name", new ColumnMeta());
        allColumns.put("user_status", new ColumnMeta());
        when(tableMeta.getAllColumns()).thenReturn(allColumns);

        List<List<Object>> rows = new ArrayList<>();
        rows.add(Arrays.asList("?", "?", "?", "?"));
        when(sqlInsertRecognizer.getInsertRows(pkIndexMap.values())).thenReturn(rows);

        doReturn(tableMeta).when(insertOrUpdateExecutor).getTableMeta();
        doReturn(pkIndexMap).when(insertOrUpdateExecutor).getPkIndex();

        Map<String, ArrayList<Object>> result = insertOrUpdateExecutor.buildImageParameters(sqlInsertRecognizer);
        Assertions.assertNotNull(result);
    }

    @Test
    public void testBuildImageSQLWithDefaultValue() {
        mockParametersWithoutPkColumn();
        List<String> columns = new ArrayList<>();
        columns.add(USER_ID_COLUMN);
        columns.add(USER_NAME_COLUMN);
        columns.add(USER_STATUS_COLUMN);
        when(sqlInsertRecognizer.getInsertColumns()).thenReturn(columns);
        Map<String, Integer> pkIndexWithoutId = new HashMap<>();
        doReturn(pkIndexWithoutId).when(insertOrUpdateExecutor).getPkIndex();

        Map<String, IndexMeta> allIndex = new HashMap<>();
        IndexMeta primary = new IndexMeta();
        primary.setIndextype(IndexType.PRIMARY);
        ColumnMeta columnMeta = new ColumnMeta();
        columnMeta.setColumnName("id");
        columnMeta.setColumnDef("AUTO_INCREMENT");
        primary.setValues(Lists.newArrayList(columnMeta));
        allIndex.put("PRIMARY", primary);
        when(tableMeta.getAllIndexes()).thenReturn(allIndex);

        List<List<Object>> insertRows = new ArrayList<>();
        insertRows.add(Arrays.asList("?", "?", "?"));
        when(sqlInsertRecognizer.getInsertRows(pkIndexWithoutId.values())).thenReturn(insertRows);

        String sql = insertOrUpdateExecutor.buildImageSQL(tableMeta);
        Assertions.assertTrue(sql.contains("DEFAULT"));
    }

    @Test
    public void testBuildTableRecords2WithEmptyParamAppenderList() {
        doReturn(tableMeta).when(insertOrUpdateExecutor).getTableMeta();

        Assertions.assertThrows(NotSupportYetException.class, () -> {
            insertOrUpdateExecutor.buildTableRecords2(
                    tableMeta, "SELECT * FROM test", new ArrayList<>(), Collections.emptyList());
        });
    }

    @Test
    public void testBuildImageParametersWithRowSizeMismatch() {
        mockParameters();
        mockInsertColumns();
        doReturn(pkIndexMap).when(insertOrUpdateExecutor).getPkIndex();

        List<List<Object>> rows = new ArrayList<>();
        rows.add(Arrays.asList("?", "?"));
        when(sqlInsertRecognizer.getInsertRows(pkIndexMap.values())).thenReturn(rows);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            insertOrUpdateExecutor.buildImageParameters(sqlInsertRecognizer);
        });
    }

    @Test
    public void testGetSelectSQL() {
        mockParameters();
        mockInsertColumns();
        mockAllIndexes();
        doReturn(pkIndexMap).when(insertOrUpdateExecutor).getPkIndex();

        List<List<Object>> insertRows = new ArrayList<>();
        insertRows.add(Arrays.asList("?", "?", "?", "?"));
        when(sqlInsertRecognizer.getInsertRows(pkIndexMap.values())).thenReturn(insertRows);

        insertOrUpdateExecutor.buildImageSQL(tableMeta);
        Assertions.assertNotNull(insertOrUpdateExecutor.getSelectSQL());
    }

    @Test
    public void testGetParamAppenderList() {
        mockParameters();
        mockInsertColumns();
        mockAllIndexes();
        doReturn(pkIndexMap).when(insertOrUpdateExecutor).getPkIndex();

        List<List<Object>> insertRows = new ArrayList<>();
        insertRows.add(Arrays.asList("?", "?", "?", "?"));
        when(sqlInsertRecognizer.getInsertRows(pkIndexMap.values())).thenReturn(insertRows);

        insertOrUpdateExecutor.buildImageSQL(tableMeta);
        Assertions.assertNotNull(insertOrUpdateExecutor.getParamAppenderList());
        Assertions.assertTrue(insertOrUpdateExecutor.getParamAppenderList().size() > 0);
    }

    protected void mockAllIndexes() {
        Map<String, IndexMeta> allIndex = new LowerCaseLinkHashMap<>();
        IndexMeta unique = new IndexMeta();
        unique.setIndextype(IndexType.UNIQUE);
        ColumnMeta columnMetaUnique = new ColumnMeta();
        columnMetaUnique.setColumnName("user_id");
        unique.setValues(Lists.newArrayList(columnMetaUnique));
        allIndex.put("user_id_unique", unique);

        IndexMeta primary = new IndexMeta();
        primary.setIndextype(IndexType.PRIMARY);
        ColumnMeta columnMeta = new ColumnMeta();
        columnMeta.setColumnName("id");
        primary.setValues(Lists.newArrayList(columnMeta));
        allIndex.put("PRIMARY", primary);
        when(tableMeta.getAllIndexes()).thenReturn(allIndex);
    }

    protected List<String> mockInsertColumns() {
        List<String> columns = new ArrayList<>();
        columns.add(ID_COLUMN);
        columns.add(USER_ID_COLUMN);
        columns.add(USER_NAME_COLUMN);
        columns.add(USER_STATUS_COLUMN);
        when(sqlInsertRecognizer.getInsertColumns()).thenReturn(columns);
        return columns;
    }

    /**
     * all insert params is variable
     * {1=[100], 2=[userId1], 3=[userName1], 4=[userStatus1], 5=[101], 6=[userId2], 7=[userName2], 8=[userStatus2]}
     */
    protected void mockParameters() {
        Map<Integer, ArrayList<Object>> parameters = new HashMap<>(4);
        ArrayList arrayList10 = new ArrayList<>();
        arrayList10.add(PK_VALUE);
        ArrayList arrayList11 = new ArrayList<>();
        arrayList11.add("userId1");
        ArrayList arrayList12 = new ArrayList<>();
        arrayList12.add("userName1");
        ArrayList arrayList13 = new ArrayList<>();
        arrayList13.add("userStatus1");
        parameters.put(1, arrayList10);
        parameters.put(2, arrayList11);
        parameters.put(3, arrayList12);
        parameters.put(4, arrayList13);
        ArrayList arrayList20 = new ArrayList<>();
        arrayList20.add(PK_VALUE + 1);
        ArrayList arrayList21 = new ArrayList<>();
        arrayList21.add("userId2");
        ArrayList arrayList22 = new ArrayList<>();
        arrayList22.add("userName2");
        ArrayList arrayList23 = new ArrayList<>();
        arrayList23.add("userStatus2");
        parameters.put(5, arrayList20);
        parameters.put(6, arrayList21);
        parameters.put(7, arrayList22);
        parameters.put(8, arrayList23);
        PreparedStatementProxy psp = (PreparedStatementProxy) this.statementProxy;
        when(psp.getParameters()).thenReturn(parameters);
    }

    /**
     * exist insert parms is constant
     * {1=[100], 2=[userId1], 3=[userName1], 4=[101], 5=[userId2], 6=[userName2]}
     */
    protected void mockImageParameterMap_contain_constant() {
        Map<Integer, ArrayList<Object>> parameters = new HashMap<>(4);
        ArrayList arrayList10 = new ArrayList<>();
        arrayList10.add(PK_VALUE);
        ArrayList arrayList11 = new ArrayList<>();
        arrayList11.add("userId1");
        ArrayList arrayList12 = new ArrayList<>();
        arrayList12.add("userName1");
        parameters.put(1, arrayList10);
        parameters.put(2, arrayList11);
        parameters.put(3, arrayList12);
        ArrayList arrayList20 = new ArrayList<>();
        arrayList20.add(PK_VALUE + 1);
        ArrayList arrayList21 = new ArrayList<>();
        arrayList21.add("userId2");
        ArrayList arrayList22 = new ArrayList<>();
        arrayList22.add("userName2");
        parameters.put(4, arrayList20);
        parameters.put(5, arrayList21);
        parameters.put(6, arrayList22);
        PreparedStatementProxy psp = (PreparedStatementProxy) this.statementProxy;
        when(psp.getParameters()).thenReturn(parameters);
    }

    protected Map<String, ArrayList<Object>> mockImageParameterMap() {
        Map<String, ArrayList<Object>> imageParameterMap = new LinkedHashMap<>();
        ArrayList<Object> idList = new ArrayList<>();
        idList.add("100");
        idList.add("101");
        imageParameterMap.put("id", idList);
        ArrayList<Object> user_idList = new ArrayList<>();
        user_idList.add("userId1");
        user_idList.add("userId2");
        imageParameterMap.put("user_id", user_idList);
        ArrayList<Object> user_nameList = new ArrayList<>();
        user_nameList.add("userName1");
        user_nameList.add("userName2");
        imageParameterMap.put("user_name", user_nameList);
        ArrayList<Object> user_statusList = new ArrayList<>();
        user_statusList.add("userStatus1");
        user_statusList.add("userStatus2");
        imageParameterMap.put("user_status", user_statusList);
        return imageParameterMap;
    }

    protected void mockParametersOfOnePk() {
        Map<Integer, ArrayList<Object>> parameters = new HashMap<>(4);
        ArrayList arrayList1 = new ArrayList<>();
        arrayList1.add(PK_VALUE);
        parameters.put(1, arrayList1);
        PreparedStatementProxy psp = (PreparedStatementProxy) this.statementProxy;
        when(psp.getParameters()).thenReturn(parameters);
    }

    protected void mockInsertRows() {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(Arrays.asList("?", "?", "?", "?"));
        when(sqlInsertRecognizer.getInsertRows(pkIndexMap.values())).thenReturn(rows);
    }

    protected void mockParametersWithoutPkColumn() {
        Map<Integer, ArrayList<Object>> parameters = new HashMap<>(4);
        ArrayList<Object> userId1 = new ArrayList<>();
        userId1.add("userId1");
        ArrayList<Object> userName1 = new ArrayList<>();
        userName1.add("userName1");
        ArrayList<Object> userStatus1 = new ArrayList<>();
        userStatus1.add("userStatus1");
        parameters.put(1, userId1);
        parameters.put(2, userName1);
        parameters.put(3, userStatus1);

        ArrayList<Object> userId2 = new ArrayList<>();
        userId2.add("userId2");
        ArrayList<Object> userName2 = new ArrayList<>();
        userName2.add("userName2");
        ArrayList<Object> userStatus2 = new ArrayList<>();
        userStatus2.add("userStatus2");
        parameters.put(4, userId2);
        parameters.put(5, userName2);
        parameters.put(6, userStatus2);
        PreparedStatementProxy psp = (PreparedStatementProxy) this.statementProxy;
        when(psp.getParameters()).thenReturn(parameters);
    }

    @Test
    public void executeAutoCommitFalseInsertScenarioTest() throws Exception {
        mockParameters();
        mockInsertColumns();
        mockAllIndexes();
        doReturn(pkIndexMap).when(insertOrUpdateExecutor).getPkIndex();
        doReturn(tableMeta).when(insertOrUpdateExecutor).getTableMeta();
        when(tableMeta.getPrimaryKeyOnlyName()).thenReturn(Collections.singletonList(ID_COLUMN));

        TableRecords emptyBeforeImage = TableRecords.empty(tableMeta);
        TableRecords afterImage = new TableRecords(tableMeta);
        doReturn(emptyBeforeImage).when(insertOrUpdateExecutor).beforeImage();

        PreparedStatementProxy psp = (PreparedStatementProxy) statementProxy;
        when(psp.getUpdateCount()).thenReturn(2);

        StatementCallback callback = mock(StatementCallback.class);
        when(callback.execute(any(), any())).thenReturn(null);
        MySQLInsertOnDuplicateUpdateExecutor executor =
                spy(new MySQLInsertOnDuplicateUpdateExecutor(statementProxy, callback, sqlInsertRecognizer));
        doReturn(pkIndexMap).when(executor).getPkIndex();
        doReturn(tableMeta).when(executor).getTableMeta();
        doReturn("SELECT * FROM test_table WHERE id = ?").when(executor).buildImageSQL(any(TableMeta.class));
        doReturn(emptyBeforeImage).when(executor).beforeImage();
        doReturn(afterImage)
                .when(executor)
                .buildTableRecords2(any(TableMeta.class), any(String.class), any(ArrayList.class), any(List.class));
        doReturn("test_lock_key").when(executor).buildLockKey(any(TableRecords.class));

        java.lang.reflect.Field selectSQLField =
                MySQLInsertOnDuplicateUpdateExecutor.class.getDeclaredField("selectSQL");
        selectSQLField.setAccessible(true);
        selectSQLField.set(executor, "SELECT * FROM test_table WHERE id = ?");

        java.lang.reflect.Field paramAppenderListField =
                MySQLInsertOnDuplicateUpdateExecutor.class.getDeclaredField("paramAppenderList");
        paramAppenderListField.setAccessible(true);
        ArrayList<List<Object>> paramList = new ArrayList<>();
        paramList.add(Arrays.asList(100));
        paramAppenderListField.set(executor, paramList);

        Method method =
                MySQLInsertOnDuplicateUpdateExecutor.class.getDeclaredMethod("executeAutoCommitFalse", Object[].class);
        method.setAccessible(true);
        Object result = method.invoke(executor, (Object) new Object[] {});

        Assertions.assertNull(result);
        verify(executor, times(1)).beforeImage();
    }

    @Test
    public void executeAutoCommitFalseUpdateScenarioTest() throws Exception {
        mockParameters();
        mockInsertColumns();
        mockAllIndexes();
        doReturn(pkIndexMap).when(insertOrUpdateExecutor).getPkIndex();
        doReturn(tableMeta).when(insertOrUpdateExecutor).getTableMeta();
        when(tableMeta.getPrimaryKeyOnlyName()).thenReturn(Collections.singletonList(ID_COLUMN));

        TableRecords beforeImage = new TableRecords(tableMeta);
        TableRecords afterImage = new TableRecords(tableMeta);
        doReturn(beforeImage).when(insertOrUpdateExecutor).beforeImage();

        PreparedStatementProxy psp = (PreparedStatementProxy) statementProxy;
        when(psp.getUpdateCount()).thenReturn(1);

        StatementCallback callback = mock(StatementCallback.class);
        when(callback.execute(any(), any())).thenReturn(null);
        MySQLInsertOnDuplicateUpdateExecutor executor =
                spy(new MySQLInsertOnDuplicateUpdateExecutor(statementProxy, callback, sqlInsertRecognizer));
        doReturn(pkIndexMap).when(executor).getPkIndex();
        doReturn(tableMeta).when(executor).getTableMeta();
        doReturn("SELECT * FROM test_table WHERE id = ?").when(executor).buildImageSQL(any(TableMeta.class));
        doReturn(beforeImage).when(executor).beforeImage();
        doReturn(afterImage)
                .when(executor)
                .buildTableRecords2(any(TableMeta.class), any(String.class), any(ArrayList.class), any(List.class));
        doReturn("test_lock_key").when(executor).buildLockKey(any(TableRecords.class));

        java.lang.reflect.Field selectSQLField =
                MySQLInsertOnDuplicateUpdateExecutor.class.getDeclaredField("selectSQL");
        selectSQLField.setAccessible(true);
        selectSQLField.set(executor, "SELECT * FROM test_table WHERE id = ?");

        java.lang.reflect.Field paramAppenderListField =
                MySQLInsertOnDuplicateUpdateExecutor.class.getDeclaredField("paramAppenderList");
        paramAppenderListField.setAccessible(true);
        ArrayList<List<Object>> paramList = new ArrayList<>();
        paramList.add(Arrays.asList(100));
        paramAppenderListField.set(executor, paramList);

        Method method =
                MySQLInsertOnDuplicateUpdateExecutor.class.getDeclaredMethod("executeAutoCommitFalse", Object[].class);
        method.setAccessible(true);
        Object result = method.invoke(executor, (Object) new Object[] {});

        Assertions.assertNull(result);
        verify(executor, times(1)).beforeImage();
    }

    @Test
    public void executeAutoCommitFalseZeroUpdateCountTest() throws Exception {
        mockParameters();
        mockInsertColumns();
        mockAllIndexes();
        doReturn(pkIndexMap).when(insertOrUpdateExecutor).getPkIndex();
        doReturn(tableMeta).when(insertOrUpdateExecutor).getTableMeta();
        when(tableMeta.getPrimaryKeyOnlyName()).thenReturn(Collections.singletonList(ID_COLUMN));

        TableRecords emptyBeforeImage = TableRecords.empty(tableMeta);
        doReturn(emptyBeforeImage).when(insertOrUpdateExecutor).beforeImage();

        PreparedStatementProxy psp = (PreparedStatementProxy) statementProxy;
        when(psp.getUpdateCount()).thenReturn(0);

        StatementCallback callback = mock(StatementCallback.class);
        when(callback.execute(any(), any())).thenReturn(null);
        MySQLInsertOnDuplicateUpdateExecutor executor =
                spy(new MySQLInsertOnDuplicateUpdateExecutor(statementProxy, callback, sqlInsertRecognizer));
        doReturn(pkIndexMap).when(executor).getPkIndex();
        doReturn(tableMeta).when(executor).getTableMeta();
        doReturn("SELECT * FROM test_table WHERE id = ?").when(executor).buildImageSQL(any(TableMeta.class));
        doReturn(emptyBeforeImage).when(executor).beforeImage();

        Method method =
                MySQLInsertOnDuplicateUpdateExecutor.class.getDeclaredMethod("executeAutoCommitFalse", Object[].class);
        method.setAccessible(true);
        Object result = method.invoke(executor, (Object) new Object[] {});

        Assertions.assertNull(result);
        verify(executor, times(1)).beforeImage();
    }

    @Test
    public void executeAutoCommitFalseMultiPkNonMySQLTest() throws Exception {
        ConnectionProxy oracleConnectionProxy = mock(ConnectionProxy.class);
        when(oracleConnectionProxy.getDbType()).thenReturn(JdbcConstants.ORACLE);

        StatementProxy oracleStatementProxy = mock(PreparedStatementProxy.class);
        when(oracleStatementProxy.getConnectionProxy()).thenReturn(oracleConnectionProxy);

        TableMeta multiPkTableMeta = mock(TableMeta.class);
        when(multiPkTableMeta.getPrimaryKeyOnlyName()).thenReturn(Arrays.asList("id", "user_id"));

        StatementCallback callback = mock(StatementCallback.class);
        MySQLInsertOnDuplicateUpdateExecutor executor =
                spy(new MySQLInsertOnDuplicateUpdateExecutor(oracleStatementProxy, callback, sqlInsertRecognizer));
        doReturn(multiPkTableMeta).when(executor).getTableMeta();
        doReturn(JdbcConstants.ORACLE).when(executor).getDbType();

        Method method =
                MySQLInsertOnDuplicateUpdateExecutor.class.getDeclaredMethod("executeAutoCommitFalse", Object[].class);
        method.setAccessible(true);

        Assertions.assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
            method.invoke(executor, (Object) new Object[] {});
        });
    }

    @Test
    public void buildUndoItemInsertTypeTest() throws Exception {
        mockParameters();
        mockInsertColumns();
        mockAllIndexes();
        doReturn(pkIndexMap).when(insertOrUpdateExecutor).getPkIndex();
        doReturn(tableMeta).when(insertOrUpdateExecutor).getTableMeta();
        when(sqlInsertRecognizer.getTableName()).thenReturn("test_table");

        TableRecords emptyBeforeImage = TableRecords.empty(tableMeta);
        TableRecords afterImage = new TableRecords(tableMeta);

        Method method = MySQLInsertOnDuplicateUpdateExecutor.class.getDeclaredMethod(
                "buildUndoItem", SQLType.class, TableRecords.class, TableRecords.class);
        method.setAccessible(true);
        SQLUndoLog result =
                (SQLUndoLog) method.invoke(insertOrUpdateExecutor, SQLType.INSERT, emptyBeforeImage, afterImage);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(SQLType.INSERT, result.getSqlType());
        Assertions.assertEquals("test_table", result.getTableName());
        Assertions.assertEquals(emptyBeforeImage, result.getBeforeImage());
        Assertions.assertEquals(afterImage, result.getAfterImage());
    }

    @Test
    public void buildUndoItemUpdateTypeTest() throws Exception {
        mockParameters();
        mockInsertColumns();
        mockAllIndexes();
        doReturn(pkIndexMap).when(insertOrUpdateExecutor).getPkIndex();
        doReturn(tableMeta).when(insertOrUpdateExecutor).getTableMeta();
        when(sqlInsertRecognizer.getTableName()).thenReturn("test_table");

        TableRecords beforeImage = new TableRecords(tableMeta);
        TableRecords afterImage = new TableRecords(tableMeta);

        Method method = MySQLInsertOnDuplicateUpdateExecutor.class.getDeclaredMethod(
                "buildUndoItem", SQLType.class, TableRecords.class, TableRecords.class);
        method.setAccessible(true);
        SQLUndoLog result = (SQLUndoLog) method.invoke(insertOrUpdateExecutor, SQLType.UPDATE, beforeImage, afterImage);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(SQLType.UPDATE, result.getSqlType());
        Assertions.assertEquals("test_table", result.getTableName());
        Assertions.assertEquals(beforeImage, result.getBeforeImage());
        Assertions.assertEquals(afterImage, result.getAfterImage());
    }

    @Test
    public void prepareUndoLogAllEmptyImagesTest() throws Exception {
        mockParameters();
        mockInsertColumns();
        mockAllIndexes();
        doReturn(pkIndexMap).when(insertOrUpdateExecutor).getPkIndex();
        doReturn(tableMeta).when(insertOrUpdateExecutor).getTableMeta();

        TableRecords emptyBeforeImage = TableRecords.empty(tableMeta);
        TableRecords emptyAfterImage = TableRecords.empty(tableMeta);

        ConnectionProxy connectionProxy = statementProxy.getConnectionProxy();

        Method method = MySQLInsertOnDuplicateUpdateExecutor.class.getDeclaredMethod(
                "prepareUndoLogAll", TableRecords.class, TableRecords.class);
        method.setAccessible(true);
        method.invoke(insertOrUpdateExecutor, emptyBeforeImage, emptyAfterImage);

        verify(connectionProxy, times(0)).appendLockKey(any());
    }

    @Test
    public void buildUndoItemAllPureInsertTest() throws Exception {
        mockParameters();
        mockInsertColumns();
        mockAllIndexes();
        doReturn(pkIndexMap).when(insertOrUpdateExecutor).getPkIndex();
        doReturn(tableMeta).when(insertOrUpdateExecutor).getTableMeta();
        when(tableMeta.getTableName()).thenReturn("test_table");

        TableRecords emptyBeforeImage = TableRecords.empty(tableMeta);
        TableRecords afterImage = new TableRecords(tableMeta);
        Row afterRow = new Row();
        Field pkField = new Field("id", java.sql.Types.INTEGER, 100);
        pkField.setKeyType(KeyType.PRIMARY_KEY);
        afterRow.add(pkField);
        afterImage.add(afterRow);

        ConnectionProxy connectionProxy = statementProxy.getConnectionProxy();

        java.lang.reflect.Field isUpdateFlagField =
                MySQLInsertOnDuplicateUpdateExecutor.class.getDeclaredField("isUpdateFlag");
        isUpdateFlagField.setAccessible(true);
        isUpdateFlagField.setBoolean(insertOrUpdateExecutor, false);

        Method method = MySQLInsertOnDuplicateUpdateExecutor.class.getDeclaredMethod(
                "buildUndoItemAll", ConnectionProxy.class, TableRecords.class, TableRecords.class);
        method.setAccessible(true);
        method.invoke(insertOrUpdateExecutor, connectionProxy, emptyBeforeImage, afterImage);

        verify(connectionProxy, times(1)).appendUndoLog(any(SQLUndoLog.class));
    }

    @Test
    public void buildUndoItemAllPureUpdateTest() throws Exception {
        mockParameters();
        mockInsertColumns();
        mockAllIndexes();
        doReturn(pkIndexMap).when(insertOrUpdateExecutor).getPkIndex();
        doReturn(tableMeta).when(insertOrUpdateExecutor).getTableMeta();
        when(tableMeta.getTableName()).thenReturn("test_table");

        TableRecords beforeImage = new TableRecords(tableMeta);
        Row beforeRow = new Row();
        Field pkField1 = new Field("id", java.sql.Types.INTEGER, 100);
        pkField1.setKeyType(KeyType.PRIMARY_KEY);
        beforeRow.add(pkField1);
        beforeImage.add(beforeRow);

        TableRecords afterImage = new TableRecords(tableMeta);
        Row afterRow = new Row();
        Field pkField2 = new Field("id", java.sql.Types.INTEGER, 100);
        pkField2.setKeyType(KeyType.PRIMARY_KEY);
        afterRow.add(pkField2);
        afterImage.add(afterRow);

        ConnectionProxy connectionProxy = statementProxy.getConnectionProxy();

        java.lang.reflect.Field isUpdateFlagField =
                MySQLInsertOnDuplicateUpdateExecutor.class.getDeclaredField("isUpdateFlag");
        isUpdateFlagField.setAccessible(true);
        isUpdateFlagField.setBoolean(insertOrUpdateExecutor, true);

        Method method = MySQLInsertOnDuplicateUpdateExecutor.class.getDeclaredMethod(
                "buildUndoItemAll", ConnectionProxy.class, TableRecords.class, TableRecords.class);
        method.setAccessible(true);
        method.invoke(insertOrUpdateExecutor, connectionProxy, beforeImage, afterImage);

        verify(connectionProxy, times(1)).appendUndoLog(any(SQLUndoLog.class));
    }

    @Test
    public void buildUndoItemAllMixedTest() throws Exception {
        mockParameters();
        mockInsertColumns();
        mockAllIndexes();
        doReturn(pkIndexMap).when(insertOrUpdateExecutor).getPkIndex();
        doReturn(tableMeta).when(insertOrUpdateExecutor).getTableMeta();
        when(tableMeta.getTableName()).thenReturn("test_table");

        TableRecords beforeImage = new TableRecords(tableMeta);
        Row beforeRow = new Row();
        Field pkField1 = new Field("id", java.sql.Types.INTEGER, 100);
        pkField1.setKeyType(KeyType.PRIMARY_KEY);
        beforeRow.add(pkField1);
        beforeImage.add(beforeRow);

        TableRecords afterImage = new TableRecords(tableMeta);
        Row afterRow1 = new Row();
        Field pkField2 = new Field("id", java.sql.Types.INTEGER, 100);
        pkField2.setKeyType(KeyType.PRIMARY_KEY);
        afterRow1.add(pkField2);
        afterImage.add(afterRow1);

        Row afterRow2 = new Row();
        Field pkField3 = new Field("id", java.sql.Types.INTEGER, 101);
        pkField3.setKeyType(KeyType.PRIMARY_KEY);
        afterRow2.add(pkField3);
        afterImage.add(afterRow2);

        ConnectionProxy connectionProxy = statementProxy.getConnectionProxy();

        java.lang.reflect.Field isUpdateFlagField =
                MySQLInsertOnDuplicateUpdateExecutor.class.getDeclaredField("isUpdateFlag");
        isUpdateFlagField.setAccessible(true);
        isUpdateFlagField.setBoolean(insertOrUpdateExecutor, true);

        Method method = MySQLInsertOnDuplicateUpdateExecutor.class.getDeclaredMethod(
                "buildUndoItemAll", ConnectionProxy.class, TableRecords.class, TableRecords.class);
        method.setAccessible(true);
        method.invoke(insertOrUpdateExecutor, connectionProxy, beforeImage, afterImage);

        verify(connectionProxy, times(2)).appendUndoLog(any(SQLUndoLog.class));
    }

    @Test
    public void buildUndoItemAllSizeMismatchTest() throws Exception {
        mockParameters();
        mockInsertColumns();
        mockAllIndexes();
        doReturn(pkIndexMap).when(insertOrUpdateExecutor).getPkIndex();
        doReturn(tableMeta).when(insertOrUpdateExecutor).getTableMeta();
        when(tableMeta.getTableName()).thenReturn("test_table");

        TableRecords beforeImage = new TableRecords(tableMeta);
        Row beforeRow1 = new Row();
        Field pkField1 = new Field("id", java.sql.Types.INTEGER, 100);
        pkField1.setKeyType(KeyType.PRIMARY_KEY);
        beforeRow1.add(pkField1);
        beforeImage.add(beforeRow1);

        Row beforeRow2 = new Row();
        Field pkField2 = new Field("id", java.sql.Types.INTEGER, 101);
        pkField2.setKeyType(KeyType.PRIMARY_KEY);
        beforeRow2.add(pkField2);
        beforeImage.add(beforeRow2);

        TableRecords afterImage = new TableRecords(tableMeta);
        Row afterRow1 = new Row();
        Field pkField3 = new Field("id", java.sql.Types.INTEGER, 100);
        pkField3.setKeyType(KeyType.PRIMARY_KEY);
        afterRow1.add(pkField3);
        afterImage.add(afterRow1);

        ConnectionProxy connectionProxy = statementProxy.getConnectionProxy();

        java.lang.reflect.Field isUpdateFlagField =
                MySQLInsertOnDuplicateUpdateExecutor.class.getDeclaredField("isUpdateFlag");
        isUpdateFlagField.setAccessible(true);
        isUpdateFlagField.setBoolean(insertOrUpdateExecutor, true);

        Method method = MySQLInsertOnDuplicateUpdateExecutor.class.getDeclaredMethod(
                "buildUndoItemAll", ConnectionProxy.class, TableRecords.class, TableRecords.class);
        method.setAccessible(true);

        Assertions.assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
            method.invoke(insertOrUpdateExecutor, connectionProxy, beforeImage, afterImage);
        });
    }
}
