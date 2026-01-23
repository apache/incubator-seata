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
package org.apache.seata.benchmark.executor;

import org.apache.seata.benchmark.config.BenchmarkConfig;
import org.apache.seata.benchmark.model.TransactionRecord;
import org.apache.seata.benchmark.saga.BenchmarkServiceInvoker;
import org.apache.seata.benchmark.saga.InventorySagaService;
import org.apache.seata.benchmark.saga.OrderSagaService;
import org.apache.seata.benchmark.saga.PaymentSagaService;
import org.apache.seata.benchmark.saga.SimpleSpelExpressionFactory;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.saga.engine.StateMachineEngine;
import org.apache.seata.saga.engine.config.AbstractStateMachineConfig;
import org.apache.seata.saga.engine.expression.ExpressionFactoryManager;
import org.apache.seata.saga.engine.impl.ProcessCtrlStateMachineEngine;
import org.apache.seata.saga.statelang.domain.DomainConstants;
import org.apache.seata.saga.statelang.domain.ExecutionStatus;
import org.apache.seata.saga.statelang.domain.StateMachineInstance;
import org.apache.seata.tm.api.GlobalTransaction;
import org.apache.seata.tm.api.GlobalTransactionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.seata.benchmark.constant.BenchmarkConstants.STATUS_COMMITTED;
import static org.apache.seata.benchmark.constant.BenchmarkConstants.STATUS_COMPENSATED;
import static org.apache.seata.benchmark.constant.BenchmarkConstants.STATUS_COMPENSATION_FAILED;
import static org.apache.seata.benchmark.constant.BenchmarkConstants.STATUS_FAILED;
import static org.apache.seata.benchmark.constant.BenchmarkConstants.STATUS_UNKNOWN;

/**
 * Saga mode transaction executor supporting both mock and real modes
 * - branches == 0: Mock mode (simplified Saga simulation without state machine)
 * - branches > 0: Real mode (state machine engine with compensation support)
 */
