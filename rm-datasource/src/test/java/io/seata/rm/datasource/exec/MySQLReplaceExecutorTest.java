 package io.seata.rm.datasource.exec;

 import java.sql.SQLException;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;

 import com.google.common.collect.Lists;
 import io.seata.rm.datasource.ConnectionProxy;
 import io.seata.rm.datasource.PreparedStatementProxy;
 import io.seata.rm.datasource.StatementProxy;
 import io.seata.rm.datasource.exec.mysql.MySQLReplaceExecutor;
 import io.seata.rm.datasource.sql.struct.*;
 import io.seata.sqlparser.SQLInsertRecognizer;
 import io.seata.sqlparser.SQLReplaceRecognizer;
 import io.seata.sqlparser.util.JdbcConstants;
 import org.junit.jupiter.api.Assertions;
 import org.junit.jupiter.api.BeforeEach;
 import org.junit.jupiter.api.Test;
 import org.mockito.Mockito;

 import static org.mockito.Mockito.*;

 /**
  * @author jingliu_xiong@foxmail.com
  */
 public class MySQLReplaceExecutorTest {

     private static final String ID_COLUMN = "id";
     private static final String USER_ID_COLUMN = "user_id";
     private static final String USER_NAME_COLUMN = "user_name";
     private static final String USER_STATUS_COLUMN = "user_status";
     private static final Integer PK_VALUE = 100;

     private StatementProxy statementProxy;

     private SQLReplaceRecognizer sqlReplaceRecognizer;

     private TableMeta tableMeta;

     private MySQLReplaceExecutor mySQLReplaceExecutor;

     private final int pkIndex = 0;
     private HashMap<String,Integer> pkIndexMap;

     @BeforeEach
     public void init() {
         ConnectionProxy connectionProxy = mock(ConnectionProxy.class);
         when(connectionProxy.getDbType()).thenReturn(JdbcConstants.MYSQL);

         statementProxy = mock(PreparedStatementProxy.class);
         when(statementProxy.getConnectionProxy()).thenReturn(connectionProxy);

         StatementCallback statementCallback = mock(StatementCallback.class);
         sqlReplaceRecognizer = mock(SQLReplaceRecognizer.class);
         tableMeta = mock(TableMeta.class);
         mySQLReplaceExecutor = Mockito.spy(new MySQLReplaceExecutor(statementProxy, statementCallback, sqlReplaceRecognizer));

         pkIndexMap = new HashMap<String,Integer>(){
             {
                 put(ID_COLUMN, pkIndex);
             }
         };
     }

     @Test
     public void TestBuildImageParameters(){
         mockParameters();
         List<String> replaceParamsList = new ArrayList<>();
         replaceParamsList.add("?,?,?,?");
         replaceParamsList.add("?,?,?,?");
         when(sqlReplaceRecognizer.getReplaceValues()).thenReturn(replaceParamsList);
         List<String> columns = mockReplaceColumns();
         mockAllIndexes();
         doReturn(tableMeta).when(mySQLReplaceExecutor).getTableMeta();
         when(sqlReplaceRecognizer.getReplaceColumns()).thenReturn(columns);
         Map<String, ArrayList<Object>> imageParameterMap = mySQLReplaceExecutor.buildImageParameters(sqlReplaceRecognizer);
         Assertions.assertEquals(imageParameterMap.toString(), mockImageParameterMap().toString());
     }

     @Test
     public void TestBuildImageParameters_contain_constant(){
         mockImageParamperterMap_contain_constant();
         List<String> replaceParamsList = new ArrayList<>();
         replaceParamsList.add("?,?,?,userStatus1");
         replaceParamsList.add("?,?,?,userStatus2");
         when(sqlReplaceRecognizer.getReplaceValues()).thenReturn(replaceParamsList);
         mockReplaceColumns();
         Map<String, ArrayList<Object>> imageParameterMap = mySQLReplaceExecutor.buildImageParameters(sqlReplaceRecognizer);
         Assertions.assertEquals(imageParameterMap.toString(), mockImageParameterMap().toString());
     }

     @Test
     public void testBuildImageSQL(){
         String selectSQLStr = "SELECT *  FROM null WHERE (user_id = ? )  OR (id = ? )  OR (user_id = ? )  OR (id = ? ) ";
         String paramAppenderListStr = "[[userId1, 100], [userId2, 101]]";
         mockImageParamperterMap_contain_constant();
         List<String> replaceParamsList = new ArrayList<>();
         replaceParamsList.add("?,?,?,userStatus1");
         replaceParamsList.add("?,?,?,userStatus2");
         when(sqlReplaceRecognizer.getReplaceValues()).thenReturn(replaceParamsList);
         when(sqlReplaceRecognizer.selectQueryIsEmpty()).thenReturn(true);
         mockReplaceColumns();
         mockAllIndexes();
         String selectSQL = mySQLReplaceExecutor.buildImageSQL(tableMeta);
         Assertions.assertEquals(selectSQLStr, selectSQL);
         Assertions.assertEquals(paramAppenderListStr, mySQLReplaceExecutor.getParamAppenderList().toString());
     }

     @Test
     public void testBeforeImage(){
         mockImageParamperterMap_contain_constant();
         List<String> replaceParamsList = new ArrayList<>();
         replaceParamsList.add("?,?,?,userStatus1");
         replaceParamsList.add("?,?,?,userStatus2");
         when(sqlReplaceRecognizer.getReplaceValues()).thenReturn(replaceParamsList);
         when(sqlReplaceRecognizer.selectQueryIsEmpty()).thenReturn(true);
         mockReplaceColumns();
         mockAllIndexes();
         doReturn(tableMeta).when(mySQLReplaceExecutor).getTableMeta();
         try {
             TableRecords tableRecords = new TableRecords();
             String selectSQL = mySQLReplaceExecutor.buildImageSQL(tableMeta);
             ArrayList<List<Object>> paramAppenderList = mySQLReplaceExecutor.getParamAppenderList();
             doReturn(tableRecords).when(mySQLReplaceExecutor).buildTableRecords2(tableMeta, selectSQL, paramAppenderList);
             TableRecords tableRecordsResult = mySQLReplaceExecutor.beforeImage();
             Assertions.assertEquals(tableRecords,tableRecordsResult);
         } catch (SQLException throwables) {
             throwables.printStackTrace();
         }
     }

     private void mockParameters() {
         Map<Integer,ArrayList<Object>> paramters = new HashMap<>(4);
         ArrayList<Object> arrayList10 = new ArrayList<>();
         arrayList10.add(PK_VALUE);
         ArrayList<Object> arrayList11 = new ArrayList<>();
         arrayList11.add("userId1");
         ArrayList<Object> arrayList12 = new ArrayList<>();
         arrayList12.add("userName1");
         ArrayList<Object> arrayList13 = new ArrayList<>();
         arrayList13.add("userStatus1");
         paramters.put(1, arrayList10);
         paramters.put(2, arrayList11);
         paramters.put(3, arrayList12);
         paramters.put(4, arrayList13);
         ArrayList<Object> arrayList20 = new ArrayList<>();
         arrayList20.add(PK_VALUE + 1);
         ArrayList<Object> arrayList21 = new ArrayList<>();
         arrayList21.add("userId2");
         ArrayList<Object> arrayList22 = new ArrayList<>();
         arrayList22.add("userName2");
         ArrayList<Object> arrayList23 = new ArrayList<>();
         arrayList23.add("userStatus2");
         paramters.put(5, arrayList20);
         paramters.put(6, arrayList21);
         paramters.put(7, arrayList22);
         paramters.put(8, arrayList23);
         PreparedStatementProxy psp = (PreparedStatementProxy) this.statementProxy;
         when(psp.getParameters()).thenReturn(paramters);
     }

     private List<String> mockReplaceColumns() {
         List<String> columns = new ArrayList<>();
         columns.add(ID_COLUMN);
         columns.add(USER_ID_COLUMN);
         columns.add(USER_NAME_COLUMN);
         columns.add(USER_STATUS_COLUMN);
         when(sqlReplaceRecognizer.getReplaceColumns()).thenReturn(columns);
         return columns;
     }

     private List<String> mockReplaceColumnsEmpty() {
         List<String> columns = new ArrayList<>();
         columns.add(ID_COLUMN);
         columns.add(USER_ID_COLUMN);
         columns.add(USER_NAME_COLUMN);
         columns.add(USER_STATUS_COLUMN);
         when(sqlReplaceRecognizer.getReplaceColumns()).thenReturn(columns);
         return columns;
     }

     private void mockAllIndexes(){
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

     /**
      * exist insert parms is constant
      * {1=[100], 2=[userId1], 3=[userName1], 4=[101], 5=[userId2], 6=[userName2]}
      */
     private void mockImageParamperterMap_contain_constant() {
         Map<Integer,ArrayList<Object>> paramters = new HashMap<>(4);
         ArrayList<Object> arrayList10 = new ArrayList<>();
         arrayList10.add(PK_VALUE);
         ArrayList<Object> arrayList11 = new ArrayList<>();
         arrayList11.add("userId1");
         ArrayList<Object> arrayList12 = new ArrayList<>();
         arrayList12.add("userName1");
         paramters.put(1, arrayList10);
         paramters.put(2, arrayList11);
         paramters.put(3, arrayList12);
         ArrayList<Object> arrayList20 = new ArrayList<>();
         arrayList20.add(PK_VALUE+1);
         ArrayList<Object> arrayList21 = new ArrayList<>();
         arrayList21.add("userId2");
         ArrayList<Object> arrayList22 = new ArrayList<>();
         arrayList22.add("userName2");
         paramters.put(4, arrayList20);
         paramters.put(5, arrayList21);
         paramters.put(6, arrayList22);
         PreparedStatementProxy psp = (PreparedStatementProxy) this.statementProxy;
         when(psp.getParameters()).thenReturn(paramters);
     }

     private Map<String, ArrayList<Object>> mockImageParameterMap(){
         Map<String, ArrayList<Object>> imageParameterMap = new HashMap<>();
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
 }
