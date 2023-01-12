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

import java.util.Iterator;
import java.util.ServiceLoader;

import com.alibaba.nacos.common.notify.DefaultPublisher;
import com.alibaba.nacos.common.notify.EventPublisher;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
class SeataDiscoveryAndConfigurationRuntimeHints implements RuntimeHintsRegistrar {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeataDiscoveryAndConfigurationRuntimeHints.class);


    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        this.registerHintsForNacos(hints);
        this.registerHintsForZookeeper(hints);
    }

    // Nacos
    private void registerHintsForNacos(RuntimeHints hints) {
        ReflectionHints reflectionHints = hints.reflection();

        // Register the 'nacos-client.jar!/nacos-version.txt' to the resource hints.
        // See com.alibaba.nacos.common.utils.VersionUtils#static
        hints.resources().registerPattern("nacos-version.txt");

        // Register following classes for JSON serialization and deserialization.
        AotUtils.registerTypesForSerialize(reflectionHints,
                "com.alibaba.nacos.api.naming.pojo.ServiceInfo",
                "com.alibaba.nacos.api.naming.pojo.Instance",
                "com.alibaba.nacos.client.naming.core.PushReceiver$PushPacket"
        );

        // Register the implementation class of the interface EventPublisher to the reflection hints.
        // See com.alibaba.nacos.common.notify.NotifyCenter#static
        Class<?> eventPublisherClass = null;
        try {
            final ServiceLoader<EventPublisher> loader = ServiceLoader.load(EventPublisher.class);
            Iterator<EventPublisher> iterator = loader.iterator();
            if (iterator.hasNext()) {
                eventPublisherClass = iterator.next().getClass();
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Load the implementation class of the interface 'EventPublisher' fail. Default implementation 'DefaultPublisher' will be used.", e);
        }
        AotUtils.registerType(reflectionHints,
                eventPublisherClass == null ? DefaultPublisher.class : eventPublisherClass,
                AotUtils.MEMBER_CATEGORIES_FOR_INSTANTIATE
        );
    }


    // Zookeeper
    private void registerHintsForZookeeper(RuntimeHints hints) {
        ReflectionHints reflectionHints = hints.reflection();

        // See io.seata.config.zk.ZookeeperConfiguration#getZkSerializer
        String serializer = ConfigurationFactory.CURRENT_FILE_INSTANCE.getConfig(FILE_CONFIG_KEY_PREFIX + SERIALIZER_KEY);
        if (StringUtils.isNotBlank(serializer)) {
            AotUtils.registerType(reflectionHints, serializer, AotUtils.MEMBER_CATEGORIES_FOR_INSTANTIATE);
        }
    }

}
