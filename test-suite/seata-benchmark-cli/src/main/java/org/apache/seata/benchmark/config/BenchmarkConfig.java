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
package org.apache.seata.benchmark.config;

import org.apache.seata.benchmark.constant.BenchmarkConstants;
import org.apache.seata.core.model.BranchType;

import java.util.EnumSet;
import java.util.Locale;

/**
 * Benchmark configuration
 */
public class BenchmarkConfig {

    private String server = "127.0.0.1:8091";
    private BranchType mode = BranchType.AT;
    private int targetTps = 100;
    private int threads = 10;
    private int duration = 60;
    private int warmupDuration = 0;
    private String applicationId = "benchmark-app";
    private String txServiceGroup = "default_tx_group";
    private int rollbackPercentage = 2;
    private int branches = 0;
    private String sagaShape;
    private String sagaWorkload = "mock";
    private String sagaFailStep;
    private Long sagaRandomSeed;
    private String sagaTimeoutStep;
    private int sagaTimeoutMs = 3000;

    public BranchType getMode() {
        return mode;
    }

    public void setMode(BranchType mode) {
        this.mode = mode;
    }

    public void setMode(String mode) {
        this.mode = BranchType.get(mode);
    }

    public int getTargetTps() {
        return targetTps;
    }

    public void setTargetTps(int targetTps) {
        this.targetTps = targetTps;
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getWarmupDuration() {
        return warmupDuration;
    }

    public void setWarmupDuration(int warmupDuration) {
        this.warmupDuration = warmupDuration;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getTxServiceGroup() {
        return txServiceGroup;
    }

    public void setTxServiceGroup(String txServiceGroup) {
        this.txServiceGroup = txServiceGroup;
    }

    public int getRollbackPercentage() {
        return rollbackPercentage;
    }

    public void setRollbackPercentage(int rollbackPercentage) {
        this.rollbackPercentage = rollbackPercentage;
    }

    public int getBranches() {
        return branches;
    }

    public void setBranches(int branches) {
        this.branches = branches;
    }

    public String getSagaShape() {
        return sagaShape;
    }

    public void setSagaShape(String sagaShape) {
        this.sagaShape = sagaShape;
    }

    public String getSagaWorkload() {
        return sagaWorkload;
    }

    public void setSagaWorkload(String sagaWorkload) {
        this.sagaWorkload = sagaWorkload;
    }

    public String getSagaFailStep() {
        return sagaFailStep;
    }

    public void setSagaFailStep(String sagaFailStep) {
        this.sagaFailStep = sagaFailStep;
    }

    public Long getSagaRandomSeed() {
        return sagaRandomSeed;
    }

    public void setSagaRandomSeed(Long sagaRandomSeed) {
        this.sagaRandomSeed = sagaRandomSeed;
    }

    public String getSagaTimeoutStep() {
        return sagaTimeoutStep;
    }

    public void setSagaTimeoutStep(String sagaTimeoutStep) {
        this.sagaTimeoutStep = sagaTimeoutStep;
    }

    public int getSagaTimeoutMs() {
        return sagaTimeoutMs;
    }

    public void setSagaTimeoutMs(int sagaTimeoutMs) {
        this.sagaTimeoutMs = sagaTimeoutMs;
    }

    public void validate() {
        validateMode();
        validateNotEmpty(server, "server");
        validateNotEmpty(applicationId, "applicationId");
        validateNotEmpty(txServiceGroup, "txServiceGroup");
        validatePositive(targetTps, "targetTps");
        validatePositive(threads, "threads");
        validatePositive(duration, "duration");
        validateNonNegative(warmupDuration, "warmupDuration");
        validateNonNegative(branches, "branches");
        validateRange(rollbackPercentage, 0, 100, "rollbackPercentage");
        validateSagaShape();
        validateSagaWorkload();
        validateSagaFailStep();
        validateSagaTimeoutStep();
        validateNonNegative(sagaTimeoutMs, "sagaTimeoutMs");
        validateTpsAndThreads();
    }

    private static final EnumSet<BranchType> SUPPORTED_MODES =
            EnumSet.of(BranchType.AT, BranchType.TCC, BranchType.SAGA, BranchType.SAGA_ANNOTATION);

    private void validateMode() {
        if (!SUPPORTED_MODES.contains(mode)) {
            throw new IllegalArgumentException(
                    "Unsupported mode: " + mode + ". Only AT, TCC, SAGA, and SAGA_ANNOTATION are supported.");
        }
    }

    private void validateNotEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
    }

    private void validatePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }

    private void validateNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative");
        }
    }

    private void validateRange(int value, int min, int max, String fieldName) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(fieldName + " must be between " + min + " and " + max);
        }
    }

    private void validateSagaShape() {
        if (sagaShape == null || sagaShape.trim().isEmpty()) {
            return;
        }

        String normalized = sagaShape.trim().toLowerCase(Locale.ROOT);
        if (!"simple".equals(normalized) && !"order".equals(normalized)) {
            throw new IllegalArgumentException("sagaShape must be one of: simple, order");
        }
        sagaShape = normalized;
    }

    private void validateSagaWorkload() {
        if (sagaWorkload == null || sagaWorkload.trim().isEmpty()) {
            sagaWorkload = "mock";
            return;
        }

        String normalized = sagaWorkload.trim().toLowerCase(Locale.ROOT);
        if (!"mock".equals(normalized) && !"db".equals(normalized)) {
            throw new IllegalArgumentException("sagaWorkload must be one of: mock, db");
        }
        sagaWorkload = normalized;
    }

    private void validateSagaFailStep() {
        if (sagaFailStep == null || sagaFailStep.trim().isEmpty()) {
            return;
        }

        String normalized = sagaFailStep.trim().toLowerCase(Locale.ROOT);
        if (!"inventory".equals(normalized) && !"payment".equals(normalized) && !"order".equals(normalized)) {
            throw new IllegalArgumentException("sagaFailStep must be one of: inventory, payment, order");
        }
        sagaFailStep = normalized;
    }

    private void validateSagaTimeoutStep() {
        if (sagaTimeoutStep == null || sagaTimeoutStep.trim().isEmpty()) {
            return;
        }

        String normalized = sagaTimeoutStep.trim().toLowerCase(Locale.ROOT);
        if (!"inventory".equals(normalized) && !"payment".equals(normalized) && !"order".equals(normalized)) {
            throw new IllegalArgumentException("sagaTimeoutStep must be one of: inventory, payment, order");
        }
        sagaTimeoutStep = normalized;
    }

    private void validateTpsAndThreads() {
        if (targetTps < BenchmarkConstants.UNLIMITED_TPS_THRESHOLD && threads > 1) {
            throw new IllegalArgumentException(String.format(
                    "Invalid configuration: Cannot use both fixed TPS (%d) and multiple threads (%d). "
                            + "Performance testing requires clear semantics. Please choose ONE of the following modes:\n"
                            + "  1. Fixed Concurrency Mode: Set --threads=<N> and --tps=%d or higher (tests max throughput under fixed concurrency)\n"
                            + "  2. Fixed TPS Mode: Set --tps=<target> and --threads=1 (tests latency at target throughput)\n"
                            + "See README.md for detailed usage examples.",
                    targetTps, threads, BenchmarkConstants.UNLIMITED_TPS_THRESHOLD));
        }
    }
}
