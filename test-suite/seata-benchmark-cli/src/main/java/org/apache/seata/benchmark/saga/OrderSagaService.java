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
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Order Saga Service for benchmark testing.
 * Simulates order creation/cancellation operations.
 */
public class OrderSagaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderSagaService.class);

    private final int rollbackPercentage;
    private final int simulatedDelayMs;

    public OrderSagaService(int rollbackPercentage, int simulatedDelayMs) {
        this.rollbackPercentage = rollbackPercentage;
        this.simulatedDelayMs = simulatedDelayMs;
    }

    /**
     * Create order (forward action)
     *
     * @param params input parameters containing userId, productId, quantity, amount
     * @return result map with success status and orderId
     */
    public Map<String, Object> createOrder(Map<String, Object> params) {
        String userId = (String) params.get("userId");
        String productId = (String) params.get("productId");
        Integer quantity = (Integer) params.get("quantity");

        String orderId = UUID.randomUUID().toString().substring(0, 8);

        LOGGER.debug("Creating order: userId={}, productId={}, quantity={}", userId, productId, quantity);

        // Simulate processing time
        simulateDelay();

        // Simulate random failure based on rollback percentage
        if (shouldFail()) {
            LOGGER.debug("Order creation failed (simulated): userId={}", userId);
            throw new RuntimeException("Simulated order creation failure");
        }

        LOGGER.debug("Order created successfully: orderId={}", orderId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", "S");
        result.put("orderId", orderId);
        result.put("userId", userId);
        result.put("productId", productId);
        return result;
    }

    /**
     * Cancel order (compensation action)
     *
     * @param params input parameters containing orderId
     * @return result map with success status
     */
    public Map<String, Object> cancelOrder(Map<String, Object> params) {
        String orderId = (String) params.get("orderId");
        String userId = (String) params.get("userId");

        LOGGER.debug("Cancelling order (compensation): orderId={}, userId={}", orderId, userId);

        // Simulate processing time
        simulateDelay();

        LOGGER.debug("Order cancelled successfully: orderId={}", orderId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", "S");
        result.put("orderId", orderId != null ? orderId : "unknown");
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
