package io.seata.server.logging.logback.extend;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.hook.DelayingShutdownHook;
import ch.qos.logback.core.util.Duration;
import io.seata.server.logging.extend.LoggingExtendPropertyResolver;
import io.seata.server.logging.extend.SeataLoggingExtendAppender;
import org.slf4j.impl.StaticLoggerBinder;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.springframework.boot.context.logging.LoggingApplicationListener.REGISTER_SHUTDOWN_HOOK_PROPERTY;

/**
 * AbstractClass for SeataLogbackLoggingExtendAppender
 *
 * @author wlx
 * @date 2022/5/30 11:28 下午
 * @see io.seata.server.logging.logback.extend.SeataLogbackLoggingLogstashExtendAppender
 * @see io.seata.server.logging.logback.extend.SeataLogbackLoggingExtendKafkaAppender
 */
public abstract class AbstractSeataLogbackLoggingExtendAppender implements SeataLoggingExtendAppender {

    static final String LOGGING_EXTEND_CONFIG_PREFIX = "logging.extend";

    protected LoggingExtendPropertyResolver propertyResolver;

    protected LoggerContext loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();

    private static final AtomicBoolean SHUTDOWN_HOOK_REGISTERED = new AtomicBoolean();

    @Override
    public void appendAppender(ConfigurableEnvironment environment) {
        if (Objects.isNull(propertyResolver)) {
            propertyResolver = new LoggingExtendPropertyResolver(environment);
        }
        Appender<ILoggingEvent> appender = createLoggingExtendAppender();
        if (!Objects.isNull(appender)) {
            appender.start();
            Logger rootLogger = getRootLogger(loggerContext);
            rootLogger.addAppender(appender);
            registerShutdownHookIfNecessary(environment);
        }

    }

    Appender<ILoggingEvent> createLoggingExtendAppender() {
        Appender<ILoggingEvent> appender = getLoggingExtendAppender();
        // double check to make sure only one appender append to rootLogger
        if (Objects.isNull(appender)) {
            synchronized (this) {
                appender = getLoggingExtendAppender();
                if (Objects.isNull(appender)) {
                    appender = loggingExtendAppender();
                }
            }
        }
        return appender;
    }

    /**
     * get appender from loggerContext if exit.
     *
     * @return appender if exit else return null
     */
    Appender<ILoggingEvent> getLoggingExtendAppender() {
        Class<?> loggingExtendAppenderType = this.loggingExtendAppenderType();
        Logger rootLogger = getRootLogger(loggerContext);
        Iterator<Appender<ILoggingEvent>> appenderIterator = rootLogger.iteratorForAppenders();
        while (appenderIterator.hasNext()) {
            Appender<ILoggingEvent> appender = appenderIterator.next();
            if (appender.getClass().isAssignableFrom(loggingExtendAppenderType)) {
                return appender;
            }
        }
        return null;
    }

    /**
     * build Appender with ConfigurableEnvironment
     *
     * @return Appender
     */
    abstract Appender<ILoggingEvent> loggingExtendAppender();

    /**
     * build Encoder with ConfigurableEnvironment
     *
     * @return Encoder
     */
    abstract Encoder<ILoggingEvent> loggingExtendEncoder();

    /**
     * definition loggingExtendAppender type
     *
     * @return loggingExtendAppender Class Type
     */
    abstract Class<?> loggingExtendAppenderType();

    /**
     * get root Logger from LoggerContext
     *
     * @param loggerContext loggerContext
     * @return root Logger
     */
    Logger getRootLogger(LoggerContext loggerContext) {
        return loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    }

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

}
