package io.seata.server.logging.logback.extend;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.hook.DelayingShutdownHook;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.Duration;
import io.seata.server.logging.extend.LoggingExtendPropertyResolver;
import io.seata.server.logging.extend.SeataLoggingExtendAppender;
import net.logstash.logback.composite.JsonProviders;
import net.logstash.logback.composite.loggingevent.LoggingEventJsonProviders;
import net.logstash.logback.composite.loggingevent.LoggingEventPatternJsonProvider;
import net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.springframework.boot.context.logging.LoggingApplicationListener.REGISTER_SHUTDOWN_HOOK_PROPERTY;

/**
 * AbstractClass for SeataLogbackLoggingExtendAppender
 *
 * @author wlx
 * @date 2022/5/30 11:28 下午
 * @see io.seata.server.logging.logback.extend.SeataLogbackLoggingLogstashExtendAppender
 * @see SeataLogbackLoggingKafkaExtendAppender
 */
public abstract class AbstractSeataLogbackLoggingExtendAppender<E extends Appender<ILoggingEvent>> implements SeataLoggingExtendAppender {

    static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AbstractSeataLogbackLoggingExtendAppender.class);

    static final String LOGGING_EXTEND_CONFIG_PREFIX = "logging.extend";

    protected LoggingExtendPropertyResolver propertyResolver;

    protected LoggerContext loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();

    private static final AtomicBoolean SHUTDOWN_HOOK_REGISTERED = new AtomicBoolean();

    /**
     * default logging pattern
     */
    protected static final String DEFAULT_PATTERN = "{\"timestamp\":\"%d{yyyy-MM-dd HH:mm:ss.SSS}\",\"level\":\"%p\",\"app_name\":\"${spring.application.name:seata-server}\",\"PORT\":\"${server.servicePort:0}\",\"thread_name\":\"%t\",\"logger_name\":\"%logger\",\"X-TX-XID\":\"%X{X-TX-XID:-}\",\"X-TX-BRANCH-ID\":\"%X{X-TX-BRANCH-ID:-}\",\"message\":\"%m\",\"stack_trace\":\"%wex\"}";


    @Override
    public void appendAppender(ConfigurableEnvironment environment) {
        if (Objects.isNull(propertyResolver)) {
            propertyResolver = new LoggingExtendPropertyResolver(environment);
        }


        if (enable()) {
            E appender = getOrCreateLoggingExtendAppender();
            if (appender.isStarted()) {
                LOGGER.warn("appender has been started, it may be configured in logback-spring.xml," +
                        "we will reset it by properties in spring.yml may not take effect.");
                this.reset(appender);
            } else {
                this.doConfiguration(appender);
                this.start(appender);
            }
            registerShutdownHookIfNecessary(environment);
            checkErrorStatus();
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
                    appender = loggingExtendAppender();
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
        Logger rootLogger = getRootLogger(loggerContext);
        rootLogger.detachAppender(appender);
        E loggingExtendAppender = loggingExtendAppender();
        doConfiguration(loggingExtendAppender);
        start(loggingExtendAppender);
        rootLogger.addAppender(loggingExtendAppender);
        LOGGER.info("reset {} appender success!", appender.getName());
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
     * build Appender with ConfigurableEnvironment
     *
     * @return Appender
     */
    abstract E loggingExtendAppender();

    /**
     * build JsonEncoder with ConfigurableEnvironment
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
     * determine whether it is necessary to appendAppender
     *
     * @return whether it is necessary to appendAppender
     */
    abstract boolean enable();

    /**
     * do Configuration for  appender
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

    private void registerShutdownHookIfNecessary(Environment environment) {
        boolean registerShutdownHook = environment.getProperty(REGISTER_SHUTDOWN_HOOK_PROPERTY, Boolean.class, false);
        // if registerShutdownHook is true ,the shutdownHook have been registered by LoggingApplicationListener
        if (!registerShutdownHook && SHUTDOWN_HOOK_REGISTERED.compareAndSet(false, true)) {
            // register delayingShutdownHook ,delaying 5s to wait other shutdownHook do logging
            DelayingShutdownHook delayingShutdownHook = new DelayingShutdownHook();
            delayingShutdownHook.setContext(loggerContext);
            delayingShutdownHook.setDelay(Duration.valueOf("5000"));
            Runtime.getRuntime().addShutdownHook(new Thread(delayingShutdownHook));
        }
    }

    private void checkErrorStatus() {
        List<Status> statuses = loggerContext.getStatusManager().getCopyOfStatusList();
        StringBuilder errors = new StringBuilder();
        for (Status status : statuses) {
            if (status.getLevel() == Status.ERROR) {
                errors.append((errors.length() > 0) ? String.format("%n") : "");
                errors.append(status.toString());
            }
        }
        if (errors.length() > 0) {
            throw new IllegalStateException(String.format("Logback configuration error detected: %n%s", errors));
        }
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
