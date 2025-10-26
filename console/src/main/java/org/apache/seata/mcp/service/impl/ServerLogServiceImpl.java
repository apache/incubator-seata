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
package org.apache.seata.mcp.service.impl;

import org.apache.seata.common.util.StringUtils;
import org.apache.seata.mcp.entity.constant.RPCConstant;
import org.apache.seata.mcp.entity.param.ServerLogParam;
import org.apache.seata.mcp.entity.pojo.NameSpaceDetail;
import org.apache.seata.mcp.entity.vo.ServerLogPageVO;
import org.apache.seata.mcp.service.MCPRPCService;
import org.apache.seata.mcp.service.ServerLogService;
import org.apache.seata.mcp.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ServerLogServiceImpl implements ServerLogService {

    @Autowired
    private MCPRPCService mcprpcService;

    private static final int SERVER_LOG_PAGE_SIZE = 2500;
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerLogServiceImpl.class);
    private static final DateTimeFormatter LOG_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final ReentrantReadWriteLock fileLock = new ReentrantReadWriteLock();

    @Override
    public ServerLogPageVO<String> analyseServerLogFile(NameSpaceDetail nameSpaceDetail, ServerLogParam param) {
        if (nameSpaceDetail == null || !nameSpaceDetail.isValid())
            return ServerLogPageVO.failure("", "Invalid NameSpace");

        checkLogParam(param);

        int pageNum = param.getPage();
        int skipCounts = (pageNum - 1) * SERVER_LOG_PAGE_SIZE;

        Pattern timestampPattern = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}\\.\\d{3})");

        String filePath = getLogFilePath(nameSpaceDetail, param);

        OptimizedPageCollector collector;
        collector = processLargeFile(filePath, param, timestampPattern, skipCounts);

        return ServerLogPageVO.success(
                collector.getPageEntries(),
                collector.getTotalCount(),
                collector.canDetermineNextPage(),
                pageNum,
                SERVER_LOG_PAGE_SIZE);
    }

    private OptimizedPageCollector processLargeFile(
            String filePath, ServerLogParam param, Pattern timestampPattern, int skipCounts) {
        OptimizedPageCollector collector =
                new OptimizedPageCollector(param, timestampPattern, skipCounts, SERVER_LOG_PAGE_SIZE);

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null && !collector.isComplete()) {
                collector.processLine(line);
            }
            collector.finish();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read large log file", e);
        }

        return collector;
    }

    private String getLogFilePath(NameSpaceDetail nameSpaceDetail, ServerLogParam param) {
        String namespace = nameSpaceDetail.getNamespace();
        String vGroup = nameSpaceDetail.getvGroup();
        if (vGroup == null) vGroup = "";
        String cluster = nameSpaceDetail.getCluster();
        if (cluster == null) cluster = "";
        String key = namespace + "." + vGroup + "." + cluster + "." + param.getLogType();

        String fileName = key + "-Server.log";
        Path filePath = Paths.get(System.getProperty("user.home"), "logs", "seata", "console", "tmp", fileName);

        fileLock.writeLock().lock();
        try {
            if (Files.exists(filePath)) {
                try {
                    BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
                    param.setLastModifyTime(attrs.lastModifiedTime().toMillis());
                    param.setCurSize(attrs.size());
                } catch (IOException e) {
                    LOGGER.warn("Failed to recheck file attributes: {}", filePath, e);
                }
            }

            Files.createDirectories(filePath.getParent());
            downloadLogFile(nameSpaceDetail, filePath.toString(), param).block();

            return filePath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create directories", e);
        } finally {
            fileLock.writeLock().unlock();
        }
    }

    private Mono<Void> downloadLogFile(NameSpaceDetail nameSpaceDetail, String outputFilePath, ServerLogParam param) {
        return mcprpcService.getCallTCLogs(
                nameSpaceDetail,
                RPCConstant.SERVER_LOG_BASE_URL + "/getCurrentServerLogFile",
                param,
                null,
                null,
                outputFilePath);
    }

    private void checkLogParam(ServerLogParam logParam) {
        Integer page = logParam.getPage();
        List<String> logMessageKeyWord = logParam.getLogMessageKeyWord() != null
                ? new ArrayList<>(Arrays.asList(logParam.getLogMessageKeyWord()))
                : new ArrayList<>();
        String logType = logParam.getLogType();
        String logMessageStartTime = logParam.getLogMessageStartTime();
        String logMessageEndTime = logParam.getLogMessageEndTime();
        String logMessageLevel = logParam.getLogMessageLevel();

        if (logMessageKeyWord.isEmpty()
                && StringUtils.isBlank(logMessageStartTime)
                && (StringUtils.isBlank(logType) || logType.equalsIgnoreCase("all"))
                && StringUtils.isBlank(logMessageLevel)
                && StringUtils.isBlank(logMessageEndTime)) {
            throw new IllegalArgumentException(
                    "It is not allowed to query log data of type all without any conditions (except page and logType).");
        }

        if (StringUtils.isNotBlank(logMessageStartTime) && StringUtils.isBlank(logMessageEndTime)) {
            throw new IllegalArgumentException("It is not allowed to determine the start time without the end time");
        }

        if (page == null || page < 1) {
            throw new IllegalArgumentException("The page number must be greater than or equal to 1");
        }

        if (StringUtils.isNotBlank(logType)) {
            if (!Arrays.asList("all", "error", "warn").contains(logType.toLowerCase())) {
                throw new IllegalArgumentException(
                        "The logType parameter value is invalid and must be: all, error, warn");
            }
        }

        if (StringUtils.isNotBlank(logMessageLevel)) {
            if (!Arrays.asList("error", "warn", "info").contains(logMessageLevel.toLowerCase())) {
                throw new IllegalArgumentException(
                        "The logMessageLevel parameter value is invalid and must be: error, warn, info");
            }
        }
    }

    private static class OptimizedPageCollector {
        private final Pattern timestampPattern;
        private final int skipCounts;
        private final int pageSize;

        private final Long startTimeMillis;
        private final Long endTimeMillis;

        private final Set<String> keywordSet;
        private final String logLevel;

        private StringBuilder currentEntry = new StringBuilder();
        private final List<String> pageEntries = new ArrayList<>();
        private int skipped = 0;
        private boolean hasMoreEntries = false;
        private boolean complete = false;

        public OptimizedPageCollector(ServerLogParam param, Pattern timestampPattern, int skipCounts, int pageSize) {
            this.timestampPattern = timestampPattern;
            this.skipCounts = skipCounts;
            this.pageSize = pageSize;

            if (StringUtils.isNotBlank(param.getLogMessageStartTime())
                    && StringUtils.isNotBlank(param.getLogMessageEndTime())) {
                this.startTimeMillis = DateUtils.convertToTimeStampFromDateTime(param.getLogMessageStartTime());
                this.endTimeMillis = DateUtils.convertToTimeStampFromDateTime(param.getLogMessageEndTime());
            } else {
                this.startTimeMillis = null;
                this.endTimeMillis = null;
            }

            if (param.getLogMessageKeyWord() != null && param.getLogMessageKeyWord().length != 0) {
                this.keywordSet = new HashSet<>(Arrays.asList(param.getLogMessageKeyWord()));
            } else {
                this.keywordSet = null;
            }

            this.logLevel = param.getLogMessageLevel();
        }

        public void processLine(String line) {
            if (complete) return;

            if (timestampPattern.matcher(line).find()) {
                flushCurrentEntry();
                currentEntry = new StringBuilder(line);
            } else if (currentEntry.length() > 0) {
                currentEntry.append("\n").append(line);
            }
        }

        public void finish() {
            flushCurrentEntry();
            complete = true;
        }

        public boolean isComplete() {
            return complete;
        }

        public boolean canDetermineNextPage() {
            return pageEntries.size() == pageSize && hasMoreEntries;
        }

        private void flushCurrentEntry() {
            if (currentEntry.length() == 0) return;

            String completeEntry = currentEntry.toString().trim();
            if (matchesFilter(completeEntry)) {
                if (skipped < skipCounts) {
                    skipped++;
                } else if (pageEntries.size() < pageSize) {
                    pageEntries.add(completeEntry);
                } else {
                    hasMoreEntries = true;
                    complete = true;
                }
            }
            currentEntry.setLength(0);
        }

        private boolean matchesFilter(String entry) {
            if (logLevel != null && !entry.toUpperCase().contains(logLevel.toUpperCase())) {
                return false;
            }

            if (keywordSet != null && !keywordSet.isEmpty()) {
                for (String key : keywordSet) {
                    if (!entry.contains(key)) {
                        return false;
                    }
                }
            }

            if (startTimeMillis != null && endTimeMillis != null) {
                return isInTimeRange(entry);
            }

            return true;
        }

        private boolean isInTimeRange(String entry) {
            try {
                Matcher matcher = timestampPattern.matcher(entry);
                if (matcher.find()) {
                    String timestampStr = matcher.group(1);
                    LocalDateTime logTime = LocalDateTime.parse(timestampStr, LOG_TIMESTAMP_FORMATTER);
                    long logMillis =
                            logTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

                    return logMillis >= startTimeMillis && logMillis <= endTimeMillis;
                }
            } catch (DateTimeParseException e) {
                LOGGER.debug("Time parsing failed: {}", e.getMessage());
            }
            return false;
        }

        public List<String> getPageEntries() {
            return pageEntries;
        }

        public boolean hasNextPage() {
            return hasMoreEntries;
        }

        public int getTotalCount() {
            return pageEntries.size();
        }
    }
}
