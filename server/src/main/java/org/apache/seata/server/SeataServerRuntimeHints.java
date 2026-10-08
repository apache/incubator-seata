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

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Registers GraalVM native-image reflection hints for all Seata SPI
 * implementation classes discovered via the EnhancedServiceLoader mechanism.
 *
 * <p>EnhancedServiceLoader reads class names from {@code META-INF/services/}
 * and {@code META-INF/seata/} descriptor files, then instantiates them
 * reflectively using {@code Class.getDeclaredConstructor()}. In native images,
 * those constructors must be registered via reflection hints.
 *
 * <p>This registrar dynamically scans all SPI descriptor files on the build
 * classpath during Spring AOT processing, so new SPI implementations are
 * automatically covered without manual curation.
 *
 * <p>The source {@code reachability-metadata.json} entries for these same
 * classes are overwritten during the native build by the GraalVM
 * {@code add-reachability-metadata} goal; programmatic hints registered here
 * are immune to that overwrite because they are compiled into AOT-generated
 * source code rather than written into the reachability metadata JSON file.
 */
public class SeataServerRuntimeHints implements RuntimeHintsRegistrar {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeataServerRuntimeHints.class);

    /**
     * Pattern to discover all SPI descriptor files across the classpath.
     * EnhancedServiceLoader reads from both locations.
     *
     * @see org.apache.seata.common.loader.EnhancedServiceLoader.InnerEnhancedServiceLoader#SERVICES_DIRECTORY
     * @see org.apache.seata.common.loader.EnhancedServiceLoader.InnerEnhancedServiceLoader#SEATA_DIRECTORY
     */
    private static final String SERVICES_PATTERN = "classpath*:META-INF/services/*";

    private static final String SEATA_PATTERN = "classpath*:META-INF/seata/*";

    @Override
    public void registerHints(@NonNull RuntimeHints hints, ClassLoader classLoader) {
        int count = 0;
        count += registerFromPattern(hints, classLoader, SERVICES_PATTERN);
        count += registerFromPattern(hints, classLoader, SEATA_PATTERN);
        LOGGER.info("Registered native reflection hints for {} Seata SPI implementation classes", count);
    }

    private int registerFromPattern(RuntimeHints hints, ClassLoader classLoader, String locationPattern) {
        int count = 0;
        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(classLoader);
            Resource[] resources = resolver.getResources(locationPattern);
            for (Resource resource : resources) {
                count += processServiceFile(hints, classLoader, resource);
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to scan SPI resources for pattern: {}", locationPattern, e);
        }
        return count;
    }

    private int processServiceFile(RuntimeHints hints, ClassLoader classLoader, Resource resource) {
        int count = 0;
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Strip comments and trim per ServiceLoader spec (JAR 3.0 §3.1)
                int commentIndex = line.indexOf('#');
                if (commentIndex >= 0) {
                    line = line.substring(0, commentIndex);
                }
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (registerClassHints(hints, classLoader, line)) {
                    count++;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to process SPI file: {}", resource.getFilename(), e);
        }
        return count;
    }

    private boolean registerClassHints(RuntimeHints hints, ClassLoader classLoader, String className) {
        try {
            Class<?> clazz = Class.forName(className, false, classLoader);
            hints.reflection()
                    .registerType(
                            clazz, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS);
            LOGGER.debug("Registered native reflection hints for SPI class: {}", className);
            return true;
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            LOGGER.debug("SPI class not available on classpath, skipping: {}", className);
            return false;
        }
    }
}
