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
import org.apache.seata.benchmark.saga.InventoryDbSagaService;
import org.apache.seata.benchmark.saga.InventorySagaService;
import org.apache.seata.benchmark.saga.OrderDbSagaService;
import org.apache.seata.benchmark.saga.OrderSagaService;
import org.apache.seata.benchmark.saga.PaymentDbSagaService;
import org.apache.seata.benchmark.saga.PaymentSagaService;
import org.apache.seata.benchmark.saga.SagaDbEnvironment;
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
    private static final String STEP_INVENTORY = "inventory";
    private static final String STEP_PAYMENT = "payment";
    private static final String STEP_ORDER = "order";
    private static final String SHAPE_SIMPLE = "simple";
    private static final String SHAPE_ORDER = "order";
    private static final String WORKLOAD_DB = "db";

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
            stateMachineConfig.setSagaFailStep(config.getSagaFailStep());
            stateMachineConfig.setSagaRandomSeed(config.getSagaRandomSeed());
            stateMachineConfig.setSagaTimeoutStep(config.getSagaTimeoutStep());
            stateMachineConfig.setSagaTimeoutMs(config.getSagaTimeoutMs());
            stateMachineConfig.setSagaWorkload(config.getSagaWorkload());
            stateMachineConfig.setBenchmarkConfig(config);
            stateMachineConfig.init();

            // Create state machine engine
            ProcessCtrlStateMachineEngine engine = new ProcessCtrlStateMachineEngine();
            engine.setStateMachineConfig(stateMachineConfig);
            this.stateMachineEngine = engine;

            LOGGER.info("Real Saga mode executor initialized");
            LOGGER.info("Available state machines: {}, {}", SIMPLE_SAGA_NAME, ORDER_SAGA_NAME);

        } catch (Exception e) {
            if (stateMachineConfig != null) {
                stateMachineConfig.destroy();
            }
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
            String stateMachineName = resolveStateMachineName(branchCount);

            // Prepare start parameters
            Map<String, Object> startParams = createStartParams();

            // Execute state machine
            StateMachineInstance instance = stateMachineEngine.startWithBusinessKey(
                    stateMachineName, stateMachineConfig.getDefaultTenantId(), businessKey, startParams);

            // Check execution result
            ExecutionStatus executionStatus = instance.getStatus();
            ExecutionStatus compensationStatus = instance.getCompensationStatus();

            if (ExecutionStatus.SU.equals(compensationStatus)) {
                status = STATUS_COMPENSATED;
            } else if (ExecutionStatus.FA.equals(compensationStatus)) {
                status = STATUS_COMPENSATION_FAILED;
            } else if (ExecutionStatus.UN.equals(compensationStatus)) {
                status = STATUS_UNKNOWN;
            } else if (ExecutionStatus.SU.equals(executionStatus)) {
                status = STATUS_COMMITTED;
                success = true;
            } else if (ExecutionStatus.FA.equals(executionStatus)) {
                status = STATUS_FAILED;
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

    private String resolveStateMachineName(int branchCount) {
        if (SHAPE_ORDER.equals(config.getSagaShape())) {
            return ORDER_SAGA_NAME;
        }
        if (SHAPE_SIMPLE.equals(config.getSagaShape())) {
            return SIMPLE_SAGA_NAME;
        }
        return branchCount >= 3 ? ORDER_SAGA_NAME : SIMPLE_SAGA_NAME;
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
        if (stateMachineConfig != null) {
            stateMachineConfig.destroy();
        }
        stateMachineEngine = null;
        stateMachineConfig = null;
    }

    /**
     * Custom StateMachineConfig for benchmark testing.
     */
    private static class BenchmarkStateMachineConfig extends AbstractStateMachineConfig {

        private int rollbackPercentage = 0;
        private String sagaFailStep;
        private Long sagaRandomSeed;
        private String sagaTimeoutStep;
        private int sagaTimeoutMs = 3000;
        private String sagaWorkload = "mock";
        private BenchmarkConfig benchmarkConfig;
        private SagaDbEnvironment sagaDbEnvironment;
        private OrderSagaService orderSagaService;
        private InventorySagaService inventorySagaService;
        private PaymentSagaService paymentSagaService;
        private OrderDbSagaService orderDbSagaService;
        private InventoryDbSagaService inventoryDbSagaService;
        private PaymentDbSagaService paymentDbSagaService;

        public void setRollbackPercentage(int rollbackPercentage) {
            this.rollbackPercentage = rollbackPercentage;
        }

        public void setSagaFailStep(String sagaFailStep) {
            this.sagaFailStep = sagaFailStep;
        }

        public void setSagaRandomSeed(Long sagaRandomSeed) {
            this.sagaRandomSeed = sagaRandomSeed;
        }

        public void setSagaTimeoutStep(String sagaTimeoutStep) {
            this.sagaTimeoutStep = sagaTimeoutStep;
        }

        public void setSagaTimeoutMs(int sagaTimeoutMs) {
            this.sagaTimeoutMs = sagaTimeoutMs;
        }

        public void setSagaWorkload(String sagaWorkload) {
            this.sagaWorkload = sagaWorkload;
        }

        public void setBenchmarkConfig(BenchmarkConfig benchmarkConfig) {
            this.benchmarkConfig = benchmarkConfig;
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

                boolean restrictFailureStep =
                        sagaFailStep != null && !sagaFailStep.trim().isEmpty();
                int serviceRollbackPct = restrictFailureStep
                        ? rollbackPercentage
                        : (rollbackPercentage > 0 ? Math.max(1, rollbackPercentage / 3) : 0);

                boolean orderFailEnabled = !restrictFailureStep || STEP_ORDER.equals(sagaFailStep);
                boolean inventoryFailEnabled = !restrictFailureStep || STEP_INVENTORY.equals(sagaFailStep);
                boolean paymentFailEnabled = !restrictFailureStep || STEP_PAYMENT.equals(sagaFailStep);
                boolean orderTimeoutEnabled = STEP_ORDER.equals(sagaTimeoutStep);
                boolean inventoryTimeoutEnabled = STEP_INVENTORY.equals(sagaTimeoutStep);
                boolean paymentTimeoutEnabled = STEP_PAYMENT.equals(sagaTimeoutStep);

                if (WORKLOAD_DB.equals(sagaWorkload)) {
                    initDbEnvironment();
                    registerDbServices(
                            serviceInvoker,
                            serviceRollbackPct,
                            orderFailEnabled,
                            inventoryFailEnabled,
                            paymentFailEnabled,
                            orderTimeoutEnabled,
                            inventoryTimeoutEnabled,
                            paymentTimeoutEnabled);
                } else {
                    registerMockServices(
                            serviceInvoker,
                            serviceRollbackPct,
                            orderFailEnabled,
                            inventoryFailEnabled,
                            paymentFailEnabled,
                            orderTimeoutEnabled,
                            inventoryTimeoutEnabled,
                            paymentTimeoutEnabled);
                }

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

        private ThreadLocal<java.util.Random> createFailureRandom(int salt) {
            return org.apache.seata.benchmark.saga.FailureRandomProvider.create(
                    sagaRandomSeed == null ? null : sagaRandomSeed + salt);
        }

        private void registerMockServices(
                BenchmarkServiceInvoker serviceInvoker,
                int serviceRollbackPct,
                boolean orderFailEnabled,
                boolean inventoryFailEnabled,
                boolean paymentFailEnabled,
                boolean orderTimeoutEnabled,
                boolean inventoryTimeoutEnabled,
                boolean paymentTimeoutEnabled) {
            orderSagaService = new OrderSagaService(
                    serviceRollbackPct,
                    5,
                    orderFailEnabled,
                    createFailureRandom(11),
                    orderTimeoutEnabled,
                    sagaTimeoutMs);
            serviceInvoker.registerService("orderService", orderSagaService);
            inventorySagaService = new InventorySagaService(
                    serviceRollbackPct,
                    5,
                    inventoryFailEnabled,
                    createFailureRandom(17),
                    inventoryTimeoutEnabled,
                    sagaTimeoutMs);
            serviceInvoker.registerService("inventoryService", inventorySagaService);
            paymentSagaService = new PaymentSagaService(
                    serviceRollbackPct,
                    5,
                    paymentFailEnabled,
                    createFailureRandom(23),
                    paymentTimeoutEnabled,
                    sagaTimeoutMs);
            serviceInvoker.registerService("paymentService", paymentSagaService);
        }

        private void registerDbServices(
                BenchmarkServiceInvoker serviceInvoker,
                int serviceRollbackPct,
                boolean orderFailEnabled,
                boolean inventoryFailEnabled,
                boolean paymentFailEnabled,
                boolean orderTimeoutEnabled,
                boolean inventoryTimeoutEnabled,
                boolean paymentTimeoutEnabled) {
            orderDbSagaService = new OrderDbSagaService(
                    sagaDbEnvironment.getDataSource(),
                    serviceRollbackPct,
                    5,
                    orderFailEnabled,
                    createFailureRandom(11),
                    orderTimeoutEnabled,
                    sagaTimeoutMs);
            serviceInvoker.registerService("orderService", orderDbSagaService);
            inventoryDbSagaService = new InventoryDbSagaService(
                    sagaDbEnvironment.getDataSource(),
                    serviceRollbackPct,
                    5,
                    inventoryFailEnabled,
                    createFailureRandom(17),
                    inventoryTimeoutEnabled,
                    sagaTimeoutMs);
            serviceInvoker.registerService("inventoryService", inventoryDbSagaService);
            paymentDbSagaService = new PaymentDbSagaService(
                    sagaDbEnvironment.getDataSource(),
                    serviceRollbackPct,
                    5,
                    paymentFailEnabled,
                    createFailureRandom(23),
                    paymentTimeoutEnabled,
                    sagaTimeoutMs);
            serviceInvoker.registerService("paymentService", paymentDbSagaService);
        }

        private void initDbEnvironment() {
            if (benchmarkConfig == null) {
                throw new IllegalStateException("BenchmarkConfig is required for DB-backed Saga workload");
            }
            sagaDbEnvironment = new SagaDbEnvironment(benchmarkConfig);
            sagaDbEnvironment.init();
        }

        public void destroy() {
            destroyServices();
            if (sagaDbEnvironment != null) {
                sagaDbEnvironment.destroy();
                sagaDbEnvironment = null;
            }
        }

        private void destroyServices() {
            if (orderSagaService != null) {
                orderSagaService.destroy();
                orderSagaService = null;
            }
            if (inventorySagaService != null) {
                inventorySagaService.destroy();
                inventorySagaService = null;
            }
            if (paymentSagaService != null) {
                paymentSagaService.destroy();
                paymentSagaService = null;
            }
            if (orderDbSagaService != null) {
                orderDbSagaService.destroy();
                orderDbSagaService = null;
            }
            if (inventoryDbSagaService != null) {
                inventoryDbSagaService.destroy();
                inventoryDbSagaService = null;
            }
            if (paymentDbSagaService != null) {
                paymentDbSagaService.destroy();
                paymentDbSagaService = null;
            }
        }
    }
}
