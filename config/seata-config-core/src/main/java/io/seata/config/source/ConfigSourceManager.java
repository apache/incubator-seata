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

import java.util.List;
import java.util.function.Predicate;

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
     * Get sources
     *
     * @return the sources
     */
    List<ConfigSource> getSources();

    /**
     * add source to first position
     *
     * @param newSource the new source
     */
    default void addSourceFirst(ConfigSource newSource) {
        this.getSources().add(0, newSource);
        this.afterAddingSource(newSource);
    }

    /**
     * add source to last position
     *
     * @param newSource the new source
     */
    default void addSourceLast(ConfigSource newSource) {
        this.getSources().add(newSource);
        this.afterAddingSource(newSource);
    }

    /**
     * add source
     *
     * @param newSource the new source
     * @param predicate the predicate
     * @param addBefore the boolean, true=before | false=after
     */
    default void addSource(ConfigSource newSource, Predicate<ConfigSource> predicate, boolean addBefore) {
        boolean added = false;

        // add before the target source, if exist
        List<ConfigSource> sources = this.getSources();
        ConfigSource targetSource;
        for (int i = 0; i < sources.size(); i++) {
            targetSource = sources.get(i);
            if (predicate.test(targetSource)) {
                if (addBefore) {
                    sources.add(i, newSource);
                    added = true;
                } else if (i < sources.size() - 1) {
                    sources.add(i + 1, newSource);
                    added = true;
                }
                break;
            }
        }

        // if not added, add to the last of the sources
        if (!added) {
            this.addSourceLast(newSource);
        }

        this.afterAddingSource(newSource);
    }

    /**
     * add source before
     *
     * @param newSource    the new source
     * @param targetSource the target source
     */
    default void addSourceBefore(ConfigSource newSource, final ConfigSource targetSource) {
        addSource(newSource, s -> s == targetSource, true);
    }

    /**
     * add source before
     *
     * @param source the source
     */
    default void addSourceBefore(ConfigSource source, final String targetSourceName) {
        addSource(source, s -> s.getTypeName().equals(targetSourceName), true);
    }

    /**
     * add new source after
     *
     * @param newSource the new source
     */
    default void addSourceAfter(ConfigSource newSource, final ConfigSource targetSource) {
        addSource(newSource, s -> s == targetSource, false);
    }

    /**
     * add new source after
     *
     * @param newSource the new source
     */
    default void addSourceAfter(ConfigSource newSource, final String targetSourceName) {
        addSource(newSource, s -> s.getTypeName().equals(targetSourceName), false);
    }

    /**
     * After adding new source, trigger this method.
     */
    default void afterAddingSource(ConfigSource newSource) {
    }

}
