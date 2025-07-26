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
package org.apache.seata.common.pool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Static registry for managing all PoolManager instances
 */
public class PoolManagerRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(PoolManagerRegistry.class);
    private static final Map<String, PoolManager> REGISTRY = new ConcurrentHashMap<>();


    public static void register(String serviceName, PoolManager poolManager) {
        if (serviceName == null || poolManager == null) {
            LOGGER.warn("Cannot register null serviceName or poolManager");
            return;
        }

        PoolManager existing = REGISTRY.put(serviceName, poolManager);
        if (existing != null) {
            LOGGER.warn("PoolManager for service {} has been replaced", serviceName);
        } else {
            LOGGER.info("PoolManager for service {} has been registered", serviceName);
        }
    }


    /**
     * Unregister a PoolManager for a given service
     */
    public static void unregister(String serviceName) {
        if (serviceName == null) {
            return;
        }

        PoolManager removed = REGISTRY.remove(serviceName);
        if (removed != null) {
            LOGGER.info("PoolManager for service {} has been unregistered", serviceName);
        }
    }

    /**
     * Get a PoolManager for a given service
     *
     * @param serviceName the service name
     * @return the pool manager or null if not found
     */
    public static PoolManager get(String serviceName) {
        return serviceName == null ? null : REGISTRY.get(serviceName);
    }

    /**
     * Get all registered service names
     *
     * @return collection of service names
     */
    public static Collection<String> getAllServiceNames() {
        return new ArrayList<>(REGISTRY.keySet());
    }

    /**
     * Get all registered PoolManagers
     *
     * @return collection of pool managers
     */
    public static Collection<PoolManager> getAllPoolManagers() {
        return new ArrayList<>(REGISTRY.values());
    }

    /**
     * Check if a service is registered
     *
     * @param serviceName the service name
     * @return true if registered, false otherwise
     */
    public static boolean isRegistered(String serviceName) {
        return serviceName != null && REGISTRY.containsKey(serviceName);
    }

    /**
     * Get the number of registered services
     *
     * @return the count of registered services
     */
    public static int size() {
        return REGISTRY.size();
    }

    /**
     * Clear all registrations
     */
    public static void clear() {
        REGISTRY.clear();
        LOGGER.info("All PoolManagers have been cleared");
    }
}
