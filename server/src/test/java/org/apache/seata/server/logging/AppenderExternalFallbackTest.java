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
package org.apache.seata.server.logging;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import org.apache.seata.server.BaseSpringBootTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestPropertySource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Regression test for the classpath fallback of the extended appenders: an existing deployment may
 * keep only a part of the logback files in its external configuration directory. When one of the
 * extended appender files is absent from that directory, the appender must still be attached using
 * the classpath copy shipped in the jar.
 */
@TestPropertySource(
        properties = {
            "logging.extend.logstash-appender.enabled=true",
            "logging.extend.kafka-appender.enabled=true",
            "logging.extend.metric-appender.enabled=true",
            "spring.config.additional-location=${seata.test.external.conf.dir}"
        })
public class AppenderExternalFallbackTest extends BaseSpringBootTest {

    private static final String EXTERNAL_CONF_DIR_PROPERTY = "seata.test.external.conf.dir";

    private static final String CONSOLE_PROBE = "EXTERNAL_CONSOLE_PROBE";

    private static final String ORIGINAL_EXTERNAL_CONF_DIR = System.getProperty(EXTERNAL_CONF_DIR_PROPERTY);

    private static final Path EXTERNAL_CONF_DIR = createPartialExternalConfDir();

    private static Path createPartialExternalConfDir() {
        try {
            Path confDir = Files.createTempDirectory("seata-partial-external-conf");
            Path logbackDir = Files.createDirectories(confDir.resolve("logback"));
            copyClasspathAppender("logback/console-appender.xml", logbackDir.resolve("console-appender.xml"), true);
            copyClasspathAppender("logback/file-appender.xml", logbackDir.resolve("file-appender.xml"), false);
            System.setProperty(
                    EXTERNAL_CONF_DIR_PROPERTY,
                    confDir.toAbsolutePath().toString().replace('\\', '/') + "/");
            return confDir;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot prepare the partial external configuration directory", e);
        }
    }

    private static void copyClasspathAppender(String resource, Path target, boolean markAsExternal) throws IOException {
        try (InputStream input =
                AppenderExternalFallbackTest.class.getClassLoader().getResourceAsStream(resource)) {
            Assertions.assertNotNull(input, "Missing classpath resource: " + resource);
            String content = new String(readAllBytes(input), StandardCharsets.UTF_8);
            if (markAsExternal) {
                content = content.replace("${CONSOLE_LOG_PATTERN}", CONSOLE_PROBE);
            }
            Files.write(target, content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    @AfterAll
    static void cleanUp() throws IOException {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Appender<ILoggingEvent> logstashAppender = context.getLogger("ROOT").getAppender("LOGSTASH");
        if (logstashAppender != null) {
            logstashAppender.stop();
        }
        if (ORIGINAL_EXTERNAL_CONF_DIR == null) {
            System.clearProperty(EXTERNAL_CONF_DIR_PROPERTY);
        } else {
            System.setProperty(EXTERNAL_CONF_DIR_PROPERTY, ORIGINAL_EXTERNAL_CONF_DIR);
        }
        deleteRecursively(EXTERNAL_CONF_DIR);
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

    @Test
    public void testExtendedAppendersFallBackToClasspathWhenExternalFilesAreMissing() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Set<String> attached = new HashSet<>();
        Iterator<Appender<ILoggingEvent>> appenderIterator =
                context.getLogger("ROOT").iteratorForAppenders();
        while (appenderIterator.hasNext()) {
            attached.add(appenderIterator.next().getName());
        }

        Assertions.assertTrue(
                attached.contains("ASYNC_CONSOLE"), "console appender must be loaded from the external directory");
        Assertions.assertTrue(
                attached.contains("ASYNC_FILE_ALL"), "file appender must be loaded from the external directory");
        Assertions.assertTrue(
                attached.contains("LOGSTASH"), "the logstash appender must fall back to the classpath definition");
        Assertions.assertTrue(
                attached.contains("KAFKA"), "the kafka appender must fall back to the classpath definition");
        Assertions.assertTrue(
                attached.contains("METRIC"), "the metric appender must fall back to the classpath definition");

        Appender<ILoggingEvent> asyncConsole = context.getLogger("ROOT").getAppender("ASYNC_CONSOLE");
        Assertions.assertInstanceOf(AsyncAppender.class, asyncConsole);
        Appender<ILoggingEvent> consoleAppender = ((AsyncAppender) asyncConsole).getAppender("CONSOLE");
        Assertions.assertInstanceOf(ConsoleAppender.class, consoleAppender);
        PatternLayoutEncoder encoder =
                (PatternLayoutEncoder) ((ConsoleAppender<ILoggingEvent>) consoleAppender).getEncoder();
        Assertions.assertTrue(
                encoder.getPattern().contains(CONSOLE_PROBE),
                "the console appender must be loaded from the external directory");
    }
}
