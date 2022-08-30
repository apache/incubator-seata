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
package io.seata.core.rpc;

import io.netty.util.NettyRuntime;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import org.apache.commons.lang.StringUtils;

import static io.seata.common.DefaultValues.DEFAULT_TRANSPORT_HEARTBEAT;

/**
 * @author goodboycoder
 */
public class BaseRpcConfig {


    /**
     * The constant CONFIG.
     */
    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();

    /**
     * The constant WORKER_THREAD_SIZE.
     */
    protected static int WORKER_THREAD_SIZE;

    /**
     * Heartbeat config
     */
    private static final boolean ENABLE_HEARTBEAT;

    private static final int DEFAULT_WRITE_IDLE_SECONDS = 5;

    private static final int READIDLE_BASE_WRITEIDLE = 3;

    /**
     * The constant MAX_WRITE_IDLE_SECONDS.
     */
    protected static final int MAX_WRITE_IDLE_SECONDS;

    /**
     * The constant MAX_READ_IDLE_SECONDS.
     */
    protected static final int MAX_READ_IDLE_SECONDS;

    /**
     * The constant MAX_ALL_IDLE_SECONDS.
     */
    protected static final int MAX_ALL_IDLE_SECONDS = 0;

    static {
        String workerThreadSize = CONFIG.getConfig(ConfigurationKeys.WORKER_THREAD_SIZE);
        if (StringUtils.isNotBlank(workerThreadSize) && StringUtils.isNumeric(workerThreadSize)) {
            WORKER_THREAD_SIZE = Integer.parseInt(workerThreadSize);
        } else if (BaseRpcConfig.WorkThreadMode.getModeByName(workerThreadSize) != null) {
            WORKER_THREAD_SIZE = BaseRpcConfig.WorkThreadMode.getModeByName(workerThreadSize).getValue();
        } else {
            WORKER_THREAD_SIZE = BaseRpcConfig.WorkThreadMode.Default.getValue();
        }


        ENABLE_HEARTBEAT = CONFIG.getBoolean(ConfigurationKeys.TRANSPORT_HEARTBEAT, DEFAULT_TRANSPORT_HEARTBEAT);
        if (ENABLE_HEARTBEAT) {
            MAX_WRITE_IDLE_SECONDS = DEFAULT_WRITE_IDLE_SECONDS;
        } else {
            MAX_WRITE_IDLE_SECONDS = 0;
        }
        MAX_READ_IDLE_SECONDS = MAX_WRITE_IDLE_SECONDS * READIDLE_BASE_WRITEIDLE;
    }

    /**
     * The enum Work thread mode.
     */
    public enum WorkThreadMode {

        /**
         * Auto work thread mode.
         */
        Auto(NettyRuntime.availableProcessors() * 2 + 1),
        /**
         * Pin work thread mode.
         */
        Pin(NettyRuntime.availableProcessors()),
        /**
         * Busy pin work thread mode.
         */
        BusyPin(NettyRuntime.availableProcessors() + 1),
        /**
         * Default work thread mode.
         */
        Default(NettyRuntime.availableProcessors() * 2);

        /**
         * Gets value.
         *
         * @return the value
         */
        public int getValue() {
            return value;
        }

        private int value;

        WorkThreadMode(int value) {
            this.value = value;
        }

        /**
         * Gets mode by name.
         *
         * @param name the name
         * @return the mode by name
         */
        public static BaseRpcConfig.WorkThreadMode getModeByName(String name) {
            for (BaseRpcConfig.WorkThreadMode mode : values()) {
                if (mode.name().equalsIgnoreCase(name)) {
                    return mode;
                }
            }
            return null;
        }
    }
}
