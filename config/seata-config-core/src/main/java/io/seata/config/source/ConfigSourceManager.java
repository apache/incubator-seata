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

    /**
     * Get main config source
     *
     * @return the main config source
     */
    ConfigSource getMainSource();

    /**
     * Sets main config source.
     *
     * @param mainSource the main config source
     */
    void setMainSource(ConfigSource mainSource);


    /**
     * Get sources
     *
     * @return the sources
     */
    List<ConfigSource> getSources();

    /**
     * add config source to first position
     *
     * @param source the config source
     */
    default void addSourceFirst(ConfigSource source) {
        this.getSources().add(0, source);
        this.afterAddSource(source);
    }

    /**
     * add config source to last position
     *
     * @param source the config source
     */
    default void addSourceLast(ConfigSource source) {
        this.getSources().add(source);
        this.afterAddSource(source);
    }

    /**
     * add config source
     *
     * @param source    the config source
     * @param predicate the predicate
     * @param addBefore the boolean, true=before | false=after
     */
    default void addSource(ConfigSource source, Predicate<ConfigSource> predicate, boolean addBefore) {
        boolean added = false;

        // add before the target source, if exist
        List<ConfigSource> sources = this.getSources();
        ConfigSource targetSource;
        for (int i = 0; i < sources.size(); i++) {
            targetSource = sources.get(i);
            if (predicate.test(targetSource)) {
                if (addBefore) {
                    sources.add(i, source);
                    added = true;
                } else if (i < sources.size() - 1) {
                    sources.add(i + 1, source);
                    added = true;
                }
                break;
            }
        }

        // if not added, add to the last of the sources
        if (!added) {
            this.addSourceLast(source);
        }

        this.afterAddSource(source);
    }

    /**
     * add config source before
     *
     * @param source the config source
     */
    default void addSourceBefore(ConfigSource source, final ConfigSource targetSource) {
        addSource(source, s -> s == targetSource, true);
    }

    /**
     * add config source before
     *
     * @param source the config source
     */
    default void addSourceBefore(ConfigSource source, final String targetSourceName) {
        addSource(source, s -> s.getTypeName().equals(targetSourceName), true);
    }

    /**
     * add config source after
     *
     * @param source the config source
     */
    default void addSourceAfter(ConfigSource source, final ConfigSource targetSource) {
        addSource(source, s -> s == targetSource, false);
    }

    /**
     * add config source after
     *
     * @param source the config source
     */
    default void addSourceAfter(ConfigSource source, final String targetSourceName) {
        addSource(source, s -> s.getTypeName().equals(targetSourceName), false);
    }

    /**
     * After add source, trigger this method.
     */
    default void afterAddSource(ConfigSource source) {
    }

}
