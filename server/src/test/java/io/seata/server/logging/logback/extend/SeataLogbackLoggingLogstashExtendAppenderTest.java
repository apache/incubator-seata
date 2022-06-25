package io.seata.server.logging.logback.extend;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.encoder.Encoder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.server.SpringBootIntegrationTest;
import io.seata.server.logging.extend.LoggingExtendPropertyResolver;
import io.seata.server.logging.extend.LoggingExtendAppenderProvider;
import io.seata.server.logging.logback.LogbackLoggingExtendAppenderProvider;
import net.logstash.logback.appender.LogstashTcpSocketAppender;
import net.logstash.logback.appender.destination.PreferPrimaryDestinationConnectionStrategy;
import net.logstash.logback.encoder.com.lmax.disruptor.BlockingWaitStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.impl.StaticLoggerBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author wlx
 * @date 2022/6/12 9:49 下午
 */
public class SeataLogbackLoggingLogstashExtendAppenderTest extends SpringBootIntegrationTest {

    @Autowired
    private ConfigurableEnvironment environment;

    private LogbackLoggingLogstashExtendAppenderProvider provider;

    private LoggerContext loggerContext;

    @BeforeEach
    void before() {
        List<LogbackLoggingExtendAppenderProvider> loggingExtendAppenderProviderList =
                EnhancedServiceLoader.loadAll(LogbackLoggingExtendAppenderProvider.class);
        provider = ((LogbackLoggingLogstashExtendAppenderProvider) loggingExtendAppenderProviderList.stream()
                .filter(item -> item instanceof LogbackLoggingLogstashExtendAppenderProvider)
                .findFirst().get());
        ILoggerFactory loggerFactory = StaticLoggerBinder.getSingleton().getLoggerFactory();
        loggerContext = (LoggerContext) loggerFactory;
        provider.setPropertyResolver(new LoggingExtendPropertyResolver(environment));
        System.setProperty("logging.extend.logstash-appender.enable", "true");
    }

    @Test
    public void defaultPatternTest() throws IOException {
        final ArgumentCaptor<LoggingEvent> captorLoggingEvent = ArgumentCaptor.forClass(
                LoggingEvent.class);
        final Appender<ILoggingEvent> mockAppender = spy(provider.getOrCreateLoggingExtendAppender());

        Logger rootLogger = this.provider.getRootLogger(loggerContext);
        mockAppender.start();
        rootLogger.addAppender(mockAppender);

        MDC.put("X-TX-XID", "xid");
        MDC.put("X-TX-BRANCH-ID", "branch-id");

        final org.slf4j.Logger logger = LoggerFactory.getLogger(SeataLogbackLoggingLogstashExtendAppenderTest.class);
        logger.info("❤❥웃유♋☮✌☏☢☠✔");
        verify(mockAppender).doAppend(captorLoggingEvent.capture());
        final LoggingEvent loggingEvent = captorLoggingEvent.getValue();
        final Encoder<ILoggingEvent> encoder = this.provider.loggingExtendJsonEncoder();
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        encoder.start();

        outputStream.write(encoder.encode(loggingEvent));
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode node = mapper.readTree(outputStream.toByteArray());

        assertThat(node.get("message").asText().equals("❤❥웃유♋☮✌☏☢☠✔")).isTrue();
        assertThat(node.get("level").asText().equals("INFO")).isTrue();
        assertThat(node.get("app_name").asText().equals(environment.getProperty("spring.application.name"))).isTrue();
        assertThat(node.get("PORT").asText().equals(System.getProperty("server.servicePort"))).isTrue();
        assertThat(node.get("logger_name").asText().equals(this.getClass().getName())).isTrue();
        assertThat(node.get("X-TX-XID").asText().equals("xid")).isTrue();
        assertThat(node.get("X-TX-BRANCH-ID").asText().equals("branch-id")).isTrue();
        assertThat(node.has("thread_name")).isTrue();
        assertThat(node.has("timestamp")).isTrue();
        assertThat(node.has("stack_trace")).isTrue();

        MDC.clear();
        encoder.stop();
        mockAppender.stop();
        outputStream.close();
    }

