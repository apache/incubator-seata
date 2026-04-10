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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Database-backed payment service for Saga benchmark.
 * @author zihenzzz
 */
public class PaymentDbSagaService {

    private final DataSource dataSource;
    private final int rollbackPercentage;
    private final int simulatedDelayMs;
    private final boolean failInjectionEnabled;
    private final Random failureRandom;
    private final boolean timeoutInjectionEnabled;
    private final int timeoutMs;

    public PaymentDbSagaService(
            DataSource dataSource,
            int rollbackPercentage,
            int simulatedDelayMs,
            boolean failInjectionEnabled,
            Random failureRandom,
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
            conn.setAutoCommit(false);
            BigDecimal balance = queryBalance(conn, accountId);
            if (balance.compareTo(amount) < 0) {
                throw new RuntimeException("Insufficient balance for account " + accountId);
            }
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "UPDATE benchmark_account SET balance = balance - ? WHERE account_id = ?")) {
                pstmt.setBigDecimal(1, amount);
                pstmt.setString(2, accountId);
                pstmt.executeUpdate();
            }
            conn.commit();
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

    private BigDecimal queryBalance(Connection conn, String accountId) throws SQLException {
        try (PreparedStatement pstmt =
                        conn.prepareStatement("SELECT balance FROM benchmark_account WHERE account_id = ?");
                ResultSet rs = executeQuery(pstmt, accountId)) {
            if (!rs.next()) {
                throw new RuntimeException("Account not found: " + accountId);
            }
            return rs.getBigDecimal(1);
        }
    }

    private ResultSet executeQuery(PreparedStatement pstmt, String accountId) throws SQLException {
        pstmt.setString(1, accountId);
        return pstmt.executeQuery();
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
        if (failureRandom != null) {
            synchronized (failureRandom) {
                return failureRandom.nextInt(100);
            }
        }
        return ThreadLocalRandom.current().nextInt(100);
    }
}
