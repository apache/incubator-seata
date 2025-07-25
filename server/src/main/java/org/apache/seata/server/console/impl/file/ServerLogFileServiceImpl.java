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
import org.apache.seata.core.constants.ConfigurationKeys;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

@Service
public class ServerLogFileServiceImpl implements ServerLogService {

    @Autowired
    private Environment env;

    private static final String serverAllLog = "all";

    private static final String serverWarnLog = "warn";

    private static final String serverErrorLog = "error";

    private static final long timeoutMillis = 5000;

    private static final Integer maxLines = 1000;

    private final Logger LOGGER = LoggerFactory.getLogger(ServerLogFileServiceImpl.class);

    @Override
    public SingleResult<?> getServerLog(ServerLogParam serverLogParam) {
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        String formattedDate = currentDate.format(formatter);
        String logTime = serverLogParam.getLogTime();
        String logType = serverLogParam.getLogType();
        Integer nextLines = serverLogParam.getNextLines();
        if(nextLines>maxLines){
            return SingleResult.failure("Exceeds the maximum number of reads in a single session");
        }
        if(StringUtils.isBlank(logType)){
            logType = "all";
            serverLogParam.setLogType(logType);
        }
        if(!logType.equals(serverAllLog) && !logType.equals(serverWarnLog) && !logType.equals(serverErrorLog)){
            return SingleResult.failure("Log type error is specified, and only logs of type all, error, and warn are allowed to be queried");
        }
        if(StringUtils.isBlank(logTime) || formattedDate.equals(logTime) || serverLogParam.getCurLogNum()==null){
            // Just analyze the server logs for the latest
            ServerLogVO logVO = getLatestServerLogs(serverLogParam);
            return SingleResult.success(logVO);
        }else{
            // Analyze the history logs
            ServerLogVO logVO = getHistoryServerLogs(serverLogParam);
            return SingleResult.success(logVO);
        }
    }

    public ServerLogVO getHistoryServerLogs(ServerLogParam serverLogParam) {
        Integer logNum = serverLogParam.getCurLogNum() == null ? 0 : serverLogParam.getCurLogNum();
        Integer cursor = serverLogParam.getCursor();

        cursor = cursor == null ? 0 : cursor;

        Integer nextLines = serverLogParam.getNextLines();
        String logType = serverLogParam.getLogType();
        String logTime = serverLogParam.getLogTime();

        String logFilePath = env.getProperty("logging.file.path") + "/history/" +
                env.getProperty("spring.application.name", "seata-server") + "." +
                System.getProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL) + "." +
                logType + "." + logTime + "." + logNum + ".log.gz";

        List<String> logMessages = new ArrayList<>();
        long totalReadTime = 0L; // Total file read time
        int lineCount = 0;

        // Record the start time of the entire file read operation
        long fileOperationStartTime = System.nanoTime();

