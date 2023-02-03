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
package io.seata.config.source;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import io.seata.common.executor.Initialize;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.config.changelistener.AbstractConfigurationChangeListenerManager;
import io.seata.config.changelistener.ConfigChangeListenerUtils;
import io.seata.config.changelistener.ConfigurationChangeEvent;
import io.seata.config.changelistener.ConfigurationChangeType;

/**
 * The type AbstractScheduledUpdateConfigSource.
 *
 * @author wang.liang
 */
public abstract class AbstractScheduledUpdateConfigSource extends AbstractConfigurationChangeListenerManager
        implements ScheduledUpdateConfigSource, Initialize {

    private static final String EXECUTOR_SERVICE_PREFIX = "scheduledUpdateSource";
    private static final long DEFAULT_EXECUTOR_SERVICE_PERIOD = 1000L;

    // TODO: remove
    private static final long t0 = System.currentTimeMillis();

    /**
     * The name
     */
    @Nonnull
    private final String name;

    private final ScheduledThreadPoolExecutor executorService;
    private final long executorServicePeriod;

    private final Map<String, String> configOldValueCacheMap = new HashMap<>();


    protected AbstractScheduledUpdateConfigSource(@Nonnull String name, boolean allowAutoUpdate, long executorServicePeriod) {
        Objects.requireNonNull(name, "The 'name' must not be null.");
        this.name = name;

        if (allowAutoUpdate) {
            this.executorService = this.buildExecutorService(name);
        } else {
            this.executorService = null;
        }

        this.executorServicePeriod = executorServicePeriod;
    }

    protected AbstractScheduledUpdateConfigSource(@Nonnull String name, boolean allowAutoUpdate) {
        this(name, allowAutoUpdate, DEFAULT_EXECUTOR_SERVICE_PERIOD);
    }

    protected AbstractScheduledUpdateConfigSource(@Nonnull String name, long executorServicePeriod) {
        this(name, executorServicePeriod > 0, executorServicePeriod);
    }

    protected AbstractScheduledUpdateConfigSource(@Nonnull String name, ScheduledThreadPoolExecutor executorService, long executorServicePeriod) {
        Objects.requireNonNull(name, "The 'name' must not be null.");
        this.name = name;
        this.executorService = executorService;
        this.executorServicePeriod = executorServicePeriod;
    }

    protected AbstractScheduledUpdateConfigSource(@Nonnull String name, ScheduledThreadPoolExecutor executorService) {
        this(name, executorService, DEFAULT_EXECUTOR_SERVICE_PERIOD);
    }


    /**
     * Check whether the configuration is changed.
     */
    private void checkWhetherConfigChanged() {
        // TODO: remove
        System.out.println("time: " + (System.currentTimeMillis() - t0));

        // First, reload config source.
        if (this.reloadConfigSource()) {
            // Then, do check whether config changed.
            getListenedConfigDataIds().forEach(this::doCheckWhetherConfigChanged);
        }
    }

    /**
     * Reload the config from the source.
     * <p>
     * Can be overridden in subclasses.
     */
    protected boolean reloadConfigSource() {
        // default: do nothing
        return true;
    }

    protected void doCheckWhetherConfigChanged(String dataId) {
        String oldValue = this.configOldValueCacheMap.get(dataId);
        String newValue = this.getLatestConfig(dataId);
        if (!Objects.equals(newValue, oldValue)) {
            // Get change type by oldValue and newValue.
            ConfigurationChangeType type = ConfigChangeListenerUtils.getChangeType(oldValue, newValue);
            // Build change event.
            ConfigurationChangeEvent event = this.buildChangeEvent(dataId, oldValue, newValue, type);
            // Trigger the listener's onChangeEvent.
            getConfigListeners(dataId).forEach(listener -> listener.onChangeEvent(event));
        }
    }

    protected ConfigurationChangeEvent buildChangeEvent(String dataId, String oldValue, String newValue, ConfigurationChangeType type) {
        return new ConfigurationChangeEvent(dataId, null, oldValue, newValue, type, this);
    }


    //region # Override ScheduledThreadPoolExecutor

    private ScheduledThreadPoolExecutor buildExecutorService(String name) {
        return new ScheduledThreadPoolExecutor(1, new NamedThreadFactory(EXECUTOR_SERVICE_PREFIX + "_" + name, 1));
    }

    private void initExecutorService() {
        if (this.executorService != null) {
            this.executorService.scheduleAtFixedRate(this::checkWhetherConfigChanged, 0, executorServicePeriod, TimeUnit.MILLISECONDS);
        }
    }

    //endregion # Override ScheduledThreadPoolExecutor


    //region # Override ScheduledUpdateConfigSource

    @Override
    public void start() {
        if (this.executorService != null && this.executorService.isShutdown()) {
            this.initExecutorService();
        }
    }

    @Override
    public void shutdown() {
        if (this.executorService != null) {
            this.executorService.shutdown();
        }
    }

    @Nonnull
    @Override
    public String getName() {
        return name;
    }

    //endregion # Override ScheduledUpdateConfigSource


    //region # Override Initialize

    /**
     * Whether initialized
     */
    private volatile boolean initialized = false;

    @Override
    public void init() {
        this.start();
        this.setInitialized(true);
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    protected void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    //endregion # Override Initialize
}
