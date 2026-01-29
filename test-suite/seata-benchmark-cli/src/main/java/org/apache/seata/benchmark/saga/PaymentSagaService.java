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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Payment Saga Service for benchmark testing.
 * Simulates payment debit/refund operations.
 */
public class PaymentSagaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentSagaService.class);

    private final int rollbackPercentage;
    private final int simulatedDelayMs;

    public PaymentSagaService(int rollbackPercentage, int simulatedDelayMs) {
        this.rollbackPercentage = rollbackPercentage;
        this.simulatedDelayMs = simulatedDelayMs;
    }

    /**
     * Debit payment (forward action)
     *
     * @param params input parameters containing accountId and amount
     * @return result map with success status
     */
    public Map<String, Object> debitPayment(Map<String, Object> params) {
        String accountId = (String) params.get("accountId");
        Object amountObj = params.get("amount");
        BigDecimal amount =
                amountObj instanceof BigDecimal ? (BigDecimal) amountObj : new BigDecimal(amountObj.toString());

        LOGGER.debug("Debiting payment: accountId={}, amount={}", accountId, amount);

        // Simulate processing time
        simulateDelay();

        // Simulate random failure based on rollback percentage
        if (shouldFail()) {
            LOGGER.debug("Payment debit failed (simulated): accountId={}", accountId);
            throw new RuntimeException("Simulated payment debit failure");
        }

        LOGGER.debug("Payment debited successfully: accountId={}, amount={}", accountId, amount);
        Map<String, Object> result = new HashMap<>();
        result.put("code", "S");
        result.put("accountId", accountId);
        result.put("amount", amount.toString());
        return result;
    }

    /**
     * Refund payment (compensation action)
     *
     * @param params input parameters containing accountId and amount
     * @return result map with success status
     */
    public Map<String, Object> refundPayment(Map<String, Object> params) {
        String accountId = (String) params.get("accountId");
        Object amountObj = params.get("amount");
        BigDecimal amount =
                amountObj instanceof BigDecimal ? (BigDecimal) amountObj : new BigDecimal(amountObj.toString());

        LOGGER.debug("Refunding payment (compensation): accountId={}, amount={}", accountId, amount);

        // Simulate processing time
        simulateDelay();

        LOGGER.debug("Payment refunded successfully: accountId={}, amount={}", accountId, amount);
        Map<String, Object> result = new HashMap<>();
        result.put("code", "S");
        result.put("accountId", accountId);
        result.put("amount", amount.toString());
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
