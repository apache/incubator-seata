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
package io.seata.server.env;

import io.seata.common.util.NumberUtils;
import io.seata.common.util.StringUtils;

import static io.seata.core.constants.ConfigurationKeys.ENV_SEATA_PORT_KEY;

/**
 * @author xingfudeshi@gmail.com
 * @author wang.liang
 */
public class ContainerHelper {

    private static final String C_GROUP_PATH = "/proc/1/cgroup";
    private static final String DOCKER_PATH = "/docker";
    private static final String KUBEPODS_PATH = "/kubepods";

    private static final String ENV_SYSTEM_KEY = "SEATA_ENV";
    private static final String ENV_SEATA_IP_KEY = "SEATA_IP";
    private static final String ENV_SERVER_NODE_KEY = "SERVER_NODE";
    private static final String ENV_STORE_MODE_KEY = "STORE_MODE";
    private static final String ENV_LOCK_STORE_MODE_KEY = "LOCK_STORE_MODE";
    private static final String ENV_SESSION_STORE_MODE_KEY = "SESSION_STORE_MODE";

    /**
     * Gets env from container.
     *
     * @return the env
     */
    public static String getEnv() {
        return StringUtils.trimToNull(System.getenv(ENV_SYSTEM_KEY));
    }

    /**
     * Gets host from container.
     *
     * @return the env
     */
    public static String getHost() {
        return StringUtils.trimToNull(System.getenv(ENV_SEATA_IP_KEY));
    }

    /**
     * Gets port from container.
     *
     * @return the env
     */
    public static int getPort() {
        return NumberUtils.toInt(System.getenv(ENV_SEATA_PORT_KEY), 0);
    }

    /**
     * Gets server node from container.
     *
     * @return the env
     */
    public static Long getServerNode() {
        return NumberUtils.toLong(System.getenv(ENV_SERVER_NODE_KEY));
    }

    /**
     * Gets store mode from container.
     *
     * @return the env
     */
    public static String getStoreMode() {
        return StringUtils.trimToNull(System.getenv(ENV_STORE_MODE_KEY));
    }

    /**
     * Gets session store mode from container.
     *
     * @return the env
     */
    public static String getSessionStoreMode() {
        return StringUtils.trimToNull(System.getenv(ENV_SESSION_STORE_MODE_KEY));
    }

    /**
     * Gets lock store mode from container.
     *
     * @return the env
     */
    public static String getLockStoreMode() {
        return StringUtils.trimToNull(System.getenv(ENV_LOCK_STORE_MODE_KEY));
    }

}
