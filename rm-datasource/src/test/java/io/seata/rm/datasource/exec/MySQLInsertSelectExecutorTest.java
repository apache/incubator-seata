package io.seata.rm.datasource.exec;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledResultSet;
import com.google.common.collect.Lists;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.rm.datasource.PreparedStatementProxy;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.exec.mysql.MySQLInsertSelectExecutor;
import io.seata.rm.datasource.mock.MockConnection;
import io.seata.rm.datasource.mock.MockDataSource;
import io.seata.rm.datasource.mock.MockDriver;
import io.seata.rm.datasource.sql.struct.ColumnMeta;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.IndexMeta;
import io.seata.rm.datasource.sql.struct.IndexType;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableMetaCache;
import io.seata.rm.datasource.sql.struct.TableMetaCacheFactory;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.sql.struct.cache.MysqlTableMetaCache;
import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author: lyx
 */
class MySQLInsertSelectExecutorTest {

    private static final String ID_COLUMN = "id";
    private static final String USER_ID_COLUMN = "user_id";
    private static final String USER_NAME_COLUMN = "user_name";
    private static final String USER_STATUS_COLUMN = "user_status";
    private static final List<Integer> PK_VALUE_LIST = Arrays.asList(100, 200);

    private StatementProxy statementProxy;

    private SQLInsertRecognizer sqlInsertRecognizer;

    private TableMeta tableMeta;

    private MySQLInsertSelectExecutorMock insertSelectExecutorMock;

    private final int pkIndex = 0;
    private HashMap<String, Integer> pkIndexMap;

    private String selectTableName = "test02";

    private String selectSQL = "select *\n" + "from " + selectTableName + "\n" + "where id = ?";

    private String tableName = "test01";

    private String originSQL = "insert ignore into " + tableName + "\n" +
            "select * from test02\n where id in (?,?);";

    private TableMeta selectTableMeta;

    @BeforeEach
    public void init() throws SQLException {
        List<String> returnValueColumnLabels = Lists.newArrayList("id");
        Object[][] returnValue = new Object[][]{
                new Object[]{1},
        };
        Object[][] columnMetas = new Object[][]{
                new Object[]{"", "", selectTableName, "id", Types.INTEGER, "INTEGER", 64, 0, 10, 1, "", "", 0, 0, 64, 1, "NO", "YES"},
        };
        Object[][] indexMetas = new Object[][]{
                new Object[]{"PRIMARY", "id", false, "", 3, 1, "A", 34},
        };
        MockDriver mockDriver = new MockDriver(returnValueColumnLabels, returnValue, columnMetas, indexMetas);
        ConnectionProxy connectionProxy = mock(ConnectionProxy.class);
        when(connectionProxy.getDbType()).thenReturn(JdbcConstants.MYSQL);
        DataSourceProxy dataSourceProxy = new DataSourceProxy(new MockDataSource());
        when(connectionProxy.getDataSourceProxy()).thenReturn(dataSourceProxy);
        MockConnection mockConnection = new MockConnection(mockDriver, "", null);
        when(connectionProxy.getTargetConnection()).thenReturn(mockConnection);
        selectTableMeta = TableMetaCacheFactory.getTableMetaCache(connectionProxy.getDbType())
                .getTableMeta(connectionProxy.getTargetConnection(), selectTableName, connectionProxy.getDataSourceProxy().getResourceId());

        statementProxy = mock(PreparedStatementProxy.class);
        when(statementProxy.getConnectionProxy()).thenReturn(connectionProxy);
        when(statementProxy.getConnection()).thenReturn(connectionProxy);

        StatementCallback statementCallback = mock(StatementCallback.class);
        sqlInsertRecognizer = mock(SQLInsertRecognizer.class);
        when(sqlInsertRecognizer.getQuerySQL()).thenReturn(selectSQL);
        when(sqlInsertRecognizer.getOriginalSQL()).thenReturn(originSQL);
        when(sqlInsertRecognizer.getTableName()).thenReturn(tableName);
        when(sqlInsertRecognizer.isIgnore()).thenReturn(true);

        this.tableMeta = mock(TableMeta.class);
        insertSelectExecutorMock = Mockito.spy(new MySQLInsertSelectExecutorMock(statementProxy, statementCallback, sqlInsertRecognizer));
        doReturn(tableMeta).when(insertSelectExecutorMock).getTableMeta(tableName);

        pkIndexMap = new HashMap<String, Integer>() {
            {
                put(ID_COLUMN, pkIndex);
            }
        };
    }

