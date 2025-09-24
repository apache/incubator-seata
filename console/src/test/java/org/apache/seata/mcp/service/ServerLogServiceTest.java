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
import org.apache.seata.mcp.entity.vo.ServerLogPageVO;
import org.apache.seata.mcp.service.impl.ServerLogServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ServerLogServiceTest {

    @Mock
    private MCPRPCService mcprpcService;

    @InjectMocks
    private ServerLogServiceImpl serverLogService;

    @TempDir
    Path tempDir;

    private NameSpaceDetail nameSpaceDetail;
    private ServerLogParam basicParam;
    private Path logFilePath;

    private static final DateTimeFormatter LOG_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @BeforeEach
    void setUp() throws IOException {
        System.setProperty("user.home", tempDir.toString());

        Path logsDir = tempDir.resolve("logs/seata/console/tmp");
        Files.createDirectories(logsDir);

        nameSpaceDetail = new NameSpaceDetail();
        nameSpaceDetail.setNamespace("test-namespace");
        nameSpaceDetail.setvGroup("test-vgroup");
        nameSpaceDetail.setCluster("test-cluster");

        basicParam = new ServerLogParam();
        basicParam.setPage(1);
        basicParam.setLogType("all");
        basicParam.setLogMessageKeyWord(Collections.singletonList("test"));

        logFilePath = logsDir.resolve("test-namespace.test-vgroup.test-cluster.all-Server.log");

        when(mcprpcService.getCallTCLogs(any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.empty());
    }

    // 1. Basic functional testing
    @Test
    void testBasicLogQuery() throws IOException {
        // Prepare test log data
        String logContent = generateTestLogContent(10, "test message");
        Files.write(logFilePath, logContent.getBytes(StandardCharsets.UTF_8));

        ServerLogPageVO<String> result = serverLogService.analyseServerLogFile(nameSpaceDetail, basicParam);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(10, result.getData().size());
        assertFalse(result.getHasMorePages());
    }

    // 2. Pagination test
    @Test
    void testPagination() throws IOException {
        String logContent = generateTestLogContent(3000, "test message");
        Files.write(logFilePath, logContent.getBytes(StandardCharsets.UTF_8));

        ServerLogPageVO<String> firstPage = serverLogService.analyseServerLogFile(nameSpaceDetail, basicParam);
        assertEquals(2500, firstPage.getData().size());
        assertTrue(firstPage.getHasMorePages());

        basicParam.setPage(2);
        ServerLogPageVO<String> secondPage = serverLogService.analyseServerLogFile(nameSpaceDetail, basicParam);
        assertEquals(500, secondPage.getData().size());
        assertEquals(500, secondPage.getTotal());
    }

    // 3. Log-level filtering tests
    @Test
    void testLogLevelFilter() throws IOException {
        String logContent = generateMixedLevelLogContent();
        Files.write(logFilePath, logContent.getBytes(StandardCharsets.UTF_8));

        basicParam.setLogMessageLevel("ERROR");
        basicParam.setLogMessageKeyWord(null);

        ServerLogPageVO<String> result = serverLogService.analyseServerLogFile(nameSpaceDetail, basicParam);

        assertFalse(result.getData().isEmpty());
        result.getData()
                .forEach(entry -> assertTrue(entry.contains("ERROR"), "SHOULD ONLY CONTAIN LOGS AT THE ERROR LEVEL"));
    }

    // 4. Keyword filtering test
    @Test
    void testKeywordFilter() throws IOException {
        String logContent = generateTestLogWithKeywords();
        Files.write(logFilePath, logContent.getBytes(StandardCharsets.UTF_8));

        basicParam.setLogMessageKeyWord(Arrays.asList("KEYWORD1", "KEYWORD2"));

        ServerLogPageVO<String> result = serverLogService.analyseServerLogFile(nameSpaceDetail, basicParam);

        result.getData().forEach(entry -> {
            boolean containsKeyword = entry.contains("KEYWORD1") || entry.contains("KEYWORD2");
            assertTrue(containsKeyword, "The specified keywords should be included");
        });
    }

    // 5. Time range filtering test
    @Test
    void testTimeRangeFilter() throws IOException {
        String logContent = generateTimeRangeLogContent();
        Files.write(logFilePath, logContent.getBytes(StandardCharsets.UTF_8));

        basicParam.setLogMessageKeyWord(null);
        basicParam.setLogMessageStartTime("2024-01-01 10:00:00");
        basicParam.setLogMessageEndTime("2024-01-01 12:00:00");

        ServerLogPageVO<String> result = serverLogService.analyseServerLogFile(nameSpaceDetail, basicParam);

        assertFalse(result.getData().isEmpty());
        result.getData().forEach(this::verifyTimeInRange);
    }

    // 6. Large file processing test
    @Test
    void testLargeFileHandling() throws IOException {
        // Create files larger than 100MB (emulated)
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 50000; i++) {
            largeContent.append(String.format(
                    "%s INFO [main] test.package.Class: Large file test message %d%n",
                    LocalDateTime.now().format(LOG_FORMATTER), i));
        }

        Files.write(logFilePath, largeContent.toString().getBytes(StandardCharsets.UTF_8));

        basicParam.setLogMessageKeyWord(Collections.singletonList("test"));

        long startTime = System.currentTimeMillis();
        ServerLogPageVO<String> result = serverLogService.analyseServerLogFile(nameSpaceDetail, basicParam);
        long endTime = System.currentTimeMillis();

        assertNotNull(result);
        assertTrue(result.isSuccess());
        // Verify performance for large file processing (should be done within a reasonable time)
        assertTrue(endTime - startTime < 10000, "Large file processing should be done in less than 10 seconds");
    }

    // 7. Empty file test
    @Test
    void testEmptyFile() throws IOException {
        Files.write(logFilePath, "".getBytes(StandardCharsets.UTF_8));

        ServerLogPageVO<String> result = serverLogService.analyseServerLogFile(nameSpaceDetail, basicParam);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().isEmpty());
        assertEquals(0, result.getTotal());
    }

    // 8. Download test when the file does not exist
    @Test
    void testFileDownload() {
        assertFalse(Files.exists(logFilePath));

        doAnswer(invocation -> {
                    String outputPath = invocation.getArgument(5);
                    String testContent = generateTestLogContent(5, "downloaded content");
                    Files.write(Paths.get(outputPath), testContent.getBytes(StandardCharsets.UTF_8));
                    return Mono.empty();
                })
                .when(mcprpcService)
                .getCallTCLogs(any(), any(), any(), any(), any(), any());

        ServerLogPageVO<String> result = serverLogService.analyseServerLogFile(nameSpaceDetail, basicParam);

        assertTrue(result.isSuccess());
        verify(mcprpcService, times(1)).getCallTCLogs(any(), any(), any(), any(), any(), any());
    }

    // 9. File expiration test
    @Test
    void testFileExpiration() throws IOException {
        // Create an old file
        Files.write(logFilePath, "old content".getBytes(StandardCharsets.UTF_8));

        // The last time to modify the file is 1 hour ago
        Files.setLastModifiedTime(
                logFilePath, java.nio.file.attribute.FileTime.from(Instant.now().minus(1, ChronoUnit.HOURS)));

        doAnswer(invocation -> {
                    String outputPath = invocation.getArgument(5);
                    String newContent = generateTestLogContent(3, "new content");
                    Files.write(Paths.get(outputPath), newContent.getBytes(StandardCharsets.UTF_8));
                    return Mono.empty();
                })
                .when(mcprpcService)
                .getCallTCLogs(any(), any(), any(), any(), any(), any());

        ServerLogPageVO<String> result = serverLogService.analyseServerLogFile(nameSpaceDetail, basicParam);

        assertTrue(result.isSuccess());
        verify(mcprpcService, times(1)).getCallTCLogs(any(), any(), any(), any(), any(), any());
    }

    // 10. Parameter validation test
    @Test
    void testInvalidParameters() {
        ServerLogParam invalidParam = new ServerLogParam();
        invalidParam.setPage(0);

        assertThrows(
                IllegalArgumentException.class,
                () -> serverLogService.analyseServerLogFile(nameSpaceDetail, invalidParam));

        invalidParam.setPage(1);
        invalidParam.setLogType("all");

        assertThrows(
                IllegalArgumentException.class,
                () -> serverLogService.analyseServerLogFile(nameSpaceDetail, invalidParam));

        invalidParam.setLogMessageStartTime("2024-01-01 10:00:00");

        assertThrows(
                IllegalArgumentException.class,
                () -> serverLogService.analyseServerLogFile(nameSpaceDetail, invalidParam));

        invalidParam.setLogMessageStartTime(null);
        invalidParam.setLogType("invalid");

        assertThrows(
                IllegalArgumentException.class,
                () -> serverLogService.analyseServerLogFile(nameSpaceDetail, invalidParam));

        invalidParam.setLogType("error");
        invalidParam.setLogMessageLevel("invalid");

        assertThrows(
                IllegalArgumentException.class,
                () -> serverLogService.analyseServerLogFile(nameSpaceDetail, invalidParam));
    }

    // 11. Composite condition filtration test
    @Test
    void testComplexFilter() throws IOException {
        String logContent = generateComplexTestLogContent();
        Files.write(logFilePath, logContent.getBytes(StandardCharsets.UTF_8));

        basicParam.setLogMessageStartTime("2024-01-01 10:30:00");
        basicParam.setLogMessageEndTime("2024-01-01 11:30:00");
        basicParam.setLogMessageKeyWord(Collections.singletonList("IMPORTANT"));
        basicParam.setLogMessageLevel("ERROR");

        ServerLogPageVO<String> result = serverLogService.analyseServerLogFile(nameSpaceDetail, basicParam);

        result.getData().forEach(entry -> {
            assertTrue(entry.contains("ERROR"), "ERROR level should be included");
            assertTrue(entry.contains("IMPORTANT"), "IMPORTANT keyword should be included");
            verifyTimeInRange(entry);
        });
    }

    // 12. Multi-line log entry testing
    @Test
    void testMultiLineLogEntries() throws IOException {
        String multiLineLog = generateMultiLineLogContent();
        Files.write(logFilePath, multiLineLog.getBytes(StandardCharsets.UTF_8));

        basicParam.setLogMessageKeyWord(Collections.singletonList("Exception"));

        ServerLogPageVO<String> result = serverLogService.analyseServerLogFile(nameSpaceDetail, basicParam);

        assertTrue(result.getData().stream()
                .anyMatch(entry -> entry.contains("Exception") && entry.contains("at com.example")));
    }

    // 13. CONCURRENT ACCESS TESTING
    @Test
    void testConcurrentAccess() throws IOException, InterruptedException {
        String logContent = generateTestLogContent(1000, "concurrent test");
        Files.write(logFilePath, logContent.getBytes(StandardCharsets.UTF_8));

        Thread[] threads = new Thread[5];
        Exception[] exceptions = new Exception[5];

        for (int i = 0; i < 5; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                try {
                    ServerLogParam param = new ServerLogParam();
                    param.setPage(1);
                    param.setLogMessageKeyWord(Collections.singletonList("concurrent"));

                    ServerLogPageVO<String> result = serverLogService.analyseServerLogFile(nameSpaceDetail, param);
                    assertNotNull(result);
                    assertTrue(result.isSuccess());
                } catch (Exception e) {
                    exceptions[threadIndex] = e;
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        for (int i = 0; i < 5; i++) {
            if (exceptions[i] != null) {
                fail("Thread " + i + " threw exception: " + exceptions[i].getMessage());
            }
        }
    }

    // 14. File cleaning function test
    @Test
    void testFileCleanup() throws IOException {
        Path expiredFile1 = tempDir.resolve("logs/seata/console/tmp/expired1.log");
        Path expiredFile2 = tempDir.resolve("logs/seata/console/tmp/expired2.log");

        Files.write(expiredFile1, "expired content 1".getBytes());
        Files.write(expiredFile2, "expired content 2".getBytes());

        Instant expiredTime = Instant.now().minus(2, ChronoUnit.MINUTES);
        Files.setLastModifiedTime(expiredFile1, java.nio.file.attribute.FileTime.from(expiredTime));
        Files.setLastModifiedTime(expiredFile2, java.nio.file.attribute.FileTime.from(expiredTime));

        serverLogService.cleanExpiredFiles();

        assertFalse(Files.exists(expiredFile1));
        assertFalse(Files.exists(expiredFile2));
    }

    private String generateTestLogContent(int count, String message) {
        StringBuilder sb = new StringBuilder();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < count; i++) {
            sb.append(String.format(
                    "%s %s [main] test.package.Class: %s %d%n",
                    now.plusSeconds(i).format(LOG_FORMATTER), "INFO", message, i));
        }

        return sb.toString();
    }

    private String generateMixedLevelLogContent() {
        StringBuilder sb = new StringBuilder();
        LocalDateTime now = LocalDateTime.now();
        String[] levels = {"INFO", "WARN", "ERROR", "DEBUG"};

        for (int i = 0; i < 20; i++) {
            String level = levels[i % levels.length];
            sb.append(String.format(
                    "%s %s [main] test.package.Class: Test message %d%n",
                    now.plusSeconds(i).format(LOG_FORMATTER), level, i));
        }

        return sb.toString();
    }

    private String generateTestLogWithKeywords() {
        StringBuilder sb = new StringBuilder();
        LocalDateTime now = LocalDateTime.now();
        String[] keywords = {"KEYWORD1", "KEYWORD2", "OTHER"};

        for (int i = 0; i < 15; i++) {
            String keyword = keywords[i % keywords.length];
            sb.append(String.format(
                    "%s INFO [main] test.package.Class: Message with %s %d%n",
                    now.plusSeconds(i).format(LOG_FORMATTER), keyword, i));
        }

        return sb.toString();
    }

    private String generateTimeRangeLogContent() {
        StringBuilder sb = new StringBuilder();
        LocalDateTime baseTime = LocalDateTime.of(2024, 1, 1, 9, 0, 0);

        for (int i = 0; i < 240; i++) {
            LocalDateTime logTime = baseTime.plusMinutes(i);
            sb.append(String.format(
                    "%s INFO [main] test.package.Class: Time range test %d%n", logTime.format(LOG_FORMATTER), i));
        }

        return sb.toString();
    }

    private String generateComplexTestLogContent() {
        StringBuilder sb = new StringBuilder();
        LocalDateTime baseTime = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
        String[] levels = {"INFO", "WARN", "ERROR"};
        String[] keywords = {"NORMAL", "IMPORTANT", "CRITICAL"};

        for (int i = 0; i < 120; i++) {
            LocalDateTime logTime = baseTime.plusMinutes(i);
            String level = levels[i % levels.length];
            String keyword = keywords[i % keywords.length];

            sb.append(String.format(
                    "%s %s [main] test.package.Class: %s message %d%n",
                    logTime.format(LOG_FORMATTER), level, keyword, i));
        }

        return sb.toString();
    }

    private String generateMultiLineLogContent() {
        StringBuilder sb = new StringBuilder();
        LocalDateTime now = LocalDateTime.now();

        sb.append(String.format("%s INFO [main] test.package.Class: Normal log message%n", now.format(LOG_FORMATTER)));

        sb.append(String.format(
                "%s ERROR [main] test.package.Class: Exception occurred%n",
                now.plusSeconds(1).format(LOG_FORMATTER)));
        sb.append("java.lang.RuntimeException: Test exception\n");
        sb.append("\tat com.example.TestClass.method(TestClass.java:123)\n");
        sb.append("\tat com.example.TestClass.main(TestClass.java:456)\n");

        sb.append(String.format(
                "%s INFO [main] test.package.Class: After exception log%n",
                now.plusSeconds(2).format(LOG_FORMATTER)));

        return sb.toString();
    }

    private void verifyTimeInRange(String logEntry) {
        try {
            String[] lines = logEntry.split("\n");
            String firstLine = lines[0];
            String timeStr = firstLine.substring(0, 23);

            LocalDateTime logTime = LocalDateTime.parse(timeStr, LOG_FORMATTER);
            LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
            LocalDateTime endTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0);

            assertTrue(
                    logTime.isAfter(startTime) || logTime.isEqual(startTime),
                    "The log time should be after the start time");
            assertTrue(
                    logTime.isBefore(endTime) || logTime.isEqual(endTime),
                    "The log time should be before the end time");
        } catch (Exception e) {
            fail("Time validation failed: " + e.getMessage());
        }
    }
}
