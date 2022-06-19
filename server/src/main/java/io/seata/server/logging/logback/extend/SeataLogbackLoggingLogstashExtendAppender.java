package io.seata.server.logging.logback.extend;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.net.ssl.KeyStoreFactoryBean;
import ch.qos.logback.core.net.ssl.SSLConfiguration;
import ch.qos.logback.core.util.Duration;
import io.seata.common.loader.LoadLevel;
import io.seata.common.util.StringUtils;
import net.logstash.logback.appender.LogstashTcpSocketAppender;
import net.logstash.logback.appender.destination.*;
import net.logstash.logback.composite.JsonProviders;
import net.logstash.logback.composite.loggingevent.LoggingEventJsonProviders;
import net.logstash.logback.composite.loggingevent.LoggingEventPatternJsonProvider;
import net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder;
import org.springframework.core.env.Environment;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

import static ch.qos.logback.core.net.AbstractSocketAppender.DEFAULT_PORT;

/**
 * The type of SeataLogbackLoggingLogstashExtendAppender to support config {@link LogstashTcpSocketAppender} with spring
 * {@link Environment}
 * you can config it in spring application.yml like this
 * <pre>
 * logging:
 *   extend:
 *     logstash-appender:
 *       destination: 127.0.0.1:4560
 * </pre>
 * We also support more configuration items defined in the {@link LogstashTcpSocketAppender}
 * <pre>
 * logging:
 *   extend:
 *     logstash-appender:
 *       destination: 127.0.0.1:4560
 *       pattern: '{"timestamp":"%date{yyyy-MM-dd HH:mm:ss.SSS}","level":"%p","app_name":"${spring.application.name:seata-server}","PORT":"${server.servicePort:0}","thread_name":"%t","logger_name":"%logger","X-TX-XID":"%X{X-TX-XID:-}","X-TX-BRANCH-ID":"%X{X-TX-BRANCH-ID:-}","message":"%m"}'
 *       keep-alive-duration: 5 minutes
 *       keep-alive-message: ping
 *       keep-alive-charset: UTF-8
 *       reconnection-delay: 30 seconds
 *       connection-strategy: preferPrimary
 *       connection-ttl: 10 seconds
 *       write-buffer-size: 8192
 *       write-timeout: 1 minute
 *       connection-timeout: 5 seconds
 *       ring-buffer-size: 8192
 *       wait-strategy: blocking
 * </pre>
 *
 * @author wlx
 * @date 2022/5/30 11:30 下午
 * @see LogstashTcpSocketAppender
 */
@LoadLevel(name = "SeataLogbackLoggingExtendKafkaAppender")
public class SeataLogbackLoggingLogstashExtendAppender extends AbstractSeataLogbackLoggingExtendAppender<LogstashTcpSocketAppender> {

    /**
     * prefix
     */
    private static final String LOGSTASH_EXTEND_CONFIG_PREFIX = LOGGING_EXTEND_CONFIG_PREFIX + ".logstash-appender";

    /**
     * destination config key
     */
    private static final String DESTINATIONS = LOGSTASH_EXTEND_CONFIG_PREFIX + ".destinations";

    /**
     * pattern config key
     */
    private static final String LOGSTASH_PATTERN = LOGSTASH_EXTEND_CONFIG_PREFIX + ".pattern";

    /**
     * default logging pattern
     */
    private static final String DEFAULT_PATTERN = "{\"timestamp\":\"%date{yyyy-MM-dd HH:mm:ss.SSS}\",\"level\":\"%p\",\"app_name\":\"${spring.application.name:seata-server}\",\"PORT\":\"${server.servicePort:0}\",\"thread_name\":\"%t\",\"logger_name\":\"%logger\",\"X-TX-XID\":\"%X{X-TX-XID:-}\",\"X-TX-BRANCH-ID\":\"%X{X-TX-BRANCH-ID:-}\",\"message\":\"%m\",\"stack_trace\":\"%wex\"}";

    /**
     * logstash appender name
     */
    private static final String LOGSTASH_APPENDER_NAME = "LOGSTASH";

    /**
     * enable
     */
    private static final String ENABLE = LOGSTASH_EXTEND_CONFIG_PREFIX + ".enable";

    @Override
    LogstashTcpSocketAppender loggingExtendAppender() {
        String destination = propertyResolver.getProperty(DESTINATIONS);
        if (StringUtils.isNullOrEmpty(destination)) {
            throw new IllegalArgumentException("destination can't be null,please config it by" + DESTINATIONS);
        }
        return new LogstashTcpSocketAppender();

    }

    @Override
    Encoder<ILoggingEvent> loggingExtendEncoder() {
        LoggingEventCompositeJsonEncoder encoder = new LoggingEventCompositeJsonEncoder();
        encoder.setProviders(jsonProviders());
        encoder.setContext(loggerContext);
        encoder.start();
        return encoder;
    }