    @Test
    void getInsertRows() throws SQLException {
        String expectResult = "[[100, NOT_PLACEHOLDER, NOT_PLACEHOLDER, NOT_PLACEHOLDER], [200, NOT_PLACEHOLDER, NULL, NOT_PLACEHOLDER]]";
        mockEmptyRecognize();
        List<List<Object>> insertRows = insertSelectExecutorMock.getInsertRows(pkIndexMap.values());
        Assertions.assertEquals(expectResult, insertRows.toString());
    }

    @Test
    void getInsertParamsValue() throws SQLException {
        String expectResult = "[100, userId1, userName1, userStatus1, 200, userId2, NULL, userStatus2]";
        mockEmptyRecognize();
        List<String> insertParamsValue = insertSelectExecutorMock.getInsertParamsValue();
        Assertions.assertEquals(expectResult, insertParamsValue.toString());
    }

    @Test
    public void testGetPkValuesByColumn() throws SQLException {
        mockEmptyRecognize();
        mockInsertColumns();
        when(tableMeta.getPrimaryKeyOnlyName()).thenReturn(Arrays.asList(new String[]{ID_COLUMN}));
        doReturn(pkIndexMap).when(insertSelectExecutorMock).getPkIndex();
        Map<String, List<Object>> pkValuesList = insertSelectExecutorMock.getPkValuesByColumn();
        Assertions.assertIterableEquals(pkValuesList.get(ID_COLUMN), PK_VALUE_LIST);
    }

    @Test
    public void buildImageSQLTest() throws SQLException {
        String expectSelectSQL = "SELECT *  FROM " + tableName + " WHERE (user_id) in((?),(?)) OR (id) in((?),(?)) ";
        HashMap<List<String>, List<Object>> paramAppenderMap = new HashMap<>();
        paramAppenderMap.put(Collections.singletonList("id"), Arrays.asList(PK_VALUE_LIST.get(0), PK_VALUE_LIST.get(1)));
        paramAppenderMap.put(Collections.singletonList("user_id"), Arrays.asList("userId1", "userId2"));
        mockGetInsertParamsValue();
        mockParametersOfOnePk();
        mockInsertColumns();
        mockAllIndexes();

        TableRecords tableRecords = new TableRecords();
        mockRecordsRow(tableRecords);

        ArrayList<Object> list = new ArrayList<>();
        list.add(PK_VALUE_LIST.get(0));
        ArrayList<Object> list1 = new ArrayList<>();
        list1.add(PK_VALUE_LIST.get(1));
        ArrayList<Object> paramFromSql = new ArrayList<>();
        paramFromSql.add(list);
        paramFromSql.add(list1);
        doReturn(tableRecords).when(insertSelectExecutorMock).buildTableRecords2(selectTableMeta, selectSQL, paramFromSql);
        insertSelectExecutorMock.superCreateInsertRecognizer();

        String imagesSQL = insertSelectExecutorMock.buildImageSQL(tableMeta);
        Assertions.assertEquals(expectSelectSQL, imagesSQL);
        Assertions.assertEquals(paramAppenderMap.toString(), insertSelectExecutorMock.getParamAppenderMap().toString());
    }

