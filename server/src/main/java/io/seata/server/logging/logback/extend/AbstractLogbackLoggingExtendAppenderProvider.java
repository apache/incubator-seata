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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.encoder.Encoder;
import io.seata.server.logging.extend.LoggingExtendPropertyResolver;
import io.seata.server.logging.logback.LogbackLoggingExtendAppenderProvider;
import net.logstash.logback.composite.JsonProviders;
import net.logstash.logback.composite.loggingevent.LoggingEventJsonProviders;
import net.logstash.logback.composite.loggingevent.LoggingEventPatternJsonProvider;
import net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Iterator;
import java.util.Objects;

/**
 * AbstractClass for LoggingExtendAppenderProvider
 *
 * @author wlx
 * @see LogbackLoggingExtendLogstashAppenderProvider
 * @see LogbackLoggingExtendKafkaAppenderProvider
 */
public abstract class AbstractLogbackLoggingExtendAppenderProvider<E extends Appender<ILoggingEvent>>
        implements LogbackLoggingExtendAppenderProvider {

    static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AbstractLogbackLoggingExtendAppenderProvider.class);

    static final String LOGGING_EXTEND_CONFIG_PREFIX = "logging.extend";

    protected LoggingExtendPropertyResolver propertyResolver;

    protected LoggerContext loggerContext;

    /**
     * appender instance
     */
    protected E appender;

    public AbstractLogbackLoggingExtendAppenderProvider(ConfigurableEnvironment environment) {
        this.loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();
        this.propertyResolver = new LoggingExtendPropertyResolver(environment);
        this.appender = getOrCreateLoggingExtendAppender();
    }

    /**
     * default logging pattern
     */
    protected static final String DEFAULT_PATTERN = "{\"timestamp\":\"%d{yyyy-MM-dd HH:mm:ss.SSS}\",\"level\":\"%p\",\"app_name\":\"${spring.application.name:seata-server}\",\"PORT\":\"${server.servicePort:0}\",\"thread_name\":\"%t\",\"logger_name\":\"%logger\",\"X-TX-XID\":\"%X{X-TX-XID:-}\",\"X-TX-BRANCH-ID\":\"%X{X-TX-BRANCH-ID:-}\",\"message\":\"%m\",\"stack_trace\":\"%wex\"}";

    @Override
    public void appendTo() {
        if (appender.isStarted()) {
            LOGGER.warn("appender has been started, " +
                    "we will reset it whit properties configured in spring.yml.");
            this.reset();
        } else {
            this.doConfiguration();
            this.start();
        }
    }

    /**
     * Get an appender from loggerContext. If not create a new appender
     *
     * @return appender
     */
    E getOrCreateLoggingExtendAppender() {
        E appender = getLoggingExtendAppender();
        if (Objects.isNull(appender)) {
            return createLoggingExtendAppender();
        }
        return appender;
    }

    /**
     * Get an appender from loggerContext.
     *
     * @return appender
     */
    @SuppressWarnings("unchecked")
    E getLoggingExtendAppender() {
        Class<?> loggingExtendAppenderType = this.loggingExtendAppenderType();
        Logger rootLogger = getRootLogger();
        Iterator<Appender<ILoggingEvent>> appenderIterator = rootLogger.iteratorForAppenders();
        while (appenderIterator.hasNext()) {
            Appender<ILoggingEvent> appender = appenderIterator.next();
            if (appender.getClass().isAssignableFrom(loggingExtendAppenderType)) {
                return (E) appender;
            }
        }
        return null;
    }

    /**
     * append appender to rootLogger
     */
    void append() {
        Logger rootLogger = getRootLogger();
        rootLogger.addAppender(appender);
    }

    @Override
    public boolean isStarted() {
        return appender.isStarted();
    }

    @Override
    public void stop() {
        if (!Objects.isNull(appender) && appender.isStarted()) {
            appender.stop();
            LOGGER.info("{} appender stopped success", appender.getName());
        }
    }

    @Override
    public void start() {
        if (Objects.isNull(appender) || appender.isStarted()) {
            return;
        }
        appender.start();
        append();
        LOGGER.info("{} appender started success", appender.getName());
    }

    void reset() {
        // detach it and create a new instance add append to rootLogger
        synchronized (this) {
            Logger rootLogger = getRootLogger();
            rootLogger.detachAppender(appender);
            this.appender = createLoggingExtendAppender();
            this.doConfiguration();
            this.append();
            this.start();
            LOGGER.info("reset {} appender success!", appender.getName());
        }
    }

    /**
     * create appender with configurableEnvironment.
     *
     * @return Appender
     */
    abstract E createLoggingExtendAppender();

    /**
     * create JsonEncoder with ConfigurableEnvironment
     *
     * @return Encoder
     */
    Encoder<ILoggingEvent> loggingExtendJsonEncoder() {
        LoggingEventCompositeJsonEncoder encoder = new LoggingEventCompositeJsonEncoder();
        encoder.setProviders(jsonProviders());
        encoder.setContext(loggerContext);
        encoder.start();
        return encoder;
    }

    /**
     * get pattern config
     *
     * @return pattern
     */
    abstract String getLoggingPattern();

    /**
     * definition loggingExtendAppender type
     *
     * @return loggingExtendAppender Class Type
     */
    abstract Class<?> loggingExtendAppenderType();

    /**
     * do configuration for appender
     */
    abstract void doConfiguration();

    /**
     * get root Logger from LoggerContext
     *
     * @return root Logger
     */
    Logger getRootLogger() {
        return loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    }

    /**
     * just for test
     *
     * @param propertyResolver propertyResolver
     */
    public void setPropertyResolver(LoggingExtendPropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
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
        String pattern = getLoggingPattern();
        patternJsonProvider.setPattern(propertyResolver.resolvePlaceholders(pattern));
        return patternJsonProvider;
    }

}
