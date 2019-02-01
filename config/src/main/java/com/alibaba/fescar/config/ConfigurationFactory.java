/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.config;

/**
 * The type Configuration factory.
 *
 * @Author: jimin.jm @alibaba-inc.com
 * @Project: fescar -all
 * @DateTime: 2018 /12/24 10:54
 * @FileName: ConfigurationFactory
 * @Description:
 */
public final class ConfigurationFactory {
    private static final Configuration FILE_INSTANCE = new FileConfiguration();

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static Configuration getInstance() {
        return FILE_INSTANCE;
    }
}