    @Test
    public void beforeImageTest() throws SQLException {
        mockInsertColumns();
        mockAllIndexes();
        mockEmptyRecognize();
        String imagesSQL = insertSelectExecutorMock.buildImageSQL(tableMeta);
        HashMap<List<String>, List<Object>> paramAppenderMap = insertSelectExecutorMock.getParamAppenderMap();
        insertSelectExecutorMock.setSelectSQL(imagesSQL);
        TableRecords tableRecords = new TableRecords();
        doReturn(tableRecords).when(insertSelectExecutorMock).buildTableRecords2(tableMeta, imagesSQL, new ArrayList<>(paramAppenderMap.values()));
        TableRecords resultRecords = insertSelectExecutorMock.beforeImage();
        Assertions.assertEquals(resultRecords,tableRecords);
    }

    private void mockEmptyRecognize() throws SQLException {
        TableRecords tableRecords = new TableRecords();
        mockRecordsRow(tableRecords);
        doReturn(tableRecords).when(insertSelectExecutorMock).buildTableRecords2(selectTableMeta, selectSQL, Collections.EMPTY_LIST);
        insertSelectExecutorMock.superCreateInsertRecognizer();
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

    private void mockGetInsertParamsValue() {
        List<String> insertParamsList = new ArrayList<>();
        insertParamsList.add("?,?,?,userStatus1");
        insertParamsList.add("?,?,?,userStatus2");
        when(sqlInsertRecognizer.getInsertParamsValue()).thenReturn(insertParamsList);
    }

    private void mockParametersOfOnePk() {
        Map<Integer, ArrayList<Object>> paramters = new HashMap<>(4);
        ArrayList arrayList1 = new ArrayList<>();
        arrayList1.add(PK_VALUE_LIST.get(0));
        paramters.put(1, arrayList1);
        ArrayList arrayList2 = new ArrayList<>();
        arrayList2.add(PK_VALUE_LIST.get(1));
        paramters.put(2, arrayList2);
        PreparedStatementProxy psp = (PreparedStatementProxy) this.statementProxy;
        when(psp.getParameters()).thenReturn(paramters);
    }

    private void mockAllIndexes() {
        List<String> primaryKeyOnlyNameList = new ArrayList<>();
        primaryKeyOnlyNameList.add("id");

        Map<String, IndexMeta> allIndex = new LinkedHashMap<>();
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

    private void mockRecordsRow(TableRecords tableRecords) {
        List<Row> rows = new ArrayList<>();
        Row row01 = new Row();
        Field field01 = new Field();
        field01.setValue(PK_VALUE_LIST.get(0));
        Field field02 = new Field();
        field02.setValue("userId1");
        Field field03 = new Field();
        field03.setValue("userName1");
        Field field04 = new Field();
        field04.setValue("userStatus1");
        List<Field> fields = new ArrayList<>();
        fields.add(field01);
        fields.add(field02);
        fields.add(field03);
        fields.add(field04);
        row01.setFields(fields);
        rows.add(row01);

        Row row02 = new Row();
        Field field001 = new Field();
        field001.setValue(PK_VALUE_LIST.get(1));
        Field field002 = new Field();
        field002.setValue("userId2");
        Field field003 = new Field();
        field003.setValue(null);
        Field field004 = new Field();
        field004.setValue("userStatus2");
        List<Field> fields01 = new ArrayList<>();
        fields01.add(field001);
        fields01.add(field002);
        fields01.add(field003);
        fields01.add(field004);
        row02.setFields(fields01);
        rows.add(row02);

        tableRecords.setRows(rows);
    }

    /**
     * the class for mock
     */
    class MySQLInsertSelectExecutorMock extends MySQLInsertSelectExecutor {

        public MySQLInsertSelectExecutorMock(StatementProxy statementProxy, StatementCallback statementCallback, SQLRecognizer sqlRecognizer) throws SQLException {
            super(statementProxy, statementCallback, sqlRecognizer);
        }

        // just for mock
        @Override
        public void createInsertRecognizer() throws SQLException {
        }

        // just for test
        public void superCreateInsertRecognizer() throws SQLException {
            super.createInsertRecognizer();
        }
    }
}