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
package org.apache.seata.benchmark;

import org.apache.seata.benchmark.config.BenchmarkConfig;
import org.apache.seata.benchmark.executor.ATModeExecutor;
import org.apache.seata.benchmark.executor.SagaAnnotationModeExecutor;
import org.apache.seata.benchmark.executor.SagaModeExecutor;
import org.apache.seata.benchmark.executor.TCCModeExecutor;
import org.apache.seata.benchmark.executor.TransactionExecutor;
import org.apache.seata.benchmark.executor.WorkloadGenerator;
import org.apache.seata.benchmark.model.BenchmarkMetrics;
import org.apache.seata.benchmark.monitor.MetricsCollector;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.rpc.ShutdownHook;
import org.apache.seata.core.rpc.netty.RmNettyRemotingClient;
import org.apache.seata.core.rpc.netty.TmNettyRemotingClient;
import org.apache.seata.rm.RMClient;
import org.apache.seata.tm.TMClient;

/**
 * Benchmark runner that executes the benchmark logic.
 * Separated from CLI application for better testability and reusability.
 */
public class BenchmarkRunner {

    private final BenchmarkConfig config;

    public BenchmarkRunner(BenchmarkConfig config) {
        this.config = config;
    }

    /**
     * Run the benchmark.
     *
     * @return exit code (0 for success, 1 for failure)
     */
    public int run() {
        return run(null);
    }

    /**
     * Run the benchmark with optional CSV export.
     *
     * @param exportCsv path to export CSV file, or null to skip export
     * @return exit code (0 for success, 1 for failure)
     */
    public int run(String exportCsv) {
        TransactionExecutor executor = null;
        WorkloadGenerator workloadGenerator = null;

        try {
            initSeataClient();

            executor = createExecutor();
            executor.init();

            BenchmarkMetrics metrics = new BenchmarkMetrics();
            MetricsCollector metricsCollector = new MetricsCollector(config, metrics);
            workloadGenerator = new WorkloadGenerator(config, executor, metrics);

            System.out.println("Starting benchmark...\n");
            workloadGenerator.start();
            workloadGenerator.waitForCompletion();

            System.out.println("\n" + metricsCollector.generateFinalReport());

            if (exportCsv != null) {
                metricsCollector.exportToCsv(exportCsv);
                System.out.println("\nMetrics exported to: " + exportCsv);
            }

            return 0;

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return 1;

        } finally {
            if (workloadGenerator != null) {
                workloadGenerator.stop();
            }
            if (executor != null) {
                executor.destroy();
            }
        }
    }

    private void initSeataClient() {
        System.out.println("Initializing Seata client...");

        String appId = config.getApplicationId();
        String txGroup = config.getTxServiceGroup();

        TMClient.init(appId, txGroup);
        RMClient.init(appId, txGroup);

        ShutdownHook shutdownHook = ShutdownHook.getInstance();
        shutdownHook.addDisposable(TmNettyRemotingClient.getInstance(appId, txGroup));
        shutdownHook.addDisposable(RmNettyRemotingClient.getInstance(appId, txGroup));

        System.out.println("Seata client initialized\n");
    }

    private TransactionExecutor createExecutor() {
        boolean isRealMode = config.getBranches() > 0;
        BranchType branchType = config.getMode();

        switch (branchType) {
            case AT:
                String atMode = isRealMode ? " (MySQL via Testcontainers)" : " (empty transaction)";
                System.out.println("Creating AT mode executor" + atMode + "\n");
                return new ATModeExecutor(config);
            case TCC:
                String tccMode = isRealMode ? " (try/confirm/cancel)" : " (empty transaction)";
                System.out.println("Creating TCC mode executor" + tccMode + "\n");
                return new TCCModeExecutor(config);
            case SAGA:
                String sagaMode = isRealMode ? " (state machine engine)" : " (empty transaction)";
                System.out.println("Creating Saga mode executor" + sagaMode + "\n");
                return new SagaModeExecutor(config);
            case SAGA_ANNOTATION:
                String sagaAnnotationMode = isRealMode ? " (annotation-based compensation)" : " (empty transaction)";
                System.out.println("Creating SAGA_ANNOTATION mode executor" + sagaAnnotationMode + "\n");
                return new SagaAnnotationModeExecutor(config);
            default:
                throw new IllegalArgumentException(
                        "Unsupported mode: " + branchType + ". Only AT, TCC, SAGA, and SAGA_ANNOTATION are supported.");
        }
    }
}
