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
package org.apache.seata.benchmark.monitor;

import org.apache.seata.benchmark.model.BenchmarkMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Metrics collector with export capability
 */
public class MetricsCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsCollector.class);

    private final BenchmarkMetrics metrics;

    public MetricsCollector(BenchmarkMetrics metrics) {
        this.metrics = metrics;
    }

    public void exportToCsv(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("Metric,Value");
            writer.println("Total Transactions," + metrics.getTotalCount());
            writer.println("Success Count," + metrics.getSuccessCount());
            writer.println("Failed Count," + metrics.getFailedCount());
            writer.println("Success Rate (%)," + String.format("%.2f", metrics.getSuccessRate()));
            writer.println("Average TPS," + String.format("%.2f", metrics.getAverageTps()));
            writer.println("Elapsed Time (s)," + metrics.getElapsedTimeSeconds());

            BenchmarkMetrics.LatencyStats latency = metrics.getLatencyStats();
            writer.println("Latency P50 (ms)," + latency.getP50());
            writer.println("Latency P95 (ms)," + latency.getP95());
            writer.println("Latency P99 (ms)," + latency.getP99());
            writer.println("Latency Max (ms)," + latency.getMax());

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            writer.println("Export Time," + sdf.format(new Date()));

            LOGGER.info("Metrics exported to: {}", filename);
        } catch (IOException e) {
            LOGGER.error("Failed to export metrics to CSV", e);
        }
    }

    public String generateFinalReport() {
        StringBuilder report = new StringBuilder();
        report.append("\n");
        report.append("===================================================\n");
        report.append("           Seata Benchmark Final Report\n");
        report.append("===================================================\n");
        report.append(String.format("Total Transactions:    %,d\n", metrics.getTotalCount()));
        report.append(String.format("Success Count:         %,d\n", metrics.getSuccessCount()));
        report.append(String.format("Failed Count:          %,d\n", metrics.getFailedCount()));
        report.append(String.format("Success Rate:          %.2f%%\n", metrics.getSuccessRate()));
        report.append(String.format("Average TPS:           %.2f\n", metrics.getAverageTps()));
        report.append(String.format("Elapsed Time:          %d seconds\n", metrics.getElapsedTimeSeconds()));
        report.append("\n");
        report.append("Latency Statistics:\n");

        BenchmarkMetrics.LatencyStats latency = metrics.getLatencyStats();
        report.append(String.format("  P50:                 %d ms\n", latency.getP50()));
        report.append(String.format("  P95:                 %d ms\n", latency.getP95()));
        report.append(String.format("  P99:                 %d ms\n", latency.getP99()));
        report.append(String.format("  Max:                 %d ms\n", latency.getMax()));
        report.append("===================================================\n");

        return report.toString();
    }
}
