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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Database-backed inventory service for Saga benchmark.
 */
public class InventoryDbSagaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryDbSagaService.class);

    private final DataSource dataSource;
    private final int rollbackPercentage;
    private final int simulatedDelayMs;
    private final boolean failInjectionEnabled;
    private final ThreadLocal<Random> failureRandom;
    private final boolean timeoutInjectionEnabled;
    private final int timeoutMs;

    public InventoryDbSagaService(
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

    public Map<String, Object> reserveInventory(Map<String, Object> params) {
        String productId = (String) params.get("productId");
        Integer quantity = (Integer) params.get("quantity");

        simulateDelay();
        if (timeoutInjectionEnabled) {
            simulateTimeout("inventory reservation");
        }
        if (shouldFail()) {
            throw new RuntimeException("Simulated inventory reservation failure");
        }

        try (Connection conn = dataSource.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement pstmt = conn.prepareStatement("UPDATE benchmark_inventory "
                        + "SET available_qty = available_qty - ?, reserved_qty = reserved_qty + ? "
                        + "WHERE product_id = ? AND available_qty >= ?")) {
                    pstmt.setInt(1, quantity);
                    pstmt.setInt(2, quantity);
                    pstmt.setString(3, productId);
                    pstmt.setInt(4, quantity);
                    if (pstmt.executeUpdate() == 0) {
                        throw new RuntimeException("Insufficient inventory for product " + productId);
                    }
                }
                conn.commit();
            } catch (Exception e) {
                rollbackQuietly(conn, e);
                throw e;
            } finally {
                restoreAutoCommit(conn, originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to reserve inventory", e);
        }

        LOGGER.debug("Reserved inventory in DB: productId={}, quantity={}", productId, quantity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", "S");
        result.put("productId", productId);
        result.put("quantity", quantity);
        return result;
    }

    public Map<String, Object> releaseInventory(Map<String, Object> params) {
        String productId = (String) params.get("productId");
        Integer quantity = (Integer) params.get("quantity");

        simulateDelay();

        try (Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement("UPDATE benchmark_inventory "
                        + "SET available_qty = available_qty + ?, reserved_qty = GREATEST(reserved_qty - ?, 0) "
                        + "WHERE product_id = ?")) {
            pstmt.setInt(1, quantity);
            pstmt.setInt(2, quantity);
            pstmt.setString(3, productId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to release inventory", e);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("code", "S");
        result.put("productId", productId);
        result.put("quantity", quantity);
        return result;
    }

    public void destroy() {
        if (failureRandom != null) {
            failureRandom.remove();
        }
    }

    private void rollbackQuietly(Connection conn, Exception original) {
        try {
            conn.rollback();
        } catch (SQLException rollbackException) {
            original.addSuppressed(rollbackException);
        }
    }

    private void restoreAutoCommit(Connection conn, boolean originalAutoCommit) {
        try {
            conn.setAutoCommit(originalAutoCommit);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to restore auto-commit for inventory connection", e);
        }
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
