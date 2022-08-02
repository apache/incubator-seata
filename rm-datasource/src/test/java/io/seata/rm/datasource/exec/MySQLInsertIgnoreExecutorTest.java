package io.seata.rm.datasource.exec;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.PreparedStatementProxy;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.exec.mysql.MySQLInsertIgnoreExecutor;
import io.seata.rm.datasource.sql.struct.ColumnMeta;
import io.seata.rm.datasource.sql.struct.IndexMeta;
import io.seata.rm.datasource.sql.struct.IndexType;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author: lyx
 */
public class MySQLInsertIgnoreExecutorTest {

    private static final String ID_COLUMN = "id";
    private static final String USER_ID_COLUMN = "user_id";
    private static final String USER_NAME_COLUMN = "user_name";
    private static final String USER_STATUS_COLUMN = "user_status";
    private static final Integer PK_VALUE = 100;

    private StatementProxy statementProxy;

    private SQLInsertRecognizer sqlInsertRecognizer;

    private TableMeta tableMeta;

    private MySQLInsertIgnoreExecutor insertIgnoreExecutor;

    private final int pkIndex = 0;
    private HashMap<String, Integer> pkIndexMap;

    @BeforeEach
    public void init() throws SQLException {
        ConnectionProxy connectionProxy = mock(ConnectionProxy.class);
        when(connectionProxy.getDbType()).thenReturn(JdbcConstants.MYSQL);

        statementProxy = mock(PreparedStatementProxy.class);
        when(statementProxy.getConnectionProxy()).thenReturn(connectionProxy);
        when(statementProxy.getConnection()).thenReturn(connectionProxy);
        StatementCallback statementCallback = mock(StatementCallback.class);
        sqlInsertRecognizer = mock(SQLInsertRecognizer.class);
        tableMeta = mock(TableMeta.class);
        insertIgnoreExecutor = Mockito.spy(new MySQLInsertIgnoreExecutor(statementProxy, statementCallback, sqlInsertRecognizer));

        pkIndexMap = new HashMap<String, Integer>() {
            {
                put(ID_COLUMN, pkIndex);
            }
        };
    }

    @Test
    public void TestBuildImageParamperters() {
        mockParameters();
        List<String> insertParamsList = new ArrayList<>();
        insertParamsList.add("?,?,?,?");
        insertParamsList.add("?,?,?,?");
        when(sqlInsertRecognizer.getInsertParamsValue()).thenReturn(insertParamsList);
        mockInsertColumns();
        Map<String, ArrayList<Object>> imageParamperterMap = insertIgnoreExecutor.buildImageParamperters(sqlInsertRecognizer);
        Assertions.assertEquals(imageParamperterMap.toString(), mockImageParamperterMap().toString());
    }

    @Test
    public void TestBuildImageParamperters_contain_constant() {
        mockImageParamperterMap_contain_constant();
        List<String> insertParamsList = new ArrayList<>();
        insertParamsList.add("?,?,?,userStatus1");
        insertParamsList.add("?,?,?,userStatus2");
        when(sqlInsertRecognizer.getInsertParamsValue()).thenReturn(insertParamsList);
        mockInsertColumns();
        Map<String, ArrayList<Object>> imageParamperterMap = insertIgnoreExecutor.buildImageParamperters(sqlInsertRecognizer);
        Assertions.assertEquals(imageParamperterMap.toString(), mockImageParamperterMap().toString());
    }

    @Test
    public void testBuildImageSQL() {
        String selectSQLStr = "SELECT *  FROM null WHERE (user_id) in((?),(?)) OR (id) in((?),(?)) ";
        String paramAppenderListStr = "{[user_id]=[userId1, userId2], [id]=[100, 101]}";
        mockImageParamperterMap_contain_constant();
        List<String> insertParamsList = new ArrayList<>();
        insertParamsList.add("?,?,?,userStatus1");
        insertParamsList.add("?,?,?,userStatus2");
        when(sqlInsertRecognizer.getInsertParamsValue()).thenReturn(insertParamsList);
        mockInsertColumns();
        mockAllIndexes();
        String selectSQL = insertIgnoreExecutor.buildImageSQL(tableMeta);
        Assertions.assertEquals(selectSQLStr, selectSQL);
        Assertions.assertEquals(paramAppenderListStr, insertIgnoreExecutor.getParamAppenderMap().toString());
    }

