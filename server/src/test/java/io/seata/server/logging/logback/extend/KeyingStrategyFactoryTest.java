/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.server.logging.logback.extend;

import ch.qos.logback.classic.LoggerContext;
import com.github.danielwegener.logback.kafka.keying.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author wlx
 */
public class KeyingStrategyFactoryTest {

    @Test
    public void keyingStrategyFactoryTest() {
        ILoggerFactory loggerFactory = StaticLoggerBinder.getSingleton().getLoggerFactory();
        LoggerContext loggerContext = (LoggerContext) loggerFactory;
        //contextName
        //hostName
        //loggerName
        //noKey
        //threadName
        KeyingStrategy<?> contextName = LogbackLoggingExtendKafkaAppenderProvider.KeyingStrategyFactory.getKeyingStrategy("contextName", loggerContext);
        KeyingStrategy<?> hostName = LogbackLoggingExtendKafkaAppenderProvider.KeyingStrategyFactory.getKeyingStrategy("hostName", loggerContext);
        KeyingStrategy<?> loggerName = LogbackLoggingExtendKafkaAppenderProvider.KeyingStrategyFactory.getKeyingStrategy("loggerName", loggerContext);
        KeyingStrategy<?> noKey = LogbackLoggingExtendKafkaAppenderProvider.KeyingStrategyFactory.getKeyingStrategy("noKey", loggerContext);
        KeyingStrategy<?> threadName = LogbackLoggingExtendKafkaAppenderProvider.KeyingStrategyFactory.getKeyingStrategy("threadName", loggerContext);

        assertThat(contextName instanceof ContextNameKeyingStrategy).isTrue();
        assertThat(hostName instanceof HostNameKeyingStrategy).isTrue();
        assertThat(loggerName instanceof LoggerNameKeyingStrategy).isTrue();
        assertThat(noKey instanceof NoKeyKeyingStrategy).isTrue();
        assertThat(threadName instanceof ThreadNameKeyingStrategy).isTrue();

    }

    @Test
    public void keyingStrategyFactoryThrowTest(){
        ILoggerFactory loggerFactory = StaticLoggerBinder.getSingleton().getLoggerFactory();
        LoggerContext loggerContext = (LoggerContext) loggerFactory;
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            LogbackLoggingExtendKafkaAppenderProvider.KeyingStrategyFactory.getKeyingStrategy("1234", loggerContext);
        });
    }
}
