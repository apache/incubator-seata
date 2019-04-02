/**
 * @(#)StoreConfig.java, Apr 02, 2019.
 * <p>
 * Copyright 2019 fenbi.com. All rights reserved.
 * FENBI.COM PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.alibaba.fescar.server.store;

import com.alibaba.fescar.config.Configuration;
import com.alibaba.fescar.config.ConfigurationFactory;

/**
 * @author lizhao
 */
public class StoreConfig {

    private static Configuration fileConfiguration = ConfigurationFactory.getInstance();

    private static final String STORE_PREFIX = "store.";

    // default 16kb
    private static final int DEFAULT_MAX_BRANCH_SESSION_SIZE = 1024 * 16;

    // default 512b
    public static final int DEFAULT_MAX_GLOBAL_SESSION_SIZE = 512;

    // default 16kb
    public static final int DEFAULT_WRITE_BUFFER_SIZE = 1024 * 16;

    public static int getMaxBranchSessionSize() {
        return fileConfiguration.getInt(STORE_PREFIX + "max-branch-session-size", DEFAULT_MAX_BRANCH_SESSION_SIZE);
    }

    public static int getMaxGlobalSessionSize() {
        return fileConfiguration.getInt(STORE_PREFIX + "max-global-session-size", DEFAULT_MAX_GLOBAL_SESSION_SIZE);
    }

    public static int getFileWriteBufferCacheSize() {
        return fileConfiguration.getInt(STORE_PREFIX + "file-write-buffer-cache-size", DEFAULT_WRITE_BUFFER_SIZE);
    }
}