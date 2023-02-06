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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The interface ConfigSourceManager.
 *
 * @author wang.liang
 */
public interface ConfigSourceManager {

    //region main source

    /**
     * Get main source
     *
     * @return the main source
     */
    ConfigSource getMainSource();

    /**
     * Sets main source.
     *
     * @param mainSource the main source
     */
    void setMainSource(ConfigSource mainSource);

    //endregion


    /**
     * Get all the sources, contains the main source.
     *
     * @return the sources
     */
    @Nonnull
    List<ConfigSource> getSources();

    /**
     * Get source map.
     *
     * @return The source map
     */
    @Nonnull
    Map<String, ConfigSource> getSourceMap();

    /**
     * Get source by name.
     *
     * @param sourceName the source name
     * @return the source
     */
    @Nullable
    default ConfigSource getSource(String sourceName) {
        return getSourceMap().get(sourceName);
    }

    /**
     * add source
     *
     * @param newSource the new source
     */
    default void addSource(@Nonnull ConfigSource newSource) {
        Objects.requireNonNull(newSource, "The 'newSource' must not be null.");


        // add before the target source, if exist
        List<ConfigSource> sources = this.getSources();
        int newSourceIndex = -1;

        ConfigSource current;
        for (int i = 0; i < sources.size(); i++) {
            current = sources.get(i);

            if (newSource.getOrder() > current.getOrder()) {
                sources.add(i, newSource);
                newSourceIndex = i;
                break;
            }
        }

        List<ConfigSource> higherSources;
        List<ConfigSource> lowerSources;
        if (newSourceIndex >= 0) {
            higherSources = sources.subList(0, newSourceIndex);
            lowerSources = sources.subList(newSourceIndex + 1, sources.size() - 1);
        } else {
            higherSources = new ArrayList<>(sources);
            lowerSources = Collections.emptyList();
            sources.add(newSource);
        }


        this.onAddedSource(newSource, higherSources, lowerSources);
    }

    /**
     * After adding a new source, trigger this method.
     *
     * @param newSource     the new source
     * @param higherSources the sources with higher priority than newSource
     * @param lowerSources  the sources with lower priority than newSource
     */
    default void onAddedSource(ConfigSource newSource, List<ConfigSource> higherSources, List<ConfigSource> lowerSources) {

    }
}
