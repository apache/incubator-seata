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
package io.seata.server.logging.logback;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.hook.DelayingShutdownHook;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.Duration;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.CollectionUtils;
import io.seata.server.logging.listener.LoggingExtendLoggerContextListener;
import io.seata.server.logging.listener.SystemPropertyLoggerContextListener;
import org.slf4j.impl.StaticLoggerBinder;
import org.springframework.boot.logging.logback.WhitespaceThrowableProxyConverter;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.springframework.boot.context.logging.LoggingApplicationListener.REGISTER_SHUTDOWN_HOOK_PROPERTY;

/**
 * The type of LogbackExtendConfigurator,to load all
 * {@link LogbackLoggingExtendAppenderProvider}.
 *
 * @author wlx
 * @see LogbackLoggingExtendAppenderProvider
 */
public final class LogbackExtendConfigurator {

    private final List<LogbackLoggingExtendAppenderProvider> loggingExtendAppenderProviders;

    private final ConfigurableEnvironment environment;

    private static final AtomicBoolean SHUTDOWN_HOOK_REGISTERED = new AtomicBoolean();

    private static final AtomicBoolean LISTENER_REGISTERED = new AtomicBoolean();

    private final LoggerContext loggerContext;

    private LogbackExtendConfigurator(ConfigurableEnvironment environment) {
        this.environment = environment;
        this.loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();
        this.loggingExtendAppenderProviders = EnhancedServiceLoader.loadAll(
                LogbackLoggingExtendAppenderProvider.class, new Class[]{ConfigurableEnvironment.class}
                , new Object[]{environment});
    }

    /**
     * append special logback logging extend configuration to loggerContext.
     */
    public void doLoggingExtendConfiguration() {
        synchronized (loggerContext.getConfigurationLock()) {
            loadDefaultConversionRule();
            loadDefaultLoggerContextListener();
            boolean necessary = false;
            if (!CollectionUtils.isEmpty(loggingExtendAppenderProviders)) {
                for (LogbackLoggingExtendAppenderProvider provider : loggingExtendAppenderProviders) {
                    if (provider.shouldAppend()) {
                        provider.appendTo();
                        necessary = true;
                    }
                }
            }
            if (necessary) {
                registerShutdownHookIfNecessary();
                registerLoggingExtendLoggerContextListenerIfNecessary();
            }
            checkErrorStatus();
        }
    }

    /**
     * stop appender
     */
    public void stop() {
        synchronized (loggerContext.getConfigurationLock()) {
            if (!CollectionUtils.isEmpty(loggingExtendAppenderProviders)) {
                loggingExtendAppenderProviders.forEach(
                        LogbackLoggingExtendAppenderProvider::stop
                );
                loggingExtendAppenderProviders.clear();
            }
        }
    }


    @SuppressWarnings("unchecked")
    void conversionRule(String conversionWord, Class<? extends Converter<?>> converterClass) {
        Assert.hasLength(conversionWord, "Conversion word must not be empty");
        Assert.notNull(converterClass, "Converter class must not be null");
        Map<String, String> registry = (Map<String, String>) this.loggerContext
                .getObject(CoreConstants.PATTERN_RULE_REGISTRY);
        if (registry == null) {
            registry = new HashMap<>();
            this.loggerContext.putObject(CoreConstants.PATTERN_RULE_REGISTRY, registry);
        }
        registry.put(conversionWord, converterClass.getName());
    }

    void loggerContextListener(LoggerContextListener loggerContextListener) {
        Assert.notNull(loggerContextListener, "loggerContextListener must not be null");
        if (!existLoggingLoggerContextListener(loggerContextListener.getClass())) {
            loggerContext.addListener(loggerContextListener);
        }
    }

    private void loadDefaultConversionRule() {
        // for default pattern "stack_trace": "%wex"
        conversionRule("wex", WhitespaceThrowableProxyConverter.class);
    }

    private void loadDefaultLoggerContextListener() {
        SystemPropertyLoggerContextListener systemPropertyLoggerContextListener = new SystemPropertyLoggerContextListener();
        systemPropertyLoggerContextListener.setContext(loggerContext);
        systemPropertyLoggerContextListener.start();
        loggerContextListener(systemPropertyLoggerContextListener);
    }

    private static class SingletonHolder {
        private static LogbackExtendConfigurator INSTANCE;
    }

    /**
     * Get resource logbackComposedLoggingExtendProvider.
     *
     * @return the resource logbackComposedLoggingExtendProvider
     */
    public static LogbackExtendConfigurator get(ConfigurableEnvironment environment) {
        if (Objects.isNull(SingletonHolder.INSTANCE)) {
            SingletonHolder.INSTANCE = new LogbackExtendConfigurator(environment);
        }
        return SingletonHolder.INSTANCE;
    }

    private void registerShutdownHookIfNecessary() {
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

    private void registerLoggingExtendLoggerContextListenerIfNecessary() {
        if (LISTENER_REGISTERED.compareAndSet(false, true)) {
            loggerContext.addListener(new LoggingExtendLoggerContextListener(environment));
        }
    }

    private boolean existLoggingLoggerContextListener(Class<? extends LoggerContextListener>
                                                              loggerContextListenerClass) {
        List<LoggerContextListener> listenerList = loggerContext.getCopyOfListenerList();
        return listenerList.stream().anyMatch(
            loggerContextListener -> loggerContextListenerClass.equals(loggerContextListener.getClass())
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
