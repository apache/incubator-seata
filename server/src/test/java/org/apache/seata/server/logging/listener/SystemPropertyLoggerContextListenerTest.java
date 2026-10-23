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
package org.apache.seata.server.logging.listener;

import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

class SystemPropertyLoggerContextListenerTest {

    private Path externalConfDir;

    @AfterEach
    void cleanUp() throws IOException {
        if (externalConfDir != null) {
            deleteRecursively(externalConfDir);
        }
    }

    @Test
    void marksOnlyAppendersPresentInTheExternalConfigurationDirectory() throws IOException {
        externalConfDir = Files.createTempDirectory("seata-external-conf");
        Path logbackDir = Files.createDirectories(externalConfDir.resolve("logback"));
        Files.write(logbackDir.resolve("kafka-appender.xml"), new byte[0]);

        LoggerContext context = new LoggerContext();
        context.putProperty(
                "EXTERNAL_CONF_DIR", externalConfDir.toAbsolutePath().toString());

        SystemPropertyLoggerContextListener listener = new SystemPropertyLoggerContextListener();
        listener.setContext(context);
        listener.start();

        Assertions.assertEquals("true", context.getProperty("KAFKA_APPENDER_EXTERNAL"));
        Assertions.assertEquals("false", context.getProperty("LOGSTASH_APPENDER_EXTERNAL"));
        Assertions.assertEquals("false", context.getProperty("METRIC_APPENDER_EXTERNAL"));
        Assertions.assertEquals("false", context.getProperty("CONSOLE_APPENDER_EXTERNAL"));
        Assertions.assertEquals("false", context.getProperty("FILE_APPENDER_EXTERNAL"));
    }

    @Test
    void marksNoAppendersWhenTheExternalConfigurationDirectoryIsNotConfigured() {
        LoggerContext context = new LoggerContext();
        context.putProperty("EXTERNAL_CONF_DIR", "");

        SystemPropertyLoggerContextListener listener = new SystemPropertyLoggerContextListener();
        listener.setContext(context);
        listener.start();

        Assertions.assertEquals("false", context.getProperty("KAFKA_APPENDER_EXTERNAL"));
        Assertions.assertEquals("false", context.getProperty("CONSOLE_APPENDER_EXTERNAL"));
    }

    private static void deleteRecursively(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new IllegalStateException("Cannot delete " + path, e);
                }
            });
        }
    }
}
