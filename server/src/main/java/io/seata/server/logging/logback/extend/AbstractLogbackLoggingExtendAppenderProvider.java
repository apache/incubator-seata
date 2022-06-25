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
 * @date 2022/5/30 11:28 下午
 * @see LogbackLoggingLogstashExtendAppenderProvider
 * @see LogbackLoggingKafkaExtendAppenderProvider
 */
public abstract class AbstractLogbackLoggingExtendAppenderProvider<E extends Appender<ILoggingEvent>>
        implements LogbackLoggingExtendAppenderProvider {

    static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AbstractLogbackLoggingExtendAppenderProvider.class);

    static final String LOGGING_EXTEND_CONFIG_PREFIX = "logging.extend";

    protected LoggingExtendPropertyResolver propertyResolver;

    protected LoggerContext loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();

    public AbstractLogbackLoggingExtendAppenderProvider(ConfigurableEnvironment environment) {
        this.propertyResolver = new LoggingExtendPropertyResolver(environment);
    }

    /**
     * default logging pattern
     */
    protected static final String DEFAULT_PATTERN = "{\"timestamp\":\"%d{yyyy-MM-dd HH:mm:ss.SSS}\",\"level\":\"%p\",\"app_name\":\"${spring.application.name:seata-server}\",\"PORT\":\"${server.servicePort:0}\",\"thread_name\":\"%t\",\"logger_name\":\"%logger\",\"X-TX-XID\":\"%X{X-TX-XID:-}\",\"X-TX-BRANCH-ID\":\"%X{X-TX-BRANCH-ID:-}\",\"message\":\"%m\",\"stack_trace\":\"%wex\"}";

    @Override
    public void appendTo() {
        E appender = getOrCreateLoggingExtendAppender();
        if (appender.isStarted()) {
            LOGGER.warn("appender has been started, " +
                    "we will reset it whit properties configured in spring.yml.");
            this.reset(appender);
        } else {
            this.doConfiguration(appender);
            this.start(appender);
        }
    }

    /**
     * Get an appender from loggerContext. If not create a new appender
     *
     * @return appender
     */
    E getOrCreateLoggingExtendAppender() {
        E appender = getLoggingExtendAppender();
        // double check to make sure only one appender append to rootLogger
        if (Objects.isNull(appender)) {
            synchronized (this) {
                appender = getLoggingExtendAppender();
                if (Objects.isNull(appender)) {
                    appender = createLoggingExtendAppender();
                    append(appender);
                }
            }
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
        Logger rootLogger = getRootLogger(loggerContext);
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
     *
     * @param appender appender
     */
    void append(E appender) {
        Logger rootLogger = getRootLogger(loggerContext);
        rootLogger.addAppender(appender);
    }

    /**
     * start appender
     *
     * @param appender appender
     */
    void start(E appender) {
        if (appender.isStarted()) {
            return;
        }
        appender.start();
        LOGGER.info("{} appender started success", appender.getName());
    }

    void reset(E appender) {
        // detach it and create a new instance add append to rootLogger
        synchronized (this) {
            Logger rootLogger = getRootLogger(loggerContext);
            rootLogger.detachAppender(appender);
            E loggingExtendAppender = createLoggingExtendAppender();
            this.doConfiguration(loggingExtendAppender);
            this.append(loggingExtendAppender);
            this.start(loggingExtendAppender);
            LOGGER.info("reset {} appender success!", appender.getName());
        }
    }

    /**
     * stop appender
     *
     * @param appender appender
     */
    void stop(E appender) {
        if (appender.isStarted()) {
            appender.stop();
            LOGGER.info("{} appender stopped success", appender.getName());
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
     *
     * @param appender appender
     */
    abstract void doConfiguration(E appender);

    /**
     * get root Logger from LoggerContext
     *
     * @param loggerContext loggerContext
     * @return root Logger
     */
    Logger getRootLogger(LoggerContext loggerContext) {
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
