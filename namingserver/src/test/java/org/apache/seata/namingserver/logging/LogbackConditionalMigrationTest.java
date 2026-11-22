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
package org.apache.seata.namingserver.logging;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.status.Status;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Verifies Logback 1.5.38 configuration and native {@code <condition>} / {@code <if>} migrations
 * for {@code namingserver/src/main/resources/logback-spring-jvm.xml} and
 * {@code namingserver/src/main/resources/logback-spring.xml} using {@code namingserver}'s own
 * classpath resources.
 */
class LogbackConditionalMigrationTest {

    private static final String EXTERNAL_CONSOLE_PROBE = "EXTERNAL_MIGRATION_CONSOLE_PROBE";

    @TempDir
    Path tempDir;

    @Test
    void logbackSpringJvmXmlParsesDefaultClasspathModeWithZeroErrors() throws Exception {
        URL fileAppenderUrl =
                LogbackConditionalMigrationTest.class.getClassLoader().getResource("logback/file-appender.xml");
        Assertions.assertNotNull(fileAppenderUrl, "Missing classpath resource: logback/file-appender.xml");
        Assertions.assertFalse(
                fileAppenderUrl.toString().contains("/server/"),
                "Expected namingserver logback/file-appender.xml, but resolved: " + fileAppenderUrl);

        Path defaultLogDir = Files.createDirectories(tempDir.resolve("naming-logs-default"));
        Map<String, Object> properties = new HashMap<>();
        properties.put("logging.file.path", defaultLogDir.toString());

        LoggerContext context = configureNamingserverLogback("logback-spring-jvm.xml", properties);
        try {
            assertNoErrorOrIncludeWarnStatuses(context);
            Set<String> attached = getRootAppenderNames(context);
            Assertions.assertTrue(attached.contains("ASYNC_CONSOLE"));
            Assertions.assertTrue(attached.contains("ASYNC_FILE_ALL"));
            Assertions.assertTrue(attached.contains("ASYNC_FILE_WARN"));
            Assertions.assertTrue(attached.contains("ASYNC_FILE_ERROR"));
            assertNamingserverFileAppenders(context, defaultLogDir);
        } finally {
            context.stop();
        }
    }

    @Test
    void logbackSpringJvmXmlParsesExternalLogBashDirModeWithZeroErrors() throws Exception {
        Path externalConfDir = Files.createDirectories(tempDir.resolve("naming-external-conf"));
        Path externalLogbackDir = Files.createDirectories(externalConfDir.resolve("logback"));
        copyClasspathResourceWithOptionalProbe(
                "logback/console-appender.xml", externalLogbackDir.resolve("console-appender.xml"), true);
        copyClasspathResourceWithOptionalProbe(
                "logback/file-appender.xml", externalLogbackDir.resolve("file-appender.xml"), false);

        Path externalLogDir = Files.createDirectories(tempDir.resolve("naming-logs-external"));
        Map<String, Object> properties = new HashMap<>();
        properties.put("logging.file.path", externalLogDir.toString());
        properties.put("spring.config.additional-location", toLogbackDirProperty(externalConfDir));

        LoggerContext context = configureNamingserverLogback("logback-spring-jvm.xml", properties);
        try {
            assertNoErrorOrIncludeWarnStatuses(context);
            Set<String> attached = getRootAppenderNames(context);
            Assertions.assertTrue(attached.contains("ASYNC_CONSOLE"));
            Assertions.assertTrue(attached.contains("ASYNC_FILE_ALL"));
            Assertions.assertTrue(attached.contains("ASYNC_FILE_WARN"));
            Assertions.assertTrue(attached.contains("ASYNC_FILE_ERROR"));
            assertExternalConsoleProbe(context);
            assertNamingserverFileAppenders(context, externalLogDir);
        } finally {
            context.stop();
        }
    }

    @Test
    void defaultLogbackSpringXmlParsesClasspathModeWithZeroErrors() throws Exception {
        Path defaultLogDir = Files.createDirectories(tempDir.resolve("naming-logs-spring-xml"));
        Map<String, Object> properties = new HashMap<>();
        properties.put("logging.file.path", defaultLogDir.toString());

        LoggerContext context = configureNamingserverLogback("logback-spring.xml", properties);
        try {
            assertNoErrorOrIncludeWarnStatuses(context);
            Set<String> attached = getRootAppenderNames(context);
            Assertions.assertTrue(attached.contains("ASYNC_CONSOLE"));
            Assertions.assertTrue(attached.contains("ASYNC_FILE_ALL"));
            Assertions.assertTrue(attached.contains("ASYNC_FILE_WARN"));
            Assertions.assertTrue(attached.contains("ASYNC_FILE_ERROR"));
            assertNamingserverFileAppenders(context, defaultLogDir);
        } finally {
            context.stop();
        }
    }

    private static LoggerContext configureNamingserverLogback(String resourceName, Map<String, Object> properties)
            throws Exception {
        try (InputStream input =
                LogbackConditionalMigrationTest.class.getClassLoader().getResourceAsStream(resourceName)) {
            Assertions.assertNotNull(input, "Missing classpath resource: " + resourceName);
            LoggerContext context = new LoggerContext();
            StandardEnvironment environment = new StandardEnvironment();
            environment.getPropertySources().addFirst(new MapPropertySource("test", properties));
            JoranConfigurator configurator = createSpringBootJoranConfigurator(environment);
            configurator.setContext(context);
            configurator.doConfigure(input);
            return context;
        }
    }

