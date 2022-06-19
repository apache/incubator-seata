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
 * @date 2022/6/14 11:43 下午
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
        KeyingStrategy<?> contextName = SeataLogbackLoggingKafkaExtendAppender.KeyingStrategyFactory.getKeyingStrategy("contextName", loggerContext);
        KeyingStrategy<?> hostName = SeataLogbackLoggingKafkaExtendAppender.KeyingStrategyFactory.getKeyingStrategy("hostName", loggerContext);
        KeyingStrategy<?> loggerName = SeataLogbackLoggingKafkaExtendAppender.KeyingStrategyFactory.getKeyingStrategy("loggerName", loggerContext);
        KeyingStrategy<?> noKey = SeataLogbackLoggingKafkaExtendAppender.KeyingStrategyFactory.getKeyingStrategy("noKey", loggerContext);
        KeyingStrategy<?> threadName = SeataLogbackLoggingKafkaExtendAppender.KeyingStrategyFactory.getKeyingStrategy("threadName", loggerContext);

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
            SeataLogbackLoggingKafkaExtendAppender.KeyingStrategyFactory.getKeyingStrategy("1234", loggerContext);
        });
    }
}
