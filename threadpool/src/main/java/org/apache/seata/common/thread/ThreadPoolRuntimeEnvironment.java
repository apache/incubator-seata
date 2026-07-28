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
package org.apache.seata.common.thread;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.DefaultValues;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Runtime helper used to resolve the thread pool mode.
 */
final class ThreadPoolRuntimeEnvironment {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadPoolRuntimeEnvironment.class);

    private static final Supplier<String> DEFAULT_THREAD_POOL_TYPE_SUPPLIER =
            ThreadPoolRuntimeEnvironment::loadConfiguredThreadPoolType;
    private static final IntSupplier DEFAULT_JDK_FEATURE_SUPPLIER = ThreadPoolRuntimeEnvironment::javaFeatureVersion;

    private static volatile Supplier<String> threadPoolTypeSupplier = DEFAULT_THREAD_POOL_TYPE_SUPPLIER;
    private static volatile IntSupplier jdkFeatureSupplier = DEFAULT_JDK_FEATURE_SUPPLIER;

    private ThreadPoolRuntimeEnvironment() {}

    static ThreadPoolType resolveThreadPoolType() {
        ThreadPoolType configuredType = ThreadPoolType.from(threadPoolTypeSupplier.get());
        if (configuredType == ThreadPoolType.PLATFORM) {
            return ThreadPoolType.PLATFORM;
        }
        int jdkFeature = jdkFeatureSupplier.getAsInt();
        if (configuredType == ThreadPoolType.VIRTUAL) {
            if (jdkFeature < 21) {
                LOGGER.warn(
                        "transport.threadpool=virtual is configured but the current JDK feature version is {} (<21). "
                                + "Virtual threads are not supported; falling back to platform threads.",
                        jdkFeature);
            }
            return jdkFeature >= 21 ? ThreadPoolType.VIRTUAL : ThreadPoolType.PLATFORM;
        }
        return jdkFeature >= 25 ? ThreadPoolType.VIRTUAL : ThreadPoolType.PLATFORM;
    }

    static void setThreadPoolTypeSupplier(Supplier<String> supplier) {
        threadPoolTypeSupplier = supplier == null ? DEFAULT_THREAD_POOL_TYPE_SUPPLIER : supplier;
    }

    static void setJdkFeatureSupplier(IntSupplier supplier) {
        jdkFeatureSupplier = supplier == null ? DEFAULT_JDK_FEATURE_SUPPLIER : supplier;
    }

    static void reset() {
        threadPoolTypeSupplier = DEFAULT_THREAD_POOL_TYPE_SUPPLIER;
        jdkFeatureSupplier = DEFAULT_JDK_FEATURE_SUPPLIER;
    }

    private static String loadConfiguredThreadPoolType() {
        Configuration configuration = ConfigurationFactory.getInstance();
        return configuration.getConfig(
                ConfigurationKeys.TRANSPORT_THREADPOOL, DefaultValues.DEFAULT_TRANSPORT_THREADPOOL);
    }

    static int javaFeatureVersion() {
        String specVersion = System.getProperty("java.specification.version", "1.8");
        if (specVersion != null) {
            specVersion = specVersion.trim();
        }
        if (specVersion.startsWith("1.")) {
            // Java 8 and earlier: "1.8", "1.7", etc.
            // Be tolerant of values like "1.8 ", "1.8.0", or "1.x".
            String legacyPart = specVersion.substring(2);
            int dotIndex = legacyPart.indexOf('.');
            if (dotIndex >= 0) {
                legacyPart = legacyPart.substring(0, dotIndex);
            }
            try {
                return Integer.parseInt(legacyPart);
            } catch (NumberFormatException e) {
                return 8;
            }
        }
        // Java 9+: "9", "11", "17", "21", "25", etc.
        try {
            return Integer.parseInt(specVersion);
        } catch (NumberFormatException e) {
            return 8;
        }
    }
}