    private JsonProviders<ILoggingEvent> jsonProviders() {
        LoggingEventJsonProviders jsonProviders = new LoggingEventJsonProviders();
        jsonProviders.setContext(loggerContext);
        jsonProviders.addPattern(loggingEventPatternJsonProvider());
        return jsonProviders;
    }

    private LoggingEventPatternJsonProvider loggingEventPatternJsonProvider() {
        LoggingEventPatternJsonProvider patternJsonProvider = new LoggingEventPatternJsonProvider();
        patternJsonProvider.setContext(loggerContext);
        String pattern = propertyResolver.getProperty(LOGSTASH_PATTERN, DEFAULT_PATTERN);
        patternJsonProvider.setPattern(propertyResolver.resolvePlaceholders(pattern));
        return patternJsonProvider;
    }

    @Override
    Class<?> loggingExtendAppenderType() {
        return LogstashTcpSocketAppender.class;
    }

    @Override
    boolean necessary() {
        return propertyResolver.getProperty(ENABLE, Boolean.class, false);
    }

    @Override
    void doConfigurationInner(LogstashTcpSocketAppender appender) {
        appender.stop();
        appender.setName(LOGSTASH_APPENDER_NAME);
        appender.setContext(loggerContext);
        Encoder<ILoggingEvent> encoder = loggingExtendEncoder();
        appender.setEncoder(encoder);
        doKeepAliveConfig(appender);
        doDestinationsConfig(appender);
        doConnectionTimeoutConfig(appender);
        doReconnectionConfig(appender);
        doWaitConfig(appender);
        doWriteConfig(appender);
        doSslConfig(appender);
    }

    private void doKeepAliveConfig(LogstashTcpSocketAppender logstashTcpSocketAppender) {
        String keepAliveDuration = propertyResolver.getProperty(LOGSTASH_EXTEND_CONFIG_PREFIX + ".keep-alive-duration");
        if (!StringUtils.isNullOrEmpty(keepAliveDuration)) {
            logstashTcpSocketAppender.setKeepAliveDuration(Duration.valueOf(keepAliveDuration));
        }
        String keepAliveMessage = propertyResolver.getProperty(LOGSTASH_EXTEND_CONFIG_PREFIX + ".keep-alive-message");
        if (!StringUtils.isNullOrEmpty(keepAliveMessage)) {
            logstashTcpSocketAppender.setKeepAliveMessage(keepAliveMessage);
        }
        String keepAliveCharset = propertyResolver.getProperty(LOGSTASH_EXTEND_CONFIG_PREFIX + ".keep-alive-charset");
        if (!StringUtils.isNullOrEmpty(keepAliveCharset)) {
            logstashTcpSocketAppender.setKeepAliveCharset(Charset.forName(keepAliveCharset));
        }
    }

    private void doDestinationsConfig(LogstashTcpSocketAppender logstashTcpSocketAppender) {
        String destinations = propertyResolver.getProperty(DESTINATIONS);
        List<InetSocketAddress> parsedDestinations = DestinationParser.parse(destinations, DEFAULT_PORT);
        parsedDestinations.forEach(
                destination -> {
                    if (!logstashTcpSocketAppender.getDestinations().contains(destination)) {
                        logstashTcpSocketAppender.addDestination(destinations);
                    }
                }
        );
        String connectionStrategy = propertyResolver.getProperty(LOGSTASH_EXTEND_CONFIG_PREFIX + ".connection-strategy");
        if (!StringUtils.isNullOrEmpty(connectionStrategy)) {
            String connectionTtl = propertyResolver.getProperty(LOGSTASH_EXTEND_CONFIG_PREFIX + ".connection-ttl");
            DestinationConnectionStrategy strategy;
            if (StringUtils.isNullOrEmpty(connectionTtl)) {
                strategy = DestinationConnectionStrategyCreator.createDestinationConnectionStrategy(connectionStrategy);

            } else {
                strategy = DestinationConnectionStrategyCreator.createDestinationConnectionStrategy(connectionStrategy,
                        connectionTtl);
            }
            logstashTcpSocketAppender.setConnectionStrategy(strategy);
        }

    }

    private void doReconnectionConfig(LogstashTcpSocketAppender logstashTcpSocketAppender) {
        String reconnectionDelay = propertyResolver.getProperty(LOGSTASH_EXTEND_CONFIG_PREFIX + ".reconnection-delay");
        if (!StringUtils.isNullOrEmpty(reconnectionDelay)) {
            logstashTcpSocketAppender.setReconnectionDelay(Duration.valueOf(reconnectionDelay));
        }
    }

    private void doConnectionTimeoutConfig(LogstashTcpSocketAppender logstashTcpSocketAppender) {
        String connectionTimeout = propertyResolver.getProperty(LOGSTASH_EXTEND_CONFIG_PREFIX + ".connection-timeout");
        if (!StringUtils.isNullOrEmpty(connectionTimeout)) {
            logstashTcpSocketAppender.setConnectionTimeout(Duration.valueOf(connectionTimeout));
        }
    }

