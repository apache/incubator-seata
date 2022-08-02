package io.seata.rm.datasource.exec;

import com.google.common.collect.Lists;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.rm.datasource.PreparedStatementProxy;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.exec.oracle.OracleInsertSelectExecutor;
import io.seata.rm.datasource.mock.MockConnection;
import io.seata.rm.datasource.mock.MockDataSource;
import io.seata.rm.datasource.mock.MockDriver;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableMetaCacheFactory;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author: lyx
 */

public class OracleInsertSelectExecutorTest {


    private static final String ID_COLUMN = "id";
    private static final String USER_ID_COLUMN = "user_id";
    private static final String USER_NAME_COLUMN = "user_name";
    private static final String USER_STATUS_COLUMN = "user_status";
    private static final List<Integer> PK_VALUE_LIST = Arrays.asList(100, 200);

    private StatementProxy statementProxy;

    private SQLInsertRecognizer sqlInsertRecognizer;

    private TableMeta tableMeta;

    private OracleInsertSelectExecutorMock insertSelectExecutorMock;

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

        List<String> columnName = Arrays.asList(ID_COLUMN, USER_ID_COLUMN, USER_NAME_COLUMN, USER_STATUS_COLUMN);
        when(sqlInsertRecognizer.getInsertColumns()).thenReturn(columnName);

        this.tableMeta = mock(TableMeta.class);
        insertSelectExecutorMock = Mockito.spy(new OracleInsertSelectExecutorMock(statementProxy, statementCallback, sqlInsertRecognizer));
        doReturn(tableMeta).when(insertSelectExecutorMock).getTableMeta(tableName);


        pkIndexMap = new HashMap<String, Integer>() {
            {
                put(ID_COLUMN, pkIndex);
            }
        };
    }


    @Test
    public void testBuildImageParamperters() throws SQLException {
        String expectResult = "[100, userId1, userName1, userStatus1, 200, userId2, NULL, userStatus2]";
        mockEmptyRecognize();
        List<String> insertParamsValue = insertSelectExecutorMock.getInsertParamsValue();
        Assertions.assertEquals(expectResult, insertParamsValue.toString());
        Map<String, ArrayList<String>> map = new HashMap<>();
        ArrayList<String> userStatusList = new ArrayList<>();
        userStatusList.add("userStatus1");
        userStatusList.add("userStatus2");
        map.put(USER_STATUS_COLUMN, userStatusList);
        ArrayList<String> userIdList = new ArrayList<>();
        userIdList.add("userId1");
        userIdList.add("userId2");
        map.put(USER_ID_COLUMN, userIdList);
        ArrayList<String> userNameList = new ArrayList<>();
        userNameList.add("userName1");
        userNameList.add("NULL");
        map.put(USER_NAME_COLUMN, userNameList);
        ArrayList<String> idList = new ArrayList<>();
        idList.add("100");
        idList.add("200");
        map.put(ID_COLUMN, idList);
        Map<String, ArrayList<Object>> resultMap = insertSelectExecutorMock.buildImageParamperters(sqlInsertRecognizer);
        Assertions.assertEquals(map.toString(),resultMap.toString());
    }

    private void mockEmptyRecognize() throws SQLException {
        TableRecords tableRecords = new TableRecords();
        mockRecordsRow(tableRecords);
        doReturn(tableRecords).when(insertSelectExecutorMock).buildTableRecords2(selectTableMeta, selectSQL, Collections.EMPTY_LIST);
        insertSelectExecutorMock.superCreateInsertRecognizer();
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
    class OracleInsertSelectExecutorMock extends OracleInsertSelectExecutor {

        public OracleInsertSelectExecutorMock(StatementProxy statementProxy, StatementCallback statementCallback, SQLRecognizer sqlRecognizer) throws SQLException {
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
