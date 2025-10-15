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

import org.apache.seata.common.result.PageResult;
import org.apache.seata.mcp.entity.param.UndoLogParam;
import org.apache.seata.mcp.entity.vo.UndoLogVO;
import org.apache.seata.mcp.service.impl.UndoLogServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UndoLogServiceTest {

    @Mock
    private BusinessDataSourceService dataSourceService;

    @InjectMocks
    private UndoLogServiceImpl service;

    @Test
    void testQueryAndAnalyzeUndoLogWithValidParam() {
        UndoLogParam param = createValidUndoLogParam();
        PageResult<UndoLogVO> expectedResult = createMockPageResult();

        when(dataSourceService.getUndoLogInfo(any(UndoLogParam.class))).thenReturn(expectedResult);

        PageResult<?> result = service.queryAndAnalyzeUndoLog(param);

        assertNotNull(result);
        assertEquals(expectedResult, result);
    }

    @Test
    void testQueryAndAnalyzeUndoLogWithBranchId() {
        UndoLogParam param = createValidUndoLogParam();
        param.setBranchId("branch-123");
        PageResult<UndoLogVO> expectedResult = createMockPageResult();

        when(dataSourceService.getUndoLogInfo(any(UndoLogParam.class))).thenReturn(expectedResult);

        PageResult<?> result = service.queryAndAnalyzeUndoLog(param);

        assertNotNull(result);
        assertEquals(expectedResult, result);
    }

    @Test
    void testQueryAndAnalyzeUndoLogWithXid() {
        UndoLogParam param = createValidUndoLogParam();
        param.setXid("xid-456");
        PageResult<UndoLogVO> expectedResult = createMockPageResult();

        when(dataSourceService.getUndoLogInfo(any(UndoLogParam.class))).thenReturn(expectedResult);

        PageResult<?> result = service.queryAndAnalyzeUndoLog(param);

        assertNotNull(result);
        assertEquals(expectedResult, result);
    }

    @Test
    void testQueryAndAnalyzeUndoLogWithLogStatus() {
        UndoLogParam param = createValidUndoLogParam();
        param.setLogStatus(1);
        PageResult<UndoLogVO> expectedResult = createMockPageResult();

        when(dataSourceService.getUndoLogInfo(any(UndoLogParam.class))).thenReturn(expectedResult);

        PageResult<?> result = service.queryAndAnalyzeUndoLog(param);

        assertNotNull(result);
        assertEquals(expectedResult, result);
    }

    @Test
    void testQueryAndAnalyzeUndoLogWithCreateTime() {
        UndoLogParam param = createValidUndoLogParam();
        UndoLogParam.CreateTime createTime = new UndoLogParam.CreateTime();
        createTime.setStartTime("2024-01-01 10:00:00");
        createTime.setEndTime("2024-01-01 11:00:00");
        param.setLogCreateTime(createTime);
        PageResult<UndoLogVO> expectedResult = createMockPageResult();

        when(dataSourceService.getUndoLogInfo(any(UndoLogParam.class))).thenReturn(expectedResult);

        PageResult<?> result = service.queryAndAnalyzeUndoLog(param);

        assertNotNull(result);
        assertEquals(expectedResult, result);
    }

    @Test
    void testQueryAndAnalyzeUndoLogWithModifyTime() {
        UndoLogParam param = createValidUndoLogParam();
        UndoLogParam.ModifyTime modifyTime = new UndoLogParam.ModifyTime();
        modifyTime.setStartTime("2024-01-01 10:00:00");
        modifyTime.setEndTime("2024-01-01 11:00:00");
        param.setLogModifiedTime(modifyTime);
        PageResult<UndoLogVO> expectedResult = createMockPageResult();

        when(dataSourceService.getUndoLogInfo(any(UndoLogParam.class))).thenReturn(expectedResult);

        PageResult<?> result = service.queryAndAnalyzeUndoLog(param);

        assertNotNull(result);
        assertEquals(expectedResult, result);
    }

    @Test
    void testQueryAndAnalyzeUndoLogWithAllParams() {
        UndoLogParam param = createValidUndoLogParam();
        param.setBranchId("branch-123");
        param.setXid("xid-456");
        param.setLogStatus(0);

        UndoLogParam.CreateTime createTime = new UndoLogParam.CreateTime();
        createTime.setStartTime("2024-01-01 10:00:00");
        createTime.setEndTime("2024-01-01 11:00:00");
        param.setLogCreateTime(createTime);

        UndoLogParam.ModifyTime modifyTime = new UndoLogParam.ModifyTime();
        modifyTime.setStartTime("2024-01-01 10:00:00");
        modifyTime.setEndTime("2024-01-01 11:00:00");
        param.setLogModifiedTime(modifyTime);

        PageResult<UndoLogVO> expectedResult = createMockPageResult();

        when(dataSourceService.getUndoLogInfo(any(UndoLogParam.class))).thenReturn(expectedResult);

        PageResult<?> result = service.queryAndAnalyzeUndoLog(param);

        assertNotNull(result);
        assertEquals(expectedResult, result);
    }

    @Test
    void testQueryAndAnalyzeUndoLogWithEmptyResult() {
        UndoLogParam param = createValidUndoLogParam();
        PageResult<UndoLogVO> emptyResult = new PageResult<>();
        emptyResult.setData(new ArrayList<>());
        emptyResult.setTotal(0);

        when(dataSourceService.getUndoLogInfo(any(UndoLogParam.class))).thenReturn(emptyResult);

        PageResult<?> result = service.queryAndAnalyzeUndoLog(param);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertNotNull(result.getData());
    }

    @Test
    void testQueryAndAnalyzeUndoLogServiceException() {
        UndoLogParam param = createValidUndoLogParam();

        when(dataSourceService.getUndoLogInfo(any(UndoLogParam.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        assertThrows(RuntimeException.class, () -> service.queryAndAnalyzeUndoLog(param));
    }

    @Test
    void testQueryAndAnalyzeUndoLogWithDifferentPageSize() {
        UndoLogParam param = createValidUndoLogParam();
        param.setPageNum(2);
        param.setPageSize(50);
        PageResult<UndoLogVO> expectedResult = createMockPageResult();

        when(dataSourceService.getUndoLogInfo(any(UndoLogParam.class))).thenReturn(expectedResult);

        PageResult<?> result = service.queryAndAnalyzeUndoLog(param);

        assertNotNull(result);
        assertEquals(expectedResult, result);
    }

    private UndoLogParam createValidUndoLogParam() {
        UndoLogParam param = new UndoLogParam();
        param.setResourceId("jdbc:mysql://localhost:3306/testdb");
        param.setPageNum(1);
        param.setPageSize(100);
        return param;
    }

    private PageResult<UndoLogVO> createMockPageResult() {
        PageResult<UndoLogVO> result = new PageResult<>();
        List<UndoLogVO> data = new ArrayList<>();

        UndoLogVO vo1 = new UndoLogVO();
        vo1.setContext("test-context-1");
        vo1.setLogStatus(0);
        vo1.setLogCreated("2024-01-01 10:00:00");
        vo1.setLogModified("2024-01-01 10:00:00");
        vo1.setRollBackInfo("rollback-info-1");
        data.add(vo1);

        UndoLogVO vo2 = new UndoLogVO();
        vo2.setContext("test-context-2");
        vo2.setLogStatus(1);
        vo2.setLogCreated("2024-01-01 11:00:00");
        vo2.setLogModified("2024-01-01 11:00:00");
        vo2.setRollBackInfo("rollback-info-2");
        data.add(vo2);

        result.setData(data);
        result.setTotal(2);
        return result;
    }
}
