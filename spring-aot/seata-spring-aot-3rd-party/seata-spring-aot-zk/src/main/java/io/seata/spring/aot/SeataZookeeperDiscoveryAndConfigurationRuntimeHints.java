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
import io.seata.config.zk.ZookeeperConfiguration;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.lang.Nullable;

/**
 * The seata zookeeper discovery and configuration runtime hints registrar
 *
 * @author wang.liang
 */
class SeataZookeeperDiscoveryAndConfigurationRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        ReflectionHints reflectionHints = hints.reflection();

        // See io.seata.config.zk.ZookeeperConfiguration#getZkSerializer
        String serializer = ConfigurationFactory.CURRENT_FILE_INSTANCE.getConfig(ZookeeperConfiguration.FILE_CONFIG_KEY_PREFIX + ZookeeperConfiguration.SERIALIZER_KEY);
        if (StringUtils.isNotBlank(serializer)) {
            AotUtils.registerType(reflectionHints, serializer, AotUtils.MEMBER_CATEGORIES_FOR_INSTANTIATE);
        }
    }

}
