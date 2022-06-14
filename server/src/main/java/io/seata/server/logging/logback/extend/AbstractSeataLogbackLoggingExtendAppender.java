package io.seata.server.logging.logback.extend;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.spi.LifeCycle;
import io.seata.server.logging.extend.LoggingExtendPropertyResolver;
import io.seata.server.logging.extend.SeataLoggingExtendAppender;
import org.slf4j.ILoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Iterator;
import java.util.Objects;

/**
 * @author wlx
 * @date 2022/5/30 11:28 下午
 */
public abstract class AbstractSeataLogbackLoggingExtendAppender implements SeataLoggingExtendAppender {

    static final String LOGGING_EXTEND_CONFIG_PREFIX = "logging.extend";

    LoggingExtendPropertyResolver propertyResolver;

    LoggerContext loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();

    Appender<ILoggingEvent> appender;

    @Override
    public void appendAppender(ConfigurableEnvironment environment) {
        if (Objects.isNull(propertyResolver)) {
            propertyResolver = new LoggingExtendPropertyResolver(environment);
        }
        Appender<ILoggingEvent> appender = createLoggingExtendAppender();
        if (!Objects.isNull(appender)) {
            this.appender = appender;
            this.start();
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


    @Override
    public void start() {
        if (!Objects.isNull(appender)) {
            appender.start();
            Logger rootLogger = getRootLogger(loggerContext);
            rootLogger.addAppender(appender);
        }
    }

    @Override
    public void stop() {
        if (!Objects.isNull(appender)) {
            appender.stop();
        }
    }

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
}
