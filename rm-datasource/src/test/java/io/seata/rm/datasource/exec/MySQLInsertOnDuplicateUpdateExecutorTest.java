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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import com.google.common.collect.Lists;
import io.seata.common.exception.NotSupportYetException;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.PreparedStatementProxy;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.exec.mysql.MySQLInsertOnDuplicateUpdateExecutor;
import io.seata.sqlparser.struct.ColumnMeta;
import io.seata.sqlparser.struct.IndexMeta;
import io.seata.sqlparser.struct.IndexType;
import io.seata.sqlparser.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * @author: yangyicong
 */
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
    protected HashMap<String,Integer> pkIndexMap;

    @BeforeEach
    public void init() {
        ConnectionProxy connectionProxy = mock(ConnectionProxy.class);
        when(connectionProxy.getDbType()).thenReturn(JdbcConstants.MYSQL);

        statementProxy = mock(PreparedStatementProxy.class);
        when(statementProxy.getConnectionProxy()).thenReturn(connectionProxy);

        StatementCallback statementCallback = mock(StatementCallback.class);
        sqlInsertRecognizer = mock(SQLInsertRecognizer.class);
        tableMeta = mock(TableMeta.class);
        insertOrUpdateExecutor = Mockito.spy(new MySQLInsertOnDuplicateUpdateExecutor(statementProxy, statementCallback, sqlInsertRecognizer));

        pkIndexMap = new HashMap<String,Integer>(){
            {
                put(ID_COLUMN, pkIndex);
            }
        };
    }

    @Test
    public void TestBuildImageParameters(){
        mockParameters();
        List<List<Object>> rows = new ArrayList<>();
        rows.add(Arrays.asList("?","?","?","?"));
        rows.add(Arrays.asList("?","?","?","?"));
        when(sqlInsertRecognizer.getInsertRows(pkIndexMap.values())).thenReturn(rows);
        mockInsertColumns();
        doReturn(pkIndexMap).when(insertOrUpdateExecutor).getPkIndex();
        Map<String, ArrayList<Object>> imageParameterMap = insertOrUpdateExecutor.buildImageParameters(sqlInsertRecognizer);
        Assertions.assertEquals(imageParameterMap.toString(),mockImageParameterMap().toString());
    }

    @Test
    public void TestBuildImageParameters_contain_constant(){
        mockImageParameterMap_contain_constant();
        List<List<Object>> insertRows = new ArrayList<>();
        insertRows.add(Arrays.asList("?","?","?","userStatus1"));
        insertRows.add(Arrays.asList("?","?","?","userStatus2"));
        when(sqlInsertRecognizer.getInsertRows(pkIndexMap.values())).thenReturn(insertRows);
        mockInsertColumns();
        doReturn(pkIndexMap).when(insertOrUpdateExecutor).getPkIndex();
        Map<String, ArrayList<Object>> imageParameterMap = insertOrUpdateExecutor.buildImageParameters(sqlInsertRecognizer);
        Assertions.assertEquals(imageParameterMap.toString(),mockImageParameterMap().toString());
    }

    @Test
    public void testBuildImageSQL(){
        String selectSQLStr = "SELECT *  FROM null WHERE (user_id = ? )  OR (id = ? )  OR (user_id = ? )  OR (id = ? ) ";
        String paramAppenderListStr = "[[userId1, 100], [userId2, 101]]";
        mockImageParameterMap_contain_constant();
        List<List<Object>> insertRows = new ArrayList<>();
        insertRows.add(Arrays.asList("?","?","?","userStatus1"));
        insertRows.add(Arrays.asList("?","?","?","userStatus2"));
        when(sqlInsertRecognizer.getInsertRows(pkIndexMap.values())).thenReturn(insertRows);
        mockInsertColumns();
        mockAllIndexes();
        doReturn(pkIndexMap).when(insertOrUpdateExecutor).getPkIndex();
        String selectSQL = insertOrUpdateExecutor.buildImageSQL(tableMeta);
        Assertions.assertEquals(selectSQLStr,selectSQL);
        Assertions.assertEquals(paramAppenderListStr,insertOrUpdateExecutor.getParamAppenderList().toString());
    }

    @Test
    public void testBeforeImage(){
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
            doReturn(tableRecords).when(insertOrUpdateExecutor).buildTableRecords2(tableMeta,selectSQL,paramAppenderList, Collections.emptyList());
            TableRecords tableRecordsResult = insertOrUpdateExecutor.beforeImage();
            Assertions.assertEquals(tableRecords,tableRecordsResult);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
    @Test
    public void testBeforeImageWithNoUnique(){
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

    protected void mockAllIndexes(){
        Map<String, IndexMeta> allIndex = new HashMap<>();
        IndexMeta primary = new IndexMeta();
        primary.setIndextype(IndexType.PRIMARY);
        ColumnMeta columnMeta = new ColumnMeta();
        columnMeta.setColumnName("id");
        primary.setValues(Lists.newArrayList(columnMeta));
        allIndex.put("id", primary);

        IndexMeta unique = new IndexMeta();
        unique.setIndextype(IndexType.PRIMARY);
        ColumnMeta columnMetaUnique = new ColumnMeta();
        columnMetaUnique.setColumnName("user_id");
        unique.setValues(Lists.newArrayList(columnMetaUnique));
        allIndex.put("user_id", unique);
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
        Map<Integer,ArrayList<Object>> paramters = new HashMap<>(4);
        ArrayList arrayList10 = new ArrayList<>();
        arrayList10.add(PK_VALUE);
        ArrayList arrayList11 = new ArrayList<>();
        arrayList11.add("userId1");
        ArrayList arrayList12 = new ArrayList<>();
        arrayList12.add("userName1");
        ArrayList arrayList13 = new ArrayList<>();
        arrayList13.add("userStatus1");
        paramters.put(1, arrayList10);
        paramters.put(2, arrayList11);
        paramters.put(3, arrayList12);
        paramters.put(4, arrayList13);
        ArrayList arrayList20 = new ArrayList<>();
        arrayList20.add(PK_VALUE+1);
        ArrayList arrayList21 = new ArrayList<>();
        arrayList21.add("userId2");
        ArrayList arrayList22 = new ArrayList<>();
        arrayList22.add("userName2");
        ArrayList arrayList23 = new ArrayList<>();
        arrayList23.add("userStatus2");
        paramters.put(5, arrayList20);
        paramters.put(6, arrayList21);
        paramters.put(7, arrayList22);
        paramters.put(8, arrayList23);
        PreparedStatementProxy psp = (PreparedStatementProxy) this.statementProxy;
        when(psp.getParameters()).thenReturn(paramters);
    }

    /**
     * exist insert parms is constant
     * {1=[100], 2=[userId1], 3=[userName1], 4=[101], 5=[userId2], 6=[userName2]}
     */
    protected void mockImageParameterMap_contain_constant() {
        Map<Integer,ArrayList<Object>> paramters = new HashMap<>(4);
        ArrayList arrayList10 = new ArrayList<>();
        arrayList10.add(PK_VALUE);
        ArrayList arrayList11 = new ArrayList<>();
        arrayList11.add("userId1");
        ArrayList arrayList12 = new ArrayList<>();
        arrayList12.add("userName1");
        paramters.put(1, arrayList10);
        paramters.put(2, arrayList11);
        paramters.put(3, arrayList12);
        ArrayList arrayList20 = new ArrayList<>();
        arrayList20.add(PK_VALUE+1);
        ArrayList arrayList21 = new ArrayList<>();
        arrayList21.add("userId2");
        ArrayList arrayList22 = new ArrayList<>();
        arrayList22.add("userName2");
        paramters.put(4, arrayList20);
        paramters.put(5, arrayList21);
        paramters.put(6, arrayList22);
        PreparedStatementProxy psp = (PreparedStatementProxy) this.statementProxy;
        when(psp.getParameters()).thenReturn(paramters);
    }

    protected Map<String, ArrayList<Object>> mockImageParameterMap(){
        Map<String, ArrayList<Object>> imageParameterMap = new LinkedHashMap<>();
        ArrayList<Object> idList = new ArrayList<>();
        idList.add("100");
        idList.add("101");
        imageParameterMap.put("id",idList);
        ArrayList<Object> user_idList = new ArrayList<>();
        user_idList.add("userId1");
        user_idList.add("userId2");
        imageParameterMap.put("user_id",user_idList);
        ArrayList<Object> user_nameList = new ArrayList<>();
        user_nameList.add("userName1");
        user_nameList.add("userName2");
        imageParameterMap.put("user_name",user_nameList);
        ArrayList<Object> user_statusList = new ArrayList<>();
        user_statusList.add("userStatus1");
        user_statusList.add("userStatus2");
        imageParameterMap.put("user_status",user_statusList);
        return imageParameterMap;
    }

    protected void mockParametersOfOnePk() {
        Map<Integer,ArrayList<Object>> paramters = new HashMap<>(4);
        ArrayList arrayList1 = new ArrayList<>();
        arrayList1.add(PK_VALUE);
        paramters.put(1, arrayList1);
        PreparedStatementProxy psp = (PreparedStatementProxy) this.statementProxy;
        when(psp.getParameters()).thenReturn(paramters);
    }

    protected void mockInsertRows() {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(Arrays.asList("?", "?", "?", "?"));
        when(sqlInsertRecognizer.getInsertRows(pkIndexMap.values())).thenReturn(rows);
    }
}
