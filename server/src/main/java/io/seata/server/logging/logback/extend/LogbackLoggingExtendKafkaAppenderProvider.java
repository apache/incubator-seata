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
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.Encoder;
import com.github.danielwegener.logback.kafka.KafkaAppender;
import com.github.danielwegener.logback.kafka.delivery.AsynchronousDeliveryStrategy;
import com.github.danielwegener.logback.kafka.keying.ContextNameKeyingStrategy;
import com.github.danielwegener.logback.kafka.keying.HostNameKeyingStrategy;
import com.github.danielwegener.logback.kafka.keying.KeyingStrategy;
import com.github.danielwegener.logback.kafka.keying.LoggerNameKeyingStrategy;
import com.github.danielwegener.logback.kafka.keying.NoKeyKeyingStrategy;
import com.github.danielwegener.logback.kafka.keying.ThreadNameKeyingStrategy;
import io.seata.common.loader.LoadLevel;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.util.Map;

/**
 * The type of SeataLogbackLoggingExtendKafkaAppender to support config {@link KafkaAppender} with spring
 * {@link Environment}
 * you can config it in spring application.yml like this
 * <pre>
 * logging:
 *   extend:
 *     kafka-appender:
 *       bootstrap-servers: 127.0.0.1:9092
 *       topic: logback_to_logstash
 * </pre>
 * <p>
 * We also support more configuration items defined in the {@link KafkaAppender} and you can add kafka
 * producer config like this
 * <pre>
 * logging:
 *   extend:
 *     kafka-appender:
 *       bootstrap-servers: 127.0.0.1:9092
 *       topic: logback_to_logstash
 *       pattern: '{"@timestamp":"%d{yyyy-MM-dd HH:mm:ss.SSS}","level":"%p","app_name":"${spring.application.name:seata-server}","PORT":"${server.servicePort:0}","thread_name":"%t","logger_name":"%logger","X-TX-XID":"%X{X-TX-XID:-}","X-TX-BRANCH-ID":"%X{X-TX-BRANCH-ID:-}","message":"%m"}'
 *       keying-strategy: noKey
 *       producer-configs:
 *         acks: 0
 *         linger:
 *           ms: 1000
 *         max:
 *           block:
 *             ms: 0
 * </pre>
 *
 * @author wlx
 * @see KafkaAppender
 */
@LoadLevel(name = "LogbackLoggingExtendKafkaAppenderProvider")
public class LogbackLoggingExtendKafkaAppenderProvider extends AbstractLogbackLoggingExtendAppenderProvider<KafkaAppender<ILoggingEvent>> {

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
     * enable
     */
    private static final String ENABLE = KAFKA_EXTEND_CONFIG_PREFIX + ".enable";

    public LogbackLoggingExtendKafkaAppenderProvider(ConfigurableEnvironment environment) {
        super(environment);
    }


    @Override
    KafkaAppender<ILoggingEvent> createLoggingExtendAppender() {
        String kafkaBootstrapServer = propertyResolver.getProperty(KAFKA_BOOTSTRAP_SERVERS);
        if (StringUtils.isNullOrEmpty(kafkaBootstrapServer)) {
            throw new IllegalArgumentException("bootstrap-servers can't be null,please config it by" + KAFKA_BOOTSTRAP_SERVERS);
        }
        String kafkaTopic = propertyResolver.getProperty(KAFKA_TOPIC);
        if (StringUtils.isNullOrEmpty(kafkaTopic)) {
            throw new IllegalArgumentException("topic can't be null,please config it by" + KAFKA_TOPIC);
        }
        return new KafkaAppender<>();
    }

    @Override
    String getLoggingPattern() {
        return propertyResolver.getProperty(KAFKA_PATTERN, DEFAULT_PATTERN);
    }

    @Override
    Class<?> loggingExtendAppenderType() {
        return KafkaAppender.class;
    }

    @Override
    void doConfiguration() {
        String kafkaTopic = propertyResolver.getProperty(KAFKA_TOPIC);
        appender.setContext(loggerContext);
        appender.setName(KAFKA);
        appender.setTopic(kafkaTopic);
        doKeyingStrategyConfig(appender);
        doDeliveryStrategyConfig(appender);
        doProducerConfig(appender);
        Encoder<ILoggingEvent> encoder = loggingExtendJsonEncoder();
        appender.setEncoder(encoder);
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
        if (CollectionUtils.isEmpty(producerConfigs)) {
            // add default producer configs
            producerConfigs.put("acks", "0");
            producerConfigs.put("linger.ms", "1000");
            producerConfigs.put("max.block.ms", "0");
        }
        // add kafka producer configs
        producerConfigs.forEach(
                appender::addProducerConfigValue
        );
    }

    @Override
    public boolean shouldAppend() {
        return propertyResolver.getProperty(ENABLE, Boolean.class, false);
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
