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
package io.seata.config;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nonnull;

import io.seata.common.executor.Initialize;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.config.processor.ConfigurationProcessor;
import io.seata.config.source.ConfigSource;

/**
 * The type Simple configuration
 *
 * @author wang.liang
 */
public class SimpleConfiguration extends AbstractConfiguration {

    public static final String DEFAULT_NAME = "simple-configuration";


    /**
     * The main source.
     */
    protected volatile ConfigSource mainSource;

    /**
     * All the sources, contains the main source.
     */
    private final List<ConfigSource> sources = new CopyOnWriteArrayList<>();

    /**
     * The source map, contains the main source.
     */
    private final Map<String, ConfigSource> sourceMap = new ConcurrentHashMap<>(8);


    public SimpleConfiguration() {
        this(DEFAULT_NAME);
    }

    public SimpleConfiguration(String name) {
        super(name);
    }


    //region # Override Initialize

    @Override
    public void init() {
        this.doInit();
        super.setInitialized(true);
    }

    /**
     * Load the processors and process current configuration.
     */
    protected void doInit() {
        List<ConfigurationProcessor> processors = EnhancedServiceLoader.loadAll(ConfigurationProcessor.class);
        for (ConfigurationProcessor processor : processors) {
            processor.process(this);
        }

        // init the sources
        sources.forEach(source -> {
            // If not initialized, do init.
            if (source instanceof Initialize && !((Initialize)source).isInitialized()) {
                ((Initialize)source).init();
            }
        });
    }

    //endregion


    //region # Override ConfigSourceManager

    @Override
    public ConfigSource getMainSource() {
        return this.mainSource;
    }

    @Override
    public void setMainSource(ConfigSource mainSource) {
        this.mainSource = mainSource;
    }

    @Nonnull
    @Override
    public List<ConfigSource> getSources() {
        return this.sources;
    }

    @Nonnull
    @Override
    public Map<String, ConfigSource> getSourceMap() {
        return this.sourceMap;
    }

    //endregion
}
