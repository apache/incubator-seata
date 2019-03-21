package com.alibaba.fescar.rm.datasource.exec;

import com.alibaba.fescar.common.exception.ShouldNeverHappenException;
import com.alibaba.fescar.rm.datasource.PreparedStatementProxy;
import com.alibaba.fescar.rm.datasource.sql.SQLInsertRecognizer;
import com.alibaba.fescar.rm.datasource.sql.struct.Null;
import com.alibaba.fescar.rm.datasource.sql.struct.TableMeta;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

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
    public void testContainsPK() {
        List<String> insertColumns=mockInsertColumns();
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

    @Test(expected =ShouldNeverHappenException.class)
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
        List pkValuesByColumn=insertExecutor.getPkValuesByColumn();
        //pk value = Null so getPkValuesByAuto
        verify(insertExecutor).getPkValuesByAuto();
        Assertions.assertThat(pkValuesByColumn).isEqualTo(pkValuesAuto);
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
