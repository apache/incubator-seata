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

import io.seata.common.exception.NotSupportYetException;
import io.seata.common.util.CollectionUtils;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.PreparedStatementProxy;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.exec.oceanbaseoracle.OceanBaseOracleInsertExecutor;
import io.seata.sqlparser.struct.TableMeta;
import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.struct.Null;
import io.seata.sqlparser.struct.SqlMethodExpr;
import io.seata.sqlparser.struct.SqlSequenceExpr;
import io.seata.sqlparser.util.JdbcConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opentest4j.TestAbortedException;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test cases for insert executor of OceanBaseOracle
 *
 * @author hsien999
 */
public class OceanBaseOracleInsertExecutorTest {
    /**
     * Table like: test(id, user_id, user_name, user_status)
     * Single pk: id; Multiple pks: (id, user_id)
     */
    private static final String ID_COLUMN = "id";
    private static final String USER_ID_COLUMN = "user_id";
    private static final String USER_NAME_COLUMN = "user_name";
    private static final String USER_STATUS_COLUMN = "user_status";
    private static final Integer PK_VALUE_ID = 100;
    private static final Integer PK_VALUE_USER_ID = 200;
    private final int pkIndexId = 0;
    private final int pkIndexUserId = 1;
    private ConnectionProxy connectionProxy;
    private StatementProxy statementProxy;
    private StatementCallback statementCallback;
    private SQLInsertRecognizer sqlInsertRecognizer;
    private OceanBaseOracleInsertExecutor insertExecutor;
    private TableMeta tableMeta;
    private HashMap<String, Integer> pkIndexMap;
    private HashMap<String, Integer> multiPkIndexMap;

    private HashMap<String, Integer> partialIndexMap;

    @BeforeEach
    public void init() {
        connectionProxy = mock(ConnectionProxy.class);
        when(connectionProxy.getDbType()).thenReturn(JdbcConstants.OCEANBASE_ORACLE);

        statementProxy = mock(PreparedStatementProxy.class);
        when(statementProxy.getConnectionProxy()).thenReturn(connectionProxy);

        statementCallback = mock(StatementCallback.class);
        sqlInsertRecognizer = mock(SQLInsertRecognizer.class);
        insertExecutor = Mockito.spy(new OceanBaseOracleInsertExecutor(statementProxy, statementCallback, sqlInsertRecognizer));

        tableMeta = mock(TableMeta.class);

        // single pk
        pkIndexMap = new HashMap<>();
        pkIndexMap.put(ID_COLUMN, pkIndexId);

        // multiple pks
        multiPkIndexMap = new HashMap<>();
        multiPkIndexMap.put(ID_COLUMN, pkIndexId);
        multiPkIndexMap.put(USER_ID_COLUMN, pkIndexUserId);

        // multiple pks without full values
        partialIndexMap = new HashMap<>();
        partialIndexMap.put(USER_ID_COLUMN, pkIndexUserId - 1);
    }

    @Test
    public void testGetPkValuesBySeq() throws Exception {
        // statement like: INSERT INTO test(id, user_id, user_name, user_status) VALUES (?, ?, ?, ?)
        // mock: pk = (id), values = (sequence, 'test', 'test', 'test'), seq value = PK_VALUE_ID(100)
        // verify: #getPkValuesByColumn returns {id=100}
        boolean multiple = false;
        mockPkColumnNames(multiple);
        mockInsertColumns(); // non essential
        mockPkIndexMap(multiple);
        mockInsertRows(multiple);
        SqlSequenceExpr expr = mockParametersWithPkSeq(multiple);

        List<Object> pkValuesSeq = Collections.singletonList(PK_VALUE_ID);
        doReturn(pkValuesSeq).when(insertExecutor).getPkValuesBySequence(expr, ID_COLUMN);

        Map<String, List<Object>> pkValuesByColumn = insertExecutor.getPkValuesByColumn();
        verify(insertExecutor, times(1)).getPkValuesBySequence(expr, ID_COLUMN);
        Assertions.assertEquals(pkValuesSeq, pkValuesByColumn.get(ID_COLUMN));
    }

