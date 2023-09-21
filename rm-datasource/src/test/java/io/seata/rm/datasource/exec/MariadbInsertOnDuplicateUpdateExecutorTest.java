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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.seata.rm.datasource.exec.mariadb.MariadbInsertOnDuplicateUpdateExecutor;
import io.seata.sqlparser.struct.TableMeta;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.seata.common.exception.NotSupportYetException;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.PreparedStatementProxy;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.util.JdbcConstants;

/**
 * @author funkye
 */
public class MariadbInsertOnDuplicateUpdateExecutorTest extends MySQLInsertOnDuplicateUpdateExecutorTest {

    protected MariadbInsertOnDuplicateUpdateExecutor insertOrUpdateExecutor;

    @BeforeEach
    @Override
    public void init() {
        ConnectionProxy connectionProxy = mock(ConnectionProxy.class);
        when(connectionProxy.getDbType()).thenReturn(JdbcConstants.MYSQL);

        statementProxy = mock(PreparedStatementProxy.class);
        when(statementProxy.getConnectionProxy()).thenReturn(connectionProxy);

        StatementCallback statementCallback = mock(StatementCallback.class);
        sqlInsertRecognizer = mock(SQLInsertRecognizer.class);
        tableMeta = mock(TableMeta.class);
        insertOrUpdateExecutor = Mockito.spy(new MariadbInsertOnDuplicateUpdateExecutor(statementProxy, statementCallback, sqlInsertRecognizer));

        pkIndexMap = new HashMap<String,Integer>(){
            {
                put(ID_COLUMN, pkIndex);
            }
        };
    }

    @Test
    @Override
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
    @Override
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
    @Override
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
    @Override
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
    @Override
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

}
