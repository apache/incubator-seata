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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.github.danielwegener.logback.kafka.KafkaAppender;
import net.logstash.logback.appender.LogstashTcpSocketAppender;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.logging.logback.appender.MetricLogbackAppender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestPropertySource;

import java.lang.reflect.Field;
import java.util.Iterator;

@TestPropertySource(
        properties = {
            "logging.extend.logstash-appender.enabled=true",
            "logging.extend.kafka-appender.enabled=true",
            "logging.extend.kafka-appender.topic=test",
            "logging.extend.metric-appender.enabled=true"
        })
public class AppenderTest extends BaseSpringBootTest {

    @Test
    public void testAppenderEnabled() {
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
