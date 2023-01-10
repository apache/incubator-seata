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
package io.seata.spring.aot;

import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationFactory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.lang.Nullable;

import static io.seata.config.zk.ZookeeperConfiguration.FILE_CONFIG_KEY_PREFIX;
import static io.seata.config.zk.ZookeeperConfiguration.SERIALIZER_KEY;

/**
 * The seata configuration runtime hints registrar
 *
 * @author wang.liang
 */
class SeataConfigurationRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        this.registerHintsForZookeeperConfiguration(hints);
    }


    private void registerHintsForZookeeperConfiguration(RuntimeHints hints) {
        ReflectionHints reflectionHints = hints.reflection();

        String serializer = ConfigurationFactory.CURRENT_FILE_INSTANCE.getConfig(FILE_CONFIG_KEY_PREFIX + SERIALIZER_KEY);
        if (StringUtils.isNotBlank(serializer)) {
            AotUtils.registerType(reflectionHints, serializer, AotUtils.MEMBER_CATEGORIES_FOR_INSTANTIATE);
        }
    }

}
