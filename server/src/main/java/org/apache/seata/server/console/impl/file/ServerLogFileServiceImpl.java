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

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

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
            try {
                size = Files.size(logPath);
            } catch (IOException e) {
                LOGGER.warn("Error get log file size: {}", e.getMessage());
            }
            if (size > MAX_LOG_FILE_SIZE) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(out ->
                                out.write(("Log File exceed the Max Size: " + MAX_LOG_FILE_SIZE + " B").getBytes()));
            }
            long totalLines = 0;
            try (Stream<String> lines = Files.lines(logPath)) {
                totalLines = lines.count();
            } catch (IOException e) {
                LOGGER.warn("Error get log total lines: {}", e.getMessage());
            }
            StreamingResponseBody responseBody = outputStream -> {
                try (FileChannel channel = FileChannel.open(logPath, StandardOpenOption.READ)) {
                    ByteBuffer buffer = ByteBuffer.allocate(512 * 1024);

                    while (channel.read(buffer) != -1) {
                        buffer.flip();
                        outputStream.write(buffer.array(), 0, buffer.limit());
                        outputStream.flush();
                        buffer.clear();
                    }
                } catch (IOException e) {
                    LOGGER.warn("Error streaming log file: {}", e.getMessage());
                }
            };
            return ResponseEntity.ok()
                    .header("X-Log-Total-Lines", String.valueOf(totalLines))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + logPath.getFileName() + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("charset", "utf-8")
                    .body(responseBody);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(out -> out.write("Server error".getBytes()));
        }
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