    @Test
    public void patternTest() throws IOException {
        String pattern = "{\"level\":\"%p\",\"thread_name\":\"%t\",\"logger_name\":\"%logger\",\"message\":\"%m\"}";
        System.setProperty("logging.extend.logstash-appender.pattern", pattern);
        final ArgumentCaptor<LoggingEvent> captorLoggingEvent = ArgumentCaptor.forClass(
                LoggingEvent.class);
        final Appender<ILoggingEvent> mockAppender = spy(provider.getOrCreateLoggingExtendAppender());

        Logger rootLogger = provider.getRootLogger(loggerContext);
        mockAppender.start();
        rootLogger.addAppender(mockAppender);

        final org.slf4j.Logger logger = LoggerFactory.getLogger(SeataLogbackLoggingLogstashExtendAppenderTest.class);
        logger.info("patternTest");
        verify(mockAppender).doAppend(captorLoggingEvent.capture());
        final LoggingEvent loggingEvent = captorLoggingEvent.getValue();
        final Encoder<ILoggingEvent> encoder = provider.loggingExtendJsonEncoder();
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        encoder.start();

        outputStream.write(encoder.encode(loggingEvent));
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode node = mapper.readTree(outputStream.toByteArray());

        assertThat(node.get("message").asText().equals("patternTest")).isTrue();
        assertThat(node.get("level").asText().equals("INFO")).isTrue();
        assertThat(node.get("logger_name").asText().equals(this.getClass().getName())).isTrue();
        assertThat(node.has("thread_name")).isTrue();

        MDC.clear();
        encoder.stop();
        mockAppender.stop();
        outputStream.close();
        // clear property
        System.clearProperty("logging.extend.logstash-appender.pattern");
    }

    @Test
    public void configTest() {
        System.setProperty("logging.extend.logstash-appender.keep-alive-duration", "1000");
        System.setProperty("logging.extend.logstash-appender.keep-alive-message", "ping");
        System.setProperty("logging.extend.logstash-appender.keep-alive-charset", "UTF-8");
        System.setProperty("logging.extend.logstash-appender.reconnection-delay", "1000");
        System.setProperty("logging.extend.logstash-appender.connection-strategy", "preferPrimary");
        System.setProperty("logging.extend.logstash-appender.connection-ttl", "1000");
        System.setProperty("logging.extend.logstash-appender.write-buffer-size", "8192");
        System.setProperty("logging.extend.logstash-appender.write-timeout:", "1000");
        System.setProperty("logging.extend.logstash-appender.connection-timeout", "1000");
        System.setProperty("logging.extend.logstash-appender.ring-buffer-size", "8192");
        System.setProperty("logging.extend.logstash-appender.wait-strategy", "blocking");

        LogstashTcpSocketAppender loggingExtendAppender = provider.createLoggingExtendAppender();
        provider.doConfiguration(loggingExtendAppender);
        assertThat(loggingExtendAppender.getKeepAliveCharset().name().equals("UTF-8")).isTrue();
        assertThat(loggingExtendAppender.getKeepAliveMessage().equals("ping")).isTrue();
        assertThat(loggingExtendAppender.getKeepAliveDuration().getMilliseconds() == 1000).isTrue();
        assertThat(loggingExtendAppender.getReconnectionDelay().getMilliseconds() == 1000).isTrue();
        assertThat(loggingExtendAppender.getConnectionStrategy().getClass().getName().equals(PreferPrimaryDestinationConnectionStrategy.class.getName())).isTrue();
        assertThat(loggingExtendAppender.getWriteBufferSize() == 8192).isTrue();
        assertThat(loggingExtendAppender.getWriteTimeout().getMilliseconds() == 1000).isTrue();
        assertThat(loggingExtendAppender.getConnectionTimeout().getMilliseconds() == 1000).isTrue();
        assertThat(loggingExtendAppender.getRingBufferSize() == 8192).isTrue();
        assertThat(loggingExtendAppender.getWaitStrategy().getClass().getName().equals(BlockingWaitStrategy.class.getName())).isTrue();

        System.clearProperty("logging.extend.logstash-appender.keep-alive-duration");
        System.clearProperty("logging.extend.logstash-appender.keep-alive-message");
        System.clearProperty("logging.extend.logstash-appender.keep-alive-charset");
        System.clearProperty("logging.extend.logstash-appender.reconnection-delay");
        System.clearProperty("logging.extend.logstash-appender.connection-strategy");
        System.clearProperty("logging.extend.logstash-appender.connection-ttl");
        System.clearProperty("logging.extend.logstash-appender.write-buffer-size");
        System.clearProperty("logging.extend.logstash-appender.write-timeout:");
        System.clearProperty("logging.extend.logstash-appender.connection-timeout");
        System.clearProperty("logging.extend.logstash-appender.ring-buffer-size");
        System.clearProperty("logging.extend.logstash-appender.wait-strategy");
    }
}
