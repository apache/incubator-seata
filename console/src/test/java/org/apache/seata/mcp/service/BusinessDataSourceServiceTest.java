/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.mcp.service;

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.result.PageResult;
import org.apache.seata.mcp.entity.param.UndoLogParam;
import org.apache.seata.mcp.entity.pojo.MCPProperties;
import org.apache.seata.mcp.entity.vo.UndoLogVO;
import org.apache.seata.mcp.service.impl.BusinessDataSourceServiceImpl;
import org.apache.seata.mcp.store.SqlExecutionTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessDataSourceServiceTest {

    @Mock
    private SqlExecutionTemplate sqlExecutionTemplate;

    @Mock
    private MCPProperties mcpProperties;

    @InjectMocks
    private BusinessDataSourceServiceImpl service;

    private Object invokePrivate(String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method method = BusinessDataSourceServiceImpl.class.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        return method.invoke(service, args);
    }

    @BeforeEach
    void setUp() {
        lenient().when(mcpProperties.getQueryDuration()).thenReturn(86400000L); // 1 day
    }

    @Test
    void testGetTableNamesBySchema() {
        String resourceId = "jdbc://mysql/testdb";
        List<Map<String, Object>> mockData = new ArrayList<>();
        Map<String, Object> table1 = new HashMap<>();
        table1.put("TABLE_NAME", "user");
        table1.put("TABLE_COMMENT", "User table");
        mockData.add(table1);

        when(sqlExecutionTemplate.query(eq(resourceId), anyString(), eq("testdb")))
                .thenReturn(mockData);

        List<String> result = service.getTableNamesBySchema(resourceId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).contains("user"));
        assertTrue(result.get(0).contains("User table"));
        verify(sqlExecutionTemplate).query(eq(resourceId), anyString(), eq("testdb"));
    }

    @Test
    void testGetTableNamesBySchemaWithBlankResourceId() {
        assertThrows(StoreException.class, () -> service.getTableNamesBySchema(""));
    }

    @Test
    void testGetTableSchemaByTableName() {
        String resourceId = "jdbc://mysql/testdb";
        String tableName = "user";
        List<Map<String, Object>> mockSchema = new ArrayList<>();
        Map<String, Object> column = new HashMap<>();
        column.put("COLUMN_NAME", "id");
        column.put("DATA_TYPE", "bigint");
        mockSchema.add(column);

        when(sqlExecutionTemplate.query(eq(resourceId), anyString(), eq("testdb"), eq(tableName)))
                .thenReturn(mockSchema);

        List<Map<String, Object>> result = service.getTableSchemaByTableName(resourceId, tableName);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("id", result.get(0).get("COLUMN_NAME"));
        verify(sqlExecutionTemplate).query(eq(resourceId), anyString(), eq("testdb"), eq(tableName));
    }

    @Test
    void testGetTableSchemaByTableNameWithBlankResourceId() {
        assertThrows(StoreException.class, () -> service.getTableSchemaByTableName("", "user"));
    }

    @Test
    void testRunSql() {
        String sql = "SELECT * FROM user";
        String resourceId = "jdbc://mysql/testdb";
        List<Map<String, Object>> mockResult = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("id", 1);
        mockResult.add(row);

        when(sqlExecutionTemplate.query(resourceId, sql)).thenReturn(mockResult);

        List<Map<String, Object>> result = service.runSql(sql, resourceId);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(sqlExecutionTemplate).query(resourceId, sql);
    }

    @Test
    void testRunSqlWithUndoLog() {
        String sql = "SELECT * FROM undo_log";
        String resourceId = "jdbc://mysql/testdb";

        StoreException exception = assertThrows(StoreException.class, () -> service.runSql(sql, resourceId));
        assertTrue(exception.getMessage().contains("analyzeUndoLog"));
    }

    @Test
    void testGetUndoLogInfoBasic() {
        UndoLogParam param = new UndoLogParam();
        param.setResourceId("jdbc://mysql/testdb");
        param.setPageNum(1);
        param.setPageSize(10);

        PageResult<UndoLogVO> mockResult = new PageResult<>();
        mockResult.setTotal(0);
        mockResult.setData(new ArrayList<>());

        when(sqlExecutionTemplate.queryForUndoLogs(anyString(), anyString(), eq(1), eq(10), any()))
                .thenReturn(mockResult);

        PageResult<UndoLogVO> result = service.getUndoLogInfo(param);

        assertNotNull(result);
        verify(sqlExecutionTemplate).queryForUndoLogs(anyString(), anyString(), eq(1), eq(10), any());
    }

    @Test
    void testGetUndoLogInfoWithBranchId() {
        UndoLogParam param = new UndoLogParam();
        param.setResourceId("jdbc://mysql/testdb");
        param.setBranchId("123456");
        param.setPageNum(1);
        param.setPageSize(10);

        PageResult<UndoLogVO> mockResult = new PageResult<>();
        when(sqlExecutionTemplate.queryForUndoLogs(anyString(), anyString(), eq(1), eq(10), any()))
                .thenReturn(mockResult);

        PageResult<UndoLogVO> result = service.getUndoLogInfo(param);

        assertNotNull(result);
    }

    @Test
    void testGetUndoLogInfoWithXid() {
        UndoLogParam param = new UndoLogParam();
        param.setResourceId("jdbc://mysql/testdb");
        param.setXid("test-xid");
        param.setPageNum(1);
        param.setPageSize(10);

        PageResult<UndoLogVO> mockResult = new PageResult<>();
        when(sqlExecutionTemplate.queryForUndoLogs(anyString(), anyString(), eq(1), eq(10), any()))
                .thenReturn(mockResult);

        PageResult<UndoLogVO> result = service.getUndoLogInfo(param);

        assertNotNull(result);
    }

    @Test
    void testGetUndoLogInfoWithLogStatus() {
        UndoLogParam param = new UndoLogParam();
        param.setResourceId("jdbc://mysql/testdb");
        param.setLogStatus(0);
        param.setPageNum(1);
        param.setPageSize(10);

        PageResult<UndoLogVO> mockResult = new PageResult<>();
        when(sqlExecutionTemplate.queryForUndoLogs(anyString(), anyString(), eq(1), eq(10), any()))
                .thenReturn(mockResult);

        PageResult<UndoLogVO> result = service.getUndoLogInfo(param);

        assertNotNull(result);
    }

    @Test
    void testGetUndoLogInfoWithCreateTime() {
        UndoLogParam param = new UndoLogParam();
        param.setResourceId("jdbc://mysql/testdb");
        param.setPageNum(1);
        param.setPageSize(10);

        UndoLogParam.CreateTime createTime = new UndoLogParam.CreateTime();
        createTime.setStartTime("2024-01-01 00:00:00");
        createTime.setEndTime("2024-01-01 01:00:00");
        param.setLogCreateTime(createTime);

        PageResult<UndoLogVO> mockResult = new PageResult<>();
        when(sqlExecutionTemplate.queryForUndoLogs(anyString(), anyString(), eq(1), eq(10), any()))
                .thenReturn(mockResult);

        PageResult<UndoLogVO> result = service.getUndoLogInfo(param);

        assertNotNull(result);
    }

    @Test
    void testGetUndoLogInfoWithModifyTime() {
        UndoLogParam param = new UndoLogParam();
        param.setResourceId("jdbc://mysql/testdb");
        param.setPageNum(1);
        param.setPageSize(10);

        UndoLogParam.ModifyTime modifyTime = new UndoLogParam.ModifyTime();
        modifyTime.setStartTime("2024-01-01 00:00:00");
        modifyTime.setEndTime("2024-01-01 01:00:00");
        param.setLogModifiedTime(modifyTime);

        PageResult<UndoLogVO> mockResult = new PageResult<>();
        when(sqlExecutionTemplate.queryForUndoLogs(anyString(), anyString(), eq(1), eq(10), any()))
                .thenReturn(mockResult);

        PageResult<UndoLogVO> result = service.getUndoLogInfo(param);

        assertNotNull(result);
    }

    @Test
    void testGetUndoLogInfoWithTimeExceedingDuration() {
        UndoLogParam param = new UndoLogParam();
        param.setResourceId("jdbc://mysql/testdb");
        param.setPageNum(1);
        param.setPageSize(10);

        UndoLogParam.CreateTime createTime = new UndoLogParam.CreateTime();
        createTime.setStartTime("2024-01-01 00:00:00");
        createTime.setEndTime("2024-12-31 23:59:59");
        param.setLogCreateTime(createTime);

        assertThrows(StoreException.class, () -> service.getUndoLogInfo(param));
    }

    @Test
    void testGetUndoLogInfoWithBlankResourceId() {
        UndoLogParam param = new UndoLogParam();
        param.setResourceId("");
        param.setPageNum(1);
        param.setPageSize(10);

        assertThrows(StoreException.class, () -> service.getUndoLogInfo(param));
    }

    @Test
    void testGetSchemaNameByResourceId() throws Exception {
        assertEquals(
                "testdb",
                invokePrivate("getSchemaNameByResourceId", new Class<?>[] {String.class}, "jdbc://mysql/testdb"));
        assertEquals("", invokePrivate("getSchemaNameByResourceId", new Class<?>[] {String.class}, ""));
        assertEquals("", invokePrivate("getSchemaNameByResourceId", new Class<?>[] {String.class}, "jdbc://mysql/"));
        assertEquals("", invokePrivate("getSchemaNameByResourceId", new Class<?>[] {String.class}, "noslash"));
    }

    @Test
    void testGetOffsetAndValidationPageQuerySql() throws Exception {
        assertEquals(
                0, invokePrivate("getOffsetAndValidationPageQuerySql", new Class<?>[] {int.class, int.class}, 1, 10));
        assertEquals(
                10, invokePrivate("getOffsetAndValidationPageQuerySql", new Class<?>[] {int.class, int.class}, 2, 10));
        assertEquals(
                100,
                invokePrivate("getOffsetAndValidationPageQuerySql", new Class<?>[] {int.class, int.class}, 11, 10));
    }

    @Test
    void testGetOffsetAndValidationPageQuerySqlWithInvalidPageNum() {
        assertThrows(
                Exception.class,
                () -> invokePrivate(
                        "getOffsetAndValidationPageQuerySql", new Class<?>[] {int.class, int.class}, 0, 10));
    }

    @Test
    void testGetOffsetAndValidationPageQuerySqlWithInvalidPageSize() {
        assertThrows(
                Exception.class,
                () -> invokePrivate(
                        "getOffsetAndValidationPageQuerySql", new Class<?>[] {int.class, int.class}, 1, -1));
    }

    @Test
    void testGetOffsetAndValidationPageQuerySqlWithExceedingPageSize() {
        assertThrows(
                Exception.class,
                () -> invokePrivate(
                        "getOffsetAndValidationPageQuerySql", new Class<?>[] {int.class, int.class}, 1, 10001));
    }

    @Test
    void testGetOffsetAndValidationPageQuerySqlWithExceedingOffset() {
        assertThrows(
                Exception.class,
                () -> invokePrivate(
                        "getOffsetAndValidationPageQuerySql", new Class<?>[] {int.class, int.class}, 100001, 10));
    }
}
