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
package io.seata.spring.boot.autoconfigure.properties.server.store;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static io.seata.common.DefaultValues.DEFAULT_SERVICE_SESSION_RELOAD_READ_SIZE;
import static io.seata.spring.boot.autoconfigure.StarterConstants.STORE_FILE_PREFIX;

/**
 * @author spilledyear@outlook.com
 */
@Component
@ConfigurationProperties(prefix = STORE_FILE_PREFIX)
public class StoreFileProperties {
    private String dir = "sessionStore";
    private Integer maxBranchSessionSize = 16384;
    private Integer maxGlobalSessionSize = 512;
    private Integer fileWriteBufferCacheSize = 16384;
    private Integer sessionReloadReadSize = DEFAULT_SERVICE_SESSION_RELOAD_READ_SIZE;
    private String flushDiskMode = "async";

    public String getDir() {
        return dir;
    }

    public StoreFileProperties setDir(String dir) {
        this.dir = dir;
        return this;
    }

    public Integer getMaxBranchSessionSize() {
        return maxBranchSessionSize;
    }

    public StoreFileProperties setMaxBranchSessionSize(Integer maxBranchSessionSize) {
        this.maxBranchSessionSize = maxBranchSessionSize;
        return this;
    }

    public Integer getMaxGlobalSessionSize() {
        return maxGlobalSessionSize;
    }

    public StoreFileProperties setMaxGlobalSessionSize(Integer maxGlobalSessionSize) {
        this.maxGlobalSessionSize = maxGlobalSessionSize;
        return this;
    }

    public Integer getFileWriteBufferCacheSize() {
        return fileWriteBufferCacheSize;
    }

    public StoreFileProperties setFileWriteBufferCacheSize(Integer fileWriteBufferCacheSize) {
        this.fileWriteBufferCacheSize = fileWriteBufferCacheSize;
        return this;
    }

    public Integer getSessionReloadReadSize() {
        return sessionReloadReadSize;
    }

    public StoreFileProperties setSessionReloadReadSize(Integer sessionReloadReadSize) {
        this.sessionReloadReadSize = sessionReloadReadSize;
        return this;
    }

    public String getFlushDiskMode() {
        return flushDiskMode;
    }

    public StoreFileProperties setFlushDiskMode(String flushDiskMode) {
        this.flushDiskMode = flushDiskMode;
        return this;
    }
}
