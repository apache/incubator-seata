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
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.status.Status;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.spring.boot.autoconfigure.SeataCoreEnvironmentPostProcessor;
import org.apache.seata.spring.boot.autoconfigure.SeataServerEnvironmentPostProcessor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;

/**
 * Verifies Logback 1.5.38 native {@code <condition>} / {@code <if>} migrations for both
 * {@code server/src/main/resources/logback-spring.xml} and
 * {@code namingserver/src/main/resources/logback-spring-jvm.xml} across default classpath
 * and external {@code LOG_BASH_DIR} modes.
 */
class LogbackConditionalMigrationTest {

    private static final String EXTERNAL_CONSOLE_PROBE = "EXTERNAL_MIGRATION_CONSOLE_PROBE";

    @TempDir
    Path tempDir;

    @Test
    void serverDefaultClasspathModeWithExtendedAppendersDisabledProducesZeroErrors() throws Exception {
        Path logDir = Files.createDirectories(tempDir.resolve("logs-default-disabled"));
        Map<String, Object> properties = new HashMap<>();
        properties.put("logging.file.path", logDir.toString());

        LoggerContext context = configureServerLogback(properties);
        try {
            assertNoErrorStatuses(context);
            Set<String> attached = getRootAppenderNames(context);
            Assertions.assertTrue(attached.contains("ASYNC_CONSOLE"));
            Assertions.assertTrue(attached.contains("ASYNC_FILE_ALL"));
            Assertions.assertTrue(attached.contains("ASYNC_FILE_WARN"));
            Assertions.assertTrue(attached.contains("ASYNC_FILE_ERROR"));
            Assertions.assertFalse(attached.contains("LOGSTASH"));
            Assertions.assertFalse(attached.contains("KAFKA"));
            Assertions.assertFalse(attached.contains("METRIC"));
        } finally {
            context.stop();
        }
    }

    @Test
    void serverDefaultClasspathModeWithExtendedAppendersEnabledProducesZeroErrors() throws Exception {
        Path logDir = Files.createDirectories(tempDir.resolve("logs-default-enabled"));
        try (ServerSocket sink = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("logging.file.path", logDir.toString());
            properties.put("spring.config.additional-location", "");
            properties.put("logging.extend.logstash-appender.enabled", "true");
            properties.put("logging.extend.logstash-appender.destination", "127.0.0.1:" + sink.getLocalPort());
            properties.put("logging.extend.kafka-appender.enabled", "true");
            properties.put("logging.extend.kafka-appender.topic", "test");
            properties.put("logging.extend.metric-appender.enabled", "true");

            LoggerContext context = configureServerLogback(properties);
            try {
                assertNoErrorStatuses(context);
                Set<String> attached = getRootAppenderNames(context);
                Assertions.assertTrue(attached.contains("ASYNC_CONSOLE"));
                Assertions.assertTrue(attached.contains("ASYNC_FILE_ALL"));
                Assertions.assertTrue(attached.contains("ASYNC_FILE_WARN"));
                Assertions.assertTrue(attached.contains("ASYNC_FILE_ERROR"));
                Assertions.assertTrue(attached.contains("LOGSTASH"));
                Assertions.assertTrue(attached.contains("KAFKA"));
                Assertions.assertTrue(attached.contains("METRIC"));
            } finally {
                context.stop();
            }
        }
    }

