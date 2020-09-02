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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static io.seata.common.DefaultValues.SERVER_DEFAULT_PORT;

/**
 * @author wang.liang
 */
public class ContainerHelper {

    private static final String C_GROUP_PATH = "/proc/1/cgroup";
    private static final String DOCKER_PATH = "/docker";
    private static final String KUBEPODS_PATH = "/kubepods";

    private static final String ENV_SYSTEM_KEY = "SEATA_ENV";
    private static final String ENV_SEATA_IP_KEY = "SEATA_IP";
    private static final String ENV_SERVER_NODE_KEY = "SERVER_NODE";
    private static final String ENV_SEATA_PORT_KEY = "SEATA_PORT";
    private static final String ENV_STORE_MODE_KEY = "STORE_MODE";

    /**
     * Judge if application is run in container.
     *
     * @return If application is run in container
     */
    public static boolean isRunningInContainer() {
        Path path = Paths.get(C_GROUP_PATH);
        if (Files.exists(path)) {
            try (Stream<String> stream = Files.lines(path)) {
                return stream.anyMatch(line -> line.contains(DOCKER_PATH) || line.contains(KUBEPODS_PATH));
            } catch (IOException e) {
                System.err.println("Judge if running in container failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return false;
    }

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
        return NumberUtils.toInt(System.getenv(ENV_SEATA_PORT_KEY), SERVER_DEFAULT_PORT);
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
}
