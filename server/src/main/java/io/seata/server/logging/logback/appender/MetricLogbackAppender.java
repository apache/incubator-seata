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
package io.seata.server.logging.logback.appender;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.LogbackException;
import io.seata.core.event.EventBus;
import io.seata.core.event.ExceptionEvent;
import io.seata.server.event.EventBusManager;

/**
 * The type metric logback appender
 *
 */
public class MetricLogbackAppender extends AppenderBase<ILoggingEvent> {

    private EventBus eventBus = EventBusManager.get();

    @Override
    protected void append(ILoggingEvent event) {
        try {
            Level level = event.getLevel();

            if (level.isGreaterOrEqual(Level.ERROR)) {
                ThrowableProxy info = (ThrowableProxy)event.getThrowableProxy();

                if (info != null) {
                    Throwable throwable = info.getThrowable();
                    eventBus.post(new ExceptionEvent(throwable.getClass().getName()));
                }
            }
        } catch (Exception ex) {
            throw new LogbackException(event.getFormattedMessage(), ex);
        }
    }
}
