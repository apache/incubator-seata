package io.seata.server.logging.logback;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.hook.DelayingShutdownHook;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.Duration;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.CollectionUtils;
import io.seata.server.logging.listener.LoggingLoggerContextListener;
import org.slf4j.impl.StaticLoggerBinder;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.springframework.boot.context.logging.LoggingApplicationListener.REGISTER_SHUTDOWN_HOOK_PROPERTY;

/**
 * The type of LogbackCompositeLoggingExtendProvider,to load all
 * {@link LogbackLoggingExtendAppenderProvider}.
 *
 * @author wlx
 * @date 2022/6/24 9:53 下午
 * @see LogbackLoggingExtendAppenderProvider
 */
public class LogbackCompositeLoggingExtendProvider {

    private final List<LogbackLoggingExtendAppenderProvider> loggingExtendAppenderProviders;

    private final ConfigurableEnvironment environment;

    private static final AtomicBoolean SHUTDOWN_HOOK_REGISTERED = new AtomicBoolean();

    private static final AtomicBoolean LOGGER_LISTENER_REGISTERED = new AtomicBoolean();

    protected LoggerContext loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();

    private LogbackCompositeLoggingExtendProvider(ConfigurableEnvironment environment) {
        this.environment = environment;
        this.loggingExtendAppenderProviders = EnhancedServiceLoader.loadAll(
                LogbackLoggingExtendAppenderProvider.class, new Class[]{ConfigurableEnvironment.class}
                , new Object[]{environment});
    }

    /**
     * append logging appender to loggerContext.
     */
    public void appendToContext() {
        if (!CollectionUtils.isEmpty(loggingExtendAppenderProviders)) {
            loggingExtendAppenderProviders.forEach(
                    provider -> {
                        if (provider.shouldAppend()) {
                            provider.appendTo();
                        }
                    }
            );
        }
        registerShutdownHookIfNecessary(environment);
        registerLoggerContextListenerIfNecessary(environment);
        checkErrorStatus();
    }

    private static class SingletonHolder {
        private static LogbackCompositeLoggingExtendProvider INSTANCE;
    }

    /**
     * Get resource logbackComposedLoggingExtendProvider.
     *
     * @return the resource logbackComposedLoggingExtendProvider
     */
    public static LogbackCompositeLoggingExtendProvider get(ConfigurableEnvironment environment) {
        if (Objects.isNull(SingletonHolder.INSTANCE)) {
            SingletonHolder.INSTANCE = new LogbackCompositeLoggingExtendProvider(environment);
        }
        return SingletonHolder.INSTANCE;
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

    private void registerLoggerContextListenerIfNecessary(ConfigurableEnvironment environment) {
        if (LOGGER_LISTENER_REGISTERED.compareAndSet(false, true) && !existLoggingLoggerContextListener()) {
            loggerContext.addListener(new LoggingLoggerContextListener(environment));
        }
    }

    private boolean existLoggingLoggerContextListener() {
        List<LoggerContextListener> listenerList = loggerContext.getCopyOfListenerList();
        return listenerList.stream().anyMatch(
                loggerContextListener -> loggerContextListener instanceof LoggingLoggerContextListener
        );
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
