// package io.seata.rm.datasource.exec;
//
// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
//
// import io.seata.rm.datasource.ConnectionProxy;
// import io.seata.rm.datasource.PreparedStatementProxy;
// import io.seata.rm.datasource.StatementProxy;
// import io.seata.rm.datasource.exec.mysql.MySQLInsertOrUpdateExecutor;
// import io.seata.rm.datasource.exec.mysql.MySQLReplaceIntoExecutor;
// import io.seata.rm.datasource.sql.struct.TableMeta;
// import io.seata.sqlparser.SQLInsertRecognizer;
// import io.seata.sqlparser.util.JdbcConstants;
// import org.junit.jupiter.api.Assertions;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.mockito.Mockito;
//
// import static org.mockito.Mockito.mock;
// import static org.mockito.Mockito.when;
//
// /**
//  * @author jingliu_xiong@foxmail.com
//  */
// public class MySQLReplaceIntoExecutorTest {
//
//     private static final String ID_COLUMN = "id";
//     private static final String USER_ID_COLUMN = "user_id";
//     private static final String USER_NAME_COLUMN = "user_name";
//     private static final String USER_STATUS_COLUMN = "user_status";
//     private static final Integer PK_VALUE = 100;
//
//     private StatementProxy statementProxy;
//
//     private SQLInsertRecognizer sqlInsertRecognizer;
//
//     private TableMeta tableMeta;
//
//     private MySQLReplaceIntoExecutor mySQLReplaceIntoExecutor;
//
//     private final int pkIndex = 0;
//     private HashMap<String,Integer> pkIndexMap;
//
//     @BeforeEach
//     public void init() {
//         ConnectionProxy connectionProxy = mock(ConnectionProxy.class);
//         when(connectionProxy.getDbType()).thenReturn(JdbcConstants.MYSQL);
//
//         statementProxy = mock(PreparedStatementProxy.class);
//         when(statementProxy.getConnectionProxy()).thenReturn(connectionProxy);
//
//         StatementCallback statementCallback = mock(StatementCallback.class);
//         sqlInsertRecognizer = mock(SQLInsertRecognizer.class);
//         tableMeta = mock(TableMeta.class);
//         mySQLReplaceIntoExecutor = Mockito.spy(new MySQLReplaceIntoExecutor(statementProxy, statementCallback, sqlInsertRecognizer));
//
//         pkIndexMap = new HashMap<String,Integer>(){
//             {
//                 put(ID_COLUMN, pkIndex);
//             }
//         };
//     }
//
//     @Test
//     public void TestBuildImageParamperters(){
//         mockParameters();
//         List<String> insertParamsList = new ArrayList<>();
//         insertParamsList.add("?,?,?,?");
//         insertParamsList.add("?,?,?,?");
//         when(sqlInsertRecognizer.getInsertParamsValue()).thenReturn(insertParamsList);
//         mockInsertColumns();
//         Map<String, ArrayList<Object>> imageParamperterMap = mySQLReplaceIntoExecutor.buildImageParamperters(sqlInsertRecognizer);
//         Assertions.assertEquals(imageParamperterMap.toString(),mockImageParamperterMap().toString());
//     }
//
//     /**
//      * all insert params is variable
//      * {1=[100], 2=[userId1], 3=[userName1], 4=[userStatus1], 5=[101], 6=[userId2], 7=[userName2], 8=[userStatus2]}
//      */
//     private void mockParameters() {
//         Map<Integer,ArrayList<Object>> paramters = new HashMap<>(4);
//         ArrayList arrayList10 = new ArrayList<>();
//         arrayList10.add(PK_VALUE);
//         ArrayList arrayList11 = new ArrayList<>();
//         arrayList11.add("userId1");
//         ArrayList arrayList12 = new ArrayList<>();
//         arrayList12.add("userName1");
//         ArrayList arrayList13 = new ArrayList<>();
//         arrayList13.add("userStatus1");
//         paramters.put(1, arrayList10);
//         paramters.put(2, arrayList11);
//         paramters.put(3, arrayList12);
//         paramters.put(4, arrayList13);
//         ArrayList arrayList20 = new ArrayList<>();
//         arrayList20.add(PK_VALUE+1);
//         ArrayList arrayList21 = new ArrayList<>();
//         arrayList21.add("userId2");
//         ArrayList arrayList22 = new ArrayList<>();
//         arrayList22.add("userName2");
//         ArrayList arrayList23 = new ArrayList<>();
//         arrayList23.add("userStatus2");
//         paramters.put(5, arrayList20);
//         paramters.put(6, arrayList21);
//         paramters.put(7, arrayList22);
//         paramters.put(8, arrayList23);
//         PreparedStatementProxy psp = (PreparedStatementProxy) this.statementProxy;
//         when(psp.getParameters()).thenReturn(paramters);
//     }
//
// }
