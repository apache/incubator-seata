package io.seata.server.logging.logback.extend;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.encoder.Encoder;
import com.github.danielwegener.logback.kafka.KafkaAppender;
import com.github.danielwegener.logback.kafka.delivery.AsynchronousDeliveryStrategy;
import com.github.danielwegener.logback.kafka.keying.*;
import io.seata.common.loader.LoadLevel;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;

import java.util.Map;

/**
 * @author wlx
 * @date 2022/5/30 11:30 下午
 */
@LoadLevel(name = "SeataLogbackLoggingExtendKafkaAppender")
public class SeataLogbackLoggingExtendKafkaAppender extends AbstractSeataLogbackLoggingExtendAppender {

    /**
     * prefix
     */
    private static final String KAFKA_EXTEND_CONFIG_PREFIX = LOGGING_EXTEND_CONFIG_PREFIX + ".kafka-appender";

    /**
     * topic config key
     */
    private static final String KAFKA_TOPIC = KAFKA_EXTEND_CONFIG_PREFIX + ".topic";

    /**
     * bootstrap-servers config key
     */
    private static final String KAFKA_BOOTSTRAP_SERVERS = KAFKA_EXTEND_CONFIG_PREFIX + ".bootstrap-servers";

    /**
     * pattern config key
     */
    private static final String KAFKA_PATTERN = KAFKA_EXTEND_CONFIG_PREFIX + ".pattern";

    /**
     * producerConfigs config prefix
     */
    private static final String KAFKA_CONFIG_PREFIX = KAFKA_EXTEND_CONFIG_PREFIX + ".producer-configs";

    /**
     * kafka appender name
     */
    private static final String KAFKA = "KAFKA";

    /**
     * default logging pattern
     */
    private static final String DEFAULT_PATTERN = "{\"@timestamp\":\"%d{yyyy-MM-dd HH:mm:ss.SSS}\",\"level\":\"%p\",\"app_name\":\"${spring.application.name:seata-server}\",\"PORT\":\"${server.servicePort:0}\",\"thread_name\":\"%t\",\"logger_name\":\"%logger\",\"X-TX-XID\":\"%X{X-TX-XID:-}\",\"X-TX-BRANCH-ID\":\"%X{X-TX-BRANCH-ID:-}\",\"message\":\"%m\",\"stack_trace\":\"%wex\"}";

    @Override
    Appender<ILoggingEvent> loggingExtendAppender() {
        String kafkaBootstrapServer = propertyResolver.getProperty(KAFKA_BOOTSTRAP_SERVERS);
        String kafkaTopic = propertyResolver.getProperty(KAFKA_TOPIC);
        if (StringUtils.isNullOrEmpty(kafkaBootstrapServer) || StringUtils.isNullOrEmpty(kafkaTopic)) {
            return null;
        }
        KafkaAppender<ILoggingEvent> kafkaAppender = new KafkaAppender<>();
        doKafkaAppenderConfig(kafkaAppender);
        Encoder<ILoggingEvent> encoder = loggingExtendEncoder();
        kafkaAppender.setEncoder(encoder);
        return kafkaAppender;
    }

    @Override
    Encoder<ILoggingEvent> loggingExtendEncoder() {
        String pattern = propertyResolver.getProperty(KAFKA_PATTERN, DEFAULT_PATTERN);
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setPattern(propertyResolver.resolvePlaceholders(pattern));
        encoder.setContext(loggerContext);
        encoder.start();
        return encoder;
    }

    @Override
    Class<?> loggingExtendAppenderType() {
        return KafkaAppender.class;
    }

    private void doKafkaAppenderConfig(KafkaAppender<ILoggingEvent> appender) {
        String kafkaTopic = propertyResolver.getProperty(KAFKA_TOPIC);
        appender.setContext(loggerContext);
        appender.setName(KAFKA);
        appender.setTopic(kafkaTopic);
        doKeyingStrategyConfig(appender);
        doDeliveryStrategyConfig(appender);
        doProducerConfig(appender);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void doKeyingStrategyConfig(KafkaAppender<ILoggingEvent> appender) {
        String keyingStrategy = propertyResolver.getProperty(KAFKA_EXTEND_CONFIG_PREFIX + ".keying-strategy", KeyingStrategyType.NO_KEY.type);
        KeyingStrategy strategy = KeyingStrategyFactory.getKeyingStrategy(keyingStrategy, loggerContext);
        appender.setKeyingStrategy(strategy);
    }

    private void doDeliveryStrategyConfig(KafkaAppender<ILoggingEvent> appender) {
        appender.setDeliveryStrategy(new AsynchronousDeliveryStrategy());
    }

    private void doProducerConfig(KafkaAppender<ILoggingEvent> appender) {
        String kafkaBootstrapServer = propertyResolver.getProperty(KAFKA_BOOTSTRAP_SERVERS);
        appender.addProducerConfigValue("bootstrap.servers", kafkaBootstrapServer);
        Map<String, Object> producerConfigs = propertyResolver.getPropertyMapByPrefix(KAFKA_CONFIG_PREFIX);
        if (CollectionUtils.isNotEmpty(producerConfigs)) {
            // add kafka configs
            producerConfigs.forEach(
                    appender::addProducerConfigValue
            );
        }
    }

    static class KeyingStrategyFactory {

        /**
         * getKeyingStrategy
         *
         * @param type          type
         * @param loggerContext loggerContext
         * @return KeyingStrategy
         */
        @SuppressWarnings("rawtypes")
        static KeyingStrategy getKeyingStrategy(String type, LoggerContext loggerContext) {
            KeyingStrategyType keyingStrategyType = KeyingStrategyType.getKeyingStrategyType(type);
            switch (keyingStrategyType) {
                case NO_KEY:
                    return new NoKeyKeyingStrategy();
                case HOST_NAME:
                    HostNameKeyingStrategy hostNameKeyingStrategy = new HostNameKeyingStrategy();
                    hostNameKeyingStrategy.setContext(loggerContext);
                    return hostNameKeyingStrategy;
                case LOGGER_NAME:
                    return new LoggerNameKeyingStrategy();
                case THREAD_NAME:
                    return new ThreadNameKeyingStrategy();
                case CONTEXT_NAME:
                    ContextNameKeyingStrategy contextNameKeyingStrategy = new ContextNameKeyingStrategy();
                    contextNameKeyingStrategy.setContext(loggerContext);
                    return contextNameKeyingStrategy;
                default:
                    throw new IllegalArgumentException("Unknown KeyingStrategy type: " + type);
            }
        }
    }

    enum KeyingStrategyType {

        /**
         * noKey KeyingStrategyType
         */
        NO_KEY("noKey"),

        /**
         * threadName
         */
        THREAD_NAME("threadName"),

        /**
         * loggerName
         */
        LOGGER_NAME("loggerName"),

        /**
         * hostName
         */
        HOST_NAME("hostName"),

        /**
         * contextName
         */
        CONTEXT_NAME("contextName");

        private String type;

        KeyingStrategyType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public static KeyingStrategyType getKeyingStrategyType(String type) {
            KeyingStrategyType[] strategyTypes = KeyingStrategyType.values();
            for (KeyingStrategyType keyingStrategyType : strategyTypes) {
                if (keyingStrategyType.type.equals(type)) {
                    return keyingStrategyType;
                }
            }
            throw new IllegalArgumentException("Unknown KeyingStrategy type:" + type);
        }
    }
}