        try (FileInputStream fis = new FileInputStream(logFilePath);
             GZIPInputStream gis = new GZIPInputStream(fis);
             InputStreamReader isr = new InputStreamReader(gis, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {

            // SKIP THE PREVIOUS LINE BASED ON CURSOR
            if (cursor > 0) {
                long skipStartTime = System.nanoTime();
                for (int i = 0; i < cursor; i++) {
                    if (br.readLine() == null) {
                        break;
                    }
                }
                long skipTime = System.nanoTime() - skipStartTime;
                LOGGER.debug("Skipping line {} takes time: {} ms", cursor, skipTime / 1_000_000.0);
            }

            // Read the log for the specified number of rows
            String line;
            while (lineCount < nextLines && (totalReadTime / 1_000_000) <= timeoutMillis) {
                long lineStartTime = System.nanoTime();
                line = br.readLine();
                long lineEndTime = System.nanoTime();

                if (line != null) {
                    logMessages.add(line);
                    lineCount++;
                    totalReadTime += (lineEndTime - lineStartTime);
                } else {
                    break;
                }
            }

            cursor += lineCount;

        } catch (IOException e) {
            LOGGER.error("Failed to read file {}: {}", logFilePath, e.getMessage(), e);
        }

        // Record the end time of the entire file operation
        long fileOperationEndTime = System.nanoTime();
        long totalFileOperationTime = fileOperationEndTime - fileOperationStartTime;

        // Convert nanoseconds to milliseconds
        long actualReadTimeMs = totalReadTime / 1_000_000;
        long totalOperationTimeMs = totalFileOperationTime / 1_000_000;

        LOGGER.info("Read {} row logs, pure read time: {} ms, total operation time: {} ms",
                lineCount, actualReadTimeMs, totalOperationTimeMs);


        return new ServerLogVO(cursor, logMessages, logNum);
    }

    public ServerLogVO getLatestServerLogs(ServerLogParam serverLogParam) {
        String logType = serverLogParam.getLogType();
        Integer cursor = serverLogParam.getCursor();
        Integer nextLines = serverLogParam.getNextLines();

        // Build log file path
        String logFilePath = env.getProperty("logging.file.path") + "/" +
                env.getProperty("spring.application.name", "seata-server") + "." +
                System.getProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL) + "." +
                logType + ".log";

        File logFile = new File(logFilePath);

        // Initialize cursor and time
        cursor = cursor == null ? 0 : cursor;

        // Check if file exists
        if (!logFile.exists() || !logFile.isFile()) {
            LOGGER.warn("Log file does not exist: {}", logFilePath);
            return new ServerLogVO(cursor, new ArrayList<>());
        }

        List<String> logMessages = new ArrayList<>();
        long totalReadTime = 0L; // Total file reading time
        int actualLineCount = 0; // Actual number of valid lines read
        int totalLinesProcessed = 0; // Total lines processed (including empty lines)

        // Record start time of entire file reading operation
        long fileOperationStartTime = System.nanoTime();

        try (ReversedLinesFileReader reader = new ReversedLinesFileReader(logFile, StandardCharsets.UTF_8)) {

            // If there's a cursor, skip corresponding lines first
            if (cursor > 0) {
                long skipStartTime = System.nanoTime();
                for (int i = 0; i < cursor; i++) {
                    String skipLine = reader.readLine();
                    if (skipLine == null) {
                        break; // End of file
                    }
                }
                long skipTime = System.nanoTime() - skipStartTime;
                LOGGER.debug("Skipped {} lines in {} ms", cursor, skipTime / 1_000_000.0);
            }

            // Read specified number of log lines
            while (actualLineCount < nextLines && (totalReadTime / 1_000_000) <= timeoutMillis) {
                long lineStartTime = System.nanoTime();
                String logMessage = reader.readLine();
                long lineEndTime = System.nanoTime();

                totalLinesProcessed++;

                if (logMessage == null) {
                    // End of file
                    break;
                }

                // Only count reading time for non-empty lines
                if (StringUtils.isNotBlank(logMessage)) {
                    logMessages.add(logMessage);
                    actualLineCount++;
                    totalReadTime += (lineEndTime - lineStartTime);
                }
            }

            // Update cursor based on total lines processed (including empty lines)
            cursor += totalLinesProcessed;

        } catch (IOException e) {
            LOGGER.error("Failed to read file {}: {}", logFilePath, e.getMessage(), e);
            // Return already read data when exception occurs
            return new ServerLogVO(cursor, logMessages);
        }

        // Record end time of entire file operation
        long fileOperationEndTime = System.nanoTime();
        long totalFileOperationTime = fileOperationEndTime - fileOperationStartTime;

        // Convert nanoseconds to milliseconds
        long actualReadTimeMs = totalReadTime / 1_000_000;
        long totalOperationTimeMs = totalFileOperationTime / 1_000_000;

        LOGGER.info("Read {} valid log lines (processed {} total lines), pure read time: {} ms, total operation time: {} ms",
                actualLineCount, totalLinesProcessed, actualReadTimeMs, totalOperationTimeMs);

        // Reverse the result since we're reading backwards to maintain chronological order (latest first)
        Collections.reverse(logMessages);

        return new ServerLogVO(cursor, logMessages);
    }

    @Override
    public SingleResult<?> getHistoryServerLogNums(ServerLogParam serverLogParam){
        String logType = StringUtils.isBlank(serverLogParam.getLogType()) ? serverAllLog : serverLogParam.getLogType();
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

    // Filename pattern: prefix. Date. Index .log.gz
    private static final Pattern LOG_PATTERN =
            Pattern.compile("^.*\\.(\\d{4}-\\d{2}-\\d{2})\\.(\\d+)\\.log\\.gz$");

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