    private void doWriteConfig(LogstashTcpSocketAppender logstashTcpSocketAppender) {
        Integer writeBufferSize = propertyResolver.getProperty(LOGSTASH_EXTEND_CONFIG_PREFIX + ".write-buffer-size",
                Integer.class);
        if (!Objects.isNull(writeBufferSize)) {
            logstashTcpSocketAppender.setWriteBufferSize(writeBufferSize);
        }

        String writeTimeout = propertyResolver.getProperty(LOGSTASH_EXTEND_CONFIG_PREFIX + ".write-timeout");
        if (!StringUtils.isNullOrEmpty(writeTimeout)) {
            logstashTcpSocketAppender.setWriteTimeout(Duration.valueOf(writeTimeout));
        }

    }

    private void doWaitConfig(LogstashTcpSocketAppender logstashTcpSocketAppender) {
        Integer ringBufferSize = propertyResolver.getProperty(LOGSTASH_EXTEND_CONFIG_PREFIX + ".ring-buffer-size",
                Integer.class);
        if (!Objects.isNull(ringBufferSize)) {
            logstashTcpSocketAppender.setRingBufferSize(ringBufferSize);
        }
        String waitStrategy = propertyResolver.getProperty(LOGSTASH_EXTEND_CONFIG_PREFIX + ".wait-strategy");
        if (!StringUtils.isNullOrEmpty(waitStrategy)) {
            logstashTcpSocketAppender.setWaitStrategyType(waitStrategy);
        }
    }

    private void doSslConfig(LogstashTcpSocketAppender logstashTcpSocketAppender) {
        String location = propertyResolver.getProperty(LOGSTASH_EXTEND_CONFIG_PREFIX + "ssl.trustStore.location");
        if (StringUtils.isNullOrEmpty(location)) {
            return;
        }
        String password = propertyResolver.getProperty(LOGSTASH_EXTEND_CONFIG_PREFIX + "ssl.trustStore.password");
        String type = propertyResolver.getProperty(LOGSTASH_EXTEND_CONFIG_PREFIX + "ssl.trustStore.type");
        String provider = propertyResolver.getProperty(LOGSTASH_EXTEND_CONFIG_PREFIX + "ssl.trustStore.provider");
        SSLConfiguration sslConfiguration = new SSLConfiguration();
        KeyStoreFactoryBean keyStoreFactoryBean = new KeyStoreFactoryBean();
        keyStoreFactoryBean.setLocation(location);
        keyStoreFactoryBean.setPassword(password);
        keyStoreFactoryBean.setType(type);
        keyStoreFactoryBean.setProvider(provider);
        sslConfiguration.setTrustStore(keyStoreFactoryBean);
        logstashTcpSocketAppender.setSsl(sslConfiguration);
    }


    static class DestinationConnectionStrategyFactory {

        private static final String PREFER_PRIMARY = "preferPrimary";
        private static final String ROUND_ROBIN = "roundRobin";
        private static final String RANDOM = "random";

        static DestinationConnectionStrategy getDestinationConnectionStrategy(String type) {
            if (PREFER_PRIMARY.equals(type)) {
                return new PreferPrimaryDestinationConnectionStrategy();
            } else if (ROUND_ROBIN.equals(type)) {
                return new RoundRobinDestinationConnectionStrategy();
            } else if (RANDOM.equals(type)) {
                return new RandomDestinationConnectionStrategy();
            } else {
                throw new IllegalArgumentException("Unknown destination connection strategy type: " + type);
            }
        }

    }

    static class DestinationConnectionStrategyCreator {

        /**
         * create DestinationConnectionStrategy by type
         *
         * @param type destinationConnectionStrategy type
         * @return DestinationConnectionStrategy
         */
        static DestinationConnectionStrategy createDestinationConnectionStrategy(String type) {
            return createDestinationConnectionStrategy(type, StringUtils.EMPTY);
        }

        /**
         * create DestinationConnectionStrategy by type and connectionTtl
         *
         * @param type          destinationConnectionStrategy type
         * @param connectionTtl connectionTtl
         * @return DestinationConnectionStrategy
         */
        static DestinationConnectionStrategy createDestinationConnectionStrategy(String type, String connectionTtl) {
            DestinationConnectionStrategy destinationConnectionStrategy = DestinationConnectionStrategyFactory.getDestinationConnectionStrategy(type);
            if (StringUtils.isNullOrEmpty(connectionTtl)) {
                return destinationConnectionStrategy;
            }
            if (destinationConnectionStrategy instanceof DestinationConnectionStrategyWithTtl) {
                ((DestinationConnectionStrategyWithTtl) destinationConnectionStrategy).setConnectionTTL(Duration.valueOf(connectionTtl));
            } else if (destinationConnectionStrategy instanceof PreferPrimaryDestinationConnectionStrategy) {
                ((PreferPrimaryDestinationConnectionStrategy) destinationConnectionStrategy).setSecondaryConnectionTTL(Duration.valueOf(connectionTtl));
            }
            return destinationConnectionStrategy;
        }
    }


}
