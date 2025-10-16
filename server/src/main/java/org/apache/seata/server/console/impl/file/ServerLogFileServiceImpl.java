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

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.server.console.entity.param.ServerLogParam;
import org.apache.seata.server.console.service.ServerLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Service
public class ServerLogFileServiceImpl implements ServerLogService {

    @Autowired
    private Environment env;

    private static final String DEFAULT_APP_NAME = "seata-server";

    private static final Integer MAX_LOG_FILE_SIZE = 500 * 1024 * 1024; // 500MB

    private final Logger LOGGER = LoggerFactory.getLogger(ServerLogFileServiceImpl.class);

    @Override
    public ResponseEntity<StreamingResponseBody> getServerLogFile(ServerLogParam serverLogParam) {
        String logPathString = buildLogFilePath(serverLogParam);
        Path logPath = Paths.get(logPathString);
        if (Files.exists(logPath)) {
            long size = 0;
            long modifyTime = 0;
            try {
                modifyTime = Files.getLastModifiedTime(logPath).toMillis();
                size = Files.size(logPath);
            } catch (IOException e) {
                LOGGER.warn("Error get log file size: {}", e.getMessage());
            }
            if (size > MAX_LOG_FILE_SIZE) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(out ->
                                out.write(("Log File exceed the Max Size: " + MAX_LOG_FILE_SIZE + " B").getBytes()));
            }
            Long lastModifyTime = serverLogParam.getLastModifyTime();
            long finalCurSize;
            if (lastModifyTime == 0 || !isSameCalendarDay(lastModifyTime, modifyTime)) {
                finalCurSize = 0;
            } else {
                finalCurSize = serverLogParam.getCurSize();
            }
            long finalSize = size;
            if (finalCurSize > size && isSameCalendarDay(lastModifyTime, modifyTime)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(out ->
                                out.write(("Log File is newer than TC's File, please check the log file").getBytes()));
            }
            StreamingResponseBody responseBody = outputStream -> {
                try (FileChannel channel = FileChannel.open(logPath, StandardOpenOption.READ)) {
                    long position = finalCurSize;
                    long remaining = finalSize;

                    while (remaining > 0) {
                        long transferred = channel.transferTo(position, remaining, Channels.newChannel(outputStream));

                        if (transferred <= 0) {
                            break;
                        }

                        position += transferred;
                        remaining -= transferred;

                        outputStream.flush();
                    }
                } catch (IOException e) {
                    LOGGER.warn("Error streaming log file: {}", e.getMessage());
                    if (e instanceof ClosedChannelException) {
                        LOGGER.info("Client closed connection during file transfer");
                    }
                }
            };
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + logPath.getFileName() + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .header("X-APPEND-NEEDED", finalCurSize == 0 ? "false" : "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("charset", "utf-8")
                    .body(responseBody);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(out -> out.write("Server error".getBytes()));
        }
    }

    public boolean isSameCalendarDay(long timestamp1, long timestamp2) {
        long days1 = timestamp1 / (1000 * 60 * 60 * 24);
        long days2 = timestamp2 / (1000 * 60 * 60 * 24);
        return days1 == days2;
    }

    private String buildLogBasePath() {
        return env.getProperty("logging.file.path", "user/logs/seata");
    }

    private String buildFilePrefix(String logType) {
        String appName = env.getProperty("spring.application.name", DEFAULT_APP_NAME);
        String port = System.getProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL, "8091");
        return appName + "." + port + "." + logType + ".";
    }

    private String buildLogFilePath(ServerLogParam param) {
        String logType = param.getLogType();
        String path = buildLogBasePath() + "/";
        String prefix = buildFilePrefix(logType);
        return path + prefix + "log";
    }
}