    @Test
    public void testGetPkValuesBySeqWithPks() throws Exception {
        // statement like: INSERT INTO test(id, user_id, user_name, user_status) VALUES (?, ?, ?, ?)
        // mock: pk = (id, user_id), values = (sequence, sequence, 'test', 'test'),
        //      seq values = (PK_VALUE_ID(100), PK_VALUE_USER_ID(200))
        // verify: #getPkValuesByColumn returns {id=100, user_id=200}
        boolean multiple = true;
        mockPkColumnNames(multiple);
        mockInsertColumns(); // non essential
        mockPkIndexMap(multiple);
        mockInsertRows(multiple);
        SqlSequenceExpr expr = mockParametersWithPkSeq(multiple);

        List<Object> pkValuesSeq1 = Collections.singletonList(PK_VALUE_ID);
        doReturn(pkValuesSeq1).when(insertExecutor).getPkValuesBySequence(expr, ID_COLUMN);
        List<Object> pkValuesSeq2 = Collections.singletonList(PK_VALUE_USER_ID);
        doReturn(pkValuesSeq2).when(insertExecutor).getPkValuesBySequence(expr, USER_ID_COLUMN);

        Map<String, List<Object>> pkValuesByColumn = insertExecutor.getPkValuesByColumn();
        verify(insertExecutor, times(1)).getPkValuesBySequence(expr, ID_COLUMN);
        verify(insertExecutor, times(1)).getPkValuesBySequence(expr, USER_ID_COLUMN);
        Assertions.assertEquals(2, pkValuesByColumn.size());
        Assertions.assertEquals(pkValuesSeq1, pkValuesByColumn.get(ID_COLUMN));
        Assertions.assertEquals(pkValuesSeq2, pkValuesByColumn.get(USER_ID_COLUMN));
    }

    @Test
    public void testGetPkValuesByAuto() throws Exception {
        // statement like: INSERT INTO test(id, user_id, user_name, user_status) VALUES (?, ?, ?, ?)
        // mock: pk = (id), values = (null, 'test', 'test', 'test'), auto value = PK_VALUE_ID(100)
        // verify: #getPkValuesByColumn returns {id=100}
        boolean multiple = false;
        mockPkColumnNames(multiple);
        mockInsertColumns(); // non essential
        mockPkIndexMap(multiple);
        mockInsertRows(multiple);
        mockParametersWithPkSeq(multiple, Collections.singletonList("null"));

        List<Object> pkValuesAuto = Collections.singletonList(PK_VALUE_ID);
        doReturn(pkValuesAuto).when(insertExecutor).getGeneratedKeys(ID_COLUMN);

        Map<String, List<Object>> pkValuesByColumn = insertExecutor.getPkValuesByColumn();
        verify(insertExecutor, times(1)).getGeneratedKeys(ID_COLUMN);
        Assertions.assertEquals(pkValuesAuto, pkValuesByColumn.get(ID_COLUMN));
    }

    @Test
    public void testGetPkValuesByAutoWithPks() throws Exception {
        // statement like: INSERT INTO test(id, user_id, user_name, user_status) VALUES (?, ?, ?, ?)
        // mock: pk = (id, user_id), values = (null, sequence, 'test', 'test'),
        //      auto and seq values = (PK_VALUE_ID(100), PK_VALUE_USER_ID(200))
        // verify: #getPkValuesByColumn returns {id=100, user_id=200}
        boolean multiple = true;
        mockPkColumnNames(multiple);
        mockInsertColumns(); // non essential
        mockPkIndexMap(multiple);
        mockInsertRows(multiple);
        SqlSequenceExpr expr = mockParametersWithPkSeq(multiple, Collections.singletonList("null"));

        List<Object> pkValuesAuto = Collections.singletonList(PK_VALUE_ID);
        doReturn(pkValuesAuto).when(insertExecutor).getGeneratedKeys(ID_COLUMN);
        List<Object> pkValuesSeq = Collections.singletonList(PK_VALUE_USER_ID);
        doReturn(pkValuesSeq).when(insertExecutor).getPkValuesBySequence(expr, USER_ID_COLUMN);

        Map<String, List<Object>> pkValuesByColumn = insertExecutor.getPkValuesByColumn();
        verify(insertExecutor, times(1)).getGeneratedKeys(ID_COLUMN);
        verify(insertExecutor, times(1)).getPkValuesBySequence(expr, USER_ID_COLUMN);
        Assertions.assertEquals(2, pkValuesByColumn.size());
        Assertions.assertEquals(pkValuesAuto, pkValuesByColumn.get(ID_COLUMN));
        Assertions.assertEquals(pkValuesSeq, pkValuesByColumn.get(USER_ID_COLUMN));
    }

