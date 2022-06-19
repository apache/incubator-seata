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

    static final String LOGGING_EXTEND_CONFIG_PREFIX = "logging.extend";

    protected LoggingExtendPropertyResolver propertyResolver;

    protected LoggerContext loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();

    private static final AtomicBoolean SHUTDOWN_HOOK_REGISTERED = new AtomicBoolean();

    @Override
    public void appendAppender(ConfigurableEnvironment environment) {
        if (Objects.isNull(propertyResolver)) {
            propertyResolver = new LoggingExtendPropertyResolver(environment);
        }
        if (necessary()) {
            E appender = getOrCreateLoggingExtendAppender();
            doConfiguration(appender);
            this.start(appender);
            registerShutdownHookIfNecessary(environment);
            checkErrorStatus();
        }
    }

    /**
     * Get an appender from loggerContext. If not, create a new appender
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

    void start(E appender) {
        if (appender.isStarted()) {
            return;
        }
        appender.start();
        Logger rootLogger = getRootLogger(loggerContext);
        rootLogger.addAppender(appender);
    }

    void stop(E appender) {
        appender.stop();
    }

    void doConfiguration(E appender) {
        if (appender.isStarted()) {
            // if appender is started, it means that the appender has been configured in XML.
            return;
        }
        doConfigurationInner(appender);
    }

    /**
     * build Appender with ConfigurableEnvironment
     *
     * @return Appender
     */
    abstract E loggingExtendAppender();

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
     * determine whether it is necessary to appendAppender
     *
     * @return whether it is necessary to appendAppender
     */
    abstract boolean necessary();

    /**
     * do Configuration for  appender
     *
     * @param appender appender
     */
    abstract void doConfigurationInner(E appender);

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

}
