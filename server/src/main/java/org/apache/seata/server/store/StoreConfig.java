/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.server.store;

import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.apache.seata.server.env.ContainerHelper;
import org.apache.seata.server.storage.file.FlushDiskMode;

import static org.apache.seata.common.DefaultValues.SERVER_DEFAULT_STORE_MODE;
import static org.apache.seata.core.constants.ConfigurationKeys.STORE_FILE_PREFIX;

/**
 */
public class StoreConfig {

    private static final Configuration CONFIGURATION = ConfigurationFactory.getInstance();
    private static StoreMode storeMode;
    private static SessionMode sessionMode;
    private static LockMode lockMode;

    /**
     * set storeMode sessionMode lockMode from StartupParameter
     *
     * @param storeMode   storeMode
     * @param sessionMode sessionMode
     * @param lockMode    lockMode
     */
    public static void setStartupParameter(String storeMode, String sessionMode, String lockMode) {
        if (StringUtils.isNotBlank(storeMode)) {
            StoreConfig.storeMode = StoreMode.get(storeMode);
        }
        if (StringUtils.isNotBlank(sessionMode)) {
            StoreConfig.sessionMode = SessionMode.get(sessionMode);
        }
        if (StringUtils.isNotBlank(lockMode)) {
            StoreConfig.lockMode = LockMode.get(lockMode);
        }
    }

    /**
     * Default 16kb.
     */
    private static final int DEFAULT_MAX_BRANCH_SESSION_SIZE = 1024 * 16;

    /**
     * Default 512b.
     */
    private static final int DEFAULT_MAX_GLOBAL_SESSION_SIZE = 512;

    /**
     * Default 16kb.
     */
    private static final int DEFAULT_WRITE_BUFFER_SIZE = 1024 * 16;

    public static int getMaxBranchSessionSize() {
        return CONFIGURATION.getInt(STORE_FILE_PREFIX + "maxBranchSessionSize", DEFAULT_MAX_BRANCH_SESSION_SIZE);
    }

    public static int getMaxGlobalSessionSize() {
        return CONFIGURATION.getInt(STORE_FILE_PREFIX + "maxGlobalSessionSize", DEFAULT_MAX_GLOBAL_SESSION_SIZE);
    }

    public static int getFileWriteBufferCacheSize() {
        return CONFIGURATION.getInt(STORE_FILE_PREFIX + "fileWriteBufferCacheSize", DEFAULT_WRITE_BUFFER_SIZE);
    }

    public static FlushDiskMode getFlushDiskMode() {
        return FlushDiskMode.findDiskMode(CONFIGURATION.getConfig(STORE_FILE_PREFIX + "flushDiskMode"));
    }

    /**
     * only for inner call
     *
     * @return
     */
    private static StoreMode getStoreMode() {
        //startup
        if (null != storeMode) {
            return storeMode;
        }
        //env
        String storeModeEnv = ContainerHelper.getStoreMode();
        if (StringUtils.isNotBlank(storeModeEnv)) {
            return StoreMode.get(storeModeEnv);
        }
        //config
        String storeModeConfig = CONFIGURATION.getConfig(ConfigurationKeys.STORE_MODE, SERVER_DEFAULT_STORE_MODE);
        return StoreMode.get(storeModeConfig);
    }

    public static SessionMode getSessionMode() {
        //startup
        if (null != sessionMode) {
            return sessionMode;
        }
        //env
        String sessionModeEnv = ContainerHelper.getSessionStoreMode();
        if (StringUtils.isNotBlank(sessionModeEnv)) {
            return SessionMode.get(sessionModeEnv);
        }
        //config
        String sessionModeConfig = CONFIGURATION.getConfig(ConfigurationKeys.STORE_SESSION_MODE);
        if (StringUtils.isNotBlank(sessionModeConfig)) {
            return SessionMode.get(sessionModeConfig);
        }
        // complication old config
        return SessionMode.get(getStoreMode().name());
    }

    public static LockMode getLockMode() {
        //startup
        if (null != lockMode) {
            return lockMode;
        }
        //env
        String lockModeEnv = ContainerHelper.getLockStoreMode();
        if (StringUtils.isNotBlank(lockModeEnv)) {
            return LockMode.get(lockModeEnv);
        }
        //config
        String lockModeConfig = CONFIGURATION.getConfig(ConfigurationKeys.STORE_LOCK_MODE);
        if (StringUtils.isNotBlank(lockModeConfig)) {
            return LockMode.get(lockModeConfig);
        }
        // complication old config
        return LockMode.get(getStoreMode().name());
    }

    public enum StoreMode {
        /**
         * The File store mode.
         */
        FILE("file"),
        /**
         * The Db store mode.
         */
        DB("db"),
        /**
         * The Redis store mode.
         */
        REDIS("redis"),
        /**
         * The Raft store mode.
         */
        RAFT("raft");

        private String name;

        StoreMode(String name) {
            this.name = name;
        }

        public static StoreMode get(String name) {
            for (StoreMode mode : StoreMode.values()) {
                if (mode.getName().equalsIgnoreCase(name)) {
                    return mode;
                }
            }
            throw new IllegalArgumentException("unknown store mode:" + name);
        }

        public String getName() {
            return name;
        }
    }

    public enum SessionMode {
        /**
         * The File store mode.
         */
        FILE("file"),
        /**
         * The Db store mode.
         */
        DB("db"),
        /**
         * The Redis store mode.
         */
        REDIS("redis"),
        /**
         * raft store
         */
        RAFT("raft");

        private String name;

        SessionMode(String name) {
            this.name = name;
        }

        public static SessionMode get(String name) {
            for (SessionMode mode : SessionMode.values()) {
                if (mode.getName().equalsIgnoreCase(name)) {
                    return mode;
                }
            }
            throw new IllegalArgumentException("unknown session mode:" + name);
        }

        public String getName() {
            return name;
        }
    }

    public enum LockMode {
        /**
         * The File store mode.
         */
        FILE("file"),
        /**
         * The Db store mode.
         */
        DB("db"),
        /**
         * The Redis store mode.
         */
        REDIS("redis"),
        /**
         * raft store
         */
        RAFT("raft");

        private String name;

        LockMode(String name) {
            this.name = name;
        }

        public static LockMode get(String name) {
            for (LockMode mode : LockMode.values()) {
                if (mode.getName().equalsIgnoreCase(name)) {
                    return mode;
                }
            }
            throw new IllegalArgumentException("unknown lock mode:" + name);
        }

        public String getName() {
            return name;
        }
    }

}
