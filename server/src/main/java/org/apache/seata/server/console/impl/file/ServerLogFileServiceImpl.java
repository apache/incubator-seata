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
package org.apache.seata.server.console.impl.file;

import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.seata.common.result.SingleResult;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.server.console.entity.param.ServerLogParam;
import org.apache.seata.server.console.entity.vo.ServerLogVO;
import org.apache.seata.server.console.service.ServerLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

@Service
public class ServerLogFileServiceImpl implements ServerLogService {

    @Autowired
    private Environment env;

    private static final String SERVER_LOG_ALL = "all";

    private static final String SERVER_LOG_WARN = "warn";

    private static final String SERVER_LOG_ERROR = "error";

    private static final String DEFAULT_APP_NAME = "seata-server";

    private static final Integer MAX_LINES = 1000;

    private static final Pattern LOG_PATTERN =
            Pattern.compile("^.*\\.(\\d{4}-\\d{2}-\\d{2})\\.(\\d+)\\.log\\.gz$");

    private final Logger LOGGER = LoggerFactory.getLogger(ServerLogFileServiceImpl.class);

    @Override
    public SingleResult<?> getServerLog(ServerLogParam param) {
        if (param.getNextLines() > MAX_LINES) {
            return SingleResult.failure("Exceeds maximum lines limit");
        }

        if (StringUtils.isBlank(param.getLogType())) {
            param.setLogType(SERVER_LOG_ALL);
        }

        if (!validLogType(param.getLogType())) {
            return SingleResult.failure("Invalid log type");
        }

        if(param.getLogTime()!=null && !isValidDate(param.getLogTime())){
            return SingleResult.failure("Invalid log time");
        }

        boolean isHistoryLog = isHistoryLog(param);
        try {
            ServerLogVO logVO = isHistoryLog ?
                    readHistoryLogs(param) : readCurrentLogs(param);
            return SingleResult.success(logVO);
        } catch (IOException e) {
            LOGGER.error("Error reading logs: {}", e.getMessage());
            return SingleResult.failure("Log read error");
        }
    }

    @Override
    public SingleResult<?> getHistoryServerLogNums(ServerLogParam serverLogParam){
        String logType = StringUtils.isBlank(serverLogParam.getLogType()) ? SERVER_LOG_ALL : serverLogParam.getLogType();
        String logTime = serverLogParam.getLogTime();
        if(StringUtils.isBlank(logTime)){
            return SingleResult.failure("logTime is required");
        }
        String logFilePath = env.getProperty("logging.file.path") + "/" +"history";
        File dir = new File(logFilePath);
        if(!dir.isDirectory()){
            throw new IllegalArgumentException("A valid folder path must be provided");
        }
        File[] files = dir.listFiles((d, name) -> name.endsWith(".log.gz"));
        String logName = env.getProperty("spring.application.name","seata-server") + "."
                + System.getProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL) + "." + logType + "." + logTime;
        List<Integer> logNums = new ArrayList<>();
        if(files!=null){
            for(File file : files){
                String fileName = file.getName();
                if(fileName.contains(logName)){
                    // target file
                    int idx = extractIndexFromFileName(fileName);
                    if(idx>=0){
                        logNums.add(idx);
                    }
                }
            }
            return SingleResult.success(logNums);
        }else{
            return SingleResult.failure("There is no relevant log index");
        }
    }

    private boolean isHistoryLog(ServerLogParam param) {
        return param.getLogTime() != null &&
                !param.getLogTime().equals(LocalDate.now().toString()) && param.getCurLogNum()!=null;
    }

    private ServerLogVO readCurrentLogs(ServerLogParam param) throws IOException {
        String logPath = buildLogFilePath(false, param);
        return readLogFile(logPath, param.getCursor(), param.getNextLines());
    }

    private ServerLogVO readHistoryLogs(ServerLogParam param) throws IOException {
        String logPath = buildLogFilePath(true, param);
        return readGzLogFile(logPath, param.getCursor(), param.getNextLines());
    }

    private String buildLogBasePath() {
        return env.getProperty("logging.file.path", "user/logs/seata");
    }

    private String buildFilePrefix(String logType) {
        String appName = env.getProperty("spring.application.name", DEFAULT_APP_NAME);
        String port = System.getProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL, "8091");
        return appName + "." + port + "." + logType + ".";
    }

    private String buildLogFilePath(boolean isHistory, ServerLogParam param) {
        String logType = param.getLogType();
        Integer logNum = param.getCurLogNum();
        logNum = logNum==null ? 0 : logNum;
        String logTime = param.getLogTime();
        String path = buildLogBasePath() + (isHistory ? "/history/" : "/");
        String prefix = buildFilePrefix(logType);

        if (isHistory) {
            return path + prefix + logTime + "." + logNum + ".log.gz";
        } else {
            return path + prefix + "log";
        }
    }

    public static boolean isValidDate(String dateString) {
        return parseDate(dateString).isPresent();
    }

    public static Optional<LocalDate> parseDate(String dateString) {
        if (dateString == null || dateString.length() != 10) {
            return Optional.empty();
        }

        try {
            // Directly using the LocalDate parsing method
            LocalDate date = LocalDate.parse(dateString);
            return Optional.of(date);
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private boolean validLogType(String logType) {
        return SERVER_LOG_ALL.equals(logType) ||
                SERVER_LOG_WARN.equals(logType) ||
                SERVER_LOG_ERROR.equals(logType);
    }

    private ServerLogVO readLogFile(String filePath, Integer cursor, int nextLines) throws IOException {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            LOGGER.warn("Log file missing: {}", filePath);
            return new ServerLogVO(cursor != null ? cursor : 0, Collections.emptyList());
        }

        List<String> logs = new ArrayList<>();
        int newCursor = cursor != null ? cursor : 0;
        int processedLines = 0;

        try (ReversedLinesFileReader reader = new ReversedLinesFileReader(file, StandardCharsets.UTF_8)) {
            // Skip the read line
            for (int i = 0; i < newCursor; i++) {
                if (reader.readLine() == null) break;
            }

            String line;
            while (logs.size() < nextLines && (line = reader.readLine()) != null) {
                processedLines++;
                if (StringUtils.isNotBlank(line)) {
                    logs.add(line);
                }
            }

            newCursor += processedLines;
        }
        return new ServerLogVO(newCursor, logs);
    }

    private ServerLogVO readGzLogFile(String filePath, Integer cursor, int nextLines) throws IOException {
        List<String> logs = new ArrayList<>();
        int newCursor = cursor != null ? cursor : 0;
        int linesRead = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        new GZIPInputStream(Files.newInputStream(Paths.get(filePath))), StandardCharsets.UTF_8))) {

            // Skip the read line
            for (int i = 0; i < newCursor; i++) {
                if (reader.readLine() == null) break;
            }

            String line;
            while (linesRead < nextLines && (line = reader.readLine()) != null) {
                logs.add(line);
                linesRead++;
            }

            newCursor += linesRead;
        }

        return new ServerLogVO(newCursor, logs);
    }

    /**
     * Extract the index sequence number of a single log file
     * @param fileName Log file name
     * @return Index serial number, returns -1 if the format does not match
     */
    public static int extractIndexFromFileName(String fileName) {
        Matcher matcher = LOG_PATTERN.matcher(fileName);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }
}
