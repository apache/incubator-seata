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

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Database-backed order service for Saga benchmark.
 */
public class OrderDbSagaService {

    private final DataSource dataSource;
    private final int rollbackPercentage;
    private final int simulatedDelayMs;
    private final boolean failInjectionEnabled;
    private final ThreadLocal<Random> failureRandom;
    private final boolean timeoutInjectionEnabled;
    private final int timeoutMs;

    public OrderDbSagaService(
            DataSource dataSource,
            int rollbackPercentage,
            int simulatedDelayMs,
            boolean failInjectionEnabled,
            ThreadLocal<Random> failureRandom,
            boolean timeoutInjectionEnabled,
            int timeoutMs) {
        this.dataSource = dataSource;
        this.rollbackPercentage = rollbackPercentage;
        this.simulatedDelayMs = simulatedDelayMs;
        this.failInjectionEnabled = failInjectionEnabled;
        this.failureRandom = failureRandom;
        this.timeoutInjectionEnabled = timeoutInjectionEnabled;
        this.timeoutMs = timeoutMs;
    }

    public Map<String, Object> createOrder(Map<String, Object> params) {
        String orderId = UUID.randomUUID().toString().substring(0, 8);
        String userId = (String) params.get("userId");
        String productId = (String) params.get("productId");
        Integer quantity = (Integer) params.get("quantity");
        BigDecimal amount = toBigDecimal(params.get("amount"));

        simulateDelay();
        if (timeoutInjectionEnabled) {
            simulateTimeout("order creation");
        }
        if (shouldFail()) {
            throw new RuntimeException("Simulated order creation failure");
        }

        try (Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement("INSERT INTO benchmark_order "
                        + "(order_id, user_id, product_id, quantity, amount, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?)")) {
            pstmt.setString(1, orderId);
            pstmt.setString(2, userId);
            pstmt.setString(3, productId);
            pstmt.setInt(4, quantity);
            pstmt.setBigDecimal(5, amount);
            pstmt.setString(6, "CREATED");
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create order", e);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("code", "S");
        result.put("orderId", orderId);
        result.put("userId", userId);
        result.put("productId", productId);
        return result;
    }

    public Map<String, Object> cancelOrder(Map<String, Object> params) {
        String orderId = (String) params.get("orderId");
        String userId = (String) params.get("userId");

        simulateDelay();

        try (Connection conn = dataSource.getConnection();
                PreparedStatement pstmt =
                        conn.prepareStatement("UPDATE benchmark_order SET status = ? WHERE order_id = ?")) {
            pstmt.setString(1, "CANCELLED");
            pstmt.setString(2, orderId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to cancel order", e);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("code", "S");
        result.put("orderId", orderId != null ? orderId : "unknown");
        result.put("userId", userId);
        return result;
    }

    public void destroy() {
        if (failureRandom != null) {
            failureRandom.remove();
        }
    }

    private BigDecimal toBigDecimal(Object amountObj) {
        return amountObj instanceof BigDecimal ? (BigDecimal) amountObj : new BigDecimal(amountObj.toString());
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
        return failInjectionEnabled && nextFailurePercent() < rollbackPercentage;
    }

    private void simulateTimeout(String operation) {
        try {
            Thread.sleep(timeoutMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        throw new RuntimeException("Simulated " + operation + " timeout");
    }

    private int nextFailurePercent() {
        return FailureRandomProvider.nextPercent(failureRandom);
    }
}