    @Test
    void serverExternalLogBashDirModeSupportsPartialOverrideAndFallbackWithZeroErrors() throws Exception {
        Path externalConfDir = Files.createDirectories(tempDir.resolve("external-conf"));
        Path externalLogbackDir = Files.createDirectories(externalConfDir.resolve("logback"));
        copyClasspathResourceWithOptionalProbe(
                "logback/console-appender.xml", externalLogbackDir.resolve("console-appender.xml"), true);
        copyClasspathResourceWithOptionalProbe(
                "logback/file-appender.xml", externalLogbackDir.resolve("file-appender.xml"), false);
        copyClasspathResourceWithOptionalProbe(
                "logback/metric-appender.xml", externalLogbackDir.resolve("metric-appender.xml"), false);

        Path logDirDisabled = Files.createDirectories(tempDir.resolve("logs-external-disabled"));
        Map<String, Object> disabledProps = new HashMap<>();
        disabledProps.put("logging.file.path", logDirDisabled.toString());
        disabledProps.put("spring.config.additional-location", toLogbackDirProperty(externalConfDir));

        LoggerContext disabledContext = configureServerLogback(disabledProps);
        try {
            assertNoErrorStatuses(disabledContext);
            Set<String> attached = getRootAppenderNames(disabledContext);
            Assertions.assertTrue(attached.contains("ASYNC_CONSOLE"));
            Assertions.assertTrue(attached.contains("ASYNC_FILE_ALL"));
            Assertions.assertFalse(attached.contains("LOGSTASH"));
            Assertions.assertFalse(attached.contains("KAFKA"));
            Assertions.assertFalse(attached.contains("METRIC"));
            assertExternalConsoleProbe(disabledContext);
        } finally {
            disabledContext.stop();
        }

        Path logDirEnabled = Files.createDirectories(tempDir.resolve("logs-external-enabled"));
        try (ServerSocket sink = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))) {
            Map<String, Object> enabledProps = new HashMap<>();
            enabledProps.put("logging.file.path", logDirEnabled.toString());
            enabledProps.put("spring.config.additional-location", toLogbackDirProperty(externalConfDir));
            enabledProps.put("logging.extend.logstash-appender.enabled", "true");
            enabledProps.put("logging.extend.logstash-appender.destination", "127.0.0.1:" + sink.getLocalPort());
            enabledProps.put("logging.extend.kafka-appender.enabled", "true");
            enabledProps.put("logging.extend.kafka-appender.topic", "test");
            enabledProps.put("logging.extend.metric-appender.enabled", "true");

            LoggerContext enabledContext = configureServerLogback(enabledProps);
            try {
                assertNoErrorStatuses(enabledContext);
                Set<String> attached = getRootAppenderNames(enabledContext);
                Assertions.assertTrue(attached.contains("ASYNC_CONSOLE"));
                Assertions.assertTrue(attached.contains("ASYNC_FILE_ALL"));
                Assertions.assertTrue(attached.contains("ASYNC_FILE_WARN"));
                Assertions.assertTrue(attached.contains("ASYNC_FILE_ERROR"));
                Assertions.assertTrue(attached.contains("LOGSTASH"));
                Assertions.assertTrue(attached.contains("KAFKA"));
                Assertions.assertTrue(attached.contains("METRIC"));
                assertExternalConsoleProbe(enabledContext);
            } finally {
                enabledContext.stop();
            }
        }
    }

    @Test
    void namingserverLogbackSpringJvmXmlParsesDefaultAndExternalModesWithZeroErrors() throws Exception {
        Path namingResourcesDir = resolveNamingserverResourcesDir();
        Path jvmXml = namingResourcesDir.resolve("logback-spring-jvm.xml");
        Assertions.assertTrue(Files.isRegularFile(jvmXml), "Missing namingserver logback-spring-jvm.xml at " + jvmXml);

        Path defaultLogDir = Files.createDirectories(tempDir.resolve("naming-logs-default"));
        Map<String, Object> defaultProps = new HashMap<>();
        defaultProps.put("logging.file.path", defaultLogDir.toString());
        LoggerContext defaultContext = configureLogbackFromPath(jvmXml, defaultProps);
        try {
            assertNoErrorStatuses(defaultContext);
            Set<String> attached = getRootAppenderNames(defaultContext);
            Assertions.assertTrue(attached.contains("ASYNC_CONSOLE"));
            Assertions.assertTrue(attached.contains("ASYNC_FILE_ALL"));
            Assertions.assertTrue(attached.contains("ASYNC_FILE_WARN"));
            Assertions.assertTrue(attached.contains("ASYNC_FILE_ERROR"));
        } finally {
            defaultContext.stop();
        }

        Path externalConfDir = Files.createDirectories(tempDir.resolve("naming-external-conf"));
        Path externalLogbackDir = Files.createDirectories(externalConfDir.resolve("logback"));
        Path namingConsoleXml = namingResourcesDir.resolve("logback/console-appender.xml");
        Path namingFileXml = namingResourcesDir.resolve("logback/file-appender.xml");
        String consoleContent = new String(Files.readAllBytes(namingConsoleXml), StandardCharsets.UTF_8)
                .replace("${CONSOLE_LOG_PATTERN}", EXTERNAL_CONSOLE_PROBE);
        Files.write(
                externalLogbackDir.resolve("console-appender.xml"), consoleContent.getBytes(StandardCharsets.UTF_8));
        Files.copy(namingFileXml, externalLogbackDir.resolve("file-appender.xml"));

        Path externalLogDir = Files.createDirectories(tempDir.resolve("naming-logs-external"));
        Map<String, Object> externalProps = new HashMap<>();
        externalProps.put("logging.file.path", externalLogDir.toString());
        externalProps.put("spring.config.additional-location", toLogbackDirProperty(externalConfDir));
        LoggerContext externalContext = configureLogbackFromPath(jvmXml, externalProps);
        try {
            assertNoErrorStatuses(externalContext);
            Set<String> attached = getRootAppenderNames(externalContext);
            Assertions.assertTrue(attached.contains("ASYNC_CONSOLE"));
            Assertions.assertTrue(attached.contains("ASYNC_FILE_ALL"));
            Assertions.assertTrue(attached.contains("ASYNC_FILE_WARN"));
            Assertions.assertTrue(attached.contains("ASYNC_FILE_ERROR"));
            assertExternalConsoleProbe(externalContext);
        } finally {
            externalContext.stop();
        }
    }

    private static LoggerContext configureServerLogback(Map<String, Object> properties) throws Exception {
        try (InputStream input =
                LogbackConditionalMigrationTest.class.getClassLoader().getResourceAsStream("logback-spring.xml")) {
            Assertions.assertNotNull(input, "Missing classpath resource: logback-spring.xml");
            return configureLogbackStream(input, properties);
        }
    }

    private static LoggerContext configureLogbackFromPath(Path xmlPath, Map<String, Object> properties)
            throws Exception {
        try (InputStream input = Files.newInputStream(xmlPath)) {
            return configureLogbackStream(input, properties);
        }
    }

    private static LoggerContext configureLogbackStream(InputStream input, Map<String, Object> properties)
            throws Exception {
        LoggerContext context = new LoggerContext();
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", properties));
        JoranConfigurator configurator = createSpringBootJoranConfigurator(environment);
        configurator.setContext(context);

        Object previousEnvironment = ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, environment);
        SeataCoreEnvironmentPostProcessor.init();
        SeataServerEnvironmentPostProcessor.init();
        try {
            configurator.doConfigure(input);
        } finally {
            restoreSpringEnvironment(previousEnvironment);
        }
        return context;
    }

    private static JoranConfigurator createSpringBootJoranConfigurator(StandardEnvironment environment)
            throws Exception {
        LoggingInitializationContext initializationContext = new LoggingInitializationContext(environment);
        Class<?> clazz = Class.forName("org.springframework.boot.logging.logback.SpringBootJoranConfigurator");
        Constructor<?> constructor = clazz.getDeclaredConstructor(LoggingInitializationContext.class);
        constructor.setAccessible(true);
        return (JoranConfigurator) constructor.newInstance(initializationContext);
    }

    @SuppressWarnings("unchecked")
    private static void restoreSpringEnvironment(Object previousEnvironment) {
        if (previousEnvironment != null) {
            ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, previousEnvironment);
        } else {
            Map<String, Object> objects =
                    (Map<String, Object>) ReflectionTestUtils.getField(ObjectHolder.class, "OBJECT_MAP");
            if (objects != null) {
                objects.remove(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
            }
        }
    }

    private static void assertNoErrorStatuses(LoggerContext context) {
        List<String> errorStatuses = context.getStatusManager().getCopyOfStatusList().stream()
                .filter(status -> status.getLevel() == Status.ERROR)
                .map(status ->
                        status.getMessage() + (status.getThrowable() != null ? " (" + status.getThrowable() + ")" : ""))
                .collect(Collectors.toList());
        Assertions.assertTrue(
                errorStatuses.isEmpty(),
                "Logback StatusManager must contain zero ERROR statuses, but found: " + errorStatuses);
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

    private static Path resolveNamingserverResourcesDir() {
        Path direct = Path.of("namingserver/src/main/resources");
        if (Files.isDirectory(direct)) {
            return direct;
        }
        Path fromModule = Path.of("../namingserver/src/main/resources");
        if (Files.isDirectory(fromModule)) {
            return fromModule;
        }
        throw new IllegalStateException("Cannot locate namingserver/src/main/resources from "
                + Path.of("").toAbsolutePath());
    }
}
