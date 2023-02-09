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
package io.seata.config.processor;

import io.seata.config.processor.impl.ConfigFilesConfigurationProcessor;

/**
 * The interface ConfigProcessorOrdered.
 * <p>
 * Process sequence: systems > defaults > others.
 *
 * @author wang.liang
 */
public interface ConfigProcessorOrdered {

    //region 1. System property and env configuration processors. Start processing first.

    /**
     * 1.1. System property processor, the {@link ConfigurationProcessor} with the highest priority.
     *
     * @see io.seata.config.processor.impl.SystemPropertyConfigurationProcessor
     */
    int SYSTEM_PROPERTY_PROCESSOR_ORDER = Integer.MIN_VALUE + 100;

    /**
     * 1.2. System env processor.
     *
     * @see io.seata.config.processor.impl.SystemEnvConfigurationProcessor
     */
    int SYSTEM_ENV_PROCESSOR_ORDER = Integer.MIN_VALUE + 200;

    //endregion


    //-------------------------------------------------------------------------


    //region 2. Default value configuration processors. Priority process than the following.

    /**
     * 2.1. Default value from property object processor.
     * <p>
     * See the processor in the module 'seata-spring-autoconfigure-core'.
     */
    int PROPERTY_OBJECT_DEFAULT_VALUE_PROCESSOR_ORDER = -200;

    /**
     * 2.2. Default value from files processor.
     *
     * @see io.seata.config.processor.impl.FilesDefaultValueConfigurationProcessor
     */
    int FILES_DEFAULT_VALUE_PROCESSOR_ORDER = -100;

    //endregion


    //-------------------------------------------------------------------------


    //region 3. Other configuration processors. Priority lower than above.

    /**
     * 3.1. Spring environment processor.
     */
    int SPRING_ENVIRONMENT_PROCESSOR_ORDER = 100;

    /**
     * 3.2. Config file processor.
     *
     * @see ConfigFilesConfigurationProcessor
     */
    int CONFIG_FILES_PROCESSOR_ORDER = 200;

    /**
     * 3.3. Config center processor.
     * <p>
     * It will be processed finally, because it needs other config sources to provide config related to the config-center.<br/>
     * For example: the address of the config-center
     *
     * @see io.seata.config.processor.impl.ConfigCenterConfigurationProcessor
     */
    int CONFIG_CENTER_PROCESSOR_ORDER = 300;

    //endregion

}
