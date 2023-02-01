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

/**
 * The interface ConfigurationSourceOrdered.
 *
 * @author wang.liang
 */
public interface ConfigSourceOrdered {

    //region order for ConfigurationSource

    int SYSTEM_PROPERTY_SOURCE_ORDER = Integer.MIN_VALUE + 100;
    int SYSTEM_ENV_SOURCE_ORDER = Integer.MIN_VALUE + 200;

    int CONFIG_CENTER_SOURCE_ORDER = 100;

    int SPRING_ENVIRONMENT_SOURCE_ORDER = 200;
    int CONFIG_FILE_SOURCE_ORDER = 300;

    //endregion


    //region order for DefaultConfigSource

    int PROPERTY_OBJECT_DEFAULT_CONFIG_SOURCE_ORDER = Integer.MIN_VALUE + 100;
    int FILE_DEFAULT_CONFIG_SOURCE_ORDER = Integer.MIN_VALUE + 200;

    //endregion
}
