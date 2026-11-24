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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.alipay.sofa.common.log.LoggerSpaceManager;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class SofaLoggingCompatibilityTest {
    @Test
    void initializesIsolatedLogbackSpaceAndWritesEvents() {
        String space = "seata-sofa-log-test";
        Logger logger = (Logger) LoggerSpaceManager.getLoggerBySpace("compatibility", space);
        LoggerContext context = logger.getLoggerContext();
        try {
            assertNotSame(
                    LoggerFactory.getILoggerFactory(),
                    context,
                    "SOFA must initialize its log space instead of falling back after a linkage error");
            ListAppender<ILoggingEvent> events = new ListAppender<>();
            events.setContext(context);
            events.start();
            logger.addAppender(events);
            logger.info("compatible logger");
            assertEquals(1, events.list.size());
            assertEquals("compatible logger", events.list.get(0).getFormattedMessage());
        } finally {
            LoggerSpaceManager.removeILoggerFactoryBySpaceName(space);
            if (context != LoggerFactory.getILoggerFactory()) {
                context.stop();
            }
        }
    }
}
