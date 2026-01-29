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
package org.apache.seata.benchmark.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Inventory Saga Service for benchmark testing.
 * Simulates inventory reservation/release operations.
 */
public class InventorySagaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventorySagaService.class);

    private final int rollbackPercentage;
    private final int simulatedDelayMs;

    public InventorySagaService(int rollbackPercentage, int simulatedDelayMs) {
        this.rollbackPercentage = rollbackPercentage;
        this.simulatedDelayMs = simulatedDelayMs;
    }

    /**
     * Reserve inventory (forward action)
     *
     * @param params input parameters containing productId and quantity
     * @return result map with success status
     */
    public Map<String, Object> reserveInventory(Map<String, Object> params) {
        String productId = (String) params.get("productId");
        Integer quantity = (Integer) params.get("quantity");

        LOGGER.debug("Reserving inventory: productId={}, quantity={}", productId, quantity);

        // Simulate processing time
        simulateDelay();

        // Simulate random failure based on rollback percentage
        if (shouldFail()) {
            LOGGER.debug("Inventory reservation failed (simulated): productId={}", productId);
            throw new RuntimeException("Simulated inventory reservation failure");
        }

        LOGGER.debug("Inventory reserved successfully: productId={}, quantity={}", productId, quantity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", "S");
        result.put("productId", productId);
        result.put("quantity", quantity);
        return result;
    }

    /**
     * Release inventory (compensation action)
     *
     * @param params input parameters containing productId and quantity
     * @return result map with success status
     */
    public Map<String, Object> releaseInventory(Map<String, Object> params) {
        String productId = (String) params.get("productId");
        Integer quantity = (Integer) params.get("quantity");

        LOGGER.debug("Releasing inventory (compensation): productId={}, quantity={}", productId, quantity);

        // Simulate processing time
        simulateDelay();

        LOGGER.debug("Inventory released successfully: productId={}, quantity={}", productId, quantity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", "S");
        result.put("productId", productId);
        result.put("quantity", quantity);
        return result;
    }

    private void simulateDelay() {
        if (simulatedDelayMs > 0) {
            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(simulatedDelayMs));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean shouldFail() {
        return ThreadLocalRandom.current().nextInt(100) < rollbackPercentage;
    }
}
