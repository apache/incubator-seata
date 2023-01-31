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
 * The interface ConfigurationSourceManager.
 *
 * @author wang.liang
 */
public interface ConfigurationSourceManager {

    /**
     * Get main configuration source
     *
     * @return the main configuration source
     */
    ConfigurationSource getMainSource();

    /**
     * Sets main configuration source.
     *
     * @param mainSource the main configuration source
     */
    void setMainSource(ConfigurationSource mainSource);


    /**
     * Get sources
     *
     * @return the sources
     */
    List<ConfigurationSource> getSources();

    /**
     * add configuration source first
     *
     * @param source the configuration source
     */
    default void addSourceFirst(ConfigurationSource source) {
        this.getSources().add(0, source);
        this.afterAddSource();
    }

    /**
     * add configuration source last
     *
     * @param source the configuration source
     */
    default void addSourceLast(ConfigurationSource source) {
        this.getSources().add(source);
        this.afterAddSource();
    }

    /**
     * add configuration source
     *
     * @param source    the configuration source
     * @param predicate the predicate
     * @param addBefore the boolean, true=before | false=after
     */
    default void addSource(ConfigurationSource source, Predicate<ConfigurationSource> predicate, boolean addBefore) {
        boolean added = false;

        // add before the target source, if exist
        List<ConfigurationSource> sources = this.getSources();
        ConfigurationSource targetSource;
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

        this.afterAddSource();
    }

    /**
     * add configuration source before
     *
     * @param source the configuration source
     */
    default void addSourceBefore(ConfigurationSource source, final ConfigurationSource targetSource) {
        addSource(source, s -> s == targetSource, true);
    }

    /**
     * add configuration source before
     *
     * @param source the configuration source
     */
    default void addSourceBefore(ConfigurationSource source, final String targetSourceName) {
        addSource(source, s -> s.getTypeName().equals(targetSourceName), true);
    }

    /**
     * add configuration source after
     *
     * @param source the configuration source
     */
    default void addSourceAfter(ConfigurationSource source, final ConfigurationSource targetSource) {
        addSource(source, s -> s == targetSource, false);
    }

    /**
     * add configuration source after
     *
     * @param source the configuration source
     */
    default void addSourceAfter(ConfigurationSource source, final String targetSourceName) {
        addSource(source, s -> s.getTypeName().equals(targetSourceName), false);
    }

    /**
     * After add source, trigger this method.
     */
    default void afterAddSource() {
    }

}