    @Test
    public void testGetPkValuesByMixedWithPks() throws Exception {
        // statement like: INSERT INTO test(user_id, user_name, user_status) VALUES (?, ?, ?)
        // mock: pk = (id, user_id), values = (sequence, 'test', 'test'),
        //      auto and seq values = (PK_VALUE_ID(100), PK_VALUE_USER_ID(200))
        // verify: #getPkValuesByColumn returns {id=100, user_id=200}
        boolean multiple = true;
        boolean partial = true;
        mockPkColumnNames(multiple);
        mockInsertColumns(partial); // non essential
        mockPkIndexMap(multiple, partial);
        mockInsertRows(multiple, partial);
        SqlSequenceExpr expr = mockParametersWithPkSeq(multiple, partial, Collections.singletonList("sequence"));

        List<Object> pkValuesAuto = Collections.singletonList(PK_VALUE_ID);
        doReturn(pkValuesAuto).when(insertExecutor).getGeneratedKeys(ID_COLUMN);
        List<Object> pkValuesSeq = Collections.singletonList(PK_VALUE_USER_ID);
        doReturn(pkValuesSeq).when(insertExecutor).getPkValuesBySequence(expr, USER_ID_COLUMN);

        Map<String, List<Object>> pkValues = insertExecutor.getPkValues();
        verify(insertExecutor, times(1)).getGeneratedKeys(ID_COLUMN);
        verify(insertExecutor, times(1)).getPkValuesBySequence(expr, USER_ID_COLUMN);
        Assertions.assertEquals(2, pkValues.size());
        Assertions.assertEquals(pkValuesAuto, pkValues.get(ID_COLUMN));
        Assertions.assertEquals(pkValuesSeq, pkValues.get(USER_ID_COLUMN));
    }

    @Test
    public void testGetPkValuesByAutoError() throws Exception {
        // statement like: INSERT INTO test(id, user_id, user_name, user_status) VALUES (?, ?, ?, ?)
        boolean multiple = true;
        mockPkColumnNames(multiple);
        mockInsertColumns(); // non essential
        mockPkIndexMap(multiple);
        mockInsertRows(multiple);
        SqlSequenceExpr expr = mockParametersWithPkSeq(multiple, Collections.singletonList("null"));

        // case1: throws NotSupportYetException when #getGeneratedKeys return empty values
        // mock: pk = (id, user_id), values = (null, sequence, 'test', 'test'), throws from: #getGeneratedKeys
        ResultSet rs = mock(ResultSet.class);
        doReturn(rs).when(statementProxy).getGeneratedKeys();
        doReturn(false).when(rs).next();

        List<Object> pkValuesSeq = Collections.singletonList(PK_VALUE_USER_ID);
        doReturn(pkValuesSeq).when(insertExecutor).getPkValuesBySequence(expr, USER_ID_COLUMN);

        Assertions.assertThrows(NotSupportYetException.class, () -> insertExecutor.getGeneratedKeys(ID_COLUMN));
        Assertions.assertThrows(NotSupportYetException.class, () -> insertExecutor.getPkValuesByColumn());

        // case2: throws NotSupportYetException error on primary key check failure
        // conditions: most one null per row & method is not allowed for multiple pks
        // mock: pk = (id, user_id), values = (null, null, 'test', 'test'), throws from: #checkPkValues
        mockParametersWithPkSeq(multiple, Arrays.asList("null", "null"));
        Assertions.assertThrows(NotSupportYetException.class, () -> insertExecutor.getPkValuesByColumn());
        // mock: pk = (id, user_id), values = (null, method, 'test', 'test'), throws from: #checkPkValues
        mockParametersWithPkSeq(multiple, Arrays.asList("null", "method"));
        Assertions.assertThrows(NotSupportYetException.class, () -> insertExecutor.getPkValuesByColumn());
    }


