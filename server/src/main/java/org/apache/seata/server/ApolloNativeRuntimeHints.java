/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.server;

import com.ctrip.framework.apollo.spring.config.ConfigPropertySourceFactory;
import com.ctrip.framework.apollo.spring.property.PlaceholderHelper;
import com.ctrip.framework.apollo.spring.property.SpringValueRegistry;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Registers reflection hints for Apollo Guice-managed classes that are not
 * automatically detected by Spring AOT processing because they are instantiated
 * via Guice instead of the Spring container.
 */
public class ApolloNativeRuntimeHints implements RuntimeHintsRegistrar {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApolloNativeRuntimeHints.class);

    private static final String[] APOLLO_GUICE_CLASSES = {
        ConfigPropertySourceFactory.class.getName(),
        PlaceholderHelper.class.getName(),
        SpringValueRegistry.class.getName(),
    };

    @Override
    public void registerHints(@NonNull RuntimeHints hints, ClassLoader classLoader) {
        for (String className : APOLLO_GUICE_CLASSES) {
            try {
                Class<?> clazz = Class.forName(className, false, classLoader);
                hints.reflection()
                        .registerType(
                                clazz,
                                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                                MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                                MemberCategory.INVOKE_PUBLIC_METHODS,
                                MemberCategory.ACCESS_DECLARED_FIELDS);
            } catch (ClassNotFoundException e) {
                // Skip classes not available on the classpath
                LOGGER.error("Apollo Guice class not found on classpath: {}", className, e);
            }
        }
    }
}
