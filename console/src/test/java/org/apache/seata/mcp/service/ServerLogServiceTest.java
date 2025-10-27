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

import org.apache.seata.mcp.entity.param.ServerLogParam;
import org.apache.seata.mcp.entity.pojo.NameSpaceDetail;
import org.apache.seata.mcp.service.impl.ServerLogServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ServerLogServiceTest {

    @Mock
    private MCPRPCService mcprpcService;

    @InjectMocks
    private ServerLogServiceImpl service;

    private Object invokePrivate(String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method method = ServerLogServiceImpl.class.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        return method.invoke(service, args);
    }

    @BeforeEach
    void setUp() {
        lenient()
                .when(mcprpcService.getCallTCLogs(any(), anyString(), any(), any(), any(), anyString()))
                .thenReturn(Mono.empty());
    }

    @Test
    void testCheckLogParamWithValidParams() throws Exception {
        ServerLogParam param = new ServerLogParam();
        param.setPage(1);
        param.setLogType("error");
        param.setLogMessageLevel("error");
        invokePrivate("checkLogParam", new Class<?>[] {ServerLogParam.class}, param);
    }

    @Test
    void testCheckLogParamWithNoConditions() {
        ServerLogParam param = new ServerLogParam();
        param.setPage(1);
        param.setLogType("all");
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> service.analyseServerLogFile(createNameSpaceDetail(), param));
        assertTrue(exception.getMessage().contains("not allowed to query log data"));
    }

    @Test
    void testCheckLogParamWithStartTimeOnly() {
        ServerLogParam param = new ServerLogParam();
        param.setPage(1);
        param.setLogMessageStartTime("2024-01-01 10:00:00");
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> service.analyseServerLogFile(createNameSpaceDetail(), param));
        assertTrue(exception.getMessage().contains("start time without the end time"));
    }

    @Test
    void testCheckLogParamWithInvalidPage() {
        ServerLogParam param = new ServerLogParam();
        param.setPage(0);
        param.setLogType("error");
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> service.analyseServerLogFile(createNameSpaceDetail(), param));
        assertTrue(exception.getMessage().contains("page number must be greater than or equal to 1"));
    }

    @Test
    void testCheckLogParamWithInvalidLogType() {
        ServerLogParam param = new ServerLogParam();
        param.setPage(1);
        param.setLogType("invalid");
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> service.analyseServerLogFile(createNameSpaceDetail(), param));
        assertTrue(exception.getMessage().contains("logType parameter value is invalid"));
    }

    @Test
    void testCheckLogParamWithInvalidLogLevel() {
        ServerLogParam param = new ServerLogParam();
        param.setPage(1);
        param.setLogMessageLevel("invalid");
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> service.analyseServerLogFile(createNameSpaceDetail(), param));
        assertTrue(exception.getMessage().contains("logMessageLevel parameter value is invalid"));
    }

    @Test
    void testAnalyseServerLogFileWithValidParams() {
        ServerLogParam param = new ServerLogParam();
        param.setPage(1);
        param.setLogType("error");
        param.setLogMessageLevel("error");
        RuntimeException exception = assertThrows(
                RuntimeException.class, () -> service.analyseServerLogFile(createNameSpaceDetail(), param));
        assertTrue(exception.getMessage().contains("Failed to read large log file"));
    }

    @Test
    void testAnalyseServerLogFileWithKeywords() {
        ServerLogParam param = new ServerLogParam();
        param.setPage(1);
        param.setLogMessageKeyWord(new String[] {"error", "timeout"});

        RuntimeException exception = assertThrows(
                RuntimeException.class, () -> service.analyseServerLogFile(createNameSpaceDetail(), param));
        assertTrue(exception.getMessage().contains("Failed to read large log file"));
    }

    @Test
    void testAnalyseServerLogFileWithTimeRange() {
        ServerLogParam param = new ServerLogParam();
        param.setPage(1);
        param.setLogMessageStartTime("2024-01-01 10:00:00");
        param.setLogMessageEndTime("2024-01-01 11:00:00");

        RuntimeException exception = assertThrows(
                RuntimeException.class, () -> service.analyseServerLogFile(createNameSpaceDetail(), param));
        assertTrue(exception.getMessage().contains("Failed to read large log file"));
    }

    @Test
    void testGetLogFilePath() throws Exception {
        NameSpaceDetail detail = createNameSpaceDetail();
        ServerLogParam param = new ServerLogParam();
        param.setLogType("error");

        String filePath = (String) invokePrivate(
                "getLogFilePath", new Class<?>[] {NameSpaceDetail.class, ServerLogParam.class}, detail, param);

        assertNotNull(filePath);
        assertTrue(filePath.contains("error-Server.log"));
    }

    @Test
    void testDownloadLogFile() throws Exception {
        NameSpaceDetail detail = createNameSpaceDetail();
        ServerLogParam param = new ServerLogParam();

        Mono<Void> result = (Mono<Void>) invokePrivate(
                "downloadLogFile",
                new Class<?>[] {NameSpaceDetail.class, String.class, ServerLogParam.class},
                detail,
                "/tmp/test.log",
                param);

        assertNotNull(result);
    }

    private NameSpaceDetail createNameSpaceDetail() {
        NameSpaceDetail detail = new NameSpaceDetail();
        detail.setNamespace("test-ns");
        detail.setCluster("test-cluster");
        detail.setvGroup("test-vgroup");
        return detail;
    }
}