    private static JoranConfigurator createSpringBootJoranConfigurator(StandardEnvironment environment)
            throws Exception {
        LoggingInitializationContext initializationContext = new LoggingInitializationContext(environment);
        Class<?> clazz = Class.forName("org.springframework.boot.logging.logback.SpringBootJoranConfigurator");
        Constructor<?> constructor = clazz.getDeclaredConstructor(LoggingInitializationContext.class);
        constructor.setAccessible(true);
        return (JoranConfigurator) constructor.newInstance(initializationContext);
    }

    private static void assertNoErrorOrIncludeWarnStatuses(LoggerContext context) {
        List<String> failures = context.getStatusManager().getCopyOfStatusList().stream()
                .filter(status -> status.getLevel() == Status.ERROR
                        || (status.getLevel() == Status.WARN && isIncludeMissingWarn(status)))
                .map(status ->
                        status.getMessage() + (status.getThrowable() != null ? " (" + status.getThrowable() + ")" : ""))
                .collect(Collectors.toList());
        Assertions.assertTrue(
                failures.isEmpty(),
                "Logback StatusManager must contain zero ERROR or include WARN statuses, but found: " + failures);
    }

    private static boolean isIncludeMissingWarn(Status status) {
        String message = status.getMessage();
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("could not find resource")
                || lower.contains("failed to build include")
                || lower.contains("failed to open")
                || lower.contains("does not exist")
                || lower.contains("include");
    }

    private static Set<String> getRootAppenderNames(LoggerContext context) {
        Set<String> names = new HashSet<>();
        Iterator<Appender<ILoggingEvent>> iterator = context.getLogger("ROOT").iteratorForAppenders();
        while (iterator.hasNext()) {
            names.add(iterator.next().getName());
        }
        return names;
    }

    private static void assertExternalConsoleProbe(LoggerContext context) {
        Appender<ILoggingEvent> asyncConsole = context.getLogger("ROOT").getAppender("ASYNC_CONSOLE");
        Assertions.assertInstanceOf(AsyncAppender.class, asyncConsole);
        Appender<ILoggingEvent> consoleAppender = ((AsyncAppender) asyncConsole).getAppender("CONSOLE");
        Assertions.assertInstanceOf(ConsoleAppender.class, consoleAppender);
        PatternLayoutEncoder encoder =
                (PatternLayoutEncoder) ((ConsoleAppender<ILoggingEvent>) consoleAppender).getEncoder();
        Assertions.assertTrue(
                encoder.getPattern().contains(EXTERNAL_CONSOLE_PROBE),
                "CONSOLE appender must be loaded from external LOG_BASH_DIR");
    }

    private static void assertNamingserverFileAppenders(LoggerContext context, Path expectedLogDir) {
        assertRollingFileAppenderPath(
                context, "ASYNC_FILE_ALL", "FILE_ALL", expectedLogDir.resolve("seata-namingserver.8081.all.log"));
        assertRollingFileAppenderPath(
                context, "ASYNC_FILE_WARN", "FILE_WARN", expectedLogDir.resolve("seata-namingserver.8081.warn.log"));
        assertRollingFileAppenderPath(
                context, "ASYNC_FILE_ERROR", "FILE_ERROR", expectedLogDir.resolve("seata-namingserver.8081.error.log"));
    }

    private static void assertRollingFileAppenderPath(
            LoggerContext context, String asyncAppenderName, String fileAppenderName, Path expectedFile) {
        Appender<ILoggingEvent> asyncAppender = context.getLogger("ROOT").getAppender(asyncAppenderName);
        Assertions.assertInstanceOf(AsyncAppender.class, asyncAppender);
        Appender<ILoggingEvent> fileAppender = ((AsyncAppender) asyncAppender).getAppender(fileAppenderName);
        Assertions.assertInstanceOf(RollingFileAppender.class, fileAppender);
        String actualFile = ((RollingFileAppender<?>) fileAppender).getFile();
        Assertions.assertNotNull(actualFile, "Missing file path on appender " + fileAppenderName);
        Assertions.assertEquals(
                expectedFile.toAbsolutePath().normalize(),
                Path.of(actualFile).toAbsolutePath().normalize(),
                "Unexpected log file path for " + fileAppenderName);
    }

    private static void copyClasspathResourceWithOptionalProbe(String resource, Path target, boolean addProbe)
            throws IOException {
        try (InputStream input =
                LogbackConditionalMigrationTest.class.getClassLoader().getResourceAsStream(resource)) {
            Assertions.assertNotNull(input, "Missing classpath resource: " + resource);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            String content = new String(output.toByteArray(), StandardCharsets.UTF_8);
            if (addProbe) {
                content = content.replace("${CONSOLE_LOG_PATTERN}", EXTERNAL_CONSOLE_PROBE);
            }
            Files.write(target, content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String toLogbackDirProperty(Path dir) {
        return dir.toAbsolutePath().toString().replace('\\', '/') + "/";
    }
}