    @Test
    public void testBeforeImages() throws SQLException {
        mockImageParamperterMap_contain_constant();
        List<String> insertParamsList = new ArrayList<>();
        insertParamsList.add("?,?,?,userStatus1");
        insertParamsList.add("?,?,?,userStatus2");
        when(sqlInsertRecognizer.getInsertParamsValue()).thenReturn(insertParamsList);
        mockInsertColumns();
        mockAllIndexes();
        String selectSQL = insertIgnoreExecutor.buildImageSQL(tableMeta);
        insertIgnoreExecutor.setSelectSQL(selectSQL);
        HashMap<List<String>, List<Object>> paramAppenderMap = insertIgnoreExecutor.getParamAppenderMap();
        doReturn(tableMeta).when(insertIgnoreExecutor).getTableMeta();
        TableRecords tableRecords = new TableRecords();
        doReturn(tableRecords).when(insertIgnoreExecutor).buildTableRecords2(tableMeta, selectSQL, new ArrayList<>(paramAppenderMap.values()));
        TableRecords beforeImage = insertIgnoreExecutor.beforeImage();
        Assertions.assertEquals(beforeImage, tableRecords);
    }

    @Test
    public void testAfterImage() throws SQLException {
        String selectSQL = "SELECT *  FROM null WHERE (id) in((?),(?)) ";
        String afterSQL = "";
        TableRecords afterImage = new TableRecords();
        doReturn(tableMeta).when(insertIgnoreExecutor).getTableMeta();
        TableRecords beforeImage = new TableRecords();
        HashMap<List<String>, List<Object>> hashMap = new HashMap<>();
        insertIgnoreExecutor.setParamAppenderMap(hashMap);
        hashMap.put(Collections.singletonList("id"), Collections.singletonList("id1"));
        insertIgnoreExecutor.setSelectSQL(selectSQL);
        doReturn(afterImage).when(insertIgnoreExecutor).buildTableRecords2(tableMeta, selectSQL + afterSQL, new ArrayList<>(hashMap.values()));
        TableRecords resultRecords = insertIgnoreExecutor.afterImage(beforeImage);
        Assertions.assertEquals(resultRecords, afterImage);
    }

    @Test
    public void testBuildUndoRow() {
        TableRecords beforeImage = new TableRecords();
        TableRecords afterImage = new TableRecords();
        getMockTableRecords(beforeImage);
        getMockTableRecords(afterImage);
        Map<SQLType, List<Row>> sqlTypeListMap = insertIgnoreExecutor.buildUndoRow(beforeImage, afterImage);
        Assertions.assertTrue(sqlTypeListMap.get(SQLType.INSERT).isEmpty());
        List<Row> insert = afterImage.getRows();
        Row insertRow = new Row();
        insert.add(insertRow);
        Row insertRow01 = new Row();
        insert.add(insertRow01);
        afterImage.setRows(insert);
        Map<SQLType, List<Row>> sqlTypeListMap01 = insertIgnoreExecutor.buildUndoRow(beforeImage, afterImage);
        Assertions.assertEquals(sqlTypeListMap01.get(SQLType.INSERT).toString(), insert.toString());
    }

    private void getMockTableRecords(TableRecords tableRecords) {
        ArrayList<Row> rows = new ArrayList<>();
        Row beforeRow01 = new Row();
        rows.add(beforeRow01);
        Row beforeRow02 = new Row();
        rows.add(beforeRow02);
        tableRecords.setRows(rows);
    }


    private void mockAllIndexes() {
        Map<String, IndexMeta> allIndex = new LinkedHashMap<>();
        List<String> primaryKeyOnlyNameList = new ArrayList<>();
        primaryKeyOnlyNameList.add("id");

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
        when(tableMeta.getPrimaryKeyOnlyName()).thenReturn(primaryKeyOnlyNameList);
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

    /**
     * all insert params is variable
     * {1=[100], 2=[userId1], 3=[userName1], 4=[userStatus1], 5=[101], 6=[userId2], 7=[userName2], 8=[userStatus2]}
     */
    private void mockParameters() {
        Map<Integer, ArrayList<Object>> paramters = new HashMap<>(4);
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
        arrayList20.add(PK_VALUE + 1);
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
    private void mockImageParamperterMap_contain_constant() {
        Map<Integer, ArrayList<Object>> paramters = new HashMap<>(4);
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
        arrayList20.add(PK_VALUE + 1);
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

    private Map<String, ArrayList<Object>> mockImageParamperterMap() {
        Map<String, ArrayList<Object>> imageParamperterMap = new HashMap<>();
        ArrayList<Object> idList = new ArrayList<>();
        idList.add("100");
        idList.add("101");
        imageParamperterMap.put("id", idList);
        ArrayList<Object> user_idList = new ArrayList<>();
        user_idList.add("userId1");
        user_idList.add("userId2");
        imageParamperterMap.put("user_id", user_idList);
        ArrayList<Object> user_nameList = new ArrayList<>();
        user_nameList.add("userName1");
        user_nameList.add("userName2");
        imageParamperterMap.put("user_name", user_nameList);
        ArrayList<Object> user_statusList = new ArrayList<>();
        user_statusList.add("userStatus1");
        user_statusList.add("userStatus2");
        imageParamperterMap.put("user_status", user_statusList);
        return imageParamperterMap;
    }
}