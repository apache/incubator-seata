/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.fescar.server.store;

import com.alibaba.fescar.config.Configuration;
import com.alibaba.fescar.config.ConfigurationFactory;

import static com.alibaba.fescar.core.constants.ConfigurationKeys.STORE_PREFIX;

/**
 * @author lizhao
 */
public class StoreConfig {

    private static final Configuration CONFIGURATION = ConfigurationFactory.getInstance();


    // default 16kb
    private static final int DEFAULT_MAX_BRANCH_SESSION_SIZE = 1024 * 16;

    // default 512b
    public static final int DEFAULT_MAX_GLOBAL_SESSION_SIZE = 512;

    // default 16kb
    public static final int DEFAULT_WRITE_BUFFER_SIZE = 1024 * 16;

    public static int getMaxBranchSessionSize() {
        return CONFIGURATION.getInt(STORE_PREFIX + "max-branch-session-size", DEFAULT_MAX_BRANCH_SESSION_SIZE);
    }

    public static int getMaxGlobalSessionSize() {
        return CONFIGURATION.getInt(STORE_PREFIX + "max-global-session-size", DEFAULT_MAX_GLOBAL_SESSION_SIZE);
    }

    public static int getFileWriteBufferCacheSize() {
        return CONFIGURATION.getInt(STORE_PREFIX + "file-write-buffer-cache-size", DEFAULT_WRITE_BUFFER_SIZE);
    }
}