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
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.PreparedStatementProxy;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.sql.SQLInsertRecognizer;
import io.seata.rm.datasource.sql.struct.ColumnMeta;
import io.seata.rm.datasource.sql.struct.Null;
import io.seata.rm.datasource.sql.struct.SqlSequenceExpr;
import io.seata.rm.datasource.sql.struct.TableMeta;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author jsbxyyx
 */
public class OracleInsertExecutorTest {

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
        connectionProxy = mock(ConnectionProxy.class);
        when(connectionProxy.getDbType()).thenReturn(JdbcConstants.ORACLE);

        statementProxy = mock(PreparedStatementProxy.class);
        when(statementProxy.getConnectionProxy()).thenReturn(connectionProxy);

        statementCallback = mock(StatementCallback.class);
        sqlInsertRecognizer = mock(SQLInsertRecognizer.class);
        tableMeta = mock(TableMeta.class);
        insertExecutor = Mockito.spy(new InsertExecutor(statementProxy, statementCallback, sqlInsertRecognizer));
    }

    @Test
    public void testPkValue_sequence() throws Exception {
        mockInsertColumns();
        SqlSequenceExpr expr = mockParametersPkWithSeq();
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        when(tableMeta.getPkName()).thenReturn(ID_COLUMN);
        List<Object> pkValuesSeq = new ArrayList<>();
        pkValuesSeq.add(PK_VALUE);

        doReturn(pkValuesSeq).when(insertExecutor).getPkValuesBySequence(expr);
        doReturn(0).when(insertExecutor).getPkIndex();

        List pkValuesByColumn = insertExecutor.getPkValuesByColumn();
        verify(insertExecutor).getPkValuesBySequence(expr);
        Assertions.assertEquals(pkValuesByColumn, pkValuesSeq);
    }

    @Test
    public void testPkValue_auto() throws Exception {
        mockInsertColumns();
        mockParametersPkWithAuto();
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        when(tableMeta.getPkName()).thenReturn(ID_COLUMN);
        List<Object> pkValuesAuto = new ArrayList<>();
        pkValuesAuto.add(PK_VALUE);

        doReturn(pkValuesAuto).when(insertExecutor).getPkValuesByAuto();
        List pkValuesByAuto = insertExecutor.getPkValuesByAuto();

        verify(insertExecutor).getPkValuesByAuto();
        Assertions.assertEquals(pkValuesByAuto, pkValuesAuto);
    }

    @Test
    public void testStatement_pkValueByAuto_NotSupportYetException() throws Exception {
        mockInsertColumns();
        mockStatementInsertRows();

        statementProxy = mock(StatementProxy.class);
        when(statementProxy.getConnectionProxy()).thenReturn(connectionProxy);
        when(connectionProxy.getDbType()).thenReturn(JdbcConstants.ORACLE);

        insertExecutor = Mockito.spy(new InsertExecutor(statementProxy, statementCallback, sqlInsertRecognizer));

        doReturn(tableMeta).when(insertExecutor).getTableMeta();

        Map<String, ColumnMeta> map = new HashMap<>();
        map.put(ID_COLUMN, mock(ColumnMeta.class));
        doReturn(map).when(tableMeta).getPrimaryKeyMap();

        ResultSet rs = mock(ResultSet.class);
        Statement statement = mock(Statement.class);
        doReturn(statement).when(statementProxy).getTargetStatement();
        doReturn(rs).when(statement).getGeneratedKeys();
        doReturn(false).when(rs).next();

        Assertions.assertThrows(NotSupportYetException.class, () -> {
            insertExecutor.getPkValuesByAuto();
        });

        int pkIndex = 0;
        doReturn(pkIndex).when(insertExecutor).getPkIndex();

        Assertions.assertThrows(NotSupportYetException.class, () -> {
            insertExecutor.getPkValuesByColumn();
        });

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
        ArrayList<Object>[] paramters = new ArrayList[4];
        ArrayList arrayList0 = new ArrayList<>();
        arrayList0.add(expr);
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

        List<List<Object>> rows = new ArrayList<>();
        rows.add(Arrays.asList("?", "?", "?"));
        when(sqlInsertRecognizer.getInsertRows()).thenReturn(rows);

        return expr;
    }

    private void mockParametersPkWithAuto() {
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

        List<List<Object>> rows = new ArrayList<>();
        rows.add(Arrays.asList("?", "?", "?", "?"));
        when(sqlInsertRecognizer.getInsertRows()).thenReturn(rows);
    }

    private void mockStatementInsertRows() {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(Arrays.asList(Null.get(), "xx", "xx", "xx"));
        when(sqlInsertRecognizer.getInsertRows()).thenReturn(rows);
    }


}
