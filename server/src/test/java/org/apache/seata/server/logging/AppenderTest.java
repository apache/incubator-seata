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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.github.danielwegener.logback.kafka.KafkaAppender;
import net.logstash.logback.appender.LogstashTcpSocketAppender;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.logging.logback.appender.MetricLogbackAppender;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestPropertySource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@TestPropertySource(
        properties = {
            "logging.extend.logstash-appender.enabled=true",
            "logging.extend.logstash-appender.destination=${seata.test.logstash.destination}",
            "logging.extend.kafka-appender.enabled=true",
            "logging.extend.kafka-appender.topic=test",
            "logging.extend.metric-appender.enabled=true"
        })
public class AppenderTest extends BaseSpringBootTest {
    private static final String DESTINATION_PROPERTY = "seata.test.logstash.destination";
    private static final String ORIGINAL_DESTINATION = System.getProperty(DESTINATION_PROPERTY);
    private static final BlockingQueue<String> RECEIVED = new LinkedBlockingQueue<>();
    private static final AtomicReference<Throwable> SINK_FAILURE = new AtomicReference<>();
    private static final Set<Socket> CONNECTIONS = ConcurrentHashMap.newKeySet();
    private static final ExecutorService SINK_THREADS = Executors.newCachedThreadPool(task -> {
        Thread thread = new Thread(task, "logstash-test-sink");
        thread.setDaemon(true);
        return thread;
    });
    private static final ServerSocket SINK = startSink();

    private static ServerSocket startSink() {
        try {
            ServerSocket server = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
            System.setProperty(DESTINATION_PROPERTY, "127.0.0.1:" + server.getLocalPort());
            SINK_THREADS.submit(() -> {
                try {
                    while (!server.isClosed()) {
                        Socket connection = server.accept();
                        CONNECTIONS.add(connection);
                        SINK_THREADS.submit(() -> {
                            try (Socket socket = connection;
                                    BufferedReader reader = new BufferedReader(
                                            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    RECEIVED.add(line);
                                }
                            } catch (IOException error) {
                                if (!server.isClosed()) {
                                    SINK_FAILURE.compareAndSet(null, error);
                                }
                            } finally {
                                CONNECTIONS.remove(connection);
                            }
                        });
                    }
                } catch (IOException error) {
                    if (!server.isClosed()) {
                        SINK_FAILURE.compareAndSet(null, error);
                    }
                }
            });
            return server;
        } catch (IOException error) {
            throw new IllegalStateException("Cannot start Logstash test sink", error);
        }
    }

    @AfterAll
    static void closeSink() throws Exception {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Appender<ILoggingEvent> appender = context.getLogger("ROOT").getAppender("LOGSTASH");
        if (appender != null) {
            appender.stop();
        }
        SINK.close();
        for (Socket connection : CONNECTIONS) {
            connection.close();
        }
        SINK_THREADS.shutdownNow();
        Assertions.assertTrue(SINK_THREADS.awaitTermination(5, TimeUnit.SECONDS));
        if (ORIGINAL_DESTINATION == null) {
            System.clearProperty(DESTINATION_PROPERTY);
        } else {
            System.setProperty(DESTINATION_PROPERTY, ORIGINAL_DESTINATION);
        }
    }

    @Test
    public void testAppenderEnabled() throws InterruptedException {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        Iterator<Appender<ILoggingEvent>> appenderIterator =
                lc.getLogger("ROOT").iteratorForAppenders();

        boolean kafkaFound = false;
        boolean metricFound = false;
        boolean logstashFound = false;

        while (appenderIterator.hasNext()) {
            Appender<ILoggingEvent> appender = appenderIterator.next();
            if (appender.getName().equals("KAFKA")) {
                KafkaAppender<ILoggingEvent> kafkaAppender = (KafkaAppender<ILoggingEvent>) appender;
                kafkaFound = true;

                try {
                    // use reflection to obtain the "protect topic" fields of the abstract class inherited by the
                    // appender instance
                    Class<?> kafkaAppenderClass = kafkaAppender.getClass();
                    Field topicField = getDeclaredFieldRecursive(kafkaAppenderClass, "topic");
                    topicField.setAccessible(true);
                    String topic = (String) topicField.get(kafkaAppender);

                    Assertions.assertEquals("test", topic);
                    Assertions.assertInstanceOf(KafkaAppender.class, appender);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            if (appender.getName().equals("METRIC")) {
                Assertions.assertInstanceOf(MetricLogbackAppender.class, appender);
                metricFound = true;
            }

            if (appender.getName().equals("LOGSTASH")) {
                Assertions.assertInstanceOf(LogstashTcpSocketAppender.class, appender);
                logstashFound = true;
            }
        }

        Assertions.assertTrue(kafkaFound);
        Assertions.assertTrue(metricFound);
        Assertions.assertTrue(logstashFound);
        String marker = "logstash-fixture-marker";
        lc.getLogger("ROOT")
                .getAppender("LOGSTASH")
                .doAppend(new LoggingEvent(
                        getClass().getName(), lc.getLogger(getClass()), Level.INFO, marker, null, null));
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        boolean delivered = false;
        while (!delivered && System.nanoTime() < deadline) {
            String line = RECEIVED.poll(100, TimeUnit.MILLISECONDS);
            delivered = line != null && line.contains(marker);
        }
        Assertions.assertNull(SINK_FAILURE.get());
        Assertions.assertTrue(delivered, "The configured Logstash appender must deliver the event to the local sink");
    }

    private static Field getDeclaredFieldRecursive(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Class<?> superClass = clazz.getSuperclass();
            if (superClass == null) {
                throw e;
            } else {
                return getDeclaredFieldRecursive(superClass, fieldName);
            }
        }
    }
}
