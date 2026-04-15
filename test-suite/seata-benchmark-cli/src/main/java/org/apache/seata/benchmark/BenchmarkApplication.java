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
import org.apache.seata.benchmark.config.BenchmarkConfigLoader;
import org.apache.seata.benchmark.constant.BenchmarkConstants;
import org.apache.seata.core.model.BranchType;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * Seata Benchmark CLI Application.
 * Handles command-line argument parsing and delegates execution to BenchmarkRunner.
 */
@Command(
        name = "seata-benchmark",
        version = "Seata Benchmark CLI 1.0.0",
        description = "Command-line benchmark tool for Seata transaction modes",
        mixinStandardHelpOptions = true)
public class BenchmarkApplication implements Callable<Integer> {

    @Option(
            names = {"-s", "--server"},
            description = "Seata Server address (host:port)",
            required = false)
    private String server;

    @Option(
            names = {"-m", "--mode"},
            description = "Transaction mode: AT, TCC, SAGA, or SAGA_ANNOTATION",
            required = false)
    private String mode;

    @Option(
            names = {"-t", "--tps"},
            description = "Target transactions per second (default: 100)")
    private Integer targetTps;

    @Option(
            names = {"--threads"},
            description = "Number of concurrent threads (default: 10)")
    private Integer threads;

    @Option(
            names = {"-d", "--duration"},
            description = "Benchmark duration in seconds (default: 60)")
    private Integer duration;

    @Option(
            names = {"--warmup-duration"},
            description = "Warmup duration in seconds (default: 0)")
    private Integer warmupDuration;

    @Option(
            names = {"--export-csv"},
            description = "Export metrics to CSV file")
    private String exportCsv;

    @Option(
            names = {"--application-id"},
            description = "Seata application ID (default: benchmark-app)")
    private String applicationId;

    @Option(
            names = {"--tx-service-group"},
            description = "Seata transaction service group (default: default_tx_group)")
    private String txServiceGroup;

    @Option(
            names = {"--rollback-percentage"},
            description = "Rollback percentage for fault injection (0-100, default: 2)")
    private Integer rollbackPercentage;

    @Option(
            names = {"--branches"},
            description = "Number of branch transactions (0=empty mode, >=1=real mode with actual execution)")
    private Integer branches;

    @Option(
            names = {"--saga-shape"},
            description = "Select SAGA state machine shape: simple or order")
    private String sagaShape;

    @Option(
            names = {"--saga-workload"},
            description = "Select SAGA workload implementation: mock or db")
    private String sagaWorkload;

    @Option(
            names = {"--saga-fail-step"},
            description = "Force SAGA forward failure at a specific step: inventory, payment, or order")
    private String sagaFailStep;

    @Option(
            names = {"--saga-random-seed"},
            description = "Seed for reproducible SAGA failure injection")
    private Long sagaRandomSeed;

    @Option(
            names = {"--saga-timeout-step"},
            description = "Simulate SAGA timeout at a specific forward step: inventory, payment, or order")
    private String sagaTimeoutStep;

    @Option(
            names = {"--saga-timeout-ms"},
            description = "Simulated timeout delay in milliseconds for SAGA timeout injection (default: 3000)")
    private Integer sagaTimeoutMs;

    public static void main(String[] args) {
        // Parse server address from args before any Seata class loading
        String serverAddr = BenchmarkConstants.DEFAULT_SERVER_ADDRESS;
        String txGroup = BenchmarkConstants.DEFAULT_TX_GROUP;
        for (int i = 0; i < args.length; i++) {
            if (("-s".equals(args[i]) || "--server".equals(args[i])) && i + 1 < args.length) {
                serverAddr = args[i + 1];
            }
            if ("--tx-service-group".equals(args[i]) && i + 1 < args.length) {
                txGroup = args[i + 1];
            }
        }

        // Set Seata configuration properties BEFORE any Seata class loading
        System.setProperty("seata.config.type", "file");
        System.setProperty("seata.registry.type", "file");
        System.setProperty("service.default.grouplist", serverAddr);
        System.setProperty("service.vgroupMapping." + txGroup, "default");

        int exitCode = new CommandLine(new BenchmarkApplication()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        System.out.println("===========================================");
        System.out.println("   Seata Benchmark CLI v1.0.0");
        System.out.println("===========================================\n");

        BenchmarkConfig config = buildConfig();
        config.validate();
        printConfiguration(config);

        BenchmarkRunner runner = new BenchmarkRunner(config);
        return runner.run(exportCsv);
    }

    private BenchmarkConfig buildConfig() {
        BenchmarkConfig config = new BenchmarkConfig();
        return BenchmarkConfigLoader.merge(
                config,
                server,
                mode,
                targetTps,
                threads,
                duration,
                warmupDuration,
                applicationId,
                txServiceGroup,
                rollbackPercentage,
                branches,
                sagaShape,
                sagaWorkload,
                sagaFailStep,
                sagaRandomSeed,
                sagaTimeoutStep,
                sagaTimeoutMs);
    }

    private void printConfiguration(BenchmarkConfig config) {
        System.out.println("Configuration:");
        System.out.println("  Server:       " + config.getServer());
        System.out.println("  Mode:         " + config.getMode());
        System.out.println("  Target TPS:   " + config.getTargetTps());
        System.out.println("  Threads:      " + config.getThreads());
        System.out.println("  Duration:     " + config.getDuration() + "s");
        if (config.getWarmupDuration() > 0) {
            System.out.println("  Warmup:       " + config.getWarmupDuration() + "s");
        }
        System.out.println("  Rollback %:   " + config.getRollbackPercentage() + "%");
        System.out.println("  Branches:     " + config.getBranches()
                + (config.getBranches() == 0 ? " (empty mode)" : " (real mode)"));
        if (config.getMode() == BranchType.SAGA
                && config.getSagaWorkload() != null
                && !config.getSagaWorkload().isEmpty()) {
            System.out.println("  Saga Workload:" + padValue(config.getSagaWorkload()));
        }
        if (config.getSagaShape() != null && !config.getSagaShape().isEmpty()) {
            System.out.println("  Saga Shape:   " + config.getSagaShape());
        }
        if (config.getSagaFailStep() != null && !config.getSagaFailStep().isEmpty()) {
            System.out.println("  Saga Fail:    " + config.getSagaFailStep());
        }
        if (config.getSagaRandomSeed() != null) {
            System.out.println("  Saga Seed:    " + config.getSagaRandomSeed());
        }
        if (config.getSagaTimeoutStep() != null && !config.getSagaTimeoutStep().isEmpty()) {
            System.out.println("  Saga Timeout: " + config.getSagaTimeoutStep());
            System.out.println("  Timeout Ms:   " + config.getSagaTimeoutMs());
        }
        System.out.println();
    }

    private String padValue(String value) {
        return value == null ? "" : "   " + value;
    }
}
