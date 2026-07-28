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
import java.util.concurrent.ThreadLocalRandom;

/**
 * Database-backed payment service for Saga benchmark.
 */
public class PaymentDbSagaService {

    private final DataSource dataSource;
    private final int rollbackPercentage;
    private final int simulatedDelayMs;
    private final boolean failInjectionEnabled;
    private final ThreadLocal<Random> failureRandom;
    private final boolean timeoutInjectionEnabled;
    private final int timeoutMs;

    public PaymentDbSagaService(
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

    public Map<String, Object> debitPayment(Map<String, Object> params) {
        String accountId = (String) params.get("accountId");
        BigDecimal amount = toBigDecimal(params.get("amount"));

        simulateDelay();
        if (timeoutInjectionEnabled) {
            simulateTimeout("payment debit");
        }
        if (shouldFail()) {
            throw new RuntimeException("Simulated payment debit failure");
        }

        try (Connection conn = dataSource.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement pstmt =
                        conn.prepareStatement("UPDATE benchmark_account SET balance = balance - ? "
                                + "WHERE account_id = ? AND balance >= ?")) {
                    pstmt.setBigDecimal(1, amount);
                    pstmt.setString(2, accountId);
                    pstmt.setBigDecimal(3, amount);
                    if (pstmt.executeUpdate() == 0) {
                        throw new RuntimeException("Insufficient balance for account " + accountId);
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
            throw new RuntimeException("Failed to debit payment", e);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("code", "S");
        result.put("accountId", accountId);
        result.put("amount", amount.toPlainString());
        return result;
    }

    public Map<String, Object> refundPayment(Map<String, Object> params) {
        String accountId = (String) params.get("accountId");
        BigDecimal amount = toBigDecimal(params.get("amount"));

        simulateDelay();

        try (Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(
                        "UPDATE benchmark_account SET balance = balance + ? WHERE account_id = ?")) {
            pstmt.setBigDecimal(1, amount);
            pstmt.setString(2, accountId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to refund payment", e);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("code", "S");
        result.put("accountId", accountId);
        result.put("amount", amount.toPlainString());
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
            throw new RuntimeException("Failed to restore auto-commit for payment connection", e);
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
