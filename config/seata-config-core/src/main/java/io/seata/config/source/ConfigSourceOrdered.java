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

import io.seata.config.processor.impl.ConfigFilesConfigurationProcessor;

/**
 * The interface ConfigSourceOrdered.
 * <p>
 * Priority: systems > others > defaults.
 *
 * @author wang.liang
 * @see ConfigSource
 */
public interface ConfigSourceOrdered {

    //region 1. System property and env sources. Priority higher than the following.

    /**
     * 1.1. System property, the {@link ConfigSource} with the highest priority.
     *
     * @see io.seata.config.source.impl.SystemPropertyConfigSource
     * @see io.seata.config.processor.impl.SystemPropertyConfigurationProcessor
     */
    int SYSTEM_PROPERTY_SOURCE_ORDER = Integer.MIN_VALUE + 100;

    /**
     * 1.2. System env
     *
     * @see io.seata.config.source.impl.SystemEnvConfigSource
     * @see io.seata.config.processor.impl.SystemEnvConfigurationProcessor
     */
    int SYSTEM_ENV_SOURCE_ORDER = Integer.MIN_VALUE + 200;

    //endregion


    //-------------------------------------------------------------------------


    //region 2. Others. Priority higher than the following.

    /**
     * 2.1. Config center, examples: nacos, apollo, consul, etcd3, spring-cloud, zookeeper...
     *
     * @see io.seata.config.processor.impl.ConfigCenterConfigurationProcessor
     */
    int CONFIG_CENTER_SOURCE_ORDER = 100;

    /**
     * 2.2. Spring environment
     */
    int SPRING_ENVIRONMENT_SOURCE_ORDER = 200;

    /**
     * 2.3. Env config files, examples: registry-dev.conf, file-dev.conf
     *
     * @see io.seata.config.source.impl.FileConfigSource
     * @see ConfigFilesConfigurationProcessor
     */
    int ENV_CONFIG_FILES_SOURCE_ORDER = 300;

    /**
     * 2.4. Common config files, examples: registry.conf, file.conf
     *
     * @see io.seata.config.source.impl.FileConfigSource
     * @see ConfigFilesConfigurationProcessor
     */
    int COMMON_CONFIG_FILES_SOURCE_ORDER = 400;

    //endregion


    //-------------------------------------------------------------------------


    //region 3. Default value sources. Priority lower than above.

    /**
     * 3.1. Default value from property object.
     */
    int PROPERTY_OBJECT_DEFAULT_VALUE_CONFIG_SOURCE_ORDER = Integer.MAX_VALUE - 200;

    /**
     * 3.2. Default value from default-config-files, examples: seata-default-config-common.conf, seata-default-config-client.conf
     *
     * @see io.seata.config.source.impl.FileConfigSource
     * @see io.seata.config.processor.impl.FilesDefaultValueConfigurationProcessor
     */
    int FILES_DEFAULT_VALUE_CONFIG_SOURCE_ORDER = Integer.MAX_VALUE - 100;

    //endregion
}
