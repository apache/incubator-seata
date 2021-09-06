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

import io.seata.common.loader.EnhancedServiceLoader;

import java.io.IOException;
import java.util.Properties;

/**
 * The Config Processor.
 *
 * @author zhixing
 */
public class ConfigProcessor {

    public static Properties loadConfig(String config,String dataType) throws IOException {
        return EnhancedServiceLoader.load(Processor.class, dataType).processor(config);
    }

}