public class SagaModeExecutor implements TransactionExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SagaModeExecutor.class);
    private static final AtomicLong BUSINESS_KEY_COUNTER = new AtomicLong(0);

    private static final String SIMPLE_SAGA_NAME = "benchmarkSimpleSaga";
    private static final String ORDER_SAGA_NAME = "benchmarkOrderSaga";

    private final BenchmarkConfig config;
    private StateMachineEngine stateMachineEngine;
    private BenchmarkStateMachineConfig stateMachineConfig;

    public SagaModeExecutor(BenchmarkConfig config) {
        this.config = config;
    }

    private boolean isRealMode() {
        return config.getBranches() > 0;
    }

    @Override
    public void init() {
        if (isRealMode()) {
            initRealMode();
        } else {
            LOGGER.info("Saga mode executor initialized (simplified mock mode)");
            LOGGER.info("Note: Full Saga annotation support requires Spring framework integration");
        }
    }

    private void initRealMode() {
        LOGGER.info("Initializing Real Saga mode executor with state machine engine");

        try {
            // Create and configure state machine config
            stateMachineConfig = new BenchmarkStateMachineConfig();
            stateMachineConfig.setRollbackPercentage(config.getRollbackPercentage());
            stateMachineConfig.init();

            // Create state machine engine
            ProcessCtrlStateMachineEngine engine = new ProcessCtrlStateMachineEngine();
            engine.setStateMachineConfig(stateMachineConfig);
            this.stateMachineEngine = engine;

            LOGGER.info("Real Saga mode executor initialized");
            LOGGER.info("Available state machines: {}, {}", SIMPLE_SAGA_NAME, ORDER_SAGA_NAME);

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Saga state machine engine", e);
        }
    }

    @Override
    public TransactionRecord execute() {
        if (isRealMode()) {
            return executeRealMode();
        } else {
            return executeMockMode();
        }
    }

    private TransactionRecord executeMockMode() {
        GlobalTransaction tx = GlobalTransactionContext.getCurrentOrCreate();
        long startTime = System.currentTimeMillis();
        String xid = null;
        String status = STATUS_UNKNOWN;
        int branchCount = config.getBranches();
        boolean success = false;

        try {
            tx.begin(30000, "benchmark-saga-tx");
            xid = tx.getXid();

            // Simulate Saga actions (forward phase)
            for (int i = 0; i < branchCount; i++) {
                simulateSagaAction(i);
            }

            if (shouldRollback()) {
                // Simulate compensation (backward phase)
                for (int i = branchCount - 1; i >= 0; i--) {
                    simulateCompensation(i);
                }
                tx.rollback();
                status = STATUS_COMPENSATED;
            } else {
                tx.commit();
                status = STATUS_COMMITTED;
                success = true;
            }

        } catch (TransactionException e) {
            LOGGER.debug("Transaction failed: {}", e.getMessage());
            status = STATUS_FAILED;
            try {
                if (tx.getStatus() != GlobalStatus.Rollbacked && tx.getStatus() != GlobalStatus.RollbackFailed) {
                    tx.rollback();
                }
            } catch (TransactionException rollbackEx) {
                LOGGER.debug("Rollback failed: {}", rollbackEx.getMessage());
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        return new TransactionRecord(xid, status, duration, branchCount, success);
    }

    private TransactionRecord executeRealMode() {
        long startTime = System.currentTimeMillis();
        String businessKey = "benchmark-" + BUSINESS_KEY_COUNTER.incrementAndGet();
        String status = STATUS_UNKNOWN;
        int branchCount = config.getBranches();
        boolean success = false;

        try {
            // Choose state machine based on branch count
            String stateMachineName = branchCount >= 3 ? ORDER_SAGA_NAME : SIMPLE_SAGA_NAME;

            // Prepare start parameters
            Map<String, Object> startParams = createStartParams();

            // Execute state machine
            StateMachineInstance instance = stateMachineEngine.startWithBusinessKey(
                    stateMachineName, stateMachineConfig.getDefaultTenantId(), businessKey, startParams);

            // Check execution result
            ExecutionStatus executionStatus = instance.getStatus();
            ExecutionStatus compensationStatus = instance.getCompensationStatus();

            if (ExecutionStatus.SU.equals(executionStatus)) {
                status = STATUS_COMMITTED;
                success = true;
            } else if (ExecutionStatus.FA.equals(executionStatus)) {
                if (compensationStatus != null) {
                    if (ExecutionStatus.SU.equals(compensationStatus)) {
                        status = STATUS_COMPENSATED;
                    } else {
                        status = STATUS_COMPENSATION_FAILED;
                    }
                } else {
                    status = STATUS_FAILED;
                }
            } else if (ExecutionStatus.UN.equals(executionStatus)) {
                status = STATUS_UNKNOWN;
            } else {
                status = executionStatus != null ? executionStatus.name() : STATUS_UNKNOWN;
            }

        } catch (Exception e) {
            LOGGER.debug("Saga execution failed: {}", e.getMessage());
            status = STATUS_FAILED;
        }

        long duration = System.currentTimeMillis() - startTime;
        return new TransactionRecord(businessKey, status, duration, branchCount, success);
    }

    private Map<String, Object> createStartParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", "user-" + ThreadLocalRandom.current().nextInt(1000));
        params.put("productId", "product-" + ThreadLocalRandom.current().nextInt(100));
        params.put("quantity", ThreadLocalRandom.current().nextInt(10) + 1);
        params.put("amount", new BigDecimal(ThreadLocalRandom.current().nextInt(1000) + 100));
        params.put("accountId", "account-" + ThreadLocalRandom.current().nextInt(1000));
        return params;
    }

    private void simulateSagaAction(int branchId) {
        // Simulated Saga forward action
        // In real implementation, this would be a @CompensationBusinessAction annotated method
        LOGGER.trace("Executing Saga action for branch {}", branchId);
    }

    private void simulateCompensation(int branchId) {
        // Simulated Saga compensation action
        // In real implementation, this would be the compensationMethod
        LOGGER.trace("Executing compensation for branch {}", branchId);
    }

    private boolean shouldRollback() {
        return ThreadLocalRandom.current().nextInt(100) < config.getRollbackPercentage();
    }

    @Override
    public void destroy() {
        if (isRealMode()) {
            destroyRealMode();
        }
        LOGGER.info("Saga mode executor destroyed");
    }

    private void destroyRealMode() {
        LOGGER.info("Destroying Real Saga mode resources");
        // StateMachineEngine doesn't have a close method
        stateMachineEngine = null;
        stateMachineConfig = null;
    }

    /**
     * Custom StateMachineConfig for benchmark testing.
     */
    private static class BenchmarkStateMachineConfig extends AbstractStateMachineConfig {

        private int rollbackPercentage = 0;

        public void setRollbackPercentage(int rollbackPercentage) {
            this.rollbackPercentage = rollbackPercentage;
        }

        @Override
        public void init() throws Exception {
            // Load state machine definitions from classpath
            try (InputStream simpleSagaStream = getClass()
                            .getClassLoader()
                            .getResourceAsStream("seata/saga/statelang/benchmark_simple_saga.json");
                    InputStream orderSagaStream = getClass()
                            .getClassLoader()
                            .getResourceAsStream("seata/saga/statelang/benchmark_order_saga.json")) {

                if (simpleSagaStream == null || orderSagaStream == null) {
                    throw new RuntimeException("Failed to load Saga state machine definitions from classpath");
                }

                // Read streams to byte arrays before closing, as super.init() may process them asynchronously
                byte[] simpleBytes = readAllBytes(simpleSagaStream);
                byte[] orderBytes = readAllBytes(orderSagaStream);

                setStateMachineDefInputStreamArray(
                        new InputStream[] {new ByteArrayInputStream(simpleBytes), new ByteArrayInputStream(orderBytes)
                        });

                // Initialize parent config
                super.init();

                // Register SpEL expression factory for parameter evaluation
                ExpressionFactoryManager expressionFactoryManager = getExpressionFactoryManager();
                SimpleSpelExpressionFactory spelExpressionFactory = new SimpleSpelExpressionFactory();
                // Register for default type (when expression doesn't start with $)
                expressionFactoryManager.putExpressionFactory(
                        ExpressionFactoryManager.DEFAULT_EXPRESSION_TYPE, spelExpressionFactory);
                // Register for empty type (when expression is like $.xxx where type is empty string)
                expressionFactoryManager.putExpressionFactory("", spelExpressionFactory);

                // Register benchmark services with the service invoker manager
                BenchmarkServiceInvoker serviceInvoker = new BenchmarkServiceInvoker();

                // Register services with configured rollback percentage
                // Divide rollback percentage by 3 for each service so total probability is approximately correct
                int serviceRollbackPct = rollbackPercentage > 0 ? Math.max(1, rollbackPercentage / 3) : 0;

                serviceInvoker.registerService("orderService", new OrderSagaService(serviceRollbackPct, 5));
                serviceInvoker.registerService("inventoryService", new InventorySagaService(serviceRollbackPct, 5));
                serviceInvoker.registerService("paymentService", new PaymentSagaService(serviceRollbackPct, 5));

                // Register the service invoker for different service types
                getServiceInvokerManager().putServiceInvoker(DomainConstants.SERVICE_TYPE_SPRING_BEAN, serviceInvoker);
            }
        }

        private byte[] readAllBytes(InputStream is) throws IOException {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[8192];
            int nRead;
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return buffer.toByteArray();
        }
    }
}
