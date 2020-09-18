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
package io.seata.config;

/**
 * The type Configuration keys.
 *
 * @author slievrly
 */
public interface ConfigurationKeys {
    /**
     * The constant FILE_ROOT_REGISTRY.
     */
    String FILE_ROOT_REGISTRY = "registry";
    /**
     * The constant FILE_ROOT_CONFIG.
     */
    String FILE_ROOT_CONFIG = "config";
    /**
     * The constant SEATA_FILE_ROOT_CONFIG
     */
    String SEATA_FILE_ROOT_CONFIG = "seata";
    /**
     * The constant FILE_CONFIG_SPLIT_CHAR.
     */
    String FILE_CONFIG_SPLIT_CHAR = ".";
    /**
     * The constant FILE_ROOT_TYPE.
     */
    String FILE_ROOT_TYPE = "type";
}
