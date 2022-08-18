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
package io.seata.config.polaris;

import java.util.concurrent.Executor;

/**
 * {@link PolarisConfigChangeListener} Definition .
 *
 * @author <a href="mailto:iskp.me@gmail.com">Palmer Xu</a> 2022-08-18
 */
public interface PolarisConfigChangeListener {

    /**
     * Get executor for execute this receive.
     *
     * @return Executor
     */
    default Executor getExecutor() {
        return null;
    }

    /**
     * Receive config info.
     *
     * @param namespace  config file namespace
     * @param group config file group
     * @param fileName config file name
     * @param content config info
     */
    void receiveConfigInfo(final String namespace, final String group, final String fileName, final String content);
}
