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

/**
 * The interface ConfigSource.
 *
 * @author wang.liang
 * @author slievrly
 */
public interface ConfigSource {

    /**
     * Get latest config.
     *
     * @param dataId       the data id
     * @param timeoutMills the timeout mills
     * @return the Latest config
     */
    Object getLatestConfig(String dataId, long timeoutMills);

    /**
     * Get the type name
     *
     * @return the type name
     */
    String getTypeName();
}