    private void mockPkColumnNames(boolean multiple) {
        // mock #getPrimaryKeyOnlyName (called in #getPkValues, #parsePkValuesFromStatement#getPkIndex#containPK)
        Map<String, Integer> indexMap = multiple ? multiPkIndexMap : pkIndexMap;
        doReturn(tableMeta).when(insertExecutor).getTableMeta();
        when(tableMeta.getPrimaryKeyOnlyName()).thenReturn(new ArrayList<>(indexMap.keySet()));
    }


    private void mockInsertColumns() {
        mockInsertColumns(false);
    }

    private void mockInsertColumns(boolean partial) {
        // mock #getInsertColumns (called in #parsePkValuesFromStatement#getPkIndex)
        List<String> fullColumns = Arrays.asList(ID_COLUMN, USER_ID_COLUMN, USER_NAME_COLUMN, USER_STATUS_COLUMN);
        if (partial) {
            fullColumns = Arrays.asList(USER_ID_COLUMN, USER_NAME_COLUMN, USER_STATUS_COLUMN);
        }
        when(sqlInsertRecognizer.getInsertColumns()).thenReturn(fullColumns);
    }

    private void mockPkIndexMap(boolean multiple) {
        mockPkIndexMap(multiple, false);
    }

    private void mockPkIndexMap(boolean multiple, boolean partial) {
        // mock #getInsertColumns (called in #parsePkValuesFromStatement)
        Map<String, Integer> indexMap = multiple ? multiPkIndexMap : pkIndexMap;
        if (multiple && partial) {
            indexMap = partialIndexMap;
        }
        doReturn(indexMap).when(insertExecutor).getPkIndex();
    }

    private void mockInsertRows(boolean multiple) {
        mockInsertRows(multiple, false);
    }

    private void mockInsertRows(boolean multiple, boolean partial) {
        // mock #getInsertRows returns(called in #parsePkValuesFromStatement)
        Map<String, Integer> indexMap = multiple ? multiPkIndexMap : pkIndexMap;
        List<List<Object>> rows = Collections.singletonList(Arrays.asList("?", "?", "?", "?"));
        if (multiple && partial) {
            indexMap = partialIndexMap;
            rows = Collections.singletonList(Arrays.asList("?", "?", "?"));
        }
        when(sqlInsertRecognizer.getInsertRows(indexMap.values())).thenReturn(rows);
    }

    private SqlSequenceExpr mockParametersWithPkSeq(boolean multiple) {
        return mockParametersWithPkSeq(multiple, false, null);
    }

    private SqlSequenceExpr mockParametersWithPkSeq(boolean multiple, List<String> autoTypes) {
        return mockParametersWithPkSeq(multiple, false, autoTypes);
    }

    private SqlSequenceExpr mockParametersWithPkSeq(boolean multiple, boolean partial, List<String> autoTypes) {
        // mock #getParameters returns(called in #parsePkValuesFromStatement)
        SqlSequenceExpr expr = new SqlSequenceExpr("seq", "nextval");
        Map<Integer, ArrayList<Object>> parameters = new HashMap<>();
        parameters.put(1, new ArrayList<>(Collections.singletonList(expr)));
        parameters.put(2, new ArrayList<>(Collections.singletonList("test")));
        parameters.put(3, new ArrayList<>(Collections.singletonList("test")));
        parameters.put(4, new ArrayList<>(Collections.singletonList("test")));

        if (multiple) {
            // simply use the same sequence with no adverse effects
            parameters.get(2).set(0, expr);
            if (partial) {
                parameters.remove(1);
                parameters.put(1, parameters.get(2));
                parameters.put(2, parameters.get(3));
                parameters.put(3, parameters.get(4));
                parameters.remove(4);
            }
        }
        if (!CollectionUtils.isEmpty(autoTypes) && autoTypes.size() <= parameters.size()) {
            for (int i = 0; i < autoTypes.size(); ++i) {
                Object o;
                switch (autoTypes.get(i).toLowerCase()) {
                    case "sequence":
                        o = expr;
                        break;
                    case "null":
                        o = Null.get();
                        break;
                    case "method":
                        o = SqlMethodExpr.get();
                        break;
                    default:
                        throw new TestAbortedException("Unknown auto type for OceanBaseOracle");
                }
                parameters.get(i + 1).set(0, o);
            }
        }
        PreparedStatementProxy pstProxy = (PreparedStatementProxy) this.statementProxy;
        when(pstProxy.getParameters()).thenReturn(parameters);
        return expr;
    }
}
